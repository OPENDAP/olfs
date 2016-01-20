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
package opendap.hai;

import opendap.coreServlet.*;
import opendap.http.error.BadRequest;
import opendap.logging.LogUtil;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * User: ndp
 * Date: Apr 22, 2008
 * Time: 3:36:36 PM
 */
public class DispatchServlet extends opendap.coreServlet.DispatchServlet {


    private Logger log;


    private Document _config;


    private AtomicInteger reqNumber;

    private String systemPath;

    private boolean isInitialized;


    private Vector<HttpResponder> responders;


    public void init() {

        if (isInitialized)
            return;

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        isInitialized = false;
        reqNumber = new AtomicInteger(0);
        systemPath = ServletUtil.getSystemPath(this, "");

        responders = new Vector<HttpResponder>();


        // dapResponders.add(new DDX(getMethod(,HttpServletRequest.class,HttpServletResponse.class)));




        responders.add(new OlfsControlApi(systemPath));
        responders.add(new BesControlApi(systemPath));


        log.info("masterDispatchRegex=\"" + getDispatchRegex() + "\"");
        log.info("Initialized.");


        isInitialized = true;
    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * OLFSConfigFileName.
     *
     * @throws javax.servlet.ServletException When the file is missing, unreadable, or fails
     *                                        to parse (as an XML document).
     */
    private Document loadConfig() throws ServletException {

        String filename = getInitParameter("ConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the Admin configuration!\n";
            log.warn(msg);
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
                return sb.build(fis);
            } finally {
                fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "gateway configuration file \"" + filename + "\" cannot be found.";
            log.warn(msg);
            //throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "gateway configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "gateway configuration file \"" + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
        }

        return null;


    }


    public Pattern getDispatchRegex() {
        String masterRegex = null;

        for (HttpResponder p : responders) {
            if (masterRegex != null)
                masterRegex += "|";
            else
                masterRegex = "";

            masterRegex += p.getRequestMatchRegexString();
        }
        return Pattern.compile(masterRegex);
    }


    /*
    public long getLastModified(HttpServletRequest req) {

        long lmt;
        lmt = -1;
        return lmt;


    }
    */


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
        LogUtil.logServerAccessStart(req, "ADMIN_SERVICE_ACCESS", "LastModified", Long.toString(reqno));

        if (ReqInfo.isServiceOnlyRequest(req))
            return -1;


        try {
            return -1;

        } catch (Exception e) {
            return -1;
        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, "ADMIN_SERVICE_ACCESS");

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


    private String getName(HttpServletRequest req) {
        return req.getPathInfo();
    }


    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        int request_status = HttpServletResponse.SC_OK;

        try {

            LogUtil.logServerAccessStart(request, "ADMIN_SERVICE_ACCESS", "HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));

            if (!redirect(request, response)) {

                String name = getName(request);

                log.debug("The client requested this: " + name);


                String requestURL = request.getRequestURL().toString();

                for (HttpResponder r : responders) {
                    boolean match = r.matches(requestURL);
                    if (match) {
                        log.debug("The request URL: " + requestURL + " matches " +
                                "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                        //dsi = new BESResource(dataSource);
                        //if(dsi.isDataset()){
                        r.respondToHttpGetRequest(request, response);
                        return;
                        //}

                    }
                }


                String msg = "BAD URL - not an OPeNDAP request suffix.";
                log.error("doGet() - {}",msg);
                throw new BadRequest(msg);

            }

        } catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this, response);
            } catch (Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            LogUtil.logServerAccessEnd(request_status, "ADMIN_SERVICE_ACCESS");
            RequestCache.closeThreadCache();
        }
    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) {

        int request_status = HttpServletResponse.SC_OK;

        try {

            LogUtil.logServerAccessStart(request, "ADMIN_SERVICE_ACCESS", "HTTP-POST", Integer.toString(reqNumber.incrementAndGet()));

            if (!redirect(request, response)) {

                String name = getName(request);

                log.debug("The client requested this: " + name);

                String requestURL = request.getRequestURL().toString();

                for (HttpResponder r : responders) {
                    if (r.matches(requestURL)) {
                        log.debug("The request URL: " + requestURL + " matches " +
                                "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                        //dsi = new BESResource(dataSource);
                        //if(dsi.isDataset()){
                        r.respondToHttpPostRequest(request, response);
                        return;
                        //}

                    }
                }

                String msg = "BAD URL - not an OPeNDAP request suffix.";
                log.error("doGet() - {}", msg);
                throw new BadRequest(msg);
            }

        } catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this,  response);
            } catch (Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            LogUtil.logServerAccessEnd(request_status, "ADMIN_SERVICE_ACCESS");
            RequestCache.closeThreadCache();
        }
    }


    /**
     *
     * This override checks to see if we are in secure mode and if not send a forbidden error.
     *
     * @param req   Same as for javax.servlet.http.HttpServlet.service()
     * @param resp   Same as for javax.servlet.http.HttpServlet.service()
     * @throws javax.servlet.ServletException    Same as for javax.servlet.http.HttpServlet.service()
     * @throws java.io.IOException   Same as for javax.servlet.http.HttpServlet.service()
     */
    @Override
    protected void service(javax.servlet.http.HttpServletRequest req, javax.servlet.http.HttpServletResponse resp)
            throws javax.servlet.ServletException, java.io.IOException {

        if (!req.isSecure()) {
            String msg = "Connection is NOT secure. Protocol: "+req.getProtocol();
            log.error(msg);
            resp.sendError(403);
        }
        else {
            log.debug("Connection is secure. Protocol: "+req.getProtocol());
        }

        super.service(req,resp);



    }



}
