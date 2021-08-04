/////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2009, 2010 OPeNDAP, Inc.
// Authors: Nathan Potter <ndp@oendap.org>
//		    James Gallagher <jgallagher@opendap.org>
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

import org.apache.commons.cli.*;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import ch.qos.logback.classic.LoggerContext;
// import ch.qos.logback.core.util.StatusPrinter;

import java.io.PrintStream;
import java.io.IOException;

import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Stack;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;

import java.net.URL;
import java.net.MalformedURLException;

import opendap.namespaces.THREDDS;
import opendap.namespaces.XLINK;

/**
 * Created by IntelliJ IDEA. User: ndp Date: Dec 1, 2009 Time: 7:37:51 AM To
 * change this template use File | Settings | File Templates.
 * 
 * @note This version of ThreddsCatalogUtil has been hacked to use metacat
 * as a cache. That never really panned out but I'm saving this in case it
 * proves useful... 
 */
public class ThreddsCatalogUtilMetacat {

	private boolean useCache = false;
	private boolean useDocumentCache = false;
	private XMLOutputter xmlo = null;
	private ResponseCache TCCache = null;
	
	private Logger log = LoggerFactory.getLogger(ThreddsCatalogUtilMetacat.class);

	/**
	 * Default ctor; build an instance that does not use the catalog cache.
	 * @throws Exception 
	 * 
	 * @throws Exception Thrown if the catalog cache cannot be initialized.
	 */
	public ThreddsCatalogUtilMetacat() throws Exception {
		this(false, "", false, "", "", "");
	}
	
	/**
	 * Constructor. Build an instance that uses the cache to eliminate looping
	 * but do not cache the actual responses.
	 * 
	 * @param useCache True if the cache should be used, false if not
	 * @throws Exception Thrown if the cache cannot be initialized.
	 */
	public ThreddsCatalogUtilMetacat(boolean useCache, String namePrefix) throws Exception {
		this(useCache, namePrefix, false, "", "", "");
	}
	
	/** 
	 * Constructor. This constructor gives the finest control over the caching 
	 * operations performed. Because some sites use lots of catalogs, it might
	 * require lots of space to cache the entire catalog. However, it would
	 * still be nice to know about (or avoid) loops!
	 * 
	 * @param useCache True if caching should be used
	 * @param namePrefix The name of the cache files
	 * @param useDocumentCache True if the response documents should be cached
	 * @throws Exception
	 */
	public ThreddsCatalogUtilMetacat(boolean useCache, String namePrefix, boolean useDocumentCache) throws Exception {
		this(useCache, namePrefix, useDocumentCache, "", "", "");
	}
	
		/** 
		 * Constructor. This constructor gives the finest control over the caching 
		 * operations performed. Because some sites use lots of catalogs, it might
		 * require lots of space to cache the entire catalog. However, it would
		 * still be nice to know about (or avoid) loops!
		 * 
		 * @param useCache True if caching should be used
		 * @param namePrefix The name of the cache files
		 * @param useDocumentCache True if the response documents should be cached
		 * @throws Exception
		 */
		public ThreddsCatalogUtilMetacat(boolean useCache, String namePrefix, boolean useDocumentCache,
				String metacatUrl, String username, String password) throws Exception {
		xmlo = new XMLOutputter(Format.getPrettyFormat());

		if (useCache) {
			this.useCache = useCache;
			this.useDocumentCache = useDocumentCache;
			
			// The second and third arguments to ResponseCache control 
			// if the existing saved-state files are used to restore the cache
			// and if the resulting cache is saved at the end of the run.
			// The last three control whether metacat is used to cache the
			// documents (if the metacatUrl string is empty, it is not used
			TCCache = new ResponseCache(namePrefix + "TC", useCache, useCache, 
					metacatUrl, username, password);
		}
		
	    // print internal logging state
	    /*
	    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    StatusPrinter.print(lc);
	    */
	}
	
	/**
	 * Implements a modified depth-first traversal of a thredds catalog. The
	 * catalog is treated as a tree-like structure, but since it is really a
	 * directed graph, catalog URLs are cached and the same URL is neither
	 * returned or 'crawled' twice. The traversal is like a depth-first
	 * traversal, but instead of recurring all the way to the leaf nodes of the
	 * pseudo-tree, it descends as it returns nodes. The seed URL is used to
	 * initialize the stack of child nodes and then nextElement() both returns
	 * the top node and visits that node, pushing all of its URLs onto the
	 * stack. This limits the HTTP accesses to at most one-per nextElement call.
	 * 
	 * @note If constructed to use a cache (the default), catalog URLs will be
	 *       cached along with the catalog responses.
	 * 
	 * @author jimg
	 * 
	 */
	public class threddsCrawlerEnumeration implements Enumeration<String> {

		// This holds catalogs not yet seen by the user of the Enumeration
		private Stack<String> childURLs;

		threddsCrawlerEnumeration(String catalogURL) throws Exception {
			childURLs = new Stack<String>();
	    	childURLs.push(catalogURL);
		}
		
		/*
		threddsCrawlerEnumeration(String catalogURL, boolean useCache) throws Exception {
			childURLs = new Stack<String>();
	    	childURLs.push(catalogURL);
		}
        */
		
		private void recur(String catalogURL) {
			Vector<String> URLs = getCatalogRefURLs(catalogURL, false);
			if (URLs != null) {
				for (String URL : URLs) {
					if (useCache && !TCCache.isVisited(URL)) {
						TCCache.setLastVisited(URL, 1);
						childURLs.push(URL);
					}
					else {	
						childURLs.push(URL);
					}
				}
			}
		}
		
		@Override
		public boolean hasMoreElements() {
			return !childURLs.isEmpty();
		}

		@Override
		public String nextElement() {
			try {
				String child = childURLs.pop();
				recur(child);
				return child;
			}
			catch (EmptyStackException e) {
				return null;
			}
			
		}
	}

	public static enum SERVICE {

		ALL,

		ADDE {
			public String toString() {
				return "ADDE";
			}
		},
		DODS {
			public String toString() {
				return "DODS";
			}
		},
		OPeNDAP {
			public String toString() {
				return "OPeNDAP";
			}
		},
		OPeNDAP_G {
			public String toString() {
				return "OPeNDAP-G";
			}
		},
		HTTPServer {
			public String toString() {
				return "HTTPServer";
			}
		},
		GridFTP {
			public String toString() {
				return "GridFTP";
			}
		},
		File {
			public String toString() {
				return "File";
			}
		},
		LAS {
			public String toString() {
				return "LAS";
			}
		},
		WMS {
			public String toString() {
				return "WMS";
			}
		},
		WFS {
			public String toString() {
				return "WFS";
			}
		},
		WCS {
			public String toString() {
				return "WCS";
			}
		},
		WSDL {
			public String toString() {
				return "WSDL";
			}
		},
		WebForm {
			public String toString() {
				return "WebForm";
			}
		},
		Catalog {
			public String toString() {
				return "Catalog";
			}
		},
		QueryCapability {
			public String toString() {
				return "QueryCapability";
			}
		},
		Resolver {
			public String toString() {
				return "Resolver";
			}
		},
		Compound {
			public String toString() {
				return "Compound";
			}
		}
	}
	
	public static void main(String[] args) {

		try {
			Options options = createCmdLineOptions();

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			ThreddsCatalogUtilMetacat tcc = new ThreddsCatalogUtilMetacat(false, "");

			//tcc.testGetCatalogURLs("http://test.opendap.org:8090/opendap/data/catalog.xml");
			
			// tcc.getDataAccessURLs("http://crawlTest.opendap.org:8080/opendap/coverage/catalog.xml",datasetURLs);
			// tcc.getDataAccessURLs("http://motherlode.ucar.edu:8080/thredds/idd/satellite.xml",datasetURLs);

			// http://motherlode.ucar.edu:8080/thredds/catalog/satellite/3.9/PR-REGIONAL_4km/20091203/catalog.html
			// tcc.getDataAccessURLs("file:/Users/ndp/hyrax/ioos/crawlTest.xml",datasetURLs);
			// tcc.getDataAccessURLs("file:/Users/ndp/hyrax/ioos/satellite.xml",datasetURLs);
			// tcc.getDataAccessURLs("http://motherlode.ucar.edu:8080/thredds/catalog/satellite/3.9/PR-REGIONAL_4km/20091203/catalog.xml",datasetURLs);

			// tcc.getDataAccessURLs("http://motherlode.ucar.edu:8080/thredds/idd/satellite.xml",datasetURLs);
			// tcc.getDataAccessURLs("http://crawlTest.opendap.org:8080/opendap/catalog.xml",datasetURLs);
			// tcc.getDataAccessURLs("http://crawlTest.opendap.org:8080/opendap/data/catalog.xml",datasetURLs);
			// tcc.getDataAccessURLs("http://oceanwatch.pfeg.noaa.gov/thredds/catalog.xml",datasetURLs);
			/*
			tcc.crawlTest(System.out,
					"http://blackburn.whoi.edu:8081/thredds/bathy_catalog.xml",
					false);
			*/
			// tcc.crawlTest(System.out,"http://motherlode.ucar.edu:8080/thredds/idd/satellite.xml",false);
		}
		catch (ParseException e) {
			System.err.println("Failed to parse command line! Msg: "
					+ e.getMessage());
		}
		catch (Exception e) {
			System.err.println("Error : " + e.getMessage());
			e.printStackTrace();
		}

	}

	private static Options createCmdLineOptions() {

		Options options = new Options();

		options
				.addOption(
						"e",
						false,
						"encode the command line arguments, cannot be used in conjunction with -d (decode).");
		options
				.addOption(
						"d",
						false,
						"decode the command line arguments, cannot be used in conjunction with -e (encode).");
		options.addOption("t", false,
				"runs internal tests and produces output on stdout.");

		return options;

	}

	public void crawlTest(PrintStream ps, String catalogURLString,
			boolean recurse) {
		Vector<String> datasetURLs;
		Vector<String> catalogURLs;

		ps.println("#########################################################");
		ps.println("Testing THREDDS Catalog Crawl Functions.");
		ps.println("");
		ps.println("Using THREDDS catalog URL: " + catalogURLString);
		ps.println("Recursion is " + (recurse ? "ON" : "OFF"));

		ps.println("");
		ps.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
		ps.println("Retrieving DAP data set access URLs");
		ps.println("");
		datasetURLs = getDataAccessURLs(catalogURLString, SERVICE.OPeNDAP,
				recurse);

		for (String datasetURL : datasetURLs) {
			ps.println("    Found DAP data set URL: " + datasetURL);
		}
		ps.println("Located " + datasetURLs.size() + " access URLs.");

		ps.println("");
		ps.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
		ps.println("Dumping all DDX documents found in catalog");
		ps.println("");
		printDDXDocuments(ps, catalogURLString, recurse);

		ps.println("");
		ps.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
		ps.println("Retrieving THREDDS catalog URLs");
		ps.println("");

		catalogURLs = getCatalogRefURLs(catalogURLString, recurse);

		for (String catalogURL : catalogURLs) {
			ps.println("    Found THREDDS Catalog URL: " + catalogURL);
		}
		ps.println("Located " + catalogURLs.size() + " THREDDS Catalog URLs.");

	}

	public void printDDXDocuments(PrintStream ps, String catalogUrlString,
			boolean recurse) {

		Vector<Document> ddxDocs = getDDXDocuments(catalogUrlString, recurse);

		for (Document doc : ddxDocs) {
			try {
				xmlo.output(doc, ps);
			}
			catch (IOException e) {
				ps.println(e.getMessage());
			}
			ps.println("\n");
		}

	}

	public Vector<Element> getDDXRootElements(String catalogUrlString,
			boolean recurse) {

		Vector<Element> ddxRootElements = new Vector<Element>();
		Vector<String> ddxUrls = getDDXUrls(catalogUrlString, recurse);

		for (String url : ddxUrls) {
			ddxRootElements.add(getDocumentRoot(url));
		}

		return ddxRootElements;

	}

	public Vector<Document> getDDXDocuments(String catalogUrlString,
			boolean recurse) {

		Vector<Document> ddxDocs = new Vector<Document>();
		Vector<String> ddxUrls = getDDXUrls(catalogUrlString, recurse);

		for (String url : ddxUrls) {
			ddxDocs.add(getDocument(url));
		}

		return ddxDocs;

	}

	public Vector<String> getDDXUrls(String catalogUrlString, boolean recurse) {

		Vector<String> datasetUrls = getDataAccessURLs(catalogUrlString,
				SERVICE.OPeNDAP, recurse);
		String url;

		for (int i = 0; i < datasetUrls.size(); i++) {
			url = datasetUrls.get(i);
			log.debug("Found DAP dataset URL: " + url);
			datasetUrls.set(i, url + ".ddx");
		}

		return datasetUrls;

	}

	/**
	 * Returns all of the THREDDS catalog URLs in the passed catalog element.
	 * 
	 * @param catalogUrlString
	 *            The URL from where the catalog was retrieved.
	 * @param catalog
	 *            The root element (the catalog element) in a THREDDS catalog
	 *            document.
	 * @param recurse
	 *            If true the code will recursively descend into all of the
	 *            child catalogs and return all the contained catalog URLs. Be
	 *            Careful!
	 * @return A vector of fully qualified URL Strings each of which points to a
	 *         THREDDS catalog document.
	 */
	private Vector<String> getCatalogRefURLs(String catalogUrlString,
			Element catalog, boolean recurse) {

		Vector<String> catalogURLs = new Vector<String>();

		try {

			String href;
			String newCatalogURL;
			Element catalogRef;
			Iterator i = catalog.getDescendants(new ElementFilter("catalogRef",
					THREDDS.NS));
			while (i.hasNext()) {
				catalogRef = (Element) i.next();
				href = catalogRef.getAttributeValue("href", XLINK.NS);

				newCatalogURL = getCatalogURL(catalogUrlString, href);

				catalogURLs.add(newCatalogURL);

				if (recurse)
					catalogURLs
							.addAll(getCatalogRefURLs(newCatalogURL, recurse));

			}

		}
		catch (MalformedURLException e) {
			log.error("Malformed URL Exception: " + catalogUrlString + " msg: "
					+ e.getMessage());
		}

		return catalogURLs;

	}

    /**
     * Returns all of the THREDDS catalog URLs contained in the THREDDS catalog
     * located at the passed URL.
     *
     * @param catalogUrlString
     *            The URL from where the catalog was retrieved.
     * @param recurse
     *            If true the code will recursively descend into all of the
     *            child catalogs and return all the contained catalog URLs. Be
     *            Careful!
     * @return A vector of fully qualified URL Strings each of which points to a
     *         THREDDS catalog document. If the catalog returned by
     *         dereferencing <code>catalogUrlString</code> is 'bad' (e.g., the
     *         server returns a 404 response), then the Vector<Sting> result
     *         will be empty.
     */
    public Vector<String> getCatalogRefURLs(String catalogUrlString,
            boolean recurse) {

        Vector<String> catalogURLs = new Vector<String>();

        Element catalog = getDocumentRoot(catalogUrlString);
        if (catalog != null)
            catalogURLs = getCatalogRefURLs(catalogUrlString, catalog, recurse);

        return catalogURLs;
    }


	/**
	 * Crawl a thredds catalog. This implements a modified depth-first traversal
	 * of the 'tree' of catalogs with 'topCatalog' as the root. In reality, a
	 * thredds catalog is a directed graph but the Enumeration returned is smart
	 * enough to avoid loops, so the resulting traversal has a tree-like feel.
	 * The algorithm is like a depth-first traversal but it has been modified so
	 * that HTTP accesses are limited to one per call to nextElement(). When
	 * nextElement is called, its value (call it 'C') is both returned and
	 * crawled so that subsequent calls return the children of 'C'. A real
	 * depth-first traversal would descend all the way to the leaf nodes -
	 * thredds catalogs that contain only references to data set and not other
	 * catalogs. In addition to cutting down on HTTP-induced latency (by capping
	 * the number of calls per invocation of nextElement()), this also ensures
	 * that the client of the Enumeration will see all of the catalogs,
	 * including 'interior' ones.
	 * 
	 * @note By default, this uses the THREDDS Catalog cache. If you want to
	 * crawl catalogs using an Enumeration and not cache the result, use the
	 * other version of this method and pass false for the useCache parameter.
	 * 
	 * @param topCatalog
	 *            The THREDDS catalog that will serve as the root node
	 * @return An Enumeration of Strings that will visit all of the catalogs in
	 *         that tree
	 * @throws Exception Thrown if the cache cannot be configured
	 */
	public Enumeration<String> getCatalogEnumeration(String topCatalog) throws Exception {
		return new threddsCrawlerEnumeration(topCatalog);
	}

	/**
	 * Crawl a collection of thredds catalogs using a Enumeration. This does not
	 * perform a complete crawl and then return the Enumeration but, instead,
	 * the HTTP calls are interwoven with the Enumeration.nextElement() calls.
	 * 
	 * @param topCatalog
	 *            The THREDDS Catalog to serve as the root node
	 * @param useCache
	 *            True if the cache should be used.
	 * @return An Enumeration
	 * @throws Exception
	 *             Thrown if the cache cannot be configured
	 */
	/*
	public Enumeration<String> getCatalogURLs(String topCatalog, boolean useCache) throws Exception {
		return new threddsCrawlerEnumeration(topCatalog, useCache);
	}
	*/
	
	/**
	 * Get access to all of the THREDDS Catalogs in the cache. Note that these
	 * URLs are returned in a random order, not the order in which they were
	 * added to the cache.
	 * 
	 * @return An Enumeration of the THREDDS Catalog URLs crawled so far.
	 */
	public Enumeration<String> getCachedCatalogEnumeration() {
		return TCCache.getLastVisitedKeys();
	}

	/**
	 * Return the THREDDS catalog associated with the given URL from the 
	 * local cache.
	 * @param url Find this THREDDS catalog
	 * @return The THREDDS catalog
	 */
	public String getCachedCatalog(String url) {
		return TCCache.getCachedResponse(url);
	}
	
	public void saveCatalogCache() throws Exception {
		TCCache.saveState();
	}
		
	public static String getUrlInfo(URL url) {
		String info = "URL:\n";

		info += "    getHost():         " + url.getHost() + "\n";
		info += "    getAuthority():    " + url.getAuthority() + "\n";
		info += "    getFile():         " + url.getFile() + "\n";
		info += "    getSystemPath():         " + url.getPath() + "\n";
		info += "    getDefaultPort():  " + url.getDefaultPort() + "\n";
		info += "    getPort():         " + url.getPort() + "\n";
		info += "    getProtocol():     " + url.getProtocol() + "\n";
		info += "    getQuery():        " + url.getQuery() + "\n";
		info += "    getRef():          " + url.getRef() + "\n";
		info += "    getUserInfo():     " + url.getUserInfo() + "\n";

		return info;
	}

	private String getServerUrlString(URL url) {

		String baseURL = null;

		String protocol = url.getProtocol();

		if (protocol.equalsIgnoreCase("file")) {
			log.debug("Protocol is FILE.");

		} else if (protocol.equalsIgnoreCase("http")) {
			log.debug("Protcol is HTTP.");

			String host = url.getHost();
			String path = url.getPath();
			int port = url.getPort();

			baseURL = protocol + "://" + host;

			if (port != -1)
				baseURL += ":" + port;
		}

		log.debug("ServerURL: " + baseURL);

		return baseURL;

	}

	private String getServerUrlString(String url) throws MalformedURLException {

		URL u = new URL(url);

		return getServerUrlString(u);

	}

	private Vector<String> getDataAccessURLs(String catalogUrlString,
			Element catalog, SERVICE service, boolean recurse) {

		Vector<String> serviceURLs = new Vector<String>();

		try {

			URL catalogURL = new URL(catalogUrlString);
			String serverURL = getServerUrlString(catalogURL);
			String msg;

			HashMap<String, Element> services = collectServices(catalog,
					service);
			msg = "#### collectServices Found services:\n";
			for (String srvcName : services.keySet())
				msg += "####     Service Name: " + srvcName + "\n"
						+ xmlo.outputString(services.get(srvcName)) + "\n";
			log.debug(msg);

			Element dataset;
			Iterator i = catalog.getChildren(THREDDS.DATASET, THREDDS.NS)
					.iterator();
			while (i.hasNext()) {
				dataset = (Element) i.next();
				collectDatasetAccessUrls(dataset, services, null, serverURL,
						serviceURLs);
			}

			log.debug("#### Accumulated " + serviceURLs.size()
					+ " access URLs.");

			if (serverURL != null && recurse) {

				String href;
				Element catalogRef;
				String newCatalogURL;
				i = catalog.getDescendants(new ElementFilter("catalogRef",
						THREDDS.NS));
				while (i.hasNext()) {
					catalogRef = (Element) i.next();
					href = catalogRef.getAttributeValue("href", XLINK.NS);
					newCatalogURL = getCatalogURL(catalogUrlString, href);
					serviceURLs.addAll(getDataAccessURLs(newCatalogURL,
							service, recurse));

				}
			}

		}
		catch (Exception e) {
			log.error("Unable to load THREDDS catalog: " + catalogUrlString
					+ " msg: " + e.getMessage());
			e.printStackTrace();
		}

		return serviceURLs;

		// log.warn("Thredds Catalog ingest not yet supported.");
	}

	/**
	 * Returns a vector of data access URIs from The THREDDS catalog located at
	 * the URL contained in the passed parameter String
	 * <code>catalogUrlString</code>.
	 * 
	 * @param catalogUrlString
	 *            The THREDDS catalog to crawl.
	 * @param catalogDoc
	 * @param service
	 *            The SERVICE whose data access URLs you wish to get.
	 * @param recurse
	 *            Controls recursion. A value of True will cause the software to
	 *            recursively traverse the catalog (via thredds:catalogRef
	 *            elements) in search of data access URLs.
	 * @return The vector of data access URLs.
	 */
	/*
	private Vector<String> getDataAccessURLs(String catalogUrlString,
			Document catalogDoc, SERVICE service, boolean recurse) {

		Vector<String> serviceURLs;

		Element catalog = catalogDoc.getRootElement();

		serviceURLs = getDataAccessURLs(catalogUrlString, catalog, service,
				recurse);

		return serviceURLs;

		// log.warn("Thredds Catalog ingest not yet supported.");
	}
	*/
	
	/**
	 * Returns a vector of data access URIs from The THREDDS catalog located at
	 * the URL contained in the passed parameter String
	 * <code>catalogUrlString</code>.
	 * 
	 * @param catalogUrlString
	 *            The THREDDS catalog to crawl.
	 * @param service
	 *            The SERVICE whose data access URLs you wish to get.
	 * @param recurse
	 *            Controls recursion. A value of True will cause the software to
	 *            recursively traverse the catalog (via thredds:catalogRef
	 *            elements) in search of data access URLs.
	 * @return The vector of data access URLs. If the catalog returned by
	 *         dereferencing <code>catalogUrlString</code> is 'bad' (e.g., the
	 *         server returns a 404 response), then the Vector<Sting> result
	 *         will be empty.
	 */

	public Vector<String> getDataAccessURLs(String catalogUrlString,
			SERVICE service, boolean recurse) {

		Vector<String> serviceURLs = new Vector<String>();

		Element catalog = getDocumentRoot(catalogUrlString);
		if (catalog != null)
			serviceURLs = getDataAccessURLs(catalogUrlString, catalog, service,
					recurse);

		return serviceURLs;

		// log.warn("Thredds Catalog ingest not yet supported.");

	}

	/**
	 * Returns the root Element of the XML document located at the URL contained
	 * in the passed parameter <code>docUrlString</code>
	 * 
	 * @param docUrlString
	 *            The URL of the document to retrieve
	 * @return The Document
	 */
	private Element getDocumentRoot(String docUrlString) {

		Element docRoot = null;

		Document doc = getDocument(docUrlString);
		if (doc != null) {
			docRoot = doc.getRootElement();
		}
		return docRoot;

	}

	/**
	 * Returns the Document object for the XML document located at the URL
	 * contained in the passed parameter String <code>docUrlString</code>.
	 * 
	 * @note This is the point in the class where a response is (possibly)
	 * cached.
	 * 
	 * @param docUrlString
	 *            The URL of the document to retrieve.
	 * @return The Document
	 */
	private Document getDocument(String docUrlString) {

		Document doc = null;
		try {

			URL docUrl = new URL(docUrlString);
			SAXBuilder sb = new SAXBuilder();

			log.debug("Retrieving XML Document: " + docUrlString);
			log.debug("Document URL INFO: \n" + getUrlInfo(docUrl));

			doc = sb.build(docUrl);
			log.debug("Loaded XML Document: \n" + xmlo.outputString(doc));
			if (useDocumentCache) {
				TCCache.setCachedResponse(docUrlString, xmlo.outputString(doc));
			}

		}
		catch (MalformedURLException e) {
			log.error("Problem with XML Document URL: " + docUrlString
					+ " Caught a MalformedURLException.  Message: "
					+ e.getMessage());
		}
		catch (IOException e) {
			log.error("Problem retrieving XML Document: " + docUrlString
					+ " Caught a IOException.  Message: " + e.getMessage());
		}
		catch (JDOMException e) {
			log.error("Problem parsing XML Document: " + docUrlString
					+ " Caught a JDOMException.  Message: " + e.getMessage());
		}

		return doc;

	}

	private String getCatalogURL(String catalogUrlString, String href)
			throws MalformedURLException {

		if (href.startsWith("/")) {
			href = getServerUrlString(catalogUrlString) + href;
		} else if (!href.startsWith("http://")) {

			log.debug("catalogUrlString: " + catalogUrlString);
			log.debug("href: " + href);
			String s;
			s = catalogUrlString
					.substring(0, catalogUrlString.lastIndexOf("/"));
			if (!s.endsWith("/"))
				s += "/";
			log.debug("s: " + s);
			href = s + href;
		}
		log.debug("Built THREDDS catalog  URL:'" + href + "'");
		return href;

	}

	private HashMap<String, Element> collectServices(Element threddsCatalog) {
		HashMap<String, Element> services = new HashMap<String, Element>();

		Iterator i = threddsCatalog.getDescendants(new ElementFilter(
				THREDDS.SERVICE, THREDDS.NS));

		Element srvcElem;
		while (i.hasNext()) {
			srvcElem = (Element) i.next();
			services.put(srvcElem.getAttributeValue("name"), srvcElem);
		}

		return services;

	}

	private HashMap<String, Element> collectServices(Element threddsCatalog,
			SERVICE s) {

		HashMap<String, Element> services = collectServices(threddsCatalog);
		HashMap<String, Element> childSrvcs;

		// If they aren't asking for everything...
		if (s != SERVICE.ALL) {
			/* boolean done = false; */
			Element service;

			Vector<String> taggedForRemoval = new Vector<String>();

			for (String serviceName : services.keySet()) {
				service = services.get(serviceName);
				if (service.getAttributeValue("serviceType").equalsIgnoreCase(
						SERVICE.Compound.toString())) {
					childSrvcs = collectServices(service, s);
					if (childSrvcs.isEmpty()) {
						taggedForRemoval.add(serviceName);
					}
				} else if (!service.getAttributeValue("serviceType")
						.equalsIgnoreCase(s.toString())) {
					taggedForRemoval.add(serviceName);
				}

			}

			for (String serviceName : taggedForRemoval) {
				services.remove(serviceName);
			}
		}
		return services;

	}

	private void collectDatasetAccessUrls(Element dataset,
			HashMap<String, Element> services, String inheritedServiceName,
			String baseServerURL, Vector<String> datasetURLs) {

		String urlPath;
		String serviceName;
		String s;
		Element metadata, dset, access;
		/* Iterator i; */

		log.debug("inheritedServiceName: " + inheritedServiceName);

		serviceName = dataset.getAttributeValue("serviceName");
		urlPath = dataset.getAttributeValue("urlPath");
		metadata = dataset.getChild("metadata", THREDDS.NS);

		if (metadata != null
				&& metadata.getAttributeValue("inherited").equalsIgnoreCase(
						"true")) {
			log.debug("Found inherited metadata");
			s = metadata.getChildText("serviceName", THREDDS.NS);
			if (s != null) {
				inheritedServiceName = s;
				log.debug("Updated inheritedServiceName to: "
						+ inheritedServiceName);
			}

		}

		if (urlPath != null) {
			log.debug("<dataset> has urlPath atttribute: " + urlPath);

			if (serviceName == null) {
				log
						.debug("<dataset> missing serviceName atttribute. Checking for child element...");
				serviceName = dataset.getChildText("serviceName", THREDDS.NS);
			}
			if (serviceName == null) {
				log
						.debug("<dataset> missing serviceName childElement. Checking for inherited serviceName...");
				serviceName = inheritedServiceName;
			}

			if (serviceName != null) {
				log.debug("<dataset> has serviceName: " + serviceName);
				datasetURLs.addAll(getAccessURLs(urlPath, serviceName,
						services, baseServerURL));

			}
		}

		Iterator i = dataset.getChildren("access", THREDDS.NS).iterator();
		while (i.hasNext()) {
			access = (Element) i.next();
			datasetURLs.addAll(getAccessURLs(access, services, baseServerURL));
		}

		i = dataset.getChildren(THREDDS.DATASET, THREDDS.NS).iterator();

		while (i.hasNext()) {
			dset = (Element) i.next();
			collectDatasetAccessUrls(dset, services, inheritedServiceName,
					baseServerURL, datasetURLs);
		}

	}

	private Vector<String> getAccessURLs(Element access,
			HashMap<String, Element> services, String baseServerURL) {
		String serviceName = access.getAttributeValue("serviceName");
		String urlPath = access.getAttributeValue("urlPath");

		return getAccessURLs(urlPath, serviceName, services, baseServerURL);

	}

	private Vector<String> getAccessURLs(String urlPath, String serviceName,
			HashMap<String, Element> services, String baseServerURL) {

		Vector<String> accessURLs = new Vector<String>();
		String access, base, serviceType, sname;
		/* Iterator i; */
		Element srvc;

		Element service = services.get(serviceName);

		if (service != null) {
			serviceType = service.getAttributeValue("serviceType");

			if (serviceType.equalsIgnoreCase("Compound")) {
				Iterator i = service.getChildren("service", THREDDS.NS).iterator();
				while (i.hasNext()) {
					srvc = (Element) i.next();
					sname = srvc.getAttributeValue("name");
					Vector<String> v = getAccessURLs(urlPath, sname, services,
							baseServerURL);
					accessURLs.addAll(v);
				}

			} else {
				base = service.getAttributeValue("base");
				access = baseServerURL + base + urlPath;
				accessURLs.add(access);
				log.debug("####  Found access URL: " + access);

			}
		}

		return accessURLs;

	}

}
