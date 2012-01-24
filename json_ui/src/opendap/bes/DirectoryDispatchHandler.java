/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

package opendap.bes;

import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import opendap.dap.Request;
import opendap.xml.Transformer;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import java.util.Date;


/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 4:34:20 PM
 */
public class DirectoryDispatchHandler implements DispatchHandler {

    private Logger log;
    private boolean initialized;

    private HttpServlet dispatchServlet;
    private String systemPath;


    private BesApi _besApi;


    public DirectoryDispatchHandler() {



        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;

    }



    public void init(HttpServlet s, Element config) throws Exception {

        if(initialized) return;

        dispatchServlet = s;
        systemPath = ServletUtil.getSystemPath(s,"");

        _besApi = new BesApi();



        initialized = true;
    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {

        boolean val = directoryDispatch(request, null, false);

        log.info("requestCanBeHandled: "+val);
        return val;

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {
        log.info("Handling Request.");
       directoryDispatch(request, response, true);

    }


    public long getLastModified(HttpServletRequest req) {

        String name = ReqInfo.getLocalUrl(req);

        if(name.endsWith("contents.html"))
            name = name.substring(0,name.lastIndexOf("contents.html"));

        log.info("getLastModified():  Tomcat requesting getlastModified() for collection: " + name );


        try {
            DataSourceInfo dsi = new BESDataSource(name,_besApi);
            log.debug("getLastModified():  Returning: " + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            log.debug("getLastModified():  Returning: -1");
            return -1;
        }
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
     * @param sendResponse If this is true a response will be sent. If it is
     *                     the request will only be evaluated to determine if a response can be
     *                     generated.
     * @return true if the request was serviced as a file request, false
     *         otherwise.
     * @throws Exception .
     */
    private boolean directoryDispatch(HttpServletRequest request,
                                     HttpServletResponse response,
                                     boolean sendResponse) throws Exception {


        String dataSetName = ReqInfo.getDataSetName(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);
        String dsName = ReqInfo.getLocalUrl(request);


        boolean isDirectoryResponse = false;
        boolean isContentsRequest = false;

        if(dataSetName != null &&
                (dataSetName.equalsIgnoreCase("contents") ||
                dataSetName.equalsIgnoreCase("catalog")) &&
                requestSuffix != null &&
                requestSuffix.equalsIgnoreCase("html")

               ){

                isDirectoryResponse = true;
                isContentsRequest = true;

        } else {
            DataSourceInfo dsi = new BESDataSource(dsName,_besApi);
            if (dsi.sourceExists() &&
                    dsi.isNode() ) {
                    isDirectoryResponse = true;
            }

        }


        if (isDirectoryResponse && sendResponse) {

            if(dsName.endsWith("/") || isContentsRequest){
                xsltDir(request, response);
            }
            else {
                // Now that we certain that this is a directory request we
                // redirect the URL without a trailing slash to the one with.
                // This keeps everything copacetic downstream when it's time
                // to build the directory document.
                response.sendRedirect(Scrub.urlContent(request.getContextPath()+"/"+dispatchServlet.getServletName()+dsName+"/"));
            }


        }


        return isDirectoryResponse;

    }


    /**
     * ************************************************************************
     * Handler for OPeNDAP directory requests. Returns an html document
     * with a list of all datasets on this server with links to their
     * DDS, DAS, Information, and HTML responses. Talks to the BES to get the
     * information.
     *
     * @param request  The <code>HttpServletRequest</code> from the client.
     * @param response The <code>HttpServletResponse</code> for the client.
     * @throws Exception when things go poorly.
     * @see opendap.coreServlet.ReqInfo
     */
    public void xsltDir(HttpServletRequest request,
                               HttpServletResponse response)
            throws Exception {


        log.info("sendDIR() request = " + request);

        String context = request.getContextPath();


        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_directory");

        response.setStatus(HttpServletResponse.SC_OK);

        Request oreq = new Request(null,request);



        String collectionName  = Scrub.urlContent(oreq.getRelativeUrl());

        if(collectionName.endsWith("/contents.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("contents.html"));
        }
        else if(collectionName.endsWith("/catalog.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("catalog.html"));
        }

        if(!collectionName.endsWith("/"))
            collectionName += "/";

        while(!collectionName.equals("/") && collectionName.startsWith("/"))
            collectionName = collectionName.substring(1);

        log.debug("collectionName:  "+collectionName);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Document showCatalogDoc = new Document();

        if(_besApi.getCatalog(collectionName,showCatalogDoc)){

            log.debug("Catalog from BES:\n"+xmlo.outputString(showCatalogDoc));

            JDOMSource besCatalog = new JDOMSource(showCatalogDoc);

            String xsltDoc = systemPath + "/xsl/contents.xsl";

            Transformer transformer = new Transformer(xsltDoc);

            transformer.setParameter("dapService",oreq.getDapServiceLocalID());
            transformer.setParameter("docsService",oreq.getDocsServiceLocalID());
            transformer.setParameter("webStartService",oreq.getWebStartServiceLocalID());
            if(FileDispatchHandler.allowDirectDataSourceAccess())
                transformer.setParameter("allowDirectDataSourceAccess","true");

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform(besCatalog, response.getOutputStream());



        }
        else {
            BESError besError = new BESError(showCatalogDoc);
            besError.sendErrorResponse(systemPath, context, response);
            log.error(besError.getMessage());

        }

    }



}
