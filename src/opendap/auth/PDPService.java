/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2018 OPeNDAP, Inc.
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
package opendap.auth;

import opendap.PathBuilder;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.logging.LogUtil;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ndp on 11/6/14.
 */
public class PDPService extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(PDPService.class);
    private static final ReentrantLock CONFIG_LOCK = new ReentrantLock();

    private static final AtomicInteger REQ_NUMBER = new AtomicInteger(0);

    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final AtomicBoolean REQUIRE_SECURE_TRANSPORT = new AtomicBoolean(true);

    private static PolicyDecisionPoint myPDP;

    @Override
    public void init() throws ServletException {
        CONFIG_LOCK.lock();
        try {
            LOG.info("BEGIN");
            if (IS_INITIALIZED.get()) {
                LOG.info("END (Already initialized, SKIPPING.)");
                return;
            }
            super.init();
            LogUtil.initLogging(this);

            String systemPath = ServletUtil.getSystemPath(this, "");
            String configFile = getInitParameter("config");
            if(configFile==null){
                configFile = PathBuilder.pathConcat(systemPath,"/WEB-INF/conf/SimplePDP.xml");
            }
            Element config;
            try {
                config = opendap.xml.Util.getDocumentRoot(configFile);
                if(config ==null)
                    throw new ServletException("Unable to read/parse configuration file: "+configFile);

                Element e = config.getChild("PolicyDecisionPoint");
                if(e==null)
                    throw new ServletException("Configuration file: "+configFile + " is missing the " +
                            "required PolicyDecisionPoint element.");
                myPDP = PolicyDecisionPoint.pdpFactory(e);

            } catch (Exception e) {
                String msg = "Unable to ingest configuration!!!! Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
                LOG.error(msg);
                throw new ServletException(msg, e);
            }
            REQUIRE_SECURE_TRANSPORT.set(config.getChild("RequireSecureTransport") != null);
            IS_INITIALIZED.set(true);
        }
        finally {
            CONFIG_LOCK.unlock();
        }
        LOG.info("END");

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
    protected long getLastModified(HttpServletRequest req) {
        try {
            RequestCache.openThreadCache();
            long reqno = REQ_NUMBER.incrementAndGet();
            LogUtil.logServerAccessStart(req, LogUtil.PDP_SERVICE_LAST_MODIFIED_LOG_ID, "LastModified", Long.toString(reqno));
            return new Date().getTime();

        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, LogUtil.PDP_SERVICE_LAST_MODIFIED_LOG_ID);
        }
    }


    private boolean redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (req.getPathInfo() == null) {
            res.sendRedirect(Scrub.urlContent(req.getRequestURI() + "/"));
            LOG.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }


    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        doEvaluate(request, response);

    }

    @Override
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) {

        doEvaluate(request, response);

    }

    private void doEvaluate(HttpServletRequest request,
                      HttpServletResponse response)  {

        String msg = "";
        int status = HttpServletResponse.SC_FORBIDDEN;

        LogUtil.logServerAccessStart(request, LogUtil.PDP_SERVICE_ACCESS_LOG_ID, request.getMethod(), Integer.toString(REQ_NUMBER.incrementAndGet()));
        try {
            if (!redirect(request, response)) {

                String uid         = request.getParameter("uid");
                if(uid == null) uid = "";

                String authContext  = request.getParameter("authContext");
                if(authContext == null) authContext = "";

                String resourceId  = request.getParameter("resourceId");
                if(resourceId == null) resourceId = "";

                String query       = request.getParameter("query");
                if(query == null) query = "";

                String action      = request.getParameter("action");
                if(action == null) action = "GET";


                StringBuilder quadTuple = new StringBuilder();

                quadTuple.append("{ ");
                quadTuple.append("uid:\"").append(uid).append("\", ");
                quadTuple.append("authContext:\"").append(authContext).append("\", ");
                quadTuple.append("resourceId:\"").append(resourceId).append("\", ");
                quadTuple.append("query:\"").append(query).append("\", ");
                quadTuple.append("action:\"").append(action).append("\"");
                quadTuple.append(" }");
                LOG.debug("{}", quadTuple);

                if(myPDP.evaluate(uid,authContext,resourceId,query,action)){
                    status = HttpServletResponse.SC_OK;
                    response.setStatus(status);
                    ServletOutputStream sos = response.getOutputStream();
                    msg = "Yes. Affirmative. Absolutely. I do.";
                    sos.println(msg);
                    LOG.debug("ACCESS PERMITTED {}", quadTuple);

                }
                else {
                    status = HttpServletResponse.SC_FORBIDDEN;
                    response.setStatus(status);
                    ServletOutputStream sos = response.getOutputStream();
                    msg = "No. Nope. Not even.";
                    sos.println(msg);
                    LOG.debug("ACCESS DENIED {}", quadTuple);
                }
            }

        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, this,  response);
                LOG.error("The Bad Things have happened. Message: {}", t.getMessage());
            }
            catch(Throwable t2){
                // It's boned now.. Leave it be.
            }
        }
        finally {
            LogUtil.logServerAccessEnd(status, LogUtil.PDP_SERVICE_ACCESS_LOG_ID);
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

        if (REQUIRE_SECURE_TRANSPORT.get()) {
            if (!req.isSecure()) {
                LOG.error("service() - Connection is NOT secure. Protocol: {}", req.getProtocol());
                resp.sendError(403);
            } else {
                LOG.debug("service() - Connection is secure. Protocol: {}", req.getProtocol());
            }
        } else {
            LOG.debug("service() - Secure transport not enforced.  Protocol: {} Scheme: {}", req.getProtocol(), req.getScheme());

        }
        super.service(req, resp);
    }

}
