package opendap.threddsHandler;

import opendap.PathBuilder;
import opendap.namespaces.THREDDS;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;


/**
 * Created by ndp on 4/21/15.
 */
public class SimpleWebServiceHandler implements WebServiceHandler {


    private String _serviceId;
    private String _base;
    private String _name;
    private String _threddsServiceType;


    public SimpleWebServiceHandler(Element serviceElement) {


        _name = serviceElement.getAttributeValue(THREDDS.NAME);
        _serviceId = _name;
        _threddsServiceType = serviceElement.getAttributeValue(THREDDS.SERVICE_TYPE);
        _base = serviceElement.getAttributeValue(THREDDS.BASE);


    }


    public SimpleWebServiceHandler(String serviceId, String name, String base, String threddsServiceType) {

        _serviceId = serviceId;
        _name = name;
        _base = base;
        _threddsServiceType = threddsServiceType;
    }


    @Override
    public void init(HttpServlet servlet, Element config) {

    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getServiceId() {
        return _serviceId;
    }

    @Override
    public String getBase() {
        return _base;
    }

    @Override
    public boolean datasetCanBeViewed(Document ddx) {
        return true;
    }

    @Override
    public String getServiceLink(String datasetUrl) {
        return _base + datasetUrl;
    }

    @Override
    public String getThreddsServiceType() {
        return _threddsServiceType;
    }


    public String getThreddsUrlPath(String datasetUrl)  {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(datasetUrl);
        return pb.toString();
    }


}
