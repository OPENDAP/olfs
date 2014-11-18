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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Use this with crawlers to manage a collection of DDX responses and the URLs
 * that reference them. This class can cache just the URLs, or both the URLs
 * and the matching responses. It can perform conditional gets as well, using
 * postgres (via ResponseCachePostgres) as a simple HTTP/1.1 cache.
 *  
 * @author jimg
 *
 */
public class DDXRetriever {
	
    private static Logger log = LoggerFactory.getLogger(DDXRetriever.class);

	/// Use the cache.
    // private boolean useCache;
    private boolean readOnly;
	
    // The DDXCache that holds both the DDXs LMT and the XML/text
    private ResponseCachePostgres DDXCache = null;
    
	public DDXRetriever(String cacheName) throws Exception {
		this(true, cacheName);
	}

	public DDXRetriever(boolean readOnly, String namePrefix)  throws Exception {
		
		// this.useCache = useCache;
		this.readOnly = readOnly;
		
		DDXCache = new ResponseCachePostgres(readOnly, namePrefix + "_DDX", "ddx_responses");
	}
	
	/**
	 * This 'main' has many conflicting features/options. I've changed the 
	 * function of this class considerably since it was written.
	 * @param args
	 */
	public static void main(String[] args) {
		DDXRetriever retriever = null;
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		
		// The default action is to read from the net, checking the cache and
		// print the document to standard output.
		
		options.addOption("v", "verbose", false, "Be verbose");
		options.addOption("r", "read-only", false, "Only rad from the cache; no updates");
		
		options.addOption("n", "cache-name", true, "Use this to set a prefix for the cache name.");
		options.addOption("d", "ddx-url", true, "Get and print the DDX using the referenced URL. If this is not given, print all of the DDX URLs in the named cache.");
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    boolean verbose = line.hasOption("verbose");
		    
		    String ddxURL = line.getOptionValue("ddx-url");
		    if (verbose && ddxURL != null && !ddxURL.isEmpty())
		    	System.out.println("DDX URL: " + ddxURL);

		    String cacheName = line.getOptionValue("cache-name");
		    if (cacheName != null && cacheName.isEmpty())
		    	throw new Exception("--cache-name is required.");
		    
		    if (verbose)
		    	System.out.println("cacheName: " + cacheName);
		    
		    boolean readOnly = line.hasOption("read-only");
		    
		    retriever = new DDXRetriever(readOnly, cacheName);

		    if (ddxURL != null && !ddxURL.isEmpty()) {
		    	System.out.println("DDX: " + retriever.getDDXDoc(ddxURL));
		    }
		    else {
		    	Enumeration<String> ddxs = retriever.getCachedDDXURLs();
		    	int i = 0;
		    	while (ddxs.hasMoreElements()) {
		    		++i;
		    		ddxURL = ddxs.nextElement();
		    		System.out.println("DDX URL: " + ddxURL);
		    		if (verbose)
		    			System.out.println("DDX: " + retriever.getCachedDDXDoc(ddxURL));
		    	}
		    	System.out.println("Found " + i +" URLs");
		    }
		    
			// Save the cache if not read-only
	    	if (!readOnly)
	    		retriever.saveDDXCache();

		}
		catch( ParseException exp ) {
		    System.err.println( "Unexpected exception:" + exp.getMessage() );
		}
		
		catch (Exception e) {
			System.err.println("Error : " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the cache. Use the methods in ResponseCachePostgres to get information
	 * from the cache. For this class the cache holds the LMTs and DDX for
	 * each URL (the URLs are the keys).
	 * @return The DDX cache.
	 */
	/*
	public ResponseCachePostgres getCache() {
		return DDXCache;
	}
	*/

    /** Simple method to test if the DDX will parse. Generally there's no 
     * need to call this but it'll be useful when developing the crawler.
     * 
     * @note This method must be called by client code; it is not used by
     * any of the methods here.
     * 
	 * @param ddxString The DDX to test
     * @return true if the DDX parses, false if the SAX parser throws an
     * exception
     */
	public boolean isWellFormedDDX(String ddxString) {
		try {
			org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
			@SuppressWarnings("unused")
			org.jdom.Document ddxDoc = sb.build(new ByteArrayInputStream(ddxString.getBytes()));
		} 
		catch (Exception e) {
			return false;
		}
		return true;
	}

	private String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * returns null which means there's no more data to read. Each line will
		 * be appended to a StringBuilder and the result returned as a String.
		 */
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} 
			finally {
				is.close();
			}
			
			return sb.toString();
		}
		else {
			return "";
		}
	}	
	
	/**
	 * Given a URL to a DDX, get the DDX document. If the DDXRetriever was
	 * built with caching turned on, this uses a poor man's HTTP/1.1 cache
	 * based on Last Modified Times. 
	 * 
	 * If caching is on, then calling this on a series of DDX URLs will fill
	 * the cache. If the cache is saved and later used again it is possible
	 * to re-read the URLs straight from the cache.
	 *  
	 * @see getCache()
	 * @param DDXURL Get the DDX referenced by this URL
	 * @return The DDX document, in a String
	 * @throws Exception 
	 */
	public String getDDXDoc(String DDXURL) throws Exception {
		String ddx = null;

		URL url = new URL(DDXURL);
		URLConnection connection = url.openConnection();

		if (DDXCache.getLastVisited(DDXURL) != 0
				&& DDXCache.getCachedResponse(DDXURL) != null)
			connection.setIfModifiedSince(DDXCache.getLastVisited(DDXURL));

		// Here's where we'd poke in a header to ask for the DAP3.2 DDX

		connection.connect();

		// Cast to a HttpURLConnection
		if (connection instanceof HttpURLConnection) {
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			int code = httpConnection.getResponseCode();

			// If we have something, process. Since a conditional get was
			// used, the response might be empty (code == 304) and nothing
			// should be done in that case
			switch (code) {
			case 200:
				ddx = convertStreamToString(httpConnection.getInputStream());
				// Update the last visited and document caches
				if (!readOnly) {
					Date date = new Date();
					DDXCache.setLastVisited(DDXURL, date.getTime());
					DDXCache.setCachedResponse(DDXURL, ddx);
				}
				break;

			case 304:
				ddx = DDXCache.getCachedResponse(DDXURL);
				if (!readOnly) {
					// Update the last visited cache to now
					Date date = new Date();
					DDXCache.setLastVisited(DDXURL, date.getTime());
				}
				break;

			default:
				log.error("Expected a 200 or 304 HTTP return code. Got: "
								+ new Integer(code).toString());
			}
		}
		else {
			throw new MalformedURLException("Expected a HTTP URL (" + DDXURL + ").");
		}

		return ddx;
	}
	
	public void cacheDDXURL(String ddxUrl) throws Exception {
		Date date = new Date();
		DDXCache.setLastVisited(ddxUrl, date.getTime());
	}
	
	public Enumeration<String> getCachedDDXURLs() {
		return DDXCache.getLastVisitedKeys();
	}
	
	public String getCachedDDXDoc(String DDXURL) throws Exception {
		if (DDXCache == null)
			throw new Exception("Caching is off but I was asked to read from the cache.");
		return DDXCache.getCachedResponse(DDXURL);
	}
	
	public long getCachedDDXLMT(String DDXURL) throws Exception {
		if (DDXCache == null)
			throw new Exception("Caching is off but I was asked to read from the cache.");
		return DDXCache.getLastVisited(DDXURL);
	}
	
	public void saveDDXCache() throws Exception {
		if (readOnly)
			throw new Exception("I was asked to save a read-only cache.");
		DDXCache.saveState();
	}
}
