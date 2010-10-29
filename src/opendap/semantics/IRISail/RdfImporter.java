/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2010 OPeNDAP, Inc.
//
// Authors:
//     Haibo Liu  <haibo@iri.columbia.edu>
//     Nathan David Potter  <ndp@opendap.org>
//     Benno Blumenthal <benno@iri.columbia.edu>
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

import opendap.xml.Transformer;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.openrdf.model.*;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.*;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.slf4j.Logger;

import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to populate the repository. A particular URL is only imported once. Bad URLs
 * are skipped. The string vector <code>imports</code>  tracks all documents that are imported
 * into the repository. The string hashset <code> urlsToBeIgnored</code> is used to track bad urls
 * that are skipped. The method <code>importReferencedRdfDocs</code> repeatedly calls method
 * <code>findNeededRDFDocuments</code> and <code>addNeededDocuments</code> until no new needed
 * RDF documents are found.
 * The method <code>findNeededRDFDocuments</code> queries the repository and passes those RDF
 * docuemts to <code>addNeededDocuments</code> which in turn adds them into the repository.
 */
public class RdfImporter {

    private Logger log;


    private HashSet<String> urlsToBeIgnored;
    private Vector<String> imports;

    private String localResourceDir;


    public RdfImporter(String resourceDir) {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        urlsToBeIgnored = new HashSet<String>();
        imports = new Vector<String>();
        this.localResourceDir = resourceDir;
    }

    public void reset() {
        urlsToBeIgnored.clear();
        imports.clear();
    }

    public String getLocalResourceDirUrl() {

        if (localResourceDir.startsWith("file:"))
            return localResourceDir;

        return "file://" + localResourceDir;

    }


    /**
     * Find and import all needed RDF documents into the repository.
     *
     * @param repository      - the RDF store.
     * @param doNotImportUrls - a Vector of String holds bad URLs.
     * @return true if added new RDF document, otherwise false.
     */
    public boolean importReferencedRdfDocs(Repository repository, Vector<String> doNotImportUrls) {

        boolean repositoryChanged = false;

        Vector<String> rdfDocList = new Vector<String>();

        if (doNotImportUrls != null)
            urlsToBeIgnored.addAll(doNotImportUrls);


        findNeededRDFDocuments(repository, rdfDocList);

        while (!rdfDocList.isEmpty()) {

            if (addNeededRDFDocuments(repository, rdfDocList)) {
                repositoryChanged = true;
            }

            rdfDocList.clear();

            findNeededRDFDocuments(repository, rdfDocList);
        }

        return repositoryChanged;
    }


    /**
     * Find all RDF documents that are referenced by existing documents in the repository.
     *
     * @param repository - the RDF store.
     * @param rdfDocs    - URLs of new needed RDF documents.
     */
    private void findNeededRDFDocuments(Repository repository, Vector<String> rdfDocs) {
        TupleQueryResult result = null;

        RepositoryConnection con = null;

        try {
            con = repository.getConnection();

            String queryString = "(SELECT distinct doc "
                    + "FROM {doc} rdf:type {rdfcache:" + Terms.StartingPoint.getLocalId() + "} "
                    + "union "
                    + "SELECT DISTINCT doc "
                    + "FROM {tp} rdf:type {rdfcache:" + Terms.StartingPoint.getLocalId() + "}; rdfcache:" + Terms.dependsOn.getLocalId() + " {doc}, "
                    + "[ {doc} rdfcache:isReplacedBy {newdoc}],"
                    + "[{doc} rdfcache:isContainedBy {cont},{tp2} rdfcache:dependsOn {cont}; rdf:type {rdfcache:StartingPoint}]"
                    + " WHERE cont=NULL AND newdoc=NULL) "
                    + "MINUS "
                    + "SELECT DISTINCT doc "
                    + "FROM CONTEXT " + "rdfcache:" + Terms.cacheContext.getLocalId() + " {doc} rdfcache:" + Terms.lastModified.getLocalId() + " {lastmod} "

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
                } catch (Exception e) {
                    log.error("Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }

            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    log.error("Caught an "+e.getClass().getName()+" Msg: " + e.getMessage());
                }
            }

        }

        log.info("Number of needed files identified:  "
                + rdfDocs.size());

    }


    /**
     * Add each of the RDF documents whose URL's are in the passed Vector to the Repository.
     *
     * @param repository - RDF store.
     * @param rdfDocs    - holds RDF documents to import.
     * @return true if one or more RDF document is added into the repository.
     */
    private boolean addNeededRDFDocuments(Repository repository, Vector<String> rdfDocs) {
        long inferStartTime, inferEndTime;
        inferStartTime = new Date().getTime();

        String documentURL;
        RepositoryConnection con = null;
        int skipCount;
        String contentType;
        HttpURLConnection httpConnection = null;

        InputStream importIS = null;
        boolean addedDocument = false;


        try {
            con = repository.getConnection();


            log.info("addNeededRDFDocuments(): rdfDocs.size=" + rdfDocs.size());
            skipCount = 0;
            while (!rdfDocs.isEmpty()) {
                documentURL = rdfDocs.remove(0);
                if (documentURL != null) {

                    try {


                        log.debug("addNeededRDFDocuments(): Checking import URL: " + documentURL);

                        if (urlsToBeIgnored.contains(documentURL)) {
                            log.error("addNeededRDFDocuments(): Previous server error, Skipping " + documentURL);
                        } else {

                            URL myurl = new URL(documentURL);


                            int rsCode;
                            httpConnection = (HttpURLConnection) myurl.openConnection();
                            log.debug("addNeededRDFDocuments(): Connected to import URL: " + documentURL);

                            rsCode = httpConnection.getResponseCode();
                            contentType = httpConnection.getContentType();

                            log.debug("addNeededRDFDocuments(): Got HTTP status code: " + rsCode);
                            log.debug("addNeededRDFDocuments(): Got Content Type:     " + contentType);

                            if (rsCode == -1) {
                                log.error("addNeededRDFDocuments(): Unable to get an HTTP status code for resource "
                                        + documentURL + " WILL NOT IMPORT!");
                                urlsToBeIgnored.add(documentURL);

                            } else if (rsCode != 200) {
                                log.error("addNeededRDFDocuments(): Error!  HTTP status code " + rsCode + " Skipping documentURL " + documentURL);
                                urlsToBeIgnored.add(documentURL);
                            } else {

                                log.debug("addNeededRDFDocuments(): Import URL appears valid ( " + documentURL + " )");


                                String transformToRdfUrl = RepositoryOps.getUrlForTransformToRdf(repository, documentURL);


                                if (transformToRdfUrl != null) {

                                    log.debug("addNeededRDFDocuments(): Transforming " + documentURL + " with " + transformToRdfUrl);

                                    if (Terms.localResources.containsKey(transformToRdfUrl)) {
                                        transformToRdfUrl = getLocalResourceDirUrl() + Terms.localResources.get(transformToRdfUrl);
                                        log.debug("addNeededRDFDocuments(): Transform URL has local copy: " + transformToRdfUrl);
                                    }


                                    Transformer t = new Transformer(transformToRdfUrl);
                                    InputStream inStream = t.transform(documentURL);

                                    log.debug("addNeededRDFDocuments(): Finished transforming RDFa " + documentURL);

                                    importUrl(con, documentURL, contentType, inStream);

                                    addedDocument = true;

                                } else if (documentURL.endsWith(".xsd")) {
                                    // XML Schema Document has known transform.

                                    transformToRdfUrl = getLocalResourceDirUrl() + "xsl/xsd2owl.xsl";

                                    log.debug("addNeededRDFDocuments(): Transforming Schema Document'" + documentURL + "' with '" + transformToRdfUrl);

                                    Transformer t = new Transformer(transformToRdfUrl);
                                    InputStream inStream = t.transform(documentURL);

                                    log.debug("addNeededRDFDocuments(): Finished transforming Xml Schema Document: '" + documentURL + "'");

                                    importUrl(con, documentURL, contentType, inStream);

                                    addedDocument = true;

                                } else if (documentURL.endsWith(".owl") || documentURL.endsWith(".rdf")) {
                                    // OWL is RDF and so is the repository - no transform needed.

                                    importUrl(con, documentURL, contentType);

                                    addedDocument = true;


                                } else if ((contentType != null) &&
                                        (contentType.equalsIgnoreCase("text/plain") ||
                                                contentType.equalsIgnoreCase("text/xml") ||
                                                contentType.equalsIgnoreCase("application/xml") ||
                                                contentType.equalsIgnoreCase("application/rdf+xml"))
                                        ) {
                                    log.debug("before getGrddlTransform ");
                                    String grddlTransformUrl = getGrddlTransform(documentURL);
                                    log.debug("after getGrddlTransform ");
                                    //log.debug("transform = " + grddlTransformUrl);
                                    if (!grddlTransformUrl.isEmpty()) {
                                        log.debug("transform = " + grddlTransformUrl);
                                        Transformer t = new Transformer(grddlTransformUrl);
                                        InputStream inStream = t.transform(documentURL);

                                        log.debug("addNeededRDFDocuments(): Finished transforming Xml Schema Document: '" + documentURL + "'");

                                        importUrl(con, documentURL, contentType, inStream);

                                        addedDocument = true;
                                    } else {
                                        log.debug("Add to repository without transforming! " + documentURL);
                                        importUrl(con, documentURL, contentType);
                                        log.debug("addNeededRDFDocuments(): Imported non owl/xsd from " + documentURL);

                                        addedDocument = true;
                                    }
                                } else {
                                    log.warn("addNeededRDFDocuments(): SKIPPING Import URL '" + documentURL + "' It does not appear to reference a " +
                                            "document that I know how to process.");
                                    urlsToBeIgnored.add(documentURL); //skip this file
                                    skipCount++;

                                }

                                log.debug("addNeededRDFDocuments(): Total non owl/xsd files skipped: " + skipCount);
                            }
                        } // while (!rdfDocs.isEmpty()

                    } catch (Exception e) {
                        log.error("addNeededRDFDocuments(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                        if (documentURL != null) {
                            log.warn("addNeededRDFDocuments(): SKIPPING Import URL '" + documentURL + "' Because bad things happened when we tried to get it.");
                            urlsToBeIgnored.add(documentURL); //skip this file
                        }
                    } finally {
                        if (importIS != null)
                            try {
                                importIS.close();
                            } catch (IOException e) {
                                log.error("addNeededRDFDocuments(): Caught " + e.getClass().getName() + " Message: " + e.getMessage());
                            }
                        if (httpConnection != null)
                            httpConnection.disconnect();

                    }
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

    /**
     * Add individual RDF document into the repository.
     *
     * @param con         - connection to the repository.
     * @param importURL   - URL of RDF document to import.
     * @param contentType - content type of the RDF document.
     * @param importIS    - input stream from of the RDF document.
     * @throws IOException         - if read importIS error.
     * @throws RDFParseException   - if parse importIS error.
     * @throws RepositoryException - if repository error.
     */
    private void importUrl(RepositoryConnection con, String importURL, String contentType, InputStream importIS) throws IOException, RDFParseException, RepositoryException {

        if (!this.imports.contains(importURL)) { // not in the repository yet

            log.debug("Importing URL " + importURL);

            ValueFactory valueFactory = con.getValueFactory();
            URI importUri = new URIImpl(importURL);


            con.add(importIS, importURL, RDFFormat.RDFXML, (Resource) importUri);
            RepositoryOps.setLTMODContext(importURL, con, valueFactory); // set last modified  time of the context
            RepositoryOps.setContentTypeContext(importURL, contentType, con, valueFactory); //

            log.debug("Finished importing URL " + importURL);
            imports.add(importURL);
        } else {
            log.error("Import URL '" + importURL + "' already has been imported! SKIPPING!");
        }
    }

    /**
     * Add individual RDF document into the repository.
     *
     * @param con         - connection to the repository
     * @param importURL   - URL of RDF document to import
     * @param contentType - Content type of the RDF document
     * @throws IOException         - if read url error.
     * @throws RDFParseException   - if parse url content error.
     * @throws RepositoryException - if repository error.
     */
    private void importUrl(RepositoryConnection con, String importURL, String contentType) throws IOException, RDFParseException, RepositoryException {

        if (!this.imports.contains(importURL)) { // not in the repository yet

            log.debug("Importing URL " + importURL);

            ValueFactory valueFactory = con.getValueFactory();
            URI importUri = new URIImpl(importURL);
            URL url = new URL(importURL);


            con.add(url, importURL, RDFFormat.RDFXML, (Resource) importUri);
            RepositoryOps.setLTMODContext(importURL, con, valueFactory); // set last modified  time of the context
            RepositoryOps.setContentTypeContext(importURL, contentType, con, valueFactory); //

            log.debug("Finished importing URL " + importURL);
            imports.add(importURL);
        } else {
            log.error("Import URL '" + importURL + "' has already been imported! SKIPPING!");
        }
    }

    /**
     * This method returns URL of GRDDL transform stylesheet. It parses the xml file
     * and returns the full URL of the attribute transformation.
     *
     * @param importUrl
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private String getGrddlTransform(String importUrl) throws IOException, JDOMException {

        SAXBuilder builder = new SAXBuilder();
        String grddlTransformUrl = "";

        Document doc = builder.build(importUrl);

        Element xmlRoot = doc.getRootElement();

        org.jdom.Namespace ns = xmlRoot.getNamespace("grddl");

        grddlTransformUrl = xmlRoot.getAttributeValue("transformation", ns);

        return grddlTransformUrl;
    }


    public static void main(String[] args) throws Exception {


        StreamSource httpSource = new StreamSource("http://schemas.opengis.net/wcs/1.1/wcsAll.xsd");
        StreamSource fileSource = new StreamSource("file:/Users/ndp/OPeNDAP/Projects/Hyrax/swdev/trunk/olfs/resources/WCS/xsl/xsd2owl.xsl");


        StreamSource transform = fileSource;
        StreamSource document = httpSource;


        XMLOutputter xmlo = new XMLOutputter();
        xmlo.output(Transformer.getTransformedDocument(document, transform), System.out);


    }


}
