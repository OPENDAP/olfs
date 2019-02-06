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
import opendap.http.mediaTypes.TextCsv;
import opendap.http.mediaTypes.TextPlain;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;


/**
 * Responder that transmits ASCII (CSV) encoded DAP2 data to the client.
 */
public class Ascii extends Dap4Responder {

    private Logger log;
    private static String _defaultRequestSuffix = ".ascii";

    public Ascii(String sysPath, BesApi besApi) {
        this(sysPath, null, _defaultRequestSuffix, besApi);
    }

    public Ascii(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, _defaultRequestSuffix, besApi);
    }

    public Ascii(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap2/ascii");
        setServiceTitle("Plain text ASCII encoded DAP2 data");
        setServiceDescription("The DAP2 Data response in plain text ASCII form.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_ASCII_Data_Service");

        setNormativeMediaType(new TextPlain(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());
    }


    public boolean isDataResponder() {
        return true;
    }

    public boolean isMetadataResponder() {
        return false;
    }


    @Override
    public boolean matches(String relativeUrl, boolean checkWithBes) {
        return super.matches(relativeUrl, checkWithBes);
    }


    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);

        String resourceID = getResourceId(relativeUrl, false);

        String constraintExpression = ReqInfo.getConstraintExpression(request);

        BesApi besApi = getBesApi();

        log.debug("sendASCII() for dataset: " + resourceID);

        MediaType responseMediaType = getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request, response);
        response.setHeader("Content-Description", "dods_ascii");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        User user = new User(request);

        OutputStream os = response.getOutputStream();
        besApi.writeDap2DataAsAscii(resourceID, constraintExpression, xdap_accept, user.getMaxResponseSize(), os);
        os.flush();
        log.debug("Sent DAP ASCII data response.");
    }
}
