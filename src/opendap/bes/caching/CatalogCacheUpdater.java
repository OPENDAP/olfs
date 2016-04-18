package opendap.bes.caching;

import opendap.coreServlet.ServletUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;

/**
 * Created by ndp on 4/15/16.
 */
public class CatalogCacheUpdater  implements ServletContextListener {

    Logger log;
    Thread catalogUpdateThread;
    BesCatalogCache bcc;
    private long _maxCacheEntries;
    private long _upDateInterval_milliseconds;

    public CatalogCacheUpdater() {
        _maxCacheEntries = 51;
        _upDateInterval_milliseconds = 13000;
    }



    public void contextInitialized(ServletContextEvent arg0) {

        log = LoggerFactory.getLogger(this.getClass());

        log.info("contextInitialized() - STARTING CATALOG CACHE UPDATER");

        ServletContext context = arg0.getServletContext();

        if(!loadConfig(context)){
            try {
                String maxEntriesString = context.getInitParameter("CatalogCache.maxCacheEntries");
                log.debug("contextInitialized(): Checking for CatalogCache.maxCacheEntries in web.xml file. value: {}",maxEntriesString);
                _maxCacheEntries = Long.parseLong(maxEntriesString);
            } catch (NumberFormatException ignore ) {}

            try {
                String updateIntervalSeconds = context.getInitParameter("CatalogCache.updateInterval");
                log.debug("contextInitialized(): Checking for CatalogCache.updateInterval in web.xml file. value:: {}",updateIntervalSeconds);
                _upDateInterval_milliseconds = Long.parseLong(updateIntervalSeconds);
                _upDateInterval_milliseconds = _upDateInterval_milliseconds * 1000; // seconds To milliseconds
                log.debug("contextInitialized(): Set _upDateInterval_milliseconds to {}",_upDateInterval_milliseconds);
            } catch (NumberFormatException ignore ) {}

        }

        bcc = new BesCatalogCache(_maxCacheEntries,_upDateInterval_milliseconds);
        catalogUpdateThread = new Thread(bcc);
        catalogUpdateThread.setName("CatalogCacheUpdateThread");
        catalogUpdateThread.setDaemon(true);
        catalogUpdateThread.start();

        log.info("contextInitialized() - CATALOG CACHE UPDATER RUNNING");

    }


    /**
     *
         <CatalogCache>
             <maxEntries>10000</maxEntries>
             <updateIntervalSeconds>10000</updateIntervalSeconds>
         </CatalogCache>


     * @param context
     * @return
     */
    private boolean loadConfig(ServletContext context){

        Document configDoc;
        String filename = ServletUtil.getConfigPath(context) + "olfs.xml";

        log.debug("Loading Configuration File: " + filename);

        try {
            configDoc = opendap.xml.Util.getDocument(filename);
            log.debug("Configuration loaded and parsed.");
        } catch (IOException | JDOMException e) {
            String msg = "Caught "+e.getClass().getName()+"  when attempting to access \""+ filename+ "\" :( Message: "+e.getMessage();
            log.error(msg);
            return false;
        }

        Element root = configDoc.getRootElement();
        if(root==null){
            String msg = "The configuration document \""+ filename+ "\" appears to be empty :(";
            log.error(msg);
            return false;
        }

        Element config = root.getChild("CatalogCache");
        if(config==null){
            String msg = "The configuration document \""+ filename+ "\" Does not contain a CatalogCache element. :(";
            log.error(msg);
            return false;
        }


        Element e = config.getChild("maxEntries");
        if(e!=null){
            String maxEntriesString = e.getTextTrim();
            log.debug("loadConfig(): Setting _maxCacheEntries to {}",maxEntriesString);
            _maxCacheEntries = Long.parseLong(maxEntriesString);
        }


        e = config.getChild("updateIntervalSeconds");
        if(e!=null){
            String updateIntervalSeconds = e.getTextTrim();
            log.debug("loadConfig(): updateIntervalSeconds: {}",updateIntervalSeconds);
            _upDateInterval_milliseconds = Long.parseLong(updateIntervalSeconds);
            _upDateInterval_milliseconds = _upDateInterval_milliseconds * 1000; // seconds To milliseconds
            log.debug("loadConfig(): Set _upDateInterval_milliseconds to {}",_upDateInterval_milliseconds);

        }

        return true;
    }




    public void contextDestroyed(ServletContextEvent arg0) {
        log.info("contextDestroyed() - STOPPING CATALOG CACHE UPDATER");
        bcc.halt();
        catalogUpdateThread.interrupt();
        bcc.destroy();
        log.info("contextDestroyed() - CATALOG CACHE UPDATER SHOULD BE FINISHED");
    }
}



