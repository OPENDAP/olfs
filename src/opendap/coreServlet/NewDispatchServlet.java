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


import opendap.bes.BESManager;
import opendap.auth.AuthenticationControls;
import opendap.bes.dap2Responders.BesApi;
import opendap.logging.LogUtil;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

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
 *
 * <p/>
 * <p>The web.xml file used to configure this servlet must contain servlet parameters identifying
 * the location of the olfs.xml file.</p>
 * <p/>
 * <p/>
 * <p/>
 */
public class NewDispatchServlet extends HttpServlet {


    /**
     * ************************************************************************
     * A thread safe hit counter
     *
     * @serial
     */
    private AtomicInteger _reqNumber;



    private HyraxService _defaultHyraxService;
    private Vector<HyraxService> _hyraxServices;

    private org.slf4j.Logger _log;

    private Document _configDoc;

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
        initLogging();

        // Timer.enable();


        RequestCache.openThreadCache();

        _reqNumber = new AtomicInteger(0);
        _hyraxServices =  new Vector<>();

        _log.debug("init() start");

        // init logging
        LogUtil.logServerStartup("init()");
        _log.info("init() start.");

        String configFile = getInitParameter("ConfigFileName");
        if (configFile == null) {
            String msg = "Servlet configuration must include a parameter called 'ConfigFileName' whose value" +
                    "is the name of the OLFS configuration file!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }

        PersistentConfigurationHandler.installDefaultConfiguration(this,configFile);

        loadConfig(configFile);

        Element config = _configDoc.getRootElement();



        Element timer = config.getChild("Timer");
        if(timer!=null){
            String enabled = timer.getAttributeValue("enabled");
            if(enabled!=null && enabled.equalsIgnoreCase("true")){
                Timer.enable();
            }
        }

        _log.info("init() - Timer is {}",Timer.isEnabled()?"ENABLED":"DISABLED");
        initBesManager();

        initAuthenticationControls();

        try {
            _defaultHyraxService = new HyraxService("default");
            _defaultHyraxService.init(this,config);

            List<Element> catalogs = (List<Element>) config.getChildren("catalog");
            for(Element catalog : catalogs){
                String title = catalog.getAttributeValue("title");
                String prefix = catalog.getAttributeValue("prefix");
                String besApiClassName = catalog.getAttributeValue("besApi");
                BesApi besApi = besApiFactory(besApiClassName);
                if(besApi != null) {
                    HyraxService hyraxService = new HyraxService(title);
                    hyraxService.init(this, config, prefix, besApi);
                    _hyraxServices.add(hyraxService);
                }
            }
        }
        catch (Exception e){
            throw new ServletException(e);
        }
        _log.info("init() complete.");
        RequestCache.closeThreadCache();
    }

    private BesApi besApiFactory(String className) throws ServletException {
        String msg;
        if (className != null) {
            try {

                _log.debug("Building BesApi: " + className);
                Class classDefinition = Class.forName(className);
                BesApi besApi = (BesApi) classDefinition.newInstance();
                return besApi;

            } catch (ClassNotFoundException e) {
                msg = "Cannot find class: " + className;
                _log.error(msg);
                throw new ServletException(msg, e);
            } catch (InstantiationException e) {
                msg = "Cannot instantiate class: " + className;
                _log.error(msg);
                throw new ServletException(msg, e);
            } catch (IllegalAccessException e) {
                msg = "Cannot access class: " + className;
                _log.error(msg);
                throw new ServletException(msg, e);
            } catch (ClassCastException e) {
                msg = "Cannot cast class: " + className + " to opendap.coreServlet.DispatchHandler";
                _log.error(msg);
                throw new ServletException(msg, e);
            } catch (Exception e) {
                msg = "Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
                _log.error(msg);
                throw new ServletException(msg, e);

            }

        } else {
            _log.error("besApiFactory() - FAILED to locate the required 'className' attribute in Handler element. SKIPPING.");
        }
        return null;
    }



        private void initAuthenticationControls()  {
        Element authControlElem = _configDoc.getRootElement().getChild(AuthenticationControls.CONFIG_ELEMENT);
        AuthenticationControls.init(authControlElem,getServletContext().getContextPath());
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

        _log.debug("Loading Configuration File: " + filename);
        try {

            File confFile = new File(filename);
            FileInputStream fis = new FileInputStream(confFile);

            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                _configDoc = sb.build(fis);
            }
            finally {
                fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "OLFS configuration file \"" + filename + "\" cannot be found.";
            _log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "OLFS configuration file \"" + filename + "\" is not readable.";
            _log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "OLFS configuration file \"" + filename + "\" cannot be parsed.";
            _log.error(msg);
            throw new ServletException(msg, e);
        }

        _log.debug("Configuration loaded and parsed.");

    }


    private void initBesManager() throws ServletException {
        Element besManagerElement = _configDoc.getRootElement().getChild("BESManager");
        if(besManagerElement ==  null){
            String msg = "Invalid configuration. Missing required 'BESManager' element. DispatchServlet FAILED to init()!";
            _log.error(msg);
            throw new ServletException(msg);

        }
        BESManager besManager  = new BESManager();
        try {
            besManager.init(besManagerElement);
        }
        catch(Exception e){
            throw new ServletException(e);
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
            System.out.println("** DebugOn **");
            StringTokenizer toker = new StringTokenizer(debugOn);
            while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
        }

    }


    /**
     * Starts the logging process.
     */
    private void initLogging() {
        LogUtil.initLogging(this);
        _log = org.slf4j.LoggerFactory.getLogger(getClass());

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

                if(LicenseManager.isExpired(request)){
                    LicenseManager.sendLicenseExpiredPage(request,response);
                    return;
                }

                int reqno = _reqNumber.incrementAndGet();
                LogUtil.logServerAccessStart(request, "HyraxAccess", "HTTP-GET", Long.toString(reqno));

                _log.debug(Util.getMemoryReport());

                _log.debug(ServletUtil.showRequest(request, reqno));
                _log.debug(ServletUtil.probeRequest(this, request));


                if(redirectForServiceOnlyRequest(request,response))
                    return;


                _log.debug("Requested relative URL: '" + relativeUrl +
                        "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                        "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");

                if (Debug.isSet("probeRequest"))
                    _log.debug(ServletUtil.probeRequest(this, request));

                HyraxService hyraxService = null;
                for(HyraxService service: _hyraxServices){
                    if(hyraxService==null && service.requestCanBeHandled(request)){
                        hyraxService = service;
                    }
                }
                if(hyraxService==null)
                    hyraxService = _defaultHyraxService;

                _log.debug("doGet() - Using HyraxService '{}'",hyraxService._title);
                hyraxService.handleRequest(request,response);

            }
            finally {
                Timer.stop(timedProcedure);
            }


        }
        catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this, response);
            }
            catch(Throwable t2) {
                try {
                    _log.error("\n########################################################\n" +
                            "Request processing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error _log attempt for this request.\n" +
                            "########################################################\n", t2);
                }
                catch(Throwable t3){
                    // It's boned now.. Leave it be.
                }
            }
        }
        finally {
            LogUtil.logServerAccessEnd(request_status, "HyraxAccess");
            RequestCache.closeThreadCache();
            _log.info("doGet(): Response completed.\n");
        }

        _log.info("doGet() - Timing Report: \n{}", Timer.report());
        Timer.reset();
    }
    //**************************************************************************



    /*
    private void send404(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        // Build a regex to use to see if they are looking for a DAP2 response:
        StringBuilder dap2Regex = new StringBuilder(".*.(");
        dap2Regex.append("dds");
        dap2Regex.append("|das");
        dap2Regex.append("|dods");
        dap2Regex.append("|asc(ii)?");
        dap2Regex.append(")");
        Pattern dap2Pattern = Pattern.compile(dap2Regex.toString(),Pattern.CASE_INSENSITIVE);


        // Build a regex to use to see if they are looking for a DAP3/4 response:
        StringBuilder dap4Regex = new StringBuilder(".*.(");
        dap4Regex.append("ddx");
        dap4Regex.append("|dmr");
        dap4Regex.append("|dap");
        dap4Regex.append("|ddx");
        dap4Regex.append("|rdf");
        dap4Regex.append(")");
        Pattern dap4Pattern = Pattern.compile(dap4Regex.toString(),Pattern.CASE_INSENSITIVE);


        String requestURL = req.getRequestURL().toString();

        if(dap2Pattern.matcher(requestURL).matches()){   // Is it a DAP2 request?
            resp.setHeader("XDODS-Server", "dods/3.2");
            resp.setHeader("XOPeNDAP-Server", "Server-Version-Unknown");
            resp.setHeader("XDAP", "3.2");
            resp.setHeader("Content-Description", "dods_error");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getOutputStream().print(
                    OPeNDAPException.getDap2Error(HttpServletResponse.SC_NOT_FOUND,
                            "Cannot locate resource: " + Scrub.completeURL(requestURL)));
        }
        else if (dap4Pattern.matcher(requestURL).matches()){  // Is it a DAP3/4 request?
            resp.setHeader("XDODS-Server", "dods/3.2");
            resp.setHeader("XOPeNDAP-Server", "Server-Version-Unknown");
            resp.setHeader("XDAP", "3.2");
            resp.setHeader("Content-Description", "dods_error");
            Document err = OPeNDAPException.getDAP32Error(
                    HttpServletResponse.SC_NOT_FOUND,
                    "Cannot locate resource: "+Scrub.completeURL(requestURL));

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            xmlo.output(err, resp.getOutputStream());

        }
        else { // Otherwise just send a web page.
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }




        _log.info("Sent Resource Not Found (404) - nothing left to check.");
        LogUtil.logServerAccessEnd(HttpServletResponse.SC_NOT_FOUND, -1, "HyraxAccess");



    }
    */



    private boolean redirectForServiceOnlyRequest(HttpServletRequest req,
                                                  HttpServletResponse res)
            throws IOException {


        // _log.debug(ServletUtil.probeRequest(this, req));

        if (ReqInfo.isServiceOnlyRequest(req)) {
            String reqURI = req.getRequestURI();
            String newURI = reqURI+"/";
            res.sendRedirect(Scrub.urlContent(newURI));
            _log.debug("Sent redirectForServiceOnlyRequest to map the servlet " +
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

        int request_status = HttpServletResponse.SC_OK;

        try {
            try {

                RequestCache.openThreadCache();

                int reqno = _reqNumber.incrementAndGet();

                LogUtil.logServerAccessStart(request, "HyraxAccess", "HTTP-POST", Long.toString(reqno));

                _log.debug(ServletUtil.showRequest(request, reqno));


                _log.debug("Requested relative URL: '" + relativeUrl +
                        "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                        "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");

                if (Debug.isSet("probeRequest"))
                    _log.debug(ServletUtil.probeRequest(this, request));



                HyraxService hyraxService = null;
                for(HyraxService service: _hyraxServices){
                    if(hyraxService==null && service.requestCanBeHandled(request)){
                        hyraxService = service;
                    }
                }
                if(hyraxService==null)
                    hyraxService = _defaultHyraxService;

                hyraxService.handleRequest(request,response);


            }
            finally {
                _log.info("doPost(): Response completed.\n");
            }

        } catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this, response);
            }
            catch(Throwable t2) {
                try {
                    _log.error("BAD THINGS HAPPENED!", t2);
                }
                catch(Throwable t3){
                    // It's boned now.. Leave it be.
                }
            }
        }
        finally{
            LogUtil.logServerAccessEnd(request_status, "HyraxAccess");
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
     *         handler claims the request.
     * @throws Exception For bad behaviour.
     */
    private DispatchHandler getDispatchHandler(HttpServletRequest request, Vector<DispatchHandler> dhvec) throws Exception {
        for (DispatchHandler dh : dhvec) {
            _log.debug("Checking handler: " + dh.getClass().getName());
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
     *         since midnight January 1, 1970 GMT
     */
    protected long getLastModified(HttpServletRequest req) {


        RequestCache.openThreadCache();

        long reqno = _reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "HyraxAccess", "LAST-MOD", Long.toString(reqno));

        long lmt = -1;

        Procedure timedProcedure = Timer.start();
        try {

            if (ReqInfo.isServiceOnlyRequest(req)) {
                return lmt;
            }


            if (!LicenseManager.isExpired(req) && !ReqInfo.isServiceOnlyRequest(req)) {

                HyraxService hyraxService = null;
                for(HyraxService service: _hyraxServices){
                    if(hyraxService==null && service.requestCanBeHandled(req)){
                        hyraxService = service;
                    }
                }
                if(hyraxService==null)
                    hyraxService = _defaultHyraxService;

                lmt = hyraxService.getLastModified(req);

            }
        } catch (Exception e) {
            _log.error("getLastModifiedTime() - Caught " + e.getClass().getName() + " msg: " + e.getMessage());
            lmt = -1;
        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, "HyraxAccess");
            Timer.stop(timedProcedure);

        }


        return lmt;

    }





    public void destroy() {

        LogUtil.logServerShutdown("destroy()");

        if(_hyraxServices != null){
            for (HyraxService dh : _hyraxServices) {
                _log.debug("Shutting down HyraxService: " + dh.getClass().getName());
                dh.destroy();
            }
        }
        _hyraxServices.clear();
        _hyraxServices = null;
        _defaultHyraxService.destroy();
        _defaultHyraxService = null;
        super.destroy();
    }




}
