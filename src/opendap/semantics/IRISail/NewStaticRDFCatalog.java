package opendap.semantics.IRISail;

import net.sf.saxon.s9api.SaxonApiException;
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


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
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
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;


import com.ontotext.trree.owlim_ext.SailImpl;

/**
 * A colon of LocalFileCatalog
 */
public class NewStaticRDFCatalog implements WcsCatalog, Runnable {


    private Logger log; // = LoggerFactory.getLogger(StaticRDFCatalog.class);


    private AtomicBoolean repositoryUpdateActive;
    private ReentrantReadWriteLock _repositoryLock;
    private IRISailRepository owlse2;
    private XMLfromRDF buildDoc;

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
    
    private RepositoryConnection con;
    private Vector<String> repositoryContexts;

    private HashMap<String, Boolean> downService;
    private Vector<String> imports;
    private Vector<String> constructs;


    public NewStaticRDFCatalog() {
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
        
        con = null;
        owlse2 = null;
        buildDoc = null;
        _lastModified = -1;
        _config = null;
        catalogCacheDirectory = ".";
        owlim_storage_folder ="owlim-storage";
        resourcePath = ".";

        initialized = false;
        
        repositoryContexts = new Vector<String>();

        downService = new HashMap<String, Boolean>();
        imports = new Vector<String>();
        

    }
    /*** 
   public static void main(String[] args) {
        long startTime, endTime;
        double elapsedTime;
        HashMap<String, Vector<String>> coverageIDServer;

        //GenerateNTriples catalog = new GenerateNTriples();
        startTime = new Date().getTime();
        NewStaticRDFCatalog catalog = new NewStaticRDFCatalog();
        if (args.length != 1) {
            catalog.log
                    .error("Usage: java -jar generatentriples.jar config_file/owl_file");
            catalog.log
                    .error("Example: java -jar generatentriples.jar file:///data/haibo/workspace/IRIWMS/wcs_service.xml");
            catalog.log
                    .error("Or: java -jar generatentriples.jar http://iri.columbia.edu/~haibo/opendaptest/datasetcoveragelist.owl");
            System.exit(1);

        }
       
        try {

            System.out.println("arg0= " + args[0]);

            String configFileName = null;
            configFileName = args[0];
            catalog.log.debug("main() using config file: " + configFileName);
            Vector<String> importURLs = new Vector<String>();
            if (configFileName.endsWith("xml")) {

                Element olfsConfig = opendap.xml.Util.getDocumentRoot(configFileName);

                catalog.log.debug("main() using config file: " + configFileName);
                catalog._config = (Element) olfsConfig.getDescendants(
                        new ElementFilter("WcsCatalog")).next();
                catalog.processConfig(catalog._config, catalog.catalogCacheDirectory,
                        catalog.resourcePath);
                catalog.log.debug("main(): Getting RDF imports.");
                importURLs = catalog.getRdfImports(catalog._config);
            } else if (configFileName.endsWith("owl")) {
                importURLs.add(configFileName);

            }

            for (String startingPointUrl :importURLs )
            catalog.startingPoints.add(startingPointUrl); // startingpoint from input file
            catalog.setupOwlimRepository();
            
            Vector<String> newStartingPoints = null;
            Vector<String> startingPointsToDrop = null;    
            try {
                catalog.con = catalog.owlse2.getConnection();
                if (catalog.con.isOpen()) {
                    catalog.log.info("Connection is OPEN!");
                    catalog.findUnneededRDFDocuments(catalog.con);
                    newStartingPoints = catalog.findNewStartingPoints(catalog.con);
                    startingPointsToDrop = catalog.findChangedStartingPoints(catalog.con);
                    catalog.findChangedRDFDocuments(catalog.con);
                    catalog.con.close();
                    catalog.log.info("Connection is Closed!");
                }
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            
            if (!catalog.dropList.isEmpty()) {

                catalog.findExternalInferencingContexts();
                catalog.con = catalog.owlse2.getConnection();
                catalog.log.debug("Droppping starting point ...");
                catalog.owlse2.dropStartingPoints(catalog.con, startingPointsToDrop);
                catalog.con.close();
                catalog.log.debug("Finished droppping starting point.");
                catalog.log.debug("Droppping changed RDFDocuments ...");
                catalog.dropContexts();
                catalog.log.debug("Finished droppping changed RDFDocuments.");
                catalog.log.debug("Updating repository ...");
                catalog.updateIriRepository();

                catalog.log.debug("Running construct rules ...");
                //catalog.con = catalog.owlse2.getConnection();
                catalog.log.debug("Updating repository ...");
                catalog.log.debug("Running construct rules ...");
                catalog.ingestSwrlRules();
                catalog.log.debug("Finished running construct rules.");
            }
            if (!newStartingPoints.isEmpty() && !catalog.newRepository) {
                
                catalog.con = catalog.owlse2.getConnection();
                catalog.owlse2.addStartingPoints(catalog.con, newStartingPoints);
                catalog.con.close(); 
                catalog.updateIriRepository();

                catalog.log.debug("Running construct rules ...");
                
                    catalog.ingestSwrlRules();
            }
            else if (catalog.newRepository) {
                catalog.con = catalog.owlse2.getConnection();
                //catalog.owlse2.addStartingPoints(catalog.con, configFileName,importURLs);
                catalog.owlse2.addStartingPoints(catalog.con, newStartingPoints);
                catalog.con.close();

                catalog.updateIriRepository();

                catalog.log.debug("Running construct rules ...");
                
                    catalog.ingestSwrlRules();
                
            }

        } catch (RepositoryException e) {
            catalog.log.error("Caught RepositoryException in main(): "
                    + e.getMessage());

        } catch (MalformedURLException e) {
            catalog.log.error("Caught MalformedURLException in main(): "
                    + e.getMessage());
        } catch (IOException e) {
            catalog.log
                    .error("Caught IOException in main(): " + e.getMessage());
        } catch (JDOMException e) {
            catalog.log.error("Caught JDOMException in main(): "
                    + e.getMessage());
        }

        String filename = catalog.catalogCacheDirectory + "owlimHorstRepository.nt";

        catalog.log.debug("main(): Dumping Semantic Repository to: " + filename);
        try {
            catalog.con = catalog.owlse2.getConnection();
            if (catalog.con.isOpen()) {
                catalog.log.info("Connection is opened!");
                catalog.dumpRepository(catalog.con, filename);
                catalog.con.close();
                catalog.log.info("Connection is closed!");
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }

        filename = catalog.catalogCacheDirectory + "owlimHorstRepository.trig";
        catalog.log.debug("main(): Dumping Semantic Repository to: " + filename);
        try {
            catalog.con = catalog.owlse2.getConnection();
            if (catalog.con.isOpen()) {
                catalog.log.info("Connection is opened!");
                catalog.dumpRepository(catalog.con, filename);
                catalog.extractCoverageDescrptionsFromRepository();
                catalog.updateCatalogCache();
                String coveragefilename = catalog.catalogCacheDirectory + "coverageXMLfromRDF.xml";
                catalog.log.debug("updateRepository2(): Dumping CoverageDescriptions Document to: "+coveragefilename);
                catalog.dumpCoverageDescriptionsDocument(coveragefilename);
                catalog.con.close();
                catalog.log.info("Connection is closed!");
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        catalog.destroy();
        endTime = new Date().getTime();
        elapsedTime = (endTime - startTime) / 1000;
        catalog.log.info("Completed generating triples in " + elapsedTime + " seconds.");
    }****/
   
    public static void main(String[] args) {

        long startTime, endTime;
        double elapsedTime;


        NewStaticRDFCatalog catalog = new NewStaticRDFCatalog();


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
               
                //catalog.updateCatalog();
                catalog.updateCatalog2();
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
    //private void updateSemanticRepository2(RepositoryConnection con,

    private void updateSemanticRepository2(Vector<String> importURLs)
            throws InterruptedException,RepositoryException {

        Vector<String> dropList = new Vector<String>();
        Vector<String> startingPoints = new Vector<String>();
        boolean isNewRepository = true;


        Date startTime = new Date();
        log.info("Evaluating importURLs for updateCatalog... ");
        RepositoryConnection con = null;
        try {


            for (String startingPointUrl : importURLs){
                startingPoints.add(startingPointUrl); // startingpoint from input file
            }

            Vector<String> newStartingPoints = null;
            Vector<String> startingPointsToDrop = null;
            try {
                con = owlse2.getConnection();
                if (con.isOpen()) {
                    log.info("Connection is OPEN!");

                    isNewRepository = RepositoryUtility.isNewRepository(con);

                    newStartingPoints = RepositoryUtility.findNewStartingPoints(con,startingPoints);

                    dropList.addAll(findUnneededRDFDocuments(con));
                    startingPointsToDrop = RepositoryUtility.findChangedStartingPoints(con, startingPoints);
                    dropList.addAll(startingPointsToDrop);
                    dropList.addAll(findChangedRDFDocuments(con));
                }
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
            finally {
                if (con != null)
                    con.close();
                log.info("Connection is Closed!");
            }


            if (isNewRepository) {

                try {
                    con = owlse2.getConnection();
                    RepositoryUtility.addStartingPoints(con, owlse2.getValueFactory(), newStartingPoints);
                }
                finally {
                    if (con != null)
                        con.close();
                }

                updateIriRepository();
                log.debug("Running construct rules ...");
                ingestSwrlRules();
            } else {
                if (!dropList.isEmpty()) {

                    dropList.addAll(findExternalInferencingContexts());
                    try {
                        con = owlse2.getConnection();
                        log.debug("Dropping starting points ...");
                        RepositoryUtility.dropStartingPoints(con, owlse2.getValueFactory(), startingPointsToDrop);
                    }
                    finally {
                        if (con != null)
                            con.close();
                    }
                    log.debug("Finished dropping starting point.");

                    dropContexts(dropList);
                }
                if (!newStartingPoints.isEmpty()) {

                    try {
                        con = owlse2.getConnection();
                        log.debug("Adding new starting point ...");
                        RepositoryUtility.addStartingPoints(con, owlse2.getValueFactory(), newStartingPoints);
                        log.debug("Finished adding nrew starting point.");
                    }
                    finally {
                        if (con != null)
                            con.close();
                    }
                }
                log.debug("Updating repository ...");
                updateIriRepository();
                log.debug("Repository update complete.");

                log.debug("Running construct rules ...");
                ingestSwrlRules();
                log.debug("Finished running construct rules.");

            }

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException in main(): "
                    + e.getMessage());

        }


        long elapsedTime = new Date().getTime() - startTime.getTime();
        log.info("Imports Evaluated. Elapsed time: " + elapsedTime + "ms");

//        log.debug("**********************************************************************************!");
//        log.info("Updating repository!");
//        Boolean updated = owlse2.update();
//
//        /*****************************************************
//         * ingest swrl rules
//         *****************************************************/
//        ingestSwrlRules();
//
//        log.info("Repository updateCatalog =" + updated);
//        log.info("RDF import complete.");

    }

    private void setupOwlimRepository() throws RepositoryException {
        log.info("Building Semantic Repository.");

        // OWLIM Sail Repository (inferencing makes this somewhat slow)
        SailImpl owlimSail = new com.ontotext.trree.owlim_ext.SailImpl();
        String resourcePath = "./";
        String cacheDirectory = "./wms-cache";
        owlse2 = new IRISailRepository(owlimSail, resourcePath, cacheDirectory); // owlim
        // inferencing

        log.info("Configuring Semantic Repository.");
        File storageDir = new File(cacheDirectory); // define local copy of
        // repository
        owlimSail.setDataDir(storageDir);
        // prepare config
        owlimSail.setParameter("storage-folder", "owlim-storage");

        // Choose the operational ruleset
        owlimSail.setParameter("ruleset", "owl-horst");

        log.info("Intializing Semantic Repository.");
        
        // Initialize repository
        owlse2.startup(); //needed
        log.info("Semantic Repository Ready.");
        
        // log.info("Open connection to the repository ...");
        // con = owlse2.getConnection();
        // log.info("Connection to the repository is opened.");
    }

    /*
     * Update repository
     */
    private void updateIriRepository() {
        Vector<String> rdfDocList = new Vector<String>();

        findNeededRDFDocuments(rdfDocList);

        while (!rdfDocList.isEmpty()) {

            addNeededRDFDocuments(rdfDocList);

            findNeededRDFDocuments(rdfDocList);
        }

    }

    /*
     * Add all rdfcache:RDFDocuments that are needed
     */
    // private void addNeededRDFDocuments(RepositoryConnection con) {
    private void addNeededRDFDocuments(Vector<String> rdfDocs) {
        URI uriaddress;
        long inferStartTime, inferEndTime;
        inferStartTime = new Date().getTime();

        String importURL = "";
        RepositoryConnection con = null;
        int notimport = 0;
        try {
            con = owlse2.getConnection();

            log.debug("rdfDocs.size=" + rdfDocs.size());
            notimport = 0;
            while (!rdfDocs.isEmpty()) {
                importURL = rdfDocs.remove(0).toString();

                log.debug("Checking import URL: " + importURL);

                URL myurl = new URL(importURL);

                HttpURLConnection hc = (HttpURLConnection) myurl
                        .openConnection();
                log.debug("Connected to import URL: " + importURL);

                int rsCode = -1;
                try {
                    rsCode = hc.getResponseCode();
                } catch (IOException e) {
                    log.error("Unable to get HTTP status code for " + importURL
                            + " Caught IOException! Msg: " + e.getMessage());
                }
                log.debug("Got HTTP status code: " + rsCode);

                if (downService.containsValue(importURL)
                        && downService.get(importURL)) {
                    log.error("Server error, Skip " + importURL);
                } else if (rsCode == -1) {
                    log.error("Unable to get an HTTP status code for resource "
                            + importURL + " WILL NOT IMPORT!");
                    downService.put(importURL, true);

                } else if (rsCode > 500) { // server error
                    if (rsCode == 503) {
                        log.error("Error 503 Skipping " + importURL);
                        downService.put(importURL, true);
                    } else
                        log.error("Server Error? Received HTTP Status code "
                                + rsCode + " for URL: " + importURL);

                } else if (rsCode == 304) {
                    log.info("Not modified " + importURL);
                    downService.put(importURL, true);
                } else if (rsCode == 404) {
                    log.error("Received HTTP 404 status for resource: "
                            + importURL);
                    downService.put(importURL, true);
                } else if (rsCode == 403) {
                    log.error("Received HTTP 403 status for resource: "
                            + importURL);
                    downService.put(importURL, true);
                } else {

                    log.debug("Import URL appears valid ( " + importURL + " )");

                    String urlsufix = importURL.substring(
                            (importURL.length() - 4), importURL.length());

                    if (urlsufix.equals(".owl") || urlsufix.equals(".rdf")) {

                        uriaddress = new URIImpl(importURL);

                        URL url;

                        url = new URL(importURL);

                        log.info("Importing URL " + url);
                        con.add(url, importURL, RDFFormat.RDFXML,
                                (Resource) uriaddress);
                        owlse2.setLTMODContext(importURL, con); // set last modified
                                                                // time of the context
                        
                        log.info("Finished importing URL " + url);

                        // setIsContainedBy(importURL,
                        // CollectionURL); //need some work here!!!

                    } else if (importURL.substring((importURL.length() - 4),
                            importURL.length()).equals(".xsd")) {

                        uriaddress = new URIImpl(importURL);

                        ByteArrayInputStream inStream;
                        log.info("Transforming URL " + importURL);
                        inStream = new ByteArrayInputStream(owlse2
                                .transformXSD(importURL).toByteArray());
                        log.info("Finished transforming URL " + importURL);
                        log.debug("Importing URL " + importURL);
                        con.add(inStream, importURL, RDFFormat.RDFXML,
                                (Resource) uriaddress);
                        owlse2.setLTMODContext(importURL, con); // set last modified
                                                                // time for the context

                        // setIsContainedBy(importURL,
                        // CollectionURL); //need some work
                        // here!!!

                        log.debug("Finished importing URL " + importURL);

                    } else {
                        notimport++;
                        URL url = new URL(importURL);
                        URLConnection urlc = url.openConnection();
                        
                        urlc.setRequestProperty("Accept",
                                        "application/rdf+xml,application/xml,text/xml,*/*");
                        // urlc.setRequestProperty("Accept",
                        // "application/rdf+xml, application/xml;
                        // q=0.9,text/xml; q=0.9, */*; q=0.2");
                        urlc.connect();

                        try {
                            InputStream inStream = urlc.getInputStream();

                            uriaddress = new URIImpl(importURL);
                            
                            owlse2.setLTMODContext(importURL, con);
                        } catch (IOException e) {
                            log.error("Caught an IOException! in urlc.getInputStream() Msg: "
                                            + e.getMessage());

                        }
                       
                        log.info("Imported non owl/xsd = " + importURL);
                        log.info("Total non owl/xsd Nr = " + notimport);
                    }
                }
                imports.add(importURL);
            } // while (!rdfDocs.isEmpty()
        } catch (IOException e) {
            log.error("Caught an IOException! Msg: " + e.getMessage());

        } catch (SaxonApiException e) {
            log.error("Caught a SaxsonException! Msg: " + e.getMessage());
        } catch (RDFParseException e) {

            log.error("Caught an RDFParseException! Msg: " + e.getMessage());
        } catch (RepositoryException e) {

            log.error("Caught an RepositoryException! Msg: " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! in addNeededRDFDocuments() Msg: "
                                + e.getMessage());
            }
            inferEndTime = new Date().getTime();
            double inferTime = (inferEndTime - inferStartTime) / 1000.0;
            log.debug("Import takes " + inferTime + " seconds");
        }
    }

    /*
     * Find all rdfcache:RDFDocuments that are needed
     */
    // private void findNeededRDFDocuments(RepositoryConnection con) {
    private void findNeededRDFDocuments(Vector<String> rdfDocs) {
        TupleQueryResult result = null;
        List<String> bindingNames;
        RepositoryConnection con = null;
        try {
            con = owlse2.getConnection();

            String queryString = "(SELECT doc "
                    + "FROM {doc} rdf:type {rdfcache:StartingPoint} "
                    + "union "
                    + "SELECT doc "
                    + "FROM {tp} rdf:type {rdfcache:StartingPoint}; rdfcache:dependsOn {doc}) "
                    + "MINUS "
                    + "SELECT doc "
                    + "FROM CONTEXT rdfcache:cachecontext {doc} rdfcache:last_modified {lastmod} "
                    + "USING NAMESPACE "
                    + "rdfcache = <"+ RepositoryUtility.rdfCacheNamespace+">";

            log.debug("queryNeededRDFDocuments: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                
                Value firstValue = bindingSet.getValue("doc");
                String doc = firstValue.stringValue();

                if (!rdfDocs.contains(doc) && !imports.contains(doc)
                        && !downService.containsValue(doc)
                        && doc.startsWith("http://")) {
                    rdfDocs.add(doc);

                    log.debug("Adding to rdfDocs: " + doc);
                }
            }

        } catch (QueryEvaluationException e) {
            log.error("Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        finally {
            if (result != null) {
                try {
                    result.close();
                } catch (QueryEvaluationException e) {
                    log.error("Caught a QueryEvaluationException! Msg: "
                            + e.getMessage());
                }
            }

            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("Caught a RepositoryException! in findNeededRDFDocuments() Msg: "
                                + e.getMessage());
            }
        }

        log.info("Number of needed files identified:  "
                + rdfDocs.size());

    }

    /*
     * Drop URIs in the drop list
     */
    // private void dropContexts(RepositoryConnection con) {
    public  void dropContexts(Vector<String> dropList) {
        RepositoryConnection con = null;

        log.debug("Dropping changed RDFDocuments and external inferencing contexts...");

        try {
            con = owlse2.getConnection();

            Thread thread = Thread.currentThread();

            URI uriDrop = null;
            log.info("Deleting contexts in drop list ...");

            ValueFactory f = owlse2.getValueFactory();

            for (String drop : dropList) {
                uriDrop = new URIImpl(drop);
                log.info("Dropping URI: " + drop);
                String pred =  RepositoryUtility.internalStartingPoint +"#last_modified";
                String contURL = RepositoryUtility.internalStartingPoint + "#cachecontext";
                URI sbj = f.createURI(drop);
                URI predicate = f.createURI(pred);
                URI cont = f.createURI(contURL);
                
                log.info("Removing context: " + sbj);

                con.remove((Resource) null, null, null, (Resource) sbj);

                log.info("Removing last_modified: " + sbj);
                con.remove(sbj, predicate, null, cont); // remove last_modified

                log.info("Finished removing context: " + sbj);

               

            }
            if (thread.isInterrupted()) {
                log.warn("dropContexts(): WARNING! Thread "
                        + thread.getName() + " was interrupted!");
                return;
            }

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        }
        finally {
            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("Caught RepositoryException! while closing connection: "
                                + e.getMessage());
            }
        }
        log.debug("Finished dropping changed RDFDocuments and external inferencing contexts.");

    }

    /**
     * Locate all of the of the contexts generated by externbal inferencing (construct rule) activities.
     * @return  A lists of contexts that were generated by construct rules (i.e. external inferencing)
     */
    private Vector<String> findExternalInferencingContexts() {
        RepositoryConnection con = null;
        TupleQueryResult result = null;

        List<String> bindingNames;
        Vector<String> externalInferencing = new Vector<String>();

        log.debug("Finding ExternalInferencing ...");
       
        try {
            con = owlse2.getConnection();
            
            String queryString = "select distinct crule from context crule {} prop {} "
                    + "WHERE crule != rdfcache:cachecontext AND crule != rdfcache:startingPoints AND NOT EXISTS (SELECT time FROM CONTEXT rdfcache:cachecontext "
                    + "{crule} rdfcache:last_modified {time}) "
                    + "using namespace "
                    + "rdfcache = <"+ RepositoryUtility.rdfCacheNamespace+">";

            log.debug("queryString: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = (BindingSet) result.next();

                    Value firstValue = bindingSet.getValue("crule");
                    if (!externalInferencing.contains(firstValue.stringValue())) {
                        externalInferencing.add(firstValue.stringValue());
                    }
                    log.debug("Adding to droplist: " + firstValue.toString());
                }
            } else {
                log.debug("No construct rule found!");
            }
        } catch (QueryEvaluationException e) {
            log.error("Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        finally {
            if (result != null) {
                try {
                    result.close();
                } catch (QueryEvaluationException e) {
                    log.error("Caught a QueryEvaluationException! Msg: "
                            + e.getMessage());
                }
            }
            try {
                con.close();
            } catch (RepositoryException e) {
                log
                        .error("Caught RepositoryException! in dropExternalInferencing() Msg: "
                                + e.getMessage());
            }

        }

        log.info("Located "
                + externalInferencing.size()+ " context generated by external inferencing (construct rules).");


        return externalInferencing;

    }

    /*
     * Find all rdfcache:RDFDocuments that are not needed and do not belong to
     * rdfcache:StartingPoints and add them to the drop-list
     */
    private Vector<String> findUnneededRDFDocuments(RepositoryConnection con) {
        TupleQueryResult result = null;
        List<String> bindingNames;
        Vector<String>  unneededRdfDocs = new Vector<String>();



                
        log.debug("Locating unneeded RDF files left over from last update ...");

        try {

            String queryString = "(SELECT doc "
                    + "FROM CONTEXT rdfcache:cachecontext "
                    + "{doc} rdfcache:last_modified {lmt} "
                    + "MINUS "
                    + "SELECT doc "
                    + "FROM {doc} rdf:type {rdfcache:StartingPoint}) "
                    + "MINUS "
                    + "SELECT doc "
                    + "FROM {tp} rdf:type {rdfcache:StartingPoint}; rdfcache:dependsOn {doc} "
                    + "USING NAMESPACE "
                    + "rdfcache = <"+ RepositoryUtility.rdfCacheNamespace+">";

            log.debug("queryUnneededRDFDocuments: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = (BindingSet) result.next();

                    Value firstValue = bindingSet.getValue("doc");
                    if (!unneededRdfDocs.contains(firstValue.stringValue())) {
                        unneededRdfDocs.add(firstValue.stringValue());

                        log.debug("Found unneeded RDF Document: "
                                + firstValue.toString());
                    }
                }
            } else {
                log.debug("No query result!");
            }
        } catch (QueryEvaluationException e) {
            log.error("Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        finally {
            if (result != null) {
                try {
                    result.close();
                } catch (QueryEvaluationException e) {
                    log.error("Caught a QueryEvaluationException! Msg: "
                            + e.getMessage());
                }
            }

        }

        log.info("Identified " + unneededRdfDocs.size()+ " unneeded RDF documents.");
        return unneededRdfDocs;

    }

    /*
     * Find all rdfcache:RDFDocuments that has changed and add them to the
     * drop-list
     */
    private Vector<String> findChangedRDFDocuments(RepositoryConnection con) {
        TupleQueryResult result = null;
        List<String> bindingNames;
        Vector<String> changedRdfDocuments = new Vector<String>();

        log.debug("Locating changeded files ...");

        try {
            String queryString = "SELECT doc,lastmod "
                    + "FROM CONTEXT rdfcache:cachecontext "
                    + "{doc} rdfcache:last_modified {lastmod} "
                    + "USING NAMESPACE "
                    + "rdfcache = <"+ RepositoryUtility.rdfCacheNamespace+">";

            log.debug("queryChangedRDFDocuments: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = (BindingSet) result.next();

                    Value firstValue = bindingSet.getValue("doc");
                    String importURL = firstValue.stringValue();
                    // Value secondtValue = bindingSet.getValue("lastmod");
                    // log.debug("DOC: " + importURL);
                    // log.debug("LASTMOD: " + secondtValue.stringValue());
                    
                    if (owlse2.olderContext(importURL) && !changedRdfDocuments.contains(importURL)) {
                        
                            changedRdfDocuments.add(importURL);

                            log.debug("Found changed RDF document: " + importURL);
                       
                    }
                }
            } else {
                log.debug("No query result!");
            }
        } catch (QueryEvaluationException e) {
            log.error("Caught an QueryEvaluationException! Msg: "
                    + e.getMessage());

        } catch (RepositoryException e) {
            log.error("Caught RepositoryException! Msg: " + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("Caught MalformedQueryException! Msg: " + e.getMessage());
        }

        finally {
            if (result != null) {
                try {
                    result.close();
                } catch (QueryEvaluationException e) {
                    log.error("Caught a QueryEvaluationException! Msg: "
                            + e.getMessage());
                }
            }

        }

        log.info("Number of changed RDF documents detected:  "
                + changedRdfDocuments.size());

        return changedRdfDocuments;
    }
    
    
    
    
    
    
    
    
    /*******************************************************/
    /*******************************************************/
    
    public void loadWcsCatalogFromRepository() throws InterruptedException, RepositoryException {
        long startTime, endTime;
        double elapsedTime;
        log.info("#############################################");
        log.info("#############################################");
        log.info("Loading WCS Catalog from Semantic Repository.");
        startTime = new Date().getTime();
        setupRepository();
        
        extractCoverageDescrptionsFromRepository();
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
            updateCatalog2();
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
    private void shutdownRepository() throws RepositoryException {

        log.debug("shutdownRepository)(): Shutting down Repository...");
            owlse2.shutDown();
        log.debug("shutdownRepository(): Repository shutdown complete.");
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
        owlse2.startup(); //needed
        log.info("Semantic Repository Ready.");

        if(Thread.currentThread().isInterrupted())
            throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");


    }



    private void ingestSwrlRules() throws RepositoryException{
        log.info("Running runConstruct ..");
        owlse2.runConstruct();
        
        String ltmod = owlse2.getLastModifiedTimeString(new Date()); 
        try{
        RepositoryConnection con = owlse2.getConnection();
        String externalInferencing = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#externalInferencing";
        owlse2.setLTMODContext(externalInferencing, ltmod, con);
        
        }
        finally {
            if (con != null)
                con.close();
            log.info("Complete running runConstruct ..");   
        }
        
    }



    private void dumpRepository(RepositoryConnection con, String filename) {

        // export repository to an n-triple file
        File outrps = new File(filename); // hard copy of repository
        try {
            FileOutputStream myFileOutputStream = new FileOutputStream(outrps);
            if (filename.endsWith("nt")) {
              
                NTriplesWriter myNTRiplesWriter = new NTriplesWriter(
                        myFileOutputStream);

                con.export(myNTRiplesWriter);
                myNTRiplesWriter.endRDF();

            }
            if (filename.endsWith("trix")) {
                
                TriXWriter myTriXWriter = new TriXWriter(myFileOutputStream);

                con.export(myTriXWriter);
                myTriXWriter.endRDF();

            }
            if (filename.endsWith("trig")) {
               
                TriGWriter myTriGWriter = new TriGWriter(myFileOutputStream);

                con.export(myTriGWriter);
                myTriGWriter.endRDF();

            }
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

        // Next we update the Repositories cached maps of of datasetUrl/serverIDs datasetUrl/wcsID
        // This bothers me: Before we moved these HashMaps int the IRISailRepository it was a general purpose class for
        // twiddling with Semantics - no it's a specialization of that general class for WCS. 
        //owlse2.updateIdCaches();



        try {
            HashMap<String, Vector<String>> coverageIdToServerMap =  getCoverageIDServerURL();
            CoverageIdGenerator.updateIdCaches(coverageIdToServerMap);
        } catch (RepositoryException e) {
            log.error("getCoverageIDServerURL(): Caught RepositoryException. msg: "
                    + e.getMessage());
        } catch (MalformedQueryException e) {
            log.error("getCoverageIDServerURL(): Caught MalformedQueryException. msg: "
                    + e.getMessage());
        } catch (QueryEvaluationException e) {
            log.error("getCoverageIDServerURL(): Caught QueryEvaluationException. msg: "
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
            log.info("destroy(): Attempting to shutdown Semantic Repository.");
            shutdownRepository();
            log.info("destroy(): Semantic Repository Has been shutdown.");

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

            log.debug("<wcs:Identifier>"+CoverageIdGenerator.getWcsIdString(datasetURL)+"</wcs:Identifier>");
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

    public String getLatitudeCoordinateDapId(String coverageId, String fieldId) {
        String qString = createQuery("A_1D_latitude", fieldId);
        String coordinateDapId = runQuery(qString);
        return coordinateDapId;
        
    }

    public String getLongitudeCoordinateDapId(String coverageId, String fieldId) {
        String qString = createQuery("A_1D_longitude", fieldId);
        String coordinateDapId = runQuery(qString);
        return coordinateDapId;
        
    }

    public String getElevationCoordinateDapId(String coverageId, String fieldId) {
        String qString = createQuery("A_elevation", fieldId);
        String coordinateDapId = runQuery(qString);
        return coordinateDapId;
        
    }

    public String getTimeCoordinateDapId(String coverageId, String fieldId) {
        String qString = createQuery("A_time", fieldId);
        String coordinateDapId = runQuery(qString);
        return coordinateDapId;
    }
    private String runQuery(String qString){
        RepositoryConnection con;
        String coordinateDapId = null; 
        try {
            con = owlse2.getConnection();
        
        TupleQueryResult result = null;
        List<String> bindingNames;
        
        TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,qString);

        result = tupleQuery.evaluate();
        
        if (result != null) {
            bindingNames = result.getBindingNames();

            while (result.hasNext()) {
                BindingSet bindingSet = (BindingSet) result.next();

                Value firstValue = bindingSet.getValue("cid");
                coordinateDapId = firstValue.stringValue();
            }
        } else {
            log.debug("No query result!");

        } 
        } catch (RepositoryException e) {
            log.error("getTimeCoordinateDapId(String coverageId, String fieldId) has a problem: " +
                    e.getMessage()) ;
            e.printStackTrace(); 
        } catch (MalformedQueryException e) {
            log.error("getTimeCoordinateDapId(String coverageId, String fieldId) has a problem: " +
                    e.getMessage()) ;
            e.printStackTrace();
        } catch (QueryEvaluationException e) {
            log.error("getTimeCoordinateDapId(String coverageId, String fieldId) has a problem: " +
                    e.getMessage()) ;
            e.printStackTrace();
        }
        return coordinateDapId;   
    }
    private String createQuery(String A_time, String fieldStr){
        String qString = "select cid FROM {" 
            + fieldStr + "} ncobj:hasCoordinate {cid} rdf:type {cfobj:"
            + A_time  + "} WHERE field=<" +fieldStr + "> "
            + "USING NAMESPACE "
            + "wcs=<http://www.opengis.net/wcs/1.1#>, "
            + "ncobj=<http://iridl.ldeo.columbia.edu/ontologies/netcdf-obj.owl#>, "
            + "cfobj=<http://iridl.ldeo.columbia.edu/ontologies/cf-obj.owl#>";
              
        return qString ;
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

        if(thread.isInterrupted()) {
            log.warn("updateCatalog(): WARNING! Thread "+thread.getName()+" was interrupted!");
            throw new InterruptedException();
        }

    }





    public void updateCatalog2()  throws RepositoryException, InterruptedException{

        setupRepository();
        extractCoverageDescrptionsFromRepository();
        try {
            if (updateRepository2())
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
        log.debug("QueryString (coverage ID and server URL): \n" + queryString);
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



    
    public boolean updateRepository2() throws InterruptedException {

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


                    log.debug("updateRepository2(): Updating Semantic Repository.");


                    //log.debug("updateRepository2(): Connecting to Repository...");
                    //RepositoryConnection con = owlse2.getConnection();
                    //log.info("updateRepository2(): Repository connection has been opened.");
                    //if(thread.isInterrupted() || stopWorking ){
                    //    log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
                    //    throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    //}


                    log.debug("updateRepository2(): Getting RDF imports.");
                    Vector<String> importURLs = getRdfImports(_config);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }

                    log.debug("updateRepository2(): Updating semantic repository.");
                    updateSemanticRepository2(importURLs);
                    //updateSemanticRepository3(importURLs);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }
                    
                    log.debug("updateRepository2(): Connecting to Repository...");
                    RepositoryConnection con = owlse2.getConnection();
                    String filename = catalogCacheDirectory + "daprepository.nt";
                    log.debug("updateRepository2(): Dumping Semantic Repository to: "+filename);
                    dumpRepository(con, filename);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }
                    log.debug("updateRepository2(): Dumping Semantic Repository to: "+filename);
                    filename = catalogCacheDirectory + "daprepository.trig";
                    dumpRepository(con, filename);
                    if(thread.isInterrupted() || stopWorking){
                        log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
                        throw new InterruptedException("Thread.currentThread.isInterrupted() returned 'true'.");
                    }
                    log.debug("Extracting CoverageDescriptions from the Repository.");
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

                    success = true;
                    repositoryUpdateActive.set(false);

                }
            }
            catch (InterruptedException e){

                throw e;
            }
            catch (Exception e) {
                log.error("updateRepository2() has a problem. Error Message: '" +
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
            log.warn("updateRepository2(): WARNING! Thread "+thread.getName()+" was interrupted!");
            throw new InterruptedException();
        }

        log.debug("updateRepository2() returning: " + success);
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
                        updateCatalog2();
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

            if(identifierElem!=null){
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
            else {
                log.error("Failed to locate wcs:Identifier element for Coverage!");
                //@todo Throw an exception (what kind??) here!!
            }
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


}