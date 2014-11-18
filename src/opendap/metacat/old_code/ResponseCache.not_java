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
//import java.io.FileReader;
//import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.io.Reader;
//import java.io.StringReader;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;

import java.sql.*;

//import edu.ucsb.nceas.metacat.client.*;

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
 *       
 * @note More performance information (4/28/10): Memory use. I retrieved ~43K
 *       THREDDS catalogs and the space required was approximately 1GB. The 
 *       catalogs were generally uniform and about 1.2K each, so 43,000 should
 *       have used about 53MB of storage space.
 *       
 * @note Switched to Postgres for the cache (which is really a misnomer, its 
 * 	     not a true cache but a persistent record of the crawl). 42K catalogs
 *       read and stored in ~5 hours. I'm still using the ConcurrentHashMap
 *       to store the 'visited' information. I've also tried the Java caching
 *       system (JCS) but that seems to be designed as a true cache where it's
 *       not possible to be sure a previously cached item is still in the cache.
 *       (5/14/10)
 *       
 * @author jimg
 * 
 */
public class ResponseCache {
	
    private static Logger log = LoggerFactory.getLogger(ResponseCache.class);

    public class ResponseCacheKeysEnumeration implements Enumeration<String> {
    	private Enumeration<String> e = null;
    	ResultSet rs = null;
    	PreparedStatement ps = null;
    	
    	ResponseCacheKeysEnumeration() {
        	log.debug("Returning the ResponseCacheKeysEnumeration; usePostges is "
        			+ new Boolean(usePostgres).toString() + ".");
    		
    		if (useJCS) {
    			log.debug("Returning an enumeration of keys from JCS.");
    		}
    		else if (usePostgres) {
    			log.debug("Returning an enumeration of keys from Postgres.");
    			try {
					ps = pgCache.prepareStatement("SELECT URL FROM " + pgTable);
					rs = ps.executeQuery();
				}
				catch (SQLException e) {
					log.error("Could not build result set for ResponseCacheKeysEnumeration.", e);
				}
			}
    		else {
    			e = responseCache.keys();
    		}
    	}
    	
		@Override
		public boolean hasMoreElements() {
			if (e != null)
				return e.hasMoreElements();
			else if (usePostgres){
				try {
					return rs.next();
				}
				catch (SQLException e1) {
					log.error("Could not get next row in the result set for a ResponseCacheKeysEnumeration.", e);
					return false;
				}
			}
			
			return false;
		}

		@Override
		public String nextElement() {
			if (e != null)
				return e.nextElement();
			else if (usePostgres) {
				try {
					return rs.getString(1);
				}
				catch (SQLException e1) {
					log.error("Could not get a string from the result set for a ResponseCacheKeysEnumeration.", e);
					return "";
				}
			}
			
			return "";
		}
		
		protected void finalize() {
			log.debug("Running finalize() in ResponseCacheKeysEnumeration.");
			if (usePostgres) {
				try {
					rs.close();
					ps.close();
				}
				catch (SQLException e1) {
					log.error("Could not get a string from the result set for a ResponseCacheKeysEnumeration.", e);
				}
			}
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
    
    // False by default, this controls whether the document cache uses 
    // JCS (if true) or a ConcurrentHashMap (if false, the default)
    private boolean useJCS = false;
    private String JCSConfigFilename = "/jcs.cache.ccf";
    private static final String cacheRegionName = "default";
    private JCS jcsCache = null;

    private boolean usePostgres = false;
    private String pgUrl = "jdbc:postgresql://localhost:5432/crawl_cache:";
    private String pgTable;
    private String pgUsername = "metacat";
    private String pgPassword = "metacat";
    private Connection pgCache = null;
    
    /** Build an instance of the ResponseCache and by default restore the saved
     *  LMT and Response information and, when the cache is no longer needed,
     *  save its state to disk.
     *  
     *  @note This constructor using ConcurrentHashMaps for both the 'visited'
     *  and 'response' caches. Thus, both caches must fit in memory at runtime.
     *  
     *  @param name The basename to use for the cache(s).
     *  
     * @throws Exception
     */
    public ResponseCache(String name) throws Exception {
    	this(name, true, true, false, "");
    }
    
    /** Build an instance of the ResponseCache. Always save the exit state.
     * 
     *  @note This constructor using ConcurrentHashMaps for both the 'visited'
     *  and 'response' caches. Thus, both caches must fit in memory at runtime.
     *
     *  @param name The basename to use for the cache(s).
     * @param restoreState True if the current saved state should be used.
     * @throws Exception
     */
    public ResponseCache(String cacheName, boolean restoreState) throws Exception {
    	this(cacheName, restoreState, true, false, "");
    }

    /** Build an instance of ResponseCache. The parameters provide  a way to 
     * control how the cache manages its persistent state inforamtion.
     * 
     *  @note This constructor using ConcurrentHashMaps for both the 'visited'
     *  and 'response' caches. Thus, both caches must fit in memory at runtime.
     *
     * @param name The basename to use for the cache(s).
     * @param restoreState True if the saved state should be used.
     * @param saveState True if the state of the cache should be saved when
     * it is removed (i.e., when finalize() is called)
     * @throws Exception
     */
    public ResponseCache(String cacheName, boolean restoreState, boolean saveState) throws Exception {
    	this(cacheName, restoreState, saveState, false, "");
    }
    
    public ResponseCache(String cacheName, boolean restoreState, boolean saveState,
    		boolean usePostgres) throws Exception {
    	this(cacheName, restoreState, saveState, false, "thredds_responses");
    }
    
    /** Build an instance of ResponseCache. The parameters provide  a way to 
     * control how the cache manages its persistent state inforamtion. Using 
     * this constructor is the only way to configure the cache to use the
     * Metacat XML database to cache response objects.
     * 
     *  @note This constructor using a ConcurrentHashMap for  the 'visited'
     *  cache and, if a metacat URL is given, the Metacat XML database for
     *  the response cache. This means that while the 'visited' cache must
     *  fit in memory, the response cache need not. Also, unlike the hash map
     *  based response cache, Metacat will retrieve documents based on XPath
     *  queries. 
     *
     * @param name The basename to use for the cache(s).
     * @param restoreState True if the saved state should be used.
     * @param saveState True if the state of the cache should be saved when
     * it is removed (i.e., when finalize() is called)
     * @param useJCS True if the Java Cache System should be used; false by
     * default.
     * @throws Exception
     */
    public ResponseCache(String cacheName, boolean restoreState, boolean saveState, 
    		boolean usePostgres, String tableName) throws Exception {

    	cacheBaseName = cacheName;
    	
    	responsesVisited = new ConcurrentHashMap<String, Long>();
    	
    	if (useJCS) {
    		log.debug("Using JCS for cache.");
    		// this.useJCS = useJCS;
			// in your constructor you might do this
            try {
            	JCS.setConfigFilename(JCSConfigFilename);
                jcsCache = JCS.getInstance(cacheRegionName);
            }
            catch ( CacheException e ) {
                log.error( "Problem initializing cache for region name ["
                  + cacheRegionName + "].", e );
            }
    	}
    	else if (usePostgres) {
    		log.debug("Configuring the Postgres data base as cache");
    		try {
    			Class.forName("org.postgresql.Driver");
    			pgCache = DriverManager.getConnection(pgUrl, pgUsername, pgPassword);
    			this.usePostgres = usePostgres;
    			
    			this.pgTable = tableName;
    		}
    		catch (ClassNotFoundException e) {
    			log.error("Could not load Postgres JDBC driver: " + e.getMessage());
    		}
    		catch (SQLException e) {
    			log.error("SQLException: " + e.getMessage());
    		}
    	}
    	else {
    		log.debug("Building ConcurrentHashMap cache data structure.");
    		responseCache = new ConcurrentHashMap<String, String>();
    	}
    	
    	saveMyState = saveState;
    	
    	try {
    		if (restoreState) {
    			restoreVisitedState();
    			if (!this.useJCS && !this.usePostgres)
    				restoreCacheState();
    		}
    	}
    	catch (Exception e) {
			throw new Exception("ResponseCache constructor said: " + e.getMessage());
		}
    }
    	
    /**
     * This won't be called when an out of memory exception is thrown.
     */
    @Override
    protected void finalize() throws Exception {
    	if (saveMyState)
    		saveState();
    	if (usePostgres && pgCache != null) {
    		pgCache.close();
    		pgCache = null;
    	}
    }
    
    /** For the cache to save its state now. 
     * 
     * @TODO Determine if this is needed.
     * @throws Exception
     */
    public void saveState() throws Exception {
    	saveVisitedState();
    	// Not appropriate for JCS
    	if (!this.useJCS && !this.usePostgres)
    		saveCacheState();
    	
    	if (usePostgres && pgCache != null) {
    		pgCache.close();
    		pgCache = null;
    	}
    }
    
    @SuppressWarnings("unchecked")
	private void restoreVisitedState() throws Exception {
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
    
    /**
     * This method will only be called when the cache is using a ConcurrentHashMap
     * to cache responses. 
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	private void restoreCacheState() throws Exception {
		FileInputStream fis;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(cacheBaseName + CacheName);
			ois = new ObjectInputStream(fis);
			
    		responseCache = (ConcurrentHashMap<String, String>)ois.readObject();
    	}
    	catch (FileNotFoundException e) {
    		// This error is not fatal so we log it but leave the cache empty.
    		log.error("Could not open the Response Cache - file not found.");
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
    
    /**
     * As with the 'restore' method for the response cache, this is only called
     * when using a ConcurrentHashMap. 
     * 
     * @throws Exception
     */
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
    	if (useJCS) {
            try {
                // if it isn't null, insert it
                if ( doc != null ) {
                	log.debug("Caching " + URL + " in JCS.");
                    jcsCache.put( URL, doc );
                }
            }
            catch ( CacheException e ) {
                 log.error( "Problem putting 'doc' in the cache, for key: " + URL, e );
            }
    	}
    	else if (usePostgres) {
    		log.debug("Caching " + URL + " in postgres.");
    		
    		PreparedStatement ps = null;
    		ResultSet rs = null;
			try {
				String select = "SELECT doc FROM " + pgTable + " WHERE URL = ?";
				ps = pgCache.prepareStatement(select);
				ps.setString(1, URL);
				log.debug("About to send: " + ps.toString() + " to the database.");
				rs = ps.executeQuery();
				if (rs != null && rs.next()) {
					String update = "UPDATE " + pgTable + " SET doc=? WHERE URL=?";
					ps = pgCache.prepareStatement(update);
					ps.setString(1, doc);
					ps.setString(2, URL);
				} else {
					String insert = "INSERT INTO " + pgTable + " (URL, doc) VALUES (?, ?)";
					ps = pgCache.prepareStatement(insert);
					ps.setString(1, URL);
					ps.setString(2, doc);

				}
				log.debug("About to send: " + ps.toString() + " to the database.");
				
				ps.executeUpdate();
			}
			catch (SQLException e) {
				log.error("Caching: Could not access the database/cache.", e);
			}
			finally {
				try {
					rs.close();
					ps.close();
				}
				catch (SQLException e) {
					log.error("Cache read: Could not close the prepared statement.", e);
				}
			}
    	}
    	else {
    		log.debug("Caching " + URL + " in a local hash map.");
    		responseCache.put(URL, doc);
    	}
    }
    
    /** Retrieve a Response document from the cache.
     * 
     * @param URL Get the document paired with this URL 
     * @return The document
     */
    public String getCachedResponse(String URL) {
    	String doc = null;
    	if (useJCS) {
    		log.debug("Reading " + URL + " from JCS.");
            doc = (String) jcsCache.get( URL );
    	}
    	else if (usePostgres) {
    		log.debug("Reading " + URL + " from postgres.");
    		PreparedStatement ps = null;
    		ResultSet rs = null;
			try {
				String select = "SELECT doc FROM " + pgTable + " WHERE URL = ?";
				ps = pgCache.prepareStatement(select);
				ps.setString(1, URL);
				
				log.debug("About to send: " + ps.toString() + " to the database.");
				rs = ps.executeQuery();
				if (rs != null) {
					int count = 0;
					while (rs.next()) {
						++count;
						doc = rs.getString(1);
					}
					rs.close();
					if (count != 1) {
						String countString = new Integer(count).toString();
						throw new Exception("While accessing the entry for [" 
								+ URL + "] the cache found " 
								+ countString
								+ " entries.");
					}
				}
			}
			catch (SQLException e) {
				log.error("Cache read: Could not access the database/cache.", e);
			}
			catch (Exception e) {
				log.error("Cache access error.", e);
			}
			finally {
				try {
					rs.close();
					ps.close();
				}
				catch (SQLException e) {
					log.error("Cache read: Could not close the prepared statement.", e);
				}
			}
    	}
    	else {
    		log.debug("Reading " + URL + " from the local hash map cache.");
    		doc = responseCache.get(URL);
    	}
    	
    	return doc;
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
