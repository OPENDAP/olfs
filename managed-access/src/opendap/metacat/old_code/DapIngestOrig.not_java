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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Vector;
import java.util.Enumeration;

import net.sf.saxon.s9api.SaxonApiException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
//import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

import opendap.metacat.ThreddsCatalogUtil;

/** Crawl a THREDDS catalog and read all of the DDX objects for the data
 * sources it references. Process those so that they are transformed into EML
 * documents and store the result in a MetaCat database. The crawl must not
 * visit the same DDX object more than once and should only fetch a new 
 * instance of a DDX when it has changed.
 * 
 * @note In this code 'DDX' is synonymous with 'dataset' in the sense that 
 * the DDX is treated as if it is the same as the dataset.
 *
 */
public class DapIngestOrig {

    private static Logger log = LoggerFactory.getLogger(DapIngest.class);
    
    /// This is the prefix for all the document ids made up of DDX URLs
    final static String docidPrefix = "DDX.";
    
    /// If metacat needs an explicit schema for our generated EML, use this.
    final static String docidSchema = "/Users/jimg/src/eml-2.10/eml.xsd";
    
    /// Login credentials for metacat.
    private String metacatUsername = "";
    private String metacatPassword = "";
    
    // This hash map is used to ensure that catalogs are not crawled more
    // than once (preventing loops, etc.)
    private int catalogsVisited;
    private int ddxsVisited;
    
    private boolean readDDXsFromCache = false;
    private boolean verbose = false;
    
    // Metacat
    private Metacat metacat = null;
    
    // Caching DDX access
    private DDXRetriever ddxRetriever;
    
    // Caching EML access
    private EMLBuilder emlBuilder;
    
    // This provides a way to get catalogs, iterate over their child URLs and
    // access DDX urls to datasets in the catalog
    private ThreddsCatalogUtil threddsCatalogUtil;
    /*    
    DapIngest(boolean useThreddsCache, boolean useDDXCache, boolean useEMLCache)
    		throws SaxonApiException, Exception {
    	this(useThreddsCache, useDDXCache, useEMLCache, "", false, "");
    }
    */
    DapIngestOrig(boolean useThreddsCache, boolean useDDXCache, boolean useEMLCache,
    		String cacheNamePrefix, boolean useMetacat, String metacatUrl)
    		throws SaxonApiException, Exception {

    	log.debug("Cache name prefix: " + cacheNamePrefix);
    	
    	threddsCatalogUtil = new ThreddsCatalogUtil(useThreddsCache, cacheNamePrefix, useThreddsCache);

    	catalogsVisited = 0;
    	ddxsVisited = 0;
    	
    	try {
    		ddxRetriever = new DDXRetriever(useDDXCache, cacheNamePrefix);
			
    		emlBuilder = new EMLBuilder(useEMLCache, cacheNamePrefix);
 		} 
    	catch (SaxonApiException e) {
			log.debug("Transform returned an SaxonApiException: " + e.getLocalizedMessage());
			throw e;
		}
    	catch (Exception e) {
			log.debug("Exception: " + e.getLocalizedMessage());
			throw e;
		}
    	
    	if (useMetacat) {
    		log.debug("Building metacat cache connection.");
			try {
				log.debug("Test Metacat: " + metacatUrl);
				metacat = MetacatFactory.createMetacatConnection(metacatUrl);

                log.debug("username: " + metacatUsername + ", password: " + metacatPassword);
                String response = metacat.login(metacatUsername, metacatPassword);
                
                log.debug("login(): response=" + response);
                /*metacatSessionId = metacat.getSessionId();*/
                
                log.debug("login(): Session ID=" + this.metacat.getSessionId());
            } 
            catch (MetacatAuthException mae) {
            	log.debug("Authorization failed:\n" + mae.getMessage());
            	throw new Exception("Metacat authorization failed:\n" + mae.getMessage());
            } 
            catch (MetacatInaccessibleException mie) {
            	log.debug("Metacat Inaccessible:\n" + mie.getMessage());
            	throw new Exception("Metacat inaccessible:\n" + mie.getMessage());
            } 
    	}

    }
 
    public static void main(String[] args) {
    	
    	DapIngestOrig ingester;
    	
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		
		// Make up a better set of options... Need to control caching for each
		// of thredds, ddx and eml, both of using the (N-1)th as input to the 
		// Nth and if results are to be cached in the DB or just looked at (for
		// trial runs).
		options.addOption("r", "read-from-ddx-cache", false, "Read ddxs from the cache");
		options.addOption("n", "no-cache", false, "Do not use caching");
		options.addOption("s", "save-cache", false, "Save the caches");
		options.addOption("v", "verbose", false, "Print information about the crawl");
		options.addOption("e", "build-eml", false, "Build EML from the DDX using XSLT");
		options.addOption("E", "read-from-eml-cache", false, "Don't get DDXs or process DDXs to make EML, just read EML from the cache.");
		
		options.addOption("N", "cache-name", true, "Use this to set a prefix for the cache name. Ignored when not using the cache.");
		options.addOption("R", "catalog-root", true, "Use this as the root catalog. Ignored when reading from the cache." );
		options.addOption("i", "insert-eml", true, "Add/Insert teh generated EML into Metacat. You must provide a URL to an instance of Metacat.");
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    boolean insertEML = line.hasOption("i");
		    String metacatURL = line.getOptionValue("i", "");
		    
		    boolean useCache = !line.hasOption( "n");
		    String cacheNamePrefix = "";
		    if (line.hasOption("N")) {
		    	log.debug("Found cache-name");
		    	cacheNamePrefix = line.getOptionValue("N", "");
		    }

		    ingester = new DapIngestOrig(useCache, useCache, useCache, cacheNamePrefix, insertEML, metacatURL);
		    
		    if (line.hasOption( "v"))
		    	ingester.verbose = true;
		    
		    ingester.readDDXsFromCache = line.hasOption("r");
		    
		    boolean emlGeneration = false;
		    if (line.hasOption( "e"))
		    	emlGeneration = true;
		    
		    String catalogURL = "";
		    if (line.hasOption("R"))
		    	catalogURL = line.getOptionValue("R");

		    if (ingester.verbose)
		    	System.out.println("Catalog Root: " + catalogURL);

		    if (line.hasOption("E"))
		    	ingester.printEMLFromCache(System.out);
		    else if (line.hasOption( "r"))
    			ingester.buildEMLFromCachedDDXs(emlGeneration, insertEML);
    		else
    			ingester.crawlCatalog(catalogURL, emlGeneration, insertEML);

		    if (ingester.verbose)
    			ingester.recordStats();
        
    		if (line.hasOption( "s")) {
    			ingester.ddxRetriever.saveDDXCache();
    			ingester.threddsCatalogUtil.saveCatalogCache();
    			ingester.emlBuilder.saveEMLCache();
    		}
    	}
		catch (ParseException pe) {
    		System.err.print("Command line option parse error: " + pe.getMessage());
			
		}
    	catch (Exception e) {
    		System.err.print("Error: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }

    void recordStats() {
    	log.info("THREDDS Catalog URLs Visited: " + catalogsVisited);
    	Enumeration<String> e = threddsCatalogUtil.getCachedCatalogEnumeration();
    	while (e.hasMoreElements()) {
    		String key = e.nextElement();
    		log.info(key + ": 1");
    	}
    	
    	log.info("DDX URLs Visited: " + ddxsVisited);
    	e = ddxRetriever.getCache().getLastVisitedKeys();
    	while (e.hasMoreElements()) {
    		String key = e.nextElement();
    		log.info(key + ": " + ddxRetriever.getCache().getLastVisited(key));
    	}
    }
    
    void crawlCatalog(String catalogURL, boolean EMLGeneration, boolean insertEML) throws Exception {
    	Enumeration<String> catalogs = threddsCatalogUtil.getCatalogEnumeration(catalogURL);
    	
    	// First get references to any DDX objects at the top level
    	log.info("About to get DDX URLS from: " + catalogURL);
    	++catalogsVisited;
    	Vector<String> DDXURLs = threddsCatalogUtil.getDDXUrls(catalogURL);
    	for (String DDXURL: DDXURLs) {
    		++ddxsVisited;
    		examineDDX(DDXURL, EMLGeneration, insertEML);
    	}
    	
    	while (catalogs.hasMoreElements()) {
    		String catalog = catalogs.nextElement();
        	log.info("About to get DDX URLS from: " + catalog);
        	++catalogsVisited;
        	DDXURLs = threddsCatalogUtil.getDDXUrls(catalog);
        	for (String DDXURL: DDXURLs) {
        		++ddxsVisited;
        		examineDDX(DDXURL, EMLGeneration, insertEML);
        	}
    	}
    }

	void buildEMLFromCachedDDXs(boolean EMLGeneration, boolean insertEML) throws Exception {
		// String emlString = null;
    	Enumeration<String> keys = ddxRetriever.getCache().getResponseKeys();
		while (keys.hasMoreElements()) {
			String DDXURL = (String) keys.nextElement();
			++ddxsVisited;
			examineDDX(DDXURL, EMLGeneration, insertEML);
		}
	}	

	void printEMLFromCache(PrintStream ps) throws Exception {
    	Enumeration<String> keys = emlBuilder.getCache().getResponseKeys();
		while (keys.hasMoreElements()) {
			String DDXURL = (String) keys.nextElement();
			++ddxsVisited;
			String eml = emlBuilder.getCache().getCachedResponse(DDXURL);
			
			if (ps != null) {
				ps.println("DDX: " + DDXURL);
				ps.println("EML:");
				ps.println(eml);
			}
			else {
				log.info("DDX: " + DDXURL);
				log.info("EML: " + eml);
			}
		}		
	}

	void insertEMLFromCache() throws Exception {
		Enumeration<String> keys = emlBuilder.getCache().getResponseKeys();
		while (keys.hasMoreElements()) {
			String ddxUrl = (String) keys.nextElement();
			++ddxsVisited;
			String eml = emlBuilder.getCache().getCachedResponse(ddxUrl);

			log.debug("DDX: " + ddxUrl);
			log.debug("EML: " + eml);
			insertEML(ddxUrl, eml);
		}
	}
	
	/**
	 * For a given DDX URL, get the DDX and then, if that works get the EML. If
	 * that works, insert the EML into the data store. Note that the ddxRetriever
	 * object implements a simple HTTP/1.1 cache and an optional test for
	 * well-formed responses.
	 * 
	 * @param ddxUrl
	 *            A DDX URL
	 */
	void examineDDX(String ddxUrl, boolean emlGeneration, boolean insertEML) throws Exception {
		String ddx = "";
		if (readDDXsFromCache)
			ddx = ddxRetriever.getCachedDDXDoc(ddxUrl);
		else
			ddx = ddxRetriever.getDDXDoc(ddxUrl);
		
		if (ddx == null) {
			throw new Exception("No DDX returned from: " + ddxUrl);
		}

		if (emlGeneration) {
			String eml = emlBuilder.getEML(ddxUrl, ddx);
			if (eml == null) {
				throw new Exception("No EML returned from: " + ddxUrl);
			}

			if (insertEML)
				insertEML(ddxUrl, eml);
		}
	}

    /**
     * To use a URL as a DocID in metacat, any dots (.) in the URL must be
     * escaped. This function repalces dots with %2e
     * @param URL
     * @return The esacped URL
     */
    private static String escapedURL(String URL) {
    	return URL.replaceAll("\\.", "%2e");
    }
    
    /**
     * Given a URL, return the corresponding document id. A metacat docuement 
     * id must be of the form <string>.<string>.<digit> Where the dots are 
     * literal and <string> must not contain any dots. Furthermore, only the
     * last two parts are used when accessing the document id; the first 
     * <string> is ignored. This method returns a document id by combining the 
     * value of the class' docidScope with an escaped URL and a '1'. 
     * @param url
     * @return A docuement id string suitable for use with metacat
     */
    private static String getDocid(String url) {
    	return docidPrefix + escapedURL(url) + ".1";
    }
    
    /**
     * Insert the EML document into Metacat. This assumes that the EML document
     * is indexed using a DDX URL. 
     * 
     * @param emlString
     */
	void insertEML(String ddxUrl, String emlString) {

		String docid = getDocid(ddxUrl);
		log.debug("Caching " + ddxUrl + "(docid:" + docid + ") in metacat.");

		try {
			// FileReader schema = new FileReader(docidSchema);
			// metacat.insert(String docid, Reader xmlDocument, Reader schema);
			metacat.insert(docid, new StringReader(emlString), null);
		}
		catch (FileNotFoundException e) {
			log.error("Could not open the file: " + docidSchema);
		}
		catch (InsufficientKarmaException e) {
			log.error("Error caching the response: Insufficent rights for the operation: " + e.getMessage());
		}
		catch (MetacatException e) {
			log.error("Error caching the response: " + e.getMessage());
		}
		catch (IOException e) {
			log.error("Error caching the response: Unknown error: " + e.getMessage());
		}
		catch (MetacatInaccessibleException e) {
			log.error("Error caching the response: Error reading the xml document: " + e.getMessage());
		}
	}
}
