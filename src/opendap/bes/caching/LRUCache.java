package opendap.bes.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class LRUCache<K,V> extends LinkedHashMap<K,V> {

    private int maxEntries;

    public LRUCache(int maxEntries, int initialCapacity) {
        super(initialCapacity, (float)Math.log(2), true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxEntries;
    }

    public int maxEntries(){ return maxEntries; }

    private void show(){
        Logger log = LoggerFactory.getLogger(LRUCache.class);
        log.debug("Show...");
        for (Map.Entry<K,V> entry : entrySet()) {
            log.debug("{}:{}", entry.getKey(), entry.getValue());
        }
    }



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
