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
package opendap.build_dmrpp;

import opendap.bes.BESManager;
import opendap.coreServlet.*;
import opendap.http.error.BadRequest;
import opendap.logging.ServletLogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;




/**
 * User: ndp
 * Date: Apr 22, 2008
 * Time: 3:36:36 PM
 */
public class BuildDmrppServlet extends HttpServlet {


    private static final Logger log  = org.slf4j.LoggerFactory.getLogger(opendap.build_dmrpp.BuildDmrppServlet.class);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final AtomicInteger reqNumber = new AtomicInteger(0);


    private static Element config;
    private static BuildDmrppDispatchHandler buildDmrppDispatchHandler;

    @Override
    public void init() throws ServletException {

        if (isInitialized.get())
            return;

        ServletLogUtil.logServerStartup("init()");

        String contextPath = getServletContext().getContextPath();
        buildDmrppDispatchHandler = new BuildDmrppDispatchHandler();

        try {
            config = loadConfig();

            initBesManager();

            buildDmrppDispatchHandler.init(this, config);


        } catch (Exception e) {
            log.error("init() Failed to load it's configuration! Caught " + e.getClass().getName() + " Message: " + e.getMessage());
            throw new ServletException(e);
        }
        log.info("Initialized.");
        isInitialized.set(true);
    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * GatewayConfigFileName.
     *
     * @throws javax.servlet.ServletException When the file is missing, unreadable, or fails
     *                                        to parse (as an XML document).
     */
    private Element loadConfig() throws ServletException {
        String configFileName="ConfigFileName";
        String filename = getInitParameter(configFileName);
        if (filename == null) {

            String msg = this.getClass().getName() +
                    " - Servlet configuration is missing init parameter '" +
                    configFileName +
                    "'." +
                    " Proceeding with out configuration.";
            log.warn(msg);
            return null;
        }

        filename = Scrub.fileName(ServletUtil.getConfigPath(this) + filename);
        log.debug("Loading Configuration File: " + filename);
        try {
            File confFile = new File(filename);
            FileInputStream fis = new FileInputStream(confFile);
            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                Document configDoc =  sb.build(fis);
                Element root = configDoc.getRootElement();
                root.detach();
                return root;
            } finally {
                fis.close();
            }
        } catch (FileNotFoundException e) {
            String msg = "The build_dmrpp configuration file \"" + filename + "\" cannot be found.";
            log.warn(msg);
            //throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "The build_dmrpp configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "The build_dmrpp configuration file \"" + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
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
    @Override
    public long getLastModified(HttpServletRequest req) {

        RequestCache.open(req);

        ServletLogUtil.logServerAccessStart(req, ServletLogUtil.BUILD_DMRPP_LAST_MODIFIED_LOG_ID, "LastModified", RequestCache.getRequestId());
        try {
            if (ReqInfo.isServiceOnlyRequest(req))
                return new Date().getTime();

            return buildDmrppDispatchHandler.getLastModified(req);
        }
        finally {
            ServletLogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, ServletLogUtil.BUILD_DMRPP_LAST_MODIFIED_LOG_ID);
            // We don't RequestCache.close() here so that the cache is
            // available for the doGet() method which comes next.
        }
    }


    private boolean redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (req.getPathInfo() == null) {
            res.sendRedirect(Scrub.urlContent(req.getRequestURI() + "/"));
            log.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }


    /**
     *
     * @param request
     * @param response
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        int request_status = HttpServletResponse.SC_OK;
        try {
            RequestCache.open(request);

            ServletLogUtil.logServerAccessStart(request, ServletLogUtil.BUILD_DMRPP_ACCESS_LOG_ID, "HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));
            if (!redirect(request, response)) {

                if(!buildDmrppDispatchHandler.requestDispatch(request,response,true)){
                    if(!response.isCommitted()){
                        log.info("Unrecognized build_dmrpp request.");
                        request_status = OPeNDAPException.anyExceptionHandler(new BadRequest("ERROR: Unrecognized Request URL!"), this,  response);
                    }
                }
            }
        } catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this,  response);
            } catch (Throwable t2) {
                log.error("THE BAD THINGS HAPPENED!", t2);
            }
        } finally {
            ServletLogUtil.logServerAccessEnd(request_status, ServletLogUtil.BUILD_DMRPP_ACCESS_LOG_ID);
            RequestCache.close();
        }
    }

    private void initBesManager() throws ServletException {
        Element besManagerElement = config.getChild(BESManager.BES_MANAGER_CONFIG_ELEMENT);
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


    @Override
    public void destroy() {
        ServletLogUtil.logServerShutdown("destroy()");
        buildDmrppDispatchHandler.destroy();
        super.destroy();
        log.info("Build dmr++ service shutdown complete.");
    }

}
