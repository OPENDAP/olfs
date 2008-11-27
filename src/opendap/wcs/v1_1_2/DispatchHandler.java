/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.wcs.v1_1_2;

import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.bes.BesXmlAPI;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.util.HashMap;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:01:59 PM
 */
public class DispatchHandler implements opendap.coreServlet.DispatchHandler {


    private Logger log;
    private boolean _initialized;
    private opendap.coreServlet.DispatchServlet dispatchServlet;
    private String _prefix = "wcsGateway/";

    private Element _config;


    private String _spoolPath;
    private String _testPath;
    private String _xmlEchoPath;


    private static final int GET_CAPABILITIES   = 0;
    private static final int DESCRIBE_COVERAGE  = 1;
    private static final int GET_COVERAGE       = 2;


    public DispatchHandler() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }


    public void init(DispatchServlet servlet, Element config) throws Exception {
        if (_initialized) return;

        dispatchServlet = servlet;
        _config = config;

        ingestPrefix();

        _initialized = true;
    }


    private void ingestPrefix() throws Exception{

        String msg;

        Element e = _config.getChild("prefix");
        if(e!=null)
            _prefix = e.getTextTrim();

        if(_prefix.equals("/")){
            msg = "Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <prefix>  " +
                    "child element whose value may not be equal to \"/\"";
            log.error(msg);
            throw new Exception(msg);
        }


        //if(!_prefix.startsWith("/"))
        //    _prefix = "/" + _prefix;

        if(_prefix.startsWith("/"))
            _prefix = _prefix.substring(1, _prefix.length());

        while(_prefix.endsWith("/")){
            _prefix = _prefix.substring(0,_prefix.length()-2);
        }

        _spoolPath = _prefix + "/spool";
        _testPath = _prefix + "/test";
        _xmlEchoPath = _prefix + "/echoXML";

        log.info("Initialized. prefix="+ _prefix);

    }




    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return wcsRequestDispatch(request, null, false);
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        wcsRequestDispatch(request, response, true);
    }



    private boolean wcsRequestDispatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       boolean sendResponse)
            throws Exception {

        String relativeURL = ReqInfo.getFullSourceName(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        boolean isWcsEndPoint = false;

        if (relativeURL != null) {

            if (relativeURL.startsWith(_prefix)) {

                isWcsEndPoint = true;

                if (sendResponse) {

                    if(relativeURL.equals(_prefix)){
                        processWcsRequest(request, response);
                        log.info("Sent WCS Response");
                    }
                    else if(relativeURL.startsWith(_spoolPath)){
                        streamDataResponse(request, response);
                        log.info("Sent WCS Data Stream");
                    }
                    else if(relativeURL.startsWith(_testPath)){
                        testWcsRequest(request, response);
                        log.info("Sent WCS Test Page");
                    }
                    else if(relativeURL.startsWith(_xmlEchoPath)){
                        echoWcsRequest(request, response);
                        log.info("Returning KVP request as XML docuemtn.");
                    }

                    else if(relativeURL.startsWith(_xmlEchoPath)){
                        echoWcsRequest(request, response);
                        log.info("Returning KVP request as XML docuemtn.");
                    }

                }
            }
        }

        return isWcsEndPoint;

    }





    public void processWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        HashMap<String,String> keyValuePairs = new HashMap<String,String>();

        String query = Scrub.urlContent(request.getQueryString());

        response.setContentType("text/xml");

        try {

            switch(getRequestType(query,keyValuePairs)){

                case  GET_CAPABILITIES:
                    getCapabilities(keyValuePairs,response.getOutputStream());
                    break;

                case  DESCRIBE_COVERAGE:
                    describeCoverage(keyValuePairs,response.getOutputStream());
                    break;

                case GET_COVERAGE:
                    getCoverage(keyValuePairs,response.getOutputStream());
                    break;

                default:
                    throw new WcsException("getRequestType() returned an invalid value.",WcsException.NO_APPLICABLE_CODE);
            }

        }
        catch(WcsException e){
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);
            response.getOutputStream().println(er.toString());
        }



    }






    public long getLastModified(HttpServletRequest req) {
        return -1;
    }

    public void destroy() {
        log.info("Destroy Complete");
    }



    public void getCapabilities(HashMap<String,String> keyValuePairs, ServletOutputStream os) throws WcsException {
        throw new WcsException("GetCapabilities is not supported (yet).",WcsException.OPERATION_NOT_SUPPORTED);
    }

    public void describeCoverage(HashMap<String,String> keyValuePairs, ServletOutputStream os) throws WcsException {
        throw new WcsException("DescribeCoverage is not supported (yet).",WcsException.OPERATION_NOT_SUPPORTED);
    }


    public void getCoverage(HashMap<String,String> keyValuePairs, ServletOutputStream os) throws WcsException {

        GetCoverageRequest wcsRequest = new GetCoverageRequest(keyValuePairs);

        Element besRequest = new Element ("request",BesXmlAPI.BES_NS);
        besRequest.setAttribute("reqID","###");

        besRequest.addContent(wcsRequest.getRequestElement());

        Document besReqDoc = new Document(besRequest);


        /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
         *
         * @todo Do It Like This Once We Fix The BES output!
         *
        try {
            if(!BesXmlAPI.besTransaction(_prefix,besReqDoc, os, os)){
                log.debug("WCS GetCoverage request failed. ");
                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                log.debug("besRequest:\n"+xmlo.outputString(besRequest));
            }
        }
        catch(Exception e){
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }
        - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


        //#####################################################################
        // @todo  Remove this section when the BES output is fixed.
        try {
            Document besResponse = new Document();
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            if(!BesXmlAPI.besTransaction(_prefix,besReqDoc, besResponse)){
                log.debug("WCS GetCoverage request failed. ");
                log.debug("besRequest:\n"+xmlo.outputString(besRequest));



            }

            Element root = besResponse.getRootElement();
            Element re = root.getChild("response");
            Element wcsContent = re.getChild("Coverages",WCS.WCS_NS);

            if(wcsContent == null)
                wcsContent = re.getChild("ExceptionReport",WCS.OWS_NS);

            if(wcsContent == null)
                throw new WcsException("Failed to find correct WCS content in " +
                        "BES response.\n\n"+xmlo.outputString(re),WcsException.NO_APPLICABLE_CODE);


            besResponse.detachRootElement();
            wcsContent.detach();
            besResponse.setRootElement(wcsContent);
            xmlo.output(besResponse,os);

        }
        catch(Exception e){
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }
        //#####################################################################

    }







    public void echoWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        HashMap<String,String> keyValuePairs = new HashMap<String,String>();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        Document reqDoc;
        String query = Scrub.urlContent(request.getQueryString());

        response.setContentType("text/xml");

        try {

            switch(getRequestType(query,keyValuePairs)){

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
                    throw new WcsException("getRequestType() returned an invalid value.",WcsException.NO_APPLICABLE_CODE);
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

        String url = Scrub.completeURL(request.getRequestURL().toString());
        String query = Scrub.completeURL(request.getQueryString());

        response.setContentType("text/html");

        String page = "<html>";
        page += "    <head>";
        page += "        <link rel='stylesheet' href='/opendap/docs/css/contents.css' type='text/css' >";
        page += "        <title>OPeNDAP Hyrax WCS Test</title>";
        page += "    </head>";
        page += "    <body>";
        page += "    <h2>OPeNDAP WCS Test Harness</h2>";
        page += "    How Nice! You sent a WCS request.";
        page += "    <h3>KVP request: </h3>";
        page += "    <pre>"+url+"?"+query+"</pre>";

        try {

            switch(getRequestType(query,keyValuePairs)){

                case  GET_CAPABILITIES:
                    page += getCapabilitiesTestPage(keyValuePairs);
                    break;

                case  DESCRIBE_COVERAGE:
                    page += describeCoverageTestPage(keyValuePairs);
                    break;

                case GET_COVERAGE:
                    page += getCoverageTestPage(keyValuePairs);
                    break;

                default:
                    throw new WcsException("getRequestType() returned an invalid value.",WcsException.NO_APPLICABLE_CODE);
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




    public String getCapabilitiesTestPage(HashMap<String,String> keyValuePairs) throws WcsException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        GetCapabilitiesRequest wcsRequest = new GetCapabilitiesRequest(keyValuePairs);
        String page="";

        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += wcsRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        Element besRequest = new Element ("request",BesXmlAPI.BES_NS);
        besRequest.setAttribute("reqID","###");

        besRequest.addContent(wcsRequest.getRequestElement());

        Document besReqDoc = new Document(besRequest);

        Document responseDoc = new Document();

        try {
            if(!BesXmlAPI.besTransaction(_prefix,besReqDoc, responseDoc)){
                log.debug("WCS GetCapabilities request failed.");
                log.debug(xmlo.outputString(responseDoc));
            }
            page += "    <h3>The BES response: </h3>";
            page += "    <pre>";
            page += xmlo.outputString(responseDoc).replaceAll("<","&lt;").replaceAll(">","&gt;");
            page += "    </pre>";


        }
        catch(Exception e){
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }

        Element root = responseDoc.getRootElement();
        Element re = root.getChild("response");
        Element coverages = re.getChild("Capabilities",WCS.OWS_NS);

        responseDoc.detachRootElement();
        coverages.detach();
        responseDoc.setRootElement(coverages);

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";
        page += xmlo.outputString(responseDoc).replaceAll("<","&lt;").replaceAll(">","&gt;");
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


        Element besRequest = new Element ("request",BesXmlAPI.BES_NS);
        besRequest.setAttribute("reqID","###");

        besRequest.addContent(wcsRequest.getRequestElement());

        Document besReqDoc = new Document(besRequest);

        Document responseDoc = new Document();

        try {
            if(!BesXmlAPI.besTransaction(_prefix,besReqDoc, responseDoc)){
                log.debug("WCS DescribeCoverage request failed.");
                log.debug(xmlo.outputString(responseDoc));
            }
            page += "    <h3>The BES response: </h3>";
            page += "    <pre>";
            page += xmlo.outputString(responseDoc).replaceAll("<","&lt;").replaceAll(">","&gt;");
            page += "    </pre>";
        }
        catch(Exception e){
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }
        Element root = responseDoc.getRootElement();
        Element re = root.getChild("response");
        Element coverages = re.getChild("CoverageDescriptions",WCS.WCS_NS);

        responseDoc.detachRootElement();
        coverages.detach();
        responseDoc.setRootElement(coverages);

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";
        page += xmlo.outputString(responseDoc).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        return page;

    }




    public String getCoverageTestPage(HashMap<String,String> keyValuePairs) throws WcsException {

        GetCoverageRequest wcsRequest = new GetCoverageRequest(keyValuePairs);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String page = "";


        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += wcsRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";


        Element besRequest = new Element ("request",BesXmlAPI.BES_NS);
        besRequest.setAttribute("reqID","###");

        besRequest.addContent(wcsRequest.getRequestElement());

        Document besReqDoc = new Document(besRequest);

        Document responseDoc = new Document();

        try {
            if(!BesXmlAPI.besTransaction(_prefix,besReqDoc, responseDoc)){
                log.debug("WCS GetCoverage request failed.");
                log.debug(xmlo.outputString(responseDoc));
            }
            page += "    <h3>The BES response: </h3>";
            page += "    <pre>";
            page += xmlo.outputString(responseDoc).replaceAll("<","&lt;").replaceAll(">","&gt;");
            page += "    </pre>";

        }
        catch(Exception e){
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);
        }


        Element root = responseDoc.getRootElement();
        Element re = root.getChild("response");
        Element coverages = re.getChild("Coverages",WCS.WCS_NS);

        responseDoc.detachRootElement();
        coverages.detach();
        responseDoc.setRootElement(coverages);

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";
        page += xmlo.outputString(responseDoc).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";



        return page;
    }


    public void streamDataResponse(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {

        String relativeURL = getRelativeURL(request);
        String dataSource =  relativeURL.substring(_spoolPath.length(),

                                                  relativeURL.length());

        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesXmlAPI.writeFile(
                dataSource,
                os,
                erros)){
            String msg = new String(erros.toByteArray());
            log.error("BES Error. Message: \n"+msg);
            os.write(msg.getBytes());

        }


    }




    private String getRelativeURL(HttpServletRequest request){
        String relativeURL = ReqInfo.getFullSourceName(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        return relativeURL;
    }



    private int getRequestType(String query, HashMap<String,String> keyValuePairs) throws WcsException{

        if(query==null)
            throw new WcsException("Missing WxS query string.",
                    WcsException.MISSING_PARAMETER_VALUE,"service");

        String[] pairs = query.split("&");

        String[] tmp;

        for(String pair: pairs){
            tmp = pair.split("=");
            if(tmp.length != 2)
                throw new WcsException("Poorly formatted request URL.",
                        WcsException.MISSING_PARAMETER_VALUE,
                        tmp[0]);

            keyValuePairs.put(tmp[0],tmp[1]);
        }

        // Make sure the client is looking for a WCS service....
        String s = keyValuePairs.get("service");
        if(s==null || !s.equals("WCS"))
            throw new WcsException("Only the WCS service (version "+
                    WCS.VERSIONS+") is supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,s);


        s = keyValuePairs.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(s.equals("GetCapabilities")){
            return GET_CAPABILITIES;
        }
        else if(s.equals("DescribeCoverage")){
            return DESCRIBE_COVERAGE;
        }
        else if(s.equals("GetCoverage")){
            return GET_COVERAGE;
        }
        else {
            throw new WcsException("The parameter 'request' has an invalid " +
                    "value of '"+s+"'.",
                    WcsException.INVALID_PARAMETER_VALUE,"request");
        }


    }






}
