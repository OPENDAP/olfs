/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
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
package opendap.threddsHandler;

import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import org.slf4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;

import thredds.servlet.ServletUtil;


import net.sf.saxon.s9api.*;

/**
 * Provides Dispatch Services for the XSLT based THREDDS catalog Handler.
 * <p/>
 * <p/>
 * <p/>
 * Date: Apr 18, 2008
 * Time: 3:46:50 PM
 */
public class Dispatch implements DispatchHandler {


    private Logger log;

    private Element config;
    private boolean initialized;
    private DispatchServlet dispatchServlet;
    private String _prefix;
    boolean useMemoryCache = false;
    Transformer catalogToHtmlTransform = null;
    ReentrantLock catalogToHtmlTransformLock;

    Transformer datasetToHtmlTransform = null;
    ReentrantLock datasetToHtmlTransformLock;

    public Dispatch() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        config = null;
        initialized = false;
        _prefix = "thredds/";

    }


    public void sendThreddsCatalogResponse(HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {

        String relativeURL = ReqInfo.getFullSourceName(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);
        String query = request.getQueryString();

        if (redirectRequest(request, response))
            return;

        /* Make sure the relative URL is really relative */
        while (relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1, relativeURL.length());


        if (query != null && query.startsWith("browseCatalog=")) {
            browseRemoteCatalog(response, relativeURL, query);
        }

        // Is the request for a presentation view (HTML version) of the catalog?
        else if (requestSuffix != null && requestSuffix.equals("html")) {

            if (query != null) {
                if (query.startsWith("dataset=")) {
                    sendDatasetHtmlPage(response, relativeURL, query);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Cannot process query: " + Scrub.urlContent(query));
                }


            } else {
                sendCatalogHTML(response, relativeURL);
            }

        } else { // Send the the raw catalog XML.
            sendCatalogXML(response, relativeURL);
        }

    }

    private boolean redirectRequest(HttpServletRequest req,
                                    HttpServletResponse res)
            throws IOException {

        boolean redirect = false;
        String relativeURL = ReqInfo.getFullSourceName(req);

        // Make sure it really is a relative URL.
        if (relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1, relativeURL.length());


        // We know the _prefix ends in slash. So, if this things is the same as
        // prefix sans slash then we redirect to catalog.html
        if (relativeURL.equals(_prefix.substring(0, _prefix.length() - 1))) {
            redirect = true;
        }

        // Redirect _prefix only requests to catalog.html
        if (relativeURL.equals(_prefix) && req.getQueryString()==null) {
            redirect = true;
        }

        // And redirect _prefix+contents.html to catalog.html
        if (relativeURL.equals(_prefix + "contents.html")) {
            redirect = true;
        }

        if (redirect) {

            String newURI;

            // make sure that we redirect to the right spot. If the relativeURL
            // ends with a slash then we don't want to add the prefix.
            if (relativeURL.endsWith("/") || relativeURL.endsWith("contents.html"))
                newURI = "catalog.html";
            else
                newURI = _prefix + "catalog.html";

            res.sendRedirect(Scrub.urlContent(newURI));
            log.debug("Sent redirectForContextOnlyRequest to map the servlet " +
                    "context to a URL that ends in a '/' character! Redirect to: " + Scrub.urlContent(newURI));
        }

        return redirect;
    }



    private void browseRemoteCatalog(HttpServletResponse response,
                                     String relativeURL,
                                     String query) throws IOException, SaxonApiException {


        //datasetToHtmlTransformLock.lock();


        String http = "http://";



        // Sanitize the incoming query.
        query = query.substring("browseCatalog=".length(), query.length());
        query = Scrub.completeURL(query);

        if (!query.startsWith(http)) {
            log.error("Catalog Must be remote: " + Scrub.completeURL(query));
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Catalog Must be remote: " + query);
            return;
        }

        // Build URL for remote system:

        String remoteHost = query.substring(0, query.indexOf('/', http.length()) + 1);


        log.debug("Processed query string: " + query);
        log.debug("Remote Catalog Host: " + remoteHost);


        // Go get the target catalog:
        HttpClient httpClient = new HttpClient();
        GetMethod request = new GetMethod(query);
        int statusCode = httpClient.executeMethod(request);

        if (statusCode != HttpStatus.SC_OK) {
            log.error("Can't find catalog: " + Scrub.completeURL(query));
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + query);
            return;
        }
        InputStream catDocIs = request.getResponseBodyAsStream();

        // Get a processor and build the catalog document as an XdmNode.
        Processor proc = new Processor(false);
        DocumentBuilder builder = proc.newDocumentBuilder();
        XdmNode catDoc = builder.build(new StreamSource(catDocIs));


        // Find the transform.
        String xsltDocName = ServletUtil.getPath(dispatchServlet, "/docs/xsl/thredds.xsl");


        // Build a parameter to pass into the XSLT
        String nodeString = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
        nodeString += "<remoteHost>" + remoteHost + "</remoteHost>";
        ByteArrayInputStream reader = new ByteArrayInputStream(nodeString.getBytes());
        XdmNode targetDatasetNode = builder.build(new StreamSource(reader));


        // Get an XSLTCompiler and load the transform.
        XsltCompiler comp = proc.newXsltCompiler();

        XsltExecutable exp = comp.compile(new StreamSource(new File(xsltDocName)));
        XsltTransformer transform = exp.load(); // loads the transform file.

        // Pass the query string as a parameter
        transform.setParameter(new QName("remoteHost"), targetDatasetNode);

        // Target the catalog document.
        transform.setInitialContextNode(catDoc);

        // Set up the transform to send the result to the client.
        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputStream(response.getOutputStream());

        transform.setDestination(out);


        // Send the catalog using the transform.

        response.setContentType("text/html");
        response.setHeader("Content-Description", "thredds_catalog");
        response.setStatus(HttpServletResponse.SC_OK);

        transform.transform();

        log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");


    }


    private void sendDatasetHtmlPage(HttpServletResponse response,
                                     String relativeURL,
                                     String query) throws IOException, JDOMException, SaxonApiException {


        //datasetToHtmlTransformLock.lock();
        Processor proc = new Processor(false);
        DocumentBuilder builder = proc.newDocumentBuilder();
        //builder.setLineNumbering(true);
        XdmNode catDoc;

        // Patch up the request URL so we can find the source catalog
        relativeURL = relativeURL.substring(0,
                relativeURL.lastIndexOf(".html")) + ".xml";


        if (relativeURL.equals(_prefix)) { // Then we have to make a top level catalog.

            // catDoc = CatalogManager.getTopLevelCatalogAsXdmNode(catalogToHtmlTransform.getProcessor());

            // Disabled this code because code based changed to require a
            // single top level catalog, catalog.xml. Thus, this clause
            // should never get excecuted.
            //
            throw new IOException("Synthetic top level catalog not supported.");
        } else {

            // Strip the prefix off of the relativeURL)
            if (relativeURL.startsWith(_prefix))
                relativeURL = relativeURL.substring(_prefix.length(), relativeURL.length());


            Catalog cat = CatalogManager.getCatalog(relativeURL);

            if (cat != null) {
                log.debug("\nFound catalog: " + relativeURL + "   " +
                        "    prefix: " + _prefix
                );
                catDoc = cat.getCatalogAsXdmNode(proc);
                log.debug("catDoc.getBaseURI(): " + catDoc.getBaseURI());
            } else {
                log.error("Can't find catalog: " + relativeURL + "   " +
                        "    prefix: " + _prefix
                );
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + Scrub.urlContent(relativeURL));
                return;
            }
        }

        query = query.substring("dataset=".length(), query.length());

        //query = "//*";

        log.debug("Processed query string: " + query);

        // Find the transform.
        String xsltDocName = ServletUtil.getPath(dispatchServlet, "/docs/xsl/dataset.xsl");


        // Build a parameter to pass into the XSLT
        String nodeString = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
        nodeString += "<targetDataset>" + query + "</targetDataset>";
        ByteArrayInputStream reader = new ByteArrayInputStream(nodeString.getBytes());
        XdmNode targetDatasetNode = builder.build(new StreamSource(reader));


        //Get an XSLTCompiler and load the transform.
        XsltCompiler comp = proc.newXsltCompiler();

        XsltExecutable exp = comp.compile(new StreamSource(new File(xsltDocName)));
        XsltTransformer transform = exp.load(); // loads the transform file.

        // Pass the query string as a parameter
        transform.setParameter(new QName("targetDataset"), targetDatasetNode);

        // Target the catalog document.
        transform.setInitialContextNode(catDoc);

        // Set up the transform to send the result to the client.
        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputStream(response.getOutputStream());

        transform.setDestination(out);


        // Send the catalog using the transform.

        response.setContentType("text/html");
        response.setHeader("Content-Description", "thredds_catalog");
        response.setStatus(HttpServletResponse.SC_OK);

        transform.transform();

        log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");


    }


    private void OLDsendDatasetHtmlPage(HttpServletResponse response,
                                        String relativeURL,
                                        String query) throws IOException, SaxonApiException {


        datasetToHtmlTransformLock.lock();

        XdmNode catDoc;

        // Patch up the request URL so we can find the source catalog
        relativeURL = relativeURL.substring(0,
                relativeURL.lastIndexOf(".html")) + ".xml";


        if (relativeURL.equals(_prefix)) { // Then we have to make a top level catalog.

            // catDoc = CatalogManager.getTopLevelCatalogAsXdmNode(catalogToHtmlTransform.getProcessor());

            // Disabled this code because code based changed to require a
            // single top level catalog, catalog.xml. Thus, this clause
            // should never get excecuted.
            //
            throw new IOException("Synthetic top level catalog not supported.");
        } else {

            // Strip the prefix off of the relativeURL)
            if (relativeURL.startsWith(_prefix))
                relativeURL = relativeURL.substring(_prefix.length(), relativeURL.length());


            Catalog cat = CatalogManager.getCatalog(relativeURL);

            if (cat != null) {
                log.debug("\nFound catalog: " + relativeURL + "   " +
                        "    prefix: " + _prefix
                );
                catDoc = cat.getCatalogAsXdmNode(datasetToHtmlTransform.getProcessor());
                log.debug("catDoc.getBaseURI(): " + catDoc.getBaseURI());
            } else {
                log.error("Can't find catalog: " + relativeURL + "   " +
                        "    prefix: " + _prefix
                );
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + Scrub.urlContent(relativeURL));
                return;
            }
        }


        query = query.substring("dataset=".length(), query.length());

        //query = "//*";

        log.debug("Processing query string: " + query);


        //Processor proc = new Processor(false);
        //XPathCompiler xpath = proc.newXPathCompiler();
        //xpath.declareNamespace("saxon", "http://saxon.sf.net/"); // not actually used, just for demonstration

        Processor proc = datasetToHtmlTransform.getProcessor();
        XPathCompiler xpath = proc.newXPathCompiler();
        xpath.declareNamespace(THREDDS.NAMESPACE_PREFIX, THREDDS.NAMESPACE_STRING);


        // DocumentBuilder builder = proc.newDocumentBuilder();
        // builder.setLineNumbering(true);
        // builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);
        // XdmNode booksDoc = builder.build(new File("data/books.xml"));

        catDoc = catDoc;// Already read the document....


        // find all the ITEM elements, and for each one display the TITLE child

        // XPathSelector selector = xpath.compile("//ITEM").load();

        XPathSelector xps = xpath.compile(query).load();


        //selector.setContextItem(booksDoc);
        xps.setContextItem(catDoc);


        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputStream(response.getOutputStream());


        QName titleName = new QName("dataset");
        for (XdmItem item : xps) {
            XdmNode node = (XdmNode) item;
            System.out.println(node.getNodeName() +
                    "(" + node.getLineNumber() + ") ");
            //proc.writeXdmValue(node, out);

        }

        // Send the catalog using the transform.

        response.setContentType("text/html");
        response.setHeader("Content-Description", "thredds_catalog");
        response.setStatus(HttpServletResponse.SC_OK);
        for (XdmItem dataset : xps) {
            if (!dataset.isAtomicValue()) {
                proc.writeXdmValue(dataset, out);
                //datasetToHtmlTransform.transform((XdmNode)dataset,response.getOutputStream());
            }
        }


        log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");


    }

    // Helper method to get the first child of an element having a given name.
    // If there is no child with the given name it returns null

    private static XdmNode getChild(XdmNode parent, QName childName) {
        XdmSequenceIterator iter = parent.axisIterator(Axis.CHILD, childName);
        if (iter.hasNext()) {
            return (XdmNode) iter.next();
        } else {
            return null;
        }
    }


    private void sendCatalogXML(HttpServletResponse response, String relativeURL) throws Exception {


        if (relativeURL.equals(_prefix)) { // Then we have to make a top level catalog.

            /*
            Document catalog = CatalogManager.getTopLevelCatalogDocument();

            // Send the XML catalog.
            response.setContentType("text/xml");
            response.setHeader("Content-Description", "dods_directory");
            response.setStatus(HttpServletResponse.SC_OK);
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            response.getOutputStream().print(xmlo.outputString(catalog));
            log.debug("Sent THREDDS catalog (raw XML).");
            */
            throw new IOException("We should never be attempting to send a " +
                    "catalog for  just the handlers prefix. The URL should" +
                    "have been redirected to $prefix/catalog.xml");

        } else {
            // Strip the prefix off of the relativeURL)
            if (relativeURL.startsWith(_prefix))
                relativeURL = relativeURL.substring(_prefix.length(), relativeURL.length());

            Catalog cat = CatalogManager.getCatalog(relativeURL);

            if (cat != null) {
                log.debug("\nFound catalog: " + relativeURL + "   " +
                        "    prefix: " + _prefix
                );

                // Send the XML catalog.
                response.setContentType("text/xml");
                response.setHeader("Content-Description", "dods_directory");
                response.setStatus(HttpServletResponse.SC_OK);
                cat.writeCatalogXML(response.getOutputStream());
                log.debug("Sent THREDDS catalog (raw XML).");

            } else {
                log.error("Can't find catalog: " + relativeURL + "   " +
                        "    prefix: " + _prefix
                );

                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't " +
                        "find catalog: " + Scrub.urlContent(relativeURL));
            }
        }

    }

    private void sendCatalogHTML(HttpServletResponse response, String relativeURL) throws SaxonApiException, IOException {

        try {

            catalogToHtmlTransformLock.lock();

            XdmNode catDoc;

            // Patch up the request URL so we can find the source catalog
            relativeURL = relativeURL.substring(0,
                    relativeURL.lastIndexOf(".html")) + ".xml";


            if (relativeURL.equals(_prefix)) { // Then we have to make a top level catalog.

                // catDoc = CatalogManager.getTopLevelCatalogAsXdmNode(catalogToHtmlTransform.getProcessor());

                // Disabled this code because code based changed to require a
                // single top level catalog, catalog.xml. Thus, this clause
                // should never get excecuted.
                //
                throw new IOException("Synthetic top level catalog not supported.");
            } else {
                // Strip the prefix off of the relativeURL)
                if (relativeURL.startsWith(_prefix))
                    relativeURL = relativeURL.substring(_prefix.length(), relativeURL.length());


                Catalog cat = CatalogManager.getCatalog(relativeURL);

                if (cat != null) {
                    log.debug("\nFound catalog: " + relativeURL + "   " +
                            "    prefix: " + _prefix
                    );
                    catDoc = cat.getCatalogAsXdmNode(catalogToHtmlTransform.getProcessor());
                    log.debug("catDoc.getBaseURI(): " + catDoc.getBaseURI());
                } else {
                    log.error("Can't find catalog: " + relativeURL + "   " +
                            "    prefix: " + _prefix
                    );
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + Scrub.urlContent(relativeURL));
                    return;
                }
            }


            // Send the catalog using the transform.

            response.setContentType("text/html");
            response.setHeader("Content-Description", "thredds_catalog");
            response.setStatus(HttpServletResponse.SC_OK);

            catalogToHtmlTransform.transform(catDoc, response.getOutputStream());

            log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");

        }
        finally {
            catalogToHtmlTransformLock.unlock();
        }


    }



    public void init(DispatchServlet servlet, Element configuration) throws Exception {


        String s;

        if (initialized) return;

        dispatchServlet = servlet;
        config = configuration;


        Element e;

        e = config.getChild("prefix");
        if (e != null)
            _prefix = e.getTextTrim();

        if (_prefix.equals("/"))
            throw new Exception("Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <_prefix>  " +
                    "child element whose value may not be equal to \"/\"");


        if (!_prefix.endsWith("/"))
            _prefix += "/";


        //if(!_prefix.startsWith("/"))
        //    _prefix = "/" + _prefix;

        if (_prefix.startsWith("/"))
            _prefix = _prefix.substring(1, _prefix.length());

        e = config.getChild("useMemoryCache");
        if (e != null) {
            s = e.getTextTrim();
            if (s.equalsIgnoreCase("true")) {
                useMemoryCache = true;
            }
        }

        log.debug("Configuration file processing complete.");


        log.debug("Processing THREDDS catalog.xml file...");

        String contentPath = ServletUtil.getContentPath(servlet);
        CatalogManager.init(contentPath);


        String fileName, pathPrefix, thisUrlPrefix;

        s = "catalog.xml";

        thisUrlPrefix = s.substring(0, s.lastIndexOf(Util.basename(s)));

        s = contentPath + s;
        fileName = "catalog.xml";
        pathPrefix = s.substring(0, s.lastIndexOf(fileName));

        log.debug("Top Level Catalog - pathPrefix: " + pathPrefix);
        log.debug("Top Level Catalog - urlPrefix: " + thisUrlPrefix);
        log.debug("Top Level Catalog - fileName: " + fileName);

        /*
        CatalogManager.addRootCatalog(
                pathPrefix,
                thisUrlPrefix,
                fileName,
                useMemoryCache);
        */
        CatalogManager.addCatalog(
                pathPrefix,
                thisUrlPrefix,
                fileName,
                useMemoryCache);

        log.debug("THREDDS catalog.xml (and children thereof) have been ingested.");


        log.debug("Loading XSLT for thredds presentation views.");

        // Create a lock for use with the thread-unsafe transformer.
        catalogToHtmlTransformLock = new ReentrantLock(true);

        try {
            catalogToHtmlTransformLock.lock();

            // ---------------------
            // Get XSLT document name
            String catalogToHtmlXslt = ServletUtil.getPath(dispatchServlet, "/docs/xsl/thredds.xsl");

            // Build an cache an XSLT transformer for the XSLT document.
            catalogToHtmlTransform = new Transformer(catalogToHtmlXslt);


            log.debug("XSLT file \"" + catalogToHtmlXslt + "\"loaded & parsed. " +
                    "Transfrom object created and cached. " +
                    "Transform lock created.");
        }
        finally {
            catalogToHtmlTransformLock.unlock();
        }


        // Create a lock for use with the thread-unsafe transformer.
        datasetToHtmlTransformLock = new ReentrantLock(true);

        try {
            datasetToHtmlTransformLock.lock();

            // ---------------------
            // Get XSLT document name
            String datasetToHtmlXslt = ServletUtil.getPath(dispatchServlet, "/docs/xsl/thredds.xsl");

            // Build an cache an XSLT transformer for the XSLT document.
            datasetToHtmlTransform = new Transformer(datasetToHtmlXslt);

            log.debug("XSLT file \"" + datasetToHtmlXslt + "\"loaded & parsed. " +
                    "Transfrom object created and cached. " +
                    "Transform lock created.");
        }
        finally {
            datasetToHtmlTransformLock.unlock();
        }


        log.info("Initialized.");
        initialized = true;
    }

    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return threddsRequestDispatch(request, null, false);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        threddsRequestDispatch(request, response, true);
    }


    private boolean threddsRequestDispatch(HttpServletRequest request,
                                           HttpServletResponse response,
                                           boolean sendResponse)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        //String requestSuffix = ReqInfo.getRequestSuffix(request);

        boolean threddsRequest = false;

        if (dataSource != null) {


            // Since we know the _prefix does not begin with slahs, strip any
            // leading slashes from the dataSource name.
            while (dataSource.startsWith("/"))
                dataSource = dataSource.substring(1, dataSource.length());


            // We know the _prefix ends in slash. So let's strip the slash
            // before we compare. This makes sure that we pick up the URL
            // that ends with the prefix and no slash
            if (dataSource.startsWith(_prefix.substring(0, _prefix.length() - 1))) {
                threddsRequest = true;
                if (sendResponse) {
                    sendThreddsCatalogResponse(request, response);
                    log.info("Sent THREDDS Response");
                }
            }
        }
        return threddsRequest;
    }


    public long getLastModified(HttpServletRequest req) {
        try {
            if (requestCanBeHandled(req)) {
                String relativeURL = ReqInfo.getFullSourceName(req);

                // Make sure it's a relative URL
                while (relativeURL.startsWith("/"))
                    relativeURL = relativeURL.substring(1, relativeURL.length());

                // Strip the prefix off of the relativeURL)
                if (relativeURL.startsWith(_prefix))
                    relativeURL = relativeURL.substring(_prefix.length(), relativeURL.length());

                // If it's a request for an HTML view of the catalog, replace the
                // .html suffix with .xml so we can find the catalog.
                if (relativeURL.endsWith(".html")) {
                    relativeURL = relativeURL.substring(0,
                            relativeURL.lastIndexOf(".html")) + ".xml";
                }

                long lm = CatalogManager.getLastModified(relativeURL);
                log.debug("lastModified(" + relativeURL + "): " + (lm == -1 ? "unknown" : new Date(lm)));
                return lm;
            }
        }
        catch (Exception e) {
            log.error(e.getMessage());
        }
        return -1;
    }

    public void destroy() {
        log.info("Destroy Complete");


    }


}




