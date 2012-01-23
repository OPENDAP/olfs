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

import opendap.bes.BESError;
import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.coreServlet.ReqInfo;
import org.jdom.Document;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;






public class DatasetInfoHtmlPage extends BesDapResponder {



    private Logger log;


    private static String defaultRequestSuffixRegex = "\\.info";


    public DatasetInfoHtmlPage(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }
    public DatasetInfoHtmlPage(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }


    public DatasetInfoHtmlPage(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }

    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String context = request.getContextPath();

        BesApi besApi = getBesApi();

        log.debug("sendINFO() for dataset: " + dataSource);

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request,response,besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = besApi.getRequestDocument(
                                                        BesApi.INFO_PAGE,
                                                        dataSource,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        0,
                                                        null,
                                                        null,
                                                        null,
                                                        BesApi.XML_ERRORS);

        if(!besApi.besTransaction(dataSource,reqDoc,os,erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(_systemPath, context, response);
            log.error("respondToHttpGetRequest() encountered a BESError: "+besError.getMessage());

        }


        os.flush();
        log.info("Sent DAP Info page.");




    }

}
