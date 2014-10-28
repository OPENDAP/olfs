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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URL;

/**
 * Created by ndp on 10/7/14.
 */
public class ShibbolethIdP extends IdProvider {


    public static final String DEFAULT_ID="shib";
    /**
     * Default service point for the mod_shib Logout
     */
    public static final String DEFAULT_LOGOUT_LOCATION = "/Shibboleth.sso/Logout";

    /**
     * Default service point for the mod_shib Login
     */
    public static final String DEFAULT_LOGIN_LOCATION = "/Shibboleth.sso/Login";


    /**
     * Service point for the mod_shib Login
     */
    protected String _loginLocation;


    /**
     * Service point for the mod_shib Login
     */
    protected String _logoutLocation;


    private Logger _log;


    public ShibbolethIdP(){
        super();
        _log = LoggerFactory.getLogger(this.getClass());

        setId(DEFAULT_ID);
        setDescription("Shibboleth Identity Provider");

        _loginLocation = DEFAULT_LOGIN_LOCATION;
        _logoutLocation = DEFAULT_LOGOUT_LOCATION;
    }



    @Override
    public void init(Element config) throws ConfigurationException {

        super.init(config);

        Element e = config.getChild("ShibLogin");
        if(e!=null){
            setLoginLocation(e.getTextTrim());
        }


        e = config.getChild("ShibLogout");
        if(e!=null){
            setLogoutLocation(e.getTextTrim());
        }


    }




    public void setLogoutLocation(String logoutLocation){  _logoutLocation =  logoutLocation; }
    public String getLogoutLocation(){ return _logoutLocation; }
    public void setLoginLocation(String loginLocation){  _loginLocation =  loginLocation; }
    public String getLoginLocation(){ return _loginLocation; }


    /**
     * @param request
     * @param response
     * @return True if login is complete and user profile has been added to session object. False otherwise.
     * @throws Exception
     */
    @Override
    public boolean doLogin(HttpServletRequest request, HttpServletResponse response) throws Exception {

        /**
         * Redirect the user back to the their original requested resource.
         */
        HttpSession session = request.getSession(false);
        String redirectUrl = request.getContextPath();


        /*
        // Do they already have a session?
        if(session!=null){
            // Yes, yes they do. Let's try to send them back from whence they came.
            String url = (String) session.getAttribute(IdFilter.ORIGINAL_REQUEST_URL);
            if(url != null) {
                redirectUrl = url;
            }
            Object idp = session.getAttribute("IdP");
            if(idp==null)
                session.setAttribute("IdP",this);
        }
        else if (request.getRemoteUser()==null) {
            // Hmmm... No session, and they haven't logged in.
            // Now if Apache httpd had been configured to 'require' a valid-user or shib-session  (or LDAP,
            // or whatever authentication at the httpd level) then this getRemoteUser() will return a valid
            // String. The null indicates that we managed to skip Apache authentication. So we punt and
            // set the redirectUrl to the httpd module mod_shib location from our configuration.
            redirectUrl = getLoginLocation();
        }


        if(!redirectUrl.startsWith("http")) {
            StringBuilder url = new StringBuilder();
            url.append("https://").append(request.getServerName()).append(redirectUrl);
            redirectUrl = url.toString();
        }
        else {
            if(!redirectUrl.startsWith("https")){
                redirectUrl = redirectUrl.replaceFirst("http://","https://");
                redirectUrl = redirectUrl.replaceFirst(":8080/","/");
                redirectUrl = redirectUrl.replaceFirst(":80/","/");
            }
        }

        */




        if (request.getRemoteUser()==null) {
            // Hmmm... The user has not logged in.


            // Now if Apache httpd had been configured to 'require' a valid-user or shib-session  (or LDAP,
            // or whatever authentication at the httpd level) then this getRemoteUser() will return a valid
            // String. The null indicates that we managed to skip Apache authentication. So we punt and
            // set the redirectUrl to the httpd module mod_shib location from our configuration.
            redirectUrl = getLoginLocation();


            // Do they have a session?
            if(session==null) {
                _log.error("doLogin() - No remoteUSer and no current session, creating new session.");
                //Oddly not, ok make them one..
                session = request.getSession(true);

            }

            // Now we stash ourselves for Logout purposes..
            session.setAttribute("IdP",this);


            // TODO Is this the correct thing or should we simply punt and throw a ConfigurationException?



        }
        else {
            // We have a user - do we log them out and start a shibboleth login?
            // No, for now we just try to bounce them back to IdFilter.ORIGINAL_REQUEST_URL


            // TODO How do I reliably know if the user has been shib authenticated?   Do we care?
            // TODO How can we stash our self w our custom logout method without breaking another IdP?







            // Do they have a session?

            if(session==null) {
                _log.error("doLogin() - No current session, creating new session.");
                //Oddly not, ok make them one..
                session = request.getSession(true);

            }
            else {
                _log.info("doLogin() - User has Session. id: {}",session.getId());

                // Let's inspect the attributes.
                String eppn = (String) session.getAttribute("eppn");
                _log.info("doLogin() - HttpSession Attribute eppn: {}",eppn);

                eppn = (String) request.getAttribute("eppn");

                _log.info("doLogin() - HttpRequest Attribute eppn: {}",eppn);



            }
            // We need to capture the original redirect url if there is one,
            // and then invalidate the session and then start a new one before we send them
            // off to authenticate.
            redirectUrl = (String) session.getAttribute(IdFilter.ORIGINAL_REQUEST_URL);

            if(redirectUrl==null){
                // Unset? Punt...
                redirectUrl = request.getContextPath();
            }

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
        HttpSession session = request.getSession(false);
        if( session != null )
        {
            session.invalidate();
        }

        response.sendRedirect(getLogoutLocation());
    }

}
