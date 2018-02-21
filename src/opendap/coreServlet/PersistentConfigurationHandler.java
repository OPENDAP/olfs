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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides methods for detecting the presence of a localized configuration directory and moving an initial
 * copy of the default configuration to that directory if it does not already exist.
 */
public class PersistentConfigurationHandler {

    private static Logger log;

    private static String defaultContentLocation="WEB-INF/conf/";

    private static final ReentrantLock _installDefaultConfigLock = new ReentrantLock();



    static{
        log = org.slf4j.LoggerFactory.getLogger(PersistentConfigurationHandler.class);
    }

        /**
         * If the following are true:
         *  - The local configuration directory exists.
         *  - The semaphore file does not exist in the configuration directory.
         *  Then the default configuration will be copied into
         *  the configuration directory.
         *  Otherwise, nothing will be done.
         * @param servlet
         */
    public static void installDefaultConfiguration(HttpServlet servlet, String semaphore) {


        _installDefaultConfigLock.lock();     // Only one thread allowed in at a time.
        try {
            Logger my_log = LoggerFactory.getLogger(PersistentConfigurationHandler.class);
            my_log.debug("PersistentContentHandler:");
            my_log.debug("    configPath:               {}",ServletUtil.getConfigPath(servlet));
            my_log.debug("    defaultConfigurationPath: {}",getDefaultConfigurationPath(servlet));
            my_log.debug("    semaphore:                {}",semaphore);
            my_log.debug("    ThreadName:               {}",Thread.currentThread().getName());
            
            // -------------
            // first time, create content directory
            String defaultConfigurationPath = getDefaultConfigurationPath(servlet);
            File defaultConfigurationFile = new File(defaultConfigurationPath);

            if (defaultConfigurationFile.exists()) {
                try {
                    if (copyDirIfSemaphoreNotPresent(defaultConfigurationPath, ServletUtil.getConfigPath(servlet), semaphore)) {
                        my_log.info("Copied default configuration from " + defaultConfigurationPath + " to directory " + ServletUtil.getConfigPath(servlet));
                    } else {
                        my_log.info("Located configuration directory semaphore ('{}') in directory '{}'", semaphore, ServletUtil.getConfigPath(servlet));
                    }
                } catch (IOException ioe) {
                    my_log.error("Failed to copy default content directory " + defaultConfigurationPath + " to " + ServletUtil.getConfigPath(servlet), ioe);
                }
            }
            //-------------
        } finally {
            _installDefaultConfigLock.unlock();
        }

    }


    private static  boolean copyDirIfSemaphoreNotPresent(
            String fromDir,
            String toDir,
            String semaphore)
            throws IOException {

            File contentFile = new File(toDir + semaphore);
            if (!contentFile.exists()) {
                copyDirTree(fromDir, toDir);
                if (!contentFile.exists()) {
                    String msg = "FAILED to locate semaphore file '" + contentFile.getAbsolutePath() + "' after copy completed.";
                    LoggerFactory.getLogger(PersistentConfigurationHandler.class).error("copyDirIfSemaphoreNotPresent() - {}", msg);
                    throw new IOException(msg);

                }
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

    private static String getDefaultConfigurationPath(HttpServlet servlet) {
      return ServletUtil.getRootPath(servlet) + defaultContentLocation;
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
        if (!toDir.exists()) {
            if(!toDir.mkdirs()){
                String msg = "FAILED to create target directory '"+toDir.getAbsolutePath()+
                              "' Unable to copy content from '"+fromDir.getAbsolutePath()+"' user.name: " +
                        System.getProperty("user.name");

                LoggerFactory.getLogger(PersistentConfigurationHandler.class).error("copyDirTree() - {}",msg);
                throw new IOException(msg);
            }
        }

        File[] files = fromDir.listFiles();
        if(files==null){
            LoggerFactory.getLogger(PersistentConfigurationHandler.class).error("copyDirTree() - Unable to locate directory {}. No content will be copied. THIS IS BAD.",fromDirName);
            return;
        }
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
                 LoggerFactory.getLogger(PersistentConfigurationHandler.class).error("Failed to close file: "+fileOutName+" Error Message: "+e.getMessage());
             }
         }

         if (in != null) {
             try{
        	    in.close();
             }
             catch(IOException e){
                 LoggerFactory.getLogger(PersistentConfigurationHandler.class).error("Failed to close file: "+fileInName+" Error Message: "+e.getMessage());
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
