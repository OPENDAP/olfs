package opendap.viewers;

import opendap.services.Service;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 5/30/14
 * Time: 6:46 PM
 * To change this template use File | Settings | File Templates.
 */
public interface WebServiceHandler extends Service {

    public void init(HttpServlet servlet, Element config);

    public String getName();

    public boolean datasetCanBeViewed(Document ddx);

    public String getServiceLink(String datasetUrl);


}
