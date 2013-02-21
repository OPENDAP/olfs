/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2012 OPeNDAP, Inc.
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
package opendap.experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;


public class EchoServlet extends HttpServlet {

    private Logger log;

    @Override
    public void init() throws ServletException {
        super.init();

        log = LoggerFactory.getLogger(this.getClass());
    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();
        String echoText = getPlainText(request);

        log.info(echoText);
        out.print(echoText);

    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/plain");
        String echoText = getPlainText(request);
        log.info(echoText);

        //System.out.print(echoText);

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();
        String echoText = getPlainText(request);

        log.info(echoText);
        out.print(echoText);

    }
    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();
        String echoText = getPlainText(request);

        log.info(echoText);
        out.print(echoText);

    }

    public String  getPlainText(HttpServletRequest request)
            throws IOException, ServletException {


        StringBuilder out = new StringBuilder();

        out.append("\n\n---------------------------------------------------------------------\n");
        out.append("HTTP Method: ").append(request.getMethod()).append("\n");
        out.append("\n");
        out.append("HTTP Request Headers");



        out.append("\n");
        Enumeration<String> headers = request.getHeaderNames();
        while(headers.hasMoreElements()){
            String headerName = headers.nextElement();
            String headerValue = request.getHeader(headerName);
            out.append("    ").append(headerName).append("            ").append(headerValue).append("\n");
        }
        out.append("\n");
        out.append("\n");

        String queryString = request.getQueryString();
        out.append("\n");
        out.append("Query String and KVP Evaluation\n");
        out.append("\n");
        out.append("  HttpServletRequest.getQueryString():     ").append(queryString).append("\n");
        out.append("\n");
        out.append("  Decoded:                                 ").append(java.net.URLDecoder.decode(queryString == null ? "null" : queryString, "UTF-8")).append("\n");
        out.append("\n");



        out.append("  HttpServletRequest.getParameter()\n");
        out.append("        keyName            value \n");
        Enumeration<String> paramNames = request.getParameterNames();

        while(paramNames.hasMoreElements()){
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            out.append("        ").append(paramName).append("            ").append(paramValue).append("\n");
        }
        out.append("\n");
        out.append("  HttpServletRequest.getParameterMap()\n");
        out.append("\n");


        Map paramMap = request.getParameterMap();

        out.append("    ParameterMap is an instance of: ").append(paramMap.getClass().getName()).append("\n");
        out.append("    ParameterMap contains ").append(paramMap.size()).append(" element(s).\n");
        out.append("\n");
        out.append("        keyName            value(s) \n");


        for(Object o:paramMap.keySet()){
            String key = (String) o;

            Object oo =   paramMap.get(key);

            String[] values = (String[]) oo;
            out.append("        ").append(key).append("            ");
            boolean first=true;
            for(String value:values){
                if(!first)
                    out.append(", ");
                out.append("'").append(value).append("'");
                first = false;
            }
            out.append("\n");

        }
        out.append("\n");


        return out.toString();

    }






}
