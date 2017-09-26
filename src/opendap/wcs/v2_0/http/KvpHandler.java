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
import opendap.coreServlet.ReqInfo;
import opendap.ppt.PPTException;
import opendap.wcs.v2_0.*;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *  KvpHandler handles the parsing and procesing of WCS requests received in the
 *  URL query string as a set of Key Value Pairs;.
 *
 *  Supported WCS Versions:
 *      WCS-2.0.1
 *      EO-WCS-2.0
 *
 */
public class KvpHandler {


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(KvpHandler.class);


    private static void transmitXML(Document doc, ServletOutputStream sos) throws WcsException {
        try {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            xmlo.output(doc,sos);
        } catch (IOException e) {
            throw new WcsException(e.getMessage(), WcsException.NO_APPLICABLE_CODE);
        }



    }

    public static void processKvpWcsRequest(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, IOException, PPTException, BadConfigurationException, BESError {
        Map<String,String[]> keyValuePairs  =  getKVP(request);
        Document wcsResponse;
        ServletOutputStream os  = null;
        try {
            os  = response.getOutputStream();
            WCS.REQUEST wcsRequestType = getRequestType(keyValuePairs);

            switch(wcsRequestType){

                case GET_CAPABILITIES:
                    String baseServiceUrl = Util.getServiceUrl(request);
                    String relativeURL = ReqInfo.getLocalUrl(request);
                    String wcsServiceUrl = PathBuilder.pathConcat(baseServiceUrl,relativeURL);
                    wcsResponse = getCapabilities(keyValuePairs, wcsServiceUrl);
                    response.setContentType("text/xml");
                    transmitXML(wcsResponse,os);
                    break;

                case DESCRIBE_COVERAGE:
                    wcsResponse = describeCoverage(keyValuePairs);
                    response.setContentType("text/xml");
                    transmitXML(wcsResponse,os);
                    break;

                case DESCRIBE_EO_COVERAGE_SET:
                    wcsResponse = describeEOCoverageSet(keyValuePairs);
                    response.setContentType("text/xml");
                    transmitXML(wcsResponse,os);
                    break;

                case GET_COVERAGE:

                    getCoverage(request, keyValuePairs, response);

                    break;

                default:
                    throw new WcsException("INTERNAL ERROR: getRequestType() returned an invalid value.",
                            WcsException.NO_APPLICABLE_CODE);
            }



        }
        catch(WcsException e){
            log.error(e.getMessage());
            WcsExceptionReport er = new WcsExceptionReport(e);
            if(os==null)
                os = response.getOutputStream();

            if(!response.isCommitted()) {
                response.setContentType("text/xml");
                response.setStatus(er.getHttpStatusCode());
            }

            os.println(er.toString());
        }


    }





    /**
     *
     * @param keyValuePairs   Key Value Pairs from WCS URL
     * @throws WcsException When bad things happen.
     */
    public static Document getCapabilities(Map<String,String[]> keyValuePairs, String serviceUrl)  throws InterruptedException, WcsException {

        GetCapabilitiesRequest wcsRequest = new GetCapabilitiesRequest(keyValuePairs);

        return GetCapabilitiesRequestProcessor.processGetCapabilitiesRequest(wcsRequest, serviceUrl);
    }


    /**
     *
     * @param keyValuePairs     Key Value Pairs from WCS URL
     * @throws WcsException  When bad things happen.
     */
    public static Document describeCoverage(Map<String,String[]> keyValuePairs )  throws InterruptedException, WcsException {

        DescribeCoverageRequest wcsRequest = new DescribeCoverageRequest(keyValuePairs);

        return DescribeCoverageRequestProcessor.processDescribeCoveragesRequest(wcsRequest);
    }


    /**
     *
     * @param keyValuePairs     Key Value Pairs from WCS URL
     * @throws WcsException  When bad things happen.
     */
    public static Document describeEOCoverageSet(Map<String,String[]> keyValuePairs )  throws InterruptedException, WcsException {

        DescribeEOCoverageSetRequest wcsRequest = new DescribeEOCoverageSetRequest(keyValuePairs);

        return DescribeEOCoverageSetRequestProcessor.processDescribeEOCoverageSetRequest(wcsRequest);
    }



    /**
     *
     * @param keyValuePairs    Key Value Pairs from WCS URL
     * @throws WcsException  When bad things happen.
     * @throws InterruptedException
     * @throws IOException
     */
    public static void getCoverage(HttpServletRequest request, Map<String, String[]> keyValuePairs, HttpServletResponse response) throws InterruptedException, WcsException, IOException, PPTException, BadConfigurationException, BESError {

        String requestUrl = HttpGetHandler.getRequestUrlWithQuery(request);
        GetCoverageRequest req = new GetCoverageRequest(requestUrl, keyValuePairs);

        req.setCfHistoryAttribute(ReqInfo.getCFHistoryEntry(request));
        GetCoverageRequestProcessor.sendCoverageResponse(req, response, false );

    }






    public static WCS.REQUEST getRequestType(Map<String,String[]> keyValuePairs) throws WcsException{


        if(keyValuePairs.isEmpty())
            throw new WcsException("Missing WxS query string.",
                    WcsException.MISSING_PARAMETER_VALUE,"service");

        // Make sure the client is looking for a WCS service....
        String[] s = keyValuePairs.get("service");
        if(s==null || !s[0].equals(WCS.SERVICE))
            throw new WcsException("Only the "+WCS.SERVICE+ " service (version "+
                    WCS.CURRENT_VERSION+") is supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,"service");


        s = keyValuePairs.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(s[0].equalsIgnoreCase(WCS.REQUEST.GET_CAPABILITIES.toString())){
            return WCS.REQUEST.GET_CAPABILITIES;
        }
        else if(s[0].equalsIgnoreCase(WCS.REQUEST.DESCRIBE_COVERAGE.toString())){
            return WCS.REQUEST.DESCRIBE_COVERAGE;
        }
        else if(s[0].equalsIgnoreCase(WCS.REQUEST.DESCRIBE_EO_COVERAGE_SET.toString())){
            return WCS.REQUEST.DESCRIBE_EO_COVERAGE_SET;
        }
        else if(s[0].equalsIgnoreCase(WCS.REQUEST.GET_COVERAGE.toString())){
            return WCS.REQUEST.GET_COVERAGE;
        }
        else {
            throw new WcsException("The parameter 'request' has an invalid " +
                    "value of '"+s+"'.",
                    WcsException.INVALID_PARAMETER_VALUE,"request");
        }


    }


    public static Map<String, String[]> getKVP(HttpServletRequest request){
        Map<String,String[]> requestParameters = new HashMap<>();
        Map pmap =  request.getParameterMap();

        for(Object o: pmap.keySet()){
            String key = (String) o;   // Get the key String
            String[] value = (String[]) pmap.get(key);  // Get the value before we change the key
            key = key.toLowerCase(); // Make the key set case insensitive;
            requestParameters.put(key,value);
        }


        String localUrl = ReqInfo.getLocalUrl(request);
        while(localUrl.startsWith("/") && !localUrl.isEmpty()){
            localUrl = localUrl.substring(1,localUrl.length());
        }
        while(localUrl.endsWith("/") && !localUrl.isEmpty()){
            localUrl = localUrl.substring(0,localUrl.length()-1);
        }
        if( localUrl!=null &&
                !localUrl.isEmpty() &&
                !requestParameters.containsKey("coverageId".toLowerCase())){

            String[] vals;
            vals = new String[1];
            vals[0] = localUrl;
            requestParameters.put("coverageId".toLowerCase(), vals);
        }


        return requestParameters;

    }





}
