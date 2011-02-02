/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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
import java.io.*;

import org.slf4j.Logger;

/**
 * Provides methods for detecting the presence of the peristent content directory and moving an initial
 * copy of the persistent content to that directory if it does not already exist.
 */
public class PersistentContentHandler {

    private static Logger log;
    static{
        log = org.slf4j.LoggerFactory.getLogger(PersistentContentHandler.class);
    }

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
        log.debug("    initialContentPath: " + getInitialContentPath(servlet));
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


    private static  boolean copyDirIfSemaphoreNotPresent(
            String fromDir,
            String toDir,
            String semaphore)
            throws IOException {

      File contentFile = new File(toDir+semaphore);
      if (!contentFile.exists()) {
        copyDirTree(fromDir, toDir);
        contentFile.createNewFile();
        return true;
      }
      return false;
    }


 /* #################################################################################################################
  *
  * FILE UTILITY METHODS
  *
  *
  */

    private static String getInitialContentPath(HttpServlet servlet) {
      return ServletUtil.getRootPath(servlet) + "initialContent/";
    }



    /**
     * Copy an entire directory tree.
     * @param fromDirName from this directory (do nothing if not exist)
     * @param toDirName to this directory (will create if not exist)
     * @throws java.io.IOException on io error
     */
    static public void copyDirTree(String fromDirName, String toDirName) throws IOException {
      File fromDir = new File(fromDirName);
      File toDir = new File(toDirName);
      if (!fromDir.exists())
        return;
      if (!toDir.exists())
        toDir.mkdirs();

      File[] files = fromDir.listFiles();
      for (int i=0; i<files.length; i++) {
        File f = files[i];
        if (f.isDirectory())
          copyDirTree(f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
        else
          copyFile( f.getAbsolutePath(), toDir.getAbsolutePath() + "/" + f.getName());
      }
    }
    /**
     * copy one file to another.
     * @param fileInName copy from this file, which must exist.
     * @param fileOutName copy to this file, which is overrwritten if already exists.
     * @throws java.io.IOException on io error
     */
   static public void copyFile(String fileInName, String fileOutName) throws IOException {
     InputStream in = null;
     OutputStream out = null;
     try {
       in = new BufferedInputStream( new FileInputStream( fileInName));
       out = new BufferedOutputStream( new FileOutputStream( fileOutName));
       copy( in, out);
     } finally {
         if (out != null){
             try{
        	    out.close();
             }
             catch(IOException e){
                 log.error("Failed to close file: "+fileOutName+" Error Message: "+e.getMessage());
             }
         }

         if (in != null) {
             try{
        	    in.close();
             }
             catch(IOException e){
                 log.error("Failed to close file: "+fileInName+" Error Message: "+e.getMessage());
             }
         }
      }
   }

    /**
     * copy all bytes from in to out.
     * @param in InputStream
     * @param out OutputStream
     * @throws java.io.IOException on io error
     */
    static private int default_file_buffersize = 9200;
    static public void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[default_file_buffersize];
        while (true) {
          int bytesRead = in.read(buffer);
          if (bytesRead == -1) break;
          out.write(buffer, 0, bytesRead);
        }
    }


}
