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
import opendap.util.Debug;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Dec 4, 2005
 * Time: 8:08:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class S4CrawlableDataset implements CrawlableDataset {

    private String _path;
    private String _name;
    private int    _size;
    private Date   _lastModified;

    private boolean _isContainer;

    private String    _parentPath;
    private S4CrawlableDataset _parent;

    private String   _besHost;
    private int      _besPort;
    private List     _childDatasetElements;

    private boolean  _isConfigured;
    private boolean  _haveCatalog;

    private ReqState _configRS;



    public S4CrawlableDataset(String path, Object o) throws IOException, PPTException, BadConfigurationException, JDOMException {

        new S4CrawlableDataset(path);

        configure((ReqState)o);

    }
    public S4CrawlableDataset(String path) {

        // Strip off the catalog request
        _path = path.endsWith("/catalog") ? path.substring( 0, path.length() - 8 ) : path;

        // Is path empty? Then make it "/"
        //_path = _path.equals("") ? "/" : _path;
        _path = _path.equals("/") ? "" : _path;

        // Determine name (i.e., last name in the path name sequence).
        _name = _path.endsWith( "/" ) ? _path.substring( 0, _path.length() - 1 ) : _path;

        //_name = _name.equals("") ? "/" : _name;
        _name = _name.equals("/") ? "" : _name;



        int index = _name.lastIndexOf( "/" );

        _parentPath = null;
        if ( index > 0){
            _parentPath = _name.substring(0,index);
            _name = _name.substring( index );
        }


        _besPort      = -1;
        _besHost      = null;
        _configRS     = null;
        _isConfigured = false;
        _haveCatalog  = false;

        if(Debug.isSet("showResponse")){
            System.out.println("S4CrawlableDataset:");
            System.out.println("    _path       = "+_path);
            System.out.println("    _name       = "+_name);
            System.out.println("    _parentPath = "+_parentPath);
        }

    }


    private void configure(ReqState rs) throws BadConfigurationException,
            IOException, PPTException, JDOMException {

        if(_isConfigured) {
            throw new BadConfigurationException("Error: You may not call S4CrawlableDataset.configure() more " +
            "than once for a given instance of S4CrawlableDataset.");
        }

        _configRS = rs;

        String besHost = _configRS.getInitParameter("BackEndServer");
        if (besHost == null)
            throw new BadConfigurationException("Servlet configuration must included BackEndServer\n");

        String besPort = _configRS.getInitParameter("BackEndServerPort");
        if (besPort == null)
            throw new BadConfigurationException("Servlet configuration must included BackEndServerPort\n");

        _besHost     = besHost;
        _besPort     = Integer.parseInt(besPort);

        if(Debug.isSet("showResponse")){
            System.out.println("    _besHost    = "+_besHost);
            System.out.println("    _besPort    = "+_besPort);
        }

        getInfo(_besHost,_besPort);

        _isConfigured = true;
    }



    public Object getConfigObject(){
        return _configRS;
    }





    private void getCatalog(String host, int port) throws PPTException, IOException, JDOMException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String product = "catalog for "+"\""+_path+"\"";

        BesAPI.besShowTransaction(product,host,port,baos);

        System.out.println("BES returned:\n"+baos);


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

        if(!_path.equals(topDataset.getChild("name").getTextTrim())){
//            throw new IOException ("Returned dataset name does not match requested name.\n"+
//                                   "Requested: " + _path + "  "+
//                                   "Returned: "+topDataset.getChild("name").getTextTrim());
            System.out.println("Returned dataset name does not match requested name.\n"+
                                   "Requested: " + _name + "  "+
                                   "Returned: "+topDataset.getChild("name").getTextTrim());
        }

        processDatasetElement(topDataset,this);

        _haveCatalog = true;


    }



    private void getInfo(String host, int port) throws PPTException, IOException, JDOMException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String product = "info for "+"\""+_path+"\"";

        BesAPI.besShowTransaction(product,host,port,baos);

        System.out.println("BES returned:\n"+baos);


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

        if(!_path.equals(topDataset.getChild("name").getTextTrim())){
  //          throw new IOException ("Returned dataset name does not match requested name.\n"+
    //                               "Requested: " + _path + "  "+
      //                             "Returned: "+topDataset.getChild("name").getTextTrim());

            System.out.println("Returned dataset name does not match requested name.\n"+
                                   "Requested: " + _name + "  "+
                                   "Returned: "+topDataset.getChild("name").getTextTrim());

        }

        processDatasetElement(topDataset,this);


    }


    private void processDatasetElement(Element dataset, S4CrawlableDataset s4c){

        s4c._name = dataset.getChild("name").getTextTrim();

        s4c._name = s4c._name.equals("/") ? "" : _name;

        s4c._size = Integer.parseInt(dataset.getChild("size").getTextTrim());

        SimpleDateFormat sdf = new SimpleDateFormat();

        s4c._lastModified = sdf.parse(
        dataset.getChild("lastmodified").getChild("date").getTextTrim() +
        dataset.getChild("lastmodified").getChild("time").getTextTrim(),
        new ParsePosition(0));

        String isContainer = dataset.getAttributeValue("thredds_container");

        if(isContainer.equalsIgnoreCase("true")){

            s4c._isContainer = true;
            s4c._childDatasetElements = dataset.getChildren("dataset");

        }
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

        if(_parentPath == null)
            return null;

        try {
            S4CrawlableDataset s4c = new S4CrawlableDataset(_parentPath,_configRS);
            _parent = s4c;
            return s4c;
        } catch (PPTException e) {
            throw new IOException(e.getMessage());
        } catch (JDOMException e) {
            throw new IOException(e.getMessage());
        } catch (BadConfigurationException e) {
            throw new IOException(e.getMessage());
        }


    }

    public boolean isCollection() {
        return _isContainer;
    }


    public List listDatasets()  {

        Element e;
        S4CrawlableDataset dataset;


        if(!isCollection())
            return null;

        try {
            if(!_haveCatalog)
                getCatalog(_besHost,_besPort);
        }
        catch(Exception ex){
            ex.printStackTrace();
            return null;
        }

        int j = 0;
        Vector childDatasets = new Vector();
        Iterator i = _childDatasetElements.iterator();
        while(i.hasNext()){
            e  = (Element) i.next();


            String newPath = this._path + (_path.equals("/") ? "" : "/") + e.getChild("name").getTextTrim();

            System.out.println("Making new dataset \""+newPath+"\" in listDatasets.");

            dataset = new S4CrawlableDataset(newPath);

            processDatasetElement(e,dataset);

            dataset._parent     = this;

            dataset._besHost    = this._besHost;
            dataset._besPort    = this._besPort;

            childDatasets.add(dataset);

            j++;

        }

        if(Debug.isSet("showResponse")) System.out.println("List Datasets found "+j+" member(s).");

        return childDatasets;
    }

    public List listDatasets(CrawlableDatasetFilter crawlableDatasetFilter) {

        if(!isCollection())
            return null;

        List l = listDatasets();

        Iterator i = l.iterator();

        while(i.hasNext()){
            CrawlableDataset cd = (CrawlableDataset) i.next();
            if(crawlableDatasetFilter != null && !crawlableDatasetFilter.accept(cd))
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
