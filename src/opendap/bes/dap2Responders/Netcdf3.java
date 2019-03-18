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
import opendap.logging.LogUtil;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/31/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class Netcdf3 extends Dap4Responder {


    private Logger log;
    private static String defaultRequestSuffix = ".nc";


    public Netcdf3(String sysPath, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath, null, defaultRequestSuffix, besApi, addTypeSuffixToDownloadFilename);
    }


    public Netcdf3(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        addTypeSuffixToDownloadFilename(addTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap4/data/netcdf-3");
        setServiceTitle("NetCDF-3 Data Response");
        setServiceDescription("NetCDF-3 representation of the DAP2 Data Response object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_Data_Service");

        setNormativeMediaType(new opendap.http.mediaTypes.Netcdf3(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }


    /**
     * For netCDF3, examine the name and prefix it with 'nc_' if the 
     * name would otherwise not be an acceptable filename (in 
     * practice this means 'if the name starts with a number').
     * 
     * {@inheritDoc}
     */
    @Override
    public String getDownloadFileName(String resourceID){

        String downloadFileName = super.getDownloadFileName(resourceID);
        /*
        // Turned this off at Fan's request - ndp 03/31/2017
        Pattern startsWithNumber = Pattern.compile("[0-9].*");
        if(startsWithNumber.matcher(downloadFileName).matches())
            downloadFileName = "nc_"+downloadFileName;
        */
        return downloadFileName;
    }



    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String resourceID = getResourceId(requestedResourceId, false);
        String cf_history_entry = ReqInfo.getCFHistoryEntry(request);
        User user = new User(request);

        log.debug("sendNormativeRepresentation(): cf_history_entry: '{}'",cf_history_entry);

        BesApi besApi = getBesApi();

        log.debug("sendNormativeRepresentation(): Sending  {} for dataset: {}",getServiceTitle(),resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request, response);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());

        response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");

        DataOutputStream os = new DataOutputStream(response.getOutputStream());
        besApi.writeDap2DataAsNetcdf3(resourceID, constraintExpression, cf_history_entry, user.getMaxResponseSize(), os);
        os.flush();
        LogUtil.setResponseSize(os.size());
        log.info("Sent {} size: {}", getServiceTitle(),os.size());
    }
}
