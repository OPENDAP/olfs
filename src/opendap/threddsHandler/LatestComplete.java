package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;

import java.util.TreeMap;

/**
 * Created by ndp on 4/19/15.
 */
public class LatestComplete extends SimpleLatest {


    String _lastModifiedLimit;


    public LatestComplete (Element e){
        super(e);

        _lastModifiedLimit = e.getAttributeValue(THREDDS.LAST_MODIFIED_LIMIT);

    }

    @Override
    public Element getProxyDataset(TreeMap<String, Element> datasets) {



        Element lastDataset = datasets.get(datasets.lastKey());

        Element proxy = (Element) lastDataset.clone();

        proxy.setAttribute(THREDDS.NAME,_name);
        proxy.setAttribute(THREDDS.SERVICE_NAME,_serviceName);

        return proxy;
    }



}
