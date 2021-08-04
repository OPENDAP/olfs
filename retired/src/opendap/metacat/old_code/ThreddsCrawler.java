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

package opendap.metacat.old_code;

import java.io.PrintStream;
import java.util.Enumeration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import opendap.metacat.ThreddsCatalogUtil;

/**
 * This class is a simple thredds catalog crawler that uses an Enumeration
 * object returned by the ThreddsCatalogUtil() class. This crawler makes an
 * instance of TCU and configures it so that caching is turned on. It can 
 * crawl a set of catalogs starting at some root node and then re-play the
 * crawl using cached data.
 * 
 * @note The 're-play' option is cool but it's also a bit of a fraud because
 * the order of the catalog URLs is not the same as for the real crawl.
 * 
 * @author jimg
 *
 */
public class ThreddsCrawler {

    private ThreddsCatalogUtil tcc;
    
    private int catalogsVisited = 0;
    private boolean verbose = false;
    private String cacheNamePrefix = "";
     
	/**
	 * @param args
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		
		ThreddsCrawler crawler = new ThreddsCrawler();
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		
		options.addOption("r", "read-cache", false, "Read catalogs from the cache");
		options.addOption("n", "no-cache", false, "Do not cache catalogs");
		options.addOption("p", "print-catalogs", false, "Print the THREDDS catalogs");
		options.addOption("v", "verbose", false, "Verbose output");
		
		options.addOption( OptionBuilder.withLongOpt( "cache-name" )
                						.withDescription( "Use this to set a prefix for the cache name." )
                						.hasArg()
                						.withArgName("cacheName")
                						.create() );

		options.addOption( OptionBuilder.withLongOpt( "catalog-root" )
		                                .withDescription( "use this as the root catalog" )
		                                .hasArg()
		                                .withArgName("catalogRoot")
		                                .create() );
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    String catalogURL = line.getOptionValue("catalog-root");
		    System.out.println("Catalog Root: " + catalogURL);
		    
		    crawler.cacheNamePrefix = line.getOptionValue("cache-name");
		    System.out.println("Cache name: " + crawler.cacheNamePrefix);
		    
		    if (line.hasOption("v"))
		    	crawler.verbose = true;
		    
		    if (line.hasOption( "n")) {
		    	if (crawler.verbose)
		    		System.out.println("Caching is off");
		    	crawler.crawlCatalog(catalogURL, false, System.out);
		    	if (crawler.verbose)
		    		System.out.println("Found " +  new Integer(crawler.catalogsVisited).toString() + " catalogs.");
		    }
		    else if (line.hasOption( "r")) {
		    	if (crawler.verbose)
		    		System.out.println("Reading from cache, no network accesses");
		    	crawler.crawlCatalogCache(System.out, line.hasOption("p"));
		    	if (crawler.verbose)
		    		System.out.println("Read " + new Integer(crawler.catalogsVisited).toString() + " catalogs.");
		    }
		    else if (line.hasOption( "p")) {
		    	throw new Exception("The Print Catalog option (-p) can only be used when reading from the cache (-r).");
		    }
		    else {
		    	if (crawler.verbose)
		    		System.out.println("Caching is on");
		    	crawler.crawlCatalog(catalogURL, true, System.out);
		    	crawler.tcc.saveCatalogCache();
		    	if (crawler.verbose)
		    		System.out.println("Found " +  new Integer(crawler.catalogsVisited).toString() + " catalogs.");
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

	ThreddsCrawler() {
		catalogsVisited = 0;
	}
	
	public void crawlCatalog(String catalogURL, boolean useCache, PrintStream ps) throws Exception {		
		tcc = new ThreddsCatalogUtil(useCache, cacheNamePrefix, false);
		
		// The ThreddsCatalogUtil caches by default, so each catalog URL
		// is recorded (but not the catalog itself) every time nextElement()
		// is called.
    	Enumeration<String> catalogs = tcc.getCatalogEnumeration(catalogURL);

    	if (verbose)
    		ps.println("Root catalog: " + catalogURL);
    	++catalogsVisited;
    	
    	while (catalogs.hasMoreElements()) {
    		String childURL = catalogs.nextElement();
    		if (verbose)
    			ps.println("catalog: " + childURL);
        	++catalogsVisited;
    	}
    }
	
	public void crawlCatalogCache(PrintStream ps, boolean printCatalog) throws Exception {
		tcc = new ThreddsCatalogUtil(true, cacheNamePrefix, true);

		Enumeration<String> catalogs = tcc.getCachedCatalogEnumeration();
    	   	
    	while (catalogs.hasMoreElements()) {
    		String childURL = catalogs.nextElement();
    		ps.println("catalog: " + childURL);
    		if (printCatalog)
    			ps.println(tcc.getCachedCatalog(childURL));
        	++catalogsVisited;
    	}
    }
}
