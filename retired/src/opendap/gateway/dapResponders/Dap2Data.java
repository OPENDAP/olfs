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
package opendap.gateway.dapResponders;

import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.dap.User;
import opendap.gateway.BesGatewayApi;
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;





@Deprecated
public class Dap2Data extends BesDapResponder {



    private Logger log;


    private BesGatewayApi _besGatewayApi;

    private static String defaultRequestSuffixRegex = "\\.dods";


    public Dap2Data(String sysPath, BesGatewayApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }

    public Dap2Data(String sysPath, String pathPrefix, BesGatewayApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }

    public Dap2Data(String sysPath, String pathPrefix,  String requestSuffixRegex, BesGatewayApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        _besGatewayApi = besApi;
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }






    @Override
    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = getBesApi().getBesDataSourceID(relativeUrl, getRequestSuffixMatchPattern(), false);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String dataSourceUrl = _besGatewayApi.getDataSourceUrl(request, getPathPrefix());


        User user = new User(request);
        int maxRS = user.getMaxResponseSize();

        log.debug("sendDAP2Data() For: " + dataSource+
                "    CE: '" + constraintExpression + "'");

        response.setContentType("application/octet-stream");
        Version.setOpendapMimeHeaders(request,response, _besGatewayApi);
        response.setHeader("Content-Description", "dods_data");


        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besGatewayApi.getRequestDocument(
                                                        BesApi.DAP2,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        maxRS,
                                                        null,
                                                        null,
                                                        null,
                                                        BesApi.DAP2_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDAP2Data() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();
        log.info("Sent DAP2 data response.");




    }

}
