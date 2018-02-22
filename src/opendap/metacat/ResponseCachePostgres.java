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

//import org.apache.jcs.JCS;
//import org.apache.jcs.access.exception.CacheException;

import java.sql.*;

/**
 * Provide a cache for XML/HTTP response objects. This can hold both the DDX
 * XML/Text version of the response/object and the Last Modified Time (LMT). The
 * class thus can provide the basis for a simple HTTP 1.1 cache where a
 * conditional GET can be used to eliminate repeat transfers of a Response. The
 * cache can also be used directly to process a collection of Responses
 * retrieved earlier.
 * 
 * The cache uses Postgres to store the information responses (i.e., documents)
 * and a ConcurrentHashMap (that it serializes those to disk files for
 * persistence) to store the LMT times of the URLs visited. In order for
 * Postgres to be used as the cache, the database must be setup. There must be
 * a database called 'crawl_cache' and it should have a table whose name will
 * be passed to the constructor of this class using the 'tableName' parameter.
 * That table should have columns named: key, url and doc. Make these using
 * "CREATE TABLE ddx_responses (key SERIAL PRIMARY KEY, url varchar(256), 
 * doc text);" where 'ddx_responses' is the value of 'tableName'.
 * 
 * This class was modified from a version that could optionally use a hash map
 * to store the responses.
 * 
 * @note Performance impacts of caching: Test case: Crawl the Response URLs, Get
 *       the DDX responses and build EML using XSLT transform. 1. Crawling 153
 *       DDX URLs with no caching took an average of 64s. 2. Crawling the 153
 *       DDX URLs with the cache where a conditional GET was used took 39s 3.
 *       Processing the cached DDXs with no HTTP access of any kind too 9s There
 *       was no difference between the time associated with saving the DDXs and
 *       LMT in the cache and not (so caching had no discernible overhead in
 *       this example)
 * 
 * @note More performance information (4/28/10): Memory use. I retrieved ~43K
 *       THREDDS catalogs and the space required was approximately 1GB. The
 *       catalogs were generally uniform and about 1.2K each, so 43,000 should
 *       have used about 53MB of storage space.
 * 
 * @note Switched to Postgres for the cache (which is really a misnomer, its not
 *       a true cache but a persistent record of the crawl). 42K catalogs read
 *       and stored in ~5 hours. I'm still using the ConcurrentHashMap to store
 *       the 'visited' information. I've also tried the Java caching system
 *       (JCS) but that seems to be designed as a true cache where it's not
 *       possible to be sure a previously cached item is still in the cache.
 *       (5/14/10) And I tried using Metacat as the 'cache', but it's too
 *       picky about the validity of the XML.
 * 
 * @todo Make a mode of operation that does not require Postgres. Sometimes you
 * 		 this is only used to store URLs and not responses, so Postgres isn't
 * 		 used. However, it's always initialized.
 *  
 * @author jimg
 * 
 */
public class ResponseCachePostgres {
	
    private static Logger log = LoggerFactory.getLogger(ResponseCachePostgres.class);

	final static String VisitedName = "_Visited.ser";
    
    private String cacheBaseName;
    private boolean readOnly;
    
    // This hash map is used to prevent reading Responses when a previous crawl
    // has already done it. Use the last modified date to determine newness.
    // This hash map will be written to disk so that the record of Responses can
    // span individual runs.
    private ConcurrentHashMap<String, Long> responsesVisited;
    
    private String pgUrl = "jdbc:postgresql://localhost:5432/crawl_cache:";
    private String pgTable;
    private String pgUsername = "metacat";
    private String pgPassword = "metacat";
    private Connection pgCache = null;
        
    public class ResponseCacheKeysEnumeration implements Enumeration<String> {
    	ResultSet rs = null;
    	PreparedStatement ps = null;
    	
		ResponseCacheKeysEnumeration() throws Exception {
			log.debug("Returning an enumeration of keys from Postgres.");
			try {
				ps = pgCache.prepareStatement("SELECT URL FROM " + pgTable);
				rs = ps.executeQuery();
			}
			catch (SQLException e) {
				log.error("Could not build result set for ResponseCacheKeysEnumeration.", e);
				throw e;
			}
		}

		@Override
		public boolean hasMoreElements() {
			try {
				return rs.next();
			}
			catch (SQLException e1) {
				log.error("Could not get next row in the result set for a ResponseCacheKeysEnumeration.", e1);
				return false;
			}
		}

		@Override
		public String nextElement() {
			try {
				return rs.getString(1);
			}
			catch (SQLException e1) {
				log.error("Could not get a string from the result set for a ResponseCacheKeysEnumeration.", e1);
				return "";
			}
		}
		
		protected void finalize() {
			log.debug("Running finalize() in ResponseCacheKeysEnumeration.");
			try {
				rs.close();
				ps.close();
			}
			catch (SQLException e1) {
				log.error("Could not get a string from the result set for a ResponseCacheKeysEnumeration.", e1);
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
    
	/**
	 * Build an instance of ResponseCachePostgres. By default this cache
	 * supports both reads and writes. It will zero-out any previously cached
	 * responses.
	 * 
	 * @param cacheName
	 *            The basename to use for the 'visited' cache.
	 * @param tableName
	 *            The name for the Postgres table in the 'crawl_cache' database
	 *            where responses should be stored.
	 * @see ResponseCachePostgres(boolean readOnly, String cacheName, String tableName)
	 */
    public ResponseCachePostgres(String cacheName, String tableName) throws Exception {
    	this(false, cacheName, tableName);
    }
    
    /** Build an instance of ResponseCachePostgres. 
     *
     * @param readOnly Is this cache object being created so that a client
     * program can read previously cached data? If this is true, the client
     * should never try to write to the cache.
     * @param cacheName The basename to use for the 'visited' cache.
     * @param tableName The name for the Postgres table in the 'crawl_cache'
     * database where responses should be stored.
     */
    public ResponseCachePostgres(boolean readOnly, String cacheName, String tableName) throws Exception {

    	cacheBaseName = cacheName;
    	this.readOnly = readOnly;
    	
    	responsesVisited = new ConcurrentHashMap<String, Long>();
    	// If we are making a new cache, then don't bother restoring the old
    	// visited caches - they will be overwritten when this exits.
		if (readOnly) {
			try {
				restoreVisitedState();
			}
			catch (Exception e) {
				log.info("Could not read the 'visited' cache from disk.");
			}
		}

		log.debug("Configuring the Postgres data base as cache using table: " + tableName);
		try {
			Class.forName("org.postgresql.Driver");
			// The pgUrl contains the name of the database within postgres.
			pgCache = DriverManager.getConnection(pgUrl, pgUsername, pgPassword);
			//String sqlCacheName = cacheName.replace('.', '_');
			pgTable = cacheName.replace('.', '_') + "_" + tableName;
			
			if (!readOnly) {
				// If this cache is to be written to, then make new tables!
				//
				// If the pgTable exists, drop it. Except that this doesn't check
				// for existence, it just drops the table and throws away any error
				// that results from the table not being there in the first place.
				// DROP TABLE pgTable
				// Make a new table. This gives the code an empty table in the DB
				// which is much faster than a table filled up with stuff.
				// CREATE TABLE pgTable(key SERIAL PRIMARY KEY, url VARCHAR(256), doc TEXT);
				Statement pg = pgCache.createStatement();
				try {
					try {
						String drop_table = "DROP TABLE " + pgTable;
						pg.executeUpdate(drop_table);
					}
					catch (SQLException e) {
						// ignored
						log.debug("Caught a sql exception when dropping table " + pgTable + " an expected error when the table does not exist");
					}

					String create_table = "CREATE TABLE " + pgTable + "(key SERIAL PRIMARY KEY, url VARCHAR(256), doc TEXT)";
					pg.executeUpdate(create_table);
				}
				catch (SQLException e) {
					log.error("Caching: Could not access the database/cache.", e);
					throw new Exception("SQLException: " + e.getMessage());
				}
				finally {
					try {
						pg.close();
					}
					catch (SQLException e) {
						log.error("Cache read: Could not close the prepared statement.", e);
					}
				}
			}
		}
		catch (ClassNotFoundException e) {
			log.error("Could not load Postgres JDBC driver: " + e.getMessage());
			throw new Exception(e.getMessage());
		}
		catch (SQLException e) {
			log.error("SQLException: " + e.getMessage());
			throw new Exception("SQLException: " + e.getMessage());
		}
    }
    	
    /**
     * This won't be called when an out of memory exception is thrown.
     */
    @Override
    protected void finalize() throws Exception {
    	saveState();
    }
    
    /** Force the cache to save its state now. 
     * 
     * @TODO Determine if this is needed.
     * @throws Exception
     */
    public void saveState() throws Exception {
    	if (!readOnly)
    		saveVisitedState();
    	
    	if (pgCache != null) {
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
    		log.error("Could not open the Responses Visited cache - file not found.");
    	}
		catch (ClassNotFoundException e) {
			throw new Exception(
					"ResponseCachePostgres: "
							+ "Could not find the class when reading the Responses Visited Hash Map."
							+ e.getMessage());
		}
		catch (ClassCastException e) {
			throw new Exception(
					"ResponseCachePostgres: "
							+ "Could not cast the persistent store to a Concurrent Hash Map"
							+ e.getMessage());
		}
		catch (java.io.IOException e) {
			throw new Exception("ResponseCachePostgres: " + "Generic Java I/O Exception."
					+ e.getMessage());
		}    	
		finally {
    		if (ois != null)
    			ois.close();
    	}
    }
    
    private void saveVisitedState() throws Exception {
    	if (readOnly)
    		throw new Exception("The ResponseCachePostgres was initialized as read-only but 'saveVisitedState()' was called.");
    	
		FileOutputStream fos;
		ObjectOutputStream oos = null;
    	try {
    		fos = new FileOutputStream(cacheBaseName + VisitedName);
    		oos = new ObjectOutputStream(fos);

    		oos.writeObject(responsesVisited);
    	}
    	catch (FileNotFoundException e) {
			throw new Exception(
					"ResponseCachePostgres: "
							+ "Could not open the Responses Visited cache - file not found."
							+ e.getMessage());
    	}
    	catch (SecurityException e) {
			throw new Exception(
					"ResponseCachePostgres: "
							+ "Could not open the Responses Visited cache - permissions violation."
							+ e.getMessage());
    	}	
    	catch (java.io.IOException e) {
			throw new Exception(
					"ResponseCachePostgres: "
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
    public void setCachedResponse(String URL, String doc) throws Exception {
		log.debug("Caching " + URL + " in postgres.");

    	if (readOnly)
    		throw new Exception("The ResponseCachePostgres was initialized as read-only but 'setCachedResponse()' was called.");
    	
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
			throw new Exception("SQLException: " + e.getMessage());
		} finally {
			try {
				rs.close();
				ps.close();
			}
			catch (SQLException e) {
				log.error("Cache read: Could not close the prepared statement.", e);
			}
		}
    }
    
    /** Retrieve a Response document from the cache.
     * 
     * @param URL Get the document paired with this URL 
     * @return The document
     */
    public String getCachedResponse(String URL) throws Exception {
    	String doc = null;
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
					throw new Exception("While accessing the entry for [" + URL + "] the cache found " + countString + " entries.");
				}
			}
		}
		catch (SQLException e) {
			log.error("Cache read: Could not access the database/cache: " + e.getMessage());
			throw new Exception("SQLException: " + e.getMessage());
		}
		catch (Exception e) {
			log.info("Cache access info:  " + e.getMessage());
			// Allow this kind of exception to just result in an empty return
			// throw e;
		}
		finally {
			try {
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
			}
			catch (SQLException e) {
				log.error("Cache read: Could not close the prepared statement: " + e.getMessage());
			}
		}
    	
    	return doc;
    }
    
    /** 
     * Get all of the keys in the Response document (postgres) cache. It's 
     * likely that you want to use the keys from the 'visited' cache instead.
     * 
     * @see getLastVisitedKeys()
     * @return An Enumeration that can be used to access all of the keys in
     * the cache. Use getCachedResponse(key) to get the Response docuements.
     */
    public Enumeration<String> getResponseKeys() throws Exception {
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
    
    /** 
     * Add or update the entry in Response URL cache. This is used to store Last
     * Modified Times for a given URL. The time used is initially the current
     * time. If the Response URL has been previously visited
     *  (see getLastVisited()), then that LMT can be used with a 
     *  conditional HTTP GET request. Of course, this cache can be used in
     *  other ways too.
     * 
     * @param URL The URL
     * @param d The Last modified time to be paired with the URL
     */
    public void setLastVisited(String URL, long d) throws Exception {
    	if (readOnly)
    		throw new Exception("The ResponseCachePostgres was initialized as read-only but 'setLastVisited()' was called.");
    
    	responsesVisited.put(URL, d);
    }
    
    /** 
     * Get all of the keys in the URL cache named when this class was 
     * instantiated. This returns all of the keys in Visited cache, not the
     * Postgres database cache; several 'visited' cached might use a single
     * table in the crawl_cache postgres database. 
     * 
     * @return An Enumeration that can be used to access all of the keys in
     * the cache. Use getLastVisited(key) to get the response LMT times.
     */
    public Enumeration<String> getLastVisitedKeys() {
    	return new ResponseVisitedKeysEnumeration();
    }
}
