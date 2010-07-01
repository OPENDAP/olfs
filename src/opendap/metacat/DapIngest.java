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
public class DapIngest {

    private static Logger log = LoggerFactory.getLogger(DapIngest.class);
    
    /// This is the prefix for all the document ids made up of DDX URLs
    final static String docidPrefix = "DDX.";
    
    /// If metacat needs an explicit schema for our generated EML, use this.
    final static String docidSchema = "/Users/jimg/src/eml-2.10/eml.xsd";
    
    private static PrintStream ps = System.out;
    
    /// Login credentials for metacat.
    private String metacatUsername = "";
    private String metacatPassword = "";
    
    private int catalogsVisited = 0;
    private int ddxsVisited = 0;
    private int emlVisited = 0;
        
    private boolean readThreddsFromCache = false;
    private boolean readDDXsFromCache = false;
    private boolean readEMLsFromCache = false;
    
    private boolean verbose = false;
    
    // Metacat (this might not get built)
    private Metacat metacat = null;
    
    // Caching DDX access (this and EML and Thredds always get built)
    private DDXRetriever ddxRetriever;
    
    // Caching EML access
    private EMLBuilder emlBuilder;
    
    // This provides a way to get catalogs, iterate over their child URLs and
    // access DDX urls to datasets in the catalog
    private ThreddsCatalogUtil threddsCatalogUtil;
    
    public static void main(String[] args) {
    	
    	DapIngest ingester = new DapIngest();
    	
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
				
		options.addOption("n", "cache-name", true, "Use this to set a prefix for the cache name.");
		options.addOption("N", "no-cache", false, "Don't cache whatever is being crawled, retrieved or built.\n"
				+ "Combined with --verbose, you can try something and see the result printed on stdout.");
		options.addOption("v", "verbose", false, "Print information about the crawl");
		options.addOption("h", "help", false, "Get help on this command");

		// These are the main options for controlling behavior.
		options.addOption("t", "thredds-crawl", true, "Crawl a collection of thredds catalogs starting at the catalog URL given.");
		options.addOption("T", "thredds-cache", false, "Read thedds catalogs from the cache.");
		options.addOption("d", "ddx-retrieve", false, "Use thredds catalogs to get ddx responses.");
		options.addOption("D", "ddx-cache", false, "Read ddx responses from the cache.");
		options.addOption("e", "eml-build", false, "Build EML from the DDX using XSLT");
		options.addOption("E", "eml-cache", false, "Read EML from the cache.");
		options.addOption("m", "metacat", true, "Stuff the EML into metacat; must include  URL to a metacat instance.");
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    // Extract all the stuff
		    String cacheNamePrefix = line.getOptionValue("cache-name", "");
		    boolean useCaching = !line.hasOption("no-cache");
		    
		    String catalogRoot = line.getOptionValue("thredds-crawl", "");
		    ingester.readThreddsFromCache = line.hasOption("thredds-cache");
		    
		    boolean ddxRetrieve = line.hasOption("ddx-retrieve");
		    ingester.readDDXsFromCache = line.hasOption("ddx-cache");
		    
		    boolean emlBuild = line.hasOption("eml-build");
		    ingester.readEMLsFromCache = line.hasOption("eml-cache");
		    
		    boolean emlInsert = false;
		    String metacatURL = line.getOptionValue("metacat", "");
		    
		    // Build our various objects as needed.
		    
		    // ThreddsCatalogUtil determines whther to read from the netowrk
		    // or the cache using its constructor. The other classes use 
		    // different methods to control reading from the cache.
	    	ingester.threddsCatalogUtil = new ThreddsCatalogUtil(useCaching, cacheNamePrefix, ingester.readThreddsFromCache);
		    
    		ingester.ddxRetriever = new DDXRetriever(useCaching, cacheNamePrefix);
			
    		ingester.emlBuilder = new EMLBuilder(useCaching, cacheNamePrefix);

        	if (metacatURL != "") {
        		log.debug("Building metacat cache connection.");
    			try {
    				log.debug("Test Metacat: " + metacatURL);
    				ingester.metacat = MetacatFactory.createMetacatConnection(metacatURL);

                    log.debug("username: " + ingester.metacatUsername + ", password: " + ingester.metacatPassword);
                    String response = ingester.metacat.login(ingester.metacatUsername, ingester.metacatPassword);
                    log.debug("login(): response=" + response);
                    log.debug("login(): Session ID=" + ingester.metacat.getSessionId());
                    
                    emlInsert = true;
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
        	
		    if (ingester.verbose) {
		    	System.out.println("Catalog Root: " + catalogRoot);
		    	System.out.println("Cache name: " + cacheNamePrefix);
		    	if (metacatURL != "")
			    	System.out.println("Metacat URL: " + metacatURL);
		    }
		    
		    if (line.hasOption("E"))
		    	ingester.printEMLFromCache(System.out);
		    else if (line.hasOption( "r"))
    			ingester.buildEMLFromCachedDDXs(emlGeneration, emlInsert);
    		else
    			ingester.crawlCatalog(catalogURL, emlGeneration, emlInsert);

		    if (ingester.verbose)
    			ingester.recordStats();
        
			ingester.threddsCatalogUtil.saveCatalogCache();
			ingester.ddxRetriever.saveDDXCache();
			ingester.emlBuilder.saveEMLCache();
    	}
		catch (ParseException pe) {
    		System.err.print("Command line option parse error: " + pe.getMessage());
			
		}
    	catch (Exception e) {
    		System.err.print("Error: " + e.getMessage());
    		e.printStackTrace(System.err);
    	}
    }

    private void recordStats() {
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
    
	public void crawlTHREDDS(String catalogRoot, boolean ddxRetrieve, boolean emlBuild, boolean emlInsert) throws Exception {
		// Unlike the DDX and EML classes, ThreddsCatalogUtil determines
		// whether to read from the network or the cache using a parameter 
		// passed to its constructor. The other classes use special methods to
		// read from the cache.
		Enumeration<String> catalogs = threddsCatalogUtil.getCatalogEnumeration(catalogRoot);

		// First get references to any DDX objects at the top level
		log.debug("About to get DDX URLS from: " + catalogRoot);
		if (verbose)
			ps.println("Root catalog: " + catalogRoot);

		++catalogsVisited;

		Vector<String> DDXURLs = null;

		if (ddxRetrieve) {
			DDXURLs = threddsCatalogUtil.getDDXUrls(catalogRoot);
			for (String DDXURL : DDXURLs) {
				++ddxsVisited;
				retrieveDDX(DDXURL, emlBuild, emlInsert);
			}
		}

		while (catalogs.hasMoreElements()) {
			String catalog = catalogs.nextElement();
			log.debug("About to get DDX URLS from: " + catalog);
			++catalogsVisited;
			if (verbose)
				ps.println("catalog: " + catalog);
			if (ddxRetrieve) {
				DDXURLs = threddsCatalogUtil.getDDXUrls(catalog);
				for (String DDXURL : DDXURLs) {
					++ddxsVisited;
					retrieveDDX(DDXURL, emlBuild, emlInsert);
				}
			}
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
	void retrieveDDX(String ddxUrl, boolean emlBuild, boolean emlInsert) throws Exception {
		String ddx = "";
		if (readDDXsFromCache)
			ddx = ddxRetriever.getCachedDDXDoc(ddxUrl);
		else
			ddx = ddxRetriever.getDDXDoc(ddxUrl);
		
		if (ddx == null)
			throw new Exception("No DDX for: " + ddxUrl);

		if (emlBuild) {
			String eml = "";
			if (readEMLsFromCache)
				eml = emlBuilder.getCachedEMLDoc(ddxUrl);
			else
				eml = emlBuilder.getEML(ddxUrl, ddx);
			
			if (eml == null)
				throw new Exception("No EML for: " + ddxUrl);

			if (emlInsert)
				insertEML(ddxUrl, eml);
		}
	}

    /**
     * To use a URL as a DocID in metacat, any dots (.) in the URL must be
     * escaped. This function repalces dots with %2e
     * @param URL
     * @return The esacped URL
     */
    private String escapedURL(String URL) {
    	return URL.replaceAll("\\.", "%2e");
    }
    
    /**
     * Given a URL, return the corresponding document id. A metacat docuement 
     * id must be of the form <string>.<string>.<digit> Where the dots are 
     * literal and <string> must not contain any dots. Furthermore, only the
     * last two parts are used when accessing the document id; the first 
     * <string> is ignored. This method returns a document id by combining the 
     * value of the class' docidPrefix with an escaped URL and a '1'. 
     * @param url
     * @return A docuement id string suitable for use with metacat
     */
    private String getDocid(String url) {
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
