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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An in-memory cache for BES showNode responses. This class is a singleton.
 */
public class BesNodeCache {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public  static final String NODE_CACHE_ELEMENT_NAME = "NodeCache";
    private static final String MAX_ENTRIES_ATTRIBUTE_NAME = "maxEntries";
    private static final String REFRESH_INTERVAL_ATTRIBUTE_NAME = "refreshInterval";
    private static final int NODE_CACHE_MAX_ENTRIES_DEFAULT = 2000;
    private static final long NODE_CACHE_REFRESH_INTERVAL_DEFAULT = 600;
    private static final long nanoInSeconds = 1000000000;

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final Logger SLOG = LoggerFactory.getLogger(NODE_CACHE_ELEMENT_NAME);

    // Update interval in seconds
    private static final AtomicLong UPDATE_INTERVAL = new AtomicLong(NODE_CACHE_REFRESH_INTERVAL_DEFAULT);

    private static LRUCache<String,NodeTransaction> lruCache=null;

    /**
     * This is a singleton class and as such all methods are static and the constructor is private because an
     * instance should never be created.
     */
    private BesNodeCache(){}

    /**
     * Initialize the BesNodeCache using an XML Element.
     * @param config The "NodeCache" configuration element
     * @throws BadConfigurationException When the configuration is broken.
     */
    public static void init(Element config) throws BadConfigurationException {

        if (config == null || !config.getName().equals(NODE_CACHE_ELEMENT_NAME))
            throw new BadConfigurationException("BesNodeCache must be passed a non-null configuration " +
                    "element named " + NODE_CACHE_ELEMENT_NAME);

        int maxEntries = NODE_CACHE_MAX_ENTRIES_DEFAULT;
        String maxEntriesString = config.getAttributeValue(MAX_ENTRIES_ATTRIBUTE_NAME);
        if(maxEntriesString!=null) {
            try {
                maxEntries = Integer.parseInt(maxEntriesString);
                if (maxEntries <= 0) {
                    maxEntries = NODE_CACHE_MAX_ENTRIES_DEFAULT;
                    String msg = "Failed to parse value of " +
                            NODE_CACHE_ELEMENT_NAME + "@" + MAX_ENTRIES_ATTRIBUTE_NAME + "! " +
                            "Value must be an integer > 0. Using default value: " + maxEntries;
                    SLOG.error(msg);
                }
            } catch (NumberFormatException nfe) {
                SLOG.error("Failed to parse value of NodeCache@maxEntries! Value must" +
                        " be an integer. Using default value: {}", maxEntries);
            }
        }

        long refreshInterval = NODE_CACHE_REFRESH_INTERVAL_DEFAULT;
        String refreshIntervalString = config.getAttributeValue(REFRESH_INTERVAL_ATTRIBUTE_NAME);
        if(refreshIntervalString != null) {
            try {
                refreshInterval = Long.parseLong(refreshIntervalString);
                if (refreshInterval <= 0) {
                    refreshInterval = NODE_CACHE_REFRESH_INTERVAL_DEFAULT;
                    String msg = "Failed to parse value of " +
                            NODE_CACHE_ELEMENT_NAME + "@" +
                            REFRESH_INTERVAL_ATTRIBUTE_NAME + "! " +
                            "Value must be an integer > 0. Using default value: " +
                            refreshInterval;
                    SLOG.error(msg);
                }
            } catch (NumberFormatException nfe) {
                SLOG.error("Failed to parse value of NodeCache@refreshInterval! Value" +
                        " must be an integer. Using default value: {}", refreshInterval);
            }
        }
        init(maxEntries, refreshInterval);
    }


    /**
     * The _actual_ init method that sets up the cache. This must be called
     * prior to using the cache.
     * @param maxEntries The maximum number of entries in the cache
     * @param updateIntervalSeconds The time any object may reside in the cache before it is removed.
     */
    public static void init(int maxEntries, long updateIntervalSeconds) {
        LOCK.lock();
        try {
            if (INITIALIZED.get()) {
                SLOG.error("BesNodeCache has already been initialized!  " +
                                "MAX_CACHE_ENTRIES: {}  UPDATE_INTERVAL: {} s",
                        lruCache.maxEntries(),
                        UPDATE_INTERVAL.get()/(nanoInSeconds*1.0));
                return;
            }

            int initCap = 1 + (int)(maxEntries * .20);
            lruCache = new LRUCache<>(maxEntries,initCap);

            UPDATE_INTERVAL.set(updateIntervalSeconds * nanoInSeconds);
            INITIALIZED.set(true);
            SLOG.debug("INITIALIZED  MAX_CACHE_ENTRIES: {}  UPDATE_INTERVAL: {} s",
                    maxEntries,
                    UPDATE_INTERVAL.get()/(nanoInSeconds*1.0));
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
    public static Element getNode(String key)
            throws JDOMException,
            BadConfigurationException,
            PPTException,
            IOException,
            BESError {

        if(!INITIALIZED.get()) {
            init(NODE_CACHE_MAX_ENTRIES_DEFAULT,NODE_CACHE_REFRESH_INTERVAL_DEFAULT);
        }

        if(key==null)
            throw new IOException("The BesApi.getNode() method was passed a key value of null. That's bad.");

        NodeTransaction nodeTransaction;
        LOCK.lock();
        try {
            SLOG.debug("BEGIN  LRUCache.size(): {}", lruCache.size());

            nodeTransaction = lruCache.get(key);

            if(staleOrMissing(nodeTransaction)){
                nodeTransaction = getAndCacheNodeTransaction(key);
            }
        }
        finally {
            SLOG.debug("END  LRUCache.size(): {}", lruCache.size());
            LOCK.unlock();
        }

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
            SLOG.info("Cache contains BESError object.  dataSource=\"" +
                    key + "\"");
            throw (BESError) responseObject;
        }
        // The responseObject should only ever be a Document or a BESError
        throw new IOException("Cached object is of unexpected type! " +
                "This is a bad thing! Object: "+responseObject.getClass().getCanonicalName());


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

            SLOG.info("Caching copy of BES showNode response for key: \"{}\"",key);
            result = putNodeTransaction(key, showNodeRequestDoc, response.clone());

        } catch (BESError be) {
            SLOG.info("The BES returned a BESError for key: \"{} \" CACHING BESError",key);
            result = putNodeTransaction(key, showNodeRequestDoc, be);
        }

        return result;
    }

    /**
     * Check to see if the passed NodeTransaction is stale. This is based
     * comparing its time in the cache with the UPDATE_INTERVAL.
     *
     * @param nodeTransaction The NodeTransaction to test.
     * @return Returns true is the NodeTransaction has been in the cache longer
     *         the UPDATE_INTERVAL.
     */
    private static boolean staleOrMissing(NodeTransaction nodeTransaction){
        boolean isStale = true;
        // if it's null it's stale, if it's nit null it might be stale...
        if(nodeTransaction!=null) {
            long timeInCache = System.nanoTime() - nodeTransaction.getTimeCreated();
            // Is it stale?
            isStale = timeInCache > UPDATE_INTERVAL.get();
            if(SLOG.isDebugEnabled()) {
                String msg ="nodeTransaction["+nodeTransaction.getKey()+ "] has been in cache for " +
                        timeInCache / (nanoInSeconds * 1.0) + " s  it's " + (isStale?"STALE":"FRESH");
                SLOG.debug(msg);
            }
        }
        return isStale;
    }



    /**
     * Takes a node response (Document or BESError) and tucks it into the NodeCache
     * @param key The name of the node that was retrieved.
     * @param request The BES showNode request document used to elicit the response.
     * @param response The object (either a Document or a BESError) returned by the BES transaction.
     */
    private static NodeTransaction putNodeTransaction(String key, Document request, Object response) {

        SLOG.debug("BEGIN  NODE_CACHE.size(): {} ", lruCache.size());

        NodeTransaction nodeTransaction = new NodeTransaction(key, request, response);
        SLOG.debug("Created nodeTransaction[{}] @ {}", key, nodeTransaction.getTimeCreated());

        NodeTransaction previous = lruCache.put(key, nodeTransaction);
        if (nodeTransaction == previous) {
            SLOG.warn("NodeTransaction cache updated with new (replacement) object for key: \"{}\"",key);
        } else {
            SLOG.debug("NodeTransaction cache updated by adding new object to cache using key \"{}\"",key);
        }

        SLOG.debug("END  LRUCache.size(): {}  ", lruCache.size());

        return nodeTransaction;

    }

    /**
     * Drops all references from the cache.
     */
    public void destroy(){
        LOCK.lock();
        try {
            lruCache.clear();
        }
        finally {
            LOCK.unlock();
        }
    }


    /**
     * This is a test method used to make a NodeTransaction without requiring a
     * running BES to receive a request and generate a response. THe point is
     * to provide a test fixture to allow the evaluation of the
     * NodeTransaction.comparable() interface for the purposes of sorting.
     *
     * @param id The id/key for the dummy NodeTransaction
     * @return A Dummy NodeTransaction for testing purposes.
     */
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

        LRUCache<String,NodeTransaction> cache = new LRUCache<>(5,3);

        String[] testKeys = {"foo", "bar", "moo", "soo", "bar", "baz"};

        NodeTransaction nt;
        NodeTransaction first = null;
        String msgFormat = "nodeTransaction - key: %s getLastAccessedTime(): %d set.size(): %d";
        String msg;

        for(String key: testKeys){
            nt = getDummyCachedNodeTransaction(key);
            cache.put(key,nt);
            msg = String.format(msgFormat,nt.getKey(),nt.getTimeCreated(),cache.size());
            log.info(msg);
            if(first==null)
                first = nt;
        }

        log.info("Original List: ");
        for(Map.Entry<String,NodeTransaction> entry : cache.entrySet()){
            log.info(" node[{}]: {}",entry.getKey(),entry.getValue().getTimeCreated());
        }

        cache.get("foo");

        log.info("List after get: ");
        for(Map.Entry<String,NodeTransaction> entry : cache.entrySet()) {
            log.info(" node[{}]: {}", entry.getKey(), entry.getValue().getTimeCreated());
        }
    }



}
