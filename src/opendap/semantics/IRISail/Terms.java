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
 */
public class Terms {

    /**
     * A comment
     */
    public static final String internalStartingPoint         = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl";

    /**
     * Another Comment
     */
    public static final String rdfCacheNamespace             = internalStartingPoint+"#";

    public static final String lastModifiedContext           = "last_modified";
    public static final String lastModifiedContextUri        = rdfCacheNamespace + lastModifiedContext;

    public static final String cacheContext                  = "cachecontext";
    public static final String cacheContextUri               = rdfCacheNamespace + cacheContext;

    public static final String contentTypeContext            = "Content-Type";
    public static final String contentTypeContextUri         = rdfCacheNamespace + contentTypeContext;

    public static final String externalInferencingContext    = "externalInferencing";
    public static final String externalInferencingContextUri = rdfCacheNamespace + externalInferencingContext;

    public static final String startingPointsContext         = "startingPoints";
    public static final String startingPointsContextUri      = rdfCacheNamespace + startingPointsContext;

    public static final String startingPointType             = "StartingPoint";
    public static final String startingPointContextUri       = rdfCacheNamespace + startingPointType;

    public static final String functionsContext              = "myfn";
    public static final String functionsContextUri           = rdfCacheNamespace + functionsContext;

    public static final String listContext                   = "mylist";
    public static final String listContextUri                = rdfCacheNamespace + listContext;

    public static final String isContainedByContext          = "isContainedBy";
    public static final String isContainedByContextUri       = rdfCacheNamespace + isContainedByContext;

    public static final String reTypeToContext               = "reTypeTo";
    public static final String reTypeToContextUri            = rdfCacheNamespace + reTypeToContext;


    public static final String dependsOnContext              = "dependsOn";
    public static final String dependsOnContextUri           = rdfCacheNamespace + dependsOnContext;
        
    public static final String serqlTextType                 = "serql_text";
    public static final String serqlTextTypeUri              = rdfCacheNamespace + serqlTextType;
    
    public static final String hasXslTransformToRdf          = "hasXslTransformToRdf";
    public static final String hasXslTransformToRdfUri       = rdfCacheNamespace + hasXslTransformToRdf;
    
    public static final String dcTermNamespace               = "http://purl.org/dc/terms/";
    public static final String isReplacedBy                  = "isReplacedBy";
    public static final String isReplacedByUri               = dcTermNamespace + isReplacedBy;
    
    public static final String rdfType                       = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";


    public static final ConcurrentHashMap<String,String> localResources;
    static {
        localResources = new ConcurrentHashMap<String,String>();
        localResources.put("http://scm.opendap.org/svn/trunk/olfs/resources/WCS/xsl/xsd2owl.xsl", "xsl/xsd2owl.xsl");

    }


}
