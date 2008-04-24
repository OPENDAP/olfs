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
package opendap.wcs_service;

import org.slf4j.Logger;
import org.jdom.Element;
import opendap.coreServlet.*;
import opendap.coreServlet.DispatchServlet;
import opendap.bes.Version;
import opendap.bes.BesAPI;
import opendap.bes.BESError;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;


/**
 * User: ndp
 * Date: Apr 22, 2008
 * Time: 3:44:39 PM
 */
public class DispatchHandler implements opendap.coreServlet.DispatchHandler{

    private Logger log;
    private boolean initialized;
    private opendap.coreServlet.DispatchServlet dispatchServlet;
    private String prefix = "wcs_service/";

    public DispatchHandler() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;

    }

    public String getWcsRequest(HttpServletRequest req) throws MalformedURLException {


        String relativeURL = ReqInfo.getFullSourceName(req);
        String requestSuffix = ReqInfo.getRequestSuffix(req);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        String wcsURL = relativeURL.substring(prefix.length(),relativeURL.length());

        wcsURL = wcsURL.substring(0,wcsURL.lastIndexOf("."+requestSuffix));

        wcsURL = UrlEncoder.hexToString(wcsURL);

        URL url = new URL(wcsURL);
        log.debug(urlInfo(url));

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




    public void init(DispatchServlet servlet, Element config) throws Exception {
        if (initialized) return;


        dispatchServlet = servlet;


        Element e = config.getChild("prefix");
        if(e!=null)
            prefix = e.getTextTrim();

        if(prefix.equals("/"))
            throw new Exception("Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <_prefix>  " +
                    "child element whose value may not be equal to \"/\"");


        if(!prefix.endsWith("/"))
            prefix += "/";


        //if(!_prefix.startsWith("/"))
        //    _prefix = "/" + _prefix;

        if(prefix.startsWith("/"))
            prefix = prefix.substring(1,prefix.length());

        log.info("Initialized. prefix="+prefix);
        initialized = true;
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
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        boolean wcsRequest = false;

        if (relativeURL != null) {

            if (relativeURL.startsWith(prefix)) {
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

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDDS() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.besGetWcsTransaction(  BesAPI.DDS,
                                          dataSource,
                                          constraintExpression,
                                          os,
                                          erros,
                                          BesAPI.DAP2_ERRORS,
                                          wcsRequestURL)
                ){

            String msg = new String(erros.toByteArray());
            log.error("sendDDS() encounterd a BESError: "+msg);
            os.write(msg.getBytes());
        }


        os.flush();



    }




    private void sendDAS(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDAS() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesAPI.besGetWcsTransaction(
                BesAPI.DAS,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_ERRORS,
                wcsRequestURL)){
            String msg = new String(erros.toByteArray());
            log.error("sendDAS() encounterd a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();



    }



    private void sendDDX(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDDX() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesAPI.besGetWcsTransaction(
                BesAPI.DDX,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_ERRORS,
                wcsRequestURL)){
            String msg = new String(erros.toByteArray());
            log.error("sendDDX() encounterd a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();



    }



    private void sendDAP2Data(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDAP2Data() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesAPI.besGetWcsTransaction(
                BesAPI.DAP2,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_ERRORS,
                wcsRequestURL)){
            String msg = new String(erros.toByteArray());
            log.error("sendDAP2Data() encounterd a BESError: "+msg);
            os.write(msg.getBytes());

        }


        os.flush();



    }

    private void sendASCII(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendASCII() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesAPI.besGetWcsTransaction(
                BesAPI.ASCII,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.XML_ERRORS,
                wcsRequestURL)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(dispatchServlet,response);
            log.error("sendASCII() encounterd a BESError: "+besError.getMessage());
        }


        os.flush();


    }


    private void sendINFO(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendINFO() for dataset: " + dataSource);

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesAPI.besGetWcsTransaction(
                BesAPI.INFO_PAGE,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.XML_ERRORS,
                wcsRequestURL)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(dispatchServlet,response);
            log.error("sendINFO() encounterd a BESError: "+besError.getMessage());

        }


        os.flush();


    }


    private void sendHTMLRequestForm(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        log.debug("sendHTMLRequestForm() for dataset: " + dataSource);

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_form");


        response.setStatus(HttpServletResponse.SC_OK);

        log.debug("sendHTMLRequestForm(): Sending HTML Data Request Form For: "
                + dataSource +
                "    CE: '" + request.getQueryString() + "'");


        OutputStream os = response.getOutputStream();

        String url = request.getRequestURL().toString();

        int suffix_start = url.lastIndexOf("." + requestSuffix);

        url = url.substring(0, suffix_start);


        log.debug("sendHTMLRequestForm(): HTML Form URL: " + url);

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.writeWcsHTMLForm(
                dataSource,
                url,
                os,
                erros,wcsRequestURL)){
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));

            besError.sendErrorResponse(dispatchServlet,response);


            String msg = besError.getMessage();
            System.out.println(msg);
            System.err.println(msg);
            log.error("sendHTMLRequestForm() encounterd a BESError: "+msg);
        }

        os.flush();




    }





}
