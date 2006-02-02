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

import opendap.dap.DODSException;
import opendap.dap.parser.ParseException;
import opendap.ppt.PPTException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Default handler for OPeNDAP directory requests. This class is used
 * by OLFS. This code exists as a seperate class in order to alleviate
 * code bloat in the OLFS class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

public class S4Dir {

    private static final boolean _Debug = false;
    private static String separator = "/";


    /**
     * ************************************************************************
     * Default handler for OPeNDAP directory requests. Returns an html document
     * with a list of all datasets on this server with links to their
     * DDS, DAS, Information, and HTML responses.
     *
     * @param request  The <code>HttpServletRequest</code> from the client.
     * @param response The <code>HttpServletResponse</code> for the client.
     * @param rs       The request state object for this client request.
     * @see ReqState
     */
    public static void sendDIR(HttpServletRequest request,
                               HttpServletResponse response,
                               ReqState rs)
            throws DODSException, PPTException, JDOMException, BESException {

        if (_Debug) System.out.println("sendDIR request = " + request);

        //String ddxCacheDir = rs.getDDXCache();
        //String ddsCacheDir = rs.getDDSCache();

        try {

            PrintWriter pw = new PrintWriter(response.getOutputStream());

            String thisServer = request.getRequestURL().toString();
            printHTMLHeader(thisServer,pw);


            Element dataset = BesAPI.showCatalog(rs.contentPath).getRootElement();

            pw.println(dataset);



            printHTMLFooter(pw);
            pw.flush();

        } catch (FileNotFoundException fnfe) {
            System.out.println("OUCH! FileNotFoundException: " + fnfe.getMessage());
            fnfe.printStackTrace(System.out);
        } catch (IOException ioe) {
            System.out.println("OUCH! IOException: " + ioe.getMessage());
            ioe.printStackTrace(System.out);
        }


    }

    private static void printHTMLHeader(String thisServer, PrintWriter pw){
        pw.println("<html>");
        pw.println("<head>");
        pw.println("<title>OPeNDAP Directory</title>");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html\">");
        pw.println("</head>");

        pw.println("<body bgcolor=\"#FFFFFF\">");


        pw.println("<h1>Server4 Directory for:</h1>");
        pw.println("<h2>" + thisServer + "</h2>");
        pw.println("<hr>");
        pw.println("<h2>Directory Service Not yet implmented.</h2>");

    }
    private static void printHTMLFooter( PrintWriter pw){
        pw.println("<hr>");
        pw.println("</html>");

    }

}





