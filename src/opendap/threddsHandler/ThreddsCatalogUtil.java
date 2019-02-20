/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import opendap.namespaces.XLINK;
import org.apache.commons.cli.Options;
import org.apache.http.HttpEntity;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * This class is a thredds catalog utility. 
 */
public class ThreddsCatalogUtil {

	private XMLOutputter xmlo = null;
	
	private Logger log;

	private CredentialsProvider credsProvider;
	/**
	 * Constructor. It will look in ~/.netrc for authentication credentials.
	 *  If another source is needed use the other constructor.
	 * 
	 */
    public ThreddsCatalogUtil() {
        log = LoggerFactory.getLogger(this.getClass());

        xmlo = new XMLOutputter(Format.getPrettyFormat());
        credsProvider = null;
        try {
            credsProvider = opendap.http.Util.getNetRCCredentialsProvider();
        } catch (IOException e) {
            String msg = "Unable to load authentication credentials from defult location. " +
                    "Try specifying the credentials location if credentials are required.";
            log.warn(msg);
        }
    }

	/**
	 *
	 * @param credentialsFilename Name of a .netrc formatted file with authentication credentials
	 *                            for various servers.
	 *
	 */
	public ThreddsCatalogUtil(String credentialsFilename) {
        log = LoggerFactory.getLogger(this.getClass());

        xmlo = new XMLOutputter(Format.getPrettyFormat());
        credsProvider = null;
        try {
            credsProvider = opendap.http.Util.getNetRCCredentialsProvider(credentialsFilename, true);
        } catch (IOException e) {
            String msg = "Unable to load authentication credentials from  " +
					credentialsFilename +
					" Message: " +e.getMessage();
			log.warn(msg);
        }
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

		threddsCrawlerEnumeration(String catalogURL) {
			childURLs = new Stack<String>();
	    	childURLs.push(catalogURL);
		}
		
		private void recur(String catalogURL) throws IOException, JDOMException {
            try {
			    Vector<String> URLs = getCatalogRefURLs(catalogURL, false);
                if (URLs != null) {
                    for (String URL : URLs) {
                        childURLs.push(URL);
                    }
                }
            } catch (InterruptedException e) {
                log.error("recur(): Caught InterruptedException returning with recursion incomplete!");
				Thread.currentThread().interrupt();
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
			catch (EmptyStackException | JDOMException | IOException e) {
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




    public static void main(String[] args) throws Exception {
        
		try {

			ThreddsCatalogUtil tcc = new ThreddsCatalogUtil();

			// Get one DMR document and print it.
			Document dmr = opendap.xml.Util.getDocument("https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/MERRA2_200.inst1_2d_asm_Nx.19920123.nc4.dmr.xml",
					opendap.http.Util.getNetRCCredentialsProvider());
			tcc.log.debug(tcc.xmlo.outputString(dmr));

			// Get all the DMR URLs in the collection
			Vector<String> urls =tcc.getDMRUrls("https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/catalog.xml",true);

			// Print the URLS
			if(urls!=null) {
				for (String url : urls) {
					tcc.log.debug(url);
				}
			}

			// Print all the DMR docs in the sub-collection (one month)
			tcc.printDMRDocuments(System.out,"https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/catalog.xml",false);


			//Options options = createCmdLineOptions();

			//CommandLineParser parser = new PosixParser();
			// CommandLine cmd = parser.parse(options, args);

			// ThreddsCatalogUtil tcc = new ThreddsCatalogUtil();

			// tcc.testGetCatalogURLs("http://test.opendap.org:8090/opendap/data/catalog.xml");

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



            // Vector<String> urls =tcc.getDataAccessURLs("https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/catalog.xml",SERVICE.OPeNDAP,true);

            // Vector<String> urls =tcc.getDMRUrls("https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/catalog.xml",true);




            //Vector<String> urls = tcc.getDDXUrls("http://localhost:8080/opendap/catalog.xml",true);




            // tcc.test_apache_client("https://goldsmr4.gesdisc.eosdis.nasa.gov/opendap/MERRA2/M2I1NXASM.5.12.4/1992/01/catalog.xml");




            // tcc.crawlTest(System.out,"http://localhost:8080/opendap/catalog.xml",cmd.hasOption("r"));

			// tcc.crawlTest(System.out, "http://blackburn.whoi.edu:8081/thredds/bathy_catalog.xml", cmd.hasOption("r"));

			// tcc.crawlTest(System.out,"http://motherlode.ucar.edu:8080/thredds/idd/satellite.xml",false);
		}
        finally{;}

	}

	private static Options createCmdLineOptions() {

		Options options = new Options();
        options.addOption(
		        "u",  "user",
				true,
                "");
		options.addOption(
		        "d",
				false,
				"decode the command line arguments, cannot be used in conjunction with -e (encode).");
		options.addOption("t", false,
				"runs internal tests and produces output on stdout.");

		options.addOption("r", false,
				"recursively descend nested THREDDS catalogs.");

		return options;

	}
	
	public void crawlTest(PrintStream ps, String catalogURLString,
			boolean recurse) throws InterruptedException, IOException, JDOMException {
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
			boolean recurse) throws InterruptedException, IOException, JDOMException {

		Vector<Document> ddxDocs = getDDXDocuments(catalogUrlString, recurse);

		for (Document doc : ddxDocs) {
			try {
				xmlo.output(doc, ps);
			}
			catch (IOException e) {
				log.error(e.getMessage());
			}
			ps.println("\n");
		}

	}

	public Vector<Element> getDDXRootElements(String catalogUrlString,
			boolean recurse) throws InterruptedException, IOException, JDOMException {

		Vector<Element> ddxRootElements = new Vector<>();
		Vector<String> ddxUrls = getDDXUrls(catalogUrlString, recurse);

		for (String url : ddxUrls) {
			ddxRootElements.add(opendap.xml.Util.getDocumentRoot(url,credsProvider));
		}

		return ddxRootElements;

	}

	public Vector<Document> getDDXDocuments(String catalogUrlString,
			boolean recurse) throws InterruptedException, IOException, JDOMException {

		Vector<Document> ddxDocs = new Vector<>();
		Vector<String> ddxUrls = getDDXUrls(catalogUrlString, recurse);

		for (String url : ddxUrls) {
            Document doc = opendap.xml.Util.getDocument(url,credsProvider);
            if(doc!=null)
                ddxDocs.add(opendap.xml.Util.getDocument(url,credsProvider));
		}

		return ddxDocs;

	}

    public Vector<String> getDDXUrls(String catalogUrlString, boolean recurse) throws InterruptedException, IOException, JDOMException {

        Vector<String> dataAccessUrls = getDataAccessURLs(catalogUrlString, SERVICE.OPeNDAP, recurse);;
        Vector<String> ddxUrls = new Vector<>();

        String url;

        for (int i = 0; i < dataAccessUrls.size(); i++) {
            url = dataAccessUrls.get(i);
            log.debug("Found DAP dataset URL: " + url);
            ddxUrls.add(url + ".ddx");
        }

        return ddxUrls;

    }


    public Vector<Element> getDMRRootElements(String catalogUrlString,
                                              boolean recurse) throws InterruptedException, IOException, JDOMException {

        Vector<Element> dmrRootElements = new Vector<>();
        Vector<String> dmrUrls = getDMRUrls(catalogUrlString, recurse);

        for (String url : dmrUrls) {
            dmrRootElements.add(opendap.xml.Util.getDocumentRoot(url,credsProvider));
        }

        return dmrRootElements;

    }


    public Vector<Document> getDMRDocuments(String catalogUrlString,
                                            boolean recurse)
            throws InterruptedException, IOException, JDOMException {

        Vector<Document> dmrDocs = new Vector<>();
        Vector<String> dmrUrls = getDMRUrls(catalogUrlString, recurse);

        for (String url : dmrUrls) {
            Document doc = opendap.xml.Util.getDocument(url,credsProvider);
            if(doc!=null)
                dmrDocs.add(opendap.xml.Util.getDocument(url,credsProvider));
        }

        return dmrDocs;

    }


    public Vector<String> getDMRUrls(String catalogUrlString, boolean recurse) throws InterruptedException, IOException, JDOMException {

        Vector<String> dataAccessUrls = getDataAccessURLs(catalogUrlString, SERVICE.OPeNDAP, recurse);;
        Vector<String> dmrUrls = new Vector<>();

        String url;

        for (int i = 0; i < dataAccessUrls.size(); i++) {
            url = dataAccessUrls.get(i);
            log.debug("Found DAP dataset URL: " + url);
            dmrUrls.add(url + ".dmr.xml");
        }

        return dmrUrls;

    }

    public void printDMRDocuments(PrintStream ps, String catalogUrlString,
                                  boolean recurse) throws InterruptedException, IOException, JDOMException {

        Vector<Document> dmrDocs = getDMRDocuments(catalogUrlString, recurse);

        for (Document doc : dmrDocs) {
            try {
                xmlo.output(doc, ps);
            }
            catch (IOException e) {
                log.error(e.getMessage());
            }
            ps.println("\n");
        }

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
			Element catalog, boolean recurse) throws InterruptedException, IOException, JDOMException {

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
            boolean recurse) throws InterruptedException, IOException, JDOMException {

        Vector<String> catalogURLs = new Vector<String>();

        Element catalog = opendap.xml.Util.getDocumentRoot(catalogUrlString,credsProvider);
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
	public Enumeration<String> getCatalogEnumeration(String topCatalog)  throws InterruptedException {
		return new threddsCrawlerEnumeration(topCatalog);
	}

	/**
	 * Crawl a collection of thredds catalogs using a Enumeration. This does not
	 * perform a complete crawl and then return the Enumeration but, instead,
	 * the HTTP calls are interwoven with the Enumeration.nextElement() calls.
	 * 
	 * @param topCatalog
	 *            The THREDDS Catalog to serve as the root node
	 * @return An Enumeration
	 * @throws Exception
	 *             Thrown if the cache cannot be configured
	 */
	public Enumeration<String> getCatalogURLs(String topCatalog)  throws InterruptedException {
		return new threddsCrawlerEnumeration(topCatalog);
	}
	
	public static String getUrlInfo(URL url)  throws InterruptedException{
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

	private String getServerUrlString(URL url)  throws InterruptedException{

		String baseURL = null;

		String protocol = url.getProtocol();

		if (protocol.equalsIgnoreCase("file")) {
			log.debug("Protocol is FILE.");

		} else if (protocol.startsWith("http")) {    // calling .startsWith() catches https and http
			log.debug("Protcol is HTTP(S).");

			String host = url.getHost();
			/* String path = url.getPath(); */
			int port = url.getPort();

			baseURL = protocol + "://" + host;

			if (port != -1)
				baseURL += ":" + port;
		}

		log.debug("ServerURL: " + baseURL);

		return baseURL;

	}

	private String getServerUrlString(String url)  throws InterruptedException, MalformedURLException {

		URL u = new URL(url);

		return getServerUrlString(u);

	}

	private Vector<String> getDataAccessURLs(String catalogUrlString,
			Element catalog, SERVICE service, boolean recurse) throws InterruptedException, IOException, JDOMException {

		Vector<String> serviceURLs = new Vector<String>();

		try {

			URL catalogURL = new URL(catalogUrlString);
			String serverURL = getServerUrlString(catalogURL);
			StringBuilder msg = new StringBuilder();

			HashMap<String, Element> services = collectServices(catalog,
					service);

            if(log.isDebugEnabled()) {
                msg.append("getDataAccessURLs() - Found services:\n");
                for (Element srvc : services.values())
                    msg.append("         Service Name: ").append(srvc.getAttributeValue("name"))
                            .append("\n").append(xmlo.outputString(srvc)).append("\n");

                log.debug(msg.toString());
            }

			Element dataset;
            for (Object o : catalog.getChildren(THREDDS.DATASET, THREDDS.NS)) {
                dataset = (Element) o;
                collectDatasetAccessUrls(dataset, services, null, serverURL,
                        serviceURLs);
            }

			log.debug("getDataAccessURLs() - Accumulated {} access URLs.",serviceURLs.size());

			if (serverURL != null && recurse) {

				String href;
				Element catalogRef;
				String newCatalogURL;
                Iterator i = catalog.getDescendants(new ElementFilter("catalogRef",
                            THREDDS.NS));
				while (i.hasNext()) {
					catalogRef = (Element) i.next();
					href = catalogRef.getAttributeValue("href", XLINK.NS);
					newCatalogURL = getCatalogURL(catalogUrlString, href);
					serviceURLs.addAll(getDataAccessURLs(newCatalogURL,
							service, recurse));

				}
			}

		} catch (MalformedURLException e) {
            log.error("getDataAccessURLs() - Unable to load THREDDS catalog: {} msg: {}",catalogUrlString, e.getMessage());
            
        }

        return serviceURLs;
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
			SERVICE service, boolean recurse) throws InterruptedException, IOException, JDOMException {

		Vector<String> serviceURLs = new Vector<String>();

		Element catalog = opendap.xml.Util.getDocumentRoot(catalogUrlString, credsProvider);
		if (catalog != null)
			serviceURLs = getDataAccessURLs(catalogUrlString, catalog, service,
					recurse);

		return serviceURLs;
	}

	private String getCatalogURL(String catalogUrlString, String href)
			 throws InterruptedException,  MalformedURLException {

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

	private HashMap<String, Element> collectServices(Element threddsCatalog)  throws InterruptedException {
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
			SERVICE s)  throws InterruptedException {

		HashMap<String, Element> services = collectServices(threddsCatalog);
		HashMap<String, Element> childSrvcs;

		// If they aren't asking for everything...
		if (s != SERVICE.ALL) {
			/* boolean done = false; */

			Vector<String> taggedForRemoval = new Vector<String>();

			for (Element service : services.values()) {
                String serviceName = service.getAttributeValue("name");
				if (service.getAttributeValue("serviceType").equalsIgnoreCase(SERVICE.Compound.toString())) {
					childSrvcs = collectServices(service, s);
					if (childSrvcs.isEmpty()) {
						taggedForRemoval.add(serviceName);
					}
				} else if (!service.getAttributeValue("serviceType").equalsIgnoreCase(s.toString())) {
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
			String baseServerURL, Vector<String> datasetURLs)  throws InterruptedException {

		String urlPath;
		String serviceName;
		String s;
		Element metadata, dset, access;
        String datasetName;
		/* Iterator i; */

		log.debug("inheritedServiceName: " + inheritedServiceName);

		serviceName = dataset.getAttributeValue("serviceName");
		urlPath = dataset.getAttributeValue("urlPath");
		metadata = dataset.getChild("metadata", THREDDS.NS);
        datasetName = dataset.getAttributeValue("name");

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
			log.debug("<dataset> has urlPath attribute: " + urlPath);

			if (serviceName == null) {
				log.debug("<dataset> missing serviceName attribute. Checking for child element...");
				serviceName = dataset.getChildText("serviceName", THREDDS.NS);
			}
			if (serviceName == null) {
				log.debug("<dataset> missing serviceName childElement. Checking for inherited serviceName...");
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
            log.debug("Located thredds:access element in dataset '"+datasetName+"'");
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
			HashMap<String, Element> services, String baseServerURL)  throws InterruptedException {
		String serviceName = access.getAttributeValue("serviceName");
		String urlPath = access.getAttributeValue("urlPath");

		return getAccessURLs(urlPath, serviceName, services, baseServerURL);

	}

	private Vector<String> getAccessURLs(String urlPath, String serviceName,
			HashMap<String, Element> services, String baseServerURL) throws InterruptedException  {

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
				log.debug("getAccessURLs() - Found access URL: " + access);

			}
		}

		return accessURLs;
	}

	public XMLOutputter getXmlo() {
		return xmlo;
	}

	public void setXmlo(XMLOutputter xmlo) {
		this.xmlo = xmlo;
	}

   

}
