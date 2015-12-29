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

package opendap.experiments;

import opendap.coreServlet.ServletUtil;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
* Created by IntelliJ IDEA.
* User: ndp
* Date: Oct 31, 2010
* Time: 7:21:07 AM
* To change this template use File | Settings | File Templates.
*/
public class WorkerThread implements Runnable, ServletContextListener {

    private Logger _log;
    private Thread _myThread;

    public WorkerThread(){
        _log = org.slf4j.LoggerFactory.getLogger(getClass());
        _log.info("In WorkerThread constructor.");

        _myThread = new Thread(this);
        _myThread.setName("BackgroundWorker" + _myThread.getName());



    }


    public void contextInitialized(ServletContextEvent arg0) {


        ServletContext sc = arg0.getServletContext();

        String contentPath = ServletUtil.getConfigPath(sc);
        _log.debug("contentPath: " + contentPath);

        String serviceContentPath = contentPath;
        if(!serviceContentPath.endsWith("/"))
            serviceContentPath += "/";
        _log.debug("_serviceContentPath: " + serviceContentPath);


        _myThread.start();
        _log.info("contextInitialized(): " + _myThread.getName() + " is started.");



    }

    public void contextDestroyed(ServletContextEvent arg0) {

        Thread thread = Thread.currentThread();

        try {
            _myThread.interrupt();
            _myThread.join();
            _log.info("contextDestroyed(): " + _myThread.getName() + " is stopped.");
        } catch (InterruptedException e) {
            _log.debug(thread.getClass().getName() + " was interrupted.");
        }
        _log.info("contextDestroyed(): Finished..");

    }



    @Override
    public void run() {
        _log.debug("In run() method.");

        long sleepTime= 20;

        Thread thread = Thread.currentThread();
        try {
            while(true && !thread.isInterrupted()){
                _log.info(thread.getName() + ": Sleeping for: " + sleepTime + " seconds");

                napTime(sleepTime);
                _log.info(thread.getName() + ": Resetting to: " + sleepTime / 1000.0 + " seconds");

            }
        } catch (InterruptedException e) {
            _log.warn(thread.getName() + " was interrupted.");
        } catch (Exception e) {
            _log.error(thread.getName() + " Caught " + e.getClass().getName() + "  Msg: " + e.getMessage());
        }
    }


    public void napTime(long intervalInSeconds) throws Exception {
        Thread.sleep(intervalInSeconds * 1000);
        _log.info(Thread.currentThread().getName() + ": Sleep timer expired.");

    }


    


}
