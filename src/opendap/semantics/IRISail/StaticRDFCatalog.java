package opendap.semantics.IRISail;

import opendap.wcs.v1_1_2.*;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.ntriples.NTriplesWriter;


import com.ontotext.trree.owlim_ext.SailImpl;

/**
 * A colon of LocalFileCatalog
 */
public class StaticRDFCatalog implements WcsCatalog, Runnable {


    private Logger log; // = LoggerFactory.getLogger(StaticRDFCatalog.class);


    private AtomicBoolean repositoryUpdateActive;
    private ReentrantReadWriteLock _repositoryLock;
    private IRISailRepository owlse2;
    private XMLfromRDF buildDoc;

    //private static boolean _useMemoryCache;
    //private  Date _cacheTime;
    private long _lastModified;

    private ConcurrentHashMap<String, CoverageDescription> coverages;
    private ReentrantReadWriteLock _catalogLock;

    private Thread catalogUpdateThread;
    private long firstUpdateDelay;
    private long catalogUpdateInterval;
    private long timeOfLastUpdate;


    private boolean stopWorking = false;

    private Element _config;


    private String catalogCacheDirectory;
    private String owlim_storage_folder;
    private String resourcePath;
    private boolean backgroundUpdates;
    private HashMap<String, Vector<String> >  coverageIDServer;

    

    private boolean initialized;


    public StaticRDFCatalog() {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        catalogUpdateInterval = 20 * 60 * 1000; // 20 minutes worth of milliseconds
        firstUpdateDelay = 5 * 1000; // 5 second worth of milliseconds
        timeOfLastUpdate = 0;
        stopWorking = false;

        _catalogLock = new ReentrantReadWriteLock();
        coverages = new ConcurrentHashMap<String, CoverageDescription>();

        _repositoryLock = new ReentrantReadWriteLock();
        repositoryUpdateActive = new AtomicBoolean();

        repositoryUpdateActive.set(false);

        backgroundUpdates = false;

        owlse2 = null;
        buildDoc = null;
        _lastModified = -1;
        _config = null;
        catalogCacheDirectory = null;
        owlim_storage_folder ="owlim-storage";
        resourcePath = null;

        initialized = false;
    }


    public static void main(String[] args) {

        long startTime, endTime;
        double elapsedTime;


        StaticRDFCatalog catalog = new StaticRDFCatalog();


        try {


            Map<String, String> env = System.getenv();
            catalog.resourcePath = ".";
            catalog.catalogCacheDirectory = ".";

            String configFileName;

            configFileName = "file:///data/haibo/workspace/ioos/wcs_service.xml";
            if (args.length > 0)
                configFileName = args[0];


            catalog.log.debug("main() using config file: " + configFileName);
            Element olfsConfig = opendap.xml.Util.getDocumentRoot(configFileName);

            catalog._config = (Element) olfsConfig.getDescendants(new ElementFilter("WcsCatalog")).next();
            catalog.processConfig(catalog._config, catalog.catalogCacheDirectory, catalog.resourcePath);

            catalog.loadWcsCatalogFromRepository();

            for (int i = 0; i < 1; i++) {
                startTime = new Date().getTime();
                //catalog.setupRepository();
                catalog.updateCatalog();
                //catalog.destroy();
                endTime = new Date().getTime();
                elapsedTime = (endTime - startTime) / 1000.0;
                catalog.log.debug("Completed catalog update in " + elapsedTime + " seconds.");
                catalog.log.debug("########################################################################################");
                catalog.log.debug("########################################################################################");
                catalog.log.debug("########################################################################################");
                catalog.setStopFlag(false);
                //Thread.sleep(5000);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            catalog.destroy();

        }
    }

    public void loadWcsCatalogFromRepository() throws InterruptedException, RepositoryException {
        long startTime, endTime;
        double elapsedTime;
        log.info("#############################################");
        log.info("#############################################");
        log.info("Loading WCS Catalog from Semantic Repository.");
        startTime = new Date().getTime();
        setupRepository();
        extractCoverageDescrptionsFromRepository();
        owlse2.updateIdCaches();
        updateCatalogCache();
        shutdownRepository();
        endTime = new Date().getTime();
        elapsedTime = (endTime - startTime) / 1000.0;
        log.info("WCS Catalog loaded from the Semantic Repository. Loaded in "+ elapsedTime + " seconds.");
        log.info("#############################################");
        log.info("#############################################");
    }

    public String getDataAccessUrl(String coverageID){

        return coverageIDServer.get(coverageID).firstElement();

    }

    public void init(Element config, String defaultCacheDirectory, String defaultResourcePath) throws Exception {

        if (initialized)
            return;



        backgroundUpdates = false;

        _config = config;

        processConfig(_config,defaultCacheDirectory, defaultResourcePath );

        loadWcsCatalogFromRepository();

        if (backgroundUpdates) {
            catalogUpdateThread = new Thread(this);
            catalogUpdateThread.start();
        } else {
            updateCatalog();
        }



        initialized = true;
    }


    private void processConfig(Element config,String defaultCacheDirectory, String defaultResourcePath){

        Element e;
        File file;

        /** ########################################################
         * Process configuration.
         */
        catalogCacheDirectory = defaultCacheDirectory;
        e = config.getChild("CacheDirectory");
        if (e != null)
            catalogCacheDirectory = e.getTextTrim();
        if (catalogCacheDirectory != null &&
                catalogCacheDirectory.length() > 0 &&
                !catalogCacheDirectory.endsWith("/"))
            catalogCacheDirectory += "/";

        file = new File(catalogCacheDirectory);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.error("Unable to create cache directory: " + catalogCacheDirectory);
                if(!catalogCacheDirectory.equals(defaultCacheDirectory)){
                    file = new File(defaultCacheDirectory);
                    if (!file.exists()) {
                        if (!file.mkdirs()) {
                            log.error("Unable to create cache directory: " + defaultCacheDirectory);
                            log.error("Process probably doomed...");
                        }
                    }
                }
                else {
                    log.error("Process probably doomed...");
                }

            }
        }
        log.info("Using catalogCacheDirectory: "+ catalogCacheDirectory);

        resourcePath = defaultResourcePath;
        e = config.getChild("ResourcePath");
        if (e != null)
            resourcePath = e.getTextTrim();

        if (resourcePath != null &&
                resourcePath.length() > 0 &&
                !resourcePath.endsWith("/"))
            resourcePath += "/";

        file = new File(this.resourcePath);
        if (!file.exists()) {
            log.error("Unable to locate resource directory: " + resourcePath);
            file = new File(defaultResourcePath);
            if (!file.exists()) {
                log.error("Unable to locate default resource directory: " + defaultResourcePath);
                log.error("Process probably doomed...");
            }

        }

        log.info("Using resourcePath: "+resourcePath);

        e = config.getChild("useUpdateCatalogThread");
        if(e != null){
            backgroundUpdates = true;
            String s = e.getAttributeValue("updateInterval");
            if (s != null){
                catalogUpdateInterval = Long.parseLong(s) * 1000;
            }
            s = e.getAttributeValue("firstUpdateDelay");
            if (s != null){
                firstUpdateDelay = Long.parseLong(s) * 1000;
            }

        }
        log.info("backgroundUpdates:       "+backgroundUpdates);
        log.info("Catalog update interval: "+catalogUpdateInterval+"ms");
        log.info("First update delay:     "+firstUpdateDelay+"ms");



    }
    private void shutdownRepository() throws RepositoryException, InterruptedException {
        log.debug("Shutting down Repository...");
        owlse2.shutDown();
        log.debug("Repository shutdown complete.");
    }

    private void setupRepository() throws RepositoryException, InterruptedException {


        log.info("Setting up Semantic Repository.");

        //OWLIM Sail Repository (inferencing makes this somewhat slow)
        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        owlse2 = new IRISailRepository(owlimSail, resourcePath, catalogCacheDirectory); //owlim inferencing


        //owlse2 = new IRISailRepository(new MemoryStore()); //memory store

        log.info("Configuring Semantic Repository.");
        File storageDir = new File(catalogCacheDirectory); //define local copy of repository
        owlimSail.setDataDir(storageDir);
        log.debug("Semantic Repository Data directory set to: "+ catalogCacheDirectory);
        // prepare config
        owlimSail.setParameter("storage-folder", owlim_storage_folder);
        log.debug("Semantic Repository 'storage-folder' set to: "+owlim_storage_folder);

        // Choose the operational ruleset
        String ruleset;
        ruleset = "owl-horst";
        //ruleset = "owl-max";

        owlimSail.setParameter("ruleset", ruleset);
        //owlimSail.setParameter("ruleset", "owl-max");
        //owlimSail.setParameter("partialRDFs", "false");
        log.debug("Semantic Repository 'ruleset' set to: "+ ruleset);


        log.info("Intializing Semantic Repository.");

        // Initialize repository
        owlse2.initialize(); //needed
        log.info("Semantic Repository Ready.");

        if(Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");


    }

    private void updateSemanticRepository(RepositoryConnection con,
            Vector<String> importURLs) throws InterruptedException,
            RepositoryException {
        URI uriaddress;
        URL inUrl;
        Thread thread = Thread.currentThread();
        Vector<String> import2Update = new Vector<String>(); //separate import and delete 
        
        Date startTime = new Date();
        log.info("Evaluating importURLs for updateCatalog... ");

        for (String importURL : importURLs) {
            try {

                // log.info("Importing "+importURL);
                inUrl = new URL(importURL);
                uriaddress = new URIImpl(importURL);
                // owlse2.printLTMODContext(importURL);
                if (owlse2.olderContext(importURL)) {// if new updateCatalog
                                                        // available delete old
                                                        // one

                   
                    if (owlse2.hasContext(uriaddress, con)) {
                        log.info("Removing URL: " + importURL);
                        con.clear((Resource) uriaddress);

                        log.info("Finished removing URL: " + importURL);

                        if (thread.isInterrupted()) {
                            log.warn("updateSemanticRepository(): WARNING! Thread "
                                            + thread.getName()
                                            + " was interrupted!");
                            return;
                        }

                        log.info("Removing last_modified_time of URL: "
                                + importURL);
                        owlse2.deleteLTMODContext(importURL, con);
                                                
                        // deleteIsContainedBy(importURL, CollectionURL); //need
                        // some work here!!!
                        log.info("Finished removing last_modified_time of URL: "
                                        + importURL);
                        if (thread.isInterrupted()) {
                            log.warn("updateSemanticRepository(): WARNING! Thread "
                                            + thread.getName()
                                            + " was interrupted!");
                            return;
                        }
                    }
                    log.info("Importing URL: " + inUrl);
                    //con.add(inUrl, importURL, RDFFormat.RDFXML,(Resource) uriaddress);
                    log.info("Finished Importing URL: " + inUrl);
                    import2Update.add(importURL); // import need to update
                    if (thread.isInterrupted()) {
                        log.warn("updateSemanticRepository(): WARNING! Thread "
                                + thread.getName() + " was interrupted!");
                        return;
                    }
                    /*
                    log.info("Setting last modified time for context: "
                                    + inUrl);
                    owlse2.setLTMODContext(importURL, con); // set last modified
                                                            // time for the
                                                            // context
                    log.info("Finished setting last modified time for context: " + inUrl);
                    if (thread.isInterrupted()) {
                        log.warn("updateSemanticRepository(): WARNING! Thread "
                                + thread.getName() + " was interrupted!");
                        return;
                    }

                     setIsContainedBy(importURL, CollectionURL); //need some work here!!!
                    log.info("Adding last_modified_time of URL: " + importURL);
                    owlse2.imports.add(importURL); // track what is added in
                                                    // the repository
                    log.info("Finished Adding last_modified_time of URL: "
                            + importURL);
                    if (thread.isInterrupted()) {
                        log.warn("updateSemanticRepository(): WARNING! Thread "
                                + thread.getName() + " was interrupted!");
                        return;
                    }
                   */
                } else {
                    if (!owlse2.imports.contains(importURL)) {
                        owlse2.imports.add(importURL); // track what is added
                                                        // in the repository
                    }
                    log.info("Skip import: " + inUrl);
                }

            } catch (MalformedURLException e) {
                log.error("Failed to import " + importURL
                        + "  MalformedURLException message: " + e.getMessage());
            } 
            //catch (IOException e) {
            //    log.error("Failed to import " + importURL
            //            + "  IOException message: " + e.getMessage());
            //} 
           // catch (RDFParseException e) {
           //     log.error("Failed to import " + importURL
           //             + "  RDFParseException message: " + e.getMessage());
           // } 
            catch (RepositoryException e) {
                log.error("Failed to import " + importURL
                        + "  RepositoryException message: " + e.getMessage());
            }

        }
        
        for (String importURL : import2Update) {
            try {
                inUrl = new URL(importURL);
                uriaddress = new URIImpl(importURL);
                log.info("Importing URL: " + inUrl);
                con.add(inUrl, importURL, RDFFormat.RDFXML,
                        (Resource) uriaddress);
                log.info("Finished Importing URL: " + inUrl);
                if (thread.isInterrupted()) {
                    log.warn("updateSemanticRepository(): WARNING! Thread "
                            + thread.getName() + " was interrupted!");
                    return;
                }

                log.info("Setting last modified time for context: " + inUrl);
                owlse2.setLTMODContext(importURL, con); // set last modified
                                                        // time for the context
                log.info("Finished setting last modified time for context: "
                        + inUrl);
                if (thread.isInterrupted()) {
                    log.warn("updateSemanticRepository(): WARNING! Thread "
                            + thread.getName() + " was interrupted!");
                    return;
                }

                // setIsContainedBy(importURL, CollectionURL); //need some work
                // here!!!
                log.info("Adding last_modified_time of URL: " + importURL);
                owlse2.imports.add(importURL); // track what is added in the
                                                // repository
                log.info("Finished Adding last_modified_time of URL: "
                        + importURL);
                if (thread.isInterrupted()) {
                    log.warn("updateSemanticRepository(): WARNING! Thread "
                            + thread.getName() + " was interrupted!");
                    return;
                }
            } catch (RDFParseException e) {
                log.error("Failed to import " + importURL
                        + "  RDFParseException message: " + e.getMessage());
            } catch (MalformedURLException e) {
                log.error("Failed to import " + importURL
                        + "  MalformedURLException message: " + e.getMessage());
            } catch (IOException e) {
                log.error("Failed to import " + importURL
                        + "  IOException message: " + e.getMessage());
            }
        }
        
        
        long elapsedTime = new Date().getTime() - startTime.getTime();
        log.info("Imports Evaluated. Elapsed time: " + elapsedTime + "ms");

        log
                .debug("**********************************************************************************!");
        log.info("Updating repository!");
        Boolean updated = owlse2.update();

        /*****************************************************
         * ingest swrl rules
         *****************************************************/
        ingestSwrlRules();

        log.info("Repository updateCatalog =" + updated);
        log.info("RDF import complete.");

    }


    private void ingestSwrlRules() throws RepositoryException{
        log.info("Running runConstruct ..");
        owlse2.runConstruct();
        log.info("Complete running runConstruct ..");
    }


    private void updateRepository(Vector<String> importURLs) throws RepositoryException,InterruptedException {

        RepositoryConnection con = owlse2.getConnection();
        log.info("Repository connection has been opened.");

        updateSemanticRepository(con, importURLs);

        log.info("Closing repository connection.");
        con.close();  //close connection first

    }


    public void dumpRepository(RepositoryConnection con, String filename) {

        //export repository to an n-triple file
        File outrps = new File(filename); //hard copy of repository
        try {
            FileOutputStream myFileOutputStream = new FileOutputStream(outrps);
            NTriplesWriter myNTRiplesWriter = new NTriplesWriter(myFileOutputStream);

            //log.info("Dumping explicit statements (as Ntriples) to: " + outrps.getAbsoluteFile());

            myNTRiplesWriter.startRDF();
            myNTRiplesWriter.endRDF();

            con.export(myNTRiplesWriter);
            log.info("Completed dumping explicit statements");


        } catch (Throwable e) {
            log.warn(e.getMessage());
        }

    }

    public void dumpRepository(String filename) throws RepositoryException {

        RepositoryConnection con = owlse2.getConnection();
        log.info("Repository connection has been opened.");

        dumpRepository(con, filename);

        log.info("Closing repository connection.");
        con.close();  //close connection first


    }

    private void processContexts() throws RepositoryException {

        RepositoryConnection con = owlse2.getConnection();
        log.info("Repository connection has been opened.");

        processContexts(con);

        log.info("Closing repository connection.");
        con.close();  //close connection first
    }

    private void processContexts(RepositoryConnection con) throws RepositoryException {

        /* ###################################################
        Looking at the code I concluded that this block was
        diagnostic cruft that seemed to be pretty useless.
        So I commented it out. We can put it back if needed. (ndp)*/

        //retrieve context
        RepositoryResult<Resource> contextID = con.getContextIDs();
        int contextTol = 0;
        if (!contextID.hasNext()) {
            log.warn("No Contexts found!");
        } else {
            while (contextID.hasNext()) {
                String ctstr = contextID.next().toString();
                log.info("Context: " + ctstr);
                owlse2.printLTMODContext(ctstr);
                contextTol++;
            }
        }
        contextID.close(); //needed to release resources
        log.info("Found  " + contextTol + " Contexts");
        /*########################################################*/

    }


    private void extractCoverageDescrptionsFromRepository() throws RepositoryException {
        RepositoryConnection con = owlse2.getConnection();
        log.info("Repository connection has been opened.");

        extractCoverageDescrptionsFromRepository(con);

        log.info("Closing repository connection.");
        con.close();  //close connection first

    }


    private void extractCoverageDescrptionsFromRepository(RepositoryConnection con) {
        //retrieve XML from the RDF store.
        log.info("Extracting CoverageDescriptions from repository.");
        buildDoc = new XMLfromRDF(con, "CoverageDescriptions", "http://www.opengis.net/wcs/1.1#CoverageDescription");
        buildDoc.getXMLfromRDF("http://www.opengis.net/wcs/1.1#CoverageDescription"); //build a JDOM doc by querying against the RDF store


    }





    private void dumpCoverageDescriptionsDocument(String filename) {
        //print out the XML
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());


        /* #####################################################
        Why is this being written out?
        Is this a diagnostic? purely for diagnostic purposes (HB Dec032009)
        Or is this file used later by some other part of the software? No. (HB Dec032009)
        Can we remove this??? Yes, once we are happy with the JDom Doc retrieval (HB Dec032009)
        */
        try {
            File destinationFile = new File(filename);
            FileOutputStream fos = new FileOutputStream(destinationFile);
            outputter.output(buildDoc.getDoc(), fos);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        /* ############################################## */


    }




    public void destroy() {


        log.debug("destroy(): Attempting to aquire WriteLock from _catalogLock and _repositoryLock.");

        ReentrantReadWriteLock.WriteLock catLock = _catalogLock.writeLock();
        ReentrantReadWriteLock.WriteLock reposLock = _repositoryLock.writeLock();


        try {
            catLock.lock();
            reposLock.lock();
            log.debug("destroy(): WriteLocks Aquired.");
            setStopFlag(true);
            if(catalogUpdateThread!=null){
                log.debug("destroy() Current thread '"+Thread.currentThread().getName()+"' Interrupting catalogUpdateThread '"+catalogUpdateThread+"'");
                catalogUpdateThread.interrupt();
                log.debug("destroy(): catalogUpdateThread '"+catalogUpdateThread+"' interrupt() called.");
            }
            log.info("destroy(): Shutting Down Semantic Repository.");

            if (!owlse2.isRepositoryDown()){
                owlse2.shutDown();
                log.info("destroy(): Semantic Repository Has Been Shutdown.");
            }else {
                log.info("destroy(): Semantic Repository was already down.");

            }
        } catch (RepositoryException e) {
            log.error("destroy(): Failed to shutdown Semantic Repository.");
        }
        finally{
            catLock.unlock();
            reposLock.unlock();
            log.debug("destroy(): Released WriteLock for _catalogLock and _repositoryLock.");
            log.debug("destroy(): Complete.");
        }

    }




    private Vector<String> getRdfImports(Element config) {

        Vector<String> rdfImports = new Vector<String>();
        Element e;
        String s;


        /**
         * Load individual dataset references
         */
        Iterator i = config.getChildren("dataset").iterator();
        String datasetURL;
        while (i.hasNext()) {
            e = (Element) i.next();
            datasetURL = e.getTextNormalize();

            if (!datasetURL.endsWith(".rdf")) {

                if (datasetURL.endsWith(".ddx") |
                        datasetURL.endsWith(".dds") |
                        datasetURL.endsWith(".das")
                        ) {
                    datasetURL = datasetURL.substring(0, datasetURL.lastIndexOf("."));
                }
                datasetURL += ".rdf";
            }
            rdfImports.add(datasetURL);
            log.info("Added dataset reference " + datasetURL + " to RDF imports list.");

            log.debug("<wcs:Identifier>"+owlse2.getWcsIdString(datasetURL)+"</wcs:Identifier>");
        }


        /**
         * Load THREDDS Catalog references.
         */
        i = config.getChildren("ThreddsCatalog").iterator();
        String catalogURL;
        boolean recurse;
        while (i.hasNext()) {
            e = (Element) i.next();
            catalogURL = e.getTextNormalize();
            recurse = false;
            s = e.getAttributeValue("recurse");
            if (s != null && s.equalsIgnoreCase("true"))
                recurse = true;

            ThreddsCatalogUtil tcu = null;
            try {
            	// Passing false means no caching but also no exception.
            	// Maybe there's a better way to code the TCU ctor?
            	tcu = new ThreddsCatalogUtil();
            }
            catch (Exception e1) {
            	log.debug("ThreddsCatalogUtil exception: " + e1.getMessage());
            }
            
            Vector<String> datasetURLs = tcu.getDataAccessURLs(catalogURL, ThreddsCatalogUtil.SERVICE.OPeNDAP, recurse);

            for (String dataset : datasetURLs) {
                dataset += ".rdf";
                rdfImports.add(dataset);
                log.info("Added dataset reference " + dataset + " to RDF imports list.");
            }

        }


        /**
         * Load RDF Imports
         */
        i = config.getChildren("RdfImport").iterator();
        while (i.hasNext()) {
            e = (Element) i.next();
            s = e.getTextNormalize();
            rdfImports.add(s);
            log.info("Added reference " + s + " to RDF imports list.");
        }

        return rdfImports;

    }


    private void ingestCatalog() throws Exception {

        log.info("Ingesting catalog from CoverageDescriptions Document built by the XMLFromRDF object...");


        List<Element> cd = buildDoc.getDoc().getRootElement().getChildren();
        Iterator<Element> i = cd.iterator();
        HashMap<String, String> idltm = owlse2.getLMT();
        String lastMDT = "nolastMDT";
        while (i.hasNext()) {
            Element e = i.next();

            List<Element> elist = e.getChildren();
            Iterator<Element> j = elist.iterator();

            while (j.hasNext()) {
                Element eID = j.next();
                String idstr = eID.getName();
                if (idstr.equalsIgnoreCase("Identifier")) {

                    lastMDT = idltm.get(eID.getText());

                    String datetime = lastMDT.substring(0, 10) + " " + lastMDT.substring(11, 19) + " +0000";

                    DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                    Date date = sdf.parse(datetime);
                    log.debug("Date and Time: " + date.getTime());

                    ingestCoverageDescription(e, date.getTime());
                    //log.debug("element: "+ eID.getText());
                    //log.debug("lastMDT = "+ lastMDT);
                    log.debug("Add element " + e.getName());
                }
            }
        }//while(i.hasNext()

        _lastModified = -1;

    }

    public void ingestCoverageDescription(URL server, Element cde, long lastModified) throws Exception {

    }


    public void ingestCoverageDescription(Element cde, long lastModified) {

        CoverageDescription cd;
        try {
            cd = new CoverageDescription(cde, lastModified);
            coverages.put(cd.getIdentifier(), cd);
            log.info("Ingested CoverageDescription: " + cd.getIdentifier());
        } catch (WcsException e) {
            XMLOutputter xmlo = new XMLOutputter(Format.getCompactFormat());
            String wcseElem = xmlo.outputString(e.getExceptionElement());
            String cvgDesc = xmlo.outputString(cde);
            log.error("ingestCoverageDescription(): Failed to ingest CoverageDescription!");
            log.error("ingestCoverageDescription(): WcsException: " + wcseElem + "");
            log.error("ingestCoverageDescription(): Here is the XML element that failed to ingest: " + cvgDesc);
        }
    }

    public boolean hasCoverage(String id) {

        log.debug("Looking for a coverage with ID: " + id);

        ReentrantReadWriteLock.ReadLock lock = _catalogLock.readLock();
        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            return coverages.containsKey(id);
        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }
    }

    public Element getCoverageDescriptionElement(String id) {

        ReentrantReadWriteLock.ReadLock lock = _catalogLock.readLock();
        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            CoverageDescription cd = coverages.get(id);

            if(cd==null)
                return null;

            return cd.getElement();
        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }
    }

    public List<Element> getCoverageDescriptionElements() throws WcsException {
        throw new WcsException("getCoverageDescriptionElements() method Not Implemented", WcsException.NO_APPLICABLE_CODE);
    }


    public CoverageDescription getCoverageDescription(String id) {
        ReentrantReadWriteLock.ReadLock lock = _catalogLock.readLock();
        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            return coverages.get(id);
        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }
    }


    public Element getCoverageSummaryElement(String id) throws WcsException {

        ReentrantReadWriteLock.ReadLock lock = _catalogLock.readLock();
        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            return coverages.get(id).getCoverageSummary();
        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }



    }

    public List<Element> getCoverageSummaryElements() throws WcsException {


        ArrayList<Element> coverageSummaries = new ArrayList<Element>();

        Enumeration e;

        CoverageDescription cd;


        ReentrantReadWriteLock.ReadLock lock = _catalogLock.readLock();

        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");
            // Get all of the unique formats.
            e = coverages.elements();
            while (e.hasMoreElements()) {
                cd = (CoverageDescription) e.nextElement();
                coverageSummaries.add(cd.getCoverageSummary());

            }
        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }





        return coverageSummaries;
    }


    public List<Element> getSupportedFormatElements() {


        ArrayList<Element> supportedFormats = new ArrayList<Element>();
        HashMap<String, Element> uniqueFormats = new HashMap<String, Element>();

        Enumeration enm;
        Element e;
        Iterator i;
        CoverageDescription cd;


        ReentrantReadWriteLock.ReadLock lock = _catalogLock.readLock();

        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");


            // Get all of the unique formats.
            enm = coverages.elements();
            while (enm.hasMoreElements()) {
                cd = (CoverageDescription) enm.nextElement();

                i = cd.getSupportedFormatElements().iterator();

                while (i.hasNext()) {
                    e = (Element) i.next();
                    uniqueFormats.put(e.getTextTrim(), e);
                }
            }

            i = uniqueFormats.values().iterator();
            while (i.hasNext()) {
                supportedFormats.add((Element) i.next());
            }
        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }


        return supportedFormats;
    }

    public List<Element> getSupportedCrsElements() {

        ArrayList<Element> supportedCRSs = new ArrayList<Element>();
        HashMap<String, Element> uniqueCRSs = new HashMap<String, Element>();

        Enumeration enm;
        Element e;
        Iterator i;
        CoverageDescription cd;


        ReentrantReadWriteLock.ReadLock lock = _catalogLock.readLock();

        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            // Get all of the unique formats.
            enm = coverages.elements();
            while (enm.hasMoreElements()) {
                cd = (CoverageDescription) enm.nextElement();

                i = cd.getSupportedCrsElements().iterator();

                while (i.hasNext()) {
                    e = (Element) i.next();
                    uniqueCRSs.put(e.getTextTrim(), e);
                }
            }

            i = uniqueCRSs.values().iterator();
            while (i.hasNext()) {
                supportedCRSs.add((Element) i.next());
            }
        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }


        return supportedCRSs;
    }

    public String getLatitudeCoordinateDapId(String coverageId) {
        return null;
    }

    public String getLongitudeCoordinateDapId(String coverageId) {
        return null;
    }

    public String getElevationCoordinateDapId(String coverageId) {
        return null;
    }

    public String getTimeCoordinateDapId(String coverageId) {
        return null;
    }


    public long getLastModified() {

        return _lastModified;
    }

    public void setStopFlag(boolean flag){
        stopWorking = flag;
    }





    public void updateCatalogCache()  throws InterruptedException{

        Thread thread = Thread.currentThread();


        int biffCount = 0;

        if (!stopWorking && !thread.isInterrupted() ) {

            ReentrantReadWriteLock.WriteLock catlock = _catalogLock.writeLock();
            ReentrantReadWriteLock.ReadLock repLock = _repositoryLock.readLock();

            try {
                repLock.lock();
                catlock.lock();
                log.debug("_catalogLock WriteLock Acquired.");

                if (!stopWorking && !thread.isInterrupted()) {

                    coverageIDServer = getCoverageIDServerURL();

                    addSupportedFormats(buildDoc.getRootElement());

                    ingestCatalog();
                    timeOfLastUpdate = new Date().getTime();

                    log.debug("Catalog Cache updated at "+ new Date(timeOfLastUpdate));


                }
            }
            catch (Exception e) {
                log.error("updateCatalog() has a problem: " +
                        e.getMessage() +
                        " biffCount: " + (++biffCount));
                e.printStackTrace();
            }
            finally {
                catlock.unlock();
                repLock.unlock();
                log.debug("_catalogLock WriteLock Released.");
            }

        }

        if(thread.isInterrupted()) {
            log.warn("updateCatalog(): WARNING! Thread "+thread.getName()+" was interrupted!");
            throw new InterruptedException();
        }

    }




    public void updateCatalog()  throws RepositoryException, InterruptedException{

        setupRepository();
        try {
            if (updateRepository())
                updateCatalogCache();
        }
        finally {
            shutdownRepository();
        }
    }


    /**
     * 
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public HashMap<String, Vector<String>> getCoverageIDServerURL() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        TupleQueryResult result = null;
        HashMap<String, Vector<String>> coverageIDServer = new HashMap<String, Vector<String>>();

        String queryString = "SELECT coverageurl,coverageid " +
                "FROM " +
                "{} wcs:CoverageDescription {coverageurl} wcs:Identifier {coverageid} " +
                "USING NAMESPACE " +
                "wcs = <http://www.opengis.net/wcs/1.1#>";
        RepositoryConnection con = owlse2.getConnection();
        log.debug("query coverage ID and server URL: \n" + queryString);
        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, queryString);

        result = tupleQuery.evaluate();
        log.debug("Qresult: " + result.hasNext());
        List<String> bindingNames = result.getBindingNames();
        //log.debug(bindingNames.probeServletContext());
        while (result.hasNext()) {
            BindingSet bindingSet = (BindingSet) result.next();
            // log.debug(bindingSet.probeServletContext());
            Vector<String> coverageURL = new Vector<String>();

            if (bindingSet.getValue("coverageid") != null && bindingSet.getValue("coverageurl") != null) {

                Value valueOfcoverageid = (Value) bindingSet.getValue("coverageid");
                Value valueOfcoverageurl = (Value) bindingSet.getValue("coverageurl");
                coverageURL.addElement(valueOfcoverageurl.stringValue());
                //log.debug("coverageid:");
                //log.debug(valueOfcoverageid.stringValue());
                //log.debug("coverageurl:");
                log.debug(valueOfcoverageurl.stringValue());
                if (coverageIDServer.containsKey(valueOfcoverageid.stringValue()))
                    coverageIDServer.get(valueOfcoverageid.stringValue()).addElement(valueOfcoverageurl.stringValue());
                else
                    coverageIDServer.put(valueOfcoverageid.stringValue(), coverageURL);

            }
        }
        con.close();
        return coverageIDServer;

    }

    public long getCatalogAge() {
        Date now = new Date();
        return now.getTime() - timeOfLastUpdate;
    }



    public boolean updateRepository() throws InterruptedException {

        boolean success = false;
        int biffCount = 0;
        Thread thread = Thread.currentThread();

        if (!stopWorking && !thread.isInterrupted() ) {

            ReentrantReadWriteLock.WriteLock lock = _repositoryLock.writeLock();

            try {
                lock.lock();
                repositoryUpdateActive.set(true);
                log.debug("_repositoryLock WriteLock Acquired.");
                if (!stopWorking && !thread.isInterrupted()) {


                    log.debug("updateRepository(): Updating Semantic Repository.");


                    log.debug("updateRepository(): Connecting to Repository...");
                    RepositoryConnection con = owlse2.getConnection();
                    log.info("updateRepository(): Repository connection has been opened.");
                    if(thread.isInterrupted() || stopWorking ){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }


                    log.debug("updateRepository(): Getting RDF imports.");
                    Vector<String> importURLs = getRdfImports(_config);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }

                    log.debug("updateRepository(): Updating semantic repository.");
                    updateSemanticRepository(con, importURLs);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }

                    String filename = catalogCacheDirectory + "daprepository2";
                    log.debug("updateRepository(): Dumping Semantic Repository to: "+filename);
                    dumpRepository(con, filename);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }
                    /*
                    log.debug("updateRepository(): Processing Contexts...");
                    processContexts(con);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }
                     */

                    log.debug("Extracting CoverageDescriptions from the Repository.");
                    extractCoverageDescrptionsFromRepository(con);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }

                    filename = catalogCacheDirectory + "coverageXMLfromRDF.xml";
                    log.debug("updateRepository(): Dumping CoverageDescriptions Document to: "+filename);
                    dumpCoverageDescriptionsDocument(filename);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }

                    log.info("updateRepository(): Closing repository!");
                    con.close();  //close connection first
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }

                    success = true;
                    repositoryUpdateActive.set(false);

                }
            }
            catch (InterruptedException e){

                throw e;
            }
            catch (Exception e) {
                log.error("updateRepository() has a problem. Error Message: '" +
                        e.getMessage() +
                        "'  biffCount: " + (++biffCount));
            }
            finally {
                lock.unlock();
                log.debug("_repositoryLock.WriteLock Released.");
                if(thread.isInterrupted())
                    throw new InterruptedException();
            }

        }

        if(thread.isInterrupted()){
            log.warn("updateRepository(): WARNING! Thread "+thread.getName()+" was interrupted!");
            throw new InterruptedException();
        }

        log.debug("updateRepository() returning: " + success);
        return success;


    }


    public void run() {

        try {
            log.info("************* STARTING CATALOG UPDATE THREAD.");
            try {
                log.info("************* CATALOG UPDATE THREAD sleeping for " + firstUpdateDelay / 1000.0 + " seconds.");
                Thread.sleep(firstUpdateDelay);

            } catch (InterruptedException e) {
                log.warn("Caught Interrupted Exception.");
                stopWorking = true;
            }

            int updateCounter = 0;
            long startTime, endTime;
            long elapsedTime, sleepTime;
            stopWorking = false;
            Thread thread = Thread.currentThread();

            while (!stopWorking) {

                try {

                    startTime = new Date().getTime();
                    try {
                        updateCatalog();
                    } catch (RepositoryException e) {
                        log.error("Problem using Repository! msg: "+e.getMessage());
                    }
                    endTime = new Date().getTime();
                    elapsedTime = (endTime - startTime);
                    updateCounter++;
                    log.debug("Completed catalog update " + updateCounter + " in " + elapsedTime / 1000.0 + " seconds.");

                    sleepTime = catalogUpdateInterval - elapsedTime;
                    stopWorking = thread.isInterrupted();
                    if (!stopWorking && sleepTime > 0) {
                        log.debug("Catalog Update thread sleeping for " + sleepTime / 1000.0 + " seconds.");
                        Thread.sleep(sleepTime);
                    }

                } catch (InterruptedException e) {
                    log.warn("Caught Interrupted Exception.");
                    stopWorking = true;

                }
            }
        }
        finally {
            destroy();
        }
        log.info("************* EXITING CATALOG UPDATE THREAD.");


    }

    private void  addSupportedFormats(Element coverages) throws MalformedURLException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element coverageDescription;
        Element identifierElem;
        Iterator i;
        String coverageID;
        String msg;
        Vector<String> servers;

        i = coverages.getChildren().iterator();
        while(i.hasNext()){
            coverageDescription  = (Element)i.next();
            identifierElem = coverageDescription.getChild("Identifier",WCS.WCS_NS);
            coverageID = identifierElem.getTextTrim();
            servers = coverageIDServer.get(coverageID);


            Vector<Element> supportedFormats = getWcsSupportedFormatElements(new URL(servers.get(0)));

            coverageDescription.addContent(supportedFormats);

            msg = "Adding supported formats to coverage "+coverageID+ "\n"+
                  "CoverageDescription Element: \n "+xmlo.outputString(coverageDescription)+"\n"+
                  "Coverage "+coverageID+" held at: \n";

            for(String s: servers){
                msg += "    "+s+"\n";
            }
            
            log.debug(msg);
        }





    }

    private Vector<Element> getWcsSupportedFormatElements(URL dapServerUrl){

        Vector<Element> sfEs = new Vector<Element>();
        String[] formats = ServerCapabilities.getSupportedFormatStrings(dapServerUrl);
        Element sf;

        for(String format: formats){
            sf = new Element("SupportedFormat",WCS.WCS_NS);
            sf.setText(format);
            sfEs.add(sf);
        }

        return sfEs;



    }




    private String getServerUrlString(URL url) {

        String baseURL = null;

        String protocol = url.getProtocol();

        if (protocol.equalsIgnoreCase("file")) {
            log.debug("Protocol is FILE.");

        } else if (protocol.equalsIgnoreCase("http")) {
            log.debug("Protocol is HTTP.");

            String host = url.getHost();
            String path = url.getPath();
            int port = url.getPort();

            baseURL = protocol + "://" + host;

            if (port != -1)
                baseURL += ":" + port;
        }

        log.debug("ServerURL: " + baseURL);

        return baseURL;

    }



              

}