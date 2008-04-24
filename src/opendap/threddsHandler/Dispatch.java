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
package opendap.threddsHandler;

import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.ReqInfo;
import opendap.wcs.*;
import opendap.bes.Version;
import opendap.bes.BesAPI;
import opendap.bes.BESError;
import org.slf4j.Logger;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import thredds.servlet.ServletUtil;

/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 3:46:50 PM
 */
public class Dispatch implements DispatchHandler{


    private Logger log;

    private Element config;
    private boolean initialized;
    private DispatchServlet dispatchServlet;
    private String _prefix;
    boolean useMemoryCache = false;

    public Dispatch() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        config = null;
        initialized = false;
        _prefix = "thredds/";

    }


    public void sendThreddsCatalogResponse(HttpServletRequest request,
                                HttpServletResponse response) throws Exception{

        String relativeURL = ReqInfo.getFullSourceName(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());


        Document catDoc;
        if(relativeURL.equals(_prefix)){
            catDoc = CatalogManager.getTopLevelCatalogDocument();
        }
        else{

            //if(requestSuffix.equals("html"))


            if(relativeURL.startsWith(_prefix))
                relativeURL = relativeURL.substring(_prefix.length(),relativeURL.length());
            Catalog cat = CatalogManager.getCatalog(relativeURL);

            if(cat!=null){
                catDoc = cat.getCatalogDocument();
                log.debug("\nFound catalog: "+relativeURL+"   " +
                        "    requestSuffix: "+requestSuffix+"   " +
                        "    prefix: " + _prefix


                );
            }
            else {
                log.error("Can't find catalog: "+relativeURL+"   " +
                        "    requestSuffix: "+requestSuffix+"   " +
                        "    prefix: " + _prefix


                );

                response.sendError(HttpServletResponse.SC_NOT_FOUND,"Can't find catalog: "+relativeURL);
                return;
            }
        }

        xmlo.output(catDoc, System.out);




        String xsltDoc = ServletUtil.getPath(dispatchServlet, "/docs/xsl/thredds.xsl");

        XSLTransformer transformer = new XSLTransformer(xsltDoc);

        Document contentsPage = transformer.transform(catDoc);


        //xmlo.output(catalog, System.out);
        //xmlo.output(contentsPage, System.out);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_directory");
        response.setStatus(HttpServletResponse.SC_OK);
        xmlo.output(contentsPage, response.getWriter());
        xmlo.output(contentsPage, System.out);



    }



    public void init(DispatchServlet servlet, Element configuration) throws Exception {


        String s;

        if (initialized) return;

        dispatchServlet = servlet;
        config = configuration;


        List children;
        Element e;

        e = config.getChild("_prefix");
        if(e!=null)
            _prefix = e.getTextTrim();

        if(_prefix.equals("/"))
            throw new Exception("Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <_prefix>  " +
                    "child element whose value may not be equal to \"/\"");


        if(!_prefix.endsWith("/"))
            _prefix += "/";


        //if(!_prefix.startsWith("/"))
        //    _prefix = "/" + _prefix;

        if(_prefix.startsWith("/"))
            _prefix = _prefix.substring(1,_prefix.length());

        e = config.getChild("useMemoryCache");
        if(e!=null){
            s = e.getTextTrim();
            if(s.equalsIgnoreCase("true")){
                useMemoryCache = true;
            }
        }


        // Get the RootCatalogs from the conifg
        children = config.getChildren("threddsCatalogRoot");

        if (children.isEmpty()) {
            throw new Exception("Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 or more <threddsCatalogRoot>  " +
                    "child elements.");
        } else {
            log.debug("Processing THREDDS catalog file(s)...");

            String contentPath = ServletUtil.getContentPath(servlet);
            String contextPath = ServletUtil.getContextPath(servlet);


            String urlPrefix = "";//ServletUtil.getContextPath(servlet);

            if(!urlPrefix.endsWith("/") && !_prefix.startsWith("/"))
                urlPrefix += "/";

            while(urlPrefix.endsWith("/") && _prefix.startsWith("/"))
                urlPrefix += urlPrefix.substring(0,urlPrefix.length()-1);

            //urlPrefix = urlPrefix + _prefix;
            //urlPrefix =  _prefix;


            if(!urlPrefix.endsWith("/"))
                urlPrefix += "/";

            urlPrefix = "";


            Iterator i = children.iterator();
            Element fileElem;
            String fileName,  pathPrefix, thisUrlPrefix;


            System.out.println(urlPrefix);
            CatalogManager.init(contentPath);
            while (i.hasNext()) {

                fileElem = (Element) i.next();


                s = fileElem.getTextTrim();



                thisUrlPrefix = s.substring(0,s.lastIndexOf(Util.basename(s)));
                thisUrlPrefix = urlPrefix + thisUrlPrefix;



                s = contentPath + s;
                fileName = Util.basename(s);
                pathPrefix = s.substring(0,s.lastIndexOf(fileName));

                log.debug("Top Level Catalog - pathPrefix: " + pathPrefix);
                log.debug("Top Level Catalog - urlPrefix: " + thisUrlPrefix);
                log.debug("Top Level Catalog - fileName: " + fileName);

                CatalogManager.addRootCatalog(
                        pathPrefix,
                        thisUrlPrefix,
                        fileName,
                        useMemoryCache);

                log.debug("configuration file: " + fileName + " processing complete.");
            }


        }

        // Read Config and establish Config state.


        log.info("Initialized.");
        initialized = true;
    }

    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return threddsRequestDispatch(request, null, false);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        threddsRequestDispatch(request, response, true);
    }


    private boolean threddsRequestDispatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       boolean sendResponse)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        if(dataSource.startsWith("/"))
            dataSource = dataSource.substring(1,dataSource.length());

        boolean threddsRequest = false;

        if (dataSource != null) {

            if (dataSource.startsWith(_prefix)) {
                threddsRequest = true;
                if (sendResponse) {
                    sendThreddsCatalogResponse(request, response);
                    log.info("Sent THREDDS Response");
                }
            }
        }

        return threddsRequest;

    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }

    public void destroy() {

        WcsManager.destroy();
        log.info("Destroy Complete");


    }






    public void sendDAPResponse(HttpServletRequest request,
                                HttpServletResponse response,
                                String projectName,
                                String siteName,
                                String serviceName,
                                String coverageName,
                                String dateName)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        Project project = WcsManager.getProject(projectName);
        if (project == null) {
            log.error("sendDAPResponse() Project:  \"" + projectName + "\" not found.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Site site = project.getSite(siteName);
        if (site == null) {
            log.error("sendDAPResponse() Site:  \"" + siteName + "\" not found.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        WcsService service = WcsManager.getWcsService(serviceName);
        if (service == null) {
            log.error("sendDAPResponse() WcsService:  \"" + serviceName + "\" not found.");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String wcsRequestURL = "";
        service.lock();
        try {

            WcsCoverageOffering coverage = service.getCoverageOffering(coverageName);
            if (coverage == null) {
                log.error("sendDAPResponse() Coverage:  \"" + serviceName + "\" not found.");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (dateName.equals("dataset"))
                dateName = null;

            wcsRequestURL = service.getWcsRequestURL(site, coverage, dateName);
            log.debug("wcsRequestURL: " + wcsRequestURL);
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
        }
        finally {
            service.unlock();
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

        if (!BesAPI.besGetWcsTransaction(BesAPI.DDS,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_ERRORS,
                wcsRequestURL)
                ) {

            String msg = new String(erros.toByteArray());
            log.error("sendDDS() encounterd a BESError: " + msg);
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


        if (!BesAPI.besGetWcsTransaction(
                BesAPI.DAS,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_ERRORS,
                wcsRequestURL)) {
            String msg = new String(erros.toByteArray());
            log.error("sendDAS() encounterd a BESError: " + msg);
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


        if (!BesAPI.besGetWcsTransaction(
                BesAPI.DDX,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_ERRORS,
                wcsRequestURL)) {
            String msg = new String(erros.toByteArray());
            log.error("sendDDX() encounterd a BESError: " + msg);
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


        if (!BesAPI.besGetWcsTransaction(
                BesAPI.DAP2,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_ERRORS,
                wcsRequestURL)) {
            String msg = new String(erros.toByteArray());
            log.error("sendDAP2Data() encounterd a BESError: " + msg);
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


        if (!BesAPI.besGetWcsTransaction(
                BesAPI.ASCII,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.XML_ERRORS,
                wcsRequestURL)) {

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(dispatchServlet, response);
            log.error("sendASCII() encounterd a BESError: " + besError.getMessage());
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


        if (!BesAPI.besGetWcsTransaction(
                BesAPI.INFO_PAGE,
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.XML_ERRORS,
                wcsRequestURL)) {

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(dispatchServlet, response);
            log.error("sendINFO() encounterd a BESError: " + besError.getMessage());

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

        if (!BesAPI.writeWcsHTMLForm(
                dataSource,
                url,
                os,
                erros, wcsRequestURL)) {
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));

            besError.sendErrorResponse(dispatchServlet, response);


            String msg = besError.getMessage();
            System.out.println(msg);
            System.err.println(msg);
            log.error("sendHTMLRequestForm() encounterd a BESError: " + msg);
        }

        os.flush();


    }


}




