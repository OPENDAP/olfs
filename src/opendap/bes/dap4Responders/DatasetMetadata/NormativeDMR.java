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

package opendap.bes.dap4Responders.DatasetMetadata;

import opendap.bes.Version;
import opendap.bes.BesApi;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.*;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.http.mediaTypes.DMR;
import opendap.logging.ServletLogUtil;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;


public class NormativeDMR extends Dap4Responder {
    private Logger log;
    private static String defaultRequestSuffix = ".dmr";


    public NormativeDMR(String sysPath, BesApi besApi, boolean addTypeSuffixToDownloadFilename, boolean enforceRequiredUserSelection, boolean showDmrppLink) {
        this(sysPath, null, defaultRequestSuffix, besApi, addTypeSuffixToDownloadFilename,enforceRequiredUserSelection, showDmrppLink);
    }

    public NormativeDMR(
            String sysPath,
            String pathPrefix,
            String requestSuffix,
            BesApi besApi,
            boolean addFileoutTypeSuffixToDownloadFilename,
            boolean enforceRequiredUserSelection,
            boolean showDmrppLink) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        addTypeSuffixToDownloadFilename(addFileoutTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("Dataset Metadata Response");
        setServiceDescription("DAP4 Dataset Description and Attribute XML Document.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#Dataset_Metadata_Response");

        setNormativeMediaType(new DMR(getRequestSuffix()));

        addAltRepResponder(new XmlDMR   (sysPath, pathPrefix, besApi, addFileoutTypeSuffixToDownloadFilename));
        addAltRepResponder(new HtmlDMR  (sysPath, pathPrefix, besApi, enforceRequiredUserSelection, showDmrppLink));
        addAltRepResponder(new RdfDMR   (sysPath, pathPrefix, besApi, addFileoutTypeSuffixToDownloadFilename));
        addAltRepResponder(new JsonDMR  (sysPath, pathPrefix, besApi, addFileoutTypeSuffixToDownloadFilename));
        addAltRepResponder(new IjsonDMR (sysPath, pathPrefix, besApi, addFileoutTypeSuffixToDownloadFilename));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }



    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }



    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String requestedResourceId = ReqInfo.getLocalUrl(request);
        QueryParameters qp = new QueryParameters(request);
        String xmlBase = getXmlBase(request);
        String resourceID = getResourceId(requestedResourceId, false);

        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());

        TransmitCoordinator tc = new ServletResponseTransmitCoordinator(response);
        DataOutputStream os = new DataOutputStream(response.getOutputStream());
        User user = new User(request);
        besApi.writeDMR(user, resourceID, qp, xmlBase, os, tc);
        os.flush();
        ServletLogUtil.setResponseSize(os.size());
        log.debug("Sent {} size:{}",getServiceTitle(),os.size());
    }
}
