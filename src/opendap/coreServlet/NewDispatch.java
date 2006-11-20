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

import thredds.servlet.DataRootHandler;
import thredds.servlet.ServletUtil;
import thredds.servlet.HtmlWriter;
import thredds.catalog.InvDatasetScan;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

/**
 * User: ndp
 * Date: Nov 15, 2006
 * Time: 3:30:36 PM
 */
public class NewDispatch extends HttpServlet {

    private static boolean forceTHREDDSCatalog = false;

    /**
     * ************************************************************************
     * Used for thread syncronization.
     *
     * @serial
     */
    private static final Object syncLock = new Object();


    /**
     * ************************************************************************
     * Count "hits" on the server...
     *
     * @serial
     */
    private int HitCounter = 0;


    private OpendapHttpDispatchHandler odh = null;
    private OpendapSoapDispatchHandler sdh = null;


    protected org.slf4j.Logger log;
    //protected String _contentPath; // Path to ${tomcat_home}/content/<context>
    //protected String _contextPath; // Path to ${tomcat_home}/webapps/<context>


    protected DataRootHandler dataRootHandler;


    protected String getDocsPath() {
        return "docs/";
    }


    /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from any of the dods code...
     *
     * @throws javax.servlet.ServletException
     */
    public void init() throws ServletException {

        super.init();
        initDebug();

        PersistentContentHandler.installInitialContent(this);


        String className = getInitParameter("OpendapHttpDispatchHandlerImplementation");
        if (className == null)
            throw new ServletException("Missing servlet parameter \"OpendapHttpDispatchHandlerImplementation\"." +
                    "A class that implements the opendap.coreServlet.OpendapHttpDispatchHandler interface must" +
                    "be identified in this (missing) servlet parameter.");


        System.out.println("\n\nOpendapHttpDispatchHandlerImplementation: " + className);

        try {
            Class classDefinition = Class.forName(className);
            odh = (OpendapHttpDispatchHandler) classDefinition.newInstance();
        } catch (InstantiationException e) {
            throw new ServletException("Cannot instantiate class: " + className, e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Cannot access class: " + className, e);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Cannot find class: " + className, e);
        }


        odh.init(this);


        className = getInitParameter("OpendapSoapDispatchHandlerImplementation");
        if (className == null)
            throw new ServletException("Missing servlet parameter \"OpendapSoapDispatchHandlerImplementation\"." +
                    "A class that implements the opendap.coreServlet.OpendapSoapDispatchHandler interface must" +
                    "be identified in this (missing) servlet parameter.");

        System.out.println("\n\nOpendapSoapDispatchHandlerImplementation: " + className);

        try {
            Class classDefinition = Class.forName(className);
            sdh = (OpendapSoapDispatchHandler) classDefinition.newInstance();
        } catch (InstantiationException e) {
            throw new ServletException("Cannot instantiate class: " + className, e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Cannot access class: " + className, e);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Cannot find class: " + className, e);
        }


        sdh.init(this);


        initTHREDDS(ServletUtil.getContextPath(this), ServletUtil.getContentPath(this));


    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * <p/>
     * Process the DebugOn initParameter and turn on the requested debugging
     * states in the Debug object.
     */
    private void initDebug() {
        // Turn on debugging.
        String debugOn = getInitParameter("DebugOn");
        if (debugOn != null) {
            System.out.println("** DebugOn **");
            StringTokenizer toker = new StringTokenizer(debugOn);
            while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
        }

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * <p/>
     * Initialize the THREDDS environment so that THREDDS works correctly.
     *
     * @param contextPath The context path for this servlet.
     * @param contentPath The path to the peristemnt configuration content for this servlet.
     */
    private void initTHREDDS(String contextPath, String contentPath) {

        thredds.servlet.ServletUtil.initDebugging(this); // read debug flags

        // init logging
        thredds.servlet.ServletUtil.initLogging(this);
        thredds.servlet.ServletUtil.logServerSetup(this.getClass().getName() + ".init()");
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        InvDatasetScan.setContext(contextPath); // This gets your context path from web.xml above.

        // This allows you to specify which servlet handles catalog requests.
        // We set it to "/catalog". Is "/ts" the servlet path for you? If so, set this to "/ts".
        // If you use the default servlet for everything (path mapping of "/*" in web.xml). set it to the empty string.
        InvDatasetScan.setCatalogServletName("/" + getServletName());

        // handles all catalogs, including ones with DatasetScan elements, ie dynamic
        DataRootHandler.init(contentPath, contextPath);
        dataRootHandler = DataRootHandler.getInstance();
        try {
            dataRootHandler.initCatalog("catalog.xml");
            //dataRootHandler.initCatalog( "extraCatalog.xml" );
        }
        catch (Throwable e) {
            log.error("Error initializing catalog: " + e.getMessage(), e);
        }

        //this.makeDebugActions();
        //dataRootHandler.makeDebugActions();
        //DatasetHandler.makeDebugActions();

        HtmlWriter.init(contextPath,
                this.getServletContext().getServletContextName(),
                odh.getVersionStringForTHREDDSCatalog(),
                this.getDocsPath(),
                "", // userCssPath
                "docs/images/cog.gif", // contextLogoPath
                "docs/images/logo.gif"  // instituteLogoPath
        );

        log.info("--- initialized " + getClass().getName());

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * <p/>
     * In this (default) implementation of the getServerName() method we just get
     * the name of the servlet and pass it back. If something different is
     * required, override this method when implementing the writeDDS() and
     * getXDODSServerVersion() methods.
     * <p/>
     * This is typically used by the getINFO() method to figure out if there is
     * information specific to this server residing in the info directory that
     * needs to be returned to the client as part of the .info response.
     *
     * @return A string containing the name of the servlet class that is running.
     */
    public String getServerName() {

        // Ascertain the name of this server.
        String servletName = this.getClass().getName();

        return (servletName);
    }


    protected long getLastModified(HttpServletRequest req) {
        return odh.getLastModified(req);
    }


    public void showRequest(HttpServletRequest req, long reqno) {
        System.out.println("-------------------------------------------");
        System.out.println("Server: " + getServerName() + "   Request #" + reqno);
        System.out.println("Client: " + req.getRemoteHost());
        System.out.println("Request Info:");
        System.out.println("  fullSourceName:               '" + ReqInfo.getFullSourceName(req) + "'");
        System.out.println("  dataSource:                   '" + ReqInfo.getDataSource(req) + "'");
        System.out.println("  dataSetName:                  '" + ReqInfo.getDataSetName(req) + "'");
        System.out.println("  collectionName:               '" + ReqInfo.getCollectionName(req) + "'");
        System.out.println("  requestSuffix:                '" + ReqInfo.getRequestSuffix(req) + "'");
        System.out.println("  CE:                           '" + ReqInfo.getConstraintExpression(req) + "'");
        System.out.println("  requestURL:                   '" + ReqInfo.getRequestURL(req) + "'");
        System.out.println("  requestForOpendapContents:     " + ReqInfo.requestForOpendapContents(req));
        System.out.println("  requestForTHREDDSCatalog:      " + ReqInfo.requestForTHREDDSCatalog(req));
        System.out.println();

        DebugLog.println("Request dataSource: '" + ReqInfo.getDataSource(req) +
                "' suffix: '" + ReqInfo.getRequestSuffix(req) +
                "' CE: '" + ReqInfo.getConstraintExpression(req) + "'");

    }


    public boolean specialRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String dataSetName = ReqInfo.getDataSetName(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        boolean specialRequest = false;

        if (dataSource != null) {

            if (        // Version Response?
                    dataSource.equalsIgnoreCase("/version")
                    ) {
                odh.sendVersion(request, response);
                log.info("Sent Version Response");
                specialRequest = true;

            } else if ( // Help Response?
                    dataSource.equalsIgnoreCase("/help") ||
                            dataSource.equalsIgnoreCase("/help/") ||
                            ((requestSuffix != null) &&
                                    requestSuffix.equalsIgnoreCase("help"))
                    ) {
                odh.sendHelpPage(request, response);
                log.info("Sent Help Page");
                specialRequest = true;

            } else if ( // System Properties Response?
                //Debug.isSet("SystemProperties") &&

                    dataSource.equalsIgnoreCase("/systemproperties")
                    ) {
                Util.sendSystemProperties(request, response, odh);
                log.info("Sent System Properties");
                specialRequest = true;

            } else if (    // Debug response?
                    Debug.isSet("DebugInterface") &&
                            dataSource.equals("/debug") &&
                            (requestSuffix != null) &&
                            requestSuffix.equals("")) {

                DebugHandler.doDebug(this, request, response, odh, this.getServletConfig());
                log.info("Sent Debug Response");
                specialRequest = true;

            } else if (  // Status Response?

                    dataSource.equalsIgnoreCase("/status")
                    ) {
                doGetStatus(request, response);
                log.info("Sent Status");
                specialRequest = true;

            } else if (dataSetName != null) {


                if (   //  Directory response?
                        dataSetName.equalsIgnoreCase("contents") &&
                                requestSuffix != null &&
                                requestSuffix.equalsIgnoreCase("html")) {


                    odh.sendDir(request, response);

                    log.info("Sent Contents (aka OPeNDAP directory).");

                    specialRequest = true;

                } else if ( //  THREDDS Catalog ?
                        dataSetName.equalsIgnoreCase("catalog") && requestSuffix != null &&
                                (requestSuffix.equalsIgnoreCase("html") || requestSuffix.equalsIgnoreCase("xml"))
                        ) {

                    if (getThreddsCatalog(request, response)) {
                        log.info("Sent Catalog");
                        specialRequest = true;
                    }
                }
            }
        }

        return specialRequest;

    }


    /**
     * ***********************************************************************
     * Handles incoming requests from clients. Parses the request and determines
     * what kind of OPeNDAP response the cleint is requesting. If the request is
     * understood, then the appropriate handler method is called, otherwise
     * an error is returned to the client.
     * <p/>
     * This method is the entry point for <code>OLFS</code>. It uses
     * the methods <code>processOpendapURL</code> to extract the OPeNDAP URL
     * information from the incoming client request. This OPeNDAP URL information
     * is cached and made accessible through get and set methods.
     * <p/>
     * After  <code>processOpendapURL</code> is called <code>loadIniFile()</code>
     * is called to load configuration information from a .ini file,
     * <p/>
     * If the standard behaviour of the servlet (extracting the OPeNDAP URL
     * information from the client request, or loading the .ini file) then
     * you should overload <code>processOpendapURL</code> and <code>loadIniFile()
     * </code>. <b> We don't recommend overloading <code>doGet()</code> beacuse
     * the logic contained there may change in our core and cause your server
     * to behave unpredictably when future releases are installed.</b>
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        // response.setHeader("Last-Modified", (new Date()).toString() );


        try {
            if (Debug.isSet("probeRequest"))
                Util.probeRequest(System.out, this, request, getServletContext(), getServletConfig());


            synchronized (syncLock) {
                long reqno = HitCounter++;

                if (Debug.isSet("showRequest")) {
                    showRequest(request, reqno);
                }

            } // synch


            String dataSource = ReqInfo.getDataSource(request);
            //String dataSetName = ReqInfo.getDataSetName(request);
            String requestSuffix = ReqInfo.getRequestSuffix(request);
            String fullSourceName = ReqInfo.getFullSourceName(request);

            if (!specialRequest(request, response)) {


                DataSourceInfo dsi = odh.getDataSourceInfo(fullSourceName);

                System.out.println(dsi);

                if (dsi.sourceExists()) {
                    if (dsi.isCollection()) {
                        if (odh.useOpendapDirectoryView()) {
                            odh.sendDir(request, response);
                        } else {
                            getThreddsCatalog(request, response);
                        }

                    } else {

                        if(!dsi.isDataset()  ||  odh.allowDirectDataSourceAccess()){
                            odh.sendFile(request, response);
                        }
                        else {
                            sendDirectAccessDenied(request,response);
                        }
                    }
                } else {


                    dsi = odh.getDataSourceInfo(dataSource);
                    System.out.println(dsi);

                    if (dsi.sourceExists()) {

                        if (dsi.isDataset()) {

                            if ( // DDS Response?
                                    requestSuffix.equalsIgnoreCase("dds")
                                    ) {
                                odh.sendDDS(request, response);
                                log.info("Sent DDS");

                            } else if ( // DAS Response?
                                    requestSuffix.equalsIgnoreCase("das")
                                    ) {
                                odh.sendDAS(request, response);
                                log.info("Sent DAS");

                            } else if (  // DDX Response?
                                    requestSuffix.equalsIgnoreCase("ddx")
                                    ) {
                                odh.sendDDX(request, response);
                                log.info("Sent DDX");

                            } else if ( // Blob Response?
                                    requestSuffix.equalsIgnoreCase("blob")
                                    ) {
                                //doGetBLOB(request, response, rs);
                                badURL(request, response);
                                log.info("Sent BAD URL Response because they asked for a Blob. Bad User!");

                            } else if ( // DataDDS (aka .dods) Response?
                                    requestSuffix.equalsIgnoreCase("dods")
                                    ) {
                                odh.sendDODS(request, response);
                                log.info("Sent DAP2 Data");

                            } else if (  // ASCII Data Response.
                                    requestSuffix.equalsIgnoreCase("asc") ||
                                            requestSuffix.equalsIgnoreCase("ascii")
                                    ) {
                                odh.sendASCII(request, response);
                                log.info("Sent ASCII");

                            } else if (  // Info Response?
                                    requestSuffix.equalsIgnoreCase("info")
                                    ) {
                                odh.sendInfo(request, response);
                                log.info("Sent Info");

                            } else if (  //HTML Request Form (aka The Interface From Hell) Response?
                                    requestSuffix.equalsIgnoreCase("html") ||
                                            requestSuffix.equalsIgnoreCase("htm")
                                    ) {
                                odh.sendHTMLRequestForm(request, response);
                                log.info("Sent HTML Request Form");


                            } else if (requestSuffix.equals("")) {
                                badURL(request, response);
                                log.info("Sent BAD URL (missing Suffix)");

                            } else {
                                badURL(request, response);
                                log.info("Sent BAD URL - nothing left to check.");
                            }

                        } else {
                            badURL(request, response);
                            log.info("Sent BAD URL - nothing left to check.");
                        }
                    } else {
                        sendResourceNotFound(request, response);

                    }

                }
            }

        } catch (Throwable e) {
            OPeNDAPException.anyExceptionHandler(e, response);
        }


    }
    //**************************************************************************


    /**
     * This helper function makes sure that an empty request path dosn't bone the THREDDS code
     * for no good reason. IN other words the top level catalog gets returned even when the URL doesn't
     * end in a "/"
     *
     * @param req Client Request
     * @param res Server Response
     * @return True if a THREDDS catalog.xml or catalog.html was returned to the client
     * @throws IOException
     * @throws ServletException
     */
    private boolean getThreddsCatalog(HttpServletRequest req, HttpServletResponse res)
            throws Exception, ServletException {


        if ((req.getPathInfo() == null)) {
            String newPath = req.getRequestURL() + "/";
            res.sendRedirect(newPath);
            log.info("Sent THREDDS redirect to avoid a null valued return to request.getPathInfo().");
            return true;
        }


        boolean wasTHREDDS = dataRootHandler.processReqForCatalog(req, res);

        if (forceTHREDDSCatalog && !wasTHREDDS) {
            if (ReqInfo.getDataSetName(req).equalsIgnoreCase("catalog") &&
                    ReqInfo.getRequestSuffix(req).equalsIgnoreCase("xml")) {

                odh.sendCatalog(req, res);
                wasTHREDDS = true;
            }
        }


        return wasTHREDDS;

    }


    /**
     * Default handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void doGetStatus(HttpServletRequest request,
                            HttpServletResponse response)
            throws IOException, ServletException {


        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println("<title>Server Status</title>");
        pw.println("<body><ul>");
        printStatus(pw);
        pw.println("</ul></body>");
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }


    // to be overridden by servers that implement status report
    protected void printStatus(PrintWriter os) throws IOException {
        os.println("<h2>Number of Requests Received = " + HitCounter + "</h2>");
    }


    /**
     * Sends an html document to the client explaining that they have used a
     * poorly formed URL and then the help page...
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void badURL(HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending Bad URL Page.");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setHeader("Content-Description", "BadURL");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));


        pw.println("<h3>Error in URL</h3>");
        pw.println("<p>The URL extension did not match any that are known by this");
        pw.println("server. Here is a list of the five extensions that are be recognized by");
        pw.println("all OPeNDAP servers:</p>");
        pw.println("<ui>");
        pw.println("    <li>ddx</li>");
        pw.println("    <li>dds</li>");
        pw.println("    <li>das</li>");
        pw.println("    <li>dods</li>");
        pw.println("    <li>info</li>");
        pw.println("    <li>html</li>");
        pw.println("    <li>ascii</li>");
        pw.println("</ui>");
        pw.println("<p>If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a></p>");

        pw.flush();

        response.setStatus(HttpServletResponse.SC_OK);


    }
    /***************************************************************************/


    /**
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {


        SOAPRequestDispatcher.doPost(request, response, odh, sdh);
    }


    public void destroy() {

        odh.destroy();
        super.destroy();
    }


    private void sendResourceNotFound(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setHeader("Content-Description", "BadURL");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        String topLevel = request.getRequestURL().substring(0,request.getRequestURL().lastIndexOf(request.getPathInfo()));

        pw.println("<h2>Resource Not Found</h2>");
        pw.println("<p>The URL <i>'"+request.getRequestURL()+"'</i> does not describe a resource that can be found on this server.</p>");
        pw.println("<p>If you would like to start at the top level of this server, go here:</p>");
        pw.println("<p><a href='"+topLevel+"'>"+topLevel+"</a></p>");
        pw.println("<p>If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a></p>");

        pw.flush();

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);


    }


    private void sendDirectAccessDenied(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setHeader("Content-Description", "BadURL");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        String topLevel = request.getRequestURL().substring(0,request.getRequestURL().lastIndexOf(request.getPathInfo()));

        pw.println("<h2>ACCESS DENIED</h2>");
        pw.println("<p>The URL <i>'"+request.getRequestURL()+"'</i> references a data source directly. </p>" +
                "<p>You must use the OPeNDAP request interface to get data from the data source.</p>");



        pw.println("<p>If you would like to start at the top level of this server, go here:</p>");
        pw.println("<p><a href='"+topLevel+"'>"+topLevel+"</a></p>");
        pw.println("<p>If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a></p>");

        pw.flush();

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);


    }


}
