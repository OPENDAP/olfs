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
package opendap.experiments;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.Date;
import java.io.IOException;


/**
 * User: ndp
 * Date: Mar 17, 2008
 * Time: 10:35:29 AM
 */
public class InnerClassServlet extends HttpServlet {




    private long cacheTime = 2000; // 2 seconds

    private IC  myIC;


    public void init() throws ServletException {

        System.out.println("Attempting to create inner class IC");
        myIC = new IC();
        System.out.println("Created inner class IC");
        myIC.update(true);
        myIC.start();

    }



    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        System.out.println("Triggering IC update.");
        myIC.update(true);
    }

    public void destroy(){
        myIC.interrupt();
        System.out.println("Interrupted IC thread.");

    }


    private class IC extends Thread {

        IC(){
            super();
            System.out.println("IC: constructor.");
        }

        public void run() {
            System.out.println("IC: Starting.");
            boolean done = false;
            while(!done) {
                try {
                    update(false);
                    done = interrupted();
                    Thread.sleep(cacheTime);
                } catch (InterruptedException e) {
                    System.out.println("IC: Interrupted " +
                            "Exception.");
                    done = true;
                }
            }
            System.out.println("IC: Exiting");
        }

        private void update(boolean force){
            System.out.println("Updating force="+force+"  current time: "+new Date());
        }
    }



}

