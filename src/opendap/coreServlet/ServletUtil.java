package opendap.coreServlet;


import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;

public class ServletUtil {

    static private org.slf4j.Logger log;
    static private boolean isLogInit = false;


    public static String getPath(HttpServlet servlet, String path) {
        ServletContext sc = servlet.getServletContext();
        String spath = sc.getRealPath(path);
        spath = spath.replace('\\', '/');
        return spath;
    }


    private static String contentPath = null;
    public static String getContentPath(HttpServlet servlet) {
      if (contentPath == null)
      {
        String tmpContentPath = "../../content" + getContextPath( servlet ) + "/";

        File cf = new File( getRootPath(servlet) + tmpContentPath );
        try{
          contentPath = cf.getCanonicalPath() +"/";
          contentPath = contentPath.replace('\\','/');
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return contentPath;
    }



    private static String contextPath = null;
    public static String getContextPath( HttpServlet servlet ) {
      if ( contextPath == null ) {
        ServletContext servletContext = servlet.getServletContext();
        String tmpContextPath = servletContext.getInitParameter( "ContextPath" );
        if ( tmpContextPath == null ) tmpContextPath = "opendap";
        contextPath = "/"+tmpContextPath;
      }
      return contextPath;

    }


    public static String getRootPath(HttpServlet servlet) {
      ServletContext sc = servlet.getServletContext();
      String rootPath = sc.getRealPath("/");
      rootPath = rootPath.replace('\\','/');
      return rootPath;
    }




}