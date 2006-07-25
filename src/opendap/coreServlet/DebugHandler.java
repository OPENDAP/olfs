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

package opendap.coreServlet;

import opendap.util.Log;
import opendap.util.Debug;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Mar 31, 2006
 * Time: 1:13:40 PM
 * To change this template use File | Settings | File Templates.
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
                               ReqState rs) throws IOException {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_debug");

        PrintStream pw = new PrintStream(response.getOutputStream());
        pw.println("<title>Debugging</title>");
        pw.println("<body><pre>");

        StringTokenizer tz = new StringTokenizer(rs.getConstraintExpression(), "=;");
        while (tz.hasMoreTokens()) {
            String cmd = tz.nextToken();
            pw.println("Cmd= " + cmd);

            if (cmd.equals("help")) {
                pw.println(" help;log;logEnd;logShow");
                pw.println(" showFlags;showInitParameters;showRequest");
                pw.println(" on|off=(flagName)");
                doDebugCmd(cmd, tz, pw); // for subclasses
            } else if (cmd.equals("log")) {
                Log.reset();
                pw.println(" logging started");
            } else if (cmd.equals("logEnd")) {
                Log.close();
                pw.println(" logging ended");
            } else if (cmd.equals("logShow")) {
                pw.println(Log.getContents());
                pw.println("-----done logShow");
            } else if (cmd.equals("on"))
                Debug.set(tz.nextToken(), true);

            else if (cmd.equals("off"))
                Debug.set(tz.nextToken(), false);

            else if (cmd.equals("showFlags")) {
                Iterator iter = Debug.keySet().iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    pw.println("  " + key + " " + Debug.isSet(key));
                }
            } else if (cmd.equals("showInitParameters")) {
                pw.println(rs.toString());
            } else if (cmd.equals("showRequest")) {
                Util.probeRequest(pw, servlet, request, servlet.getServletContext(), servlet.getServletConfig());
            } else if (!doDebugCmd(cmd, tz, pw)) { // for subclasses
                pw.println("  unrecognized command");
            }
        }

        pw.println("--------------------------------------");
        pw.println("Logging is " + (Log.isOn() ? "on" : "off"));
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
