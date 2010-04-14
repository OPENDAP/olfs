package opendap.namespaces;

import org.jdom.Element;
import org.jdom.Namespace;
import opendap.wcs.v1_1_2.WcsException;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 7:39:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class NS {

    public static boolean checkNamespace(Element e, String name, Namespace namespace)  {

        if(e==null || !e.getName().equals(name) || !e.getNamespace().equals(namespace))
            return false;

        return true;

    }


}
