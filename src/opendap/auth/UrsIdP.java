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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import opendap.PathBuilder;
import opendap.logging.ServletLogUtil;
import org.jdom.Element;
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
public class UrsIdP extends IdProvider{

    public static final String DEFAULT_AUTH_CONTEXT="urs";
    public static final String AUTHORIZATION_HEADER_KEY="authorization";
    public static final String OAUTH_USER_ID_ENDPOINT_PATH="/oauth/tokens/user";

    private Logger log;

    private String ursUrl;
    private String clientAppId;
    private String clientAppAuthCode;

    private static final String ERR_PRFX = "ERROR! msg: ";


    public UrsIdP(){
        super();
        log = LoggerFactory.getLogger(this.getClass());
        setAuthContext(DEFAULT_AUTH_CONTEXT);
        setDescription("The NASA Earthdata Login (formerly known as URS)");
    }


    @Override
    public void init(Element config, String serviceContext) throws ConfigurationException {
        super.init(config, serviceContext);

        Element e;

        e = getConfigElement(config,"UrsUrl");
        ursUrl = e.getTextTrim();

        e = getConfigElement(config,"UrsClientId");
        clientAppId = e.getTextTrim();

        e = getConfigElement(config,"UrsClientAuthCode");
        clientAppAuthCode = e.getTextTrim();

    }


    /**
     * Just a little worker method.
     * @param config
     * @param childName
     * @return
     * @throws ConfigurationException
     */
    private Element getConfigElement(Element config, String childName) throws ConfigurationException {
        Element e = config.getChild(childName);
        if(e == null){
            String msg = this.getClass().getSimpleName() + " configuration must contain a <" + childName + "> element.";
            log.error("{}",msg);
            throw new ConfigurationException(msg);
        }
        return e;
    }



    public String getUrsUrl() {
        return ursUrl;
    }

    public void setUrsUrl(String ursUrl) throws ServletException{
        if(ursUrl == null){
            String msg = "BAD CONFIGURATION - URS IdP Module must be configured with a URS Service URL. (urs_url)";
            log.error("{}{}", ERR_PRFX,msg);
            throw new ServletException(msg);
        }

        this.ursUrl = ursUrl;
    }




    public String getUrsClientAppId() {
        return clientAppId;
    }

    public void setUrsClientAppId(String ursClientApplicationId) throws ServletException{

        if(ursClientApplicationId == null){
            String msg = "BAD CONFIGURATION - URS IdP Module must be configured with a Client Application ID. (client_id)";
            log.error("{}{}", ERR_PRFX,msg);
            throw new ServletException(msg);
        }
        clientAppId = ursClientApplicationId;
    }



    public String getUrsClientAppAuthCode() {
        return clientAppAuthCode;
    }

    public void setUrsClientAppAuthCode(String ursClientAppAuthCode) throws ServletException {
        if(ursClientAppAuthCode == null){
            String msg = "BAD CONFIGURATION - URS IdP Module must be configured with a Client Authorization Code. (client_auth_code)";
            log.error("{}{}", ERR_PRFX,msg);
            throw new ServletException(msg);
        }

        this.clientAppAuthCode = ursClientAppAuthCode;
    }



    void getEDLUserProfile(UserProfile userProfile, String endpoint, String tokenType, String accessToken ) throws IOException {
        Map<String, String> headers = new HashMap<>();
        // Now that we have an access token, we can retrieve the user profile. This
        // is returned as a JSON document.
        String url = PathBuilder.pathConcat(ursUrl, endpoint) + "?client_id=" + getUrsClientAppId();
        String authHeader = tokenType + " " + accessToken;
        headers.put("Authorization", authHeader);

        log.info("URS User Profile Request URL: {}", url);
        log.info("URS User Profile Request Authorization Header: {}", authHeader);

        String contents = Util.submitHttpRequest(url, headers, null);

        log.info("URS User Profile: {}", contents);

        userProfile.ingestJsonProfileString(contents);
    }

// curl -X POST -d 'token=<token>&client_id=<‘your application client_id’> https://urs.earthdata.nasa.gov/oauth/tokens/user




    /**
     * Old Way:
     * curl -X POST -d 'token=<token>&client_id=<‘your application client_id’> https://urs.earthdata.nasa.gov/oauth/tokens/user
     *
     * New Way:
     * curl -X POST -d 'token=<token>’ -H ‘Authorization: ‘Basic <base64appcreds>’ https://urs.earthdata.nasa.gov/oauth/tokens/user
     *
     *
     * @param accessToken
     * @return
     */
    String getEdlUserId(String accessToken) throws IOException {

        Map<String, String> headers = new HashMap<>();
        String url = PathBuilder.pathConcat(getUrsUrl(),OAUTH_USER_ID_ENDPOINT_PATH);

        StringBuilder post_body= new StringBuilder();
        post_body.append("token=").append(accessToken);
        String auth_header_value="Basic "+ getUrsClientAppAuthCode();
        headers.put("Authorization",auth_header_value);

        log.debug("UID request: url: {} post_body: {}",url,post_body.toString());

        String contents = Util.submitHttpRequest(url, headers, post_body.toString());
        log.debug("url {} returned contents: {}",url,contents);

        JsonParser jparse = new JsonParser();
        JsonObject profile = jparse.parse(contents).getAsJsonObject();
        String uid = profile.get("uid").getAsString();

        log.debug("uid: {}",uid);

        return uid;
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
     */
	public boolean doLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        HttpSession session = request.getSession();
        log.debug("BEGIN (session: {})",session.getId());

        UserProfile userProfile = new UserProfile();
        userProfile.setAuthContext(getAuthContext());

        // Add the this instance of UserProfile to the session for retrieval
        // down stream on this request.
        // We set the state of the instance of userProfile below.
        session.setAttribute(IdFilter.USER_PROFILE, userProfile);

        Util.debugHttpRequest(request,log);

        String authorization_header_value = request.getHeader(AUTHORIZATION_HEADER_KEY);
        if(authorization_header_value != null){

            if(EarthDataLoginAccessToken.checkAuthorizationHeader(authorization_header_value)){

                EarthDataLoginAccessToken edlat = new EarthDataLoginAccessToken(authorization_header_value,getUrsClientAppId());
                userProfile.setEDLAccessToken(edlat);
                String uid = getEdlUserId(edlat.getAccessToken());
                userProfile.setUID(uid);
            }
        }
        else {
            // Check to see if we have a code returned from URS. If not, we must
            // redirect the user to URS to start the authentication process.
            String code = request.getParameter("code");
            if (code == null) {
                //String url = getUrsUrl() + "/oauth/authorize?client_id=" + getUrsClientAppId() +
                //   "&response_type=code&redirect_uri=" + request.getRequestURL();

                String url;
                url = PathBuilder.pathConcat(getUrsUrl(), "/oauth/authorize?");
                url += "client_id=" + getUrsClientAppId();
                url += "&";
                url += "response_type=code&redirect_uri=" + request.getRequestURL();

                log.info("Redirecting client to URS SSO. URS Code Request URL: {}", ServletLogUtil.scrubEntry(url));
                response.sendRedirect(url);

                log.debug("END (session: {})", session.getId());
                return false;
            }

            log.info("URS Code: {}", ServletLogUtil.scrubEntry(code));

            // If we get here, the the user was redirected by URS back to our application,
            // and we have a code. We now exchange the code for a token, which is
            // returned as a json document.
            String url = getUrsUrl() + "/oauth/token";

            String postData = "grant_type=authorization_code&code=" + code +
                    "&redirect_uri=" + request.getRequestURL();

            Map<String, String> headers = new HashMap<>();

            String authHeader = "Basic " + getUrsClientAppAuthCode();
            headers.put("Authorization", authHeader);

            log.info("URS Token Request URL: {}", url);
            log.info("URS Token Request POST data: {}", ServletLogUtil.scrubEntry(postData));
            log.info("URS Token Request Authorization Header: {}", authHeader);

            String contents = Util.submitHttpRequest(url, headers, postData);

            log.info("URS Token: {}", contents);


            // Parse the json to extract the token.
            JsonParser jparse = new JsonParser();
            JsonObject json = jparse.parse(contents).getAsJsonObject();


            EarthDataLoginAccessToken edlat = new EarthDataLoginAccessToken(json, getUrsClientAppId());
            userProfile.setEDLAccessToken(edlat);
            getEDLUserProfile(userProfile,edlat.getEndPoint(),edlat.getTokenType(),edlat.getAccessToken());
            log.info("URS UID: {}", userProfile.getUID());
        }

        // Finally, redirect the user back to the their original requested resource.
        String redirectUrl = (String) session.getAttribute(IdFilter.RETURN_TO_URL);
        log.debug("session.getAttribute(RETURN_TO_URL): {} (session: {})", redirectUrl, session.getId());

        if (redirectUrl == null) {
            redirectUrl = PathBuilder.normalizePath(serviceContext, true, false);
        }
        log.info("Authentication Completed. Redirecting client to redirectUrl: {}", redirectUrl);

        response.sendRedirect(redirectUrl);

        log.debug("END (session: {})", session.getId());
        return true;

	}

    @Override
    public String getLoginEndpoint(){
        return PathBuilder.pathConcat(AuthenticationControls.getLoginEndpoint(), authContext);
    }


    @Override
    public String getLogoutEndpoint() {
        return AuthenticationControls.getLogoutEndpoint();
    }





}
