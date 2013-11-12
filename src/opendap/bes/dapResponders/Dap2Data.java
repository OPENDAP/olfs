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

import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.dap.User;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;






public class Dap2Data extends Dap4Responder {



    private Logger log;


    private static String _defaultRequestSuffix = ".dods";


    public Dap2Data(String sysPath, BesApi besApi) {
        this(sysPath,null, _defaultRequestSuffix,besApi);
    }

    public Dap2Data(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, _defaultRequestSuffix,besApi);
    }


    public Dap2Data(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap2/data");
        setServiceTitle("DAP2 Data");
        setServiceDescription("DAP2 Data Object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP2:_Data_Service");


        setNormativeMediaType(new ServiceMediaType("application","octet-stream", getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }



    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {



        BesApi besApi = getBesApi();


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl,false);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        User user = new User(request);
        int maxRS = user.getMaxResponseSize();


        log.debug("sendNormativeRepresentation() For: " + resourceID+
                "    CE: '" + constraintExpression + "'");

        response.setContentType(getNormativeMediaType().getMimeType());
        Version.setOpendapMimeHeaders(request,response,besApi);
        response.setHeader("Content-Description", "dods_data");


        String downloadFileName = Scrub.fileName(relativeUrl.substring(relativeUrl.lastIndexOf("/") + 1, relativeUrl.length()));
        String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";
        response.setHeader("Content-Disposition", contentDisposition);


        String xdap_accept = request.getHeader("XDAP-Accept");


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();



        /*
        Document reqDoc =
                besApi.getRequestDocument(
                        BesApi.DAP2_DATA,
                        resourceID,
                        constraintExpression,
                        xdap_accept,
                        maxRS,
                        null,
                        null,
                        null,
                        BesApi.DAP2_ERRORS);

        if(!besApi.besTransaction(resourceID,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }

*/

        if(!besApi.writeDap2Data(resourceID,constraintExpression,xdap_accept,user.getMaxResponseSize(),os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendNormativeRepresentation() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();
        log.info("sendNormativeRepresentation() Sent DAP2 data response.");




    }

}
