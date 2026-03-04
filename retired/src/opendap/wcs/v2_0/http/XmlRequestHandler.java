/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0.http;

import opendap.PathBuilder;
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.dap.User;
import opendap.io.HyraxStringEncoding;
import opendap.ppt.PPTException;
import opendap.wcs.v2_0.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.Date;

/**
 * This class is responsible for processing WCS XML request documents.
 *
 * Supports WCS-2.0.1 only
 */
public class XmlRequestHandler implements opendap.coreServlet.DispatchHandler, WcsResponder {
    protected Logger log;
    //protected HttpServlet dispatchServlet;

    protected boolean _initialized;
    private String _prefix;

    protected Element _config;


    public XmlRequestHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
    }
    public void init(HttpServlet servlet, Element config) throws Exception {
        init(servlet,config,null);
    }

    public void init(HttpServlet servlet, Element config, BesApi besApi) throws Exception {
        if (_initialized) return;

        //dispatchServlet = servlet;
        _config = config;
        ingestPrefix();
        _initialized = true;
    }


    private void ingestPrefix() {

        Element e = _config.getChild("prefix");
        if(e!=null)
            _prefix = e.getTextTrim();

        if(_prefix.startsWith("/"))
            _prefix = _prefix.substring(1, _prefix.length());

        while(_prefix.endsWith("/")){
            _prefix = _prefix.substring(0,_prefix.length()-2);
        }


        log.info("Initialized. prefix="+ _prefix);

    }



    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return wcsRequestDispatch(request, null, false);
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        wcsRequestDispatch(request, response, true);
    }

    public long getLastModified(HttpServletRequest req) {
        return new Date().getTime();
    }

    public void destroy() {

    }


    private boolean wcsRequestDispatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       boolean sendResponse) throws InterruptedException {

        String relativeURL = ReqInfo.getLocalUrl(request);

        if (relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1, relativeURL.length());


        boolean isWcsEndPoint = false;
        if (relativeURL != null) {
            if (relativeURL.startsWith(_prefix)) {
                isWcsEndPoint = true;
                if (sendResponse){

                    try {

                        handleWcsRequest(request, response);
                    }
                    catch (WcsException wcse) {
                        log.error(wcse.getMessage());
                        WcsExceptionReport er = new WcsExceptionReport(wcse);
                        handleWcsError(er,response);
                    }

                }
            }
        }

        return isWcsEndPoint;

    }


    private void handleWcsRequest(HttpServletRequest request, HttpServletResponse response) throws WcsException, InterruptedException {

        User user = new User(request);
        BufferedReader sis = getRequestReader(request);
        String encoding = getEncoding(request);

        Document wcsRequestDoc = parseWcsRequest(sis, encoding);

        Element wcsRequest = wcsRequestDoc.getRootElement();
        String serviceUrl = PathBuilder.pathConcat(Util.getServiceUrl(request),_prefix);

        String requestUrl=HttpGetHandler.getRequestUrlWithQuery(request);

        handleWcsRequest(user, wcsRequest,serviceUrl,requestUrl, response);



    }

    private void handleWcsRequest(User user, Element wcsRequest, String serviceUrl, String requestUrl, HttpServletResponse response) throws WcsException, InterruptedException {

        Document wcsResponse;
        switch (getRequestType(wcsRequest)) {

            case GET_CAPABILITIES:
                GetCapabilitiesRequest getCapabilitiesRequest = new  GetCapabilitiesRequest(wcsRequest);
                wcsResponse = getCapabilities(user, getCapabilitiesRequest, serviceUrl);
                sendWcsResponse(wcsResponse,response);
                break;

            case DESCRIBE_COVERAGE:
                DescribeCoverageRequest wcsDCR = new DescribeCoverageRequest(wcsRequest);
                wcsResponse = describeCoverage(user, wcsDCR);
                sendWcsResponse(wcsResponse,response);
                break;

            case GET_COVERAGE:
                GetCoverageRequest getCoverageRequest = new GetCoverageRequest(user, requestUrl,wcsRequest);
                try {
                    sendCoverageResponse(user, getCoverageRequest, response);
                } catch (IOException | PPTException | BadConfigurationException | BESError e) {
                    throw new WcsException("FAILED to complete the GetCoverage operation, :(  Caught "+
                    e.getClass().getName()+ "  message: "+ e.getMessage(),WcsException.NO_APPLICABLE_CODE);
                }
                break;

            default:
                throw new WcsException("The request document  was  invalid. " +
                        "The root element was name: '" + wcsRequest.getName() + "' in namespace: '" + wcsRequest.getNamespace().getURI() + "'.",
                        WcsException.MISSING_PARAMETER_VALUE, "wcs:GetCapabilities,wcs:DescribeCoverage,wcs:GetCoverage");

        }


    }

    private void handleWcsError(WcsExceptionReport er, HttpServletResponse response) {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        try {
            ServletOutputStream os = response.getOutputStream();
            response.setStatus(er.getHttpStatusCode());
            xmlo.output(er.getReport(),os);
        } catch (IOException e) {
            log.error("FAILED to transmit WcsException to client. Message: ",e.getMessage());
        }


    }



    private void sendWcsResponse(Document wcsResponse, HttpServletResponse response) throws WcsException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        try {
            response.setContentType("text/xml");
            ServletOutputStream os = response.getOutputStream();
            xmlo.output(wcsResponse, os);
        } catch (IOException e) {
            throw new WcsException(e.getMessage(), WcsException.NO_APPLICABLE_CODE);
        }

    }


    static public class NoOpEntityResolver implements EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new StringReader(""));
        }
    }



    public Document parseWcsRequest(BufferedReader sis, String encoding) throws WcsException {


        String sb = "";
        StringBuilder reqDocBuilder = new StringBuilder();
        int length;

        while (sb != null) {
            try {
                sb = sis.readLine();
                if (sb != null) {

                    length = sb.length() + reqDocBuilder.length();
                    if (length > WCS.MAX_REQUEST_LENGTH) {
                        throw new WcsException("Post Body (WCS Request Document) too long. Try again with something smaller.",
                                WcsException.INVALID_PARAMETER_VALUE,
                                "WCS Request Document");
                    }
                    reqDocBuilder.append(sb);
                }
            } catch (IOException e) {
                throw new WcsException("Failed to read WCS Request Document. Mesg: " + e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "WCS Request Document");
            }
        }
        String reqDoc = reqDocBuilder.toString();

        try {
            reqDoc = URLDecoder.decode(reqDoc, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new WcsException("Failed to URLDecode Wcs Request Document. Attempted with encoding '" + encoding + "'  Message: " + e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document.");
        }


        Document requestDoc;
        try {
            // Parse the XML doc into a Document object.
            SAXBuilder saxBuilder = new SAXBuilder();

            // I added these next two bits to stop ENTITY resolution,
            // which is important for security reasons - ndp
            saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxBuilder.setEntityResolver(new NoOpEntityResolver());

            ByteArrayInputStream baos = new ByteArrayInputStream(reqDoc.getBytes(HyraxStringEncoding.getCharset()));
            requestDoc = saxBuilder.build(baos);
            return requestDoc;
        } catch (Exception e) {
            throw new WcsException("Failed to parse WCS request. Message: " + e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document");
        }


    }



    public Document getCapabilities(User user, GetCapabilitiesRequest wcsRequest, String serviceUrl) throws InterruptedException, WcsException {

        return GetCapabilitiesRequestProcessor.processGetCapabilitiesRequest(user, wcsRequest, serviceUrl);
    }


    public Document describeCoverage(User user, DescribeCoverageRequest wcsRequest) throws InterruptedException, WcsException {

        return DescribeCoverageRequestProcessor.processDescribeCoveragesRequest(user, wcsRequest);
    }



    /**
     *
     * @param req    A GetCoverageREquest object.
     * @param response  The HttpServletResponse to which the coverage will be sent.
     * @throws WcsException  When bad things happen.
     * @throws InterruptedException When it gets interrupted.
     */
    public void sendCoverageResponse(User user, GetCoverageRequest req, HttpServletResponse response) throws InterruptedException, WcsException, IOException, PPTException, BadConfigurationException, BESError {

        GetCoverageRequestProcessor.sendCoverageResponse(user, req, response, false );

    }



    private static WCS.REQUEST getRequestType(Element req) throws WcsException{
        if(req == null){
            throw new WcsException("Poorly formatted WCS request. Missing " +
                    "root element of document.",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }

        String name = req.getName();

        if(name.equals(WCS.REQUEST.GET_CAPABILITIES.toString())){
            return WCS.REQUEST.GET_CAPABILITIES;
        }
        else if(name.equals(WCS.REQUEST.DESCRIBE_COVERAGE.toString())){
            return WCS.REQUEST.DESCRIBE_COVERAGE;
        }
        else if(name.equals(WCS.REQUEST.GET_COVERAGE.toString())){
            return WCS.REQUEST.GET_COVERAGE;
        }
        else {
            throw new WcsException("The request document  was  invalid. " +
                    "The root element was name: '"+name+"' in namespace: '"+req.getNamespace().getURI()+"'.",
                    WcsException.MISSING_PARAMETER_VALUE,"wcs:GetCapabilities,wcs:DescribeCoverage,wcs:GetCoverage");
        }
    }


    public  String getEncoding(HttpServletRequest request){

        String encoding = request.getCharacterEncoding();
        if(encoding==null)
            encoding = "UTF-8";

        return encoding;
    }

    public  BufferedReader getRequestReader(HttpServletRequest request) throws WcsException {
        BufferedReader sis;
        try {
            sis = request.getReader();
        } catch (IOException e) {
            throw new WcsException("Failed to retrieve WCS Request document input stream. Message: " + e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document");
        }
        return sis;

    }

}
