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
import org.slf4j.Logger;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;

import thredds.servlet.ServletUtil;
import net.sf.saxon.s9api.*;

/**
 * Provides Dispatch Services for the XSLT based THREDDS catalog Handler.
 *
 *
 *
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
    Transform xsltTransform = null;
    ReentrantLock transformLock;

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


        if(redirectForPrefixOnlyRequest(request,response))
            return;

        /* Make sure the relative URL is really relative */
        while(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());


        // Is the request for a presentation view (HTML version) of the catalog?
        if(requestSuffix!=null && requestSuffix.equals("html")){
            sendCatalogHTML(response,relativeURL);
        }
        else { // Send the the raw catalog XML.
            sendCatalogXML(response,relativeURL);
        }

    }

    private void sendCatalogXML(HttpServletResponse response, String relativeURL)throws Exception {
        if(relativeURL.equals(_prefix)){ // Then we have to make a top level catalog.

            Document catalog = CatalogManager.getTopLevelCatalogDocument();

            // Send the XML catalog.
            response.setContentType("text/xml");
            response.setHeader("Content-Description", "dods_directory");
            response.setStatus(HttpServletResponse.SC_OK);
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            response.getOutputStream().print(xmlo.outputString(catalog));
            log.debug("Sent THREDDS catalog (raw XML).");

        }
        else{
            // Strip the prefix off of the relativeURL)
            if(relativeURL.startsWith(_prefix))
                relativeURL = relativeURL.substring(_prefix.length(),relativeURL.length());

            Catalog cat = CatalogManager.getCatalog(relativeURL);

            if(cat!=null){
                log.debug("\nFound catalog: "+relativeURL+"   " +
                        "    prefix: " + _prefix
                );

                // Send the XML catalog.
                response.setContentType("text/xml");
                response.setHeader("Content-Description", "dods_directory");
                response.setStatus(HttpServletResponse.SC_OK);
                cat.writeCatalogXML(response.getOutputStream());
                log.debug("Sent THREDDS catalog (raw XML).");

            }
            else {
                log.error("Can't find catalog: "+relativeURL+"   " +
                        "    prefix: " + _prefix
                );

                response.sendError(HttpServletResponse.SC_NOT_FOUND,"Can't " +
                        "find catalog: "+Scrub.urlContent(relativeURL));
            }
        }

    }

    private void sendCatalogHTML(HttpServletResponse response, String relativeURL)throws SaxonApiException, IOException{
        transformLock.lock();
        try {

            XdmNode catDoc;

            // Patch up the request URL so we can find the source catalog
            relativeURL = relativeURL.substring(0,
                    relativeURL.lastIndexOf(".html")) + ".xml";


            if(relativeURL.equals(_prefix)){ // Then we have to make a top level catalog.
                catDoc = CatalogManager.getTopLevelCatalogAsXdmNode(xsltTransform.getProcessor());
            }
            else{
                // Strip the prefix off of the relativeURL)
                if(relativeURL.startsWith(_prefix))
                    relativeURL = relativeURL.substring(_prefix.length(),relativeURL.length());


                Catalog cat = CatalogManager.getCatalog(relativeURL);

                if(cat!=null){
                    log.debug("\nFound catalog: "+relativeURL+"   " +
                            "    prefix: " + _prefix
                    );
                    catDoc = cat.getCatalogAsXdmNode(xsltTransform.getProcessor());
                }
                else {
                    log.error("Can't find catalog: "+relativeURL+"   " +
                            "    prefix: " + _prefix
                    );
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,"Can't find catalog: "+Scrub.urlContent(relativeURL));
                    return;
                }
            }


            // Send the catalog using the transform.

            response.setContentType("text/html");
            response.setHeader("Content-Description", "thredds_catalog");
            response.setStatus(HttpServletResponse.SC_OK);

            xsltTransform.transform(catDoc,response.getOutputStream());

            log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");

        }
        finally {
            transformLock.unlock();
        }


    }




    private boolean redirectForPrefixOnlyRequest(HttpServletRequest req,
                                                  HttpServletResponse res)
            throws IOException {

        String relativeURL = ReqInfo.getFullSourceName(req);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());


        // We know the _prefix ends in slash. So, if this things is the same as
        // prefix sans slash then we redirect.
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

        e = config.getChild("prefix");
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
            CatalogManager.init(contentPath);



            Iterator i = children.iterator();
            Element fileElem;
            String fileName,  pathPrefix, thisUrlPrefix;
            while (i.hasNext()) {

                fileElem = (Element) i.next();

                s = fileElem.getTextTrim();

                thisUrlPrefix = s.substring(0,s.lastIndexOf(Util.basename(s)));

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

        // ---------------------
        // Get XSLT document name
        String xsltDoc = ServletUtil.getPath(dispatchServlet, "/docs/xsl/thredds.xsl");

        // Build an cache an XSLT transformer for the XSLT document.
        xsltTransform = new Transform(xsltDoc);

        // Create a lock for use with the thread-unsafe transformer.
        transformLock = new ReentrantLock(true);


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
        //String requestSuffix = ReqInfo.getRequestSuffix(request);


        if(dataSource.startsWith("/"))
            dataSource = dataSource.substring(1,dataSource.length());

        boolean threddsRequest = false;

        if (dataSource != null) {
            // We know the _prefix ends in slash. So let's strip the slash
            // before we compare. This makes sure that we pick up the URL
            // that ends with the prefix and no slash
            if (dataSource.startsWith(_prefix.substring(0,_prefix.length()-1))) {
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
        try {
            if(requestCanBeHandled(req)){
                String relativeURL = ReqInfo.getFullSourceName(req);

                // Make sure it's a relative URL
                while(relativeURL.startsWith("/"))
                        relativeURL = relativeURL.substring(1,relativeURL.length());

                // Strip the prefix off of the relativeURL)
                if(relativeURL.startsWith(_prefix))
                    relativeURL = relativeURL.substring(_prefix.length(),relativeURL.length());

                // If it's a request for an HTML view of the catalog, replace the
                // .html suffix with .xml so we can find the catalog.
                if(relativeURL.endsWith(".html")){
                    relativeURL = relativeURL.substring(0,
                        relativeURL.lastIndexOf(".html")) + ".xml";
                }

                long lm = CatalogManager.getLastModified(relativeURL);
                log.debug("lastModified("+relativeURL+"): "+new Date(lm));
                return lm;
            }
        }
        catch(Exception e){
            log.error(e.getMessage());
        }
        return -1;
    }

    public void destroy() {
        log.info("Destroy Complete");


    }


}




