/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.olfs;

import opendap.dap.parser.ParseException;
import opendap.dap.DODSException;
import opendap.util.Debug;
import opendap.util.Log;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Date;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 17, 2005
 * Time: 11:42:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class Util {


    private static boolean track = false;

    // debug
    // private ArrayList prArr = null;

    private class RequestDebug {
        long reqno;
        String threadDesc;
        boolean done = false;

        RequestDebug(long reqno, String threadDesc) {
            this.reqno = reqno;
            this.threadDesc = threadDesc;
        }
    }


    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see S4Html
     */
    public static void doGetSystemProps(HttpServletRequest request,
                                        HttpServletResponse response,
                                        ReqState rs)
            throws IOException {


        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
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

    public static void probeRequest(PrintStream ps, HttpServletRequest request, ServletContext scntxt, ServletConfig scnfg) {

        Enumeration e;
        int i;


        ps.println("####################### PROBE ##################################");
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
        if(c==null)
            ps.println("   None.");
        else{
            ps.println();
            for(i=0; i<c.length ;i++)
                ps.println("        cookie["+i+"]: " + c[i]);
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

        ps.println("    getMajorVersion:       "+scntxt.getMajorVersion());
        ps.println("    getMinorVersion:       "+scntxt.getMinorVersion());
        ps.println("    getServerInfo:         "+scntxt.getServerInfo());
        ps.println("    getServletContextName: "+scntxt.getServletContextName());
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


    /**
     * ************************************************************************
     * Turns a ParseException into a DODS error and sends it to the client.
     *
     * @param pe       The <code>ParseException</code> that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public static void parseExceptionHandler(ParseException pe, HttpServletResponse response) {

        if (Debug.isSet("showException")) {
            System.out.println(pe);
            pe.printStackTrace();
            Log.printThrowable(pe);
        }

        System.out.println(pe);
        pe.printStackTrace();

        try {
            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");

            // Strip any double quotes out of the parser error message.
            // These get stuck in auto-magically by the javacc generated parser
            // code and they break our error parser (bummer!)
            String msg = pe.getMessage().replace('\"', '\'');

            DODSException de2 = new DODSException(DODSException.CANNOT_READ_FILE, msg);
            de2.print(eOut);
        } catch (IOException ioe) {
            System.out.println("Cannot respond to client! IO Error: " + ioe.getMessage());
        }

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Sends a DODS error to the client.
     *
     * @param de       The DODS exception that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public static void dodsExceptionHandler(DODSException de, HttpServletResponse response) {

        if (Debug.isSet("showException")) {
            de.print(System.out);
            de.printStackTrace();
            Log.printDODSException(de);
        }

        try {
            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());

            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");

            de.print(eOut);

        } catch (IOException ioe) {
            System.out.println("Cannot respond to client! IO Error: " + ioe.getMessage());
            Log.println("Cannot respond to client! IO Error: " + ioe.getMessage());
        }


    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Sends an error to the client.
     *
     * @param e        The exception that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public static void IOExceptionHandler(IOException e, HttpServletResponse response, ReqState rs) {

        try {
            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");

            // Strip any double quotes out of the parser error message.
            // These get stuck in auto-magically by the javacc generated parser
            // code and they break our error parser (bummer!)
            String msg = e.getMessage();
            if (msg != null)
                msg = msg.replace('\"', '\'');

            DODSException de2 = new DODSException(DODSException.CANNOT_READ_FILE, msg);
            de2.print(eOut);

            if (Debug.isSet("showException")) {
                // Error message
                System.out.println("DODServlet ERROR (IOExceptionHandler): " + e);
                System.out.println(rs);
                if (track) {
                    RequestDebug reqD = (RequestDebug) rs.getUserObject();
                    System.out.println("  request number: " + reqD.reqno + " thread: " + reqD.threadDesc);
                }
                e.printStackTrace();
                Log.printThrowable(e);
            }

        } catch (IOException ioe) {
            System.out.println("Cannot respond to client! IO Error: " + ioe.getMessage());
        }

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Sends an error to the client.
     *
     * @param e        The exception that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public static void anyExceptionHandler(Throwable e, HttpServletResponse response, ReqState rs) {

        try {
            System.out.println("DODServlet ERROR (anyExceptionHandler): " + e);
            System.out.println(rs);
            if (track) {
                RequestDebug reqD = (RequestDebug) rs.getUserObject();
                System.out.println("  request number: " + reqD.reqno + " thread: " + reqD.threadDesc);
            }
            e.printStackTrace();
            Log.printThrowable(e);

            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");

            // Strip any double quotes out of the parser error message.
            // These get stuck in auto-magically by the javacc generated parser
            // code and they break our error parser (bummer!)
            String msg = e.getMessage();
            if (msg != null)
                msg = msg.replace('\"', '\'');

            DODSException de2 = new DODSException(DODSException.UNDEFINED_ERROR, msg);
            de2.print(eOut);

        } catch (IOException ioe) {
            System.out.println("Cannot respond to client! IO Error: " + ioe.getMessage());
        }


    }
    /***************************************************************************/

}
