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

import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.logging.LogUtil;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ndp on 11/6/14.
 */
public class PDPService extends HttpServlet {

    private Logger _log;
    private String _accessLogName = "PDP_SERVICE_ACCESS";


    private ReentrantLock _configLock;

    private Element _config;


    private AtomicInteger _reqNumber;

    private String _systemPath;

    private boolean _isInitialized;

    private boolean _requireSecureTransport;



    private PolicyDecisionPoint _myPDP;

    public PDPService(){
        _isInitialized = false;
        _reqNumber = new AtomicInteger(0);
        _requireSecureTransport = true;
        _configLock = new ReentrantLock();

    }

    public void init() throws ServletException {
        super.init();
        _configLock.lock();
        try {
            initLogging();
            _log.info("init() - BEGIN");
            if (_isInitialized) {
                _log.info("init() - END (Already initialized. Nothing changed.)");
                return;
            }
            
            _systemPath = ServletUtil.getSystemPath(this, "");
            _requireSecureTransport = false;


            String configFile = getInitParameter("config");
            if(configFile==null){
                configFile = _systemPath + "/WEB-INF/conf/SimplePDP.xml";
            }
            try {
                _config = opendap.xml.Util.getDocumentRoot(configFile);
                Element e = _config.getChild("PolicyDecisionPoint");
                _myPDP = PolicyDecisionPoint.pdpFactory(e);
                e = _config.getChild("RequireSecureTransport");
                if(e !=null){
                    _requireSecureTransport = true;
                }
            } catch (Exception e) {
                String msg = "Unable to ingest configuration!!!! Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
                _log.error(msg);
                throw new ServletException(msg, e);
            }
            _isInitialized = true;
            _log.info("init() - END");
        }
        finally {
            _configLock.unlock();
        }
    }



    /**
     * Starts the logging process.
     */
    private void initLogging() {
        LogUtil.initLogging(this);
        _log = org.slf4j.LoggerFactory.getLogger(getClass());

    }

    /*
    private void load_config() throws ServletException {
        String msg;
        String configFile = getInitParameter("config");

        if(configFile==null){
            configFile = _systemPath + "/WEB-INF/conf/SimplePDP.xml";
        }

        try {
            _config = opendap.xml.Util.getDocumentRoot(configFile);

            Element e = _config.getChild("PolicyDecisionPoint");

            _myPDP = PolicyDecisionPoint.pdpFactory(e);


            e = _config.getChild("RequireSecureTransport");
            if(e !=null){
                _requireSecureTransport = true;
            }


        } catch (Exception e) {
            msg = "Unable to ingest configuration!!!! Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            _log.error(msg);
            throw new ServletException(msg, e);

        }


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



        try {
            RequestCache.openThreadCache();

            long reqno = _reqNumber.incrementAndGet();
            LogUtil.logServerAccessStart(req, _accessLogName, "LastModified", Long.toString(reqno));
            return -1;

        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, _accessLogName);
        }


    }


    private boolean redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (req.getPathInfo() == null) {
            res.sendRedirect(Scrub.urlContent(req.getRequestURI() + "/"));
            _log.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        doEvaluate(request, response);

    }

    public void doPost(HttpServletRequest request,
                      HttpServletResponse response) {

        doEvaluate(request, response);

    }

    public void doEvaluate(HttpServletRequest request,
                      HttpServletResponse response)  {

        String msg = "";
        int status = HttpServletResponse.SC_FORBIDDEN;

        LogUtil.logServerAccessStart(request,_accessLogName, request.getMethod(), Integer.toString(_reqNumber.incrementAndGet()));
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
                _log.debug("doGet() - {}", quadTuple);

                if(_myPDP.evaluate(uid,authContext,resourceId,query,action)){
                    status = HttpServletResponse.SC_OK;
                    response.setStatus(status);
                    ServletOutputStream sos = response.getOutputStream();
                    msg = "Yes. Affirmative. Absolutely. I do.";
                    sos.println(msg);
                    _log.debug("doEvaluate() - ACCESS PERMITTED {}", quadTuple);

                }
                else {
                    status = HttpServletResponse.SC_FORBIDDEN;
                    response.setStatus(status);
                    ServletOutputStream sos = response.getOutputStream();
                    msg = "No. Nope. Not even.";
                    sos.println(msg);
                    _log.debug("doEvaluate() - ACCESS DENIED {}", quadTuple);
                }
            }

        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, this,  response);
                _log.error("doEvaluate() - The Bad Things have happened. Message: {}", t.getMessage());
            }
            catch(Throwable t2){
                // It's boned now.. Leave it be.
            }
        }
        finally {
            LogUtil.logServerAccessEnd(status, _accessLogName);
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

        if (_requireSecureTransport) {
            if (!req.isSecure()) {
                _log.error("service() - Connection is NOT secure. Protocol: " + req.getProtocol());
                resp.sendError(403);
            } else {
                _log.debug("service() - Connection is secure. Protocol: " + req.getProtocol());
            }
        } else {
            _log.debug("service() - Secure transport not enforced.  Protocol: {} Scheme: {}", req.getProtocol(), req.getScheme());

        }
        super.service(req, resp);
    }

}
