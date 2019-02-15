package opendap.bes.caching;

import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.namespaces.BES;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In memory cache for BES show catalog responses. In practice this is not designed for data but for BES API stuff like
 * show catalog, show info, etc. It will cache in memory any object that you wish however and associate it with
 * what ever "key" string you associate with it..
 */
public class BesCatalogCache implements Runnable{


    private static final Logger LOG = LoggerFactory.getLogger(BesCatalogCache.class);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final ConcurrentHashMap<String,CatalogTransaction> CATALOG_TRANSACTION_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentSkipListSet<CatalogTransaction> MOST_RECENT = new ConcurrentSkipListSet<>();

    private static final AtomicLong MAX_CACHE_ENTRIES = new AtomicLong(50); // # of entries in cache
    private static final AtomicLong UPDATE_INTERVAL = new AtomicLong(10); // Update interval in seconds
    private static final double CACHE_REDUCTION_FACTOR = 0.2; // Amount to reduce cache when purging


    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);
    private static final AtomicBoolean HALT =new AtomicBoolean(false);


    public BesCatalogCache(long maxEntries, long updateInterval) {
        if(ENABLED.get())
            return;

        MAX_CACHE_ENTRIES.set(maxEntries);
        UPDATE_INTERVAL.set(updateInterval);
        ENABLED.set(true);
        LOG.debug("BesCatalogCache() - CREATED  MAX_CACHE_ENTRIES: {}  UPDATE_INTERVAL: {}", MAX_CACHE_ENTRIES.get(), UPDATE_INTERVAL.get());
    }

    /**
     * This private class is used to wrap whatever object is being cached along with data used to
     * operate in the cache. Most significantly this class implements the Comparable interface such that
     * the "natural" ordering of instances will be based on the last time each instance was accessed by the server.
     * This is not an autonomous operation and is tightly coupled with code in "BesCatalogCache.getCatalog()" to
     * ensure that the ordering remains correct.
     */
    private static class CatalogTransaction implements Comparable  {

        private static final AtomicLong counter = new AtomicLong(0);

        private Document request;
        private Object response;
        long lastUpdateTime;
        long lastAccessedTime;
        private String key;
        private long serialNumber;


        public CatalogTransaction(String key, Document request, Object response){
            this.key = key;
            this.request = (Document)request.clone();


            // Dump the timeout context from the request.
            List list = this.request.getRootElement().getChildren("setContext", BES.BES_NS);
            List<Element> dropList = new ArrayList<>();
            for(Object o : list){
                Element setContextElement = (Element) o;
                String contextName=setContextElement.getAttributeValue("name");
                if(contextName.equals("bes_timeout")){
                    dropList.add(setContextElement);
                }
            }
            for(Element dropMe: dropList){
                dropMe.detach();
            }



            this.response = response;
            lastAccessedTime = System.nanoTime();
            lastUpdateTime = lastAccessedTime;
            serialNumber = counter.getAndIncrement();
        }

        public Object getResponse(){
            return response;
        }

        public Document getRequest(){
            return (Document) request.clone();
        }

        /**
         * The evaluation is based on the last accessed time (firstly) and the serial number of the
         * CatalogTransaction (secondly). If the last accessed times of two objects are the same
         * (unlikely but possible) then the serial numbers are used to determine the hierarchy/ranking/relation
         * @param o object (CatalogTransaction) to be compared
         * @return
         */
        @Override
        public int compareTo(Object o) {
            if (!(o instanceof CatalogTransaction))
                throw new ClassCastException("An instance of a CatalogTransaction object was expected.");
            CatalogTransaction that = (CatalogTransaction) o;
            if(this==that)
                return 0;

            if(this.lastAccessedTime == that.lastAccessedTime){
                LOG.warn("compareTo() - Required object serial numbers to differentiate " +
                        "instances. this: {} that: {}",this.serialNumber, that.serialNumber);
                return (int) (this.serialNumber - that.serialNumber);
            }


            // Why return like this? Because the return value is an integer and the computation produces a long
            // incorrect conversion (over/under flow) could change sign of result.
            return (this.lastAccessedTime - that.lastAccessedTime)>0?1:-1;


        }
        @Override
        public boolean equals(Object aThat) {
            if (this == aThat) return true;
            if (!(aThat instanceof CatalogTransaction)) return false;

            CatalogTransaction that = (CatalogTransaction)aThat;

            return ( (this.lastAccessedTime == that.lastAccessedTime) &&
                     (this.request == that.request)  &&
                     (this.response == that.response)

                   );

        }

        @Override public int hashCode() {
            int result = 73;
            result += lastAccessedTime + (request ==null?0: request.hashCode()) + (response ==null?0: response.hashCode());
            return result;
        }

    }







    private static void purgeLeastRecentlyAccessed(){


        LOCK.lock();
        try {

            // Cache not full? Then return...
            if (CATALOG_TRANSACTION_CACHE.size() < MAX_CACHE_ENTRIES.get())
                return;

            int dropNum = (int) (MAX_CACHE_ENTRIES.get() * CACHE_REDUCTION_FACTOR);
            LOG.debug("purgeLeastRecentlyAccessed() - BEGIN  CATALOG_TRANSACTION_CACHE.size(): {}  MOST_RECENT.size(): {}", CATALOG_TRANSACTION_CACHE.size(), MOST_RECENT.size());
            LOG.debug("purgeLeastRecentlyAccessed() - dropNum: {}",dropNum);
            LOG.debug("purgeLeastRecentlyAccessed() - Before purge CATALOG_TRANSACTION_CACHE.size(): {}", CATALOG_TRANSACTION_CACHE.size());
            LOG.debug("purgeLeastRecentlyAccessed() - Before purge MOST_RECENT.size(): {}", MOST_RECENT.size());

            List<CatalogTransaction> purgeList = new ArrayList<>(dropNum);

            Iterator<CatalogTransaction> oldestToNewest = MOST_RECENT.iterator();
            for(int i=0; i<dropNum && oldestToNewest.hasNext(); i++ ){
                CatalogTransaction co = oldestToNewest.next();
                purgeList.add(co);
                LOG.debug("purgeLeastRecentlyAccessed() - Purging CatalogTransaction for key {}",co.key);
                CATALOG_TRANSACTION_CACHE.remove(co.key);
            }
            MOST_RECENT.removeAll(purgeList);
            LOG.debug("purgeLeastRecentlyAccessed() - After purge CATALOG_TRANSACTION_CACHE.size(): {}", CATALOG_TRANSACTION_CACHE.size());
            LOG.debug("purgeLeastRecentlyAccessed() - After purge MOST_RECENT.size(): {}", MOST_RECENT.size());
            LOG.debug("purgeLeastRecentlyAccessed() - END");

        }
        finally {
            LOCK.unlock();
        }

    }

    /**
     * Updates the MOST_RECENT list by dropping the passed CatalogTransaction from the MOST_RECENT list, updating
     * the CatalogTransaction's access time and then adding the updated CatalogTransaction back to the MOST_RECENT list.
     * @param cct The CatalogTransaction whose access time needs updating.
     */
    private static  void updateMostRecentlyAccessed(CatalogTransaction cct){

        LOCK.lock();
        try {
            LOG.debug("updateMostRecentlyAccessed() - Updating MOST_RECENT list.  MOST_RECENT.size(): {}", MOST_RECENT.size());
            if (MOST_RECENT.contains(cct)) {
                LOG.debug("updateMostRecentlyAccessed() - MOST_RECENT list contains CatalogTransaction. Will drop.");
                MOST_RECENT.remove(cct);
            }
            cct.lastAccessedTime = System.nanoTime();

            boolean status = MOST_RECENT.add(cct);
            if (status) {
                LOG.debug("updateMostRecentlyAccessed() - MOST_RECENT list successfully added updated CatalogTransaction.");

            } else {
                LOG.debug("updateMostRecentlyAccessed() - MOST_RECENT list FAIL to add updated CatalogTransaction as it" +
                        " appears to be already in the MOST_RECENT collection.");

            }
            LOG.debug("updateMostRecentlyAccessed() - MOST_RECENT list updated.  MOST_RECENT.size(): {}", MOST_RECENT.size());
        }
        finally {
            LOCK.unlock();
        }

    }

    public static void putCatalogTransaction(String key, Document request, Object response) {

        if(!ENABLED.get())
            return;



        LOCK.lock();
        try {
            LOG.debug("putCatalogTransaction() - BEGIN  CATALOG_TRANSACTION_CACHE.size(): {}  " +
                    "MOST_RECENT.size(): {}", CATALOG_TRANSACTION_CACHE.size(), MOST_RECENT.size());

            purgeLeastRecentlyAccessed();

            CatalogTransaction co = new CatalogTransaction(key, request, response);
            LOG.debug("putCatalogTransaction() - CatalogTransaction created: {}", co.lastAccessedTime);

            boolean success = MOST_RECENT.add(co);
            if (!success) {
                LOG.warn("putCatalogTransaction() - CatalogTransaction cache MOST_RECENT list " +
                        "ALREADY contained CatalogTransaction {} for key: \"{}\"", co, key);
            }

            CatalogTransaction previous = CATALOG_TRANSACTION_CACHE.put(key, co);
            if (co != previous) {
                LOG.warn("putCatalogTransaction() - CatalogTransaction cache updated with new object for key: \"{}\"",key);
            } else {
                LOG.debug("putCatalogTransaction() - CatalogTransaction cache updated by adding " +
                        "new object to cache using key \"{}\"",key);
            }

            LOG.debug("putCatalogTransaction() - END  CATALOG_TRANSACTION_CACHE.size(): {}  " +
                    "MOST_RECENT.size(): {}", CATALOG_TRANSACTION_CACHE.size(), MOST_RECENT.size());

        } finally {
            LOCK.unlock();
        }
    }

    private static int getCurrentCacheSize(){
        LOCK.lock();
        try {
            return MOST_RECENT.size();
        }
        finally {
            LOCK.unlock();
        }
    }

    public static Object getCatalog(String key){
        if(!ENABLED.get())
            return null;

        LOCK.lock();
        try {
            LOG.debug("BEGIN  CATALOG_TRANSACTION_CACHE.size(): {}  MOST_RECENT.size(): {}", CATALOG_TRANSACTION_CACHE.size(), MOST_RECENT.size());

            if(key==null)
                return null;
            CatalogTransaction co = CATALOG_TRANSACTION_CACHE.get(key);

            if(co!=null) {
                LOG.debug("Found CatalogTransaction for key \"{}\"",key);
                updateMostRecentlyAccessed(co);
                return co.getResponse();
            }
            else {
                LOG.debug("No CatalogTransaction cached for key \"{}\"",key);
            }

            LOG.debug("END  CATALOG_TRANSACTION_CACHE.size(): {}  MOST_RECENT.size(): {}", CATALOG_TRANSACTION_CACHE.size(), MOST_RECENT.size());

        }
        finally {
            LOCK.unlock();
        }


        return null;
    }


    private static CatalogTransaction getDummyCachedCatalogTransaction(String id){
        Document request;
        Document response;
        Element e;

        request = new Document();
        e=new Element("request");
        e.setAttribute("id",id);
        request.setRootElement(e);

        response = new Document();
        e= new Element("response");
        e.setAttribute("id",id);
        response.setRootElement(e);
        return new CatalogTransaction(id,request,response);

    }


    private static void updateCatalogTransaction(String resourceId)
            throws JDOMException, BadConfigurationException, PPTException, IOException, InterruptedException {
        LOG.info("Updating \"{}\"",resourceId);

        CatalogTransaction cTransaction = CATALOG_TRANSACTION_CACHE.get(resourceId);

        if(cTransaction!=null) {

            BesApi besApi = new BesApi();
            try {
                Document response = new Document();
                besApi.besTransaction(resourceId, cTransaction.getRequest(), response);
                cTransaction.response = response.clone();
            } catch (BESError be) {
                LOG.info("The showCatalog returned a BESError for id: \"{}\" CACHING Error. (responseCacheKey=\"{}\")",resourceId,resourceId);
                cTransaction.response = be;
            }
            cTransaction.lastUpdateTime = System.nanoTime();
            LOG.info("Finished updating \"{}\"",resourceId);
        }
        else {
            LOG.info("Nothing to update! The CatalogTransaction for resource \"{}\" is not cached.",resourceId);

        }
    }

    @Override
    public void run() {

        if(!ENABLED.get())
            return;

        Thread thread = Thread.currentThread();
        boolean interrupted = false;

        LOG.info("************* CATALOG UPDATE THREAD.({}) HAS BEEN STARTED.",thread.getName());

        long updateCounter = 0;
        while (!interrupted && !HALT.get()) {
            long startTime;
            long endTime;
            long elapsedTime;
            try {

                startTime = new Date().getTime();
                update();
                endTime = new Date().getTime();

                elapsedTime = (endTime - startTime);
                updateCounter++;

                LOG.info("Completed catalog update {} in {} seconds.", updateCounter, elapsedTime / 1000.0);

                long sleepTime = UPDATE_INTERVAL.get() - elapsedTime;
                if (!HALT.get() && sleepTime > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(thread.getName()).append(" sleeping for ").append(sleepTime/1000.0);
                    sb.append(" cache: ").append(getCurrentCacheSize()).append("/").append(MAX_CACHE_ENTRIES.get());
                    LOG.info("{}",sb);
                    Thread.sleep(sleepTime);
                }

            } catch (BadConfigurationException | PPTException | IOException | JDOMException e) {
                LOG.error("Catalog Update FAILED!!! Caught {} Message: {}", e.getClass().getName(), e.getMessage());
                HALT.set(true);
            }
            catch (InterruptedException e) {
                LOG.warn("run(): {} caught InterruptedException. Stopping...", thread.getName());
                interrupted=true;
                HALT.set(true);
                LOG.info("run(): ************* CATALOG UPDATE THREAD.({}) IS EXITING.", Thread.currentThread().getName());
                Thread.currentThread().interrupt();
            }
        }


    }

    private void update() throws JDOMException, BadConfigurationException, PPTException, IOException, InterruptedException {
        LOCK.lock();
        try {
            for (String resourceId : CATALOG_TRANSACTION_CACHE.keySet()) {
                updateCatalogTransaction(resourceId);
                if(HALT.get())
                    return;
            }
        }
        finally {
            LOCK.unlock();
        }
    }



    public void destroy(){

        LOCK.lock();
        try {
            HALT.set(true);
            CATALOG_TRANSACTION_CACHE.clear();
            MOST_RECENT.clear();
        }
        finally {
            LOCK.unlock();
        }

    }

    public void halt(){
        HALT.set(true);
    }




    public static void main(String[] args) {

        Logger log = LoggerFactory.getLogger(BesCatalogCache.class);

        TreeSet<CatalogTransaction> set = new TreeSet<>();

        String[] testKeys = {"foo", "bar", "moo", "soo", "bar", "baz"};

        CatalogTransaction co;
        CatalogTransaction first = null;
        String msgFormat = "key: %s co.getTime(): %d set.size(): %d";
        String msg;

        for(String key: testKeys){
            co = getDummyCachedCatalogTransaction(key);
            set.add(co);
            msg = String.format(msgFormat,co.key,co.lastAccessedTime,set.size());
            log.info(msg);
            if(first==null)
                first = co;
        }

        log.info("Original List: ");
        for(CatalogTransaction cco: set){
            log.info("{}: {}",cco.key,cco.lastAccessedTime);
        }

        if(first!=null) {
            set.remove(first);
            first.lastAccessedTime = System.nanoTime();
            set.add(first);
        }

        log.info("List after remove and replace: ");
        for(CatalogTransaction cco: set){
            log.info("{}: {}",cco.key,cco.lastAccessedTime);
        }

    }

}
