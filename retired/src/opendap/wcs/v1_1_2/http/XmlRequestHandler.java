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

import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.wcs.v1_1_2.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 4:50:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class XmlRequestHandler implements opendap.coreServlet.DispatchHandler, WcsResponder {
    protected Logger log;
    protected HttpServlet dispatchServlet;

    protected boolean _initialized;
    protected String _prefix;

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
        return -1;
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


    public void handleWcsRequest(HttpServletRequest request, HttpServletResponse response) throws WcsException, InterruptedException {

        BufferedReader sis = getRequestReader(request);
        String encoding = getEncoding(request);

        Document wcsRequestDoc = parseWcsRequest(sis, encoding);

        Element wcsRequest = wcsRequestDoc.getRootElement();
        String serviceUrl = Util.getServiceUrlString(request, _prefix);

        handleWcsRequest(wcsRequest,serviceUrl,response);



    }

    public void handleWcsRequest(Element wcsRequest, String serviceUrl, HttpServletResponse response) throws WcsException, InterruptedException {

        Document wcsResponse;
        switch (getRequestType(wcsRequest)) {

            case WCS.GET_CAPABILITIES:
                GetCapabilitiesRequest getCapabilitiesRequest = new  GetCapabilitiesRequest(wcsRequest);
                wcsResponse = getCapabilities(getCapabilitiesRequest, serviceUrl);
                sendWcsResponse(wcsResponse,response);
                break;

            case WCS.DESCRIBE_COVERAGE:
                DescribeCoverageRequest wcsDCR = new DescribeCoverageRequest(wcsRequest);
                wcsResponse = describeCoverage(wcsDCR);
                sendWcsResponse(wcsResponse,response);
                break;

            case WCS.GET_COVERAGE:

                GetCoverageRequest wcsGCR = new GetCoverageRequest(wcsRequest);

                if (wcsGCR.isStore()) {
                    wcsResponse = getStoredCoverage(wcsGCR);
                    sendWcsResponse(wcsResponse,response);
                }
                else {
                    sendCoverageResponse(wcsGCR, response);
                }


                break;

            default:
                throw new WcsException("The request document  was  invalid. " +
                        "The root element was name: '" + wcsRequest.getName() + "' in namespace: '" + wcsRequest.getNamespace().getURI() + "'.",
                        WcsException.MISSING_PARAMETER_VALUE, "wcs:GetCapabilities,wcs:DescribeCoverage,wcs:GetCoverage");

        }


    }

    public void handleWcsError(WcsExceptionReport er, HttpServletResponse response) {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        try {
            ServletOutputStream os = response.getOutputStream();
            xmlo.output(er.getReport(),os);
        } catch (IOException e) {
            log.error("FAILED to transmit WcsException to client. Message: ",e.getMessage());
        }


    }



    public void sendWcsResponse(Document wcsResponse, HttpServletResponse response) throws WcsException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        try {
            response.setContentType("text/xml");
            ServletOutputStream os = response.getOutputStream();
            xmlo.output(wcsResponse, os);
        } catch (IOException e) {
            throw new WcsException(e.getMessage(), WcsException.NO_APPLICABLE_CODE);
        }

    }


    public Document parseWcsRequest(BufferedReader sis, String encoding) throws WcsException {


        String sb = "";
        String reqDoc = "";
        int length;

        while (sb != null) {
            try {
                sb = sis.readLine();
                if (sb != null) {

                    length = sb.length() + reqDoc.length();
                    if (length > WCS.MAX_REQUEST_LENGTH) {
                        throw new WcsException("Post Body (WCS Request Document) too long. Try again with something smaller.",
                                WcsException.INVALID_PARAMETER_VALUE,
                                "WCS Request Document");
                    }
                    reqDoc += sb;
                }
            } catch (IOException e) {
                throw new WcsException("Failed to read WCS Request Document. Mesg: " + e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "WCS Request Document");
            }
        }

        try {
            reqDoc = URLDecoder.decode(reqDoc, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new WcsException("Failed to URLDecode Wcs REquest Document. Attempted with encodeing '" + encoding + "'  Message: " + e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document.");
        }


        Document requestDoc;
        try {
            // Parse the XML doc into a Document object.
            SAXBuilder saxBuilder = new SAXBuilder();

            ByteArrayInputStream baos = new ByteArrayInputStream(reqDoc.getBytes());
            requestDoc = saxBuilder.build(baos);
            return requestDoc;
        } catch (Exception e) {
            throw new WcsException("Failed to parse WCS request. Message: " + e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document");
        }


    }



    public Document getCapabilities(GetCapabilitiesRequest wcsRequest, String serviceUrl) throws InterruptedException, WcsException {

        return CapabilitiesRequestProcessor.processGetCapabilitiesRequest(wcsRequest, serviceUrl);
    }


    public Document describeCoverage(DescribeCoverageRequest wcsRequest) throws InterruptedException, WcsException {

        return DescribeCoverageRequestProcessor.processDescribeCoveragesRequest(wcsRequest);
    }


    public Document getStoredCoverage(GetCoverageRequest req) throws InterruptedException, WcsException {


        return CoverageRequestProcessor.getStoredCoverageResponse(req);
    }

    /**
     *
     * @param req    A GetCoverageREquest object.
     * @param response  The HttpServletResponse to which the coverage will be sent.
     * @throws WcsException  When bad things happen.
     * @throws InterruptedException When it gets interrupted.
     */
    public void sendCoverageResponse(GetCoverageRequest req, HttpServletResponse response)  throws InterruptedException, WcsException {

        CoverageRequestProcessor.sendCoverageResponse(req, response, false );

    }



    public static int getRequestType(Element req) throws WcsException{
        if(req == null){
            throw new WcsException("Poorly formatted WCS request. Missing " +
                    "root element of document.",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }

        String name = req.getName();

        if(name.equals("GetCapabilities")){
            return WCS.GET_CAPABILITIES;
        }
        else if(name.equals("DescribeCoverage")){
            return WCS.DESCRIBE_COVERAGE;
        }
        else if(name.equals("GetCoverage")){
            return WCS.GET_COVERAGE;
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
