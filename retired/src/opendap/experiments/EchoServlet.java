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

package opendap.experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Map;


public class EchoServlet extends HttpServlet {

    private Logger log;

    @Override
    public void init() throws ServletException {
        super.init();

        log = LoggerFactory.getLogger(this.getClass());

        Runtime runtime = Runtime.getRuntime();

        NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        sb.append("init() - free memory: " + format.format(freeMemory / 1024) + "\n");
        sb.append("init() - allocated memory: " + format.format(allocatedMemory / 1024) + "\n");
        sb.append("init() - max memory: " + format.format(maxMemory / 1024) + "\n");
        sb.append("init() - total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n");


        log.info(sb.toString());

    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        log.info("doGet() - BEGIN");

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();



        StringBuilder echoText = new StringBuilder();
        int contentLength = getPlainText(request,echoText);
        log.info(echoText.toString());
        out.print(echoText.toString());
        out.flush();

        if(contentLength!=-1){
            log.info("doGet() - Processing Request body. contentLength: {}",contentLength);
            out.print("Request Body: \n");
            out.print(convertStreamToString(request.getInputStream(),contentLength).toString());
            out.print("\n");
            out.print("\n");

        }
        log.info("doGet() - END");

    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        log.info("doHead() - BEGIN");

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();



        StringBuilder echoText = new StringBuilder();
        int contentLength = getPlainText(request,echoText);
        log.info(echoText.toString());
        out.print(echoText.toString());
        out.flush();

        if(contentLength!=-1){
            log.info("doHead() - Processing Request body. contentLength: {}",contentLength);
            out.print("Request Body: \n");
            out.print(convertStreamToString(request.getInputStream(),contentLength).toString());
            out.print("\n");
            out.print("\n");

        }
        log.info("doHead() - END");

    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        log.info("doPut() - BEGIN");

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();



        StringBuilder echoText = new StringBuilder();
        int contentLength = getPlainText(request,echoText);
        log.info(echoText.toString());
        out.print(echoText.toString());
        out.flush();

        if(contentLength!=-1){
            log.info("doPut() - Processing Request body. contentLength: {}",contentLength);
            out.print("Request Body: \n");
            out.print(convertStreamToString(request.getInputStream(),contentLength).toString());
            out.print("\n");
            out.print("\n");

        }
        log.info("doPut() - END");

    }


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        log.info("doPost() - BEGIN");
        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();



        StringBuilder echoText = new StringBuilder();
        int contentLength = getPlainText(request,echoText);
        log.info(echoText.toString());
        out.print(echoText.toString());
        out.flush();

        if(contentLength!=-1){
            log.info("doPost() - Processing Request body. contentLength: {}",contentLength);
            out.print("Request Body: \n");
            out.print(convertStreamToString(request.getInputStream(),contentLength).toString());
            out.print("\n");
            out.print("\n");

        }

        log.info("doPost() - END");

    }




    public int  getPlainText(HttpServletRequest request, StringBuilder out)
            throws IOException, ServletException {


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


        int contentLength = request.getContentLength();
        log.info("getPlainText() - request.getContentLength() returned "+contentLength);

        out.append("request.getContentLength(): ").append(contentLength).append("\n");
        out.append("\n");

        if(contentLength==-1){
            log.info("getPlainText() - Retrieving Content-Length header.");
            String s = request.getHeader("Content-Length");
            if(s!=null){
                contentLength = Integer.parseInt(s);
            }
        }

        log.info("getPlainText() - Using contentLength: "+contentLength);


        String ctype = request.getHeader("Content-Type");

        if(ctype!=null && ctype.equalsIgnoreCase("application/x-www-form-urlencoded")){

            out.append("Content-Type indicates that the request body is form url encoded. Utilizing Servlet API to evaluate parameters.\n\n");

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

        }
        else {
            out.append("Content-Type indicates that the request body is NOT form url encoded.\n\n");

            return contentLength;
        }







        return contentLength;

    }




    public String scannerConvertStreamToString(java.io.InputStream is) throws IOException {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String result = "";

        if(s.hasNext()){
            log.info("convertStreamToString() - Scanner found at least one token in the stream.");
            result = s.next();
            log.info("convertStreamToString() - Token size: "+result.length());
        }


        return result;
    }



    public StringBuilder convertStreamToString(ServletInputStream is, int size) throws IOException {


        log.info("convertStreamToString() - BEGIN");

        log.info("convertStreamToString() - size: "+size);


        StringBuilder result = new StringBuilder(size);



        int ret;
        boolean done = false;
        while(!done){
            ret = is.read();
            if(ret==-1)
                done = true;
            else
                result.append((char)ret);

        }




        log.info("convertStreamToString() - END");

        return result;
    }





}
