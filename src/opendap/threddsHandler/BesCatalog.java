package opendap.threddsHandler;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesDapDispatcher;
import opendap.bes.dap2Responders.BesApi;
import opendap.dap.Dap2Service;
import opendap.namespaces.THREDDS;
import opendap.ppt.PPTException;
import opendap.services.Service;
import opendap.services.ServicesRegistry;
import opendap.viewers.NcWmsService;
import opendap.viewers.WebServiceHandler;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.List;
import java.util.Vector;
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



    private ReentrantReadWriteLock _catalogLock;
    private byte[] _rawCatalogBuffer;


    private byte[] _clientResponseCatalogBuffer;


    BesCatalog(BesApi besApi,
               String catalogKey,
               String besCatalogResourceId,
               String besCatalogToThreddsCatalogTransformFilename,
               Vector<Element> metadata,
               Filter catalogFilter
    ) throws JDOMException, BadConfigurationException, PPTException, IOException, SaxonApiException {

        _log = LoggerFactory.getLogger(this.getClass());

        _catalogLock = new ReentrantReadWriteLock();

        _besApi = besApi;

        _catalogKey = catalogKey;

        _besCatalogResourceId = besCatalogResourceId;

        _ingestTransformer = besCatalogToThreddsCatalogTransformFilename;

        _metadata = metadata;

        _catalogFilter = catalogFilter;

        loadCatalog();

    }


    private void loadCatalog() throws JDOMException, BadConfigurationException, PPTException, IOException, SaxonApiException {


        ReentrantReadWriteLock.WriteLock lock = _catalogLock.writeLock();
        try {
            lock.lock();

            Document besShowCatalogDoc = loadBesCatalog();

            Transformer showCatalogToThreddsCatalog = new Transformer(_ingestTransformer);


            WebServiceHandler dapService = ServicesRegistry.getWebServiceById(Dap2Service.ID);


            showCatalogToThreddsCatalog.setParameter("dapService", dapService.getBase());


            String base = null;
            String dsId = null;

            Service s = ServicesRegistry.getWebServiceById(NcWmsService.ID);
            if (s != null && s instanceof NcWmsService) {
                NcWmsService nws = (NcWmsService) s;
                base = nws.getBase();
                dsId = nws.getDynamicServiceId();
                showCatalogToThreddsCatalog.setParameter("ncWmsServiceBase", base);
                showCatalogToThreddsCatalog.setParameter("ncWmsDynamicServiceId", dsId);
            }
            _log.debug("cacheRawCatalogFileContent() - ncWMS service base:" + base);


            if (BesDapDispatcher.allowDirectDataSourceAccess())
                showCatalogToThreddsCatalog.setParameter("allowDirectDataSourceAccess", "true");

            if (BesDapDispatcher.useDAP2ResourceUrlResponse())
                showCatalogToThreddsCatalog.setParameter("useDAP2ResourceUrlResponse", "true");

            JDOMSource besCatalog = new JDOMSource(besShowCatalogDoc);

            //ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //showCatalogToThreddsCatalog.transform(besCatalog, baos);

            Document catalog = showCatalogToThreddsCatalog.getTransformedDocument(besCatalog);
            Element catalogElement = catalog.getRootElement();

            Element topDataset =  catalogElement.getChild(THREDDS.DATASET, THREDDS.NS);


            topDataset.setAttribute(THREDDS.NAME,getCatalogKey());


            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


            /**
             * Add Metadata!!
             */

            if(_metadata!=null){
                for(Element mdata : _metadata){
                    _log.debug("cacheRawCatalogFileContent() - Retrieved  metadata element: \n{}",xmlo.outputString(mdata));

                    topDataset.addContent(0,(Element) mdata.clone());
                }
            }



            /**
             * Apply Filter!!
             */

            List elementList = topDataset.getChildren(THREDDS.DATASET,THREDDS.NS);

            Vector<Element> dropList = new Vector<>();

            for(Object o : elementList){
                Element dataset = (Element) o;
                String name = dataset.getAttributeValue(THREDDS.NAME);

                if(!_catalogFilter.include(name,false)){
                    dropList.add(dataset);
                }
            }

            elementList = topDataset.getChildren(THREDDS.CATALOG_REF,THREDDS.NS);

            for(Object o : elementList){
                Element catRef = (Element) o;
                String name = catRef.getAttributeValue(THREDDS.NAME);

                if(!_catalogFilter.include(name,false)){
                    dropList.add(catRef);
                }
            }


            for(Element e : dropList)
                e.detach();










            String catalogString  = xmlo.outputString(catalog);
            _clientResponseCatalogBuffer = catalogString.getBytes();
            _log.debug("getCatalog() - Built THREDDS catalog from BES catalog response: \n{}",catalogString);


        } finally {
            lock.unlock();
        }


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
                _log.debug("getCatalogAsXdmNode(): Reading ingestTransform processed catalog from memory cache.");
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
