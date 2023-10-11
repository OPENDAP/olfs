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

import net.sf.saxon.s9api.SaxonApiException;
import opendap.PathBuilder;
import opendap.xml.Transformer;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * Created by ndp on 4/22/15.
 */
public class AuthenticationControls {

    public static final String CONFIG_ELEMENT = "EnableAuthenticationControls";

    private static Logger log;
    private static boolean initialized;

    private static String loginPath;
    private static String logoutPath;
    private static String defaultLoginPath;
    private static String defaultLogoutPath;

    private static String loginBanner;

    static {
        log = LoggerFactory.getLogger(AuthenticationControls.class);
        loginPath = null;
        logoutPath = null;
        initialized = false;
        defaultLoginPath = "/login";
        defaultLogoutPath = "/logout";
        loginBanner = "Welcome To The Burrow!";
    }

    private AuthenticationControls() {
    }


    public static boolean isIntitialized(){
        return initialized;
    }



    /**
     * Since a constructor cannot be defined for an interface there needs to
     * be a way to initialize the objects state. The init() method is that way.
     * The IsoDispatchHandler that creates an instance of IsoDispatchHandler will
     * pass itself into it along with the XML element that declared the
     * IsoDispatchHandler in the configuration file (usually olfs.xml). The
     * contents of this XML Element are not restricted and may (should?)
     * contain any required information for configuration not availableInChunk by
     * interogating the IsoDispatchHandler's methods.
     *
     * @param config A JDOM Element objct containing the XML Element that
     *               announced which implementation of IsoDispatchHandler to use. It may (or
     *               may not) contain additional confguration information.
     * @throws Exception When the bad things happen.
     * @see opendap.coreServlet.DispatchServlet
     */
    public static void init(Element config, String contextPath) {

       if(initialized) {
           log.warn("init() - AuthenticationControls have ALREADY been initialized. " +
                   "This subsequent attempt at initialization and the concomitant configuration have been ignored!");
           return;
       }

        if (config != null) {
            if(config.getName().equals(CONFIG_ELEMENT)) {

                loginPath = defaultLoginPath;
                Element e = config.getChild("login");
                if (e != null) {
                    loginPath = e.getTextTrim();
                }
                loginPath = PathBuilder.pathConcat(contextPath, loginPath);
                log.info("init() - Login Path: {}", loginPath);

                logoutPath = defaultLogoutPath;
                e = config.getChild("logout");
                if (e != null) {
                    logoutPath = e.getTextTrim();
                }
                logoutPath = PathBuilder.pathConcat(contextPath, logoutPath);
                log.info("init() - Logout Path: {}", logoutPath);


                // Init the Login Banner
                e = config.getChild("LoginBanner");
                if(e!=null){
                    loginBanner = e.getTextTrim();
                }
                log.info("init() - Login Banner: {}", loginBanner);

                initialized = true;
            }
        }
        else {
           // Despite The AuthenticationControls not being initialized we still need these values to be valid.
            loginPath = PathBuilder.pathConcat(contextPath, defaultLoginPath);
            logoutPath = PathBuilder.pathConcat(contextPath, defaultLogoutPath);
        }

    }

    public static String getLoginBanner(){
        return loginBanner;
    }

    public static String getLogoutEndpoint(){
        return logoutPath;
    }

    public static String getLoginEndpoint() {
        return loginPath;
    }


    public static void setLoginParameters(Transformer transformer, HttpServletRequest request) throws SaxonApiException {

        if(initialized) {
            String userId = Util.getUID(request);

            log.debug("xsltDir() - UserId: {}", userId);
            if (userId != null) {
                transformer.setParameter("userId", userId);
            }
            log.debug("xsltDir() - loginPath: {}", loginPath);
            if (loginPath != null) {
                transformer.setParameter("loginLink", loginPath);
            }
            log.debug("xsltDir() - logoutPath: {}", logoutPath);
            if (logoutPath != null) {
                transformer.setParameter("logoutLink", logoutPath);
            }
        }
        else {
            log.debug("setLoginParameters() - AuthenticationControls have not been initialized. " +
                    "No Parameters Will Be Set.");
        }

    }
}
