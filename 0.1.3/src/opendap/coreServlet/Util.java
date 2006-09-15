/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
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

package opendap.coreServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Mar 31, 2006
 * Time: 6:32:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util {


    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public static void sendSystemProperties(HttpServletRequest request,
                                            HttpServletResponse response,
                                            OpendapHttpDispatchHandler odh)
            throws IOException {


        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println("<html>");
        pw.println("<title>System Properties</title>");
        pw.println("<hr>");
        pw.println("<body><h2>System Properties</h2>");
        pw.println("<h3>Date: " + new Date() + "</h3>");

        Properties sysp = System.getProperties();
        Enumeration e = sysp.propertyNames();

        pw.println("<ul>");
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();

            String value = System.getProperty(name);

            pw.println("<li>" + name + ": " + value + "</li>");
        }
        pw.println("</ul>");

        pw.println("<h3>Runtime Info:</h3>");

        Runtime rt = Runtime.getRuntime();
        pw.println("JVM Max Memory:   " + (rt.maxMemory() / 1024) / 1000. + " MB (JVM Maximum Allowable Heap)<br>");
        pw.println("JVM Total Memory: " + (rt.totalMemory() / 1024) / 1000. + " MB (JVM Heap size)<br>");
        pw.println("JVM Free Memory:  " + (rt.freeMemory() / 1024) / 1000. + " MB (Unused part of heap)<br>");
        pw.println("JVM Used Memory:  " + ((rt.totalMemory() - rt.freeMemory()) / 1024) / 1000. + " MB (Currently active memory)<br>");

        pw.println("<hr>");
        pw.println("</body>");
        pw.println("</html>");
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }

    /**
     * ************************************************************************
     * This is a bit of instrumentation that I kept around to let me look at the
     * state of the incoming <code>HttpServletRequest</code> from the client.
     * This method calls the <code>get*</code> methods of the request and prints
     * the results to standard out.
     *
     * @param request The <code>HttpServletRequest</code> object to probe.
     */

    public static void probeRequest(PrintStream ps, HttpServlet servlet, HttpServletRequest request, ServletContext scntxt, ServletConfig scnfg) {

        Enumeration e;
        int i;


        ps.println("####################### PROBE ##################################");
        ps.println("");
        ps.println("HttpServlet:");
        ps.println("    getServletName(): "+servlet.getServletName());
        ps.println("    getServletInfo(): "+servlet.getServletInfo());
        ps.println("");
        ps.println("");
        ps.println("");
        ps.println("");
        ps.println("");
        ps.println("");
        ps.println("The HttpServletRequest object is actually an instance of:");
        ps.println("    " + request.getClass().getName());
        ps.println("");
        ps.println("HttpServletRequest Interface:");
        ps.println("    getAuthType:           " + request.getAuthType());
        ps.println("    getContextPath:        " + request.getContextPath());
        ps.println("    getMethod:             " + request.getMethod());
        ps.println("    getPathInfo:           " + request.getPathInfo());
        ps.println("    getPathTranslated:     " + request.getPathTranslated());
        ps.println("    getRequestURL:         " + request.getRequestURL());
        ps.println("    getQueryString:        " + request.getQueryString());
        ps.println("    getRemoteUser:         " + request.getRemoteUser());
        ps.println("    getRequestedSessionId: " + request.getRequestedSessionId());
        ps.println("    getRequestURI:         " + request.getRequestURI());
        ps.println("    getServletPath:        " + request.getServletPath());
        ps.println("    isRequestedSessionIdFromCookie: " + request.isRequestedSessionIdFromCookie());
        ps.println("    isRequestedSessionIdValid:      " + request.isRequestedSessionIdValid());
        ps.println("    isRequestedSessionIdFromURL:    " + request.isRequestedSessionIdFromURL());
        //ps.println("    isUserInRole:                   " + request.isUserInRole());

        ps.println("");
        ps.print("    Cookies:");
        Cookie c[] = request.getCookies();
        if (c == null)
            ps.println("   None.");
        else {
            ps.println();
            for (i = 0; i < c.length; i++)
                ps.println("        cookie[" + i + "]: " + c[i]);
        }

        ps.println("");
        i = 0;
        e = request.getHeaderNames();
        ps.println("    Header Names:");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            ps.print("        Header[" + i + "]: " + s);
            ps.println(": " + request.getHeader(s));
        }

        ps.println("");
        ps.println("ServletRequest Interface:");
        ps.println("    getCharacterEncoding:  " + request.getCharacterEncoding());
        ps.println("    getContentType:        " + request.getContentType());
        ps.println("    getContentLength:      " + request.getContentLength());
        ps.println("    getProtocol:           " + request.getProtocol());
        ps.println("    getScheme:             " + request.getScheme());
        ps.println("    getServerName:         " + request.getServerName());
        ps.println("    getServerPort:         " + request.getServerPort());
        ps.println("    getRemoteAddr:         " + request.getRemoteAddr());
        ps.println("    getRemoteHost:         " + request.getRemoteHost());
        //ps.println("    getRealPath:           "+request.getRealPath());


        ps.println(".............................");
        ps.println("");
        i = 0;
        e = request.getAttributeNames();
        ps.println("    Attribute Names:");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            ps.print("        Attribute[" + i + "]: " + s);
            ps.println(" Type: " + request.getAttribute(s));
        }

        ps.println(".............................");
        ps.println("");
        i = 0;
        e = request.getParameterNames();
        ps.println("    Parameter Names:");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            ps.print("        Parameter[" + i + "]: " + s);
            ps.println(" Value: " + request.getParameter(s));
        }

        ps.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
        ps.println(" . . . . . . . . . Servlet Infomation API  . . . . . . . . . . . . . .");
        ps.println("");

        ps.println("Servlet Context:");

        ps.println("    getMajorVersion:       " + scntxt.getMajorVersion());
        ps.println("    getMinorVersion:       " + scntxt.getMinorVersion());
        ps.println("    getServerInfo:         " + scntxt.getServerInfo());
        ps.println("    getServletContextName: " + scntxt.getServletContextName());
        ps.println("");

        i = 0;
        e = scntxt.getAttributeNames();
        ps.println("    Attribute Names:");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            ps.print("        Attribute[" + i + "]: " + s);
            ps.println(" Type: " + scntxt.getAttribute(s));
        }

        ps.println("    ServletContext.getRealPath(\".\"): " + scntxt.getRealPath("."));
        ps.println("    ServletContext.getMajorVersion(): " + scntxt.getMajorVersion());
//        ps.println("ServletContext.getMimeType():     " + scntxt.getMimeType());
        ps.println("    ServletContext.getMinorVersion(): " + scntxt.getMinorVersion());
//        ps.println("ServletContext.getRealPath(): " + sc.getRealPath());


        ps.println(".............................");
        ps.println("Servlet Config:");
        ps.println("");


        i = 0;
        e = scnfg.getInitParameterNames();
        ps.println("    InitParameters:");
        while (e.hasMoreElements()) {
            String p = (String) e.nextElement();
            ps.print("        InitParameter[" + i + "]: " + p);
            ps.println(" Value: " + scnfg.getInitParameter(p));
            i++;
        }
        ps.println("");
        ps.println("######################## END PROBE ###############################");
        ps.println("");


    }
    /***************************************************************************/

}
