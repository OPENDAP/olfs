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

package opendap.bes.dap4Responders.DataResponse;

import opendap.bes.Version;
import opendap.bes.BesApi;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.MimeBoundary;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.dap.User;
import opendap.dap4.Dap4Error;
import opendap.dap4.QueryParameters;
import opendap.http.mediaTypes.Dap4Data;
import opendap.io.HyraxStringEncoding;
import opendap.logging.ServletLogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/5/12
 * Time: 10:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class NormativeDR extends Dap4Responder {
    private Logger log;
    private static String defaultRequestSuffix = ".dap";

    //private String storedResultPrefix = "storedResults/";


    public NormativeDR(String sysPath, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath, null, defaultRequestSuffix, besApi, addTypeSuffixToDownloadFilename);
    }


    public NormativeDR(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());


        addTypeSuffixToDownloadFilename(addTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap4/data");
        setServiceTitle("DAP4 Data Response");
        setServiceDescription("DAP4 Data Response object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP4:_Data_Response");


        setNormativeMediaType(new Dap4Data(getRequestSuffix()));

        addAltRepResponder(new CsvDR          (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new XmlDR          (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new Netcdf3DR      (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new Netcdf4DR      (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new GeoTiffDR      (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new GmlJpeg2000DR  (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new JsonDR         (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new IjsonDR        (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));
        addAltRepResponder(new CovJsonDR      (sysPath, pathPrefix, besApi, addTypeSuffixToDownloadFilename));


        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }





    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String xmlBase = getXmlBase(request);
        String resourceID = getResourceId(relativeUrl, false);
        QueryParameters qp = new  QueryParameters(request);

        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request, response);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        String contentDisposition = " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"";
        response.setHeader("Content-Disposition", contentDisposition);

        MimeBoundary mb = new MimeBoundary();
        String startID = mb.newContentID();

        User user = new User(request);

        DataOutputStream os;
        ByteArrayOutputStream srr = null;
        if(qp.isStoreResultRequest()){
            srr = new ByteArrayOutputStream();
            os = new DataOutputStream(srr);
        }
        else {
            os = new DataOutputStream(response.getOutputStream());
        }
        besApi.writeDap4Data(
                user,
                resourceID,
                qp,
                xmlBase,
                startID,
                mb.getBoundary(),
                os);
        if(qp.isStoreResultRequest()){
            handleStoreResultResponse(srr, response);
        }
        os.flush();
        ServletLogUtil.setResponseSize(os.size());
        log.debug("Sent {} size: {}",getServiceTitle(),os.size());
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
            sos.write(d4e.toString().getBytes(HyraxStringEncoding.getCharset()));
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
