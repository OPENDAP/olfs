package opendap.dap;

import opendap.coreServlet.HttpResponder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public abstract class DapResponder extends HttpResponder  {

    private static String matchAnythingRegex = ".*";

    protected String requestSuffixRegex;

    private Logger log;


    public DapResponder(String sysPath, String requestSuffix) {
        this(sysPath,null,requestSuffix);
    }

    public DapResponder(String sysPath, String pathPrefix, String reqSuffix) {
        super(sysPath, pathPrefix, matchAnythingRegex);

        log = LoggerFactory.getLogger(this.getClass());


        setRequestSuffixRegex(reqSuffix);
    }


    public void setRequestSuffixRegex(String reqSuffixRegex){

        requestSuffixRegex = reqSuffixRegex;

        String requestMatchRegex;

        requestMatchRegex = matchAnythingRegex + requestSuffixRegex;

        setRequestMatchRegex(requestMatchRegex);

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


        String xmlBase = trimRequestSuffix(requestUrl);


        log.debug("@xml:base='{}'",xmlBase);
        return xmlBase;
    }


    public String trimRequestSuffix(String requestString){


        String trimmedRequestString = requestString;
        String regex = requestSuffixRegex;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(requestString);

        while (matcher.find()) {
            log.debug("trimRequestSuffix() - matcher.find() found the text \""+matcher.group()+"\" starting at " +
               "index "+matcher.start()+" and ending at index "+matcher.end());

            if(matcher.end() == requestString.length()){
                trimmedRequestString = requestString.substring(0,matcher.start());
            }

        }


         return trimmedRequestString;


    }


}
