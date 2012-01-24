/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

package opendap.semantics.wcs;

import opendap.coreServlet.Scrub;
import opendap.logging.LogUtil;
import opendap.semantics.IRISail.*;
import opendap.threddsHandler.ThreddsCatalogUtil;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParseException;


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
public class StaticRdfCatalog implements WcsCatalog, Runnable {


    private Logger log;


    private ReentrantReadWriteLock _repositoryLock;
    private XMLfromRDF buildDoc;

    private long _catalogLastModifiedTime;

    private ReentrantReadWriteLock _catalogLock; // Protects coverages.
    private HashMap<String, Vector<String>> coverageIDServer;
    //private ConcurrentHashMap<String, CoverageDescription> coverages;

    private ConcurrentSkipListMap<String, CoverageDescription> coverages;



    private Thread catalogUpdateThread;
    private long firstUpdateDelay;
    private long catalogUpdateInterval;
    private long timeOfLastUpdate;



    private URL _configFile;
    private String _semanticPreloadFile;


    private String catalogCacheDirectory;
    private String owlim_storage_folder;
    private String resourcePath;
    private boolean backgroundUpdates;
    private boolean overrideBackgroundUpdates;

    private Vector<String> doNotImportTheseUrls;


    private boolean initialized;


    public StaticRdfCatalog() {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        catalogUpdateInterval = 20 * 60 * 1000; // 20 minutes worth of milliseconds
        firstUpdateDelay = 5 * 1000; // 5 seconds worth of milliseconds
        timeOfLastUpdate = 0;

        _catalogLock = new ReentrantReadWriteLock();
        coverages = new ConcurrentSkipListMap<String, CoverageDescription>();

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
        if (!workingDir.endsWith("/")){
            workingDir =  workingDir+"/";
        }
        LogUtil.initLogging(workingDir);


        StaticRdfCatalog catalog = new StaticRdfCatalog();
        String semanticPreloadFile = "http://xml.opendap.org/semantics/Dap_To_Wcs-1.1.owl";


        try {

            catalog.resourcePath = workingDir;
            catalog.catalogCacheDirectory = workingDir + "repositorycache/";

            String configFileName;

            configFileName = "file:///data/haibo/workspace/ioos/wcs_service.xml";
            if (args.length > 0)
                configFileName = args[0];


            catalog.log.debug("main() using config file: " + configFileName);

            URL configFileUrl = new URL(configFileName);


            catalog.overrideBackgroundUpdates = true;

            startTime = new Date().getTime();

            
           // catalog.init(configFileUrl, catalog.catalogCacheDirectory, catalog.resourcePath);

            catalog.init(configFileUrl, semanticPreloadFile, catalog.resourcePath,catalog.catalogCacheDirectory);

            
            endTime = new Date().getTime();

            elapsedTime = (endTime - startTime) / 1000.0;
            catalog.log.debug("Completed catalog update in " + elapsedTime + " seconds.");
            catalog.log.debug("########################################################################################");
            catalog.log.debug("########################################################################################");
            catalog.log.debug("########################################################################################");


        } catch (MalformedURLException e) {
            catalog.log.error("init(): Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
        } catch (InterruptedException e) {
            catalog.log.error("init(): Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
        }

    }


    /**
     *
     * @param configFile
     * @param defaultResourcePath
     * @param defaultCacheDirectory
     * @throws Exception
     */
     public void init(URL configFile, String defaultResourcePath, String defaultCacheDirectory) throws InterruptedException {
        init( configFile,  null, defaultResourcePath,  defaultCacheDirectory);

    }


    /**
     *
     * @param configFile
     * @param semanticPreloadFile
     * @param defaultResourcePath
     * @param defaultCacheDirectory
     * @throws Exception
     */
    public void init(URL configFile, String semanticPreloadFile, String defaultResourcePath, String defaultCacheDirectory) throws InterruptedException {

        if (initialized)
            return;

        backgroundUpdates = false;

        _configFile = configFile;
        _semanticPreloadFile = semanticPreloadFile;

        try {

            SAXBuilder sb = new SAXBuilder();
            Element configFileRoot = sb.build(configFile).getRootElement();
            Element catalogConfig = configFileRoot.getChild("WcsCatalog");


            processConfig(catalogConfig, defaultCacheDirectory, defaultResourcePath);

            loadWcsCatalogFromRepository();

            if (backgroundUpdates && !overrideBackgroundUpdates) {
                catalogUpdateThread = new Thread(this);
                catalogUpdateThread.setName("CatalogUpdate"+catalogUpdateThread.getName());
                catalogUpdateThread.start();
            } else {
                update();
            }

        } catch (IOException e) {
            log.error("init(): Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
        } catch (RepositoryException e) {
            log.error("init(): Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
        } catch (JDOMException e) {
            log.error("init(): Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
        } catch (RDFParseException e) {
            log.error("init(): Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
        }

        initialized = true;
    }
    


    /**
     * Update the repository, coverage description document, catalog and the repository dump file.
     * @throws RepositoryException
     * @throws InterruptedException
     * @throws IOException
     * @throws JDOMException
     * @throws RDFParseException 
     */
    public void update() throws RepositoryException, InterruptedException, IOException, JDOMException, RDFParseException {

        String filename;
        boolean repositoryChanged;
        Lock repositoryWriteLock = _repositoryLock.writeLock();

        Repository repository = setupRepository();

        try {


            repositoryWriteLock.lock();


            ProcessController.setProcessingState(true);

            log.debug("updateRepository(): Getting starting points (RDF imports).");
            Vector<String> startingPoints = getRdfImports(_configFile);
            
                        
            if(_semanticPreloadFile!=null)
                startingPoints.add(_semanticPreloadFile);

            log.info("update(): Updating Repository...");
            String loadfromtrig = null; //load repository from a trig file
            repositoryChanged = RepositoryOps.updateSemanticRepository(repository, startingPoints, doNotImportTheseUrls, 
                    resourcePath, catalogCacheDirectory, loadfromtrig);
            if (repositoryChanged) {
                String workingDir = "./";
                File memoryStore = new File(workingDir + "_MemoryStore_");
                if(memoryStore.exists()){ //assume _MemoryStore_ is generated by dropping with memory store
                repository.shutDown();
                File repoPath = new File(catalogCacheDirectory);
                RepositoryOps.deleteDirectory(repoPath);
                repository = RepositoryOps.setupOwlimSailRepository(catalogCacheDirectory, loadfromtrig);
                RepositoryOps.loadFromMem(repository);
                RepositoryOps.deleteDirectory(memoryStore);
                }
                RepositoryOps.updateExternalInference(repository, doNotImportTheseUrls, resourcePath, catalogCacheDirectory, loadfromtrig);            
            
            }
            filename = catalogCacheDirectory + "afterupdateSemanticRepository.trig";
            log.debug("updateRepository(): Dumping Semantic Repository to: " + filename);
            RepositoryOps.dumpRepository(repository, filename);

            ProcessController.checkState();

            if (repositoryChanged) {
                log.info("update(): Repository has been changed!");

                log.info("update(): Extracting CoverageDescriptions from the Repository...");
                extractCoverageDescriptionsFromRepository(repository);

                ProcessController.checkState();

                filename = catalogCacheDirectory + "coverageXMLfromRDF.xml";
                log.info("update(): Dumping CoverageDescriptions Document to: " + filename);
                dumpCoverageDescriptionsDocument(filename);

                ProcessController.checkState();

                log.info("update(): Updating catalog cache....");
                updateCatalogCache(repository);

            } else {
                log.info("update(): The repository was unchanged.");
            }


        }
        finally {
            try {
                shutdownRepository(repository);
            }
            catch(Exception e){
                log.error("update(): Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
            }

            ProcessController.setProcessingState(false);

            repositoryWriteLock.unlock();

            log.info("update(): Catalog update complete.");
        }
        


    }

    /**
     * Extract coverage description from the RDF store and update catalog cache.
     * @throws InterruptedException - if the process is interrupted.
     * @throws RepositoryException - if a problem occurs when setting up the repository.
     * @throws IOException 
     * @throws RDFParseException 
     */
    private void loadWcsCatalogFromRepository() throws InterruptedException, RepositoryException, RDFParseException, IOException {
        long startTime, endTime;
        double elapsedTime;
        log.info("#############################################");
        log.info("#############################################");
        log.info("Loading WCS Catalog from Semantic Repository.");
        startTime = new Date().getTime();
        Repository repository = setupRepository();
        try {
            extractCoverageDescriptionsFromRepository(repository);
            updateCatalogCache(repository);
        }
        finally {
            try {
                shutdownRepository(repository);
            }
            catch(Exception e){
                log.error("Caught "+e.getClass().getName()+"  Message: "+e.getMessage());
            }
        }
        endTime = new Date().getTime();
        elapsedTime = (endTime - startTime) / 1000.0;
        log.info("WCS Catalog loaded from the Semantic Repository. Loaded in " + elapsedTime + " seconds.");
        log.info("#############################################");
        log.info("#############################################");
    }

    public String getDataAccessUrl(String coverageID)  throws InterruptedException {

        return coverageIDServer.get(coverageID).firstElement();

    }

    /**
     * Process the configration file passed in as the root element of an xml file.
     * Set the catalog cache directory, resource path and the interval of running
     * updating catalog thread.
     *  
     * @param config - the root element of the configuration file.
     * @param defaultCacheDirectory - the default location on disk to store the repository and its dump
     * files.
     * @param defaultResourcePath - the default directory for the transform style sheets and other local
     * files.  
     */
    private void processConfig(Element config, String defaultCacheDirectory, String defaultResourcePath) throws InterruptedException {

        Element e;
        File file;

        //########################################################
        //  Process Catalog Cache Directory.
        //
        defaultCacheDirectory = Scrub.fileName(defaultCacheDirectory);

        catalogCacheDirectory = defaultCacheDirectory;
        e = config.getChild("CacheDirectory");
        if (e != null)
            catalogCacheDirectory = Scrub.fileName(e.getTextTrim());
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
        defaultResourcePath = Scrub.fileName(defaultResourcePath);          
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


    private void shutdownRepository(Repository repository) throws InterruptedException, RepositoryException {

        log.debug("shutdownRepository)(): Shutting down Repository...");
        repository.shutDown();
        log.debug("shutdownRepository(): Repository shutdown complete.");
    }

    /**
     * Setup and initialize the repository. Set the OWLIM rule set, catalog cache directory and
     * owlim storage directory.
     * @return repository - a reference to the repository
     * @throws RepositoryException - if cannot setup the repository.
     * @throws InterruptedException - if the process is interrupted.
     * @throws IOException 
     * @throws RDFParseException 
     */
    private Repository setupRepository() throws RepositoryException, InterruptedException, RDFParseException, IOException {


        log.info("Setting up Semantic Repository.");

        //OWLIM Sail Repository (inferencing makes this somewhat slow)
        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        Repository repository = new IRISailRepository(owlimSail); //owlim inferencing


        log.info("Configuring Semantic Repository.");
        File storageDir = new File(Scrub.fileName(catalogCacheDirectory)); //define local copy of repository
        owlimSail.setDataDir(storageDir);
        log.debug("Semantic Repository Data directory set to: " + catalogCacheDirectory);
        // prepare config
        owlimSail.setParameter("storage-folder", owlim_storage_folder);
        log.debug("Semantic Repository 'storage-folder' set to: " + owlim_storage_folder);

        // Choose the operational ruleset
        String ruleset;
        ruleset = "owl-horst-optimized";
        //ruleset = "owl-max-optimized";

        owlimSail.setParameter("ruleset", ruleset);
        //owlimSail.setParameter("ruleset", "owl-max");
        
        //owlimSail.setParameter("partialRDFs", "true");
        
        log.debug("Semantic Repository 'ruleset' set to: " + ruleset);


        log.info("Intializing Semantic Repository.");

        // Initialize repository
        repository.initialize(); //needed
        
        log.info("Semantic Repository Ready.");


        ProcessController.checkState();


        return repository;

    }


    private void extractCoverageDescriptionsFromRepository(Repository repository) throws RepositoryException, InterruptedException {
        RepositoryConnection con = null;



        try {
            con = repository.getConnection();
            log.info("Repository connection has been opened.");

            extractCoverageDescriptionsFromRepository(con);

        }
        finally {
            if (con != null) {
                try {
                    log.info("Closing repository connection.");
                    con.close();
                }
                catch (RepositoryException e) {
                    log.error("Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                }
            }
        }



    }

    /**
     * Instantiates XMLfromRDF and build CoverageDescription XML from the repository.
     * @param con-connection to the repository
     * @throws InterruptedException - if the process is interrupted.
     */
    private void extractCoverageDescriptionsFromRepository(RepositoryConnection con) throws InterruptedException {


        ProcessController.checkState();
        try {
            //retrieve XML from the RDF store.
            log.info("extractCoverageDescriptionsFromRepository() - Extracting CoverageDescriptions from repository.");
            log.info("extractCoverageDescriptionsFromRepository() - Building CoverageDescription XML from repository.");
            buildDoc = new XMLfromRDF(con, "CoverageDescriptions", "http://www.opengis.net/wcs/1.1#CoverageDescription");
            buildDoc.getXMLfromRDF("http://www.opengis.net/wcs/1.1#CoverageDescription"); //build a JDOM doc by querying against the RDF store

            // Next we update the  cached maps  of datasetUrl/serverIDs and datasetUrl/wcsID
            // held in the CoverageIDGenerator so that subsequent calls to the CoverageIdGenerator
            // create new IDs correctly.
            log.info("extractCoverageDescriptionsFromRepository() - Updating CoverageIdGenerator Id Caches.");
            HashMap<String, Vector<String>> coverageIdToServerMap = getCoverageIDServerURL(con);
            CoverageIdGenerator.updateIdCaches(coverageIdToServerMap);
        } catch (RepositoryException e) {
            log.error("extractCoverageDescriptionsFromRepository():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("extractCoverageDescriptionsFromRepository():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("extractCoverageDescriptionsFromRepository():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        }
        ProcessController.checkState();


    }


    private void dumpCoverageDescriptionsDocument(String filename) throws InterruptedException {
        //print out the XML
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream fos = null;

        filename = Scrub.fileName(filename);
        try {
            fos = new FileOutputStream(filename);
            outputter.output(buildDoc.getDoc(), fos);
        } catch (IOException e1) {
            log.error("dumpCoverageDescriptionsDocument(): Failed to dump wcs:CovergaeDescriptions document.  Message: "+ e1.getMessage());
        }
        finally {
            if(fos!=null){
                try {
                    fos.close();
                }
                catch(Exception e){
                    log.error("dumpCoverageDescriptionsDocument(): "+ e.getMessage());
                }
            }
        }


    }



    /**
     * Retrieve the data set list and the import RDF files from the config file.
     * @param configFile - the configure file for the semantic engine.
     * @return a String vector holding list of files to import.
     * @throws IOException - if reading the configure file fails.
     * @throws JDOMException - if fail to create a JDOM element. 
     */
    private Vector<String> getRdfImports(URL configFile) throws InterruptedException, IOException, JDOMException {

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

            ThreddsCatalogUtil tcu = new ThreddsCatalogUtil();
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
     * @param repository - the RDF store.
     * @throws Exception - if cannot connected to the repository, pasing dateTime String fail,
     */
    private void ingestWcsCatalog(Repository repository) throws InterruptedException  {

        log.info("ingestWcsCatalog(): Ingesting catalog from CoverageDescriptions Document built by the XMLFromRDF object...");


        log.info("ingestWcsCatalog(): Flushing catalog.");
        coverages.clear();


        HashMap<String, String> lmtfc;
        String coverageLMT;
        String coverageID;
        Element idElement;
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        long lastModifiedTime;
        String dapVariableID;
        RepositoryConnection con = null;


        // Get the children of the CoverageDescription element in buildDoc. jhrg 12/21/11
        List coverageDescriptions = buildDoc.getDoc().getRootElement().getChildren();

        try {

            con = repository.getConnection();
            lmtfc = getLastModifiedTimesForWcsCoverages(con);


            for (Object o : coverageDescriptions) {
                Element cde = (Element) o;

                idElement = cde.getChild("Identifier", WCS.WCS_NS);

                if (idElement != null) {
                    coverageID = idElement.getTextTrim();
                    coverageLMT = lmtfc.get(coverageID);

                    if(coverageLMT == null){
                        log.error("ingestWcsCatalog(): Failed to obtain last modified time for coverage "+coverageID);
                        log.error("ingestWcsCatalog(): CoverageDescription "+coverageID+" cannot be ingested. Skipping...");
                    }
                    else {
                        String dateTime = coverageLMT.substring(0, 10) + " " + coverageLMT.substring(11, 19) + " +0000";
                        log.debug("ingestWcsCatalog(): CoverageDescription '" + coverageID + "' has a last modified time of " + dateTime);
                        lastModifiedTime = sdf.parse(dateTime).getTime();
                        CoverageDescription coverageDescription = ingestWcsCoverageDescription(cde, lastModifiedTime);

                        if (_catalogLastModifiedTime < lastModifiedTime)
                            _catalogLastModifiedTime = lastModifiedTime;

                        if (coverageDescription != null) {

                            for (String fieldID : coverageDescription.getFieldIDs()) {
                                log.debug("Getting DAP Coordinate IDs for FieldID: " + fieldID);

                                dapVariableID = getDapGridId(con, coverageID, fieldID);
                                coverageDescription.setDapGridArrayId(fieldID, dapVariableID);

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
                        log.debug("ingestWcsCatalog(): Ingested CoverageDescription '" + coverageID + "'");
                    }
                }
                else {
                    log.error("ingestWcsCatalog(): Failed to obtain wcs:Identifier for a generated wcs:Coverage.");
                }

            }
        } catch (RepositoryException e) {
            log.error("ingestWcsCatalog(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } catch (ParseException e) {
            log.error("ingestWcsCatalog(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } finally {
            if (con != null) {
                try {
                    log.info("ingestWcsCatalog(): Closing repository connection.");
                    con.close();
                }
                catch (RepositoryException e) {
                    log.error("ingestWcsCatalog(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                }
            }
        }

    }

    /**
     * Ingest individual coverage description element into the WCS catalog.
     * @param cde - coverage description element.
     * @param lastModified - last modified time of the RDF document.
     * @return WCS CoverageDescription.
     */
    private CoverageDescription ingestWcsCoverageDescription(Element cde, long lastModified)  throws InterruptedException {

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
     * Returns a Hash containing last modified times of each context (URI) in the
     * repository, keyed by the context name.
     * @param con A connection to the repository from which to harvest the contexts and their associated last
     * modified times.
     * @return a HashMap of last modified times and context pair.
     */
    public HashMap<String, String> getLastModifiedTimesForWcsCoverages(RepositoryConnection con) throws InterruptedException {
        TupleQueryResult result = null;
        String ltmodstr = "";
        String idstr = "";
        HashMap<String, String> idltm = new HashMap<String, String>();
        String queryString = "SELECT DISTINCT id, lmt "
                + "FROM "
                + "{doc} wcs:CoverageDescription {cd} wcs:Identifier {id},"
                + "{doc} rdfcache:"+Terms.lastModified.getLocalId() +" {lmt} "
                + "using namespace "
                + "rdfcache = <"+ Terms.rdfCacheNamespace+">, "
                + "wcs= <http://www.opengis.net/wcs/1.1#>";


        try {

            log.debug("Query for wcs:Coverage last modified times: {} ",queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);
            result = tupleQuery.evaluate();

            BindingSet bindingSet;
            Value valueOfID = null;
            Value valueOfLMT;


            log.debug("Query for wcs:Coverage last modified times. result.hasNext(): {} ",result.hasNext());

            while (result.hasNext()) {
                bindingSet = (BindingSet) result.next();

                valueOfLMT = (Value) bindingSet.getValue("lmt");
                ltmodstr = valueOfLMT.stringValue();

                valueOfID = (Value) bindingSet.getValue("id");
                idstr = valueOfID.stringValue();

                idltm.put(idstr, ltmodstr);

                // log.debug("ID:" + valueOfID.stringValue());
                // log.debug("LMT:" + valueOfLMT.stringValue());

            }
        } catch (QueryEvaluationException e) {
            log.error("getLastModifiedTimesForWcsCoverages(): Caught a QueryEvaluationException! Msg: "
                    + e.getMessage());
        } catch (RepositoryException e) {
            log.error("getLastModifiedTimesForWcsCoverages(): Caught a RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("getLastModifiedTimesForWcsCoverages(): Caught a MalformedQueryException! Msg: "
                    + e.getMessage());
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (Exception e) {
                    log.error("getLastModifiedTimesForWcsCoverages(): Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }
        }

        return idltm;
    }
    /*******************************************************/


    /**
     * Check if the coverage exists.
     * @param id - coverage ID.
     * @return true if the coverage exits, otherwise false.
     */
    public boolean hasCoverage(String id)  throws InterruptedException {

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

    public Element getCoverageDescriptionElement(String id) throws InterruptedException {

        Lock catalogReadlock = _catalogLock.readLock();
        try {
            catalogReadlock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            CoverageDescription cd ;//= coverages.get(id);
            cd = coverages.get(id);

            if (cd == null)
                return null;

            return cd.getElement();
        }
        finally {
            catalogReadlock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }
    }

    public List<Element> getCoverageDescriptionElements() throws InterruptedException, WcsException {
        throw new WcsException("getCoverageDescriptionElements() method Not Implemented", WcsException.NO_APPLICABLE_CODE);
    }


    public CoverageDescription getCoverageDescription(String id) throws InterruptedException {
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


    public Element getCoverageSummaryElement(String id) throws InterruptedException, WcsException {

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

    public List<Element> getCoverageSummaryElements() throws InterruptedException, WcsException {


        ArrayList<Element> coverageSummaries = new ArrayList<Element>();



        Lock lock = _catalogLock.readLock();

        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");
            // Get all of the unique formats.


            /*
            e = coverages.elements();
            while (e.hasMoreElements()) {
                cd = (CoverageDescription) e.nextElement();
                coverageSummaries.add(cd.getCoverageSummary());

            }
            */

            for(CoverageDescription cd : coverages.values()){
                coverageSummaries.add(cd.getCoverageSummary());
            }

        }
        finally {
            lock.unlock();
            log.debug("_catalogLock ReadLock Released.");
        }


        return coverageSummaries;
    }


    public List<Element> getSupportedFormatElements() throws InterruptedException {


        ArrayList<Element> supportedFormats = new ArrayList<Element>();
        HashMap<String, Element> uniqueFormats = new HashMap<String, Element>();

        Collection<CoverageDescription> enm;
        Element e;
        Iterator i;


        Lock lock = _catalogLock.readLock();

        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");


            // Get all of the unique formats.


            for(CoverageDescription cd : coverages.values()) {

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

    public List<Element> getSupportedCrsElements() throws InterruptedException {

        ArrayList<Element> supportedCRSs = new ArrayList<Element>();
        HashMap<String, Element> uniqueCRSs = new HashMap<String, Element>();

        Element e;
        Iterator i;


        Lock lock = _catalogLock.readLock();

        try {
            lock.lock();
            log.debug("_catalogLock ReadLock Acquired.");

            // Get all of the unique formats.
            for(CoverageDescription cd : coverages.values()) {
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
     * Get the Dap Id of the dap Grid variable associated with the passed WCS field ID.
     * @param con - connection to the repository.
     * @param coverageId - coverage ID.
     * @param fieldId - field ID.
     * @return coordinateDapI e.g. latitude coordinate Dap Id. Null if not found.
     */
    private String getDapGridId(RepositoryConnection con, String coverageId, String fieldId) throws InterruptedException {
        //log.debug("getLatitudeCoordinateDapId(): Getting the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        String qString = createDapGridIdQuery(coverageId, fieldId);
        String dapGridId = runQuery(con, qString, "gridid");
        log.debug("getDapGridArrayId(): '" + dapGridId + "' is the DAP variable Grid ID that represents the latitude coordinate for FieldID: " + fieldId);
        return dapGridId;

    }

    /**
     * Create the query string for retrieving the Dap Grid ID given the the coverage and field ID.
     * @param coverageStr - the coverage ID string.
     * @param fieldStr - the field ID string.
     * @return the query string.
     */
     private String createDapGridIdQuery(String coverageStr, String fieldStr) throws InterruptedException {

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
     * @param con - connection to the repository.
     * @param coverageId - the coverage ID string.
     * @param fieldId - the field ID string.
     * @return latitude coordinate Dap Id.
     */
    private String getLatitudeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId)throws InterruptedException {
        //log.debug("getLatitudeCoordinateDapId(): Getting the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_latitude", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getLatitudeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the latitude coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    /**
     * Get longitude coordonate Dap ID, given the coverage ID and field ID.
     * @param con - connection to the repository
     * @param coverageId - the coverage ID string.
     * @param fieldId - the field ID string.
     * @return longitude coordinate Dap Id.
     */
    private String getLongitudeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) throws InterruptedException {
        //log.debug("getLongitudeCoordinateDapId(): Getting the DAP variable ID that represents the longitude coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_longitude", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getLongitudeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the longitude coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    /**
     * Get elevation coordinate ID, given the coverage ID and field ID.
     * @param con - connection to the repository
     * @param coverageId - the coverage ID string.
     * @param fieldId - the field ID string.
     * @return elevation coordinate Dap Id. Null if not found.
     */
    private String getElevationCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId)throws InterruptedException  {
        //log.debug("getElevationCoordinateDapId(): Getting the DAP variable ID that represents the elevation coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_elevation", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getElevationCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the elevation coordinate for FieldID: " + fieldId);
        return coordinateDapId;

    }

    /**
     * Get time coordinate ID, given the coverage ID and field ID.
     * @param con - connection to the repository
     * @param coverageId - the coverage ID string.
     * @param fieldId - the field ID string.
     * @return time coordinate Dap Id.    Null if not found.
     */
    private String getTimeCoordinateDapId(RepositoryConnection con, String coverageId, String fieldId) throws InterruptedException {
        //log.debug("getTimeCoordinateDapId(): Getting the DAP variable ID that represents the time coordinate for FieldID: " + fieldId);
        String qString = createCoordinateIdQuery("A_time", coverageId, fieldId);
        String coordinateDapId = runQuery(con, qString, "cidid");
        //log.debug("getTimeCoordinateDapId(): '" + coordinateDapId + "' is the DAP variable ID that represents the time coordinate for FieldID: " + fieldId);
        return coordinateDapId;
    }

    /**
     * Get time units, given the coverage ID and field ID.
     * @param con - connection to the repository
     * @param coverageId - the coverage ID string
     * @param fieldId - the field ID string.
     * @return time units expressed as a string.   Null if not found.
     */
    private String getTimeUnits(RepositoryConnection con, String coverageId, String fieldId) throws InterruptedException {
        String qString = createCoordinateUnitsQuery("A_time", coverageId, fieldId);
        String coordinateUnit = runQuery(con, qString, "unit");
        log.debug("getTimeUnits(): '" + coordinateUnit + "' is the units of the time coordinate for FieldID: " + fieldId);
        return coordinateUnit;
    }

    /**
     * Evaluate the query string for getting coordinate Dap ID and units.
     * @param con - connection to the repository.
     * @param qString - query string.
     * @param cidid - the search key used in the query string.
     * @return the coordinate dap ID when cidid="cidic", coordinate units when cidid="unit",
     * dap grid ID when cidid="gridid".  Null if not found.
     */
    private String runQuery(RepositoryConnection con, String qString, String cidid) throws InterruptedException {
        String coordinateDapId = null;
        try {

            TupleQueryResult result;

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL, qString);

            result = tupleQuery.evaluate();

            if (result != null && result.hasNext()) {

                while (result.hasNext()) {

                    BindingSet bindingSet = result.next();

                    Value firstValue = bindingSet.getValue(cidid);

                    coordinateDapId = firstValue.stringValue();
                }
            } else {
                log.debug("No query result!");

            }
        } catch (RepositoryException e) {
            log.error("runQuery(): Query FAILED. Caught " + e.getClass().getName() + " Message: " + e.getMessage()+" Query String used was: "+ qString);
        } catch (QueryEvaluationException e) {
            log.error("runQuery(): Query FAILED. Caught " + e.getClass().getName() + " Message: " + e.getMessage()+" Query String used was: "+ qString);
        } catch (MalformedQueryException e) {
            log.error("runQuery(): Query FAILED. Caught " + e.getClass().getName() + " Message: " + e.getMessage()+" Query String used was: "+ qString);
        }

        return coordinateDapId;
    }

    /**
     * Create the query string, given the coverage and field.
     * @param coordinateName - A_latitude/longitude/elevation/time.
     * @param coverageStr - coverage ID string.
     * @param fieldStr - field ID string.
     * @return query string for coordinate ID.
     */
    private String createCoordinateIdQuery(String coordinateName, String coverageStr, String fieldStr) throws InterruptedException {

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
     * @param coordinateName - A_latitude/longitude/elevation/time.
     * @param coverageStr  - coverage ID string.
     * @param fieldStr  - field ID string.
     * @return query string for coordinate unit.
     */
    private String createCoordinateUnitsQuery(String coordinateName, String coverageStr, String fieldStr) throws InterruptedException {

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
     * Get the last modified time of the catalog.
     * @return  the last modified time of the catalog.
     */
    public long getLastModified() {

        return _catalogLastModifiedTime;
    }

    /**
     * Update catalog cache from the repository by calling <code>ingestWcsCatalog(repository)</code>.
     * @param repository - the RDF store.
     * @throws InterruptedException - if the process is interrupted.
     */
    private void updateCatalogCache(Repository repository) throws InterruptedException {



        int biffCount = 0;

        ProcessController.checkState();

        Lock catalogWriteLock = _catalogLock.writeLock();

        try {
            catalogWriteLock.lock();

            log.debug("_catalogLock WriteLock Acquired.");

            ProcessController.checkState();

            coverageIDServer = getCoverageIDServerURL(repository);

            addSupportedFormats(buildDoc.getRootElement());

            ingestWcsCatalog(repository);
            timeOfLastUpdate = new Date().getTime();


            log.debug("Catalog Cache updated at " + new Date(timeOfLastUpdate));



        }
        catch (RepositoryException e) {
            log.error("updateCatalogCache():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } catch (MalformedURLException e) {
            log.error("updateCatalogCache():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("updateCatalogCache():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("updateCatalogCache():  Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        } finally {
            catalogWriteLock.unlock();
            log.debug("_catalogLock WriteLock Released.");
        }

        ProcessController.checkState();



    }

    /**
     * Get coverage ID and server URL from the repository.
     * @param repo - the RDF store.
     * @return A hashmap of coverage ID and list of URLs of servers.
     * @throws RepositoryException - if cannot connect to the repository.
     * @throws MalformedQueryException - if the query is wrong.
     * @throws QueryEvaluationException - if fail in evaluating the query.
     */

    private HashMap<String, Vector<String>> getCoverageIDServerURL(Repository repo) throws InterruptedException,  RepositoryException, MalformedQueryException, QueryEvaluationException {
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
     * @param con - connection to a repository.
     * @return hashmap of coverage ID and list of URLs of servers.
     * @throws RepositoryException - if the connection to the repository is fail.
     * @throws MalformedQueryException - if the query is malformed.
     * @throws QueryEvaluationException - if fail in evaluating the query.
     */
    private HashMap<String, Vector<String>> getCoverageIDServerURL(RepositoryConnection con) throws InterruptedException, RepositoryException, MalformedQueryException, QueryEvaluationException {
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
     * Ingest supported format in the coverage document.
     * @param coverages - an element id the XML document of coverages.
     * @throws MalformedURLException - if the URL of server ID is malformed. 
     */
    private void addSupportedFormats(Element coverages) throws InterruptedException, MalformedURLException {

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
     * @param dapServerUrl - URL of the DAP server.
     * @return a vector of elements of WCS supported format.
     */
    private Vector<Element> getWcsSupportedFormatElements(URL dapServerUrl) throws InterruptedException {

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



    /**
     * This is the "main()" method of the catalog update thread. It runs the
     * <code>update()</code> which updates the repository and catalog cache. and then
     * sleeps
     */
    public void run() {

        ProcessController.enableProcessing();
        int updateCounter = 0;
        long startTime, endTime;
        long elapsedTime, sleepTime;
        Thread thread = Thread.currentThread();



        log.info("run(): ************* CATALOG UPDATE THREAD.("+thread.getName()+") HAS BEEN STARTED.");
        try {
            log.info("run(): ************* CATALOG UPDATE THREAD ("+thread.getName()+") will commence in " +
                    firstUpdateDelay / 1000.0 + " seconds. Sleeping now...");
            Thread.sleep(firstUpdateDelay);

        } catch (InterruptedException e) {
            log.warn("run(): "+thread.getName()+" caught InterruptedException. Stopping...");
            ProcessController.stopProcessing();
        }


        while (ProcessController.continueProcessing()) {

            try {

                startTime = new Date().getTime();
                try {
                    update();
                } catch (RepositoryException e) {
                    log.error("run(): Catalog Update FAILED!!! Caught " + e.getClass().getName() +
                            "  Message: " + e.getMessage());
                } catch (RDFParseException e) {
                    log.error("run(): Catalog Update FAILED!!! Caught " + e.getClass().getName() +
                            "  Message: " + e.getMessage());
                } catch (JDOMException e) {
                    log.error("run(): Catalog Update FAILED!!! Caught " + e.getClass().getName() +
                            "  Message: " + e.getMessage());
                } catch (IOException e) {
                    log.error("run(): Catalog Update FAILED!!! Caught " + e.getClass().getName() +
                            "  Message: " + e.getMessage());
                } catch (InterruptedException e) {
                    log.warn("run(): "+thread.getName()+" caught InterruptedException. Stopping...");
                } catch (RuntimeException e) {
                    log.warn("run(): "+thread.getName()+" caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
                }

                ProcessController.checkState();


                endTime = new Date().getTime();
                elapsedTime = (endTime - startTime);
                updateCounter++;
                log.info("run(): Completed catalog update " + updateCounter + " in " + elapsedTime / 1000.0 + " seconds.");

                sleepTime = catalogUpdateInterval - elapsedTime;
                if (ProcessController.continueProcessing() && sleepTime > 0) {
                    log.info("run(): "+thread.getName()+" sleeping for " + sleepTime / 1000.0 + " seconds.");
                    Thread.sleep(sleepTime);
                }

            } catch (InterruptedException e) {
                log.warn("run(): "+thread.getName()+" caught InterruptedException. Stopping...");
                ProcessController.stopProcessing();

            }
        }

        log.info("run(): ************* CATALOG UPDATE THREAD.("+Thread.currentThread().getName()+") IS EXITING.");


    }

    public void destroy() {



        ReentrantReadWriteLock.WriteLock catLock = _catalogLock.writeLock();
        ReentrantReadWriteLock.WriteLock reposLock = _repositoryLock.writeLock();

        Thread thisThread = Thread.currentThread();

        try {
            ProcessController.stopProcessing();

            if(!reposLock.tryLock()){
                log.warn("destroy(): Repository is WRITE LOCKED...");
                if(catalogUpdateThread != null && catalogUpdateThread.isAlive()){
                    log.info("destroy() Current thread '" + thisThread.getName() + "' Interrupting catalogUpdateThread '" + catalogUpdateThread + "'");
                    catalogUpdateThread.interrupt();
                    log.info("destroy(): catalogUpdateThread '" + catalogUpdateThread.getName() + "' interrupt() called.");
                }
                try {
                    log.info("destroy(): Waiting for '" + catalogUpdateThread.getName() + "' to stop...");
                    catalogUpdateThread.join();
                    log.info("destroy(): '" + catalogUpdateThread.getName() + "' has stopped.");

                } catch (InterruptedException e) {
                    log.error("destroy(): Caught InterruptedException while waiting for  '" + catalogUpdateThread.getName() + "' to finish...");
                }

            }


            log.info("destroy(): Attempting to WriteLock _catalogLock.");
            catLock.lock();
            log.info("destroy(): Attempting to  WriteLock  _repositoryLock.");
            reposLock.lock();
            log.info("destroy(): WriteLocks Acquired.");
            
            if (catalogUpdateThread != null) {
                while(catalogUpdateThread.isAlive()){
                    log.info("destroy() Current thread '" + thisThread.getName() + "' Interrupting catalogUpdateThread '" + catalogUpdateThread + "'");
                    catalogUpdateThread.interrupt();
                    log.info("destroy(): catalogUpdateThread '" + catalogUpdateThread.getName() + "' interrupt() called.");
                    try {
                        log.info("destroy(): Waiting for '" + catalogUpdateThread.getName() + "' to stop...");
                        catalogUpdateThread.join();
                        log.info("destroy(): '" + catalogUpdateThread.getName() + "' has stopped.");

                    } catch (InterruptedException e) {
                        log.error("destroy(): Caught InterruptedException while waiting for  '" + catalogUpdateThread.getName() + "' to finish...");
                    }
                     
                }

            }

        }
        finally {
            catalogUpdateThread = null;
            catLock.unlock();
            reposLock.unlock();
            log.info("destroy(): Released WriteLock for _catalogLock and _repositoryLock.");
            log.info("destroy(): Complete.");
        }

    }



}