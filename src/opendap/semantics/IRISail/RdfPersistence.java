package iri.generatentriples;
import net.sf.saxon.s9api.SaxonApiException;
import org.jdom.Element;
import org.slf4j.Logger;
import java.util.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.List;
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
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;

public class RdfPersistence {
   
        private Logger log;

        private IRISailRepository owlse2;
        
        
       
        private Vector<String> repositoryContexts;
     
        private HashMap<String, Boolean> downService;
       
        private Vector<String> imports;
        
       
        public RdfPersistence(IRISailRepository repository) {
            log = org.slf4j.LoggerFactory.getLogger(this.getClass());
           
            owlse2 = repository;
            repositoryContexts = new Vector<String>();
                        
            downService = new HashMap<String, Boolean>();
            imports = new Vector<String>();
            
         
        }


        public void updateSemanticRepository(Vector<String> importURLs)
        throws InterruptedException,RepositoryException {

            Vector<String> dropList = new Vector<String>();
            Vector<String> startingPoints = new Vector<String>();
            boolean isNewRepository = true;


            Date startTime = new Date();
            log.info("-----------------------------------------------------------------------");
            log.info("updateSemanticRepository() Start.");
            log.debug(RepositoryUtility.showContexts(owlse2));
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
                  log.error("Caught RepositoryException updateSemanticRepository(Vector<String> importURLs)" +
                          e.getMessage());
                }
                catch (QueryEvaluationException e) {
                    log.error("Caught QueryEvaluationException updateSemanticRepository(Vector<String> importURLs)" +
                            e.getMessage());
                    
                } catch (MalformedQueryException e) {
                    log.error("Caught MalformedQueryException updateSemanticRepository(Vector<String> importURLs)" +
                            e.getMessage());  
                } finally {
                    if (con != null)
                        con.close();
                    log.info("Connection is Closed!");
                }

                log.debug(RepositoryUtility.showContexts(owlse2));

                if (isNewRepository) {

                    try {
                        RepositoryUtility.addInternalStartingPoint(owlse2);
                        con = owlse2.getConnection();
                        RepositoryUtility.addStartingPoints(con, owlse2.getValueFactory(), newStartingPoints);
                    }
                    finally {
                        if (con != null)
                            con.close();
                    }

                    log.debug("Updating repository ...");
                    boolean modelChanged = true;
                    //if(updateIriRepository()){
                    while(modelChanged){
                        log.debug("Repository update complete. Changes detected.");

                        log.debug("Running construct rules ...");
                        ingestSwrlRules();
                        log.debug("Finished running construct rules.");
                        modelChanged = updateIriRepository();
                    }
                    //} else{
                    //    log.debug("Repository update complete. No changes detected, rules not rerun..");

                    //}
                    log.debug(RepositoryUtility.showContexts(owlse2));

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
                        log.debug(RepositoryUtility.showContexts(owlse2));

                        dropContexts(dropList);
                        log.debug(RepositoryUtility.showContexts(owlse2));

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
                        log.debug(RepositoryUtility.showContexts(owlse2));

                    }
                    log.debug("Updating repository ...");
                    boolean modelChanged = updateIriRepository();
                    
                    if(modelChanged || !dropList.isEmpty()) {
                        log.debug("Running construct rules ...");
                        ingestSwrlRules();
                        log.debug("Finished running construct rules.");
                        modelChanged = updateIriRepository();
                        while(modelChanged){
                            log.debug(RepositoryUtility.showContexts(owlse2));

                            log.debug("Repository update complete. Changes detected.");

                            log.debug("Running construct rules ...");
                            ingestSwrlRules();
                            log.debug("Finished running construct rules.");
                            log.debug(RepositoryUtility.showContexts(owlse2));
                            modelChanged = updateIriRepository();
                        }

                    } else{
                        log.debug("Repository update complete. No changes detected, rules not rerun..");
                        log.debug(RepositoryUtility.showContexts(owlse2));

                    }


                }

            } catch (RepositoryException e) {
                log.error("Caught RepositoryException in main(): "
                        + e.getMessage());
                
            }


            long elapsedTime = new Date().getTime() - startTime.getTime();
            log.info("Imports Evaluated. Elapsed time: " + elapsedTime + "ms");

            log.info("updateSemanticRepository2() End.");
            log.info("-----------------------------------------------------------------------");
            

        }
        
        /*******************************************
         * Update repository
         */
        private boolean updateIriRepository() {

            boolean repositoryChanged = false;

            Vector<String> rdfDocList = new Vector<String>();

            findNeededRDFDocuments(rdfDocList);

            while (!rdfDocList.isEmpty()) {
                repositoryChanged = true;

                addNeededRDFDocuments(rdfDocList);

                findNeededRDFDocuments(rdfDocList);
            }

            return repositoryChanged;
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
                        URL urlImport = new URL(importURL);
                        URLConnection urlc = urlImport.openConnection();
                        urlc.connect();
                        String contentType = urlc.getContentType();
                        //@todo make this a more robust
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
                            owlse2.setContentTypeContext(importURL,contentType, con); //
                            log.debug("Finished importing URL " + importURL);

                        } else {
                            notimport++;
                                                        
                            //urlc.setRequestProperty("Accept",
                            //                "application/rdf+xml,application/xml,text/xml,*/*");
                            // urlc.setRequestProperty("Accept",
                            // "application/rdf+xml, application/xml;
                            // q=0.9,text/xml; q=0.9, */*; q=0.2");
                                                        
                            try {
                                InputStream inStream = urlc.getInputStream();

                                uriaddress = new URIImpl(importURL);
                            if (contentType.equalsIgnoreCase("text/xml")||contentType.equalsIgnoreCase("application/xml")
                                || contentType.equalsIgnoreCase("application/rdf+xml"))   {
                                con.add(inStream, importURL, RDFFormat.RDFXML,
                                        (Resource) uriaddress);
                                log.info("Imported non owl/xsd = " + importURL);
                            }
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
                    imports.add(importURL); //skip this file
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


        private String getContentTypeContext(String urlstring) {
            String contentType = "";
            try {
                URL myurl = new URL(urlstring);
                HttpURLConnection hc = (HttpURLConnection) myurl.openConnection();
                
                contentType = hc.getContentType();
                
            } catch (MalformedURLException e) {
                log.error("Caught a MalformedQueryException! Msg: "
                        + e.getLocalizedMessage());
            } catch (IOException e) {
                log.error("Caught an IOException! Msg: " + e.getMessage(), e);
            }
            return contentType;
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
                    con.remove((Resource) sbj, null, null);

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
                        + "WHERE crule != rdfcache:cachecontext "
                        + "AND crule != rdfcache:startingPoints " 
                        + "AND NOT EXISTS (SELECT time FROM CONTEXT rdfcache:cachecontext "
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
                            log.debug("Adding to external inferencing list: " + firstValue.toString());
                        }
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

                log.debug("Query for NeededRDFDocuments: " + queryString);

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
                        //+ "WHERE doc != <" + RepositoryUtility.rdfCacheNamespace+"externalInferencing> "
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

        

        public void destroy() {

            try {

                log.info("Shutting Down Semantic Repository.");

                // con.close();
                owlse2.shutDown();
                log.info("Semantic Repository Has Been Shutdown.");
            } catch (RepositoryException e) {
                log.error("destroy(): Failed to shutdown Semantic Repository.");
            } finally {

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

                    if (datasetURL.endsWith(".ddx") | datasetURL.endsWith(".dds")
                            | datasetURL.endsWith(".das")) {
                        datasetURL = datasetURL.substring(0, datasetURL
                                .lastIndexOf("."));
                    }
                    datasetURL += ".rdf";
                }
                rdfImports.add(datasetURL);
                log.info("Added dataset reference " + datasetURL
                        + " to RDF imports list.");
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

        private void processContexts(RepositoryConnection con)
                throws RepositoryException {

            // retrieve context
            RepositoryResult<Resource> contextID = con.getContextIDs();
            int contextTol = 0;

            while (contextID != null && contextID.hasNext()) {
                String ctstr = contextID.next().toString();
                log.info("Context: " + ctstr);
                owlse2.printRDFContext(ctstr);
                repositoryContexts.add(ctstr);
                owlse2.printLTMODContext(ctstr);
                contextTol++;

            }

            contextID.close(); // needed to release resources
            log.info("Found  " + contextTol + " Contexts");

        }

 
        private void ingestSwrlRules() throws RepositoryException {
            log.info("Running runConstruct ..");
            owlse2.runConstruct();
            
            log.info("Complete running runConstruct ..");
        }


        public void dropStartingPoints(SailRepository repo, Vector<String> startingPointUrls) {
            RepositoryConnection con = null;
            ValueFactory valueFactory;

            try {
                con = repo.getConnection();
                valueFactory = repo.getValueFactory();
                dropStartingPoints(con, valueFactory, startingPointUrls);
            }
            catch (RepositoryException e) {
                log.error(e.getClass().getName()+": Failed to open repository connection. Msg: "
                        + e.getMessage());
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (RepositoryException e) {
                        log.error(e.getClass().getName()+": Failed to close repository connection. Msg: "
                                + e.getMessage());
                    }
                }
            }


        }
        public  void dropStartingPoints(RepositoryConnection con, ValueFactory valueFactory, Vector<String> startingPointUrls) {

            String pred = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";


            URI startingPointValue = null;
            URI isa = valueFactory.createURI(pred);
            URI context = valueFactory.createURI(RepositoryUtility.rdfCacheNamespace+"startingPoints");
            URI startingPointType = valueFactory.createURI(RepositoryUtility.rdfCacheNamespace+"StartingPoint");
            URL url;

            try {


                for (String importURL : startingPointUrls) {

                    url = new URL(importURL);
                    startingPointValue = valueFactory.createURI(importURL);
                    con.remove((Resource) startingPointValue, isa, (Value) startingPointType, (Resource) context);

                    log.info("Removed starting point " + importURL + " from the repository. (N-Triple: <" + startingPointValue + "> <" + isa
                            + "> " + "<" + startingPointType + "> " + "<" + context + "> )");
                }


            } catch (RepositoryException e) {
                log.error("In addStartingPoints, caught an RepositoryException! Msg: "
                        + e.getMessage());

            } catch (MalformedURLException e) {

                log.error("In addStartingPoints, caught an MalformedURLException! Msg: "
                        + e.getMessage());
                //} catch (RDFParseException e) {
                //    log.error("In addStartingPoints, caught an RDFParseException! Msg: "
                //            + e.getMessage());
            } catch (IOException e) {
                log.error("In addStartingPoints, caught an IOException! Msg: "
                        + e.getMessage());
            }

        }


    }


