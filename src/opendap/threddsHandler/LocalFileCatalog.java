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
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.Date;
import java.util.Vector;
import java.util.Enumeration;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import net.sf.saxon.s9api.*;

import javax.xml.transform.stream.StreamSource;

/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 4:54:27 PM
 */
public class LocalFileCatalog implements Catalog {



    private  Logger log;
    private  String _name;
    private  String _pathPrefix;
    private  String _urlPrefix;
    private  String _fileName;
    private  byte[] _buffer;
    private boolean _useMemoryCache;
    private Vector<Catalog> _children;
    private Date _cacheTime;
    private static ReentrantReadWriteLock _catalogLock;

    public LocalFileCatalog( String pathPrefix,
                    String urlPrefix,
                    String fname,
                    boolean useMemoryCache) throws Exception{

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        log.debug("Configuring new "+getClass().getName());

        _catalogLock = new ReentrantReadWriteLock();

       



        _fileName = fname;
        _pathPrefix = pathPrefix;
        _urlPrefix = urlPrefix;

        _useMemoryCache = useMemoryCache;
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
        if(!catalogFile.isFile()){
            msg = "THREDDS Catalog "+ fname +" is not a regular file.";
            log.error(msg);
            throw new IOException(msg);
        }

        if(_useMemoryCache){
            cacheCatalogFileContent();
        }
        else{
            _buffer = null;
            _cacheTime = new Date(catalogFile.lastModified());
        }



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
        _useMemoryCache = false;
        _cacheTime   = null;
    }


    public void addChild(LocalFileCatalog c){

        Lock lock =  _catalogLock.writeLock();
        try {
            lock.lock();
            _children.add(c);
        }
        finally {
            lock.unlock();
        }
    }

    public Enumeration<Catalog> getChildren(){
        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            return _children.elements();
        }
        finally {
            lock.unlock();
        }
    }



    public boolean usesMemoryCache(){
        return _useMemoryCache;
    }

    /**
     * CAREFUL! Only call this function if you have acquired a WriteLock for
     * the catalog!!
     * @throws Exception When it can't read the file.
     */
    private void cacheCatalogFileContent() throws Exception {

        Lock lock =  _catalogLock.writeLock();
        try {
            lock.lock();
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
                try{
                    fis.close();
                }
                catch(IOException e){
                    log.error("Failed to close THREDDS catalog file: "+fname+
                            " Error MEssage: "+e.getMessage());

                }

            }
            _cacheTime = new Date();

            log.info("Using memory cache for: "+fname);
        }
        finally {
            lock.unlock();
        }


    }

    public boolean needsRefresh(){

        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            String fname = _pathPrefix + _fileName;
            File catalogFile = new File(fname);
            if (catalogFile.lastModified() > _cacheTime.getTime()) {

                log.debug("THREDDS Catalog file: "+fname+" needs to re-ingested");

                return true;
            }

            return false;
        }
        finally{
            lock.unlock();
        }
    }




    public void writeCatalogXML(OutputStream os) throws Exception {

        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
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
                    try{
                        fis.close();
                    }
                    catch(IOException e){
                        log.error("Failed to close THREDDS catalog file: "+fname+
                        " Error MEssage: "+e.getMessage());
                    }

                }

            }
        }
        finally {
            lock.unlock();
        }
    }


    /**
     *
     * @return The catalog parsed into a JDOM Document.
     * @throws IOException When things can't be read.
     * @throws JDOMException When things can't be parsed.
     */
    //@todo Consider optimizing this to cache the document after parsing.
    public Document getCatalogDocument() throws IOException, JDOMException {

        InputStream is = null;

        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            SAXBuilder sb = new SAXBuilder();
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
            if(is!=null){
                try {
                    is.close();
                }
                catch(IOException e){
                    log.error("Failed to close InputStream. Error Message: "+e.getMessage());
                }

            }
            lock.unlock();
        }

    }

    /**
     *
     * @param proc XSLT processor that will be used to transform document.
     * @return The catalog parsed into an XdmNode
     * @throws IOException When things can't be read.
     * @throws SaxonApiException When things can't be parsed.
     */
     //@todo Consider optimizing this to cache the document after parsing.
    public XdmNode getCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {

        XdmNode catalog;
        InputStream is = null;

        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            if (_buffer != null) {
                is = new ByteArrayInputStream(_buffer);
                log.debug("getCatalogDocument(): Reading catalog from memory cache.");
            } else {
                is = new FileInputStream(_pathPrefix + _fileName);
                log.debug("getCatalogDocument(): Reading catalog from file.");
            }


            DocumentBuilder builder = proc.newDocumentBuilder();
            builder.setLineNumbering(true);
            //builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);

            catalog = builder.build(new StreamSource(is));
        }
        finally {
            if(is!=null){
                try {
                    is.close();
                }
                catch(IOException e){
                    log.error("Failed to close InputStream. Error Message: "+e.getMessage());
                }

            }
            lock.unlock();
        }
        return catalog;
    }

    /**
     *
     * @param builder The DocumentBuilder used to build the document.
     * @return The catalog parsed into an XdmNode
     * @throws IOException When things can't be read.
     * @throws SaxonApiException When things can't be parsed.
     */
     //@todo Consider optimizing this to cache the document after parsing.
    public XdmNode getCatalogAsXdmNode(DocumentBuilder builder) throws IOException, SaxonApiException {

        XdmNode catalog;
        InputStream is = null;

        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            if (_buffer != null) {
                is = new ByteArrayInputStream(_buffer);
                log.debug("getCatalogDocument(): Reading catalog from memory cache.");
            } else {
                is = new FileInputStream(_pathPrefix + _fileName);
                log.debug("getCatalogDocument(): Reading catalog from file.");
            }
            //builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);

            catalog = builder.build(new StreamSource(is));
        }
        finally {
            if(is!=null){
                try {
                    is.close();
                }
                catch(IOException e){
                    log.error("Failed to close InputStream. Error Message: "+e.getMessage());
                }

            }
            lock.unlock();
        }
        return catalog;
    }


    public String getName(){

        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            return _name;
        }
        finally {
            lock.unlock();
        }
    }

    public String getPathPrefix(){
        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            return _pathPrefix;
        }
        finally {
            lock.unlock();
        }
    }

    public String getUrlPrefix(){
        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            return _urlPrefix;
        }
        finally {
            lock.unlock();
        }
    }

    public String getFileName(){
        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            return _fileName;
        }
        finally {
            lock.unlock();
        }
    }


    public long getLastModified(){
        Lock lock =  _catalogLock.readLock();
        try {
            lock.lock();
            String fname = _pathPrefix + _fileName;
            File catalogFile = new File(fname);
            return catalogFile.lastModified();
        }
        finally {
            lock.unlock();
        }
    }


}
