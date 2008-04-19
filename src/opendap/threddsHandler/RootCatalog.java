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
package opendap.thredds;

import org.slf4j.Logger;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.HashMap;


/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 3:55:43 PM
 */
public class RootCatalog {


    private static Logger log;
    private static String contentPath;
    private static Document config;




    private static HashMap<String,Catalog> catalogs = new HashMap<String,Catalog>();
    private static boolean isIntialized=false;


    public static void init(String contentPath){

        if(isIntialized) return;


        log = org.slf4j.LoggerFactory.getLogger(RootCatalog.class);

        log.debug("Configuring...");

        if(isIntialized){
            log.error(" Configuration has already been done. isInitialized(): "+isIntialized);
            return;
        }

        RootCatalog.contentPath = contentPath;
        isIntialized = true;
    }




    public static void addCatalog(String fname,
                                  boolean cacheCatalogFileContent)
            throws Exception {

        Catalog catalog = new Catalog(fname, cacheCatalogFileContent);

        catalogs.put(catalog.getName(),catalog);

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(config, System.out);

        //configure();

        //confFilename = fname;



    }



    public Catalog getCatalog(String name){

        return catalogs.get(name);

    }




    public static void destroy(){

        log.debug("Destroyed");

    }

}
