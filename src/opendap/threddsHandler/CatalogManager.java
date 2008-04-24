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

import org.slf4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;


/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 3:55:43 PM
 */
public class CatalogManager {


    private static Logger log;
    private static String contentPath;
    private static Document config;



    private static Vector<Catalog> rootCatalogs = new Vector<Catalog>();

    private static HashMap<String,Catalog> catalogs = new HashMap<String,Catalog>();
    private static boolean isIntialized=false;


    public static void init(String contentPath){

        if(isIntialized) return;


        log = org.slf4j.LoggerFactory.getLogger(CatalogManager.class);

        log.debug("Configuring...");

        if(isIntialized){
            log.error(" Configuration has already been done. isInitialized(): "+isIntialized);
            return;
        }

        CatalogManager.contentPath = contentPath;
        isIntialized = true;
    }


    public static void addRootCatalog(String pathPrefix,String urlPrefix, String fname,
                                  boolean cacheCatalogFileContent)
            throws Exception {

        Catalog catalog = new Catalog(pathPrefix,urlPrefix,fname,cacheCatalogFileContent);
        rootCatalogs.add(catalog);
        addCatalog(catalog,cacheCatalogFileContent);
    }



    public static Document getTopLevelCatalogDocument() {

        Element catRef, catalogRoot;
        String href, title, name;

        Document catalog = new Document(new Element(THREDDS.CATALOG));
        catalogRoot = catalog.getRootElement();
        catalogRoot.setNamespace(Namespace.getNamespace(THREDDS.NAMESPACE_STRING));
        catalogRoot.addNamespaceDeclaration(XLINK.NS);
        catalogRoot.setAttribute(THREDDS.NAME, "HyraxThreddsHandler");


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


        return catalog;
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
        for (Object o : catDoc.getRootElement().getChildren(THREDDS.CATALOG_REF,THREDDS.NS)) {
            catRef = (Element) o;

            href = catRef.getAttributeValue(XLINK.HREF,XLINK.NS);
            thisUrlPrefix = catalog.getUrlPrefix() + href.substring(0,href.length() - Util.basename(href).length());

            thisPathPrefix = catalog.getPathPrefix() + href;
            catFname = Util.basename(thisPathPrefix);
            thisPathPrefix = thisPathPrefix.substring(0,thisPathPrefix.lastIndexOf(catFname));

            Catalog thisCatalog = new Catalog(thisPathPrefix,thisUrlPrefix,catFname,cacheCatalogFileContent);
            addCatalog(thisCatalog,cacheCatalogFileContent);
        }
    }




    public static Catalog getCatalog(String name){

        return catalogs.get(name);

    }




    public static void destroy(){

        log.debug("Destroyed");

    }

}
