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
package opendap.wcs.gatewayClient;

import org.jdom.Element;
import org.jdom.Attribute;
import org.slf4j.Logger;

import java.util.*;

/**
 *
 * A Project is the top level organization.
 * Each Project instance should have one or more Sites.
 *
 * @see opendap.wcs.gatewayClient.Site
 */
public class Project {

    private Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    private Element config;
    private boolean isThreddsCatalog;

    HashMap<String,Site> siteMapper;
    Vector<Site> siteList;


    Vector<Element> threddsRootCatalogs;


    public Project(Element configuration) throws Exception{

        log.debug("Configuring...");

        siteMapper = new HashMap<String,Site>();
        siteList = new Vector<Site>();
        isThreddsCatalog = false;

        config = (Element) configuration.clone();

        config();

        log.debug(this.toString());

    }

    private void config() throws Exception{

        Attribute attr;
        Element elm;
        List sites;
        Site site;
        Iterator i;

        if(!config.getName().equals("Project"))
            throw new Exception("Cannot build a "+getClass()+" using " +
                    "<"+config.getName()+"> element.");

        attr = config.getAttribute("name");
        if(attr==null)
            throw new Exception("Missing \"name\" attribute. " +
                    "Project elements must have a name attribute.");
        log.debug("name: "+attr.getValue());


        sites = config.getChildren("Site");

        if(sites.isEmpty())
            throw new Exception("Invalid Configuration. " +
                    "Project elements must contain (1 or more <Site> elements) " +
                    "OR (1 or more<ThreddsCatalogRoot> elements.");


        i = sites.iterator();

        while(i.hasNext()){
            elm = (Element)i.next();

            site = new Site(elm);
            siteMapper.put(site.getName(),site);
            siteList.add(site);
        }



    }






    public int getSize(){
        return siteMapper.size();
    }

    public String getName(){
        return config.getAttributeValue("name");
    }


    public Site getSite(String name){
        return siteMapper.get(name);
    }


    public Vector<Site> getSites(){
        return siteList;
    }

    public  String[] getSiteNames(){

        String[] names = new String[siteList.size()];
        int j=0;

        for(Site site : siteList){
            names[j++] = site.getName();
        }

        return names;
    }

}
