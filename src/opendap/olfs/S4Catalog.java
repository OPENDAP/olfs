/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.olfs;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import opendap.ppt.PPTException;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Dec 4, 2005
 * Time: 8:08:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class S4Catalog implements CrawlableDataset {

    private String _path;
    private String _name;
    private int    _size;
    private Date   _lastModified;

    private boolean _isContainer;

    private String    _parentName;
    private S4Catalog _parent;

    private String   _besHost;
    private int      _besPort;
    private List     _childDatasetElements;




    public S4Catalog(String path, String besHost, int besPort) throws IOException, PPTException, JDOMException {
        _path = path;
        // Determine name (i.e., last name in the path name sequence).
        _name = path.endsWith( "/" ) ? path.substring( 0, path.length() - 1 ) : path;
        int index = _name.lastIndexOf( "/" );

        _parentName = null;
        if ( index != -1 ){
            _name = _name.substring( index + 1 );

            if(index > 0)
                _parentName = _name.substring(0,index);

        }

        _besHost     = besHost;
        _besPort     = besPort;
        getCatalog(besHost,besPort);


    }

    public S4Catalog(){

    }






    private void getCatalog(String host, int port) throws PPTException, IOException, JDOMException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String product = " for "+"\""+_path+"\";";

        BesAPI.besShowTransaction(product,host,port,baos);

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc = sb.build(new ByteArrayInputStream(baos.toByteArray()));

        // Tweak it!

        // First find the response Element

        Element topDataset = doc.getRootElement().getChild("response").getChild("dataset");

        // Disconnect it from it's parent and then rename it.
        topDataset.detach();
        doc.detachRootElement();
        doc.setRootElement(topDataset);

        if(!_name.equals(topDataset.getChild("name").getTextTrim())){
            throw new IOException ("Returned dataset name does not match requested name.");
        }

        processDatasetElement(topDataset,this);


    }


    private void processDatasetElement(Element dataset, S4Catalog s4c){

        s4c._name = dataset.getChild("name").getTextTrim();
        s4c._size = Integer.parseInt(dataset.getChild("size").getTextTrim());

        SimpleDateFormat sdf = new SimpleDateFormat();

        s4c._lastModified = sdf.parse(
        dataset.getChild("lastmodified").getChild("date").getTextTrim() +
        dataset.getChild("lastmodified").getChild("time").getTextTrim(),
        new ParsePosition(0));

        s4c._childDatasetElements = dataset.getChildren("dataset");

        s4c._isContainer = !s4c._childDatasetElements.isEmpty();

    }






    public String getPath() {
        return _path;
    }

    public String getName() {
        return _name;
    }

    public CrawlableDataset getParentDataset() throws IOException {

        if(_parent != null){
            return _parent;
        }

        if(_parentName == null)
            return null;

        try {
            return new S4Catalog(_parentName,_besHost,_besPort);
        } catch (PPTException e) {
            throw new IOException(e.getMessage());
        } catch (JDOMException e) {
            throw new IOException(e.getMessage());
        }


    }

    public boolean isCollection() {
        return _isContainer;
    }


    public List listDatasets()  {

        if(!isCollection())
            return null;

        try {
            if(_childDatasetElements == null)
                getCatalog(_besHost,_besPort);
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }

        Vector childDatasets = new Vector();
        Iterator i = _childDatasetElements.iterator();

        Element e;
        S4Catalog dataset;
        while(i.hasNext()){
            e  = (Element) i.next();
            dataset = new S4Catalog();

            processDatasetElement(e,dataset);

            dataset._parent     = this;
            dataset._parentName = this._name;
            dataset._path       = this._path + "/"+this._name;
            dataset._besHost    = this._besHost;
            dataset._besPort    = this._besPort;

            childDatasets.add(dataset);

        }

        return childDatasets;
    }

    public List listDatasets(CrawlableDatasetFilter crawlableDatasetFilter) {

        if(!isCollection())
            return null;

        List l = listDatasets();

        Iterator i = l.iterator();

        while(i.hasNext()){
            CrawlableDataset cd = (CrawlableDataset) i.next();
            if(!crawlableDatasetFilter.accept(cd))
                l.remove(cd);
        }


        return l;
    }

    public long length() {
        return _size;
    }

    public Date lastModified() {
        return _lastModified;
    }
}
