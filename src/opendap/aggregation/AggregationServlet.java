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

package opendap.aggregation;

import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.ppt.PPTException;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
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

// Some commentary from Nathan via chat:
/*

// How to handle the error stream to the two-stream BesApi methods.
// The other stream is the HTTP response stream. 

OutputStream os = response.getOutputStream();
ByteArrayOutputStream errors = new ByteArrayOutputStream();

if(!besApi.writeDMR(resourceID,qp,xmlBase,os,erros)){
String msg = new String(erros.toByteArray());
 log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
 os.write(msg.getBytes());

}
*/

public class AggregationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger _log;
    private BesApi _besApi;

    @Override
    public void init() throws ServletException {
        super.init();

        _log = LoggerFactory.getLogger(this.getClass());
        _besApi = new BesApi();

        _log.info("Initialized Aggregation #2.");

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

        _log.info(sb.toString());
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        _log.debug("doGet() - BEGIN");

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();

        StringBuilder echoText = new StringBuilder();
        getPlainText(request,echoText);

        out.print(echoText.toString());
        out.flush();

        // get the bes version info and dump it out
        // getBesVersion(String dataSource, Document response)
        Document version = null;
        try {
			_besApi.getBesVersion("/", version);
		} catch (BadConfigurationException | PPTException | JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(version, out);
        
        _log.debug("doGet() - END");
    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        _log.info("doHead() - BEGIN");

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();



        StringBuilder echoText = new StringBuilder();
        int contentLength = getPlainText(request,echoText);
        _log.info(echoText.toString());
        out.print(echoText.toString());
        out.flush();

        if(contentLength!=-1){
            _log.info("doHead() - Processing Request body. contentLength: {}",contentLength);
            out.print("Request Body: \n");
            out.print(convertStreamToString(request.getInputStream(),contentLength).toString());
            out.print("\n");
            out.print("\n");

        }
        _log.info("doHead() - END");

    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        _log.info("doPut() - BEGIN");

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();



        StringBuilder echoText = new StringBuilder();
        int contentLength = getPlainText(request,echoText);
        _log.info(echoText.toString());
        out.print(echoText.toString());
        out.flush();

        if(contentLength!=-1){
            _log.info("doPut() - Processing Request body. contentLength: {}",contentLength);
            out.print("Request Body: \n");
            out.print(convertStreamToString(request.getInputStream(),contentLength).toString());
            out.print("\n");
            out.print("\n");

        }
        _log.info("doPut() - END");

    }


    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        _log.info("doPost() - BEGIN");
        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();



        StringBuilder echoText = new StringBuilder();
        int contentLength = getPlainText(request,echoText);
        _log.info(echoText.toString());
        out.print(echoText.toString());
        out.flush();

        if(contentLength!=-1){
            _log.info("doPost() - Processing Request body. contentLength: {}",contentLength);
            out.print("Request Body: \n");
            out.print(convertStreamToString(request.getInputStream(),contentLength).toString());
            out.print("\n");
            out.print("\n");

        }

        _log.info("doPost() - END");

    }




    private int  getPlainText(HttpServletRequest request, StringBuilder out)
            throws IOException, ServletException {

        out.append("\n---------------------------------------------------------------------\n");
        out.append("HTTP Method: ").append(request.getMethod()).append("\n");
        out.append("\n");
        out.append("HTTP Request Headers");
        out.append("\n");
        Enumeration<String> headers = request.getHeaderNames();
        while(headers.hasMoreElements()){
            String headerName = headers.nextElement();
            String headerValue = request.getHeader(headerName);
            out.append("    ").append(headerName).append(": ").append(headerValue).append("\n");
        }

        out.append("\n");

        String queryString = request.getQueryString();
        out.append("Query String and KVP Evaluation\n");
        out.append("\n");
        out.append("  HttpServletRequest.getQueryString(): ").append(queryString).append("\n");
        out.append("\n");
        out.append("  Decoded: ").append(java.net.URLDecoder.decode(queryString == null ? "null" : queryString, "UTF-8")).append("\n");
        out.append("\n");

        int contentLength = request.getContentLength();
        out.append("request.getContentLength(): ").append(contentLength).append("\n");
        out.append("\n");

        if(contentLength==-1){
            _log.info("getPlainText() - Retrieving Content-Length header.");
            String s = request.getHeader("Content-Length");
            if(s!=null){
                contentLength = Integer.parseInt(s);
            }
        }

        String ctype = request.getHeader("Content-Type");

        if(ctype!=null && ctype.equalsIgnoreCase("application/x-www-form-urlencoded")){

            out.append("Content-Type indicates that the request body is form url encoded. Utilizing Servlet API to evaluate parameters.\n");

            out.append("  HttpServletRequest.getParameter()\n");
            out.append("        keyName            value \n");
            Enumeration<String> paramNames = request.getParameterNames();

            while(paramNames.hasMoreElements()){
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                out.append("        ").append(paramName).append(": ").append(paramValue).append("\n");
            }
   
            out.append("  HttpServletRequest.getParameterMap()\n");
            out.append("\n");

            Map paramMap = request.getParameterMap();

            out.append("  ParameterMap is an instance of: ").append(paramMap.getClass().getName()).append("\n");
            out.append("  ParameterMap contains ").append(paramMap.size()).append(" element(s).\n");
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
            out.append("Content-Type indicates that the request body is NOT form url encoded.\n");
        }

        return contentLength;

    }

    private StringBuilder convertStreamToString(ServletInputStream is, int size) throws IOException {
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

        return result;
    }

}
