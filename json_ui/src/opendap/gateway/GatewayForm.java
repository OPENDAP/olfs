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
package opendap.gateway;

import opendap.bes.Version;
import opendap.coreServlet.HttpResponder;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/1/11
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayForm extends HttpResponder {



    Logger log;

    private static String defaultRegex = ".*";


    public GatewayForm(String sysPath) {
        super(sysPath, null, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public GatewayForm(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }




    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String contextPath = request.getContextPath();



        String gatewayFormFile = _systemPath + "/gateway/gateway_form.html";
        String form = readFileAsString(gatewayFormFile);


        //String gatewayFormResource = "resources/gateway/gateway_form.html";
        //InputStream is = this.getClass().getClassLoader().getSystemResourceAsStream(gatewayFormResource);
        //form  = streamToString(is);


        form = form.replaceAll("<CONTEXT_PATH />",contextPath);
        form = form.replaceAll("<SERVLET_NAME />","/docs");


        log.debug("respondToHttpGetRequest(): Sending Gateway Page ");

        response.setContentType("text/html");
        response.setHeader("Content-Description", "gateway_form");

        ServletOutputStream sos  = response.getOutputStream();

        sos.println(form);

    }


}
