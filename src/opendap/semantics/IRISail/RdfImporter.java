package opendap.semantics.IRISail;

import net.sf.saxon.s9api.*;
import org.openrdf.model.*;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 5, 2010
 * Time: 12:05:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class RdfImporter {

    private Logger log;


    private HashSet<String> urlsToBeIgnored;
    private Vector<String> imports;

    private String resourceDir;


    public RdfImporter(String resourceDir) {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        urlsToBeIgnored = new HashSet<String>();
        imports = new Vector<String>();
        this.resourceDir = resourceDir;
    }

    public void reset() {
        urlsToBeIgnored.clear();
        imports.clear();
    }

    /**
     * ****************************************
     * Update repository
     *
     * @param repository
     * @param doNotImportUrls
     * @return
     */
    public boolean importReferencedRdfDocs(Repository repository, Vector<String> doNotImportUrls) {

        boolean repositoryChanged = false;

        Vector<String> rdfDocList = new Vector<String>();

        if (doNotImportUrls != null)
            urlsToBeIgnored.addAll(doNotImportUrls);



        findNeededRDFDocuments(repository, rdfDocList);

        while (!rdfDocList.isEmpty()) {

            if(addNeededRDFDocuments(repository, rdfDocList)){
                repositoryChanged = true;
            }

            rdfDocList.clear();

            findNeededRDFDocuments(repository, rdfDocList);
        }

        return repositoryChanged;
    }


    /**
     * Find all rdfcache:RDFDocuments that are referenced by existing documents in the repository.
     *
     * @param repository
     * @param rdfDocs
     */
    private void findNeededRDFDocuments(Repository repository, Vector<String> rdfDocs) {
        TupleQueryResult result = null;
        List<String> bindingNames;
        RepositoryConnection con = null;

        try {
            con = repository.getConnection();

            String queryString = "(SELECT doc "
                    + "FROM {doc} rdf:type {rdfcache:"+Terms.startingPointType +"} "
                    + "union "
                    + "SELECT doc "
                    + "FROM {tp} rdf:type {rdfcache:"+Terms.startingPointType +"}; rdfcache:"+Terms.dependsOnContext+" {doc}) "
                    + "MINUS "
                    + "SELECT doc "
                    + "FROM CONTEXT "+"rdfcache:"+Terms.cacheContext+" {doc} rdfcache:"+Terms.lastModifiedContext+" {lastmod} "

                    + "USING NAMESPACE "
                    + "rdfcache = <" + Terms.rdfCacheNamespace + ">";

            log.debug("Query for NeededRDFDocuments: " + queryString);

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                Value firstValue = bindingSet.getValue("doc");
                String doc = firstValue.stringValue();

                if (!rdfDocs.contains(doc) && !imports.contains(doc)
                        && !urlsToBeIgnored.contains(doc)
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
     * Add the each of the RDF documents whose URL's are in the passed Vector to the Repository.
     *
     * @param repository
     * @param rdfDocs
     */
    private boolean addNeededRDFDocuments(Repository repository, Vector<String> rdfDocs) {
        URI uriaddress;
        long inferStartTime, inferEndTime;
        inferStartTime = new Date().getTime();

        String importURL = null;
        RepositoryConnection con = null;
        int skipCount = 0;
        String contentType = "";
        HttpURLConnection hc = null;
        InputStream importIS = null;
        boolean addedDocument = false;


        try {
            con = repository.getConnection();


            log.debug("rdfDocs.size=" + rdfDocs.size());
            skipCount = 0;
            while (!rdfDocs.isEmpty()) {
                importURL = rdfDocs.remove(0);

                try {


                    log.debug("Checking import URL: " + importURL);

                    if (urlsToBeIgnored.contains(importURL)) {
                        log.error("Previous server error, Skipping " + importURL);
                    } else {

                        URL myurl = new URL(importURL);


                        int rsCode;
                        hc = (HttpURLConnection) myurl.openConnection();
                        log.debug("Connected to import URL: " + importURL);

                        rsCode = hc.getResponseCode();
                        contentType = hc.getContentType();
                        importIS = hc.getInputStream();

                        log.debug("Got HTTP status code: " + rsCode);
                        log.debug("Got Content Type:     " + contentType);

                        if (rsCode == -1) {
                            log.error("Unable to get an HTTP status code for resource "
                                    + importURL + " WILL NOT IMPORT!");
                            urlsToBeIgnored.add(importURL);

                        } else if (rsCode != 200) {
                            log.error("Error!  HTTP status code " + rsCode + " Skipping importURL " + importURL);
                            urlsToBeIgnored.add(importURL);
                        } else {

                            log.debug("Import URL appears valid ( " + importURL + " )");
                            //@todo make this a more robust

                            if (importURL.endsWith(".owl") || importURL.endsWith(".rdf")) {

                                importUrl(repository, con, importURL, importIS, contentType);

                                addedDocument = true;


                            } else if (importURL.endsWith(".xsd")) {

                                String transformFile = "xsl/xsd2owl.xsl";

                                ByteArrayInputStream inStream;
                                log.info("Transforming  '" + importURL+"' with "+transformFile);
                                inStream = transform(importIS,resourceDir+transformFile);

                                log.info("Finished transforming URL " + importURL);

                                importUrl(repository, con, importURL, inStream, contentType);

                                addedDocument = true;


                            } else if (importURL.endsWith("+psdef/")) {

                                String transformFile = "xsl/RDFa2RDFXML.xsl";

                                ByteArrayInputStream inStream;
                                log.info("Transforming " + importURL+" with "+transformFile);

                                inStream = transform(importIS,resourceDir+transformFile);

                                log.info("Finished transforming RDFa " + importURL);

                                importUrl(repository, con, importURL, inStream, contentType);

                                addedDocument = true;


                            } else {

                                //urlc.setRequestProperty("Accept",
                                //                "application/rdf+xml,application/xml,text/xml,*/*");
                                // urlc.setRequestProperty("Accept",
                                // "application/rdf+xml, application/xml;
                                // q=0.9,text/xml; q=0.9, */*; q=0.2");


                                if ((contentType != null) &&
                                        (contentType.equalsIgnoreCase("text/plain") ||
                                                contentType.equalsIgnoreCase("text/xml") ||
                                                contentType.equalsIgnoreCase("application/xml") ||
                                                contentType.equalsIgnoreCase("application/rdf+xml"))
                                        ) {
                                    importUrl(repository, con, importURL, importIS, contentType);
                                    log.info("Imported non owl/xsd from " + importURL);
                                    addedDocument = true;

                                } else {
                                    log.warn("SKIPPING Import URL '" + importURL + "' It does not appear to reference a " +
                                            "document that I know how to process.");
                                    urlsToBeIgnored.add(importURL); //skip this file
                                    skipCount++;

                                }

                                log.info("Total non owl/xsd files skipped: " + skipCount);
                            }
                        }
                    } // while (!rdfDocs.isEmpty()

                } catch (Exception e) {
                    log.error("addNeededRDFDocuments(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                    if (importURL != null){
                        log.warn("SKIPPING Import URL '"+importURL+"' Because bad things haoppened when we tried to get it.");
                        urlsToBeIgnored.add(importURL); //skip this file
                    }
                } finally {
                    if (importIS != null)
                        try {
                            importIS.close();
                        } catch (IOException e) {
                            log.error("addNeededRDFDocuments(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                        }
                    if (hc != null)
                        hc.disconnect();

                }
            }
        }
        catch (RepositoryException e) {
            log.error("addNeededRDFDocuments(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        }
        finally {
            if (con != null) {

                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("Caught an RepositoryException! in addNeededRDFDocuments() Msg: "
                            + e.getMessage());
                }
            }
            inferEndTime = new Date().getTime();
            double inferTime = (inferEndTime - inferStartTime) / 1000.0;
            log.debug("Import takes " + inferTime + " seconds");


        }
        return addedDocument;

    }


    private void importUrl(Repository repository, RepositoryConnection con, String importURL, InputStream importIS, String contentType ) throws IOException, RDFParseException, RepositoryException {
        log.info("Importing URL " + importURL);
        URI uriaddress = new URIImpl(importURL);
        con.add(importIS, importURL, RDFFormat.RDFXML, (Resource) uriaddress);
        setLTMODContext(importURL, con, repository.getValueFactory()); // set last modified
        // time of the context
        setContentTypeContext(importURL, contentType, con, repository.getValueFactory()); //

        log.info("Finished importing URL " + importURL);
        imports.add(importURL);

    }

    


    /**
     * Insert a statement declaring the content type of the document.
     *
     * @param importURL
     * @param contentType
     * @param con
     */
    public void setContentTypeContext(String importURL, String contentType, RepositoryConnection con, ValueFactory valueFactory) {
        if (!this.imports.contains(importURL)) { // not in the repository yet

            URI s = valueFactory.createURI(importURL);
            URI contentTypeContext = valueFactory.createURI(Terms.contentTypeContextUri);
            URI cacheContext = valueFactory.createURI(Terms.cacheContextUri);

            Literal o = valueFactory.createLiteral(contentType);

            try {

                con.add((Resource) s, contentTypeContext, (Value) o, (Resource) cacheContext);

            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! Msg: "
                        + e.getMessage());

            }

        }
    }



    /**
     * Set last_modified_time of the URI in the repository.
     * @param importURL
     * @param con
     */
    public void setLTMODContext(String importURL, RepositoryConnection con,ValueFactory valueFactory) {
        String ltmod = RepositoryUtility.getLTMODContext(importURL);
        setLTMODContext(importURL, ltmod, con, valueFactory);
    }


    /**
     *
     *
     * @param importURL
     * @param ltmod
     * @param con
     */
    public void setLTMODContext(String importURL, String ltmod, RepositoryConnection con, ValueFactory valueFactory) {

        if (!imports.contains(importURL)) { // not in the repository yet
            // log.debug(importURL);
            // log.debug("lastmodified " + ltmod);
            URI s = valueFactory.createURI(importURL);
            URI p = valueFactory.createURI(Terms.lastModifiedContextUri);
            URI cont = valueFactory.createURI(Terms.cacheContextUri);
            URI sxd = valueFactory.createURI("http://www.w3.org/2001/XMLSchema#dateTime");
            Literal o = valueFactory.createLiteral(ltmod, sxd);

            try {

                con.add((Resource) s, p, (Value) o, (Resource) cont);

            } catch (RepositoryException e) {
                log.error("Caught an RepositoryException! Msg: "
                        + e.getMessage());

            }

        }
    }


    public ByteArrayInputStream transform(InputStream is, String xsltFileName) throws SaxonApiException {
        return transform(new StreamSource(is),xsltFileName);

    }

    /**
     * Compile and execute a simple transformation that applies a stylesheet to
     * an input stream, and serializing the result to an OutPutStream
     *
     * @param sourceURL
     * @return
     * @throws net.sf.saxon.s9api.SaxonApiException
     */
    public ByteArrayInputStream transform(StreamSource sourceURL, String xsltFileName)
            throws SaxonApiException {
        log.debug("Executing XSL Transform Operation.");
        log.debug("XSL Transform Filename: "+xsltFileName);

        String doc = sourceURL.getSystemId();
        if(doc==null)
            doc = sourceURL.getPublicId();

        if(doc==null)
            doc = "StreamSource";


        log.debug("Document to transform: "+doc);


        Processor proc = new Processor(false);
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp = comp.compile(new StreamSource(new File(
                xsltFileName)));

        XdmNode source = proc.newDocumentBuilder().build(sourceURL);
        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        out.setOutputStream(outStream);
        XsltTransformer trans = exp.load();
        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();
        log.info(outStream.toString());
        log.debug("XSL Transform complete.");
        return new ByteArrayInputStream(outStream.toByteArray());


    }
}
