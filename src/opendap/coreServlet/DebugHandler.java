/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
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
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Enumeration;

/**
 *
 * Handles debuging interface for the servlet.
 *
 */
public class DebugHandler {


    /**
     * ************************************************************************
     * Default handler for debug requests;
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response object.
     */
    public static void doDebug(HttpServlet servlet, HttpServletRequest request,
                               HttpServletResponse response,
                               OpendapHttpDispatchHandler odh,
                               ServletConfig sc) throws Exception {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", odh.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_debug");

        PrintStream pw = new PrintStream(response.getOutputStream());
        pw.println("<title>Debugging</title>");
        pw.println("<body><pre>");

        StringTokenizer tz = new StringTokenizer(ReqInfo.getConstraintExpression(request), "=;");
        while (tz.hasMoreTokens()) {
            String cmd = tz.nextToken();
            pw.println("Cmd= " + cmd);

            if (cmd.equals("help")) {
                pw.println(" help;log;logEnd;logShow");
                pw.println(" showFlags;showInitParameters;showRequest");
                pw.println(" on|off=(flagName)");
                doDebugCmd(cmd, tz, pw); // for subclasses
            } else if (cmd.equals("log")) {
                DebugLog.reset();
                pw.println(" logging started");
            } else if (cmd.equals("logEnd")) {
                DebugLog.close();
                pw.println(" logging ended");
            } else if (cmd.equals("logShow")) {
                pw.println(DebugLog.getContents());
                pw.println("-----done logShow");
            } else if (cmd.equals("on"))
                Debug.set(tz.nextToken(), true);

            else if (cmd.equals("off"))
                Debug.set(tz.nextToken(), false);

            else if (cmd.equals("showFlags")) {
                for (Object o : Debug.keySet()) {
                    String key = (String) o;
                    pw.println("  " + key + " " + Debug.isSet(key));
                }
            } else if (cmd.equals("showInitParameters")) {


                String ts = "  InitParameters:\n";
                Enumeration e = sc.getInitParameterNames();
                while (e.hasMoreElements()) {
                    String name = (String) e.nextElement();
                    String value = sc.getInitParameter(name);

                    ts += "    " + name + ": '" + value + "'\n";
                }

                pw.println(ts);


            } else if (cmd.equals("showRequest")) {
                Util.probeRequest(pw, servlet, request, servlet.getServletContext(), servlet.getServletConfig());
            } else if (!doDebugCmd(cmd, tz, pw)) { // for subclasses
                pw.println("  unrecognized command");
            }
        }

        pw.println("--------------------------------------");
        pw.println("Logging is " + (DebugLog.isOn() ? "on" : "off"));
        Iterator iter = Debug.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            boolean val = Debug.isSet(key);
            if (val)
                pw.println("  " + key + " " + Debug.isSet(key));
        }

        pw.println("</pre></body>");
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }

    protected static boolean doDebugCmd(String cmd, StringTokenizer tz, PrintStream pw) {
        return false;
    }

}
