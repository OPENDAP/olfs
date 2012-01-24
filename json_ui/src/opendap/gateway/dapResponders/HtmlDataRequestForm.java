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

import opendap.bes.BESError;
import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.gateway.BesGatewayApi;
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/29/11
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class HtmlDataRequestForm extends BesDapResponder {



    private Logger log;


    private BesGatewayApi _besGatewayApi;

    private static String defaultRequestSuffixRegex = "\\.html?";


    public HtmlDataRequestForm(String sysPath, BesGatewayApi besApi) {
        this(sysPath, null, defaultRequestSuffixRegex, besApi);
    }

    public HtmlDataRequestForm(String sysPath, String pathPrefix, BesGatewayApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffixRegex, besApi);
    }

    public HtmlDataRequestForm(String sysPath, String pathPrefix,  String requestSuffixRegex, BesGatewayApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        _besGatewayApi = besApi;
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }



    @Override
    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        String dataSourceUrl = _besGatewayApi.getDataSourceUrl(request, getPathPrefix());
        String context = request.getContextPath();


        log.debug("sendHTMLRequestForm() for dataset: " + dataSource);

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request,response, _besGatewayApi);
        response.setHeader("Content-Description", "dods_form");


        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        log.debug("sendHTMLRequestForm(): Sending HTML Data Request Form For: "
                + dataSource +
                "    CE: '" + request.getQueryString() + "'");


        OutputStream os = response.getOutputStream();

        String url = request.getRequestURL().toString();

        int suffix_start = url.lastIndexOf("." + requestSuffix);

        url = url.substring(0, suffix_start);


        log.debug("sendHTMLRequestForm(): HTML Form URL: " + url);

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document reqDoc = _besGatewayApi.getRequestDocument(
                                                        BesApi.HTML_FORM,
                                                        dataSourceUrl,
                                                        null,
                                                        xdap_accept,
                                                        0,
                                                        null,
                                                        url,
                                                        null,
                                                        BesApi.XML_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource,reqDoc,os,erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));

            besError.sendErrorResponse(_systemPath, context, response);


            String msg = besError.getMessage();
            log.error("sendHTMLRequestForm() encountered a BESError: "+msg);
        }

        os.flush();




    }

}
