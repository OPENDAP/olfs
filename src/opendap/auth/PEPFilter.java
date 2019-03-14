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
import opendap.coreServlet.ServletUtil;
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

    private Logger _log;
    private PolicyDecisionPoint _pdp;
    private boolean _everyOneMustHaveUid;
    private String _unauthorizedMsg = "We don't know who you are! Login and let us know, and then maybe you can have what you want.";

    private boolean _is_initialized;
    private FilterConfig _filterConfig;
    //private String _defaultLogingEndpoint;
    private static final String _configParameterName = "config";
    private static final String _defaultConfigFileName = "user-access.xml";

    public PEPFilter() {
        _everyOneMustHaveUid = false;
        //_defaultLogingEndpoint=null;
        _is_initialized = false;
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        _filterConfig = filterConfig;
        _log = LoggerFactory.getLogger(_filterConfig.getFilterName());
        try {
            init();
        }
        catch (IOException | JDOMException se){
            _log.warn("init() - INITIALIZATION HAS BEEN POSTPONED! FAILED TO INITIALIZE PEPFilter! " +
                    "Caught {} Message: {} ",se.getClass().getName(),se.getMessage());

        }

    }
    public void init() throws IOException, JDOMException, ConfigurationException {

        if(_is_initialized)
            return;
        _log.info("init() - Initializing PEPFilter...");

        String configFileName = _filterConfig.getInitParameter(_configParameterName);
        if(configFileName==null){
            configFileName = _defaultConfigFileName;
            String msg = "init() - The web.xml configuration for "+getClass().getName()+
                    " does not contain an init-parameter named \""+_configParameterName+"\" " +
                    "Using the DEFAULT name: "+configFileName;
            _log.warn(msg);
        }

        String configDirName = ServletUtil.getConfigPath(_filterConfig.getServletContext());
        File configFile = new File(configDirName,configFileName);
        Element config;
        config = opendap.xml.Util.getDocumentRoot(configFile);

        Element e = config.getChild("PolicyDecisionPoint");
        _pdp = PolicyDecisionPoint.pdpFactory(e);

        e = config.getChild("EveryOneMustHaveId");
        if(e !=null){
            _everyOneMustHaveUid = true;
        }

        _is_initialized = true;
        _log.info("init() - PEPFilter HAS BEEN INITIALIZED!");
    }







    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws  ServletException {

        if(!_is_initialized) {
            try {
                init();
            }
            catch (IOException | JDOMException  e){
                String msg = "doFilter() - PEPFilter INITIALIZATION HAS FAILED! " +
                        "Caught "+ e.getClass().getName() + " Message: " + e.getMessage();
                _log.error(msg);
                OPeNDAPException.setCachedErrorMessage(msg);
                throw new ServletException(msg,e);
            }
        }


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
                IdProvider ipd = userProfile.getIdP();
                authContext = ipd.getAuthContext();
            }
        }

        if(userId==null) {
            String remoteUser = hsReq.getRemoteUser();
            if (remoteUser == null) {
                Principal userPrinciple = hsReq.getUserPrincipal();
                if (userPrinciple != null) {
                    userId = userPrinciple.getName();
                }
            } else {
                userId = remoteUser;
            }
            // @FIXME Deal with authContext for Tomacat and APache httpd authenticated users
        }

        try {
            // So - Do they have to be authenticated?
            if (userId == null && _everyOneMustHaveUid) {
                if (IdPManager.hasDefaultProvider()) {
                    hsRes.sendRedirect(IdPManager.getDefaultProvider().getLoginEndpoint());
                } else {
                    OPeNDAPException.setCachedErrorMessage(_unauthorizedMsg);
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
                        OPeNDAPException.setCachedErrorMessage(_unauthorizedMsg);
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
    }

    @Override
    public void destroy() {
        _log = null;
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
        return _pdp.evaluate(userId, authContext, resourceId,queryString,action);
    }

}
