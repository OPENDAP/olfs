/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
//
// Copyright (c) 2006 OPeNDAP, Inc.
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
import org.slf4j.Logger;

/**
 * Provides methods for detecting the presence of the peristent content directory and moving an initial
 * copy of the persistent content to that directory if it does not already exist.
 */
public class PersistentContentHandler {


    /**
     * Checks to see if the persistent content directory exists, if it doesn't it is created and populated
     * with initial content from the distribution.
     * @param servlet
     */
    public static void installInitialContent(HttpServlet servlet) {

        Logger log = org.slf4j.LoggerFactory.getLogger(PersistentContentHandler.class);


        String semaphore = ".INIT";

        log.debug("PersistentContentHandler:");
        log.debug("    contentPath:        " + ServletUtil.getContentPath(servlet));
        log.debug("    initialContentPath: " + ServletUtil.getInitialContentPath(servlet));
        log.debug("    semaphore:          " + semaphore);


        // -------------
        // first time, create content directory
        String initialContentPath = getInitialContentPath(servlet);
        File initialContentFile = new File(initialContentPath);

        if (initialContentFile.exists()) {
            try {
                if (copyDirIfSemaphoreNotPresent(initialContentPath, ServletUtil.getContentPath(servlet), semaphore)) {
                    log.info("Copied inital content directory " + initialContentPath + " to " + ServletUtil.getContentPath(servlet));
                }
            }
            catch (IOException ioe) {
                    log.error("Failed to copy initial content directory " + initialContentPath + " to " + ServletUtil.getContentPath(servlet),ioe);
            }
        }
        //-------------

    }

    private static String getInitialContentPath(HttpServlet servlet) {
      return ServletUtil.getRootPath(servlet) + "initialContent/";
    }


    private static  boolean copyDirIfSemaphoreNotPresent(
            String fromDir,
            String toDir,
            String semaphore)
            throws IOException {

      File contentFile = new File(toDir+semaphore);
      if (!contentFile.exists()) {
        IO.copyDirTree(fromDir, toDir);
        contentFile.createNewFile();
        return true;
      }
      return false;
    }





}
