package opendap.semantics.IRISail;

import opendap.logging.LogUtil;
import opendap.wcs.v1_1_2.*;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.openrdf.query.*;
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
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;


import com.ontotext.trree.owlim_ext.SailImpl;

/**
 * A colon of LocalFileCatalog
 */
public class NewStaticRDFCatalog implements WcsCatalog, Runnable {


    private Logger log; 


    private AtomicBoolean repositoryUpdateActive;
    private ReentrantReadWriteLock _repositoryLock;
    private XMLfromRDF buildDoc;

    private long _catalogLastModifiedTime;

    private ConcurrentHashMap<String, CoverageDescription> coverages;
    private ReentrantReadWriteLock _catalogLock;

    private Thread catalogUpdateThread;
    private long firstUpdateDelay;
    private long catalogUpdateInterval;
    private long timeOfLastUpdate;


    private boolean stopWorking = false;

    private URL _configFile;


    private String catalogCacheDirectory;
    private String owlim_storage_folder;
    private String resourcePath;
    private boolean backgroundUpdates;
    private boolean overrideBackgroundUpdates;
    private HashMap<String, Vector<String>> coverageIDServer;


    private boolean initialized;


    public NewStaticRDFCatalog() {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        catalogUpdateInterval = 20 * 60 * 1000; // 20 minutes worth of milliseconds
        firstUpdateDelay = 5 * 1000; // 5 seconds worth of milliseconds
        timeOfLastUpdate = 0;
        stopWorking = false;

        _catalogLock = new ReentrantReadWriteLock();
        coverages = new ConcurrentHashMap<String, CoverageDescription>();

        _repositoryLock = new ReentrantReadWriteLock();

        repositoryUpdateActive = new AtomicBoolean();

        repositoryUpdateActive.set(false);

        backgroundUpdates = false;
        overrideBackgroundUpdates = false;

        buildDoc = null;
        _catalogLastModifiedTime = -1;
        _configFile = null;
        catalogCacheDirectory = ".";
        owlim_storage_folder = "owlim-storage";
        resourcePath = ".";

        initialized = false;


    }


    /*******************************************************/
    /*******************************************************/

    /**
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        long startTime, endTime;
        double elapsedTime;

        String workingDir =  System.getProperty("user.dir");
        LogUtil.initLogging(workingDir);
        

        NewStaticRDFCatalog catalog = new NewStaticRDFCatalog();


        try {

            catalog.resourcePath = workingDir;
            catalog.catalogCacheDirectory = workingDir;

            String configFileName;

            configFileName = "file:///data/haibo/workspace/ioos/wcs_service.xml";
            if (args.length > 0)
                configFileName = args[0];


            catalog.log.debug("main() using config file: " + configFileName);

            URL configFileUrl = new URL(configFileName);


            catalog.overrideBackgroundUpdates = true;

            startTime = new Date().getTime();
            catalog.init(configFileUrl, catalog.catalogCacheDirectory, catalog.resourcePath);
            endTime = new Date().getTime();

            elapsedTime = (endTime - startTime) / 1000.0;
            catalog.log.debug("Completed catalog update in " + elapsedTime + " seconds.");
            catalog.log.debug("########################################################################################");
            catalog.log.debug("########################################################################################");
            catalog.log.debug("########################################################################################");


        } catch (Exception e) {
            catalog.log.error("Caught " + e.getClass().getName() + " in main(): "
                    + e.getMessage());
            e.printStackTrace();
        }

    }
    /*******************************************************/



    /**
     * ***************************************************
     */
    public void init(URL configFile, String defaultCacheDirectory, String defaultResourcePath) throws Exception {

        if (initialized)
            return;

        backgroundUpdates = false;

        _configFile = configFile;


        SAXBuilder sb = new SAXBuilder();
        Element configFileRoot = sb.build(configFile).getRootElement();
        Element catalogConfig = configFileRoot.getChild("WcsCatalog");

        processConfig(catalogConfig, defaultCacheDirectory, defaultResourcePath);

        loadWcsCatalogFromRepository();

        if (backgroundUpdates && !overrideBackgroundUpdates) {
            catalogUpdateThread = new Thread(this);
            catalogUpdateThread.start();
        } else {
            updateCatalog();
        }

        initialized = true;
    }




    public void update(){

        try {

            updateCatalog();
        }
        catch(Exception e){
            log.error("update(): FAILED!!! Caught "+e.getClass().getName()+"   Message: "+e.getMessage());
        }
    }
    

    public void updateCatalog() throws RepositoryException, InterruptedException, IOException, JDOMException {

        IRISailRepository repository = setupRepository();
        try {
            log.debug("updateRepository(): Getting starting points (RDF imports).");
            Vector<String> startingPoints = getRdfImports(_configFile);

            log.info("updateCatalog(): Updating Repository...");
            Vector<String> doNotImportTheseUrls = null;
            if (updateRepository(repository, startingPoints, doNotImportTheseUrls)) {
                log.info("updateCatalog(): Extracting CoverageDescriptions from the Repository...");
                extractCoverageDescrptionsFromRepository(repository);

                String filename = catalogCacheDirectory + "coverageXMLfromRDF.xml";
                log.info("updateCatalog(): Dumping CoverageDescriptions Document to: " + filename);
                dumpCoverageDescriptionsDocument(filename);

                log.info("updateCatalog(): Updating catalog cache....");
                updateCatalogCache(repository);

            } else {
                log.info("updateCatalog(): The repository was unchanged, nothing else to do.");
            }
        }
        finally {
            shutdownRepository(repository);
        }

    }

    public boolean updateRepository(IRISailRepository repository, Vector<String> startingPoints, Vector<String> doNotImportTheseUrls) throws RepositoryException, InterruptedException {

        boolean repositoryChanged = RdfPersistence.updateSemanticRepository(repository, startingPoints, doNotImportTheseUrls, resourcePath);

        String filename = catalogCacheDirectory + "owlimHorstRepository.nt";
        log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
        RepositoryUtility.dumpRepository(repository, filename);

        filename = catalogCacheDirectory + "owlimHorstRepository.trig";
        log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
        RepositoryUtility.dumpRepository(repository, filename);


        return repositoryChanged;


    }

    public void loadWcsCatalogFromRepository() throws InterruptedException, RepositoryException {
        long startTime, endTime;
        double elapsedTime;
        log.info("#############################################");
        log.info("#############################################");
        log.info("Loading WCS Catalog from Semantic Repository.");
        startTime = new Date().getTime();
        IRISailRepository repository = setupRepository();
        try {
            extractCoverageDescrptionsFromRepository(repository);
            updateCatalogCache(repository);
        }
        finally {
            shutdownRepository(repository);
        }
        endTime = new Date().getTime();
        elapsedTime = (endTime - startTime) / 1000.0;
        log.info("WCS Catalog loaded from the Semantic Repository. Loaded in " + elapsedTime + " seconds.");
        log.info("#############################################");
        log.info("#############################################");
    }

    public String getDataAccessUrl(String coverageID) {

        return coverageIDServer.get(coverageID).firstElement();

    }


    private void processConfig(Element config, String defaultCacheDirectory, String defaultResourcePath) {

        Element e;
        File file;

        //########################################################
        //  Process Catalog Cache Directory.
        //
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
                if (!catalogCacheDirectory.equals(defaultCacheDirectory)) {
                    file = new File(defaultCacheDirectory);
                    if (!file.exists()) {
                        if (!file.mkdirs()) {
                            log.error("Unable to create cache directory: " + defaultCacheDirectory);
                            log.error("Process probably doomed...");
                        }
                    }
                } else {
                    log.error("Process probably doomed...");
                }

            }
        }
        log.info("Using catalogCacheDirectory: " + catalogCacheDirectory);



        //########################################################
        //  Process Resource Path.
        //
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

        log.info("Using resourcePath: " + resourcePath);



        //########################################################
        //  Process useUpdateCatalogThread
        //
        e = config.getChild("useUpdateCatalogThread");
        if (e != null) {
            backgroundUpdates = true;
            String s = e.getAttributeValue("updateInterval");
            if (s != null) {
                catalogUpdateInterval = Long.parseLong(s) * 1000;
            }
            s = e.getAttributeValue("firstUpdateDelay");
            if (s != null) {
                firstUpdateDelay = Long.parseLong(s) * 1000;
            }

        }
        log.info("backgroundUpdates:       " + backgroundUpdates);
        log.info("Catalog update interval: " + catalogUpdateInterval + "ms");
        log.info("First update delay:     " + firstUpdateDelay + "ms");


    }








    private void shutdownRepository(IRISailRepository repository) throws RepositoryException {

        log.debug("shutdownRepository)(): Shutting down Repository...");
        repository.shutDown();
        log.debug("shutdownRepository(): Repository shutdown complete.");
    }

    private IRISailRepository setupRepository() throws RepositoryException, InterruptedException {


        log.info("Setting up Semantic Repository.");

        //OWLIM Sail Repository (inferencing makes this somewhat slow)
        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        IRISailRepository repository = new IRISailRepository(owlimSail); //owlim inferencing


        log.info("Configuring Semantic Repository.");
        File storageDir = new File(catalogCacheDirectory); //define local copy of repository
        owlimSail.setDataDir(storageDir);
        log.debug("Semantic Repository Data directory set to: " + catalogCacheDirectory);
        // prepare config
        owlimSail.setParameter("storage-folder", owlim_storage_folder);
        log.debug("Semantic Repository 'storage-folder' set to: " + owlim_storage_folder);

        // Choose the operational ruleset
        String ruleset;
        ruleset = "owl-horst";
        //ruleset = "owl-max";

        owlimSail.setParameter("ruleset", ruleset);
        //owlimSail.setParameter("ruleset", "owl-max");
        //owlimSail.setParameter("partialRDFs", "false");
        log.debug("Semantic Repository 'ruleset' set to: " + ruleset);


        log.info("Intializing Semantic Repository.");

        // Initialize repository
        repository.startup(); //needed

        log.info("Semantic Repository Ready.");

        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");


        return repository;

    }


    private void extractCoverageDescrptionsFromRepository(IRISailRepository repository) throws RepositoryException {
        RepositoryConnection con = repository.getConnection();
        log.info("Repository connection has been opened.");

        extractCoverageDescrptionsFromRepository(con);

        log.info("Closing repository connection.");
        con.close();  //close connection first

    }


    private void extractCoverageDescrptionsFromRepository(RepositoryConnection con) {
        //retrieve XML from the RDF store.
        log.info("extractCoverageDescrptionsFromRepository() - Extracting CoverageDescriptions from repository.");
        log.info("extractCoverageDescrptionsFromRepository() - Building CoverageDescription XML from repository.");
        buildDoc = new XMLfromRDF(con, "CoverageDescriptions", "http://www.opengis.net/wcs/1.1#CoverageDescription");
        buildDoc.getXMLfromRDF("http://www.opengis.net/wcs/1.1#CoverageDescription"); //build a JDOM doc by querying against the RDF store

        // Next we update the  cached maps  of datasetUrl/serverIDs and datasetUrl/wcsID
        // held in the CoverageIDGenerator so that subsequent calls to the CoverageIdGenerator
        // create new IDs correctly.

        try {
            log.info("extractCoverageDescrptionsFromRepository() - Updating CoverageIdGenerator Id Caches.");
            HashMap<String, Vector<String>> coverageIdToServerMap = getCoverageIDServerURL(con);
            CoverageIdGenerator.updateIdCaches(coverageIdToServerMap);
        } catch (RepositoryException e) {
            log.error("extractCoverageDescrptionsFromRepository(): Caught RepositoryException. msg: "
                    + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("extractCoverageDescrptionsFromRepository(): Caught MalformedQueryException. msg: "
                    + e.getMessage());
        } catch (QueryEvaluationException e) {

            log.error("extractCoverageDescrptionsFromRepository(): Caught QueryEvaluationException. msg: "
                    + e.getMessage());
        }


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
            FileOutputStream fos = new FileOutputStream(filename);
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
            if (catalogUpdateThread != null) {
                log.debug("destroy() Current thread '" + Thread.currentThread().getName() + "' Interrupting catalogUpdateThread '" + catalogUpdateThread + "'");
                catalogUpdateThread.interrupt();
                log.debug("destroy(): catalogUpdateThread '" + catalogUpdateThread + "' interrupt() called.");
            }

        }
        finally {
            catLock.unlock();
            reposLock.unlock();
            log.debug("destroy(): Released WriteLock for _catalogLock and _repositoryLock.");
            log.debug("destroy(): Complete.");
        }

    }


    private Vector<String> getRdfImports(URL configFile) throws IOException, JDOMException {

        Vector<String> rdfImports = new Vector<String>();
        Element e;
        String s;

        SAXBuilder sb = new SAXBuilder();
        Element configFileRoot = sb.build(configFile).getRootElement();
        Element catalogConfig = configFileRoot.getChild("WcsCatalog");


        /**
         * Load individual dataset references
         */
        Iterator i = catalogConfig.getChildren("dataset").iterator();
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

            log.debug("<wcs:Identifier>" + CoverageIdGenerator.getWcsIdString(datasetURL) + "</wcs:Identifier>");
        }


        /**
         * Load THREDDS Catalog references.
         */
        i = catalogConfig.getChildren("ThreddsCatalog").iterator();
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
        i = catalogConfig.getChildren("RdfImport").iterator();
        while (i.hasNext()) {
            e = (Element) i.next();
            s = e.getTextNormalize();
            rdfImports.add(s);
            log.info("Added reference " + s + " to RDF imports list.");
        }

        return rdfImports;

    }


    private void ingestCatalog(IRISailRepository repository) throws Exception {

        log.info("Ingesting catalog from CoverageDescriptions Document built by the XMLFromRDF object...");

        log.info("Flushing catalog.");
        coverages.clear();


        HashMap<String, String> lmtfc;
        String contextLMT;
        String coverageID;
        Element idElement;
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        long lastModifiedTime;
        String dapVariableID;
        RepositoryConnection con = null;


        List<Element> coverageDescriptions = buildDoc.getDoc().getRootElement().getChildren();

        try {
            con = repository.getConnection();
            lmtfc = RepositoryUtility.getLastModifiedTimesForContexts(con);

            for (Element cde : coverageDescriptions) {

                idElement = cde.getChild("Identifier", WCS.WCS_NS);

                if (idElement != null) {
                    coverageID = idElement.getTextTrim();
                    contextLMT = lmtfc.get(coverageID);

                    String dateTime = contextLMT.substring(0, 10) + " " + contextLMT.substring(11, 19) + " +0000";
                    log.debug("CoverageDescription '" + coverageID + "' has a last modified time of " + dateTime);
                    lastModifiedTime = sdf.parse(dateTime).getTime();
                    CoverageDescription coverageDescription = ingestCoverageDescription(cde, lastModifiedTime);

                    if (_catalogLastModifiedTime < lastModifiedTime)
                        _catalogLastModifiedTime = lastModifiedTime;

                    if (coverageDescription != null) {

                        for (String fieldID : coverageDescription.getFieldIDs()) {
                            log.debug("Getting DAP Coordinate IDs for FieldID: " + fieldID);

                            dapVariableID = getLatitudeCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setLatitudeCoordinateDapId(fieldID, dapVariableID);

                            dapVariableID = getLongitudeCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setLongitudeCoordinateDapId(fieldID, dapVariableID);

                            dapVariableID = getElevationCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setElevationCoordinateDapId(fieldID, dapVariableID);

                            dapVariableID = getTimeCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setTimeCoordinateDapId(fieldID, dapVariableID);
                        }

                    }

                    log.debug("Ingested CoverageDescription '" + coverageID + "'");

                }

            }
        }
        finally {
            if (con != null)
                con.close();
        }

    }


    private CoverageDescription ingestCoverageDescription(Element cde, long lastModified) {

        CoverageDescription cd = null;
        try {
            cd = new CoverageDescription(cde, lastModified);
            coverages.put(cd.getIdentifier(), cd);
            //log.info("Ingested CoverageDescription: " + cd.getIdentifier());
        } catch (WcsException e) {
            XMLOutputter xmlo = new XMLOutputter(Format.getCompactFormat());
            String wcseElem = xmlo.outputString(e.getExceptionElement());
            String cvgDesc = xmlo.outputString(cde);
            log.error("ingestCoverageDescription(): Failed to ingest CoverageDescription!");
            log.error("ingestCoverageDescription(): WcsException: " + wcseElem + "");
            log.error("ingestCoverageDescription(): Here is the XML element that failed to ingest: " + cvgDesc);
        }

        return cd;
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

            if (cd == null)
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

    private String getLatitudeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getLatitudeCoordinateDapId(): Getting the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_latitude", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString);
        //log.debug("getLatitudeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    private String getLongitudeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getLongitudeCoordinateDapId(): Getting the DAP variable ID that represents the longitude coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_longitude", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString);
        //log.debug("getLongitudeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the longitude coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    private String getElevationCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getElevationCoordinateDapId(): Getting the DAP variable ID that represents the elevation coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_elevation", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString);
        //log.debug("getElevationCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the elevation coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    private String getTimeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getTimeCoordinateDapId(): Getting the DAP variable ID that represents the time coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_time", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString);
        //log.debug("getTimeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the time coordinate for FieldID: " + fieldId);
        return coordinateDapId;
    }

    private String runQuery(RepositoryConnection con, String qString) {
        String coordinateDapId = null;
        try {

            TupleQueryResult result = null;

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qString);

            result = tupleQuery.evaluate();

            if (result != null && result.hasNext()) {

                while (result.hasNext()) {

                    BindingSet bindingSet = result.next();

                    Value firstValue = bindingSet.getValue("cidid");

                    coordinateDapId = firstValue.stringValue();
                }
            } else {
                log.error("No query result!");

            }
        } catch (Exception e) {
            log.error("runQuery(): Query FAILED. Caught "+ e.getClass().getName()+ " Message: " + e.getMessage());
        }
        return coordinateDapId;
    }

    private String createCoordinateIdQuery(String coordinateName, String coverageStr, String fieldStr) {

        String qString = "select cid,cidid " +
                "FROM {cover} wcs:Identifier {covid} ; wcs:Range {} wcs:Field " +
                "{field} wcs:Identifier {fieldid}, " +
                "{field} ncobj:hasCoordinate {cid} rdf:type {cfobj:"+coordinateName+"}; dap:localId {cidid} " +
                "WHERE covid= \"" + coverageStr + "\" " +
                "AND fieldid=\"" + fieldStr + "\" " +
                "USING NAMESPACE " +
                "wcs=<http://www.opengis.net/wcs/1.1#>, " +
                "ncobj=<http://iridl.ldeo.columbia.edu/ontologies/netcdf-obj.owl#>, " +
                "cfobj=<http://iridl.ldeo.columbia.edu/ontologies/cf-obj.owl#>, " +
                "dap=<http://xml.opendap.org/ontologies/opendap-dap-3.2.owl#>";


        log.debug("createCoordinateIdQuery: Built query string: '" + qString + "'");
        return qString;
    }

    public long getLastModified() {

        return _catalogLastModifiedTime;
    }

    public void setStopFlag(boolean flag) {
        stopWorking = flag;
    }


    public void updateCatalogCache(IRISailRepository repository) throws InterruptedException {

        Thread thread = Thread.currentThread();


        int biffCount = 0;

        if (!stopWorking && !thread.isInterrupted()) {

            ReentrantReadWriteLock.WriteLock catlock = _catalogLock.writeLock();
            ReentrantReadWriteLock.ReadLock repLock = _repositoryLock.readLock();

            try {
                repLock.lock();
                catlock.lock();
                log.debug("_catalogLock WriteLock Acquired.");

                if (!stopWorking && !thread.isInterrupted()) {

                    coverageIDServer = getCoverageIDServerURL(repository);

                    addSupportedFormats(buildDoc.getRootElement());

                    ingestCatalog(repository);
                    timeOfLastUpdate = new Date().getTime();

                    log.debug("Catalog Cache updated at " + new Date(timeOfLastUpdate));


                }
            }
            catch (Exception e) {
                log.error("updateCatalogCache() has a problem: " +
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

        if (thread.isInterrupted()) {
            log.warn("updateCatalog(): WARNING! Thread " + thread.getName() + " was interrupted!");
            throw new InterruptedException();
        }

    }


    public HashMap<String, Vector<String>> getCoverageIDServerURL(IRISailRepository repo) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        RepositoryConnection con = null;
        HashMap<String, Vector<String>> coverageIDServer;

        try {
            con = repo.getConnection();
            coverageIDServer = getCoverageIDServerURL(con);
            return coverageIDServer;

        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error(e.getClass().getName() + ": Failed to close repository connection. Msg: "
                            + e.getMessage());
                }
            }
        }
    }


    /**
     * @param con
     * @return
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    public HashMap<String, Vector<String>> getCoverageIDServerURL(RepositoryConnection con) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        TupleQueryResult result;
        HashMap<String, Vector<String>> coverageIDServer = new HashMap<String, Vector<String>>();

        String queryString = "SELECT coverageurl,coverageid " +
                "FROM " +
                "{} wcs:CoverageDescription {coverageurl} wcs:Identifier {coverageid} " +
                "USING NAMESPACE " +
                "wcs = <http://www.opengis.net/wcs/1.1#>";


        log.debug("getCoverageIDServerURL() - QueryString (coverage ID and server URL): \n" + queryString);
        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, queryString);

        result = tupleQuery.evaluate();
        log.debug("getCoverageIDServerURL() - Qresult: " + result.hasNext());
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Vector<String> coverageURL = new Vector<String>();

            if (bindingSet.getValue("coverageid") != null && bindingSet.getValue("coverageurl") != null) {

                Value valueOfcoverageid = bindingSet.getValue("coverageid");
                Value valueOfcoverageurl = bindingSet.getValue("coverageurl");
                coverageURL.addElement(valueOfcoverageurl.stringValue());
                log.debug("getCoverageIDServerURL() - coverageid: " + valueOfcoverageid.stringValue());
                log.debug("getCoverageIDServerURL() - coverageurl: " + valueOfcoverageurl.stringValue());
                if (coverageIDServer.containsKey(valueOfcoverageid.stringValue()))
                    coverageIDServer.get(valueOfcoverageid.stringValue()).addElement(valueOfcoverageurl.stringValue());
                else
                    coverageIDServer.put(valueOfcoverageid.stringValue(), coverageURL);

            }
        }
        return coverageIDServer;

    }


    /*
    public boolean updateRepository_OLD(Vector<String> startingPoints) throws InterruptedException {

        boolean success = false;
        
        int biffCount = 0;
        Thread thread = Thread.currentThread();
        
        RdfPersistence updateRepository = new RdfPersistence(); 
        
        try {
            updateRepository.updateSemanticRepository(owlse2,startingPoints);
            
         //##########################################################################
         //  Dump repository to disk as N-Triples
         log.debug("updateRepository2(): Connecting to Repository...");
         RepositoryConnection con = owlse2.getConnection();
         String filename = catalogCacheDirectory + "daprepository.nt";
         log.debug("updateRepository2(): Dumping Semantic Repository to: "+filename);
         RepositoryUtility.dumpRepository(con, filename);
         if(thread.isInterrupted() || stopWorking){
             log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
             throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
         }

         //#########################################################################
         //Dump repository to disk as Triples with their contexts.
         log.debug("updateRepository2(): Dumping Semantic Repository to: "+filename);
         filename = catalogCacheDirectory + "daprepository.trig";
         RepositoryUtility.dumpRepository(con, filename);
         if(thread.isInterrupted() || stopWorking){
             log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
         }


         
         log.debug("updateRepository2(): Extracting CoverageDescriptions from the Repository.");
         extractCoverageDescrptionsFromRepository(con);
         if(thread.isInterrupted() || stopWorking){
             log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
             throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
         }

         filename = catalogCacheDirectory + "coverageXMLfromRDF.xml";
         log.debug("updateRepository2(): Dumping CoverageDescriptions Document to: "+filename);
         dumpCoverageDescriptionsDocument(filename);
         if(thread.isInterrupted() || stopWorking){
             log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
             throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
         }

         log.info("updateRepository2(): Closing repository!");
         con.close();  //close connection first
         if(thread.isInterrupted() || stopWorking){
             log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
             throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
         }

        } catch (InterruptedException e) {
            log.error("Thread interrupted "+ e.getMessage());
        } catch (RepositoryException e) {
            log.error(" updateRepository(Vector<String> startingPoints) caught RepositoryException"+ e.getMessage());
        }
        return success;
    }
    */


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
                    } catch (Exception e) {
                        log.error("Catalog Update FAILED!!! Caught "+e.getClass().getName()+"  Message: " + e.getMessage());
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

    private void addSupportedFormats(Element coverages) throws MalformedURLException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element coverageDescription;
        Element identifierElem;
        Iterator i;
        String coverageID;
        String msg;
        Vector<String> servers;

        i = coverages.getChildren().iterator();
        while (i.hasNext()) {
            coverageDescription = (Element) i.next();
            identifierElem = coverageDescription.getChild("Identifier", WCS.WCS_NS);

            if (identifierElem != null) {
                coverageID = identifierElem.getTextTrim();
                servers = coverageIDServer.get(coverageID);


                Vector<Element> supportedFormats = getWcsSupportedFormatElements(new URL(servers.get(0)));

                coverageDescription.addContent(supportedFormats);

                msg = "Adding supported formats to coverage " + coverageID  +
                        //"CoverageDescription Element: \n " + xmlo.outputString(coverageDescription) + "\n" +
                        "Coverage  held at: \n";

                for (String s : servers) {
                    msg += "    " + s + "\n";
                }

                log.debug(msg);
            } else {
                log.error("addSupportedFormats() - Failed to locate wcs:Identifier element for Coverage!");
                //@todo Throw an exception (what kind??) here!!
            }
        }


    }

    private Vector<Element> getWcsSupportedFormatElements(URL dapServerUrl) {

        Vector<Element> sfEs = new Vector<Element>();
        String[] formats = ServerCapabilities.getSupportedFormatStrings(dapServerUrl);
        Element sf;

        for (String format : formats) {
            sf = new Element("SupportedFormat", WCS.WCS_NS);
            sf.setText(format);
            sfEs.add(sf);
        }

        return sfEs;


    }


    private void checkExecutionState() throws InterruptedException {

        Thread thread = Thread.currentThread();

        boolean isInterrupted = thread.isInterrupted();

        if (isInterrupted || stopWorking) {
            log.warn("updateRepository2(): WARNING! Thread " + thread.getName() + " was interrupted!");
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned '" + isInterrupted + "'. stopWorking='" + stopWorking + "'");
        }

    }

}