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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ndp on 9/25/14.
 */
public class UrsLogin extends IdProvider{

    public static final String DEFAULT_ID="urs";

    private Logger _log;

    private String _ursUrl;
    private String _clientAppId;
    private String _clientAppAuthCode;


    public UrsLogin(){
        super();
        _log = LoggerFactory.getLogger(this.getClass());
        setId(DEFAULT_ID);
        setDescription("The NASA EOSDIS User Registration System");
    }




    public void init(Element config) throws ConfigurationException {

        if(config == null){
            throw new ConfigurationException("init(): Configuration element may not be null.");
        }

        Element e;
        String eName;


        e = config.getChild("id");
        if(e != null){
            setId(e.getTextTrim());
        }


        eName = "UrsUrl";
        e = config.getChild(eName);
        if(e == null){
            String msg = this.getClass().getSimpleName() + " configuration must contain a <"+eName+"> element.";
            _log.error("init(): {}",msg);
            throw new ConfigurationException(msg);
        }
        _ursUrl = e.getTextTrim();


        eName = "UrsClientId";
        e = config.getChild(eName);
        if(e == null){
            String msg = this.getClass().getSimpleName() + " configuration must contain a <"+eName+"> element.";
            _log.error("init(): {}",msg);
            throw new ConfigurationException(msg);
        }
        _clientAppId = e.getTextTrim();

        eName = "UrsClientAuthCode";
        e = config.getChild(eName);
        if(e == null){
            String msg = this.getClass().getSimpleName() + " configuration must contain a <"+eName+"> element.";
            _log.error("init(): {}",msg);
            throw new ConfigurationException(msg);
        }
        _clientAppAuthCode = e.getTextTrim();

    }




    public String getUrsUrl() {
        return _ursUrl;
    }

    public void setUrsUrl(String ursUrl) throws ServletException{
        if(ursUrl == null){
            String msg = "BAD CONFIGURATION - URS Login Module must be configured with a URS Service URL. (urs_url)";
            _log.error("UrsLogin() - {}",msg);
            throw new ServletException(msg);
        }

        _ursUrl = ursUrl;
    }




    public String getUrsClientAppId() {
        return _clientAppId;
    }

    public void setUrsClientAppId(String ursClientApplicationId) throws ServletException{

        if(ursClientApplicationId == null){
            String msg = "BAD CONFIGURATION - URS Login Module must be configured with a Client Application ID. (client_id)";
            _log.error("UrsLogin() - {}",msg);
            throw new ServletException(msg);
        }
        _clientAppId = ursClientApplicationId;
    }



    public String getUrsClientAppAuthCode() {
        return _clientAppAuthCode;
    }

    public void setUrsClientAppAuthCode(String ursClientAppAuthCode) throws ServletException {
        if(ursClientAppAuthCode == null){
            String msg = "BAD CONFIGURATION - URS Login Module must be configured with a Client Authorization Code. (client_auth_code)";
            _log.error("UrsLogin() - {}",msg);
            throw new ServletException(msg);
        }

        this._clientAppAuthCode = ursClientAppAuthCode;
    }







    /**
     * Performs the user login operations.
     * This method does not actually generate any output. It performs a series
     * of redirects, depending upon the current state.
     *
     * 1) If the user is already logged in, it just redirects them back to the
     *    home page.
     *
     * 2) If no 'code' query parameter is found, it will redirect the user to URS
     *    to start the authentication process.
     *
     * 3) If a 'code' query parameter is found, it assumes the call is a redirect
     *    from a successful URS authentication, and will attempt to perform the
     *    token exchange.

     *
     * @param request
     * @param response
     * @return True if login is complete and user profile has been added to session object. False otherwise.
     * @throws IOException
     * @throws ParseException
     */
	public boolean doLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ParseException
    {
        HttpSession session = request.getSession();


        /**
         * Check to see if we have a code returned from URS. If not, we must
         * redirect the user to URS to start the authentication process.
         */
        String code = request.getParameter("code");

        if( code == null )
        {
            String url = getUrsUrl() + "/oauth/authorize?client_id=" + getUrsClientAppId() +
                "&response_type=code&redirect_uri=" + request.getRequestURL();

            _log.info("URS Code Request URL: {}",url);


            response.sendRedirect(url);
            return false;
        }


        /**
         * If we get here, the the user was redirected by URS back to our application,
         * and we have a code. We now exchange the code for a token, which is
         * returned as a json document.
         */
        String url = getUrsUrl() + "/oauth/token";

        String post_data = "grant_type=authorization_code&code=" + code +
            "&redirect_uri=" + request.getRequestURL();

        Map<String, String> headers = new HashMap<String,String>();

        String authHeader = "Basic " + getUrsClientAppAuthCode();
        headers.put("Authorization", authHeader );

        _log.info("URS Token URL: {}",url);
        _log.info("URS Toke POST data: {}",post_data);
        _log.info("URS  Authorization Header: {}",authHeader);

        String contents = Util.submitHttpRequest(url, headers, post_data);


        /**
         * Parse the json to extract the token.
         */
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(contents);


        OAuthAccessToken oat = new OAuthAccessToken(json);


        /**
         * Now that we have an access token, we can retrieve the user profile. This
         * is returned as a JSON document.
         */
        url = _ursUrl + oat.getEndPoint();
        headers.put("Authorization", oat.getTokenType()+ " " + oat.getAccessToken());
        contents = Util.submitHttpRequest(url, headers, null);


        /**
         * Parse the json to extract the user id, first and last names,
         * and email address. We store these in the session. These four
         * parameters are mandatory, and will always exist in the user
         * profile.
         */
        json = (JSONObject) parser.parse(contents);

        UserProfile userProfile = new UserProfile(json);

        userProfile.setAttribute("IdProvider", getId());

        session.setAttribute("user_profile", userProfile);

        /**
         * Finally, redirect the user back to the their original requested resource.
         */

        String redirectUrl = (String) session.getAttribute("original_request_url");


        session.setAttribute("IdP",this);

        response.sendRedirect(redirectUrl);

        return true;

	}






}
