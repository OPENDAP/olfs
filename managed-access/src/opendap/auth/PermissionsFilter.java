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

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.Iterator;


/**
 * Creates a policy enforcement point (PEP) for this server.
 */
public class PermissionsFilter implements Filter {

    private Logger _log;


    private PolicyDecisionPoint _pdp;

    private boolean _everyOneMustHaveProfile;







    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        _log = LoggerFactory.getLogger(filterConfig.getFilterName());

        _everyOneMustHaveProfile = false;

        String msg;

        String configFile = filterConfig.getInitParameter("config");

        Element config;

        try {
            config = opendap.xml.Util.getDocumentRoot(configFile);

            Element e = config.getChild("PolicyDecisionPoint");

            _pdp = pdpFactory(e);


        } catch (Exception e) {
            msg = "Unable to ingest configuration!!!! Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            _log.error(msg);
            throw new ServletException(msg, e);

        }



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


        HttpServletRequest  hsReq = (HttpServletRequest)  request;
        HttpServletResponse hsRes = (HttpServletResponse) response;


        String userId = null;
        // String remoteUser = hsReq.getRemoteUser();
        Principal userPrinciple = hsReq.getUserPrincipal();
        if(userPrinciple != null) {
            userId = userPrinciple.getName();
        }


        if(userId == null  && _everyOneMustHaveProfile) {
            hsRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }


        if(requestIsGranted(userId, hsReq)){
            filterChain.doFilter(hsReq, hsRes);
        }
        else {
            if(userId == null)
                hsRes.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            else
                hsRes.sendError(HttpServletResponse.SC_FORBIDDEN);
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
