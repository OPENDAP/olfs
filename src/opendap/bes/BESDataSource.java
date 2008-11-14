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

package opendap.bes;

import opendap.coreServlet.DataSourceInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;

import java.util.Date;
import java.text.SimpleDateFormat;


/**
 * User: ndp
 * Date: Nov 15, 2006
 * Time: 7:10:51 PM
 */
public class BESDataSource implements DataSourceInfo {

    private String BESDateFormat = "yyyy-MM-dd HH:mm:ss";

    private boolean exists;
    private boolean accessible;
    private boolean collection;
    private boolean data;

    private String name;
    private long size;
    private Date lastModified;

    private String requestedDataSource;




    BESDataSource(String dataSourceName) throws Exception {

        Logger log = org.slf4j.LoggerFactory.getLogger(getClass());


        requestedDataSource = dataSourceName;
        exists              = false;
        accessible          = false;
        collection          = false;
        data                = false;
        name                = null;
        size                = -1;
        lastModified        = null;

        Document info = new Document();




        if(BesXmlAPI.getInfo(dataSourceName,info)){

            exists      = true;
            accessible  = true;
           
            Element dataset = info.getRootElement().getChild("response").getChild("dataset");

            String isCollection = dataset.getAttributeValue("thredds_collection");
            collection = isCollection == null || isCollection.equalsIgnoreCase("true");
            String isData = dataset.getAttributeValue("isData");
            data = isData == null || isData.equalsIgnoreCase("true");


            name = dataset.getChildText("name");
            String s = dataset.getChildText("size");
            size = Long.valueOf(s);

            SimpleDateFormat sdf = new SimpleDateFormat(BESDateFormat);


            lastModified = sdf.parse(dataset.getChild("lastmodified").getChildTextTrim("date") + " " +
                                     dataset.getChild("lastmodified").getChildTextTrim("time"));

        }
        else {

            BESError err = new BESError(info);

            exists        = !err.notFound();
            accessible    = !err.forbidden();
            collection    = false;
            data          = false;
            name          = null;
            size          = -1;
            lastModified  = null;

            log.debug("BES request for info document for: \""+dataSourceName+"\" returned an error");

        }




    }

    public  boolean sourceExists(){
        return exists;
    }

    public  boolean sourceIsAccesible(){
        return accessible;
    }


    public  boolean isCollection(){
        return collection;
    }

    public  boolean isDataset(){
        return data;
    }

    public String getName(){
        return name;
    }

    public long getSize(){
        return size;
    }

    public Date getLastModfiedDate(){
        return lastModified;
    }

    public long lastModified(){
        return lastModified.getTime();
    }

    public String getRequestedDataSource(){
        return requestedDataSource;
    }


    public String toString(){
        String s = "BESDataSource("+requestedDataSource+"):\n";

        s += "    exists:        "+exists+"\n";
        if(exists){
            s += "    name:          " + name         + "\n";
            s += "    isCollection:  " + collection   + "\n";
            s += "    isDataset:     " + data         + "\n";
            s += "    size:          " + size         + "\n";
            s += "    lastModified:  " + lastModified + "\n";
        }
        return s;
    }



}
