package opendap.coreServlet;


import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Enumeration;

public class ServletUtil {

    static private org.slf4j.Logger log;
    static private boolean isLogInit = false;

    public static final String DEFAULT_CONTEXT_PATH = "opendap";
    private static String contextPath = null;



    private static String contentPath = null;

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
      if (contentPath == null)
      {
        String tmpContentPath = "../../content" + getContextPath( servlet ) + "/";
        String filename =  Scrub.fileName(getRootPath(servlet) + tmpContentPath);

        File cf = new File( filename );
        try{
          contentPath = cf.getCanonicalPath() +"/";
          contentPath = contentPath.replace('\\','/');
        } catch (IOException e) {
            log.error("Failed to produce a content path! Error: "+e.getMessage());
         }
      }
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
     * @param servlet
     * @return
     */

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
     * @param servlet
     * @return
     */
    public static String getContextPath( HttpServlet servlet ) {
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
      return servlet.getServletContext().getContextPath();
    }

    public static String getSystemPath(HttpServlet servlet, String path) {
        ServletContext sc = servlet.getServletContext();
        String spath = sc.getRealPath(path);
        spath = spath.replace('\\', '/');
        return spath;
    }


    public static String getRootPath(HttpServlet servlet) {
      ServletContext sc = servlet.getServletContext();
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


}