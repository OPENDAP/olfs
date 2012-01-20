package opendap.dap;

import opendap.coreServlet.HttpResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 11/28/11
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DapResponder extends HttpResponder  {

    private static String defaultRegex = ".*";

    protected String requestSuffix;

    private Logger log;


    public DapResponder(String sysPath, String requestSuffix) {
        this(sysPath,null,requestSuffix);
    }

    public DapResponder(String sysPath, String pathPrefix, String reqSuffix) {
        super(sysPath, pathPrefix, defaultRegex);

        log = LoggerFactory.getLogger(DapResponder.class);


        setRequestSuffix(reqSuffix);
    }


    public void setRequestSuffix(String reqSuffix){

        requestSuffix = reqSuffix;
        String regex;

        String conditionalSlash = "\\";
        if(!requestSuffix.startsWith("."))
            conditionalSlash = "";
        regex = defaultRegex + conditionalSlash + requestSuffix;

        setRegexPattern(regex);

    }

    public String getRequestSuffix(){
        return requestSuffix;
    }



    public String getXmlBase(HttpServletRequest req){

        String forwardRequestUri = (String)req.getAttribute("javax.servlet.forward.request_uri");
        String requestUrl = req.getRequestURL().toString();


        if(forwardRequestUri != null){
            String server = req.getServerName();
            int port = req.getServerPort();
            String scheme = req.getScheme();
            requestUrl = scheme + "://" + server + ":" + port + forwardRequestUri;
        }

        String[] urlParts = requestUrl.split(requestSuffix);

        StringBuilder xmlBase =  new StringBuilder();
        for(int i=0; i < urlParts.length; i++){
            if(xmlBase.length()>0){
                xmlBase.append(requestSuffix);
            }
            xmlBase.append(urlParts[i]);
        }

        log.debug("@xml:base='"+xmlBase+"'");
        return xmlBase.toString();
    }




}
