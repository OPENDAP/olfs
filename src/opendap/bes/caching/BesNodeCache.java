package opendap.bes.caching;

import opendap.bes.BES;
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
    private static final String MAX_ENTRIES_ATTRIBUTE_NAME = "maxEntries";
    private static final String REFRESH_INTERVAL_ATTRIBUTE_NAME = "refreshInterval";
    private static final long NODE_CACHE_MAX_ENTRIES_DEFAULT = 2000;
    private static final long NODE_CACHE_REFRESH_INTERVAL_DEFAULT = 600;


    private static final Logger LOG = LoggerFactory.getLogger(BesNodeCache.class);
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final ConcurrentHashMap<String,NodeTransaction> NODE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentSkipListSet<NodeTransaction> MOST_RECENTLY_ACCESSED = new ConcurrentSkipListSet<>();

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
     * The primary public method used to retrieve BES showNode command responses. Caching happens within this call.
     * @param key The name of the BES node to retrieve.
     * @return The BES showNode response for "key"
     */
    public static Element getNode(String key) throws JDOMException, BadConfigurationException, PPTException, IOException, BESError {
        if(!ENABLED.get())
            return null;

        LOCK.lock();
        try {
            LOG.debug("BEGIN  NODE_CACHE.size(): {}  MOST_RECENTLY_ACCESSED.size(): {}", NODE_CACHE.size(), MOST_RECENTLY_ACCESSED.size());

            if(key==null)
                throw new IOException("The BesApi.getNode() method was passed a key value of null. That's bad.");


            NodeTransaction nodeTransaction = NODE_CACHE.get(key);
            if(nodeTransaction!=null) {
                LOG.debug("Found NodeTransaction[{}]",key);

                if(dropStaleNodeTransaction(nodeTransaction)) {
                    // It got dropped so we need to replace it
                    nodeTransaction = getAndCacheNodeTransaction(key);
                }
            }
            else {
                LOG.debug("NodeTransaction[{}] is not in NODE_CACHE, retrieving.",key);
                nodeTransaction = getAndCacheNodeTransaction(key);
            }

            updateMostRecentlyAccessed(nodeTransaction);

            // Now we need to sort out the response - Document or Error?
            Object responseObject = nodeTransaction.getResponse();

            if(responseObject instanceof Document){
                Document cachedNodeDoc = (Document)responseObject;
                Element root = cachedNodeDoc.getRootElement();
                Element newRoot =  (Element) root.clone();
                newRoot.detach();
                return newRoot;
            }

            if (responseObject instanceof BESError) {
                LOG.info("Cache contains BESError object.  dataSource=\"" +
                        key + "\"");
                throw (BESError) responseObject;
            }
            // The responseObject should only ever be a Document or a BESError
            throw new IOException("Cached object is of unexpected type! " +
                    "This is a bad thing! Object: "+responseObject.getClass().getCanonicalName());

        }
        finally {
            LOG.debug("END  NODE_CACHE.size(): {}  MOST_RECENTLY_ACCESSED.size(): {}", NODE_CACHE.size(), MOST_RECENTLY_ACCESSED.size());
            LOCK.unlock();
        }

    }

    /**
     * Solicits a showNode response from the BES for the passed parameter key. Once the response is received the response
     * is used to make a new NodeTransaction which is then placed in the cache associated with the value of key.
     * If the BES returns an error that is handled the same way, the error object is used to make a new NodeTransaction
     * which is placed in the cache associated with the value of key.
     * @param key The name of the node to retrieve from the BES using the showNode command.
     * @return The NodeTransaction built from the BES response.
     * @throws BadConfigurationException When a BES cannot be located.
     * @throws PPTException When the PPT exchange between the BES process and the OLFS fails.
     * @throws JDOMException When the documents cannot be parsed.
     * @throws IOException When theings cannot be read or written.
     */
    private static NodeTransaction getAndCacheNodeTransaction(String key) throws BadConfigurationException, PPTException, JDOMException, IOException {

        LOG.info("Acquiring showNode response for key \"{}\"", key );
        LOCK.lock();
        try {

            BES bes = BesApi.getBES(key);
            Document showNodeRequestDoc = BesApi.getShowNodeRequestDocument(key);
            Document response = new Document();

            NodeTransaction result;
            try {
                bes.besTransaction(showNodeRequestDoc, response);
                // Get the root element.
                Element root = response.getRootElement();
                if (root == null)
                    throw new IOException("BES showNode response for " + key + " was empty! No root element");

                Element showNode = root.getChild("showNode", opendap.namespaces.BES.BES_NS);
                if (showNode == null)
                    throw new IOException("BES showNode response for " + key + " was malformed! No showNode element");

                showNode.setAttribute("prefix", bes.getPrefix());

                LOG.info("Caching copy of BES showNode response for key: \"{}\"",key);
                result = putNodeTransaction(key, showNodeRequestDoc, response.clone());

            } catch (BESError be) {
                LOG.info("The BES returned a BESError for key: \"{} \" CACHING BESError",key);
                result = putNodeTransaction(key, showNodeRequestDoc, be);
            }

            return result;
        }
        finally {
            LOCK.unlock();
        }
    }


    /**
     * Removes the passed NodeTransaction from both the cache (NODE_CACHE) and from the most recently accessed list
     * (MOST_RECENTLY_ACCESSED)
     * @param nodeTransaction The NodeTransaction to drop from the cache.
     * @return True if the NodeTransaction was dropped from the cache, false otherwise.
     */
    private static boolean dropStaleNodeTransaction(NodeTransaction nodeTransaction){
        LOCK.lock();
        try {
            boolean dropped = false;
            long timeInCache = System.nanoTime() - nodeTransaction.getLastUpdateTime();
            LOG.debug("nodeTransaction[{}] has been in cache {} s",nodeTransaction.getKey(),timeInCache/(nanoInSeconds*1.0));

            // Is it stale?
            if(timeInCache > UPDATE_INTERVAL.get()) {

                NodeTransaction nt = NODE_CACHE.remove(nodeTransaction.getKey());
                LOG.debug("Remove NodeTransaction[{}] from NODE_CACHE returned: {}", nodeTransaction.getKey(), (nt == null) ? "NULL" : nt);
                LOG.debug("NODE_CACHE.size(): {}", NODE_CACHE.size());

                if (MOST_RECENTLY_ACCESSED.remove(nodeTransaction)) {
                    LOG.debug("Successfully dropped NodeTransaction[{}] from MOST_RECENTLY_ACCESSED (size: {})", nodeTransaction.getKey(), MOST_RECENTLY_ACCESSED.size());
                } else {
                    LOG.error("FAILED to drop NodeTransaction[{}] from MOST_RECENTLY_ACCESSED (size: {})", nodeTransaction.getKey(), MOST_RECENTLY_ACCESSED.size());
                }

                dropped = true;
            }
            return dropped;
        }
        finally {
            LOCK.unlock();
        }
    }



    /**
     * Updates the MOST_RECENTLY_ACCESSED list by dropping the passed CatalogTransaction from the MOST_RECENTLY_ACCESSED list, updating
     * the CatalogTransaction's access time and then adding the updated CatalogTransaction back to the MOST_RECENTLY_ACCESSED list.
     * @param nodeTransaction The CatalogTransaction whose access time needs updating.
     */
    private static  void updateMostRecentlyAccessed(NodeTransaction nodeTransaction){

        boolean status;
        LOCK.lock();
        try {
            LOG.debug("Updating MOST_RECENTLY_ACCESSED list.  MOST_RECENTLY_ACCESSED.size(): {}", MOST_RECENTLY_ACCESSED.size());

            status = MOST_RECENTLY_ACCESSED.remove(nodeTransaction);
            if(status) {
                LOG.debug("Removed NodeTransaction[{},{}] from MOST_RECENTLY_ACCESSED", nodeTransaction.getKey(),nodeTransaction);
            }
            else {
                LOG.debug("The NodeTransaction[{},{}] was not found in MOST_RECENTLY_ACCESSED", nodeTransaction.getKey(),nodeTransaction);
            }
            LOG.debug("MOST_RECENTLY_ACCESSED list updated.  MOST_RECENTLY_ACCESSED.size(): {}", MOST_RECENTLY_ACCESSED.size());

            nodeTransaction.updateAccessedTime();

            status = MOST_RECENTLY_ACCESSED.add(nodeTransaction);
            LOG.debug("{} NodeTransaction[{}] to MOST_RECENTLY_ACCESSED",status?"Added":"FAILED to add",nodeTransaction.getKey());
            LOG.debug("MOST_RECENTLY_ACCESSED list updated.  MOST_RECENTLY_ACCESSED.size(): {}", MOST_RECENTLY_ACCESSED.size());
        }
        finally {
            LOCK.unlock();
        }

    }


    /**
     * Takes a node response (Document or BESError) and tucks it into the NodeCache
     * @param key The name of the node that was retrieved.
     * @param request The BES showNode request document used to elicit the response.
     * @param response The object (either a Document or a BESError) returned by the BES transaction.
     */
    private static NodeTransaction putNodeTransaction(String key, Document request, Object response) {

        LOCK.lock();
        try {
            LOG.debug("BEGIN  NODE_CACHE.size(): {} ", NODE_CACHE.size());

            // Make sure the cache has not grown too large.
            purgeLeastRecentlyAccessed();

            NodeTransaction nodeTransaction = new NodeTransaction(key, request, response);
            LOG.debug("Created nodeTransaction[{}] @ {}", key, nodeTransaction.getLastUpdateTime());

            NodeTransaction previous = NODE_CACHE.put(key, nodeTransaction);
            if (nodeTransaction == previous) {
                LOG.warn("NodeTransaction cache updated with new (replacement) object for key: \"{}\"",key);
            } else {
                LOG.debug(" NodeTransaction cache updated by adding new object to cache using key \"{}\"",key);
            }

            LOG.debug("END  NODE_CACHE.size(): {}", NODE_CACHE.size());

            return nodeTransaction;

        } finally {
            LOCK.unlock();
        }
    }



    /**
     * If the cache is smaller than MAX_CACHE_ENTRIES then this is a no-op.
     * If the cache has grown larger than MAX_CACHE_ENTRIES then it will be reduced by
     * rsize = (CACHE_REDUCTION_FACTOR * MAX_CACHE_ENTRIES) entries. The reduction is done by iterating over the
     * MOST_RECENTLY_ACCESSED list, from oldest (longest since accessed) to newest (least time since access) and
     * collecting "rsize" NodeTransactions and removing them from both the NODE_CACHE and the MOST_RECENTLY_ACCESSED
     * list.
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
            LOG.debug("BEGIN  NODE_CACHE.size(): {}  MOST_RECENTLY_ACCESSED.size(): {}", NODE_CACHE.size(), MOST_RECENTLY_ACCESSED.size());
            LOG.debug("dropNum: {}",dropNum);
            LOG.debug("Before purge NODE_CACHE.size(): {}", NODE_CACHE.size());
            LOG.debug("Before purge MOST_RECENTLY_ACCESSED.size(): {}", MOST_RECENTLY_ACCESSED.size());

            List<NodeTransaction> purgeList = new ArrayList<>(dropNum);

            Iterator<NodeTransaction> oldestToNewest = MOST_RECENTLY_ACCESSED.iterator();
            for(int i=0; i<dropNum && oldestToNewest.hasNext(); i++ ){
                NodeTransaction nodeTransaction = oldestToNewest.next();
                purgeList.add(nodeTransaction);
                LOG.debug("Purging CatalogTransaction for key {}",nodeTransaction.getKey());
                NODE_CACHE.remove(nodeTransaction.getKey());
            }
            MOST_RECENTLY_ACCESSED.removeAll(purgeList);
            LOG.debug("After purge NODE_CACHE.size(): {}", NODE_CACHE.size());
            LOG.debug("After purge MOST_RECENTLY_ACCESSED.size(): {}", MOST_RECENTLY_ACCESSED.size());
            LOG.debug("END");

        }
        finally {
            LOCK.unlock();
        }

    }

    /**
     * Drops all references from the cache.
     */
    public void destroy(){
        LOCK.lock();
        try {
            NODE_CACHE.clear();
            MOST_RECENTLY_ACCESSED.clear();
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

        set.remove(first);
        first.updateAccessedTime();
        set.add(first);


        log.info("List after remove and replace: ");
        for(NodeTransaction nodeT: set){
            log.info(" node[{}]: {}",nodeT.getKey(),nodeT.getLastAccessedTime());
        }

    }

}
