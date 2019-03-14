/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.wcs.v1_1_2.http;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.http.error.BadRequest;
import opendap.http.error.NotFound;
import opendap.semantics.IRISail.ProcessController;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    private String _testPath;
    private String _xmlEchoPath;
    private String _describeCoveragePath;
    private String _contextPath;
    private String _resourcePath;

    private boolean _enableUpdateUrl;


    private static final int GET_CAPABILITIES   = 0;
    private static final int DESCRIBE_COVERAGE  = 1;
    private static final int GET_COVERAGE       = 2;

    public HttpGetHandler()  {
        this(false);
    }

    public HttpGetHandler(boolean enableUpdateUrl)  {

        super();

        _enableUpdateUrl = enableUpdateUrl;
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }

    public void init(HttpServlet dispatchServlet, Element e) throws ServletException {
        init(dispatchServlet);
    }

    public void init(HttpServlet dispatchServlet, Element e, BesApi besApi) throws ServletException {
        init(dispatchServlet);
    }



    public void init(HttpServlet dispatchServlet) throws ServletException {
        String contextPath = ServletUtil.getContextPath(dispatchServlet);
        String resourcePath = ServletUtil.getSystemPath(dispatchServlet, "/");
        init(contextPath, resourcePath);
    }



    public void init(String contextPath, String resourcePath) throws ServletException {


        if (_initialized) return;


        _testPath              = "test";
        _xmlEchoPath           = "echoXML";
        _describeCoveragePath  = "describeCoverage";


        _resourcePath = resourcePath;
        log.debug("_resourcePath: "+_resourcePath);


        _contextPath = contextPath;
        log.debug("_contextPath: "+_contextPath);

        _initialized = true;
        log.info("Initialized. ");

    }


    public boolean isUpdateUrlEnabled(){
        return _enableUpdateUrl;
    }

    public void setEnableUpdateUrl(boolean state){
        _enableUpdateUrl = state;
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
        log.info("Shutting down catalog.");
        CatalogWrapper.destroy();
        log.info("Destroy Complete");
    }


    private boolean wcsRequestDispatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       boolean sendResponse)
            throws Exception {

        String dataAccessBase = Util.getServiceUrl(request);
        String relativeURL = ReqInfo.getLocalUrl(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        String serviceURL = Util.getServiceUrl(request);
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
                        KvpHandler.processKvpWcsRequest(serviceURL, dataAccessBase,query,response);
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
                    else if(_enableUpdateUrl && relativeURL.equals("update()")){
                        log.info("Updating catalog.");
                        update(request, response);
                        log.info("Catalog update complete.");
                    }
                    else if(q==null){
                        sendCapabilitesPresentationPage(request, response);
                        log.info("Sent WCS Capabilities Response Presentation Page.");
                    }
                    else {
                        String msg = "The request does not resolve to a WCS service operation that this server supports.";
                        log.error("doPost() - {}",msg);
                        throw new BadRequest(msg);
                    }

                }
        }

        return isWcsEndPoint;

    }


    public void update(HttpServletRequest request, HttpServletResponse response) throws Exception{
        ServletOutputStream sos = response.getOutputStream();

        if(ProcessController.isCurrentlyProcessing()){
            sos.println("<html><body><hr/><hr/>");
            sos.println("<h3>Catalog is currently being updated...</h3>");
            sos.println("<hr/><hr/></body></html>");

        }
        else {
            sos.println("<html><body><hr/><hr/>");
            sos.println("<h3>Last WCS Catalog update completed in "+ProcessController.getLastProcessingElapsedTime()/1000.0+" seconds.</h3>");
            sos.println("<h3>Starting catalog update. This may take some time to complete...</h3>");
            sos.println("<hr/><hr/></body></html>");
            sos.flush();

            Thread updater = new Thread(new catalogUpdater());
            updater.start();

        }


    }
    public class catalogUpdater implements Runnable {

       public void run(){
           try {
           CatalogWrapper.update();
           } catch (Exception e) {
               log.error("catalogUpdater(): Caught "+e.getClass().getName()+" Message: "+e.getMessage());
           }
       }
    }





    public void sendDescribeCoveragePage(HttpServletRequest request, HttpServletResponse response) throws Exception {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        String xsltDoc = _resourcePath+ "xsl/coverageDescription.xsl";
        log.debug("sendDescribeCoveragePage()  xsltDoc: "+xsltDoc);

        String serviceUrl = Util.getServiceUrl(request);
        log.debug("sendDescribeCoveragePage()  serviceUrl: "+serviceUrl);

        String id = request.getQueryString();
        Element cde = CatalogWrapper.getCoverageDescriptionElement(id);
        if(cde==null) {
            String msg = "Unable to locate the WCS Coverage Description for "+id;
            throw new NotFound(msg);
        }
        Document coverageDescription = new Document(cde);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "HTML wcs:DescribeCoverage");


        opendap.xml.Transformer t = new   opendap.xml.Transformer(xsltDoc);
        t.setParameter("ServicePrefix",serviceUrl);
        t.setParameter("UpdateIsRunning",ProcessController.isCurrentlyProcessing()+"");

        XdmNode descCover = t.build(new StreamSource(new ByteArrayInputStream(xmlo.outputString(coverageDescription).getBytes())));

        t.transform(descCover,response.getOutputStream());




    }

    public void sendCapabilitesPresentationPage(HttpServletRequest request, HttpServletResponse response) throws Exception {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        String xsltDoc =  _resourcePath + "xsl/capabilities.xsl";
        log.debug("sendCapabilitesPresentationPage()  xsltDoc: "+xsltDoc);

        String serviceUrl = Util.getServiceUrl(request);
        log.debug("sendCapabilitesPresentationPage()  serviceUrl: "+serviceUrl);

        Document capabilitiesDoc = CapabilitiesRequestProcessor.getFullCapabilitiesDocument(serviceUrl);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "HTML wcs:Capabilities");

        opendap.xml.Transformer t = new   opendap.xml.Transformer(xsltDoc);
        t.setParameter("ServicePrefix",serviceUrl);
        t.setParameter("ServerIDs",getServerIDs(t.getDocumentBuilder()));
        t.setParameter("UpdateIsRunning",ProcessController.isCurrentlyProcessing()+"");

        XdmNode capDoc = t.build(new StreamSource(new ByteArrayInputStream(xmlo.outputString(capabilitiesDoc).getBytes())));

        t.transform(capDoc,response.getOutputStream());



    }


    private XdmNode getServerIDs(DocumentBuilder build) throws SaxonApiException {


        String nodeString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
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
            WcsExceptionReport er = new WcsExceptionReport(e);
            response.getOutputStream().println(er.toString());
        }



    }







    public void testWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws InterruptedException, IOException {

        HashMap<String,String> keyValuePairs = new HashMap<String,String>();
        String serviceUrl = Util.getServiceUrl(request);

        String url = Scrub.completeURL(request.getRequestURL().toString());
        String query = Scrub.completeURL(request.getQueryString());

        response.setContentType("text/html");

        String page = "<html>";
        page += "    <head>";
        page += "        <link rel='stylesheet' href='"+serviceUrl+"/docs/css/contents.css' type='text/css' >";
        page += "        <title>OPeNDAP Hyrax WCS Test</title>";
        page += "    </head>";
        page += "    <body>";
        page += "    <img alt=\"OPeNDAP Logo\" src='"+serviceUrl+"/docs/images/logo.png'/>";
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
            WcsExceptionReport er = new WcsExceptionReport(e);

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


    public String getCapabilitiesTestPage(String serviceUrl, HashMap<String,String> keyValuePairs) throws InterruptedException, WcsException {

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


    public String describeCoverageTestPage(HashMap<String,String> keyValuePairs) throws InterruptedException, WcsException {

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




    public String getCoverageTestPage(HttpServletRequest req, HashMap<String,String> keyValuePairs) throws InterruptedException, WcsException {

        GetCoverageRequest getCoverageRequest = new GetCoverageRequest(keyValuePairs);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String page = "";


        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += getCoverageRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";

        Document wcsResponse = KvpHandler.getStoredCoverage(keyValuePairs);

        page += xmlo.outputString(wcsResponse).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";


        return page;
    }









}
