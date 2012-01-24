/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

import net.sf.saxon.s9api.SaxonApiException;
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

import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * This class is used to populate the repository. A particular URL is only
 * imported once. Bad URLs are skipped. The string vector <code>imports</code>
 * tracks all documents that are imported into the repository. The string
 * hashset <code> urlsToBeIgnored</code> is used to track bad urls that are
 * skipped. The method <code>importReferencedRdfDocs</code> repeatedly calls
 * method <code>findNeededRDFDocuments</code> and
 * <code>addNeededDocuments</code> until no new needed RDF documents are found.
 * The method <code>findNeededRDFDocuments</code> queries the repository and
 * passes those RDF docuemts to <code>addNeededDocuments</code> which in turn
 * adds them into the repository.
 */
public class RdfImporter {

    private Logger log;

    private HashSet<String> urlsToBeIgnored;
    private Vector<String> imports;
    private Vector<String> serversDown;

    private String localResourceDir;

    public RdfImporter(String resourceDir) {
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        urlsToBeIgnored = new HashSet<String>();
        imports = new Vector<String>();
        serversDown = new Vector<String>();
        this.localResourceDir = resourceDir;
    }

    public void reset() throws InterruptedException {
        urlsToBeIgnored.clear();
        imports.clear();
    }

    public String getLocalResourceDirUrl() throws InterruptedException {

        if (localResourceDir.startsWith("file:"))
            return localResourceDir;

        return "file://" + localResourceDir;

    }

    /**
     * Find and import all needed RDF documents into the repository.
     * 
     * @param repository
     *            - the RDF store.
     * @param doNotImportUrls
     *            - a Vector of String holds bad URLs.
     * @return true if added new RDF document, otherwise false.
     */
    public boolean importReferencedRdfDocs(Repository repository,
            Vector<String> doNotImportUrls) throws InterruptedException {

        boolean repositoryChanged = false;

        Vector<String> rdfDocList = new Vector<String>();

        if (doNotImportUrls != null)
            urlsToBeIgnored.addAll(doNotImportUrls);

        findNeededRDFDocuments(repository, rdfDocList);

        ProcessController.checkState();

        while (!rdfDocList.isEmpty()) {

            if (importRdfDocuments(repository, rdfDocList)) {
                repositoryChanged = true;
            }

            ProcessController.checkState();

            rdfDocList.clear();

            findNeededRDFDocuments(repository, rdfDocList);
            ProcessController.checkState();

        }

        return repositoryChanged;
    }

    /**
     * Find all RDF documents that are referenced by existing documents in the
     * repository.
     * 
     * @param repository
     *            - the RDF store.
     * @param rdfDocs
     *            - URLs of new needed RDF documents.
     */
    private void findNeededRDFDocuments(Repository repository,
            Vector<String> rdfDocs) throws InterruptedException {
        TupleQueryResult result = null;

        RepositoryConnection con = null;

        try {
            con = repository.getConnection();

            String queryString = "(SELECT distinct doc "
                    + "FROM {doc} rdf:type {rdfcache:"
                    + Terms.StartingPoint.getLocalId()
                    + "} "
                    + "union "
                    + "SELECT DISTINCT doc "
                    + "FROM {tp} rdf:type {rdfcache:"
                    + Terms.StartingPoint.getLocalId()
                    + "}; rdfcache:"
                    + Terms.dependsOn.getLocalId()
                    + " {doc}, "
                    + "[ {doc} rdfcache:isReplacedBy {newdoc}],"
                    + "[{doc} rdfcache:isContainedBy {cont},{tp2} rdfcache:dependsOn {cont}; rdf:type {rdfcache:StartingPoint}]"
                    + " WHERE cont=NULL AND newdoc=NULL) "
                    + "MINUS "
                    + "SELECT DISTINCT doc "
                    + "FROM CONTEXT "
                    + "rdfcache:"
                    + Terms.cacheContext.getLocalId()
                    + " {doc} rdfcache:"
                    + Terms.lastModified.getLocalId()
                    + " {lastmod} "

                    + "USING NAMESPACE "
                    + "rdfcache = <"
                    + Terms.rdfCacheNamespace + ">";

            log.debug("findNeededRDFDocuments(): Query String: '" + queryString
                    + "'");

            TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SERQL,
                    queryString);

            result = tupleQuery.evaluate();

            while (result.hasNext()) {
                BindingSet bindingSet = result.next();

                Value firstValue = bindingSet.getValue("doc");
                String doc = firstValue.stringValue();

                if (!rdfDocs.contains(doc) && !imports.contains(doc)
                        && !urlsToBeIgnored.contains(doc)
                // && doc.startsWith("http://")) {//local owl file not allowed
                ) { // local owl file allowed
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
                    log.error("Caught an " + e.getClass().getName() + " Msg: "
                            + e.getMessage());
                }
            }

            if (con != null) {
                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("Caught an " + e.getClass().getName() + " Msg: "
                            + e.getMessage());
                }
            }

        }
        if (rdfDocs.size() > 0)
            log.info("findNeededRDFDocuments(): Number of needed files identified:  "
                    + rdfDocs.size());

    }

    /**
     * Add each of the RDF documents whose URL's are in the passed Vector to the
     * Repository.
     * 
     * @param repository
     *            - RDF store.
     * @param rdfDocs
     *            - holds RDF documents to import.
     * @return true if one or more RDF document is added into the repository.
     * @throws InterruptedException
     *             When work is interrupted..
     */
    private boolean importRdfDocuments(Repository repository,
            Vector<String> rdfDocs) throws InterruptedException {
        long inferStartTime, inferEndTime;
        inferStartTime = new Date().getTime();

        String documentURL;
        RepositoryConnection con = null;
        int skipCount;
        // String contentType;
        // HttpURLConnection httpConnection = null;

        InputStream importIS = null;
        boolean addedDocument = false;

        try {
            con = repository.getConnection();
            ValueFactory valueFactory = repository.getValueFactory();

            log.info("importRdfDocuments():  Adding " + rdfDocs.size()
                    + " document(s).");
            skipCount = 0;
            while (!rdfDocs.isEmpty()) {
                documentURL = rdfDocs.remove(0);
                if (documentURL != null) {

                    try {

                        log.debug("importRdfDocuments(): Checking import URL: "
                                + documentURL);

                        if (urlsToBeIgnored.contains(documentURL)) {
                            log.error("importRdfDocuments(): Previous server error, Skipping "
                                    + documentURL);
                        } else {
                            if (documentURL.startsWith("http://") || documentURL.startsWith("https://")) {

                                addedDocument = addHttpRdf(documentURL, con,
                                        valueFactory, skipCount);
                            } else if (documentURL.startsWith("file://")) {
                                addedDocument = addFileRdf(documentURL, con,
                                        valueFactory, skipCount);
                            }

                        }

                    } finally {
                        if (importIS != null)
                            try {
                                importIS.close();
                            } catch (IOException e) {
                                log.error("importRdfDocuments(): Caught "
                                        + e.getClass().getName() + " Message: "
                                        + e.getMessage());
                            }

                    }
                }
            } // while (!rdfDocs.isEmpty()
            log.debug("importRdfDocuments(): Total non owl/xsd files skipped: "
                    + skipCount);
        } catch (RepositoryException e) {
            log.error("importRdfDocuments(): Caught " + e.getClass().getName()
                    + " Message: " + e.getMessage());
        } finally {
            if (con != null) {

                try {
                    con.close();
                } catch (RepositoryException e) {
                    log.error("importRdfDocuments(): Caught an RepositoryException! in importRdfDocuments() Msg: "
                            + e.getMessage());
                }
            }
            inferEndTime = new Date().getTime();
            double inferTime = (inferEndTime - inferStartTime) / 1000.0;
            log.debug("importRdfDocuments(): Import takes " + inferTime
                    + " seconds");

        }
        return addedDocument;

    }

    private boolean addFileRdf(String documentURL, RepositoryConnection con,
            ValueFactory valueFactory, int skipCount)
            throws InterruptedException {
        boolean addedDocument = false;
        log.debug("importRdfDocuments(): Import URL appears valid ( "
                + documentURL + " )");
        try {
            String transformToRdfUrl = RepositoryOps.getUrlForTransformToRdf(
                    con, valueFactory, documentURL);
            FileDataSource fdSource = new FileDataSource(documentURL);
            String contentType = fdSource.getContentType();

            if (transformToRdfUrl != null) {

                log.debug("importRdfDocuments(): Transforming " + documentURL
                        + " with " + transformToRdfUrl);

                if (Terms.localResources.containsKey(transformToRdfUrl)) {
                    // transformToRdfUrl = getLocalResourceDirUrl() +
                    // Terms.localResources.get(transformToRdfUrl);
                    log.debug("importRdfDocuments(): Transform URL has local copy: "
                            + transformToRdfUrl);
                }

                Transformer t = new Transformer(transformToRdfUrl);
                InputStream inStream = t.transform(documentURL);

                log.debug("importRdfDocuments(): Finished transforming RDFa "
                        + documentURL);

                importUrl(con, documentURL, contentType, inStream,
                        transformToRdfUrl);

                addedDocument = true;

            } else if (documentURL.endsWith(".xsd")) {
                // XML Schema Document has known transform.

                // transformToRdfUrl = getLocalResourceDirUrl() +
                // "xsl/xsd2owl.xsl";
                transformToRdfUrl = "http://scm.opendap.org/svn/trunk/olfs/resources/WCS/xsl/xsd2owl.xsl";
                log.debug("importRdfDocuments(): Transforming Schema Document'"
                        + documentURL + "' with '" + transformToRdfUrl);

                Transformer t = new Transformer(transformToRdfUrl);
                InputStream inStream = t.transform(documentURL);

                log.debug("importRdfDocuments(): Finished transforming Xml Schema Document: '"
                        + documentURL + "'");

                importUrl(con, documentURL, contentType, inStream,
                        transformToRdfUrl);

                addedDocument = true;

            } else if (documentURL.endsWith(".owl")
                    || documentURL.endsWith(".rdf")) {
                // OWL is RDF and so is the repository - no transform needed.

                importUrl(con, documentURL, contentType);

                addedDocument = true;

            } else if ((contentType != null)
                    && (contentType.equalsIgnoreCase("text/plain")
                            || contentType.equalsIgnoreCase("text/xml")
                            || contentType.equalsIgnoreCase("application/xml") || contentType
                            .equalsIgnoreCase("application/rdf+xml"))) {
                log.debug("before getGrddlTransform ");
                String grddlTransformUrl = getGrddlTransform(documentURL);
                log.debug("after getGrddlTransform ");
                // log.debug("transform = " + grddlTransformUrl);
                if (grddlTransformUrl != null && !grddlTransformUrl.isEmpty()) {
                    log.debug("transform = " + grddlTransformUrl);
                    Transformer t = new Transformer(grddlTransformUrl);
                    InputStream inStream = t.transform(documentURL);

                    log.debug("importRdfDocuments(): Finished transforming Xml Schema Document: '"
                            + documentURL + "'");

                    importUrl(con, documentURL, contentType, inStream,
                            grddlTransformUrl);

                    addedDocument = true;
                } else {
                    log.debug("Add to repository without transforming! "
                            + documentURL);
                    importUrl(con, documentURL, contentType);
                    log.debug("importRdfDocuments(): Imported non owl/xsd from "
                            + documentURL);

                    addedDocument = true;
                }
            } else {
                log.warn("importRdfDocuments(): SKIPPING Import URL '"
                        + documentURL + "' It does not appear to reference a "
                        + "document that I know how to process.");
                urlsToBeIgnored.add(documentURL); // skip this file
                skipCount++;

            }

        } catch (RDFParseException e) {
            handleImportError(e, documentURL);
        } catch (IOException e) {
            handleImportError(e, documentURL);
        } catch (SaxonApiException e) {
            handleImportError(e, documentURL);
        } catch (JDOMException e) {
            handleImportError(e, documentURL);
        } catch (RepositoryException e) {
            handleImportError(e, documentURL);
        }
        return addedDocument;
    }

    private boolean addHttpRdf(String documentURL, RepositoryConnection con,
            ValueFactory valueFactory, int skipCount)
            throws InterruptedException {

        URL myurl;
        HttpURLConnection httpConnection;
        String contentType;
        boolean addedDocument = false;

        int rsCode;
        try {
            myurl = new URL(documentURL);
            if (!serversDown.contains(myurl.getHost())) {
                httpConnection = (HttpURLConnection) myurl.openConnection();
                log.debug("importRdfDocuments(): Connected to import URL: "
                        + documentURL);

                rsCode = httpConnection.getResponseCode();
                contentType = httpConnection.getContentType();

                log.debug("importRdfDocuments(): Got HTTP status code: "
                        + rsCode);
                log.debug("importRdfDocuments(): Got Content Type:     "
                        + contentType);

                if (rsCode == -1) {
                    log.error("importRdfDocuments(): Unable to get an HTTP status code for resource "
                            + documentURL + " WILL NOT IMPORT!");
                    urlsToBeIgnored.add(documentURL);
                    URL inUrlHost = new URL(documentURL);
                    serversDown.add(inUrlHost.getHost());
                } else if (rsCode != 200) {
                    log.error("importRdfDocuments(): Error!  HTTP status code "
                            + rsCode + " Skipping documentURL " + documentURL);
                    urlsToBeIgnored.add(documentURL);
                    URL inUrlHost = new URL(documentURL);
                    serversDown.add(inUrlHost.getHost());
                } else {

                    log.debug("importRdfDocuments(): Import URL appears valid ( "
                            + documentURL + " )");

                    String transformToRdfUrl = RepositoryOps
                            .getUrlForTransformToRdf(con, valueFactory,
                                    documentURL);

                    if (transformToRdfUrl != null) {

                        log.debug("importRdfDocuments(): Transforming "
                                + documentURL + " with " + transformToRdfUrl);

                        if (Terms.localResources.containsKey(transformToRdfUrl)) {
                            // transformToRdfUrl = getLocalResourceDirUrl() +
                            // Terms.localResources.get(transformToRdfUrl);
                            log.debug("importRdfDocuments(): Transform URL has local copy: "
                                    + transformToRdfUrl);
                        }

                        Transformer t = new Transformer(transformToRdfUrl);
                        InputStream inStream = t.transform(documentURL);

                        log.debug("importRdfDocuments(): Finished transforming RDFa "
                                + documentURL);

                        importUrl(con, documentURL, contentType, inStream,
                                transformToRdfUrl);

                        addedDocument = true;

                    } else if (documentURL.endsWith(".xsd")) {
                        // XML Schema Document has known transform.

                        // transformToRdfUrl = getLocalResourceDirUrl() +
                        // "xsl/xsd2owl.xsl";
                        transformToRdfUrl = "http://scm.opendap.org/svn/trunk/olfs/resources/WCS/xsl/xsd2owl.xsl";
                        log.debug("importRdfDocuments(): Transforming Schema Document'"
                                + documentURL + "' with '" + transformToRdfUrl);

                        Transformer t = new Transformer(transformToRdfUrl);
                        InputStream inStream = t.transform(documentURL);

                        log.debug("importRdfDocuments(): Finished transforming Xml Schema Document: '"
                                + documentURL + "'");

                        importUrl(con, documentURL, contentType, inStream,
                                transformToRdfUrl);

                        addedDocument = true;

                    } else if (documentURL.endsWith(".owl")
                            || documentURL.endsWith(".rdf")) {
                        // OWL is RDF and so is the repository - no transform
                        // needed.

                        importUrl(con, documentURL, contentType);

                        addedDocument = true;

                    } else if ((contentType != null)
                            && (contentType.equalsIgnoreCase("text/plain")
                                    || contentType.equalsIgnoreCase("text/xml")
                                    || contentType
                                            .equalsIgnoreCase("application/xml") || contentType
                                    .equalsIgnoreCase("application/rdf+xml"))) {
                        log.debug("before getGrddlTransform ");
                        String grddlTransformUrl = getGrddlTransform(documentURL);
                        log.debug("after getGrddlTransform ");
                        // log.debug("transform = " + grddlTransformUrl);
                        if (grddlTransformUrl != null
                                && !grddlTransformUrl.isEmpty()) {
                            log.debug("transform = " + grddlTransformUrl);
                            Transformer t = new Transformer(grddlTransformUrl);
                            InputStream inStream = t.transform(documentURL);

                            log.debug("importRdfDocuments(): Finished transforming Xml Schema Document: '"
                                    + documentURL + "'");

                            importUrl(con, documentURL, contentType, inStream,
                                    grddlTransformUrl);

                            addedDocument = true;
                        } else {
                            log.debug("Add to repository without transforming! "
                                    + documentURL);
                            importUrl(con, documentURL, contentType);
                            log.debug("importRdfDocuments(): Imported non owl/xsd from "
                                    + documentURL);

                            addedDocument = true;
                        }
                    } else {
                        log.warn("importRdfDocuments(): SKIPPING Import URL '"
                                + documentURL
                                + "' It does not appear to reference a "
                                + "document that I know how to process.");
                        urlsToBeIgnored.add(documentURL); // skip this file
                        skipCount++;

                    }

                    // log.debug("importRdfDocuments(): Total non owl/xsd files skipped: "
                    // + skipCount);
                }
                httpConnection.disconnect();
            } else {
                log.warn("importRdfDocuments(): SKIPPING Import URL '"
                        + documentURL + ". The host is down.");
                urlsToBeIgnored.add(documentURL); // skip this file
                skipCount++;

            }// if (!serversDown.contains(myurl.getHost()))
        } catch (RDFParseException e) {
            handleImportError(e, documentURL);
        } catch (IOException e) {
            handleImportError(e, documentURL);
        } catch (SaxonApiException e) {
            handleImportError(e, documentURL);
        } catch (JDOMException e) {
            handleImportError(e, documentURL);
        } catch (RepositoryException e) {
            handleImportError(e, documentURL);
        }
        return addedDocument;
    }

    private void handleImportError(Exception e, String documentURL)
            throws InterruptedException {
        log.error("importRdfDocuments(): Caught " + e.getClass().getName()
                + " Message: " + e.getMessage());
        if (documentURL != null) {
            log.warn("importRdfDocuments(): SKIPPING Import URL '"
                    + documentURL
                    + "' Because bad things happened when we tried to get it.");
            urlsToBeIgnored.add(documentURL); // skip this file
        }

    }

    /**
     * Add individual RDF document into the repository.
     * 
     * @param con
     *            - connection to the repository.
     * @param importURL
     *            - URL of RDF document to import.
     * @param contentType
     *            - content type of the RDF document.
     * @param importIS
     *            - input stream from of the RDF document.
     * @throws IOException
     *             - if read importIS error.
     * @throws RDFParseException
     *             - if parse importIS error.
     * @throws RepositoryException
     *             - if repository error.
     */
    private void importUrl(RepositoryConnection con, String importURL,
            String contentType, InputStream importIS, String transformToRdfUrl)
            throws InterruptedException, IOException, RDFParseException,
            RepositoryException {

        if (!this.imports.contains(importURL)) { // not in the repository yet

            log.debug("Importing URL " + importURL);

            ValueFactory valueFactory = con.getValueFactory();
            URI importUri = new URIImpl(importURL);

            con.add(importIS, importURL, RDFFormat.RDFXML, (Resource) importUri);
            RepositoryOps.setLTMODContext(importURL, con, valueFactory); // set
                                                                         // last
                                                                         // modified
                                                                         // time
                                                                         // of
                                                                         // the
                                                                         // context
            RepositoryOps.setContentTypeContext(importURL, contentType, con,
                    valueFactory); //
            RepositoryOps.setLTMODContext(transformToRdfUrl, con, valueFactory);

            log.debug("Finished importing URL " + importURL);
            imports.add(importURL);
        } else {
            log.error("Import URL '" + importURL
                    + "' has already been imported! SKIPPING!");
        }
    }

    /**
     * Add individual RDF document into the repository.
     * 
     * @param con
     *            - connection to the repository
     * @param importURL
     *            - URL of RDF document to import
     * @param contentType
     *            - Content type of the RDF document
     * @throws IOException
     *             - if read url error.
     * @throws RDFParseException
     *             - if parse url content error.
     * @throws RepositoryException
     *             - if repository error.
     */
    private void importUrl(RepositoryConnection con, String importURL,
            String contentType) throws InterruptedException, IOException,
            RDFParseException, RepositoryException {

        if (!this.imports.contains(importURL)) { // not in the repository yet

            log.debug("Importing URL " + importURL);

            ValueFactory valueFactory = con.getValueFactory();
            URI importUri = new URIImpl(importURL);
            URL url = new URL(importURL);

            con.add(url, importURL, RDFFormat.RDFXML, (Resource) importUri);
            RepositoryOps.setLTMODContext(importURL, con, valueFactory); // set
                                                                         // last
                                                                         // modified
                                                                         // time
                                                                         // of
                                                                         // the
                                                                         // context
            RepositoryOps.setContentTypeContext(importURL, contentType, con,
                    valueFactory); //

            log.debug("Finished importing URL " + importURL);
            imports.add(importURL);
        } else {
            log.error("Import URL '" + importURL
                    + "' has already been imported! SKIPPING!");
        }
    }

    /**
     * This method returns URL of GRDDL transform stylesheet. It parses the xml
     * file and returns the full URL of the attribute transformation.
     * 
     * @param importUrl
     * @return URL of GRDDL transform stylesheet
     * @throws IOException
     * @throws JDOMException
     */
    private String getGrddlTransform(String importUrl)
            throws InterruptedException, IOException, JDOMException {

        SAXBuilder builder = new SAXBuilder();
        String grddlTransformUrl = "";

        Document doc = builder.build(importUrl);

        Element xmlRoot = doc.getRootElement();

        org.jdom.Namespace ns = xmlRoot.getNamespace("grddl");

        if (ns != null) {
            grddlTransformUrl = xmlRoot.getAttributeValue("transformation", ns);
            return grddlTransformUrl;
        }

        return null;

    }

    public static void main(String[] args) throws Exception {

        StreamSource httpSource = new StreamSource(
                "http://schemas.opengis.net/wcs/1.1/wcsAll.xsd");
        StreamSource fileSource = new StreamSource(
                "file:/Users/ndp/OPeNDAP/Projects/Hyrax/swdev/trunk/olfs/resources/WCS/xsl/xsd2owl.xsl");

        StreamSource transform = fileSource;
        StreamSource document = httpSource;

        XMLOutputter xmlo = new XMLOutputter();
        xmlo.output(Transformer.getTransformedDocument(document, transform),
                System.out);

    }

}
