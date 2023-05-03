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

import opendap.bes.BesApi;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.dap.User;
import opendap.http.mediaTypes.TextPlain;
import opendap.logging.ServletLogUtil;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/29/11
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class DAS extends Dap4Responder {


    private Logger log;



    private static String _defaultRequestSuffix = ".das";


    public DAS(String sysPath, BesApi besApi) {
        this(sysPath,null, _defaultRequestSuffix,besApi);
    }

    public DAS(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, _defaultRequestSuffix,besApi);
    }


    public DAS(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap2/das");
        setServiceTitle("DAP2 DAS");
        setServiceDescription("DAP2 Dataset Attribute Structure (DAS).");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_DAS_Service");

        setNormativeMediaType(new TextPlain(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }

    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }


    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl,false);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        BesApi besApi = getBesApi();

        log.debug("sendDAS() for dataset: " + resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dods_das");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);

        DataOutputStream os = new DataOutputStream(response.getOutputStream());
        User user = new User(request);
        besApi.writeDAS(user, resourceID, constraintExpression, os);
        os.flush();
        ServletLogUtil.setResponseSize(os.size());
        log.debug("Sent {} size:{}",getServiceTitle(),os.size());
    }
}
