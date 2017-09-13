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

package opendap.dap4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 10/27/12
 * Time: 2:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util {

    private static Logger log;

    static {
        log = LoggerFactory.getLogger(Util.class);
    }


    public static boolean isReadableFile(File f) {
        String msg;


        if (!f.exists()) {
            msg = "Cannot find file: " + f;
            log.warn(msg);
            return false;
        }

        if (!f.canRead()) {
            msg = "Cannot read from file: " + f;
            log.warn(msg);
            return false;
        }

        if (f.isDirectory()) {
            msg = "File " + f + " is actually a directory.";
            log.warn(msg);
            return false;
        }

        return true;

    }

    public static boolean isReadableDir(File f) {
        String msg;


        if (!f.exists()) {
            msg = "Cannot find directory: " + f;
            log.warn(msg);
            return false;
        }

        if (!f.canRead()) {
            msg = "Cannot read from directory: " + f;
            log.warn(msg);
            return false;
        }

        if (!f.isDirectory()) {
            msg = "Directory " + f + " is not actually a directory.";
            log.warn(msg);
            return false;
        }

        return true;

    }


    /**
     * null proof string contains test
     * @param str the String 
     * @param sub the test/candiate Sub-string 
     * @return true only if str contains sub
     */
    public static boolean stringContains(String str, String sub) {
    	boolean flag = false;
    	
    	if (str == null) {
    		// no action
    	}
    	else if (str.trim().length() == 0) {
    		// no action 
    	}
    	else if (sub == null) {
    		// no action
    	}
    	else if (sub.trim().length() == 0) {
    		// no action
    	}
    	else if (str.trim().toLowerCase().contains(sub.trim().toLowerCase())) {
    	  flag = true;	
    	}
    	
    	return flag;
    }
    
    /**
     * return string of zero length for nulls and blanks, trimmed otherwise
     * @param str
     * @return
     */
    public static String nullProof(String str) {
    	if (str == null) return "";
    	else if (str.trim().length() == 0) return "";
    	else return str.trim();
    }


}
