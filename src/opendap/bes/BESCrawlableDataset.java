/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2007 OPeNDAP, Inc.
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

package opendap.bes;

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
 * <p/>
 * User: ndp
 * Date: Dec 4, 2005
 * Time: 8:08:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class BESCrawlableDataset implements CrawlableDataset, Comparable {

    private String _threddsPath;
    private String _besPath;
    private String _name;
    private int _length;
    private Date _lastModified;

    private boolean _isCollection;
    private boolean _isData;

    private String _parentPath;
    private BESCrawlableDataset _parent;

    private List _childDatasetElements;

    //private boolean  _isConfigured;
    private boolean _haveCatalog;
    private boolean _haveInfo;
    private boolean _exists;


    private Element _config;

    private Logger log;


    private static String _datasetRootPrefix = "/bes";


    public BESCrawlableDataset(String path, Object o)  {

        //Debug.set("CrawlableDataset", true);
        init();

        // Config is not currently used, we just save it anyway.
        _config = (Element) o;
        log.debug("BESCrawlableDataset config: " + _config);

        // We just check the BES to make sure it's configured. It should always
        // be configured at this point!
        if (!BesAPI.isConfigured()) {
            log.error("\n\n\n!!!!!!!!! BES IS NOT CONFIGURED !!!!!!!!\n\n\n");
            //throw new BESException("BES not configured!!!!");
        }


        processPath(path);

    }

    public static String getDatasetRootPrefix()  {
        return _datasetRootPrefix;
    }



    /**
     * Returns the path for this dataset.
     * <b>Does not initiate or require previous BES interaction.</b>
     *
     * @return The path for this dataset.
     */
    public String getPath() {

        return getThreddsPath();
    }


    /**
     * Returns the path to the parent of this dataset.
     * <b>Does not initiate or require previous BES interaction.</b>
     *
     * @return The path to the parent of this dataset.
     */
    public String getParentPath() {
        return _parentPath;
    }

    /**
     * Returns the name of this dataset. Equivalent to the calling
     * the UNIX command <code>basename</code> on the <code>path</code>
     * of this dataset.
     * <b>Does not initiate or require previous BES interaction.</b>
     *
     * @return The path to the parent of this dataset.
     */
    public String getName() {
        return _name;
    }


    /**
     * Returns the parent dataset of this dataset. The parent will
     * be created if neccesary.
     * <p/>
     * <b>Does not initiate or require previous BES interaction.</b>
     *
     * @return The parent of this dataset, null if there is no parent.
     */
    public CrawlableDataset getParentDataset() {

        if (_parent != null) {
            return _parent;
        }

        if (getParentPath() == null){
            log.debug("getParentDataset: Dataset "+getPath()+" has no parent. " +
                    "Returning null.");
            return null;
        }

        BESCrawlableDataset cds = new BESCrawlableDataset(getParentPath(), _config);
        _parent = cds;
        return cds;

    }

    /**
     * Compares this BESCrawlableDataset to the passed one based on on a
     * lexicographically assement of their names.
     * <p/>
     * <b>Does not initiate or require previous BES interaction.</b>
     *
     * @param o The datset to which to compare this one
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public int compareTo(Object o) {

        return _name.compareTo(((CrawlableDataset) o).getName());
    }


    /**
     * Returns the configuration object passed into the constructor of this
     * dataset. Since the configuration object is not currently used, this
     * may be null.
     * <p/>
     * <b>Does not initiate or require previous BES interaction.</b>
     *
     * @return The configuration object passed into the constructor of this
     *         dataset.
     */
    public Object getConfigObject() {
        return _config;
    }


    /**
     * Returns a dataset whose path matches the concatenation of the path to
     * this dataset and the passed string. There is no guarantee that returned
     * dataset represents a dataset that exists in the system. You'll need to
     * check that for yourself.
     * <p/>
     * <b>Does not initiate or require previous BES interaction.</b>
     *
     * @param relativePath Relative path of dataset to create.
     * @return A dataset whose path matches the concatenation of the path to
     *         this dataset and the passed string.
     */
    public BESCrawlableDataset getDescendant(String relativePath) {

        if (getPath().endsWith("/") || relativePath.indexOf("/")==0)
            return new BESCrawlableDataset(getPath() + relativePath, _config);
        else
            return new BESCrawlableDataset(getPath() + "/" + relativePath, _config);


    }


    /**
     * Returns true if this is a valid dataset (one that actually exists),
     * false otherwise.
     * <p/>
     * <b>Initiates or requires previous BES interaction.</b>
     *
     * @return True if this is a valid dataset (one that actually exists),
     *         false otherwise.
     */
    public boolean exists() {

        getInfo();

        return _exists;

    }


    /**
     * Returns true if this dataset is a collection, false otherwise.
     * <p/>
     * <b>Initiates or requires previous BES interaction.</b>
     *
     * @return True if this dataset is a collection, false otherwise
     */
    public boolean isCollection() {

        getInfo();

        return _isCollection;
    }


    /**
     * Returns a list containing all of the child datasets of this dataset.
     * <p/>
     * <b>Initiates or requires previous BES interaction.</b>
     *
     * @return The list of child datasets of this dataset, null if this dataset
     *         is not a collection.
     */
    public List listDatasets() {

        Element e;
        BESCrawlableDataset dataset;


        if (!isCollection()){
            log.error("listDatasets(): This dataset is not a collection.");
            return null;
        }

        try {
            getCatalog();
        }
        catch (Exception ex) {
            log.error("listDatasets(): Cannot get catalog from BES for " +
                    "dataset " + getPath() + "  Returning null.", ex);
            return null;
        }

        int j = 0;
        Vector<BESCrawlableDataset> childDatasets = new Vector<BESCrawlableDataset>();
//        try {
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

                dataset = new BESCrawlableDataset(newPath, _config);
                try {
                    processDatasetElement(e, dataset);
                }
                catch(BESError besex){
                    log.error("listDatasets(): Failed to process BES catalog element for " +
                            "dataset " + getPath() + "  Returning null."+besex);
                    return null;

                }

                dataset._parent = this;
                dataset._config = this._config;
                log.debug("Made: " + dataset);


                childDatasets.add(dataset);

                j++;

            }
//        }
//        catch (BESException b) {
//
//            log.error("listDatasets():  BESException caught. ", b);
//        }

        // Sort them by name
        Collections.sort(childDatasets);


        log.debug("List Datasets found " + j + " member(s).");

        return childDatasets;
    }


    /**
     * Returns a list containing all of the child datasets of this dataset
     * filtered using the passed <code>CrawlableDatasetFilter</code>
     * <p/>
     * <p/>
     * <b>Initiates or requires previous BES interaction.</b>
     *
     * @param cdf The dataset filter to apply to the list pf child datasets.
     * @return A filtered list of datasets, null if this dataset is not a
     *         collection.
     */
    public List listDatasets(CrawlableDatasetFilter cdf) {

        if (!isCollection()){
            log.error("listDatasets(): This dataset is not a collection. " +
                    "There is no collection to filter.");
            return null;
        }

        List list = this.listDatasets();

        if (cdf == null) return list;

        log.debug("Filtering CrawlableDataset list.");

        List<CrawlableDataset> retList = new ArrayList<CrawlableDataset>();
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


    /**
     * Returns the size, in bytes, of this dataset.
     * <p/>
     * <b>Initiates or requires previous BES interaction.</b>
     *
     * @return The size in bytes of this dataset. -1 if the size is not known
     *         or if the this dataset is a collection.
     */
    public long length() {
        getInfo();
        return _length;
    }

    /**
     * Returns the last modified date of this dataset.
     * <p/>
     * <b>Initiates or requires previous BES interaction.</b>
     *
     * @return The last modified date of this dataset.
     */
    public Date lastModified() {
        getInfo();
        return _lastModified;
    }


    public boolean isData(){
        getInfo();
        return _isData;
    }

    public String toString() {
        String s = "";

        s += "[BESCrawlableDataset  ";
        s += "<_exists: " + _exists + "> ";
        s += "<_threddsPath: " + _threddsPath + "> ";
        s += "<_name: " + _name + "> ";
        s += "<_length: " + _length + "> ";
        s += "<_lastModified: " + _lastModified + "> ";
        s += "<_isCollection: " + _isCollection + "> ";
        s += "<_haveCatalog: " + _haveCatalog + "> ";
        s += "<_haveInfo: " + _haveInfo + "> ";
        s += "<_parentPath: " + _parentPath + "> ";
        s += "<_parent.getName(): " + (_parent == null ? "null" : _parent.getName()) + "> ";
        s += "]";
        return s;

    }


    public static String besPath2ThreddsPath(String path) {
        String threddsPath;

        if (path.startsWith(_datasetRootPrefix))
            threddsPath = path;
        else {
            if (path.startsWith("/") || path.equals(""))
                threddsPath = _datasetRootPrefix + path;
            else
                threddsPath = _datasetRootPrefix + "/" + path;
        }
        // Is path empty? Then make it "/"
        //_path = path.equals("") ? _datasetRootPrefix : path; // Does THREDDS want the top to be "/" or empty??
        //_path = _path.equals("/") ? "" : _path;   // Does THREDDS want the top to be "/" or empty??

        return threddsPath;
    }



    private void init() {

        _exists = false;
        _threddsPath = null;
        _besPath = null;
        _name = null;
        _parentPath = null;
        _lastModified = null;
        _parent = null;
        _childDatasetElements = null;
        _isCollection = false;
        _isData = false;
        _haveCatalog = false;
        _haveInfo = false;
        _config = null;
        _length = -1;

        log = org.slf4j.LoggerFactory.getLogger(getClass());

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

        return tpath.substring(_datasetRootPrefix.length());

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


    private void getCatalog() throws PPTException, IOException, JDOMException, BadConfigurationException, BESError {

        if (_haveCatalog)
            return;

        String besPath = getBesPath();
        log.debug("Getting catalog for: \"" + besPath + "\"");
        Document doc = new Document();
        if(BesAPI.showCatalog(besPath,doc)){

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
        else {
            processError(doc);
        }


        _haveCatalog = true;


    }


    /**
     * Basically set the set of this CrawableDataset to a state compatible with
     * an error.
     *
     * @param besError the error to process
     */
    private void processError(Document besError){


        init();

    }






    private void getInfo() {

        if (_haveInfo)
            return;

        try {
            String besPath = getBesPath();
            log.debug("Getting info for: \"" + besPath + "\"");




            Document doc = new Document();


            if(BesAPI.getInfoDocument(besPath,doc)){
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
            else {

                _haveInfo = true;
                _exists = false;

                log.debug("BES request for info document for: \""+besPath+"\" returned an error");

            }

        }
        catch (Exception e) {

            _haveInfo = true;
            _exists = false;

        }



    }

    private void processDatasetElement(Element dataset,
                                       BESCrawlableDataset cds)
            throws BESError {

        Element e;
        String s;
        // -- Process name
        String path = besPath2ThreddsPath(dataset.getChild("name").getTextTrim());

        cds._name = getNameFromPath(path);
        //cds._name = cds._name.equals("/") ? "" : cds._name;
        // --- --- --- ---



        // -- Process size
        e = dataset.getChild("size");
        if(e==null)
            throw new BESError("Catalog <dataset> element is missing " +
                "child element <size>.");
        s = e.getTextTrim();
        cds._length = Integer.parseInt(s);
        // --- --- --- ---



        // --- Process date
        e = dataset.getChild("lastmodified");
        if(e==null)
            throw new BESError("Catalog <dataset> element is missing " +
                "child element <lastmodified>.");

        e = e.getChild("date");
        if(e==null)
            throw new BESError("Catalog <lastmodified> element is missing " +
                "child element <date>.");

        s = e.getTextTrim();
        String date = s;
        // --- --- --- ---




        // -- Process time
        e = dataset.getChild("lastmodified");
        if(e==null)
            throw new BESError("Catalog <dataset> element is missing " +
                "child element <lastmodified>.");

        e = e.getChild("time");
        if(e==null)
            throw new BESError("Catalog <lastmodified> element is missing " +
                "child element <time>.");

        String time = e.getTextTrim();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

        cds._lastModified = sdf.parse(date + " " + time + " UTC", new ParsePosition(0));
        // --- --- --- ---



        // -- Process collection (if it is one)
        String isCollection = dataset.getAttributeValue("thredds_collection");
        if(isCollection==null)
            throw new BESError("Catalog <dataset> element is missing " +
                "attribute  \"thredds_collection\"");

        if (isCollection.equalsIgnoreCase("true")) {
            cds._isCollection = true;
            cds._childDatasetElements = dataset.getChildren("dataset");
        }
        // --- --- --- ---





        // -- Process isData flag
        String isData = dataset.getAttributeValue("isData");
        if(isData==null)
            throw new BESError("Catalog <dataset> element is missing " +
                "attribute  \"isData\"");
        cds._isData = isData.equalsIgnoreCase("true");

        cds._haveInfo = true;
        cds._exists = true;


    }


    private String getThreddsPath() {

        return _threddsPath;
    }

    private String getBesPath() {

        return _besPath;
    }


}
