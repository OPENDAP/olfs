/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.noaa_s3;

import opendap.bes.BESManager;
import opendap.coreServlet.*;
import opendap.logging.LogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/13/13
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3CatalogServlet extends HttpServlet {

    private Logger _log;
    private boolean _initialized;

    //private String systemPath;
    private AtomicInteger _reqNumber;


    private String _servletContext;

    private S3DapDispatchHandler _s3DapDispatcher;



    public S3CatalogServlet() {

        _log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;
        _reqNumber = new AtomicInteger(0);
        _servletContext = null;
    }



    @Override
    public void init() throws ServletException {

        if(_initialized) return;


        _servletContext = this.getServletContext().getContextPath();


        /**
         * ###########################################################################
         *
         * These things could be in a configuration file
         *
         */
        String s3BucketContext = "/nodc";
        String s3BucketName = "ocean-archive.data.nodc.noaa.gov";
        S3CatalogManager.theManager().addBucket(s3BucketContext,s3BucketName);


        String dapServiceContextPath = _servletContext +"/dap";
        S3CatalogManager.theManager().setDapServiceContext(dapServiceContextPath);


        String catalogServiceContextPath = _servletContext+"/catalog";
        S3CatalogManager.theManager().setCatalogServiceContext(catalogServiceContextPath);

        Element besConfiguration = getDefaultBesManagerConfig();

        /**
         * ###########################################################################
         */



        _s3DapDispatcher = new S3DapDispatchHandler(S3CatalogManager.theManager().getDapServiceContext());

        try {
            BESManager besManager = new BESManager();
            besManager.init(this,besConfiguration);
            _s3DapDispatcher.init(this, getDefaultDapDispatchConfig() );

        } catch (Exception e) {
            _log.error("Failed to initialize {}", _s3DapDispatcher.getClass().getName());
            throw new ServletException(e);
        }






        _initialized = true;
    }

    Element getDefaultDapDispatchConfig() {
        Element config = new Element("config");

        config.addContent(new Element("AllowDirectDataSourceAccess"));

        return config;
    }


    Element getDefaultBesManagerConfig() {

        Element e;
        Element handler = new Element("Handler");
        handler.setAttribute("className","opendap.bes.BESManager");

        Element bes = new Element("BES");
        handler.addContent(bes);

        e = new Element("prefix");
        e.setText("/");
        bes.addContent(e);


        e = new Element("host");
        e.setText("localhost");
        bes.addContent(e);


        //e = new Element("adminPort");
        //e.setText("11022");
        //bes.addContent(e);


        e = new Element("port");
        e.setText("10022");
        bes.addContent(e);


        e = new Element("maxResponseSize");
        e.setText("0");
        bes.addContent(e);


        e = new Element("ClientPool");
        e.setAttribute("maximum","10");
        e.setAttribute("maxCmds","2000");
        bes.addContent(e);



        return handler;
    }







    private boolean redirectToCatalog(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String relativeUrl =ReqInfo.getLocalUrl(req);
        String redirectTo;
        if(relativeUrl.endsWith("/")){
            redirectTo = S3CatalogManager.theManager().getCatalogServiceContext() + relativeUrl + "catalog.xml";
            res.sendRedirect(Scrub.urlContent(redirectTo));
            _log.debug("redirectToCatalog() Redirected request for node to THREDDS catalog: {}", redirectTo);
            return true;

        }

        return false;
    }


    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        try {
            LogUtil.logServerAccessStart(request, "S3_ACCESS", "HTTP-GET", Integer.toString(_reqNumber.incrementAndGet()));

            if (!redirectToCatalog(request, response)) {  // Do we send the catalog redirect?


                String requestURI = request.getRequestURI();

                if (requestURI.startsWith(S3CatalogManager.theManager().getCatalogServiceContext())) {
                    if (!directoryDispatch(request, response)) {  // Is it a catalog request?
                        // We don't know how to cope, looks like it's time to 404!
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to locate requested resource.");
                        _log.info("Sent 404 Response.");

                    }
                } else if (requestURI.startsWith(S3CatalogManager.theManager().getDapServiceContext())) {

                    if (!_s3DapDispatcher.requestDispatch(request, response, true)) { // Is it a DAP request?

                        // We don't know how to cope, looks like it's time to 404!
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to locate requested resource.");
                        _log.info("Sent 404 Response.");

                    }
                }
                else {   // It's not a catalog request, and it's not a dap request, so:
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
            LogUtil.logServerAccessEnd(0, -1, "S3_ACCESS");
        }

    }

    public long getLastModified(HttpServletRequest req) {


        RequestCache.openThreadCache();
        long reqno = _reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "S3", "LastModified", Long.toString(reqno));

        _log.debug("getLastModified() - BEGIN");

        try {
            LogUtil.logServerAccessStart(req, "S3CATALOG_ACCESS", "LastModified", Long.toString(reqno));

            String requestURI = req.getRequestURI();

            if(requestURI.startsWith(S3CatalogManager.theManager().getCatalogServiceContext())){
                return getCatalogLastModified(req);
            }
            else if (requestURI.startsWith(S3CatalogManager.theManager().getDapServiceContext())) {
                return _s3DapDispatcher.getLastModified(req);
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

        String requestUrl = req.getRequestURL().toString();
        String relativeURL = ReqInfo.getLocalUrl(req);

        S3Index s3i;
        boolean newIndex = false;

        s3i = S3CatalogManager.theManager().getIndex(requestUrl);

        if(s3i == null) {
            String bucketName = S3CatalogManager.theManager().getBucketName(relativeURL);
            String bucketContext = S3CatalogManager.theManager().getBucketContext(relativeURL);

            s3i = new S3Index(req, bucketContext, bucketName);
            newIndex = true;
            _log.debug("getLastModified() - Making new S3Index for '{}'",requestUrl);
        }
        else {
            _log.debug("getLastModified() - Retrieved cached S3Index for '{}'",requestUrl);
        }

        long lmt = s3i.getLastModified();

        if(lmt!=-1 && newIndex){
            S3CatalogManager.theManager().putIndex(requestUrl, s3i);
            _log.debug("getLastModified() - Cached S3Index for '{}'",requestUrl);
        }


        _log.debug("getLastModified() - END ({})",lmt);


        return lmt;


    }


    /**
     * Performs dispatching for file requests. If a request is not for a
     * special service or an OPeNDAP service then we attempt to resolve the
     * request to a "file" located within the context of the data handler and
     * return it's contents.
     *
     * @param request      .
     * @param response     .
     * @return true if the request was serviced as a directory request, false
     *         otherwise.
     * @throws Exception .
     */
    private boolean directoryDispatch(HttpServletRequest request,
                                     HttpServletResponse response)  {

        _log.debug("directoryDispatch() - BEGIN");

        boolean handled = false;


        String requestUrl = request.getRequestURL().toString();
        String relativeUrl = ReqInfo.getLocalUrl(request);

        String bucketContext = S3CatalogManager.theManager().getBucketContext(relativeUrl);
        String bucketName = S3CatalogManager.theManager().getBucketName(relativeUrl);

        S3Index s3i;

        s3i = S3CatalogManager.theManager().getIndex(requestUrl);
        boolean newIndex = false;
        if(s3i == null) {
            s3i = new S3Index(request,bucketContext, bucketName);
            newIndex = true;
            _log.debug("directoryDispatch() - Making new S3Index for '{}'",requestUrl);
        }
        else {
            _log.debug("directoryDispatch() - Retrieved cached S3Index for '{}'",requestUrl);

        }


        try {

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            Element catalog = s3i.getThreddsCatalog();

            Document threddsDoc = new Document();


            HashMap<String,String> piMap = new HashMap<String,String>( 2 );
            piMap.put( "type", "text/xsl" );
            piMap.put( "href", _servletContext +"/xsl/threddsPresentation.xsl" );
            ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

            threddsDoc.addContent(pi);

            threddsDoc.setRootElement(catalog);


            response.setContentType("text/xml");

            xmlo.output(threddsDoc,response.getOutputStream());


            if(newIndex) {
                S3CatalogManager.theManager().putIndex(requestUrl, s3i);
                _log.debug("directoryDispatch() - Caching S3Index for '{}'",requestUrl);
            }

            handled = true;

        } catch (IOException e) {
            _log.error("Unable to access s3 object: {} Msg: {}",s3i.getS3IndexUrlString(),e.getMessage());
        } catch (JDOMException e) {
            _log.error("Unable to parse s3 Index: {} Msg: {}", s3i.getS3IndexUrlString(), e.getMessage());
        }

        _log.debug("directoryDispatch() - END ({})", handled);

        return handled;

    }





}
