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
package opendap.ngap;

import opendap.coreServlet.*;
import opendap.logging.LogUtil;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */



public class NgapServlet extends DispatchServlet   {

    private static final Logger LOG = LoggerFactory.getLogger(NgapServlet.class);
    private static NgapDispatchHandler ngapDispatchHandler;

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
        ngapDispatchHandler = new NgapDispatchHandler();

        try {
            ngapDispatchHandler.init(this,super.configDoc.getRootElement());
        } catch (Exception e) {
            throw new ServletException(e.getMessage());
        }

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
        LogUtil.logServerAccessStart(req, LogUtil.HYRAX_LAST_MODIFIED_ACCESS_LOG_ID, "LastModified", Long.toString(reqno));
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
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, LogUtil.HYRAX_LAST_MODIFIED_ACCESS_LOG_ID);
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
                LogUtil.logServerAccessStart(request, LogUtil.HYRAX_ACCESS_LOG_ID, "HTTP-GET", Long.toString(reqno));
                LOG.debug(Util.getMemoryReport());
                LOG.debug(ServletUtil.showRequest(request, reqno));
                //log.debug(AwsUtil.probeRequest(this, request));
                if(redirectForServiceOnlyRequest(request,response))
                    return;
                if (Debug.isSet("probeRequest"))
                    LOG.debug(ServletUtil.probeRequest(this, request));
                /**
                 * Do NGAP STUFF
                 */
                ngapDispatchHandler.requestDispatch(request, response, true);
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
            LogUtil.logServerAccessEnd(request_status, LogUtil.HYRAX_ACCESS_LOG_ID);
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
