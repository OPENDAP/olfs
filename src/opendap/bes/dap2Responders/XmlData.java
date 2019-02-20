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
import opendap.http.mediaTypes.TextXml;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/31/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class XmlData extends Dap4Responder {
    private Logger log;



    private static String _defaultRequestSuffix = ".xdap";


    public XmlData(String sysPath, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath,null, _defaultRequestSuffix,besApi, addTypeSuffixToDownloadFilename);
    }




    public XmlData(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        addTypeSuffixToDownloadFilename(addTypeSuffixToDownloadFilename);
        setServiceRoleId("http://services.opendap.org/dap2/xml-data");
        setServiceTitle("DAP2 XML Data Response");
        setServiceDescription("An XML document containing both the DAP2 dataset's structural metadata along with data values.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_Data_Service");

        setNormativeMediaType(new TextXml(getRequestSuffix()));
        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());


    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }


    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl, false);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);
        User user = new User(request);

        BesApi besApi = getBesApi();

        log.debug("respondToHttpGetRequest(): Sending XML Data response For: " + resourceID +
                    "    CE: '" + constraintExpression + "'");

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request,response);
        response.setHeader("Content-Description", "dap_xml");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");
        response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        besApi.writeDap2DataAsXml(
                resourceID,
                constraintExpression,
                xdap_accept,
                user.getMaxResponseSize(),
                xmlBase,
                os);
        os.flush();
        log.info("Sent XML Data response.");
    }
}
