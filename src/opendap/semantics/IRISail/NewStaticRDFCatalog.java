/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2010 OPeNDAP, Inc.
//
// Authors:
//     Nathan David Potter  <ndp@opendap.org>
//     Haibo Liu  <haibo@iri.columbia.edu>
//     M. Benno Blumenthal <benno@iri.columbia.edu>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.semantics.IRISail;

import opendap.logging.LogUtil;
import opendap.wcs.v1_1_2.*;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.slf4j.Logger;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
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
 * This class is used to retrieve coverage descriptions of a data set. A Sesame-OWLIM RDF
 * store is initialized and populated and an xml document of coverages of the served data sets
 * is generated. This xml document is used to answer request posted by a client.
 * The RDF store is updated periodically to make sure the information served is up to date.
 * Care is taken to make sure the RDF store can be used by multiple users at the same time.
 * </p>
 * The RDF store is populated by importing and ingesting semantic inference rules and owl/xml
 * schema files and rdf files describing data sets.
 * The <code>doNotImportTheseUrls</code> is a string vector that holds
 * URL of bad files that should not be imported.
 * The <code>catalogCacheDirectory</code> holds the OWLIM persistent directory
 * <code>owlim_storage_folder</code>, the coverage description document and the dump of the
 * whole repository files in triple graph format.
 * 
 *
 */
public class NewStaticRDFCatalog implements WcsCatalog, Runnable {


    private Logger log;


    private ReentrantReadWriteLock _repositoryLock;
    private XMLfromRDF buildDoc;

    private long _catalogLastModifiedTime;

    private ReentrantReadWriteLock _catalogLock; // Protects coverages.
    private HashMap<String, Vector<String>> coverageIDServer;
    private ConcurrentHashMap<String, CoverageDescription> coverages;

    private Thread catalogUpdateThread;
    private long firstUpdateDelay;
    private long catalogUpdateInterval;
    private long timeOfLastUpdate;



    private URL _configFile;


    private String catalogCacheDirectory;
    private String owlim_storage_folder;
    private String resourcePath;
    private boolean backgroundUpdates;
    private boolean overrideBackgroundUpdates;

    private Vector<String> doNotImportTheseUrls;


    private boolean initialized;


    public NewStaticRDFCatalog() {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        catalogUpdateInterval = 20 * 60 * 1000; // 20 minutes worth of milliseconds
        firstUpdateDelay = 5 * 1000; // 5 seconds worth of milliseconds
        timeOfLastUpdate = 0;

        _catalogLock = new ReentrantReadWriteLock();
        coverages = new ConcurrentHashMap<String, CoverageDescription>();

        _repositoryLock = new ReentrantReadWriteLock();

        backgroundUpdates = false;
        overrideBackgroundUpdates = false;

        buildDoc = null;
        _catalogLastModifiedTime = -1;
        _configFile = null;
        catalogCacheDirectory = ".";
        owlim_storage_folder = "owlim-storage";
        resourcePath = ".";

        doNotImportTheseUrls = new Vector<String>();

        initialized = false;


    }

    /**
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        long startTime, endTime;
        double elapsedTime;

        String workingDir = System.getProperty("user.dir");
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
     * 
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
            update();
        }

        initialized = true;
    }

    /**
     * Update the repository, coverage description document, catalog and the repository dump file.
     * @throws RepositoryException
     * @throws InterruptedException
     * @throws IOException
     * @throws JDOMException
     */
    public void update() throws RepositoryException, InterruptedException, IOException, JDOMException {

        String filename;
        boolean repositoryChanged;
        Lock repositoryWriteLock = _repositoryLock.writeLock();

        Repository repository = setupRepository();

        try {


            repositoryWriteLock.lock();

            log.debug("updateRepository(): Getting starting points (RDF imports).");
            Vector<String> startingPoints = getRdfImports(_configFile);

            log.info("updateCatalog(): Updating Repository...");
            repositoryChanged = RepositoryOps.updateSemanticRepository(repository, startingPoints, doNotImportTheseUrls, resourcePath);

            filename = catalogCacheDirectory + "owlimHorstRepository.trig";
            log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
            RepositoryOps.dumpRepository(repository, filename);


            if (repositoryChanged) {
                log.info("updateCatalog(): Repository has been changed!");

                log.info("updateCatalog(): Extracting CoverageDescriptions from the Repository...");
                extractCoverageDescrptionsFromRepository(repository);

                filename = catalogCacheDirectory + "coverageXMLfromRDF.xml";
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
            repositoryWriteLock.unlock();

        }

    }

    /**
     * Extract coverage description from the RDF store and update catalog cache.
     * @throws InterruptedException
     * @throws RepositoryException
     */
    private void loadWcsCatalogFromRepository() throws InterruptedException, RepositoryException {
        long startTime, endTime;
        double elapsedTime;
        log.info("#############################################");
        log.info("#############################################");
        log.info("Loading WCS Catalog from Semantic Repository.");
        startTime = new Date().getTime();
        Repository repository = setupRepository();
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

    /**
     * Process the configration file passed in as the root element of an xml file.
     * Set the catalog cache directory, resource path and the interval of running
     * updating catalog thread.
     *  
     * @param config
     * @param defaultCacheDirectory
     * @param defaultResourcePath
     */
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


    private void shutdownRepository(Repository repository) throws RepositoryException {

        log.debug("shutdownRepository)(): Shutting down Repository...");
        repository.shutDown();
        log.debug("shutdownRepository(): Repository shutdown complete.");
    }

    /**
     * Setup and initialize the repository. Set the OWLIM rule set, catalog cache directory and
     * owlim storage directory.
     * @return repository-a reference to the repository
     * @throws RepositoryException
     * @throws InterruptedException
     */
    private Repository setupRepository() throws RepositoryException, InterruptedException {


        log.info("Setting up Semantic Repository.");

        //OWLIM Sail Repository (inferencing makes this somewhat slow)
        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        Repository repository = new IRISailRepository(owlimSail); //owlim inferencing


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
        repository.initialize(); //needed

        log.info("Semantic Repository Ready.");


        ProcessingState.checkState();


        return repository;

    }


    private void extractCoverageDescrptionsFromRepository(Repository repository) throws RepositoryException, InterruptedException {
        RepositoryConnection con = null;



        try {
            con = repository.getConnection();
            log.info("Repository connection has been opened.");

            extractCoverageDescrptionsFromRepository(con);

        }
        finally {
            if (con != null) {
                try {
                    log.info("Closing repository connection.");
                    con.close();
                }
                catch (Exception e) {
                    log.error("Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                }
            }
        }



    }

    /**
     * Instantiates XMLfromRDF and build CoverageDescription XML from the repository.
     * @param con-connection to the repository
     * @throws InterruptedException
     */
    private void extractCoverageDescrptionsFromRepository(RepositoryConnection con) throws InterruptedException {


        ProcessingState.checkState();
        try {
            //retrieve XML from the RDF store.
            log.info("extractCoverageDescrptionsFromRepository() - Extracting CoverageDescriptions from repository.");
            log.info("extractCoverageDescrptionsFromRepository() - Building CoverageDescription XML from repository.");
            buildDoc = new XMLfromRDF(con, "CoverageDescriptions", "http://www.opengis.net/wcs/1.1#CoverageDescription");
            buildDoc.getXMLfromRDF("http://www.opengis.net/wcs/1.1#CoverageDescription"); //build a JDOM doc by querying against the RDF store

            // Next we update the  cached maps  of datasetUrl/serverIDs and datasetUrl/wcsID
            // held in the CoverageIDGenerator so that subsequent calls to the CoverageIdGenerator
            // create new IDs correctly.
            log.info("extractCoverageDescrptionsFromRepository() - Updating CoverageIdGenerator Id Caches.");
            HashMap<String, Vector<String>> coverageIdToServerMap = getCoverageIDServerURL(con);
            CoverageIdGenerator.updateIdCaches(coverageIdToServerMap);
        } catch (Exception e) {
            log.error("extractCoverageDescrptionsFromRepository():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        }
        ProcessingState.checkState();


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
            ProcessingState.stopProcessing();
            if (catalogUpdateThread != null) {
                log.debug("destroy() Current thread '" + Thread.currentThread().getName() + "' Interrupting catalogUpdateThread '" + catalogUpdateThread + "'");
                catalogUpdateThread.interrupt();
                log.debug("destroy(): catalogUpdateThread '" + catalogUpdateThread + "' interrupt() called.");
            }
            catLock.lock();
            reposLock.lock();
            log.debug("destroy(): WriteLocks Aquired.");

        }
        finally {
            catLock.unlock();
            reposLock.unlock();
            log.debug("destroy(): Released WriteLock for _catalogLock and _repositoryLock.");
            log.debug("destroy(): Complete.");
        }

    }

    /**
     * Retrieve the data set list and the import RDF files from the config file.
     * @param configFile
     * @return a String vector holding list of files to import
     * @throws IOException
     * @throws JDOMException
     */
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

    /**
     * Ingest coverage description document into the WCS catalog.
     * @param repository
     * @throws Exception
     */
    private void ingestWcsCatalog(Repository repository) throws Exception {

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
            lmtfc = RepositoryOps.getLastModifiedTimesForContexts(con);

            for (Element cde : coverageDescriptions) {

                idElement = cde.getChild("Identifier", WCS.WCS_NS);

                if (idElement != null) {
                    coverageID = idElement.getTextTrim();
                    contextLMT = lmtfc.get(coverageID);

                    String dateTime = contextLMT.substring(0, 10) + " " + contextLMT.substring(11, 19) + " +0000";
                    log.debug("CoverageDescription '" + coverageID + "' has a last modified time of " + dateTime);
                    lastModifiedTime = sdf.parse(dateTime).getTime();
                    CoverageDescription coverageDescription = ingestWcsCoverageDescription(cde, lastModifiedTime);

                    if (_catalogLastModifiedTime < lastModifiedTime)
                        _catalogLastModifiedTime = lastModifiedTime;

                    if (coverageDescription != null) {

                        for (String fieldID : coverageDescription.getFieldIDs()) {
                            log.debug("Getting DAP Coordinate IDs for FieldID: " + fieldID);

                            dapVariableID = getDapGridId(con, coverageID, fieldID);
                            coverageDescription.setDapGridId(fieldID, dapVariableID);

                            dapVariableID = getLatitudeCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setLatitudeCoordinateDapId(fieldID, dapVariableID);

                            dapVariableID = getLongitudeCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setLongitudeCoordinateDapId(fieldID, dapVariableID);

                            dapVariableID = getElevationCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setElevationCoordinateDapId(fieldID, dapVariableID);

                            dapVariableID = getTimeCoordinateDapId(con, coverageID, fieldID);
                            coverageDescription.setTimeCoordinateDapId(fieldID, dapVariableID);

                            String timeUnits = getTimeUnits(con, coverageID, fieldID);
                            coverageDescription.setTimeUnits(fieldID, timeUnits);
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

    /**
     * Ingest individual coverage description element into the WCS catalog.
     * @param cde-coverage description element
     * @param lastModified
     * @return
     */
    private CoverageDescription ingestWcsCoverageDescription(Element cde, long lastModified) {

        CoverageDescription cd = null;
        try {
            cd = new CoverageDescription(cde, lastModified);
            coverages.put(cd.getIdentifier(), cd);
            //log.info("Ingested CoverageDescription: " + cd.getIdentifier());
        } catch (WcsException e) {
            XMLOutputter xmlo = new XMLOutputter(Format.getCompactFormat());
            String wcseElem = xmlo.outputString(e.getExceptionElement());
            String cvgDesc = xmlo.outputString(cde);
            log.error("ingestWcsCoverageDescription(): Failed to ingest CoverageDescription!");
            log.error("ingestWcsCoverageDescription(): WcsException: " + wcseElem + "");
            log.error("ingestWcsCoverageDescription(): Here is the XML element that failed to ingest: " + cvgDesc);
        }

        return cd;
    }

    /**
     * Check if the coverage exists.
     * @param id-coverage
     * @return
     */
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

        Lock catalogReadlock = _catalogLock.readLock();
        try {
            catalogReadlock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            CoverageDescription cd = coverages.get(id);

            if (cd == null)
                return null;

            return cd.getElement();
        }
        finally {
            catalogReadlock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }
    }

    public List<Element> getCoverageDescriptionElements() throws WcsException {
        throw new WcsException("getCoverageDescriptionElements() method Not Implemented", WcsException.NO_APPLICABLE_CODE);
    }


    public CoverageDescription getCoverageDescription(String id) {
        Lock lock = _catalogLock.readLock();
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

        Lock lock = _catalogLock.readLock();
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


        Lock lock = _catalogLock.readLock();

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


        Lock lock = _catalogLock.readLock();

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


        Lock lock = _catalogLock.readLock();

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

    /**
     * Get the Dap Id of the dap Grid variable associated with the passed WCS filed ID.
     * @param con-connection to the repository
     * @param coverageId
     * @param fieldId
     * @return coordinateDapI-latitude coordinate Dap Id
     */
    private String getDapGridId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getLatitudeCoordinateDapId(): Getting the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        String qString = createDapGridIdQuery(coverageId, fieldId);
        String dapGridId = runQuery(con, qString, "gridid");
        log.debug("getDapGridId(): '" + dapGridId + "' is the DAP variable Grid ID that represents the latitude coordinate for FieldID: " + fieldId);
        return dapGridId;

    }

     private String createDapGridIdQuery(String coverageStr, String fieldStr) {

        String qString = "select grid, gridid " +
                "FROM {cover} wcs:Identifier {covid} ; wcs:Range {} wcs:Field " +
                "{field} wcs:Identifier {fieldid}, " +
                "{grid} cf2wcs:hasPart {field} ; rdf:type {dapns:Grid} ; dap:localId {gridid} " +
                "WHERE covid= \"" + coverageStr + "\" " +
                "AND fieldid=\"" + fieldStr + "\" " +
                "UNION " +
                "select field, gridid " +
                "FROM " +
                "{cover} wcs:Identifier {covid} ; wcs:Range {} wcs:Field " +
                "{field} wcs:Identifier {fieldid} ; rdf:type {dapns:Grid} ; dap:localId {gridid} " +
                "WHERE covid= \"" + coverageStr + "\" " +
                "AND fieldid=\"" + fieldStr + "\" " +
                "USING NAMESPACE " +
                "wcs=<http://www.opengis.net/wcs/1.1#>, " +
                "ncobj=<http://iridl.ldeo.columbia.edu/ontologies/netcdf-obj.owl#>, " +
                "cfobj=<http://iridl.ldeo.columbia.edu/ontologies/cf-obj.owl#>, " +
                "cf2wcs=<http://iridl.ldeo.columbia.edu/ontologies/cf2wcs.owl#>, " +
                "dapns=<http://xml.opendap.org/ns/DAP/3.2#>, " +
                "dap=<http://xml.opendap.org/ontologies/opendap-dap-3.2.owl#>";

        log.debug("createDapGridIdQuery: Built query string: '" + qString + "'");
        return qString;
    }
    /**
     * Get latitude coordinate Dap ID, given the coverage ID and field ID.
     * @param con-connection to the repository
     * @param coverageId
     * @param fieldId
     * @return coordinateDapI-latitude coordinate Dap Id
     */
    private String getLatitudeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getLatitudeCoordinateDapId(): Getting the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_latitude", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getLatitudeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    /**
     * Get longitude coordonate Dap ID, given the coverage ID and field ID.
     * @param con-connection to the repository
     * @param coverageId
     * @param fieldId
     * @return coordinateDapI-longitude coordinate Dap Id
     */
    private String getLongitudeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getLongitudeCoordinateDapId(): Getting the DAP variable ID that represents the longitude coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_longitude", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getLongitudeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the longitude coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    /**
     * Get elevation coordinate ID, given the coverage ID and field ID.
     * @param con-connection to the repository
     * @param coverageId
     * @param fieldId
     * @return coordinateDapI-elevation coordinate Dap Id
     */
    private String getElevationCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getElevationCoordinateDapId(): Getting the DAP variable ID that represents the elevation coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_elevation", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getElevationCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the elevation coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    /**
     * Get time coordinate ID, given the coverage ID and field ID.
     * @param con-connection to the repository
     * @param coverageId
     * @param fieldId
     * @return coordinateDapI-time coordinate Dap Id
     */
    private String getTimeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) {
        //log.debug("getTimeCoordinateDapId(): Getting the DAP variable ID that represents the time coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_time", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getTimeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the time coordinate for FieldID: " + fieldId);
        return coordinateDapId;
    }

    /**
     * Get time units, given the coverage ID and field ID.
     * @param con-connection to the repository
     * @param coverageId
     * @param fieldId
     * @return coordinateUnit-time unit
     */
    private String getTimeUnits(RepositoryConnection con, String coverageId, String fieldId) {
        String qString = createCoordinateUnitsQuery("A_time", coverageId, fieldId);
        String coordinateUnit = runQuery(con, qString, "unit");
        log.debug("getTimeUnits(): '" + coordinateUnit + "' is the units of the time coordinate for FieldID: " + fieldId);
        return coordinateUnit;
    }

    /**
     * Evaluate the query string for getting coordinate Dap ID and units.
     * @param con-connection to the repository
     * @param qString-query string
     * @param cidid-the search key used in the query string
     * @return the coordinate dap ID or coordinate units
     */
    private String runQuery(RepositoryConnection con, String qString, String cidid) {
        String coordinateDapId = null;
        try {

            TupleQueryResult result = null;

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qString);

            result = tupleQuery.evaluate();

            if (result != null && result.hasNext()) {

                while (result.hasNext()) {

                    BindingSet bindingSet = result.next();

                    Value firstValue = bindingSet.getValue(cidid);

                    coordinateDapId = firstValue.stringValue();
                }
            } else {
                log.error("No query result!");

            }
        } catch (Exception e) {
            log.error("runQuery(): Query FAILED. Caught " + e.getClass().getName() + " Message: " + e.getMessage());
            log.error("query is: "+ qString);
        }
        return coordinateDapId;
    }

    /**
     * Create the query string, given the coverage and field.
     * @param coordinateName-A_latitude/longitude/elevation/time
     * @param coverageStr
     * @param fieldStr
     * @return   query string
     */
    private String createCoordinateIdQuery(String coordinateName, String coverageStr, String fieldStr) {

        String qString = "select cid,cidid " +
                "FROM {cover} wcs:Identifier {covid} ; wcs:Range {} wcs:Field " +
                "{field} wcs:Identifier {fieldid}, " +
                "{field} ncobj:hasCoordinate {cid} rdf:type {cfobj:" + coordinateName + "}; dap:localId {cidid} " +
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

    /**
     * Create the query string for coordinate units, given the coverage and field.
     * @param coordinateName
     * @param coverageStr
     * @param fieldStr
     * @return
     */
    private String createCoordinateUnitsQuery(String coordinateName, String coverageStr, String fieldStr) {

        String qString = "select cid,unit " +
                "FROM {cover} wcs:Identifier {covid} ; wcs:Range {} wcs:Field " +
                "{field} wcs:Identifier {fieldid}, " +
                "{field} ncobj:hasCoordinate {cid} rdf:type {cfobj:" + coordinateName + "}; cfatt:units {unit} " +
                "WHERE covid= \"" + coverageStr + "\" " +
                "AND fieldid=\"" + fieldStr + "\" " +
                "USING NAMESPACE " +
                "wcs=<http://www.opengis.net/wcs/1.1#>, " +
                "ncobj=<http://iridl.ldeo.columbia.edu/ontologies/netcdf-obj.owl#>, " +
                "cfobj=<http://iridl.ldeo.columbia.edu/ontologies/cf-obj.owl#>, " +
                "cfatt=<http://iridl.ldeo.columbia.edu/ontologies/cf-att.owl#>, "+
                "dap=<http://xml.opendap.org/ontologies/opendap-dap-3.2.owl#>";
        
        log.debug("createTimeUnitsQuery: Built query string: '" + qString + "'");
        return qString;
    }

    /**
     * Get last modified time.
     * @return
     */
    public long getLastModified() {

        return _catalogLastModifiedTime;
    }

    /**
     * Update catalog cache from the repository by calling <code>ingestWcsCatalog(repository)</code>
     * @param repository
     * @throws InterruptedException
     */
    private void updateCatalogCache(Repository repository) throws InterruptedException {



        int biffCount = 0;

        ProcessingState.checkState();

        Lock catalogWriteLock = _catalogLock.writeLock();

        try {
            catalogWriteLock.lock();

            log.debug("_catalogLock WriteLock Acquired.");

            ProcessingState.checkState();

            coverageIDServer = getCoverageIDServerURL(repository);

            addSupportedFormats(buildDoc.getRootElement());

            ingestWcsCatalog(repository);
            timeOfLastUpdate = new Date().getTime();


            log.debug("Catalog Cache updated at " + new Date(timeOfLastUpdate));



        }
        catch (Exception e) {
            log.error("updateCatalogCache() has a problem: " +
                    e.getMessage() +
                    " biffCount: " + (++biffCount));
            e.printStackTrace();
        }
        finally {
            catalogWriteLock.unlock();
            log.debug("_catalogLock WriteLock Released.");
        }

        ProcessingState.checkState();



    }

    /**
     * Get coverage ID and server URL from the repository.
     * @param repo-repository
     * @return coverageIDServer-hashmap of coverage ID and list of URLs of servers
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */

    private HashMap<String, Vector<String>> getCoverageIDServerURL(Repository repo) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
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
     * Get coverage ID and server URL through the connection to a repository.
     * @param con-connection to a repository
     * @return hashmap of coverage ID and list of URLs of servers
     * @throws RepositoryException
     * @throws MalformedQueryException
     * @throws QueryEvaluationException
     */
    private HashMap<String, Vector<String>> getCoverageIDServerURL(RepositoryConnection con) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
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


    /**
     * Run <code>update()</code> which updates the repository and catalog cache.
     */
    public void run() {

        try {
            ProcessingState.enableProcessing();
            int updateCounter = 0;
            long startTime, endTime;
            long elapsedTime, sleepTime;
            Thread thread = Thread.currentThread();


            log.info("************* STARTING CATALOG UPDATE THREAD.");
            try {
                log.info("************* CATALOG UPDATE THREAD sleeping for " + firstUpdateDelay / 1000.0 + " seconds.");
                Thread.sleep(firstUpdateDelay);

            } catch (InterruptedException e) {
                log.warn("Caught Interrupted Exception.");
                ProcessingState.stopProcessing();
            }


            while (ProcessingState.continueProcessing()) {

                try {

                    startTime = new Date().getTime();
                    try {
                        update();
                    } catch (Exception e) {
                        log.error("Catalog Update FAILED!!! Caught " + e.getClass().getName() + "  Message: " + e.getMessage());
                    }
                    endTime = new Date().getTime();
                    elapsedTime = (endTime - startTime);
                    updateCounter++;
                    log.debug("Completed catalog update " + updateCounter + " in " + elapsedTime / 1000.0 + " seconds.");

                    sleepTime = catalogUpdateInterval - elapsedTime;
                    if (ProcessingState.continueProcessing() && sleepTime > 0) {
                        log.debug("Catalog Update thread sleeping for " + sleepTime / 1000.0 + " seconds.");
                        Thread.sleep(sleepTime);
                    }

                } catch (InterruptedException e) {
                    log.warn("Caught Interrupted Exception.");
                    ProcessingState.stopProcessing();

                }
            }
        }
        finally {
            destroy();
        }
        log.info("************* EXITING CATALOG UPDATE THREAD.");


    }

    /**
     * Ingest supported format in the coverage document.
     * @param coverages
     * @throws MalformedURLException
     */
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

                msg = "Adding supported formats to coverage " + coverageID +
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

    /**
     * Get supported formats from the dap server.
     * @param dapServerUrl
     * @return
     */
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




}