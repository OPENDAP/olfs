/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
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
package opendap.coreServlet;


import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Enumeration;

public class ServletUtil {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(ServletUtil.class);

    private static final String VALUE_FRAGMENT = " value: ";

    // We use this to ensure an instance of this collection of "functions" is never instantiated.
    private ServletUtil(){}



    /**
     * Returns the path to the "Content" directory for the OLFS. This is the location that the OLFS uses to
     * keep service related content such as:
     *   <ui>
     *     <li>Configuration information</li>
     *     <li>THREDDS catalog files.</li>
     *     <li>Log files</li>
     *   </ui>
     *
     * Things here will not be overwritten when the server is upgraded. (Although some upgrades may require that
     * content in this directory be modifed before the upgrade can work.) Thus this directory is also referred to
     * as the "peristent content path" or "peristent content directory" in other parts of the documenttion.
     *
     * @param servlet  The HttpServlet that is running.
     * @return  A String containing the content path (aka the peristent content path) for the web application.
     */
    public static String getConfigPath(HttpServlet servlet) {
        return getConfigPath(servlet.getServletContext());
    }


    /**
     * Returns the path to the "Content" directory for the OLFS. This is the location that the OLFS uses to
     * keep service related content such as:
     *   <ui>
     *     <li>Configuration information</li>
     *     <li>THREDDS catalog files.</li>
     *     <li>Log files</li>
     *   </ui>
     *
     * Things here will not be overwritten when the server is upgraded. (Although some upgrades may require that
     * content in this directory be modified before the upgrade can work.) Thus this directory is also referred to
     * as the "persistent content path" or "persistent content directory" in other parts of the documentation.
     *
     * @param sc  The ServletContext for this servlet that is running.
     * @return  A String containing the content path (aka the persistent content path) for the web application.
     */
    public static String getConfigPath(ServletContext sc) {

        String etcOlfsConfigDir  = "/etc/olfs/";
        String usrShareConfigDir = "/usr/share/olfs/";
        String webappConfDir     = "WEB-INF/conf";

        String configMsgBase = "Using config location: {}";

        // Check to see if the /etc/olfs location is available
        log.debug("Trying location: {}", etcOlfsConfigDir);
        if (pathIsGood(etcOlfsConfigDir)) {
            // It's good so we'll use it.
            log.info(configMsgBase,  etcOlfsConfigDir);
            return etcOlfsConfigDir;
        }

        // Nope... Check to see if the /usr/share/olfs location is available
        log.debug("The location {} was not available. Trying location: {}",etcOlfsConfigDir,usrShareConfigDir);
        if (pathIsGood(usrShareConfigDir)) {
            // It's good so we'll use it.
            log.info(configMsgBase, usrShareConfigDir);
            return usrShareConfigDir;
        }

        // And NOPE.
        // The default locations and the environment defined location did not work out so we fall back to the
        // default configuration location in the web application deployment directory.
        String configDirName = sc.getRealPath(webappConfDir);
        log.warn("Failed to locate localized configuration directory. Falling back to bundled application config in: {}", configDirName);
        String configPath="FAILED_To_Determine_Config_Path!";

        File cf = new File(configDirName);
        try {
            configPath = cf.getCanonicalPath() + "/";
            // @TODO Understand (again) why the backslash replacement happens below and investigate if asking for a path separator from some java api the way to go.
            configPath = configPath.replace('\\', '/');
        } catch (IOException e) {
            log.error("Failed to produce a config path! Error: {}", e.getMessage());
        }
        return configPath;
    }

    private static boolean pathIsGood(String path){
        File pathDir = new File(path);
        if(pathDir.exists()){
            if(pathDir.isDirectory()){
                if(pathDir.canRead()){
                    if(pathDir.canWrite()){
                        return true;
                    }
                    else {
                        log.info("The directory {} cannot be written to by the user {}", path,System.getProperty("user.name"));
                    }
                }
                else {
                    log.info("The directory {} cannot be read to by the user {}", path,System.getProperty("user.name"));
                }
            }
            else {
                log.info("The path {} is not a directory.",path);
            }
        }
        else {
            log.info("The path {} does not exist.",path);
        }
        return   false;
    }


    /**
     * Returns the path to the web applications "context" directory as defined by the value of the
     * web appications &lt;initParameter&gt; ContextPath. This directory is where the web application is unpacked. It
     * contains:
     * <ui>
     *   <li> All of the libraries (jar files, class files, etc.).</li>
     *   <li> Initial content used to bootstrap a new installation.</li>
     *   <li> XSL, HTML, CSS, XML, nad other documents.</li>
     *   <li> Images.</li>
     *   <li> Other resources bundled with the web application</li>
     * </ui>
     * Code in many DispatchHandlers uses this path string to locate required files for use during
     * runtime.
     *
     * @param servlet Servlet instance to evaluate
     * @return Returns the path to the web applications "context" directory
     */
    public static String getContextPath( HttpServlet servlet ) {
      return getContextPath(servlet.getServletContext());
    }


    /**
     * Returns the path to the web applications "context" directory as defined by the value of the
     * web appications &lt;initParameter&gt; ContextPath. This directory is where the web application is unpacked. It
     * contains:
     * <ui>
     *   <li> All of the libraries (jar files, class files, etc.).</li>
     *   <li> Initial content used to bootstrap a new installation.</li>
     *   <li> XSL, HTML, CSS, XML, nad other documents.</li>
     *   <li> Images.</li>
     *   <li> Other resources bundled with the web application</li>
     * </ui>
     * Code in many DispatchHandlers uses this path string to locate required files for use during
     * runtime.
     *
     * @param sc ServletContext to evaluate
     * @return Returns the path to the web applications "context" directory
     */
    public static String getContextPath( ServletContext sc ) {
      String contextPath = sc.getContextPath();
      log.debug("context path: '{}'",contextPath);
      return contextPath;
    }


    public static String getSystemPath(HttpServlet servlet, String path) {
        ServletContext sc = servlet.getServletContext();
        return getSystemPath(sc, path);
    }

    public static String getSystemPath(ServletContext sc, String path) {
        String spath = sc.getRealPath(path);
        spath = spath.replace('\\', '/');
        return spath;
    }


    public static String getRootPath(HttpServlet servlet) {
        return getRootPath(servlet.getServletContext());
    }

    public static String getRootPath(ServletContext sc) {
        String rootPath = sc.getRealPath("/");
        rootPath = rootPath.replace('\\', '/');
        return rootPath;
    }


    public static String toString(HttpServlet servlet) {
        StringBuilder s = new StringBuilder("ServletUtil:\n");
        s.append("    getContentPath(): ").append(getConfigPath(servlet)).append("\n");
        s.append("    getContextPath(): ").append(getContextPath(servlet)).append("\n");
        s.append("    getRootPath(): ").append(getRootPath(servlet)).append("\n");
        s.append("    getSystemPath(): ").append(getSystemPath(servlet, "/")).append("\n");
        s.append(probeServlet(servlet));
        return s.toString();
    }


    public static String probeServlet(HttpServlet servlet) {

        String pName;
        String pVal;

        StringBuilder s = new StringBuilder("HttpServlet:\n");
        s.append("    getServletInfo(): ").append(servlet.getServletInfo()).append("\n");
        s.append("    getServletName(): ").append(servlet.getServletName()).append("\n");

        Enumeration e = servlet.getInitParameterNames();
        s.append("    Servlet Parameters:\n");
        if (e.hasMoreElements()) {
            while (e.hasMoreElements()) {
                pName = (String) e.nextElement();
                pVal = servlet.getInitParameter(pName);
                s.append("        name: ").append(pName).append(VALUE_FRAGMENT).append(pVal).append("\n");
            }
        } else
            s.append("        No Servlet Parameters Found.\n");

        ServletConfig scfg = servlet.getServletConfig();
        ServletContext scntxt = servlet.getServletContext();
        s.append("    HttpServlet.getServletConfig(): ").append(scfg).append("\n");
        s.append(probeServletConfig(scfg));
        s.append("    HttpServlet.ServletContext(): ").append(scntxt).append("\n");
        s.append(probeServletContext(scntxt));
        return s.toString();
    }

    public static String probeServletConfig(ServletConfig scfg) {
        StringBuilder s = new StringBuilder();
        Enumeration e;
        String pName, pVal;

        s.append("    ServletConfig:\n");
        s.append("       getServletName():").append(scfg.getServletName()).append("\n");
        e = scfg.getInitParameterNames();
        s.append("       Servlet Parameters:\n");
        if(e.hasMoreElements()){
            while(e.hasMoreElements()){
                pName = (String) e.nextElement();
                pVal = scfg.getInitParameter(pName);
                s.append("            name: ").append(pName).append(VALUE_FRAGMENT).append(pVal).append("\n");
            }
        }
        else
            s.append("            No Servlet Parameters Found.\n");

        ServletContext scntxt = scfg.getServletContext();
        s.append("       ServletConfig.getServletContext(): ").append(scntxt).append("\n");
        s.append(probeServletContext(scntxt));

        return s.toString();

    }

    public static String probeServletContext(ServletContext sc) {
        StringBuilder s = new StringBuilder();
        String pVal, pName;
        Enumeration e;

        s.append("    ServletContext:\n");

        s.append("       getServletContextName():").append(sc.getServletContextName()).append("\n");
        s.append("       getContextPath():").append(sc.getContextPath()).append("\n");

        e = sc.getAttributeNames();
        s.append("       ServletContext Attributes:\n");
        if (e.hasMoreElements()) {
            while (e.hasMoreElements()) {
                pName = (String) e.nextElement();
                s.append("            name: ").append(pName).append("\n");
            }

        } else
            s.append("        No Servlet Context Attributes Found.\n");

        e = sc.getInitParameterNames();
        s.append("       ServletContext Parameters:\n");
        if (e.hasMoreElements()) {
            while (e.hasMoreElements()) {
                pName = (String) e.nextElement();
                pVal = sc.getInitParameter(pName);
                s.append("           name: ").append(pName).append(VALUE_FRAGMENT).append(pVal).append("\n");
            }
        } else
            s.append("           No ServletContext Parameters Found.\n");

        try {
            s.append("       getResource(\"/\"): ").append(sc.getResource("/")).append("\n");
        } catch (MalformedURLException e1) {
            log.error("Could not perform ServletContext.getResource(\"/\"). Error Message: {}", e1.getMessage());
        }

        s.append("       getMajorVersion(): ").append(sc.getMajorVersion()).append("\n");
        s.append("       getMinorVersion(): ").append(sc.getMinorVersion()).append("\n");
        s.append("       getServerInfo(): ").append(sc.getServerInfo()).append("\n");
        return s.toString();
    }


    /**
     * Writes information about the incomming request to a String.
     *
     * @param req   The current request
     * @param reqno The request number.
     * @return A string containing infformation about the passed HttpServletRequest req
     */
    public static String showRequest(HttpServletRequest req, long reqno) {

        StringBuilder sb = new StringBuilder();

        sb.append("\n-------------------------------------------\n");
        sb.append("showRequest()\n");
        sb.append("  Request #").append(reqno).append("\n");
        sb.append("  HttpServletRequest Object:\n");
        sb.append("    getServerName():          ").append(req.getServerName()).append("\n");
        sb.append("    getServerPort():          ").append(req.getServerPort()).append("\n");
        sb.append("    getProtocol():            ").append(req.getProtocol()).append("\n");
        sb.append("    getScheme():              ").append(req.getScheme()).append("\n");

        sb.append("    getRemoteHost():          ").append(req.getRemoteHost()).append("\n");
        sb.append("    getRemoteUser():          ").append(req.getRemoteUser()).append("\n");
        sb.append("    getRequestURL():          ").append(req.getRequestURL()).append("\n");
        sb.append("    getRequestURI():          ").append(req.getRequestURI()).append("\n");
        sb.append("    getContextPath():         ").append(req.getContextPath()).append("\n");
        sb.append("    getQueryString():         ").append(req.getQueryString()).append("\n");
        sb.append("    getAuthType():            ").append(req.getAuthType()).append("\n");
        sb.append("    getMethod():              ").append(req.getMethod()).append("\n");
        sb.append("    getPathInfo():            ").append(req.getPathInfo()).append("\n");
        sb.append("    getPathTranslated():      ").append(req.getPathTranslated()).append("\n");
        // We used to ask the request to return the value of getRequestedSessionId() but not so much now, thanks sonar...

        sb.append("    getServletPath():         ").append(req.getServletPath()).append("\n");

        sb.append("    getCharacterEncoding():   ").append(req.getCharacterEncoding()).append("\n");
        sb.append("    getContentType():         ").append(req.getContentType()).append("\n");
        sb.append("    getLocalAddr():           ").append(req.getLocalAddr()).append("\n");
        sb.append("    getLocalName():           ").append(req.getLocalName()).append("\n");

        sb.append("  HttpServletRequest Attributes: \n");

        Enumeration attrNames = req.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            Object value = req.getAttribute(attrName);
            sb.append("    ").append(value.getClass().getName()).append(" ").append(attrName).append("='");
            sb.append(value.toString()).append("'\n");

        }
        sb.append(showSession(req.getSession(true)));

        sb.append("  Request Info:\n");
        sb.append("    localUrl:                  '").append(ReqInfo.getLocalUrl(req)).append("'\n");
        sb.append("    dataSetName:               '").append(ReqInfo.getDataSetName(req)).append("'\n");
        sb.append("    requestSuffixRegex:             '").append(ReqInfo.getRequestSuffix(req)).append("'\n");
        sb.append("    CE:                        '");
        try {
            sb.append(ReqInfo.getConstraintExpression(req)).append("'\n");
        } catch (IOException e) {
            sb.append("Encountered IOException when attempting get the constraint expression! Msg: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n");
        sb.append("ReqInfo:\n");
        sb.append(ReqInfo.toString(req));
        sb.append("-------------------------------------------");

        return sb.toString();

    }


    public static String showSession(HttpSession session) {

        StringBuilder sb = new StringBuilder();
        sb.append("  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .\n");
        sb.append("  Session: \n");
        sb.append("    getId():                   ").append(session.getId()).append("\n");
        sb.append("    getCreationTime():         ").append(session.getCreationTime()).append("\n");
        sb.append("    getLastAccessedTime():     ").append(session.getLastAccessedTime()).append("\n");
        sb.append("    getMaxInactiveInterval():  ").append(session.getMaxInactiveInterval()).append("\n");

        sb.append("    Attributes: \n");
        Enumeration attrNames = session.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            Object value = session.getAttribute(attrName);
            sb.append("      ").append(value.getClass().getName()).append(" ").append(attrName).append("=\"");
            sb.append(value.toString()).append("\"\n");

        }
        sb.append("  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .\n");
        return sb.toString();
    }


    /**
     * ************************************************************************
     * This is a bit of instrumentation that I kept around to let me look at the
     * state of the incoming <code>HttpServletRequest</code> from the client.
     * This method calls the <code>get*</code> methods of the request and prints
     * the results to standard out.
     *
     * @param servlet The Servlet to Probe
     * @param request The <code>HttpServletRequest</code> object to probe.
     * @return A string containing the probe inormation
     */
    public static String probeRequest(HttpServlet servlet, HttpServletRequest request) {

        Enumeration e;
        int i;
        String minorSeparator = ".............................\n";


        StringBuilder probeMsg = new StringBuilder();
        probeMsg.append("####################### PROBE ##################################\n");

        if (request == null) {
            probeMsg.append("The passed HttpServletRequest instance was 'null'. Nothing to report.\n");
        } else {

            probeMsg.append("\n");
            probeMsg.append("The HttpServletRequest object is actually an instance of:").append("\n");
            probeMsg.append("    ").append(request.getClass().getName()).append("\n");
            probeMsg.append("\n");
            probeMsg.append("HttpServletRequest Interface: \n");
            probeMsg.append("    getAuthType:           ").append(request.getAuthType()).append("\n");
            probeMsg.append("    getContextPath:        ").append(request.getContextPath()).append("\n");
            probeMsg.append("    getMethod:             ").append(request.getMethod()).append("\n");
            probeMsg.append("    getPathInfo:           ").append(request.getPathInfo()).append("\n");
            probeMsg.append("    getPathTranslated:     ").append(request.getPathTranslated()).append("\n");
            probeMsg.append("    getRequestURL:         ").append(request.getRequestURL()).append("\n");
            probeMsg.append("    getQueryString:        ").append(request.getQueryString()).append("\n");
            probeMsg.append("    getRemoteUser:         ").append(request.getRemoteUser()).append("\n");

            probeMsg.append("    getRequestURI:         ").append(request.getRequestURI()).append("\n");
            probeMsg.append("    getServletPath:        ").append(request.getServletPath()).append("\n");
            probeMsg.append("    isRequestedSessionIdFromCookie: ").append(request.isRequestedSessionIdFromCookie()).append("\n");
            probeMsg.append("    isRequestedSessionIdValid:      ").append(request.isRequestedSessionIdValid()).append("\n");
            probeMsg.append("    isRequestedSessionIdFromURL:    ").append(request.isRequestedSessionIdFromURL()).append("\n");
            probeMsg.append("\n");
            probeMsg.append("    Cookies:\n");

            Cookie c[] = request.getCookies();
            if (c == null)
                probeMsg.append("   None.").append("\n");
            else {
                probeMsg.append("\n");
                for (i = 0; i < c.length; i++)
                    probeMsg.append("        cookie[").append(i).append("]: ").append(c[i]).append("\n");
            }

            probeMsg.append("\n");
            i = 0;
            e = request.getHeaderNames();
            probeMsg.append("    Header Names:").append("\n");
            while (e.hasMoreElements()) {
                i++;
                String s = (String) e.nextElement();
                probeMsg.append("       Header[").append(i).append("]: ").append(s);
                probeMsg.append(": ").append(request.getHeader(s)).append("\n");
            }

            probeMsg.append("\n");
            probeMsg.append("ServletRequest Interface:").append("\n");
            probeMsg.append("    getCharacterEncoding:  ").append(request.getCharacterEncoding()).append("\n");
            probeMsg.append("    getContentType:        ").append(request.getContentType()).append("\n");
            probeMsg.append("    getContentLength:      ").append(request.getContentLength()).append("\n");
            probeMsg.append("    getProtocol:           ").append(request.getProtocol()).append("\n");
            probeMsg.append("    getScheme:             ").append(request.getScheme()).append("\n");
            probeMsg.append("    getServerName:         ").append(request.getServerName()).append("\n");
            probeMsg.append("    getServerPort:         ").append(request.getServerPort()).append("\n");
            probeMsg.append("    getRemoteAddr:         ").append(request.getRemoteAddr()).append("\n");
            probeMsg.append("    getRemoteHost:         ").append(request.getRemoteHost()).append("\n");

            probeMsg.append(minorSeparator);
            probeMsg.append("\n");
            i = 0;
            e = request.getAttributeNames();
            probeMsg.append("    Attribute Names:").append("\n");
            while (e.hasMoreElements()) {
                i++;
                String s = (String) e.nextElement();
                probeMsg.append("        Attribute[").append(i).append("]: ").append(s);
                probeMsg.append(VALUE_FRAGMENT).append(request.getAttribute(s)).append("\n");
            }

            probeMsg.append(minorSeparator);
            probeMsg.append("\n");
            i = 0;
            e = request.getParameterNames();
            probeMsg.append("    Parameter Names:").append("\n");
            while (e.hasMoreElements()) {
                i++;
                String s = (String) e.nextElement();
                probeMsg.append("        Parameter[").append(i).append("]: ").append(s);
                probeMsg.append(VALUE_FRAGMENT).append(request.getParameter(s)).append("\n");
            }

        }
        probeMsg.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -").append("\n");
        probeMsg.append(" . . . . . . . . . Servlet Information API  . . . . . . . . . . . . . .").append("\n");
        probeMsg.append("\n");


        if (servlet == null) {
            probeMsg.append("Supplied reference to HttpServlet was null.\nNothing additional to report.").append("\n");

        } else {
            probeMsg.append("HttpServlet:").append("\n");
            probeMsg.append("    getServletName(): ").append(servlet.getServletName()).append("\n");
            probeMsg.append("    getServletInfo(): ").append(servlet.getServletInfo()).append("\n");
            probeMsg.append("\n");
            probeMsg.append("\n");

            ServletContext scntxt = servlet.getServletContext();
            ServletConfig scnfg = servlet.getServletConfig();


            probeMsg.append("Servlet Context:").append("\n");

            probeMsg.append("    getMajorVersion:       ").append(scntxt.getMajorVersion()).append("\n");
            probeMsg.append("    getMinorVersion:       ").append(scntxt.getMinorVersion()).append("\n");
            probeMsg.append("    getServerInfo:         ").append(scntxt.getServerInfo()).append("\n");
            probeMsg.append("    getServletContextName: ").append(scntxt.getServletContextName()).append("\n");
            probeMsg.append("\n");

            i = 0;
            e = scntxt.getAttributeNames();
            probeMsg.append("    Attribute Names:").append("\n");
            while (e.hasMoreElements()) {
                i++;
                String s = (String) e.nextElement();
                probeMsg.append("        Attribute[").append(i).append("]: ").append(s);
                probeMsg.append(" Type: ").append(scntxt.getAttribute(s)).append("\n");
            }

            probeMsg.append("    ServletContext.getRealPath(\".\"): ").append(scntxt.getRealPath(".")).append("\n");
            probeMsg.append("    ServletContext.getMajorVersion(): ").append(scntxt.getMajorVersion()).append("\n");
            probeMsg.append("    ServletContext.getMinorVersion(): ").append(scntxt.getMinorVersion()).append("\n");

            probeMsg.append(minorSeparator);
            probeMsg.append("Servlet Config:").append("\n");
            probeMsg.append("\n");

            i = 0;
            e = scnfg.getInitParameterNames();
            probeMsg.append("    InitParameters:").append("\n");
            while (e.hasMoreElements()) {
                String p = (String) e.nextElement();
                probeMsg.append("        InitParameter[").append(i).append("]: ").append(p);
                probeMsg.append(VALUE_FRAGMENT).append(scnfg.getInitParameter(p)).append("\n");
                i++;
            }
        }

        probeMsg.append("\n");
        probeMsg.append("######################## END PROBE ###############################").append("\n");
        probeMsg.append("\n");

        return probeMsg.toString();

    }
}