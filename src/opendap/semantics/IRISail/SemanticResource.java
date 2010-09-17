package opendap.semantics.IRISail;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 17, 2010
 * Time: 11:17:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class SemanticResource {
    private String _localId;
    private String _namespaceUri;
    public SemanticResource(String namespaceUri, String localId){
        _localId = localId;
        _namespaceUri = namespaceUri;
    }
    public String getLocalId(){
        return _localId;
    }
    public String getUri(){
        return _namespaceUri+_localId;
    }
    public String getNamespace(){
        return _namespaceUri;
    }

    public String toString(){
        return getUri();
    }
}
