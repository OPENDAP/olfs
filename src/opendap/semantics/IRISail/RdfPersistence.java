package opendap.semantics.IRISail;

import org.slf4j.Logger;

import java.util.*;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class RdfPersistence {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(RdfPersistence.class);


    /**
     * @param repository        The repository on which to operate.
     * @param startingPointUrls The list pof starting point URLs from the configuration file (aka "THE starting point")
     * @return Returns true if the update results in changes to the repository.
     * @throws InterruptedException If the thread of execution is interrupted.
     * @throws RepositoryException  When there are problems working with the repository.
     */
    public static boolean updateSemanticRepository(IRISailRepository repository, Vector<String> startingPointUrls)
            throws InterruptedException, RepositoryException {


        Vector<String> dropList = new Vector<String>();
        Vector<String> startingPoints = new Vector<String>();
        Vector<String> newStartingPoints = new Vector<String>();
        Vector<String> startingPointsToDrop = null;
        boolean repositoryHasBeenChanged = false;

        RdfImporter rdfImporter = new RdfImporter();


        Date startTime = new Date();
        log.info("-----------------------------------------------------------------------");
        log.info("updateSemanticRepository() Start.");
        log.debug(RepositoryUtility.showContexts(repository));
        RepositoryConnection con = null;
        try {


            for (String startingPointUrl : startingPointUrls) {
                startingPoints.add(startingPointUrl); // starting point from input file
            }

            try {
                con = repository.getConnection();
                if (con.isOpen()) {
                    log.info("Connection is OPEN!");


                    newStartingPoints = RepositoryUtility.findNewStartingPoints(con, startingPoints);

                    dropList.addAll(findUnneededRDFDocuments(con));
                    startingPointsToDrop = RepositoryUtility.findChangedStartingPoints(con, startingPoints);
                    dropList.addAll(startingPointsToDrop);
                    dropList.addAll(findChangedRDFDocuments(con));
                }
            } catch (RepositoryException e) {
                log.error("Caught RepositoryException updateSemanticRepository(Vector<String> startingPointUrls)" +
                        e.getMessage());
            } finally {
                if (con != null)
                    con.close();
                log.info("Connection is Closed!");
            }

            log.debug(RepositoryUtility.showContexts(repository));

            boolean modelChanged = false;
            if (!dropList.isEmpty()) {

                log.debug("Add external inferencing contexts to dropList");
                dropList.addAll(findExternalInferencingContexts(repository));

                log.debug("Dropping starting points ...");
                RepositoryUtility.dropStartingPoints(repository, startingPointsToDrop);
                log.debug("Finished dropping starting points.");

                log.debug(RepositoryUtility.showContexts(repository));

                log.debug("Dropping contexts.");
                dropContexts(repository, dropList);
                log.debug(RepositoryUtility.showContexts(repository));

                modelChanged = true;

            }
            if (!newStartingPoints.isEmpty()) {

                log.debug("Adding new starting point ...");
                RepositoryUtility.addStartingPoints(repository, newStartingPoints);
                log.debug("Finished adding new starting point.");

                log.debug(RepositoryUtility.showContexts(repository));
                modelChanged = true;

            }

            log.debug("Checking for referenced documents that are not already in the repository.");
            boolean foundNewDocuments = rdfImporter.importReferencedRdfDocs(repository);
            if(foundNewDocuments){
                modelChanged = true;
            }

            if (modelChanged) {

                log.debug("Updating repository ...");

                while (modelChanged) {
                    log.debug("Repository changes detected.");
                    log.debug(RepositoryUtility.showContexts(repository));

                    log.debug("Running construct rules ...");
                    repository.runConstruct();
                    log.debug("Finished running construct rules.");
                    log.debug(RepositoryUtility.showContexts(repository));
                    modelChanged = rdfImporter.importReferencedRdfDocs(repository);
                }


                repositoryHasBeenChanged = true;

            } else {
                log.debug("Repository update complete. No changes detected, rules not rerun..");
                log.debug(RepositoryUtility.showContexts(repository));

            }


        } catch (RepositoryException e) {
            log.error("Caught RepositoryException in main(): "
                    + e.getMessage());

        }


        long elapsedTime = new Date().getTime() - startTime.getTime();
        log.info("Imports Evaluated. Elapsed time: " + elapsedTime + "ms");

        log.info("updateSemanticRepository() End.");
        log.info("-----------------------------------------------------------------------");


        return repositoryHasBeenChanged;
    }

    /*
    * Drop URIs in the drop list
    */
    // private void dropContexts(RepositoryConnection con) {

    public static void dropContexts(IRISailRepository repository, Vector<String> dropList) {
        RepositoryConnection con = null;

        log.debug("Dropping changed RDFDocuments and external inferencing contexts...");

        try {
            con = repository.getConnection();

            Thread thread = Thread.currentThread();

            log.info("Deleting contexts in drop list ...");
            ValueFactory valueFactory = repository.getValueFactory();

            for (String drop : dropList) {
                log.info("Dropping URI: " + drop);
                String pred = RepositoryUtility.internalStartingPoint + "#last_modified";
                String contURL = RepositoryUtility.internalStartingPoint + "#cachecontext";
                URI sbj = valueFactory.createURI(drop);
                URI predicate = valueFactory.createURI(pred);
                URI cont = valueFactory.createURI(contURL);

                log.info("Removing context: " + sbj);
                con.remove((Resource) null, null, null, (Resource) sbj);
                con.remove(sbj, null, null);

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
                if (con != null)
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
     *
     * @param repository The repository to operate on.
     * @return A lists of contexts that were generated by construct rules (i.e. external inferencing)
     */
    private static Vector<String> findExternalInferencingContexts(IRISailRepository repository) {
        RepositoryConnection con = null;
        TupleQueryResult result = null;

        //List<String> bindingNames;
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
                    + "rdfcache = <" + RepositoryUtility.rdfCacheNamespace + ">";

            log.debug("queryString: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                //bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();

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
                + externalInferencing.size() + " context generated by external inferencing (construct rules).");


        return externalInferencing;

    }


    /*
    * Find all rdfcache:RDFDocuments that are not needed and do not belong to
    * rdfcache:StartingPoints and add them to the drop-list
    */

    private static Vector<String> findUnneededRDFDocuments(RepositoryConnection con) {
        TupleQueryResult result = null;
        //List<String> bindingNames;
        Vector<String> unneededRdfDocs = new Vector<String>();


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
                    + "rdfcache = <" + RepositoryUtility.rdfCacheNamespace + ">";

            log.debug("queryUnneededRDFDocuments: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                //bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();

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

        log.info("Identified " + unneededRdfDocs.size() + " unneeded RDF documents.");
        return unneededRdfDocs;

    }


    /*
    * Find all rdfcache:RDFDocuments that has changed and add them to the
    * drop-list.
    * @param repository The repository on which to operate.
    */

    private static Vector<String> findChangedRDFDocuments(RepositoryConnection con) {
        TupleQueryResult result = null;
        //List<String> bindingNames;
        Vector<String> changedRdfDocuments = new Vector<String>();

        log.debug("Locating changeded files ...");

        try {
            String queryString = "SELECT doc,lastmod "
                    + "FROM CONTEXT rdfcache:cachecontext "
                    + "{doc} rdfcache:last_modified {lastmod} "
                    + "USING NAMESPACE "
                    + "rdfcache = <" + RepositoryUtility.rdfCacheNamespace + ">";

            log.debug("queryChangedRDFDocuments: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            if (result != null) {
                //bindingNames = result.getBindingNames();

                while (result.hasNext()) {
                    BindingSet bindingSet = result.next();

                    Value firstValue = bindingSet.getValue("doc");
                    String importURL = firstValue.stringValue();
                    // Value secondtValue = bindingSet.getValue("lastmod");
                    // log.debug("DOC: " + importURL);
                    // log.debug("LASTMOD: " + secondtValue.stringValue());

                    if (RepositoryUtility.olderContext(con, importURL) && !changedRdfDocuments.contains(importURL)) {

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


