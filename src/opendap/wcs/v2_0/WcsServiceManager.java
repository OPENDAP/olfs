/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0;

import opendap.xml.Util;
import org.apache.http.client.CredentialsProvider;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class manages the WCS service.
 *
 * STARTUP: Reads the ServiceIdentification.xml, ServiceProvider.xml,
 * OperationsMetadata.xml in the config directory to determine the service metadata. When the Operations
 * Metadata is transmited the service endpoint is adjusted to match the runtime service. Additionally
 * all of the WcsCatalog instances defined in the configuration and instantiated and intialized.
 *
 * RUNTIME: Provides WCS request processing a one stop should for the WcsCatalog instance needed to
 * fufill the requiest.
 *
 */
public class WcsServiceManager {

    private static final Logger _log = org.slf4j.LoggerFactory.getLogger(WcsServiceManager.class);

    private static Date _serviceStartTime;

    private static String SERVICE_ID_FILENAME = "ServiceIdentification.xml";
    private static File serviceIdFile;
    private static AtomicReference<Element> serviceIdentification = new AtomicReference<Element>();

    private static String SERVICE_PROVIDER_FILENAME = "ServiceProvider.xml";
    private static File serviceProviderFile;
    private static AtomicReference<Element> serviceProvider       = new AtomicReference<Element>();

    private static String OPERATIONS_METADATA_FILENAME = "OperationsMetadata.xml";
    private static File opsMetadataFile;
    private static AtomicReference<Element> operationsMetadata    = new AtomicReference<Element>();

    private static String WCS_CATALOG_ELEMENT_NAME = "WcsCatalog";

    /**
     * This is the string in the template OperationsMetadata file and we replace it with the one
     * we get from Tomcat
     */
    private static final String SERVICE_END_POINT= "@SERVICE_ENDPOINT_URL@";


    private static WcsCatalog _defaultCatalog;
    private static CopyOnWriteArrayList<WcsCatalog> _wcsCatalogs;
    private static boolean _intitialized;
    private static String _serviceConfigFileName;
    private static String _serviceContentPath;
    private static String _serviceContextPath;

    private static CredentialsProvider _credentialsProvider;

    static {
        _defaultCatalog = null;
        _wcsCatalogs =  new CopyOnWriteArrayList<>();
        _intitialized = false;
        _serviceConfigFileName = null;
        _serviceContentPath=null;
        _serviceStartTime = null;
        _credentialsProvider = null;
    }




    //        URL serviceConfigFile = getServiceConfigurationUrl(serviceConfigPath,configFileName);

    public static void init(String serviceContextPath, String serviceConfigPath, String configFileName) throws ServletException {
        if(_intitialized)
            return;

        String msg;
        SAXBuilder sb = new SAXBuilder();
        Document configDoc = null;

        _serviceContentPath = serviceConfigPath;
        _serviceContextPath = serviceContextPath;
        _serviceConfigFileName = configFileName;

        File serviceConfigFile =  new File(_serviceContentPath,_serviceConfigFileName);
        try {
            configDoc = sb.build(serviceConfigFile);
            if(configDoc==null) {
                msg = "The WCS 2.0 servlet is unable to locate the configuration document '"+serviceConfigFile+"'";
                _log.error(msg);
                throw new ServletException(msg);
            }

            ingestCapabilitiesMetadata(_serviceContentPath);

        } catch (JDOMException e) {
            throw new ServletException(e);
        } catch (IOException e) {
            throw new ServletException(e);
        }

        Element configFileRoot = configDoc.getRootElement();
        if(configFileRoot==null) {
            msg = "The WCS 2.0 servlet is unable to locate the root element of the configuration document '"+serviceConfigFile+"'";
            _log.error(msg);
            throw new ServletException(msg);
        }

        Element e1 = configFileRoot.getChild("Credentials");
        try {
            if (e1 == null) {
                _credentialsProvider = opendap.http.Util.getNetRCCredentialsProvider();
                _log.info("Using Default Credentials file: ~/.netrc");
            } else {
                String credsFilename = e1.getTextTrim();
                _credentialsProvider = opendap.http.Util.getNetRCCredentialsProvider(credsFilename, true);
                _log.info("Using Credentials file: {}", credsFilename);
            }
        } catch (IOException e) {
            _log.warn("I was unable to generate a CredentialsProvider for authenticated activities :(");
            _credentialsProvider = null;
        }
        ingestWcsCatalogDefs(configFileRoot);

        _serviceStartTime = new Date();
        _intitialized = true;
    }


    public static void ingestCapabilitiesMetadata(String metadataDir) throws IOException,JDOMException  {
        Element e2;

        serviceIdFile = new File(metadataDir,SERVICE_ID_FILENAME);
        e2 = opendap.xml.Util.getDocumentRoot(serviceIdFile);
        serviceIdentification.set(e2);
        _log.debug("Loaded wcs:ServiceIdentfication from: "+serviceIdFile);

        serviceProviderFile = new File(metadataDir,SERVICE_PROVIDER_FILENAME);
        e2 = opendap.xml.Util.getDocumentRoot(serviceProviderFile);
        serviceProvider.set(e2);
        _log.debug("Loaded wcs:ServiceProvider from: "+serviceProviderFile);

        opsMetadataFile = new File(metadataDir, OPERATIONS_METADATA_FILENAME);
        e2 = opendap.xml.Util.getDocumentRoot(opsMetadataFile);
        operationsMetadata.set(e2);
        _log.debug("Loaded wcs:OperationsMetadata from: "+opsMetadataFile);
    }

    private void updateCapabilitiesMetadata() throws Exception{

        Element e2;

        if(_serviceStartTime.getTime() < serviceIdFile.lastModified()){
            e2 = opendap.xml.Util.getDocumentRoot(serviceIdFile);
            serviceIdentification.set(e2);
        }

        if(_serviceStartTime.getTime() < serviceProviderFile.lastModified()){
            e2 = opendap.xml.Util.getDocumentRoot(serviceProviderFile);
            serviceProvider.set(e2);
        }

        if(_serviceStartTime.getTime() < opsMetadataFile.lastModified()){
            e2 = Util.getDocumentRoot(opsMetadataFile);
            operationsMetadata.set(e2);
        }
        _serviceStartTime = new Date();

    }


    private static void ingestWcsCatalogDefs(Element config) throws ServletException {

        _log.info("ingestWcsCatalogs() - BEGIN.");
        String msg;
        List<Element> wcsCatalogElements = (List<Element>) config.getChildren(WCS_CATALOG_ELEMENT_NAME);

        if(wcsCatalogElements.isEmpty()) {
            msg = "The WCS service could not find a catalog implmentation! No child "+
                    WCS_CATALOG_ELEMENT_NAME+ " elements were found. The catalog is empty. :(";
            _log.error(msg);
            _defaultCatalog = new DummyCatalog();
            _wcsCatalogs.add(_defaultCatalog);
            return;
        }

        for(Element wcsCatalogElement: wcsCatalogElements){
            String className =  wcsCatalogElement.getAttributeValue("className");
            if(className==null) {
                msg = "The WCS 2.0 servlet is unable to locate the 'className' attribute of the <WcsCatalog> element"+
                        "in the configuration file: " + _serviceConfigFileName + "'";
                _log.error(msg);
                throw new ServletException(msg);
            }

            WcsCatalog wcsCatalog = null;
            try {
                _log.debug("Building WcsCatalog implementation: " + className);
                Class classDefinition = Class.forName(className);
                wcsCatalog = (WcsCatalog) classDefinition.newInstance();
            }
            catch ( Exception e){
                msg = "Failed to build WcsCatalog implementation: "+className+
                        " Caught an exception of type "+e.getClass().getName() + " Message: "+ e.getMessage();
                _log.error(msg);
                throw new ServletException(msg, e);
            }

            try {
                wcsCatalog.init(wcsCatalogElement, _serviceContentPath, _serviceContextPath);
            } catch (Exception e) {
                _log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
                throw new ServletException(e);
            }

            if(wcsCatalogElements.indexOf(wcsCatalogElement)==0)
                _defaultCatalog = wcsCatalog;

            _wcsCatalogs.add(wcsCatalog);
        }
        _log.info("ingestWcsCatalogs() - DONE.");
    }


    /**
     * Returns the first WcsCatalog instance that  to "matches()" the coverageId.
     * If nothing matches the default catalig will be returned
     * @param coverageId
     * @return
     */
    public static WcsCatalog getCatalog(String coverageId){
        Vector<WcsCatalog> matches = new Vector<>();
        for(WcsCatalog wcsCatalog: _wcsCatalogs){
            if(wcsCatalog.matches(coverageId)) {
                matches.add(wcsCatalog);
                _log.debug("getCatalog() - WcsCatalog '{}' matched coverageId '{}'",wcsCatalog.getClass().getSimpleName(),coverageId);
            }
        }
        WcsCatalog result;
        if(matches.isEmpty())
            result = _defaultCatalog;
        else
            result = matches.get(0);

        _log.debug("getCatalog() - Returning  instanceOf '{}'",result.getClass().getSimpleName(),coverageId);

        return result;
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
            if(href.startsWith(SERVICE_END_POINT)){
                href = href.replaceFirst(SERVICE_END_POINT,serviceUrl);
            }
            get.setAttribute("href",href,WCS.XLINK_NS);
        }

        i =  omd.getDescendants(new ElementFilter("Post",WCS.OWS_NS));
        while(i.hasNext()){
            post =  (Element)i.next();
            href = post.getAttributeValue("href",WCS.XLINK_NS);
            if(href.startsWith(SERVICE_END_POINT)){
                href = href.replaceFirst(SERVICE_END_POINT,serviceUrl);
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
                        allowedValues.addContent(getSupportedFormatNames());
                    }
                }
            }
        }
        return omd;
    }



    private static Vector<Element> getSupportedFormatNames(){

        Vector<Element> sfEs = new Vector<Element>();
        Vector<String> formats = ServerCapabilities.getSupportedFormatNames(null);
        Element sf;

        for(String format: formats){
            sf = new Element("Value",WCS.OWS_NS);
            sf.setText(format);
            sfEs.add(sf);
        }
        return sfEs;

    }

    public static long getLastModified(){
        return _serviceStartTime.getTime();
    }

    public static WcsCatalog getDefaultCatalog(){
        return _defaultCatalog;
    }

    public static CredentialsProvider getCredentialsProvider(){
        return _credentialsProvider;
    }


    public static void updateCatalogs(){

    }

    public static void destroy() {

    }

}
