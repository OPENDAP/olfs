/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
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


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import thredds.servlet.ServletUtil;

/**
 * This servlet provides the dispatching for all OPeNDAP requests.
 * <p/>
 * <p>This server will respond to both HTTP GET and POST requests. The GET dispatching is
 * done in this class, and the POST dispatching (which is in fact the SOAP inrterface)
 * is done in <code>SOAPRequestDispatcher</code></p>
 * <p/>
 * <p/>
 * <p>This server is built designed so that the actual handling of the dispatch is done
 * through code that is identified at run time through the web.xml configuration of the
 * servlet. In particular the HTTP GET request are handled by a class the implements the
 * OpendapHttpDispatchHandler interface. The SOAP requests (via HTTP POST) are handled by a
 * class the implements the OpendapSOAPDispatchHandler interface.<p>
 * <p/>
 * <p>The web.xml file used to configure this servlet must contain servlet parameters identifying
 * an implmentation clas for both these interfaces.</p>
 * <p/>
 * <p/>
 * <p/>
 */
public class DispatchServlet extends HttpServlet {


    /**
     * ************************************************************************
     * Count "hits" on the server...
     *
     * @serial
     */
    private long reqNumber = 0;
    //private int lastModifiedHits = 0;

    /**
     * ************************************************************************
     * Used synchronize access to the hit counter.
     *
     * @serial
     */
    private Semaphore syncLock;


    private Vector<DispatchHandler> httpGetDispatchHandlers;
    private Vector<DispatchHandler> httpPostDispatchHandlers;

    private OpendapHttpDispatchHandler odh = null;
    private OpendapSoapDispatchHandler sdh = null;
    private ThreddsHandler tdh = null;
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

        syncLock = new Semaphore(1);
        httpGetDispatchHandlers = new Vector<DispatchHandler>();
        Vector<Element> httpGetHandlerConfigs = new Vector<Element>();
        httpPostDispatchHandlers = new Vector<DispatchHandler>();
        Vector<Element> httpPostHandlerConfig = new Vector<Element>();


        initDebug();
        initLogging();

        ReqInfo.init();

        // init logging
        PerfLog.logServerSetup("init()");
        log.info("init() start.");

        PersistentContentHandler.installInitialContent(this);


        loadConfig();


        buildHandlers("HttpGetHandlers", httpGetDispatchHandlers, httpGetHandlerConfigs);
        identifyRequiredGetHandlers(httpGetDispatchHandlers);
        intitializeHandlers(httpGetDispatchHandlers, httpGetHandlerConfigs);


        buildHandlers("HttpPostHandlers", httpPostDispatchHandlers, httpPostHandlerConfig);
        intitializeHandlers(httpPostDispatchHandlers, httpPostHandlerConfig);

        log.info("init() complete.");


    }

    public ThreddsHandler getThreddsDispatchHandler() {
        return tdh;
    }

    public String getDocsPath() {
        return "docs/";
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

        filename = ServletUtil.getContentPath(this) + filename;

        log.debug("Loading Configuration File: " + filename);


        try {

            File confFile = new File(filename);

            // Parse the XML doc into a Document object.
            SAXBuilder sb = new SAXBuilder();
            configDoc = sb.build(new FileInputStream(confFile));

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
     * @param type             A String containing the name of DispatchHandler list from
     *                         the OLFS to build from.
     * @param dispatchHandlers A Vector in which to store the built
     *                         DispatchHandler instances
     * @param handlerConfigs   A Vector in which to store the configuration
     *                         Element for each DispatchHandler
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


        for (DispatchHandler dh : dhv) {
            if (dh instanceof ThreddsHandler) {
                if (tdh == null)
                    tdh = (ThreddsHandler) dh;
                else {
                    msg = "Only one instance of ThreddsHandler is allowed in a configuration!";
                    log.error(msg);
                    throw new ServletException(msg);
                }
            }
        }
        if (tdh == null) {
            msg = "There must be an instance ThreddsHandler in the configuration!";
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
        thredds.servlet.ServletUtil.initLogging(this);
        PerfLog.initLogging(this);
        log = org.slf4j.LoggerFactory.getLogger(getClass());

    }


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


        try {
            syncLock.acquireUninterruptibly();
            reqNumber++;
            PerfLog.logServerAccessStart(request, "HyraxAccess", "HTTP-GET", Long.toString(reqNumber));
            log.debug(Util.showRequest(request, reqNumber));

            log.info("Requested dataSource: '" + ReqInfo.getDataSource(request) +
                    "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                    "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");

            syncLock.release();


            if (Debug.isSet("probeRequest"))
                Util.probeRequest(System.out, this, request, getServletContext(), getServletConfig());


            DispatchHandler dh = getHandler(request, httpGetDispatchHandlers);
            if (dh != null) {
                log.debug("Request being handled by: " + dh.getClass().getName());
                dh.handleRequest(request, response);
                PerfLog.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");

            } else {
                sendResourceNotFound(request, response);
                log.info("Sent Resource Not Found (404) - nothing left to check.");
                PerfLog.logServerAccessEnd(HttpServletResponse.SC_NOT_FOUND, -1, "HyraxAccess");
            }


        } catch (Throwable e) {
            OPeNDAPException.anyExceptionHandler(e, response);
        }


    }
    //**************************************************************************



    /**
     * @param request  .
     * @param response .
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {

        try {
            // START Synchronized section

            syncLock.acquireUninterruptibly();
            reqNumber++;
            PerfLog.logServerAccessStart(request, "HyraxAccess", "HTTP-POST", Long.toString(reqNumber));
            log.debug(Util.showRequest(request, reqNumber));

            log.info("Requested dataSource: '" + ReqInfo.getDataSource(request) +
                   "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                   "' CE: '" + ReqInfo.getConstraintExpression(request) + "'");
            syncLock.release();
            // END Synchronized section

            if (Debug.isSet("probeRequest"))
                Util.probeRequest(System.out, this, request, getServletContext(), getServletConfig());


            DispatchHandler dh = getHandler(request, httpPostDispatchHandlers);
            if (dh != null) {
                log.debug("Request being handled by: " + dh.getClass().getName());
                dh.handleRequest(request, response);
                PerfLog.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");

            } else {
                sendResourceNotFound(request, response);
                log.info("Sent Resource Not Found (404) - nothing left to check.");
                PerfLog.logServerAccessEnd(HttpServletResponse.SC_NOT_FOUND, -1, "HyraxAccess");
            }


        } catch (Throwable e) {
            OPeNDAPException.anyExceptionHandler(e, response);
        }

    }
    /**
     * Returns the first handler in the vector of DispatchHandlers that claims
     * be able to handle the incoming request.
     *
     * @param request The request we are looking to handle
     * @param dhvec   A Vector of DispatchHandlers that will be asked if they can
     *                handle the request.
     * @return The DispatchHandler that can handle the request, null if no
     *         handler claims the request.
     * @throws Exception For bad behaviour.
     */
    private DispatchHandler getHandler(HttpServletRequest request, Vector<DispatchHandler> dhvec) throws Exception {
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
     * the only entity capable of determining the last modified dat the job is passed  through to it.
     *
     * @param req The current request
     * @return Returns the time the HttpServletRequest object was last modified, in milliseconds
     *         since midnight January 1, 1970 GMT
     */
    protected long getLastModified(HttpServletRequest req) {

        try {
            syncLock.acquireUninterruptibly();
            long reqno = ++reqNumber;
            //lastModifiedHits++;
            PerfLog.logServerAccessStart(req, "HyraxAccess", "LastModified", Long.toString(reqno));
            syncLock.release();

            DispatchHandler dh = getHandler(req, httpGetDispatchHandlers);
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
            PerfLog.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");

        }

    }



    public void destroy() {

        for (DispatchHandler dh : httpGetDispatchHandlers) {
            log.debug("Shutting down handler: " + dh.getClass().getName());
            dh.destroy();
        }


        super.destroy();
    }


    private void sendResourceNotFound(HttpServletRequest request, HttpServletResponse response) throws Exception {

        response.setContentType("text/html");
        response.setHeader("Content-Description", "BadURL");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        String topLevel = request.getRequestURL().substring(0, request.getRequestURL().lastIndexOf(request.getPathInfo()));

        pw.println("<h2>Resource Not Found</h2>");
        pw.println("<p>The URL <i>'" + request.getRequestURL() + "'</i> does not describe a resource that can be found on this server.</p>");
        pw.println("<p>If you would like to start at the top level of this server, go here:</p>");
        pw.println("<p><a href='" + topLevel + "'>" + topLevel + "</a></p>");
        pw.println("<p>If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a></p>");

        pw.flush();


    }


}
