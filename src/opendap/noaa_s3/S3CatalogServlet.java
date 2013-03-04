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
import opendap.namespaces.THREDDS;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/13/13
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3CatalogServlet extends HttpServlet {

    private Logger log;
    private boolean initialized;

    //private String systemPath;
    private AtomicInteger reqNumber;


    private static ConcurrentHashMap<String, S3Index> catalogNodes;



    private String s3ServiceContext = "/s3";

    // We could easily maintain a MAP of  buckets and contexts...
    private String s3BucketContext = "/index";
    private String s3BucketName = "ocean-archive.data.nodc.noaa.gov";


    private DispatchHandler _s3Dispatcher;

    public S3CatalogServlet() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;
        reqNumber = new AtomicInteger(0);
        catalogNodes = new ConcurrentHashMap<String, S3Index>();

    }



    @Override
    public void init() throws ServletException {

        if(initialized) return;

        _s3Dispatcher = new DispatchHandler(s3ServiceContext);

        try {
            BESManager besManager = new BESManager();
            besManager.init(this,getDefaultBesManagerConfig());
            _s3Dispatcher.init(this, getDefaultDapDispatchConfig() );
        } catch (Exception e) {
            log.error("Failed to initialize {}",_s3Dispatcher.getClass().getName());
            throw new ServletException(e);
        }

        initialized = true;
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
            redirectTo = s3ServiceContext + s3BucketContext + relativeUrl + "catalog.xml";
            res.sendRedirect(Scrub.urlContent(redirectTo));
            log.debug("redirectToCatalog() Redirected request for node to THREDDS catalog: {}", redirectTo);
            return true;

        }

        return false;
    }



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        try {
            LogUtil.logServerAccessStart(request, "S3CATALOG_ACCESS", "HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));

            if (!redirectToCatalog(request, response)) {

                if (!directoryDispatch(request, response)) {

                    if (!_s3Dispatcher.requestDispatch(request, response, true)) {

                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to locate requested resource.");
                        log.info("Sent 404 Response.");

                    }
                }

            }

        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            } catch (Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            RequestCache.closeThreadCache();
            LogUtil.logServerAccessEnd(0, -1, "S3CATALOG_ACCESS");
        }

    }

    public long getLastModified(HttpServletRequest req) {


        RequestCache.openThreadCache();

        log.debug("getLastModified() - BEGIN");

        long reqno = reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "S3CATALOG_ACCESS", "LastModified", Long.toString(reqno));


        String requestUrl = req.getRequestURL().toString();

        S3Index s3i;
        boolean newIndex = false;

        s3i = catalogNodes.get(requestUrl);

        if(s3i == null) {
            s3i = new S3Index(req,s3BucketName);
            newIndex = true;
            log.debug("getLastModified() - Making new S3Index for '{}'",requestUrl);
        }
        else {
            log.debug("getLastModified() - Retrieved cached S3Index for '{}'",requestUrl);
        }

        long lmt = s3i.getLastModified();

        if(lmt!=-1 && newIndex){
            catalogNodes.put(requestUrl, s3i);
            log.debug("getLastModified() - Caching S3Index for '{}'",requestUrl);
        }

        log.debug("getLastModified() - END ({})",lmt);


        return lmt;

    }


    public void destroy() {
        log.info("Destroy complete.");

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

        log.debug("directoryDispatch() - BEGIN");

        boolean handled = false;


        String requestUrl = request.getRequestURL().toString();

        S3Index s3i;

        s3i = catalogNodes.get(requestUrl);

        boolean newIndex = false;
        if(s3i == null) {
            s3i = new S3Index(request,s3BucketName);
            newIndex = true;
            log.debug("directoryDispatch() - Making new S3Index for '{}'",requestUrl);
        }
        else {
            log.debug("directoryDispatch() - Retrieved cached S3Index for '{}'",requestUrl);

        }


        try {


            String s3TestServiceContext = s3ServiceContext + s3BucketContext;

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            Element catalog = s3i.getThreddsCatalog(s3TestServiceContext,getThreddsCatalogServices());

            Document threddsDoc = new Document();


            HashMap<String,String> piMap = new HashMap<String,String>( 2 );
            piMap.put( "type", "text/xsl" );
            piMap.put( "href", s3ServiceContext+"/xsl/threddsPresentation.xsl" );
            ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

            threddsDoc.addContent(pi);

            threddsDoc.setRootElement(catalog);


            response.setContentType("text/xml");

            xmlo.output(threddsDoc,response.getOutputStream());


            if(newIndex) {
                catalogNodes.put(requestUrl, s3i);
                log.debug("directoryDispatch() - Caching S3Index for '{}'",requestUrl);
            }

            handled = true;

        } catch (IOException e) {
            log.error("Unable to access s3 object: {} Msg: {}",s3i.getS3IndexUrlString(),e.getMessage());
        } catch (JDOMException e) {
            log.error("Unable to parse s3 Index: {} Msg: {}", s3i.getS3IndexUrlString(), e.getMessage());
        }

        log.debug("directoryDispatch() - END ({})", handled);

        return handled;

    }

    private HashMap<String, Element> getThreddsCatalogServices(){
        HashMap<String,Element> services = new HashMap<String, Element>();


        Element service = new Element(THREDDS.SERVICE, THREDDS.NS);
        service.setAttribute("name","dap");
        service.setAttribute("serviceType","OPeNDAP");
        service.setAttribute("base",s3ServiceContext + s3BucketContext);

        services.put("dap",service);


        return services;
    }






}
