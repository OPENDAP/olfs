/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes;

import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.ReqInfo;
import opendap.io.HyraxStringEncoding;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintStream;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Apr 19, 2007
 * Time: 12:23:49 PM
 */
public class VersionDispatchHandler implements DispatchHandler {


    private org.slf4j.Logger log;
    private boolean initialized;

    private BesApi _besApi;


    // Matches the top level server version service name and  the dataset level version response too.
    // For now we're not using it since he havea DAP4 data responder for that.
    // private static String _versionMatchRegexString = "(/version(/)?$)|(\\.ver$)";


    // Matches the top level server version service name
    private static String _versionMatchRegexString = "/version(/)?$";

    private Pattern _requestMatchPattern ;

    public VersionDispatchHandler() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _requestMatchPattern = Pattern.compile(_versionMatchRegexString, Pattern.CASE_INSENSITIVE);

        initialized = false;

    }

    @Override
    public void init(HttpServlet s, Element config) throws Exception {
        init(s, config, new BesApi());
    }

    @Override
    public void init(HttpServlet s, Element config, BesApi besApi) throws Exception {

        if (initialized) return;

        _besApi = besApi;
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
        return new Date().getTime();
    }


    public void destroy() {
        log.info("Destroy complete.");

    }


    public boolean versionDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);

        boolean versionRequest = false;

        if (relativeUrl != null) {



            Matcher m = _requestMatchPattern.matcher(relativeUrl);
            if (m.matches()) {
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

        PrintStream ps = new PrintStream(response.getOutputStream(), false,  HyraxStringEncoding.getCharset().name());

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
            xout.output(getGroupVersionDocument(), System.out);
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
