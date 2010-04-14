/////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2010 OPeNDAP, Inc.
// Author: James Gallagher <jgallagher@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.metacat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provide a cache for XML/HTTP response objects. This can hold both the DDX
 * XML/Text version of the response/object and the Last Modified Time (LMT). The
 * class thus can provide the basis for a simple HTTP 1.1 cache where a
 * conditional GET can be used to eliminate repeat transfers of a Response. The
 * cache can also be used directly to process a collection of Responses
 * retrieved earlier.
 * 
 * The cache uses ConcurrentHashMap objects to store the information and
 * serializes those to disk files for persisteance.
 * 
 * @note Performance impacts of caching: Test case: Crawl the Response URLs, Get
 *       the DDX responses and build EML using XSLT transform. 1. Crawling 153
 *       DDX URLs with no caching took an average of 64s. 2. Crawling the 153
 *       DDX URLs with the cache where a conditional GET was used took 39s 3.
 *       Processing the cached DDXs with no HTTP access of any kind too 9s There
 *       was no difference bewteen the time associated with saving the DDXs and
 *       LMT in the cache and not (so caching had no discernable overhead in
 *       this example)
 * @author jimg
 * 
 */
public class ResponseCache {
    public class ResponseCacheKeysEnumeration implements Enumeration<String> {
    	private Enumeration<String> e;
 
    	ResponseCacheKeysEnumeration() {
    		e = responseCache.keys();
    	}
    	
		@Override
		public boolean hasMoreElements() {
			return e.hasMoreElements();
		}

		@Override
		public String nextElement() {
			return e.nextElement();
		}
	}

    public class ResponseVisitedKeysEnumeration implements Enumeration<String> {
    	private Enumeration<String> e;
 
    	ResponseVisitedKeysEnumeration() {
    		e = responsesVisited.keys();
    	}
    	
		@Override
		public boolean hasMoreElements() {
			return e.hasMoreElements();
		}

		@Override
		public String nextElement() {
			return e.nextElement();
		}
	}
    
	final static String VisitedName = "Visited.save";
    final static String CacheName = "Cache.save";
    
    private String cacheBaseName;
    
    // This hash map is used to prevent reading Responses when a previous crawl
    // has already done it. Use the last modified date to determine newness.
    // This hash map will be written to disk so that the record of Responses can
    // span individual runs.
    private ConcurrentHashMap<String, Long> responsesVisited;
    
    // Cache the Response documents themselves. This will allow for EML processing
    // runs without actually crawling and re-crawling remote servers.
    private ConcurrentHashMap<String, String> responseCache;
    
    private boolean saveMyState;
    
    /** Build an instance of the ResponseCache and by default restore the saved
     *  LMT and Response information and, when the cache is no longer needed,
     *  save its state to disk.
     *  
     * @throws Exception
     */
    public ResponseCache(String name) throws Exception {
    	this(name, true, true);
    }
    
    /** Build an instance of the ResponseCache. Always save the exit state.
     * 
     * @param restoreState True if the current saved state should be used.
     * @throws Exception
     */
    public ResponseCache(String cacheName, boolean restoreState) throws Exception {
    	this(cacheName, restoreState, true);
    }

    /** Build an instance of ResponseCache. The parameters provide  a way to 
     * control how the cache manages its persistent state inforamtion.
     * 
     * @param restoreState True if the saved state should be used.
     * @param saveState True if the state of the cache should be saved when
     * it is removed (i.e., when finalize() is called)
     * @throws Exception
     */
    public ResponseCache(String cacheName, boolean restoreState, boolean saveState) throws Exception {

    	cacheBaseName = cacheName;
    	
    	responsesVisited = new ConcurrentHashMap<String, Long>();
    	responseCache = new ConcurrentHashMap<String, String>();
    	
    	saveMyState = saveState;
    	
    	try {
    		if (restoreState) {
    			restoreVisitedState();
    			restoreCacheState();
    		}
    	}
    	catch (Exception e) {
			throw new Exception("ResponseCache constructor said: " + e.getMessage());
		}
    }
    	
    protected void finalize() throws Exception {
    	if (saveMyState)
    		saveState();
    }
    
    /** For the cache to save its state now. 
     * 
     * @TODO Determine if this is needed.
     * @throws Exception
     */
    public void saveState() throws Exception {
    	saveVisitedState();
    	saveCacheState();
    }
    
    @SuppressWarnings("unchecked")
	private void restoreVisitedState() throws Exception{
		FileInputStream fis;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(cacheBaseName + VisitedName);
			ois = new ObjectInputStream(fis);
			
    		responsesVisited = (ConcurrentHashMap<String, Long>)ois.readObject();
    	}
    	catch (FileNotFoundException e) {
    		//log.error("Could not open the Responses Visited cache - file not found.");
    	}
		catch (ClassNotFoundException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not find the class when reading the Responses Visited Hash Map."
							+ e.getMessage());
		}
		catch (ClassCastException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not cast the persistent store to a Concurrent Hash Map"
							+ e.getMessage());
		}
		catch (java.io.IOException e) {
			throw new Exception("ResponseCache: " + "Generic Java I/O Exception."
					+ e.getMessage());
		}    	
		finally {
    		if (ois != null)
    			ois.close();
    	}
    }
    
    private void saveVisitedState() throws Exception {
		FileOutputStream fos;
		ObjectOutputStream oos = null;
    	try {
    		fos = new FileOutputStream(cacheBaseName + VisitedName);
    		oos = new ObjectOutputStream(fos);

    		oos.writeObject(responsesVisited);
    	}
    	catch (FileNotFoundException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not open the Responses Visited cache - file not found."
							+ e.getMessage());
    	}
    	catch (SecurityException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not open the Responses Visited cache - permissions violation."
							+ e.getMessage());
    	}	
    	catch (java.io.IOException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Generic Java I/O Exception."
							+ e.getMessage());
    	}
    	finally {
    		if (oos != null)
    			oos.close();
    	}
    }
    
    @SuppressWarnings("unchecked")
	private void restoreCacheState() throws Exception {
		FileInputStream fis;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(cacheBaseName + CacheName);
			ois = new ObjectInputStream(fis);
			
    		// Wrap this Response Cache in its own class and use this:
    		//@SuppressWarnings("unchecked")
    		// to quite the warning. later...
    		responseCache = (ConcurrentHashMap<String, String>)ois.readObject();
    	}
    	catch (FileNotFoundException e) {
    		// This error is not fatal so we log it but leave the cache empty.
    		//log.error("Could not open the Response Cache - file not found.");
    	}
    	catch (SecurityException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not open the Response Cache - permissions violation."
							+ e.getMessage());
    	}
    	catch(ClassNotFoundException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not find the class when reading the Response Cache Hash Map."
							+ e.getMessage());
    	}
    	catch (ClassCastException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not cast the persistent store to a Concurrent Hash Map"
							+ e.getMessage());
    	}
    	catch (java.io.IOException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Generic Java I/O Exception."
							+ e.getMessage());
    	}
    	finally {
    		if (ois != null)
    			ois.close();
    	}
    }
    
    private void saveCacheState() throws Exception {
		FileOutputStream fos;
		ObjectOutputStream oos = null;
    	try {
    		fos = new FileOutputStream(cacheBaseName + CacheName);
    		oos = new ObjectOutputStream(fos);

    		oos.writeObject(responseCache);
    	}
    	catch (FileNotFoundException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not open the Response Cache - file not found."
							+ e.getMessage());
    	}
    	catch (SecurityException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Could not open the Response Cache - permissions violation."
							+ e.getMessage());
    	}	
    	catch (java.io.IOException e) {
			throw new Exception(
					"ResponseCache: "
							+ "Generic Java I/O Exception."
							+ e.getMessage());
    	}
    	finally {
    		if (oos != null)
    			oos.close();
    	}
    }
    
    /** Add a Response document to the cache using its URL as a key.
     * 
     * @param URL The URL
     * @param doc The Docuemnt
     */
    public void setCachedResponse(String URL, String doc) {
    	responseCache.put(URL, doc);
    }
    
    /** Retrieve a Response document from the cache.
     * 
     * @param URL Get the document paired with this URL 
     * @return The document
     */
    public String getCachedResponse(String URL) {
    	return responseCache.get(URL);    	
    }
    
    /** Get all of the keys in the Response document cache.
     * 
     * @return An Enumeration that can be used to access all of the keys in
     * the cache. Use getCachedResponse(key) to get the Response docuements.
     */
    public Enumeration<String> getResponseKeys() {
    	return new ResponseCacheKeysEnumeration();
    }

    /** When was this Response URL last visited?
     * 
     * @param URL The URL
     * @return The time when this URL was last visited or 0 if it's never
     * been looked at. Time is given in seconds since 1 Jan 1970.
     */
    public long getLastVisited(String URL) {
    	if (responsesVisited.containsKey(URL))
    		return responsesVisited.get(URL);
    	else
    		return 0;
    }
    
    /** Has this URL been visited?
     * 
     * @param URL The URL
     * @return true if the URL has been visited.
     */
    public boolean isVisited(String URL) {
    	return responsesVisited.containsKey(URL);
    }
    
    /** Add or update the entry in Response URL cache. This is used to store Last
     * Modified Times for a given URL. The time used is initially the current
     * time. If the Response URL has been previously visited
     *  (see getLastVisited()), then that LMT can be used with a 
     *  conditional HTTP GET request. Of course, this cache can be used in
     *  other ways too.
     * 
     * @param URL The URL
     * @param d The Last modified time to be paired with the URL
     */
    public void setLastVisited(String URL, long d) {
    	responsesVisited.put(URL, d);
    }
    
    /** Get all of the keys in the URL cache.
     * 
     * @return An Enumeration that can be used to access all of the keys in
     * the cache. Use getLastVisited(key) to get the response LMT times.
     */
    public Enumeration<String> getLastVisitedKeys() {
    	return new ResponseVisitedKeysEnumeration();
    }
}
