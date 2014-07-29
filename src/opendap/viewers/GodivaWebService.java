package opendap.viewers;

import opendap.coreServlet.ServletUtil;
import opendap.namespaces.DAP;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;

import javax.servlet.http.HttpServlet;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 5/30/14
 * Time: 9:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class GodivaWebService implements  WebServiceHandler {

    private String _serviceId;
    private String _applicationName;

    private Element _config;

    private String _godivaBase;
    private String _godivaServiceUrl;


    private String _wmsServiceBase;
    private String _wmsServiceUrl;

    public GodivaWebService(){

        _serviceId         = "godiva";
        _applicationName   = "Godiva Data Visualization";

        _godivaBase        = "/ncWMS/godiva2.html";
        _godivaServiceUrl  = "http://localhost:8080/ncWMS/godiva2.html";

        _wmsServiceBase    = "/ncWMS";
        _wmsServiceUrl     = "http://localhost:8080/ncWMS";

    }


    @Override
    public void init(HttpServlet servlet, Element config) {
        _config = config;


        Element e;
        String s;

        s =_config.getAttributeValue("serviceId");
        if(s!=null && s.length()!=0)
            _serviceId = s;

        e =_config.getChild("applicationName");
        if(e!=null){
            s = e.getTextTrim();
            if(s!=null && s.length()!=0)
                _applicationName = s;
        }


        e = _config.getChild("NcWmsService");
        if(e!=null){
            s = e.getAttributeValue("href");
            if(s!=null && s.length()!=0)
                _wmsServiceUrl = s;


            s = e.getAttributeValue("base");
            if(s!=null && s.length()!=0)
                _wmsServiceBase = s;


        }

        e = _config.getChild("Godiva");
        if(e!=null){

            s = e.getAttributeValue("href");
            if(s!=null && s.length()!=0)
                _godivaServiceUrl = s;

            s = e.getAttributeValue("base");
            if(s!=null && s.length()!=0)
                _godivaBase = s;
        }

    }

    @Override
    public String getName() {
        return _applicationName;
    }

    @Override
    public String getServiceId() {
        return _serviceId;
    }

    @Override
    public boolean datasetCanBeViewed(Document ddx) {
        Element dataset = ddx.getRootElement();

        Iterator i = dataset.getDescendants(new ElementFilter("Grid", DAP.DAPv32_NS));

        return i.hasNext();
    }

    @Override
    public String getServiceLink(String datasetUrl) {

        // note that we escape the '?' and '=' characters.
        return _godivaBase + "?server="+ _wmsServiceUrl + "%3FDATASET%3d" + _wmsServiceBase + datasetUrl;
    }

    @Override
    public String getBase() {

        return _godivaBase;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("\n");
        sb.append("    serviceId: ").append(_serviceId).append("\n");
        sb.append("    base: ").append(_godivaBase).append("\n");
        sb.append("    applicationName: ").append(_applicationName).append("\n");
        sb.append("    WmsService: ").append(_wmsServiceUrl).append("\n");
        sb.append("    Service Link: ").append(getServiceLink("<datasetUrl>")).append("\n");

        return sb.toString();
    }

    private String computeDefaultServiceBase(HttpServlet servlet){

        String context = servlet.getServletContext().getContextPath();
        String servletName = servlet.getServletName();

        String servletBase = context + "/" + servletName;






        System.out.println(ServletUtil.probeServlet(servlet));

        return "";

    }
}
