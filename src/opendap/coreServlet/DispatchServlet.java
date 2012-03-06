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


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

import opendap.logging.LogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.SAXBuilder;

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

        PersistentContentHandler.installInitialContent(this);


        loadConfig();


        buildHandlers("HttpGetHandlers", httpGetDispatchHandlers, httpGetHandlerConfigs);
        //identifyRequiredGetHandlers(httpGetDispatchHandlers);
        intitializeHandlers(httpGetDispatchHandlers, httpGetHandlerConfigs);


        buildHandlers("HttpPostHandlers", httpPostDispatchHandlers, httpPostHandlerConfig);
        intitializeHandlers(httpPostDispatchHandlers, httpPostHandlerConfig);

        log.info("init() complete.");


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

        filename = Scrub.fileName(ServletUtil.getContentPath(this) + filename);

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


        Element httpGetHandlerElements = configDoc.getRootElement().getChild("DispatchHandlers").getChild(type);

        log.debug("Building "+ type);


        for (Object o : httpGetHandlerElements.getChildren("Handler")) {
            Element handlerElement = (Element) o;
            handlerConfigs.add(handlerElement);
            String className = handlerElement.getAttribute("className").getValue();
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
                msg = "Cannot cast class: " + className + " to opendap.coreServlet.IsoDispatchHandler";
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (Exception e) {
                msg = "Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
                log.error(msg);
                throw new ServletException(msg, e);

            }

            dispatchHandlers.add(dh);
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


    private void identifyRequiredGetHandlers(Vector<DispatchHandler> dhv) throws ServletException {
        String msg;
        for (DispatchHandler dh : dhv) {
            if (dh instanceof OpendapHttpDispatchHandler) {
                if (odh == null)
                    odh = (OpendapHttpDispatchHandler) dh;
                else {
                    msg = "Only one instance of OpendapHttpDispatchHandler is allowed in a configuration!";
                    log.error(msg);
                    throw new ServletException(msg);
                }
            }
        }
        if (odh == null) {
            msg = "There must be an instance OpendapHttpDispatchHandler in the configuration!";
            log.error(msg);
            throw new ServletException(msg);
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
        String dataSource =  ReqInfo.getBesDataSourceID(relativeUrl);

        try {
            try {
                RequestCache.startRequestIfNeeded();

                int reqno = reqNumber.incrementAndGet();
                LogUtil.logServerAccessStart(request, "HyraxAccess", "HTTP-GET", Long.toString(reqno));

                log.debug(Util.getMemoryReport());

                log.debug(Util.showRequest(request, reqno));
                log.debug(Util.probeRequest(this, request));


                if(redirectForServiceOnlyRequest(request,response))
                    return;


                log.info("Requested dataSource: '" + dataSource +
                        "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                        "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");



                if (Debug.isSet("probeRequest"))
                    log.debug(Util.probeRequest(this, request));


                DispatchHandler dh = getDispatchHandler(request, httpGetDispatchHandlers);
                if (dh != null) {
                    log.debug("Request being handled by: " + dh.getClass().getName());
                    dh.handleRequest(request, response);
                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");
    
                } else {
                    send404(request,response);
                }
            }
            finally {
                RequestCache.endRequest();
                log.info("doGet(): Response completed.\n");
            }


        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            }
            catch(Throwable t2) {
            	try {
            		log.error("\n########################################################\n" +
                                "Request proccessing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }


    }
    //**************************************************************************


    private void send404(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        // Build a regex to use to see if they are looking for a DAP2 response:
        String dap2Regex = ".*.(";
        dap2Regex += "dds";
        dap2Regex += "|das";
        dap2Regex += "|dods";
        dap2Regex += "|asc(ii)?";
        dap2Regex += ")";
        Pattern dap2Pattern = Pattern.compile(dap2Regex,Pattern.CASE_INSENSITIVE);


        // Build a regex to use to see if they are looking for a DAP3/4 response:
        String dap4Regex = ".*.(";
        dap4Regex += "ddx";
        dap4Regex += "|rdf";
        dap4Regex += ")";
        Pattern dap4Pattern = Pattern.compile(dap4Regex,Pattern.CASE_INSENSITIVE);


        String requestURL = req.getRequestURL().toString();

        if(dap2Pattern.matcher(requestURL).matches()){   // Is it a DAP2 request?
            resp.setHeader("XDODS-Server", "dods/3.2");
            resp.setHeader("XOPeNDAP-Server", "Server-Version-Unknown");
            resp.setHeader("XDAP", "3.2");
            resp.setHeader("Content-Description", "dods_error");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getOutputStream().print(
                    OPeNDAPException.getDAP2Error(
                            OPeNDAPException.NO_SUCH_FILE,
                            "Cannot locate resource: "+Scrub.completeURL(requestURL)));
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



        Util.probeRequest(this, req);

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
     * @throws IOException       .
     * @throws ServletException    .
     */
    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource =  ReqInfo.getBesDataSourceID(relativeUrl);

        try {
            try {

                RequestCache.startRequestIfNeeded();

                int reqno = reqNumber.incrementAndGet();

                LogUtil.logServerAccessStart(request, "HyraxAccess", "HTTP-POST", Long.toString(reqno));

                log.debug(Util.showRequest(request, reqno));


                log.info("Requested dataSource: '" + dataSource +
                       "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                       "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");

                if (Debug.isSet("probeRequest"))
                    log.debug(Util.probeRequest(this, request));


                DispatchHandler dh = getDispatchHandler(request, httpPostDispatchHandlers);
                if (dh != null) {
                    log.debug("Request being handled by: " + dh.getClass().getName());
                    dh.handleRequest(request, response);
                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");

                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    log.info("Sent Resource Not Found (404) - nothing left to check.");
                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_NOT_FOUND, -1, "HyraxAccess");
                }
            }
            finally {
                RequestCache.endRequest();
                log.info("doPost(): Response completed.\n");
            }

        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
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

        RequestCache.startRequest();

        long reqno = reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "HyraxAccess", "LastModified", Long.toString(reqno));

        if(ReqInfo.isServiceOnlyRequest(req))
            return -1;

        try {

            DispatchHandler dh = getDispatchHandler(req, httpGetDispatchHandlers);
            if (dh != null) {
                log.debug("getLastModified() -  Request being handled by: " + dh.getClass().getName());
                return dh.getLastModified(req);

            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
        finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");

        }


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
