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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Vector;
import java.util.Enumeration;

import net.sf.saxon.s9api.SaxonApiException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import edu.ucsb.nceas.metacat.client.InsufficientKarmaException;
import edu.ucsb.nceas.metacat.client.Metacat;
import edu.ucsb.nceas.metacat.client.MetacatAuthException;
import edu.ucsb.nceas.metacat.client.MetacatException;
import edu.ucsb.nceas.metacat.client.MetacatFactory;
import edu.ucsb.nceas.metacat.client.MetacatInaccessibleException;

import opendap.metacat.DDXRetriever;
import opendap.metacat.EMLBuilder;
import opendap.metacat.ThreddsCatalogUtil;
import opendap.metacat.ThreddsCatalogUtil.ThreddsCrawlerEnumeration;

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
    final static String docidScope = "opendap";
    
    /// If metacat needs an explicit schema for our generated EML, use this.
    final static String docidSchema = "/Users/jimg/src/eml-2.10/eml.xsd";
    
    final static PrintStream ps = System.out;
    final static PrintStream err = System.err;
    
    /// Login credentials for metacat.
    private String metacatUsername = "uid=jimg,o=unaffiliated,dc=ecoinformatics,dc=org";
    private String metacatPassword = "p7th0n";
    
    private int catalogsVisited = 0;
    private int ddxsVisited = 0;
    private int emlVisited = 0;
    // Increment once for each insert
    private Integer metacatId = 1;
    // This should change rarely
    private String metacatRevision = "2";
        
    private boolean readThreddsFromCache = false;
    private boolean readDDXsFromCache = false;
    private boolean readEMLsFromCache = false;
    
    private boolean verbose = false;
    private boolean veryVerbose = false;
    
    // Metacat (this might not get built)
    private Metacat metacat = null;
    
    // Caching DDX access (this and EML and Thredds always get built)
    private DDXRetriever ddxRetriever = null;
    
    // Caching EML access
    private EMLBuilder emlBuilder = null;
    
    // This provides a way to get catalogs, iterate over their child URLs and
    // access DDX urls to datasets in the catalog
    private ThreddsCatalogUtil threddsCatalogUtil = null;
    
    public static void main(String[] args) {
    	
    	DapIngest ingester = new DapIngest();
    	
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
				
		options.addOption("n", "cache-name", true, "Use this to set a prefix for the cache name.");
		
		options.addOption("S", "no-save-cache", false, "Use the cache, but don't save it to disk");
		options.addOption("N", "no-cache", false, "Don't cache whatever is being crawled, retrieved or built. Combined with --verbose, you can try something and see the result printed on stdout.");
		options.addOption("v", "verbose", false, "Print information about the crawl");
		options.addOption("V", "very-verbose", false, "Print resultant documents from the crawl (DDX and EML) in addition to whatever --verbose does.");
		options.addOption("h", "help", false, "Get help on this command");

		// These are the main options for controlling behavior.
		options.addOption("t", "thredds-crawl", true, "Crawl a collection of thredds catalogs starting at the catalog URL given.");
		options.addOption("T", "thredds-cache", true, "Read thedds catalogs from the cache.");
		options.addOption("r", "restart-crawl", false, "Restart a crawl");
		
		options.addOption("d", "ddx-retrieve", false, "Use thredds catalogs to get ddx responses.");
		options.addOption("D", "ddx-cache", false, "Read ddx responses from the cache.");
		
		options.addOption("e", "eml-build", false, "Build EML from the DDX using XSLT");
		options.addOption("E", "eml-cache", false, "Read EML from the cache.");
		options.addOption("P", "eml-print-all-cached", false, "Just look at the EML cache and print the URLs. Use --very-verbose to print the documents, too. Ignore thredds and ddx options; without --cache-name this option returns all of the EML in the postgres cache");
		options.addOption("I", "eml-insert-all-cached", false, "Just look at the EML cache and insert everything into metacat. Ignore thredds and ddx options; without --cache-name this option inserts all of the EML in the postgres cache.");
		options.addOption("F", "eml-from-file", true, "Read an eml document from the named file and insert into metacat");
		
		options.addOption("m", "metacat", true, "Stuff the EML into metacat; must include  URL to a metacat instance.");
		
		boolean saveCache = true;
		
		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );

		    // If --help is given, print the help and exit.
		    if (line.hasOption("help")) {
		    	HelpFormatter formatter = new HelpFormatter();
		    	formatter.printHelp( "dap_ingest [options]", options );
		    	return;
		    }
		    	
		    // Extract all the options stuff here.
		    String cacheNamePrefix = line.getOptionValue("cache-name", "");
		    boolean useCaching = !line.hasOption("no-cache");
		    saveCache = !line.hasOption("no-save-cache");
		    
		    ingester.verbose = line.hasOption("verbose");
		    ingester.veryVerbose = line.hasOption("very-verbose");
		    if (ingester.veryVerbose)
		    	ingester.verbose = true;
		    
		    String catalogRoot = line.getOptionValue("thredds-crawl", "");
		    ingester.readThreddsFromCache = line.hasOption("thredds-cache");
		    if (ingester.readThreddsFromCache)
		    	catalogRoot = line.getOptionValue("thredds-cache", "");
		    boolean restart = line.hasOption("restart-crawl");
		    
		    boolean ddxRetrieve = line.hasOption("ddx-retrieve") || line.hasOption("ddx-cache");
		    ingester.readDDXsFromCache = line.hasOption("ddx-cache");
		    
		    boolean emlBuild = line.hasOption("eml-build") || line.hasOption("eml-cache");
		    ingester.readEMLsFromCache = line.hasOption("eml-cache");
		    
		    boolean emlInsert = false;
		    String metacatURL = line.getOptionValue("metacat", "");
		    if (metacatURL != "")
		    	emlInsert = true;
		    boolean eml_print_all_cached = line.hasOption("eml-print-all-cached");
		    boolean eml_insert_all_cached = line.hasOption("eml-insert-all-cached");
		    boolean emlFromFile = line.hasOption("eml-from-file");
		    String emlFilename = line.getOptionValue("eml-from-file", "");    	
		    
		    // Build our various objects as needed. I could optimize this so
		    // that objects are built only if needed, but that makes it harder
		    // to sort out different parts.
		    
		    // ThreddsCatalogUtil determines whether to read from the network
		    // or the cache using its constructor. The other classes use 
		    // different methods to control reading from the cache.
	    	ingester.threddsCatalogUtil = new ThreddsCatalogUtil(useCaching, cacheNamePrefix, ingester.readThreddsFromCache);
		    
	    	// For most operations we will need these.
			try {
				ingester.ddxRetriever = new DDXRetriever(useCaching, cacheNamePrefix);

				ingester.emlBuilder = new EMLBuilder(useCaching, cacheNamePrefix);
			}
			catch (SaxonApiException e) {
				log.debug("Transform returned an SaxonApiException: " + e.getLocalizedMessage());
				throw e;
			}
			catch (Exception e) {
				log.debug("Exception: " + e.getLocalizedMessage());
				throw e;
			}
	    	
			// Since building the metacat object means connecting to metacat,
			// only do this when its needed.
        	if (emlInsert) {
        		log.debug("Building metacat cache connection.");
    			try {
    				log.debug("Test Metacat: " + metacatURL);
    				ingester.metacat = MetacatFactory.createMetacatConnection(metacatURL);

                    log.debug("username: " + ingester.metacatUsername + ", password: " + ingester.metacatPassword);
                    
                    String response = ingester.metacat.login(ingester.metacatUsername, ingester.metacatPassword);
                    
                    log.debug("login(): response=" + response);
                    log.debug("login(): Session ID=" + ingester.metacat.getSessionId());
                    
                    String id = ingester.metacat.getLastDocid(docidScope);
                    log.debug("getLastDocid(): ID=" + id);
                    
                    // if not null, use the value, else use the default of 1
                    if (!isEmpty(id)) {
                    	// TODO Parse id, get number, load into metacatId
                    	++ingester.metacatId;
                    }

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
        	
        	// Now do whatever we've been told to do...
        		
		    if (ingester.veryVerbose) {
		        // print internal logger state
		        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		        StatusPrinter.print(lc);

		        // 
		    	System.out.println("Catalog Root: " + catalogRoot);
		    	System.out.println("Cache name: " + cacheNamePrefix);
		    	if (metacatURL != "")
			    	System.out.println("Metacat URL: " + metacatURL);
		    }
        	
		    if (emlFromFile)
		    	ingester.insertEMLFromLocalFile(emlFilename);
		    else if (eml_print_all_cached)
		    	ingester.printEMLFromCache(cacheNamePrefix);
		    else if (eml_insert_all_cached)
		    	ingester.insertEMLFromCache(cacheNamePrefix);
		    else
		    	ingester.crawlTHREDDS(catalogRoot, restart, ddxRetrieve,  emlBuild,  emlInsert);
		    
		    if (ingester.verbose)
    			ingester.recordStats();
     	}
		catch (ParseException pe) {
    		err.println("Command line option parse error: " + pe.getMessage());
			
		}
    	catch (Exception e) {
    		err.println("Error: " + e.getMessage());
    		e.printStackTrace(err);
    	}
		finally {
			try {
				// Might add code to save the sate of a crawl here and add an
				// option that enables the restart feature
				if (saveCache) {
					if (ingester.threddsCatalogUtil != null)
						ingester.threddsCatalogUtil.saveCatalogCache();
					if (ingester.ddxRetriever != null)
						ingester.ddxRetriever.saveDDXCache();
					if (ingester.emlBuilder != null)
						ingester.emlBuilder.saveEMLCache();
				}
			}
			catch (Exception e) {
				err.println("Error saving cache state!");
				e.printStackTrace(err);
			}
    	}
    }
    
    private static boolean isEmpty(String s) {
    	return (s == null || s.length() == 0 || s.equals("null"));
    }

    private void recordStats() {
    	ps.println("THREDDS Catalog URLs Visited: " + catalogsVisited);
    	log.info("THREDDS Catalog URLs Visited: " + catalogsVisited);
    	Enumeration<String> e = threddsCatalogUtil.getCachedCatalogEnumeration();
    	while (e.hasMoreElements()) {
    		String key = e.nextElement();
    		log.info(key + ": 1");
    	}
    	
    	ps.println("DDX URLs Visited: " + ddxsVisited);
    	log.info("DDX URLs Visited: " + ddxsVisited);
    	e = ddxRetriever.getCache().getLastVisitedKeys();
    	while (e.hasMoreElements()) {
    		String key = e.nextElement();
    		log.info(key + ": " + ddxRetriever.getCache().getLastVisited(key));
    	}
    	
    	ps.println("EML Built for DDXs: " + emlVisited);
    	log.info("EML Built for DDXs: " + emlVisited);
    	e = emlBuilder.getCache().getLastVisitedKeys();
    	while (e.hasMoreElements()) {
    		String key = e.nextElement();
    		log.info(key + ": " + emlBuilder.getCache().getLastVisited(key));
    	}

    }
    
	private void crawlTHREDDS(String catalogRoot, boolean restart, 
			boolean ddxRetrieve, boolean emlBuild, boolean emlInsert) throws Exception {
		// Unlike the DDX and EML classes, ThreddsCatalogUtil determines
		// whether to read from the network or the cache using a parameter 
		// passed to its constructor. The other classes use special methods to
		// read from the cache.
		ThreddsCrawlerEnumeration catalogs = null;
		try {
			if (restart)
				catalogs = threddsCatalogUtil.getCatalogEnumeration();
			else
				catalogs = threddsCatalogUtil.getCatalogEnumeration(catalogRoot);

			// First get references to any DDX objects at the top level
			log.debug("About to get DDX URLS from: " + catalogRoot);

			++catalogsVisited;

			Vector<String> DDXURLs = null;

			if (ddxRetrieve) {
				DDXURLs = threddsCatalogUtil.getDDXUrls(catalogRoot);
				for (String DDXURL : DDXURLs) {
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
						retrieveDDX(DDXURL, emlBuild, emlInsert);
					}
				}
			}
		}
		catch (Exception e) {
			err.println("Error: " + e.getMessage());
			log.debug("Error: " + e.getMessage());
		}
		finally {
			// If we instantiated a ThreddsCrawlerEnumeration, then no matter
			// how this code exits, save the crawler's stack in case of a
			// restart.
			if (catalogs != null)
				catalogs.saveState();
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
	private void retrieveDDX(String ddxUrl, boolean emlBuild, boolean emlInsert) throws Exception {
		String ddx = "";
		if (readDDXsFromCache)
			ddx = ddxRetriever.getCachedDDXDoc(ddxUrl);
		else
			ddx = ddxRetriever.getDDXDoc(ddxUrl);
		
		if (ddx == null)
			throw new Exception("No DDX for [" + ddxUrl +"]");
		
		++ddxsVisited;
		
		if (verbose)
			ps.println("DDX: " + ddxUrl);
		if (veryVerbose)
			ps.println("DDX Document:\n" + ddx);
		
		if (emlBuild) {
			String eml = "";
			if (readEMLsFromCache)
				eml = emlBuilder.getCachedEMLDoc(ddxUrl);
			else
				eml = emlBuilder.getEML(ddxUrl, ddx);
			
			if (eml == null)
				throw new Exception("No EML for DDX [" + ddxUrl + "]");

			++ emlVisited;
			
			if (veryVerbose)
				ps.println("EML Document:\n" + eml);

			if (emlInsert)
				insertEML(ddxUrl, eml);
		}
	}
    
    /**
     * Given a URL, return the corresponding document id. A metacat document 
     * id must be of the form <string>.<string>.<digit> Where the dots are 
     * literal and <string> must not contain any dots. Furthermore, only the
     * last two parts are used when accessing the document id; the first 
     * <string> is ignored. This method returns a document id by combining the 
     * value of the class' docidScope with an escaped URL and a '1'. 
     * 
     * @todo Fix this comment
     * @return A document id string suitable for use with metacat
     */
    private String getDocid() {
    	return docidScope + "." + metacatId.toString() + "." + metacatRevision;
    }
    
    /**
     * Insert the EML document into Metacat. This assumes that the EML document
     * is indexed using a DDX URL. 
     * 
     * @param emlString
     */
	private void insertEML(String ddxUrl, String emlString) throws Exception {

		String docid = getDocid();
		log.debug("Storing " + ddxUrl + "(docid:" + docid + ") in metacat.");

		try {
			// FileReader schema = new FileReader(docidSchema);
			// metacat.insert(String docid, Reader xmlDocument, Reader schema);
			++metacatId;
			String response = metacat.insert(docid, new StringReader(emlString), null);
			
			if (verbose)
				ps.println("Metacat's response: " + response);
		}
		catch (FileNotFoundException e) {
			err.println("Could not open the file: " + docidSchema);
			log.error("Could not open the file: " + docidSchema);
			throw new Exception("FileNotFoundException: " + e.getMessage());
		}
		catch (InsufficientKarmaException e) {
			err.println("Error storing the response: Insufficent rights for the operation: " + e.getMessage());
			log.error("Error storing the response: Insufficent rights for the operation: " + e.getMessage());
			throw new Exception("InsufficientKarmaException: " + e.getMessage());
		}
		catch (MetacatInaccessibleException e) {
			err.println("Error storing the response: Error reading the xml document: " + e.getMessage());
			log.error("Error storing the response: Error reading the xml document: " + e.getMessage());
			throw new Exception("MetacatInaccessibleException: " + e.getMessage());
		}
		catch (MetacatException e) {
			err.println("Error storing the response [" + ddxUrl + "]: " + e.getMessage());
			log.error("Error storing the response [" + ddxUrl + "]: " + e.getMessage());
			log.error("The EML document: [" + emlString + "]");
		}
		catch (IOException e) {
			err.println("Error storing the response: Unknown error [" + ddxUrl + "]: " + e.getMessage());
			log.error("Error storing the response: Unknown error [" + ddxUrl + "]: " + e.getMessage());
		}
	}
	
	private void printEMLFromCache(String cacheNamePrefix) throws Exception {
		Enumeration<String> keys;
		if (cacheNamePrefix != "")
			keys = emlBuilder.getCache().getLastVisitedKeys();
		else 
			keys = emlBuilder.getCache().getResponseKeys();
		
		while (keys.hasMoreElements()) {
			String DDXURL = (String) keys.nextElement();
			++emlVisited;
			
			ps.println("DDX: " + DDXURL);

			if (veryVerbose) {
				String eml = emlBuilder.getCache().getCachedResponse(DDXURL);
				ps.println("EML:");
				ps.println(eml);
			}
		}		
	}

	private static String trim_space(String arg) {
		if (isEmpty(arg))
			return arg;
		else if (arg.charAt(0) == ' ')
			return arg.substring(1);
		else
			return arg;
	}

	private void insertEMLFromCache(String cacheNamePrefix) throws Exception {
		Enumeration<String> keys;
		if (cacheNamePrefix != "")
			keys = emlBuilder.getCache().getLastVisitedKeys();
		else 
			keys = emlBuilder.getCache().getResponseKeys();
		
		while (keys.hasMoreElements()) {
			String ddxUrl = trim_space((String) keys.nextElement());
			
			ps.println("DDX: [" + ddxUrl + "]");
			
			++emlVisited;
			String eml = emlBuilder.getCache().getCachedResponse(ddxUrl);
			
			if (eml != null) {
				if (veryVerbose)
					ps.println("EML: " + eml);
				
				insertEML(ddxUrl, eml);
			}
			else {
				if (verbose)
					ps.println("EML: <null>");
			}	
		}
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

	private void insertEMLFromLocalFile(String filename) throws Exception {

		try {
			// Open the file
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);

			String eml = convertStreamToString(in);
			ps.println("Filename: " + filename);

			if (eml != null) {
				if (veryVerbose)
					ps.println("EML: " + eml);

				insertEML(filename, eml);
			}
			else {
				if (veryVerbose)
					ps.println("EML: <null>");
			}
		}
		catch (IOException e) {
			err.println("Error: Could not open the file: " + e.getMessage());
			e.printStackTrace(err);
		}
	}
}
