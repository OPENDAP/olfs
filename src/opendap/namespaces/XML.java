package opendap.namespaces;

import org.jdom.Namespace;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/24/12
 * Time: 8:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class XML {

    public static final String NAMESPACE_STRING = "http://www.w3.org/XML/1998/namespace";
    public static final String NAMESPACE_PREFIX = "xml";

    public static final Namespace NS =  Namespace.getNamespace(NAMESPACE_PREFIX,NAMESPACE_STRING);

}
