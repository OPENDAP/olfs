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
package opendap.ngap;

import opendap.coreServlet.*;
import opendap.http.error.BadRequest;
import opendap.logging.LogUtil;
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
 * User: dan
 * Date: Feb. 13, 2020
 * Time: 1:40PM
 * Cloned from: opendap.gateway
 */
@Deprecated
public class NGAP_DispatchServlet extends HttpServlet {


    private static final Logger log  = org.slf4j.LoggerFactory.getLogger(DispatchServlet.class);
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final AtomicInteger reqNumber = new AtomicInteger(0);

    private static Element config;
    private static NGAP_DispatchHandler ngapDispatchHandler;

    @Override
    public void init() throws ServletException {

        if (isInitialized.get())
            return;

        LogUtil.logServerStartup("init()");

        ngapDispatchHandler = new NGAP_DispatchHandler();

        try {
            config = loadConfig();
            ngapDispatchHandler.init(this, config);

        } catch (Exception e) {
            log.error("init() Failed to load it's configuration! Caught " + e.getClass().getName() + " Message: " + e.getMessage());
            throw new ServletException(e);
        }
        log.info("Initialized.");
        isInitialized.set(true);
    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * NGAPConfigFileName.
     *
     * @throws javax.servlet.ServletException When the file is missing, unreadable, or fails
     *                                        to parse (as an XML document).
     */
    private Element loadConfig() throws ServletException {

        String filename = getInitParameter("NGAPConfigFileName");
        if (filename == null) {
            StringBuilder msg = new StringBuilder();

            msg.append(this.getClass().getName())
                    .append(" - Servlet configuration is missing init parameter 'NGAPConfigFileName'.")
                    .append(" Proceeding with out configuration.");
            log.warn(msg.toString());
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
            String msg = "NGAP configuration file \"" + filename + "\" cannot be found.";
            log.warn(msg);
            //throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "NGAP configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "NGAP configuration file \"" + filename + "\" cannot be parsed.";
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

        RequestCache.openThreadCache();

        long reqno = reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, LogUtil.NGAP_ACCESS_LAST_MODIFIED_LOG_ID, "LastModified", Long.toString(reqno));
        try {
            if (ReqInfo.isServiceOnlyRequest(req))
                return new Date().getTime();

            return ngapDispatchHandler.getLastModified(req);
        }
        finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, LogUtil.NGAP_ACCESS_LAST_MODIFIED_LOG_ID);
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
            LogUtil.logServerAccessStart(request, LogUtil.NGAP_ACCESS_LOG_ID, "HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));
            if (!redirect(request, response)) {

                if(!ngapDispatchHandler.requestDispatch(request,response,true)){
                    if(!response.isCommitted()){
                        log.info("Sent BAD URL - not an OPeNDAP request suffix.");
                        // throw new BadRequest("Bad Gateway URL! Not an OPeNDAP request suffix");
                        request_status = OPeNDAPException.anyExceptionHandler(new BadRequest("Bad NGAP URL! Not an OPeNDAP request suffix"), this,  response);
                    }
                }
            }
        } catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this,  response);
            } catch (Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            LogUtil.logServerAccessEnd(request_status, LogUtil.NGAP_ACCESS_LOG_ID);
            RequestCache.closeThreadCache();
        }
    }


    @Override
    public void destroy() {
        LogUtil.logServerShutdown("destroy()");
        ngapDispatchHandler.destroy();
        super.destroy();
        log.info("NGAP service shutdown complete.");
    }

}
