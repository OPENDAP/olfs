/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.bes.dapResponders;

import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.coreServlet.MimeBoundary;
import opendap.coreServlet.ReqInfo;
import opendap.dap.User;
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;


@Deprecated
public class DataDDX extends Dap4Responder {



    private Logger log;


    private static String _defaultRequestSuffix = ".dap";


    public DataDDX(String sysPath, BesApi besApi) {
        this(sysPath,null, _defaultRequestSuffix,besApi);
    }

    public DataDDX(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, _defaultRequestSuffix,besApi);
    }



    public DataDDX(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/data");
        setServiceTitle("DAP4 Data");
        setServiceDescription("DAP4 Data object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Data_Service");

        setNormativeMediaType(new ServiceMediaType("Multipart","Related", _defaultRequestSuffix));

        log.debug("defaultRequestSuffix: '{}'", _defaultRequestSuffix);

    }

    public boolean needsBesToMatch(){
        return true;
    }

    public boolean needsBesToRespond(){
        return true;
    }

    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String xmlBase = getXmlBase(request);
        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl, false);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        BesApi besApi = getBesApi();



        MimeBoundary mb = new MimeBoundary();
        String startID = mb.newContentID();

        log.debug("sendDataDDX() for dataset: " + resourceID+
                "    CE: '" + constraintExpression + "'");


        response.setContentType("Multipart/Related;  "+
                                "type=\"text/xml\";  "+
                                "start=\"<"+startID+">\";  "+
                                "boundary=\""+mb.getBoundary()+"\"");


        Version.setOpendapMimeHeaders(request,response,besApi);
        response.setHeader("Content-Description", "dap4_data_ddx");

        // This header indicates to the client that the content of this response
        // is dependant on the HTTP request header XDAP-Accept
        response.setHeader("Vary", "XDAP-Accept");

        // Because the content of this response is dependant on a client provided
        // HTTP header (XDAP-Accept) it is useful to include this Cach-Control
        // header to make caching work correctly...
        response.setHeader("Cache-Control", "public");


        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");


        User user = new User(request);



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document reqDoc = besApi.getDataDDXRequest(resourceID,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        user.getMaxResponseSize(),
                                                        xmlBase,
                                                        startID,
                                                        mb.getBoundary());

        if(!besApi.besTransaction(resourceID,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();
        log.info("Sent DAP4 Data DDX response.");




    }

}
