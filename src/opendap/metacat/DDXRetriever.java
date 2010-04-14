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
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class handles the task of getting a DDX given its URL. It will
 *  test the returned document to see if it is well-formed and it will
 *  cache the document. 
 *  
 * @author jimg
 *
 */
public class DDXRetriever {
	/// Use DDXsVisted cache. If true, visited DDX won't be revisited.
	private boolean useDDXsVisited;

	/// Cache the DDX responses
	private boolean saveDDXs;
	
    // The DDXCache that holds both the DDXs LMT and the XML/text
    private ResponseCache DDXCache;
    
    private static Logger log;

	public DDXRetriever() throws Exception {
		this(LoggerFactory.getLogger(DapIngest.class), true, true, "");
	}
	
	public DDXRetriever(boolean useDDXsVisited, boolean saveDDXs, String namePrefix) throws Exception {
		this(LoggerFactory.getLogger(DapIngest.class), useDDXsVisited, saveDDXs, namePrefix);
	}

	public DDXRetriever(Logger log, boolean useDDXsVisited, boolean saveDDXs, String namePrefix) throws Exception {
		DDXRetriever.log = log;
		this.useDDXsVisited = useDDXsVisited;
		this.saveDDXs = saveDDXs;
		
		// The first parameter to DDXCache() restores the cache from its
		// persistent form and will cause the cache to be written when 
		// the DDXCache instance is collected.
		if (useDDXsVisited || saveDDXs)
			DDXCache = new ResponseCache(namePrefix + "DDX", useDDXsVisited, saveDDXs);
		else
			DDXCache = null;
	}
	
	/**
	 * @param args
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		DDXRetriever retriever = null;
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		
		// The default action is to read from the net, checking the cache and
		// print the document to standard output.
		options.addOption("r", "read-cache", false, "Read DDX from the cache");
		options.addOption("n", "no-cache", false, "Do not cache DDXs. Ignored with -r or -p");
		options.addOption("p", "print-cached", false, "Print all of the cached DDXs");
		
		options.addOption( OptionBuilder.withLongOpt( "cache-name" )
				.withDescription( "Use this to set a prefix for the cache name." )
				.hasArg()
				.withArgName("cacheName")
				.create() );

		options.addOption( OptionBuilder.withLongOpt( "ddx-url" )
		                                .withDescription( "use this as the DDX URL" )
		                                .hasArg()
		                                .withArgName("ddxURL")
		                                .create() );
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    String ddxURL = line.getOptionValue("ddx-url");
		    System.out.println("DDX URL: " + ddxURL);

		    boolean useCache = !line.hasOption( "n");
		    String cacheNamePrefix = line.getOptionValue("cache-name");
		    
		    retriever = new DDXRetriever(useCache, useCache, cacheNamePrefix);

		    if (line.hasOption( "r")) {
		    	System.out.println("DDX: " + retriever.getCachedDDXDoc(ddxURL));
		    }
		    else if (line.hasOption( "p")) {
		    	Enumeration<String> ddxs = retriever.DDXCache.getLastVisitedKeys();
		    	while (ddxs.hasMoreElements()) {
		    		ddxURL = ddxs.nextElement();
		    		System.out.println("DDX URL: " + ddxURL);
		    		System.out.println("DDX: " + retriever.DDXCache.getCachedResponse(ddxURL));
		    	}
		    }
		    else {
		    	System.out.println("DDX: " + retriever.getDDXDoc(ddxURL));
		    }
		    
			// Save the cache if the neither the 'no-cache' nor read-cache
			// options were used (in the latter case, nothing was added to the
			// cache).
	    	if (!(line.hasOption( "n") && line.hasOption( "r")))
	    		retriever.DDXCache.saveState();

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
	 * Get the cache. Use the methods in ResponseCache to get information
	 * from the cache. For this class the cache holds the LMTs and DDX for
	 * each URL (the URLs are the keys).
	 * @return The DDX cache.
	 */
	public ResponseCache getCache() {
		return DDXCache;
	}

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
			log.error("Exception while testing DDX: " + e.getLocalizedMessage());
			return false;
		}
		return true;
	}

	private String convertStreamToString(InputStream is) throws IOException {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "UTF-8"));
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
			} finally {
				is.close();
			}
			return sb.toString();
		} else {
			return "";
		}
	}	
	
	/**
	 * Given a URL to a DDX, get the DDX document. If the DDXRetriever was
	 * built with caching turned on, this uses a poor man's HTTP/1.1 cache
	 * based on Last Modified Times. 
	 * 
	 *  If caching is on, then calling this on a series of DDX URLs will fill
	 *  the cache. If the cache is saved and later used again it is possible
	 *  to re-read the URLs straight from the cache.
	 *  
	 *  @see getCache()
	 * @param DDXURL Get the DDX referenced by this URL
	 * @return The DDX document, in a String
	 */
	public String getDDXDoc(String DDXURL) {
		String ddx = null;
		try {
			URL url = new URL(DDXURL);
			URLConnection connection = url.openConnection();

			if (useDDXsVisited)
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
					if (saveDDXs) {
						Date date = new Date();
						DDXCache.setLastVisited(DDXURL, date.getTime());
						DDXCache.setCachedResponse(DDXURL, ddx);
						log.debug("Adding/Updating entry for " + DDXURL);
					}
					break;
					
				case 304:
					if (useDDXsVisited)
						ddx = DDXCache.getCachedResponse(DDXURL);
					// Update the last visited cache to now
					if (saveDDXs) {
						Date date = new Date();
						DDXCache.setLastVisited(DDXURL, date.getTime());
						log.debug("Updating last visited entry for " + DDXURL);
					}
					break;
					
				default:
					throw new IOException("Expected a 200 or 304 HTTP return code. Got: " + new Integer(code).toString());
				}
			} 
			else {
				throw new MalformedURLException("Expected a HTTP URL ("
						+ DDXURL + ").");
			}
		}
		catch (MalformedURLException e) {
			log.error("Problem with XML Document URL: " + DDXURL
					+ " Caught a MalformedURLException.  Message: "
					+ e.getMessage());
		} catch (IOException e) {
			log.error("Problem retrieving XML Document: " + DDXURL
					+ " Caught a IOException.  Message: " + e.getMessage());
		}
		
		return ddx;
	}
	
	public String getCachedDDXDoc(String DDXURL) throws Exception {
		if (!useDDXsVisited || DDXCache == null)
			throw new Exception("Caching is off but I was asked to read from the cache.");
		return DDXCache.getCachedResponse(DDXURL);
	}
	
	public long getCachedDDXLMT(String DDXURL) throws Exception {
		if (!useDDXsVisited || DDXCache == null)
			throw new Exception("Caching is off but I was asked to read from the cache.");
		return DDXCache.getLastVisited(DDXURL);
	}
	
	public void saveDDXCache() throws Exception {
		if (!useDDXsVisited || DDXCache == null)
			throw new Exception("Caching is off but I was asked to save the cache.");
		DDXCache.saveState();
	}
}
