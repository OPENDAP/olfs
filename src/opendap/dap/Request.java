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

package opendap.dap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 27, 2010
 * Time: 12:21:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class Request {

    private Logger log;
    private HttpServletRequest _request;

    public Request(HttpServlet servlet, HttpServletRequest request){
        log = LoggerFactory.getLogger(this.getClass());
        _request = request;
    }


    /**
     * Local ID for the service is whatever is after the third slash
     * in the URL ie after the server name (or IP) and port number combination and up to the first
     * dataset collection term in the URL.
     *
     * For example in the URL http://localhost:8080/opendap/hyrax/data/fnoc1.nc this
     * method will return /opendap/hyrax/ because the
     *
     * Or think of it as "the server" + "the service" is the minimum
     * URL to get to the DAP service. Thus http://localhost:8080/opendap/hyrax/ is the full DAP
     * service URL. for the preceding example.
     *
     *
     *
     *
     * @return
     */
    public String getServiceLocalId(){

        String contextName = _request.getContextPath();
        String servletName = _request.getServletPath();

        String dapService = contextName + servletName;

        log.debug("getServiceLocalId(): "+dapService);

        return dapService;

    }

    public String getContextPath(){
        return _request.getContextPath();
    }


    public String getDocsServiceLocalID(){

        String contextName = _request.getContextPath();

        String docsService = contextName + "/docs";

        log.debug("getDocsServiceLocalID(): "+docsService);

        return docsService;

    }


    public String getWebStartServiceLocalID(){

        String contextName = _request.getContextPath();

        String webStartService = contextName + "/webstart";

        log.debug("getWebStartServiceLocalID(): "+webStartService);

        return webStartService;

    }




    public String getServiceUrl()  {

        String transport  = _request.getScheme();
        String serverName = _request.getServerName();
        int    serverPort = _request.getServerPort();


        String dapService = getServiceLocalId();

        String serviceUrl = transport + "://"+ serverName + ":"+ serverPort + dapService;
        
        StringBuilder requestUrl = new StringBuilder();

        requestUrl.append(transport).append("://").append(serverName);
        if( transport.equalsIgnoreCase("http") && serverPort != 80) {
            requestUrl.append(":").append(serverPort);
        }
        else if( transport.equalsIgnoreCase("https") && serverPort != 443) {
            requestUrl.append(":").append(serverPort);
        }
        requestUrl.append(dapService);

        log.debug("getServiceUrl(): "+serviceUrl);

        return serviceUrl;

    }


    /**
     * Returns the relativeURL name for this request. This is essentially the same as the value
     * of HttpServletRequest.getPathInfo() except that it is never null. If HttpServletRequest.getPathInfo()
     * is null then the full source name is "/".
     * @return The relative URL = _request.getPathInfo()
     */
    public String getRelativeUrl(){
        String s = _request.getPathInfo();
        if(s == null){ // If the requestPath is null, then we are at the top level, or "/" as it were.
            s = "/";
        }
        return s;
    }




        

}
