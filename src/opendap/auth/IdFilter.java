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

import org.apache.http.client.CredentialsProvider;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;


public class IdFilter implements Filter {

    private Logger _log;


    public static final String ORIGINAL_REQUEST_URL = "original_request_url";
    public static final String USER_PROFILE         = "user_profile";
    public static final String IDENTITY_PROVIDER    = "identity_provider";

    private ConcurrentHashMap<String,IdProvider> _idProviders;

    private String _loginBanner;




    public void init(FilterConfig filterConfig) throws ServletException {

        _log = LoggerFactory.getLogger(this.getClass());

        _idProviders = new ConcurrentHashMap<String, IdProvider>();

        String configFileName = filterConfig.getInitParameter("config");


        try {
            Element config = opendap.xml.Util.getDocumentRoot(configFileName);

            for (Object o : config.getChildren("IdProvider")) {

                Element idpConfig = (Element) o;

                IdProvider idp = idpFactory(idpConfig);

                _idProviders.put(idp.getId(), idp);

            }





        } catch (Exception e) {
            throw new ServletException(e);
        }



        _loginBanner =  filterConfig.getInitParameter("login_banner");
        if(_loginBanner==null){
            _loginBanner = "Welcome to The Burrow.";
        }

    }



    public IdProvider idpFactory(Element config) throws ConfigurationException {
        String msg;


        if(config==null) {
            msg = "Configuration MAY NOT be null!.";
            _log.error("idpFactory():  {}",msg);
            throw new ConfigurationException(msg);
        }


        String idpClassName = config.getAttributeValue("class");

        if(idpClassName==null) {
            msg = "IdProvider definition must contain a \"class\" attribute whose value is the class name of the IdProvider implementation to be created.";
            _log.error("idpFactory(): {}",msg);
            throw new ConfigurationException(msg);
        }

        try {

            _log.debug("idpFactory(): Building PolicyDecisionPoint: " + idpClassName);
            Class classDefinition = Class.forName(idpClassName);
            IdProvider idp = (IdProvider) classDefinition.newInstance();

            idp.init(config);

            return idp;


        } catch (Exception e) {
            msg = "Unable to manufacture an instance of "+idpClassName+"  Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
            _log.error("pdpFactory(): {}"+msg);
            throw new ConfigurationException(msg, e);

        }


    }
















    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {


        HttpServletRequest  hsReq = (HttpServletRequest)  request;
        HttpServletResponse hsRes = (HttpServletResponse) response;

        String requestURI = hsReq.getRequestURI();
        String contextPath =  hsReq.getContextPath();

        // Get session, make new as needed.
        HttpSession session = hsReq.getSession(true);


        // Cache the original  request URI in the session if it's not there already
        String originalResourceRequestUrl = (String) session.getAttribute(ORIGINAL_REQUEST_URL);
        if(originalResourceRequestUrl==null){
            originalResourceRequestUrl = hsReq.getRequestURL().toString();
            session.setAttribute(ORIGINAL_REQUEST_URL,originalResourceRequestUrl);
        }


        // Intercept login/logout requests
        if( requestURI.equals(contextPath+"/logout") )
        {
            doLogout(hsReq, hsRes);
            return;
        }
        else if( requestURI.equals(contextPath+"/login") )
        {
            doLandingPage(hsReq, hsRes);
            return;
        }
        else if( requestURI.equals(contextPath+"/guest") )
        {
            doGuest(hsReq, hsRes);
            return;
        }
        else
        {

            // Check IdProviders to see is this request is a login context.

            for(IdProvider idProvider: _idProviders.values()){

                String loginContext =  contextPath+"/login"+ idProvider.getLoginContext();

                if( requestURI.equals(loginContext)) {
                    if(originalResourceRequestUrl.equals(loginContext))
                        session.setAttribute(ORIGINAL_REQUEST_URL,contextPath);

                    try {

                        /**
                         * Run the login gizwhat. This may involve simply collecting credentials from the user and
                         * forwarding them on to the IdP, or it may involve a complex dance of redirection in which
                         * the user drives their browser through an elaborate auth like OAuth2 so they can come back
                         * to this very spot with some kind of cookie/token/thingy that lets the doLogin invocation
                         * complete.
                         */
                        idProvider.doLogin(hsReq,hsRes);
                        /**
                         * We return here and don't do the filter chain because the "doLogin" method will, when
                         * completed send a 302 redirect to the client. Thus we want the process to stop here until
                         * login is completed
                         */
                        return;

                    } catch (Exception e) {
                        _log.error("doFilter() - {} Login Interaction FAILED! Message: {}",idProvider.getId(), e.getMessage());
                        throw new IOException(e);
                    }
                }


            }


        }

        /**
         * We get here because the user is NOT trying to login. Since Tomcat and the Servlet API have their own
         * "login" scheme (name & password based) API we need to check if _our_ login thing ran and if so (detected by
         * the presence of the user_profile attribute in the session) we need to spoof the API to show our
         * authenticated user.
         */
        UserProfile up = (UserProfile) session.getAttribute(USER_PROFILE);

        if(up != null) {
            AuthenticatedHttpRequest authReq = new AuthenticatedHttpRequest(hsReq);
            authReq.setUid(up.getUID());
            hsReq = authReq;
        }

        filterChain.doFilter(hsReq, hsRes);

    }

    public void destroy() {
        _log = null;
    }





    /**
     * Logs a user out.
     * This method simply terminates the local session and redirects the user back
     * to the home page.
     */
    private void doLogout(HttpServletRequest request, HttpServletResponse response)
	        throws IOException
    {

        _log.info("doLogout() - BEGIN");

        _log.info("doLogout() - Retrieving session...");
        HttpSession session = request.getSession(false);
        if( session != null )
        {
            _log.info("doLogout() - Got session...");

            UserProfile up = (UserProfile) session.getAttribute(USER_PROFILE);
            if(up!=null){
                _log.info("doLogout() - Logging out user '{}'",up.getUID());
                IdProvider idProvider = up.getIdP();
                if(idProvider!=null){
                    _log.info("doLogout() - Calling '{}' logout handler.",idProvider.getId());
                    idProvider.doLogout(request,response);
                    _log.info("doLogout() - END");
                    return ;
                }

            } else {
                _log.info("doLogout() - Missing UserProfile object....");

            }
        }
        _log.info("doLogout() - Punting with session.invalidate()");

        // This is essentially a "punt" since things aren't as expected.
        session.invalidate();
        response.sendRedirect(request.getContextPath());

        _log.info("doLogout() - END");

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
	private void doGuest(HttpServletRequest request, HttpServletResponse response) throws IOException

    {


        HttpSession session = request.getSession(false);
        String redirectUrl = null;

        if(session != null) {
            redirectUrl = (String) session.getAttribute(ORIGINAL_REQUEST_URL);
            session.invalidate();
        }

        if(redirectUrl == null){
            redirectUrl = request.getContextPath();
        }


        session = request.getSession(true);


        session.setAttribute(ORIGINAL_REQUEST_URL, redirectUrl);
        session.setAttribute(USER_PROFILE, new GuestProfile());

        /**
         * Finally, redirect the user back to the their original requested resource.
         */


        response.sendRedirect(redirectUrl);

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


        _log.debug("doLandingPage() - Building noProfile String.");
        StringBuilder noProfile = new StringBuilder();

        noProfile.append("<p><b>You are not currently logged on.</b></p><br />");

        noProfile.append("<i>You may login using one of these identity providers:</i>");

        noProfile.append("<ul>");

        for(IdProvider idProvider: _idProviders.values()) {
            String contextPath = request.getContextPath();

            String loginContext = contextPath + "/login" + idProvider.getLoginContext();

            noProfile.append("<li><a href=\"").append(loginContext).append("\">");
            noProfile.append(idProvider.getDescription());
            noProfile.append("</a><br/><br/></li>");


        }


        noProfile.append("</ul>");

        noProfile.append("<i>Or you may:</i><br />");
        noProfile.append("<ul>");
        noProfile.append("<li><a href=\"").append(request.getContextPath()).append("/guest\">Use a 'guest' profile.</a> </li>");
        noProfile.append("</ul>");

        _log.debug("doLandingPage() - Setting Response Headers...");

        response.setContentType("text/html");
        response.setHeader("Content-Description", "Login Page");
        response.setHeader("Cache-Control", "max-age=0, no-cache, no-store");


        _log.debug("doLandingPage() - Writing page contents.");

        /**
         * Generate the html page header
         */
		PrintWriter out = response.getWriter();
		out.println("<html><head><title></title></head>");
		out.println("<body><h1>"+_loginBanner+"</h1>");
		out.println("<br/>");



        out.println("<p>request.getRemoteUser(): " + request.getRemoteUser() + "</p>");
        out.println("<p>request.getUserPrincipal(): " + request.getUserPrincipal() + "</p>");
        if(request.getUserPrincipal() !=null){
            out.println("<p>request.getUserPrincipal().getName(): " + request.getUserPrincipal().getName() + "</p>");

        }


        /**
         * Create the body, depending upon whether the user has authenticated
         * or not.
         */

        if(session != null){


            UserProfile userProfile = (UserProfile) session.getAttribute(USER_PROFILE);
            if( userProfile != null ){
                String first_name = userProfile.getAttribute("first_name");
                String last_name =  userProfile.getAttribute("last_name");

    		    out.println("<p>Welcome " + first_name + " " + last_name + "</p>");
    		    out.println("<p><a href=\"" + request.getContextPath() + "/logout\">logout</a></p>");

                out.println("<h3>Profile</h3>");

                Enumeration attrNames = session.getAttributeNames();

                if(attrNames.hasMoreElements()){
                    out.println("<dl>");
                    while(attrNames.hasMoreElements()){
                        String attrName = attrNames.nextElement().toString();
                        String attrValue = session.getAttribute(attrName).toString();
                        out.println("<dt><b>"+attrName+"</b></dt><dd>"+attrValue+"</dd>");


                    }
                    out.println("</dl>");

                }


            }
            else if(request.getUserPrincipal() !=null){
                out.println("<p>Welcome " + request.getUserPrincipal().getName() + "</p>");
                out.println("<p><a href=\"" + request.getContextPath() + "/logout\">logout</a></p>");

            }
            else {
                out.println(noProfile.toString());
            }

        }
        else {
            out.println(noProfile.toString());
        }



        /**
         * Finish up the page
         */
        out.println("</body></html>");
	}


}
