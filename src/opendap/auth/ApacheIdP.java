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
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Created by ndp on 10/7/14.
 */
public class ApacheIdP extends IdProvider {


    public static final String DEFAULT_AUTHENTICATION_CONTEXT="apache";

    /**
     * Default service point for the mod_shib Logout
     */
    public static final String DEFAULT_LOGOUT_LOCATION = "/Logout";

    /**
     * Default service point for the mod_shib Login
     */
    public static final String DEFAULT_LOGIN_LOCATION = "/Login";

    private String _loginEndpoint;
    private String _logoutEndpoint;



    private Logger _log;


    public ApacheIdP(){
        super();
        _log = LoggerFactory.getLogger(this.getClass());

        setAuthContext(DEFAULT_AUTHENTICATION_CONTEXT);
        setDescription("Apache Identity Provider");

        _loginEndpoint = DEFAULT_LOGIN_LOCATION;
        _logoutEndpoint = DEFAULT_LOGOUT_LOCATION;

    }



    @Override
    public void init(Element config, String serviceContext) throws ConfigurationException {
        super.init(config,serviceContext);

        Element e = config.getChild("login");
        if(e!=null){
            _loginEndpoint= e.getTextTrim();
        }

        e = config.getChild("logout");
        if(e!=null){
            _logoutEndpoint = e.getTextTrim();
        }
    }




    /**
     * @param request
     * @param response
     * @return True if login is complete and user profile has been added to session object. False otherwise.
     * @throws Exception
     */
    @Override
    public boolean doLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {

        /**
         * Redirect the user back to the their original requested resource.
         */
        HttpSession session = request.getSession(false);
        String redirectUrl = null;

        String uid = request.getRemoteUser();

        if (uid==null) {

            // Hmmm... The user has not logged in.

            StringBuilder msg = new StringBuilder();
            msg.append("User should be logged in (and is not) in order to be able to access this page. This is probably the ")
                    .append("result of a failed Security configuration element in Apache."
                    );

            _log.error("doLogin() - OUCH! {}",msg.toString());
            OPeNDAPException.setCachedErrorMessage(msg.toString());
            throw new ConfigurationException(msg.toString());
        }
        else {
            // We have a user - so let's make sure they have a profile,
            // and then we just try to bounce them back to IdFilter.RETURN_TO_URL

            _log.info("doLogin() - User has uid: {}", uid);
            /*

            // Do they have a profile?
            UserProfile up = (UserProfile) session.getAttribute(IdFilter.USER_PROFILE);
            if (up == null){
                // Nope. Make one.
                up = new UserProfile();
                up.setIdP(this);
                up.setAttribute("uid", uid);
            }
            session.setAttribute(IdFilter.USER_PROFILE, up);

            */
            redirectUrl = (String) session.getAttribute(IdFilter.RETURN_TO_URL);
        }
        if(redirectUrl==null){
            redirectUrl = request.getContextPath();
        }
        _log.info("doLogin(): redirecting to {}",redirectUrl);
        response.sendRedirect(redirectUrl);
        return true;
    }


    /**
     * Logs a user out.
     * This method simply terminates the local session and redirects the user back
     * to the home page.
     */
    public void doLogout(HttpServletRequest request, HttpServletResponse response)
	        throws IOException
    {
        String redirectUrl = getServiceContext();
        HttpSession session = request.getSession(false);
        if( session != null )
        {
            String returnToUrl =  (String) session.getAttribute(IdFilter.RETURN_TO_URL);
            if(returnToUrl!=null)
                redirectUrl =returnToUrl;
            session.invalidate();
        }

        response.sendRedirect(redirectUrl);
    }

    @Override
    public String getLoginEndpoint(){
        return _loginEndpoint;
    }


    @Override
    public String getLogoutEndpoint() {
        return _logoutEndpoint;
    }


}
