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
import opendap.logging.ServletLogUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;


public class IdFilter implements Filter {

    private Logger log;
    private static final String logName = "AuthenticationLog";
    private AtomicLong counter;

    public static final String RETURN_TO_URL      = "return_to_url";
    public static final String USER_PROFILE       = "user_profile";
    public static final String IDENTITY_PROVIDER  = "identity_provider";

    private boolean isInitialized;
    private FilterConfig filterConfig;

    private static final String CONFIG_PARAMETER_NAME = "config";
    private static final String DEFAULT_CONFIG_FILE_NAME = "user-access.xml";


    private String guestEndpoint;
    private boolean enableGuestProfile;

    public IdFilter(){
        isInitialized = false;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        try {
            init();
        }
        catch (IOException | JDOMException se){
            log.warn("init() - INITIALIZATION HAS BEEN POSTPONED! FAILED TO INITIALIZE IdFilter! " +
                    "Caught {} Message: {} ",se.getClass().getName(),se.getMessage());
        }

    }

    private void init() throws IOException, JDOMException {

        if(isInitialized)
            return;

        counter = new AtomicLong(0);
        ServletLogUtil.initLogging(filterConfig.getServletContext());
        log = LoggerFactory.getLogger(this.getClass());
        log.info("init() - Initializing IdFilter...");

        String context = filterConfig.getServletContext().getContextPath();

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


        Element e = config.getChild("EnableGuestProfile");
        if(e!=null){
            enableGuestProfile = true;
            guestEndpoint = PathBuilder.pathConcat(context, "guest");
        }
        log.info("init() - Guest Profile {}", enableGuestProfile ?"Has Been ENABLED!":"Is DISABLED!");

        // Set up authentication controls. If the configuration element is missing that's fine
        // because we know that it will still configure the login/logout endpoint values.
        AuthenticationControls.init(config.getChild(AuthenticationControls.CONFIG_ELEMENT),context);

        IdPManager.setServiceContext(context);
        // Load ID Providers (Might be several)
        for (Object o : config.getChildren("IdProvider")) {
            Element idpConfig = (Element) o;
            IdPManager.addProvider(idpConfig);
        }
        isInitialized = true;
        log.info("init() - IdFilter HAS BEEN INITIALIZED!");
    }


    public void doFilter(ServletRequest sreq, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

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
            RequestCache.openThreadCache();

            HttpServletRequest request = (HttpServletRequest) sreq;
            HttpServletRequest hsReq = request;

            String requestId = this.getClass().getName()+"-"+counter.incrementAndGet();
            ServletLogUtil.logServerAccessStart(request,logName,request.getMethod(), requestId);

            // Get session, make new as needed.
            HttpSession session = hsReq.getSession(true);
            log.debug("BEGIN (requestId: {}) (session: {})",requestId, session.getId());

            Util.debugHttpRequest(request,log);

            HttpServletResponse hsRes = (HttpServletResponse) response;
            String requestURI = hsReq.getRequestURI();
            String contextPath = hsReq.getContextPath();

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

            // Intercept login/logout requests
            if (requestURI.equals(AuthenticationControls.getLogoutEndpoint())) {
                doLogout(hsReq, hsRes);
                return;
            } else if (AuthenticationControls.isIntitialized() && requestURI.equals(AuthenticationControls.getLoginEndpoint())) {
                doLandingPage(hsReq, hsRes);
                return;
            } else if (enableGuestProfile && requestURI.equals(guestEndpoint)) {
                doGuestLogin(hsReq, hsRes);
                return;
            } else {
                // Check IdProviders to see if this request is a valid login context.
                for (IdProvider idProvider : IdPManager.getProviders()) {

                    String loginEndpoint = idProvider.getLoginEndpoint();
                    if(requestURI.equals(loginEndpoint)) {
                        synchronized (session) {
                            // Check the RETURN_TO_URL and if it's the login endpoint
                            // return to the root dir of the web application after
                            // authenticating.
                            String returnToUrl = (String) session.getAttribute(RETURN_TO_URL);
                            log.debug("Retrieved RETURN_TO_URL: {} (session: {})",returnToUrl,session.getId());
                            if (returnToUrl != null && returnToUrl.equals(loginEndpoint)) {
                                String msg = "Setting session RETURN_TO_URL("+RETURN_TO_URL+ ") to: "+contextPath;
                                msg += " (session: "+session.getId()+")";
                                log.debug(msg);
                                session.setAttribute(RETURN_TO_URL, contextPath);
                            }
                        }
                        try {
                            //
                            // Run the login gizwhat. This may involve simply collecting credentials from the user and
                            // forwarding them on to the IdP, or it may involve a complex dance of redirection in which
                            // the user drives their browser through an elaborate auth like OAuth2 so they can come back
                            // to this very spot with some kind of cookie/token/thingy that lets the doLogin invocation
                            // complete.
                            //
                            idProvider.doLogin(hsReq, hsRes);
                            //
                            // We return here and don't do the filter chain because the "doLogin" method will, when
                            // completed send a 302 redirect to the client. Thus we want the process to stop here until
                            // login is completed
                            //
                            log.debug("END (session: {})",session.getId());
                            return;

                        } catch (IOException e) {
                            String msg = "Your Login Transaction FAILED!   " +
                                    "Authentication Context: '"+idProvider.getAuthContext()+
                                    "' Message: "+ e.getMessage();
                            log.error("doFilter() - {}", msg);
                            OPeNDAPException.setCachedErrorMessage(msg);
                            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_UNAUTHORIZED,msg);
                            log.debug("END (session: {})",session.getId());
                            return;
                        }
                    }
                }
            }

            //
            // We get here because the user is NOT trying to login. Since Tomcat and the Servlet API have their own
            // "login" scheme (name & password based) API we need to check if _our_ login thing ran and if so (detected by
            // the presence of the USER_PROFILE attribute in the session) we need to spoof the API to show our
            // authenticated user.
            //
            UserProfile up = (UserProfile) session.getAttribute(USER_PROFILE);
            if (up != null) {
                log.debug("Found UserProfile object in Session, this is an authenticated request for user: {}",up.getUID());
                AuthenticatedHttpRequest authReq = new AuthenticatedHttpRequest(hsReq);
                authReq.setUid(up.getUID());
                hsReq = authReq;
            }
            else {
                log.debug("No UserProfile object found in Session. Request is not authenticated.");
            }
            // Cache the  request URL in the session. We do this here because we know by now that the request was
            // not for a "reserved" endpoint for login/logout etc. and we DO NOT want to cache those locations.
            synchronized(session) {
                Util.cacheRequestUrlAsNeeded(session,requestUrl, requestURI,contextPath);
            }
            filterChain.doFilter(hsReq, hsRes);
            log.debug("END (session: {})",session.getId());
            ServletLogUtil.logServerAccessEnd(200,logName);

        }
        finally {
            RequestCache.closeThreadCache();
        }
    }



    /**
     *
     */
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
        String redirectUrl  = request.getContextPath();
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.info("doLogout() - Got session...");
            String href = (String) session.getAttribute(RETURN_TO_URL);
            redirectUrl = href!=null?href:redirectUrl;

            UserProfile up = (UserProfile) session.getAttribute(USER_PROFILE);
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
     */
	private void doGuestLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        HttpSession session = request.getSession(false);
        String redirectUrl = request.getContextPath();
        if(session != null) {
            redirectUrl = (String) session.getAttribute(RETURN_TO_URL);
            session.invalidate();
        }
        HttpSession guestSession = request.getSession(true);
        synchronized (guestSession) {
            guestSession.setAttribute(RETURN_TO_URL, redirectUrl);
            guestSession.setAttribute(USER_PROFILE, new GuestProfile());
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
     * Displays the application home page.
     * This method displays a welcome page for users. If the user has authenticated,
     * then it will display his/her name, and provide a logout link. If the user
     * has not authenticated, then a login link will be displayed.
     *
     */
	private void doLandingPage(HttpServletRequest request, HttpServletResponse response)
	        throws IOException
    {
        HttpSession session = request.getSession();
        log.debug("doLandingPage() - Building noProfile String.");

        String dtb = "<dt><b>";

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
            noProfile.append("<li><a href=\"").append(request.getContextPath()).append("/guest\">Use a 'guest' profile.</a> </li>");
            noProfile.append("</ul>");
        }
        if(session!=null){
            String origUrl = (String) session.getAttribute(RETURN_TO_URL);
            noProfile.append("<dl>");
            if(origUrl!=null){
                noProfile.append(dtb).append("After authenticating you will be returned to:").append("</b></dt><dd><pre><a href='").append(origUrl).append("'>").append(origUrl).append("</a></pre></dd>");
            }
            noProfile.append("</dl>");
        }


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
		out.println("<html><head><title></title></head>");
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
        if(session != null){
            UserProfile userProfile = (UserProfile) session.getAttribute(USER_PROFILE);
            if( userProfile != null ){
                IdProvider userIdP = userProfile.getIdP();
                String firstName = userProfile.getAttribute("first_name");
                if(firstName!=null)
                    firstName = firstName.replaceAll("\"","");

                String lastName =  userProfile.getAttribute("last_name");
                if(lastName!=null)
                    lastName = lastName.replaceAll("\"","");

    		    out.println("<p>Greetings " + firstName + " " + lastName + ", this is your profile.</p>");
    		    out.println("You logged into Hyrax with <em>"+userIdP.getDescription()+"</em>");
    		    out.println("<p><b><a href=\"" + userIdP.getLogoutEndpoint() + "\"> - - Click Here To Logout - - </a></b></p>");
                out.println("<h3>"+firstName+"'s Profile</h3>");

                String origUrl = (String) session.getAttribute(RETURN_TO_URL);

                out.println("<dl>");
                if(origUrl!=null){
                    out.println(dtb + RETURN_TO_URL +"</b></dt><dd><pre><a href='"+origUrl+"'>"+origUrl+"</a></pre></dd>");
                }
                out.println(dtb + USER_PROFILE+"</b></dt><dd><pre>"+userProfile+"</pre></dd>");
                out.println("</dl>");

                out.println("<hr />");
                out.println("<pre>");
                out.print("<b>All_Session_Attributes</b>: [ ");

                Enumeration attrNames = session.getAttributeNames();
                if(attrNames.hasMoreElements()){
                    while(attrNames.hasMoreElements()){
                        String attrName = attrNames.nextElement().toString();
                        out.print("\""+attrName+"\"");
                        out.print((attrNames.hasMoreElements()?", ":""));
                    }
                }
                out.println(" ]</pre>");
                out.println("<hr />");
            }
            else if(request.getUserPrincipal() != null){
                out.println("<p>Welcome " + Encode.forHtml(request.getUserPrincipal().getName()) + "</p>");
                out.println("<p><a href=\"" + request.getContextPath() + "/logout\">logout</a></p>");
            }
            else {
                out.println(noProfile.toString());
            }
        }
        else {
            out.println(noProfile.toString());
        }
        // Finish up the page
        out.println("</body></html>");
	}
}
