/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
//
// Copyright (c) 2006 OPeNDAP, Inc.
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
import org.slf4j.Logger;
import opendap.ppt.PPTException;

/**
 * This implmentation of the THREDDS CrawlableDataset interface provides the connection
 * between the core THREDDS functionalities and the BES.
 *
 * User: ndp
 * Date: Dec 4, 2005
 * Time: 8:08:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class BESCrawlableDataset implements CrawlableDataset, Comparable {

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

    private Logger log;


    public static BESCrawlableDataset getRootDataset() throws IOException, PPTException, BadConfigurationException, BESException, JDOMException {
        return new BESCrawlableDataset("/root", null);
    }

    public BESCrawlableDataset(String path, Object o) throws IOException, PPTException, BadConfigurationException, JDOMException, BESException {

        //Debug.set("CrawlableDataset", true);
        init();

        _config = (Element) o;

        log.debug("BESCrawlableDataset config: " + _config);

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
        log = org.slf4j.LoggerFactory.getLogger(getClass());

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

            log.debug("getPath()        = " + getPath());
            log.debug("getThreddsPath() = " + getThreddsPath());
            log.debug("getBesPath()     = " + getBesPath());
            log.debug("getName()        = " + getName());
            log.debug("getParentPath()  = " + getParentPath());

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
        //_path = path.equals("") ? "/root" : path; // Does THREDDS want the top to be "/" or empty??
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

            try {
                BesAPI.configure(new BESConfig(_config));
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            log.debug("Looks like we are already configured, checking...");
        }

        if (!BesAPI.isConfigured())
            log.error("\n\n\n!!!!!!!!! BES IS NOT CONFIGURED !!!!!!!!\n\n\n");

    }


    public Object getConfigObject() {
        return _config;
    }


    private void getCatalog() throws PPTException, IOException, JDOMException, BadConfigurationException, BESException {

        if (_haveCatalog)
            return;

        String besPath = getBesPath();
        log.debug("Getting catalog for: \"" + besPath + "\"");
        Document doc = BesAPI.showCatalog(besPath);
        Element topDataset = doc.getRootElement();


        if (!besPath.equals(topDataset.getChild("name").getTextTrim())) {
//            throw new IOException("Returned dataset name does not match requested name.\n" +
//                    "Requested: " + besPath + "  " +
//                    "Returned: " + topDataset.getChild("name").getTextTrim());
            log.warn("Returned dataset name does not match requested name.\n" +
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
        log.debug("Getting info for: \"" + besPath + "\"");
        Document doc = BesAPI.getInfoDocument(besPath);
        Element topDataset = doc.getRootElement();

        if (!besPath.equals(topDataset.getChild("name").getTextTrim())) {
//            throw new IOException("Returned dataset name does not match requested name.\n" +
//                    "Requested: " + besPath + "  " +
//                    "Returned: " + topDataset.getChild("name").getTextTrim());

            log.warn("Returned dataset name does not match requested name.\n" +
                    "Requested: " + besPath + "  " +
                    "Returned: " + topDataset.getChild("name").getTextTrim());

        }

        processDatasetElement(topDataset, this);

    }

    private void processDatasetElement(Element dataset, BESCrawlableDataset cds) {

        // Process name
        String path = besPath2ThreddsPath(dataset.getChild("name").getTextTrim());

        cds._name = getNameFromPath(path);
        //cds._name = cds._name.equals("/") ? "" : cds._name;

        // Process size
        cds._size = Integer.parseInt(dataset.getChild("size").getTextTrim());

        // process date and time
        String date = dataset.getChild("lastmodified").getChild("date").getTextTrim();
        String time = dataset.getChild("lastmodified").getChild("time").getTextTrim();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

        cds._lastModified = sdf.parse(date + " " + time+" UTC", new ParsePosition(0));

        // Process collection (if it is one)
        String isCollection = dataset.getAttributeValue("thredds_collection");
        if (isCollection.equalsIgnoreCase("true")) {
            cds._isCollection = true;
            cds._childDatasetElements = dataset.getChildren("dataset");
        }

        cds._haveInfo = true;


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
            BESCrawlableDataset cds = new BESCrawlableDataset(getParentPath(), _config);
            _parent = cds;
            return cds;
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
        Vector <BESCrawlableDataset> childDatasets  = new Vector <BESCrawlableDataset>() ;
        for (Object _childDatasetElement : _childDatasetElements) {

            e = (Element) _childDatasetElement;


            String newPath = this.getThreddsPath() +
                            (this.getThreddsPath().endsWith("/") ? "" : "/") +
                             e.getChild("name").getTextTrim();

            log.debug("Making new dataset \"" + newPath + "\" in listDatasets().");

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
            log.debug("Made: " + dataset);


            childDatasets.add(dataset);

            j++;

        }

        // Sort them by name
        Collections.sort(childDatasets);


        log.debug("List Datasets found " + j + " member(s).");

        return childDatasets;
    }



    public List listDatasets(CrawlableDatasetFilter cdf) {

        if (!isCollection())
            return null;

        List list = this.listDatasets();

        if (cdf == null) return list;

        log.debug("Filtering CrawlableDataset list.");

        List <CrawlableDataset> retList = new ArrayList<CrawlableDataset>();
        for (Object aList : list) {
            CrawlableDataset curDs = (CrawlableDataset) aList;
            if (cdf.accept(curDs)) {
                log.debug("    Filter found matching dataset: " + curDs);
                retList.add(curDs);
            } else {
                log.debug("    Filter discarded dataset: " + curDs);

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

    /**
     * Compares this BESCrawlableDataset to the passed one based on on a lexicographically assement of
     * their names.
     * @param o
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to,
     * or greater than the specified object.
     */
    public int compareTo(Object o) {

        return _name.compareTo(((BESCrawlableDataset)o).getName());
    }
}
