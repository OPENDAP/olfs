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
package opendap.bes.dap2Responders;

import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.dap.User;
import opendap.dap4.Dap4Error;
import opendap.dap4.QueryParameters;
import opendap.io.HyraxStringEncoding;
import opendap.logging.LogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;


public class Dap2Data extends Dap4Responder {



    private Logger log;


    private static String _defaultRequestSuffix = ".dods";


    public Dap2Data(String sysPath, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath,null, _defaultRequestSuffix,besApi,addTypeSuffixToDownloadFilename);
    }



    public Dap2Data(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi,boolean addTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        addTypeSuffixToDownloadFilename(addTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap2/data");
        setServiceTitle("DAP2 Data");
        setServiceDescription("DAP2 Data Object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_Data_Service");


        setNormativeMediaType(new opendap.http.mediaTypes.Dap2Data(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }







    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        BesApi besApi = getBesApi();

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl,false);
        QueryParameters qp = new  QueryParameters(request);
        User user = new User(request);

        // Here we must use the ReqInfo.getConstraintExpression() call
        // because tht is where the {}{}{} notation support is located.
        // Using the query remainder, post DAP4 processing:
        //
        //   String dap2CE = qp.getQueryRemainder();
        //
        // Will only work if we migrate the {}{}{} support into
        // the  QueryParameters class.
        //
        String dap2CE = ReqInfo.getConstraintExpression(request);

        log.debug("resourceID: {}   CE: '{}' ",resourceID, dap2CE );

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dods_data");
        response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");

        String xdap_accept = request.getHeader("XDAP-Accept");

        DataOutputStream os;
        ByteArrayOutputStream srr = null;
        if(qp.isStoreResultRequest()){
            srr = new ByteArrayOutputStream();
            os = new DataOutputStream(srr);
        }
        else {
            os = new DataOutputStream(response.getOutputStream());
        }
        besApi.writeDap2Data(resourceID,dap2CE,qp.getAsync(),qp.getStoreResultRequestServiceUrl(),xdap_accept,user.getMaxResponseSize(),os);
        if(qp.isStoreResultRequest()){
            handleStoreResultResponse(srr, response);
        }
        os.flush();
        RequestCache.put(LogUtil.RESPONSE_SIZE_KEY,os.size());
        log.debug("Sent {} size:{}",getServiceTitle(),os.size());
    }



    public void handleStoreResultResponse(ByteArrayOutputStream besResponse,  HttpServletResponse resp) throws IOException {


        ServletOutputStream sos = resp.getOutputStream();

        SAXBuilder sb = new SAXBuilder();
        Document doc;
        try {
            doc = sb.build(new ByteArrayInputStream(besResponse.toByteArray()));

        } catch (JDOMException e) {
            String msg = "Failed to parse asynchronous response from BES!";
            log.error("handleStoreResultResponse() - " + msg + " Message: " + e.getMessage());
            Dap4Error d4e = new Dap4Error();
            d4e.setHttpStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            d4e.setMessage(msg);
            d4e.setOtherInformation(besResponse.toString( HyraxStringEncoding.getCharset().name()));
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            sos.print(d4e.toString());
            sos.flush();
            return;
        }

        Element asyncResponse = doc.getRootElement();
        String status = asyncResponse.getAttributeValue("status");

        if(status.equalsIgnoreCase("required")){
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setHeader("X-DAP-Async-Required", "true");
        }
        else if (status.equalsIgnoreCase("accepted")){
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            resp.setHeader("X-DAP-Async-Accepted", "true");
        }
        else if(status.equalsIgnoreCase("rejected")){
            resp.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        sos.write(besResponse.toByteArray());
        sos.flush();


    }


}
