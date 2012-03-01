package opendap.threddsHandler;

import net.sf.saxon.s9api.SaxonApiException;
import opendap.namespaces.THREDDS;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * This class provides is a singleton designed to manage the various inherited metadata objects propagated into the
 * THREDDS catalog system by THREDDS datasetScan elements.
 * User: ndp
 * Date: Jun 28, 2010
 * Time: 12:53:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class InheritedMetadataManager {

    /**
     * This transform takes a THREDDS catalog as input and creates an XML file that contains elements representing
     * each thredds:datasetScan element that contains metadata elements whose 'inherited' attribute is true. A
     * metadataRootPath which represents where in the BES directory hierarchy the metadata is to be injected, is
     * computed for each applicable service and added  to the response.
     */
    private static String transform =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                    "<!DOCTYPE xsl:stylesheet []>\n" +
                    "<xsl:stylesheet version=\"2.0\"\n" +
                    "                xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
                    "                xmlns:fn=\"http://www.w3.org/2005/02/xpath-functions\"\n" +
                    "                xmlns:wcs=\"http://www.opengis.net/wcs\"\n" +
                    "                xmlns:gml=\"http://www.opengis.net/gml\"\n" +
                    "                xmlns:thredds=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\"\n" +
                    "                xmlns:ncml=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\"\n" +
                    "                xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" +
                    "\n" +
                    "                >\n" +
                    "    <xsl:output method='html'  encoding='UTF-8' indent='yes'/>\n" +
                    "\n" +
                    "    <xsl:key name=\"service-by-name\" match=\"//thredds:service\" use=\"@name\"/>\n" +
                    "\n" +
                    "    <xsl:template match=\"@* | node()\">\n" +
                    "            <xsl:apply-templates />\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:service\">\n" +
                    "        <xsl:copy-of select=\".\"/>\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:catalog\">\n" +
                    "        <catalogIngest>\n" +
                    "            <xsl:apply-templates />\n" +
                    "        </catalogIngest>\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "    <xsl:template match=\"thredds:datasetScan[thredds:metadata/@inherited='true']\">\n" +
                    "\n" +
                    "        <xsl:variable name=\"serviceName\" select=\"thredds:metadata/thredds:serviceName\"/>\n" +
                    "\n" +
                    "        <xsl:variable name=\"datasetScanLocation\">\n" +
                    "            <xsl:choose>\n" +
                    "                <xsl:when test=\"substring(@location,string-length(@location))='/'\">\n" +
                    "                    <xsl:value-of select=\"@location\"/>\n" +
                    "                </xsl:when>\n" +
                    "                <xsl:otherwise>\n" +
                    "                    <xsl:value-of select=\"concat(@location,'/')\"/>\n" +
                    "                </xsl:otherwise>\n" +
                    "\n" +
                    "            </xsl:choose>\n" +
                    "        </xsl:variable>\n" +
                    "\n" +
                    "        <xsl:variable name=\"datasetScanName\" select=\"@name\"/>\n" +
                    "\n" +
                    "        <xsl:variable name=\"serviceElement\" select=\"key('service-by-name', $serviceName)\"/>\n" +
                    "\n" +
                    "        <xsl:variable name=\"dapServices\"\n" +
                    "                      select=\"$serviceElement[@serviceType='OPENDAP'] | $serviceElement/thredds:service[@serviceType='OPENDAP'] \"/>\n" +
                    "\n" +
                    "        <datasetScanIngest name=\"{$datasetScanName}\">\n" +
                    "\n" +
                    "            <xsl:for-each select=\"$dapServices\">\n" +
                    "\n" +
                    "                <xsl:variable name=\"base\" select=\"@base\"/>\n" +
                    "\n" +
                    "                <xsl:variable name=\"lastCharOfBase\" select=\"substring($base,string-length($base))\"/>\n" +
                    "\n" +
                    "                <xsl:variable name=\"metadataRootPath\">\n" +
                    "                    <xsl:choose>\n" +
                    "\n" +
                    "                        <xsl:when test=\"$lastCharOfBase='/' and starts-with($datasetScanLocation,'/')\">\n" +
                    "                            <xsl:variable name=\"location\"\n" +
                    "                                          select=\"substring($datasetScanLocation,2,string-length($datasetScanLocation))\"/>\n" +
                    "                            <xsl:variable name=\"targetURL\" select=\"concat($base,$location)\"/>\n" +
                    "                            <xsl:value-of select=\"$targetURL\"/>\n" +
                    "                        </xsl:when>\n" +
                    "\n" +
                    "                        <xsl:when test=\"$lastCharOfBase!='/' and not(starts-with($datasetScanLocation,'/'))\">\n" +
                    "                            <xsl:variable name=\"targetURL\" select=\"concat($base,'/',$datasetScanLocation)\"/>\n" +
                    "                            <xsl:value-of select=\"$targetURL\"/>\n" +
                    "                        </xsl:when>\n" +
                    "\n" +
                    "                        <xsl:otherwise>\n" +
                    "                            <xsl:variable name=\"targetURL\" select=\"concat($base,$datasetScanLocation)\"/>\n" +
                    "                            <xsl:value-of select=\"$targetURL\"/>\n" +
                    "                        </xsl:otherwise>\n" +
                    "\n" +
                    "                    </xsl:choose>\n" +
                    "\n" +
                    "                </xsl:variable>\n" +
                    "\n" +
                    "                <metadataRootPath>\n" +
                    "                    <xsl:value-of select=\"$metadataRootPath\"/>\n" +
                    "                </metadataRootPath>\n" +
                    "\n" +
                    "            </xsl:for-each>\n" +
                    "\n" +
                    //"            <xsl:copy-of select=\"$serviceElement\"/>\n" +
                    "            <xsl:copy-of select=\"thredds:metadata[@inherited='true']\"/>\n" +
                    "\n" +
                    "        </datasetScanIngest>\n" +
                    "\n" +
                    "    </xsl:template>\n" +
                    "\n" +
                    "\n" +
                    "</xsl:stylesheet>\n" +
                    "";


    private static Logger log = org.slf4j.LoggerFactory.getLogger(InheritedMetadataManager.class);
    private static Transformer _dsIngestTransformer;

    static {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(transform.getBytes());
            _dsIngestTransformer = new Transformer(new StreamSource(is));
        } catch (SaxonApiException e) {
            log.error("FAILED to build transform! Msg: " + e.getMessage());
            _dsIngestTransformer = null;
        }
    }

    /**
     * Lock for thread safe operation.
     */
    private static ReentrantReadWriteLock _inventoryLock = new ReentrantReadWriteLock();


    /**
     * This ConcurrentHashMap<String,String[]> represents: ConcurrentHashMap<String catalogKey ,String[] metadataRootPathsFromThisCatalog>
     */
    private static ConcurrentHashMap<String, String[]> _catalog2MetadataMap = new ConcurrentHashMap<String, String[]>();

    /**
     * This  ConcurrentHashMap<String,HashMap<String,Vector<Element>>>  represents
     * ConcurrentHashMap<String metadataRootPath,HashMap<String catalogKey,Vector<Element metadataElement>>>
     */
    private static ConcurrentHashMap<String, HashMap<String, Vector<Element>>> _inheritedMetadata = new ConcurrentHashMap<String, HashMap<String, Vector<Element>>>();

    /**
     * This ConcurrentHashMap<String,HashMap<String,Element>> represents:
     * ConcurrentHashMap<String metadataRootPath,HashMap<String catalogKey,Element inheritedService>>
     */
    private static ConcurrentHashMap<String, HashMap<String, Vector<Element>>> _inheritedServices = new ConcurrentHashMap<String, HashMap<String, Vector<Element>>>();


    /**
     * Inspect a THREDDS Catalog object and add any datasetScan metadata whose 'inherited' attribute is set to true
     * into the inventory.
     * @param catalog   The Catalog object to inspect.
     * @throws SaxonApiException When Saxon parsing fails.
     * @throws IOException  When reading a catalog fails.
     * @throws JDOMException   When JDOM parsing fails.
     */
    public static void ingestInheritedMetadata(Catalog catalog) throws SaxonApiException, IOException, JDOMException {

        String catalogKey = catalog.getCatalogKey();


        ReentrantReadWriteLock.WriteLock writeLock = _inventoryLock.writeLock();
        try {
            writeLock.lock();

            JDOMSource cat = new JDOMSource(catalog.getRawCatalogDocument());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();


            SAXBuilder sb = new SAXBuilder();
            Document dsIngest;
            Element metadataRootPathElement, metadataElement;
            Iterator datasetScanIngests, metadataRootPathIterator;
            String metadataRootPath;
            Vector<String> metadataRootPaths = new Vector<String>();
            Vector<Element> metadataElements;
            HashMap<String, Vector<Element>> metadataForThisRootPath;
            HashMap<String, Vector<Element>> inheritedServicesForThisRootPath;

            _dsIngestTransformer.transform(cat, baos);


            dsIngest = sb.build(new ByteArrayInputStream(baos.toByteArray()));


            // Round up all of the services
            Iterator i = dsIngest.getDescendants(new ElementFilter("service",THREDDS.NS));
            HashMap<String,Element> serviceByName = new HashMap<String,Element>();
            Element service;
            while(i.hasNext()){
                service = (Element)i.next();
                serviceByName.put(service.getAttributeValue("name"),service);
            }


            //Round up all of the dataset scan elements that got morphed into datasetScanIngest Elements
            datasetScanIngests = dsIngest.getDescendants(new ElementFilter("datasetScanIngest"));
            Vector<Element> ingests = new Vector<Element>();
            while (datasetScanIngests.hasNext()) {
                ingests.add((Element) datasetScanIngests.next());
            }


            for (Element dsi : ingests) {
                log.debug("Processing datasetScan '" + dsi.getAttributeValue("name") + "'");

                metadataElement = dsi.getChild("metadata", THREDDS.NS);
                if (metadataElement != null) {
                    metadataElement.detach();
                    log.debug("Found inherited metadata.");




                    //################################################################################
                    //@todo Right now it removes the serviceName elements. Why is that a good thing?
                    //@todo If we leave them then we have to see if the serviceName resolves in the
                    //@todo code that produces uses the metadata in a new catalog. Right now that's
                    //@todo in BESThreddsDispatchHandler.handleRequest()
                    /*
                    i = metadataElement.getChildren("serviceName", THREDDS.NS).iterator();
                    Vector<Element> serviceNameElements = new Vector<Element>();
                    while (i.hasNext()) {
                        serviceNameElements.add((Element) i.next());
                    }
                    for (Element serviceName : serviceNameElements) {
                        serviceName.detach();
                        log.debug("Removed Element <thredds:serviceName>" + serviceName.getTextTrim() + "</thredds:serviceName> from inherited metadata.");
                    }
                    */
                    //@todo We should probably carry the serviceName and service definitions through to the output catalog.
                    //################################################################################


                    // The THREDDS specification states that multiple serviceName Elements will be ignored.
                    // So we're only going to get the first one.
                    // see http://www.unidata.ucar.edu/projects/THREDDS/tech/catalog/v1.0.2/InvCatalogSpec.html#threddsMetadataGroup
                    Element inheritedService = null;
                    Element serviceName = metadataElement.getChild("serviceName", THREDDS.NS);
                    if(serviceName != null){
                        service = serviceByName.get(serviceName.getTextTrim());
                        inheritedService = getServiceDefintion(service);
                    }

                    // This may be a poorly named variable, the value of metadataRootPath is the place
                    // in the BES directory hierarchy the metadata is to be injected
                    metadataRootPathIterator = dsi.getDescendants(new ElementFilter("metadataRootPath"));
                    while (metadataRootPathIterator.hasNext()) {
                        metadataRootPathElement = (Element) metadataRootPathIterator.next();
                        metadataRootPath = metadataRootPathElement.getTextTrim();
                        if (!_inheritedMetadata.contains(metadataRootPath)) {
                            log.debug("Found new metadataRootPath: '"+metadataRootPath+"' Creating storage HashMap");
                            _inheritedMetadata.put(metadataRootPath, new HashMap<String, Vector<Element>>());
                        }
                        metadataForThisRootPath = _inheritedMetadata.get(metadataRootPath);
                        if (!metadataForThisRootPath.containsKey(catalogKey)) {
                            log.debug("The catalog '"+catalogKey+"' is a new contributor of metadata to metadataRootPath '"+metadataRootPath+"' Creating storage Vector");
                            metadataForThisRootPath.put(catalogKey, new Vector<Element>());
                        }

                        metadataElements = metadataForThisRootPath.get(catalogKey);

                        log.debug("Adding metadata element to inventory.");
                        metadataElements.add(metadataElement);


                        if(inheritedService!=null){
                            log.debug("Processing inheritedService for metadataRootPath: '"+metadataRootPath+"' catalogKey: '",catalogKey+"'");

                            if (!_inheritedServices.contains(metadataRootPath)) {
                                log.debug("Creating inherited services storage HashMap for " +
                                        "metadataRootPath: '"+metadataRootPath+"'");
                                _inheritedServices.put(metadataRootPath, new HashMap<String, Vector<Element>>());
                            }

                            inheritedServicesForThisRootPath = _inheritedServices.get(metadataRootPath);

                            if(!inheritedServicesForThisRootPath.containsKey(catalogKey)){
                                log.debug("Creating inherited services storage Vector for " +
                                        "metadataRootPath: '"+metadataRootPath+"' originating from catalogKey: '",catalogKey+"'");
                                inheritedServicesForThisRootPath.put(catalogKey, new Vector<Element>());
                            }



                            log.debug("Adding service '"+inheritedService.getAttributeValue("name")+
                                    "' to inherited services inventory for metadataRootPath: "+metadataRootPath+"' " +
                                    "originating from catalogKey: '",catalogKey+"'");
                            Vector<Element> fromThisCatalogKey =  inheritedServicesForThisRootPath.get(catalogKey);
                            fromThisCatalogKey.add(inheritedService);
                        }
                        log.debug("Adding metadataRootPath '"+metadataRootPath+"'to list of metadataRootPaths " +
                                "spawned by catalog '"+catalogKey+"'");
                        metadataRootPaths.add(metadataRootPath);
                    }
                }

            }
            log.debug("Adding mapping of catalog '"+catalogKey+"' to a collection of metadataRootPaths to inventory.");
            _catalog2MetadataMap.put(catalogKey, metadataRootPaths.toArray(new String[metadataRootPaths.size()]));

        }
        finally {
            writeLock.unlock();
        }
    }


    /**
     * Purge all of the inherited metadata in the inventory that was extracted from the catalog associated
     * with catalogKey.
     * @param catalogKey   The catalogKey of the catalog to purge.
     */
    public static void purgeInheritedMetadata(String catalogKey) {


        HashMap<String, Vector<Element>> metadataForThisRootPath;
        HashMap<String, Vector<Element>> servicesForThisRootPath;

        ReentrantReadWriteLock.WriteLock writeLock = _inventoryLock.writeLock();

        try {
            writeLock.lock();

            // Get all of the metadataRootPaths that were tied to this catalogKey
            String[] metadataRootPathsInThisCatalog = _catalog2MetadataMap.get(catalogKey);

            if(metadataRootPathsInThisCatalog!=null){
                // Look at each of those metadataRootPaths
                for (String metadataRootPath : metadataRootPathsInThisCatalog) {

                    // Get the metadata collection for this metadataRootPath
                    metadataForThisRootPath = _inheritedMetadata.get(metadataRootPath);

                    if(metadataForThisRootPath!=null){

                        // Remove all of the metadata elements from this metadataRootPath that came from this catalogKey
                        log.debug("purgeInheritedMetadata(): Removing all metadata in metadataRootPath '"+metadataRootPath +
                                " that was ingested from the catalog '"+catalogKey+"' (catalogKey)");
                        metadataForThisRootPath.remove(catalogKey);

                        // If the metadataRootPath is now empty (no more metadata associated with it) remove it from the collection.
                        if (metadataForThisRootPath.isEmpty()){
                            log.debug("purgeInheritedMetadata(): Removing metadata container for metadataRootPath '"+metadataRootPath +
                                    " from inventory (it's now empty)");

                            _inheritedMetadata.remove(metadataRootPath);
                        }
                    }

                    // Purge the inherited service definitions for this metadataRootPath.
                    servicesForThisRootPath = _inheritedServices.get(metadataRootPath);

                    if(servicesForThisRootPath!=null){


                        // Remove  the service element from this metadataRootPath that came from this catalogKey
                        log.debug("purgeInheritedMetadata(): Removing service defintions in metadataRootPath '"+metadataRootPath +
                                " that were ingested from the catalog '"+catalogKey+"' (catalogKey)");

                        servicesForThisRootPath.remove(catalogKey);

                        // If the metadataRootPath is now empty (no more services associated with it) remove it from the collection.
                        if (servicesForThisRootPath.isEmpty()){
                            log.debug("purgeInheritedMetadata(): Removing inherited services container for metadataRootPath '"+metadataRootPath +
                                    " from inventory (it's now empty)");
                            _inheritedServices.remove(metadataRootPath);
                        }


                    }


                }

                // Remove the inherited metadata associations for this catalogKey
                log.debug("purgeInheritedMetadata(): Removing catalog '"+catalogKey+"' (catalogKey) from " +
                        "inheritedMetadataInventory");

                _catalog2MetadataMap.remove(catalogKey);
            }
            else {
                // Remove the inherited metadata associations for this catalogKey
                log.debug("purgeInheritedMetadata(): No Metadata root paths found in catalog '"+catalogKey+"' (catalogKey) in " +
                        "inheritedMetadataInventory");

            }


        }
        finally {
            writeLock.unlock();
        }


    }


    public static boolean hasInheritedMetadata(String catalogKey) {

        ReentrantReadWriteLock.ReadLock readLock = _inventoryLock.readLock();

        try {
            readLock.lock();

            Enumeration<String> metadataRootPaths = _inheritedMetadata.keys();
            String metadataRootPath;

            while (metadataRootPaths.hasMoreElements()) {
                metadataRootPath = metadataRootPaths.nextElement();
                if (catalogKey.startsWith(metadataRootPath)) {
                    log.debug("Found inherited metadata for catalog '"+catalogKey+"'");
                    return true;
                }
            }

            return false;
        }
        finally {
            readLock.unlock();
        }
    }

    public static boolean hasInheritedServices(String catalogKey) {

        ReentrantReadWriteLock.ReadLock readLock = _inventoryLock.readLock();

        try {
            readLock.lock();

            Enumeration<String> metadataRootPaths = _inheritedServices.keys();
            String metadataRootPath;

            while (metadataRootPaths.hasMoreElements()) {
                metadataRootPath = metadataRootPaths.nextElement();
                if (catalogKey.startsWith(metadataRootPath)) {
                    log.debug("Found inherited metadata for catalog '"+catalogKey+"'");
                    return true;
                }
            }

            return false;
        }
        finally {
            readLock.unlock();
        }
    }

    public static Vector<Element> getInheritedMetadata(String catalogKey) {


        ReentrantReadWriteLock.ReadLock readLock = _inventoryLock.readLock();
        try {
            readLock.lock();

            String metadataRootPath;
            HashMap<String, Vector<Element>> metadataForThisRootPath;

            // Put the result in here...
            Vector<Element> metadataElements = new Vector<Element>();

            // Look for all applicable metadata in the inheritance tree.
            Enumeration<String> metadataRootPaths = _inheritedMetadata.keys();
            while (metadataRootPaths.hasMoreElements()) {

                metadataRootPath = metadataRootPaths.nextElement();
                if (catalogKey.startsWith(metadataRootPath)) {
                    log.debug("Found applicable inherited metadata collection for catalog '"+catalogKey+"'");

                    // Found applicable metadata collection
                    metadataForThisRootPath = _inheritedMetadata.get(metadataRootPath);

                    for (String sourceCatalogKey : metadataForThisRootPath.keySet()) {
                        for (Element metadata : metadataForThisRootPath.get(sourceCatalogKey)) {
                            log.debug("Adding metadata element to returned collection.");

                            metadataElements.add((Element) metadata.clone());

                        }
                    }


                }
            }

            return metadataElements;
        }
        finally {
            readLock.unlock();
        }


    }


    public static Element getInheritedServices(String catalogKey) {


        ReentrantReadWriteLock.ReadLock readLock = _inventoryLock.readLock();
        try {
            readLock.lock();

            // Put the results in here...
            Element inheritedServices = new Element("inheritedServices");

            String metadataRootPath;
            HashMap<String, Vector<Element>> servicesForThisRootPath;

            HashMap<String, Element> serviceElements = new HashMap<String, Element>();

            // Look for all applicable metadata in the inheritance tree.
            Enumeration<String> metadataRootPaths = _inheritedServices.keys();
            while (metadataRootPaths.hasMoreElements()) {

                metadataRootPath = metadataRootPaths.nextElement();
                if (catalogKey.startsWith(metadataRootPath)) {
                    log.debug("Found applicable inherited service definition for catalog '"+catalogKey+"'");

                    // Found applicable metadata collection
                    servicesForThisRootPath = _inheritedServices.get(metadataRootPath);

                    for (String sourceCatalogKey : servicesForThisRootPath.keySet()) {

                        Vector<Element> services =  servicesForThisRootPath.get(sourceCatalogKey);
                        for(Element service:  services){
                            String serviceName = service.getAttributeValue("name");
                            if(!serviceElements.containsKey(serviceName)){
                                log.debug("Adding service element to returned collection.");
                                serviceElements.put(serviceName, (Element) service.clone());
                            }
                            else {
                                log.error("The service '"+serviceName+"' is multiply defined! Ignoring duplicate.");
                            }
                        }
                    }


                }
            }

            inheritedServices.addContent(serviceElements.values());
            return inheritedServices;
        }
        finally {
            readLock.unlock();
        }


    }


    private static Element getServiceDefintion(Element service){
        Element serviceParent = service.getParentElement();
        if(!serviceParent.getName().equals("service") ||  serviceParent.getNamespacePrefix().equals("thredds")){

            return service;

        }

        return getServiceDefintion(serviceParent);



    }


}
