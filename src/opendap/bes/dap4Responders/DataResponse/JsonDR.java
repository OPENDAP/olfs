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
import opendap.coreServlet.*;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.http.mediaTypes.Json;
import opendap.logging.ServletLogUtil;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;

/**
 * Responder that transmits JSON encoded DAP4 data to the client.
 */
public class JsonDR extends Dap4Responder {


    private Logger log;
    private static String defaultRequestSuffix = ".json";
    // private String requestSuffix;



    public JsonDR(String sysPath, BesApi besApi, boolean addFileoutTypeSuffixToDownloadFilename) {
        this(sysPath, null, defaultRequestSuffix, besApi, addFileoutTypeSuffixToDownloadFilename);
    }

    public JsonDR(String sysPath, String pathPrefix, BesApi besApi, boolean addFileoutTypeSuffixToDownloadFilename) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi, addFileoutTypeSuffixToDownloadFilename);
    }

    public JsonDR(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi, boolean addFileoutTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        addTypeSuffixToDownloadFilename(addFileoutTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap4/data/json");
        setServiceTitle("JSON Data Response");
        setServiceDescription("JSON representation of the DAP4 Data Response object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP4:_Data_Response");

        setNormativeMediaType(new Json(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }





    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String requestedResourceId = ReqInfo.getLocalUrl(request);
        // String xmlBase = getXmlBase(request);
        QueryParameters qp = new  QueryParameters(request);
        String resourceID = getResourceId(requestedResourceId, false);
        User user = new User(request);

        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());

        Version.setOpendapMimeHeaders(request, response);

        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());

        TransmitCoordinator tc = new ServletResponseTransmitCoordinator(response);
        DataOutputStream os = new DataOutputStream(response.getOutputStream());
        besApi.writeDap4DataAsJson(
                user,
                resourceID,
                qp,
                os, tc);
        os.flush();
        ServletLogUtil.setResponseSize(os.size());
        log.debug("Sent {} size: {}",getServiceTitle(),os.size());
    }
}
