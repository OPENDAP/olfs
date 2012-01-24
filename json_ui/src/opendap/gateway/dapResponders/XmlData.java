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
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/31/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class XmlData extends BesDapResponder {
    private Logger log;


    private BesGatewayApi _besGatewayApi;

    private static String defaultRequestSuffixRegex = "\\.xdods";


    public XmlData(String sysPath, BesGatewayApi besApi) {
        this(sysPath, null, defaultRequestSuffixRegex, besApi);
    }

    public XmlData(String sysPath, String pathPrefix, BesGatewayApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffixRegex, besApi);
    }

    public XmlData(String sysPath, String pathPrefix,  String requestSuffixRegex, BesGatewayApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        _besGatewayApi = besApi;
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }


    @Override
    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);


        String dataSourceUrl = _besGatewayApi.getDataSourceUrl(request, getPathPrefix());



        User user = new User(request);
        int maxRS = user.getMaxResponseSize();


        log.debug("respondToHttpGetRequest(): Sending XML Data response For: " + dataSource +
                    "    CE: '" + constraintExpression + "'");


        response.setContentType("text/xml");
        Version.setOpendapMimeHeaders(request,response, _besGatewayApi);
        response.setHeader("Content-Description", "dap_xml");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();



        Document reqDoc = _besGatewayApi.getRequestDocument(
                                                        BesApi.XML_DATA,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        maxRS,
                                                        xmlBase,
                                                        null,
                                                        null,
                                                        BesApi.XML_ERRORS);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        log.debug("_besGatewayApi.getRequestDocument() returned:\n "+xmlo.outputString(reqDoc));

        if(!_besGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDDX() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }



        os.flush();
        log.info("Sent XML Data response.");


    }
}
