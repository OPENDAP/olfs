/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes;

import opendap.coreServlet.ResourceInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * User: ndp
 * Date: Nov 15, 2006
 * Time: 7:10:51 PM
 */
public class BESResource implements ResourceInfo {

    public static final String BESDateFormat = "yyyy-MM-dd'T'HH:mm:ss";

    private boolean _exists;
    private boolean _accessible;
    private boolean _isNode;
    private boolean _isData;

    private String _name;
    private long _size;
    private Date _lastModified;

    private String requestedDataSource;

    private BesApi _besApi;


    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;


    public BESResource(String dataSourceName, BesApi besApi) throws Exception {

        Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

        _besApi = besApi;

        requestedDataSource = dataSourceName;
        _exists             = false;
        _accessible = false;
        _isNode = false;
        _isData = false;
        _name                = null;
        _size = -1;
        _lastModified = null;

        Document nodeDoc = new Document();

        if(besApi == null){
            _exists        = false;
            _accessible = false;
            _isNode = false;
            _isData = false;
            _name          = null;
            _size = -1;
            _lastModified = null;
            log.error("BESResource(): Received a null value for the BesApi instance!");
            return;
        }



        try {
            besApi.getBesNode(dataSourceName, nodeDoc);

            _exists = true;
            _accessible = true;
            Element root = nodeDoc.getRootElement();
            if(root==null)
                throw new IOException("BES showNode response for "+dataSourceName+" was empty! No root element");

            Element showNode  = root.getChild("showNode",BES_NS);
            if(showNode==null)
                throw new IOException("BES showNode response for "+dataSourceName+" was empty! No showNode element");

            Element nodeElement = showNode.getChild("node",BES_NS);

            if(nodeElement==null){
                Element itemElement  = showNode.getChild("item",BES_NS);
                if(itemElement == null)
                    throw new IOException("BES showNode response for " + dataSourceName + " did not contain " +
                            "expected content! No top level node or item element");
                _name = itemElement.getAttributeValue("name");
                String s = itemElement.getAttributeValue("size");
                if (s != null) {
                    _size = Long.valueOf(s);
                } else {
                    _size = itemElement.getChildren().size();
                }

                s = itemElement.getAttributeValue("lastModified");
                if (s != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat(BESDateFormat);
                    _lastModified = sdf.parse(itemElement.getAttributeValue("lastModified"));
                }

                String isNode = itemElement.getAttributeValue("type");
                _isNode = isNode!=null && isNode.equalsIgnoreCase("node");

                s = itemElement.getAttributeValue("isData");
                _isData = s != null && s.equalsIgnoreCase("true");
            }
            else {

                _isNode = true;
                _name = nodeElement.getAttributeValue("name");
                String s = nodeElement.getAttributeValue("size");
                if (s != null) {
                    _size = Long.valueOf(s);
                } else {
                    _size = nodeElement.getChildren().size();
                }

                s = nodeElement.getAttributeValue("lastModified");
                if (s != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat(BESDateFormat);
                    _lastModified = sdf.parse(nodeElement.getAttributeValue("lastModified"));
                }


                s = nodeElement.getAttributeValue("isData");
                _isData = s != null && s.equalsIgnoreCase("true");
            }

        }
        catch (BESError err ){
            _exists        = !err.notFound();
            _accessible = !err.forbidden();
            _isNode = false;
            _isData = false;
            _name          = null;
            _size = -1;
            _lastModified = null;
            log.debug("BES request for info document for: \""+dataSourceName+"\" returned an error");
        }
    }

    public  boolean sourceExists(){
        return _exists;
    }

    public  boolean sourceIsAccesible(){
        return _accessible;
    }


    public  boolean isNode(){
        return _isNode;
    }

    public  boolean isDataset(){
        return _isData;
    }

    public String getName(){
        return _name;
    }

    public long getSize(){
        return _size;
    }

    public Date getLastModifiedDate(){
        return _lastModified;
    }

    public long lastModified(){
        if(_lastModified !=null)
            return _lastModified.getTime();
        return -1;
    }

    // public String getRequestedDataSource(){ return requestedDataSource; }
    // public String[] getServiceRefs(){ return serviceRefs; }


    public String toString(){
        String s = "BESResource("+requestedDataSource+"):\n";

        s += "    exists:  "+_exists+"\n";
        if(_exists){
            s += "    name:         " + _name         + "\n";
            s += "    isNode:       " + _isNode + "\n";
            s += "    isDataset:    " + _isData + "\n";
            s += "    size:         " + _size + "\n";
            s += "    lastModified: " + _lastModified + "\n";
        }
        return s;
    }


}
