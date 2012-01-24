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

package opendap.bes;

import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.ReqInfo;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.io.PrintStream;

/**
 * User: ndp
 * Date: Apr 19, 2007
 * Time: 12:23:49 PM
 */
public class VersionDispatchHandler implements DispatchHandler {


    private org.slf4j.Logger log;
    private boolean initialized;

    private BesApi _besApi;

    public VersionDispatchHandler() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        initialized = false;

    }

    public void init(HttpServlet s, Element config) throws Exception {

        if (initialized) return;


        _besApi = new BesApi();

        initialized = true;

        log.info("Initialized.");
    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return versionDispatch(request, null, false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        versionDispatch(request, response, true);

    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }


    public void destroy() {
        log.info("Destroy complete.");

    }


    public boolean versionDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {
        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource =  ReqInfo.getBesDataSourceID(relativeUrl);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        boolean versionRequest = false;

        if (dataSource != null) {
            if (        // Version Response?
                    dataSource.equalsIgnoreCase("/version")  ||
                    dataSource.equalsIgnoreCase("/version/") ||
                    (requestSuffix!=null &&
                    requestSuffix.equalsIgnoreCase("ver"))
                    ) {
                versionRequest = true;
                if (sendResponse) {
                    sendVersion(request, response);
                    log.debug("Sent Version Response");
                }
            }


        }
        return versionRequest;
    }


    /**
     * ************************************************************************
     * Default handler for the client's version request.
     * <p/>
     * <p>Returns a plain text document with server version and OPeNDAP core
     * version #'s
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void sendVersion
            (HttpServletRequest
                    request,
             HttpServletResponse
                     response)
            throws Exception {

        log.debug("sendVersion()");

        response.setContentType("text/xml");
        response.setHeader("Content-Description", "dods_version");

        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);

        PrintStream ps = new PrintStream(response.getOutputStream());

        Document vdoc = _besApi.getCombinedVersionDocument();

        if (vdoc == null) {
            throw new ServletException("Internal Error: Version Document not initialized.");
        }
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        //XMLOutputter xout = new XMLOutputter();
        xout.output(vdoc, ps);
        ps.flush();

/*
        if (Debug.isSet("showResponse")) {
            xout.output(getVersionDocument(), System.out);
            System.out.println("Document Sent.");
            System.out.println("\nMIME Headers:");
            System.out.println("    XDODS-Server: " + getXDODSServerVersion());
            System.out.println("    XOPeNDAP-Server: " + getXOPeNDAPServerVersion());
            System.out.println("    XDAP: " + getXDAPVersion(request));
            System.out.println("\nEnd Response.");
        }

*/


    }


}
