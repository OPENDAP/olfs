package opendap.wcs.v2_0;

import org.jdom.Element;

import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/9/12
 * Time: 11:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class AxisSubset {
    String _id;
    String[] _keyIDs;


    void init(){
        _id = null;
        _keyIDs = null;
    }

    AxisSubset(Element as) throws WcsException {

        init();

        Element e;

        WCS.checkNamespace(as,"AxisSubset",WCS.WCS_NS);

        e = as.getChild("Identifier",WCS.WCS_NS);
        if(e==null){
            throw new WcsException("The wcs:AxisSubset is required to have a wcs:Identifier child element.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:AxisSubset/wcs:Identifier");
        }
        _id = e.getTextNormalize();


        List keyList = as.getChildren("Key",WCS.WCS_NS);

        _keyIDs = new String[keyList.size()];

        Iterator i = keyList.iterator();
        int index = 0;
        while(i.hasNext()){
            e = (Element)i.next();
            _keyIDs[index++] = e.getText();
        }
    }

    String getID(){
        return _id;
    }

    String[] getKeys(){
        return _keyIDs;
    }

    Element getElement(){
        Element axisSubset = new Element("AxisSubset",WCS.WCS_NS);
        Element e = new Element("Identifier",WCS.WCS_NS);
        e.setText(_id);
        axisSubset.addContent(e);

        for(String keyID: _keyIDs){
            e = new Element("Key",WCS.WCS_NS);
            e.setText(keyID);
            axisSubset.addContent(e);
        }

        return axisSubset;


    }


}
