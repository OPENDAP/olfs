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
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
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
    private int _size;
    private Date _lastModified;

    private boolean _isCollection;

    private String _parentPath;
    private S4CrawlableDataset _parent;

    private List _childDatasetElements;

    //private boolean  _isConfigured;
    private boolean _haveCatalog;
    private boolean _haveInfo;


    private Element _config;


    public S4CrawlableDataset(String path, Object o) throws IOException, PPTException, BadConfigurationException, JDOMException, BESException {

        Debug.set("CrawlableDataset",true);
        init();

        _config = (Element) o;

        if(Debug.isSet("CrawlableDataset")) System.out.println("\n\n\n\n\nS4CrawlableDataset config: "+_config);

        //XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
        //xo.output(_config,System.out);

        //try{
        configure();
        //}
        //catch(Exception e){
        //    System.out.println("OOPS!");
        //    e.printStackTrace(System.out);
        //}
        //System.out.println("\n\n\n\n\n");

        processPath(path);

        getInfo();

    }

    private void init(){
        _path                 = null;
        _name                 = null;
        _parentPath           = null;
        _lastModified         = null;
        _parent               = null;
        _childDatasetElements = null;
        _isCollection         = false;
        _haveCatalog          = false;
        _haveInfo             = false;
        _config               = null;
        _size                 = -1;

    }

    private S4CrawlableDataset(String path)  {

        init();

        processPath(path);
        //getInfo();

    }

    private void processPath(String path) {


        // Is path empty? Then make it "/"
        _path = path.equals("") ? "/" : path;     // Does THREDDS want the top to be "/" or empty??

        //_path = _path.equals("/") ? "" : _path;   // Does THREDDS want the top to be "/" or empty??

        _name = getNameFromPath(_path);

        _parentPath = getParentPath(_path,_name);

        //Got the catalog yet?
        _haveCatalog = false;

        if(Debug.isSet("CrawlableDataset")) {
            System.out.println("S4CrawlableDataset:");
            System.out.println("    _path            = " + _path);
            System.out.println("    _name            = " + _name);
            System.out.println("    _parentPath      = " + _parentPath);
        }

    }

    private String getParentPath(String path, String name){

        String pp = path.substring(0,path.lastIndexOf(name));
        if(pp.endsWith("/") && !pp.equals("/"))
            pp = path.substring(0,pp.lastIndexOf("/"));
        return pp;

    }

    private String getNameFromPath(String path){

        // Determine name (i.e., last name in the path name sequence).
        String name = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        name = name.equals("") ? "/" : name;

        //_name = _name.equals("/") ? "" : _name;   // Does THREDDS want the top to be "/" or empty??

        int index = name.lastIndexOf("/");
        if (index > 0)
            name = name.substring(index+1,name.length());

        return name;

    }



    private void configure() {


        if (_config != null) {

            //System.out.println("Configuring BES...");
            String besHost = _config.getChildTextTrim("besHost", _config.getNamespace());
            String besPortString = _config.getChildTextTrim("besPort", _config.getNamespace());
            //System.out.println("besHost: "+besHost+"   besPortString: "+besPortString);

            int besPort = Integer.parseInt(besPortString);

            //System.out.println("besHost: "+besHost+"   besPort: "+besPort+"\n\n");

            BesAPI.configure(besHost, besPort);
        } else {
            if(Debug.isSet("CrawlableDataset")) System.out.println("Looks like we are already configured, checking...");
            if (!BesAPI.isConfigured())
                System.out.println("BES IS NOT CONFIGURED!\n\n\n");
        }


    }


    public Object getConfigObject() {
        return _config;
    }


    private void getCatalog() throws PPTException, IOException, JDOMException, BadConfigurationException, BESException {

        if(_haveCatalog)
            return;

        if(Debug.isSet("CrawlableDataset")) System.out.println("Getting catalog for: "+_path);
        Document doc = BesAPI.showCatalog(_path);
        Element topDataset = doc.getRootElement();

        if (!_path.equals(topDataset.getChild("name").getTextTrim())) {
            throw new IOException("Returned dataset name does not match requested name.\n" +
                    "Requested: " + _path + "  " +
                    "Returned: " + topDataset.getChild("name").getTextTrim());
//            System.out.println("Returned dataset name does not match requested name.\n"+
//                                   "Requested: " + _name + "  "+
//                                   "Returned: "+topDataset.getChild("name").getTextTrim());
        }

        processDatasetElement(topDataset, this);

        _haveCatalog = true;


    }


    private void getInfo() throws PPTException, IOException, JDOMException, BadConfigurationException, BESException {

        if(_haveInfo)
            return;

        if(Debug.isSet("CrawlableDataset")) System.out.println("Getting info for: "+_path);
        Document doc = BesAPI.showInfo(_path);
        Element topDataset = doc.getRootElement();

        if (!_path.equals(topDataset.getChild("name").getTextTrim())) {
            throw new IOException("Returned dataset name does not match requested name.\n" +
                    "Requested: " + _path + "  " +
                    "Returned: " + topDataset.getChild("name").getTextTrim());

//            System.out.println("Returned dataset name does not match requested name.\n"+
//                                   "Requested: " + _name + "  "+
//                                   "Returned: "+topDataset.getChild("name").getTextTrim());

        }

        processDatasetElement(topDataset, this);

    }

    private void processDatasetElement(Element dataset, S4CrawlableDataset s4c) {

        // Process name
        s4c._name = getNameFromPath(dataset.getChild("name").getTextTrim());
        s4c._name = s4c._name.equals("/") ? "" : s4c._name;

        // Process size
        s4c._size = Integer.parseInt(dataset.getChild("size").getTextTrim());

        // process date and time
        String date = dataset.getChild("lastmodified").getChild("date").getTextTrim();
        String time = dataset.getChild("lastmodified").getChild("time").getTextTrim();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        s4c._lastModified = sdf.parse(date + " " +time, new ParsePosition(0));

        // Process collection (if it is one)
        String isCollection = dataset.getAttributeValue("thredds_collection");
        if (isCollection.equalsIgnoreCase("true")) {
            s4c._isCollection = true;
            s4c._childDatasetElements = dataset.getChildren("dataset");
        }

        s4c._haveInfo = true;


    }


    public String getPath() {

        return _path;
    }

    public String getParentPath(){
        return _parentPath;
    }

    public String getName() {
        return _name;
    }

    public CrawlableDataset getParentDataset() throws IOException {

        if (_parent != null) {
            return _parent;
        }

        if (_parentPath == null)
            return null;

        try {
            S4CrawlableDataset s4c = new S4CrawlableDataset(_parentPath, _config);
            _parent = s4c;
            return s4c;
        } catch (PPTException e) {
            throw new IOException(e.getMessage());
        } catch (JDOMException e) {
            throw new IOException(e.getMessage());
        } catch (BadConfigurationException e) {
            throw new IOException(e.getMessage());
        } catch (BESException e) {
            throw new IOException(e.getMessage());
        }


    }

    public boolean isCollection() {
        return _isCollection;
    }


    public List listDatasets() {

        Element e;
        S4CrawlableDataset dataset;


        if (!isCollection())
            return null;

        try {
            if (!_haveCatalog)
                getCatalog();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        int j = 0;
        Vector childDatasets = new Vector();
        Iterator i = _childDatasetElements.iterator();
        while (i.hasNext()) {

            e = (Element) i.next();


            String newPath = this._path + (_path.endsWith("/") ? "" : "/") + e.getChild("name").getTextTrim();

            if(Debug.isSet("CrawlableDataset")) System.out.println("\n\n\nMaking new dataset \"" + newPath + "\" in listDatasets().");

            // Show me what I've got...
            //XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
            //try {
            //    xo.output(e,System.out);
            //    System.out.println("\n");
            //} catch (IOException e1) {
            //    e1.printStackTrace();
            //}
            //------

            dataset = new S4CrawlableDataset(newPath);
            processDatasetElement(e,dataset);

            dataset._parent = this;
            dataset._config = this._config;
            if(Debug.isSet("CrawlableDataset")) System.out.println("Made: "+dataset);

            childDatasets.add(dataset);


            j++;

        }

        if(Debug.isSet("CrawlableDataset")) System.out.println("List Datasets found " + j + " member(s).");

        return childDatasets;
    }

    public List listDatasets(CrawlableDatasetFilter cdf) {

        if (!isCollection())
            return null;

        List list = this.listDatasets();

        if (cdf == null) return list;

        if(Debug.isSet("CrawlableDataset")) System.out.println("Filtering CrawlableDataset list.");

        List retList = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            CrawlableDataset curDs = (CrawlableDataset) it.next();
            if (cdf.accept(curDs)) {
                if(Debug.isSet("CrawlableDataset")) System.out.println("    Filter found matching dataset: "+curDs);
                retList.add(curDs);
            }
            else {
                if(Debug.isSet("CrawlableDataset")) System.out.println("    Filter discarded dataset: "+curDs);

            }
        }
        return (retList);
    }


    public long length() {
        return _size;
    }

    public Date lastModified() {
        return _lastModified;
    }

    public String toString(){
        String s="";

        s += "[CrawlableDataset  ";
        s += "<Path: "+getPath()+ "> ";
        s += "<Name: "+getName()+ "> ";
        s += "<Size: "+ length() + "> ";
        s += "<LastModified: "+lastModified()+ "> ";
        s += "<Collection: "+isCollection()+ "> ";
        s += "<_haveCatalog: "+_haveCatalog+ "> ";
        s += "<_haveInfo: "+_haveInfo+ "> ";
        s += "<_parentPath: "+_parentPath+ "> ";
        s += "<_parent.getName(): "+ (_parent==null ? "null" :_parent.getName() )+ "> ";
        s += "]";
        return s;

    }

}
