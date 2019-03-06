package opendap.bes.caching;

import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
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
public class BesNodeCache {

    public static final String NODE_CACHE_ELEMENT_NAME = "NodeCache";
    public static final String MAX_ENTRIES_ATTRIBUTE_NAME = "maxEntries";
    public static final String REFRESH_INTERVAL_ATTRIBUTE_NAME = "refreshInterval";
    public static final long NODE_CACHE_MAX_ENTRIES_DEFAULT = 2000;
    public static final long NODE_CACHE_REFRESH_INTERVAL_DEFAULT = 600;


    private static final Logger LOG = LoggerFactory.getLogger(BesNodeCache.class);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final ConcurrentHashMap<String,NodeTransaction> NODE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentSkipListSet<NodeTransaction>    MOST_RECENT = new ConcurrentSkipListSet<>();

    private static final AtomicLong MAX_CACHE_ENTRIES = new AtomicLong(50); // # of entries in cache
    private static final AtomicLong UPDATE_INTERVAL = new AtomicLong(10); // Update interval in seconds
    private static final double CACHE_REDUCTION_FACTOR = 0.2; // Amount to reduce cache when purging

    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    private static final long nanoInSeconds = 1000000000;


    /**
     * This is a singleton class and as such all methods are static and the constructor is private because an
     * instance should never be created.
     */
    private BesNodeCache(){}

    /**
     * Initialize the Node Cache using an XML Element.
     * @param config The "NodeCache" configuration element
     * @throws BadConfigurationException When the configuration is broken.
     */
    public static void init(Element config) throws BadConfigurationException {

        if (config == null || !config.getName().equals(NODE_CACHE_ELEMENT_NAME))
            throw new BadConfigurationException("BesNodeCache must be passed a non-null configuration " +
                    "element named " + NODE_CACHE_ELEMENT_NAME);

        long maxEntries = NODE_CACHE_MAX_ENTRIES_DEFAULT;
        String maxEntriesString = config.getAttributeValue(MAX_ENTRIES_ATTRIBUTE_NAME);
        try {
            maxEntries = Long.parseLong(maxEntriesString);
            if (maxEntries <= 0) {
                maxEntries = NODE_CACHE_MAX_ENTRIES_DEFAULT;
                String msg = "Failed to parse value of " +
                        NODE_CACHE_ELEMENT_NAME + "@" + MAX_ENTRIES_ATTRIBUTE_NAME + "! " +
                        "Value must be an integer > 0. Using default value: " + maxEntries;
                LOG.error(msg);
            }
        } catch (NumberFormatException nfe) {
            LOG.error("Failed to parse value of NodeCache@maxEntries! Value must be an integer. Using default value: {}", maxEntries);
        }

        long refreshInterval = NODE_CACHE_REFRESH_INTERVAL_DEFAULT;
        String refreshIntervalString = config.getAttributeValue(REFRESH_INTERVAL_ATTRIBUTE_NAME);
        try {
            refreshInterval = Long.parseLong(refreshIntervalString);
            if (refreshInterval <= 0) {
                refreshInterval = NODE_CACHE_REFRESH_INTERVAL_DEFAULT;
                String msg = "Failed to parse value of " +
                        NODE_CACHE_ELEMENT_NAME + "@" + REFRESH_INTERVAL_ATTRIBUTE_NAME + "! " +
                        "Value must be an integer > 0. Using default value: " + refreshInterval;
                LOG.error(msg);
            }
        } catch (NumberFormatException nfe) {
            LOG.error("Failed to parse value of NodeCache@refreshInterval! Value must be an integer. Using default value: {}", refreshInterval);
        }
        init(maxEntries, refreshInterval);
    }


    /**
     * The _actual_ init method that sets up the cache
     * @param maxEntries The maximum number of entries in the cache
     * @param updateIntervalSeconds The time any object may reside in the cache before it is removed.
     */
    public static void init(long maxEntries, long updateIntervalSeconds) {
        LOCK.lock();
        try {
            if (ENABLED.get()) {
                LOG.error("BesNodeCache has already been initialized!  MAX_CACHE_ENTRIES: {}  UPDATE_INTERVAL: {} s", MAX_CACHE_ENTRIES.get(), UPDATE_INTERVAL.get()/(nanoInSeconds*1.0));
                return;
            }

            MAX_CACHE_ENTRIES.set(maxEntries);
            UPDATE_INTERVAL.set(updateIntervalSeconds * nanoInSeconds);
            ENABLED.set(true);
            LOG.debug("INITIALIZED  MAX_CACHE_ENTRIES: {}  UPDATE_INTERVAL: {} s", MAX_CACHE_ENTRIES.get(), UPDATE_INTERVAL.get()/(nanoInSeconds*1.0));
        }
        finally {
            LOCK.unlock();
        }
    }


    /**
     *
     */
    private static void purgeLeastRecentlyAccessed(){
        LOCK.lock();
        try {

            // Cache not full? Then return...
            if (NODE_CACHE.size() < MAX_CACHE_ENTRIES.get())
                return;

            int dropNum = (int) (MAX_CACHE_ENTRIES.get() * CACHE_REDUCTION_FACTOR);
            if(dropNum==0) {
                dropNum = 1;
            }
            LOG.debug("BEGIN  NODE_CACHE.size(): {}  MOST_RECENT.size(): {}", NODE_CACHE.size(), MOST_RECENT.size());
            LOG.debug("dropNum: {}",dropNum);
            LOG.debug("Before purge NODE_CACHE.size(): {}", NODE_CACHE.size());
            LOG.debug("Before purge MOST_RECENT.size(): {}", MOST_RECENT.size());

            List<NodeTransaction> purgeList = new ArrayList<>(dropNum);

            Iterator<NodeTransaction> oldestToNewest = MOST_RECENT.iterator();
            for(int i=0; i<dropNum && oldestToNewest.hasNext(); i++ ){
                NodeTransaction nodeTransaction = oldestToNewest.next();
                purgeList.add(nodeTransaction);
                LOG.debug("Purging CatalogTransaction for key {}",nodeTransaction.getKey());
                NODE_CACHE.remove(nodeTransaction.getKey());
            }
            MOST_RECENT.removeAll(purgeList);
            LOG.debug("After purge NODE_CACHE.size(): {}", NODE_CACHE.size());
            LOG.debug("After purge MOST_RECENT.size(): {}", MOST_RECENT.size());
            LOG.debug("END");

        }
        finally {
            LOCK.unlock();
        }

    }

    /**
     * Updates the MOST_RECENT list by dropping the passed CatalogTransaction from the MOST_RECENT list, updating
     * the CatalogTransaction's access time and then adding the updated CatalogTransaction back to the MOST_RECENT list.
     * @param nodeTransaction The CatalogTransaction whose access time needs updating.
     */
    private static  void updateMostRecentlyAccessed(NodeTransaction nodeTransaction){

        boolean status;
        LOCK.lock();
        try {
            LOG.debug("Updating MOST_RECENT list.  MOST_RECENT.size(): {}", MOST_RECENT.size());
            if (MOST_RECENT.contains(nodeTransaction)) {
                LOG.debug("MOST_RECENT list contains NodeTransaction. Removing...");
                status = MOST_RECENT.remove(nodeTransaction);
                LOG.debug("{} NodeTransaction[{}] from MOST_RECENT",status?"Removed":"Failed to remove",nodeTransaction.getKey());
                LOG.debug("MOST_RECENT list updated.  MOST_RECENT.size(): {}", MOST_RECENT.size());
            }
            nodeTransaction.logAccess();

            status = MOST_RECENT.add(nodeTransaction);
            LOG.debug("{} NodeTransaction[{}] to MOST_RECENT",status?"Added":"FAILED to add",nodeTransaction.getKey());
            LOG.debug("MOST_RECENT list updated.  MOST_RECENT.size(): {}", MOST_RECENT.size());
        }
        finally {
            LOCK.unlock();
        }

    }

    /**
     *
     * @param key
     * @param request
     * @param response
     */
    public static void putNodeTransaction(String key, Document request, Object response) {

        if(!ENABLED.get())
            return;

        LOCK.lock();
        try {
            LOG.debug("BEGIN  NODE_CACHE.size(): {}  MOST_RECENT.size(): {}",
                    NODE_CACHE.size(), MOST_RECENT.size());

            purgeLeastRecentlyAccessed();

            NodeTransaction nodeTransaction = new NodeTransaction(key, request, response);
            LOG.debug("Created nodeTransaction[{}] @ {}", key, nodeTransaction.getLastUpdateTime());

            boolean success = MOST_RECENT.add(nodeTransaction);
            if (!success) {
                LOG.warn("NodeTransaction cache MOST_RECENT list " +
                        "ALREADY contained NodeTransaction {} for key: \"{}\"", nodeTransaction, key);
            }

            NodeTransaction previous = NODE_CACHE.put(key, nodeTransaction);
            if (nodeTransaction == previous) {
                LOG.warn("NodeTransaction cache updated with new (replacement) object for key: \"{}\"",key);
            } else {
                LOG.debug(" NodeTransaction cache updated by adding new object to cache using key \"{}\"",key);
            }

            LOG.debug("END  NODE_CACHE.size(): {} MOST_RECENT.size(): {}",
                    NODE_CACHE.size(), MOST_RECENT.size());

        } finally {
            LOCK.unlock();
        }
    }


    /**
     *
     * @param key
     * @return
     */
    public static Object getNode(String key){
        if(!ENABLED.get())
            return null;

        LOCK.lock();
        try {
            LOG.debug("BEGIN  NODE_CACHE.size(): {}  MOST_RECENT.size(): {}", NODE_CACHE.size(), MOST_RECENT.size());

            if(key==null)
                return null;
            NodeTransaction nodeTransaction = NODE_CACHE.get(key);

            if(nodeTransaction!=null) {
                LOG.debug("Found NodeTransaction[{}]",key);

                // Here is where to figure out if it's stale.
                long timeInCache = System.nanoTime() - nodeTransaction.getLastUpdateTime();
                LOG.debug("nodeTransaction[{}] has been in cache {} s",nodeTransaction.getKey(),timeInCache/(nanoInSeconds*1.0));
                if(timeInCache > UPDATE_INTERVAL.get()) {
                    // Yup, it's stale, so we dump it and then the caller can choose to replace it.
                    NodeTransaction nt = NODE_CACHE.remove(key);
                    LOG.debug("Remove NodeTransaction[{}] from NODE_CACHE returned: {}",nodeTransaction.getKey(), (nt==null)?"NULL":nt.getKey());
                    LOG.debug("NODE_CACHE.size(): {}",NODE_CACHE.size());

                    if(MOST_RECENT.remove(nodeTransaction)){
                        LOG.debug("Successfully dropped NodeTransaction[{}] from MOST_RECENT (size: {})",nodeTransaction.getKey(), MOST_RECENT.size());
                    }
                    else {
                        LOG.error("FAILED to drop NodeTransaction[{}] from MOST_RECENT (size: {})",nodeTransaction.getKey(), MOST_RECENT.size());
                    }
                    return null;
                }
                updateMostRecentlyAccessed(nodeTransaction);
                return nodeTransaction.getResponse();
            }
            else {
                LOG.debug("NodeTransaction[{}] is not in NODE_CACHE",key);
            }

            LOG.debug("END  NODE_CACHE.size(): {}  MOST_RECENT.size(): {}", NODE_CACHE.size(), MOST_RECENT.size());

        }
        finally {
            LOCK.unlock();
        }


        return null;
    }


    public void destroy(){
        LOCK.lock();
        try {
            NODE_CACHE.clear();
            MOST_RECENT.clear();
        }
        finally {
            LOCK.unlock();
        }
    }


    private static NodeTransaction getDummyCachedNodeTransaction(String id){
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
        return new NodeTransaction(id,request,response);
    }


    public static void main(String[] args) {

        Logger log = LoggerFactory.getLogger(BesNodeCache.class);

        TreeSet<NodeTransaction> set = new TreeSet<>();

        String[] testKeys = {"foo", "bar", "moo", "soo", "bar", "baz"};

        NodeTransaction nodeTransaction;
        NodeTransaction first = null;
        String msgFormat = "nodeTransaction - key: %s getLastAccessedTime(): %d set.size(): %d";
        String msg;

        for(String key: testKeys){
            nodeTransaction = getDummyCachedNodeTransaction(key);
            set.add(nodeTransaction);
            msg = String.format(msgFormat,nodeTransaction.getKey(),nodeTransaction.getLastAccessedTime(),set.size());
            log.info(msg);
            if(first==null)
                first = nodeTransaction;
        }

        log.info("Original List: ");
        for(NodeTransaction nodeT: set){
            log.info(" node[{}]: {}",nodeT.getKey(),nodeT.getLastAccessedTime());
        }

        if(first!=null) {
            set.remove(first);
            first.logAccess();
            set.add(first);
        }

        log.info("List after remove and replace: ");
        for(NodeTransaction nodeT: set){
            log.info(" node[{}]: {}",nodeT.getKey(),nodeT.getLastAccessedTime());
        }

    }

}
