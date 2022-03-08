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

package opendap.noaa_s3;

import opendap.bes.BESManager;
import opendap.coreServlet.*;
import opendap.logging.LogUtil;
import opendap.logging.Procedure;
import opendap.logging.Timer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/13/13
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3CatalogServlet extends HttpServlet {

    private org.slf4j.Logger _log;
    private boolean _initialized;
    //private String systemPath;
    private AtomicInteger _reqNumber;
    private Document _configDoc;
    private S3DapDispatchHandler _s3DapDispatcher;

    private static final String defaultDapServiceContext = "/dap";
    private static final String defaultCatalogServiceContext = "/catalog";
    private static final String defaultCacheDir = "/tmp/S3";


    public S3CatalogServlet() {
        _initialized = false;
        _reqNumber = new AtomicInteger(0);
        //_servletContext = null;
    }

    @Override
    public void init() throws ServletException {

        if(_initialized) return;
        super.init();
        initLogging();
        RequestCache.openThreadCache();

        // init logging
        LogUtil.logServerStartup("init()");
        _log.info("init() start.");
        PersistentConfigurationHandler.installDefaultConfiguration(this,"s3.xml");
        ingestConfig();
        _s3DapDispatcher = new S3DapDispatchHandler(S3CatalogManager.theManager().getDapServiceContext());
        try {
            Element besConfiguration = getBesManagerConfig();
            BESManager.init(getServletContext(),besConfiguration);
            _s3DapDispatcher.init(this, getDapDispatConfig() );

        } catch (Exception e) {
            _log.error("Failed to initialize {}", _s3DapDispatcher.getClass().getName());
            throw new ServletException(e);
        }
        RequestCache.closeThreadCache();
        _log.info("init() complete.");
        _initialized = true;
    }



    private void ingestConfig() throws ServletException {

        loadConfig();
        String servletContextPath = getServletContext().getContextPath();
        Element s3Config = _configDoc.getRootElement();

        //=================================================================================
        //   Dap Service Config
        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        String dapServiceContext;
        Element dapServiceElement = s3Config.getChild("DapService");
        if(dapServiceElement!=null){
            dapServiceContext = dapServiceElement.getAttributeValue("context");
            if(dapServiceContext == null){
                dapServiceContext = defaultDapServiceContext;
                _log.warn("ingestConfig() - DapService is missing 'context' attribute. Using default DAP service context.");
            }
        }
        else {
            dapServiceContext = defaultDapServiceContext;
            _log.warn("ingestConfig() - DapService element is missing. Using default DAP service context.");
        }
        S3CatalogManager.theManager().setDapServiceContext(servletContextPath + dapServiceContext);
        _log.info("ingestConfig() - DapService context is set to '{}'",S3CatalogManager.theManager().getDapServiceContext());


        //=================================================================================
        //   Catalog Service Config
        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        String catalogServiceContext;
        Element catalogServiceElement = s3Config.getChild("CatalogService");
        if(catalogServiceElement!=null){
            catalogServiceContext = catalogServiceElement.getAttributeValue("context");
            if(catalogServiceContext == null){
                catalogServiceContext = defaultCatalogServiceContext;
                _log.warn("ingestConfig() - CatalogService is missing 'context' attribute. Using default Catalog service context.");
            }
        }
        else {
            catalogServiceContext = defaultCatalogServiceContext;
            _log.warn("ingestConfig() - CatalogService element is missing. Using default Catalog service context.");

        }
        S3CatalogManager.theManager().setCatalogServiceContext(servletContextPath + catalogServiceContext);
        _log.info("ingestConfig() - CatalogService context is set to '{}'",S3CatalogManager.theManager().getCatalogServiceContext());

        //=================================================================================
        //   Cache Directory Config
        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        String cacheDir;
        Element catalogCacheElement = s3Config.getChild("CatalogCache");
        if(catalogCacheElement!=null){
            cacheDir = catalogCacheElement.getTextTrim();
            if(cacheDir == null){
                cacheDir = defaultCacheDir;
                _log.warn("ingestConfig() - CatalogCache is missing text content. Using default Catalog cache directory.");
            }
        }
        else {
            cacheDir = defaultCacheDir;
            _log.warn("ingestConfig() - CatalogCache element is missing. Using default Catalog cache directory.");
        }
        _log.info("ingestConfig() - Catalog cache directory is set to '{}'",cacheDir);
        S3CatalogManager.theManager().setS3CatalogCacheDir(cacheDir);

        //=================================================================================
        //   Bucket Config
        //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
        boolean hasBuckets = false;
        for(Object o :s3Config.getChildren("Bucket")){
            Element bucket = (Element) o;
            String name = bucket.getAttributeValue("name");
            String context = bucket.getAttributeValue("context");
            String loadCatalogOnStart = bucket.getAttributeValue("loadCatalogOnStart");

            if(name==null){
                _log.error("ingestConfig() - ERROR: Bucket element is missing the required 'name' attribute. SKIPPING...");
            }
            if(context==null){
                context = name;
                _log.warn("ingestConfig() - Bucket element is missing the optional 'context' attribute. Using bucket name '{}' as context name.",name);

            }
            _log.info("ingestConfig() - Adding bucket '{}' under context '{}'",name,context);
            S3CatalogManager.theManager().addBucket(context,name);

            if(loadCatalogOnStart!=null && loadCatalogOnStart.equalsIgnoreCase("true")){
                try {
                    _log.info("Loading Catalog Index Files For AWS S3 Bucket '{}'.",name);
                    S3CatalogManager.theManager().ingestIndex(context,name);
                } catch (JDOMException | IOException e) {
                    String msg = "ERROR! Failed to ingest Index of bucket '"+name+"' msg: "+e.getMessage();
                    throw new ServletException(msg,e);
                }
                try {
                    _log.info("Loading Catalog File References From Index Of AWS S3 Bucket '{}'.",name);
                    S3CatalogManager.theManager().ingestIndexedFiles(context, name);
                } catch (JDOMException | IOException e) {
                    String msg = "ERROR! Failed to ingest Index of bucket '"+name+"' msg: "+e.getMessage();
                    throw new ServletException(msg,e);
                }
            }
            hasBuckets = true;
        }
        if(!hasBuckets) {
            String msg = "ERROR! The S3 service configuration contains no buckets! I can haz BUCKETS?!?! ";
            throw new ServletException(msg);
        }
    }







    /**
     * Loads the configuration file specified in the servlet parameter
     * S3ConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private void loadConfig() throws ServletException {

        String filename = getInitParameter("S3ConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the S3 Service configuration!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }
        filename = Scrub.fileName(ServletUtil.getConfigPath(this) + filename);

        _log.debug("Loading Configuration File: " + filename);
        try {
            _configDoc = opendap.xml.Util.getDocument(filename);
        } catch (IOException e) {
            String msg = "OLFS configuration file \"" + filename + "\" is not readable. msg: "+e.getMessage();
            _log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "OLFS configuration file \"" + filename + "\" cannot be parsed. msg: "+e.getMessage();
            _log.error(msg);
            throw new ServletException(msg, e);
        }
        _log.debug("Configuration loaded and parsed.");
    }

    /**
     * Starts the logging process.
     */
    private void initLogging() {
        LogUtil.initLogging(this);
        _log = org.slf4j.LoggerFactory.getLogger(getClass());

    }

    private Element getDapDispatConfig(){
        Element dapDispatchConfig = _configDoc.getRootElement().getChild("DapDispatch");

        if(dapDispatchConfig == null) {
            _log.warn("getDapDispatConfig() - DapDispatch element not found in configuration file. Using defaults.");
            dapDispatchConfig = new Element("DapDispatch");
            // config.addContent(new Element("AllowDirectDataSourceAccess"));
            dapDispatchConfig.addContent(new Element("UseDAP2ResourceUrlResponse"));
        }
        else {
            _log.debug("getDapDispatConfig() - Found DapDispatch element in configuration file.");
        }
        if(_log.isDebugEnabled()){
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            _log.debug("getDapDispatConfig() - Using DapDispatch config:\n{}",xmlo.outputString(dapDispatchConfig));

        }
        return dapDispatchConfig;
    }


    private Element getBesManagerConfig() {
        Element besManagerConfig = _configDoc.getRootElement().getChild(BESManager.BES_MANAGER_CONFIG_ELEMENT);
        if(besManagerConfig==null){
            _log.warn("getBesManagerConfig() - BESManager element not found in configuration file. Using defaults.");
            Element e;
            besManagerConfig = new Element("BESManager");

            Element bes = new Element("BES");
            besManagerConfig.addContent(bes);

            e = new Element("prefix");
            e.setText("/");
            bes.addContent(e);

            e = new Element("host");
            e.setText("localhost");
            bes.addContent(e);

            e = new Element("port");
            e.setText("10022");
            bes.addContent(e);

            e = new Element("maxResponseSize");
            e.setText("0");
            bes.addContent(e);

            e = new Element("ClientPool");
            e.setAttribute("maximum","10");
            e.setAttribute("maxCmds","2000");
            bes.addContent(e);
        }
        else {
            _log.debug("getBesManagerConfig() - Found BESManager element in configuration file.");
        }
        if(_log.isDebugEnabled()){
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            _log.debug("getBesManagerConfig() - Using BESManager config:\n{}",xmlo.outputString(besManagerConfig));

        }
        return besManagerConfig;
    }

    private boolean redirectToCatalog(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String relativeUrl =ReqInfo.getLocalUrl(req);
        String redirectTo;
        if(relativeUrl.endsWith("/")){
            redirectTo = S3CatalogManager.theManager().getCatalogServiceContext() + relativeUrl + "catalog.xml";
            res.sendRedirect(Scrub.urlContent(redirectTo));
            _log.debug("redirectToCatalog() Redirected request for node to THREDDS catalog: {}", redirectTo);
            return true;
        }
        return false;
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {
        Procedure tKey = Timer.start();
        int status = HttpServletResponse.SC_OK;
        try {
            LogUtil.logServerAccessStart(request, LogUtil.S3_SERVICE_ACCESS_LOG_ID, "HTTP-GET", Integer.toString(_reqNumber.incrementAndGet()));
            if (!redirectToCatalog(request, response)) {  // Do we send the catalog redirect?
                String requestURI = request.getRequestURI();
                String catalogServiceContext = S3CatalogManager.theManager().getCatalogServiceContext();
                String dapServiceContext = S3CatalogManager.theManager().getDapServiceContext();
                if (requestURI.startsWith(catalogServiceContext)) {
                    if (!directoryDispatch(request, response)) {  // Is it a catalog request?
                        // We don't know how to cope, looks like it's time to 404!
                        status = HttpServletResponse.SC_NOT_FOUND;
                        response.sendError(status, "Unable to locate requested catalog.");
                        _log.info("Sent 404 Response.");
                    }
                } else if (requestURI.startsWith(dapServiceContext)) {
                    if (!_s3DapDispatcher.requestDispatch(request, response, true)) { // Is it a DAP request?

                        // We don't know how to cope, looks like it's time to 404!
                        status = HttpServletResponse.SC_NOT_FOUND;
                        response.sendError(status, "Unable to locate requested DAP dataset.");
                        _log.info("Sent 404 Response.");
                    }
                }
                else {   // It's not a catalog request, and it's not a dap request, so:
                    // Looks like it's time to 404!
                    status = HttpServletResponse.SC_NOT_FOUND;
                    response.sendError(status, "Unable to locate requested resource.");
                    _log.info("Sent 404 Response.");
                }
            }
        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, this, response);
            } catch (Throwable t2) {
                _log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            RequestCache.closeThreadCache();
            LogUtil.logServerAccessEnd(status, LogUtil.S3_SERVICE_ACCESS_LOG_ID);
            Timer.stop(tKey);
        }
    }

    public long getLastModified(HttpServletRequest req) {


        Procedure tKey = Timer.start();
        RequestCache.openThreadCache();
        long reqno = _reqNumber.incrementAndGet();
        long lmt = new Date().getTime();

        LogUtil.logServerAccessStart(req, LogUtil.S3_SERVICE_LAST_MODIFIED_LOG_ID, "LastModified", Long.toString(reqno));
        try {
            String requestURI = req.getRequestURI();
            String catalogServiceContext = S3CatalogManager.theManager().getCatalogServiceContext();
            String dapServiceContext = S3CatalogManager.theManager().getDapServiceContext();


            if (requestURI.startsWith(catalogServiceContext)) {
                lmt = getCatalogLastModified(req);
            } else if (requestURI.startsWith(dapServiceContext)) {
                lmt = _s3DapDispatcher.getLastModified(req);
            }
            return lmt;
        }
        finally {
            Timer.stop(tKey);
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, LogUtil.S3_SERVICE_LAST_MODIFIED_LOG_ID);
        }
    }



    public void destroy() {
        _log.info("Destroy complete.");
    }


    private long getCatalogLastModified(HttpServletRequest req){

        String requestUrl = req.getRequestURL().toString();
        String relativeURL = ReqInfo.getLocalUrl(req);
        String indexId = getS3IndexId(req);
        S3Index s3i;
        boolean newIndex = false;

        s3i = S3CatalogManager.theManager().getIndex(indexId);
        if(s3i == null) {
            String bucketName = S3CatalogManager.theManager().getBucketName(relativeURL);
            String bucketContext = S3CatalogManager.theManager().getBucketContext(relativeURL);
            s3i = new S3Index(req, bucketContext, bucketName);
            newIndex = true;
            _log.debug("getCatalogLastModified() - Making new S3Index for '{}'",requestUrl);
        }
        else {
            _log.debug("getCatalogLastModified() - Retrieved memory cached S3Index for '{}'",requestUrl);
        }

        long lmt = s3i.getLastModified();
        if(lmt!=-1 && newIndex){
            S3CatalogManager.theManager().putIndex(s3i);
            _log.debug("getCatalogLastModified() - Cached (in memory) S3Index for '{}'", requestUrl);
        }
        _log.debug("getCatalogLastModified() - END ({})",lmt);
        return lmt;
    }


    /**
     * Performs dispatching for file requests. If a request is not for a
     * special service or an OPeNDAP service then we attempt to resolve the
     * request to a "file" located within the context of the data handler and
     * return it's contents.
     *
     * @param request      .
     * @param response     .
     * @return true if the request was serviced as a directory request, false
     *         otherwise.
     */
    private boolean directoryDispatch(HttpServletRequest request,
                                     HttpServletResponse response)  {
        _log.debug("directoryDispatch() - BEGIN");
        boolean handled = false;
        String requestUrl = request.getRequestURL().toString();
        String relativeUrl = ReqInfo.getLocalUrl(request);
        String indexId = getS3IndexId(request);

        String bucketContext = S3CatalogManager.theManager().getBucketContext(relativeUrl);
        String bucketName = S3CatalogManager.theManager().getBucketName(relativeUrl);

        S3Index s3i = S3CatalogManager.theManager().getIndex(indexId);
        boolean newIndex = false;
        if(s3i == null) {
            s3i = new S3Index(request,bucketContext, bucketName);
            newIndex = true;
            _log.debug("directoryDispatch() - Making new S3Index for '{}'",requestUrl);
        }
        else {
            _log.debug("directoryDispatch() - Retrieved cached S3Index for '{}'",requestUrl);
        }
        try {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            Element catalog = s3i.getThreddsCatalog();
            Document threddsDoc = new Document();

            HashMap<String,String> piMap = new HashMap<>( 2 );
            piMap.put( "type", "text/xsl" );
            piMap.put( "href", getServletContext().getContextPath() +"/xsl/threddsPresentation.xsl" );
            ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

            threddsDoc.addContent(pi);
            threddsDoc.setRootElement(catalog);
            response.setContentType("text/xml");
            xmlo.output(threddsDoc,response.getOutputStream());

            if(newIndex) {
                S3CatalogManager.theManager().putIndex(s3i);
                _log.debug("directoryDispatch() - Cached S3Index for '{}'",requestUrl);
            }
            handled = true;
        } catch (IOException e) {
            _log.error("IOException - Unable to access s3 object: {} Msg: {}",s3i.getResourceUrl(),e.getMessage());
        } catch (JDOMException e) {
            _log.error("JDOMException - Unable to parse s3 Index: {} Msg: {}", s3i.getResourceUrl(), e.getMessage());
        }
        _log.debug("directoryDispatch() - END ({})", handled);
        return handled;
    }



    private  String getS3IndexId(HttpServletRequest request){
        StringBuilder sb = new StringBuilder();
        String collectionName = getCollectionName(request);
        sb.append(collectionName).append(S3Index.getCatalogIndexString());
        return sb.toString();
    }



    private  String getCollectionName(HttpServletRequest request){

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String collectionName  = Scrub.urlContent(relativeUrl);
        if(collectionName.endsWith("/contents.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("contents.html"));
        }
        else if(collectionName.endsWith("/catalog.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("catalog.html"));
        }
        else if(collectionName.endsWith("/catalog.xml")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("catalog.xml"));
        }
        if(!collectionName.endsWith("/"))
            collectionName += "/";

        while(collectionName.startsWith("//"))
            collectionName = collectionName.substring(1);

        _log.debug("getCollectionName() returning  "+collectionName);
        return collectionName;
    }
}
