package opendap.semantics.IRISail;

import net.sf.saxon.s9api.*;
import org.openrdf.model.*;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
     * Update repository. Find and import documents into the repository.
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
            

            log.debug("addNeededRDFDocuments(): rdfDocs.size=" + rdfDocs.size());
            skipCount = 0;
            while (!rdfDocs.isEmpty()) {
                importURL = rdfDocs.remove(0);

                try {


                    log.debug("addNeededRDFDocuments(): Checking import URL: " + importURL);

                    if (urlsToBeIgnored.contains(importURL)) {
                        log.error("addNeededRDFDocuments(): Previous server error, Skipping " + importURL);
                    } else {

                        URL myurl = new URL(importURL);


                        int rsCode;
                        hc = (HttpURLConnection) myurl.openConnection();
                        log.debug("addNeededRDFDocuments(): Connected to import URL: " + importURL);

                        rsCode = hc.getResponseCode();
                        contentType = hc.getContentType();
                        importIS = hc.getInputStream();

                        log.debug("addNeededRDFDocuments(): Got HTTP status code: " + rsCode);
                        log.debug("addNeededRDFDocuments(): Got Content Type:     " + contentType);

                        if (rsCode == -1) {
                            log.error("addNeededRDFDocuments(): Unable to get an HTTP status code for resource "
                                    + importURL + " WILL NOT IMPORT!");
                            urlsToBeIgnored.add(importURL);

                        } else if (rsCode != 200) {
                            log.error("addNeededRDFDocuments(): Error!  HTTP status code " + rsCode + " Skipping importURL " + importURL);
                            urlsToBeIgnored.add(importURL);
                        } else {

                            log.debug("addNeededRDFDocuments(): Import URL appears valid ( " + importURL + " )");
                            //@todo make this a more robust
                            String transformFile = getXsltStylesheet(repository, importURL);
                            log.debug("addNeededRDFDocuments(): Transformation =  " + transformFile);
                            if (transformFile != null){
                                
                                ByteArrayInputStream inStream;
                                log.info("addNeededRDFDocuments(): Transforming " + importURL+" with "+transformFile);

                                inStream = transform(importIS,transformFile);

                                log.info("addNeededRDFDocuments(): Finished transforming RDFa " + importURL);

                                importUrl(con, importURL, contentType, inStream);

                                addedDocument = true;  
                            }else if(importURL.endsWith(".owl") || importURL.endsWith(".rdf")) {

                                importUrl(con, importURL, contentType, importIS);

                                addedDocument = true;


                            } else if (importURL.endsWith(".xsd")) {

                                transformFile = "xsl/xsd2owl.xsl";

                                ByteArrayInputStream inStream;
                                log.info("addNeededRDFDocuments(): Transforming  '" + importURL+"' with "+transformFile);
                                inStream = transform(importIS,resourceDir+transformFile);

                                log.info("addNeededRDFDocuments(): Finished transforming URL " + importURL);

                                importUrl(con, importURL, contentType, inStream);

                                addedDocument = true;

                            } else {



                                if ((contentType != null) &&
                                        (contentType.equalsIgnoreCase("text/plain") ||
                                                contentType.equalsIgnoreCase("text/xml") ||
                                                contentType.equalsIgnoreCase("application/xml") ||
                                                contentType.equalsIgnoreCase("application/rdf+xml"))
                                        ) {
                                    importUrl(con, importURL, contentType, importIS);
                                    log.info("addNeededRDFDocuments(): Imported non owl/xsd from " + importURL);
                                    addedDocument = true;

                                } else {
                                    log.warn("addNeededRDFDocuments(): SKIPPING Import URL '" + importURL + "' It does not appear to reference a " +
                                            "document that I know how to process.");
                                    urlsToBeIgnored.add(importURL); //skip this file
                                    skipCount++;

                                }

                                log.info("addNeededRDFDocuments(): Total non owl/xsd files skipped: " + skipCount);
                            }
                        }
                    } // while (!rdfDocs.isEmpty()

                } catch (Exception e) {
                    log.error("addNeededRDFDocuments(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                    if (importURL != null){
                        log.warn("addNeededRDFDocuments(): SKIPPING Import URL '"+importURL+"' Because bad things happened when we tried to get it.");
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
                    log.error("addNeededRDFDocuments(): Caught an RepositoryException! in addNeededRDFDocuments() Msg: "
                            + e.getMessage());
                }
            }
            inferEndTime = new Date().getTime();
            double inferTime = (inferEndTime - inferStartTime) / 1000.0;
            log.debug("addNeededRDFDocuments(): Import takes " + inferTime + " seconds");


        }
        return addedDocument;

    }


    private void importUrl(RepositoryConnection con, String importURL, String contentType, InputStream importIS) throws IOException, RDFParseException, RepositoryException {

        if (!this.imports.contains(importURL)) { // not in the repository yet

            log.info("Importing URL " + importURL);

            ValueFactory valueFactory = con.getValueFactory();
            URI importUri = new URIImpl(importURL);


            con.add(importIS, importURL, RDFFormat.RDFXML, (Resource) importUri);
            RepositoryOps.setLTMODContext(importURL, con, valueFactory); // set last modified  time of the context
            RepositoryOps.setContentTypeContext(importURL, contentType, con, valueFactory); //

            log.info("Finished importing URL " + importURL);
            imports.add(importURL);
        }
        else {
            log.error("Import URL '"+importURL+"' already has been imported! SKIPPING!");
        }
    }



    /**
     * Take file to transform and the style sheet to apply, return transformed file as 
     * ByteArrayInputStream
     * @param is
     * @param xsltFileName
     * @return
     * @throws SaxonApiException
     * @throws IOException
     */
    public ByteArrayInputStream transform(InputStream is, String xsltFileName) throws SaxonApiException, IOException {
        return transform(new StreamSource(is),xsltFileName);
    }

    /**
     * Compile and execute a simple transformation that applies a style sheet to
     * an input stream, and serializing the result to an OutPutStream
     *
     * @param sourceURL
     * @return ByteArrayInputStream
     * @throws net.sf.saxon.s9api.SaxonApiException
     * @throws IOException 
     */
    public ByteArrayInputStream transform(StreamSource sourceURL, String xsltFileName)
            throws SaxonApiException, IOException {
        log.debug("transform(): Executing XSL Transform Operation.");
        log.debug("transform(): XSL Transform Filename: " + xsltFileName);

        String doc = sourceURL.getSystemId();
        if (doc == null)
            doc = sourceURL.getPublicId();

        if (doc == null)
            doc = "StreamSource";


        log.debug("transform(): Document to transform: " + doc);


        Processor proc = new Processor(false);
        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp = null;
        if (xsltFileName.startsWith("http://")) {
            URL myurl = new URL(xsltFileName);
            HttpURLConnection hc = (HttpURLConnection) myurl.openConnection();
            exp = comp.compile(new StreamSource(hc.getInputStream()));
        } else {
            exp = comp.compile(new StreamSource(new File(xsltFileName)));
        }
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

    /**
     * Return URL of the transformation file.
     * @param importUrl-the file to transform
     * @param repository-the repository instance
     * @return xsltTransformationFileUrl-Url of the transformation stylesheet
     */
    private String getXsltStylesheet(Repository repository, String importUrl){
        RepositoryConnection con = null;
        String xsltTransformationFileUrl = null;
        ValueFactory f = null;
        RepositoryResult<Statement> statements = null;
        
        try {
            con = repository.getConnection();
            f = repository.getValueFactory();  
                        
            String hasTran = Terms.rdfCacheNamespace + Terms.hasXsltTransformation;
            
            URI sbj = f.createURI(importUrl);
            URI prd = f.createURI(hasTran);
            statements = con.getStatements((Resource)sbj, prd, null,true);
            
            while (statements.hasNext()){
                Statement s = statements.next();
                xsltTransformationFileUrl= s.getObject().stringValue();
                log.debug("Transformation file= " + xsltTransformationFileUrl);
            }
        } catch (RepositoryException e) {
            log.error("Caught a RepositoryException! in getXsltTransformation Msg: "
                    +e.getMessage());
        }finally {
            try {
                statements.close();
            } catch (RepositoryException e) {
                log.error("Caught a RepositoryException while closing statements! in getXsltTransformation Msg: "
                        +e.getMessage());
            }
           
            try {
                con.close();
            } catch (RepositoryException e) {
                log.error("Caught a RepositoryException! in getXsltTransformation Msg: "
                        + e.getMessage());
            }
        }
        
        return xsltTransformationFileUrl;
    }
}
