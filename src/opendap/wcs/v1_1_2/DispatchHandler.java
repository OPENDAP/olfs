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
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.HashMap;
import java.net.URL;
import java.net.URI;
import java.io.IOException;

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







    public DispatchHandler() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }


    public void init(DispatchServlet servlet, Element config) throws Exception {
        if (_initialized) return;

        String msg;
        Element host;
        List hosts;
        URL url;
        URI uri;


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



        boolean wcsEndPoint = false;

        if (relativeURL != null) {

            if (relativeURL.startsWith(_prefix)) {
                wcsEndPoint = true;
                if (sendResponse) {
                    processWcsRequest(request, response);
                    log.info("Sent WCS Response");
                }
            }
        }

        return wcsEndPoint;

    }



    public void processWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        try {


            HashMap<String,String> keyValuePairs = new HashMap<String,String>();

            String query = request.getQueryString();

            if(query==null)
                throw new WcsException("Missing WxS query string.",
                        WcsException.MISSING_PARAMETER_VALUE,"service");

            String[] pairs = request.getQueryString().split("&");

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
                        WCS.WCS_VERSION+") is supported.",
                        WcsException.OPERATION_NOT_SUPPORTED,s);




            // Make sure the client can accept the correct WCS version...
            s = keyValuePairs.get("AcceptedVersions");
            if(s!=null){
                boolean compatible = false;
                tmp = s.split(",");
                for(String ver:tmp){
                    if(ver.equals(WCS.WCS_VERSION))
                        compatible=true;
                }
                if(!compatible)
                    throw new WcsException("Client requested unsupported WCS " +
                            "version(s): "+s,
                            WcsException.VERSION_NEGOTIATION_FAILED,null);
            }




            s = keyValuePairs.get("request");


            if(s == null){
                throw new WcsException("Poorly formatted request URL. Missing " +
                        "key value pair for 'request'",
                        WcsException.MISSING_PARAMETER_VALUE,"request");
            }
            else if(s.equals("GetCapabilities")){

                GetCapabilitiesRequest wcsRequest = new GetCapabilitiesRequest(keyValuePairs);
                response.setContentType("text/ascii");
                wcsRequest.serialize(response.getOutputStream());

            }
            else if(s.equals("DescribeCoverage")){
                DescribeCoverageRequest wcsRequest = new DescribeCoverageRequest(keyValuePairs);
                response.setContentType("text/ascii");
                wcsRequest.serialize(response.getOutputStream());


            }
            else if(s.equals("GetCoverage")){
                GetCoverageRequest wcsRequest = new GetCoverageRequest(keyValuePairs);
                response.setContentType("text/ascii");
                wcsRequest.serialize(response.getOutputStream());
            }
            else {
                throw new WcsException("The parameter 'request' has an invalid " +
                        "value of '"+s+"'.",
                        WcsException.INVALID_PARAMETER_VALUE,"request");

            }



        }
        catch(WcsException e){
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);
            response.setContentType("text/xml");
            er.serialize(response.getOutputStream());
        }



    }















    public long getLastModified(HttpServletRequest req) {
        return -1;
    }

    public void destroy() {
        log.info("Destroy Complete");
    }


}
