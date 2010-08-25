package opendap.semantics.IRISail;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 25, 2010
 * Time: 12:39:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class Terms {
    public static final String internalStartingPoint         = "http://iridl.ldeo.columbia.edu/ontologies/rdfcache.owl";
    public static final String rdfCacheNamespace             = internalStartingPoint+"#";

    public static final String lastModifiedContext           = "last_modified";
    public static final String lastModifiedContextUri        = rdfCacheNamespace + lastModifiedContext;

    public static final String cacheContext                  = "cachecontext";
    public static final String cacheContextUri               = rdfCacheNamespace + cacheContext;

    public static final String contentTypeContext            = "contenttype";
    public static final String contentTypeContextUri         = rdfCacheNamespace + contentTypeContext;

    public static final String externalInferencingContext    = "externalInferencing";
    public static final String externalInferencingContextUri = rdfCacheNamespace + externalInferencingContext;

    public static final String startingPointsContext         = "startingPoints";
    public static final String startingPointsContextUri      = rdfCacheNamespace + startingPointsContext;

    public static final String startingPointType = "StartingPoint";
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


    public static final String rdfType                       = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
}
