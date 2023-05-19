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

import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.*;
import opendap.dap.User;
import opendap.http.error.Forbidden;
import opendap.namespaces.BES;
import opendap.ppt.PPTException;
import opendap.services.FileService;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * Provides access to files held in the BES that the BES does not recognize as data.
 *
 */
public class FileDispatchHandler implements DispatchHandler {

    private org.slf4j.Logger log;
    //private static boolean allowDirectDataSourceAccess = false;
    private boolean initialized;

    private BesApi _besApi;

    public FileDispatchHandler() {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;

    }

    public void init(HttpServlet servlet,Element config) throws Exception {
        init(servlet, config, new BesApi());
    }

    public void init(HttpServlet servlet,Element config, BesApi besApi) throws Exception {
        if(initialized) return;
        _besApi = besApi;
        initialized = true;
    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return fileDispatch(request, null, false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

       if(!fileDispatch(request, response, true))
           log.debug("FileDispatch request failed inexplicably!");

    }


    public long getLastModified(HttpServletRequest req) {

        String name = ReqInfo.getLocalUrl(req);
        log.debug("getLastModified(): Tomcat requesting getlastModified() for collection: " + name );

        try {
            ResourceInfo dsi = new BESResource(name,_besApi);
            log.debug("getLastModified(): Returning: " + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            long now = new Date().getTime();
            log.debug("getLastModified(): Returning current date/time: {}",now);
            return now;
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
    public boolean fileDispatch(HttpServletRequest request,
                                HttpServletResponse response,
                                boolean sendResponse) throws Exception {

        String localUrl = ReqInfo.getLocalUrl(request);

        // TODO: Is this the correct order of eval? Should it check for the ".file" suffix first?
        ResourceInfo dsi = new BESResource(localUrl,_besApi);
        if (!dsi.sourceExists() && localUrl.endsWith(FileService.getFileServiceSuffix())) {
            localUrl =  localUrl.substring(0,localUrl.lastIndexOf(FileService.getFileServiceSuffix()));
            dsi = new BESResource(localUrl,_besApi);
        }

        boolean isFileResponse = false;
        if (dsi.sourceExists()) {
            if (!dsi.isNode()) {
                isFileResponse = true;
                if (sendResponse) {
                    if(dsi.sourceIsAccesible()){
                        if (!dsi.isDataset() ){
                            sendFile(request, response);
                        } else {
                            throw new Forbidden("This server does not support the direct download of the source data." +
                                    "You may use one of the subsetting interfaces to request parts of the data.");
                        }
                    }
                    else {
                        throw new Forbidden("You do not have permission to access the requested resource.");
                    }
                }
            }
        }
        else {

        }

        return isFileResponse;

    }




    public void sendFile(HttpServletRequest req,
                         HttpServletResponse response)
            throws JDOMException, BadConfigurationException, PPTException, BESError, IOException {


        String name = ReqInfo.getLocalUrl(req);

        User user= new User(req);

        log.debug("sendFile(): Sending file \"" + name + "\"");

        String downloadFileName = Scrub.fileName(name.substring(name.lastIndexOf("/")+1));

        log.debug("sendFile() downloadFileName: " + downloadFileName );


        Document nodeDoc = new Document();
        _besApi.getBesNode(name,nodeDoc);
        XMLOutputter xmlo = new XMLOutputter();

        log.debug(xmlo.outputString(nodeDoc));

        Element root = nodeDoc.getRootElement();
        Element showNode = root.getChild("showNode", BES.BES_NS);
        Element item = showNode.getChild("item", BES.BES_NS);
        String sizeStr = item.getAttributeValue("size");
        String lastModified = item.getAttributeValue("lastModified");

        long fileSize = Long.parseLong(sizeStr);
        response.setHeader("Content-Length", Long.toString(fileSize));
        response.setHeader("Last-Modified", lastModified);

        // I commented these two lines  out because it was incorrectly causing browsers to downloadJobOutput
        // (as opposed to display) EVERY file retrieved.
        //String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";
        //response.setHeader("Content-Disposition",contentDisposition);

        String suffix = ReqInfo.getRequestSuffix(req);

        if (suffix != null) {
            MediaType responseMediaType = MimeTypes.getMediaType(suffix);
            if (responseMediaType != null) {
                response.setContentType(responseMediaType.getMimeType());
                log.debug("sendFile() - MIME type: " + responseMediaType.getMimeType() + "  ");
            }
        }


        TransmitCoordinator tc = new ServletResponseTransmitCoordinator(response);
        ServletOutputStream sos = response.getOutputStream();
        _besApi.writeFile(user, name, sos, tc);

        sos.flush();
    }



}
