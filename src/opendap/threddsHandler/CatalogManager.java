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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import opendap.namespaces.THREDDS;


/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 3:55:43 PM
 */
public class CatalogManager {


    private static Logger log;
    private static String contentPath;




    private static HashMap<String, Catalog> catalogs = new HashMap<String, Catalog>();
    private static ReentrantReadWriteLock _catalogsLock;
    private static boolean isIntialized=false;

    private static ReentrantReadWriteLock _myLock;


    public static ReentrantReadWriteLock getRWLock(){
        return _myLock;
    }



    public static void init(String contentPath){

        if(isIntialized) return;


        log = org.slf4j.LoggerFactory.getLogger(CatalogManager.class);

        log.debug("Configuring...");

        if(isIntialized){
            log.error(" Configuration has already been done. isInitialized(): "+isIntialized);
            return;
        }

        _myLock = new ReentrantReadWriteLock();
        _catalogsLock = new ReentrantReadWriteLock();
        CatalogManager.contentPath = contentPath;

        isIntialized = true;
    }



    public static void addCatalog(String pathPrefix,
                                  String urlPrefix,
                                  String fname,
                                  boolean cacheCatalogFileContent)
            throws Exception {

        LocalFileCatalog catalog = new LocalFileCatalog(pathPrefix,urlPrefix,fname,cacheCatalogFileContent);

        ReentrantReadWriteLock.WriteLock lock = _catalogsLock.writeLock();
        try {
            lock.lock();
            addCatalog(catalog,cacheCatalogFileContent);
        }
        finally {
            lock.unlock();
        }

    }


    private static void addCatalog(Catalog catalog,
                                  boolean cacheCatalogFileContent)
            throws Exception {


        String index;

        index = catalog.getUrlPrefix()+catalog.getFileName();

        catalogs.put(index,catalog);

        Document catDoc = catalog.getCatalogDocument();
        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        //xmlo.output(catDoc, System.out);

        Element catRef;
        String href, catFname, thisUrlPrefix, thisPathPrefix;
        Iterator i=  catDoc.getRootElement().getDescendants(new ElementFilter(THREDDS.CATALOG_REF, opendap.namespaces.THREDDS.NS));

        while(i.hasNext()){

            catRef = (Element) i.next();

            href = catRef.getAttributeValue(XLINK.HREF,XLINK.NS);

            if(href.startsWith("http://")){
                log.info("Found catalogRef that references an external " +
                        "catalog. Target catalog not processed. The catalogRef element " +
                        "will remain in the catalog and will not be cached.");
                // @todo Add remote catalog caching support?
            }
            else if(href.startsWith("/")) {
                log.info("Found thredds:catalogRef whose xlink:href attribute " +
                        "begins with a \"/\" character. " +
                        "This may mean that the catalog is pointing " +
                        "to another catalog service. Also, it is not an href " +
                        "expressed in terms of the relative content path. " +
                        "Target catalog not processed as a file. " +
                        "The catalogRef element " +
                        "will remain in the catalog. This will allow it to " +
                        "appear correctly in thredds catalog output. But it's contents " +
                        "will not be cached.");
                // @todo Add support for catalog caching within the local server? Mabye not.
            }
            else {


                thisUrlPrefix = catalog.getUrlPrefix() + href.substring(0,href.length() - Util.basename(href).length());

                thisPathPrefix = catalog.getPathPrefix() + href;
                catFname = Util.basename(thisPathPrefix);
                thisPathPrefix = thisPathPrefix.substring(0,thisPathPrefix.lastIndexOf(catFname));

                LocalFileCatalog thisCatalog = new LocalFileCatalog(thisPathPrefix,thisUrlPrefix,catFname,cacheCatalogFileContent);
                addCatalog(thisCatalog,cacheCatalogFileContent);
                catalog.addChild(thisCatalog);
            }
        }
    }


    public static Catalog getCatalog(String name){

        Catalog cat = null;

        ReentrantReadWriteLock.ReadLock lock = _catalogsLock.readLock();
        try {
            lock.lock();
            cat =  catalogs.get(name);
        }
        finally {
            lock.unlock();
        }
        cat = updateCatalogIfRequired(cat);

        return cat;

    }

    public static long getLastModified(String name){

        Catalog cat;

        ReentrantReadWriteLock.ReadLock lock = _catalogsLock.readLock();
        try {
            lock.lock();
            cat =  catalogs.get(name);
            if(cat!=null)
                return cat.getLastModified();
        }
        finally {
            lock.unlock();
        }

        return -1;
    }



    public static Catalog updateCatalogIfRequired(Catalog c){

        ReentrantReadWriteLock.WriteLock lock = _catalogsLock.writeLock();
        try {
            lock.lock();

            if(c!=null && c.needsRefresh()){
                LocalFileCatalog newCat;
                try {
                    newCat = new LocalFileCatalog(c.getPathPrefix(),c.getUrlPrefix(),c.getFileName(),c.usesMemoryCache());

                    /*
                    if(rootCatalogs.contains(c)){
                        log.debug("Removing "+c.getName()+" from rootCatalogs collection.");
                        rootCatalogs.remove(c);
                        log.debug("Adding new "+newCat.getName()+" to rootCatalogs collection.");
                        rootCatalogs.add(newCat);
                    }
                    */

                    log.debug("Purging "+c.getName());
                    purgeCatalog(c);
                    log.debug("Adding new catalog "+newCat.getName()+" to catalogs collection.");
                    addCatalog(newCat, newCat.usesMemoryCache());
                    return newCat;
                }
                catch(Exception e){
                    log.error("Could not update Catalog: "+c.getName());
                    return null;
                }
            }
            else {
                return c;

            }

        }
        finally {
            lock.unlock();
        }



    }
    private static void purgeCatalog(Catalog c){
         String index;
         Catalog child;
         Enumeration children;

         children = c.getChildren();

         while(children.hasMoreElements()){
             child = (Catalog) children.nextElement();
             purgeCatalog(child);
         }

        index = c.getUrlPrefix()+c.getFileName();
        catalogs.remove(index);
        c.destroy();
        log.debug("Purged Catalog: "+index);

    }






    public static void destroy(){

        log.debug("Destroyed");

    }


    public String toString(){
        String s = "THREDDS Catalog Manager:\n";

        s += "    ContentPath: " + contentPath + "\n";

        for(Catalog c: catalogs.values()){
            s += "    Catalog Name: "+c.getName() + "\n";
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
        // our configuration. So - All of these top level catalogs can't change.
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
        log.debug("getCatalogDocument(): Reading catalog from memory cache.");

        source = proc.newDocumentBuilder().build(new StreamSource(is));

        return source;
    }
    */


}
