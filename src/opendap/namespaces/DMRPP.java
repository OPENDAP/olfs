package opendap.namespaces;

import org.jdom.Namespace;

public class DMRPP {
    /**
     * This class should never be instantiated.
     */
    private DMRPP(){ throw new IllegalStateException("opendap.namespaces.DMRPP class"); }

    public static final String    NAMESPACE_STRING = "http://xml.opendap.org/dap/dmrpp/1.0.0#";
    public static final Namespace NS = Namespace.getNamespace("dmrpp",NAMESPACE_STRING);

    public static final String TRUST = "trust";
    public static final String CHUNKS = "chunks";
    public static final String CHUNK = "chunk";
    public static final String HREF = "href";
    public static final String COMPRESSION_TYPE = "compressionType";
    public static final String CHUNK_DIMENSION_SIZES   = "chunkDimensionSizes";
    public static final String OFFSET  = "offset";
    public static final String NBYTES   = "nBytes";
    public static final String CHUNK_POSITION_IN_ARRAY   = "chunkPositionInArray";

}
