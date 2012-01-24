package opendap.experiments;

import opendap.coreServlet.ServletUtil;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
* Created by IntelliJ IDEA.
* User: ndp
* Date: Oct 31, 2010
* Time: 7:21:07 AM
* To change this template use File | Settings | File Templates.
*/
public class WorkerThread implements Runnable, ServletContextListener {

    private Logger log;
    private Thread myThread;

    public WorkerThread(){
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        log.info("In WorkerThread constructor.");

        myThread = new Thread(this);
        myThread.setName("BackgroundWorker"+myThread.getName());



    }


    public void contextInitialized(ServletContextEvent arg0) {


        ServletContext sc = arg0.getServletContext();

        String contentPath = ServletUtil.getContentPath(sc);
        log.debug("contentPath: "+contentPath);

        String serviceContentPath = contentPath;
        if(!serviceContentPath.endsWith("/"))
            serviceContentPath += "/";
        log.debug("_serviceContentPath: "+serviceContentPath);


        myThread.start();
        log.info("contextInitialized(): "+myThread.getName()+" is started.");



    }

    public void contextDestroyed(ServletContextEvent arg0) {

        Thread thread = Thread.currentThread();

        try {
            myThread.interrupt();
            myThread.join();
            log.info("contextDestroyed(): "+myThread.getName()+" is stopped.");
        } catch (InterruptedException e) {
            log.debug(thread.getClass().getName()+" was interrupted.");
        }
        log.info("contextDestroyed(): Finished..");

    }



    @Override
    public void run() {
        log.debug("In run() method.");

        long sleepTime= 20;

        Thread thread = Thread.currentThread();
        try {
            while(true && !thread.isInterrupted()){
                log.info(thread.getName()+": Sleeping for: "+sleepTime+" seconds");

                napTime(sleepTime);
                log.info(thread.getName()+": Resetting to: "+sleepTime/1000.0+" seconds");

            }
        } catch (InterruptedException e) {
            log.warn(thread.getName()+" was interrupted.");
        } catch (Exception e) {
            log.error(thread.getName()+" Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
        }
    }


    public void napTime(long intervalInSeconds) throws Exception {
        Thread.sleep(intervalInSeconds * 1000);
        log.info(Thread.currentThread().getName()+": Sleep timer expired.");

    }


    


}
