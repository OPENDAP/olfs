package opendap.wcs.v1_1_2.http;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.wcs.v1_1_2.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 20, 2010
 * Time: 4:31:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpGetHandler implements opendap.coreServlet.DispatchHandler {



    private final Logger log;
    private boolean _initialized;
    private HttpServlet dispatchServlet;
    private String _predfix;

    private Element _config;


    private static final String _coveragesTerminus = "/coverages";

    private String _form;
    private String _configurationForm;
    private String _testPath;
    private String _xmlEchoPath;
    private String _coveragesPath;
    private String _describeCoveragePath;
    private String _serviceContentPath;
    private String _contextPath;
    private String _resourcePath;
    private String _defaultWcsServiceConfigFilename = "wcs_service.xml";
    private String _serviceConfigFilename;
    private URL    _serviceConfigUrl;
    private static final int GET_CAPABILITIES   = 0;
    private static final int DESCRIBE_COVERAGE  = 1;
    private static final int GET_COVERAGE       = 2;


    public HttpGetHandler()  {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }

    public void init(HttpServlet dispatchServlet, Element e) throws ServletException {

        init(dispatchServlet);

        // throw new ServletException("This specialization uses a different configuration pattern ");

    }



    public void init(HttpServlet dispatchServlet) throws ServletException {

        String contextPath = ServletUtil.getContextPath(dispatchServlet);

        String resourcePath = ServletUtil.getSystemPath(dispatchServlet, "/");

        String contentPath = ServletUtil.getContentPath(dispatchServlet);

        init(contextPath, resourcePath, contentPath);

    }



    public void init(String contextPath, String resourcePath, String contentPath) throws ServletException {


        if (_initialized) return;




        String msg;

        _testPath              = "test";
        _form                  = "form";
        _configurationForm     = "config";
        _xmlEchoPath           = "echoXML";
        _coveragesPath         = _coveragesTerminus;
        _describeCoveragePath  = "describeCoverage";


        _resourcePath = resourcePath;

        log.debug("_resourcePath: "+_resourcePath);

        _serviceContentPath = contentPath;
        if(!_serviceContentPath.endsWith("/"))
            _serviceContentPath += "/";

        _contextPath = contextPath;

        log.debug("_serviceContentPath: "+_serviceContentPath);


        _initialized = true;
        log.info("Initialized. ");

    }




    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return wcsRequestDispatch(request, null, false);
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        wcsRequestDispatch(request, response, true);
    }

    public long getLastModified(HttpServletRequest req) {
        return CatalogWrapper.getLastModified();
    }

    public void destroy() {
        CatalogWrapper.destroy();
        log.info("Destroy Complete");
    }


    private boolean wcsRequestDispatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       boolean sendResponse)
            throws Exception {

        String dataAccessBase = ReqInfo.getServiceUrl(request);
        String relativeURL = ReqInfo.getRelativeUrl(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        String serviceURL = getServiceUrlString(request);
        String query      = request.getQueryString();

        boolean isWcsEndPoint = false;

        if (relativeURL != null) {

                isWcsEndPoint = true;

                if (sendResponse) {

                    String q = request.getQueryString();

                    if (relativeURL.equals("/")) {
                        response.sendRedirect(_contextPath+"/");
                        log.debug("Sent redirect from / to "+_contextPath+"/");
                    }
                    else if(relativeURL.equals("") && q!=null){
                        response.setContentType("text/xml");
                        ServletOutputStream os = response.getOutputStream();
                        KvpHandler.processKvpWcsRequest(serviceURL, dataAccessBase,query,os);
                        log.info("Sent WCS Response");
                    }
                    else if(relativeURL.startsWith(_testPath)){
                        testWcsRequest(request, response);
                        log.info("Sent WCS Test Page");
                    }
                    else if(relativeURL.startsWith(_xmlEchoPath)){
                        echoWcsRequest(request, response);
                        log.info("Returned WCS KVP request as an WCS XML request document.");
                    }
                    else if(relativeURL.startsWith(_describeCoveragePath)){
                        sendDescribeCoveragePage(request, response);
                        log.info("Returned WCS Describe Coverage Presentation Page.");
                    }
                    else if(relativeURL.equals("update()")){
                        log.info("Updating catalog.");
                        update(request, response);
                        log.info("Catalog update complete.");
                    }
                    else if(q==null){
                        sendCapabilitesPresentationPage(request, response);
                        log.info("Sent WCS Capabilities Response Presentation Page.");
                    }
                    else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }

                }
        }

        return isWcsEndPoint;

    }


    public void update(HttpServletRequest request, HttpServletResponse response) throws Exception{

        Date startTime = new Date();
        CatalogWrapper.update();
        Date endTime = new Date();

        double elapsedTime = (endTime.getTime() - startTime.getTime())/1000.0;
        ServletOutputStream sos = response.getOutputStream();

        sos.println("<html><body><h3>");
        sos.println("WCS Catalog update completed in "+elapsedTime+" seconds");        
        sos.println("</h3></body></html>");

    }




    public void sendDescribeCoveragePage(HttpServletRequest request, HttpServletResponse response) throws Exception {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        String xsltDoc = _resourcePath+ "xsl/coverageDescription.xsl";
        log.debug("sendDescribeCoveragePage()  xsltDoc: "+xsltDoc);

        String serviceUrl = getServiceUrlString(request);
        log.debug("sendDescribeCoveragePage()  serviceUrl: "+serviceUrl);

        String id = request.getQueryString();
        Element cde = CatalogWrapper.getCoverageDescriptionElement(id);
        if(cde==null)
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        Document coverageDescription = new Document(cde);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "HTML wcs:DescribeCoverage");


        opendap.xml.Transformer t = new   opendap.xml.Transformer(xsltDoc);
        t.setParameter("ServicePrefix",serviceUrl);

        XdmNode descCover = t.build(new StreamSource(new ByteArrayInputStream(xmlo.outputString(coverageDescription).getBytes())));

        t.transform(descCover,response.getOutputStream());






    }

    public void sendCapabilitesPresentationPage(HttpServletRequest request, HttpServletResponse response) throws Exception {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        String xsltDoc =  _resourcePath + "xsl/capabilities.xsl";
        log.debug("sendCapabilitesPresentationPage()  xsltDoc: "+xsltDoc);

        String serviceUrl = getServiceUrlString(request);
        log.debug("sendCapabilitesPresentationPage()  serviceUrl: "+serviceUrl);

        Document capabilitiesDoc = CapabilitiesRequestProcessor.getFullCapabilitiesDocument(serviceUrl);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "HTML wcs:Capabilities");

        opendap.xml.Transformer t = new   opendap.xml.Transformer(xsltDoc);
        t.setParameter("ServicePrefix",serviceUrl);


        t.setParameter("ServerIDs",getServerIDs(t.getDocumentBuilder()));

        XdmNode capDoc = t.build(new StreamSource(new ByteArrayInputStream(xmlo.outputString(capabilitiesDoc).getBytes())));

        t.transform(capDoc,response.getOutputStream());



    }


    private XdmNode getServerIDs(DocumentBuilder build) throws SaxonApiException {


        String nodeString = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n";
        nodeString += "<ServerIDs>";

        String serverID;
        for(String serverURL: CoverageIdGenerator.getServerURLs()){
            serverID  = CoverageIdGenerator.getServerID(serverURL);
            nodeString += "<server id='"+serverID+"' url='"+serverURL+"' />";
        }

        nodeString += "</ServerIDs>";

        ByteArrayInputStream reader = new ByteArrayInputStream(nodeString.getBytes());

        return build.build(new StreamSource(reader));
    }








    public void echoWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        HashMap<String,String> keyValuePairs = new HashMap<String,String>();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        Document reqDoc;
        String query = request.getQueryString();

        response.setContentType("text/xml");

        try {

            switch(KvpHandler.getRequestType(query,keyValuePairs)){

                case  GET_CAPABILITIES:
                    GetCapabilitiesRequest gcr = new GetCapabilitiesRequest(keyValuePairs);
                    reqDoc = new Document(gcr.getRequestElement());
                    break;

                case  DESCRIBE_COVERAGE:
                    DescribeCoverageRequest dcr = new DescribeCoverageRequest(keyValuePairs);
                    reqDoc = new Document(dcr.getRequestElement());
                    break;

                case GET_COVERAGE:
                    GetCoverageRequest gc = new GetCoverageRequest(keyValuePairs);
                    reqDoc = new Document(gc.getRequestElement());
                    break;

                default:
                    throw new WcsException("INTERNAL ERROR: getRequestType() returned an invalid value.",
                            WcsException.NO_APPLICABLE_CODE);
            }

            xmlo.output(reqDoc,response.getOutputStream());


        }
        catch(WcsException e){
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);
            response.getOutputStream().println(er.toString());
        }



    }







    public void testWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        HashMap<String,String> keyValuePairs = new HashMap<String,String>();
        String serviceUrl = getServiceUrlString(request);

        String url = Scrub.completeURL(request.getRequestURL().toString());
        String query = Scrub.completeURL(request.getQueryString());

        response.setContentType("text/html");

        String page = "<html>";
        page += "    <head>";
        page += "        <link rel='stylesheet' href='"+serviceUrl+"/docs/css/contents.css' type='text/css' >";
        page += "        <title>OPeNDAP Hyrax WCS Test</title>";
        page += "    </head>";
        page += "    <body>";
        page += "    <img alt=\"OPeNDAP Logo\" src='"+serviceUrl+"/docs/images/logo.gif'/>";
        page += "    <h2>OPeNDAP WCS Test Harness</h2>";
        page += "    How Nice! You sent a WCS request.";
        page += "    <h3>KVP request: </h3>";
        page += "    <pre>"+url+"?"+query+"</pre>";

        try {

            switch(KvpHandler.getRequestType(query,keyValuePairs)){

                case  GET_CAPABILITIES:
                    page += getCapabilitiesTestPage(serviceUrl, keyValuePairs);
                    break;

                case  DESCRIBE_COVERAGE:
                    page += describeCoverageTestPage(keyValuePairs);
                    break;

                case GET_COVERAGE:
                    page += getCoverageTestPage(request,keyValuePairs);
                    break;

                default:
                    throw new WcsException("INTERNAL ERROR: getRequestType() returned an invalid value.",
                            WcsException.NO_APPLICABLE_CODE);
            }


            page += "    </body>";
            page += "</html>";

            response.getOutputStream().println(page);

        }
        catch(WcsException e){
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);

            page += "<h1>ERROR</h1>";
            page += "    After some deliberation we have rejected your request.";
            page += "    <h3>Here's why: </h3>";
            page += "    <pre>";
            page += er.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
            page += "    </pre>";
            page += "    </body>";
            page += "</html>";

            response.getOutputStream().println(page);

        }



    }


    public String getCapabilitiesTestPage(String serviceUrl, HashMap<String,String> keyValuePairs) throws WcsException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        GetCapabilitiesRequest wcsRequest = new GetCapabilitiesRequest(keyValuePairs);
        String page="";

        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += wcsRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";

        Document wcsResponse = KvpHandler.getCapabilities(keyValuePairs,serviceUrl);

        page += xmlo.outputString(wcsResponse).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";


        return page;

    }


    public String describeCoverageTestPage(HashMap<String,String> keyValuePairs) throws WcsException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        DescribeCoverageRequest wcsRequest = new DescribeCoverageRequest(keyValuePairs);
        String page = "";

        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += wcsRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";

        Document wcsResponse = KvpHandler.describeCoverage(keyValuePairs);

        page += xmlo.outputString(wcsResponse).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";



        return page;

    }




    public String getCoverageTestPage(HttpServletRequest req, HashMap<String,String> keyValuePairs) throws WcsException {

        GetCoverageRequest getCoverageRequest = new GetCoverageRequest(keyValuePairs);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String page = "";


        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += getCoverageRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";

        Document wcsResponse = KvpHandler.getCoverage(keyValuePairs,ReqInfo.getServiceUrl(req));

        page += xmlo.outputString(wcsResponse).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";


        return page;
    }






    public static String getServiceUrlString(HttpServletRequest request){
        return ReqInfo.getServiceUrl(request);

    }









}
