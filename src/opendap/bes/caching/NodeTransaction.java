package opendap.bes.caching;

import com.sun.istack.Nullable;
import opendap.bes.BESError;
import org.jdom.Document;

/**
 * This class is used to wrap the BES response object (Document or BESError) that
 * is being cached along the creation time and the Node's key (aka datasource)
 * string. The creation time is used by the cache to determine when the cached
 * object has become "stale" and needs to be refreshed.
 */
class NodeTransaction  {

    private long timeCreated;
    private String key;

    private Document besResponseDoc;
    private BESError besError;

    /**
     * Creates a NodeTransaction with a creation time of "now".
     *
     * @param key The key value string (aka datasource) which elicited the
     *            response document from the BES.
     * @param response The response Document (returned by the BES) associated
     *                 with "key".
     */
    NodeTransaction(String key, Document response){
        this.key = key;
        this.besResponseDoc = response;
        this.besError = null;
        timeCreated = System.nanoTime();
    }

    /**
     * Creates a NodeTransaction with a creation time of "now".
     * @param key The key value string (aka datasource) which elicited the
     *            error from the BES.
     * @param error The BESError returned by the BES in response to a showNode
     *              request for "key".
     */
    NodeTransaction(String key, BESError error){
        this.key = key;
        this.besResponseDoc = null;
        this.besError = error;
        timeCreated = System.nanoTime();
    }

    /**
     * Returns the key value string used to elict the BE showNode response
     * held in this NodeTransaction.
     * @return The key string
     */
    public String getKey(){
        return key;
    }

    /**
     *
     * @return The time (aka System.nanoTime()) that this NodeTransaction
     * instance was created.
     */
    long getTimeCreated() {
        return timeCreated;
    }

    /**
     * @return The BES showNode response returned by the BES in response to a
     * showNode request for "key".
     */
    Document getResponseDocument(){ return besResponseDoc; }

    /**
     * @return The BESError object returned by the BES in response to a showNode
     * request for "key".
     */
    BESError getBesError() { return besError; }

    /**
     *
     * @return True is the BES returned a BESError object in response to a
     * showNode request for "key". False otherwise.
     */
    public boolean isError(){ return besError!=null;}

}
