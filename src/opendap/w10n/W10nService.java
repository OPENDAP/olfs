package opendap.w10n;

import opendap.PathBuilder;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;

/**
 * Created by ndp on 4/15/15.
 */
public class W10nService implements WebServiceHandler {

    public static final String ID = "w10n";

    private String _serviceId;
    private String _base;
    private String _name;
    private String _threddsServiceType;



    public W10nService() {

        _serviceId = ID;
        _name = ID +" Service";
        _base = "/opendap/"+ID;
        _threddsServiceType = ID;
    }

    @Override
    public void init(HttpServlet servlet, Element config) {
        String base = servlet.getServletContext().getContextPath() + "/" +servlet.getServletName() + "/";
        setBase(base);
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

    public void setBase(String base) {
        _base = base;
    }

    @Override
    public boolean datasetCanBeViewed(Document ddx) {
        return true;
    }

    @Override
    public String getServiceLink(String datasetUrl) {

        PathBuilder pb = new PathBuilder();
        pb.append(_base).pathAppend(datasetUrl).append("/");
        return pb.toString();
    }



    @Override
    public String getThreddsServiceType() {
        return _threddsServiceType;
    }

    public String getThreddsUrlPath(String datasetUrl)  {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(datasetUrl).append("/");
        return pb.toString();
    }



}

