package opendap.ncml;

import net.sf.saxon.s9api.SaxonApiException;
import opendap.bes.BESManager;
import opendap.bes.BadConfigurationException;
import opendap.bes.dapResponders.BesApi;
import opendap.namespaces.*;
import opendap.ppt.PPTException;
import opendap.threddsHandler.ThreddsCatalogUtil;
import opendap.threddsHandler.Catalog;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/7/11
 * Time: 5:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class NcmlManager {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(NcmlManager.class);



    /**
     * Lock for thread safe operation.
     */
    private static ReentrantReadWriteLock _inventoryLock = new ReentrantReadWriteLock();


    /**
     *   The key String is the dapAccessId
     *   The value Element is the ncml:netcdf element that defines the dataset.
     *
     */
    private static ConcurrentHashMap<String, Element> _ncmlDatasets = new ConcurrentHashMap<String,Element>();



    /**
     *   The key String is the thredds catalogKey
     *   The value Vector<String>  is the ncmlDatasetIds found in the catalog.
     *
     */
    private static ConcurrentHashMap<String, Vector<String> > _ncmlDatasetIdsByCatalogKey = new ConcurrentHashMap<String,Vector<String>>();


    /**
     *   The key String is the dapAccessId
     *   The value long is the last modified time for the catalog that contains the dataset.
     *
     */
    private static ConcurrentHashMap<String, Long> _ncmlDatasetsLastModifiedTimes = new ConcurrentHashMap<String,Long>();



    private static BesApi _besApi = new BesApi();





    public static  void ingestNcml(Catalog catalog) throws SaxonApiException, IOException, JDOMException, InterruptedException {


        Document catDoc = catalog.getCatalogDocument();


        Vector<String> ncmlDatasetIds =  new Vector<String>();

        Vector<Element> ncmlDatasets = getNcmlDatasetElements(catDoc.getRootElement());

        for(Element dataset : ncmlDatasets){
            Vector<String> localDapAccessIds = getLocalDapAccessIDs(dataset,catDoc.getRootElement());

            Element netcdf = dataset.getChild(NCML.NETCDF,NCML.NS);

            if(netcdf!=null){

                for(String dapAccessID: localDapAccessIds){


                    log.debug("DAP ACCESS ID: {}",dapAccessID);

                     _ncmlDatasets.put(dapAccessID, (Element)netcdf.clone());
                    _ncmlDatasetsLastModifiedTimes.put(dapAccessID, catalog.getLastModified());
                     ncmlDatasetIds.add(dapAccessID);

                    sendNcmlToBes(dapAccessID, netcdf);

                }

            }
            else {
                log.error("Encountered unanticipated NCML content in dataset[{}]. This will not be ingested.",dataset.getAttributeValue("ID"));
            }
        }

        if(_ncmlDatasetIdsByCatalogKey.replace(catalog.getCatalogKey(),ncmlDatasetIds)==null){
            _ncmlDatasetIdsByCatalogKey.put(catalog.getCatalogKey(),ncmlDatasetIds);
        }


    }

    public static  void purgeNcmlDatasets(Catalog catalog)  {

        if(catalog != null){

            Vector<String> ncmlDatasetIds =  _ncmlDatasetIdsByCatalogKey.remove(catalog.getCatalogKey());

            if(ncmlDatasetIds!=null){
                for(String ncmlDatasetId: ncmlDatasetIds){
                    _ncmlDatasets.remove(ncmlDatasetId);
                    _ncmlDatasetsLastModifiedTimes.remove(ncmlDatasetId);

                }
            }
        }
        else {
            log.warn("purgeNcmlDatasets() - Received a null Catalog instance!");
        }


    }


    public static boolean isNcmlDataset(String dapAccessID){

        return _ncmlDatasets.containsKey(dapAccessID);

    }

    public static long getLastModified(String dapAccessID){

        Long lmt = _ncmlDatasetsLastModifiedTimes.get(dapAccessID);


        if(lmt==null)
            lmt = (long) -1;


        return lmt;
    }

    public static Element getNcmlDatasetContainer(String dapAccessID){

        opendap.bes.BES bes = BESManager.getBES(dapAccessID);
        String besDatasetID = bes.trimPrefix(dapAccessID);

        log.debug("BES Dataset ID: {}",besDatasetID);

        Element netcdf = _ncmlDatasets.get(dapAccessID);

        if(netcdf==null)
            return null;


        return getSetContainerElement(besDatasetID, "ncml", (Element)netcdf.clone());
    }



    public static void sendNcmlToBes(String dapAccessID, Element netcdf){
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat() );
        opendap.bes.BES bes = BESManager.getBES(dapAccessID);

        String besDatasetID = bes.trimPrefix(dapAccessID);

        log.debug("BES Dataset ID: {}",besDatasetID);

        Element request = new Element("request", opendap.namespaces.BES.BES_NS);


        Element setContainer = getSetContainerElement(besDatasetID, "ncml", (Element) netcdf.clone());;


        log.debug("bes:setContainer: \n{}",xmlo.outputString(setContainer));

        request.addContent(setContainer);

        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":bes_request]";

        request.setAttribute("reqID",reqID);


        Document besCmd = new Document(request);

        Document response  = new Document();


        try {
            if(!_besApi.besTransaction(bes,besCmd,response)){
                log.error("Failed to ingest NcML dataset '{}' BES Error Object: {}",dapAccessID,xmlo.outputString(response));
            }
        } catch (BadConfigurationException e) {
            log.error("Failed to ingest NcML dataset '{}' Msg: {}",dapAccessID,e.getMessage());
        } catch (PPTException e) {
            log.error("Failed to ingest NcML dataset '{}' Msg: {}",dapAccessID,e.getMessage());
        } catch (JDOMException e) {
            log.error("Failed to ingest NcML dataset '{}' Msg: {}", dapAccessID, e.getMessage());
        } catch (IOException e) {
            log.error("Failed to ingest NcML dataset '{}' Msg: {}", dapAccessID, e.getMessage());
        }

    }



    public static  Vector<Element> getNcmlDatasetElements(Element catalog){

        Vector<Element> ncmlDatasets = new Vector<Element>();

        Iterator alldatasets = catalog.getDescendants(new ElementFilter(THREDDS.DATASET, THREDDS.NS));

        Iterator ncmlContent;
        Element dataset;
        while(alldatasets.hasNext()){
            dataset = (Element) alldatasets.next();
            ncmlContent = dataset.getDescendants(new ElementFilter(NCML.NS));

            if(ncmlContent.hasNext())
                ncmlDatasets.add(dataset);
        }

        return ncmlDatasets;
    }










    private static  Vector<String> getLocalDapAccessIDs(Element dataset, Element catalog) throws InterruptedException {


        ThreddsCatalogUtil.SERVICE opendapService = ThreddsCatalogUtil.SERVICE.OPeNDAP;

        Vector<String> serviceURLs = new Vector<String>();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


            String msg;

            HashMap<String, Element> services = collectServices(catalog,opendapService);
            msg = "#### collectServices Found services:\n";
            for (String srvcName : services.keySet())
                msg += "####     Service Name: " + srvcName + "\n"
                        + xmlo.outputString(services.get(srvcName)) + "\n";
            log.debug(msg);

            collectLocalDatasetAccessUrls(dataset, services, null, "", serviceURLs);

            log.debug("#### Accumulated " + serviceURLs.size()
                    + " access URLs.");



        return serviceURLs;
    }


    private static String getServerUrlString(String url)  throws InterruptedException, MalformedURLException {

        URL u = new URL(url);

        return getServerUrlString(u);

    }

    private static String getServerUrlString(URL url)  throws InterruptedException{

        String baseURL = null;

        String protocol = url.getProtocol();

        if (protocol.equalsIgnoreCase("file")) {
            log.debug("Protocol is FILE.");

        } else if (protocol.equalsIgnoreCase("http")) {
            log.debug("Protcol is HTTP.");

            String host = url.getHost();
            /* String path = url.getPath(); */
            int port = url.getPort();

            baseURL = protocol + "://" + host;

            if (port != -1)
                baseURL += ":" + port;
        }

        log.debug("ServerURL: " + baseURL);

        return baseURL;

    }



    private static HashMap<String, Element> collectServices(Element threddsCatalog,
            ThreddsCatalogUtil.SERVICE s)  throws InterruptedException {

        HashMap<String, Element> services = collectServices(threddsCatalog);
        HashMap<String, Element> childSrvcs;

        // If they aren't asking for everything...
        if (s != ThreddsCatalogUtil.SERVICE.ALL) {
            /* boolean done = false; */
            Element service;

            Vector<String> taggedForRemoval = new Vector<String>();

            for (String serviceName : services.keySet()) {
                service = services.get(serviceName);
                if (service.getAttributeValue("serviceType").equalsIgnoreCase(
                        ThreddsCatalogUtil.SERVICE.Compound.toString())) {
                    childSrvcs = collectServices(service, s);
                    if (childSrvcs.isEmpty()) {
                        taggedForRemoval.add(serviceName);
                    }
                } else if (!service.getAttributeValue("serviceType")
                        .equalsIgnoreCase(s.toString())) {
                    taggedForRemoval.add(serviceName);
                }

            }

            for (String serviceName : taggedForRemoval) {
                services.remove(serviceName);
            }
        }
        return services;

    }
    private static HashMap<String, Element> collectServices(Element threddsCatalog)  throws InterruptedException {
        HashMap<String, Element> services = new HashMap<String, Element>();

        Iterator i = threddsCatalog.getDescendants(new ElementFilter(
                THREDDS.SERVICE, THREDDS.NS));

        Element srvcElem;
        while (i.hasNext()) {
            srvcElem = (Element) i.next();
            services.put(srvcElem.getAttributeValue("name"), srvcElem);
        }

        return services;

    }


    private static void collectLocalDatasetAccessUrls(Element dataset,
                                                      HashMap<String, Element> services, String inheritedServiceName,
                                                      String baseServerURL, Vector<String> datasetURLs)  throws InterruptedException {

        String urlPath;
        String serviceName;
        String s;
        Element metadata, dset, access;
        String datasetName;
        /* Iterator i; */

        log.debug("inheritedServiceName: " + inheritedServiceName);

        serviceName = dataset.getAttributeValue("serviceName");
        urlPath = dataset.getAttributeValue("urlPath");
        metadata = dataset.getChild("metadata", THREDDS.NS);
        datasetName = dataset.getAttributeValue("name");

        if (metadata != null){
            String inheritedAttr = metadata.getAttributeValue("inherited");

            if(inheritedAttr!=null && inheritedAttr.equalsIgnoreCase("true")) {
                log.debug("Found inherited metadata");
                s = metadata.getChildText("serviceName", THREDDS.NS);
                if (s != null) {
                    inheritedServiceName = s;
                    log.debug("Updated inheritedServiceName to: "
                            + inheritedServiceName);
                }
            }

        }

        if (urlPath != null) {
            log.debug("<dataset> has urlPath attribute: " + urlPath);

            if (serviceName == null) {
                log.debug("<dataset> missing serviceName attribute. Checking for child element...");
                serviceName = dataset.getChildText("serviceName", THREDDS.NS);
            }
            if (serviceName == null) {
                log.debug("<dataset> missing serviceName childElement. Checking for inherited serviceName...");
                serviceName = inheritedServiceName;
            }

            if (serviceName != null) {
                log.debug("<dataset> has serviceName: " + serviceName);
                datasetURLs.addAll(getLocalAccessURLs(urlPath, serviceName, services));

            }
        }

        Iterator i = dataset.getChildren("access", THREDDS.NS).iterator();
        while (i.hasNext()) {
            access = (Element) i.next();
            log.debug("Located thredds:access element in dataset '"+datasetName+"'");
            datasetURLs.addAll(getLocalAccessURLs(access, services));
        }

        i = dataset.getChildren(THREDDS.DATASET, THREDDS.NS).iterator();

        while (i.hasNext()) {
            dset = (Element) i.next();
            collectLocalDatasetAccessUrls(dset, services, inheritedServiceName,
                    baseServerURL, datasetURLs);
        }

    }





    private static Vector<String> getLocalAccessURLs(Element access,
                                                     HashMap<String, Element> services)  throws InterruptedException {
        String serviceName = access.getAttributeValue("serviceName");
        String urlPath = access.getAttributeValue("urlPath");

        return getLocalAccessURLs(urlPath, serviceName, services);

    }

    private static Vector<String> getLocalAccessURLs(String urlPath, String serviceName,
                                                     HashMap<String, Element> services) throws InterruptedException  {

        Vector<String> accessURLs = new Vector<String>();
        String access, serviceType, sname;
        /* Iterator i; */
        Element srvc;

        Element service = services.get(serviceName);

        if (service != null) {
            serviceType = service.getAttributeValue("serviceType");

            if (serviceType.equalsIgnoreCase("Compound")) {
                Iterator i = service.getChildren("service", THREDDS.NS).iterator();
                while (i.hasNext()) {
                    srvc = (Element) i.next();
                    sname = srvc.getAttributeValue("name");
                    Vector<String> v = getLocalAccessURLs(urlPath, sname, services);
                    accessURLs.addAll(v);
                }

            } else {
                access = urlPath;
                if(!access.startsWith("/"))
                    access = "/" + access;
                accessURLs.add(access);
                log.debug("####  Found access URL: " + access);

            }
        }

        return accessURLs;
    }
    /**
     * Returns the root Element of the XML document located at the URL contained
     * in the passed parameter <code>docUrlString</code>
     *
     * @param docUrlString
     *            The URL of the document to retrieve
     * @return The Document
     */
    private static Element getDocumentRoot(String docUrlString)  throws InterruptedException {

        Element docRoot = null;

        Document doc = getDocument(docUrlString);
        if (doc != null) {
            docRoot = doc.getRootElement();
        }
        return docRoot;
    }

    /**
     * Returns the Document object for the XML document located at the URL
     * contained in the passed parameter String <code>docUrlString</code>.
     *
     * @note This is the point in the class where a response is (possibly)
     * cached.
     *
     * @param docUrlString
     *            The URL of the document to retrieve.
     * @return The Document
     */
    private static Document getDocument(String docUrlString)  throws InterruptedException {

        Document doc = null;
        try {

            URL docUrl = new URL(docUrlString);
            SAXBuilder sb = new SAXBuilder();

            log.debug("Retrieving XML Document: " + docUrlString);
            log.debug("Document URL INFO: \n" + getUrlInfo(docUrl));

            doc = sb.build(docUrl);
            log.debug("Loaded XML Document.");
        }
        catch (MalformedURLException e) {
            log.error("Problem with XML Document URL: " + docUrlString
                    + " Caught a MalformedURLException.  Message: "
                    + e.getMessage());
        }
        catch (IOException e) {
            log.error("Problem retrieving XML Document: " + docUrlString
                    + " Caught a IOException.  Message: " + e.getMessage());
        }
        catch (JDOMException e) {
            log.error("Problem parsing XML Document: " + docUrlString
                    + " Caught a JDOMException.  Message: " + e.getMessage());
        }

        return doc;

    }

    public static String getUrlInfo(URL url)  throws InterruptedException{
        String info = "URL:\n";

        info += "    getHost():         " + url.getHost() + "\n";
        info += "    getAuthority():    " + url.getAuthority() + "\n";
        info += "    getFile():         " + url.getFile() + "\n";
        info += "    getSystemPath():         " + url.getPath() + "\n";
        info += "    getDefaultPort():  " + url.getDefaultPort() + "\n";
        info += "    getPort():         " + url.getPort() + "\n";
        info += "    getProtocol():     " + url.getProtocol() + "\n";
        info += "    getQuery():        " + url.getQuery() + "\n";
        info += "    getRef():          " + url.getRef() + "\n";
        info += "    getUserInfo():     " + url.getUserInfo() + "\n";

        return info;
    }




    public static Element getSetContainerElement(String name,
                                                 String space,
                                                 Element source
    ) {

        Element e = new Element("setContainer",opendap.namespaces.BES.BES_NS);
        e.setAttribute("name",name);
        e.setAttribute("space",space);
        e.addContent(source);
        return e;
    }






}
