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
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import opendap.metacat.ThreddsCatalogUtil;
import opendap.metacat.ThreddsCatalogUtil.ThreddsCrawlerEnumeration;

/**
 * This class is a simple DDX crawler that uses an Enumeration
 * object returned by the ThreddsCatalogUtil() class. This crawler makes an
 * instance of TCU and configures it so that caching is turned on. It can 
 * crawl a set of catalogs, either by reading them from the network or from
 * the cache employed by TCU, and for each catalog, retrieve the DDX responses
 * for the DAP data sources they reference.
 * 
 * Adapted from the ThreddsCrawler class.
 * 
 * @note The 're-play' option is cool but it's also a bit of a fraud because
 * the order of the catalog URLs is not the same as for the real crawl.
 * 
 * @author jimg
 *
 */
public class DDXCrawler {

    private static Logger log = LoggerFactory.getLogger(DDXCrawler.class);

    private ThreddsCatalogUtil threddsCatalogUtil = null;
    
    private DDXRetriever ddxRetriever = null;

    private int catalogsVisited = 0;
    private int DDXsVisited = 0;
    
    private boolean verbose = false;
    private boolean printDDX = false;
    private boolean fetchDDX = false;
    
    private String cacheNamePrefix = "";
    	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * This is how one might handler ctrl-c and similar stuff; I have yet 
		 * to work out the details, however.
		Runtime.getRuntime().addShutdownHook(new Thread() {
		      @Override
		      public void run() {
		    	  log.debug("In custom shutdown handler");
					crawler.threddsCatalogUtil.saveCatalogCache();
					crawler.ddxRetriever.saveDDXCache();
		      }
		    });
		*/
		DDXCrawler crawler = new DDXCrawler();
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		
		options.addOption("t", "read-from-thredds-cache", false, "Use only cached THREDDS catalogs; do not read from the network.");
		options.addOption("f", "fetch-ddx", false, "Fetch ddx responses");
		// options.addOption("T", "dont-cache-thredds", false, "Do not cache THREDDS responses");
		// options.addOption("d", "dont-cache-ddx", false, "Do not cache DDX responses");
		options.addOption("p", "print-ddx", false, "Print the DDX responses");
		options.addOption("v", "verbose", false, "Verbose output");
		options.addOption("R", "restore", false, "Restore the crawl from a saved state file");
		options.addOption("h", "help", false, "Print online help");
		
		options.addOption("n", "cache-name", true, "Use this to set a prefix for the cache name");
		options.addOption("r", "catalog-root", true, "Use this as the root catalog");
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

			if (line.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("ddx_crawler [options] --catalog-root <catalog.xml url>", options);
				return;
			}
		    
			if (!line.hasOption("catalog-root"))
				throw new Exception("Must provide catalog-root (-r).");
			
		    String catalogURL = line.getOptionValue("catalog-root");
		    System.out.println("Catalog Root: " + catalogURL);
		    
		    if (line.hasOption("cache-name")) {
		    	crawler.cacheNamePrefix = line.getOptionValue("cache-name");
		    }
		    else {
		    	int start = catalogURL.indexOf("//") + 2;
		    	crawler.cacheNamePrefix = catalogURL.substring(start, catalogURL.indexOf('/', start));
		    }
		    
		    System.out.println("Cache name: " + crawler.cacheNamePrefix);
		    
		    crawler.verbose = line.hasOption("verbose");
		    crawler.printDDX = line.hasOption("print-ddx");
		    crawler.fetchDDX = line.hasOption("fetch-ddx");
		    
		    // By default this code caches the THREDDS and DDX URLs and 
		    // documents (if the latter are being accessed).
		    //boolean writeThreddsCache = true; // !line.hasOption("dont-cache-thredds");;
		    //boolean useDDXCache = true; // !line.hasOption("dont-cache-ddx");
		    boolean readFromThreddsCache = line.hasOption("read-from-thredds-cache");
		    boolean restoreState = line.hasOption("restore");
		    
		    // If reading from the thredds cache; don't also write to it!
		    //if (readFromThreddsCache)
		    	//writeThreddsCache = false;
		    
		    if (crawler.verbose) {
		    	//System.out.println("writeThreddsCache: " + writeThreddsCache);
		    	//System.out.println("useDDXCache: " + useDDXCache);
		    	System.out.println("readFromThreddsCache: " + readFromThreddsCache);
		    	System.out.println("restoreState: " + restoreState);
		    }
		    
			// In this program, the ThreddsCatalogUtils _always_ writes to the cache
			// when it reads a new document. The readFromThreddsCache determines
			// if a cached catalog is preferred over a network read.
		    crawler.threddsCatalogUtil = new ThreddsCatalogUtil(crawler.cacheNamePrefix, readFromThreddsCache);
		    crawler.ddxRetriever = new DDXRetriever(false, crawler.cacheNamePrefix);

			crawler.crawlCatalog(catalogURL, System.out, restoreState);
			
			if (crawler.verbose) {
				System.out.println("Found " + new Integer(crawler.catalogsVisited).toString() + " catalogs,");
				System.out.println("... and " + new Integer(crawler.DDXsVisited).toString() + " DDX responses.");
			}
		}
		catch( ParseException exp ) {
		    System.err.println( "Unexpected exception:" + exp.getMessage() );
		}
		catch (Exception e) {
			System.err.println("Error : " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			try {
				log.debug("In DDXCrawler.main finally, saving cache files");
				if (crawler != null) {
					crawler.threddsCatalogUtil.saveCatalogCache();
					crawler.ddxRetriever.saveDDXCache();
				}
			}
			catch (Exception e) {
				System.err.println("Error saving cache files: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	public void crawlCatalog(String catalogURL, PrintStream ps, boolean restoreState) throws Exception {

		ThreddsCrawlerEnumeration catalogs = null;
		
		// If restoreState is false, start a new crawl from the catalogURL
		// but if it's true, initialize the TCU using the saved state (see
		// 'else' below).
		try {
			if (!restoreState)
				catalogs = (ThreddsCrawlerEnumeration) threddsCatalogUtil.getCatalogEnumeration(catalogURL);
			else
				catalogs = (ThreddsCrawlerEnumeration) threddsCatalogUtil.getCatalogEnumeration();

			while (catalogs.hasMoreElements()) {
				String childURL = catalogs.nextElement();
				
				crawlOneCatalogUrl(ps, childURL);
			}
		}
		catch (Exception e) {
			log.debug("Error (saving crawl state): " + e.getLocalizedMessage());
			//catalogs.saveState();
			throw new Exception(e);
		}
		finally {
			catalogs.saveState();
		}
		/*finally {
			threddsCatalogUtil.saveCatalogCache();
			ddxRetriever.saveDDXCache();
		}*/
	}

	private void crawlOneCatalogUrl(PrintStream ps, String catalogURL) throws Exception {
		log.debug("About to get DDX URLS from: " + catalogURL);
		++catalogsVisited;
		if (verbose)
			ps.println("catalog: " + catalogURL);

		Vector<String> DDXURLs = threddsCatalogUtil.getDDXUrls(catalogURL);
		for (String DDXURL : DDXURLs) {
			++DDXsVisited;
			if (fetchDDX) {
				String ddx = ddxRetriever.getDDXDoc(DDXURL);
				if (ddx == null)
					log.error("No DDX returned from: " + DDXURL);
				else if (verbose) {
					ps.println("DDX: " + DDXURL);
					if (printDDX)
						ps.println(ddx);
				}
			}
			else {
				// Just save the URL without the expensive DDX retrieval
				// Set the LMT to zero so that it is cached in the URL cache
				// but if we need the document it will be retrieved without
				// first checking the DB.
				ddxRetriever.cacheDDXURL(DDXURL);
				if (verbose)
					ps.println("DDX: " + DDXURL);
			}
		}
	}
}
