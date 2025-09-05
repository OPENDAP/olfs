/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2022 OPeNDAP, Inc.
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

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import com.auth0.jwt.JWT;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.InvalidPublicKeyException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import opendap.PathBuilder;
import opendap.coreServlet.ReqInfo;
import opendap.http.error.Forbidden;
import opendap.io.HyraxStringEncoding;
import opendap.logging.LogUtil;
import opendap.logging.Procedure;
import opendap.logging.Timer;
import opendap.logging.ServletLogUtil;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static opendap.auth.IdFilter.USER_PROFILE;

/**
 * Created by ndp on 9/25/14.
 */
public class UrsIdP extends IdProvider{

    public static final String DEFAULT_AUTH_CONTEXT="urs";
    public static final String OAUTH_USER_ID_ENDPOINT_PATH="/oauth/tokens/user";

    public static final String URS_URL_KEY = "UrsUrl";
    public static final String URS_CLIENT_ID_KEY = "UrsClientId";
    public static final String URS_CLIENT_AUTH_CODE_KEY = "UrsClientAuthCode";
    public static final String URS_CLIENT_APP_PUBLIC_KEYS_KEY = "UrsClientAppPublicKeys";

    public static final String REJECT_UNSUPPORTED_AUTHZ_SCHEMES_KEY = "RejectUnsupportedAuthzSchemes";

    private Logger log;
    private Logger logProfiling;

    private String ursUrl;
    private String clientAppId;
    private String clientAppAuthCode;
    private String clientAppPublicKeys;
    private boolean rejectUnsupportedAuthzSchemes;

    private static final String ERR_PREFIX = "ERROR! msg: ";


    public UrsIdP(){
        super();
        log = LoggerFactory.getLogger(this.getClass());
        logProfiling = org.slf4j.LoggerFactory.getLogger(ServletLogUtil.CLOUDWATCH_PROFILING_LOG);
        setAuthContext(DEFAULT_AUTH_CONTEXT);
        setDescription("The NASA Earthdata Login (formerly known as URS)");
        rejectUnsupportedAuthzSchemes =  false;
    }


    @Override
    public void init(Element config, String serviceContext) throws ConfigurationException {
        super.init(config, serviceContext);

        Element e;

        e = getConfigElement(config,URS_URL_KEY);
        ursUrl = e.getTextTrim();

        e = getConfigElement(config,URS_CLIENT_ID_KEY);
        clientAppId = e.getTextTrim();

        e = getConfigElement(config,URS_CLIENT_AUTH_CODE_KEY);
        clientAppAuthCode = e.getTextTrim();

        e = getOptionalConfigElement(config,URS_CLIENT_APP_PUBLIC_KEYS_KEY);
        clientAppPublicKeys = e != null ? e.getTextTrim() : "";

        e = config.getChild(REJECT_UNSUPPORTED_AUTHZ_SCHEMES_KEY);
        if(e != null)
            rejectUnsupportedAuthzSchemes = true;

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

    /**
     * Worker method to get value of `childName` from `config`
     * @param config
     * @param childName
     * @return element found in `config`, `null` if `childName` not present
     */
    private Element getOptionalConfigElement(Element config, String childName) {
        Element e = config.getChild(childName);
        if(e == null){
            String msg = this.getClass().getSimpleName() + " configuration does not contain optional <" + childName + "> element.";
            log.info("{}", msg);
        }
        return e;
    }

    public String getUrsUrl() {
        return ursUrl;
    }

    public void setUrsUrl(String ursUrl) throws ServletException{
        if(ursUrl == null){
            String msg = "BAD CONFIGURATION - URS IdP Module must be configured with a URS Service URL. (urs_url)";
            log.error("{}{}", ERR_PREFIX,msg);
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
            log.error("{}{}", ERR_PREFIX,msg);
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
            log.error("{}{}", ERR_PREFIX,msg);
            throw new ServletException(msg);
        }

        this.clientAppAuthCode = ursClientAppAuthCode;
    }

    public String getUrsClientAppPublicKeys() {
        return clientAppPublicKeys;
    }

    public void setUrsClientAppPublicKeys(String ursClientAppPublicKeys) {
        this.clientAppPublicKeys = ursClientAppPublicKeys;
    }

    void getEDLUserProfile(UserProfile userProfile) throws IOException {

        EarthDataLoginAccessToken edlat = userProfile.getEDLAccessToken();

        Map<String, String> headers = new HashMap<>();
        // Now that we have an access token, we can retrieve the user profile. This
        // is returned as a JSON document.

        // EDL endpoint: /api/users/user_id
        String url;
        String edl_user_api_endpoint = edlat.getEndPoint();

        if (edlat.getEndPoint() == null) {
            url = PathBuilder.pathConcat(ursUrl, "/api/users/");
            url = PathBuilder.pathConcat(url, userProfile.getUID());
        }
        else {
            url = PathBuilder.pathConcat(ursUrl, edl_user_api_endpoint);
        }

        url += "?client_id=" + getUrsClientAppId();
        String authHeader = edlat.getTokenType() + " " + edlat.getAccessToken();
        headers.put("Authorization", authHeader);

        log.info("URS User Profile Request URL: {}", url);
        log.info("URS User Profile Request Authorization Header: {}", authHeader);

        String contents = Util.submitHttpRequest(url, headers, null);

        log.info("URS User Profile: {}", contents);

        userProfile.ingestJsonProfileString(contents);
    }

    // curl -X POST -d 'token=<token>&client_id=<‘your application client_id’>
    // https://urs.earthdata.nasa.gov/oauth/tokens/user

    /**
     * Return key in `jwksPublicKeys` with key id ("kid") `publicKeyId`;
     * return `null` if no matching key.
     *
     * @param jwksPublicKeys JSON Web Key Set (JWKS) of public keys
     * @param publicKeyId The key id of the requested key in `jwksPublicKeys`
     * @return The requested public `key` if present; `null` otherwise
     * @throws InvalidPublicKeyException
     * @throws JsonParseException
     * @throws IllegalStateException
     */
    RSAPublicKey getPublicKeyForId(String jwksPublicKeys, String publicKeyId) throws InvalidPublicKeyException, JsonParseException, IllegalStateException {
        // Safety first!
        if (jwksPublicKeys == null || jwksPublicKeys.isEmpty() || publicKeyId == null) {
            return null;
        }

        // 1. Parse the key set (JSON web key set, i.e. JWKS)
        JsonObject jwks = JsonParser.parseString(jwksPublicKeys).getAsJsonObject();
        JsonArray jwksKeys = jwks.getAsJsonArray("keys");
         if (jwksKeys == null) {
            return null;
        }

        // 2. Pull out the requested key
        JsonObject jwkJson = null;
        for (int i = 0; i < jwksKeys.size(); i++) {
            JsonObject jwk = jwksKeys.get(i).getAsJsonObject();
            if (jwk.has("kid") && jwk.get("kid").getAsString().equals(publicKeyId)) {
                jwkJson = jwk;
                break;
            }
        }
        if (jwkJson == null) {
            return null;
        }

        // 3. Convert key's json entry to a public key
        Map<String, Object> jwkValues = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : jwkJson.entrySet()) {
            jwkValues.put(entry.getKey(), entry.getValue().getAsString());
        }

        Jwk jwk = Jwk.fromValues(jwkValues);
        PublicKey j = jwk.getPublicKey();
        return (RSAPublicKey) j;
    }

    /**
     * Return value of `key` after decoding `inputStr` into JSON object;
     * return `null` if encoded string does not contain JSON, 
     * `key` is not present in decoded object, or return value is not a String.
     *
     * @param inputStr Base64-encoded JSON
     * @param key The key of the value to be returned from the decoded `inputStr`
     * @return The value of `key` if present in encoded `inputStr` and a String, `null` otherwise
     */
    static String getStringValueFromEncodedJson(String inputStr, String key) {
        String decodedStr = null;
        try {
            decodedStr = new String(Base64.getDecoder().decode(inputStr),
                HyraxStringEncoding.getCharset());
            if (decodedStr == null || decodedStr.isEmpty()) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        JsonElement json;
        try {
            json = JsonParser.parseString(decodedStr);
        } catch (JsonSyntaxException e) {
            return null;
        }
        if (!json.isJsonObject()) {
            return null;
        }

        JsonObject decodedJson;
        try {
            decodedJson = json.getAsJsonObject();
        } catch (JsonParseException e) {
            return null;
        }

        if (decodedJson != null && decodedJson.has(key)) {
            JsonElement value = decodedJson.get(key);
            try {
                return value.getAsString();
            } catch (IllegalStateException e) {
                return null;
            }
        }
        return null;
    }

    
    /**
     * Return the value of "uid" from the `accessToken` JWT's payload;
     * `null` if `accessToken` is fails verification.
     *
     * @param publicKeys  A stringified JWK Set
     * @param accessToken An EDL JWT token
     * @return The `accessToken`'s user id (`uid`)
     */
    String getEdlUserIdFromToken(String publicKeys, String accessToken) {
        String errorPrefix = "Failure to validate access token locally;";
        // Safety first!
        if (accessToken == null) {
            log.error("{}{} access token is null.", ERR_PREFIX, errorPrefix);
            return null;
        }

        // 1. Figure out which public key the access token requires
        DecodedJWT unverifiedJwt = null;
        try {
            unverifiedJwt = JWT.decode(accessToken);
        } catch (JWTDecodeException e) {
            log.error("{}{} unable to load access token as JWT. Details: {}", ERR_PREFIX, errorPrefix, e.getMessage());
            return null;
        }
        String publicKeyId = getStringValueFromEncodedJson(unverifiedJwt.getHeader(), "sig");
        if (publicKeyId == null) {
            log.error("{}{} access token missing required field `sig`.", ERR_PREFIX, errorPrefix);
            return null;
        }

        // 2. From the set of public access keys provided, get the one specifically
        // required by our access token
        RSAPublicKey publicKey;
        try {
            publicKey = getPublicKeyForId(publicKeys, publicKeyId);
        } catch (InvalidPublicKeyException | JsonParseException e) {
            log.error("{}{} no valid matching public key found in `{}`. Details: {}", ERR_PREFIX, errorPrefix, publicKeys,
                    e.getMessage());
            return null;
        }
        if (publicKey == null) {
            log.error("{}{} no public key found for access token `{}`.", ERR_PREFIX, errorPrefix, publicKeyId);
            return null;
        }

        // 3. Use that key to verify the access token
        try {
            Algorithm algorithm = Algorithm.RSA256(publicKey);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(unverifiedJwt);
        } catch (JWTVerificationException e) {
            log.error("{}{} access token failed verification. Details: {}", ERR_PREFIX, errorPrefix, e.getMessage());
            return null;
        }

        // ...finally, pull user id from the payload
        String uid = getStringValueFromEncodedJson(unverifiedJwt.getPayload(), "uid");
        log.debug("uid: {}", uid);
        return uid;
    }

    /**
     * Worker method to post contents of `msg` to `logProfiling`
     * @param msg
     * @return void
     */
    private void writeToProfilingLog(String msg) {
        if(ServletLogUtil.useDualCloudWatchLogs.get()) {
            logProfiling.info("Profile timing: {} - {}", msg, Instant.now());
        }
    }

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

        String contents;
        try {
            writeToProfilingLog("Request EDL authentication");
            Logger edlLog = LoggerFactory.getLogger("EDL_LOG");
            Timer.enable();
            Timer.reset();
            Procedure timedProc = Timer.start();
            contents = Util.submitHttpRequest(url, headers, post_body.toString());
            Timer.stop(timedProc);
            String report = Timer.report();
            edlLog.info(report);
        }
        finally {
            writeToProfilingLog("Receive EDL authentication response");
            Timer.reset();
            Timer.disable();
        }

        log.debug("url {} returned contents: {}",url,contents);

        JsonParser jparse = new JsonParser();
        JsonObject profile = jparse.parse(contents).getAsJsonObject();
        String uid = profile.get("uid").getAsString();


        log.debug("uid: {}",uid);

        return uid;
    }

    /**
     * Builds the EDL returnTo URL. By using the URL returned by
     * ReqInfo.getRequestUrlPath() we capitalize on the work done inside to
     * sort out the correct request scheme/protocol.
     * @param request The request to assess
     * @return The EDL redirect_uri parameter value for the EDL authorization
     * redirect being built for the requesting client.
     */
    private String getEdlRedirectUri(HttpServletRequest request){
        String edlRedirectUrl =  ReqInfo.getRequestUrlPath(request);
        edlRedirectUrl = edlRedirectUrl.substring(0,edlRedirectUrl.indexOf("/",9));
        edlRedirectUrl = PathBuilder.pathConcat(edlRedirectUrl, getLoginEndpoint());
        return edlRedirectUrl;
    }

    /**
     * Checks the passed request for an Authorization header and if present
     * attempts to use the header to perform a step with EDL that validates
     * the token. Only works for Bearer tokens atm.
     *
     * @param request The HttpServletRequest whose headers we will examine for
     *                an Authorization header.
     * @param userProfile The user profile that will receive the Authentication
     *                    context, EDL Token, and UID if the token checks out.
     * @return True if the token "worked" to identify/authenticate the UserProfile
     * @throws IOException
     * @throws Forbidden
     */
    public boolean doTokenAuthentication(HttpServletRequest request, UserProfile userProfile) throws IOException, Forbidden {

        if (userProfile == null) {
            return false;
        }

        writeToProfilingLog("Start token authentication");
        boolean foundValidAuthToken = false;

        String authz_hdr_value = request.getHeader(AUTHORIZATION_HEADER_KEY);
        if(authz_hdr_value != null && !authz_hdr_value.isEmpty()) {
            if (AuthorizationHeader.isBearer(authz_hdr_value)) {
                EarthDataLoginAccessToken edlat = new EarthDataLoginAccessToken(authz_hdr_value, getUrsClientAppId());
                userProfile.setEDLAccessToken(edlat);
                userProfile.setAuthContext(getAuthContext());

                // Get the user's ID, which implicitly validates the access token
                // as no id will be returned for an invalid access token.
                // Attempt local verification if public keys have been provided...
                String token = edlat.getAccessToken();
                if (!getUrsClientAppPublicKeys().isEmpty()) {
                    String uid = getEdlUserIdFromToken(getUrsClientAppPublicKeys(), token);
                    if (uid == null) {
                        log.error("{}Unable to validate EDL access token locally; falling back to remote validation", ERR_PREFIX);
                    } else {
                        userProfile.setUID(uid);
                        foundValidAuthToken = true;
                    }
                }
                log.debug("Local EDL token validation result: valid={}, uid={}", foundValidAuthToken, userProfile.getUID());

                // Fall back to the EDL endpoint on local failure or lack of local public keys
                if (!foundValidAuthToken) {
                    String uid = getEdlUserId(token);
                    userProfile.setUID(uid);

                    // Successful retrieval indicates that the token is valid
                    foundValidAuthToken = userProfile.getUID() != null;
                    log.debug("Remote EDL token validation result: valid={}, uid={}", foundValidAuthToken, userProfile.getUID());
                }
            }
            else if (rejectUnsupportedAuthzSchemes) {
                writeToProfilingLog("Failed token authentication");
                    String msg = "Received an unsolicited/unsupported/unanticipated/unappreciated ";
                    msg += "header. 'Authorization Scheme: ";
                    msg += AuthorizationHeader.getScheme(authz_hdr_value) + "' ";
                    if (AuthorizationHeader.isBasic(authz_hdr_value)) {
                        msg += "Your request included unencrypted credentials that this ";
                        msg += "service is not prepared to receive. Please check the version ";
                        msg += "and configuration of your client software as this is a security ";
                        msg += "concern and needs to be corrected. ";
                    }
                    msg += "I am sorry, but I cannot allow this.";
                    throw new Forbidden(msg);
                }
            else {
                String msg = "WARNING - Received unexpected Authorization header, IGNORED! ";
                msg += "Authorization Scheme: {}";
                log.warn(msg, AuthorizationHeader.getScheme(authz_hdr_value));
            }
        }
        writeToProfilingLog("End token authentication - Valid token:" + foundValidAuthToken);
        return foundValidAuthToken;
    }


    /**
     *
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
	public boolean doLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException, Forbidden
    {
        HttpSession session = request.getSession();
        log.debug("BEGIN (session: {})",session.getId());

        UserProfile userProfile = new UserProfile();
        userProfile.setAuthContext(getAuthContext());

        // Add this instance of UserProfile to the session for retrieval
        // down stream on this request.
        // We set the state of the instance of userProfile below.
        session.setAttribute(USER_PROFILE, userProfile);

        Util.debugHttpRequest(request,log);

        // Check to see if we have a code returned from URS. If not, we must
        // redirect the user to EDL to start the authentication process.
        String code = request.getParameter("code");
        if (code == null) {
            String url;
            url = PathBuilder.pathConcat(getUrsUrl(), "/oauth/authorize?");
            url += "client_id=" + getUrsClientAppId();
            url += "&";

            String returnToUrl = ReqInfo.getRequestUrlPath(request);

            url += "response_type=code&redirect_uri=" + returnToUrl;

            log.info("Redirecting client to EDL SSO. URS Code Request URL: {}", LogUtil.scrubEntry(url));
            response.sendRedirect(url);

            log.debug("END (session: {})", session.getId());
            return false;
        }

        log.info("EDL Code: {}", LogUtil.scrubEntry(code));

        // If we get here, the user was redirected by URS back to our application,
        // and we have a code. We now exchange the code for a token, which is
        // returned as a json document.
        String url = getUrsUrl() + "/oauth/token";

        String postData = "grant_type=authorization_code&code=" + code +
                "&redirect_uri=" + ReqInfo.getRequestUrlPath(request);

        Map<String, String> headers = new HashMap<>();

        String authHeader = "Basic " + getUrsClientAppAuthCode();
        headers.put("Authorization", authHeader);

        log.info("URS Token Request URL: {}", url);
        log.info("URS Token Request POST data: {}", LogUtil.scrubEntry(postData));
        log.info("URS Token Request Authorization Header: {}", authHeader);

        String contents = Util.submitHttpRequest(url, headers, postData);

        log.info("URS Token: {}", contents);


        // Parse the json to extract the token.
        JsonParser jparse = new JsonParser();
        JsonObject json = jparse.parse(contents).getAsJsonObject();


        EarthDataLoginAccessToken edlat = new EarthDataLoginAccessToken(json, getUrsClientAppId());
        userProfile.setEDLAccessToken(edlat);
        getEDLUserProfile(userProfile);
        log.info("URS UID: {}", userProfile.getUID());

        // Finally, redirect the user back to the original requested resource.
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
