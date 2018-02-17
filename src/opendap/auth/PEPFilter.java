/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2014 OPeNDAP, Inc.
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
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    private String _defaultLogingEndpoint;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        _filterConfig = filterConfig;
        _log = LoggerFactory.getLogger(_filterConfig.getFilterName());
        _everyOneMustHaveUid = false;
        _defaultLogingEndpoint=null;
        _is_initialized = false;


        try {
            init();
        }
        catch (IOException | JDOMException | ConfigurationException se){
            _log.warn("init() - INITIALIZATION HAS BEEN POSTPONED! FAILED TO INITIALIZE PEPFilter! " +
                    "Caught {} Message: {} ",se.getClass().getName(),se.getMessage());

        }

    }
    public void init() throws IOException, JDOMException, ConfigurationException {

        if(_is_initialized)
            return;
        _log.info("init() - Initializing PEPFilter...");

        String configFile = _filterConfig.getInitParameter("config");
        Element config;
        config = opendap.xml.Util.getDocumentRoot(configFile);
        Element e = config.getChild("PolicyDecisionPoint");
        _pdp = pdpFactory(e);
        e = config.getChild("EveryOneMustHaveId");
        if(e !=null){
            _everyOneMustHaveUid = true;
        }
        e = config.getChild("UseDefaultLoginEndpoint");
        if(e !=null){
            String href = e.getAttributeValue("href");
            if(href!=null){
                _defaultLogingEndpoint = PathBuilder.pathConcat(_filterConfig.getServletContext().getContextPath(),href);
                _log.info("init() - Using Default Login Endpoint: {}",_defaultLogingEndpoint);
            }
            else {
                _log.error("init() - The configuration parameter UseDefaultLoginEndpoint is missing the " +
                        "required href attribute! UseDefaultLoginEndpoint is DISABLED.");
            }
        }
        _is_initialized = true;
        _log.info("init() - PEPFilter HAS BEEN INITIALIZED!");
    }


    public PolicyDecisionPoint pdpFactory(Element config) throws ConfigurationException {
        String msg;
        if(config==null) {
            msg = "Configuration MAY NOT be null!.";
            _log.error("pdpFactory():  {}",msg);
            throw new ConfigurationException(msg);
        }
        String pdpClassName = config.getAttributeValue("class");
        if(pdpClassName==null) {
            msg = "PolicyDecisionPoint definition must contain a \"class\" attribute whose value is the class name of the PolicyDecisionPoint implementation to be created.";
            _log.error("pdpFactory(): {}",msg);
            throw new ConfigurationException(msg);
        }
        try {
            _log.debug("pdpFactory(): Building PolicyDecisionPoint: " + pdpClassName);
            Class classDefinition = Class.forName(pdpClassName);
            PolicyDecisionPoint pdp = (PolicyDecisionPoint) classDefinition.newInstance();
            pdp.init(config);
            return pdp;
        } catch (Exception e) {
            msg = "Unable to manufacture an instance of "+pdpClassName+"  Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            _log.error("pdpFactory(): {}"+msg);
            throw new ConfigurationException(msg, e);
        }
    }





    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        if(!_is_initialized) {
            try {
                init();
            }
            catch (IOException | JDOMException | ConfigurationException e){
                String msg = "doFilter() - PEPFilter INITIALIZATION HAS FAILED! " +
                        "Caught "+ e.getClass().getName() + " Message: " + e.getMessage();
                _log.error(msg);
                throw new ServletException(msg,e);
            }
        }


        HttpServletRequest  hsReq = (HttpServletRequest)  request;
        HttpServletResponse hsRes = (HttpServletResponse) response;

        // If they are authenticated then we should be able to get the remoteUser() or UserPrinciple
        String userId = null;
        String remoteUser = hsReq.getRemoteUser();
        if(remoteUser == null) {
            Principal userPrinciple = hsReq.getUserPrincipal();
            if (userPrinciple != null) {
                userId = userPrinciple.getName();
            }
        }
        else {
            userId = remoteUser;
        }

        // So - Do they have to be authenticated?
        if(userId == null  && _everyOneMustHaveUid) {
            if(_defaultLogingEndpoint!=null) {
                hsRes.sendRedirect(_defaultLogingEndpoint);
            }
            else {
                OPeNDAPException.setCachedErrorMessage(_unauthorizedMsg);
                hsRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return;
        }
        // Are they allowed access?
        if(requestIsGranted(userId, hsReq)){
            // Yup, so we just move along...
            filterChain.doFilter(hsReq, hsRes);
        }
        else {
            // Access was denied, so...
            if(userId == null) {
                // If they aren't logged in then we tell them to do that
                if(_defaultLogingEndpoint!=null) {
                    hsRes.sendRedirect(_defaultLogingEndpoint);
                }
                else {
                    OPeNDAPException.setCachedErrorMessage(_unauthorizedMsg);
                    hsRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                }
            }
            else {
                // If they are logged in then we tell them NO.
                OPeNDAPException.setCachedErrorMessage("I'm Sorry "+userId+", But I'm Afraid You Can't Do That.");
                hsRes.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    @Override
    public void destroy() {
        _log = null;
    }


    public boolean requestIsGranted(String userId, HttpServletRequest request){

        if(userId == null){
            userId = "";
        }
        String resourceId   = request.getRequestURI();
        String queryString  = request.getQueryString();
        if(queryString == null){
            queryString = "";
        }
        String action       = request.getMethod();
        return _pdp.evaluate(userId,resourceId,queryString,action);
    }

}
