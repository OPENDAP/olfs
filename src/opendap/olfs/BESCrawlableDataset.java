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
import opendap.ppt.PPTException;
import opendap.util.Debug;

/**
 * This implmentation of the THREDDS CrawlableDataset interface provides the connection
 * between the core THREDDS functionalities and the BES.
 *  
 * User: ndp
 * Date: Dec 4, 2005
 * Time: 8:08:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class BESCrawlableDataset implements CrawlableDataset {

    private String _threddsPath;
    private String _besPath;
    private String _name;
    private int _size;
    private Date _lastModified;

    private boolean _isCollection;

    private String _parentPath;
    private BESCrawlableDataset _parent;

    private List _childDatasetElements;

    //private boolean  _isConfigured;
    private boolean _haveCatalog;
    private boolean _haveInfo;


    private Element _config;


    public static BESCrawlableDataset getRootDataset() throws IOException, PPTException, BadConfigurationException, BESException, JDOMException {
        return new BESCrawlableDataset("/root", null);
    }

    public BESCrawlableDataset(String path, Object o) throws IOException, PPTException, BadConfigurationException, JDOMException, BESException {

        //Debug.set("CrawlableDataset", true);
        init();

        _config = (Element) o;

        if (Debug.isSet("CrawlableDataset")) System.out.println("\n\n\n\n\nS4CrawlableDataset config: " + _config);

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

        //System.out.println(this);

    }

    private void init() {
        _threddsPath = null;
        _besPath = null;
        _name = null;
        _parentPath = null;
        _lastModified = null;
        _parent = null;
        _childDatasetElements = null;
        _isCollection = false;
        _haveCatalog = false;
        _haveInfo = false;
        _config = null;
        _size = -1;

    }

    private BESCrawlableDataset(String path) {

        init();

        processPath(path);
        //getInfo();
        //System.out.println(this);

    }

    private void processPath(String path) {


        _threddsPath = path;
        _besPath = threddsPath2BesPath(getThreddsPath());
        _name = getNameFromPath(getBesPath());
        _parentPath = getParentPath(getBesPath(), getName());

        //Got the catalog yet?
        _haveCatalog = false;

        if (Debug.isSet("CrawlableDataset")) {
            System.out.println("BESCrawlableDataset:");
            System.out.println("     getPath()        = " + getPath());
            System.out.println("     getThreddsPath() = " + getThreddsPath());
            System.out.println("     getBesPath()     = " + getBesPath());
            System.out.println("     getName()        = " + getName());
            System.out.println("     getParentPath()  = " + getParentPath());
        }

    }


    private String threddsPath2BesPath(String tpath) {

        return tpath.substring("/root".length());


    }


    public static String besPath2ThreddsPath(String path) {
        String threddsPath;

        if (path.startsWith("/root"))
            threddsPath = path;
        else {
            if (path.startsWith("/") || path.equals(""))
                threddsPath = "/root" + path;
            else
                threddsPath = "/root/" + path;
        }
        // Is path empty? Then make it "/"
        //_path = path.equals("") ? "/root" : path;     // Does THREDDS want the top to be "/" or empty??

        //_path = _path.equals("/") ? "" : _path;   // Does THREDDS want the top to be "/" or empty??
        return threddsPath;
    }


    private String getParentPath(String path, String name) {

        String pp = path.substring(0, path.lastIndexOf(name));
        if (pp.endsWith("/") && !pp.equals("/"))
            pp = path.substring(0, pp.lastIndexOf("/"));


        return besPath2ThreddsPath(pp);

    }

    private String getNameFromPath(String path) {

        //System.out.println("path: "+path);
        String name = path;

        // Determine name (i.e., last name in the path name sequence).
        name = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;

        //name = name.equals("") ? "/" : name;

        name = name.equals("/") ? "" : name;   // Does THREDDS want the top to be "/" or empty??

        int index = name.lastIndexOf("/");
        if (index > 0)
            name = name.substring(index + 1, name.length());

        if (name.startsWith("/"))
            name = name.substring(1);

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
            if (Debug.isSet("CrawlableDataset"))
                System.out.println("Looks like we are already configured, checking...");
            if (!BesAPI.isConfigured())
                System.out.println("BES IS NOT CONFIGURED!\n\n\n");
        }


    }


    public Object getConfigObject() {
        return _config;
    }


    private void getCatalog() throws PPTException, IOException, JDOMException, BadConfigurationException, BESException {

        if (_haveCatalog)
            return;

        String besPath = getBesPath();
        if (Debug.isSet("CrawlableDataset")) System.out.println("Getting catalog for: \"" + besPath + "\"");
        Document doc = BesAPI.showCatalog(besPath);
        Element topDataset = doc.getRootElement();


        if (!besPath.equals(topDataset.getChild("name").getTextTrim())) {
//            throw new IOException("Returned dataset name does not match requested name.\n" +
//                    "Requested: " + besPath + "  " +
//                    "Returned: " + topDataset.getChild("name").getTextTrim());
            System.out.println("Returned dataset name does not match requested name.\n" +
                    "Requested: " + besPath + "  " +
                    "Returned: " + topDataset.getChild("name").getTextTrim());
        }

        processDatasetElement(topDataset, this);

        _haveCatalog = true;


    }


    private void getInfo() throws PPTException, IOException, JDOMException, BadConfigurationException, BESException {

        if (_haveInfo)
            return;

        String besPath = getBesPath();
        if (Debug.isSet("CrawlableDataset")) System.out.println("Getting info for: \"" + besPath + "\"");
        Document doc = BesAPI.showInfo(besPath);
        Element topDataset = doc.getRootElement();

        if (!besPath.equals(topDataset.getChild("name").getTextTrim())) {
//            throw new IOException("Returned dataset name does not match requested name.\n" +
//                    "Requested: " + besPath + "  " +
//                    "Returned: " + topDataset.getChild("name").getTextTrim());

            System.out.println("Returned dataset name does not match requested name.\n" +
                    "Requested: " + besPath + "  " +
                    "Returned: " + topDataset.getChild("name").getTextTrim());

        }

        processDatasetElement(topDataset, this);

    }

    private void processDatasetElement(Element dataset, BESCrawlableDataset s4c) {

        // Process name
        String path = besPath2ThreddsPath(dataset.getChild("name").getTextTrim());

        s4c._name = getNameFromPath(path);
        //s4c._name = s4c._name.equals("/") ? "" : s4c._name;

        // Process size
        s4c._size = Integer.parseInt(dataset.getChild("size").getTextTrim());

        // process date and time
        String date = dataset.getChild("lastmodified").getChild("date").getTextTrim();
        String time = dataset.getChild("lastmodified").getChild("time").getTextTrim();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        s4c._lastModified = sdf.parse(date + " " + time, new ParsePosition(0));

        // Process collection (if it is one)
        String isCollection = dataset.getAttributeValue("thredds_collection");
        if (isCollection.equalsIgnoreCase("true")) {
            s4c._isCollection = true;
            s4c._childDatasetElements = dataset.getChildren("dataset");
        }

        s4c._haveInfo = true;


    }


    public String getPath() {

        return getThreddsPath();
    }

    private String getThreddsPath() {

        return _threddsPath;
    }

    private String getBesPath() {

        return _besPath;
    }

    public String getParentPath() {
        return _parentPath;
    }

    public String getName() {
        return _name;
    }

    public CrawlableDataset getParentDataset() throws IOException {

        if (_parent != null) {
            return _parent;
        }

        if (getParentPath() == null)
            return null;

        try {
            BESCrawlableDataset s4c = new BESCrawlableDataset(getParentPath(), _config);
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
        BESCrawlableDataset dataset;


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


            String newPath = this.getThreddsPath() + (this.getThreddsPath().endsWith("/") ? "" : "/") + e.getChild("name").getTextTrim();

            if (Debug.isSet("CrawlableDataset"))
                System.out.println("\n\n\nMaking new dataset \"" + newPath + "\" in listDatasets().");

            // Show me what I've got...
            //XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
            //try {
            //    xo.output(e,System.out);
            //    System.out.println("\n");
            //} catch (IOException e1) {
            //    e1.printStackTrace();
            //}
            //------

            dataset = new BESCrawlableDataset(newPath);
            processDatasetElement(e, dataset);

            dataset._parent = this;
            dataset._config = this._config;
            if (Debug.isSet("CrawlableDataset")) System.out.println("Made: " + dataset);

            childDatasets.add(dataset);


            j++;

        }

        if (Debug.isSet("CrawlableDataset")) System.out.println("List Datasets found " + j + " member(s).");

        return childDatasets;
    }

    public List listDatasets(CrawlableDatasetFilter cdf) {

        if (!isCollection())
            return null;

        List list = this.listDatasets();

        if (cdf == null) return list;

        if (Debug.isSet("CrawlableDataset")) System.out.println("Filtering CrawlableDataset list.");

        List retList = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext();) {
            CrawlableDataset curDs = (CrawlableDataset) it.next();
            if (cdf.accept(curDs)) {
                if (Debug.isSet("CrawlableDataset")) System.out.println("    Filter found matching dataset: " + curDs);
                retList.add(curDs);
            } else {
                if (Debug.isSet("CrawlableDataset")) System.out.println("    Filter discarded dataset: " + curDs);

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

    public String toString() {
        String s = "";

        s += "[CrawlableDataset  ";
        s += "<Path: " + getPath() + "> ";
        s += "<Name: " + getName() + "> ";
        s += "<Size: " + length() + "> ";
        s += "<LastModified: " + lastModified() + "> ";
        s += "<Collection: " + isCollection() + "> ";
        s += "<_haveCatalog: " + _haveCatalog + "> ";
        s += "<_haveInfo: " + _haveInfo + "> ";
        s += "<_parentPath: " + _parentPath + "> ";
        s += "<_parent.getName(): " + (_parent == null ? "null" : _parent.getName()) + "> ";
        s += "]";
        return s;

    }

}
