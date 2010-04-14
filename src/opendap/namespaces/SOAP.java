package opendap.namespaces;

import org.jdom.Namespace;

/**
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 7:29:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class SOAP {

    public static final String    NAMESPACE_STRING = "http://www.w3.org/2003/05/soap-envelope";
    public static final Namespace NS = Namespace.getNamespace("soap",NAMESPACE_STRING);
    public static final String    SCHEMA_LOCATION_BASE= "http://www.w3.org/2003/05/soap-envelope/";


}
