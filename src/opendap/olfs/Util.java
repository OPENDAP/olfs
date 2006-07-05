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

import opendap.dap.parser.ParseException;
import opendap.dap.DODSException;
import opendap.util.Debug;
import opendap.util.Log;
import opendap.coreServlet.ReqState;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import java.io.*;
import java.util.Enumeration;
import java.util.Date;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 17, 2005
 * Time: 11:42:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class Util {


    private static boolean track = false;

    // debug
    // private ArrayList prArr = null;

    private class RequestDebug {
        long reqno;
        String threadDesc;
        boolean done = false;

        RequestDebug(long reqno, String threadDesc) {
            this.reqno = reqno;
            this.threadDesc = threadDesc;
        }
    }


    /**
      * ************************************************************************
      * Sends a DODS error to the client.
      *
      * @param de       The DODS exception that caused the problem.
      * @param response The <code>HttpServletResponse</code> for the client.
      */
     public static void opendapExceptionHandler(DODSException de, HttpServletResponse response) {
        if (Debug.isSet("showException")) {
            de.print(System.out);
            de.printStackTrace();
            Log.printDODSException(de);
        }


         try {

             response.setHeader("Content-Description", "dods_error");

             // This should probably be set to "plain" but this works, the
             // C++ slients don't barf as they would if I sent "plain" AND
             // the C++ don't expect compressed data if I do this...
             response.setHeader("Content-Encoding", "");

             PrintStream ps = new PrintStream(response.getOutputStream());
             de.print(ps);
             if (Debug.isSet("showException")) 
                 de.printStackTrace(ps);
             ps.flush();


         } catch (IOException ioe) {
             System.out.println("Cannot respond to client! IO Error: " + ioe.getMessage());
             Log.println("Cannot respond to client! IO Error: " + ioe.getMessage());
         }


     }
     /***************************************************************************/

    /**
      * ************************************************************************
      * Sends a DODS error to the client.
      *
      * @param e       The Exception that caused the problem.
      * @param response The <code>HttpServletResponse</code> for the client.
      */
     public static void anyExceptionHandler(Exception e, HttpServletResponse response) {


        DODSException de = new DODSException(DODSException.UNKNOWN_ERROR,e.getMessage());

        de.setStackTrace(e.getStackTrace());

        opendapExceptionHandler(de,response);


     }
     /***************************************************************************/


}
