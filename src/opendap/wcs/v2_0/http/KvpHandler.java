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
package opendap.wcs.v2_0.http;

import opendap.wcs.v2_0.*;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 8, 2009
 * Time: 12:03:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class KvpHandler {


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(KvpHandler.class);


    public static void processKvpWcsRequest(String serviceURL, String requestUrl, Map<String,String[]> keyValuePairs, HttpServletResponse response) throws InterruptedException, IOException {



        Document wcsResponse;
        XMLOutputter xmlo;

        ServletOutputStream os = null;


        try {

            int wcsRequestType = getRequestType(keyValuePairs);




            switch(wcsRequestType){

                case  WCS.GET_CAPABILITIES:
                    wcsResponse = getCapabilities(keyValuePairs, serviceURL);
                    xmlo = new XMLOutputter(Format.getPrettyFormat());
                    try {
                        response.setContentType("text/xml");
                        os = response.getOutputStream();
                        xmlo.output(wcsResponse,os);
                    } catch (IOException e) {
                        throw new WcsException(e.getMessage(), WcsException.NO_APPLICABLE_CODE);
                    }

                    break;

                case  WCS.DESCRIBE_COVERAGE:
                    wcsResponse = describeCoverage(keyValuePairs);
                    xmlo = new XMLOutputter(Format.getPrettyFormat());
                    try {
                        response.setContentType("text/xml");
                        os = response.getOutputStream();
                        xmlo.output(wcsResponse,os);
                    } catch (IOException e) {
                        throw new WcsException(e.getMessage(), WcsException.NO_APPLICABLE_CODE);
                    }

                    break;

                case WCS.GET_COVERAGE:

                    getCoverage(requestUrl, keyValuePairs,response);

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

        return CapabilitiesRequestProcessor.processGetCapabilitiesRequest(wcsRequest, serviceUrl);
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
     * @param keyValuePairs    Key Value Pairs from WCS URL
     * @throws WcsException  When bad things happen.
     * @throws InterruptedException
     * @throws IOException
     */
    public static void getCoverage(String requestUrl, Map<String, String[]> keyValuePairs, HttpServletResponse response) throws InterruptedException, WcsException, IOException {

        GetCoverageRequest req = new GetCoverageRequest(requestUrl, keyValuePairs);

        CoverageRequestProcessor.sendCoverageResponse(req, response, false );

    }






    public static int getRequestType(Map<String,String[]> keyValuePairs) throws WcsException{


        if(keyValuePairs.isEmpty())
            throw new WcsException("Missing WxS query string.",
                    WcsException.MISSING_PARAMETER_VALUE,"service");

        // Make sure the client is looking for a WCS service....
        String[] s = keyValuePairs.get("service");
        if(s==null || !s[0].equals(WCS.SERVICE))
            throw new WcsException("Only the WCS service (version "+
                    WCS.CURRENT_VERSION+") is supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,"service");


        s = keyValuePairs.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(s[0].equalsIgnoreCase("GetCapabilities")){
            return WCS.GET_CAPABILITIES;
        }
        else if(s[0].equalsIgnoreCase("DescribeCoverage")){
            return WCS.DESCRIBE_COVERAGE;
        }
        else if(s[0].equalsIgnoreCase("GetCoverage")){
            return WCS.GET_COVERAGE;
        }
        else {
            throw new WcsException("The parameter 'request' has an invalid " +
                    "value of '"+s+"'.",
                    WcsException.INVALID_PARAMETER_VALUE,"request");
        }


    }


}
