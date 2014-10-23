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

/**
 * Created by ndp on 10/7/14.
 */
public class ShibbolethLogin extends IdProvider {


    public static final String DEFAULT_ID="shib";
    public static final String DEFAULT_LOGOUT_LOCATION = "/Shibboleth.sso/Logout";


    private Logger _log;


    public ShibbolethLogin(){
        super();
        _log = LoggerFactory.getLogger(this.getClass());

        setId(DEFAULT_ID);
        setLogoutLocation(DEFAULT_LOGOUT_LOCATION);
        setDescription("Shibboleth Authentication");
    }



    @Override
    public void init(Element config) throws ConfigurationException {

        _log.info("init(): Initializing {}. login context: {}",getDescription(),getLoginContext());

    }

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
        HttpSession session = request.getSession();
        String redirectUrl = request.getContextPath();
        if(session!=null){
            String url = (String) session.getAttribute("original_request_url");
            if(url != null) {
                redirectUrl = url;
            }
        }

        String protocol = request.getScheme();


        if(protocol.equalsIgnoreCase("https")){
            redirectUrl = redirectUrl.replace("http://","https://");
            redirectUrl = redirectUrl.replace(":8080/",":8443/");
        }


        session.setAttribute("IdP",this);


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
