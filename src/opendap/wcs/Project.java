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
package opendap.wcs;

import org.jdom.Element;
import org.jdom.Attribute;
import org.slf4j.Logger;

import java.util.*;

/**
 * User: ndp
 * Date: Mar 13, 2008
 * Time: 2:03:01 PM
 */
public class Project {

    private Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    private Element config;

    HashMap<String,Site> sites;


    public Project(Element configuration) throws Exception{

        log.debug("Configuring...");

        sites = new HashMap<String,Site>();

        config = (Element) configuration.clone();

        config();

        System.out.println(this);

    }

    private void config() throws Exception{

        Attribute attr;
        Element elm;
        List k;
        Site site;

        if(!config.getName().equals("Project"))
            throw new Exception("Cannot build a "+getClass()+" using " +
                    "<"+config.getName()+"> element.");

        attr = config.getAttribute("name");
        if(attr==null)
            throw new Exception("Missing \"name\" attribute. " +
                    "WCSService elements must have a name attribute.");
        log.debug("name: "+attr.getValue());


        k = config.getChildren("Site");

        if(k.isEmpty())
            throw new Exception("Missing <Site> element. " +
                    "<WCSProject name=\""+getName()+"\"> elements must have a " +
                    "<Site> element.");

        Iterator i = k.iterator();

        while(i.hasNext()){
            elm = (Element)i.next();

            site = new Site(elm);
            sites.put(site.getName(),site);
        }




    }

    public int getSize(){
        return sites.size();
    }

    public String getName(){
        return config.getAttributeValue("name");
    }


    public Site getSite(String name){
        return sites.get(name);
    }


    public Collection<Site> getSites(){
        return sites.values();
    }
    public  String[] getSiteNames(){

        Collection<Site> sc = sites.values();

        String[] names = new String[sites.size()];

        int j=0;
        for (Site site : sc) {
            names[j++] = site.getName();
        }

        return names;
    }

}
