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
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.auth.AuthenticationControls;
import opendap.dap.User;
import opendap.gateway.BesGatewayApi;
import opendap.http.error.BadGateway;
import opendap.http.error.BadRequest;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import opendap.ppt.PPTException;
import opendap.xml.Transformer;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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


    private Logger log;

    private Element _config;
    private BesApi _besApi;
    private boolean _initialized;
    private HttpServlet _dispatchServlet;
    private String _prefix;
    private boolean _useMemoryCache = false;
    private boolean _allowRemoteCatalogTraversal = false;

    private String _catalogToHtmlTransformFile = "/xsl/threddsCatalogPresentation.xsl";
    private Transformer _catalogToHtmlTransform = null;
    private ReentrantLock _catalogToHtmlTransformLock;

    private String _datasetToHtmlTransformFile = "/xsl/threddsDatasetDetail.xsl";
    private Transformer _datasetToHtmlTransform = null;
    private ReentrantLock _datasetToHtmlTransformLock;


    private String _besNodeToDatasetScanCatalogTrasformFile = "/xsl/besNodeToDatasetScanCatalog.xsl";

    private String _staticCatalogIngestTransformFile = "/xsl/threddsCatalogIngest.xsl";

    public StaticCatalogDispatch() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
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


    /**
     * Returns the appropriate THREDDS catalog response. Basically the method embodies the THEREDDS/TDS
     * web UI.
     *
     *
     *
     * @param request
     * @param response
     * @throws Exception
     */
    public void sendThreddsCatalogResponse(HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {


        Procedure timedProc = Timer.start();
        try {
            User user = new User(request);

            String catalogKey = getCatalogKeyFromRelativeUrl(ReqInfo.getLocalUrl(request));
            String requestSuffix = ReqInfo.getRequestSuffix(request);
            String query = request.getQueryString();

            Request orq = new Request(null, request);

            if (redirectRequest(request, response))
                return;


            // Are we browsing a remote catalog? a remote dataset?
            if (query != null && query.startsWith("browseCatalog=")) {

                if(!_allowRemoteCatalogTraversal)
                    throw new BadRequest("Remote Catalog Browsing Has Been DISABLED.");

                browseRemoteCatalog(user, orq, response, query);
            }
            else if (query != null && query.startsWith("browseDataset=")) {
                if(!_allowRemoteCatalogTraversal)
                    throw new BadRequest("Remote Dataset Browsing Has Been DISABLED.");
                browseRemoteDataset(user, orq, response, query);
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
            log.debug("Sent redirectForContextOnlyRequest to map the servlet " +
                    "context to a URL that ends in a '/' character! Redirect to: " + Scrub.urlContent(newURI));
        }

        return redirect;
    }


    private void browseRemoteDataset(User user,
                                     Request oRequest,
                                     HttpServletResponse response,
                                     String query)
            throws IOException, BadRequest, BadGateway, JDOMException, BadConfigurationException, PPTException, BESError {


        String http = "http://";
        String https = "https://";

        // Sanitize the incoming query.
        query = Scrub.completeURL(query);
        log.debug("Processing query string: " + query);

        if (!query.startsWith("browseDataset=")) {
            log.error("Not a browseDataset request: " + Scrub.completeURL(query));
            throw new IOException("Not a browseDataset request!");
        }

        query = query.substring("browseDataset=".length(), query.length());

        String targetDataset = query.substring(0, query.indexOf('&'));

        String remoteCatalog = query.substring(query.indexOf('&') + 1, query.length());
        String remoteRelativeURL = remoteCatalog.substring(0, remoteCatalog.lastIndexOf('/') + 1);

        String remoteHost;

        if (remoteCatalog.startsWith(https)) {
            remoteHost = remoteCatalog.substring(0, remoteCatalog.indexOf('/', https.length()) + 1);
        }
        else if(remoteCatalog.startsWith(http)){
            remoteHost = remoteCatalog.substring(0, remoteCatalog.indexOf('/', http.length()) + 1);
        }
        else {
            String msg = "Catalog must be remote: " + remoteCatalog;
            log.error(msg);
            throw new BadRequest(msg);
        }

        log.debug("targetDataset: " + targetDataset);
        log.debug("remoteCatalog: " + remoteCatalog);
        log.debug("remoteHost: " + remoteHost);

        BesApi besApi = new BesGatewayApi();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        besApi.writeFile(user, remoteCatalog,baos);
        ByteArrayInputStream catDocIs = new ByteArrayInputStream(baos.toByteArray());

        String typeMatch = _besApi.getBesCombinedTypeMatch();
        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        //log.debug("browseRemoteDataset() - BES Combined TypeMatch: {}",typeMatch);

        _datasetToHtmlTransformLock.lock();
        try {
            try {
                _datasetToHtmlTransform.reloadTransformIfRequired();

                // Build the catalog document as an XdmNode.
                XdmNode catDoc = _datasetToHtmlTransform.build(new StreamSource(catDocIs));

                _datasetToHtmlTransform.setParameter("serviceContext", oRequest.getContextPath());
                _datasetToHtmlTransform.setParameter("docsService", oRequest.getDocsServiceLocalID());
                _datasetToHtmlTransform.setParameter("targetDataset", targetDataset);
                _datasetToHtmlTransform.setParameter("remoteCatalog", remoteCatalog);
                _datasetToHtmlTransform.setParameter("remoteRelativeURL", remoteRelativeURL);
                _datasetToHtmlTransform.setParameter("remoteHost", remoteHost);
                _datasetToHtmlTransform.setParameter("typeMatch", typeMatch);


                // Set up the Http headers.
                response.setContentType("text/html");
                response.setHeader("Content-Description", "thredds_catalog");
                response.setStatus(HttpServletResponse.SC_OK);

                // Send the transformed document.
                _datasetToHtmlTransform.transform(catDoc, response.getOutputStream());

                log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");


            } catch (SaxonApiException sapie) {
                throw new BadGateway("Could not ingest remote resource as a THREDDS Catalog. msg: "+sapie.getMessage());
            } finally {
                _datasetToHtmlTransform.clearAllParameters();

                if (catDocIs != null) {
                    try {
                        catDocIs.close();
                    } catch (IOException e) {
                        log.error("Failed to close InputStream for " + remoteCatalog + " Error Message: " + e.getMessage());
                    }
                }
            }
        }
        finally {
            _datasetToHtmlTransformLock.unlock();
        }


    }


    /**
     * This retrieves a remote THREDDS catalog.
     *
     * TODO We could skip using the http client and use the BES gateway and ask for STREAM which should return the
     * TODO catalog bytes and subject th request to the BES security stack.
     * @param oRequest
     * @param response
     * @param query
     * @throws OPeNDAPException
     * @throws IOException
     * @throws JDOMException
     */
    private void browseRemoteCatalog(User user, Request oRequest, HttpServletResponse response,
                                     String query) throws OPeNDAPException, IOException, JDOMException {


        // Sanitize the incoming query.
        query = query.substring("browseCatalog=".length(), query.length());
        String remoteCatalog = Scrub.completeURL(query);

        String remoteHost;

        String http = "http://";
        String https = "https://";
        if (remoteCatalog.startsWith(https)) {
            remoteHost = remoteCatalog.substring(0, remoteCatalog.indexOf('/', https.length()) + 1);
        }
        else if(remoteCatalog.startsWith(http)){
            remoteHost = remoteCatalog.substring(0, remoteCatalog.indexOf('/', http.length()) + 1);
        }
        else {
            String msg = "Catalog Must be remote: " + Scrub.completeURL(remoteCatalog);
            log.error(msg);
            throw new BadRequest(msg);
        }

        // Build URL for remote system:
        String remoteRelativeURL = remoteCatalog.substring(0, remoteCatalog.lastIndexOf('/') + 1);

        log.debug("Remote Catalog: " + remoteCatalog);
        log.debug("Remote Catalog Host: " + remoteHost);
        log.debug("Remote Catalog RelativeURL: " + remoteRelativeURL);

        BesApi besApi = new BesGatewayApi();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        besApi.writeFile(user, remoteCatalog, baos);
        ByteArrayInputStream catDocIs = new ByteArrayInputStream(baos.toByteArray());

        String typeMatch = _besApi.getBesCombinedTypeMatch();
        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        //log.debug("browseRemoteDataset() - BES Combined TypeMatch: {}",typeMatch);

        _catalogToHtmlTransformLock.lock();
        try {
            try {
                _catalogToHtmlTransform.reloadTransformIfRequired();

                // Build the catalog document as an XdmNode.
                XdmNode catDoc = _catalogToHtmlTransform.build(new StreamSource(catDocIs));

                _catalogToHtmlTransform.setParameter("serviceContext", _dispatchServlet.getServletContext().getContextPath());
                _catalogToHtmlTransform.setParameter("dapService", oRequest.getServiceLocalId());
                //_datasetToHtmlTransform.setParameter("serviceContext", oRequest.getServiceLocalId());
                _catalogToHtmlTransform.setParameter("docsService", oRequest.getDocsServiceLocalID());

                _catalogToHtmlTransform.setParameter("remoteHost", remoteHost);
                _catalogToHtmlTransform.setParameter("remoteRelativeURL", remoteRelativeURL);
                _catalogToHtmlTransform.setParameter("remoteCatalog", remoteCatalog);
                _catalogToHtmlTransform.setParameter("typeMatch", typeMatch);


                // Set up the Http headers.
                response.setContentType("text/html");
                response.setHeader("Content-Description", "thredds_catalog");
                response.setStatus(HttpServletResponse.SC_OK);

                // Send the transformed documet.
                _catalogToHtmlTransform.transform(catDoc, response.getOutputStream());

                log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");

            } catch (SaxonApiException sapie) {
                throw new BadGateway("Could not ingest remote resource as a THREDDS Catalog. msg: "+sapie.getMessage());
            } finally {
                // Clean up the transform before releasing it.
                _catalogToHtmlTransform.clearAllParameters();
            }
        }
        finally {
            _catalogToHtmlTransformLock.unlock();
        }


    }


    private void sendDatasetHtmlPage(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String catalogKey,
                                     String query) throws IOException, JDOMException, SaxonApiException, BESError {


        XdmNode catDoc;

        _datasetToHtmlTransformLock.lock();
        try {
            try {
                _datasetToHtmlTransform.reloadTransformIfRequired();

                Request orq = new Request(null, request);


                Catalog cat = CatalogManager.getCatalog(catalogKey);

                if (cat != null) {
                    log.debug("\nFound catalog: " + catalogKey + "   " +
                                    "    prefix: " + _prefix
                    );
                    catDoc = cat.getCatalogAsXdmNode(_datasetToHtmlTransform.getProcessor());
                    if (catDoc == null) {
                        String msg = "FAILED to retrieve catalog document associated with file '" + cat.getFileName() + "' UNABLE TO FORMULATE A RESPONSE.";
                        log.error("sendCatalogHTML() - {}", msg);
                        throw new BadConfigurationException(msg);

                    }
                    log.debug("catDoc.getServiceUrl(): " + catDoc.getBaseURI());
                } else {
                    log.error("Can't find catalog: " + Scrub.urlContent(catalogKey) + "   " +
                                    "    prefix: " + _prefix
                    );
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't find catalog: " + Scrub.urlContent(catalogKey));
                    return;
                }


                String targetDataset = query.substring("dataset=".length(), query.length());

                //query = "//*";

                log.debug("targetDataset: " + targetDataset);

                // Pass the docsService  parameter to the transform
                _datasetToHtmlTransform.setParameter("serviceContext", _dispatchServlet.getServletContext().getContextPath());
                _datasetToHtmlTransform.setParameter("docsService", orq.getDocsServiceLocalID());
                _datasetToHtmlTransform.setParameter("targetDataset", targetDataset);


                AuthenticationControls.setLoginParameters(_datasetToHtmlTransform, request);


                // Set up the http headers.
                response.setContentType("text/html");
                response.setHeader("Content-Description", "thredds_catalog");
                response.setStatus(HttpServletResponse.SC_OK);


                // Send the transformed documet.
                _datasetToHtmlTransform.transform(catDoc, response.getOutputStream());

                log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");


            } catch (BadConfigurationException e) {
                e.printStackTrace();
            } catch (PPTException e) {
                e.printStackTrace();
            } finally {
                _datasetToHtmlTransform.clearAllParameters();

            }
        }
        finally {
            _datasetToHtmlTransformLock.unlock();
        }


    }

    private void sendCatalogHTML(HttpServletRequest request, HttpServletResponse response, String catalogKey)
            throws SaxonApiException, IOException, JDOMException, BadConfigurationException, PPTException, BESError {

        _catalogToHtmlTransformLock.lock();
        try {

            try {
                Request orq = new Request(null, request);


                _catalogToHtmlTransform.reloadTransformIfRequired();

                XdmNode catDoc;

                Catalog cat = CatalogManager.getCatalog(catalogKey);

                if (cat != null) {
                    log.debug("\nFound catalog: " + catalogKey + "   " +
                                    "    prefix: " + _prefix
                    );
                    catDoc = cat.getCatalogAsXdmNode(_catalogToHtmlTransform.getProcessor());
                    if (catDoc == null) {
                        String msg = "FAILED to retrieve catalog document associated with file '" + cat.getFileName() + "' UNABLE TO FORMULATE A RESPONSE.";
                        log.error("sendCatalogHTML() - {}", msg);
                        throw new BadConfigurationException(msg);

                    }

                    log.debug("catDoc.getServiceUrl(): " + catDoc.getBaseURI());
                } else {
                    log.error("Can't find catalog: " + Scrub.urlContent(catalogKey) + "   " +
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

                log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");

            } finally {
                _catalogToHtmlTransform.clearAllParameters();
            }
        }
        finally {
            _catalogToHtmlTransformLock.unlock();
        }


    }


    private void sendCatalogXML(Request oRequest, HttpServletResponse response, String catalogKey) throws Exception {


        Catalog cat = CatalogManager.getCatalog(catalogKey);

        if (cat != null) {
            log.debug("\nFound catalog: " + catalogKey + "   " +
                            "    prefix: " + _prefix
            );

            // Send the XML catalog.
            response.setContentType("text/xml");
            response.setHeader("Content-Description", "dods_directory");
            response.setStatus(HttpServletResponse.SC_OK);
            cat.writeCatalogXML(response.getOutputStream());
            log.debug("Sent THREDDS catalog XML.");

        } else {
            log.error("Can't find catalog: " + Scrub.urlContent(catalogKey) + "   " +
                            "    prefix: " + _prefix
            );

            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Can't " +
                    "find catalog: " + Scrub.urlContent(catalogKey));
        }



    }

    @Override
    public void init(HttpServlet servlet, Element configuration) throws Exception {
        // TODO USe an instance of BesGatewayApi here? I think it could be fine...
        init(servlet,configuration,new BesApi());
    }

    @Override
    public void init(HttpServlet servlet, Element configuration, BesApi besApi) throws Exception {

        _besApi = besApi;

        String s;

        if (_initialized) return;

        _dispatchServlet = servlet;
        _config = configuration;

        _prefix = "thredds";
        _useMemoryCache = true;

        String ingestTransformFile = ServletUtil.getSystemPath(servlet, _staticCatalogIngestTransformFile);

        Element threddsService = _config.getChild("ThreddsService");
        if (threddsService != null) {
            s = threddsService.getAttributeValue("prefix");
            if (s != null)
                _prefix = s;

            if (_prefix.equals("/"))
                throw new Exception("Bad Configuration. The <Handler> " +
                        "element that declares " + this.getClass().getName() +
                        " MUST provide 1 <prefix>  " +
                        "child element whose value may not be equal to \"/\"");

            if (!_prefix.endsWith("/"))
                _prefix += "/";

            while (_prefix.startsWith("/") && _prefix.length()>1)
                _prefix = _prefix.substring(1, _prefix.length());


            s = threddsService.getAttributeValue("useMemoryCache");
            if (s != null){
                if(s.equalsIgnoreCase("true")) {
                    _useMemoryCache = true;
                }
                else {
                    _useMemoryCache = false;
                }
            }

            s = threddsService.getAttributeValue("allowRemote");
            if (s != null){
                if(s.equalsIgnoreCase("true")) {
                    _allowRemoteCatalogTraversal = true;
                }
                else {
                    _allowRemoteCatalogTraversal = false;
                }
            }

            Element e;
            e = threddsService.getChild("ingestTransformFile");
            if (e != null) {
                ingestTransformFile = e.getTextTrim();
            }

        }
        log.debug("init() - prefix: {}", _prefix);
        log.debug("init() - useMemoryCache: {}", _useMemoryCache);
        log.debug("init() - allowRemoteCatalogTraversal: {}", _allowRemoteCatalogTraversal);
        log.debug("init() - Using ingest transform file: " + ingestTransformFile);


        String besNodeToDatasetScanCatalogTransformFile = ServletUtil.getSystemPath(servlet, _besNodeToDatasetScanCatalogTrasformFile);
        Element e = _config.getChild("besNodeToDatasetScanCatalogTransformFile");
        if (e != null) {
            besNodeToDatasetScanCatalogTransformFile = e.getTextTrim();
        }
        log.debug("init() - Using BES Node to DatasetScan Catalog transform file: " + besNodeToDatasetScanCatalogTransformFile);

        log.debug("init() - Configuration file ingest complete.");

        log.debug("init() - Processing THREDDS catalog.xml file...");

        String configPath = ServletUtil.getConfigPath(servlet);
        CatalogManager.init(configPath, ingestTransformFile, besNodeToDatasetScanCatalogTransformFile, _besApi);


        String fileName, pathPrefix, thisUrlPrefix;

        s = "catalog.xml";

        thisUrlPrefix = s.substring(0, s.lastIndexOf(Util.basename(s)));

        s = configPath + s;
        fileName = "catalog.xml";
        pathPrefix = s.substring(0, s.lastIndexOf(fileName));

        log.debug("init() - Top Level Catalog - pathPrefix: " + pathPrefix);
        log.debug("init() - Top Level Catalog - urlPrefix: " + thisUrlPrefix);
        log.debug("init() - Top Level Catalog - fileName: " + fileName);

        /*
        ServiceManager.addRootCatalog(
                pathPrefix,
                thisUrlPrefix,
                fileName,
                useMemoryCache);
        */

        log.debug("init() - Memory report prior to static thredds catalog ingest: \n{}", opendap.coreServlet.Util.getMemoryReport());

        CatalogManager.addCatalog(
                pathPrefix,
                thisUrlPrefix,
                fileName,
                _useMemoryCache);

        log.debug("init() - Memory report post static thredds catalog ingest: \n{}", opendap.coreServlet.Util.getMemoryReport());

        log.debug("init() - THREDDS catalog.xml (and children thereof) have been ingested.");


        log.debug("init() - Loading XSLT for thredds presentation views.");

        // Create a lock for use with the thread-unsafe transformer.
        _catalogToHtmlTransformLock = new ReentrantLock(true);

        try {
            _catalogToHtmlTransformLock.lock();

            // ---------------------
            // Get XSLT document name
            String catalogToHtmlXslt = ServletUtil.getSystemPath(_dispatchServlet, _catalogToHtmlTransformFile);

            // Build an cache an XSLT transformer for the XSLT document.
            _catalogToHtmlTransform = new Transformer(catalogToHtmlXslt);


            log.debug("init() - XSLT file \"" + catalogToHtmlXslt + "\"loaded & parsed. " +
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

            log.debug("init() - XSLT file \"" + datasetToHtmlXslt + "\"loaded & parsed. " +
                    "Transfrom object created and cached. " +
                    "Transform lock created.");
        }
        finally {
            _datasetToHtmlTransformLock.unlock();
        }


        log.info("init() - Initialized.");
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
                    log.info("Sent THREDDS Response");
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
                long lmt = CatalogManager.getLastModified(catalogKey);
                log.debug("lastModified({}): {}", lmt, new Date(lmt));
                return lmt;
            }
        }
        catch (Exception e) {
            log.error("Failed to get a last modified time for '{}' msg: {}", Scrub.urlContent(catalogKey), e.getMessage());
        }
        finally {
            Timer.stop(timedProc);
        }

        return new Date().getTime();
    }

    public void destroy() {

        CatalogManager.destroy();
        log.info("Destroy Complete");


    }




}




