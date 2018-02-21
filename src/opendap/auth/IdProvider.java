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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Created by ndp on 9/24/14.
 */
public abstract class  IdProvider {


    protected String _authContext;
    private String _description;
    protected String _serviceContext;

    private boolean _isDefaultProvider;


    public IdProvider(){
        _authContext = null;
        _description = "Abstract Identification Service Provider";
        _isDefaultProvider = false;
        _serviceContext = null;
    }

    public boolean isDefault(){ return _isDefaultProvider; }

    public  String getAuthContext(){ return _authContext; }
    public  void setAuthContext(String authContext){ _authContext = authContext; }

    public  String getDescription(){ return _description; }
    public  void setDescription(String d){ _description = d; }

    public String getServiceContext(){ return _serviceContext;}
    public void setServiceContext(String sc){ _serviceContext = sc;}


    public abstract String getLoginEndpoint();

    public abstract String getLogoutEndpoint();

    public void init(Element config, String serviceContext) throws ConfigurationException{

        if(config == null){
            throw new ConfigurationException("init(): Configuration element may not be null.");
        }
        Element e = config.getChild("id");
        if(e!=null){
            setAuthContext(e.getTextTrim());
        }

        e = config.getChild("description");
        if(e!=null){
            setDescription(e.getTextTrim());
        }

        e = config.getChild("isDefault");
        if(e!=null){
            _isDefaultProvider = true;
        }

        _serviceContext = serviceContext;
    }

    /**
     *
     * @param request
     * @param response
     * @return True if login is complete and user profile has been added to session object. False otherwise.
     * @throws Exception
     */
    public abstract boolean doLogin(HttpServletRequest request, HttpServletResponse response) throws Exception;


    /**
     * Logs a user out.
     * This method simply terminates the local session and redirects the user back
     * to the home page.
     */
    public void doLogout(HttpServletRequest request, HttpServletResponse response)
	        throws IOException
    {
        String redirectUrl = request.getContextPath();
        HttpSession session = request.getSession(false);
        if( session != null )
        {
            String href = (String) session.getAttribute(IdFilter.ORIGINAL_REQUEST_URL);
            redirectUrl = href!=null?href:redirectUrl;
            session.invalidate();
        }
        response.sendRedirect(redirectUrl);
    }

}
