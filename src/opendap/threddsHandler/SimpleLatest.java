package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;

import java.util.TreeMap;

/**
 * Created by ndp on 4/19/15.
 */
public class SimpleLatest implements Proxy {

    String _name;
    boolean _top;
    String _serviceName;

    public SimpleLatest(Element e){

        _name = e.getAttributeValue(THREDDS.NAME);
        if(_name==null)
            _name="latest";

        _top = true;
        String s = e.getAttributeValue(THREDDS.TOP);
        if(s!=null){
            _top = Boolean.parseBoolean(s);
        }


        _serviceName = e.getAttributeValue(THREDDS.SERVICE_NAME);
        if(_serviceName==null)
            _serviceName = "latest";

    }


    @Override
    public Element getProxyDataset(TreeMap<String, Element> datasets) {


        Element lastDataset = datasets.get(datasets.lastKey());

        Element proxy = (Element) lastDataset.clone();

        proxy.setAttribute(THREDDS.NAME,_name);
        proxy.setAttribute(THREDDS.SERVICE_NAME,_serviceName);

        return proxy;
    }

    public boolean isTop() {
        return _top;
    }
}
