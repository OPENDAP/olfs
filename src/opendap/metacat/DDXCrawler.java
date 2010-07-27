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
//import java.util.Enumeration;
import java.util.Vector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
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
		
		DDXCrawler crawler = new DDXCrawler();
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		
		options.addOption("t", "use-thredds-cache", false, "Use cached thredds catalogs");
		options.addOption("f", "fetch-ddx", false, "Fetch ddx responses");
		options.addOption("d", "cache-ddx", false, "Cache ddx responses");
		options.addOption("p", "print-ddx", false, "Print the DDX responses");
		options.addOption("v", "verbose", false, "Verbose output");
		options.addOption("R", "restore", false, "Restore the crawl from a saved state file");
		
		options.addOption("n", "cache-name", true, "Use this to set a prefix for the cache name");
		options.addOption("r", "catalog-root", true, "Use this as the root catalog");
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    String catalogURL = line.getOptionValue("catalog-root");
		    System.out.println("Catalog Root: " + catalogURL);
		    
		    crawler.cacheNamePrefix = line.getOptionValue("cache-name");
		    System.out.println("Cache name: " + crawler.cacheNamePrefix);
		    
		    crawler.verbose = line.hasOption("verbose");
		    crawler.printDDX = line.hasOption("print-ddx");
		    crawler.fetchDDX = line.hasOption("fetch-ddx");
		    
		    // The sense of these caching options is odd because they are to
		    // not use caching and are false (i.e., caching is on by default.
		    boolean useDDXCache = line.hasOption("cache-ddx");
		    boolean readFromThreddsCache = line.hasOption("use-thredds-cache");
		    boolean restoreState = line.hasOption("restore");
		    
			// In this program, the ThreddsCatalogUtils _always_ writes to the cache
			// when it reads a new document. The readFromThreddsCache determines
			// if a cached catalog is preferred over a network read.
		    crawler.threddsCatalogUtil = new ThreddsCatalogUtil(true, crawler.cacheNamePrefix, readFromThreddsCache);
		    crawler.ddxRetriever = new DDXRetriever(useDDXCache, crawler.cacheNamePrefix);

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
	}

	public void crawlCatalog(String catalogURL, PrintStream ps, boolean restoreState) throws Exception {

		ThreddsCrawlerEnumeration catalogs = null;
		
		try {
			if (!restoreState)
				catalogs = (ThreddsCrawlerEnumeration) threddsCatalogUtil.getCatalogEnumeration(catalogURL);
			else 
				catalogs = (ThreddsCrawlerEnumeration) threddsCatalogUtil.getCatalogEnumeration();
			
			Vector<String> DDXURLs = null;

			if (!restoreState) {
				log.debug("About to get DDX URLS from: [" + catalogURL + "]");
				++catalogsVisited;
				if (verbose)
					ps.println("Root catalog: " + catalogURL);

				// First get references to any DDX objects at the top level
				DDXURLs = threddsCatalogUtil.getDDXUrls(catalogURL);
				for (String DDXURL : DDXURLs) {
					++DDXsVisited;
					if (fetchDDX) {
						String ddx = ddxRetriever.getDDXDoc(DDXURL);
						if (ddx == null)
							log.error("No DDX returned from: " + DDXURL);
						else if (verbose) {
							ps.println("Top URL: " + DDXURL);
							if (printDDX)
								ps.println("DDX: " + ddx);
						}
					}
					else {
						// Just save the URL without the expensive DDX retrieval
						// Set the LMT to zero so 
						ddxRetriever.getCache().setLastVisited(DDXURL, 0);
						if (verbose)
							ps.println("Top URL: " + DDXURL);
					}
				}
			}

			while (catalogs.hasMoreElements()) {
				String childURL = catalogs.nextElement();
				log.debug("About to get DDX URLS from: " + childURL);
				++catalogsVisited;
				if (verbose)
					ps.println("catalog: " + childURL);

				DDXURLs = threddsCatalogUtil.getDDXUrls(childURL);
				for (String DDXURL : DDXURLs) {
					++DDXsVisited;
					if (fetchDDX) {
						String ddx = ddxRetriever.getDDXDoc(DDXURL);
						if (ddx == null)
							log.error("No DDX returned from: " + DDXURL);
						else if (verbose) {
							ps.println("URL: " + DDXURL);
							if (printDDX)
								ps.println("DDX: " + ddx);
							/*
							 * if(DDXURL.equals(
							 * "http://test.opendap.org:8080/opendap/hyrax/data/oaflux/daily/lhsh_oaflux_1981.nc.ddx"
							 * ) && !restoreState) throw new
							 * Exception("simulated error!");
							 */
						}
					}
					else {
						// Just save the URL without the expensive DDX retrieval
						// Set the LMT to zero so
						ddxRetriever.getCache().setLastVisited(DDXURL, 0);
						if (verbose)
							ps.println("Top URL: " + DDXURL);
					}
				}
			}
		}
		catch (Exception e) {
			log.debug("Error: " + e.getLocalizedMessage());
			catalogs.saveState();
			throw new Exception(e);
		}
		finally {
			threddsCatalogUtil.saveCatalogCache();
			ddxRetriever.saveDDXCache();
		}
	}
}
