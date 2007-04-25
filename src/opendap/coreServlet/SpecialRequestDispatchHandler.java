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

import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Performs dispatching for "special" server requests.
 * <ui>
 * <li> help - returns the help page for Hyrax  </li>
 * <li> systemproperties - returns an html document describing the state of the "system" </li>
 * <li> debug -   </li>
 * <li> status -    </li>
 * </ui>
 *
 */
public class SpecialRequestDispatchHandler implements DispatchHandler {

    private org.slf4j.Logger log;
    private DispatchServlet servlet;
    private boolean initialized;

    public SpecialRequestDispatchHandler() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        servlet = null;
        initialized = false;

    }


    public void init(DispatchServlet s, Element config) throws Exception {

        if(initialized) return;


        servlet = s;

        initialized = true;

        log.info("Initialized.");
    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return specialRequestDispatch(request,null,false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        specialRequestDispatch(request,response,true);

    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }





    public void destroy() {
        log.info("Destroy complete.");

    }












    private boolean specialRequestDispatch(HttpServletRequest request,
                                          HttpServletResponse response,
                                          boolean sendResponse)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        boolean specialRequest = false;

        if (dataSource != null) {

            if ( // Help Response?
                    dataSource.equalsIgnoreCase("/help") ||
                            dataSource.equalsIgnoreCase("/help/") ||
                            ((requestSuffix != null) &&
                                    requestSuffix.equalsIgnoreCase("help"))
                    ) {
                specialRequest = true;
                if(sendResponse){
                    sendHelpPage(request, response);
                    log.info("Sent Help Page");
                }

            } else if ( // System Properties Response?
                //Debug.isSet("SystemProperties") &&

                    dataSource.equalsIgnoreCase("/systemproperties")
                    ) {
                specialRequest = true;
                if(sendResponse){
                    Util.sendSystemProperties(request, response);
                    log.info("Sent System Properties");
                }

            } else if (    // Debug response?
                    Debug.isSet("DebugInterface") &&
                            dataSource.equals("/debug") &&
                            (requestSuffix != null) &&
                            requestSuffix.equals("")) {

                specialRequest = true;
                if(sendResponse){
                    DebugHandler.doDebug(servlet, request, response);
                    log.info("Sent Debug Response");
                }

            } else if (  // Status Response?

                    dataSource.equalsIgnoreCase("/status")) {

                specialRequest = true;
                if(sendResponse){
                    doGetStatus(request, response);
                    log.info("Sent Status");
                }

            }
        }

        return specialRequest;

    }


    /**
     * Default handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @throws java.io.IOException If unablde to right to the response.
     */
    public void doGetStatus(HttpServletRequest request,
                            HttpServletResponse response)
            throws Exception {


        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println("<title>Server Status</title>");
        pw.println("<body><ul>");
        printStatus(pw);
        pw.println("</ul></body>");
        pw.flush();

    }

    // to be overridden by servers that implement status report
    protected void printStatus(PrintWriter os) throws IOException {
        os.println("<h2>Status not implemented.</h2>");
    }


    public void sendHelpPage(HttpServletRequest request,
                             HttpServletResponse response)
            throws Exception {


        log.debug("sendHelpPage()");

        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_help");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");
        response.setStatus(HttpServletResponse.SC_OK);


        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(response.getOutputStream()));

        pw.println("<h3>OPeNDAP Server Help</h3>");
        pw.println("To access most of the features of this OPeNDAP server, append");
        pw.println("one of the following a eight suffixes to a URL: .das, .dds, .dods, .ddx, .info,");
        pw.println(".ver or .help. Using these suffixes, you can ask this server for:");
        pw.println("<dl>");
        pw.println("<dt> das  </dt> <dd> Dataset Attribute Structure (DAS)</dd>");
        pw.println("<dt> dds  </dt> <dd> Dataset Descriptor Structure (DDS)</dd>");
        pw.println("<dt> dods </dt> <dd> DataDDS object (A constrained DDS populated with data)</dd>");
        pw.println("<dt> ddx  </dt> <dd> XML version of the DDS/DAS</dd>");
        pw.println("<dt> info </dt> <dd> info object (attributes, types and other information)</dd>");
        pw.println("<dt> html </dt> <dd> html form for this dataset</dd>");
        pw.println("<dt> ver  </dt> <dd> return the version number of the server</dd>");
        pw.println("<dt> help </dt> <dd> help information (this text)</dd>");
        pw.println("</dl>");
        pw.println("For example, to request the DAS object from the FNOC1 dataset at URI/GSO (a");
        pw.println("experiments dataset) you would appand `.das' to the URL:");
        pw.println("http://opendap.gso.uri.edu/cgi-bin/nph-nc/data/fnoc1.nc.das.");

        pw.println("<p><b>Note</b>: Many OPeNDAP clients supply these extensions for you so you don't");
        pw.println("need to append them (for example when using interfaces supplied by us or");
        pw.println("software re-linked with a OPeNDAP client-library). Generally, you only need to");
        pw.println("add these if you are typing a URL directly into a WWW browser.");
        pw.println("<p><b>Note</b>: If you would like version information for this server but");
        pw.println("don't know a specific data file or data set name, use `/version' for the");
        pw.println("filename. For example: http://opendap.gso.uri.edu/cgi-bin/nph-nc/version will");
        pw.println("return the version number for the netCDF server used in the first example. ");

        pw.println("<p><b>Suggestion</b>: If you're typing this URL into a WWW browser and");
        pw.println("would like information about the dataset, use the `.info' extension.");

        pw.println("<p>If you'd like to see a data values, use the `.html' extension and submit a");
        pw.println("query using the customized form.");


        pw.flush();


    }



}
