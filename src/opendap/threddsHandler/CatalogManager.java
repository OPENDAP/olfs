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
import opendap.PathBuilder;
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Scrub;
import opendap.namespaces.THREDDS;
import opendap.ncml.NcmlManager;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;


/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 3:55:43 PM
 */
public class CatalogManager {


    private static Logger _log;
    private static String _contentPath;
    private static String _catalogIngestTransformFilename;
    private static String _besCatalogToThreddsCatalogTransformFilename;

    private static BesApi _besApi;


    private static ConcurrentHashMap<String, DatasetScan>   _datasetScans = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Catalog>       _catalogs     = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String[]>      _children     = new ConcurrentHashMap<>();

    private static ReentrantLock _catalogLock  = new ReentrantLock();


    private static boolean _isInitialized = false;


    public static final String DEFAULT_CATALOG_NAME = "catalog.xml";


    public static void init(String contentPath,
                            String catalogIngestTransformFilename,
                            String besCatalogToThreddsCatalogTransformFilename,
                            BesApi besApi) {

        _log = org.slf4j.LoggerFactory.getLogger(CatalogManager.class);

        _log.debug("Configuring...");

        if (_isInitialized) {
            _log.error(" Configuration has already been done. isInitialized(): " + _isInitialized);
            return;
        }

        _contentPath = contentPath;
        _catalogIngestTransformFilename = catalogIngestTransformFilename;
        _besCatalogToThreddsCatalogTransformFilename = besCatalogToThreddsCatalogTransformFilename;
        _besApi = besApi;
        _isInitialized = true;
    }

    public static void addCatalog(String pathPrefix,
                                  String urlPrefix,
                                  String fname,
                                  boolean cacheCatalogFileContent)
            throws Exception {

        LocalFileCatalog catalog = null;

        try {
             catalog = new LocalFileCatalog(pathPrefix, urlPrefix, fname, _catalogIngestTransformFilename, cacheCatalogFileContent);
        }
        catch (Exception e){
            _log.error("Failed to build catalog from file: "+fname);
        }

        if(catalog !=null){
            try{
                _catalogLock.lock();
                _log.debug("addCatalog(): Catalog locked.");

                addCatalog(catalog, cacheCatalogFileContent);
            }
            finally {
                _catalogLock.unlock();
                _log.debug("addCatalog(): Catalog unlocked.");

            }
        }

    }


    private static void addCatalog(Catalog catalog,
                                   boolean cacheCatalogFileContent)
            throws Exception {


        String catalogKey = catalog.getCatalogKey();

        // If this catalog has already been added,  then don't mess with it.
        if (_catalogs.containsKey(catalogKey)){
            _log.warn("The catalog '"+catalogKey+"' is already in the collection. It must be removed (purgeCatalog()) " +
                    "before it can be added again.");
            return;
        }


        if (_children.containsKey(catalogKey)) {
            String msg = "addCatalog() Invalid State! Although the list of catalogs does not contain a " +
                    "reference to the catalog '" + catalogKey + "' the list of children does!!!";
            _log.error(msg);
            throw new Exception(msg);
        }


        Document catDoc = catalog.getRawCatalogDocument();
        if(catDoc==null){
            String msg = "FAILED to get catalog Document object for the catalog associated with file "+catalog.getFileName()+"'";
            _log.error("addCatalog() - {}", msg);
            throw new BadConfigurationException(msg);
        }
        _log.debug("addCatalog() - Loaded Catalog document: \n{}",new XMLOutputter(Format.getPrettyFormat()).outputString(catDoc));

        Element catRef;
        String href, catFname, thisUrlPrefix, thisPathPrefix;


        // Get all of the catalogRef elements in the  catalog document.
        Iterator i = catDoc.getRootElement().getDescendants(new ElementFilter(THREDDS.CATALOG_REF, opendap.namespaces.THREDDS.NS));

        Vector<String> catalogChildren = new Vector<String>();
        while (i.hasNext()) {

            // For each one of them...
            catRef = (Element) i.next();

            // get the URL of the catalog.
            href = catRef.getAttributeValue(XLINK.HREF, XLINK.NS);

            if (href.startsWith("http://") || href.startsWith("https://")) {
                _log.info("Found catalogRef that references an external " +
                        "catalog: '"+href+"' Target catalog not processed. The catalogRef element " +
                        "will remain in the catalog and will not be cached.");
                // @todo Add remote catalog caching support?
            } else if (href.startsWith("/")) {
                _log.info("Found thredds:catalogRef whose xlink:href attribute " +
                        "begins with a \"/\" character: '" + href +"' "+
                        "This may mean that the catalog is pointing " +
                        "to another catalog service. Also, it is not an href " +
                        "expressed in terms of the relative content path. " +
                        "Target catalog not processed as a file. " +
                        "The catalogRef element " +
                        "will remain in the catalog. This will allow it to " +
                        "appear correctly in thredds catalog output. But it's contents " +
                        "will not be cached.");
                // @todo Add support for catalog caching within the local server? Mabye not.
            } else {

                // Since it's not a remote catalog, or an absolute path (starting with '/') then
                // we will conclude that it is a static THREDDS catalog file. Let's slurp it up into
                // a LocalFileCatalog object.

                thisUrlPrefix = catalog.getUrlPrefix() + href.substring(0, href.length() - Util.basename(href).length());

                thisPathPrefix = catalog.getPathPrefix() + href;
                catFname = Util.basename(thisPathPrefix);
                thisPathPrefix = thisPathPrefix.substring(0, thisPathPrefix.lastIndexOf(catFname));

                LocalFileCatalog thisCatalog = null;
                try {
                    thisCatalog = new LocalFileCatalog(thisPathPrefix,
                        thisUrlPrefix,
                        catFname,
                        _catalogIngestTransformFilename,
                        cacheCatalogFileContent);

                    addCatalog(thisCatalog, cacheCatalogFileContent);
                    String thisCatalogIndex = thisCatalog.getCatalogKey();
                    catalogChildren.add(thisCatalogIndex);

                }
                catch( Exception e){
                    _log.error("addCatalog() - Failed to build catalog. Dropping. File: "+thisPathPrefix + catFname+ " Msg: "+e.getMessage());
                }



            }
        }

        if (!catalogChildren.isEmpty()) {
            String[] s = new String[catalogChildren.size()];
            _children.put(catalogKey, catalogChildren.toArray(s));
        }


        /** ###############################################################################
         *
         * Ingest datasetScan Elements..
         *
         *
         *
         */

         Document rawCatalog = catalog.getRawCatalogDocument();


         // Get all of the datasetScan elements in the  catalog document.
         i = rawCatalog.getRootElement().getDescendants(new ElementFilter(THREDDS.DATASET_SCAN, THREDDS.NS));


        while (i.hasNext()) {
            // For each one of them...
            Element dssElem = (Element) i.next();

            addDatasetScan(catalog,dssElem);
        }

        /** ###############################################################################  */




        _log.debug("Ingesting inherited metadata (if any) for catalog '"+catalog.getName()+"'");
        InheritedMetadataManager.ingestInheritedMetadata(catalog);

        _log.debug("Ingesting NcML datasets (if any) for catalog '"+catalog.getName()+"'");
        NcmlManager.ingestNcml(catalog);

        _catalogs.put(catalogKey, catalog);


    }



    private static void addDatasetScan(Catalog catalog,Element dssElem) throws BadConfigurationException{


        DatasetScan ds = new DatasetScan(catalog,  dssElem,  _besCatalogToThreddsCatalogTransformFilename, _besApi);


        PathBuilder pb = new PathBuilder();

        pb.append(catalog.getPathPrefix()).append(ds.getPath());
        _datasetScans.put(pb.toString(),ds);


    }





    public static Catalog getCatalog(String catalogKey) throws JDOMException, BadConfigurationException, PPTException, IOException, SaxonApiException, BESError {
        Catalog cat = getCatalogAndUpdateIfRequired(catalogKey);

        if(cat == null){

            Catalog datasetScanCatalog = (Catalog) RequestCache.get(catalogKey);

            if(datasetScanCatalog == null ){

                DatasetScan datasetScan = null;
                for(DatasetScan ds : _datasetScans.values()) {
                    if(ds.matches(catalogKey)){

                        _log.info("Found DatasetScan matching catalogKey '{}' datasetScan: \n'{}'",catalogKey,ds);
                        if(datasetScan==null){
                            datasetScan = ds;
                        }

                    }

                }
                if(datasetScan==null)
                    return null;


                datasetScanCatalog = datasetScan.getCatalog(catalogKey);
                if(datasetScanCatalog==null){
                    String msg = "FAILED to retrieve the catalog identified by the key '{}' from the DatasetScan instance "+datasetScan.getName();
                    _log.error("getCatalog() - {}",msg);
                    throw new BadConfigurationException(msg);
                }
                RequestCache.put(catalogKey,datasetScanCatalog);

            }

            return datasetScanCatalog;

        }
        return cat;



    }

    public static long getLastModified(String catalogKey) throws JDOMException, BadConfigurationException, PPTException, IOException {

        Catalog cat;

        try {
            cat = getCatalog(catalogKey);
            if (cat != null)
                return cat.getLastModified();
        }
        catch (SaxonApiException | BESError e) {
            _log.info("Failed to retrieve catalog: {}  (msg: {})",catalogKey, e.getMessage());
        }
        return new Date().getTime();
    }

    /**
     *
     * @param catalogKey   Is the    catalogKeyIntoThe
     * @return
     */
    private static Catalog getCatalogAndUpdateIfRequired(String catalogKey) {


        if (catalogKey == null)
            return null;

        try {
            _catalogLock.lock();
            _log.debug("getCatalogAndUpdateIfRequired(): Catalog locked.");

            Catalog c = _catalogs.get(catalogKey);
            if (c == null)
                return null;

            if (c.needsRefresh()) {


                _log.debug("getCatalogAndUpdateIfRequired(): Catalog '" + catalogKey + "' needs to be updated.");

                LocalFileCatalog newCat;
                try {
                    newCat = new LocalFileCatalog(c.getPathPrefix(), c.getUrlPrefix(), c.getFileName(), c.getIngestTransformFilename(), c.usesMemoryCache());

                    //Thread.sleep(10000);

                    _log.debug("getCatalogAndUpdateIfRequired(): Purging catalog '" + catalogKey +
                            "' and it's children from catalog collection.");

                    purgeCatalog(catalogKey);


                    _log.debug("getCatalogAndUpdateIfRequired(): Adding new catalog for catalogKey " +
                            newCat.getCatalogKey() + " to _catalogs collection.");

                    addCatalog(newCat, newCat.usesMemoryCache());

                    return newCat;
                }
                catch (Exception e) {
                    _log.error("getCatalogAndUpdateIfRequired(): Could not update Catalog: " + c.getName()+ "Msg: "+e.getMessage());
                    return null;
                }
            } else {
                _log.debug("getCatalogAndUpdateIfRequired(): Catalog '" + catalogKey + "' does NOT need updated.");
                return c;

            }
        }
        finally {
            _catalogLock.unlock();
            _log.debug("getCatalogAndUpdateIfRequired(): Catalog unlocked.");
        }


    }



    /**
     * Purges the  THREDDS catalog connected graph from the system, starting at the catalog associated with the
     * passed catalogKey.
     *
     * @param catalogKey
     */
    private static void purgeCatalog(String catalogKey) {
        Catalog catalog;
        String childCatalogKeys[];


        if (catalogKey != null) {

            _log.debug("purgeCatalog(): Removing catalog: " + catalogKey);
            catalog = _catalogs.remove(catalogKey);

            if (catalog == null) {
                _log.warn("purgeCatalog(): Catalog '" + Scrub.urlContent(catalogKey) + "' not in catalog collection!!");
            }

            childCatalogKeys = _children.get(catalogKey);
            if (childCatalogKeys != null) {

                _log.debug("purgeCatalog(): Purging the childCatalogKeys of catalog: " + catalogKey);

                for (String childCatalogKey : childCatalogKeys) {
                    purgeCatalog(childCatalogKey);
                }

                _children.remove(catalogKey);
            } else {
                _log.info("purgeCatalog(): Catalog '" + Scrub.urlContent(catalogKey) + "' has no childCatalogKeys.");
            }

            _log.debug("purgeCatalog(): Purging inherited metadata (if any) for catalogKey: " +catalogKey);
            InheritedMetadataManager.purgeInheritedMetadata(catalogKey);

            _log.debug("purgeCatalog(): Purging NcML data sets (if any) for catalogKey: " +catalogKey);
            NcmlManager.purgeNcmlDatasets(catalog);


            // catalog.destroy();
            _log.debug("purgeCatalog(): Purged catalog: " + catalogKey);


        }


    }


    public static void destroy() {

        for (Catalog c : _catalogs.values()) {
            c.destroy();
        }
        _catalogs.clear();
        _children.clear();
        _log.debug("Destroyed");

    }


    public String toString() {
        StringBuilder s = new StringBuilder("THREDDS Catalog Manager:\n");

        s.append("    ContentPath: " + _contentPath + "\n");

        for (Catalog c : _catalogs.values()) {
            s.append("    Catalog Name: ").append(c.getName()).append("\n");
            s.append("        file:        ").append(c.getFileName()).append("\n");
            s.append("        pathPrefix:  ").append(c.getPathPrefix()).append("\n");
            s.append("        urlPrefix:   ").append(c.getUrlPrefix()).append("\n");
        }

        return s.toString();
    }


    //private static Vector<Catalog> rootCatalogs = new Vector<Catalog>();
    //private static Document config;

    /*
    public static void addRootCatalog(String pathPrefix,String urlPrefix, String fname,
                                  boolean cacheCatalogFileContent)
            throws Exception {

        Catalog catalog = new Catalog(pathPrefix,urlPrefix,fname,cacheCatalogFileContent);

        ReentrantReadWriteLock.WriteLock lock = _catalogsLock.writeLock();
        try {
            lock.lock();
            rootCatalogs.add(catalog);
            addCatalog(catalog,cacheCatalogFileContent);
        }
        finally {
            lock.unlock();
        }

    }
    */


    /*
    public static Document getTopLevelCatalogDocument() {

        Element catRef, catalogRoot;
        String href, title, name;

        Document catalog = new Document(new Element(THREDDS.CATALOG));
        catalogRoot = catalog.getRootElement();
        catalogRoot.setNamespace(Namespace.getNamespace(THREDDS.BES_NAMESPACE_STRING));
        catalogRoot.addNamespaceDeclaration(XLINK.NS);
        catalogRoot.setAttribute(THREDDS.NAME, "HyraxThreddsHandler");

        // We only need a read lock here because we are NOT going to reread
        // our configuration. So - All of these top level _catalogs can't change.
        // Their content can change, but we can't add or remove from the list.
        // If one of them has a change, then that will get loaded when the
        // changed catalog gets accessed.
        ReentrantReadWriteLock.ReadLock lock = _catalogsLock.readLock();
        try {
            lock.lock();
            for (Catalog cat : rootCatalogs) {
                catRef = new Element(THREDDS.CATALOG_REF,THREDDS.NS);

                href = cat.getUrlPrefix() + cat.getFileName();
                catRef.setAttribute(XLINK.HREF,href,XLINK.NS);

                title = cat.getName();
                catRef.setAttribute(XLINK.TITLE,title,XLINK.NS);

                name = cat.getName();
                catRef.setAttribute(THREDDS.NAME,name);

                catalogRoot.addContent(catRef);
            }
        }
        finally {
            lock.unlock();
        }


        return catalog;
    }
    */

    /*
    public static XdmNode getTopLevelCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {

        XdmNode source;
        InputStream is;

        Document tlcat = getTopLevelCatalogDocument();

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        byte[] buffer = xmlo.outputString(tlcat).getBytes();

        is = new ByteArrayInputStream(buffer);
        _log.debug("getCatalogDocument(): Reading catalog from memory cache.");

        source = proc.newDocumentBuilder().build(new StreamSource(is));

        return source;
    }
    */


}
