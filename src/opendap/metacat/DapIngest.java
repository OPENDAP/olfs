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

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

//import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

import net.sf.saxon.s9api.SaxonApiException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.semantics.IRISail.ThreddsCatalogUtil;

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
public class DapIngest {

    /*final static String ddx2emlPath = "ddx2eml-2.0.xsl";*/
    
    private static Logger log = LoggerFactory.getLogger(DapIngest.class);

    // This hash map is used to ensure that catalogs are not crawled more
    // than once (preventing loops, etc.)
    /* private ConcurrentHashMap<String, Integer> catalogsVisited;*/
    private int catalogsVisited;
    private int DDXsVisited;
    /*private Date date;*/
    
    private boolean verbose = false;
    
    // Caching DDX access
    private DDXRetriever DDXSource;
    
    // Caching EML access
    private EMLRetriever EMLSource;
    
    // Cache EML here
    /*private ResponseCache EMLCache;*/
    
    // This is the transformer that takes the DDX and returns EML
    /*private Transformer transformer;*/

    // This provides a way to get catalogs, iterate over their child URLs and
    // access DDX urls to datasets in the catalog
    private ThreddsCatalogUtil tcc;
        
    DapIngest(boolean useThreddsCache, boolean useDDXCache, boolean useEMLCache)
    		throws SaxonApiException, Exception {
    	this(useThreddsCache, useDDXCache, useEMLCache, "");
    }
    
    DapIngest(boolean useThreddsCache, boolean useDDXCache, boolean useEMLCache,
    		String cacheNamePrefix)
    		throws SaxonApiException, Exception {

    	tcc = new ThreddsCatalogUtil(useThreddsCache, cacheNamePrefix);

    	catalogsVisited = 0;
    	DDXsVisited = 0;
    	/*date = new Date();*/
    	
    	try {
    		DDXSource = new DDXRetriever(/*log,*/ useDDXCache, useDDXCache, cacheNamePrefix);
    		
			/*transformer = new Transformer(ddx2emlPath);*/
			
    		EMLSource = new EMLRetriever(/*log,*/ useEMLCache, useEMLCache, cacheNamePrefix);
    		
    		/*
			if (useEMLCache)
				EMLCache = new ResponseCache(cacheNamePrefix + "EML", useEMLCache, useEMLCache);
			else
				EMLCache = null;
			*/

		} 
    	catch (SaxonApiException e) {
			log.debug("Transform returned an SaxonApiException: " + e.getLocalizedMessage());
			throw e;
		}
    	catch (Exception e) {
			log.debug("Exception: " + e.getLocalizedMessage());
			throw e;
		}
    }
 
	@SuppressWarnings("static-access")
    public static void main(String[] args) {
    	
    	DapIngest ingester;
    	
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		
		options.addOption("r", "read-cache", false, "Read ddxs from the cache");
		options.addOption("n", "no-cache", false, "Do not use caching");
		options.addOption("s", "save-cache", false, "Save the caches");
		options.addOption("v", "verbose", false, "Print information about the crawl");
		options.addOption("e", "eml", false, "Build EML from the DDX using XSLT");
		options.addOption("R", "read-eml-cache", false, "Don't get DDXs or process DDXs to make EML, just read EML from the cache.");
		
		options.addOption( OptionBuilder.withLongOpt( "cache-name" )
		                                .withDescription( "Use this to set a prefix for the cache name." )
		                                .hasArg()
		                                .withArgName("cacheName")
		                                .create("N") );
		
		options.addOption( OptionBuilder.withLongOpt( "catalog-root" )
		                                .withDescription( "Use this as the root catalog. Ignored when reading from the cache." )
		                                .hasArg()
		                                .withArgName("catalogRoot")
		                                .create() );
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    boolean useCache = line.hasOption( "r") || !line.hasOption( "n");
		    String cacheNamePrefix = line.getOptionValue("cache-name");
		    
		    ingester = new DapIngest(useCache, useCache, useCache, cacheNamePrefix);
		    
		    if (line.hasOption( "v"))
		    	ingester.verbose = true;
		    
		    boolean EmlGeneration = false;
		    if (line.hasOption( "e"))
		    	EmlGeneration = true;
		    
		    String catalogURL = line.getOptionValue("catalog-root");
		    if (ingester.verbose)
		    	System.out.println("Catalog Root: " + catalogURL);

		    if (line.hasOption("R"))
		    	ingester.printEMLFromCache(System.out);
		    else if (line.hasOption( "r"))
    			ingester.internEMLFromCachedDDXs(EmlGeneration);
    		else
    			ingester.crawlCatalog(catalogURL, EmlGeneration);

		    if (ingester.verbose)
    			ingester.recordStats();
        
    		if (line.hasOption( "s")) {
    			ingester.DDXSource.saveDDXCache();
    			ingester.tcc.saveCatalogCache();
    			ingester.EMLSource.saveEMLCache();
    		}
    	}
		catch (ParseException pe) {
    		System.err.print("Command line option parse error: " + pe.getMessage());
			
		}
    	catch (Exception e) {
    		System.err.print("Error: " + e.getMessage());
    	}
    }

    void recordStats() {
    	log.info("THREDDS Catalog URLs Visited: " + catalogsVisited);
    	Enumeration<String> e = tcc.getCachedCatalogURLs();
    	while (e.hasMoreElements()) {
    		String key = e.nextElement();
    		log.info(key + ": 1");
    	}
    	
    	log.info("DDX URLs Visited: " + DDXsVisited);
    	e = DDXSource.getCache().getLastVisitedKeys();
    	while (e.hasMoreElements()) {
    		String key = e.nextElement();
    		log.info(key + ": " + DDXSource.getCache().getLastVisited(key));
    	}
    }
    
    void crawlCatalog(String catalogURL, boolean EMLGeneration) throws Exception {
    	Enumeration<String> catalogs = tcc.getCatalogURLs(catalogURL);
    	
    	// First get references to any DDX objects at the top level
    	log.info("About to get DDX URLS from: " + catalogURL);
    	++catalogsVisited;
    	Vector<String> DDXURLs = tcc.getDDXUrls(catalogURL, false);
    	for (String DDXURL: DDXURLs) {
    		++DDXsVisited;
    		examineDDX(DDXURL, EMLGeneration);
    	}
    	
    	while (catalogs.hasMoreElements()) {
    		String catalog = catalogs.nextElement();
        	log.info("About to get DDX URLS from: " + catalog);
        	++catalogsVisited;
        	DDXURLs = tcc.getDDXUrls(catalog, false);
        	for (String DDXURL: DDXURLs) {
        		++DDXsVisited;
        		examineDDX(DDXURL, EMLGeneration);
        	}
    	}
    }

	void internEMLFromCachedDDXs(boolean EMLGeneration) throws Exception {
		// String emlString = null;
    	Enumeration<String> keys = DDXSource.getCache().getResponseKeys();
		while (keys.hasMoreElements()) {
			String DDXURL = (String) keys.nextElement();
			++DDXsVisited;
			examineDDX(DDXURL, EMLGeneration);
		}
	}	

	void printEMLFromCache(PrintStream ps) {
    	Enumeration<String> keys = EMLSource.getCache().getResponseKeys();
		while (keys.hasMoreElements()) {
			String DDXURL = (String) keys.nextElement();
			++DDXsVisited;
			String eml = EMLSource.getCache().getCachedResponse(DDXURL);
			
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
	
	/**
	 * For a given DDX URL, get the DDX and then, if that works get the EML. If
	 * that works, insert the EML into the data store. Note that the DDXSource
	 * object implements a simple HTTP/1.1 cache and an optional test for
	 * well-formed responses.
	 * 
	 * @param DDXURL
	 *            A DDX URL
	 */
	void examineDDX(String DDXURL, boolean EMLGeneration) throws Exception {

		String ddx = DDXSource.getDDXDoc(DDXURL);
		if (ddx == null) {
			throw new Exception("No DDX returned from: " + DDXURL);
		}

		if (EMLGeneration) {
			String eml = EMLSource.getEML(DDXURL, ddx);
			if (eml == null) {
				throw new Exception("No EML returned from: " + DDXURL);
			}
			/*
			if (useEMLCache != null) {
				if (verbose)
					log.info("Caching the EML doc: " + eml);

				EMLCache.setLastVisited(DDXURL, date.getTime());
				EMLCache.setCachedResponse(DDXURL, eml);
			}
			*/
			insertEML(eml);
		}
	}
    
    /** Given a DDX in a string, transform it to EML. This method is used with
     *  other methods that work with the DDX, such as the test for 
     *  wellformedness or the DDX caching.
     *  
     *  @param ddxString The DDX document to transform
     *  @returns The EML result, in a String
     *  @throws UnsupportedEncodingException 
     */
	/*
	String getEML(String ddxString) throws SaxonApiException, UnsupportedEncodingException {
		// Get the EML document
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XdmNode ddxXdm = null;
		try {
			ddxXdm = transformer.build(new StreamSource(new ByteArrayInputStream(ddxString.getBytes("UTF-8"))));
		}
		catch (SaxonApiException e) {
			log.error("Problem building XdmNode. Message: " + e.getMessage());
			throw e;
		} catch (UnsupportedEncodingException e) {
			log.error("Problem building XdmNode. Message: " + e.getMessage());
			throw e;
		}
		
		try {
			transformer.transform(ddxXdm, os);
		} 
		catch (SaxonApiException e) {
			log.error("Problem with XSLT transform. Message: " + e.getMessage());
			throw e;
		}

		return os.probeServletContext();
	}
    */
    

    /**
     *
     * @param emlString
     */
    void insertEML(String emlString) {
		// For now, just print it...
		//log.info("Built EML Document:\n" + emlString);
    }
}
