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

import javax.print.DocFlavor;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;


public class EchoServlet extends HttpServlet {


    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {









        getPlainText(request,response);

    }

    public void getPlainText(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/plain");
        ServletOutputStream out = response.getOutputStream();


        out.println("---------------------------------------------------------------------");
        out.println("---------------------------------------------------------------------");

        out.println("");
        out.println("HTTP Request Headers");


        out.println("");
        out.println("    Header Name            Value");
        Enumeration<String> headers = request.getHeaderNames();
        while(headers.hasMoreElements()){
            String headerName = headers.nextElement();
            String headerValue = request.getHeader(headerName);
                out.println("    "+headerName +"            "+headerValue);
        }
        out.println();
        out.println();

        String queryString = request.getQueryString();
        out.println("---------------------------------------------------------------------");
        out.println("Query String and KVP Evaluation");
        out.println();
        out.println("  HttpServletRequest.getQueryString():     "+ queryString);
        out.println();
        out.println("  Decoded:                                 "+java.net.URLDecoder.decode(queryString==null?"null":queryString,"UTF-8"));
        out.println();


        out.println("---------------------------------------------------------------------");
        out.println("HttpServletRequest.getParameter()");
        out.println("    keyName            value ");
        Enumeration<String> paramNames = request.getParameterNames();

        while(paramNames.hasMoreElements()){
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
                out.println("    "+paramName+"            "+paramValue);
        }
        out.println();
        out.println("---------------------------------------------------------------------");
        out.println("HttpServletRequest.getParameterMap()");
        out.println();


        Map paramMap = request.getParameterMap();

        out.println("ParameterMap is an instance of: "+paramMap.getClass().getName());
        out.println("ParameterMap contains "+paramMap.size()+" element(s).");
        out.println();
        out.println("    keyName            value(s) ");


        for(Object o:paramMap.keySet()){
            String key = (String) o;

            Object oo =   paramMap.get(key);

            String[] values = (String[]) oo;
            out.print("    "+key+"            ");
            boolean first=true;
            for(String value:values){
                if(!first)
                    out.print(", ");
                out.print("'"+value+"'");
                first = false;
            }
            out.println();

        }
        out.println();

    }




    public void getHtml(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("text/html");
        ServletOutputStream out = response.getOutputStream();


        out.println("<html>");
        out.println("<head><title>Echo page</title></head>");
        out.println("<body>");

        out.println("<hr />");
        out.println("<h3>HTTP Request Headers</h3>");


        out.println("<table>");
        out.println("<th>Header Name</th><th>Value</th>");
        Enumeration<String> headers = request.getHeaderNames();
        while(headers.hasMoreElements()){
            String headerName = headers.nextElement();
            String headerValue = request.getHeader(headerName);
            out.println("<tr>");
                out.println("<td style=\"text-align: right;\"><code><strong>"+headerName+"</strong></code>&nbsp;&nbsp;</td>");
                out.println("<td><code> "+headerValue+"</code></td>");
            out.println("</tr>");
        }
        out.println("</table>");

        out.println("<hr />");

        out.println("<h3>Query String and KVP Evaluation</h3>");

        out.println("<table>");

        out.println("<tr>");
            out.println("<td style=\"text-align: left;\"><code><strong>Raw &nbsp;</strong></code></td>");
            out.println("<td><pre>"+StringEscapeUtils.escapeHtml(request.getQueryString())+"</pre></td>");
        out.println("</tr>");

        out.println("<tr>");
            out.println("<td style=\"text-align: left;\"><code><strong>Decoded &nbsp;</strong></code></td>");
            out.println("<td><pre>"+StringEscapeUtils.escapeHtml(java.net.URLDecoder.decode(request.getQueryString()+"","UTF-8"))+"</pre></td>");
        out.println("</tr>");

        out.println("</table>");


        out.println("<br />");
        out.println("<hr style=\"border:dashed #00CCFF; border-width:1px 0 0 0; height:0;line-height:0px;font-size:0;margin:0;padding:0;\">");
        out.println("<br />");

        out.println("<table>");
        out.println("<th style=\"text-align: left;\">keyName</th><th style=\"text-align: left;\">value</th>");
        Enumeration<String> paramNames = request.getParameterNames();

        while(paramNames.hasMoreElements()){
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            out.println("<tr>");
                out.println("<td style=\"text-align: left;\"><code><strong>"+paramName+"</strong></code>&nbsp;&nbsp;</td>");
                out.println("<td><pre>"+StringEscapeUtils.escapeHtml(paramValue)+"</pre></td>");
            out.println("</tr>");
        }
        out.println("</table>");


        out.println("<hr />");


        Map paramMap = request.getParameterMap();

        out.println("<p>ParameterMap is an instance of: "+paramMap.getClass().getName()+"</p>");
        out.println("<p>ParameterMap contains "+paramMap.size()+" elements.</p>");
        out.println("<table>");
        out.println("<th style=\"text-align: left;\">keyName</th><th style=\"text-align: left;\">value</th>");


        for(Object o:paramMap.keySet()){
            String key = (String) o;

            Object oo =   paramMap.get(key);

            String[] values = (String[]) oo;
            out.println("<tr>");
            out.println("<td style=\"text-align: left;\"><code><strong>"+key+"</strong></code>&nbsp;&nbsp;</td>");
            out.println("<td><pre>");
            boolean first=true;
            for(String value:values){
                if(!first)
                    out.print(", ");
                out.print(StringEscapeUtils.escapeHtml(value));
                first = false;
            }
            out.println("\n</pre></td>");

            out.println("</tr>");
        }
        out.println("</table>");

        out.println("<hr />");

        out.println("</body>");
        out.println("</html>");
    }








}
