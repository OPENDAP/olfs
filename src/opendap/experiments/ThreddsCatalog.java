/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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
package opendap.experiments;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;

/**
 * User: ndp
 * Date: Mar 11, 2008
 * Time: 6:07:40 PM
 */


public class  ThreddsCatalog {
    Document catalog;

    HashMap<String,Element> childDatasets;

    public ThreddsCatalog(String filename){


        try {

            Element catalog = loadCatalog(filename);

            childDatasets = new HashMap<String,Element>();

            loadAllDatasets(catalog,getPath(filename),childDatasets);


        }
        catch(Exception e){
            e.printStackTrace(System.out);
        }

    }



    public int size(){
        return childDatasets.size();
    }








    public static String getPath(String filename){
        String path="";


        if(filename.contains("/")){
            path = filename.substring(0,filename.lastIndexOf("/"));
        }

        if(!path.endsWith("/"))
            path += "/";


        System.out.println("    filename: "+filename+"\n    path: "+path);


        return path;

    }



    public static Element loadCatalog(String fullFileName){


        File catalogFile = new File(fullFileName);

        if(!catalogFile.exists()){
            System.out.println("FAIL: The file " + fullFileName + " does not exist");
            return null;
        }

        if(!catalogFile.canRead()){
            System.out.println("FAIL: Cannot read the file: " + fullFileName );
            return null;
        }

        try {


            System.out.println("Loading THREDDS catalog file: "+fullFileName);


            FileInputStream fis = new FileInputStream(catalogFile);

            // Parse the XML doc into a Document object.
            SAXBuilder sb = new SAXBuilder();
            Document catalog  = sb.build(fis);
            fis.close();

            return catalog.getRootElement();

        }
        catch(Exception e){
            e.printStackTrace(System.out);
        }


        return null;



    }



    static ElementFilter datasetFilter = new ElementFilter("dataset");
    static ElementFilter datasetScanFilter = new ElementFilter("datasetScan");
    static ElementFilter catalogRefFilter = new ElementFilter("catalogRef");
    static ElementFilter propertyFilter = new ElementFilter("property");

    int node_count=0;


    static String xlinkNameSpace="http://www.w3.org/1999/xlink";
    static Namespace xlink = Namespace.getNamespace(xlinkNameSpace);

    public static void loadAllDatasets(Element catalog, String path, HashMap<String,Element> childDatasets){

        System.out.println("loadAllDatasets: ");
        System.out.println("    path: "+path);


        Iterator i;

        cacheChildDatasets(catalog,path,childDatasets);


        i = catalog.getDescendants(catalogRefFilter);
        Element thisCatalogRef, thisCatalog;
        Attribute name, href, title;
        String nameValue, hrefValue, titleValue, filename;

        while(i.hasNext()){
            thisCatalogRef = (Element)i.next();
            name = thisCatalogRef.getAttribute("name");
            if(name!=null)
                nameValue = name.getValue();
            else
                nameValue = null;

            href = thisCatalogRef.getAttribute("href",xlink);
            if(href!=null)
                hrefValue = href.getValue();
            else
                hrefValue = null;

            title = thisCatalogRef.getAttribute("title",xlink);
            if(title!=null)
                titleValue = title.getValue();
            else
                titleValue = null;

            filename = path+hrefValue;



            System.out.println("catalogRef: "+path);
            System.out.println("    nameValue:   "+nameValue);
            System.out.println("    hrefValue:   "+hrefValue);
            System.out.println("    titleValue:  "+titleValue);
            System.out.println("    path:        "+path);
            System.out.println("    filename:    "+filename);


            thisCatalog = loadCatalog(filename);

            if(thisCatalog != null)
                loadAllDatasets(thisCatalog, getPath(filename), childDatasets);







        }


    }








    public static void cacheChildDatasets(Element catalog,
                                   String path,
                                   HashMap<String,Element> childDatasets){
        Iterator i = catalog.getDescendants(datasetFilter);
        Element thisDataset;
        Attribute urlPath;
        String urlPathValue;

        while(i.hasNext()){
            thisDataset = (Element)i.next();
            urlPath = thisDataset.getAttribute("urlPath");
            if(urlPath!=null){
                urlPathValue = urlPath.getValue();
                //System.out.println("Adding dataset with key: "+urlPathValue);
                childDatasets.put(urlPathValue,thisDataset);
            }
        }

    }










}


