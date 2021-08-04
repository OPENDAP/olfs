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


import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 21, 2010
 * Time: 9:21:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class WorkerThreadServlet extends HttpServlet {

    private Logger log;

    private AtomicInteger hitCounter;

    //private Document configDoc;

    private WorkerThread worker;
    private Thread workerThread;

    public WorkerThreadServlet(){
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        hitCounter = new AtomicInteger();
        hitCounter.set(0);
    }

    public void init() throws ServletException {
        super.init();



        worker = new WorkerThread();

        workerThread = new Thread(worker);

        workerThread.setName("ServletWorker"+workerThread.getName());
        workerThread.start();
        log.info("init(): Worker Thread started.");
        log.info("init(): complete.");


    }



    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        try {


            ServletOutputStream os = resp.getOutputStream();

            os.println(
                    "<html><body>" +
                    "<hr/>" +
                    "<h3>Request "+hitCounter.incrementAndGet()+" processed.</h3>" +
                    "<hr/>" +
                    "</body></html>"
                    );

        }
        catch (Throwable t) {

            log.error(t.getMessage());
        }

    }


    public void destroy() {


        while(workerThread.isAlive()){
            log.info("destroy(): "+workerThread.getName()+" isAlive(): "+workerThread.isAlive());

            log.info("destroy(): Interrupting "+workerThread.getName()+"...");
            workerThread.interrupt();

            log.info("destroy(): Waiting for "+workerThread.getName()+" to complete ...");

            try {
                workerThread.join();

                if(workerThread.isAlive()){
                    log.error("destroy(): "+workerThread.getName()+" is still Alive!!.");
                }
                else {
                    log.info("destroy(): "+workerThread.getName()+" has stopped.");
                }
            } catch (InterruptedException e) {
                log.info("destroy(): INTERRUPTED while waiting for WorkerThread "+workerThread.getName()+" to complete...");
                log.info("destroy(): "+workerThread.getName()+" isAlive(): "+ workerThread.isAlive());
            }

        }
        log.info("destroy(): Destroy completed.");

        super.destroy();
    }



}
