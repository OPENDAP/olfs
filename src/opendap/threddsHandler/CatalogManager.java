/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2010 OPeNDAP, Inc.
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

import org.slf4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import opendap.namespaces.THREDDS;


/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 3:55:43 PM
 */
public class CatalogManager {


    private static Logger _log;
    private static String _contentPath;
    private static String _catalogIngestTransformFilename;


    private static ConcurrentHashMap<String, Catalog> _catalogs = new ConcurrentHashMap<String, Catalog>();
    private static ConcurrentHashMap<String, String[]> _children = new ConcurrentHashMap<String, String[]>();
    private static boolean _isIntialized = false;


    public static void init(String contentPath, String catalogIngestTransformFilename) {

        _log = org.slf4j.LoggerFactory.getLogger(CatalogManager.class);

        _log.debug("Configuring...");

        if (_isIntialized) {
            _log.error(" Configuration has already been done. isInitialized(): " + _isIntialized);
            return;
        }

        _contentPath = contentPath;
        _catalogIngestTransformFilename = catalogIngestTransformFilename;
        _isIntialized = true;
    }


    private static String getMapIndex(Catalog c) {
        String index = c.getUrlPrefix() + c.getFileName();
        return index;

    }


    public static void addCatalog(String pathPrefix,
                                  String urlPrefix,
                                  String fname,
                                  boolean cacheCatalogFileContent)
            throws Exception {

        LocalFileCatalog catalog = new LocalFileCatalog(pathPrefix, urlPrefix, fname, _catalogIngestTransformFilename, cacheCatalogFileContent);
        addCatalog(catalog, cacheCatalogFileContent);

    }


    private static void addCatalog(Catalog catalog,
                                   boolean cacheCatalogFileContent)
            throws Exception {


        String index;

        index = getMapIndex(catalog);

        // If if this catalog has already been added,  then don't mess with it.
        if (_catalogs.containsKey(index))
            return;


        if (_children.containsKey(index)) {
            String msg = "addCatalog() Invalid State! Although the list of catalogs does not contain a " +
                    "reference to the catalog '" + index + "' the list of children does!!!";
            _log.error(msg);
            throw new Exception(msg);
        }


        Document catDoc = catalog.getCatalogDocument();
        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        //xmlo.output(catDoc, System.out);

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

            if (href.startsWith("http://")) {
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

                // Since it's not a remote catalog, and an absolute path (starting with '/') then
                // we will conclude that it is a static THREDDS catalog file. Let's slurp it up into
                // a LocalFileCatalog object.

                thisUrlPrefix = catalog.getUrlPrefix() + href.substring(0, href.length() - Util.basename(href).length());

                thisPathPrefix = catalog.getPathPrefix() + href;
                catFname = Util.basename(thisPathPrefix);
                thisPathPrefix = thisPathPrefix.substring(0, thisPathPrefix.lastIndexOf(catFname));

                LocalFileCatalog thisCatalog = new LocalFileCatalog(thisPathPrefix,
                        thisUrlPrefix,
                        catFname,
                        _catalogIngestTransformFilename,
                        cacheCatalogFileContent);

                addCatalog(thisCatalog, cacheCatalogFileContent);
                String thisCatalogIndex = getMapIndex(thisCatalog);
                catalogChildren.add(thisCatalogIndex);

            }
        }

        if (!catalogChildren.isEmpty()) {
            String[] s = new String[catalogChildren.size()];
            _children.put(index, catalogChildren.toArray(s));
        }
        _catalogs.put(index, catalog);


    }


    public static Catalog getCatalog(String name) {

        Catalog cat = _catalogs.get(name);
        cat = updateCatalogIfRequired(cat);

        return cat;

    }

    public static long getLastModified(String name) {

        Catalog cat;

        cat = _catalogs.get(name);
        if (cat != null)
            return cat.getLastModified();

        return -1;
    }


    private static Catalog updateCatalogIfRequired(Catalog c) {


        String index = getMapIndex(c);
        if (c != null && c.needsRefresh()) {

            _log.debug("updateCatalogIfRequired(): Catalog '" + index + "' needs to be updated.");

            LocalFileCatalog newCat;
            try {
                newCat = new LocalFileCatalog(c.getPathPrefix(), c.getUrlPrefix(), c.getFileName(), c.getIngestTransformFilename(), c.usesMemoryCache());


                _log.debug("updateCatalogIfRequired(): Purging catalog '" + index + "' and it's children from catalog collection.");
                purgeCatalog(c);


                index = getMapIndex(newCat);
                _log.debug("updateCatalogIfRequired: Adding new catalog " + index + " to _catalogs collection.");
                addCatalog(newCat, newCat.usesMemoryCache());
                return newCat;
            }
            catch (Exception e) {
                _log.error("updateCatalogIfRequired: Could not update Catalog: " + c.getName());
                return null;
            }
        } else {
            return c;

        }


    }


    /**
     * Starting at the passed catalog purges the  THREDDS catalog connected graph from the system.
     *
     * @param c
     */
    private static void purgeCatalog(Catalog c) {
        Catalog child;
        String children[];


        if (c != null) {

            String index = getMapIndex(c);
            _log.debug("purgeCatalog(): Removing catalog: " + index);

            if (_catalogs.remove(index) == null) {
                _log.warn("purgeCatalog(): Catalog '" + index + "' not in catalog collection!!");
            }

            children = _children.get(index);
            if (children != null) {

                _log.debug("purgeCatalog(): Purging the children of catalog: " + index);

                for (String childIndex : children) {
                    child = _catalogs.get(childIndex);
                    purgeCatalog(child);

                }

                _children.remove(index);
            } else {
                _log.info("purgeCatalog(): Catalog '" + index + "' has no children.");
            }


            // c.destroy();
            _log.debug("purgeCatalog(): Purged catalog: " + index);


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
        String s = "THREDDS Catalog Manager:\n";

        s += "    ContentPath: " + _contentPath + "\n";

        for (Catalog c : _catalogs.values()) {
            s += "    Catalog Name: " + c.getName() + "\n";
            s += "        file:        " + c.getFileName() + "\n";
            s += "        pathPrefix:  " + c.getPathPrefix() + "\n";
            s += "        urlPrefix:   " + c.getUrlPrefix() + "\n";
        }

        return s;
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
