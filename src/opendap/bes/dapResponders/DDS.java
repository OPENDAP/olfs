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
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/29/11
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class DDS extends Dap4Responder {


    private Logger log;



    private static String _defaultRequestSuffix = ".dds";


    public boolean needsBesToMatch(){
        return true;
    }

    public boolean needsBesToRespond(){
        return true;
    }

    public DDS(String sysPath, BesApi besApi) {
        this(sysPath,null, _defaultRequestSuffix,besApi);
    }

    public DDS(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, _defaultRequestSuffix,besApi);
    }


    public DDS(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap2/dds");
        setServiceTitle("DAP2 DDS");
        setServiceDescription("DAP2 Data Description Structure (DDS).");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP2:_DDS_Service");

        setNormativeMediaType(new ServiceMediaType("text","plain", getRequestSuffix()));
        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }


    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl, false);


        String constraintExpression = ReqInfo.getConstraintExpression(request);

        BesApi besApi = getBesApi();


        log.debug("Sending DDS for dataset: " + resourceID);

        response.setContentType(getServiceMediaType());
        Version.setOpendapMimeHeaders(request,response,besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!besApi.writeDDS(resourceID,constraintExpression,xdap_accept,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }

        /*
        Document reqDoc = besApi.getRequestDocument(
                                                        BesApi.DDS,
                                                        resourceID,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        0,
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

        os.flush();
        log.debug("Sent DAP DDS.");




    }

}
