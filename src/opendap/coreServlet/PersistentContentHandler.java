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


import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;

import thredds.util.IO;
import thredds.servlet.ServletUtil;

/**
 * User: ndp
 * Date: Jul 25, 2006
 * Time: 11:54:34 AM
 */
public class PersistentContentHandler {

    public static void installInitialContent(HttpServlet servlet) {

        String semaphore = ".INIT";

        if (Debug.isSet("PersistentContentHandler")) {
            System.out.println("PersistentContentHandler:");
            System.out.println("    contentPath:        " + ServletUtil.getContentPath(servlet));
            System.out.println("    initialContentPath: " + ServletUtil.getInitialContentPath(servlet));
            System.out.println("    semaphore:          " + semaphore);
        }

        // -------------
        // first time, create content directory
        String initialContentPath = getInitialContentPath(servlet);
        File initialContentFile = new File(initialContentPath);

        if (initialContentFile.exists()) {
            try {
                if (copyDirIfSemaphoreNotPresent(initialContentPath, ServletUtil.getContentPath(servlet), semaphore)) {
                    if (Debug.isSet("PersistentContentHandler"))
                        System.out.println("Copied inital content directory " + initialContentPath + " to " + ServletUtil.getContentPath(servlet));
                }
            }
            catch (IOException ioe) {
                if (Debug.isSet("PersistentContentHandler")) {
                    System.out.println("Failed to copy initial content directory " + initialContentPath + " to " + ServletUtil.getContentPath(servlet));
                    ioe.printStackTrace(System.out);
                }
            }
        }
        //-------------

    }

    public static String getInitialContentPath(HttpServlet servlet) {
      return ServletUtil.getRootPath(servlet) + "initialContent/";
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
