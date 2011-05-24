/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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
package opendap.wcs.v1_1_2.http;

import opendap.wcs.v1_1_2.*;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import opendap.coreServlet.ReqInfo;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 4:50:49 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class XmlRequestHandler implements opendap.coreServlet.DispatchHandler, WcsResponder {
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
                                       boolean sendResponse)
            throws Exception {

        String relativeURL = ReqInfo.getLocalUrl(request);

        if (relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1, relativeURL.length());


        boolean isWcsEndPoint = false;
        if (relativeURL != null) {
            if (relativeURL.startsWith(_prefix)) {
                isWcsEndPoint = true;
                if (sendResponse)
                    handleWcsRequest(request, response);
            }
        }

        return isWcsEndPoint;

    }


    protected abstract void handleWcsRequest(HttpServletRequest request, HttpServletResponse response) throws Exception;



    public Document getCapabilities(Element reqElem, String serviceUrl) throws InterruptedException, WcsException {
        GetCapabilitiesRequest wcsRequest = new GetCapabilitiesRequest(reqElem);

            return CapabilitiesRequestProcessor.processGetCapabilitiesRequest(wcsRequest, serviceUrl);
    }




    public Document describeCoverage(Element reqElem) throws InterruptedException, WcsException {
        DescribeCoverageRequest wcsRequest = new DescribeCoverageRequest(reqElem);

            return DescribeCoverageRequestProcessor.processDescribeCoveragesRequest(wcsRequest);
    }




    public Document getCoverage( Element reqElem) throws InterruptedException, WcsException {

        GetCoverageRequest req = new GetCoverageRequest(reqElem);

        return CoverageRequestProcessor.processCoverageRequest(req);
    }


    public Document getWcsResponse(String serviceUrl, WcsResponder wcsResponder, Element wcsRequest) throws InterruptedException {


        try {
            Document wcsResponse;
            switch (getRequestType(wcsRequest)) {

                case WCS.GET_CAPABILITIES:
                    wcsResponse = wcsResponder.getCapabilities(wcsRequest, serviceUrl);
                    break;

                case WCS.DESCRIBE_COVERAGE:
                    wcsResponse = wcsResponder.describeCoverage(wcsRequest);
                    break;

                case WCS.GET_COVERAGE:
                    //@todo The URL passed here is used to construct data access URLs for the WCS response.
                    // The serviceURL + localID will produce the access URL.
                    wcsResponse = wcsResponder.getCoverage(wcsRequest);
                    break;

                default:
                    throw new WcsException("The request document  was  invalid. " +
                            "The root element was name: '"+wcsRequest.getName()+"' in namespace: '"+wcsRequest.getNamespace().getURI()+"'.",
                            WcsException.MISSING_PARAMETER_VALUE,"wcs:GetCapabilities,wcs:DescribeCoverage,wcs:GetCoverage");

            }

            return wcsResponse;

        }
        catch (WcsException e) {
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);
            return er.getReport();
        }


    }

    public Document getWcsResponse(String serviceUrl, WcsResponder responder, InputStream reqDoc) throws InterruptedException {

        try {

            Document requestDoc;
            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                requestDoc = sb.build(reqDoc);
            }
            catch(Exception e){
                throw new WcsException(e.getMessage(), WcsException.INVALID_PARAMETER_VALUE,"WCS Request Document");
            }

            return getWcsResponse(serviceUrl, responder,requestDoc.getRootElement());


        }
        catch (WcsException e) {
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);
            return er.getReport();
        }



    }

    private int getRequestType(Element req) throws WcsException{
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
}
