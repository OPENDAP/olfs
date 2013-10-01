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
package opendap.wcs.v1_1_2;

import opendap.xml.Util;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ndp
 * Date: Mar 30, 2009
 * Time: 4:05:15 PM
 */
public class CatalogWrapper {


    private Date _cacheTime;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CatalogWrapper.class);

    private static File serviceIdFile;
    private static AtomicReference<Element> serviceIdentification = new AtomicReference<Element>();
    private static File serviceProviderFile;
    private static AtomicReference<Element> serviceProvider       = new AtomicReference<Element>();
    private static File opsMetadataFile;
    private static AtomicReference<Element> operationsMetadata    = new AtomicReference<Element>();

    
    private static String _defaultServiceUrl= "http://your.domain.name:8080/opendap/WCS";


    private static WcsCatalog _catalogImpl;

    private static boolean intitialized = false;

    public static void init(Element config, WcsCatalog catalog) throws Exception {
        if(intitialized)
            return;

        ingestCapabilitiesMetadata(config);

        _catalogImpl = catalog;

        intitialized = true;
    }


    public static void init(String  metadataDir, WcsCatalog catalog) throws Exception {
        if(intitialized)
            return;

        ingestCapabilitiesMetadata(metadataDir);

        _catalogImpl = catalog;

        intitialized = true;
    }



    public static String getDataAccessUrl(String coverageID) throws InterruptedException {
        return _catalogImpl.getDataAccessUrl(coverageID);
    }


    public static boolean hasCoverage(String id) throws InterruptedException {
        return _catalogImpl.hasCoverage(id);

    }

    public static CoverageDescription getCoverageDescription(String id) throws InterruptedException, WcsException{
        return _catalogImpl.getCoverageDescription(id);

    }

    public static Element getCoverageDescriptionElement(String id)  throws InterruptedException, WcsException{
        return _catalogImpl.getCoverageDescriptionElement(id);
    }


    public static Element getCoverageSummaryElement(String id)  throws InterruptedException, WcsException{
        return _catalogImpl.getCoverageSummaryElement(id);
    }

    public static List<Element> getCoverageSummaryElements()  throws InterruptedException, WcsException{
        return _catalogImpl.getCoverageSummaryElements();
    }

    public static List<Element> getSupportedFormatElements() throws InterruptedException {
        return _catalogImpl.getSupportedFormatElements();
    }

    public static List<Element> getSupportedCrsElements() throws InterruptedException {
        return _catalogImpl.getSupportedCrsElements();
    }


    private static void ingestCapabilitiesMetadata(Element config) throws Exception  {
        Element e1, e2;
        String msg;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        e1 = config.getChild("ServiceIdentification");
        if(e1==null){
            msg = "Cannot find ServiceIdentification element in " +
                    "configuration element: \n"+ xmlo.outputString(config);
            log.error(msg);
            throw new IOException(msg);
        }
        serviceIdFile = new File(e1.getTextTrim());
        e2 = Util.getDocumentRoot(serviceIdFile);
        serviceIdentification.set(e2);
        log.debug("Loaded wcs:ServiceIdentfication from: "+serviceIdFile);


        e1 = config.getChild("ServiceProvider");
        if(e1==null){
            msg = "Cannot find ServiceProvider element in " +
                    "configuration element: \n"+ xmlo.outputString(config);
            log.error(msg);
            throw new IOException(msg);
        }
        serviceProviderFile = new File(e1.getTextTrim());
        e2 = Util.getDocumentRoot(serviceProviderFile);
        serviceProvider.set(e2);
        log.debug("Loaded wcs:ServiceProvider from: "+serviceProviderFile);

        e1 = config.getChild("OperationsMetadata");
        if(e1==null){
            msg = "Cannot find OperationsMetadata element in " +
                    "configuration element: \n"+ xmlo.outputString(config);
            log.error(msg);
            throw new IOException(msg);
        }
        opsMetadataFile = new File(e1.getTextTrim());
        e2 = Util.getDocumentRoot(opsMetadataFile);
        operationsMetadata.set(e2);
        log.debug("Loaded wcs:OperationsMetadata from: "+opsMetadataFile);

    }


    public static void ingestCapabilitiesMetadata(String metadataDir) throws Exception  {

        Element e2;

        if(!metadataDir.endsWith("/"))
            metadataDir += "/";



        serviceIdFile = new File(metadataDir + "ServiceIdentification.xml");
        e2 = Util.getDocumentRoot(serviceIdFile);
        serviceIdentification.set(e2);
        log.debug("Loaded wcs:ServiceIdentfication from: "+serviceIdFile);

        serviceProviderFile = new File(metadataDir + "ServiceProvider.xml");
        e2 = Util.getDocumentRoot(serviceProviderFile);
        serviceProvider.set(e2);
        log.debug("Loaded wcs:ServiceProvider from: "+serviceProviderFile);

        opsMetadataFile = new File(metadataDir + "OperationsMetadata.xml");
        e2 = Util.getDocumentRoot(opsMetadataFile);
        operationsMetadata.set(e2);
        log.debug("Loaded wcs:OperationsMetadata from: "+opsMetadataFile);


    }

    private void updateCapabilitiesMetadata() throws Exception{

        Element e2;

        if(_cacheTime.getTime() < serviceIdFile.lastModified()){
            e2 = Util.getDocumentRoot(serviceIdFile);
            serviceIdentification.set(e2);
        }

        if(_cacheTime.getTime() < serviceProviderFile.lastModified()){
            e2 = Util.getDocumentRoot(serviceProviderFile);
            serviceProvider.set(e2);
        }

        if(_cacheTime.getTime() < opsMetadataFile.lastModified()){
            e2 = Util.getDocumentRoot(opsMetadataFile);
            operationsMetadata.set(e2);
        }
        _cacheTime = new Date();

    }


    public static long getLastModified(){

        //return -1;
        return _catalogImpl.getLastModified();
    }









    public  static Element getServiceIdentificationElement(){
        return (Element) serviceIdentification.get().clone();
    }

    public  static Element getServiceProviderElement(){
        return (Element) serviceProvider.get().clone();

    }

    public  static Element getOperationsMetadataElement(String serviceUrl){

        Element omd = (Element)operationsMetadata.get().clone();
        Element get, post, operation, allowedValues, parameter;
        String href, name;
        boolean foundIt;
        Iterator i, j;


        // Localize the access links
        i =  omd.getDescendants(new ElementFilter("Get",WCS.OWS_NS));
        while(i.hasNext()){
            get =  (Element)i.next();
            href = get.getAttributeValue("href",WCS.XLINK_NS);
            if(href.startsWith(_defaultServiceUrl)){
                href = href.replaceFirst(_defaultServiceUrl,serviceUrl);
            }
            get.setAttribute("href",href,WCS.XLINK_NS);
        }

        i =  omd.getDescendants(new ElementFilter("Post",WCS.OWS_NS));
        while(i.hasNext()){
            post =  (Element)i.next();
            href = post.getAttributeValue("href",WCS.XLINK_NS);
            if(href.startsWith(_defaultServiceUrl)){
                href = href.replaceFirst(_defaultServiceUrl,serviceUrl);
            }
            post.setAttribute("href",href,WCS.XLINK_NS);
        }



        // Set the supported Formats

        i=omd.getChildren("Operation",WCS.OWS_NS).iterator();
        while(i.hasNext()){
            operation = (Element) i.next();
            name = operation.getAttributeValue("name");
            if(name!=null && name.equals("GetCoverage")){
                foundIt = false;
                j = operation.getChildren("Parameter",WCS.OWS_NS).iterator();
                while(j.hasNext()){
                    parameter = (Element) j.next();
                    name = parameter.getAttributeValue("name");
                    if(name!= null && name.equals("Format")){
                        foundIt = true;
                        allowedValues =  parameter.getChild("AllowedValues",WCS.OWS_NS);
                        if(allowedValues==null){
                            allowedValues =  new Element("AllowedValues",WCS.OWS_NS);
                            parameter.addContent(allowedValues);
                        }

                        allowedValues.addContent(getSupportedFormatsForGetCoverageOperation());


                    }

                }
     
            }

        }






        return omd;
    }



    private static Vector<Element> getSupportedFormatsForGetCoverageOperation(){

        Vector<Element> sfEs = new Vector<Element>();
        String[] formats = ServerCapabilities.getSupportedFormatStrings(null);
        Element sf;

        for(String format: formats){
            sf = new Element("Value",WCS.OWS_NS);
            sf.setText(format);
            sfEs.add(sf);
        }

        return sfEs;



    }



    public static void destroy() {

        _catalogImpl.destroy();

        _catalogImpl = null;

        log.info("Destroy Complete");
    }

    public static void update() throws Exception{

        _catalogImpl.update();

        log.info("Update Complete");
          
    }






}
