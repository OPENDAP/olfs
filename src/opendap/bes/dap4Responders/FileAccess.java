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

package opendap.bes.dap4Responders;

import opendap.bes.BESError;
import opendap.bes.BESResource;
import opendap.bes.BadConfigurationException;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.MimeTypes;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.Scrub;
import opendap.ppt.PPTException;
import org.apache.commons.httpclient.HttpStatus;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/6/12
 * Time: 7:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileAccess extends Dap4Responder {

    private Logger log;
    private static String defaultRequestSuffix = ".file";
    private boolean allowDirectDataSourceAccess = false;



    public FileAccess(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public FileAccess(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public FileAccess(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/file-access");
        setServiceTitle("Data File Access");
        setServiceDescription("Simple download access to the underlying data file.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services");

        setNormativeMediaType(new MediaType("*","*", getRequestSuffix()));

        log.debug("defaultRequestSuffix:             '{}'", defaultRequestSuffix);

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }



    public void sendNormativeRepresentation(HttpServletRequest req, HttpServletResponse response) throws Exception {

        String requestedResourceId = ReqInfo.getLocalUrl(req);

        String resourceID = getResourceId(requestedResourceId, false);




        BesApi besApi = getBesApi();

        ResourceInfo dsi = new BESResource(resourceID, besApi);
        if (dsi.sourceExists()) {
            if (!dsi.isNode()) {
                if (dsi.sourceIsAccesible()) {
                    if (dsi.isDataset()) {
                        if (allowDirectDataSourceAccess()) {
                            sendDatasetFile(resourceID, response);

                        }
                        else {
                            log.warn("respondToHttpGetRequest() - Sending Access Denied for resource: " + Scrub.completeURL(resourceID));
                            sendDirectAccessDenied(req,response);
                        }
                    }
                    else {
                        String errMsg = "Unable to locate BES resource: " + Scrub.completeURL(resourceID);
                        log.info("respondToHttpGetRequest() - {}", errMsg);
                        sendHttpErrorResponse(HttpStatus.SC_NOT_FOUND, errMsg, "docs", response);
                    }
                }
                else {
                    String errMsg = "BES data source {} is not accessible." + Scrub.completeURL(resourceID);
                    log.info("respondToHttpGetRequest() - {}", errMsg);
                    sendHttpErrorResponse(HttpStatus.SC_NOT_FOUND, errMsg, "docs", response);
                }

            }
            else {
                String errMsg = "You may not downloadJobOutput nodes/directories, only files." + Scrub.completeURL(resourceID);
                log.info("respondToHttpGetRequest() - {}", errMsg);
                sendHttpErrorResponse(HttpStatus.SC_FORBIDDEN, errMsg, "docs", response);
            }

        }
        else {
            String errMsg = "Unable to locate BES resource: " + Scrub.completeURL(resourceID);
            log.info("matches() - {}", errMsg);
            sendHttpErrorResponse(HttpStatus.SC_NOT_FOUND, errMsg, "docs", response);
        }






    }

    private void sendDatasetFile(String dataSourceId, HttpServletResponse response) throws IOException, BESError, BadConfigurationException, PPTException {
        log.debug("sendDatasetFile() - Sending dataset file \"" + dataSourceId + "\"");

        String downloadFileName = Scrub.fileName(dataSourceId.substring(dataSourceId.lastIndexOf("/") + 1));

        log.debug("respondToHttpGetRequest() - downloadFileName: " + downloadFileName);

        String contentDisposition = " attachment; filename=\"" + downloadFileName + "\"";

        response.setHeader("Content-Disposition", contentDisposition);


        String suffix = ReqInfo.getSuffix(dataSourceId);

        if (suffix != null) {
            String mType = MimeTypes.getMimeType(suffix);

            if (mType != null)
                response.setContentType(mType);

            log.debug("respondToHttpGetRequest() -    MIME type: " + mType + "  ");
        }

        ByteArrayOutputStream erros = new ByteArrayOutputStream();
        BesApi besApi = getBesApi();

        ServletOutputStream sos = response.getOutputStream();
        if (!besApi.writeFile(dataSourceId, sos, erros)) {
            String msg = new String(erros.toByteArray());
            log.error(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
        }

        sos.flush();
        log.debug("Sent {}",getServiceTitle());


    }

    public void setAllowDirectDataSourceAccess(boolean allowed){
        allowDirectDataSourceAccess = allowed;
    }

    public boolean allowDirectDataSourceAccess(){
        return allowDirectDataSourceAccess;
    }



    private void sendDirectAccessDenied(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setContentType("text/html");
        response.setHeader("Content-Description", "BadURL");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        String serviceUrl = ReqInfo.getServiceUrl(request);


        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\"> ");
        pw.println("<head>  ");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />");
        pw.println("    <link rel='stylesheet' href='/opendap/docs/css/contents.css' type='text/css' />");
        pw.println("<title>Hyrax:  Access Denied</title>");
        pw.println("</head>");
        pw.println("");
        pw.println("<body>");
        pw.println("<img alt=\"OPeNDAP Logo\" src=\"/opendap/docs/images/logo.gif\"/>");

        pw.println("<h1>Hyrax : Access Denied (403) </h1>");
        pw.println("<hr align=\"left\" size=\"1\" noshade=\"noshade\" />");



        pw.println("<div class=\"large\">The requested URL directly references a data source. </div>");
        pw.println("<p>You must use the OPeNDAP request interface to get data from the data source.</p>");


        pw.println("<p>If you would like to start at the top level of this server, go here:");
        pw.println("<a href='" + Scrub.completeURL(serviceUrl) + "'>" + Scrub.completeURL(serviceUrl) + "</a></p>");
        pw.println("<p>If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@opendap.org\">support@opendap.org</a></p>");

        pw.println("<hr align=\"left\" size=\"1\" noshade=\"noshade\" />");
        pw.println("<h1 >Hyrax : Access Denied (403) </h1>");
        pw.println("</body>");
        pw.println("</html>");
        pw.flush();

        /*
        pw.println("<table width=\"100%\" border=\"0\">");
        pw.println("  <tr>");
        pw.println("    <td><img src=\"/opendap/docs/images/forbidden.png\" alt=\"Forbidden!\" width=\"350\" height=\"313\" /></td> ");
        pw.println("    <td align=\"center\"><strong>You do not have permission to access the requested resource. </strong>");
        pw.println("      <p align=\"left\">&nbsp;</p>");
        pw.println("      <p align=\"left\">&nbsp;</p></td>");
        pw.println("  </tr>");
        pw.println("</table>");
        */

    }




    @Override
    public Element getNormativeLink(String datasetUrl){

        String href = datasetUrl + getNormativeMediaType().getMediaSuffix();

        String mimeType = getNormativeMediaType().getMimeType();

        String suffix = ReqInfo.getSuffix(datasetUrl);

        if (suffix != null) {
            String mType = MimeTypes.getMimeType(suffix);

            if (mType != null)
                mimeType = mType;

        }

        log.debug("getNormativeLink() -    MIME type: " + mimeType + "  ");

        Element link = getLinkElement(mimeType,href,"The normative form of the "+getServiceTitle());

        return link;

    }



}
