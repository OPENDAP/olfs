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
package opendap.wcsGateway;

import opendap.bes.dapResponders.BesApi;
import opendap.dap.User;
import opendap.gateway.BesGatewayApi;
import org.slf4j.Logger;
import org.jdom.Element;
import org.jdom.Document;
import opendap.coreServlet.*;
import opendap.bes.Version;
import opendap.bes.BESError;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.List;


/**
 * User: ndp
 * Date: Apr 22, 2008
 * Time: 3:44:39 PM
 */
public class DispatchHandler implements opendap.coreServlet.DispatchHandler{

    private Logger log;
    private boolean _initialized;

    private HttpServlet dispatchServlet;
    private String systemPath;

    private String _prefix = "wcsGateway/";

    private Element _config;


    private BesGatewayApi _besGatewayApi;





    public DispatchHandler() {

        super();

        _besGatewayApi = new BesGatewayApi();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }

    public String getWcsRequest(HttpServletRequest req) throws Exception {


        String relativeURL = ReqInfo.getLocalUrl(req);
        String requestSuffix = ReqInfo.getRequestSuffix(req);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        String wcsURL = relativeURL.substring(_prefix.length(),relativeURL.length());

        wcsURL = wcsURL.substring(0,wcsURL.lastIndexOf("."+requestSuffix));

        wcsURL = UrlEncoder.hexToString(wcsURL);

        boolean trusted = false;

        if(!_config.getChildren("wcsHost").isEmpty()){
            String allowedHost;
            for(Object o : _config.getChildren("wcsHost")){
                allowedHost = ((Element)o).getTextTrim();
                if(wcsURL.startsWith(allowedHost))
                    trusted = true;
            }

        }
        if(!trusted){
            log.error("No trusted hosts found to match: "+wcsURL);
            return null;
        }
        else
            return wcsURL;


    }

    public static String urlInfo(URL url){
        String msg = "\n";

        msg += "URL: "+url.toString()+"\n";
        msg += "  protocol:      "+url.getProtocol()+"\n";
        msg += "  host:          "+url.getHost()+"\n";
        msg += "  port:          "+url.getPort()+"\n";
        msg += "  default port:  "+url.getDefaultPort()+"\n";
        msg += "  path:          "+url.getPath()+"\n";
        msg += "  query:         "+url.getQuery()+"\n";
        msg += "  file:          "+url.getFile()+"\n";
        msg += "  ref:           "+url.getRef()+"\n";
        msg += "  user info:     "+url.getUserInfo()+"\n";
        msg += "  hash code:     "+url.hashCode()+"\n";

        try {
            msg += "  URI:           "+url.toURI().toASCIIString()+"\n";
        } catch (URISyntaxException e) {
            msg += "  URI:            error: Could not express the URL as URI because: "+e.getMessage()+"\n";
        }

        return msg;
    }






    public static String uriInfo(URI uri){

        String msg = "\n";


        msg += "URI: "+uri.toString()+"\n";
        msg += "  Authority:              "+uri.getAuthority()+"\n";
        msg += "  Host:                   "+uri.getHost()+"\n";
        msg += "  Port:                   "+uri.getPort()+"\n";
        msg += "  Path:                   "+uri.getPath()+"\n";
        msg += "  Query:                  "+uri.getQuery()+"\n";
        msg += "  hashCode:               "+uri.hashCode()+"\n";
        msg += "  Fragment:               "+uri.getFragment()+"\n";
        msg += "  RawAuthority:           "+uri.getRawAuthority()+"\n";
        msg += "  RawFragment:            "+uri.getRawFragment()+"\n";
        msg += "  RawPath:                "+uri.getRawPath()+"\n";
        msg += "  RawQuery:               "+uri.getRawQuery()+"\n";
        msg += "  RawSchemeSpecificPart:  "+uri.getRawSchemeSpecificPart()+"\n";
        msg += "  RawUSerInfo:            "+uri.getRawUserInfo()+"\n";
        msg += "  Scheme:                 "+uri.getScheme()+"\n";
        msg += "  SchemeSpecificPart:     "+uri.getSchemeSpecificPart()+"\n";
        msg += "  UserInfo:               "+uri.getUserInfo()+"\n";
        msg += "  isAbsoulte:             "+uri.isAbsolute()+"\n";
        msg += "  isOpaque:               "+uri.isOpaque()+"\n";
        msg += "  ASCIIString:            "+uri.toASCIIString()+"\n";

        try {
            msg += "  URL:                    "+uri.toURL()+"\n";
        } catch (Exception e) {
            msg += "  URL:                    uri.toURL() FAILED msg="+e.getMessage();
        }

        return msg;
    }





    public void init(HttpServlet servlet, Element config) throws Exception {
        if (_initialized) return;

        String msg;
        Element host;
        List hosts;
        URL url;
        URI uri;


        dispatchServlet = servlet;
        systemPath = ServletUtil.getSystemPath(servlet,"");

        _config = config;

        ingestPrefix();

        hosts = config.getChildren("wcsHost");

        if(hosts.isEmpty()){
            msg = "Configuration Warning: The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " did not provide 1 or more <wcsHost> " +
                    "child elements to limit the WCS services that " +
                    "may be accessed. This not recomended.";

            log.warn(msg);
/*
            msg = "Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 or more <wcsHost>  " +
                    "child elements whose value should be the base URL " +
                    "for the WCS services that may be accessed.";
            log.error(msg);
            throw new Exception(msg);
*/
        }
        else {

            for (Object o : hosts) {
                host = (Element) o;

                url = new URL(host.getTextTrim());
                log.debug(urlInfo(url));


                uri = new URI(host.getTextTrim());
                log.debug(uriInfo(uri));

                log.info("Adding " + url + " to allowed hosts list.");
            }

        }








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



        if(!_prefix.endsWith("/"))
            _prefix += "/";


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

        String relativeURL = ReqInfo.getLocalUrl(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        boolean wcsRequest = false;

        if (relativeURL != null) {

            if (relativeURL.startsWith(_prefix)) {
                wcsRequest = true;
                if (sendResponse) {
                    sendDAPResponse(request, response);
                    log.info("Sent WCS Response");
                }
            }
        }

        return wcsRequest;

    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }

    public void destroy() {
        log.info("Destroy Complete");
    }





    public void sendDAPResponse(HttpServletRequest request,
                                HttpServletResponse response)
            throws Exception {

        String requestSuffix = ReqInfo.getRequestSuffix(request);

        String wcsRequestURL = getWcsRequest(request);


        if(wcsRequestURL==null){
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }



        if ( // DDS Response?
                requestSuffix.equalsIgnoreCase("dds")
                ) {

            sendDDS(request, response, wcsRequestURL);
            log.info("Sent DDS");


        } else if ( // DAS Response?
                requestSuffix.equalsIgnoreCase("das")
                ) {
            sendDAS(request, response, wcsRequestURL);
            log.info("Sent DAS");


        } else if (  // DDX Response?
                requestSuffix.equalsIgnoreCase("ddx")
                ) {
            sendDDX(request, response, wcsRequestURL);
            log.info("Sent DDX");


        } else if ( // DAP2 (aka .dods) Response?
                requestSuffix.equalsIgnoreCase("dods")
                ) {
            sendDAP2Data(request, response, wcsRequestURL);
            log.info("Sent DAP2 Data");


        } else if (  // ASCII Data Response.
                requestSuffix.equalsIgnoreCase("asc") ||
                        requestSuffix.equalsIgnoreCase("ascii")
                ) {
            sendASCII(request, response, wcsRequestURL);
            log.info("Sent ASCII");


        } else if (  // Info Response?
                requestSuffix.equalsIgnoreCase("info")
                ) {
            sendINFO(request, response, wcsRequestURL);
            log.info("Sent Info");


        } else if (  //HTML Request Form (aka The Interface From Hell) Response?
                requestSuffix.equalsIgnoreCase("html") ||
                        requestSuffix.equalsIgnoreCase("htm")
                ) {
            sendHTMLRequestForm(request, response, wcsRequestURL);
            log.info("Sent HTML Request Form");


        } else if (requestSuffix.equals("")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            log.info("Sent BAD URL (missing Suffix)");

        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            log.info("Sent BAD URL - not an OPeNDAP request suffix.");
        }

    }

    private void sendDDS(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        User user = new User(request);
        int maxRS = user.getMaxResponseSize();

        log.debug("sendDDS() for dataset: " + dataSource);

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response,_besGatewayApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document reqDoc = _besGatewayApi.getRequestDocument(
                BesApi.DDS,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                null,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource, reqDoc, os, erros)){

            String msg = new String(erros.toByteArray());
            log.error("sendDDS() encountered a BESError: "+msg);
            os.write(msg.getBytes());
        }


        os.flush();



    }




    private void sendDAS(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDAS() for dataset: " + dataSource);

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response,_besGatewayApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besGatewayApi.getRequestDocument(
                BesApi.DAS,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                null,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource, reqDoc, os, erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDAS() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();



    }



    private void sendDDX(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String requestUrl = request.getRequestURL().toString();
        String xmlBase = requestUrl.substring(0,requestUrl.lastIndexOf(".ddx"));

        log.debug("sendDDX() for dataset: " + dataSource);

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response,_besGatewayApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besGatewayApi.getRequestDocument(
                BesApi.DDX,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                xmlBase,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource, reqDoc, os, erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDDX() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();



    }



    private void sendDAP2Data(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);


        User user = new User(request);
        int maxRS = user.getMaxResponseSize();

        log.debug("sendDAP2Data() for dataset: " + dataSource);

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response,_besGatewayApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besGatewayApi.getRequestDocument(
                BesApi.DAP2,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                maxRS,
                null,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource, reqDoc, os, erros)){
            String msg = new String(erros.toByteArray());
            log.error("sendDAP2Data() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();



    }

    private void sendASCII(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String context = request.getContextPath();

        User user = new User(request);
        int maxRS = user.getMaxResponseSize();


        log.debug("sendASCII() for dataset: " + dataSource);

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response,_besGatewayApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besGatewayApi.getRequestDocument(
                BesApi.ASCII,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                maxRS,
                null,
                null,
                null,
                BesApi.XML_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource, reqDoc, os, erros)){

            BESError besError = new BESError( new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(systemPath, context, response);
            log.error("sendASCII() encountered a BESError: "+besError.getMessage());
        }


        os.flush();


    }


    private void sendINFO(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);


        String context = request.getContextPath();


        log.debug("sendINFO() for dataset: " + dataSource);

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request,response,_besGatewayApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besGatewayApi.getRequestDocument(
                BesApi.INFO_PAGE,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                null,
                null,
                null,
                BesApi.XML_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource, reqDoc, os, erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(systemPath, context, response);
            log.error("sendINFO() encountered a BESError: "+besError.getMessage());

        }


        os.flush();


    }


    private void sendHTMLRequestForm(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        String context = request.getContextPath();

        log.debug("sendHTMLRequestForm() for dataset: " + dataSource);

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request,response,_besGatewayApi);
        response.setHeader("Content-Description", "dods_form");


        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        log.debug("sendHTMLRequestForm(): Sending HTML Data Request Form For: "
                + dataSource +
                "    CE: '" + request.getQueryString() + "'");


        OutputStream os = response.getOutputStream();

        String url = request.getRequestURL().toString();

        int suffix_start = url.lastIndexOf("." + requestSuffix);

        url = url.substring(0, suffix_start);


        log.debug("sendHTMLRequestForm(): HTML Form URL: " + url);

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document reqDoc = _besGatewayApi.getRequestDocument(
                BesApi.HTML_FORM,
                wcsRequestURL,
                null,
                xdap_accept,
                0,
                null,
                url,
                null,
                BesApi.XML_ERRORS);

        if(!_besGatewayApi.besTransaction(dataSource, reqDoc, os, erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));

            besError.sendErrorResponse(systemPath, context, response);


            String msg = besError.getMessage();
            log.error("sendHTMLRequestForm() encountered a BESError: "+msg);
        }

        os.flush();




    }





}
