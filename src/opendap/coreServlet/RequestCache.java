/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.coreServlet;

import org.slf4j.Logger;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ndp
 * Date: Nov 19, 2008
 * Time: 11:19:36 AM
 */
public class RequestCache {

    private static final String REQUEST_ID_KEY = "request-id";

    private static final Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(RequestCache.class);
    }

    private static final ConcurrentHashMap<Thread, HashMap<String,CachedObj>> cache;
    static {
        cache = new ConcurrentHashMap<>();
    }

    private static final ReentrantLock lock;
    static {
        lock = new ReentrantLock();
    }
    //private static long maxCacheTime = 10000; // in milliseconds

    private static class CachedObj {
        private final Object myObj;
        Date creationTime;
        public CachedObj (Object o){
            myObj = o;
            creationTime = new Date();
        }

        public Date getCreationTime(){
            return (Date)creationTime.clone();
        }
        public Object getObj(){
            return myObj;
        }

    }

    /**
     * Opens a new request cache for the current thread (one request per thread
     * in Tomcat land). If the cache is already set up for this thread nothing
     * is done. If a new cache is created then the request id is computed and
     * added to the cache.
     * @param request The request which is being serviced by the current thread.
     */
    public static void open(HttpServletRequest request){
        Thread thisThread = Thread.currentThread();
        if(cache.containsKey(thisThread)){
            log.info("Request cache for thread: " + Thread.currentThread() +
                    " already exists. No need to create one.");
            HashMap<String,CachedObj> tcache = cache.get(thisThread);
            CachedObj idObj = tcache.get(REQUEST_ID_KEY);
            if(idObj==null || !(idObj.getObj() instanceof String) ){
                RequestId reqId = ReqInfo.getRequestId(request);
                put(REQUEST_ID_KEY,reqId);
                log.info("Cached new request id: {}", reqId);
            }
            else {
                log.info("Found existing request id: {}", ((String)idObj.getObj()));
            }
        }
        else {
            HashMap<String, CachedObj> hm = new HashMap<>();
            cache.put(thisThread, hm);
            log.info("Created request cache for thread: {}", thisThread.getName());
            RequestId reqId = ReqInfo.getRequestId(request);
            put(REQUEST_ID_KEY,reqId);
            log.info("Cached new request id: {}", reqId);
        }
    }

    public static void close(){
        int size = 0;
        Thread thisThread = Thread.currentThread();
        HashMap<String,CachedObj> value = cache.remove(thisThread);
        if(value!=null){
            size = value.size();
            value.clear();
        }
        log.info("Closed RequestCache for thread: {} Cache contained {} values.",thisThread,size);
    }


    public static void put(String key, Object o){

        CachedObj co = new CachedObj(o);
        HashMap<String, CachedObj> hm = cache.get(Thread.currentThread());

        if(hm == null) {
            log.warn("put() - Thread cache not initialized. {} object not cached under key '{}'", o.getClass().getName(),key);
            return;
        }

        co = hm.put(key, co);
        if (co != null) {
            log.warn("put() - Response cache updated with new Object for key: \"" + key + "\"");
        } else {
            log.debug("put() - Response cache updated by adding new object to cache using key \"" +
                    key + "\"");
        }
    }


    public static Object get(String key){

        HashMap<String, CachedObj> hm = cache.get(Thread.currentThread());
        if(hm==null || key==null)
            return null;

        CachedObj co = hm.get(key);
        if(co!=null) {
            log.debug("Found cached document for key \""+ key+"\"");
            return co.getObj();
        }
        else {
            log.debug("No Document cached for key \""+ key+"\"");
        }
        return null;
    }

    /**
     * Every request cache instance has some type of request id. See
     * opendap.coreServlet.ReqInfo.getRequestId() for more on how that's
     * handled.
     *
     * @return The request id being serviced by the current thread.
     */
    public static RequestId getRequestId(){
        Object id = get(REQUEST_ID_KEY);
        if(id != null){
            return (RequestId)id;
        }
        return new RequestId("ERROR(NOT FATAL): No request id was located in the RequestCache.");
    }

}
