package opendap.namespaces;

import org.jdom.Namespace;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jan 12, 2009
 * Time: 12:49:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class DAP {
    public static final String    DAPv32_NAMESPACE_STRING = "http://xml.opendap.org/ns/DAP3.2";
    public static final Namespace DAPv32_NS = Namespace.getNamespace("dap",DAPv32_NAMESPACE_STRING);
    public static final String    DAPv32_SCHEMA_LOCATION= "http://xml.opendap.org/dap/dap3.2.xsd";
    
    public static final String    DAPv40_NAMESPACE_STRING = "http://xml.opendap.org/ns/DAP.0";
    public static final Namespace DAPv40_NS = Namespace.getNamespace("dap",DAPv40_NAMESPACE_STRING);
    public static final String    DAPv40_SCHEMA_LOCATION= "http://xml.opendap.org/dap/dap4.0.xsd";


}
