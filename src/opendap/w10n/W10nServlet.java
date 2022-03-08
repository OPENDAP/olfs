/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.w10n;

import opendap.coreServlet.ServletUtil;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.LicenseManager;
import opendap.coreServlet.Util;
import opendap.coreServlet.Debug;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.Scrub;
import opendap.logging.ServletLogUtil;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import opendap.services.ServicesRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class W10nServlet extends HttpServlet   {

    private static final Logger LOG = LoggerFactory.getLogger(W10nServlet.class);
    private static final W10nResponder W10N_RESPONDER = new W10nResponder();

    /**
     * ************************************************************************
     * A thread safe hit counter
     *
     * @serial
     */
    private static final AtomicInteger _reqNumber =  new AtomicInteger(0);

    /**
     * ************************************************************************
     *
     * @throws javax.servlet.ServletException
     */
    @Override
    public void init() throws ServletException {
        super.init();
        W10N_RESPONDER.setSystemPath(ServletUtil.getSystemPath(this,""));
        W10nService w10nService = new W10nService();
        w10nService.init(this,null);
        ServicesRegistry.addService(w10nService);
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
        RequestCache.openThreadCache();
        long reqno = _reqNumber.incrementAndGet();
        ServletLogUtil.logServerAccessStart(req, ServletLogUtil.HYRAX_LAST_MODIFIED_ACCESS_LOG_ID, "LastModified", Long.toString(reqno));
        long lmt = new Date().getTime();
        Procedure timedProcedure = Timer.start();
        try {
            if (ReqInfo.isServiceOnlyRequest(req)) {
                return lmt;
            }
            // @TODO Create a meaningful implementation of getLastModified for w10n service.
        } catch (Exception e) {
            LOG.error("Caught: {}  Message: {} ", e.getClass().getName(), e.getMessage());
        } finally {
            ServletLogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, ServletLogUtil.HYRAX_LAST_MODIFIED_ACCESS_LOG_ID);
            Timer.stop(timedProcedure);
        }
        return lmt;
    }

    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        int request_status = HttpServletResponse.SC_OK;
        try {
            Procedure timedProc = Timer.start();
            try {
                if(LicenseManager.isExpired(request)){
                    LicenseManager.sendLicenseExpiredPage(request,response);
                    return;
                }
                RequestCache.openThreadCache();

                int reqno = _reqNumber.incrementAndGet();
                ServletLogUtil.logServerAccessStart(request, ServletLogUtil.HYRAX_ACCESS_LOG_ID, "HTTP-GET", Long.toString(reqno));
                LOG.debug(Util.getMemoryReport());
                LOG.debug(ServletUtil.showRequest(request, reqno));
                //log.debug(AwsUtil.probeRequest(this, request));
                if(redirectForServiceOnlyRequest(request,response))
                    return;
                if (Debug.isSet("probeRequest"))
                    LOG.debug(ServletUtil.probeRequest(this, request));
                /**
                 * Do w10n STUFF
                 */
                W10N_RESPONDER.sendW10NResponse(request, response);
            }
            finally {
                LOG.info("doGet(): Response completed.\n");
                Timer.stop(timedProc);
            }
        }
        catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this,  response);
            }
            catch(Throwable t2) {
                try {
                    String msg = "\n########################################################\n" +
                            "Request processing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error log attempt for this request.\n" +
                            "########################################################\n{}";
                    LOG.error(msg,t2);
                }
                catch(Throwable t3){
                    // It's boned now.. Leave it be.
                }
            }
        }
        finally {
            ServletLogUtil.logServerAccessEnd(request_status, ServletLogUtil.HYRAX_ACCESS_LOG_ID);
            RequestCache.closeThreadCache();
        }

        LOG.info(Timer.report());
        Timer.reset();
    }

    //**************************************************************************
    private boolean redirectForServiceOnlyRequest(HttpServletRequest req,
                                                  HttpServletResponse res)
            throws IOException {
        // String localUrl = ReqInfo.getLocalUrl(req);
        // @SuppressWarnings("unchecked")
        // Map<String,String[]> queryParameters = req.getParameterMap();
        ServletUtil.probeRequest(this, req);
        if (ReqInfo.isServiceOnlyRequest(req)) {
            String reqURI = req.getRequestURI();
            String newURI = reqURI+"/";
            res.sendRedirect(Scrub.urlContent(newURI));
            LOG.debug("Sent redirectForServiceOnlyRequest to map the servlet context to a URL that ends in a '/' character!");
            return true;
        }
        return false;
    }
    //**************************************************************************


}
