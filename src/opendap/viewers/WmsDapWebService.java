package opendap.viewers;

import opendap.coreServlet.ServletUtil;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 6/4/14
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class WmsDapWebService  implements  WebServiceHandler {



    private String _serviceId;
    private String _base;
       private String _applicationName;

       private Element _config;

       private String _wmsServiceUrl;

       public WmsDapWebService(){

           _serviceId = "wms";
           _applicationName = "WMS Service";
           _wmsServiceUrl = "http://localhost:8080/wms/wms/hyrax";
           _base = "/wms/wms/hyrax";


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


           e = _config.getChild("WmsService");

           if(e!=null){

               s = e.getAttributeValue("href");
               if(s!=null && s.length()!=0)
                   _wmsServiceUrl = s;

               s = e.getAttributeValue("base");
               if(s!=null && s.length()!=0)
                   _base = s;
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
           return true;  //To change body of implemented methods use File | Settings | File Templates.
       }

       @Override
       public String getServiceLink(String datasetUrl) {

           return  _wmsServiceUrl + datasetUrl + "?SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0" ;
       }

       public String getBase(){

           return _base;

       }

       @Override
       public String toString(){
           StringBuilder sb = new StringBuilder();
           sb.append(getClass().getSimpleName()).append("\n");
           sb.append("    serviceId: ").append(_serviceId).append("\n");
           sb.append("    base: ").append(_base).append("\n");
           sb.append("    applicationName: ").append(_applicationName).append("\n");
           sb.append("    WmsService: ").append(_wmsServiceUrl).append("\n");

           return sb.toString();
       }


}
