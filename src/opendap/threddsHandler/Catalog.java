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
import org.jdom.input.SAXBuilder;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;

import java.io.*;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

import javax.xml.transform.stream.StreamSource;

/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 4:54:27 PM
 */
public class Catalog {



    private  Logger log;
    private  String _name;
    private  String _pathPrefix;
    private  String _urlPrefix;
    private  String _fileName;
    private  byte[] _buffer;
    private boolean _cacheFile;
    private Vector<Catalog> _children;
    private Date _cacheTime;


    public Catalog( String pathPrefix,
                    String urlPrefix,
                    String fname,
                    boolean cacheFile) throws Exception{

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        log.debug("Configuring new "+getClass().getName());

        _fileName = fname;
        _pathPrefix = pathPrefix;
        _urlPrefix = urlPrefix;

        _cacheFile = cacheFile;
        _children = new Vector<Catalog>();

        log.debug("pathPrefix: " + _pathPrefix);
        log.debug("urlPrefix:  " + _urlPrefix);
        log.debug("fileName:   " + _fileName);




        String msg;

        fname = _pathPrefix + _fileName;

        log.debug("Complete FileName: " + fname);

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

        if(_cacheFile){
            cacheCatalogFileContent();
        }
        else
            _buffer = null;


        Document catalog  = getCatalogDocument();

        Element ce = catalog.getRootElement();

        _name = ce.getAttributeValue("name");

        if(_name ==null){
            msg = "THREDDS ERROR: <catalog> element missing \"name\" attribute.";
            log.error(msg);
            throw new Exception(msg);
        }

        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        //xmlo.output(catalog, System.out);


    }

    public void destroy(){
        _children.clear();
        _name        = null;
        _pathPrefix  = null;
        _urlPrefix   = null;
        _fileName    = null;
        _buffer      = null;
        _cacheFile   = false;
        _cacheTime   = null;
    }


    public void addChild(Catalog c){
        _children.add(c);
    }

    public Enumeration<Catalog> getChildren(){
        return _children.elements();
    }



    public boolean usesCache(){
        return _cacheFile;
    }

    /**
     * CAREFUL! Only call this function if you have acquired a WriteLock for
     * the catalog!!
     * @throws Exception When it can't read the file.
     */
    private void cacheCatalogFileContent() throws Exception {

        String fname = _pathPrefix + _fileName;

        File catalogFile = new File(fname);
        _buffer  = new byte[(int)catalogFile.length()];

        log.debug("Loading THREDDS catalog file: "+ fname);

        FileInputStream fis = new FileInputStream(fname);

        try {
            int count = 0, ret;
            while(count<catalogFile.length()){
                ret = fis.read(_buffer);
                if(ret<0){
                    log.error("Premature end of file reached. file: "+ fname);
                    throw new Exception("Premature end of file reached.");
                }
                count += ret;
            }
        }
        finally {
            fis.close();
        }
        _cacheTime = new Date();

        log.info("Using memory cache for: "+fname);


    }

    public boolean needsRefresh(){

        if(_cacheFile){ // It only needs refreshed if it's
                        // cached in the first place

            String fname = _pathPrefix + _fileName;
            File catalogFile = new File(fname);

            if (catalogFile.lastModified() > _cacheTime.getTime()) {

                log.debug("THREDDS Catalog file: "+fname+" needs to re-ingested");

                return true;
            }

        }
        return false;
    }




    public void writeCatalogXML(OutputStream os) throws Exception {

            String fname = _pathPrefix+_fileName;

            if(_buffer!=null){
                os.write(_buffer);
            }
            else {

                File catalogFile = new File(fname);

                log.debug("Loading THREDDS catalog file: "+ fname);

                byte[] buf  = new byte[2048];

                FileInputStream fis = new FileInputStream(fname);

                try {
                    int count = 0, ret;

                    while(count<catalogFile.length()){
                        ret = fis.read(buf);
                        if(ret<0){
                            log.error("Premature end of file reached. file: "+ fname);
                            throw new Exception("Premature end of file reached.");
                        }

                        os.write(buf,0,ret);
                        count += ret;
                    }
                }
                finally {
                    fis.close();
                }

            }
    }



    public Document getCatalogDocument() throws Exception {

        InputStream is=null;
        SAXBuilder sb = new SAXBuilder();

        try {
        if(_buffer!=null){
            is = new ByteArrayInputStream(_buffer);
            log.debug("getCatalogDocument(): Reading catalog from memory cache.");
        }
        else {
            is = new FileInputStream(_pathPrefix+_fileName);
            log.debug("getCatalogDocument(): Reading catalog from file.");
        }
        return  sb.build(is);
        }
        finally {
            if(is!=null)
                is.close();
        }

    }

    public XdmNode getCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {

        XdmNode source;
        InputStream is = null;

        try {
            if (_buffer != null) {
                is = new ByteArrayInputStream(_buffer);
                log.debug("getCatalogDocument(): Reading catalog from memory cache.");
            } else {
                is = new FileInputStream(_pathPrefix + _fileName);
                log.debug("getCatalogDocument(): Reading catalog from file.");
            }
            source = proc.newDocumentBuilder().build(new StreamSource(is));
        }
        finally {
            if (is != null)
                is.close();
        }
        return source;
    }


    public String getName(){
        return _name;
    }

    public String getPathPrefix(){
        return _pathPrefix;
    }

    public String getUrlPrefix(){
        return _urlPrefix;
    }

    public String getFileName(){
        return _fileName;
    }


    public long getLastModified(){
        String fname = _pathPrefix + _fileName;
        File catalogFile = new File(fname);
        return catalogFile.lastModified();        
    }


}
