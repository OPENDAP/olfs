package opendap.semantics.IRISail;
import net.sf.saxon.s9api.SaxonApiException;
import org.slf4j.Logger;
import java.util.*;
import java.io.ByteArrayInputStream;
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
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

public class RdfPersistence {
   
        private static Logger log = org.slf4j.LoggerFactory.getLogger(RdfPersistence.class);
        


        public static void updateSemanticRepository(IRISailRepository repository, Vector<String> startingPointUrls)
        throws InterruptedException,RepositoryException {

            Vector<String> dropList = new Vector<String>();
            Vector<String> startingPoints = new Vector<String>();
            boolean isNewRepository = true;

            RdfImporter rdfImporter = new RdfImporter();


            Date startTime = new Date();
            log.info("-----------------------------------------------------------------------");
            log.info("updateSemanticRepository() Start.");
            log.debug(RepositoryUtility.showContexts(repository));
            RepositoryConnection con = null;
            try {


                for (String startingPointUrl : startingPointUrls){
                    startingPoints.add(startingPointUrl); // startingpoint from input file
                }

                Vector<String> newStartingPoints = null;
                Vector<String> startingPointsToDrop = null;
                try {
                    con = repository.getConnection();
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
                  log.error("Caught RepositoryException updateSemanticRepository(Vector<String> startingPointUrls)" +
                          e.getMessage());
                }
                catch (QueryEvaluationException e) {
                    log.error("Caught QueryEvaluationException updateSemanticRepository(Vector<String> startingPointUrls)" +
                            e.getMessage());
                    
                } catch (MalformedQueryException e) {
                    log.error("Caught MalformedQueryException updateSemanticRepository(Vector<String> startingPointUrls)" +
                            e.getMessage());  
                } finally {
                    if (con != null)
                        con.close();
                    log.info("Connection is Closed!");
                }

                log.debug(RepositoryUtility.showContexts(repository));

                if (isNewRepository) {

                    try {
                        RepositoryUtility.addInternalStartingPoint(repository);
                        con = repository.getConnection();
                        RepositoryUtility.addStartingPoints(con, repository.getValueFactory(), newStartingPoints);
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
                        repository.runConstruct();
                        log.debug("Finished running construct rules.");
                        modelChanged = rdfImporter.importReferencedRdfDocs(repository);
                    }
                    //} else{
                    //    log.debug("Repository update complete. No changes detected, rules not rerun..");

                    //}
                    log.debug(RepositoryUtility.showContexts(repository));

                } else {
                    if (!dropList.isEmpty()) {

                        dropList.addAll(findExternalInferencingContexts(repository));
                        try {
                            con = repository.getConnection();
                            log.debug("Dropping starting points ...");
                            RepositoryUtility.dropStartingPoints(con, repository.getValueFactory(), startingPointsToDrop);
                        }
                        finally {
                            if (con != null)
                                con.close();
                        }
                        log.debug("Finished dropping starting point.");
                        log.debug(RepositoryUtility.showContexts(repository));

                        dropContexts(repository,dropList);
                        log.debug(RepositoryUtility.showContexts(repository));

                    }
                    if (!newStartingPoints.isEmpty()) {

                        try {
                            con = repository.getConnection();
                            log.debug("Adding new starting point ...");
                            RepositoryUtility.addStartingPoints(con, repository.getValueFactory(), newStartingPoints);
                            log.debug("Finished adding nrew starting point.");
                        }
                        finally {
                            if (con != null)
                                con.close();
                        }
                        log.debug(RepositoryUtility.showContexts(repository));

                    }
                    log.debug("Updating repository ...");
                    boolean modelChanged = rdfImporter.importReferencedRdfDocs(repository);
                    
                    if(modelChanged || !dropList.isEmpty()) {
                        log.debug("Running construct rules ...");
                        repository.runConstruct();
                        log.debug("Finished running construct rules.");
                        modelChanged = rdfImporter.importReferencedRdfDocs(repository);
                        while(modelChanged){
                            log.debug(RepositoryUtility.showContexts(repository));

                            log.debug("Repository update complete. Changes detected.");

                            log.debug("Running construct rules ...");
                            repository.runConstruct();
                            log.debug("Finished running construct rules.");
                            log.debug(RepositoryUtility.showContexts(repository));
                            modelChanged = rdfImporter.importReferencedRdfDocs(repository);
                        }

                    } else{
                        log.debug("Repository update complete. No changes detected, rules not rerun..");
                        log.debug(RepositoryUtility.showContexts(repository));

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

        /*
         * Drop URIs in the drop list
         */
        // private void dropContexts(RepositoryConnection con) {
        public  static void dropContexts(IRISailRepository repository, Vector<String> dropList) {
            RepositoryConnection con = null;

            log.debug("Dropping changed RDFDocuments and external inferencing contexts...");

            try {
                con = repository.getConnection();

                Thread thread = Thread.currentThread();

                URI uriDrop = null;
                log.info("Deleting contexts in drop list ...");



                ValueFactory f = repository.getValueFactory();

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
        private static Vector<String> findExternalInferencingContexts(IRISailRepository repository) {
            RepositoryConnection con = null;
            TupleQueryResult result = null;

            List<String> bindingNames;
            Vector<String> externalInferencing = new Vector<String>();

            log.debug("Finding ExternalInferencing ...");
           
            try {
                con = repository.getConnection();
                
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
         * Find all rdfcache:RDFDocuments that are not needed and do not belong to
         * rdfcache:StartingPoints and add them to the drop-list
         */
        private static Vector<String> findUnneededRDFDocuments(RepositoryConnection con) {
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
        private static Vector<String> findChangedRDFDocuments(RepositoryConnection con) {
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
                        
                        if (RepositoryUtility.olderContext(con,importURL) && !changedRdfDocuments.contains(importURL)) {
                            
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

        


    }


