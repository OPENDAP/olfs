package opendap.bes.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.concurrent.ExecutorService;

/**
 * Created by ndp on 4/15/16.
 */
public class CatalogCacheUpdater  implements ServletContextListener {

    Logger log;
    Thread catalogUpdateThread;
    BesCatalogCache bcc;

    public void contextInitialized(ServletContextEvent arg0) {

        log = LoggerFactory.getLogger(this.getClass());

        log.info("contextInitialized() - STARTING CATALOG CACHE UPDATER");

        bcc = new BesCatalogCache();

        catalogUpdateThread = new Thread(bcc);
        catalogUpdateThread.setName("CatalogCacheUpdateThread");
        catalogUpdateThread.setDaemon(true);
        catalogUpdateThread.start();

        log.info("contextInitialized() - CATALOG CACHE UPDATER RUNNING");

    }

    public void contextDestroyed(ServletContextEvent arg0) {
        log.info("contextDestroyed() - STOPPING CATALOG CACHE UPDATER");
        bcc.halt();
        catalogUpdateThread.interrupt();
        bcc.destroy();
        log.info("contextDestroyed() - CATALOG CACHE UPDATER SHOULD BE FINISHED");
    }
}



