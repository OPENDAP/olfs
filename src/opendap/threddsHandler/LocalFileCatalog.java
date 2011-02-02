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
package opendap.threddsHandler;

import opendap.xml.Transformer;
import org.slf4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.Date;
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


    private Logger log;
    private String _name;

    /**
     *  Deinfes where in the system 
     */
    private String _pathPrefix;
    private String _urlPrefix;
    private String _fileName;
    private boolean _useMemoryCache;
    private Date _cacheTime;


    private ReentrantReadWriteLock _catalogLock;
    private byte[] _rawCatalogBuffer;


    private Transformer _ingestTransformer;
    private String _transformOnIngestFilename;
    private byte[] _clientResponseCatalogBuffer;


    public LocalFileCatalog(String pathPrefix,
                            String urlPrefix,
                            String fname,
                            String transformOnIngestFilename,
                            boolean useMemoryCache) throws Exception {

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        log.debug("-------------------------------------------------------");
        log.debug("Configuring new " + getClass().getName());

        _catalogLock = new ReentrantReadWriteLock();


        _transformOnIngestFilename = transformOnIngestFilename;
        _fileName = fname;
        _pathPrefix = pathPrefix;
        _urlPrefix = urlPrefix;

        _useMemoryCache = useMemoryCache;

        log.debug("pathPrefix: " + _pathPrefix);
        log.debug("urlPrefix:  " + _urlPrefix);
        log.debug("fileName:   " + _fileName);


        String msg;

        fname = _pathPrefix + _fileName;

        log.debug("Complete FileName: " + fname);

        File catalogFile = new File(fname);

        if (!catalogFile.exists()) {
            msg = "Cannot find catalog file: " + fname;
            log.error(msg);
            throw new IOException(msg);
        }

        if (!catalogFile.canRead()) {
            msg = "Cannot read catalog file: " + fname;
            log.error(msg);
            throw new IOException(msg);
        }
        if (!catalogFile.isFile()) {
            msg = "THREDDS Catalog " + fname + " is not a regular file.";
            log.error(msg);
            throw new IOException(msg);
        }


        _clientResponseCatalogBuffer = null;
        _rawCatalogBuffer = null;

        if (_useMemoryCache) {
            cacheRawCatalogFileContent();
        } else {
            _cacheTime = new Date(catalogFile.lastModified());
        }


        if (_transformOnIngestFilename != null) {


            log.debug("_transformOnIngestFilename: " + _transformOnIngestFilename);

            File ingestTransformFile = new File(_transformOnIngestFilename);

            if (!ingestTransformFile.exists()) {
                msg = "Cannot find the ingest XSL transformation file: " + _transformOnIngestFilename;
                log.error(msg);
                throw new IOException(msg);
            }

            if (!ingestTransformFile.canRead()) {
                msg = "Cannot the ingest XSL transformation file: " + _transformOnIngestFilename;
                log.error(msg);
                throw new IOException(msg);
            }
            if (!ingestTransformFile.isFile()) {
                msg = "XSLT file '" + _transformOnIngestFilename + "' is not a regular file.";
                log.error(msg);
                throw new IOException(msg);
            }
            _ingestTransformer = new Transformer(_transformOnIngestFilename);


            if (_useMemoryCache) {
                log.debug("Caching (in memory) a copy of transformed catalog.");
                cacheIngestTransformedCatalog();
                log.info("PASSED Catalog Ingest: Cached catalog file via ingest Transform.");
            } else {
                XdmNode node = getCatalogAsXdmNode(_ingestTransformer.getProcessor());
                if (node != null)
                    log.info("PASSED Catalog Ingest: Loaded/parsed catalog file via ingest Transform.");
            }


        }

        Document catalog = getRawCatalogDocument();

        Element ce = catalog.getRootElement();

        _name = ce.getAttributeValue("name");

        if (_name == null) {
            msg = "THREDDS ERROR: <catalog> element missing \"name\" attribute.";
            log.error(msg);
            throw new Exception(msg);
        }


        log.debug("Catalog '"+getName()+"' has been built and parsed.");


        log.debug("-------------------------------------------------------");

        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        //xmlo.output(catalog, System.out);


    }

    public void destroy() {


        _name = null;
        _pathPrefix = null;
        _urlPrefix = null;
        _fileName = null;
        _rawCatalogBuffer = null;
        _useMemoryCache = false;
        _cacheTime = null;
    }


    public boolean usesMemoryCache() {
        return _useMemoryCache;
    }





    /**
     *
     * @throws Exception When it can't read the file.
     */
    private void cacheIngestTransformedCatalog() throws Exception {

        Lock lock = _catalogLock.writeLock();
        try {
            lock.lock();
            if (_ingestTransformer != null) {
                log.debug("cacheIngestTransformedCatalog(): Applying catalog ingestTransform.");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XdmNode catalog = getRawCatalogAsXdmNode(_ingestTransformer.getProcessor());
                _ingestTransformer.transform(catalog, baos);
                log.debug("cacheIngestTransformedCatalog(): Caching ingestTransform result. Size= " + baos.size() + " bytes");
                _clientResponseCatalogBuffer = baos.toByteArray();
                if(_cacheTime==null)
                    _cacheTime = new Date();
            } else {
                log.error("cacheIngestTransformedCatalog(): Cannot cache ingest transformed catalog! Ingest transformation is a null value.");
                _clientResponseCatalogBuffer = null;
            }

        }
        finally {
            lock.unlock();
        }


    }


    /**
     *
     * @throws Exception When it can't read the file.
     */
    private void cacheRawCatalogFileContent() throws Exception {

        Lock lock = _catalogLock.writeLock();
        try {
            lock.lock();
            String fname = _pathPrefix + _fileName;

            File catalogFile = new File(fname);
            _rawCatalogBuffer = new byte[(int) catalogFile.length()];

            log.debug("Loading THREDDS catalog file: " + fname);

            FileInputStream fis = new FileInputStream(fname);

            try {
                int count = 0, ret;
                while (count < catalogFile.length()) {
                    ret = fis.read(_rawCatalogBuffer);
                    if (ret < 0) {
                        log.error("Premature end of file reached. file: " + fname);
                        throw new Exception("Premature end of file reached.");
                    }
                    count += ret;
                }
            }
            finally {
                try {
                    fis.close();
                }
                catch (IOException e) {
                    log.error("Failed to close THREDDS catalog file: " + fname +
                            " Error MEssage: " + e.getMessage());

                }

            }
            _cacheTime = new Date();

            log.info("Using memory cache for: " + fname);
        }
        finally {
            lock.unlock();
        }


    }

    /**
     * CAREFUL! Only call this function if you have acquired a WriteLock for
     * the catalog!!
     *
     * @throws Exception When it can't read the file.
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   <p/>
     *                   InputStream catDocIs = request.getResponseBodyAsStream();
     *                   <p/>
     *                   <p/>
     *                   try {
     *                   catalogToHtmlTransformLock.lock();
     *                   catalogToHtmlTransform.reloadTransformIfRequired();
     *                   <p/>
     *                   // Build the catalog document as an XdmNode.
     *                   XdmNode catDoc = catalogToHtmlTransform.build(new StreamSource(catDocIs));
     *                   <p/>
     *                   catalogToHtmlTransform.setParameter("remoteHost", remoteHost);
     *                   catalogToHtmlTransform.setParameter("remoteRelativeURL", remoteRelativeURL);
     *                   catalogToHtmlTransform.setParameter("remoteCatalog", remoteCatalog);
     *                   <p/>
     *                   // Set up the Http headers.
     *                   response.setContentType("text/html");
     *                   response.setHeader("Content-Description", "thredds_catalog");
     *                   response.setStatus(HttpServletResponse.SC_OK);
     *                   <p/>
     *                   // Send the transformed documet.
     *                   catalogToHtmlTransform.transform(catDoc,response.getOutputStream());
     *                   <p/>
     *                   log.debug("Used saxon to send THREDDS catalog (XML->XSLT(saxon)->HTML).");
     */

    public boolean needsRefresh() {

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            String fname = _pathPrefix + _fileName;
            File catalogFile = new File(fname);
            long fileLastModified = catalogFile.lastModified();
            if (fileLastModified > _cacheTime.getTime()) {

                log.debug("The THREDDS Catalog file: " + fname + " has changed and needs to re-ingested");

                return true;
            }

            return false;
        }
        finally {
            lock.unlock();
        }
    }


    public void writeRawCatalogXML(OutputStream os) throws Exception {

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            String fname = _pathPrefix + _fileName;

            if (_rawCatalogBuffer != null) {
                log.debug("writeRawCatalogXML(): Sending cached raw catalog.");
                os.write(_rawCatalogBuffer);
            } else {

                File catalogFile = new File(fname);

                log.debug("writeRawCatalogXML(): Loading THREDDS catalog file: " + fname);

                byte[] buf = new byte[2048];

                FileInputStream fis = new FileInputStream(fname);

                try {
                    int count = 0, ret;

                    while (count < catalogFile.length()) {
                        ret = fis.read(buf);
                        if (ret < 0) {
                            log.error("writeRawCatalogXML(): Premature end of file reached. file: " + fname);
                            throw new Exception("Premature end of file reached.");
                        }

                        os.write(buf, 0, ret);
                        count += ret;
                    }
                }
                finally {
                    try {
                        fis.close();
                    }
                    catch (IOException e) {
                        log.error("writeRawCatalogXML(): Failed to close THREDDS catalog file: " + fname +
                                " Error MEssage: " + e.getMessage());
                    }

                }

            }
        }
        finally {
            lock.unlock();
        }
    }

    public void writeCatalogXML(OutputStream os) throws Exception {

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            String fname = _pathPrefix + _fileName;

            if (_clientResponseCatalogBuffer != null) {
                os.write(_clientResponseCatalogBuffer);
                log.debug("writeCatalogXML(): Sending cached ingetTransform processed catalog.");

            } else if (_rawCatalogBuffer != null) {
                os.write(_rawCatalogBuffer);
                log.debug("writeCatalogXML(): Sending cached raw catalog.");

            } else {


                log.debug("writeCatalogXML(): Loading THREDDS catalog file: " + fname);


                if (_ingestTransformer != null) {
                    log.debug("writeCatalogXML(): Applying catalog ingestTransform.");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    XdmNode catalog = getRawCatalogAsXdmNode(_ingestTransformer.getProcessor());
                    _ingestTransformer.transform(catalog, baos);
                    log.debug("writeCatalogXML(): Sending ingestTransform result. Writing " + baos.size() + " bytes");
                    os.write(baos.toByteArray());
                } else {
                    File catalogFile = new File(fname);
                    InputStream fis = new FileInputStream(catalogFile);
                    byte[] buf = new byte[2048];
                    try {
                        int count = 0, ret;

                        while (count < catalogFile.length()) {
                            ret = fis.read(buf);
                            if (ret < 0) {
                                log.error("writeCatalogXML() Premature end of file reached. file: " + fname);
                                throw new Exception("Premature end of file reached.");
                            }

                            os.write(buf, 0, ret);
                            count += ret;
                        }
                    }
                    finally {
                        try {
                            fis.close();
                        }
                        catch (IOException e) {
                            log.error("writeCatalogXML(): Failed to close THREDDS catalog file: " + fname +
                                    " Error MEssage: " + e.getMessage());
                        }

                    }
                }

            }
        }
        finally {
            lock.unlock();
        }
    }


    /**
     * @return The catalog parsed into a JDOM Document.
     * @throws IOException   When things can't be read.
     * @throws JDOMException When things can't be parsed.
     */
    //@todo Consider optimizing this to cache the document after parsing.
    public Document getRawCatalogDocument() throws IOException, JDOMException {

        InputStream is = null;

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            SAXBuilder sb = new SAXBuilder();
            if (_rawCatalogBuffer != null) {
                is = new ByteArrayInputStream(_rawCatalogBuffer);
                log.debug("getCatalogDocument(): Reading catalog from memory cache.");
            } else {
                is = new FileInputStream(_pathPrefix + _fileName);
                log.debug("getCatalogDocument(): Reading catalog from file.");
            }
            return sb.build(is);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("Failed to close InputStream. Error Message: " + e.getMessage());
                }

            }
            lock.unlock();
        }

    }

    /**
     * @return The catalog parsed into a JDOM Document.
     * @throws IOException   When things can't be read.
     * @throws JDOMException When things can't be parsed.
     */
    //@todo Consider optimizing this to cache the document after parsing.
    public Document getCatalogDocument() throws IOException, JDOMException, SaxonApiException {

        InputStream is = null;
        Document catDoc = null;

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            SAXBuilder sb = new SAXBuilder();

            if (_clientResponseCatalogBuffer != null) {
                is = new ByteArrayInputStream(_clientResponseCatalogBuffer);
            } else if (_rawCatalogBuffer != null) {
                is = new ByteArrayInputStream(_rawCatalogBuffer);
                log.debug("getCatalogDocument(): Reading catalog from memory cache.");
            } else {
                is = new FileInputStream(_pathPrefix + _fileName);

                if (_ingestTransformer != null) {
                    log.debug("getCatalogDocument(): Applying catalog ingestTransform.");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    XdmNode catalog = getRawCatalogAsXdmNode(_ingestTransformer.getProcessor());
                    _ingestTransformer.transform(catalog, baos);

                    is = new ByteArrayInputStream(baos.toByteArray());
                }

            }
            catDoc = sb.build(is);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("getCatalogDocument(): Failed to close InputStream. Error Message: " + e.getMessage());
                }

            }
            lock.unlock();
        }
        return catDoc;

    }

    /**
     * @param proc XSLT processor that will be used to transform document.
     * @return The catalog parsed into an XdmNode
     * @throws IOException       When things can't be read.
     * @throws SaxonApiException When things can't be parsed.
     */
    //@todo Consider optimizing this to cache the document after parsing.
    public XdmNode getRawCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {

        XdmNode catalog;
        InputStream is = null;

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            if (_rawCatalogBuffer != null) {
                is = new ByteArrayInputStream(_rawCatalogBuffer);
                log.debug("getRawCatalogAsXdmNode(): Reading catalog from memory cache.");
            } else {
                String fname = _pathPrefix + _fileName;
                is = new FileInputStream(fname);
                log.debug("getRawCatalogAsXdmNode(): Reading catalog from file: " + fname);
            }


            DocumentBuilder builder = proc.newDocumentBuilder();
            builder.setLineNumbering(true);
            //builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);

            catalog = builder.build(new StreamSource(is));
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("getRawCatalogAsXdmNode(): Failed to close InputStream. Error Message: " + e.getMessage());
                }

            }
            lock.unlock();
        }
        return catalog;
    }

    /**
     * @param proc XSLT processor that will be used to transform document.
     * @return The catalog parsed into an XdmNode
     * @throws IOException       When things can't be read.
     * @throws SaxonApiException When things can't be parsed.
     */
    //@todo Consider optimizing this to cache the document after parsing.
    public XdmNode getCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {

        XdmNode catalog;
        InputStream is = null;

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            if (_clientResponseCatalogBuffer != null) {
                is = new ByteArrayInputStream(_clientResponseCatalogBuffer);
                log.debug("getCatalogAsXdmNode(): Reading ingestTransform processed catalog from memory cache.");
            } else if (_rawCatalogBuffer != null) {
                is = new ByteArrayInputStream(_rawCatalogBuffer);
                log.debug("getCatalogAsXdmNode(): Reading raw catalog from memory cache.");
            } else {
                is = new FileInputStream(_pathPrefix + _fileName);
                log.debug("getCatalogAsXdmNode(): Reading catalog from file.");

                if (_ingestTransformer != null) {
                    log.debug("getCatalogAsXdmNode(): Applying catalog ingestTransform.");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    catalog = getRawCatalogAsXdmNode(_ingestTransformer.getProcessor());
                    _ingestTransformer.transform(catalog, baos);

                    is = new ByteArrayInputStream(baos.toByteArray());
                }
            }


            DocumentBuilder builder = proc.newDocumentBuilder();
            builder.setLineNumbering(true);
            //builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);

            catalog = builder.build(new StreamSource(is));
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("Failed to close InputStream. Error Message: " + e.getMessage());
                }

            }
            lock.unlock();
        }
        return catalog;
    }


    public String getName() {

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            return _name;
        }
        finally {
            lock.unlock();
        }
    }


    public String getCatalogKey() {
        
        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            String index = getUrlPrefix() + getFileName();
            return index;
        }
        finally {
            lock.unlock();
        }

    }
    
    public String getPathPrefix() {
        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            return _pathPrefix;
        }
        finally {
            lock.unlock();
        }
    }

    public String getUrlPrefix() {
        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            return _urlPrefix;
        }
        finally {
            lock.unlock();
        }
    }

    public String getFileName() {
        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            return _fileName;
        }
        finally {
            lock.unlock();
        }
    }


    public long getLastModified() {
        Lock lock = _catalogLock.readLock();
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

    public String getIngestTransformFilename() {
        return _transformOnIngestFilename;
    }

    public Transformer getIngestTransformer() {
        return _ingestTransformer;
    }


}
