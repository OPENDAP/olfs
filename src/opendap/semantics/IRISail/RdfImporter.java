package opendap.semantics.IRISail;

import net.sf.saxon.s9api.SaxonApiException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 5, 2010
 * Time: 12:05:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class RdfImporter {

    private Logger log;


    private HashMap<String, Boolean> downService;
    private Vector<String> imports;


    public RdfImporter() {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        downService = new HashMap<String, Boolean>();
        imports = new Vector<String>();
    }

    public void reset(){
        downService.clear();
        imports.clear();
    }

    /*******************************************
     * Update repository
     */
    public boolean importReferencedRdfDocs(IRISailRepository repository) {

        boolean repositoryChanged = false;

        Vector<String> rdfDocList = new Vector<String>();

        findNeededRDFDocuments(repository, rdfDocList);

        while (!rdfDocList.isEmpty()) {
            repositoryChanged = true;

            addNeededRDFDocuments(repository, rdfDocList);

            findNeededRDFDocuments(repository, rdfDocList);
        }

        return repositoryChanged;
    }



    /**
     * Find all rdfcache:RDFDocuments that are referenced by existing documents in the repository.
     * @param repository
     * @param rdfDocs
     */
    private void findNeededRDFDocuments(IRISailRepository repository, Vector<String> rdfDocs) {
        TupleQueryResult result = null;
        List<String> bindingNames;
        RepositoryConnection con = null;
        try {
            con = repository.getConnection();

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





    /**
     *  Add the each of the RDF documents whose URL's are in the passed Vector to the Repository.
     * @param repository
     * @param rdfDocs
     */
    private void addNeededRDFDocuments(IRISailRepository repository, Vector<String> rdfDocs) {
        URI uriaddress;
        long inferStartTime, inferEndTime;
        inferStartTime = new Date().getTime();

        String importURL = "";
        RepositoryConnection con = null;
        int notimport = 0;
        try {
            con = repository.getConnection();

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
                    log.error("Server error, Skipping " + importURL);
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
                        repository.setLTMODContext(importURL, con); // set last modified
                                                                // time of the context

                        log.info("Finished importing URL " + url);

                    } else if (importURL.substring((importURL.length() - 4),
                            importURL.length()).equals(".xsd")) {

                        uriaddress = new URIImpl(importURL);

                        ByteArrayInputStream inStream;
                        log.info("Transforming URL " + importURL);
                        inStream = new ByteArrayInputStream(repository
                                .transformXSD(importURL).toByteArray());
                        log.info("Finished transforming URL " + importURL);
                        log.debug("Importing URL " + importURL);
                        con.add(inStream, importURL, RDFFormat.RDFXML,
                                (Resource) uriaddress);
                        repository.setLTMODContext(importURL, con); // set last modified
                                                                // time for the context
                        repository.setContentTypeContext(importURL,contentType, con); //
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
                            repository.setLTMODContext(importURL, con);
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






}
