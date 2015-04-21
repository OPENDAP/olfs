package opendap.services;

import opendap.PathBuilder;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;

/**
 * Created by ndp on 4/21/15.
 */
public class FileService implements WebServiceHandler {


    public static final String ID = "file";

    private String _serviceId;
    private String _base;
    private String _name;
    private String _threddsServiceType;



    public FileService() {

        _serviceId = ID;
        _name = "File Access Service";
        _base = "/opendap/hyrax/";
        _threddsServiceType = "File";
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
        return false;
    }

    @Override
    public String getServiceLink(String datasetUrl) {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(_base).pathAppend(datasetUrl).append(".file");
        return pb.toString();

    }

    @Override
    public String getThreddsServiceType() {
        return _threddsServiceType;
    }


    public String getThreddsUrlPath(String datasetUrl)  {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(datasetUrl).append(".file");
        return pb.toString();
        }

}
