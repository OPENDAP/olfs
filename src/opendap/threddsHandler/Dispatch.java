/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.threddsHandler;

import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.wcs.*;
import org.slf4j.Logger;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.io.*;

import thredds.servlet.ServletUtil;

/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 3:46:50 PM
 */
public class Dispatch implements DispatchHandler{


    private Logger log;

    private Element config;
    private boolean initialized;
    private DispatchServlet dispatchServlet;
    private String _prefix;
    boolean useMemoryCache = false;

    public Dispatch() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        config = null;
        initialized = false;
        _prefix = "thredds/";

    }


    public void sendThreddsCatalogResponse(HttpServletRequest request,
                                HttpServletResponse response) throws Exception{

        String relativeURL = ReqInfo.getFullSourceName(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        boolean useXSLT = false;


        if(redirectForPrefixOnlyRequest(request,response))
            return;

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        if(requestSuffix!=null && requestSuffix.equals("html")){
            useXSLT = true;
            relativeURL = relativeURL.substring(0, relativeURL.lastIndexOf(".html")) + ".xml";
        }


        Document catDoc;
        if(relativeURL.equals(_prefix)){
            useXSLT = true;
            catDoc = CatalogManager.getTopLevelCatalogDocument();
        }
        else{



            if(relativeURL.startsWith(_prefix))
                relativeURL = relativeURL.substring(_prefix.length(),relativeURL.length());
            Catalog cat = CatalogManager.getCatalog(relativeURL);

            if(cat!=null){
                log.debug("\nFound catalog: "+relativeURL+"   " +
                        "    requestSuffix: "+requestSuffix+"   " +
                        "    prefix: " + _prefix


                );
                if(useXSLT){
                    catDoc = cat.getCatalogDocument();
                }
                else {

                    response.setContentType("text/xml");
                    response.setHeader("Content-Description", "dods_directory");
                    response.setStatus(HttpServletResponse.SC_OK);

                    cat.printCatalog(response.getOutputStream());
                    log.debug("Sent THREDDS catalog (raw XML).");
                    return;
                }
            }
            else {
                log.error("Can't find catalog: "+relativeURL+"   " +
                        "    requestSuffix: "+requestSuffix+"   " +
                        "    prefix: " + _prefix


                );

                response.sendError(HttpServletResponse.SC_NOT_FOUND,"Can't find catalog: "+relativeURL);
                return;
            }
        }



        String xsltDoc = ServletUtil.getPath(dispatchServlet, "/docs/xsl/thredds.xsl");
        XSLTransformer transformer = new XSLTransformer(xsltDoc);
        Document contentsPage = transformer.transform(catDoc);

        //xmlo.output(catalog, System.out);
        //xmlo.output(contentsPage, System.out);
        response.setContentType("text/html");
        response.setHeader("Content-Description", "thredds_catalog");
        response.setStatus(HttpServletResponse.SC_OK);
        xmlo.output(contentsPage, response.getWriter());
        //xmlo.output(contentsPage, System.out);
        log.debug("Sent transformed THREDDS catalog (XML->XSLT->HTML).");



    }

    private boolean redirectForPrefixOnlyRequest(HttpServletRequest req,
                                                  HttpServletResponse res)
            throws IOException {

        String relativeURL = ReqInfo.getFullSourceName(req);


        // We know the _prefix ens in slash. So, if this things is the same as
        // prefix sans slash then we redirect
        if (relativeURL.equals(_prefix.substring(0,_prefix.length()-1))) {
            String newURI = _prefix;
            res.sendRedirect(Scrub.urlContent(newURI));
            log.debug("Sent redirectForContextOnlyRequest to map the servlet " +
                    "context to a URL that ends in a '/' character! Redirect to: "+Scrub.urlContent(newURI));
            return true;
        }
        return false;
    }



    public void init(DispatchServlet servlet, Element configuration) throws Exception {


        String s;

        if (initialized) return;

        dispatchServlet = servlet;
        config = configuration;


        List children;
        Element e;

        e = config.getChild("_prefix");
        if(e!=null)
            _prefix = e.getTextTrim();

        if(_prefix.equals("/"))
            throw new Exception("Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <_prefix>  " +
                    "child element whose value may not be equal to \"/\"");


        if(!_prefix.endsWith("/"))
            _prefix += "/";


        //if(!_prefix.startsWith("/"))
        //    _prefix = "/" + _prefix;

        if(_prefix.startsWith("/"))
            _prefix = _prefix.substring(1,_prefix.length());

        e = config.getChild("useMemoryCache");
        if(e!=null){
            s = e.getTextTrim();
            if(s.equalsIgnoreCase("true")){
                useMemoryCache = true;
            }
        }


        // Get the RootCatalogs from the conifg
        children = config.getChildren("threddsCatalogRoot");

        if (children.isEmpty()) {
            throw new Exception("Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 or more <threddsCatalogRoot>  " +
                    "child elements.");
        } else {
            log.debug("Processing THREDDS catalog file(s)...");

            String contentPath = ServletUtil.getContentPath(servlet);
            String contextPath = ServletUtil.getContextPath(servlet);


            String urlPrefix = "";//ServletUtil.getContextPath(servlet);

            if(!urlPrefix.endsWith("/") && !_prefix.startsWith("/"))
                urlPrefix += "/";

            while(urlPrefix.endsWith("/") && _prefix.startsWith("/"))
                urlPrefix += urlPrefix.substring(0,urlPrefix.length()-1);

            //urlPrefix = urlPrefix + _prefix;
            //urlPrefix =  _prefix;


            if(!urlPrefix.endsWith("/"))
                urlPrefix += "/";

            urlPrefix = "";


            Iterator i = children.iterator();
            Element fileElem;
            String fileName,  pathPrefix, thisUrlPrefix;


            log.debug("urlPrefix: "+urlPrefix);
            CatalogManager.init(contentPath);
            while (i.hasNext()) {

                fileElem = (Element) i.next();


                s = fileElem.getTextTrim();



                thisUrlPrefix = s.substring(0,s.lastIndexOf(Util.basename(s)));
                thisUrlPrefix = urlPrefix + thisUrlPrefix;



                s = contentPath + s;
                fileName = Util.basename(s);
                pathPrefix = s.substring(0,s.lastIndexOf(fileName));

                log.debug("Top Level Catalog - pathPrefix: " + pathPrefix);
                log.debug("Top Level Catalog - urlPrefix: " + thisUrlPrefix);
                log.debug("Top Level Catalog - fileName: " + fileName);

                CatalogManager.addRootCatalog(
                        pathPrefix,
                        thisUrlPrefix,
                        fileName,
                        useMemoryCache);

                log.debug("configuration file: " + fileName + " processing complete.");
            }


        }

        // Read Config and establish Config state.


        log.info("Initialized.");
        initialized = true;
    }

    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return threddsRequestDispatch(request, null, false);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        threddsRequestDispatch(request, response, true);
    }


    private boolean threddsRequestDispatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       boolean sendResponse)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        if(dataSource.startsWith("/"))
            dataSource = dataSource.substring(1,dataSource.length());

        boolean threddsRequest = false;

        if (dataSource != null) {

            if (dataSource.startsWith(_prefix)) {
                threddsRequest = true;
                if (sendResponse) {
                    sendThreddsCatalogResponse(request, response);
                    log.info("Sent THREDDS Response");
                }
            }
        }

        return threddsRequest;

    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }

    public void destroy() {

        WcsManager.destroy();
        log.info("Destroy Complete");


    }




}




