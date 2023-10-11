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
import opendap.coreServlet.ServletUtil;
import opendap.logging.ServletLogUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.security.Principal;


/**
 * Creates a policy enforcement point (PEP) for this server.
 */
public class PEPFilter implements Filter {

    private Logger log;
    private PolicyDecisionPoint pdp;
    private boolean everyOneMustHaveUid;
    private String unauthorizedMsg = "We don't know who you are! Login and let us know, and then maybe you can have what you want.";

    private boolean isInitialized;
    private FilterConfig filterConfig;

    private static final String configParameterName = "config";
    private static final String defaultConfigFileName = "user-access.xml";

    public PEPFilter() {
        everyOneMustHaveUid = false;
        isInitialized = false;
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        try {
            init();
        }
        catch (IOException | JDOMException se){
            log.warn("init() - INITIALIZATION HAS BEEN POSTPONED! FAILED TO INITIALIZE PEPFilter! " +
                    "Caught {} Message: {} ",se.getClass().getName(),se.getMessage());

        }

    }
    private void init() throws IOException, JDOMException {

        if(isInitialized)
            return;
        ServletLogUtil.initLogging(filterConfig.getServletContext());
        log = LoggerFactory.getLogger(this.getClass());
        log.info("init() - Initializing PEPFilter...");

        String configFileName = filterConfig.getInitParameter(configParameterName);
        if(configFileName==null){
            configFileName = defaultConfigFileName;
            String msg = "init() - The web.xml configuration for "+getClass().getName()+
                    " does not contain an init-parameter named \""+ configParameterName +"\" " +
                    "Using the DEFAULT name: "+configFileName;
            log.warn(msg);
        }

        String configDirName = ServletUtil.getConfigPath(filterConfig.getServletContext());
        File configFile = new File(configDirName,configFileName);
        Element config;
        config = opendap.xml.Util.getDocumentRoot(configFile);

        Element e = config.getChild("PolicyDecisionPoint");
        pdp = PolicyDecisionPoint.pdpFactory(e);

        e = config.getChild("EveryOneMustHaveId");
        if(e !=null){
            everyOneMustHaveUid = true;
        }

        isInitialized = true;
        log.info("init() - PEPFilter HAS BEEN INITIALIZED!");
    }







    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws  ServletException {

        if(!isInitialized) {
            try {
                init();
            }
            catch (IOException | JDOMException  e){
                String msg = "doFilter() - PEPFilter INITIALIZATION HAS FAILED! " +
                        "Caught "+ e.getClass().getName() + " Message: " + e.getMessage();
                log.error(msg);
                OPeNDAPException.setCachedErrorMessage(msg);
                throw new ServletException(msg,e);
            }
        }
        try {

            RequestCache.openThreadCache();


            HttpServletRequest  hsReq = (HttpServletRequest)  request;
            HttpServletResponse hsRes = (HttpServletResponse) response;

            // If they are authenticated then we should be able to get the remoteUser() or UserPrinciple
            String userId = null;
            String authContext = null;
            HttpSession session = hsReq.getSession(false);
            if(session!=null){
                UserProfile userProfile = (UserProfile) session.getAttribute(IdFilter.USER_PROFILE);
                if(userProfile!=null){
                    userId = userProfile.getUID();
                    IdProvider idP = userProfile.getIdP();
                    authContext = idP.getAuthContext();
                }
            }
            if(userId==null) {
                userId = Util.getUID(hsReq);
                // @FIXME Deal with authContext for Tomcat and Apache
                //   httpd authenticated users
            }

            // So - Do they have to be authenticated?
            if (userId == null && everyOneMustHaveUid) {
                if (IdPManager.hasDefaultProvider()) {
                    hsRes.sendRedirect(IdPManager.getDefaultProvider().getLoginEndpoint());
                } else {
                    OPeNDAPException.setCachedErrorMessage(unauthorizedMsg);
                    hsRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
                return;
            }

            // Are they allowed access?
            if (requestIsGranted(userId, authContext, hsReq)) {
                // Yup, so we just move along...
                filterChain.doFilter(hsReq, hsRes);
            } else {
                // Access was denied, so...
                if (userId == null) {
                    // If they aren't logged in then we tell them to do that
                    if (IdPManager.hasDefaultProvider()) {
                        hsRes.sendRedirect(IdPManager.getDefaultProvider().getLoginEndpoint());
                    } else {
                        OPeNDAPException.setCachedErrorMessage(unauthorizedMsg);
                        hsRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                } else {
                    // If they are logged in then we tell them NO.
                    OPeNDAPException.setCachedErrorMessage("I'm Sorry " + userId + ", But I'm Afraid You Can't Do That.");
                    hsRes.sendError(HttpServletResponse.SC_FORBIDDEN);
                }
            }
        }
        catch(IOException e){
            OPeNDAPException.setCachedErrorMessage(e.getMessage());
            throw new ServletException(e.getMessage(),e);
        }
        finally{
            RequestCache.closeThreadCache();

        }

    }

    @Override
    public void destroy() {
        log = null;
    }


    public boolean requestIsGranted(String userId, String authContext,  HttpServletRequest request){

        if(userId == null){
            userId = "";
        }
        String resourceId   = request.getRequestURI();
        String queryString  = request.getQueryString();
        if(queryString == null){
            queryString = "";
        }
        String action       = request.getMethod();
        return pdp.evaluate(userId, authContext, resourceId,queryString,action);
    }

}
