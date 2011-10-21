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

import org.jdom.Element;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.IOException;

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
    private HttpServlet servlet;
    private boolean initialized;

    public SpecialRequestDispatchHandler() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        servlet = null;
        initialized = false;

    }


    public void init(HttpServlet s, Element config) throws Exception {

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

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
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

            }
 //@todo add these commented out responses to the as yet to be implemented admin interface
/*
            else if ( // System Properties Response?
                //Debug.isSet("SystemProperties") &&

                    dataSource.equalsIgnoreCase("/systemproperties")
                    ) {
                specialRequest = true;
                if(sendResponse){
                    Util.sendSystemProperties(request, response);
                    log.info("Sent System Properties");
                }

            }
            else if (  // Status Response?

                    dataSource.equalsIgnoreCase("/status")) {

                specialRequest = true;
                if(sendResponse){
                    doGetStatus(request, response);
                    log.info("Sent Status");
                }

            }
*/


        }

        return specialRequest;

    }


    /**
     * Default handler for OPeNDAP status requests; not publically availableInChunk,
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

        String context = request.getContextPath();
        String helpPage = context+"/jsp/help.jsp";

        response.sendRedirect(helpPage);
        log.debug("Sent redirect to help page: "+helpPage);

    }



}
