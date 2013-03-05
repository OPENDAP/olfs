/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.noaa_s3;

import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.gateway.HexAsciiEncoder;
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
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/13/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3Index extends RemoteResource {

    Logger log;
    private String _bucketName;
    private String _index = "/index.xml";
    private String _s3ObjIdForIndex;

    private Element _indexElement;

    public static final String S3_CATALOG_NAMESPACE_STRING = "http://nodc.noaa.gov/s3/catalog/1.0";
    public static final Namespace S3_CATALOG_NAMESPACE =  Namespace.getNamespace("s3c",S3_CATALOG_NAMESPACE_STRING);


    private ConcurrentHashMap<String, S3Index> parents;
    private ConcurrentHashMap<String, S3Index> children;




    public S3Index(HttpServletRequest req, String bucketName) {
        super();
        log = LoggerFactory.getLogger(getClass());
        _bucketName = bucketName;
        _s3ObjIdForIndex = getS3IndexObjectString(req);
        _indexElement = null;
        //_useMemoryCache = useMemCache;
        String resourceUrl = getS3IndexUrlString();
        setResourceUrl(resourceUrl);
    }





    public String getBucketName(){
        return _bucketName;
    }

    public String gets3IndexObjectId(){
        return _s3ObjIdForIndex;
    }


    public Element getIndexElement() throws JDOMException, IOException {
        if(_indexElement == null) {
            loadIndexObject();
        }
        return _indexElement;
    }


    /**
     *
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    public void loadIndexObject_OLD() throws IOException, JDOMException {

        String indexObjUrl = getS3IndexUrlString();

        log.debug("indexObjUrl: " + indexObjUrl);


         _indexElement = opendap.xml.Util.getDocumentRoot(indexObjUrl);

        _indexElement.detach();





        //List filesList = _indexElement.getChildren("file", S3_CATALOG_NAMESPACE);
        //List foldersList = _indexElement.getChildren("folder", S3_CATALOG_NAMESPACE);

    }



    private void loadIndexObject() throws IOException, JDOMException {

        String indexObjUrl = getS3IndexUrlString();

        log.debug("indexObjUrl: " + indexObjUrl);



        Document indexDoc = opendap.xml.Util.getDocument(getResourceAsStream());

        _indexElement = indexDoc.getRootElement();
        _indexElement.detach();

        log.debug("getIndexDocumentRoot(): Retrieved S3 index document.");



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






    public Element getThreddsCatalog(String catalogServiceContext, HashMap<String,Element> services) throws JDOMException, IOException {

        Element threddsCatalog = new Element(THREDDS.CATALOG,THREDDS.NS);

        threddsCatalog.addNamespaceDeclaration(XLINK.NS);

        for(Element service:services.values()){
            threddsCatalog.addContent(service);
        }

        Element catalogDataset = new Element(THREDDS.DATASET,THREDDS.NS);

        String name = getIndexPath().equals("")?getIndexDelimiter():getIndexPath();

        StringBuilder id = new StringBuilder();

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
        HexAsciiEncoder encoder = new HexAsciiEncoder();

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

                String urlPath =  getS3DatasetResourceID(fileName);


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


    public String getS3DatasetUrl(String fileName) throws JDOMException, IOException {

        StringBuilder s3DatasetUrl =  new StringBuilder();
        s3DatasetUrl.append(getIndexBase()).append(getIndexPath()).append(getIndexDelimiter()).append(fileName);

        return s3DatasetUrl.toString();
    }


    public String getS3DatasetResourceID(String fileName) throws JDOMException, IOException {

        StringBuilder s3DatasetUrl =  new StringBuilder();
        s3DatasetUrl.append(getIndexPath()).append(getIndexDelimiter()).append(fileName);

        return s3DatasetUrl.toString();
    }






    /**
     *    <folder name="data" size="6252136166" count="1805"/>
     *
     *    <thredds:catalogRef name="data" xlink:href="/context/data/catalog.xml" xlink:title="data" xlink:type="simple" ID="/context/navid/"/>
     *
     *
     * @param catalogServiceContext
     * @return
     * @throws java.io.IOException
     * @throws org.jdom.JDOMException
     */
    public Vector<Element> getThreddsCatalogRefs(String catalogServiceContext) throws JDOMException, IOException {

        Vector<Element> catalogRefs = new Vector<Element>();

        String indexPath = getIndexPath();
        String delim = getIndexDelimiter();


        List filesList = _indexElement.getChildren("folder", S3_CATALOG_NAMESPACE);

        for(Object o: filesList){
            Element folder = (Element) o;
            String folderName = folder.getAttributeValue("name");
            String folderCount = folder.getAttributeValue("count");
            String folderSize = folder.getAttributeValue("size");


            Element catalogRef = new Element(THREDDS.CATALOG_REF,THREDDS.NS);

            catalogRef.setAttribute("name",folderName);
            catalogRef.setAttribute("title",folderName,XLINK.NS);
            catalogRef.setAttribute("type","simple",XLINK.NS);


            StringBuilder id = new StringBuilder();

            id.append(getIndexPath()).append(getIndexDelimiter()).append(folderName).append(getIndexDelimiter());

            catalogRef.setAttribute("ID",id.toString());

            StringBuilder href = new StringBuilder();

            href.append(catalogServiceContext).append(id).append("catalog.xml");

            catalogRef.setAttribute("href",href.toString(),XLINK.NS);

            catalogRefs.add(catalogRef);
        }


        return catalogRefs;

    }


    public String getS3IndexUrlString(){

        StringBuilder sb = new StringBuilder();

        sb.append("http://").append(getBucketName()).append(".s3.amazonaws.com").append(gets3IndexObjectId());

        return sb.toString();

    }


    private  String getS3IndexObjectString(HttpServletRequest request){

        StringBuilder sb = new StringBuilder();

        String collectionName = getCollectionName(request);

        sb.append(collectionName).append(_index);

        return sb.toString();

    }



    private  String getCollectionName(HttpServletRequest request){

        String relativeUrl = ReqInfo.getLocalUrl(request);
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


}
