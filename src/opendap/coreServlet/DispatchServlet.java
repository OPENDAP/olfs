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
import opendap.http.AuthenticationControls;
import opendap.logging.LogUtil;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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
public class DispatchServlet extends HttpServlet {


    /**
     * ************************************************************************
     * A thread safe hit counter
     *
     * @serial
     */
    private AtomicInteger reqNumber;



    private Vector<DispatchHandler> httpGetDispatchHandlers;
    private Vector<DispatchHandler> httpPostDispatchHandlers;

    private OpendapHttpDispatchHandler odh = null;
    // private ThreddsHandler tdh = null;
    private org.slf4j.Logger log;

    private Document configDoc;


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

        reqNumber = new AtomicInteger(0);

        log.debug("init() start");

        /*
        String xslTransformerFactoryImpl = "com.icl.saxon.TransformerFactoryImpl";
        String xslTransformerFactoryProperty = "javax.xml.transform.TransformerFactory";

        log.info("init(): Setting System Property " +
                xslTransformerFactoryProperty +
                "="+xslTransformerFactoryImpl);
        System.setProperty(xslTransformerFactoryProperty,xslTransformerFactoryImpl);
        */



        httpGetDispatchHandlers = new Vector<DispatchHandler>();
        Vector<Element> httpGetHandlerConfigs = new Vector<Element>();
        httpPostDispatchHandlers = new Vector<DispatchHandler>();
        Vector<Element> httpPostHandlerConfig = new Vector<Element>();


        // init logging
        LogUtil.logServerStartup("init()");
        log.info("init() start.");

        PersistentConfigurationHandler.installDefaultConfiguration(this);


        loadConfig();


        Element timer = configDoc.getRootElement().getChild("Timer");
        if(timer!=null){
            String enabled = timer.getAttributeValue("enabled");
            if(enabled!=null && enabled.equalsIgnoreCase("true")){
                Timer.enable();
            }
        }

        log.info("init() - Timer is {}",Timer.isEnabled()?"ENABLED":"DISABLED");



        initBesManager();

        initAuthenticationControls();


        buildHandlers("HttpGetHandlers", httpGetDispatchHandlers, httpGetHandlerConfigs);
        //identifyRequiredGetHandlers(httpGetDispatchHandlers);
        intitializeHandlers(httpGetDispatchHandlers, httpGetHandlerConfigs);


        buildHandlers("HttpPostHandlers", httpPostDispatchHandlers, httpPostHandlerConfig);
        intitializeHandlers(httpPostDispatchHandlers, httpPostHandlerConfig);
        if(httpPostDispatchHandlers.size()==0){
            log.info("No POST handlers configured. Adding the NoPostHandler.");
            httpPostDispatchHandlers.add(new NoPostHandler());
        }

        log.info("init() complete.");

        RequestCache.closeThreadCache();

    }



    private void initAuthenticationControls() throws ServletException {

        Element authControlElem = configDoc.getRootElement().getChild(AuthenticationControls.CONFIG_ELEMENT);


        try {
            if(authControlElem !=  null){
                AuthenticationControls.init(authControlElem);
            }
        }
        catch(Exception e){
            throw new ServletException(e);
        }


    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * OLFSConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private void loadConfig() throws ServletException {

        String filename = getInitParameter("OLFSConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the OLFS configuration!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }

        filename = Scrub.fileName(ServletUtil.getConfigPath(this) + filename);

        log.debug("Loading Configuration File: " + filename);


        try {

            File confFile = new File(filename);
            FileInputStream fis = new FileInputStream(confFile);

            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                configDoc = sb.build(fis);
            }
            finally {
            	fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "OLFS configuration file \"" + filename + "\" cannot be found.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "OLFS configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "OLFS configuration file \"" + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
        }

        log.debug("Configuration loaded and parsed.");

    }


    private void initBesManager() throws ServletException {

        Element besManagerElement = configDoc.getRootElement().getChild("BESManager");

        if(besManagerElement ==  null){
            String msg = "Invalid configuration. Missing required 'BESManager' element. DispatchServlet FAILED to init()!";
            log.error(msg);
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
     * Navigates the config document to instantiate an ordered list of
     * Dispatch Handlers. Once built the list is searched for a single instance
     * of an OpendapHttpDispatchHandler and a single instance of a
     * ThreddsHandler. Then all of the handlers are initialized by
     * calling their init() methods and passing into them the XML Element
     * that defined them from the config document.
     *
     * @param type             A String containing the name of IsoDispatchHandler list from
     *                         the OLFS to build from.
     * @param dispatchHandlers A Vector in which to store the built
     *                         IsoDispatchHandler instances
     * @param handlerConfigs   A Vector in which to store the configuration
     *                         Element for each IsoDispatchHandler
     * @throws ServletException When things go poorly
     */
    private void buildHandlers(String type, Vector<DispatchHandler> dispatchHandlers, Vector<Element> handlerConfigs) throws ServletException {

        String msg;


        Element httpHandlerElements = configDoc.getRootElement().getChild("DispatchHandlers").getChild(type);

        log.debug("Building "+ type);

        if(httpHandlerElements!=null){

            for (Object o : httpHandlerElements.getChildren("Handler")) {
                Element handlerElement = (Element) o;
                handlerConfigs.add(handlerElement);
                String className = handlerElement.getAttributeValue("className");
                if(className!=null) {

                    DispatchHandler dh;
                    try {

                        log.debug("Building Handler: " + className);
                        Class classDefinition = Class.forName(className);
                        dh = (DispatchHandler) classDefinition.newInstance();


                    } catch (ClassNotFoundException e) {
                        msg = "Cannot find class: " + className;
                        log.error(msg);
                        throw new ServletException(msg, e);
                    } catch (InstantiationException e) {
                        msg = "Cannot instantiate class: " + className;
                        log.error(msg);
                        throw new ServletException(msg, e);
                    } catch (IllegalAccessException e) {
                        msg = "Cannot access class: " + className;
                        log.error(msg);
                        throw new ServletException(msg, e);
                    } catch (ClassCastException e) {
                        msg = "Cannot cast class: " + className + " to opendap.coreServlet.DispatchHandler";
                        log.error(msg);
                        throw new ServletException(msg, e);
                    } catch (Exception e) {
                        msg = "Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
                        log.error(msg);
                        throw new ServletException(msg, e);

                    }

                    dispatchHandlers.add(dh);
                }
                else {
                    log.error("buildHandlers() - FAILED to locate the required 'className' attribute in Handler element. SKIPPING.");
                }
            }
        }

        log.debug(type + " Built.");

    }

    private void intitializeHandlers(Vector<DispatchHandler> dispatchHandlers, Vector<Element> handlerConfigs) throws ServletException {

        log.debug("Initializing Handlers.");
        String msg;

        try {
            DispatchHandler dh;
            Element config;
            for (int i = 0; i < dispatchHandlers.size(); i++) {
                dh = dispatchHandlers.get(i);
                config = handlerConfigs.get(i);
                dh.init(this, config);
            }
        }
        catch (Exception e) {
            msg = "Could not init() a handler! Caught " + e.getClass().getName() + " Msg: " + e.getMessage();
            log.error(msg);
            throw new ServletException(msg, e);
        }


        log.debug("Handlers Initialized.");


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
        log = org.slf4j.LoggerFactory.getLogger(getClass());

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


        try {
            Procedure timedProcedure = Timer.start();

            RequestCache.openThreadCache();

            try {

                if(LicenseManager.isExpired(request)){
                    LicenseManager.sendLicenseExpiredPage(request,response);
                    return;
                }



                int reqno = reqNumber.incrementAndGet();
                LogUtil.logServerAccessStart(request, "HyraxAccess", "HTTP-GET", Long.toString(reqno));

                log.debug(Util.getMemoryReport());

                log.debug(ServletUtil.showRequest(request, reqno));
                //log.debug(AwsUtil.probeRequest(this, request));


                if(redirectForServiceOnlyRequest(request,response))
                    return;


                log.debug("Requested relative URL: '" + relativeUrl +
                        "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                        "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");



                if (Debug.isSet("probeRequest"))
                    log.debug(ServletUtil.probeRequest(this, request));


                DispatchHandler dh = getDispatchHandler(request, httpGetDispatchHandlers);
                if (dh != null) {
                    log.debug("Request being handled by: " + dh.getClass().getName());
                    dh.handleRequest(request, response);
                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");
    
                } else {
                    //send404(request,response);
                    OPeNDAPException oe = new OPeNDAPException(HttpServletResponse.SC_NOT_FOUND, "Failed to locate resource: "+relativeUrl);
                    throw oe;
                }
            }
            finally {
                Timer.stop(timedProcedure);
            }


        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, this, request.getContextPath(), response);
            }
            catch(Throwable t2) {
            	try {
            		log.error("\n########################################################\n" +
                                "Request processing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
        finally {
            RequestCache.closeThreadCache();
            log.info("doGet(): Response completed.\n");
        }

        log.info("doGet() - Timing Report: \n{}", Timer.report());
        Timer.reset();
    }
    //**************************************************************************


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




        log.info("Sent Resource Not Found (404) - nothing left to check.");
        LogUtil.logServerAccessEnd(HttpServletResponse.SC_NOT_FOUND, -1, "HyraxAccess");



    }


    private boolean redirectForServiceOnlyRequest(HttpServletRequest req,
                                                  HttpServletResponse res)
            throws IOException {



        ServletUtil.probeRequest(this, req);

        if (ReqInfo.isServiceOnlyRequest(req)) {
            String reqURI = req.getRequestURI();
            String newURI = reqURI+"/";
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

        try {
            try {

                RequestCache.openThreadCache();

                int reqno = reqNumber.incrementAndGet();

                LogUtil.logServerAccessStart(request, "HyraxAccess", "HTTP-POST", Long.toString(reqno));

                log.debug(ServletUtil.showRequest(request, reqno));


                log.debug("Requested relative URL: '" + relativeUrl +
                       "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                       "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");

                if (Debug.isSet("probeRequest"))
                    log.debug(ServletUtil.probeRequest(this, request));


                DispatchHandler dh = getDispatchHandler(request, httpPostDispatchHandlers);
                if (dh != null) {
                    log.debug("Request being handled by: " + dh.getClass().getName());
                    dh.handleRequest(request, response);
                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");

                } else {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    log.error("Failed to locate default NoPostHandler!!");
                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1, "HyraxAccess");
                }



            }
            finally {
                log.info("doPost(): Response completed.\n");
            }

        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, this, request.getContextPath(), response);
            }
            catch(Throwable t2) {
            	try {
            		log.error("BAD THINGS HAPPENED!", t2);
            	}
            	catch(Throwable t3){
            		// It's boned now.. Leave it be.
            	}
            }
        }
        finally{
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
            log.debug("Checking handler: " + dh.getClass().getName());
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

        long reqno = reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "HyraxAccess", "LastModified", Long.toString(reqno));


        long lmt = -1;

        Procedure timedProcedure = Timer.start();
        try {

            if (ReqInfo.isServiceOnlyRequest(req)) {
                LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");
                return -1;
            }


            if (!LicenseManager.isExpired(req) && !ReqInfo.isServiceOnlyRequest(req)) {

                DispatchHandler dh = getDispatchHandler(req, httpGetDispatchHandlers);
                if (dh != null) {
                    log.debug("getLastModified() -  Request being handled by: " + dh.getClass().getName());
                    lmt = dh.getLastModified(req);

                }
            }
        } catch (Exception e) {
            log.error("getLastModifiedTime() - Caught " + e.getClass().getName() + " msg: " + e.getMessage());
            lmt = -1;
        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");
            Timer.stop(timedProcedure);

        }


        return lmt;

    }


    public void destroy() {

        LogUtil.logServerShutdown("destroy()");

        if(httpGetDispatchHandlers != null){
            for (DispatchHandler dh : httpGetDispatchHandlers) {
                log.debug("Shutting down handler: " + dh.getClass().getName());
                dh.destroy();
            }
        }


        super.destroy();
    }




}
