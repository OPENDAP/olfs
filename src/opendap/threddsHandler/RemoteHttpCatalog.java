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

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.slf4j.Logger;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Date;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.DocumentBuilder;

/**
 * User: ndp
 * Date: Dec 30, 2008
 * Time: 12:13:45 PM
 */
public class RemoteHttpCatalog implements Catalog {


    private Logger log;
    private  String _name;
    private  String _pathPrefix;
    private  String _urlPrefix;
    private  String _fileName;
    private  byte[] _buffer;
    private boolean _useMemoryCache;
    private Date _cacheTime;




    public RemoteHttpCatalog( String pathPrefix,
                    String urlPrefix,
                    String fname,
                    boolean useMemoryCache) throws Exception{

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        log.debug("Configuring new "+getClass().getName());

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

        if(_useMemoryCache){
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


    public void destroy() {
        _name        = null;
        _pathPrefix  = null;
        _urlPrefix   = null;
        _fileName    = null;
        _buffer      = null;
        _useMemoryCache = false;
        _cacheTime   = null;
    }






    public String getCatalogKey() {
        return null;

    }
    private void cacheCatalogFileContent(){log.debug("cacheCatalogFileContent(): WARNING STUB FIRING!");}

    public boolean usesMemoryCache() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean needsRefresh() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void writeCatalogXML(OutputStream os) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    public void writeRawCatalogXML(OutputStream os) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Document getCatalogDocument() throws IOException, JDOMException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Document getRawCatalogDocument() throws IOException, JDOMException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public XdmNode getCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public XdmNode getRawCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getPathPrefix() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUrlPrefix() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getFileName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    public long getLastModified() {
        return -1;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getIngestTransformFilename(){
        return null;
    }

}
