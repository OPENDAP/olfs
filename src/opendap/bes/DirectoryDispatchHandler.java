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

package opendap.bes;

import opendap.coreServlet.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import java.util.Date;

import thredds.servlet.ServletUtil;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 4:34:20 PM
 */
public class DirectoryDispatchHandler implements DispatchHandler {

    private Logger log;
    private boolean initialized;
    private boolean useDefaultOpendapDirectoryView;
    private DispatchServlet dispatchServlet;


    public DirectoryDispatchHandler() {



        log = org.slf4j.LoggerFactory.getLogger(getClass());
        useDefaultOpendapDirectoryView = false;
        initialized = false;

    }



    public void init(DispatchServlet s, Element config) throws Exception {

        if(initialized) return;

        dispatchServlet = s;

        Element dv = config.getChild("DefaultDirectoryView");
        if(dv!=null){
            String val = dv.getTextTrim();
            if(val!=null) {
                if(val.equalsIgnoreCase("opendap")){
                    useDefaultOpendapDirectoryView = true;
                } else if(val.equalsIgnoreCase("thredds")){
                    useDefaultOpendapDirectoryView = false;
                } else {
                    throw new BadConfigurationException("The " +
                            "<DefaultDirectoryView> may have one of two " +
                            "values. Ethier \"OPeNDAP\" or \"THREDDS\".");
                }
            }
        }

        log.info("Initialized. Using " + (useDefaultOpendapDirectoryView?"OPeNDAP":"THREDDS") + " default directory view.");

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

        String name = ReqInfo.getFullSourceName(req);

        if(name.endsWith("contents.html"))
            name = name.substring(0,name.lastIndexOf("contents.html"));

        log.info("getLastModified():  Tomcat requesting getlastModified() for collection: " + name );


        try {
            DataSourceInfo dsi = new BESDataSource(name);
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
        String dsName = ReqInfo.getFullSourceName(request);


        boolean isDirectoryResponse = false;
        boolean isContentsRequest = false;

        if(dataSetName != null &&
            dataSetName.equalsIgnoreCase("contents") &&
            requestSuffix != null &&
            requestSuffix.equalsIgnoreCase("html")) {

            isDirectoryResponse = true;
            isContentsRequest = true;

        } else {
            DataSourceInfo dsi = new BESDataSource(dsName);
            if (dsi.sourceExists() &&
                    dsi.isCollection() &&
                    useDefaultOpendapDirectoryView) {

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
                response.sendRedirect(Scrub.urlContent(request.getContextPath()+dsName+"/"));
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

        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_directory");

        response.setStatus(HttpServletResponse.SC_OK);



        String collectionName  = Scrub.urlContent(ReqInfo.getFullSourceName(request));

        if(collectionName.endsWith("/contents.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("contents.html"));
        }

        if(!collectionName.endsWith("/"))
            collectionName += "/";

        log.debug("collectionName:  "+collectionName);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Document catalog = new Document();

        if(BesXmlAPI.getCatalog(collectionName,catalog)){

            String xsltDoc = ServletUtil.getPath(dispatchServlet,"/docs/xsl/contents.xsl");

            XSLTransformer transformer = new XSLTransformer(xsltDoc);

            Document contentsPage = transformer.transform(catalog);

            xmlo.output(contentsPage, response.getWriter());

            log.debug("Catalog from BES:\n"+xmlo.outputString(catalog));
            log.debug("HTML Presentation view of BES Catalog:\n"+xmlo.outputString(contentsPage));

        }
        else {
            BESError besError = new BESError(catalog);
            besError.sendErrorResponse(dispatchServlet,response);
            log.error(besError.getMessage());

        }

    }



}
