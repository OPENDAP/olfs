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

    static private org.slf4j.Logger log = LoggerFactory.getLogger(ServletUtil.class);

    //public static final String DEFAULT_CONTEXT_PATH = "opendap";

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
    public static String getContentPath(HttpServlet servlet) {
      return getContentPath(servlet.getServletContext());
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
     * content in this directory be modifed before the upgrade can work.) Thus this directory is also referred to
     * as the "peristent content path" or "peristent content directory" in other parts of the documenttion.
     *
     * @param sc  The ServletContext for this servlet that is running.
     * @return  A String containing the content path (aka the peristent content path) for the web application.
     */
    public static String getContentPath(ServletContext sc) {
        String contentPath="FAILED_To_Determine_Content_Path!";
        String tmpContentPath = "../../content" + sc.getContextPath() + "/";
        String filename =  Scrub.fileName(getRootPath(sc) + tmpContentPath);

        File cf = new File( filename );
        try{
          contentPath = cf.getCanonicalPath() +"/";
          contentPath = contentPath.replace('\\','/');
        } catch (IOException e) {
            log.error("Failed to produce a content path! Error: "+e.getMessage());
         }
        log.debug("content path: '"+contentPath+"'");
      return contentPath;
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
/*
      if ( contextPath == null ) {
        ServletContext servletContext = servlet.getServletContext();
        String tmpContextPath = servletContext.getInitParameter( "ContextPath" );
        if ( tmpContextPath == null )
            tmpContextPath = DEFAULT_CONTEXT_PATH;

        if(!tmpContextPath.startsWith("/"))
          contextPath = "/"+tmpContextPath;
      }
      return contextPath;
*/
      String contextPath = sc.getContextPath();
      log.debug("context path: '"+contextPath+"'");

      return contextPath;
    }





    public static String getSystemPath(HttpServlet servlet, String path) {
        ServletContext sc = servlet.getServletContext();
        return getSystemPath(sc,path);
    }

    public static String getSystemPath(ServletContext sc, String path) {
        String spath = sc.getRealPath(path);
        spath = spath.replace('\\', '/');
        return spath;
    }


    public static String getRootPath(HttpServlet servlet) {
      return getRootPath( servlet.getServletContext());
    }

    public static String getRootPath(ServletContext sc) {
      String rootPath = sc.getRealPath("/");
      rootPath = rootPath.replace('\\','/');
      return rootPath;
    }



    public static String toString(HttpServlet servlet){
        String s = "ServletUtil:\n";

        s+= "    getContentPath(): "+ getContentPath(servlet) + "\n";
        s+= "    getContextPath(): "+ getContextPath(servlet) + "\n";
        s+= "    getRootPath(): "+ getRootPath(servlet) + "\n";
        s+= "    getSystemPath(): "+ getSystemPath(servlet,"/") + "\n";


        s += probeServlet(servlet);

        return s;
    }


    public static String probeServlet(HttpServlet servlet){

        String pName;
        String pVal;

        String s = "HttpServlet:\n";

        s+= "    getServletInfo(): "+ servlet.getServletInfo() + "\n";
        s+= "    getServletName(): "+ servlet.getServletName() + "\n";

        Enumeration e  = servlet.getInitParameterNames();
        s += "    Servlet Parameters:\n";
        if(e.hasMoreElements()){
            while(e.hasMoreElements()){
                pName = (String) e.nextElement();
                pVal = servlet.getInitParameter(pName);
                s +=  "        name: "+pName+" value: "+pVal+"\n";
            }
        }
        else
            s +=  "        No Servlet Parameters Found.\n";
        


        ServletConfig scfg = servlet.getServletConfig();
        ServletContext scntxt = servlet.getServletContext();


        s += "    HttpServlet.getServletConfig(): "+scfg+"\n";
        s += probeServletConfig(scfg);
        s += "    HttpServlet.ServletContext(): "+scntxt+"\n";
        s += probeServletContext(scntxt);


        return s;

    }

    public static String probeServletConfig(ServletConfig scfg){
        String s ="";
        Enumeration e;
        String pName, pVal;

        s += "    ServletConfig:\n";
        s += "       getServletName():"+scfg.getServletName()+"\n";
        e  = scfg.getInitParameterNames();
        s += "       Servlet Parameters:\n";
        if(e.hasMoreElements()){
            while(e.hasMoreElements()){
                pName = (String) e.nextElement();
                pVal = scfg.getInitParameter(pName);
                s +=  "            name: "+pName+" value: "+pVal+"\n";
            }
        }
        else
            s +=  "            No Servlet Parameters Found.\n";
        ServletContext scntxt = scfg.getServletContext();
        s += "       ServletConfig.getServletContext(): "+scntxt+"\n";
        s += probeServletContext(scntxt);

        return s;

    }

    public static String probeServletContext(ServletContext sc){
        String s = "";
        String pVal, pName;
        Enumeration e;

        s += "    ServletContext:\n";

        s += "       getServletContextName():"+sc.getServletContextName()+"\n";
        s += "       getContextPath():"+sc.getContextPath()+"\n";
       
        e = sc.getAttributeNames();
        s += "       ServletContext Attributes:\n";
        if(e.hasMoreElements()){
            while(e.hasMoreElements()){
                pName = (String) e.nextElement();
                s +=  "            name: "+pName+"\n";
            }

        }
        else
            s +=  "        No Servlet Context Attributes Found.\n";


        e  = sc.getInitParameterNames();
        s += "       ServletContext Parameters:\n";
        if(e.hasMoreElements()){
            while(e.hasMoreElements()){
                pName = (String) e.nextElement();
                pVal = sc.getInitParameter(pName);
                s +=  "           name: "+pName+" value: "+pVal+"\n";
            }
        }
        else
            s +=  "           No ServletContext Parameters Found.\n";


        try {
            s+= "       getResource(\"/\"): "+sc.getResource("/")+"\n";
        }
        catch (MalformedURLException e1) {
            log.error("Could not perform ServletCOntext.getREsource \"/\". Error Message: "+e1.getMessage());
        }


        s += "       getMajorVersion(): "+sc.getMajorVersion()+"\n";
        s += "       getMinorVersion(): "+sc.getMinorVersion()+"\n";
        s += "       getServerInfo(): "+sc.getServerInfo()+"\n";

        return s;

    }


    /**
     * Writes information about the incomming request to a String.
     * @param req The current request
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
        sb.append("    getRequestedSessionId():  ").append(req.getRequestedSessionId()).append("\n");

        sb.append("    getServletPath():         ").append(req.getServletPath()).append("\n");

        sb.append("    getCharacterEncoding():   ").append(req.getCharacterEncoding()).append("\n");
        sb.append("    getContentType():         ").append(req.getContentType()).append("\n");
        sb.append("    getLocalAddr():           ").append(req.getLocalAddr()).append("\n");
        sb.append("    getLocalName():           ").append(req.getLocalName()).append("\n");

        sb.append("  HttpServletRequest Attributes: \n");
        
        Enumeration attrNames =  req.getAttributeNames();
        while(attrNames.hasMoreElements()){
            String attrName = (String) attrNames.nextElement();
            Object value = req.getAttribute(attrName);
            sb.append("    ").append(value.getClass().getName()).append(" ").append(attrName).append("='");
            sb.append(value.toString()).append("'\n");

        }

        sb.append(showSession(req.getSession(true)));

        sb.append("  Request Info:\n");
        sb.append("    baseURI:                   '").append(ReqInfo.getServiceUrl(req)).append("'\n");
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
        Enumeration attrNames =  session.getAttributeNames();
        while(attrNames.hasMoreElements()){
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


        StringBuilder probeMsg = new StringBuilder();
        probeMsg.append("####################### PROBE ##################################\n");

        probeMsg.append("\n");
        probeMsg.append("The HttpServletRequest object is actually an instance of:").append("\n");
        probeMsg.append("    ").append(request.getClass().getName()).append("\n");
        probeMsg.append("\n");
        probeMsg.append("HttpServletRequest Interface: \n");
        probeMsg.append("    getAuthType:           ").append( request.getAuthType()).append( "\n");
        probeMsg.append("    getContextPath:        ").append( request.getContextPath()).append( "\n");
        probeMsg.append("    getMethod:             ").append( request.getMethod()).append( "\n");
        probeMsg.append("    getPathInfo:           ").append( request.getPathInfo()).append( "\n");
        probeMsg.append("    getPathTranslated:     ").append( request.getPathTranslated()).append( "\n");
        probeMsg.append("    getRequestURL:         ").append( request.getRequestURL()).append( "\n");
        probeMsg.append("    getQueryString:        ").append( request.getQueryString()).append( "\n");
        probeMsg.append("    getRemoteUser:         ").append( request.getRemoteUser()).append( "\n");
        probeMsg.append("    getRequestedSessionId: ").append( request.getRequestedSessionId()).append( "\n");
        probeMsg.append("    getRequestURI:         ").append( request.getRequestURI()).append( "\n");
        probeMsg.append("    getServletPath:        ").append( request.getServletPath()).append( "\n");
        probeMsg.append("    isRequestedSessionIdFromCookie: ").append( request.isRequestedSessionIdFromCookie()).append( "\n");
        probeMsg.append("    isRequestedSessionIdValid:      ").append( request.isRequestedSessionIdValid()).append( "\n");
        probeMsg.append("    isRequestedSessionIdFromURL:    ").append( request.isRequestedSessionIdFromURL()).append( "\n");
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
        //probeMsg.append("    getRealPath:           "+request.getRealPath()).append("\n");


        probeMsg.append(".............................").append("\n");
        probeMsg.append("\n");
        i = 0;
        e = request.getAttributeNames();
        probeMsg.append("    Attribute Names:").append("\n");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg.append("        Attribute[").append(i).append("]: ").append(s);
            probeMsg.append(" Value: ").append(request.getAttribute(s)).append("\n");
        }

        probeMsg.append(".............................").append("\n");
        probeMsg.append("\n");
        i = 0;
        e = request.getParameterNames();
        probeMsg.append("    Parameter Names:").append("\n");
        while (e.hasMoreElements()) {
            i++;
            String s = (String) e.nextElement();
            probeMsg.append("        Parameter[").append(i).append("]: ").append(s);
            probeMsg.append(" Value: ").append(request.getParameter(s)).append("\n");
        }

        probeMsg.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -").append("\n");
        probeMsg.append(" . . . . . . . . . Servlet Information API  . . . . . . . . . . . . . .").append("\n");
        probeMsg.append("\n");



        if(servlet == null){
            probeMsg.append("Supplied reference to HttpServlet was null.\nNothing additional to report.").append("\n");

        }
        else {
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
    //        probeMsg.append("ServletContext.getMimeType():     ").append(scntxt.getMimeType()).append("\n");
            probeMsg.append("    ServletContext.getMinorVersion(): ").append(scntxt.getMinorVersion()).append("\n");
    //        probeMsg.append("ServletContext.getRealPath(): ").append(sc.getRealPath()).append("\n");




            probeMsg.append(".............................").append("\n");



            probeMsg.append("Servlet Config:").append("\n");
            probeMsg.append("\n");


            i = 0;
            e = scnfg.getInitParameterNames();
            probeMsg.append("    InitParameters:").append("\n");
            while (e.hasMoreElements()) {
                String p = (String) e.nextElement();
                probeMsg.append("        InitParameter[").append(i).append("]: ").append(p);
                probeMsg.append(" Value: ").append(scnfg.getInitParameter(p)).append("\n");
                i++;
            }

        }

        probeMsg.append("\n");
        probeMsg.append("######################## END PROBE ###############################").append("\n");
        probeMsg.append("\n");

        return probeMsg.toString();

    }
}