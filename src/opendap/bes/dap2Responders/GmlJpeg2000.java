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
import opendap.coreServlet.Scrub;
import opendap.dap.User;
import opendap.http.mediaTypes.Jpeg2000;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * Responder that transmits GML-JPEG2000 encoded DAP2 data to the client.
 */
public class GmlJpeg2000 extends Dap4Responder {

    private Logger log;
    private static String defaultRequestSuffix = ".jp2";

    public GmlJpeg2000(String sysPath, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath, null, defaultRequestSuffix, besApi, addTypeSuffixToDownloadFilename);
    }

    public GmlJpeg2000(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        addTypeSuffixToDownloadFilename(addTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap4/data/gmljp2");
        setServiceTitle("GML-JPEG2000 Data Response");
        setServiceDescription("GML-JPEG2000 representation of the DAP4 Data Response object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_Data_Service");

        setNormativeMediaType(new Jpeg2000(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());
    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }


    @Override
    public boolean matches(String relativeUrl, boolean checkWithBes){
        return super.matches(relativeUrl,checkWithBes);
    }


    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String resourceID = getResourceId(requestedResourceId, false);


        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");

        Version.setOpendapMimeHeaders(request, response, besApi);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());

        Version.setOpendapMimeHeaders(request, response, besApi);

        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());



        String xdap_accept = "3.2";
        User user = new User(request);


        OutputStream os = response.getOutputStream();

        besApi.writeDap2DataAsGmlJpeg2000(
                resourceID,
                constraintExpression,
                xdap_accept,
                user.getMaxResponseSize(),
                os);

        os.flush();
        log.debug("Sent {}",getServiceTitle());



    }



}
