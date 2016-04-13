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

package opendap.noaa_s3;

import opendap.aws.s3.S3Object;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.namespaces.THREDDS;
import opendap.namespaces.XLINK;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/13/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3Index extends S3Object {

    Logger log;
    private String _bucketContext;
    private static String _index = "/index.xml";

    private Element _indexElement;

    public static final String S3_CATALOG_NAMESPACE_STRING = "http://nodc.noaa.gov/s3/catalog/1.0";
    public static final Namespace S3_CATALOG_NAMESPACE =  Namespace.getNamespace("s3c",S3_CATALOG_NAMESPACE_STRING);







    public S3Index(HttpServletRequest req, String bucketContext, String bucketName) {
        super();
        log = LoggerFactory.getLogger(getClass());
        _bucketContext = bucketContext;
        setBucketName(bucketName);
        String key = getS3IndexId(req);
        setKey(key);
        setResourceUrl(getS3Url(getBucketName(), getKey()));
    }

    public S3Index(String bucketName, String key) {
        super(bucketName,key);
        log = LoggerFactory.getLogger(getClass());
        _bucketContext = "";
        _indexElement = null;
        setResourceUrl(getS3Url(getBucketName(), getKey()));

    }

    /**
     * Creates the default root index for bucketName.
     * @param bucketName Amazon S3 bucket to create root index for.
     */
    public S3Index(String bucketName) {
        super();
        log = LoggerFactory.getLogger(getClass());
        _bucketContext = "";
        setBucketName(bucketName);
        String key = "/"+_index;
        setKey(key);
        setResourceUrl(getS3Url(getBucketName(), getKey()));
        _indexElement = null;
    }


    public S3Index(String bucketName, String key, String s3CacheRoot) {
        super(bucketName, key, s3CacheRoot);
        log = LoggerFactory.getLogger(getClass());
        _bucketContext = "";
        setResourceUrl(getS3Url(getBucketName(), getKey()));
        _indexElement = null;
    }




    public Element getIndexElement() throws JDOMException, IOException {
        //@todo Check last modified time and refresh as needed.
        if(_indexElement == null) {
            loadIndex();
        }
        return _indexElement;
    }




    private void loadIndex() throws IOException, JDOMException {

        log.debug("loadIndex() - BEGIN [bucket:{}][key:{}]",getBucketName(),getKey());
        log.debug("loadIndex() - indexUrl: " + getResourceUrl());

        Document indexDoc = opendap.xml.Util.getDocument(getResourceAsStream());

        _indexElement = indexDoc.getRootElement();
        _indexElement.detach();

        log.debug("loadIndex(): Retrieved S3 index document.");
        log.debug("loadIndex() - indexUrl: " + getResourceUrl());

        log.debug("loadIndex() - END");


    }


    public String getBucketContext(){
        return _bucketContext;
    }

    public void setBucketContext(String bucketContext){
        _bucketContext = bucketContext;
    }



    String getIndexBase() throws JDOMException, IOException {
        Element index = getIndexElement();
        return index.getAttributeValue("base");
    }

    String getIndexPath() throws JDOMException, IOException {
        Element index = getIndexElement();
        return index.getAttributeValue("path");
    }

    String getIndexDelimiter() throws JDOMException, IOException {
        Element index = getIndexElement();
        return index.getAttributeValue("delimiter");
    }



    private HashMap<String, Element> getThreddsCatalogServices(){
        HashMap<String,Element> services = new HashMap<String, Element>();


        // Define the DAP service
        Element service = new Element(THREDDS.SERVICE, THREDDS.NS);
        service.setAttribute("name","dap");
        service.setAttribute("serviceType","OPeNDAP");
        service.setAttribute("base",S3CatalogManager.theManager().getDapServiceContext());

        services.put("dap",service);


        return services;
    }





    public Element getThreddsCatalog() throws JDOMException, IOException {

        Element threddsCatalog = new Element(THREDDS.CATALOG,THREDDS.NS);

        HashMap<String,Element> services = getThreddsCatalogServices();

        threddsCatalog.addNamespaceDeclaration(XLINK.NS);

        for(Element service:services.values()){
            threddsCatalog.addContent(service);
        }

        Element catalogDataset = new Element(THREDDS.DATASET,THREDDS.NS);

        String name = getIndexPath().equals("")?getIndexDelimiter():getIndexPath();

        StringBuilder id = new StringBuilder();


        String catalogServiceContext = S3CatalogManager.theManager().getCatalogServiceContext();

        id.append(catalogServiceContext).append(name);

        catalogDataset.setAttribute("name",name);
        catalogDataset.setAttribute("ID",id.toString());

        catalogDataset.addContent(getThreddsCatalogRefs(catalogServiceContext));
        catalogDataset.addContent(getThreddsDatasets(services));

        threddsCatalog.addContent(catalogDataset);

        return threddsCatalog;

    }






    /**
     *   <file name="FGDC_meta_0077816_long_version.xml" last-modified="2013-02-01T17:43:44.000Z" size="152333"/>
     * @return
     */
    public Vector<Element> getThreddsDatasets(HashMap<String,Element> services) throws JDOMException, IOException {

        Vector<Element> threddsDatasets = new Vector<Element>();

        String indexPath = getIndexPath();
        String delim = getIndexDelimiter();


        List filesList = _indexElement.getChildren("file", S3_CATALOG_NAMESPACE);

        for(Object o: filesList){

            Element file = (Element) o;
            String fileName = file.getAttributeValue("name");
            String fileLastModified = file.getAttributeValue("last-modified");
            String fileSize = file.getAttributeValue("size");

            if(!fileName.equals("")) {

                Element dataset = new Element(THREDDS.DATASET, THREDDS.NS);
                Element dataSize = new Element(THREDDS.DATASIZE, THREDDS.NS);
                Element date = new Element(THREDDS.DATE, THREDDS.NS);

                dataset.addContent(dataSize);
                dataset.addContent(date);


                StringBuilder id = new StringBuilder();
                id.append(indexPath).append(delim).append(fileName);

                dataset.setAttribute("name",fileName);
                dataset.setAttribute("ID",id.toString());

                dataSize.setAttribute("units","bytes");
                dataSize.setText(fileSize);

                date.setAttribute("type","modified");
                date.setText(fileLastModified);


                // String s3DatasetUrl =  getS3DatasetUrl(fileName);
                // String urlPath = "/" + encoder.encode(s3DatasetUrl.toString());

                String urlPath =  getS3ResourceID(fileName);


                for(String serviceName:services.keySet()){

                    Element access = new Element(THREDDS.ACCESS, THREDDS.NS);

                    access.setAttribute("serviceName",serviceName);

                    access.setAttribute("urlPath",urlPath);

                    dataset.addContent(access);

                }


                threddsDatasets.add(dataset);
            }

        }


        return threddsDatasets;

    }






    /**
     *    <folder name="data" size="6252136166" count="1805"/>
     *
     *    <thredds:catalogRef name="data" xlink:href="/context/data/catalog.xml" xlink:title="data" xlink:type="simple" ID="/context/navid/"/>
     *
     *
     * @param catalogServiceContext The "service context" in which the catalog operates.
     * @return A Vector of Element objects representing the XML THREDDS catalogRef elements contained in the THREDDS catalog
     * node represented by the index file.
     * @throws java.io.IOException
     * @throws org.jdom.JDOMException
     */
    public Vector<Element> getThreddsCatalogRefs(String catalogServiceContext) throws JDOMException, IOException {

        Vector<Element> catalogRefs = new Vector<Element>();


        Element indexElement = getIndexElement();


        List foldersList = indexElement.getChildren("folder", S3_CATALOG_NAMESPACE);

        for(Object o: foldersList){
            Element folder = (Element) o;
            String folderName = folder.getAttributeValue("name");


            Element catalogRef = new Element(THREDDS.CATALOG_REF,THREDDS.NS);

            catalogRef.setAttribute("name",folderName);
            catalogRef.setAttribute("title",folderName,XLINK.NS);
            catalogRef.setAttribute("type","simple",XLINK.NS);


            StringBuilder id = new StringBuilder();

            id.append(getIndexPath()).append(getIndexDelimiter()).append(folderName).append(getIndexDelimiter());

            catalogRef.setAttribute("ID",id.toString());

            StringBuilder href = new StringBuilder();

            href.append(catalogServiceContext).append(_bucketContext).append(id).append("catalog.xml");

            catalogRef.setAttribute("href",href.toString(),XLINK.NS);

            catalogRefs.add(catalogRef);
        }


        return catalogRefs;

    }


    /**
     * 
     * @param fileName
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public String getS3ResourceID(String fileName) throws JDOMException, IOException {

        StringBuilder resourceId =  new StringBuilder();
        resourceId.append(_bucketContext).append(getIndexPath()).append(getIndexDelimiter()).append(fileName);

        return resourceId.toString();
    }



    private  String getS3IndexId(HttpServletRequest request){

        StringBuilder sb = new StringBuilder();

        String collectionName = getCollectionName(request);

        sb.append(collectionName).append(_index);

        return sb.toString();

    }



    private  String getCollectionName(HttpServletRequest request){

        String relativeUrl = ReqInfo.getLocalUrl(request);
        if(relativeUrl.startsWith(_bucketContext))
            relativeUrl = relativeUrl.substring(_bucketContext.length());

        String collectionName  = Scrub.urlContent(relativeUrl);

        if(collectionName.endsWith("/contents.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("contents.html"));
        }
        else if(collectionName.endsWith("/catalog.html")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("catalog.html"));
        }
        else if(collectionName.endsWith("/catalog.xml")){
            collectionName = collectionName.substring(0,collectionName.lastIndexOf("catalog.xml"));
        }

        if(!collectionName.endsWith("/"))
            collectionName += "/";

        while(collectionName.startsWith("//"))
            collectionName = collectionName.substring(1);

        log.debug("getCollectionName() returning  "+collectionName);

        return collectionName;
    }


    /**
     *
     * <pre>
     * <?xml version="1.0" encoding="UTF-8"?>
     * <?xml-stylesheet type='text/xsl' href='/ocean-archive.data.nodc.noaa.gov//index.xsl'?>
     * <index xmlns="http://nodc.noaa.gov/s3/catalog/1.0" base="http://ocean-archive.data.nodc.noaa.gov" path="" name="ocean-archive.data.nodc.noaa.gov" delimiter="/" encoding="UTF-8">
     *   <folder name="0000841" size="12374900179" count="89099"/>
     *   <folder name="0001467" size="134979214" count="86"/>
     *   <folder name="0043269" size="248832168394" count="61910"/>
     *   <folder name="0077816" size="18764208623" count="5442"/>
     *   <folder name="0087989" size="502782436280" count="288"/>
     *   <folder name="0095107" size="221388385700" count="8946"/>
     *   <folder name="0097969" size="192721819112" count="1834"/>
     *   <folder name="0099041" size="3501057505212" count="671756"/>
     *   <folder name="0099042" size="57510702097" count="4649"/>
     *   <folder name="ocean-archive.data.nodc.noaa.gov" size="6508" count="7"/>
     * </index>
     * </pre>
     *
     *
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public Vector<String> getChildIndexKeys() throws JDOMException, IOException{
        Vector<String> childIndexKeys = new Vector<String>();

        Element indexElement = getIndexElement();

        List foldersList = indexElement.getChildren("folder", S3_CATALOG_NAMESPACE);
        for(Object o: foldersList){
            Element folder = (Element) o;
            String folderName = folder.getAttributeValue("name");

            StringBuilder key = new StringBuilder();

            key.append(getIndexPath())
                    .append(getIndexDelimiter())
                    .append(folderName)
                    .append(getIndexDelimiter())
                    .append(_index);



            childIndexKeys.add(key.toString());
        }

        return childIndexKeys;

    }





    public Vector<String> getChildIndexKeys(boolean recurse, int maxLevels) throws JDOMException, IOException{
        return  getChildIndexKeys(recurse, maxLevels, 0);
    }

    private  Vector<String> getChildIndexKeys(boolean recurse, int maxLevels, int level) throws JDOMException, IOException{

        Vector<String> childIndexKeys = getChildIndexKeys();

        Vector<String> grandChildIndexKeys;
        if(recurse && ((maxLevels==0) || (level<maxLevels))) {
            Vector<String> descendaents = new Vector<String>();
            for(String childIndexKey: childIndexKeys){

                S3Index childIndex = new S3Index(getBucketName(),childIndexKey,getS3CacheRoot());

                grandChildIndexKeys = childIndex.getChildIndexKeys(recurse, maxLevels, level + 1);
                descendaents.addAll(grandChildIndexKeys);
            }
            childIndexKeys.addAll(descendaents);
        }

        return   childIndexKeys;

    }


    public Vector<S3Index> getChildren() throws JDOMException, IOException {

        Vector<String> childIndexKeys = getChildIndexKeys();

        Vector<S3Index> children = new Vector<S3Index>();

        for(String childIndexKey: childIndexKeys){

            S3Index childIndex = new S3Index(getBucketName(),childIndexKey,getS3CacheRoot());
            childIndex.setBucketContext(getBucketContext());

            children.add(childIndex);

        }
        return children;

    }


    public Vector<S3Index> getChildren(boolean recurse, int maxLevels) throws JDOMException, IOException{
        return    getChildren(recurse, maxLevels, 0);
    }


    public Vector<S3Index> getChildren(boolean recurse, int maxLevels, int level)throws JDOMException, IOException{
        Vector<S3Index> children = getChildren();

        Vector<S3Index> grandChildren;
        if(recurse && ((maxLevels==0) || (level<maxLevels))) {
            Vector<S3Index> descendaents = new Vector<S3Index>();
            for(S3Index child: children){
                grandChildren = child.getChildren(recurse, maxLevels, level + 1);
                descendaents.addAll(grandChildren);
            }
            children.addAll(descendaents);
        }

        return children;

    }








    public void updateCachedIndexAsNeeded(boolean recurse, int maxLevels) throws JDOMException, IOException{
        updateCachedIndexAsNeeded(recurse, maxLevels, 0);
    }




    private void updateCachedIndexAsNeeded(boolean recurse, int maxLevels, int level)throws JDOMException, IOException{

        if(getS3CacheRoot()==null)
            throw new IOException("updateCachedIndexAsNeeded() - s3CacheRootDirectory has not been set.");

        log.debug("updateCachedIndexAsNeeded() - key:  {}    level: {}",getKey(),level);
        updateCachedObjectAsNeeded();

        if(recurse && ((maxLevels==0) || (level<maxLevels))) {
            Vector<S3Index> children = getChildren();
            for(S3Index child: children){
                child.updateCachedIndexAsNeeded(recurse, maxLevels, level + 1);
            }
        }
    }







    /**
     *
     * <pre>
     * <?xml version="1.0" encoding="UTF-8"?>
     * <?xml-stylesheet type='text/xsl' href='/ocean-archive.data.nodc.noaa.gov//index.xsl'?>
     * <index xmlns="http://nodc.noaa.gov/s3/catalog/1.0" base="http://ocean-archive.data.nodc.noaa.gov" path="" name="ocean-archive.data.nodc.noaa.gov" delimiter="/" encoding="UTF-8">
     *   <folder name="0000841" size="12374900179" count="89099"/>
     *   <folder name="0001467" size="134979214" count="86"/>
     *   <folder name="0043269" size="248832168394" count="61910"/>
     *   <folder name="0077816" size="18764208623" count="5442"/>
     *   <folder name="0087989" size="502782436280" count="288"/>
     *   <folder name="0095107" size="221388385700" count="8946"/>
     *   <folder name="0097969" size="192721819112" count="1834"/>
     *   <folder name="0099041" size="3501057505212" count="671756"/>
     *   <folder name="0099042" size="57510702097" count="4649"/>
     *   <folder name="ocean-archive.data.nodc.noaa.gov" size="6508" count="7"/>
     * </index>
     * </pre>
     *
     *
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public Vector<String> getChildResourceKeys() throws JDOMException, IOException{
        Vector<String> childResourceKeys = new Vector<String>();


        Element indexElement = getIndexElement();

        List fileList = indexElement.getChildren("file", S3_CATALOG_NAMESPACE);
        for(Object o: fileList){
            Element file = (Element) o;
            String fileName = file.getAttributeValue("name");

            StringBuilder key = new StringBuilder();

            key.append(getIndexPath())
                    .append(getIndexDelimiter())
                    .append(fileName);

            childResourceKeys.add(key.toString());
        }


        return childResourceKeys;

    }

    public Vector<String> getChildResourceKeys(boolean recurse, int maxLevels) throws JDOMException, IOException{
        return    getChildResourceKeys(recurse, maxLevels, 0);
    }


    public Vector<String> getChildResourceKeys(boolean recurse, int maxLevels, int level)throws JDOMException, IOException{
        Vector<S3Index> children = getChildren();

        Vector<String> childResourceKeys = getChildResourceKeys();

        Vector<String> grandChildResourceKeys;
        Vector<String> descendants = new Vector<String>();
        if(recurse && ((maxLevels==0) || (level<maxLevels))) {
            for(S3Index child: children){
                grandChildResourceKeys = child.getChildResourceKeys(recurse, maxLevels, level + 1);
                descendants.addAll(grandChildResourceKeys);
            }
            childResourceKeys.addAll(descendants);
            descendants.clear();
        }

        return childResourceKeys;

    }


    /**
     *
     * <pre>
     * <?xml version="1.0" encoding="UTF-8"?>
     * <?xml-stylesheet type='text/xsl' href='/ocean-archive.data.nodc.noaa.gov//index.xsl'?>
     * <index xmlns="http://nodc.noaa.gov/s3/catalog/1.0" base="http://ocean-archive.data.nodc.noaa.gov" path="" name="ocean-archive.data.nodc.noaa.gov" delimiter="/" encoding="UTF-8">
     *   <folder name="0000841" size="12374900179" count="89099"/>
     *   <folder name="0001467" size="134979214" count="86"/>
     *   <folder name="0043269" size="248832168394" count="61910"/>
     *   <folder name="0077816" size="18764208623" count="5442"/>
     *   <folder name="0087989" size="502782436280" count="288"/>
     *   <folder name="0095107" size="221388385700" count="8946"/>
     *   <folder name="0097969" size="192721819112" count="1834"/>
     *   <folder name="0099041" size="3501057505212" count="671756"/>
     *   <folder name="0099042" size="57510702097" count="4649"/>
     *   <folder name="ocean-archive.data.nodc.noaa.gov" size="6508" count="7"/>
     * </index>
     * </pre>
     *
     *
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public Vector<S3IndexedFile> getIndexedFiles() throws JDOMException, IOException{
        Vector<S3IndexedFile> childIndexedFiles = new Vector<S3IndexedFile>();


        Element indexElement = getIndexElement();

        List fileList = indexElement.getChildren("file", S3_CATALOG_NAMESPACE);
        for(Object o: fileList){
            Element file = (Element) o;

            String fileName = file.getAttributeValue("name");
            StringBuilder key = new StringBuilder();
            key.append(getIndexPath())
                    .append(getIndexDelimiter())
                    .append(fileName);



            String lmtString = file.getAttributeValue("last-modified");
            long lmt = getTimeFromLMTString(lmtString);

            long size;
            String sizeString = file.getAttributeValue("size");
            try {
                size = Integer.parseInt(sizeString);
            } catch (NumberFormatException e) {
                size = -1;
            }

            S3IndexedFile s3if = new S3IndexedFile(getBucketName(),key.toString(),lmt, size);
            s3if.setS3CacheRoot(getS3CacheRoot());

            childIndexedFiles.add(s3if);
        }


        return childIndexedFiles;

    }


    /**
     *   <file name="JA2_GPN_2PdP097_015_20110219_120533_20110219_130146.nc" last-modified="2013-07-02T20:00:54.000Z" size="6386228"/>
     */

    private long getTimeFromLMTString(String lmtString){
        long lmt;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
            if(lmtString.endsWith("Z"))
                lmtString = lmtString.substring(0,lmtString.lastIndexOf('Z')) + "GMT";
            Date d = format.parse(lmtString);
            lmt = d.getTime();
        } catch (Exception e) {
            log.error("ERROR: Failed to parse last modified time string {}  MESSAGE: {}",lmtString,e.getMessage());
            lmt = -1;
        }
        return lmt;


    }


    public Vector<S3IndexedFile> getChildIndexedFiles(boolean recurse, int maxLevels) throws JDOMException, IOException{
        return    getChildIndexedFiles(recurse, maxLevels, 0);
    }


    public Vector<S3IndexedFile> getChildIndexedFiles(boolean recurse, int maxLevels, int level)throws JDOMException, IOException{
        Vector<S3Index> children = getChildren();

        Vector<S3IndexedFile> childResourceObjects = getIndexedFiles();

        Vector<S3IndexedFile> grandChildObjects;
        Vector<S3IndexedFile> descendants = new Vector<S3IndexedFile>();
        if(recurse && ((maxLevels==0) || (level<maxLevels))) {
            for(S3Index child: children){
                grandChildObjects = child.getChildIndexedFiles(recurse, maxLevels, level + 1);
                descendants.addAll(grandChildObjects);
            }
            childResourceObjects.addAll(descendants);
            descendants.clear();
        }

        return childResourceObjects;

    }


    public static String getCatalogIndexString(){
        return _index;
    }



}
