package opendap.bes;

import org.slf4j.Logger;

import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ndp on 4/4/16.
 */
public class BesResponseCache {


    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(BesResponseCache.class);
    }
    private static ReentrantLock lock;
    static {
        lock = new ReentrantLock();
    }

    private static long _max_cache_size = 50; // # of entries in cache
    private static double _cache_reduction_factor = 0.2; // Amount to reduce cache when purging



    private static class CachedObj implements Comparable  {
        private Object _myObj;
        Date _lastAccessedTime;
        private String _key;
        private long _serialNumber;
        private static AtomicLong counter = new AtomicLong(0);


        public CachedObj (String key, Object o){
            _key = key;
            _myObj = o;
            _lastAccessedTime = new Date();
            _serialNumber = counter.getAndIncrement();
        }

        public Object getObj(){

            if(_myObj !=null)
                return _myObj;

            return null;
        }

        @Override
        public int compareTo(Object o) {
            if (!(o instanceof CachedObj))
                throw new ClassCastException("An instance of a CachedObj object was expected.");
            CachedObj that = (CachedObj) o;
            if(this==that)
                return 0;

            if(this._lastAccessedTime.getTime() == that._lastAccessedTime.getTime()){
                return (int) (this._serialNumber - that._serialNumber);
            }

            return (int) (this._lastAccessedTime.getTime() - that._lastAccessedTime.getTime());


        }
        @Override
        public boolean equals(Object aThat) {
            if (this == aThat) return true;
            if (!(aThat instanceof CachedObj)) return false;

            CachedObj that = (CachedObj)aThat;

            return ( (this._lastAccessedTime == that._lastAccessedTime ) && ( this._myObj == that._myObj) );

        }

        @Override public int hashCode() {
            int result = 73;
            result += _lastAccessedTime.hashCode() + (_myObj==null?0: _myObj.hashCode());
            return result;
        }

    }


    private static ConcurrentHashMap<String,CachedObj> besResponseCache;
    static {
        besResponseCache = new ConcurrentHashMap<>();
    }

    private static ConcurrentSkipListSet<CachedObj> mostRecent;
    static {
        mostRecent = new ConcurrentSkipListSet<>();
    }



    private static void purgeLeastRecentlyAccessed(){

        int dropNum = (int) (_max_cache_size * _cache_reduction_factor);

        lock.lock();
        try {
            log.debug("purgeLeastRecentlyAccessed() - BEGIN  besResponseCache.size(): {}  mostRecent.size(): {}",besResponseCache.size(),mostRecent.size());
            log.debug("purgeLeastRecentlyAccessed() - dropNum: {}",dropNum);
            log.debug("purgeLeastRecentlyAccessed() - Before purge besResponseCache.size(): {}",besResponseCache.size());
            log.debug("purgeLeastRecentlyAccessed() - Before purge mostRecent.size(): {}",mostRecent.size());

            Vector<CachedObj> purgeList = new Vector<>(dropNum);

            Iterator<CachedObj> oldestToNewest = mostRecent.iterator();
            for(int i=0; i<dropNum && oldestToNewest.hasNext(); i++ ){
                CachedObj co = oldestToNewest.next();
                purgeList.add(co);
                log.debug("purgeLeastRecentlyAccessed() - Purging CachedObj for key {}",co._key);
                besResponseCache.remove(co._key);
            }
            mostRecent.removeAll(purgeList);
            log.debug("purgeLeastRecentlyAccessed() - After purge besResponseCache.size(): {}",besResponseCache.size());
            log.debug("purgeLeastRecentlyAccessed() - After purge mostRecent.size(): {}",mostRecent.size());
            log.debug("purgeLeastRecentlyAccessed() - END");

        }
        finally {
            lock.unlock();
        }




    }

    public static void putBesResponse(String key, Object o) {
        lock.lock();
        try {
            log.debug("putBesResponse() - BEGIN  besResponseCache.size(): {}  mostRecent.size(): {}",besResponseCache.size(),mostRecent.size());

            if (besResponseCache.size() >= _max_cache_size)
                purgeLeastRecentlyAccessed();

            CachedObj co = new CachedObj(key, o);
            log.debug("putBesResponse() - CachedObj created: {}"+ co._lastAccessedTime.getTime());

            boolean success = mostRecent.add(co);
            if (!success) {
                log.warn("putBesResponse() - BES Response cache mostRecent list ALREADY contained CachedObj "+ co + "  for key: \"" +
                        key + "\"");
            }

            CachedObj previous = besResponseCache.put(key, co);
            if (co != previous) {
                log.warn("putBesResponse() - BES Response cache updated with new object for key: \"" + key + "\"");
            } else {
                log.debug("putBesResponse() - BES Response cache updated by adding new object to cache using key \"" +
                        key + "\"");
            }

            log.debug("putBesResponse() - END  besResponseCache.size(): {}  mostRecent.size(): {}",besResponseCache.size(),mostRecent.size());

        } finally {
            lock.unlock();
        }
    }


    public static Object getBesResponse(String key){

        lock.lock();;
        try {
            log.debug("getBesResponse() - BEGIN  besResponseCache.size(): {}  mostRecent.size(): {}",besResponseCache.size(),mostRecent.size());

            if(key==null)
                return null;


            CachedObj co = besResponseCache.get(key);

            if(co!=null) {
                log.debug("getBesResponse() - Found cached BES Response for key \""+ key+"\"");

                log.debug("getBesResponse() - Updating mostRecent list.  mostRecent.size(): {}",mostRecent.size());
                // Update the mostRecent list.
                if(mostRecent.contains(co)){
                    log.debug("getBesResponse() - mostRecent list contains CachedObj. Will drop.");
                    mostRecent.remove(co);
                }
                co._lastAccessedTime = new Date();
                if(mostRecent.contains(co)){
                    log.debug("getBesResponse() - mostRecent list STILL contains CachedObj.");
                    //mostRecent.remove(co);
                }

                boolean status = mostRecent.add(co);
                if(status){
                    log.debug("getBesResponse() - mostRecent list successfully added updated CachedObj.");

                }
                else {
                    log.debug("getBesResponse() - mostRecent list FAIL to add updated CachedObj as it" +
                            " appears to be already in the mostRecent collection.");

                }
                log.debug("getBesResponse() - mostRecent list updated.  mostRecent.size(): {}",mostRecent.size());

                return co.getObj();

            }
            else {
                log.debug("getBesResponse() - No BES Response  cached for key \""+ key+"\"");
            }

            log.debug("getBesResponse() - END  besResponseCache.size(): {}  mostRecent.size(): {}",besResponseCache.size(),mostRecent.size());

        }
        finally {
            lock.unlock();
        }


        return null;
    }



    public static void main(String args[]) {



        TreeSet<CachedObj> set = new TreeSet<>();

        CachedObj co, a1,a2,a3;
        AtomicLong counter = new AtomicLong(0);

        co = new CachedObj("foo", new String("jhwe"));
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime.getTime()+" set.size(): "+set.size());
        a1 = co;
        a2 = co;
        a3 = co;

        co = new CachedObj("bar", new String("jhq23fgwe"));
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime.getTime()+" set.size(): "+set.size());

        co = new CachedObj("moo", new String("1wqgwebq"));
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime.getTime()+" set.size(): "+set.size());

        co = new CachedObj("soo", new String("j223rhwe"));
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime.getTime()+" set.size(): "+set.size());

        co = new CachedObj("boo", new String("rv24"));
        set.add(co);
        System.out.println("key: "+co._key+" co.getTime(): "+co._lastAccessedTime.getTime()+" set.size(): "+set.size());


        System.out.println("List: ");
        for(CachedObj cco: set){
            System.out.println(cco._key);
        }

        set.remove(a1);
        a1._lastAccessedTime = new Date();
        set.add(a1);

        System.out.println("List: ");
        for(CachedObj cco: set){
            System.out.println(cco._key+": "+cco._lastAccessedTime.getTime());
        }



    }
}
