package opendap.bes.caching;

import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
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


    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(BesCatalogCache.class);
    }
    private static ReentrantLock lock;
    static {
        lock = new ReentrantLock();
    }

    private static ConcurrentHashMap<String,CatalogTransaction> catalogTransactionCache;
    static {
        catalogTransactionCache = new ConcurrentHashMap<>();
    }

    private static ConcurrentSkipListSet<CatalogTransaction> mostRecent;
    static {
        mostRecent = new ConcurrentSkipListSet<>();
    }


    private static AtomicLong _maxCacheEntries = new AtomicLong(50); // # of entries in cache
    private static AtomicLong _updateInterval = new AtomicLong(10); // Update interval in seconds
    private static double _cache_reduction_factor; // Amount to reduce cache when purging


    private static boolean ENABLED=false;

    private static AtomicBoolean halt=new AtomicBoolean(false);


    public BesCatalogCache(long maxEntries, long updateInterval) {
        if(ENABLED)
            return;

        _maxCacheEntries.set(maxEntries);
        _updateInterval.set(updateInterval);
        _cache_reduction_factor =  0.2;
        ENABLED = true;
        log.debug("BesCatalogCache() - CREATED  _maxCacheEntries: {}  _updateInterval: {}", _maxCacheEntries.get(),_updateInterval.get());
    }

    /**
     * This private class is used to wrap whatever object is being cached along with data used to
     * operate in the cache. Most significantly this class implements the Comparable interface such that
     * the "natural" ordering of instances will be based on the last time each instance was accessed by the server.
     * This is not an autonomous operation and is tightly coupled with code in "BesCatalogCache.getCatalog()" to
     * ensure that the ordering remains correct.
     */
    private static class CatalogTransaction implements Comparable  {
        private Document _request;
        private Object _response;
        long _lastUpdateTime;
        long _lastAccessedTime;
        private String _key;
        private long _serialNumber;
        private static AtomicLong counter = new AtomicLong(0);


        public CatalogTransaction(String key, Document request, Object response){
            _key = key;
            _request = request;
            _response = response;
            _lastAccessedTime = System.nanoTime();
            _lastUpdateTime = _lastAccessedTime;
            _serialNumber = counter.getAndIncrement();
        }

        public Object getResponse(){
            return _response;
        }

        public Document getRequest(){
            return _request;
        }

        /**
         * The evaluation is based on the last accessed time (firstly) and the serial number of the
         * CatalogTransaction (secondly). If the last accessed times of two objects are the same
         * (unlikely but possible) then the serial numbers are used to determine the hierarchy/ranking/relation
         * @param o
         * @return
         */
        @Override
        public int compareTo(Object o) {
            if (!(o instanceof CatalogTransaction))
                throw new ClassCastException("An instance of a CatalogTransaction object was expected.");
            CatalogTransaction that = (CatalogTransaction) o;
            if(this==that)
                return 0;

            if(this._lastAccessedTime == that._lastAccessedTime){
                log.warn("compareTo() - Required object serial numbers to differentiate " +
                        "instances. this: {} that: {}",this._serialNumber, that._serialNumber);
                return (int) (this._serialNumber - that._serialNumber);
            }


            // Why return like this? Because the return value is an integer and the computation produces a long
            // incorrect conversion (over/under flow) could change sign of result.
            return (this._lastAccessedTime - that._lastAccessedTime)>0?1:-1;


        }
        @Override
        public boolean equals(Object aThat) {
            if (this == aThat) return true;
            if (!(aThat instanceof CatalogTransaction)) return false;

            CatalogTransaction that = (CatalogTransaction)aThat;

            return ( (this._lastAccessedTime == that._lastAccessedTime ) &&
                     (this._request == that._request)  &&
                     (this._response == that._response)

                   );

        }

        @Override public int hashCode() {
            int result = 73;
            result += _lastAccessedTime + (_request==null?0: _request.hashCode()) + (_response==null?0: _response.hashCode());
            return result;
        }

    }







    private static void purgeLeastRecentlyAccessed(){


        lock.lock();
        try {

            // Cache not full? Then return...
            if (catalogTransactionCache.size() < _maxCacheEntries.get())
                return;

            int dropNum = (int) (_maxCacheEntries.get() * _cache_reduction_factor);
            log.debug("purgeLeastRecentlyAccessed() - BEGIN  catalogTransactionCache.size(): {}  mostRecent.size(): {}", catalogTransactionCache.size(),mostRecent.size());
            log.debug("purgeLeastRecentlyAccessed() - dropNum: {}",dropNum);
            log.debug("purgeLeastRecentlyAccessed() - Before purge catalogTransactionCache.size(): {}", catalogTransactionCache.size());
            log.debug("purgeLeastRecentlyAccessed() - Before purge mostRecent.size(): {}",mostRecent.size());

            Vector<CatalogTransaction> purgeList = new Vector<>(dropNum);

            Iterator<CatalogTransaction> oldestToNewest = mostRecent.iterator();
            for(int i=0; i<dropNum && oldestToNewest.hasNext(); i++ ){
                CatalogTransaction co = oldestToNewest.next();
                purgeList.add(co);
                log.debug("purgeLeastRecentlyAccessed() - Purging CatalogTransaction for key {}",co._key);
                catalogTransactionCache.remove(co._key);
            }
            mostRecent.removeAll(purgeList);
            log.debug("purgeLeastRecentlyAccessed() - After purge catalogTransactionCache.size(): {}", catalogTransactionCache.size());
            log.debug("purgeLeastRecentlyAccessed() - After purge mostRecent.size(): {}",mostRecent.size());
            log.debug("purgeLeastRecentlyAccessed() - END");

        }
        finally {
            lock.unlock();
        }

    }

    /**
     * Updates the mostRecent list by dropping the passed CatalogTransaction from the mostRecent list, updating
     * the CatalogTransaction's access time and then adding the updated CatalogTransaction back to the mostRecent list.
     * @param cct The CatalogTransaction whose access time needs updating.
     */
    private static  void updateMostRecentlyAccessed(CatalogTransaction cct){

        lock.lock();
        try {
            log.debug("updateMostRecentlyAccessed() - Updating mostRecent list.  mostRecent.size(): {}", mostRecent.size());
            if (mostRecent.contains(cct)) {
                log.debug("updateMostRecentlyAccessed() - mostRecent list contains CatalogTransaction. Will drop.");
                mostRecent.remove(cct);
            }
            cct._lastAccessedTime = System.nanoTime();

            boolean status = mostRecent.add(cct);
            if (status) {
                log.debug("updateMostRecentlyAccessed() - mostRecent list successfully added updated CatalogTransaction.");

            } else {
                log.debug("updateMostRecentlyAccessed() - mostRecent list FAIL to add updated CatalogTransaction as it" +
                        " appears to be already in the mostRecent collection.");

            }
            log.debug("updateMostRecentlyAccessed() - mostRecent list updated.  mostRecent.size(): {}", mostRecent.size());
        }
        finally {
            lock.unlock();
        }

    }

    public static void putCatalogTransaction(String key, Document request, Object response) {

        if(!ENABLED)
            return;


        lock.lock();
        try {
            log.debug("putCatalogTransaction() - BEGIN  catalogTransactionCache.size(): {}  " +
                    "mostRecent.size(): {}", catalogTransactionCache.size(),mostRecent.size());

            purgeLeastRecentlyAccessed();

            CatalogTransaction co = new CatalogTransaction(key, request, response);
            log.debug("putCatalogTransaction() - CatalogTransaction created: {}", co._lastAccessedTime);

            boolean success = mostRecent.add(co);
            if (!success) {
                log.warn("putCatalogTransaction() - CatalogTransaction cache mostRecent list " +
                        "ALREADY contained CatalogTransaction {} for key: \"{}\"", co, key);
            }

            CatalogTransaction previous = catalogTransactionCache.put(key, co);
            if (co != previous) {
                log.warn("putCatalogTransaction() - CatalogTransaction cache updated with new object for key: \"{}\"",key);
            } else {
                log.debug("putCatalogTransaction() - CatalogTransaction cache updated by adding " +
                        "new object to cache using key \"{}\"",key);
            }

            log.debug("putCatalogTransaction() - END  catalogTransactionCache.size(): {}  " +
                    "mostRecent.size(): {}", catalogTransactionCache.size(),mostRecent.size());

        } finally {
            lock.unlock();
        }
    }


    public static Object getCatalog(String key){
        if(!ENABLED)
            return null;

        lock.lock();
        try {
            log.debug("getCatalog() - BEGIN  catalogTransactionCache.size(): {}  mostRecent.size(): {}", catalogTransactionCache.size(),mostRecent.size());

            if(key==null)
                return null;
            CatalogTransaction co = catalogTransactionCache.get(key);

            if(co!=null) {
                log.debug("getCatalog() - Found CatalogTransaction for key \""+ key+"\"");
                updateMostRecentlyAccessed(co);
                return co.getResponse();
            }
            else {
                log.debug("getCatalog() - No CatalogTransaction cached for key \""+ key+"\"");
            }

            log.debug("getCatalog() - END  catalogTransactionCache.size(): {}  mostRecent.size(): {}", catalogTransactionCache.size(),mostRecent.size());

        }
        finally {
            lock.unlock();
        }


        return null;
    }


    private static CatalogTransaction getDummyCachedCatalogTransaction(String id){
        Document request;
        Document response;
        Element e;

        id="foo";
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
        String logPrefix = "updateCatalogTransaction() - ";
        log.info(logPrefix + "Updating \"{}\"",resourceId);

        CatalogTransaction cTransaction = catalogTransactionCache.get(resourceId);

        if(cTransaction!=null) {

            BesApi besApi = new BesApi();
            try {
                Document response = new Document();
                besApi.besTransaction(resourceId, cTransaction._request, response);
                cTransaction._response = response.clone();
            } catch (BESError be) {
                log.info(logPrefix + "The showCatalog returned a BESError for id: \"" + resourceId +
                        "\"  CACHING. (responseCacheKey=\"" + resourceId + "\")");
                cTransaction._response = be;
            }
            cTransaction._lastUpdateTime = System.nanoTime();
            log.info(logPrefix + "Finished updating \"{}\"",resourceId);
        }
        else {
            log.info(logPrefix + "Nothing to update! The CatalogTransaction " +
                    "for resource \"{}\" is not cached.",resourceId);

        }
    }



    public static void main(String args[]) {



        TreeSet<CatalogTransaction> set = new TreeSet<>();

        CatalogTransaction co, a1;



        co = getDummyCachedCatalogTransaction("foo");
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime+" set.size(): "+set.size());
        a1 = co;

        co = getDummyCachedCatalogTransaction("bar");
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime +" set.size(): "+set.size());

        co = getDummyCachedCatalogTransaction("moo");
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime +" set.size(): "+set.size());

        co = getDummyCachedCatalogTransaction("soo");
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime +" set.size(): "+set.size());

        co = getDummyCachedCatalogTransaction("boo");
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime +" set.size(): "+set.size());


        System.out.println("List: ");
        for(CatalogTransaction cco: set){
            System.out.println(cco._key+": "+cco._lastAccessedTime );
        }

        set.remove(a1);
        a1._lastAccessedTime = System.nanoTime();
        set.add(a1);

        System.out.println("List: ");
        for(CatalogTransaction cco: set){
            System.out.println(cco._key+": "+cco._lastAccessedTime );
        }



    }
    @Override
    public void run() {

        if(!ENABLED)
            return;

        Thread thread = Thread.currentThread();
        boolean interrupted = false;

        log.info("run(): ************* CATALOG UPDATE THREAD.("+thread.getName()+") HAS BEEN STARTED.");

        long updateCounter = 0;
        while (!interrupted && !halt.get()) {

            try {

                long startTime = new Date().getTime();
                try {
                    update();
                }
                catch (InterruptedException e) {
                    log.error("run(): Catalog Update FAILED!!! Caught " + e.getClass().getName() +
                            "  Message: " + e.getMessage());
                    interrupted = true;
                }
                catch (BadConfigurationException | PPTException | IOException | JDOMException e) {
                    log.error("run(): Catalog Update FAILED!!! Caught " + e.getClass().getName() +
                            "  Message: " + e.getMessage());
                    halt.set(true);
                }

                long endTime = new Date().getTime();
                long elapsedTime = (endTime - startTime);
                updateCounter++;
                log.info("run(): Completed catalog update " + updateCounter + " in " + elapsedTime / 1000.0 + " seconds.");

                long sleepTime = _updateInterval.get() - elapsedTime;
                if (!halt.get() && sleepTime > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("run(): ").append(thread.getName()).append(" sleeping for ").append(sleepTime/1000.0);
                    sb.append(" cache: ").append(mostRecent.size()).append("/").append(_maxCacheEntries.get());
                    log.info(sb.toString());
                    Thread.sleep(sleepTime);
                }

            } catch (InterruptedException e) {
                log.warn("run(): "+thread.getName()+" caught InterruptedException. Stopping...");
                interrupted=true;
                halt.set(true);

            }
        }

        log.info("run(): ************* CATALOG UPDATE THREAD.("+Thread.currentThread().getName()+") IS EXITING.");

    }

    private void update() throws JDOMException, BadConfigurationException, PPTException, IOException, InterruptedException {
        lock.lock();
        try {
            for (String resourceId : catalogTransactionCache.keySet()) {
                updateCatalogTransaction(resourceId);
                if(halt.get())
                    return;
            }
        }
        finally {
            lock.unlock();
        }

    }


    public void destroy(){

        lock.lock();
        try {

            halt.set(true);

            catalogTransactionCache.clear();
            catalogTransactionCache = null;

            mostRecent.clear();
            mostRecent = null;
        }
        finally {
            lock.unlock();
        }

    }

    public void halt(){
        halt.set(true);
    }

}
