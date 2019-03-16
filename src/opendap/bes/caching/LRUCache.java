package opendap.bes.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class implements an in-memory Least Recently Used (LRU) cache using
 * LinkedHashMap.
 *
 * This class is not thread safe and must be protected in a multi-threaded
 * environment by utilizing external locks or wrapping it like:
 *
 *    Map m = Collections.synchronizedMap(new LRUCache(...));
 *
 * (See https://docs.oracle.com/javase/7/docs/api/java/util/LinkedHashMap.html
 * for more on this.)
 *
 * @param <K>
 * @param <V>
 */
class LRUCache<K,V> extends LinkedHashMap<K,V> {

    private int maxEntries;

    /**
     * Creates an LRUCache with a maximum size of maxEntries and an initial
     * capacity of initialCapacity
     * @param maxEntries The maximum number of entries allowed in the cache.
     * @param initialCapacity The initial capacity of the cache.
     */
    LRUCache(int maxEntries, int initialCapacity) {
        super(initialCapacity, (float)Math.log(2), true);
        this.maxEntries = maxEntries;
    }

    /**
     * This overridden method implements the policy for automatically removing
     * the least recently used mappings when the cache is full and new mappings
     * are added to the map.
     * @param eldest The least recently accessed entry
     * @return True when the the least recently accessed entry should be
     * removed from the cache, false otherwise.
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxEntries;
    }

    /**
     *
     * @return The maximum number of entries in the cache.
     */
    int maxEntries(){ return maxEntries; }



    /**
     * Simple method to simplify the test code in main(...).
     */
    private void show(){
        Logger log = LoggerFactory.getLogger(LRUCache.class);
        log.debug("Show...");
        for (Map.Entry<K,V> entry : entrySet()) {
            log.debug("{}:{}", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Test code.
     * @param args Are ignored.
     */
    public static void main(String[] args){
        Logger log = LoggerFactory.getLogger(LRUCache.class);

        int maxEntries = 10;
        int initialCapacity = 5;

        long start = System.nanoTime();

        LRUCache<String,Long> cache = new LRUCache<>(maxEntries,initialCapacity);

        log.debug("Loading values");
        for(int i=0; i<10 ; i++){
            cache.put(Integer.toString(i), System.nanoTime()-start);
        }
        cache.show();

        log.debug("Retrieving values");
        for(int i=1; i<10 ; i+=2){
            cache.get(Integer.toString(i));
        }
        cache.show();

        log.debug("Loading values");
        for(int i=10; i<30 ; i++){
            cache.put(Integer.toString(i), System.nanoTime()-start);
        }
        cache.show();

        log.debug("Retrieving values");
        for(int i=0; i<20 ; i+=2){
            cache.get(Integer.toString(i));
        }

        log.debug("Loading values");
        for(int i=0; i<5 ; i++){
            cache.put(Integer.toString(i), System.nanoTime()-start);
        }
        cache.show();
    }
}
