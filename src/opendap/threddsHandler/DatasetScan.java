package opendap.threddsHandler;

import net.sf.saxon.s9api.SaxonApiException;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.namespaces.THREDDS;
import opendap.ppt.PPTException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created by ndp on 4/13/15.
 */
public class DatasetScan  extends Dataset {

    private Logger _log;

    private BesApi _besApi;

    private Catalog _parentCatalog;
    private String _besCatalogToThreddsCatalogTransformFilename;

    private Filter _filter;
    private Vector<Proxy> _proxies;

    private Element _sourceDatasetScanElement;

    private boolean _useServiceRegistryServices;



    public DatasetScan(
            Catalog parentCatalog,
            Element datasetScan,
            String besCatalogToThreddsCatalogTransformFilename,
            BesApi besApi
    )throws BadConfigurationException {
        super(datasetScan);
        _log = LoggerFactory.getLogger(this.getClass());

        _sourceDatasetScanElement = _sourceDatasetElement;

        _parentCatalog = parentCatalog;

        _besCatalogToThreddsCatalogTransformFilename = besCatalogToThreddsCatalogTransformFilename;
        _besApi = besApi;


        _filter = new Filter(getFilter());
        _proxies = getProxies();

        _useServiceRegistryServices = true;

    }

    public String getPath(){
        return _sourceDatasetScanElement.getAttributeValue("path");
    }

    public String getLocation(){
        return _sourceDatasetScanElement.getAttributeValue("location");
    }

    public Element getNamer(){
        return getCopy(THREDDS.NAMER, THREDDS.NS);
    }

    public Element getFilter(){
        return getCopy(THREDDS.FILTER, THREDDS.NS);
    }

    public boolean increasingSort(){
        boolean ascending = true;
        Element sortElement  = _sourceDatasetScanElement.getChild(THREDDS.SORT,THREDDS.NS);
        if(sortElement!=null){
            Element lexigraphicByNameElement  = sortElement.getChild(THREDDS.LEXIGRAPHIC_BY_NAME,THREDDS.NS);
            if(lexigraphicByNameElement!=null){
                String increasing  = lexigraphicByNameElement.getAttributeValue(THREDDS.INCREASING);
                if(increasing!=null)
                    ascending = Boolean.parseBoolean(increasing);
            }
        }
        return ascending;

    }

    private Element getServiceByName(String name) throws JDOMException, SaxonApiException, IOException {

        Element catalogElement = _parentCatalog.getCatalogDocument().getRootElement();

        Iterator<Element> i = catalogElement.getDescendants(new ElementFilter(THREDDS.SERVICE, THREDDS.NS));

        while(i.hasNext()){

            Element service = i.next();
            String serviceName = service.getAttributeValue(THREDDS.NAME);

            if(serviceName.equalsIgnoreCase(name)){
                return getCopy(service);

            }

        }

        return null;
    }


    private Vector<Element> getDatasetScanServiceElements() throws JDOMException, SaxonApiException, IOException {

        Vector<Element> services = new Vector<>();

        Iterator<Element> i = _sourceDatasetScanElement.getDescendants(new ElementFilter(THREDDS.SERVICE_NAME, THREDDS.NS));


        while(i.hasNext()){
            Element serviceNameElement = i.next();
            String serviceName = serviceNameElement.getTextTrim();

            Element service = getServiceByName(serviceName);

            if (service == null ) {
                _log.error("getDatasetScanServiceElements() - Unable to locate service named '{}'. Skipping.",serviceName);
            }
            else {
                services.add(service);
            }
        }

        return services;
    }



    public Vector<Proxy> getProxies(){

        Vector<Proxy> proxies = new Vector<>();

        Element addProxiesElement =  getCopy(THREDDS.ADD_PROXIES, THREDDS.NS);

        if(addProxiesElement==null)
            return null;

        List<Element> proxiesList = addProxiesElement.getChildren();

        for(Element proxy : proxiesList){
            if(proxy.getName().equals(THREDDS.SIMPLE_LATEST)){
                proxies.add(new SimpleLatest(proxy));
            }
            else if(proxy.getName().equals(THREDDS.LATEST_COMPLETE)){
                proxies.add(new LatestComplete(proxy));
            }

            // Note that we ignore anything we don't have code to handle...
        }



        return proxies;


    }

    public Element getAddTimeCoverage(){
        return getCopy(THREDDS.ADD_TIME_COVERAGE, THREDDS.NS);
    }

    private Element getCopy(String name, Namespace ns){
        Element e = _sourceDatasetScanElement.getChild(name , ns);

        if(e==null)
            return null;


        return (Element) e.clone();

    }

    private Element getCopy(Element e){
        if(e==null)
            return null;
        return (Element) e.clone();
    }




    private String getUrlPrefix() {


        return _parentCatalog.getUrlPrefix() + getPath();

    }

    public boolean matches(String catalogKey){

        String urlPrefix = getUrlPrefix();

        if(catalogKey.startsWith(urlPrefix)){
            Element filter = getFilter();

            if(filter != null){
                _log.error("matches() - Sorry! The filter element is not yet supported.");
            }

            return true;
        }

        return false;


    }


    public Catalog getCatalog(String catalogKey) throws JDOMException, BadConfigurationException, PPTException, IOException, SaxonApiException {


        if(!matches(catalogKey))
            return null;

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        String catalogPath = catalogKey;
        if(!catalogPath.endsWith("/")){
            int lastSlash = catalogPath.lastIndexOf("/");
            if(lastSlash>0) {
                catalogPath = catalogPath.substring(0, catalogPath.lastIndexOf("/"));
            }
            else {

            }
        }



        String besCatalogResourceId = catalogPath;



        if(besCatalogResourceId.startsWith(getUrlPrefix())){
            besCatalogResourceId = besCatalogResourceId.substring(getUrlPrefix().length());
        }




        /*
        Replaced by catalogPath computation above

        if(besCatalogResourceId.endsWith(CatalogManager.DEFAULT_CATALOG_NAME)){
            besCatalogResourceId = besCatalogResourceId.substring(0,besCatalogResourceId.lastIndexOf(CatalogManager.DEFAULT_CATALOG_NAME));
        }
         */






        while(besCatalogResourceId.startsWith("/") && besCatalogResourceId.length()>1)
            besCatalogResourceId = besCatalogResourceId.substring(1);


        String location = getLocation();
        while(location.endsWith("/") && location.length()>1)
            location  = location.substring(0,location.length()-1);


        besCatalogResourceId = location + "/" + besCatalogResourceId;

        Vector<Element> metadata = getMetadata();


        /*
        Replaced by catalogPath computation above
        if(catalogKey.endsWith(CatalogManager.DEFAULT_CATALOG_NAME))
            catalogKey = catalogKey.substring(0,catalogKey.length() - CatalogManager.DEFAULT_CATALOG_NAME.length());

        */



        Namer namer = new Namer(getNamer(), catalogPath);
        AddTimeCoverage addTimeCoverage = new AddTimeCoverage(getAddTimeCoverage(), catalogPath);

        Vector<Element> services  = getDatasetScanServiceElements();

        BesCatalog besCatalog =
                new BesCatalog(
                        _besApi,
                        catalogPath,
                        besCatalogResourceId,
                        _besCatalogToThreddsCatalogTransformFilename,
                        metadata,
                        _filter,
                        increasingSort(),
                        namer,
                        addTimeCoverage,
                        _proxies,
                        services,
                        _useServiceRegistryServices
                );


        return besCatalog;

    }


}
