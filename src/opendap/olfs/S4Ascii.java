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

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import javax.servlet.http.*;

import opendap.dap.*;
import opendap.dap.parser.ParseException;
import opendap.util.*;
import opendap.servers.ascii.asciiFactory;
import opendap.servers.ascii.toASCII;
import opendap.ppt.PPTException;


/**
 * Default handler for OPeNDAP ascii requests. This class is used
 * by OLFS. This code exists as a seperate class in order to alleviate
 * code bloat in the OLFS class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class S4Ascii {

    private static final boolean _Debug = true;


    /**
     * ************************************************************************
     * Default handler for OPeNDAP ascii requests. Returns OPeNDAP data in
     * comma delimited ascii columns for ingestion into some not so
     * OPeNDAP enabled application such as MS-Excel. Accepts constraint expressions
     * in exactly the same way as the regular OPeNDAP dataserver.
     */
    public static void sendASCII(HttpServletRequest request,
                                 HttpServletResponse response,
                                 ReqState rs) throws PPTException, DODSException, DDSException, ParseException, IOException {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending OPeNDAP ASCII Data For: " + rs.getDataSet() +
                    "    CE: '" + request.getQueryString() + "'");


        String requestURL, ce;
        DConnect url;
        DataDDS dds;

        if (request.getQueryString() == null) {
            ce = "";
        } else {
            ce = "?" + request.getQueryString();
        }

        int suffixIndex = request.getRequestURL().toString().lastIndexOf(".");

        requestURL = request.getRequestURL().substring(0, suffixIndex);

        if (Debug.isSet("showResponse")) {
            System.out.println("New Request URL Resource: '" + requestURL + "'");
            System.out.println("New Request Constraint Expression: '" + ce + "'");
        }

        ServerVersion sv = new ServerVersion(rs.getXDODSServer());

        if (Debug.isSet("asciiResponse")) {
            System.out.println("    Major Server Version: " + sv.getMajor());
            System.out.println("    Minor Server Version: " + sv.getMinor());
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BesAPI.getDODS(rs.getDataSet(), rs.getConstraintExpression(), os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        dds = new DataDDS(sv, new asciiFactory());
        dds.parse(new HeaderInputStream(is));
        dds.readData(is, null);


        if (Debug.isSet("asciiResponse")) System.out.println(" ASCII DDS: ");
        if (Debug.isSet("asciiResponse")) dds.print(System.out);

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        PrintWriter pwDebug = new PrintWriter(System.out);

        //pw.println("<pre>");
        dds.print(pw);
        pw.println("---------------------------------------------");


        String s = "";
        Enumeration e = dds.getVariables();

        while (e.hasMoreElements()) {
            BaseType bt = (BaseType) e.nextElement();
            if (Debug.isSet("asciiResponse")) ((toASCII) bt).toASCII(pwDebug, true, null, true);
            //bt.toASCII(pw,addName,getNAme(),true);
            ((toASCII) bt).toASCII(pw, true, null, true);
        }

        //pw.println("</pre>");
        pw.flush();
        if (Debug.isSet("asciiResponse")) pwDebug.flush();


        if (Debug.isSet("asciiResponse")) System.out.println(" dodsASCII done");
    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for OPeNDAP ascii requests. Returns OPeNDAP data in
     * comma delimited ascii columns for ingestion into some not so
     * OPeNDAP enabled application such as MS-Excel. Accepts constraint expressions
     * in exactly the same way as the regular OPeNDAP dataserver.
     */
    public static void sendASCII_old(HttpServletRequest request,
                                     HttpServletResponse response,
                                     ReqState rs) {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending OPeNDAP ASCII Data For: " + rs.getDataSet() +
                    "    CE: '" + request.getQueryString() + "'");


        String requestURL, ce;
        DConnect url;
        DataDDS dds;

        if (request.getQueryString() == null) {
            ce = "";
        } else {
            ce = "?" + request.getQueryString();
        }

        int suffixIndex = request.getRequestURL().toString().lastIndexOf(".");

        requestURL = request.getRequestURL().substring(0, suffixIndex);

        if (Debug.isSet("showResponse")) {
            System.out.println("New Request URL Resource: '" + requestURL + "'");
            System.out.println("New Request Constraint Expression: '" + ce + "'");
        }

        try {

            if (Debug.isSet("asciiResponse")) System.out.println("Making connection to .dods service...");
            url = new DConnect(requestURL, true);

            if (Debug.isSet("asciiResponse")) System.out.println("Requesting data...");
            dds = url.getData(ce, null, new asciiFactory());

            if (Debug.isSet("asciiResponse")) System.out.println(" ASC DDS: ");
            if (Debug.isSet("asciiResponse")) dds.print(System.out);

            PrintWriter pw = new PrintWriter(response.getOutputStream());
            PrintWriter pwDebug = new PrintWriter(System.out);

            //pw.println("<pre>");
            dds.print(pw);
            pw.println("---------------------------------------------");


            String s = "";
            Enumeration e = dds.getVariables();

            while (e.hasMoreElements()) {
                BaseType bt = (BaseType) e.nextElement();
                if (Debug.isSet("asciiResponse")) ((toASCII) bt).toASCII(pwDebug, true, null, true);
                //bt.toASCII(pw,addName,getNAme(),true);
                ((toASCII) bt).toASCII(pw, true, null, true);
            }

            //pw.println("</pre>");
            pw.flush();
            if (Debug.isSet("asciiResponse")) pwDebug.flush();

        }
        catch (FileNotFoundException fnfe) {
            System.out.println("OUCH! FileNotFoundException: " + fnfe.getMessage());
            fnfe.printStackTrace(System.out);
        }
        catch (MalformedURLException mue) {
            System.out.println("OUCH! MalformedURLException: " + mue.getMessage());
            mue.printStackTrace(System.out);
        }
        catch (IOException ioe) {
            System.out.println("OUCH! IOException: " + ioe.getMessage());
            ioe.printStackTrace(System.out);
        }
        catch (Throwable t) {
            System.out.println("OUCH! Throwable: " + t.getMessage());
            t.printStackTrace(System.out);
        }

        if (Debug.isSet("asciiResponse")) System.out.println(" dodsASCII done");
    }
    /***************************************************************************/


}





