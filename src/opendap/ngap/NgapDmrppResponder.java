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

package opendap.ngap;

import opendap.bes.BesApi;
import opendap.bes.Version;
import opendap.ngap.NgapBesApi;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.*;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.http.mediaTypes.TextXml;
import opendap.logging.ServletLogUtil;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;


public class NgapDmrppResponder extends Dap4Responder {



    private Logger log;
    private static String defaultRequestSuffix = ".dmrpp";



    public NgapDmrppResponder(String sysPath, NgapBesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath,null, defaultRequestSuffix, besApi, addTypeSuffixToDownloadFilename);
    }

    public NgapDmrppResponder(String sysPath, String pathPrefix, NgapBesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi, addTypeSuffixToDownloadFilename);
    }

    public NgapDmrppResponder(String sysPath, String pathPrefix, String requestSuffixRegex, NgapBesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        addTypeSuffixToDownloadFilename(addTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("DMR++ annotated DMR.");
        setServiceDescription("An annotated Dataset Metadata Response document containing dmr++ markup for remote data access.");
        setServiceDescriptionLink("https://opendap.github.io/DMRpp-wiki/DMRpp.html");

        setNormativeMediaType(new TextXml(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }



    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }


    @Override
    public BesApi getBesApi() {
        BesApi besApi = super.getBesApi();
        if(besApi instanceof NgapBesApi)
            return besApi;

        return null;
    }

    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String requestedResourceId = ReqInfo.getLocalUrl(request);
        QueryParameters qp = new QueryParameters(request);
        String xmlBase = getXmlBase(request);
        String resourceID = getResourceId(requestedResourceId, false);

        NgapBesApi besApi = (NgapBesApi) getBesApi();
        if (besApi == null){
            throw new OPeNDAPException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"An instance of NgapBesApi was not found. Unable to proceed with dmr++ response as the" +
                    "NgapBesApi contains a specialization required for this feature." );
        }

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());
        //response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");

        TransmitCoordinator tc = new ServletResponseTransmitCoordinator(response);
        DataOutputStream os = new DataOutputStream(response.getOutputStream());
        User user = new User(request);
        besApi.writeDmrpp(user,resourceID,qp,xmlBase,os, tc);
        os.flush();
        ServletLogUtil.setResponseSize(os.size());
        log.debug("Sent {} size:{}",getServiceTitle(),os.size());
    }
}
