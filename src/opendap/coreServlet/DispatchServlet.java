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

package opendap.coreServlet;


import opendap.auth.AuthenticationControls;
import opendap.bes.BESManager;
import opendap.http.error.NotFound;
import opendap.logging.LogUtil;
import opendap.logging.Procedure;
import opendap.logging.Timer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This servlet provides the dispatching for all OPeNDAP requests.
 * <p/>
 * <p>This server will respond to both HTTP GET and POST requests.
 * activities are handled by ordered collections of DispatchHandlers.
 * <p/>
 * <p/>
 * <p>This server is designed so that the dispatch activities are handled by
 * ordered collections of DispatchHandlers are identified at run time through
 * the olfs.xml configuration file. The olfs.xml file is identified in the
 * servlets web.xml file. The olfs.xml file is typically located in
 * $CATALINE_HOME/content/opendap.
 * <p>
 * <p/>
 * <p>The web.xml file used to configure this servlet must contain servlet parameters identifying
 * the location of the olfs.xml file.</p>
 * <p/>
 * <p/>
 * <p/>
 */
public class DispatchServlet extends HttpServlet {


    /**
     * ************************************************************************
     * A thread safe hit counter
     *
     * @serial
     */
    private static final AtomicInteger reqNumber = new AtomicInteger(0);
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final ReentrantLock INIT_LOCK = new ReentrantLock();

    private static final ArrayList<DispatchHandler> httpGetDispatchHandlers = new ArrayList<>();
    private static final ArrayList<DispatchHandler> httpPostDispatchHandlers = new ArrayList<>();

    private static final Logger log = LoggerFactory.getLogger(DispatchServlet.class);

    protected static Document configDoc;

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
        INIT_LOCK.lock();
        try {
            if (IS_INITIALIZED.get())
                return;

            super.init();
            initDebug();
            LogUtil.initLogging(this);

            // Timer.enable()

            RequestCache.openThreadCache();

            log.debug("BEGIN");

            LogUtil.logServerStartup("init()");
            log.info("init() start.");

            String configFile = getInitParameter("ConfigFileName");
            if (configFile == null) {
                String msg = "Servlet configuration must include a parameter called 'ConfigFileName' whose value" +
                        "is the name of the OLFS configuration file!\n";
                log.error(msg);
                throw new ServletException(msg);
            }

            PersistentConfigurationHandler.installDefaultConfiguration(this, configFile);

            loadConfig(configFile);

            Element config = configDoc.getRootElement();

            Element enableCombinedLog = config.getChild("EnableCombinedLog");
            if(enableCombinedLog!=null){
                LogUtil.useCombinedLog(true);
            }

            boolean enablePost = false;
            Element postConfig = config.getChild("HttpPost");
            if (postConfig != null) {
                String enabled = postConfig.getAttributeValue("enabled");
                if (enabled.equalsIgnoreCase("true"))
                    enablePost = true;
            }


            Element timer = config.getChild("Timer");
            String timerStatus = "DISABLED";
            if (timer != null) {
                String enabled = timer.getAttributeValue("enabled");
                if (enabled != null && enabled.equalsIgnoreCase("true")) {
                    Timer.enable();
                    timerStatus = "ENABLED";
                }
            }
            log.info("init() - Timer is {}", timerStatus);

            initBesManager();

            initAuthenticationControls();

            try {
                loadHyraxServiceHandlers(httpGetDispatchHandlers, httpPostDispatchHandlers, enablePost, config);
            } catch (Exception e) {
                throw new ServletException(e);
            }
            RequestCache.closeThreadCache();
            IS_INITIALIZED.set(true);
            log.info("END");
        } finally {
            INIT_LOCK.unlock();
        }
    }


    private void initAuthenticationControls() {
        Element authControlElem = configDoc.getRootElement().getChild(AuthenticationControls.CONFIG_ELEMENT);
        AuthenticationControls.init(authControlElem, getServletContext().getContextPath());
    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * ConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private void loadConfig(String confFileName) throws ServletException {

        String filename = Scrub.fileName(ServletUtil.getConfigPath(this) + confFileName);
        String errorMsgBase = "OLFS configuration file \"";

        log.debug("Loading Configuration File: {}", filename);
        try {

            File confFile = new File(filename);
            FileInputStream fis = new FileInputStream(confFile);

            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                configDoc = sb.build(fis);
            } finally {
                fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = errorMsgBase + filename + "\" cannot be found.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = errorMsgBase + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = errorMsgBase + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
        }

        log.debug("Configuration loaded and parsed.");

    }


    private void initBesManager() throws ServletException {
        Element besManagerElement = configDoc.getRootElement().getChild(BESManager.BES_MANAGER_CONFIG_ELEMENT);
        if (besManagerElement == null) {
            String msg = "Invalid configuration. Missing required 'BESManager' element. DispatchServlet FAILED to init()!";
            log.error(msg);
            throw new ServletException(msg);
        }
        try {
            BESManager.init(getServletContext(), besManagerElement);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }


    /**
     * <Handler className="opendap.bes.VersionDispatchHandler" />
     * <p>
     * <!-- Bot Blocker
     * - This handler can be used to block access from specific IP addresses
     * - and by a range of IP addresses using a regular expression.
     * -->
     * <!-- <Handler className="opendap.coreServlet.BotBlocker"> -->
     * <!-- <IpAddress>127.0.0.1</IpAddress> -->
     * <!-- This matches all IPv4 addresses, work yours out from here.... -->
     * <!-- <IpMatch>[012]?\d?\d\.[012]?\d?\d\.[012]?\d?\d\.[012]?\d?\d</IpMatch> -->
     * <!-- Any IP starting with 65.55 (MSN bots the don't respect robots.txt  -->
     * <!-- <IpMatch>65\.55\.[012]?\d?\d\.[012]?\d?\d</IpMatch>   -->
     * <!-- </Handler>  -->
     * <Handler className="opendap.ncml.NcmlDatasetDispatcher" />
     * <Handler className="opendap.threddsHandler.StaticCatalogDispatch">
     * <prefix>thredds</prefix>
     * <useMemoryCache>true</useMemoryCache>
     * </Handler>
     * <Handler className="opendap.gateway.DispatchHandler">
     * <prefix>gateway</prefix>
     * <UseDAP2ResourceUrlResponse />
     * </Handler>
     * <Handler className="opendap.bes.BesDapDispatcher" >
     * <!-- AllowDirectDataSourceAccess
     * - If this element is present then the server will allow users to request
     * - the data source (file) directly. For example a user could just get the
     * - underlying NetCDF files located on the server without using the OPeNDAP
     * - request interface.
     * -->
     * <!--
     *     UseDAP2ResourceUrlResponse
     *     When enabled, the server will provide a DAP2-style response
     *     to requests for a dataset resource URL, meaning that the response
     *     will be either source data file or an HTTP 403 Forbidden error, as
     *     defined by the state of the AllowDirectDataSourceAccess feature.
     *
     *     If UseDAP2ResourceUrlResponse is not enabled (not present in the
     *     configuration, or commented out) the server will default to returning
     *     the DAP4 Dataset Services Response (DSR) when a dataset resource URL
     *     is requested.
     *
     *     See Dap4 specification for more:
     *     https://docs.opendap.org/index.php?title=OPULS_Development#DAP4_Specification
     * -->
     * <UseDAP2ResourceUrlResponse />
     *
     * <!--
     *     DataRequestForm
     *
     *     Defines the DAP data model version for the DAta Request Form linked to
     *     from the "blue-bar" catalog.html pages generated from  either the
     *     DDX (for DAP2) or the DMR (for DAP4).
     * -->
     * <DataRequestForm type="dap4" />
     *
     *
     * </Handler>
     * <Handler className="opendap.bes.DirectoryDispatchHandler" />
     * <Handler className="opendap.bes.BESThreddsDispatchHandler"/>
     * <Handler className="opendap.bes.FileDispatchHandler" />
     *
     * @param httpGetHandlers The list of GET handlers for the OLFS to use.
     * @param httpPostHandlers The list of POST handlers for the OLFS to use.
     * @param enablePost If the value is TRU then the POST handling will be enabled.
     * @param config The configuration Element to use when configuring the service.
     * @throws Exception
     */
    private void loadHyraxServiceHandlers(
            List<DispatchHandler> httpGetHandlers,
            List<DispatchHandler> httpPostHandlers,
            boolean enablePost, Element config) throws Exception {

        if (config == null)
            throw new ServletException("Bad configuration! The configuration element was NULL");

        Element botBlocker = config.getChild("BotBlocker");
        Element noDynamicNavigation = config.getChild("NoDynamicNavigation");

        httpGetHandlers.add(new opendap.bes.VersionDispatchHandler());
        if (botBlocker != null)
            httpGetHandlers.add(new opendap.coreServlet.BotBlocker());
        httpGetHandlers.add(new opendap.ncml.NcmlDatasetDispatcher());
        httpGetHandlers.add(new opendap.threddsHandler.StaticCatalogDispatch());
        httpGetHandlers.add(new opendap.gateway.DispatchHandler());
        httpGetHandlers.add(new opendap.ngap.NgapDispatchHandler());
        httpGetHandlers.add(new opendap.bes.BesDapDispatcher());

        if (enablePost) {
            // The DAP dispatch handler does POST
            httpPostHandlers.add( new opendap.bes.BesDapDispatcher());
            // And the NGAP dispatch handler does POST
            httpPostHandlers.add(new opendap.ngap.NgapDispatchHandler());
        }

        if(noDynamicNavigation!=null) {
            log.info("Dynamic Site Navigation Has Been Disabled. " +
                    "Only user supplied static THREDDS catalogs will be available" +
                    "for content navigation and discovery.");
        }
        else {
            // Load the dynamic catalog reponse handlers.
            httpGetHandlers.add(new opendap.bes.DirectoryDispatchHandler());
            httpGetHandlers.add(new opendap.bes.BESThreddsDispatchHandler());
        }

        // httpGetHandlers.add(new opendap.bes.FileDispatchHandler());

        for (DispatchHandler dh : httpGetHandlers) {
            dh.init(this, config);
        }
        for (DispatchHandler dh : httpPostHandlers) {
            dh.init(this, config);
        }
    }


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
            log.info("** DebugOn **");
            StringTokenizer toker = new StringTokenizer(debugOn);
            while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
        }

    }

    /**
     * Temporary implementation HEAD denial response. Because in most cases
     * the server has to do a lot of work to get a Content-Length value.
     *
     * The default implementation of doHead() appears to replace the
     * ServletOutputStream in the response with a a stream that counts bytes
     * but does not transmot them. Maybe this makes sense for an file service,
     * but not here. We could refine this by adding DispatchHandler.doHead()
     * and having each handler do something that makes sense.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        String relativeUrl = ReqInfo.getLocalUrl(request);

        if(relativeUrl.equalsIgnoreCase("/")) {
            try {
                super.doHead(request, response);
            }
            catch(ServletException se){
                log.error("ERROR - Failed to produce HEAD response for {}",relativeUrl);
            }
        }
        else {
            String msg = "HEAD is not allowed in this area.";
            response.setHeader("Disposition", msg);
            response.setHeader("Allow", "GET, POST");
            try {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
            }
            catch(IOException ioe){
                log.error("ERROR - To send METHOD NOT ALLOWED error to client for {}",relativeUrl);
            }
        }
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
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {


        String relativeUrl = ReqInfo.getLocalUrl(request);

        int request_status = HttpServletResponse.SC_OK;

        try {
            Procedure timedProcedure = Timer.start();

            RequestCache.openThreadCache();

            try {

                if (LicenseManager.isExpired(request)) {
                    LicenseManager.sendLicenseExpiredPage(request, response);
                    return;
                }

                int reqno = reqNumber.incrementAndGet();
                LogUtil.logServerAccessStart(request, LogUtil.HYRAX_ACCESS_LOG_ID, "HTTP-GET", Long.toString(reqno));

                if (redirectForServiceOnlyRequest(request, response))
                    return;

                if (log.isDebugEnabled()) {
                    log.debug(Util.getMemoryReport());
                    log.debug(ServletUtil.showRequest(request, reqno));
                    log.debug(ServletUtil.probeRequest(this, request));
                    String msg = "Requested relative URL: '" + relativeUrl +
                            "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                            "' CE: '" + ReqInfo.getConstraintExpression(request) + "'";
                    log.debug(msg);
                }

                if (Debug.isSet("probeRequest"))
                    log.debug(ServletUtil.probeRequest(this, request));


                DispatchHandler dh = getDispatchHandler(request, httpGetDispatchHandlers);
                if (dh != null) {
                    log.debug("Request being handled by: {}", dh.getClass().getName());
                    dh.handleRequest(request, response);

                } else {
                    request_status = OPeNDAPException.anyExceptionHandler(new NotFound("Failed to locate resource: " + relativeUrl), this, response);
                }
            } finally {
                Timer.stop(timedProcedure);
            }
        } catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this, response);
            } catch (Throwable t2) {
                try {
                    log.error("\n########################################################\n" +
                            "Request processing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error log attempt for this request.\n" +
                            "########################################################\n", t2);
                } catch (Throwable t3) {
                    // It's boned now.. Leave it be.
                }
            }
        } finally {
            LogUtil.logServerAccessEnd(request_status, LogUtil.HYRAX_ACCESS_LOG_ID);
            RequestCache.closeThreadCache();
            log.info("Response completed.\n");
        }

        log.info("Timing Report: \n{}", Timer.report());
        Timer.reset();
    }
    //**************************************************************************


    private boolean redirectForServiceOnlyRequest(HttpServletRequest req,
                                                  HttpServletResponse res)
            throws IOException {

        if (ReqInfo.isServiceOnlyRequest(req)) {
            String reqURI = req.getRequestURI();
            String newURI = reqURI + "/";
            res.sendRedirect(Scrub.urlContent(newURI));
            log.debug("Sent redirectForServiceOnlyRequest to map the servlet " +
                    "context to a URL that ends in a '/' character!");
            return true;
        }
        return false;
    }


    /**
     * @param request  .
     * @param response .
     */
    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) {

        String relativeUrl = ReqInfo.getLocalUrl(request);

        int httpStatus = HttpServletResponse.SC_OK;

        try {
            try {

                RequestCache.openThreadCache();

                int reqno = reqNumber.incrementAndGet();

                LogUtil.logServerAccessStart(request, LogUtil.HYRAX_ACCESS_LOG_ID, "HTTP-POST", Long.toString(reqno));

                if (log.isDebugEnabled()) {
                    log.debug(ServletUtil.showRequest(request, reqno));
                    String msg = "Requested relative URL: '" + relativeUrl +
                            "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                            "' CE: '" + ReqInfo.getConstraintExpression(request) + "'";
                    log.debug(msg);
                }
                if (Debug.isSet("probeRequest"))
                    log.debug(ServletUtil.probeRequest(this, request));


                DispatchHandler dh = getDispatchHandler(request, httpPostDispatchHandlers);
                if (dh != null) {
                    log.debug("Request being handled by: {}", dh.getClass().getName());
                    dh.handleRequest(request, response);

                } else {
                    httpStatus = OPeNDAPException.anyExceptionHandler(new NotFound("Failed to locate resource: " + relativeUrl), this, response);
                }

            } finally {
                log.info("doPost(): Response completed.\n");
            }

        } catch (Throwable t) {
            try {
                httpStatus = OPeNDAPException.anyExceptionHandler(t, this, response);
            } catch (Throwable t2) {
                try {
                    log.error("BAD THINGS HAPPENED!", t2);
                } catch (Throwable t3) {
                    // It's boned now.. Leave it be.
                }
            }
        } finally {
            LogUtil.logServerAccessEnd(httpStatus, LogUtil.HYRAX_ACCESS_LOG_ID);
            RequestCache.closeThreadCache();
        }
    }


    /**
     * Returns the first handler in the vector of DispatchHandlers that claims
     * be able to handle the incoming request.
     *
     * @param request The request we are looking to handle
     * @param dhvec   A Vector of DispatchHandlers that will be asked if they can
     *                handle the request.
     * @return The IsoDispatchHandler that can handle the request, null if no
     * handler claims the request.
     * @throws Exception For bad behaviour.
     */
    private DispatchHandler getDispatchHandler(HttpServletRequest request, List<DispatchHandler> dhvec) throws Exception {
        for (DispatchHandler dh : dhvec) {
            log.debug("Checking handler: {}", dh.getClass().getName());
            if (dh.requestCanBeHandled(request)) {
                return dh;
            }
        }
        return null;
    }


    /**
     * Gets the last modified date of the requested resource. Because the data handler is really
     * the only entity capable of determining the last modified date the job is passed  through to it.
     *
     * @param req The current request
     * @return Returns the time the HttpServletRequest object was last modified, in milliseconds
     * since midnight January 1, 1970 GMT
     */
    @Override
    protected long getLastModified(HttpServletRequest req) {

        RequestCache.openThreadCache();

        long reqno = reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, LogUtil.HYRAX_LAST_MODIFIED_ACCESS_LOG_ID, "LastModified", Long.toString(reqno));

        long lmt = new Date().getTime();

        Procedure timedProcedure = Timer.start();
        try {
            if (ReqInfo.isServiceOnlyRequest(req)) {
                return lmt;
            }
            if (!LicenseManager.isExpired(req) && !ReqInfo.isServiceOnlyRequest(req)) {

                DispatchHandler dh = getDispatchHandler(req, httpGetDispatchHandlers);
                if (dh != null) {
                    log.debug("getLastModified() -  Request being handled by: {}", dh.getClass().getName());
                    lmt = dh.getLastModified(req);
                }
            }
        } catch (Exception e) {
            log.error("Caught: {}  Message: {} ", e.getClass().getName(), e.getMessage());
            lmt = new Date().getTime();
        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, LogUtil.HYRAX_LAST_MODIFIED_ACCESS_LOG_ID);
            Timer.stop(timedProcedure);
        }
        return lmt;
    }


    @Override
    public void destroy() {

        LogUtil.logServerShutdown("destroy()");

        for (DispatchHandler dh : httpGetDispatchHandlers) {
            log.debug("Shutting down handler: {}", dh.getClass().getName());
            dh.destroy();
        }
        BESManager.destroy();
        super.destroy();
    }


}
