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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
        msg += "    fullSourceName:            '" + ReqInfo.getLocalUrl(req) + "'\n";
        msg += "    dataSource:                '" + ReqInfo.getBesDataSourceID(ReqInfo.getLocalUrl(req)) + "'\n";
        msg += "    dataSetName:               '" + ReqInfo.getDataSetName(req) + "'\n";
        msg += "    requestSuffixRegex:             '" + ReqInfo.getRequestSuffix(req) + "'\n";
        msg += "    CE:                        '" + ReqInfo.getConstraintExpression(req) + "'\n";
        msg += "    CE:                        '" + ReqInfo.getConstraintExpression(req) + "'\n";
        msg += "\n";
        msg += "    getPathInfo():             '" + req.getPathInfo()+"'\n";
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


        StringBuilder probeMsg = new StringBuilder();
        probeMsg.append("####################### PROBE ##################################\n");

        probeMsg.append("\n");
        probeMsg.append("The HttpServletRequest object is actually an instance of:").append("\n");
        probeMsg.append("    ").append(request.getClass().getName()).append("\n");
        probeMsg.append("\n");
        probeMsg.append("HttpServletRequest Interface: \n");
        probeMsg.append("    getAuthType:           ").append( request.getAuthType()).append( "\n");
        probeMsg.append("    getContextPath:        ").append( request.getContextPath()).append( "\n");
        probeMsg.append("    getMethod:             ").append( request.getMethod()).append( "\n");
        probeMsg.append("    getPathInfo:           ").append( request.getPathInfo()).append( "\n");
        probeMsg.append("    getPathTranslated:     ").append( request.getPathTranslated()).append( "\n");
        probeMsg.append("    getRequestURL:         ").append( request.getRequestURL()).append( "\n");
        probeMsg.append("    getQueryString:        ").append( request.getQueryString()).append( "\n");
        probeMsg.append("    getRemoteUser:         ").append( request.getRemoteUser()).append( "\n");
        probeMsg.append("    getRequestedSessionId: ").append( request.getRequestedSessionId()).append( "\n");
        probeMsg.append("    getRequestURI:         ").append( request.getRequestURI()).append( "\n");
        probeMsg.append("    getServletPath:        ").append( request.getServletPath()).append( "\n");
        probeMsg.append("    isRequestedSessionIdFromCookie: ").append( request.isRequestedSessionIdFromCookie()).append( "\n");
        probeMsg.append("    isRequestedSessionIdValid:      ").append( request.isRequestedSessionIdValid()).append( "\n");
        probeMsg.append("    isRequestedSessionIdFromURL:    ").append( request.isRequestedSessionIdFromURL()).append( "\n");
        probeMsg.append("\n");
        probeMsg.append("    Cookies:\n");

        Cookie c[] = request.getCookies();
        if (c == null)
            probeMsg.append("   None.").append("\n");
        else {
            probeMsg.append("\n");
            for (i = 0; i < c.length; i++)
                probeMsg.append("        cookie[").append(i).append("]: ").append(c[i]).append("\n");
        }

        probeMsg.append("\n");
        i = 0;
        e = request.getHeaderNames();
        probeMsg.append("    Header Names:").append("\n");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg.append("       Header[").append(i).append("]: ").append(s);
            probeMsg.append(": ").append(request.getHeader(s)).append("\n");
        }

        probeMsg.append("\n");
        probeMsg.append("ServletRequest Interface:").append("\n");
        probeMsg.append("    getCharacterEncoding:  ").append(request.getCharacterEncoding()).append("\n");
        probeMsg.append("    getContentType:        ").append(request.getContentType()).append("\n");
        probeMsg.append("    getContentLength:      ").append(request.getContentLength()).append("\n");
        probeMsg.append("    getProtocol:           ").append(request.getProtocol()).append("\n");
        probeMsg.append("    getScheme:             ").append(request.getScheme()).append("\n");
        probeMsg.append("    getServerName:         ").append(request.getServerName()).append("\n");
        probeMsg.append("    getServerPort:         ").append(request.getServerPort()).append("\n");
        probeMsg.append("    getRemoteAddr:         ").append(request.getRemoteAddr()).append("\n");
        probeMsg.append("    getRemoteHost:         ").append(request.getRemoteHost()).append("\n");
        //probeMsg.append("    getRealPath:           "+request.getRealPath()).append("\n");


        probeMsg.append(".............................").append("\n");
        probeMsg.append("\n");
        i = 0;
        e = request.getAttributeNames();
        probeMsg.append("    Attribute Names:").append("\n");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg.append("        Attribute[").append(i).append("]: ").append(s);
            probeMsg.append(" Value: ").append(request.getAttribute(s)).append("\n");
        }

        probeMsg.append(".............................").append("\n");
        probeMsg.append("\n");
        i = 0;
        e = request.getParameterNames();
        probeMsg.append("    Parameter Names:").append("\n");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg.append("        Parameter[").append(i).append("]: ").append(s);
            probeMsg.append(" Value: ").append(request.getParameter(s)).append("\n");
        }

        probeMsg.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -").append("\n");
        probeMsg.append(" . . . . . . . . . Servlet Infomation API  . . . . . . . . . . . . . .").append("\n");
        probeMsg.append("\n");



        if(servlet == null){
            probeMsg.append("Supplied reference to HttpServlet was null.\nNothing additional to report.").append("\n");

        }
        else {
            probeMsg.append("HttpServlet:").append("\n");
            probeMsg.append("    getServletName(): ").append(servlet.getServletName()).append("\n");
            probeMsg.append("    getServletInfo(): ").append(servlet.getServletInfo()).append("\n");
            probeMsg.append("\n");
            probeMsg.append("\n");

            ServletContext scntxt = servlet.getServletContext();
            ServletConfig scnfg = servlet.getServletConfig();


            probeMsg.append("Servlet Context:").append("\n");

            probeMsg.append("    getMajorVersion:       ").append(scntxt.getMajorVersion()).append("\n");
            probeMsg.append("    getMinorVersion:       ").append(scntxt.getMinorVersion()).append("\n");
            probeMsg.append("    getServerInfo:         ").append(scntxt.getServerInfo()).append("\n");
            probeMsg.append("    getServletContextName: ").append(scntxt.getServletContextName()).append("\n");
            probeMsg.append("\n");

            i = 0;
            e = scntxt.getAttributeNames();
            probeMsg.append("    Attribute Names:").append("\n");
            while (e.hasMoreElements()) {
                i++;
                String s = (String) e.nextElement();
                probeMsg.append("        Attribute[").append(i).append("]: ").append(s);
                probeMsg.append(" Type: ").append(scntxt.getAttribute(s)).append("\n");
            }

            probeMsg.append("    ServletContext.getRealPath(\".\"): ").append(scntxt.getRealPath(".")).append("\n");
            probeMsg.append("    ServletContext.getMajorVersion(): ").append(scntxt.getMajorVersion()).append("\n");
    //        probeMsg.append("ServletContext.getMimeType():     ").append(scntxt.getMimeType()).append("\n");
            probeMsg.append("    ServletContext.getMinorVersion(): ").append(scntxt.getMinorVersion()).append("\n");
    //        probeMsg.append("ServletContext.getRealPath(): ").append(sc.getRealPath()).append("\n");




            probeMsg.append(".............................").append("\n");



            probeMsg.append("Servlet Config:").append("\n");
            probeMsg.append("\n");


            i = 0;
            e = scnfg.getInitParameterNames();
            probeMsg.append("    InitParameters:").append("\n");
            while (e.hasMoreElements()) {
                String p = (String) e.nextElement();
                probeMsg.append("        InitParameter[").append(i).append("]: ").append(p);
                probeMsg.append(" Value: ").append(scnfg.getInitParameter(p)).append("\n");
                i++;
            }

        }

        probeMsg.append("\n");
        probeMsg.append("######################## END PROBE ###############################").append("\n");
        probeMsg.append("\n");

        return probeMsg.toString();

    }


    public static String urlInfo(URL url){
        String msg = "\n";

        msg += "URL: "+url.toString()+"\n";
        msg += "  protocol:      "+url.getProtocol()+"\n";
        msg += "  host:          "+url.getHost()+"\n";
        msg += "  port:          "+url.getPort()+"\n";
        msg += "  default port:  "+url.getDefaultPort()+"\n";
        msg += "  path:          "+url.getPath()+"\n";
        msg += "  query:         "+url.getQuery()+"\n";
        msg += "  file:          "+url.getFile()+"\n";
        msg += "  ref:           "+url.getRef()+"\n";
        msg += "  user info:     "+url.getUserInfo()+"\n";
        msg += "  hash code:     "+url.hashCode()+"\n";

        try {
            msg += "  URI:           "+url.toURI().toASCIIString()+"\n";
        } catch (URISyntaxException e) {
            msg += "  URI:            error: Could not express the URL as URI because: "+e.getMessage()+"\n";
        }

        return msg;
    }

    public static String uriInfo(URI uri){

        String msg = "\n";


        msg += "URI: "+uri.toString()+"\n";
        msg += "  Authority:              "+uri.getAuthority()+"\n";
        msg += "  Host:                   "+uri.getHost()+"\n";
        msg += "  Port:                   "+uri.getPort()+"\n";
        msg += "  Path:                   "+uri.getPath()+"\n";
        msg += "  Query:                  "+uri.getQuery()+"\n";
        msg += "  hashCode:               "+uri.hashCode()+"\n";
        msg += "  Fragment:               "+uri.getFragment()+"\n";
        msg += "  RawAuthority:           "+uri.getRawAuthority()+"\n";
        msg += "  RawFragment:            "+uri.getRawFragment()+"\n";
        msg += "  RawPath:                "+uri.getRawPath()+"\n";
        msg += "  RawQuery:               "+uri.getRawQuery()+"\n";
        msg += "  RawSchemeSpecificPart:  "+uri.getRawSchemeSpecificPart()+"\n";
        msg += "  RawUSerInfo:            "+uri.getRawUserInfo()+"\n";
        msg += "  Scheme:                 "+uri.getScheme()+"\n";
        msg += "  SchemeSpecificPart:     "+uri.getSchemeSpecificPart()+"\n";
        msg += "  UserInfo:               "+uri.getUserInfo()+"\n";
        msg += "  isAbsoulte:             "+uri.isAbsolute()+"\n";
        msg += "  isOpaque:               "+uri.isOpaque()+"\n";
        msg += "  ASCIIString:            "+uri.toASCIIString()+"\n";

        try {
            msg += "  URL:                    "+uri.toURL()+"\n";
        } catch (Exception e) {
            msg += "  URL:                    uri.toURL() FAILED msg="+e.getMessage();
        }

        return msg;
    }
}
