package opendap.wcs.v1_1_2;

import org.jdom.Element;

import java.util.Vector;
import java.net.URI;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 9, 2009
 * Time: 8:31:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Metadata {

    private XLink xlink = null;

    private Vector<Element> content = null;

    private URI about;

    Element metadata = null;

    Metadata(Element md) throws WcsException {
        WCS.checkNamespace(md,"Metadata",WCS.OWS_NS);

        metadata = md;

    }


    public Element getElement(){
        return metadata;
    }
    
    

    

}
