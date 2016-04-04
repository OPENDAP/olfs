package opendap.bes;

import org.slf4j.Logger;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ndp on 4/4/16.
 */
public class BesResponseCache {


    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(BesResponseCache.class);
    }



    private static class CachedObj implements Comparable  {
        private Object myObj;
        Date lastAccessedTime;
        public CachedObj (Object o){
            myObj = o;
            lastAccessedTime = new Date();
        }

        public Date getCreationTime(){
            return (Date)lastAccessedTime.clone();
        }
        public Object getObj(){

            if(myObj !=null)
                return myObj;

            return null;
        }

        @Override
        public int compareTo(Object o) {
            if (!(o instanceof CachedObj))
                throw new ClassCastException("An instance of a CachedObj object was expected.");
            CachedObj that = (CachedObj) o;
            if(this==that) return 0;
            return (int) (this.lastAccessedTime.getTime() - that.lastAccessedTime.getTime());


        }
        @Override
        public boolean equals(Object aThat) {
            if (this == aThat) return true;
            if (!(aThat instanceof CachedObj)) return false;

            CachedObj that = (CachedObj)aThat;

            return ( (this.lastAccessedTime == that.lastAccessedTime ) && ( this.myObj == that.myObj ) );

        }

        @Override public int hashCode() {
            int result = 73;
            result += lastAccessedTime.hashCode() + (myObj==null?0:myObj.hashCode());
            return result;
        }

    }


    private static ConcurrentHashMap<String,CachedObj> besResponseCache;
    static {
        besResponseCache = new ConcurrentHashMap<>();
    }

    public static void putBesResponse(String key, Object o){
        CachedObj co = new CachedObj(o);
        co = besResponseCache.put(key, co);
        if (co != null) {
            log.warn("putBesResponse() - BES Response cache updated with new object for key: \"" + key + "\"");
        } else {
            log.debug("putBesResponse() - BES Response cache updated by adding new object to cache using key \"" +
                    key + "\"");
        }
    }

    public static void putBesResponseIfAbsent(String key, Object o){
        CachedObj co = new CachedObj(o);
        co = besResponseCache.putIfAbsent(key, co);
        if (co != null) {
            log.debug("putBesResponseIfAbsent() - BES Response cache NOT updated, key already present: \"" + key + "\"");
        } else {
            log.debug("putBesResponseIfAbsent() - BES Response cache updated by adding new object to cache using key \"" +
                    key + "\"");
        }
    }

    public static Object getBesResponse(String key){


        if(key==null)
            return null;

        CachedObj co = besResponseCache.get(key);


        if(co!=null) {
            log.debug("getBesResponse() - Found cached BES Response for key \""+ key+"\"");

            /*
            long now = new Date().getTime();
            long elapsedCacheTime = now - co.getCreationTime().getTime();

            if(elapsedCacheTime > maxCacheTime ){
                log.info("Cached document expired "+ elapsedCacheTime+"ms old.");
                hm.remove(key);
                log.info("Cached document for  "+ key +" removed from cache.");
                return null;
            }
            */
            return co.getObj();

        }
        else {
            log.debug("getBesResponse() - No BES Response  cached for key \""+ key+"\"");
        }


        return null;
    }

}
