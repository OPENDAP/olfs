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

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.http.AuthenticationControls;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import opendap.ppt.PPTException;
import opendap.xml.Transformer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides Dispatch Services for the XSLT based THREDDS catalog Handler.
 * <p/>
 * <p/>
 * <p/>
 * Date: Apr 18, 2008
 * Time: 3:46:50 PM
 */
public class StaticCatalogDispatch implements DispatchHandler {


    private Logger _log;

    private Element _config;
    private BesApi _besApi;
    private boolean _initialized;
    private HttpServlet _dispatchServlet;
    private String _prefix;
    boolean _useMemoryCache = false;

    String _catalogToHtmlTransformFile = "/xsl/threddsCatalogPresentation.xsl";
    Transformer _catalogToHtmlTransform = null;
    ReentrantLock _catalogToHtmlTransformLock;

    String _datasetToHtmlTransformFile = "/xsl/threddsDatasetDetail.xsl";
    Transformer _datasetToHtmlTransform = null;
    ReentrantLock _datasetToHtmlTransformLock;



    String _staticCatalogIngestTransformFile = "/xsl/threddsCatalogIngest.xsl";

    public StaticCatalogDispatch() {

        super();

        _log = org.slf4j.LoggerFactory.getLogger(getClass());
        _config = null;
        _initialized = false;
        _prefix = "thredds/";

    }


    public String getCatalogKeyFromRelativeUrl(String relativeUrl) throws IOException {


        String catalogKey = relativeUrl;

        // Make sure it doesn't star with a '/'
        while (catalogKey.startsWith("/"))
            catalogKey = catalogKey.substring(1, catalogKey.length());

        if (catalogKey.equals(_prefix)) { // Then we have to make a top level catalog.

            // So we cast it to the default top level catalog.
            catalogKey += "catalog.xml";
        }


        // Strip the prefix off of the relativeURL)
        if (catalogKey.startsWith(_prefix))
            catalogKey = catalogKey.substring(_prefix.length(), catalogKey.length());

        // If it's a request for an HTML view of the catalog, replace the
        // .html suffix with .xml so we can find the catalog.
        if (catalogKey.endsWith(".html")) {
            catalogKey = catalogKey.substring(0,
                    catalogKey.lastIndexOf(".html")) + ".xml";
        }

        return catalogKey;
    }


    public void sendThreddsCatalogResponse(HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {


        Procedure timedProc = Timer.start();
        try {

            String catalogKey = getCatalogKeyFromRelativeUrl(ReqInfo.getLocalUrl(request));
            String requestSuffix = ReqInfo.getRequestSuffix(request);
            String query = request.getQueryString();

            Request orq = new Request(null, request);

            if (redirectRequest(request, response))
                return;

            // Are we browsing a remote catalog? a remote dataset?
            if (query != null && query.startsWith("browseCatalog=")) {
                // browseRemoteCatalog(response, query);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else if (query != null && query.startsWith("browseDataset=")) {
                // browseRemoteDataset(response, query);
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

            // Is the request for a presentation view (HTML version) of the catalog?
            else if (requestSuffix != null && requestSuffix.equals("html")) {

                if (query != null) {
                    if (query.startsWith("dataset=")) {
                        sendDatasetHtmlPage(request, response, catalogKey, query);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Cannot process query: " + Scrub.urlContent(query));
                    }


                } else {
                    sendCatalogHTML(request, response, catalogKey);
                }

            } else { // Send the the raw catalog XML.
                sendCatalogXML(orq, response, catalogKey);
            }
        }
        finally {
            Timer.stop(timedProc);
        }

    }

    private boolean redirectRequest(HttpServletRequest req,
                                    HttpServletResponse res)
            throws IOException {

        boolean redirect = false;
        String relativeURL = ReqInfo.getLocalUrl(req);

        // Make sure it really is a relative URL.
        if (relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1, relativeURL.length());


        // We know the _prefix ends in slash. So, if this things is the same as
        // prefix sans slash then we redirect to catalog.html
        if (relativeURL.equals(_prefix.substring(0, _prefix.length() - 1))) {
            redirect = true;
        }

        // Redirect _prefix only requests to catalog.html
        if (relativeURL.equals(_prefix) && req.getQueryString() == null) {
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
            _log.debug("Sent redirectForContextOnlyRequest to map the servlet " +
                    "context to a URL that ends in a '/' character! Redirect to: " + Scrub.urlContent(newURI));
        }

        return redirect;
    }


    private void browseRemoteDataset(Request oRequest,
                                     HttpServletResponse response,
                                     String query) throws IOException, SaxonApiException {


        String http = "http://";

        // Sanitize the incoming query.
        query = Scrub.completeURL(query);
        _log.debug("Processing query string: " + query);

        if (!query.startsWith("browseDataset=")) {
            _log.error("Not a browseDataset request: " + Scrub.completeURL(query));
            throw new IOException("Not a browseDataset request!");
        }


        query = query.substring("browseDataset=".length(), query.length());

        String targetDataset = query.substring(0, query.indexOf('&'));

        String remoteCatalog = query.substring(query.indexOf('&') + 1, query.length());


        if (!remoteCatalog.startsWith(http)) {
            _log.error("Catalog Must be remote: " + remoteCatalog);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Catalog Must be remote: " + remoteCatalog);
            return;
        }


        String remoteHost = remoteCatalog.substring(0, remoteCatalog.indexOf('/', http.length()) + 1);

        _log.debug("targetDataset: " + targetDataset);
        _log.debug("remoteCatalog: " + remoteCatalog);
        _log.debug("remoteHost: " + remoteHost);


        // Go get the target catalog:
        HttpClient httpClient = new HttpClient();
        GetMethod request = new GetMethod(remoteCatalog);
        int statusCode = httpClient.executeMethod(request);

        if (statusCode != HttpStatus.SC_OK) {
            _log.error("Can't find catalog: " + remoteCatalog);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + remoteCatalog);
            return;
        }

        InputStream catDocIs = null;

        try {
            catDocIs = request.getResponseBodyAsStream();
            _datasetToHtmlTransformLock.lock();
            _datasetToHtmlTransform.reloadTransformIfRequired();

            // Build the catalog document as an XdmNode.
            XdmNode catDoc = _datasetToHtmlTransform.build(new StreamSource(catDocIs));

            _datasetToHtmlTransform.setParameter("serviceContext", oRequest.getServiceLocalId());
            _datasetToHtmlTransform.setParameter("docsService", oRequest.getDocsServiceLocalID());
            _datasetToHtmlTransform.setParameter("targetDataset", targetDataset);
            _datasetToHtmlTransform.setParameter("remoteCatalog", remoteCatalog);
            _datasetToHtmlTransform.setParameter("remoteHost", remoteHost);


            // Set up the Http headers.
            response.setContentType("text/html");
            response.setHeader("Content-Description", "thredds_catalog");
            response.setStatus(HttpServletResponse.SC_OK);

            // Send the transformed document.
            _datasetToHtmlTransform.transform(catDoc, response.getOutputStream());

            _log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");


        }
        catch (SaxonApiException sapie) {
            if (response.isCommitted()) {
                return;
            }
            // Set up the Http headers.
            response.setContentType("text/html");
            response.setHeader("Content-Description", "ERROR");
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);

            // Responed with error.
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Remote resource does not appear to reference a THREDDS Catalog.");
        }
        finally {
            _datasetToHtmlTransform.clearAllParameters();

            if (catDocIs != null) {
                try {
                    catDocIs.close();
                }
                catch (IOException e) {
                    _log.error("Failed to close InputStream for " + remoteCatalog + " Error Message: " + e.getMessage());
                }
            }
            _datasetToHtmlTransformLock.unlock();
        }


    }


    private void browseRemoteCatalog(Request oRequest, HttpServletResponse response,
                                     String query) throws IOException, SaxonApiException {


        String http = "http://";


        // Sanitize the incoming query.
        query = query.substring("browseCatalog=".length(), query.length());
        String remoteCatalog = Scrub.completeURL(query);

        if (!remoteCatalog.startsWith(http)) {
            _log.error("Catalog Must be remote: " + Scrub.completeURL(remoteCatalog));
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Catalog Must be remote: " + remoteCatalog);
            return;
        }

        // Build URL for remote system:

        String remoteHost = remoteCatalog.substring(0, remoteCatalog.indexOf('/', http.length()) + 1);
        String remoteRelativeURL = remoteCatalog.substring(0, remoteCatalog.lastIndexOf('/') + 1);


        _log.debug("Remote Catalog: " + remoteCatalog);
        _log.debug("Remote Catalog Host: " + remoteHost);
        _log.debug("Remote Catalog RelativeURL: " + remoteRelativeURL);


        // Go get the target catalog:
        HttpClient httpClient = new HttpClient();
        GetMethod request = new GetMethod(remoteCatalog);
        int statusCode = httpClient.executeMethod(request);

        if (statusCode != HttpStatus.SC_OK) {
            _log.error("Can't find catalog: " + remoteCatalog);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + remoteCatalog);
            return;
        }
        InputStream catDocIs = null;


        try {
            catDocIs = request.getResponseBodyAsStream();
            _catalogToHtmlTransformLock.lock();
            _catalogToHtmlTransform.reloadTransformIfRequired();

            // Build the catalog document as an XdmNode.
            XdmNode catDoc = _catalogToHtmlTransform.build(new StreamSource(catDocIs));

            _catalogToHtmlTransform.setParameter("serviceContext", _dispatchServlet.getServletContext().getContextPath());
            _catalogToHtmlTransform.setParameter("dapService", oRequest.getServiceLocalId());
            _datasetToHtmlTransform.setParameter("serviceContext", oRequest.getServiceLocalId());
            _catalogToHtmlTransform.setParameter("docsService", oRequest.getDocsServiceLocalID());

            _catalogToHtmlTransform.setParameter("remoteHost", remoteHost);
            _catalogToHtmlTransform.setParameter("remoteRelativeURL", remoteRelativeURL);
            _catalogToHtmlTransform.setParameter("remoteCatalog", remoteCatalog);


            // Set up the Http headers.
            response.setContentType("text/html");
            response.setHeader("Content-Description", "thredds_catalog");
            response.setStatus(HttpServletResponse.SC_OK);

            // Send the transformed documet.
            _catalogToHtmlTransform.transform(catDoc, response.getOutputStream());

            _log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");

        }
        catch (SaxonApiException sapie) {
            if (response.isCommitted()) {
                return;
            }
            // Set up the Http headers.
            response.setContentType("text/html");
            response.setHeader("Content-Description", "ERROR");
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);

            // Responed with error.
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Remote resource does not appear to reference a THREDDS Catalog.");
        }
        finally {
            // Clean up the transform before releasing it.
            _catalogToHtmlTransform.clearAllParameters();

            if (catDocIs != null) {
                try {
                    catDocIs.close();
                }
                catch (IOException e) {
                    _log.error("Failed to close InputStream for " + remoteCatalog + " Error Message: " + e.getMessage());
                }
            }
            _catalogToHtmlTransformLock.unlock();
        }


    }


    private void sendDatasetHtmlPage(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String catalogKey,
                                     String query) throws IOException, JDOMException, SaxonApiException {


        XdmNode catDoc;

        try {
            _datasetToHtmlTransformLock.lock();
            _datasetToHtmlTransform.reloadTransformIfRequired();

            Request orq = new Request(null,request);



            Catalog cat = CatalogManager.getCatalog(catalogKey);

            if (cat != null) {
                _log.debug("\nFound catalog: " + catalogKey + "   " +
                                "    prefix: " + _prefix
                );
                catDoc = cat.getCatalogAsXdmNode(_datasetToHtmlTransform.getProcessor());
                _log.debug("catDoc.getServiceUrl(): " + catDoc.getBaseURI());
            } else {
                _log.error("Can't find catalog: " + Scrub.urlContent(catalogKey) + "   " +
                                "    prefix: " + _prefix
                );
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + Scrub.urlContent(catalogKey));
                return;
            }

            String targetDataset = query.substring("dataset=".length(), query.length());

            //query = "//*";

            _log.debug("targetDataset: " + targetDataset);

            // Pass the docsService  parameter to the transform
            _datasetToHtmlTransform.setParameter("serviceContext",_dispatchServlet.getServletContext().getContextPath());
            _datasetToHtmlTransform.setParameter("docsService", orq.getDocsServiceLocalID());
            _datasetToHtmlTransform.setParameter("targetDataset", targetDataset);


            AuthenticationControls.setLoginParameters(_datasetToHtmlTransform, request);


            // Set up the http headers.
            response.setContentType("text/html");
            response.setHeader("Content-Description", "thredds_catalog");
            response.setStatus(HttpServletResponse.SC_OK);


            // Send the transformed documet.
            _datasetToHtmlTransform.transform(catDoc, response.getOutputStream());

            _log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");


        } catch (BadConfigurationException e) {
            e.printStackTrace();
        } catch (PPTException e) {
            e.printStackTrace();
        } finally {
            _datasetToHtmlTransform.clearAllParameters();
            _datasetToHtmlTransformLock.unlock();

        }


    }

    private void sendCatalogHTML(HttpServletRequest request, HttpServletResponse response, String catalogKey) throws SaxonApiException, IOException, JDOMException, BadConfigurationException, PPTException {


        try {
            Request orq = new Request(null,request);


            _catalogToHtmlTransformLock.lock();
            _catalogToHtmlTransform.reloadTransformIfRequired();

            XdmNode catDoc;

            Catalog cat = CatalogManager.getCatalog(catalogKey);

            if (cat != null) {
                _log.debug("\nFound catalog: " + catalogKey + "   " +
                                "    prefix: " + _prefix
                );
                catDoc = cat.getCatalogAsXdmNode(_catalogToHtmlTransform.getProcessor());
                if(catDoc==null){
                    String msg = "FAILED to retrieve catalog document associated with file '"+cat.getFileName()+"' UNABLE TO FORMULATE A RESPONSE.";
                    _log.error("sendCatalogHTML() - {}",msg);
                    throw new BadConfigurationException(msg);

                }

                _log.debug("catDoc.getServiceUrl(): " + catDoc.getBaseURI());
            } else {
                _log.error("Can't find catalog: " + Scrub.urlContent(catalogKey) + "   " +
                                "    prefix: " + _prefix
                );
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + Scrub.urlContent(catalogKey));
                return;
            }


            // Send the catalog using the transform.

            response.setContentType("text/html");
            response.setHeader("Content-Description", "thredds_catalog");
            response.setStatus(HttpServletResponse.SC_OK);

            _catalogToHtmlTransform.setParameter("serviceContext", _dispatchServlet.getServletContext().getContextPath());
            _catalogToHtmlTransform.setParameter("dapService", orq.getServiceLocalId());
            _catalogToHtmlTransform.setParameter("docsService", orq.getDocsServiceLocalID());

            AuthenticationControls.setLoginParameters(_catalogToHtmlTransform, request);

            _catalogToHtmlTransform.transform(catDoc, response.getOutputStream());

            _log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");

        }
        finally {
            _catalogToHtmlTransform.clearAllParameters();
            _catalogToHtmlTransformLock.unlock();
        }


    }


    private void sendCatalogXML(Request oRequest, HttpServletResponse response, String catalogKey) throws Exception {


        Catalog cat = CatalogManager.getCatalog(catalogKey);

        if (cat != null) {
            _log.debug("\nFound catalog: " + catalogKey + "   " +
                            "    prefix: " + _prefix
            );

            // Send the XML catalog.
            response.setContentType("text/xml");
            response.setHeader("Content-Description", "dods_directory");
            response.setStatus(HttpServletResponse.SC_OK);
            cat.writeCatalogXML(response.getOutputStream());
            _log.debug("Sent THREDDS catalog XML.");

        } else {
            _log.error("Can't find catalog: " + Scrub.urlContent(catalogKey) + "   " +
                            "    prefix: " + _prefix
            );

            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't " +
                    "find catalog: " + Scrub.urlContent(catalogKey));
        }



    }


    public void init(HttpServlet servlet, Element configuration) throws Exception {


        String s;

        if (_initialized) return;

        _dispatchServlet = servlet;
        _config = configuration;


        Element e;

        e = _config.getChild("prefix");
        if (e != null)
            _prefix = e.getTextTrim();

        if (_prefix.equals("/"))
            throw new Exception("Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <prefix>  " +
                    "child element whose value may not be equal to \"/\"");

        if (!_prefix.endsWith("/"))
            _prefix += "/";

        while (_prefix.startsWith("/") && _prefix.length()>1)
            _prefix = _prefix.substring(1, _prefix.length());

        _log.debug("init() - prefix: {}", _prefix);


        e = _config.getChild("useMemoryCache");
        if (e != null) {
            s = e.getTextTrim();
            if (s.equalsIgnoreCase("true")) {
                _useMemoryCache = true;
            }
        }
        _log.debug("init() - useMemoryCache: {}", _useMemoryCache);


        String ingestTransformFile = null;
        e = _config.getChild("ingestTransformFile");
        if (e != null) {
            ingestTransformFile = e.getTextTrim();
        }

        String besCatalogToDatasetScanCatalogTransformFile = null;
        e = _config.getChild("besCatalogToDatasetScanCatalogTransformFile");
        if (e != null) {
            besCatalogToDatasetScanCatalogTransformFile = e.getTextTrim();
        }

        _log.debug("init() - Configuration file ingest complete.");

        /* USe the generic one for now and if later we want to reuse this code we can pass in another on through the
        config */
        _besApi = new BesApi();

        if (ingestTransformFile == null) {

            ingestTransformFile = ServletUtil.getSystemPath(servlet, _staticCatalogIngestTransformFile);
        }

        _log.debug("init() - Using ingest transform file: " + ingestTransformFile);

        if (besCatalogToDatasetScanCatalogTransformFile == null) {
            besCatalogToDatasetScanCatalogTransformFile = ServletUtil.getSystemPath(servlet, "/xsl/besCatalogToDatasetScanCatalog.xsl");
        }
        _log.debug("init() - Using BES ingest transform file: " + besCatalogToDatasetScanCatalogTransformFile);



        _log.debug("init() - Processing THREDDS catalog.xml file...");

        String contentPath = ServletUtil.getContentPath(servlet);
        CatalogManager.init(contentPath, ingestTransformFile, besCatalogToDatasetScanCatalogTransformFile, _besApi);


        String fileName, pathPrefix, thisUrlPrefix;

        s = "catalog.xml";

        thisUrlPrefix = s.substring(0, s.lastIndexOf(Util.basename(s)));

        s = contentPath + s;
        fileName = "catalog.xml";
        pathPrefix = s.substring(0, s.lastIndexOf(fileName));

        _log.debug("init() - Top Level Catalog - pathPrefix: " + pathPrefix);
        _log.debug("init() - Top Level Catalog - urlPrefix: " + thisUrlPrefix);
        _log.debug("init() - Top Level Catalog - fileName: " + fileName);

        /*
        CatalogManager.addRootCatalog(
                pathPrefix,
                thisUrlPrefix,
                fileName,
                useMemoryCache);
        */

        _log.debug("init() - Memory report prior to static thredds catalog ingest: \n{}", opendap.coreServlet.Util.getMemoryReport());

        CatalogManager.addCatalog(
                pathPrefix,
                thisUrlPrefix,
                fileName,
                _useMemoryCache);

        _log.debug("init() - Memory report post static thredds catalog ingest: \n{}", opendap.coreServlet.Util.getMemoryReport());

        _log.debug("init() - THREDDS catalog.xml (and children thereof) have been ingested.");


        _log.debug("init() - Loading XSLT for thredds presentation views.");

        // Create a lock for use with the thread-unsafe transformer.
        _catalogToHtmlTransformLock = new ReentrantLock(true);

        try {
            _catalogToHtmlTransformLock.lock();

            // ---------------------
            // Get XSLT document name
            String catalogToHtmlXslt = ServletUtil.getSystemPath(_dispatchServlet, _catalogToHtmlTransformFile);

            // Build an cache an XSLT transformer for the XSLT document.
            _catalogToHtmlTransform = new Transformer(catalogToHtmlXslt);


            _log.debug("init() - XSLT file \"" + catalogToHtmlXslt + "\"loaded & parsed. " +
                    "Transfrom object created and cached. " +
                    "Transform lock created.");
        }
        finally {
            _catalogToHtmlTransformLock.unlock();
        }


        // Create a lock for use with the thread-unsafe transformer.
        _datasetToHtmlTransformLock = new ReentrantLock(true);

        try {
            _datasetToHtmlTransformLock.lock();

            // ---------------------
            // Get XSLT document name
            String datasetToHtmlXslt = ServletUtil.getSystemPath(_dispatchServlet, _datasetToHtmlTransformFile);

            // Build an cache an XSLT transformer for the XSLT document.
            _datasetToHtmlTransform = new Transformer(datasetToHtmlXslt);

            _log.debug("init() - XSLT file \"" + datasetToHtmlXslt + "\"loaded & parsed. " +
                    "Transfrom object created and cached. " +
                    "Transform lock created.");
        }
        finally {
            _datasetToHtmlTransformLock.unlock();
        }


        _log.info("init() - Initialized.");
        _initialized = true;
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

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = relativeUrl; //_besApi.getBesDataSourceID(relativeUrl,false);
        //String requestSuffixRegex = ReqInfo.getRequestSuffix(request);

        boolean threddsRequest = false;

        if (dataSource != null) {


            // Since we know the _prefix does not begin with slash, strip any
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
                    _log.info("Sent THREDDS Response");
                }
            }
        }
        return threddsRequest;
    }


    public long getLastModified(HttpServletRequest req) {

        Procedure timedProc = Timer.start();

        RequestCache.openThreadCache();

        String catalogKey = null;
        try {
            catalogKey = getCatalogKeyFromRelativeUrl(ReqInfo.getLocalUrl(req));
            if (requestCanBeHandled(req)) {
                long lm = CatalogManager.getLastModified(catalogKey);
                _log.debug("lastModified(" + catalogKey + "): " + (lm == -1 ? "unknown" : new Date(lm)));
                return lm;
            }
        }
        catch (Exception e) {
            _log.error("Failed to get a last modified time for '" + Scrub.urlContent(catalogKey) + "'  msg: " + e.getMessage());
        }
        finally {
            Timer.stop(timedProc);
        }

        return -1;
    }

    public void destroy() {

        CatalogManager.destroy();
        _log.info("Destroy Complete");


    }




}




