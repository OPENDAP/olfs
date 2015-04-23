package opendap.dap;

import opendap.PathBuilder;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;

/**
 * Created by ndp on 4/15/15.
 */
public class Dap2Service  implements WebServiceHandler {

    public static final String ID = "dap2";

    private String _serviceId;
    private String _base;
    private String _name;



    public Dap2Service() {

        _serviceId = ID;
        _name = "DAP2 Service";
        _base = "/opendap/hyrax/";
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
        pb.pathAppend(_base).pathAppend(datasetUrl).append(".ddx");
        return pb.toString();
    }

    @Override
    public String getThreddsUrlPath(String datasetUrl) {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(_base).pathAppend(datasetUrl);// .append(".ddx");
        return datasetUrl;
    }

    public String getThreddsServiceType() {
        return "OpenDAP";
    }

}
