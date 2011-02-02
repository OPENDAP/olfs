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
 * Some handy servlet sniffing utility methods.
 */
public class Util {

    /**
     * Writes information about the incomming request to a String.
     * @param req The current request
     * @param reqno The request number.
     * @return A string containing infformation about the passed HttpServletRequest req
     */
    public static String showRequest(HttpServletRequest req, long reqno) {

        String msg = "\n-------------------------------------------\n";
        msg += "showRequest():\n";
        msg += "  Request #" + reqno + "\n";
        msg += "  Client:  " + req.getRemoteHost() + "\n";
        msg += "  Request Info:\n";
        msg += "    baseURI:                   '" + ReqInfo.getServiceUrl(req) + "'\n";
        msg += "    fullSourceName:            '" + ReqInfo.getRelativeUrl(req) + "'\n";
        msg += "    dataSource:                '" + ReqInfo.getBesDataSourceID(ReqInfo.getRelativeUrl(req)) + "'\n";
        msg += "    dataSetName:               '" + ReqInfo.getDataSetName(req) + "'\n";
        msg += "    requestSuffix:             '" + ReqInfo.getRequestSuffix(req) + "'\n";
        msg += "    CE:                        '" + ReqInfo.getConstraintExpression(req) + "'\n";
        msg +="\n";
        msg +="ReqInfo:\n";
        msg += ReqInfo.toString(req);
        msg += "-------------------------------------------";

        return msg;

    }



    /**
     * ************************************************************************
     *
     * @param pw  The PrintWriter to which the system properties should be
     * written.
     * @throws IOException When things go poorly.
     */
    public static void printSystemProperties(PrintWriter pw)
            throws Exception {


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

        pw.flush();

    }


    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically availableInChunk,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @param pw       The server's <code> HttpServletResponse</code> response
     *                 object.
     * @throws IOException When things go poorly.
     */
    public static void sendSystemProperties(HttpServletRequest request,
                                            HttpServletResponse response,
                                            PrintWriter pw)
            throws Exception {


        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");
        response.setStatus(HttpServletResponse.SC_OK);

        printSystemProperties(pw);
        pw.println("<h3>Runtime Info:</h3>");
        printMemoryReport(pw);
        pw.println("<hr>");
        pw.println("</body>");
        pw.println("</html>");



    }


    public static void printMemoryReport(PrintWriter pw){

        pw.print(getMemoryReport());

    }

    public static String getMemoryReport(){
        String msg;
        Runtime rt = Runtime.getRuntime();
        msg =  "Memory Usage:\n"    ;
        msg += " JVM Max Memory:   " + (rt.maxMemory() / 1024) / 1000. + " MB (JVM Maximum Allowable Heap)\n";
        msg += " JVM Total Memory: " + (rt.totalMemory() / 1024) / 1000. + " MB (JVM Heap size)\n";
        msg += " JVM Free Memory:  " + (rt.freeMemory() / 1024) / 1000. + " MB (Unused part of heap)\n";
        msg += " JVM Used Memory:  " + ((rt.totalMemory() - rt.freeMemory()) / 1024) / 1000. + " MB (Currently active memory)\n";
        return msg;

    }

    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically availableInChunk,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @param ps       The server's <code> HttpServletResponse</code> response
     *                 object.
     * @throws IOException When things go poorly.
     */
    public static void sendSystemProperties(HttpServletRequest request,
                                     HttpServletResponse response,
                                     PrintStream ps)
            throws Exception {


        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");

        ps.println("<html>");
        ps.println("<title>System Properties</title>");
        ps.println("<hr>");
        ps.println("<body><h2>System Properties</h2>");
        ps.println("<h3>Date: " + new Date() + "</h3>");

        Properties sysp = System.getProperties();
        Enumeration e = sysp.propertyNames();

        ps.println("<ul>");
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();

            String value = System.getProperty(name);

            ps.println("<li>" + name + ": " + value + "</li>");
        }
        ps.println("</ul>");

        ps.println("<h3>Runtime Info:</h3>");

        Runtime rt = Runtime.getRuntime();
        ps.println("JVM Max Memory:   " + (rt.maxMemory() / 1024) / 1000. + " MB (JVM Maximum Allowable Heap)<br>");
        ps.println("JVM Total Memory: " + (rt.totalMemory() / 1024) / 1000. + " MB (JVM Heap size)<br>");
        ps.println("JVM Free Memory:  " + (rt.freeMemory() / 1024) / 1000. + " MB (Unused part of heap)<br>");
        ps.println("JVM Used Memory:  " + ((rt.totalMemory() - rt.freeMemory()) / 1024) / 1000. + " MB (Currently active memory)<br>");

        ps.println("<hr>");
        ps.println("</body>");
        ps.println("</html>");
        ps.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }

    /**
     * ************************************************************************
     * This is a bit of instrumentation that I kept around to let me look at the
     * state of the incoming <code>HttpServletRequest</code> from the client.
     * This method calls the <code>get*</code> methods of the request and prints
     * the results to standard out.
     *
     * @param servlet The Servlet to Probe
     * @param request The <code>HttpServletRequest</code> object to probe.
     * @return A string containing the probe inormation
     */
    public static String probeRequest(HttpServlet servlet, HttpServletRequest request) {

        Enumeration e;
        int i;
        ServletContext scntxt = servlet.getServletContext();
        ServletConfig scnfg = servlet.getServletConfig();


        String probeMsg = "####################### PROBE ##################################\n";
        probeMsg += "\n";
        probeMsg += "HttpServlet:"+ "\n" ;
        probeMsg += "    getServletName(): "+servlet.getServletName() + "\n" ;
        probeMsg += "    getServletInfo(): "+servlet.getServletInfo() + "\n" ;
        probeMsg += "\n";
        probeMsg += "\n";
        probeMsg += "\n";
        probeMsg += "\n";
        probeMsg += "\n";
        probeMsg += "\n";
        probeMsg += "The HttpServletRequest object is actually an instance of:" + "\n" ;
        probeMsg += "    " + request.getClass().getName() + "\n" ;
        probeMsg += "\n";
        probeMsg += "HttpServletRequest Interface:"+ "\n" ;
        probeMsg += "    getAuthType:           " + request.getAuthType() + "\n" ;
        probeMsg += "    getContextPath:        " + request.getContextPath() + "\n" ;
        probeMsg += "    getMethod:             " + request.getMethod() + "\n" ;
        probeMsg += "    getPathInfo:           " + request.getPathInfo() + "\n" ;
        probeMsg += "    getPathTranslated:     " + request.getPathTranslated() + "\n" ;
        probeMsg += "    getRequestURL:         " + request.getRequestURL() + "\n" ;
        probeMsg += "    getQueryString:        " + request.getQueryString() + "\n" ;
        probeMsg += "    getRemoteUser:         " + request.getRemoteUser() + "\n" ;
        probeMsg += "    getRequestedSessionId: " + request.getRequestedSessionId() + "\n" ;
        probeMsg += "    getRequestURI:         " + request.getRequestURI() + "\n" ;
        probeMsg += "    getServletPath:        " + request.getServletPath() + "\n" ;
        probeMsg += "    isRequestedSessionIdFromCookie: " + request.isRequestedSessionIdFromCookie() + "\n" ;
        probeMsg += "    isRequestedSessionIdValid:      " + request.isRequestedSessionIdValid() + "\n" ;
        probeMsg += "    isRequestedSessionIdFromURL:    " + request.isRequestedSessionIdFromURL() + "\n" ;
        //ps.println("    isUserInRole:                   " + request.isUserInRole());

        probeMsg += "\n";
        probeMsg += "    Cookies:" + "\n";
        Cookie c[] = request.getCookies();
        if (c == null)
            probeMsg += "   None." + "\n";
        else {
            probeMsg += "\n";
            for (i = 0; i < c.length; i++)
                probeMsg += "        cookie[" + i + "]: " + c[i] + "\n";
        }

        probeMsg += "\n";
        i = 0;
        e = request.getHeaderNames();
        probeMsg += "    Header Names:"+ "\n" ;
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg += "       Header[" + i + "]: " + s;
            probeMsg += ": " + request.getHeader(s)+ "\n" ;
        }

        probeMsg += "\n";
        probeMsg += "ServletRequest Interface:"+ "\n" ;
        probeMsg += "    getCharacterEncoding:  " + request.getCharacterEncoding()+ "\n" ;
        probeMsg += "    getContentType:        " + request.getContentType()+ "\n" ;
        probeMsg += "    getContentLength:      " + request.getContentLength()+ "\n" ;
        probeMsg += "    getProtocol:           " + request.getProtocol()+ "\n" ;
        probeMsg += "    getScheme:             " + request.getScheme()+ "\n" ;
        probeMsg += "    getServerName:         " + request.getServerName()+ "\n" ;
        probeMsg += "    getServerPort:         " + request.getServerPort()+ "\n" ;
        probeMsg += "    getRemoteAddr:         " + request.getRemoteAddr()+ "\n" ;
        probeMsg += "    getRemoteHost:         " + request.getRemoteHost()+ "\n" ;
        //probeMsg += "    getRealPath:           "+request.getRealPath()+ "\n" ;


        probeMsg += "............................."+ "\n" ;
        probeMsg += "\n";
        i = 0;
        e = request.getAttributeNames();
        probeMsg += "    Attribute Names:"+ "\n" ;
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg += "        Attribute[" + i + "]: " + s;
            probeMsg += " Type: " + request.getAttribute(s)+ "\n" ;
        }

        probeMsg += "............................."+ "\n" ;
        probeMsg += "\n";
        i = 0;
        e = request.getParameterNames();
        probeMsg += "    Parameter Names:"+ "\n" ;
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg += "        Parameter[" + i + "]: " + s;
            probeMsg += " Value: " + request.getParameter(s)+ "\n" ;
        }

        probeMsg += "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -"+ "\n" ;
        probeMsg += " . . . . . . . . . Servlet Infomation API  . . . . . . . . . . . . . ."+ "\n" ;
        probeMsg += "\n";

        probeMsg += "Servlet Context:"+ "\n" ;

        probeMsg += "    getMajorVersion:       " + scntxt.getMajorVersion()+ "\n" ;
        probeMsg += "    getMinorVersion:       " + scntxt.getMinorVersion()+ "\n" ;
        probeMsg += "    getServerInfo:         " + scntxt.getServerInfo()+ "\n" ;
        probeMsg += "    getServletContextName: " + scntxt.getServletContextName()+ "\n" ;
        probeMsg += "\n";

        i = 0;
        e = scntxt.getAttributeNames();
        probeMsg += "    Attribute Names:"+ "\n" ;
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg += "        Attribute[" + i + "]: " + s;
            probeMsg += " Type: " + scntxt.getAttribute(s)+ "\n" ;
        }

        probeMsg += "    ServletContext.getRealPath(\".\"): " + scntxt.getRealPath(".")+ "\n" ;
        probeMsg += "    ServletContext.getMajorVersion(): " + scntxt.getMajorVersion()+ "\n" ;
//        probeMsg += "ServletContext.getMimeType():     " + scntxt.getMimeType()+ "\n" ;
        probeMsg += "    ServletContext.getMinorVersion(): " + scntxt.getMinorVersion()+ "\n" ;
//        probeMsg += "ServletContext.getRealPath(): " + sc.getRealPath()+ "\n" ;


        probeMsg += "............................."+ "\n" ;
        probeMsg += "Servlet Config:"+ "\n" ;
        probeMsg += "\n";


        i = 0;
        e = scnfg.getInitParameterNames();
        probeMsg += "    InitParameters:"+ "\n" ;
        while (e.hasMoreElements()) {
            String p = (String) e.nextElement();
            probeMsg += "        InitParameter[" + i + "]: " + p;
            probeMsg += " Value: " + scnfg.getInitParameter(p)+ "\n" ;
            i++;
        }
        probeMsg += "\n";
        probeMsg += "######################## END PROBE ###############################"+ "\n" ;
        probeMsg += "\n";

        return probeMsg;

    }








}
