package opendap.services;

import org.jdom.Document;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 6/4/14
 * Time: 7:18 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Service {
    public String getName();

    public String getServiceId();

    public String getBase();

    public boolean datasetCanBeViewed(Document ddx);

}
