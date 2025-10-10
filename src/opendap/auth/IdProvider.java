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

import opendap.http.error.Forbidden;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static opendap.auth.IdFilter.USER_PROFILE;

/**
 * Base class for the ID Provider implementations.
 */
public abstract class IdProvider {


    public static final String AUTHORIZATION_HEADER_KEY="authorization";
    protected String authContext;
    private String description;
    protected String serviceContextPath;

    private boolean isDefaultProvider;
    private boolean useReturnToUrlPostLogout;


    public IdProvider(){
        authContext = null;
        description = "Abstract Identification Service Provider";
        isDefaultProvider = false;
        serviceContextPath = null;
        useReturnToUrlPostLogout = false;
    }

    public boolean isDefault(){ return isDefaultProvider; }

    public  String getAuthContext(){ return authContext; }
    public  void setAuthContext(String authContext){ this.authContext = authContext; }

    public  String getDescription(){ return description; }
    public  void setDescription(String d){ description = d; }

    public String getServiceContextPath(){ return serviceContextPath;}
    public void setServiceContextPath(String sc){ serviceContextPath = sc;}


    public abstract String getLoginEndpoint();

    public abstract String getLogoutEndpoint();

    public void init(Element config, String serviceContextPath) throws ConfigurationException{

        if(config == null){
            throw new ConfigurationException("init(): Configuration element may not be null.");
        }
        Element e = config.getChild("authContext");
        if(e!=null){
            setAuthContext(e.getTextTrim());
        }

        e = config.getChild("description");
        if(e!=null){
            setDescription(e.getTextTrim());
        }

        e = config.getChild("isDefault");
        if(e!=null){
            isDefaultProvider = true;
        }

        e = config.getChild("UseReturnToUrlPostLogout");
        if(e!=null){
            useReturnToUrlPostLogout = true;
        }

        setServiceContextPath(serviceContextPath);
    }

    /**
     *
     * @param request
     * @param response
     * @return True if login is complete and user profile has been added to session object. False otherwise.
     * @throws IOException
     */
    public abstract boolean doLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, Forbidden;


    public abstract boolean doTokenAuthentication(HttpServletRequest request, UserProfile userProfile) throws IOException, Forbidden ;

     /**
         * Logs a user out.
         * This method simply terminates the local session and redirects the user back
         * to the home page.
         */
    public void doLogout(HttpServletRequest request, HttpServletResponse response)
	        throws IOException
    {
        String redirectUrl = getServiceContextPath();
        HttpSession session = request.getSession(false);
        if( session != null ) {
            invalidate((UserProfile) session.getAttribute(USER_PROFILE));
            if(useReturnToUrlPostLogout) {
                String href = (String) session.getAttribute(IdFilter.RETURN_TO_URL);
                redirectUrl = href!=null?href:redirectUrl;
            }
            session.invalidate();
        }
        response.sendRedirect(redirectUrl);
    }

    /**
     * Used to invalidate any persistent user state during a
     * logout or transaction invalidation process.
     * @param userProfile
     * @throws IOException
     */
    public  void invalidate(UserProfile userProfile) throws IOException {
        // Nothing to see here folks. Moove along..
    }

}

