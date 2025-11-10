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

import opendap.PathBuilder;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.ServletUtil;
import opendap.coreServlet.RequestId;
import opendap.http.error.Forbidden;
import opendap.logging.ServletLogUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static opendap.logging.ServletLogUtil.logEDLProfiling;


public class IdFilter implements Filter {

    private Logger log;
    private static final String logName = "AuthenticationLog";
    private AtomicLong counter;
    private static final java.util.concurrent.locks.Lock initLock;
    static {
        initLock = new ReentrantLock();
    }

    public static final String RETURN_TO_URL      = "return_to_url";
    public static final String USER_PROFILE       = "user_profile";

    private boolean isInitialized;
    private FilterConfig filterConfig;

    private static final String CONFIG_PARAMETER_NAME = "config";
    private static final String DEFAULT_CONFIG_FILE_NAME = "user-access.xml";

    private static final String SHOW_USER_INFO_ELEM = "ShowUserInfoOnProfilePage";
    private static boolean showUserProfileDetails = false;
    private static final String MAX_SESSION_LIFE_ELEM = "MaxSessionLife";
    private static final String UNITS_ATTR = "units";
    private static final long DEFAULT_MAX_SESSION_TIME_SECONDS = 60; // 60 seconds in milliseconds
    private double maxSessionTimeSeconds = DEFAULT_MAX_SESSION_TIME_SECONDS;

    private String guestEndpoint;
    private boolean enableGuestProfile;
    private String serviceContextPath;

    public IdFilter(){
        isInitialized = false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        initLock.lock();
        try {
            if (isInitialized)
                return;

            this.filterConfig = filterConfig;
            try {
                System.out.println("IdFilter.init() - config file: " + filterConfig.getInitParameter(CONFIG_PARAMETER_NAME));
                init();
            } catch (IOException | JDOMException se) {
                log.warn("init() - INITIALIZATION HAS BEEN POSTPONED! FAILED TO INITIALIZE IdFilter! " +
                        "Caught {} Message: {} ", se.getClass().getName(), se.getMessage());
            }
        }
        finally {
            initLock.unlock();
        }

    }

    private enum TimeUnits { MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS, WEEKS }

    /**
     * Lenient conversion of a time units string to an enum value.
     * @param timeUnitsStr The units string
     * @return The enum value for the units
     */
    TimeUnits stringToTimeUnits(String timeUnitsStr) {
        if(timeUnitsStr!=null){
            if(timeUnitsStr.toLowerCase().startsWith("mil")){
                return TimeUnits.MILLISECONDS;
            }
            if(timeUnitsStr.toLowerCase().startsWith("s")){
                return TimeUnits.SECONDS;
            }
            if(timeUnitsStr.toLowerCase().startsWith("min")){
                return TimeUnits.MINUTES;
            }
            if(timeUnitsStr.toLowerCase().startsWith("h")){
                return TimeUnits.HOURS;
            }
            if(timeUnitsStr.toLowerCase().startsWith("d")){
                return TimeUnits.DAYS;
            }
            if(timeUnitsStr.toLowerCase().startsWith("w")){
                return TimeUnits.WEEKS;
            }
        }
        log.warn("WARNING - Failed to recognize submitted time units. Falling back to default time units of SECONDS");
        return TimeUnits.SECONDS;
    }

    /**
     * Computes a time duration value in seconds from a value and units string.
     * @param valueStr A numeric time duration value.
     * @param unitsStr The units of the time duration value.
     * @return The time in seconds represented by valueStr and unitsStr
     */
    double computeMaxSessionTimeSeconds(String valueStr, String unitsStr){
        double value = Double.parseDouble(valueStr);
        double valueSeconds;
        switch (stringToTimeUnits(unitsStr)) {
            case MILLISECONDS:
                valueSeconds = value / 1000; // milliseconds to seconds
                break;
            case SECONDS:
                valueSeconds = value;
                break;
            case MINUTES:
                valueSeconds = value * 60; // Minutes to seconds
                break;
            case HOURS:
                valueSeconds = value * 60 * 60; // Hours to seconds
                break;
            case DAYS:
                valueSeconds = value * 60 * 60 * 24; // days to seconds
                break;
            case WEEKS:
                valueSeconds = value * 60 * 60 * 24 * 7; // weeks to seconds.
                break;
            default:
                valueSeconds = value; // default to units in seconds
                break;
        }
        return valueSeconds;
    }

    private String getServiceContextPath(){ return serviceContextPath; }
    /**
     *
     * @throws IOException
     * @throws JDOMException
     */
    private void init() throws IOException, JDOMException {

        initLock.lock();
        try {
            if(isInitialized)
                return;

            serviceContextPath = Util.fullyQualifiedPath(filterConfig.getServletContext().getContextPath());

            counter = new AtomicLong(0);
            ServletLogUtil.initLogging(filterConfig.getServletContext());
            log = LoggerFactory.getLogger(this.getClass());
            log.info("init() - Initializing IdFilter...");

            String configFileName = filterConfig.getInitParameter(CONFIG_PARAMETER_NAME);
            if(configFileName==null){
                configFileName = DEFAULT_CONFIG_FILE_NAME;
                String msg = "init() - The web.xml configuration for "+getClass().getName()+
                        " does not contain an init-parameter named \""+ CONFIG_PARAMETER_NAME +"\" " +
                        "Using the DEFAULT name: "+configFileName;
                log.warn(msg);
            }

            String configDirName = ServletUtil.getConfigPath(filterConfig.getServletContext());
            File configFile = new File(configDirName,configFileName);

            // Load Config File
            Element config;
            config = opendap.xml.Util.getDocumentRoot(configFile);

            Element e;
            //    <MaxSessionLife units="seconds">15</MaxSessionLife>
            e = config.getChild(MAX_SESSION_LIFE_ELEM);
            if(e!=null){
                maxSessionTimeSeconds = computeMaxSessionTimeSeconds(e.getTextTrim(), e.getAttributeValue(UNITS_ATTR));
            }

            //    <ShowUserInfoInProfile />
            e = config.getChild(SHOW_USER_INFO_ELEM);
            if(e!=null){
                showUserProfileDetails = true;
            }

            e = config.getChild("EnableGuestProfile");
            if(e!=null){
                enableGuestProfile = true;
                guestEndpoint = PathBuilder.pathConcat(getServiceContextPath(), "guest");
            }
            log.info("init() - Guest Profile {}", enableGuestProfile ?"Has Been ENABLED!":"Is DISABLED!");

            // Set up authentication controls. If the configuration element is missing that's fine
            // because we know that it will still configure the login/logout endpoint values.
            AuthenticationControls.init(config.getChild(AuthenticationControls.CONFIG_ELEMENT),getServiceContextPath());

            IdPManager.setServiceContextPath(getServiceContextPath());
            // Load ID Providers (Might be several)
            for (Object o : config.getChildren("IdProvider")) {
                Element idpConfig = (Element) o;
                IdPManager.addProvider(idpConfig);
            }
            isInitialized = true;

            log.info("init() - IdFilter HAS BEEN INITIALIZED!");
            }
        finally {
            initLock.unlock();
        }

    }


    boolean sessionIsExpired(HttpSession session){
        boolean expired = false;
        double sessionInUseTime = (System.currentTimeMillis() - session.getCreationTime())/1000.0;
        if (sessionInUseTime > maxSessionTimeSeconds) {
            expired = true;
        }
        return expired;
    }

    HttpSession getSession(HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if(session == null){
            session = request.getSession(true);
        }
        log.debug("session.isNew(): {}", session.isNew());

        if(sessionIsExpired(session)){
            log.debug("Invalidating expired session: {}", session.getId());
            session.invalidate();
            session = request.getSession(true);
        }
        return session;
    }

    public void doFilter(ServletRequest sreq, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        // Ensure initialization has been accomplished
        if (!isInitialized) {
            try {
                init();
            } catch (IOException | JDOMException e) {
                String msg = "doFilter() - IdFilter INITIALIZATION HAS FAILED! " +
                        "Caught " + e.getClass().getName() + " Message: " + e.getMessage();
                log.error(msg);
                OPeNDAPException.setCachedErrorMessage(msg);
                throw new ServletException(msg,e);
            }
        }

        try {

            HttpServletRequest request = (HttpServletRequest) sreq;
            RequestCache.open(request);
            RequestId requestId = RequestCache.getRequestId();

            HttpServletRequest hsReq = request;

            ServletLogUtil.logServerAccessStart(request,logName,request.getMethod(), requestId);

            // Get session, make new as needed, invalidate expired session as needed.
            HttpSession session = getSession(request);
            log.debug("BEGIN ({}) (session-id: {})",requestId, session.getId());
            log.debug("session.isNew(): {}", session.isNew());

            Util.debugHttpRequest(request,log);

            HttpServletResponse hsRes = (HttpServletResponse) response;
            String requestURI = hsReq.getRequestURI();
            // Since the request may come from other deployment contexts we check the request for the context.
            String contextPath = Util.fullyQualifiedPath(hsReq.getContextPath());

            // FIXME The following needs to be replaced with a mechanism that does not require the query
            //  to be added to the request URL in order for the redirect to produce the target request.
            //  Why? Because the query may be too large for a URL on many servers.
            //  What do? Maybe we use a thread safe cache to hold the CE and replace it in the redirect
            //  with the md5 hash of the query and then use that for a lookup down stream?
            String requestUrl = ReqInfo.getRequestUrlPath(hsReq);
            String query = ReqInfo.getConstraintExpression(hsReq);
            if(!query.isEmpty()) {
                requestUrl += "?" + query;
            }

            // Intercept requests for the login/logout endpoints
            if (requestURI.equals(AuthenticationControls.getLogoutEndpoint())) {
                doLogout(hsReq, hsRes);
                return;
            }
            else if (AuthenticationControls.isIntitialized() &&
                    requestURI.equals(AuthenticationControls.getLoginEndpoint())) {
                // A user profile page with
                // only the ability to login/logout. No editing.
                doUserProfilePage(hsReq, hsRes);
                return;
            }
            else if (enableGuestProfile && requestURI.equals(guestEndpoint)) {
                // This is the Guest User login endpoint which may be disabled.
                doGuestLogin(hsReq, hsRes);
                return;
            }
            else {
                // Check IdProviders to see if this request is someone's
                // login end point context.
                for (IdProvider idProvider : IdPManager.getProviders()) {

                    String loginEndpoint = idProvider.getLoginEndpoint();
                    if(requestURI.equals(loginEndpoint)) {
                        // We take the first matching IdProvider
                        // then we send the request to matching IdProvider
                        // to do the login.
                        synchronized (session) {
                            // Check the RETURN_TO_URL and if it's the login endpoint
                            // return to the root dir of the web application after
                            // authenticating.
                            String returnToUrl = Util.stringFromJson( (String) session.getAttribute(RETURN_TO_URL));
                            log.debug("Retrieved RETURN_TO_URL: {} (session: {})",returnToUrl,session.getId());
                            if (returnToUrl != null && returnToUrl.equals(loginEndpoint)) {
                                String msg = "Setting session RETURN_TO_URL("+RETURN_TO_URL+ ") to: "+contextPath;
                                msg += " (session: "+session.getId()+")";
                                log.debug(msg);
                                session.setAttribute(RETURN_TO_URL, Util.toJson(contextPath));
                            }
                        }
                        long profilingStartTime = System.currentTimeMillis();
                        boolean loginComplete = false;
                        try {
                            //
                            // Run the login gizwhat. This may involve simply
                            // collecting credentials from the user and
                            // forwarding them on to the IdP, or it may involve
                            // a complex dance of redirection in which
                            // the user drives their browser through an
                            // elaborate scheme like OAuth2, so they can come
                            // back to this very spot with some kind of
                            // cookie/token/thingy that lets the doLogin
                            // invocation complete.
                            //
                            loginComplete = idProvider.doLogin(hsReq, hsRes);
                            //
                            // We return here and don't do the filter chain
                            // because the "doLogin" method will, when
                            // completed send the client a 302 redirect to the
                            // RETURN_TO_URL. We want the processing to end
                            // here.
                            //
                            log.debug("END (session: {})",session.getId());
                            // NOTE: We don't call filterChain.doFilter() here
                            // because the contract is that the
                            // IpProvider.doLogin() method is going to get the
                            // user authenticated (or not). After authentication
                            // the IpProvider.doLogin() will look at the users
                            // session to find the IdFilter.RETURN_TO_URL and
                            // redirect the client there. Otherwise, return some
                            // error object.
                            return;


                        } catch (IOException | Forbidden e) {
                            String msg = "Your Login Transaction FAILED!   " +
                                    "Authentication Context: '"+idProvider.getAuthContext()+
                                    "' Message: "+ e.getMessage();
                            log.error("doFilter() - {}", msg);
                            OPeNDAPException.setCachedErrorMessage(msg);
                            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED,msg);
                            log.debug("END (session: {})",session.getId());
                            return;
                        } finally {
                            logEDLProfiling("Handle login operation - Login now concluded? " + loginComplete, profilingStartTime);
                        }
                    }
                }
            }

            //
            // We only ever get here because the user is NOT requesting one of
            // the login endpoints.
            // Since Tomcat and the Servlet API have their own
            // "login" scheme (name & password based) API we need to check if
            // _our_ login thing ran and if so (detected by
            // the presence of the USER_PROFILE attribute in the session) we
            // need to a special HttpRequest object to hold our authenticated
            // user and then pass it on to the filter chain.
            //
            UserProfile up = UserProfile.fromJson((String) session.getAttribute(USER_PROFILE));
            if(up != null && !up.validateUserFootPrint(hsReq)){
                if(IdPManager.hasDefaultProvider()) {
                    // @TODO Maybe use the IdPManager.getDefaultProvider().doLogout(hsReq,hsRes); here?
                    IdPManager.getDefaultProvider().invalidate(up);
                }
                up = null;
                session.setAttribute(USER_PROFILE, null);
                session.invalidate();
                session = request.getSession(true);
            }

            if (up != null) {
                log.debug("Found UserProfile object in Session, this is an authenticated request for user: {}",up.getUID());
                AuthenticatedHttpRequest authReq = new AuthenticatedHttpRequest(hsReq);
                authReq.setUid(up.getUID());
                hsReq = authReq;
            }
            else {
                // Lacking a UserProfile we check for a default IdP and then try
                // a token based authentication.
                log.debug("No UserProfile object found in Session. Request is not yet authenticated. " +
                        "Checking Authorization headers...");
                if (IdPManager.hasDefaultProvider()) {
                    try {
                        UserProfile userProfile = new UserProfile(request);
                        boolean retVal = false;
                        long profilingStartTime = System.currentTimeMillis();
                        try {
                            retVal = IdPManager.getDefaultProvider().doTokenAuthentication(request, userProfile);
                        } finally {
                            // We only care about logging token validation if there was ever a token to BE validated.
                            // If there was no authz_hdr_value, there was never even the hint of a token to be validated,
                            // and `doTokenAuthentication` will have returned immediately (and returned "false", although
                            // we don't care about that here).
                            String authz_hdr_value = request.getHeader(IdProvider.AUTHORIZATION_HEADER_KEY);
                            if (authz_hdr_value != null) {
                                logEDLProfiling("Validate token - Is valid? " + retVal, profilingStartTime);
                            }
                        }
                        if(retVal){
                            log.info("Validated Authorization header. uid: {}, sessionId: {}", userProfile.getUID(), session.getId());

                            // By adding the UserProfile to the session here
                            // it's available for the PEPFilter which is invoked
                            // by the call to:
                            //     filterChain.doFilter(hsReq, hsRes);
                            // below. This is key to data access w/o redirects
                            // or sessions. Well, there's a session, but for
                            // tokens it only needs to persist for the duration
                            // of the current request
                            session.setAttribute(USER_PROFILE, userProfile.toJson(false));

                            // By replacing the HttpServletRequest with our own version
                            // We can inject the uid into
                            //   HttpServletRequest.getRemoteUser()
                            //   HttpServletRequest.getUserPrincipal()
                            //
                            AuthenticatedHttpRequest authReq = new AuthenticatedHttpRequest(hsReq);
                            authReq.setUid(userProfile.getUID());
                            hsReq = authReq;
                        }
                    }
                    catch (Forbidden http_403){
                        log.error("Unable to validate Authorization header. Message: "+http_403.getMessage());
                    }
                }
            }

            // Cache the request URL in the session. We do this here because we know by now that the request was
            // not for a "reserved" endpoint for login/logout etc. and we DO NOT want to cache those locations.
            synchronized(session) {
                Util.cacheRequestUrlAsNeeded(session, requestUrl, requestURI, contextPath);
            }

            // This call leads to the PEPFilter, wooo!
            filterChain.doFilter(hsReq, hsRes);

            log.debug("END (session: {} returnToUrl: {})",session.getId(),Util.stringFromJson( (String) session.getAttribute(RETURN_TO_URL)));
            ServletLogUtil.logServerAccessEnd(200,logName);
        }
        finally {
            RequestCache.close();
        }
    }



    /**
     *
     */
    @Override
    public void destroy() {
        log = null;
    }


    /**
     * Logs a user out.
     * This method simply terminates the local session and redirects the user back
     * to the home page.
     */
    private void doLogout(HttpServletRequest request, HttpServletResponse response)
	        throws IOException {

        log.info("doLogout() - BEGIN");
        log.info("doLogout() - Retrieving session...");
        // Since the request may come from other deployment contexts we check the request for the context.
        String redirectUrl  = Util.fullyQualifiedPath(request.getContextPath());
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("doLogout() - Got session...");
            String href = Util.stringFromJson( (String) session.getAttribute(RETURN_TO_URL));
            redirectUrl = href!=null?href:redirectUrl;

            UserProfile up = UserProfile.fromJson((String) session.getAttribute(USER_PROFILE));
            if (up != null) {
                log.info("doLogout() - Logging out user '{}'", up.getUID());
                IdProvider idProvider = up.getIdP();
                if (idProvider != null) {
                    log.info("doLogout() - Calling '{}' logout handler.", idProvider.getAuthContext());
                    // Redirect to RETURN_TO_URL is done by idProvider
                    idProvider.doLogout(request, response);
                    log.info("doLogout() - END");
                    return;
                }
            } else {
                log.info("doLogout() - Missing UserProfile object....");
            }
        }
        log.info("doLogout() - redirectUrl: {}", redirectUrl);
        log.info("doLogout() - Punting with session.invalidate()");
        // This is essentially a "punt" since things aren't as expected.
        if(session!=null)
            session.invalidate();

        try {
            response.sendRedirect(redirectUrl);
        }
        catch(IOException e){
            OPeNDAPException.setCachedErrorMessage(e.getMessage());
            throw e;
        }
        log.info("doLogout() - END");
    }



    /**
     * Performs the user login operations.
     * This method does not actually generate any output. It performs a series
     * of redirects, depending upon the current state.
     * <p>
     * 1) If the user is already logged in, it just redirects them back to the
     *    home page.
     * <p>
     * 2) If no 'code' query parameter is found, it will redirect the user to URS
     *    to start the authentication process.
     * <p>
     * 3) If a 'code' query parameter is found, it assumes the call is a redirect
     *    from a successful URS authentication, and will attempt to perform the
     *    token exchange.
     */
	private void doGuestLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        HttpSession session = request.getSession(false);
        // Since the request may come from other deployment contexts we check the request for the context.
        String redirectUrl = Util.fullyQualifiedPath(request.getContextPath());
        if(session != null) {
            redirectUrl = Util.stringFromJson( (String) session.getAttribute(RETURN_TO_URL));
            session.invalidate();
        }
        HttpSession guestSession = request.getSession(true);
        synchronized (guestSession) {
            guestSession.setAttribute(RETURN_TO_URL, Util.toJson(redirectUrl));
            guestSession.setAttribute(USER_PROFILE, (new GuestProfile()).toJson(false));
        }

        //
        // Finally, redirect the user back to the their original requested resource.
        //
        try {
            response.sendRedirect(redirectUrl);
        }
        catch(IOException e){
            OPeNDAPException.setCachedErrorMessage(e.getMessage());
            throw e;
        }
	}

    /**
     * HTML Shortcuts: openers and closers
     */
    private static final String oDTS = "<dt><strong>";
    private static final String cDTS = "</strong></dt>";

    private static final String oDTPS = "<dt><pre><strong>";
    private static final String cDTPS = "</strong></pre></dt>";

    private static final String oDDPS = "<dd><pre><span style='background-color: #E0E0E0;'>";
    private static final String cDDPS = "</span></pre></dd>";

    private String noProfileContent(String contextPath, HttpSession session){
        log.debug("Building noProfile String.");


        StringBuilder noProfile = new StringBuilder();

        noProfile.append("<p><b>You are not currently logged on.</b></p><br />");


        noProfile.append("<b><i>You may login using one of these identity providers:</i></b>");

        noProfile.append("<ul>");

        for(IdProvider idProvider: IdPManager.getProviders()) {
            noProfile.append("<li><a href=\"").append(idProvider.getLoginEndpoint()).append("\">");
            noProfile.append(idProvider.getDescription());
            noProfile.append("</a><br/><br/></li>");
        }
        noProfile.append("</ul>");
        if(enableGuestProfile) {
            noProfile.append("<i>Or you may:</i><br />");
            noProfile.append("<ul>");
            noProfile.append("<li><a href=\"").append(contextPath).append("/guest\">Use a 'guest' profile.</a> </li>");
            noProfile.append("</ul>");
        }
        if(session!=null){
            String origUrl = Util.stringFromJson( (String) session.getAttribute(RETURN_TO_URL));
            noProfile.append("<dl>");
            if(origUrl!=null){
                noProfile.append(oDTS).append("After authenticating you will be returned to:").append(cDTS);
                noProfile.append("<dd><pre><a href='").append(origUrl).append("'>").append(origUrl).append("</a></pre></dd>");
            }
            noProfile.append("</dl>");
        }
        return noProfile.toString();
    }

    /**
     * Displays the users profile page.
     * This method displays a profile page for users. If the user has authenticated,
     * then it will display his/her name, and provide a logout link. If the user
     * has not authenticated, then a login link will be displayed.
     *
     */
	private void doUserProfilePage(HttpServletRequest request, HttpServletResponse response)
	        throws IOException
    {
        // Since the request may come from other deployment contexts we check the request for the context.
        String srvcCntxtPth = Util.fullyQualifiedPath(request.getContextPath());
        log.debug("doLandingPage() - Setting Response Headers...");

        response.setContentType("text/html");
        response.setHeader("Content-Description", "Login Page");
        response.setHeader("Cache-Control", "max-age=0, no-cache, no-store");
        log.debug("doLandingPage() - Writing page contents.");

        // Generate the html page header
        PrintWriter out;
        try {
            out = response.getWriter();
        }
        catch(IOException e){
            OPeNDAPException.setCachedErrorMessage(e.getMessage());
            throw e;
        }
		out.println("<html><head><title>Greetings User!</title></head>");
		out.println("<body><h1>"+AuthenticationControls.getLoginBanner()+"</h1>");
        out.println("<hr/>");

        if(request.getRemoteUser()!=null || request.getUserPrincipal()!=null) {
            out.println("<p>request.getRemoteUser(): " + (request.getRemoteUser()==null?"not set": Encode.forHtml(request.getRemoteUser())) + "</p>");
            if (request.getUserPrincipal() != null) {
                out.println("<p>request.getUserPrincipal().getName(): " + Encode.forHtml(request.getUserPrincipal().getName()) + "</p>");

            }
            out.println("<hr/>");
        }

        //Create the body, depending upon whether the user has authenticated
        // or not.
        HttpSession session = request.getSession();
        if(session != null){
            log.debug("session.isNew(): {}", session.isNew());
            UserProfile userProfile = UserProfile.fromJson((String) session.getAttribute(USER_PROFILE));
            if( userProfile != null ){
                IdProvider userIdP = userProfile.getIdP();
                String name = userProfile.getUID();
                if (showUserProfileDetails) {
                    name = userProfile.getAttribute("first_name");
                    if(name!=null)
                        name = name.replaceAll("\"","");

                    String lastName =  userProfile.getAttribute("last_name");
                    if(lastName!=null)
                        name += " " + lastName.replaceAll("\"","");
                }

                if(showUserProfileDetails) {
                    out.println("<p>Greetings <strong>" + name + "</strong>, this is your profile page.</p>");
                }

    		    out.println("You logged into Hyrax with <em>"+userIdP.getDescription()+"</em>");
    		    out.println("<pre><b><a href=\"" + userIdP.getLogoutEndpoint() + "\">Click Here To Logout</a></b></pre>");

                if(showUserProfileDetails) {
                    out.println("<h3>"+name+"'s Profile</h3>");

                    String origUrl = Util.stringFromJson( (String) session.getAttribute(RETURN_TO_URL));

                    out.println("<dl>");
                    if(origUrl!=null){
                        out.println(oDTPS + RETURN_TO_URL + cDTPS +"<dd><pre><a href='"+origUrl+"'>"+origUrl+"</a></pre></dd>");
                    }

                    out.println(oDTPS + "token:"+ cDTPS +"<dd><pre>" + userProfile.getEDLAccessToken()+"</pre></dd>");


                    out.println(oDTPS + USER_PROFILE + ".toString()" + cDTPS + "<dd><pre>" + userProfile + "</pre></dd>");

                    out.println(oDTPS + USER_PROFILE + ".toJson(</strong>pretty=true<strong>):" + cDTPS + oDDPS + userProfile.toJson(true) + cDDPS);
                    out.println(oDTPS + USER_PROFILE + ".toJson(</strong>pretty=false<strong>):" + cDTPS + oDDPS + userProfile.toJson(false) + cDDPS);

                    out.println("</dl>");

                    //
                    // -- -- --  Print Session Info - BEGIN  -- -- -- -- -- -- -- -- -- -- -- --
                    //
                    out.println("<hr />");
                    out.println(oDTPS + "Session State Information" + cDTPS);
                    long timeNow = System.currentTimeMillis();
                    double sessionInUseTime = (timeNow-session.getCreationTime())/1000.0;
                    out.println("<pre>");
                    out.println("                     session.isNew():  " + session.isNew());
                    out.println("                     session.getId():  " + session.getId());
                    out.println("    session.getMaxInactiveInterval():  " + session.getMaxInactiveInterval() + " seconds.");
                    out.println("           session.getCreationTime():  " + session.getCreationTime() + " milliseconds since epoch.");
                    out.println("               maxSessionTimeSeconds:  " + maxSessionTimeSeconds + " seconds.");
                    out.println("                    sessionInUseTime:  " + sessionInUseTime + " seconds.");

                    out.println("</pre>");
                    out.println(oDTPS + "Session Attributes" + cDTPS);

                    Enumeration<String> attrNames = session.getAttributeNames();
                    if(attrNames.hasMoreElements()){
                        while(attrNames.hasMoreElements()){
                            String attrName = attrNames.nextElement();
                            out.print("    " + oDTPS + "\"" + attrName + "\": " + cDTPS + oDDPS + session.getAttribute(attrName) + cDDPS +"\n");
                            out.print((attrNames.hasMoreElements()?"\n":""));
                        }
                    }
                    // -- -- --  Print Session Info - END  -- -- -- -- -- -- -- -- -- -- -- --
                }
                out.println("<hr />");
            }
            else if(request.getUserPrincipal() != null){
                out.println("<p>Welcome " + Encode.forHtml(request.getUserPrincipal().getName()) + "</p>");
                out.println("<p><a href=\"" + srvcCntxtPth + "/logout\">logout</a></p>");
            }
            else {
                out.println( noProfileContent( srvcCntxtPth, session) );
            }
        }
        else {
            out.println(noProfileContent(srvcCntxtPth, session));
        }
        // Finish up the page
        out.println("</body></html>");
	}
}
