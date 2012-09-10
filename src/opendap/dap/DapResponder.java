package opendap.dap;

import opendap.coreServlet.HttpResponder;
import opendap.namespaces.DAP;
import opendap.namespaces.XLINK;
import org.jdom.Element;
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

    private String _serviceRoleId = null;
    private String _serviceTitle = null;
    private String _serviceDescription = null;
    private String _serviceDescriptionLink = null;
    private String _preferredServiceSuffix = null;
    private String _serviceMediaType = null;


    public DapResponder(String sysPath, String requestSuffix) {
        this(sysPath,null,requestSuffix);
    }

    public DapResponder(String sysPath, String pathPrefix, String reqSuffixRegex) {
        super(sysPath, pathPrefix, matchAnythingRegex);

        log = LoggerFactory.getLogger(this.getClass());

        setRequestSuffixRegex(reqSuffixRegex);
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


        String xmlBase = removeRequestSuffixFromString(requestUrl);


        log.debug("@xml:base='{}'",xmlBase);
        return xmlBase;
    }


    public String removeRequestSuffixFromString(String requestString){
        String trimmedRequestString = requestString;
        String regex = requestSuffixRegex;

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(requestString);

        while (matcher.find()) {
            log.debug("removeRequestSuffixFromString() - matcher.find() found the text \""+matcher.group()+"\" starting at " +
               "index "+matcher.start()+" and ending at index "+matcher.end());

            if(matcher.end() == requestString.length()){
                trimmedRequestString = requestString.substring(0,matcher.start());
            }

        }
         return trimmedRequestString;
    }



    public Element getServiceElement(String datasetUrl){
        Element service;

        String role_id;

        String suffix = getPreferredServiceSuffix();
        role_id = getServiceRoleId();
        String title = getServiceTitle();
        service = new org.jdom.Element("Service", DAP.DAPv40_DatasetServices_NS);
        service.setAttribute("title",title);
        service.setAttribute("role",role_id);

        String descriptionText = getServiceDescription();
        String descriptionLink = getServiceDescriptionLink();

        if(descriptionText!=null  ||  descriptionLink!=null){

            Element description = new org.jdom.Element("Description",DAP.DAPv40_DatasetServices_NS);

            if(descriptionLink!=null)
                description.setAttribute("href",descriptionLink);

            if(descriptionText!=null)
                description.setText(descriptionText);

            service.addContent(description);
        }

        Element link = new Element("link",DAP.DAPv40_DatasetServices_NS);
        link.setAttribute("href",datasetUrl+suffix);
        if(getServiceMediaType()!=null)
            link.setAttribute("type",getServiceMediaType());

        service.addContent(link);


        return service;

    }


    public String getServiceMediaType(){
        return _serviceMediaType;
    }
    public String getServiceRoleId(){
        return _serviceRoleId;
    }
    public String getServiceTitle(){
        return _serviceTitle;
    }
    public String getServiceDescription(){
        return _serviceDescription;
    }
    public String getServiceDescriptionLink(){
        return _serviceDescriptionLink;
    }
    public String getPreferredServiceSuffix(){
        return _preferredServiceSuffix;
    }

    protected void setServiceRoleId(String serviceRoleId){
        _serviceRoleId = serviceRoleId;
    }
    protected void setServiceTitle(String serviceTitle){
        _serviceTitle = serviceTitle;
    }
    protected void setServiceDescription(String serviceDescription){
        _serviceDescription = serviceDescription;
    }
    protected void setServiceDescriptionLink(String serviceDescriptionLink){
        _serviceDescriptionLink =  serviceDescriptionLink;
    }
    protected void setPreferredServiceSuffix(String preferredServiceSuffix){
        _preferredServiceSuffix = preferredServiceSuffix;
    }

    protected void setServiceMediaType(String mediaType){
        _serviceMediaType = mediaType;
    }





}
