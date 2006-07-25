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

package opendap.coreServlet;

import opendap.util.Debug;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;

import thredds.util.IO;

/**
 * User: ndp
 * Date: Jul 21, 2006
 * Time: 12:58:46 PM
 */
public class InitialContentHandler_OLD {


    public static String getRootPath(HttpServlet servlet) {
        ServletContext sc = servlet.getServletContext();
        String rootPath = sc.getRealPath("/");
        rootPath = rootPath.replace('\\', '/');
        return rootPath;
    }

    public static String getPath(HttpServlet servlet, String path) {
        ServletContext sc = servlet.getServletContext();
        String spath = sc.getRealPath(path);
        spath = spath.replace('\\', '/');
        return spath;
    }


    private static String contextPath = null;

    public static String getContextPath(HttpServlet servlet) {
        if (contextPath == null) {
            ServletContext servletContext = servlet.getServletContext();
            String tmpContextPath = servletContext.getInitParameter("ContextPath");  // cannot be overridden in the ServletParams file
            if (tmpContextPath == null) tmpContextPath = "opendap";
            contextPath = "/" + tmpContextPath;
        }
        return contextPath;
    }

    private static String contentPath = null;

    public static String getContentPath(HttpServlet servlet) {
        if (contentPath == null) {
            String tmpContentPath = "../../content" + getContextPath(servlet) + "/";

            File cf = new File(getRootPath(servlet) + tmpContentPath);
            try {
                contentPath = cf.getCanonicalPath() + "/";
                contentPath = contentPath.replace('\\', '/');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return contentPath;
    }

    public static String getInitialContentPath(HttpServlet servlet) {
        return getRootPath(servlet) + "initialContent/";
    }


    public static void installInitialContent(HttpServlet servlet) {

        String semaphore = ".INIT";

        if (Debug.isSet("InitialContentHandler_OLD")) {
            System.out.println("InitialContentHandler_OLD:");
            System.out.println("    contentPath:        " + getContentPath(servlet));
            System.out.println("    initialContentPath: " + getInitialContentPath(servlet));
            System.out.println("    semaphore:          " + semaphore);
        }

        // -------------
        // first time, create content directory
        String initialContentPath = getInitialContentPath(servlet);
        File initialContentFile = new File(initialContentPath);

        if (initialContentFile.exists()) {
            try {
                if (copyDirIfSemaphoreNotPresent(initialContentPath, getContentPath(servlet), semaphore)) {
                    if (Debug.isSet("InitialContentHandler_OLD"))
                        System.out.println("Copied inital content directory " + initialContentPath + " to " + getContentPath(servlet));
                }
            }
            catch (IOException ioe) {
                if (Debug.isSet("InitialContentHandler_OLD")) {
                    System.out.println("Failed to copy initial content directory " + initialContentPath + " to " + getContentPath(servlet));
                    ioe.printStackTrace(System.out);
                }
            }
        }
        //-------------

    }




    static private boolean copyDirIfSemaphoreNotPresent(String fromDir, String toDir, String semaphore) throws IOException {
      File contentFile = new File(toDir+semaphore);
      if (!contentFile.exists()) {
        IO.copyDirTree(fromDir, toDir);
        contentFile.createNewFile();
        return true;
      }
      return false;
    }





}
