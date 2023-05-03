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
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.http.mediaTypes.TextCsv;
import opendap.logging.ServletLogUtil;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/5/12
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CsvDR extends Dap4Responder {


    private Logger log;
    private static String defaultRequestSuffix = ".csv";



    public CsvDR(String sysPath, BesApi besApi, boolean addFileoutTypeSuffixToDownloadFilename) {
        this(sysPath, null, defaultRequestSuffix, besApi, addFileoutTypeSuffixToDownloadFilename);
    }

    public CsvDR(String sysPath, String pathPrefix, BesApi besApi, boolean addFileoutTypeSuffixToDownloadFilename) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi, addFileoutTypeSuffixToDownloadFilename);
    }

    public CsvDR(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi, boolean addFileoutTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/data/csv");
        setServiceTitle("CSV Data Response");
        setServiceDescription("A comma separated values (CSV) representation of the DAP4 Data Response object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP4:_Data_Response");

        setNormativeMediaType(new TextCsv(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }

    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }

    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String localUrl = ReqInfo.getLocalUrl(request);
        QueryParameters qp = new QueryParameters(request);

        String resourceID = getResourceId(localUrl, false);

        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request, response);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());

        response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");

        User user = new User(request);

        DataOutputStream os = new DataOutputStream(response.getOutputStream());
        besApi.writeDap4DataAsCsv(user, resourceID, qp,os);
        os.flush();
        ServletLogUtil.setResponseSize(os.size());
        log.debug("Sent {}  size: {}",getServiceTitle(),os.size());
    }


}
