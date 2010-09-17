/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP Web Coverage Service Project."
//
// Copyright (c) 2010 OPeNDAP, Inc.
//
// Authors:
//     Haibo Liu  <haibo@iri.columbia.edu>
//     Nathan David Potter  <ndp@opendap.org>
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
package opendap.semantics.IRISail;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds all terms defined in this package.  
 * The strings are frequently defined
 * in pairs, e.g. lastModified and lastModifiedUri, where the first
 * the local name, and the second has the namespace prepended to make a complete URI.
 */
public class Terms {

    /**
     * is the URL of an owl document defining the classes and properties that the persistent RDF cache
     * uses in its inferencing and queries against the repository. 
     * It is always a rdfcache:StartingPoint, consequently it is always
     * included in the repository.
     */
    public static final String internalStartingPoint = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl";


    /**
     * is the namespace used for the classes and properties used by the persistent RDF cache.
     * The internalStartingPoint has to include the owl document that defines these classes/properties.
     */
    public static final String rdfCacheNamespace = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl#";


    /**
     * is the local name of the property used to hold the last modified time of a document.
     * The code uses the cacheContext to hold the last modified time of each document read in
     */
    public static final String lastModified = "last_modified";


    /**
     * is the URI of the property used to hold the last modified time of a document.
     * The code uses the cacheContext to hold the last modified time of each document read in
     */
    public static final String lastModifiedUri = rdfCacheNamespace + lastModified;


    /**
     * is the local name of the context used to hold information the code keeps on each document read in.
     */
    public static final String cacheContext = "cachecontext";


    /**
     * is the URI of the context used to hold information the code keeps on each document read in.
     */
    public static final String cacheContextUri = rdfCacheNamespace + cacheContext;


    /**
     * is the local name of the property used to hold the content-type of a document.
     * The code uses the cacheContext to hold the content-type of each document read in.
     */
    public static final String contentType = "Content-Type";


    /**
     * is the URI of the property used to hold the content-type of a document.
     * is the URI of the property used to hold the content-type of a document.
     * The code uses the cacheContext to hold the content-type of each document read in.
     */
    public static final String contentTypeUri         = rdfCacheNamespace + contentType;


    /**
     * is the local name of the context used to hold the externalInferencing.
     * The code can be easily changed to use the URI of each rule to hold the rule's output
     * in it's own context, but deletes were faster with a single context to hold all the
     * external inferencing.
     */
    public static final String externalInferencingContext    = "externalInferencing";


    /**
     * is the URI of the context used to hold the externalInferencing.
     * The code can be easily changed to use the URI of each rule to hold the rule's output
     * in it's own context, but deletes were faster with a single context to hold all the
     * external inferencing.
     */
    public static final String externalInferencingContextUri = rdfCacheNamespace + externalInferencingContext;


    /**
     * is the local name of the context used to hold the {doc} rdf:type {rdfcache:StartingPoint} statements.
     * There are two classes of documents in the repository:  StartingPoints, and documents needed
     * (directly or indirectly) by the StartingPoints. This context holds the statements which declare
     * that the rdfcache:StartingPoint documents are in that class.
     */
    public static final String startingPointsContext = "startingPoints";


    /**
     * is the URI of the context used to hold the {doc} rdf:type {rdfcache:StartingPoint} statements.
     * There are two classes of documents in the repository:  StartingPoints, and documents needed
     * (directly or indirectly) by the StartingPoints. This context holds the statements which declare
     * that the rdfcache:StartingPoint documents are in that class.
     */
    public static final String startingPointsContextUri = rdfCacheNamespace + startingPointsContext;


    /**
     * is the local name of the Class that holds the StartingPoints, i.e. the documents that are included
     * in the repository even if they are not needed by another document.
     */
    public static final String startingPointType = "StartingPoint";


    /**
     * is the URI of the Class that holds the StartingPoints, i.e. the documents that are included
     * in the repository even if they are not needed by another document.
     */
    public static final String startingPointUri = rdfCacheNamespace + startingPointType;


    /**
     * is the local name of the property used to hold the function name in an external function call.
     * External function calls from serql_text construct statements
     * are implemented by creating a blank node that contains the information
     * necessary for the function call -- when that blank node is passed back from sesame
     * that information is used to make the function call, at which point the results of
     * the function replace the blank node.
     */
    public static final String callFunction = "myfn";


    /**
     * is the URI of the property used to hold the function name in an external function call.
     * External function calls from serql_text construct statements
     * are implemented by creating a blank node that contains the information
     * necessary for the function call -- when that blank node is passed back from sesame
     * that information is used to make the function call, at which point the results of
     * the function replace the blank node.
     */
    public static final String callFunctionUri = rdfCacheNamespace + callFunction;


    /**
     * is the local name of the property used to hold the function argument list
     * in an external function call.
     * External function calls from serql_text construct statements
     * are implemented by creating a blank node that contains the information
     * necessary for the function call -- when that blank node is passed back from sesame
     * that information is used to make the function call, at which point the results of
     * the function replace the blank node.
     */
    public static final String withArguments = "mylist";


    /**
     * is the URI of the property used to hold the function argument list
     * in an external function call.
     * External function calls from serql_text construct statements
     * are implemented by creating a blank node that contains the information
     * necessary for the function call -- when that blank node is passed back from sesame
     * that information is used to make the function call, at which point the results of
     * the function replace the blank node.
     */
    public static final String withArgumentsUri = rdfCacheNamespace + withArguments;


    /**
     * is the local name of the property that connects a containing document to the documents
     * it contains.  It is used to suppress searching and loading the contained documents.
     */
    public static final String isContainedBy = "isContainedBy";


    /**
     * is the URI of the property that connects a containing document to the documents
     * it contains.  It is used to suppress searching and loading the contained documents.
     */
    public static final String isContainedByUri = rdfCacheNamespace + isContainedBy;


    /**
     * is the local name of the property that casts the type of the subject.
     */
    public static final String reTypeToContext = "reTypeTo";


    /**
     * is the URI of the property that casts the type of the subject.
     */
    public static final String reTypeToContextUri = rdfCacheNamespace + reTypeToContext;


    /**
     * is the local name of the property that connects documents to the other documents
     * that they require.  It is transitive, and in particular it is used to find the
     * other documents that the rdfcache:StartingPoint(s) depend on; those documents are also
     * included in the repository.  For example, owl:imports implies rdfcache:dependsOn, so all
     * owl ontology documents referenced by StartingPoints (directly or indirectly), are read in.
     */
    public static final String dependsOn = "dependsOn";


    /**
     * is the URI of the property that connects documents to the other documents
     * that they require.  It is transitive, and in particular it is used to find the
     * other documents that the rdfcache:StartingPoint(s) depend on; those documents are also
     * included in the repository.  For example, owl:imports implies rdfcache:dependsOn, so all
     * owl ontology documents referenced by StartingPoints (directly or indirectly), are read in.
     */
    public static final String dependsOnUri = rdfCacheNamespace + dependsOn;


    /**
     * is the local name of the property that connects a rdfcache:ConstructRule to its
     * SeRQL construct statement.  These rules are executed and added to the repository
     * in the ExternalInferencing phase of the semantic processing.
     */
    public static final String hasSerqlConstructQuery = "serql_text";


    /**
     * is the URI of the property that connects a rdfcache:ConstructRule to its
     * SeRQL construct statement.  These rules are executed and added to the repository
     * in the ExternalInferencing phase of the semantic processing.
     */
    public static final String hasSerqlConstructQueryUri = rdfCacheNamespace + hasSerqlConstructQuery;


    /**
     * is the local name of the property that connects a document to the XSL transform
     * that converts it to RDF.  The codes checks for this property so that XML files can
     * be interpreted as RDF and included in the repository.  OWL-based logic in some of
     * the ontologies insure that members of certain classes have this property set, and
     * rdf:range properties of certain import statements insure that class membership is
     * established by the import statement itself.
     */
    public static final String hasXslTransformToRdf          = "hasXslTransformToRdf";


    /**
     * is the URI of the property that connects a document to the XSL transform
     * that converts it to RDF.  The codes checks for this property so that XML files can
     * be interpreted as RDF and included in the repository.  OWL-based logic in some of
     * the ontologies insure that members of certain classes have this property set, and
     * rdf:range properties of certain import statements insure that class membership is
     * established by the import statement itself.
     */
    public static final String hasXslTransformToRdfUri       = rdfCacheNamespace + hasXslTransformToRdf;


    /**
     * is the namespace for Dublin Core (dc) Terms.
     */
    public static final String dcTermNamespace               = "http://purl.org/dc/terms/";


    /**
     * is the local name for the dc term which connotes that one document has been superceded
     * by a replacement.
     */
    public static final String isReplacedBy                  = "isReplacedBy";


    /**
     * is the URI for the dc term which connotes that one document has been superceded
     * by a replacement.
     */
    public static final String isReplacedByUri               = dcTermNamespace + isReplacedBy;


    /**
     * is the URI for the RDF property <b>type</b>, which connotes class membership.
     */
    public static final String rdfType                       = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";




    /**
     * is a hashmap which maps URIs to the corresponding local resource file.  It insures that
     * the code base is used to hold the version of that file that is used by the code
     * rather than depending on the web.
     */
    public static final ConcurrentHashMap<String,String> localResources;

    static {
        localResources = new ConcurrentHashMap<String,String>();
        localResources.put("http://scm.opendap.org/svn/trunk/olfs/resources/WCS/xsl/xsd2owl.xsl", "xsl/xsd2owl.xsl");
        localResources.put("http://scm.opendap.org/svn/trunk/olfs/resources/WCS/xsl/RDFa2RDFXML.xsl", "xsl/RDFa2RDFXML.xsl");

    }


}
