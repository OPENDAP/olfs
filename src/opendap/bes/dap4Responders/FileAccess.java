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

import opendap.bes.*;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.MimeTypes;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.Scrub;
import opendap.dap.Request;
import opendap.dap.User;
import opendap.http.error.Forbidden;
import opendap.http.error.NotFound;
import opendap.io.HyraxStringEncoding;
import opendap.ppt.PPTException;
import org.jdom.Element;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    private DatasetUrlResponseAction d_datasetUrlResponseAction;
    private DataRequestFormType d_dataRequestFormType;

    public FileAccess(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public FileAccess(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        d_datasetUrlResponseAction = DatasetUrlResponseAction.requestForm;
        d_dataRequestFormType = DataRequestFormType.dap4;

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


    public void setDatasetUrlResponseAction(DatasetUrlResponseAction action){ d_datasetUrlResponseAction = action;}
    public DatasetUrlResponseAction datasetUrlResponseAction(){ return d_datasetUrlResponseAction;}

    public void setDatasetRequestFormType(DataRequestFormType type){ d_dataRequestFormType = type;}
    public DataRequestFormType datasetRequestFormType(){ return d_dataRequestFormType;}


    private boolean d_allowDirectDataSourceAccess = false;


    public void setAllowDirectDataSourceAccess(boolean allowed){
        d_allowDirectDataSourceAccess = allowed;
    }
    public boolean allowDirectDataSourceAccess() {
        return d_allowDirectDataSourceAccess;
    }



    public void sendNormativeRepresentation(HttpServletRequest req, HttpServletResponse response) throws Exception {

        String requestedResourceId = ReqInfo.getLocalUrl(req);
        String resourceID = getResourceId(requestedResourceId, false);
        boolean isFileServiceUrl = false;
        if(resourceID.endsWith(requestSuffix)) {
            resourceID = resourceID.substring(0, resourceID.length() - requestSuffix.length());
            isFileServiceUrl = true;
        }

        User user = new User(req);

        BesApi besApi = getBesApi();

        ResourceInfo dsi = new BESResource(resourceID, besApi);
        if (dsi.sourceExists()) {
            if (!dsi.isNode()) {
                if (dsi.sourceIsAccesible()) {
                    if (dsi.isDataset()) {
                        if(isFileServiceUrl){
                            if(d_allowDirectDataSourceAccess) {
                                log.debug("Sending source dataset file: " + Encode.forHtml(resourceID));
                                sendDatasetFile(user, resourceID, response);
                            }
                            else {
                                log.debug("Sending Access Denied for resource: " + Encode.forHtml(resourceID));
                                sendDirectAccessDenied(req, response);
                            }
                        }
                        else {
                            switch(d_datasetUrlResponseAction){
                                case download:
                                {
                                    if(d_allowDirectDataSourceAccess) {
                                        log.debug("Sending source dataset file: " + Encode.forHtml(resourceID));
                                        sendDatasetFile(user, resourceID, response);
                                    }
                                    else {
                                        log.debug("Sending Access Denied for resource: " + Encode.forHtml(resourceID));
                                        sendDirectAccessDenied(req, response);
                                    }
                                    break;
                                }

                                case dsr:
                                {
                                    String redirectUrl =  ReqInfo.getRequestUrlPath(req) + ".dsr";
                                    log.debug("Redirecting request for dataset URL to DSR: {}",redirectUrl);
                                    response.sendRedirect(redirectUrl);
                                    break;
                                }

                                case requestForm:
                                default:
                                {
                                    String redirectUrl =  ReqInfo.getRequestUrlPath(req);
                                    switch(BesDapDispatcher.dataRequestFormType()){
                                        case dap2:
                                            redirectUrl += ".html";
                                            break;
                                        case dap4:
                                        default:
                                            redirectUrl += ".dmr.html";
                                            break;
                                    }
                                    log.debug("Redirecting request for dataset URL to Data Request Form: {}",redirectUrl);
                                    response.sendRedirect(redirectUrl);
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        String errMsg = "Unable to locate BES resource: " + resourceID;
                        log.debug(errMsg);
                        throw new NotFound(errMsg);
                        //String errMsg = "Unable to locate BES resource. Message:" + Encode.forHtml(resourceID);
                        //sendHttpErrorResponse(HttpServletResponse.SC_NOT_FOUND, errMsg, "docs", response);
                    }
                }
                else {
                    String errMsg = "Requested data source " + resourceID + " is not accessible.";

                    log.debug(errMsg);
                    throw new NotFound(errMsg);
                    //String errMsg = "BES data source {} is not accessible. Message:" + Encode.forHtml(resourceID);
                    //sendHttpErrorResponse(HttpServletResponse.SC_NOT_FOUND, errMsg, "docs", response);
                }

            }
            else {
                String errMsg = "You may not download nodes/directories, only files. Requested resource: " + resourceID;
                log.debug(errMsg);
                throw new Forbidden(errMsg);
                //String errMsg = "You may not download nodes/directories, only files. Message:" + Encode.forHtml(resourceID);
                //sendHttpErrorResponse(HttpServletResponse.SC_FORBIDDEN, errMsg, "docs", response);
            }

        }
        else {
            String errMsg = "Unable to locate resource: " + resourceID;
            log.debug("matches() - {}", errMsg);
            throw new NotFound(errMsg);
            //String errMsg = "Unable to locate BES resource: " + Encode.forHtml(resourceID);
            //sendHttpErrorResponse(HttpServletResponse.SC_NOT_FOUND, errMsg, "docs", response);
        }






    }

    @Override
    public String getRequestSuffix(){
        return "";
    }


    private void sendDatasetFile(User user, String dataSourceId, HttpServletResponse response)
            throws IOException, BESError, BadConfigurationException, PPTException {
        log.debug("sendDatasetFile() - Sending dataset file \"" + dataSourceId + "\"");

        response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(dataSourceId)+"\"");


        String suffix = ReqInfo.getSuffix(dataSourceId);
        BesApi besApi = getBesApi();

        if (suffix != null) {
            MediaType responseMediaType = MimeTypes.getMediaType(suffix);
            if (responseMediaType != null) {
                response.setContentType(responseMediaType.getMimeType());
                log.debug("sendDatasetFile() - MIME type: " + responseMediaType.getMimeType() + "  ");
            }
        }


        ServletOutputStream sos = response.getOutputStream();
        besApi.writeFile(user, dataSourceId, sos);

        sos.flush();


        log.debug("sendDatasetFile() - Sent {}",getServiceTitle());


    }



    private void sendDirectAccessDenied(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setContentType("text/html");
        response.setHeader("Content-Description", "BadURL");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), HyraxStringEncoding.getCharset()));

        String serviceUrl = new Request(null,request).getServiceUrl();


        String context = request.getContextPath();
        pw.println("<html xmlns=\"http://www.w3.org/1999/xhtml\"> ");
        pw.println("<head>  ");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        pw.println("    <link rel='stylesheet' href='" + context + "/docs/css/contents.css' type='text/css' />");
        pw.println("<title>Hyrax:  Access Denied</title>");
        pw.println("</head>");
        pw.println("");
        pw.println("<body>");
        pw.println("<img alt=\"OPeNDAP Logo\" src=\"" + context + "/docs/images/logo.png\"/>");

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

    @Override
    public boolean matches(String relativeUrl){

        log.debug("matches() -    BEGIN");

        if(relativeUrl.endsWith(requestSuffix))
            relativeUrl = relativeUrl.substring(0,relativeUrl.length()-requestSuffix.length());

        return super.matches(relativeUrl);

    }


}
