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
import opendap.logging.LogUtil;
import org.slf4j.Logger;
import org.jdom.Document;

import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import opendap.coreServlet.*;
import opendap.bes.Version;
import opendap.bes.BESError;

/**
 * User: ndp
 * Date: Apr 22, 2008
 * Time: 3:36:36 PM
 */
public class DispatchServlet extends opendap.coreServlet.DispatchServlet {



    private Logger log;

    private String systemPath;

    private BesApi _besApi;


    private AtomicInteger reqNumber;


    @Override
    public void init() throws ServletException {
        super.init();
        _besApi = new BesApi();
        reqNumber = new AtomicInteger(0);
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        systemPath = ServletUtil.getSystemPath(this,"");
    }


    public long getLastModified(HttpServletRequest req) {

        long lmt;
        lmt = -1;
        return lmt;


    }


    private boolean redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (req.getPathInfo() == null) {
            res.sendRedirect(Scrub.urlContent(req.getRequestURI()+"/"));
            log.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }


    public String getWcsRequest(HttpServletRequest req) throws MalformedURLException {


        String relativeURL = ReqInfo.getLocalUrl(req);
        String requestSuffix = ReqInfo.getRequestSuffix(req);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        String wcsURL = relativeURL;

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



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            {

        try {

            LogUtil.logServerAccessStart(request, "WCS_SERVICE_ACCESS","HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));

            if (!redirect(request, response)) {

                String name = request.getPathInfo();

                log.debug("The client requested this: " + name);

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

        }
        catch( Throwable t){
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            }
            catch(Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        }
    }



    private void sendDDS(HttpServletRequest request, HttpServletResponse response, String wcsRequestURL) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDDS() for dataset: " + dataSource);

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response,_besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document reqDoc = _besApi.getRequestDocument(
                BesApi.DDS,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                null,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besApi.besTransaction(dataSource, reqDoc, os, erros)){

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
        Version.setOpendapMimeHeaders(request,response,_besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besApi.getRequestDocument(
                BesApi.DAS,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                null,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besApi.besTransaction(dataSource, reqDoc, os, erros)){
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
        Version.setOpendapMimeHeaders(request,response,_besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besApi.getRequestDocument(
                BesApi.DDX,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                xmlBase,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besApi.besTransaction(dataSource, reqDoc, os, erros)){
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
        Version.setOpendapMimeHeaders(request,response,_besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besApi.getRequestDocument(
                BesApi.DAP2,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                maxRS,
                null,
                null,
                null,
                BesApi.DAP2_ERRORS);

        if(!_besApi.besTransaction(dataSource, reqDoc, os, erros)){
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
        Version.setOpendapMimeHeaders(request,response,_besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besApi.getRequestDocument(
                BesApi.ASCII,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                maxRS,
                null,
                null,
                null,
                BesApi.XML_ERRORS);

        if(!_besApi.besTransaction(dataSource, reqDoc, os, erros)){

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
        Version.setOpendapMimeHeaders(request,response,_besApi);
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        Document reqDoc = _besApi.getRequestDocument(
                BesApi.INFO_PAGE,
                wcsRequestURL,
                constraintExpression,
                xdap_accept,
                0,
                null,
                null,
                null,
                BesApi.XML_ERRORS);

        if(!_besApi.besTransaction(dataSource, reqDoc, os, erros)){

            BESError besError = new BESError( new ByteArrayInputStream(erros.toByteArray()));
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
        Version.setOpendapMimeHeaders(request,response,_besApi);
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

        Document reqDoc = _besApi.getRequestDocument(
                BesApi.HTML_FORM,
                wcsRequestURL,
                null,
                xdap_accept,
                0,
                null,
                url,
                null,
                BesApi.XML_ERRORS);

        if(!_besApi.besTransaction(dataSource, reqDoc, os, erros)){

            BESError besError = new BESError( new ByteArrayInputStream(erros.toByteArray()));

            besError.sendErrorResponse(systemPath, context, response);


            String msg = besError.getMessage();
            log.error("sendHTMLRequestForm() encountered a BESError: "+msg);
        }

        os.flush();




    }


}
