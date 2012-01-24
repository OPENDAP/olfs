package opendap.dap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 27, 2010
 * Time: 12:21:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class Request {

    private Logger log;
    private HttpServletRequest _request;

    public Request(HttpServlet servlet, HttpServletRequest request){
        log = LoggerFactory.getLogger(this.getClass());
        _request = request;
    }


    /**
     * Local ID for the service is whatever is after the third slash
     * in the URL ie after the server name (or IP) and port number combination and up to the first
     * dataset collection term in the URL.
     *
     * For example in the URL http://localhost:8080/opendap/hyrax/data/fnoc1.nc this
     * method will return /opendap/hyrax/ because the
     *
     * Or think of it as "the server" + "the service" is the minimum
     * URL to get to the DAP service. Thus http://localhost:8080/opendap/hyrax/ is the full DAP
     * service URL. for the preceding example.
     *
     *
     *
     *
     * @return
     */
    public String getDapServiceLocalID(){

        String contextName = _request.getContextPath();
        String servletName = _request.getServletPath();

        String dapService = contextName + servletName;

        log.debug("getDapServiceLocalID(): "+dapService);

        return dapService;

    }


    public String getDocsServiceLocalID(){

        String contextName = _request.getContextPath();

        String docsService = contextName + "/docs";

        log.debug("getDocsServiceLocalID(): "+docsService);

        return docsService;

    }


    public String getWebStartServiceLocalID(){

        String contextName = _request.getContextPath();

        String webStartService = contextName + "/webstart";

        log.debug("getWebStartServiceLocalID(): "+webStartService);

        return webStartService;

    }




    public String getDapServiceUrl()  {

        String transport  = _request.getScheme();
        String serverName = _request.getServerName();
        int    serverPort = _request.getServerPort();


        String dapService = getDapServiceLocalID();

        String serviceUrl = transport + "://"+ serverName + ":"+ serverPort + dapService;


        log.debug("getDapServiceURL(): "+serviceUrl);

        return serviceUrl;

    }


    /**
     * Returns the relativeURL name for this request. This is essentially the same as the value
     * of HttpServletRequest.getPathInfo() except that it is never null. If HttpServletRequest.getPathInfo()
     * is null then the full source name is "/".
     * @return The relative URL = _request.getPathInfo()
     */
    public String getRelativeUrl(){

        String name=_request.getPathInfo();

        if(name == null){ // If the requestPath is null, then we are at the top level, or "/" as it were.
            name = "/";

        }
        return name;

    }




        

}
