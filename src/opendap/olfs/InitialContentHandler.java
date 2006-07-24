/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.olfs;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;

/**
 * User: ndp
 * Date: Jul 21, 2006
 * Time: 12:58:46 PM
 */
public class InitialContentHandler {



    public static String getRootPath(HttpServlet servlet) {
      ServletContext sc = servlet.getServletContext();
      String rootPath = sc.getRealPath("/");
      rootPath = rootPath.replace('\\','/');
      return rootPath;
    }

    public static String getPath(HttpServlet servlet, String path) {
      ServletContext sc = servlet.getServletContext();
      String spath = sc.getRealPath(path);
      spath = spath.replace('\\','/');
      return spath;
    }

    /* public static String getRootPathUrl(HttpServlet servlet) {
      String rootPath = getRootPath( servlet);
      rootPath = StringUtil.replace(rootPath, ' ', "%20");
      return rootPath;
    } */

    private static String contextPath = null;
    public static String getContextPath( HttpServlet servlet ) {
      if ( contextPath == null ) {
        ServletContext servletContext = servlet.getServletContext();
        String tmpContextPath = servletContext.getInitParameter( "ContextPath" );  // cannot be overridden in the ServletParams file
        if ( tmpContextPath == null ) tmpContextPath = "opendap";
        contextPath = "/"+tmpContextPath;
      }
      return contextPath;
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

    public static String getInitialContentPath(HttpServlet servlet) {
      return getRootPath(servlet) + "initialContent/";
    }

    public static String formFilename(String dirPath, String filePath) {
      if ((dirPath == null) || (filePath == null))
        return null;

      if (filePath.startsWith("/"))
        filePath = filePath.substring(1);

      return dirPath.endsWith("/") ? dirPath + filePath : dirPath + "/" + filePath;
    }





    public static void garf(HttpServlet serv){


        System.out.println("getRootPath():           "+getRootPath(serv));
        System.out.println("getPath(\"tesDir\"):       "+getPath(serv,"testDir"));
        System.out.println("getContextPath():        "+getContextPath(serv));
        System.out.println("getContentPath():        "+getContentPath(serv));
        System.out.println("getInitialContentPath(): "+getInitialContentPath(serv));


        System.out.println("formFilename():           "+formFilename(getInitialContentPath(serv),"testDir"));


    }


}
