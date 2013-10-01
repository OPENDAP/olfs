/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.aws.glacier;

import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Scrub;
import opendap.logging.LogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/21/13
 * Time: 7:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class CatalogServlet extends HttpServlet {

    private Logger _log;
    private boolean _initialized;

    //private String systemPath;
    private AtomicInteger _reqNumber;


    private String _servletContext;




    public CatalogServlet() {
        _initialized = false;
        _reqNumber = new AtomicInteger(0);
        _servletContext = null;
    }



    @Override
    public void init() throws ServletException {

        if(_initialized) return;
        LogUtil.initLogging(this);
        _log = org.slf4j.LoggerFactory.getLogger(getClass());


        _servletContext = this.getServletContext().getContextPath();


        /**
         * ###########################################################################
         *
         * These things could be in a configuration file
         *
         */
        Element config = null;


        /**
         * ###########################################################################
         */



        try {
            GlacierArchiveManager.theManager().init(config);
        } catch (IOException e) {
            String msg = new StringBuilder().append("Failed to initialize the GlacierArchive Manager!! IOException: ").append(e.getMessage()).toString();
            e.printStackTrace();
            throw new ServletException(msg);
        } catch (JDOMException e) {
            String msg = new StringBuilder().append("Failed to initialize the GlacierArchive Manager!! IOException: ").append(e.getMessage()).toString();
            e.printStackTrace();
            throw new ServletException(msg);
        }




        _initialized = true;
    }




    private boolean redirectDirToCatalogXml(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String relativeUrl = ReqInfo.getLocalUrl(req);
        String redirectTo;
        if(relativeUrl.endsWith("/")){
            redirectTo = _servletContext + relativeUrl + "catalog.xml";
            res.sendRedirect(Scrub.urlContent(redirectTo));
            _log.debug("redirectDirToCatalogXml() Redirected request for node to THREDDS catalog: {}", redirectTo);
            return true;

        }

        return false;
    }


    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException,
            java.io.IOException{

        try {
            LogUtil.logServerAccessStart(request, "GLACIER_CATALOG_ACCESS", "HTTP-GET", Integer.toString(_reqNumber.incrementAndGet()));

            if (!redirectDirToCatalogXml(request, response)) {  // Do we send the catalog redirect?


                String requestURI = request.getRequestURI();

                if (requestURI.startsWith(_servletContext) ){
                    if (!directoryDispatch(request, response)) {  // Is it a catalog request?
                        // We don't know how to cope, looks like it's time to 404!
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to locate requested resource.");
                        _log.info("Sent 404 Response.");

                    }
                }
                else {   // It's not a catalog request, so:
                    // Looks like it's time to 404!
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to locate requested resource.");
                    _log.info("Sent 404 Response.");
                }


            }

        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            } catch (Throwable t2) {
                _log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            RequestCache.closeThreadCache();
            LogUtil.logServerAccessEnd(0, -1, "GLACIER_CATALOG_ACCESS");
        }

    }

    public long getLastModified(HttpServletRequest req) {


        RequestCache.openThreadCache();
        long reqno = _reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "GLACIER_CATALOG_ACCESS", "LastModified", Long.toString(reqno));

        _log.debug("getLastModified() - BEGIN");

        try {

            String requestURI = req.getRequestURI();

            if(requestURI.startsWith(_servletContext)){
                return getCatalogLastModified(req);
            }
            return -1;

        }
        finally {
            _log.debug("getLastModified() - END");

        }



    }



    public void destroy() {
        _log.info("Destroy complete.");

    }





    public long getCatalogLastModified(HttpServletRequest req){

        String resourceId = getCatalogIndexResourceId(req);

        long lmt = -1;

        Index index = GlacierArchiveManager.theManager().getIndex(resourceId);

        if(index == null) {
            _log.debug("getLastModified() - The requested index '{}' was not Found.",resourceId);
        }
        else {
            _log.debug("getLastModified() - Retrieved cached Index for '{}'",resourceId);
            lmt = index.getLastModified();
        }



        _log.debug("getLastModified() - END ({})",lmt);


        return lmt;


    }


    private String _catalogSuffix = "catalog.xml";

    private String getCatalogIndexResourceId(HttpServletRequest request){
        String relativeUrl = ReqInfo.getLocalUrl(request);

        while(relativeUrl.startsWith("/") && relativeUrl.length()>1)
            relativeUrl = relativeUrl.substring(1);

        if(relativeUrl.endsWith("/catalog.xml"))
            relativeUrl = relativeUrl.substring(0,relativeUrl.lastIndexOf(_catalogSuffix)) + "/index.xml";

        return relativeUrl;


    }


    /**
     * Performs dispatching for index (aka catalog) requests.
     *
     * @param request      .
     * @param response     .
     * @return true if the request was serviced as a directory request, false
     *         otherwise.
     * @throws Exception .
     */
    private boolean directoryDispatch(HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {

        _log.debug("directoryDispatch() - BEGIN");

        boolean handled = false;



        String requestUrl = request.getRequestURL().toString();
        String indexResourceId = getCatalogIndexResourceId(request);

        Index index;



        String vaultName = GlacierArchiveManager.theManager().getVaultName(indexResourceId);

        //indexResourceId = indexResourceId.substring(vaultName.length());


        index = GlacierArchiveManager.theManager().getIndex(indexResourceId);


        if(index == null) {
            _log.debug("directoryDispatch() - No index associated with indexResourceId '{}'",indexResourceId);
            return false;
        }

        _log.debug("directoryDispatch() - Retrieved cached Glacier Index for '{}'",requestUrl);


        try {

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


            String catalogServiceContext = GlacierArchiveManager.theManager().getCatalogServiceContext(indexResourceId);
            String dapServiceContext = GlacierArchiveManager.theManager().getDapServiceContext(indexResourceId);


            Element catalog = index.getThreddsCatalog(vaultName,catalogServiceContext,dapServiceContext);

            Document threddsDoc = new Document();


            HashMap<String,String> piMap = new HashMap<String,String>( 2 );
            piMap.put( "type", "text/xsl" );
            piMap.put( "href", _servletContext +"/xsl/threddsPresentation.xsl" );
            ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

            threddsDoc.addContent(pi);

            threddsDoc.setRootElement(catalog);


            response.setContentType("text/xml");

            xmlo.output(threddsDoc,response.getOutputStream());

            handled = true;

        } catch (IOException e) {
            _log.error("Unable to access Index object: {} Msg: {}",index.getResourceId(),e.getMessage());
        } catch (JDOMException e) {
            _log.error("Unable to parse Index: {} Msg: {}", index.getResourceId(), e.getMessage());
        }

        _log.debug("directoryDispatch() - END ({})", handled);

        return handled;

    }



}
