package opendap.threddsHandler;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesDapDispatcher;
import opendap.bes.dap2Responders.BesApi;
import opendap.namespaces.BES;
import opendap.namespaces.THREDDS;
import opendap.ppt.PPTException;
import opendap.services.FileService;
import opendap.services.ServicesRegistry;
import opendap.services.WebServiceHandler;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by ndp on 4/16/15.
 */
public class BesCatalog implements Catalog {

    private Logger _log;
    private String _name;

    /**
     * Deinfes where in the system
     */
    private String _catalogKey;
    private BesApi _besApi;
    private String _besCatalogResourceId;
    private String _ingestTransformer;
    private Vector<Element> _metadata;
    private Filter _catalogFilter;
    private boolean _ascendingOrder;
    private Namer _catalogNamer;
    private AddTimeCoverage _addTimeCoverage;
    private Vector<Proxy> _catalogProxies;
    private Vector<Element> _catalogServices;

    private boolean _useServiceRegistryServices;




    private ReentrantReadWriteLock _catalogLock;
    private byte[] _rawCatalogBuffer;


    private byte[] _clientResponseCatalogBuffer;


    BesCatalog(BesApi besApi,
               String catalogKey,
               String besCatalogResourceId,
               String besCatalogToThreddsCatalogTransformFilename,
               Vector<Element> metadata,
               Filter catalogFilter,
               boolean ascendingOrder,
               Namer namer,
               AddTimeCoverage addTimeCoverage,
               Vector<Proxy> proxies,
               Vector<Element> catalogServices,
               boolean useServiceRegistryServices
    ) throws JDOMException, BadConfigurationException, PPTException, IOException, SaxonApiException {

        _log = LoggerFactory.getLogger(this.getClass());



        _useServiceRegistryServices = useServiceRegistryServices;


        _catalogLock = new ReentrantReadWriteLock();

        _besApi = besApi;

        _catalogKey = catalogKey;

        _besCatalogResourceId = besCatalogResourceId;

        _ingestTransformer = besCatalogToThreddsCatalogTransformFilename;

        _metadata = metadata;

        _catalogFilter = catalogFilter;

        _ascendingOrder = ascendingOrder;

        _catalogNamer = namer;

        _addTimeCoverage = addTimeCoverage;

        _catalogProxies = proxies;

        _catalogServices = catalogServices;

        loadCatalog();

    }


    private Vector<Element> getThreddsCatalogServiceElements(Map<String, WebServiceHandler> catalogServices){

        Element serviceElement;
        Vector<Element> services = new Vector<>();
        for(WebServiceHandler service: catalogServices.values()){

            if(service.getThreddsServiceType()!=null){
                serviceElement = new Element(THREDDS.SERVICE,THREDDS.NS);
                serviceElement.setAttribute(THREDDS.NAME,service.getServiceId());
                serviceElement.setAttribute(THREDDS.SERVICE_TYPE,service.getThreddsServiceType());
                serviceElement.setAttribute(THREDDS.BASE,service.getBase());
                services.add(serviceElement);
            }

        }

        return services;
    }

    private TreeMap<String, WebServiceHandler> getStaticCatalogServices(){

        return getStaticCatalogServicesWorker(_catalogServices);

    }
    private TreeMap<String, WebServiceHandler> getStaticCatalogServicesWorker(List<Element> services){
        TreeMap<String, WebServiceHandler> sCatServices = new TreeMap<>();

        for(Element serviceElement : services){
            if(serviceElement.getAttributeValue(THREDDS.SERVICE_TYPE).equalsIgnoreCase(THREDDS.COMPOUND)){

                sCatServices.putAll(getStaticCatalogServicesWorker((List<Element>) serviceElement.getChildren(THREDDS.SERVICE,THREDDS.NS)));
            }
            else {
                SimpleWebServiceHandler swh = new SimpleWebServiceHandler(serviceElement);
                sCatServices.put(swh.getServiceId(), swh);
            }
        }

        return sCatServices;

    }


    private void loadCatalog() throws JDOMException, BadConfigurationException, PPTException, IOException, SaxonApiException {


        ReentrantReadWriteLock.WriteLock lock = _catalogLock.writeLock();
        try {
            lock.lock();

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            Document besShowCatalogDoc = loadBesCatalog();

            Transformer showCatalogToThreddsCatalog = new Transformer(_ingestTransformer);


            JDOMSource besCatalog = new JDOMSource(besShowCatalogDoc);

            Document catalog = showCatalogToThreddsCatalog.getTransformedDocument(besCatalog);

            Element catalogElement = catalog.getRootElement();


            /**
             * Add Services to catalog.
             */
            TreeMap<String, WebServiceHandler> catalogServices =  new TreeMap<>();


            // Use ServiceRegistry??
            if(_useServiceRegistryServices){
                catalogServices.putAll(ServicesRegistry.getWebServiceHandlers());
            }

            // Put those in the catalog...
            int position = 0;
            for(Element service : getThreddsCatalogServiceElements(catalogServices)){
                catalogElement.addContent(position++,service);
            }


            // Get any services defined on the catalog from the catalog file ingest.
            for(Element service: _catalogServices){
                Element copy = (Element) service.clone();
                _log.debug("loadCatalog() - Adding catalog service: \n",xmlo.outputString(copy));
                catalogElement.addContent(position++, (Element) service.clone());
            }


            Element topDataset =  catalogElement.getChild(THREDDS.DATASET, THREDDS.NS);


            topDataset.setAttribute(THREDDS.NAME,getCatalogKey());



            /**
             * Add Metadata!!
             */

            if(_metadata!=null){
                for(Element mdata : _metadata){
                    _log.debug("cacheRawCatalogFileContent() - Retrieved  metadata element: \n{}",xmlo.outputString(mdata));

                    topDataset.addContent(0,(Element) mdata.clone());
                }
            }



            /** ############################################################################################
             * Here we groom the client response catalog by applying all of the datasetScan directives
             * (directives is my word, not a THREDDS word)
             */

            Vector<Element> graphElements = new Vector<>();

            // Grab all the dataset elements
            graphElements.addAll((List<Element>) topDataset.getChildren(THREDDS.DATASET,THREDDS.NS));


            // Grab all the catalogRef elements
            graphElements.addAll((List<Element>) topDataset.getChildren(THREDDS.CATALOG_REF,THREDDS.NS));


            // Deatach everything!
            // graphElements.forEach(org.jdom.Element::detach);  // Java 8 too soon....
            for(Element e :graphElements){
                e.detach();
            }

            Vector<Element> dropList = new Vector<>();


            // Apply the filter by determining what to drop...
            boolean isNode;
            for(Element e : graphElements){

                String name = e.getAttributeValue(THREDDS.NAME);
                isNode = false;
                if(e.getName().equals(THREDDS.CATALOG_REF)) {
                    isNode = true;
                }

                if(!_catalogFilter.include(name,isNode)){
                    dropList.add(e);
                    _log.debug("loadCatalog(): Filter dropped {}",name);
                }

            }

            // Drop that stuff that didn't pass the filter.
            for(Element e : dropList)
                graphElements.remove(e);


            TreeMap<String, Element> notRenamed = new TreeMap<>();
            for(Element e: graphElements)
                notRenamed.put(e.getAttributeValue(THREDDS.NAME),(Element)e.clone());


            /**
             * Make thredds:access for each service on bes datasets, and file access otherwise.
             *
             *
             *
             */

            catalogServices.putAll(getStaticCatalogServices());

            addServiceAccessToDatasets(graphElements,catalogServices);


            String name, newName, elementType;
            for(Element e : graphElements){

                elementType = e.getName();
                name = e.getAttributeValue(THREDDS.NAME);


                // Add time coverage if needed.
                // Is this an atomic dataset and not a collection?
                if(elementType==THREDDS.DATASET  && !e.getDescendants(new ElementFilter(THREDDS.DATASET,THREDDS.NS)).hasNext() ) {
                    Element timeCoverage = _addTimeCoverage.getTimeCoverage(name);
                    if (timeCoverage != null)
                        e.addContent(1,timeCoverage);
                }

                // Apply the Namer to get news names, if any.
                newName = _catalogNamer.getName(name);
                if(newName!=null){
                    e.setAttribute(THREDDS.NAME,newName);
                }

            }


            // Pack (possibly) newly named elements into a sorted thing.
            TreeMap<String, Element> elementsByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for(Element e : graphElements){
                name = e.getAttributeValue(THREDDS.NAME);
                elementsByName.put(name,e);
            }



            // Rebuild document in the correct order.
            if(_ascendingOrder) {
                for (Element e : elementsByName.values()) {
                    topDataset.addContent(e);
                }
            }
            else {
                for (Element e : elementsByName.descendingMap().values()) {
                    topDataset.addContent(e);
                }

            }



            if(_catalogProxies!=null){
                for(Proxy proxy: _catalogProxies){

                    Element proxyDataset = proxy.getProxyDataset(notRenamed);
                    if(proxyDataset!=null){

                        addServiceAccessToDataset(proxyDataset,catalogServices);


                        if(proxy.isTop()){
                            position = 1;
                            if(_metadata.isEmpty())
                                position = 0;
                            topDataset.addContent(position,proxyDataset);
                        }
                        else {
                            topDataset.addContent(proxyDataset);
                        }
                    }


                }

            }



            String catalogString  = xmlo.outputString(catalog);
            _clientResponseCatalogBuffer = catalogString.getBytes();
            _log.debug("getCatalog() - Built THREDDS catalog from BES catalog response: \n{}",catalogString);


        } finally {
            lock.unlock();
        }


    }



    public void addServiceAccessToDataset(Element e ,TreeMap<String, WebServiceHandler> catalogServices) {
        if (e.getName().equals(THREDDS.DATASET)) {

            String datasetID = e.getAttributeValue(THREDDS.ID);
            Element access;

            if (isBesDataset(e)) {


                for (WebServiceHandler wsh : catalogServices.values()) {
                    access = new Element(THREDDS.ACCESS, THREDDS.NS);

                    access.setAttribute("serviceName", wsh.getServiceId());

                    access.setAttribute("urlPath", wsh.getThreddsUrlPath(datasetID));

                    if (wsh.getServiceId().equalsIgnoreCase(FileService.ID)) {
                        if (BesDapDispatcher.allowDirectDataSourceAccess()) {
                            e.addContent(access);
                        }

                    } else {
                        e.addContent(access);
                    }

                }
            } else {
                WebServiceHandler wsh = ServicesRegistry.getWebServiceById(FileService.ID);
                access = new Element(THREDDS.ACCESS, THREDDS.NS);

                access.setAttribute("serviceName", wsh.getServiceId());

                access.setAttribute("urlPath", datasetID);
                e.addContent(access);


            }

        }

    }

    public void addServiceAccessToDatasets(Vector<Element> graphElements,TreeMap<String, WebServiceHandler> catalogServices) {

        for (Element e : graphElements) {

            addServiceAccessToDataset(e,catalogServices);

        }
    }





    private boolean isBesDataset(Element e){


        List<Element> list = e.getChildren(BES.SERVICE_REF,BES.BES_NS);
        Vector<Element> besServiceRefs = new Vector<>(list);

        boolean isBesDataset = false;
        for(Element besServiceRef: besServiceRefs) {

            if(besServiceRef.getTextTrim().equals(BES.DAP_SERVICE_ID)){
                isBesDataset = true;
            }
            else {
                XMLOutputter xmlo =  new XMLOutputter(Format.getCompactFormat());
                _log.warn("loadCatalog() - Unexpected content! BES returned: '{}'",xmlo.outputString(besServiceRef));
            }
            besServiceRef.detach();
        }
        return isBesDataset;

    }



    private Document loadBesCatalog() throws JDOMException, BadConfigurationException, PPTException, IOException {

        ReentrantReadWriteLock.WriteLock lock = _catalogLock.writeLock();

        try {
            lock.lock();

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            Document besShowCatalogDoc = new Document();
            if (!_besApi.getBesCatalog(_besCatalogResourceId, besShowCatalogDoc)) {

                _log.error(xmlo.outputString(besShowCatalogDoc));

            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();


            xmlo.output(besShowCatalogDoc, baos);

            _rawCatalogBuffer = baos.toByteArray();

            _log.debug("loadBesCatalog() - Loaded BES catalog response: \n{}", baos.toString());

            return besShowCatalogDoc;

        } finally {
            lock.unlock();
        }


    }


    @Override
    public void destroy() {
        _clientResponseCatalogBuffer = null;
        _rawCatalogBuffer = null;

    }

    @Override
    public boolean usesMemoryCache() {
        return false;
    }

    @Override
    public boolean needsRefresh() {
        return false;
    }

    @Override
    public void writeCatalogXML(OutputStream os) throws Exception {

        os.write(_clientResponseCatalogBuffer);

    }

    @Override
    public void writeRawCatalogXML(OutputStream os) throws Exception {

    }

    @Override
    public Document getCatalogDocument() throws IOException, JDOMException, SaxonApiException {
        return null;
    }

    @Override
    public Document getRawCatalogDocument() throws IOException, JDOMException, SaxonApiException {
        return null;
    }

    @Override
    public XdmNode getCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {
        XdmNode catalog;
        InputStream is = null;

        Lock lock = _catalogLock.readLock();
        try {
            lock.lock();
            if (_clientResponseCatalogBuffer != null) {
                is = new ByteArrayInputStream(_clientResponseCatalogBuffer);
                _log.debug("getCatalogAsXdmNode(): Reading ingestTransform processed catalog from memory cache. \n{}",new String(_clientResponseCatalogBuffer));
            } else {
                throw new IOException("getCatalogAsXdmNode() - Catalog file was not previously ingested.");
            }


            DocumentBuilder builder = proc.newDocumentBuilder();
            builder.setLineNumbering(true);
            //builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);

            catalog = builder.build(new StreamSource(is));
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    _log.error("Failed to close InputStream. Error Message: " + e.getMessage());
                }

            }
            lock.unlock();
        }
        return catalog;
    }

    @Override
    public XdmNode getRawCatalogAsXdmNode(Processor proc) throws IOException, SaxonApiException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getCatalogKey() {
        return _catalogKey;
    }

    @Override
    public String getPathPrefix() {
        return null;
    }

    @Override
    public String getUrlPrefix() {
        return null;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public String getIngestTransformFilename() {
        return null;
    }

    @Override
    public long getLastModified() {
        return -1;
    }
}
