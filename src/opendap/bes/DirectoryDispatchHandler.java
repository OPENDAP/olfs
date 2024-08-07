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

package opendap.bes;

import opendap.PathBuilder;
import opendap.auth.AuthenticationControls;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.viewers.ViewersServlet;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    private BesApi d_besApi;

    private boolean d_allowDirectDataSourceAccess = false;

    public DirectoryDispatchHandler() {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;
    }

    public void init(HttpServlet s, Element config) throws Exception {
        init(s,config,new BesApi());
    }

    public void init(HttpServlet s, Element config, BesApi besApi) throws Exception {

        if(initialized) return;

        dispatchServlet = s;
        systemPath = ServletUtil.getSystemPath(s,"");
        d_besApi = besApi;

        d_allowDirectDataSourceAccess = false;
        Element dv = config.getChild("AllowDirectDataSourceAccess");
        if (dv != null) {
            d_allowDirectDataSourceAccess = true;
        }
        log.info("AllowDirectDataSourceAccess: {}", d_allowDirectDataSourceAccess);

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

        Request oreq = new Request(null,req);
        String collectionName = getCollectionName(oreq);

        log.debug("getLastModified():  Tomcat requesting getlastModified() for collection: " + collectionName );


        try {
            ResourceInfo dsi = new BESResource(collectionName, d_besApi);
            log.debug("getLastModified():  Returning: " + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            log.debug("getLastModified():  Returning: -1");
            return new Date().getTime();
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
            ResourceInfo dsi = new BESResource(dsName, d_besApi);
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

        log.info("xsltDir() BEGIN request = " + request);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "dap_directory");
        response.setHeader("Cache-Control", "max-age=0, no-cache, no-store");

        Request oreq = new Request(null,request);

        String collectionName  = getCollectionName(oreq);
        String collectionURL = PathBuilder.pathConcat(ReqInfo.getServiceUrl(request),collectionName);

        Document showNodeDoc = new Document();
        d_besApi.getBesNode(collectionName, showNodeDoc);

        if(log.isDebugEnabled()){
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            log.debug("Catalog from BES:\n"+xmlo.outputString(showNodeDoc));
        }
        JDOMSource besNode = new JDOMSource(showNodeDoc);

        AdminInfo adminInfo = d_besApi.getAdminInfo(collectionName);
        String publisherJsonLD = adminInfo.getAsJsonLdPublisher();

        String xsltDoc = systemPath + "/xsl/dap4Contents.xsl";
        if(BesDapDispatcher.dataRequestFormType() == DataRequestFormType.dap2)
            xsltDoc = systemPath + "/xsl/node_contents.xsl";

        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String supportEmail = d_besApi.getSupportEmail(requestedResourceId);
        String mailtoHrefAttributeValue = OPeNDAPException.getSupportMailtoLink(request,200,"n/a",supportEmail);

        Transformer transformer = new Transformer(xsltDoc);
        transformer.setParameter("dapService",oreq.getServiceLocalId());
        transformer.setParameter("docsService",oreq.getDocsServiceLocalID());
        transformer.setParameter("viewersService", ViewersServlet.getServiceId());
        transformer.setParameter("collectionURL",collectionURL);
        transformer.setParameter("catalogPublisherJsonLD",publisherJsonLD);
        transformer.setParameter("supportLink", mailtoHrefAttributeValue);

        if(d_allowDirectDataSourceAccess)
            transformer.setParameter("allowDirectDataSourceAccess","true");

        transformer.setParameter("datasetUrlResponseType",BesDapDispatcher.datasetUrlResponseActionStr());

        AuthenticationControls.setLoginParameters(transformer,request);

        // Transform the BES  showCatalog response into a HTML page for the browser
        transformer.transform(besNode, response.getOutputStream());
        // transformer.transform(besCatalog, System.out);

    }


    private String getCollectionName(Request oreq){

        String collectionName  = Scrub.urlContent(oreq.getRelativeUrl());
        if(collectionName.endsWith("/contents.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("contents.html"));
        }
        else if(collectionName.endsWith("/catalog.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("catalog.html"));
        }

        collectionName = PathBuilder.normalizePath(collectionName, true, true);

        /*
        while(!collectionName.equals("/") && collectionName.startsWith("/"))
            collectionName = collectionName.substring(1);

        if(!collectionName.equals("/"))
            collectionName = "/" + collectionName;

        if(!collectionName.endsWith("/"))
            collectionName += "/";

        */

        log.debug("collectionName:  "+collectionName);

        return collectionName;
    }


}
