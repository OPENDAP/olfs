package opendap.threddsHandler;

import org.jdom.Element;

import java.util.TreeMap;

/**
 * Created by ndp on 4/19/15.
 */
public interface Proxy {




    public Element getProxyDataset(TreeMap<String, Element> datasets);

    public boolean isTop();




}
