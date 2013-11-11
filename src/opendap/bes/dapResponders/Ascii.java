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
package opendap.bes.dapResponders;

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.coreServlet.ReqInfo;
import opendap.dap.User;
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;



public class Ascii extends Dap4Responder {

    private Logger log;

    private static String _defaultRequestSuffix = ".ascii";


    public Ascii(String sysPath, BesApi besApi) {
        this(sysPath,null, _defaultRequestSuffix,besApi);
    }

    public Ascii(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, _defaultRequestSuffix,besApi);
    }

    public Ascii(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap2/ascii");
        setNormativeMediaType(new ServiceMediaType("text", "plain", getRequestSuffix()));
        setServiceTitle("DAP2 ASCII Data");
        setServiceDescription("The DAP2 Data response in ASCII form.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_ASCII_Data_Service");

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


        String relativeUrl = ReqInfo.getLocalUrl(request);

        String resourceID = getResourceId(relativeUrl, false);

        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String context = request.getContextPath();

        BesApi besApi = getBesApi();


        log.debug("sendASCII() for dataset: " + resourceID);

        response.setContentType(getNormativeMediaType().getMimeType());
        Version.setOpendapMimeHeaders(request,response,besApi);
        response.setHeader("Content-Description", "dods_ascii");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");



        User user = new User(request);
        int maxRS = user.getMaxResponseSize();



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        /*
        Document reqDoc =
                besApi.getRequestDocument(
                        BesApi.ASCII,
                        resourceID,
                        constraintExpression,
                        xdap_accept,
                        maxRS,
                        null,
                        null,
                        null,
                        BesApi.XML_ERRORS);

        if(!besApi.besTransaction(resourceID,reqDoc,os,erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(_systemPath,context, response);
            log.error("respondToHttpGetRequest() encountered a BESError: "+besError.getMessage());
        }
        */


        if(!besApi.writeASCII(resourceID,constraintExpression,xdap_accept,user.getMaxResponseSize(),os,erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(_systemPath,context, response);
            log.error("respondToHttpGetRequest() encountered a BESError: "+besError.getMessage());
        }




        os.flush();
        log.debug("Sent DAP ASCII data response.");


    }

}
