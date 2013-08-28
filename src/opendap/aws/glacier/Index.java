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

package opendap.aws.glacier;

import opendap.namespaces.THREDDS;
import opendap.namespaces.XLINK;
import opendap.noaa_s3.S3CatalogManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;



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
 *   <file name="" last-modified="2013-02-01T17:37:21.000Z" size="0"/>
 *   <file name="mooshuupork" last-modified="2013-02-01T17:37:21.000Z" size="1027"/>
 *   <file name="Data_URL_0043629.html" last-modified="2013-02-06T01:10:23.000Z" size="4829"/>
 * </index>
 * </pre>
 *
 * <?xml version="1.0" encoding="UTF-8"?>
 *  <?xml-stylesheet type='text/xsl' href='/ocean-archive.data.nodc.noaa.gov//index.xsl'?>
 *   <index xmlns="http://nodc.noaa.gov/s3/catalog/1.0" base="http://ocean-archive.data.nodc.noaa.gov" path="/0043269/1.1/data/0-data" name="ocean-archive.data.nodc.noaa.gov" delimiter="/" encoding="UTF-8">
 *   <folder name="ogdr" size="253881251111" count="62472"/>
 *   <file name="" last-modified="2013-02-01T17:37:21.000Z" size="0"/>
 *   <file name="Data_URL_0043629.html" last-modified="2013-02-06T01:10:23.000Z" size="4829"/>
 * </index>

 *
 */
public class Index {

    private Logger log = null;


    private String _resourceId;
    private File _cacheFile = null;
    private String _base;
    private String _indexFileConvention = "/index.xml";
    private long _lastModified;


    private Element _indexElement;
    public static final String INDEX_NAMESPACE_STRING = "http://nodc.noaa.gov/s3/catalog/1.0";
    public static final Namespace INDEX_NAMESPACE =  Namespace.getNamespace("s3c", INDEX_NAMESPACE_STRING);



    public Index(File cacheFile) throws IOException, JDOMException {
        super();
        log = LoggerFactory.getLogger(this.getClass());

        if(cacheFile==null)
            throw new IOException("Oops... Index file was null valued.");

        _cacheFile = new File(cacheFile.getAbsolutePath());


        loadIndex();

    }


    public String getIndexFileConvention(){
        return _indexFileConvention;
    }


    public String getResourceId(){
        return _resourceId;
    }

    public void setResourceId(String resourceId){
        _resourceId = resourceId;
    }



    public Element getIndexElement() throws JDOMException, IOException {
        //@todo Check last modified time and refresh as needed.
        if(_indexElement == null) {
            loadIndex();
        }
        return _indexElement;
    }




    private void loadIndex() throws IOException, JDOMException {

        FileInputStream fis = null;

        try {
            fis =  new FileInputStream(getIndexFile());

            Document indexDoc = opendap.xml.Util.getDocument(fis);
            _indexElement = indexDoc.getRootElement();
            _indexElement.detach();

            File indexFile = getIndexFile();
            _lastModified = indexFile.lastModified();

            log.debug("loadIndex(): Retrieved Index document.");
        }
        finally {
            if(fis!=null) {
                try {
                    //fis.close();
                }
                catch(Exception e){
                    log.debug("loadIndex() - Failed to close FileInputStream. msg: {}",e.getMessage());
                }
            }
        }




    }

    String getBase() throws JDOMException, IOException {
        if(_base!=null)
            return _base;

        Element index = getIndexElement();
        _base =  index.getAttributeValue("base");
        return _base;
    }

    public void setBase(String base) throws JDOMException, IOException {
        _base = base;
        Element index = getIndexElement();
        index.setAttribute("base",base);
    }

    String getPath() throws JDOMException, IOException {
        Element index = getIndexElement();
        return index.getAttributeValue("path");
    }

    String getDelimiter() throws JDOMException, IOException {
        Element index = getIndexElement();
        return index.getAttributeValue("delimiter");
    }


    private HashMap<String, Element> getThreddsCatalogServices(String dapServiceContext){
        HashMap<String,Element> services = new HashMap<String, Element>();


        // Define the DAP service
        Element service = new Element(THREDDS.SERVICE, THREDDS.NS);
        service.setAttribute("name","dap");
        service.setAttribute("serviceType","OPeNDAP");
        service.setAttribute("base", dapServiceContext);

        services.put("dap",service);


        return services;
    }





    public Element getThreddsCatalog(String vaultName, String catalogServiceContext, String dapServiceContext) throws JDOMException, IOException {

        Element threddsCatalog = new Element(THREDDS.CATALOG,THREDDS.NS);

        HashMap<String,Element> services = getThreddsCatalogServices(dapServiceContext);

        threddsCatalog.addNamespaceDeclaration(XLINK.NS);

        for(Element service:services.values()){
            threddsCatalog.addContent(service);
        }

        Element catalogDataset = new Element(THREDDS.DATASET,THREDDS.NS);

        String name = vaultName + (getPath().equals("")?getDelimiter():getPath());
        catalogDataset.setAttribute("name",name);

        StringBuilder id = new StringBuilder();
        id.append(catalogServiceContext).append(name);
        catalogDataset.setAttribute("ID",id.toString());


        catalogDataset.addContent(getThreddsCatalogRefs(catalogServiceContext));
        catalogDataset.addContent(getThreddsDatasets(services,dapServiceContext));

        threddsCatalog.addContent(catalogDataset);

        return threddsCatalog;

    }




    /**
     *   <file name="FGDC_meta_0077816_long_version.xml" last-modified="2013-02-01T17:43:44.000Z" size="152333"/>
     * @return
     */
    public Vector<Element> getThreddsDatasets(HashMap<String,Element> services, String dapServiceContext) throws JDOMException, IOException {

        Vector<Element> threddsDatasets = new Vector<Element>();

        String indexPath = getPath();
        String delim = getDelimiter();


        List filesList = _indexElement.getChildren("file", INDEX_NAMESPACE);

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

                String urlPath = getCatalogId(fileName);


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


        List foldersList = indexElement.getChildren("folder", INDEX_NAMESPACE);

        for(Object o: foldersList){
            Element folder = (Element) o;
            String folderName = folder.getAttributeValue("name");


            Element catalogRef = new Element(THREDDS.CATALOG_REF,THREDDS.NS);

            catalogRef.setAttribute("name",folderName);
            catalogRef.setAttribute("title",folderName,XLINK.NS);
            catalogRef.setAttribute("type","simple",XLINK.NS);


            StringBuilder id = new StringBuilder();

            id.append(getPath()).append(getDelimiter()).append(folderName).append(getDelimiter());

            catalogRef.setAttribute("ID",id.toString());

            StringBuilder href = new StringBuilder();

            href.append(catalogServiceContext).append(id).append("catalog.xml");

            catalogRef.setAttribute("href",href.toString(),XLINK.NS);

            catalogRefs.add(catalogRef);
        }


        return catalogRefs;

    }


    // @todo Move to class(es?) that hold collections of Index and know the catalogContext name.
    public String getCatalogId(String fileName) throws JDOMException, IOException {

        StringBuilder resourceId =  new StringBuilder();
        resourceId.append(getPath()).append(getDelimiter()).append(fileName);

        return resourceId.toString();
    }




    private File getIndexFile() throws IOException {

        if(_cacheFile==null)
            return null;


        return new File(_cacheFile.getAbsolutePath());

    }



    protected void setCacheFile(File f){
        _cacheFile = f;
    }



    public void writeIndexToFile(File targetFile) throws JDOMException, IOException {


        log.debug("writeIndexToFile() - targetFile: '{}'", targetFile);

        File parent = targetFile.getParentFile();

        if(!parent.exists() && !parent.mkdirs()){
            throw new IOException("Couldn't create The parent directory: " + parent);
        }

        if(!targetFile.exists()) {
            log.debug("writeIndexToFile() - Attempting to create target file: '{}'",targetFile.getAbsolutePath());
            targetFile.createNewFile();
        }

        FileOutputStream target_os = null;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        Document indexDoc = new Document((Element)_indexElement.clone());
        try {
            target_os = new FileOutputStream(targetFile);

            xmlo.output(indexDoc,target_os);

        }
        finally {

            if(target_os!=null)
                target_os.close();
        }

    }


    public long getLastModified() {

        return _lastModified;
    }



}
