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
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.SAXBuilder;

import java.io.*;

/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 4:54:27 PM
 */
public class Catalog {



    private  Logger log;
    private  String path;
    private  byte[] _buffer;
    private String configFileName;

    private String name;

    public Catalog(String fname, boolean cacheFile) throws Exception{

        log = org.slf4j.LoggerFactory.getLogger(RootCatalog.class);

        log.debug("Configuring...");

        configFileName = fname;

        String msg;

        File catalogFile = new File(fname);

        if(!catalogFile.exists()){
            msg = "Cannot find file: "+ fname;
            log.error(msg);
            throw new IOException(msg);
        }

        if(!catalogFile.canRead()){
            msg = "Cannot read file: "+ fname;
            log.error(msg);
            throw new IOException(msg);
        }

        if(cacheFile)
            cacheCatalogFileContent();
        else
            _buffer = null;


        Document catalog  = getCatalogDocument();

        Element ce = catalog.getRootElement();

        name = ce.getAttributeValue("name");

        if(name==null){
            log.error("THREDDS ERROR: <catalog> element missing \"name\" attribute.");
            throw new Exception("THREDDS ERROR: <catalog> element missing \"name\" attribute.");
        }

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(catalog, System.out);



    }


    public void cacheCatalogFileContent() throws Exception {

        String msg;

        File catalogFile = new File(configFileName);

        if(!catalogFile.exists()){
            msg = "Cannot find file: "+ configFileName;
            log.error(msg);
            throw new IOException(msg);
        }

        if(!catalogFile.canRead()){
            msg = "Cannot read file: "+ configFileName;
            log.error(msg);
            throw new IOException(msg);
        }
        log.debug("Loading THREDDS catalog file: "+ configFileName);

        FileInputStream fis = new FileInputStream(configFileName);

        byte[] buf  = new byte[(int)catalogFile.length()];

        int count = 0, ret;

        while(count<catalogFile.length()){
            ret = fis.read(buf);
            if(ret<0){
                log.error("Premature end of file reached. file: "+ configFileName);
                throw new Exception("Premature end of file reached.");
            }
            count += ret;
        }
        fis.close();

    }



    public Document getCatalogDocument() throws Exception {

        InputStream is;

        if(_buffer!=null){
             is= new ByteArrayInputStream(_buffer);
        }
        else {
             is = new FileInputStream(configFileName);
        }

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();

        return  sb.build(is);
    }

    public String getName(){
        return name;
    }






}
