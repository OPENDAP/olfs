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

package opendap.bes;


import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.lang.reflect.Method;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;
import org.slf4j.Logger;

/**
 * Handler for DAP requests.
 * @deprecated
 */
public class DapDispatchHandler implements OpendapHttpDispatchHandler {

    private Logger log;
    private boolean initialized;
    private HttpServlet _servlet;

    private String systemPath;

    public DapDispatchHandler() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;

    }

    private HashMap<Pattern,Method> dispatchMethods;





    /**
     * ************************************************************************
     * Intitializes any state needed for the handler.
     */
    public void init(HttpServlet ds, Element config) throws Exception {

        if(initialized) return;

        _servlet = ds;
        systemPath = ServletUtil.getSystemPath(_servlet,"");


        dispatchMethods = new HashMap<Pattern,Method>();


        registerDispatchMethod(".*\\.ddx",     "sendDDX");
        registerDispatchMethod(".*\\.dds",     "sendDDS");
        registerDispatchMethod(".*\\.das",     "sendDAS");
        registerDispatchMethod(".*\\.dods",    "sendDAP2Data");
        registerDispatchMethod(".*\\.dap",     "sendDataDDX");
        registerDispatchMethod(".*\\.info",    "sendInfo");
        registerDispatchMethod(".*\\.html?",   "sendHTMLRequestForm");
        registerDispatchMethod(".*\\.asc(ii)?","sendASCII");
        registerDispatchMethod(".*\\.nc",      "sendNetcdfFileOut");
        registerDispatchMethod(".*\\.rdf",     "sendDDX2RDF");
        registerDispatchMethod(".*\\.xdods",   "sendXmlData");


        log.info("masterDispatchRegex=\""+getDispatchRegex()+"\"");
        log.info("Initialized.");
        initialized = true;



    }


    private void registerDispatchMethod(String regexPattern, String methodName) throws NoSuchMethodException {
        dispatchMethods.put(
                Pattern.compile(regexPattern,Pattern.CASE_INSENSITIVE),
                this.getClass().getMethod(methodName,HttpServletRequest.class,HttpServletResponse.class)
        );
    }

    public Pattern getDispatchRegex(){
        String masterRegex = null;

        for(Pattern p : dispatchMethods.keySet()){
            if(masterRegex != null)
                masterRegex += "|";
            else
                masterRegex = "";

            masterRegex += p.pattern();
        }
        return Pattern.compile(masterRegex);
    }


    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {



        if(!initialized)
            throw new Exception("DapDispatchHandler has not been initialized!");

       return dataSetDispatch(request,null,false);

    }

    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {


        if(!initialized)
            throw new Exception("DapDispatchHandler has not been initialized!");

        dataSetDispatch(request,response,true);


    }


    public long getLastModified(HttpServletRequest req) {


        String relativeUrl = ReqInfo.getLocalUrl(req);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);


        if(!initialized)
            return -1;

        log.debug("getLastModified(): Tomcat requesting getlastModified() " +
                "for collection: " + dataSource );


        String requestURL = req.getRequestURL().toString();

        for(Pattern p: dispatchMethods.keySet()){

            if(p.matcher(requestURL).matches()){
                log.info("The request URL: "+requestURL+" matches " +
                        "the pattern: \""+p.pattern()+"\"");

                try {
                    log.debug("getLastModified(): Getting datasource info for "+dataSource);
                    DataSourceInfo dsi = getDataSourceInfo(dataSource);
                    log.debug("getLastModified(): Returning: " + new Date(dsi.lastModified()));

                    return dsi.lastModified();

                } catch (Exception e) {
                    log.debug("getLastModified(): Returning: -1");
                    return -1;
                }

            }

        }

        return -1;


    }


    public void destroy() {
        log.info("Destroy complete.");
    }








    /**
     * Performs dispatching for OPeNDAP data requests. The OPeNDAP response
     * suite consists of:
     * <ui>
     * <li>dds - The OPeNDAP Data Description Service document for the
     * requested dataset. </li>
     * <li>das - The OPeNDAP Data Attribute Service document for the requested
     * dataset. </li>
     * <li>ddx - The OPeNDAP DDX document, an XML document that combines the
     * DDS and the DAS. </li>
     * <li>dods - The OPeNDAP DAP2 data service. Returns data to the user as
     * described in
     * the DAP2 specification </li>
     * <li>ascii - The requested data as columns of ASCII values. </li>
     * <li>info - An HTML document providing a easy to read view of the DDS
     * and DAS information. </li>
     * <li>html - The HTML request form from which users can choose wich
     * components of a dataset they wish
     * to retrieve. </li>
     * </ui>
     *
     * @param request  The client request that we are evaluating.
     * @param response The respons ethat we are sending back.
     * @param sendResponse If this is true a response will be sent. If it is
     * the request will only be evaluated to determine if a response can be
     * generated.
     * @return true if the request was handled as an OPeNDAP service request,
     * false otherwise.
     * @throws Exception .
     */
    private boolean dataSetDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);

        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        DataSourceInfo dsi;

        Method dispatchMethod;

        String requestURL = request.getRequestURL().toString();
        for(Pattern p: dispatchMethods.keySet()){
            if(p.matcher(requestURL).matches()){
                log.info("The request URL: "+requestURL+" matches " +
                        "the pattern: \""+p.pattern()+"\"");

                dsi = getDataSourceInfo(dataSource);

                if(dsi.isDataset()){
                    if(sendResponse){

                        dispatchMethod = dispatchMethods.get(p);
                        dispatchMethod.invoke(this,request,response);
                    }
                    return true;
                }
            }
        }
        return false;
    }







    public DataSourceInfo getDataSourceInfo(String dataSourceName) throws Exception {
        return new BESDataSource(dataSourceName, new BesApi());
    }




    /**
     * ************************************************************************
     * Default handler for the client's DAS request. Operates on the assumption
     * that the DAS information is cached on a disk local to the server. If you
     * don't like that, then you better Animal it in your server :)
     * <p/>
     * <p>Once the DAS has been parsed it is sent to the requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDAS(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDAS() for dataset: " + dataSource+
                "    CE: '" + constraintExpression + "'");

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dods_das");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        String xdap_accept = request.getHeader("XDAP-Accept");

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeDAS(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){

            response.setHeader("Content-Description", "dods_error");

            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }


        os.flush();
        log.info("Sent DAS");


    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the writeDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDDS(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDDS() for dataset: " + dataSource+
                "    CE: '" + constraintExpression + "'");

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        String xdap_accept = request.getHeader("XDAP-Accept");


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeDDS(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){

            response.setHeader("Content-Description", "dods_error");

            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }

        os.flush();

        log.info("Sent DDS");

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the writeDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDDX(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = request.getRequestURL().toString();

        log.debug("sendDDX() for dataset: " + dataSource+
                "    CE: '" + constraintExpression + "'");

        response.setContentType("text/xml");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        // This will need to be dependent on the DAP version with 3.2 and
        // earlier using dods_ddx and 3.3 or later using dap4_ddx.
        response.setHeader("Content-Description", "dods_ddx");

        // This hedaer indicates to the client that the content of this response
        // is dependant on the HTTP request header XDAP-Accept
        response.setHeader("Vary", "XDAP-Accept");

        // Because the content of this response is dependant on a client provided
        // HTTP header (XDAP-Accept) it is useful to include this Cach-Control
        // header to make caching work correctly...
        response.setHeader("Cache-Control", "public");


        String xdap_accept = request.getHeader("XDAP-Accept");


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeDDX(
                dataSource,
                constraintExpression,
                xdap_accept,
                xmlBase,
                os,
                erros)){
            response.setHeader("Content-Description", "dods_error");
            String msg = new String(erros.toByteArray());
            log.error("BES Error. Message: \n"+msg);
            os.write(msg.getBytes());

        }


        os.flush();
        log.info("Sent DDX");

    }
    /***************************************************************************/



    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the writeDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDataDDX(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = request.getRequestURL().toString();


        MimeBoundary mb = new MimeBoundary();
        String startID = mb.newContentID();

        log.debug("sendDataDDX() for dataset: " + dataSource+
                "    CE: '" + constraintExpression + "'");


        response.setContentType("Multipart/Related;  "+
                                "type=\"text/xml\";  "+
                                "start=\"<"+startID+">\";  "+
                                "boundary=\""+mb.getBoundary()+"\"");


        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dap4_data_ddx");

        // This hedaer indicates to the client that the content of this response
        // is dependant on the HTTP request header XDAP-Accept
        response.setHeader("Vary", "XDAP-Accept");

        // Because the content of this response is dependant on a client provided
        // HTTP header (XDAP-Accept) it is useful to include this Cach-Control
        // header to make caching work correctly...
        response.setHeader("Cache-Control", "public");



        String xdap_accept = request.getHeader("XDAP-Accept");


        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();



        if(!BesXmlAPI.writeDataDDX(
                dataSource,
                constraintExpression,
                xdap_accept,
                xmlBase,
                startID,
                mb.getBoundary(),
                os,
                erros)){
            response.setHeader("Content-Description", "dods_error");
            String msg = new String(erros.toByteArray());
            log.error("BES Error. Message: \n"+msg);
            os.write(msg.getBytes());

        }


        os.print(mb.getClosingBoundary());
        os.flush();
        log.info("Sent DataDDX");

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's data request. Requires the writeDDS()
     * method implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed, the data is read (using the class in the
     * localized server factory etc.), compared to the constraint expression,
     * and then sent to the client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDAP2Data(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDAP2Data() For: " + dataSource+
                "    CE: '" + constraintExpression + "'");

        response.setContentType("application/octet-stream");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dods_data");


        String xdap_accept = request.getHeader("XDAP-Accept");

        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeDap2Data(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){
            response.setHeader("Content-Description", "dods_error");
            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }

        os.flush();
        log.info("Sent DAP2 data response.");

    }

    /***************************************************************************/



    public void sendASCII(HttpServletRequest request,
                          HttpServletResponse response)
            throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String context = request.getContextPath();

        response.setContentType("text/plain");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dods_ascii");


        String xdap_accept = request.getHeader("XDAP-Accept");


        log.debug("sendASCII(): Data For: " + dataSource +
                    "    CE: '" + constraintExpression + "'");



        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesXmlAPI.writeASCII(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){

//            String msg = new String(erros.toByteArray());
//            log.error(msg);
//            os.write(msg.getBytes());

            response.setHeader("Content-Description", "dods_error");
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            //besError.setErrorCode(BESError.INTERNAL_ERROR);
            besError.sendErrorResponse(systemPath, context, response);
            log.error(besError.getMessage());
        }

        os.flush();
        log.info("Sent ASCII.");


    }


    
    public void sendXmlData(HttpServletRequest request,
                          HttpServletResponse response)
            throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String context = request.getContextPath();

        response.setContentType("text/xml");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dap_xml");


        String xdap_accept = request.getHeader("XDAP-Accept");


        log.debug("sendXmlData(): Data For: " + dataSource +
                    "    CE: '" + constraintExpression + "'");



        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesXmlAPI.writeXmlDataResponse(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){

//            String msg = new String(erros.toByteArray());
//            log.error(msg);
//            os.write(msg.getBytes());

            response.setHeader("Content-Description", "dods_error");
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            //besError.setErrorCode(BESError.INTERNAL_ERROR);
            besError.sendErrorResponse(systemPath, context, response);
            log.error(besError.getMessage());
        }

        os.flush();
        log.info("Sent XML Data Response.");


    }




    public void sendHTMLRequestForm(HttpServletRequest request,
                                    HttpServletResponse response)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String requestSuffix = ReqInfo.getRequestSuffix(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        String context = request.getContextPath();

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dods_form");



        String xdap_accept = request.getHeader("XDAP-Accept");

        log.debug("sendHTMLRequestForm(): Sending HTML Data Request Form For: "
                + dataSource +
                "    CE: '" + constraintExpression + "'");


        OutputStream os = response.getOutputStream();

        String url = request.getRequestURL().toString();

        int suffix_start = url.lastIndexOf("." + requestSuffix);

        url = url.substring(0, suffix_start);


        log.debug("sendHTMLRequestForm(): HTML Form URL: " + url);

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeHTMLForm(
                dataSource,
                xdap_accept,
                url,
                os,
                erros)){
            response.setHeader("Content-Description", "dods_error");
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(systemPath, context, response);
            log.error(besError.getMessage());
        }

        os.flush();


        log.info("Sent HTML data request form.");


    }


    public void sendInfo(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);

        String context = request.getContextPath();

        response.setContentType("text/html");
        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "dods_description");


        String xdap_accept = request.getHeader("XDAP-Accept");

        log.debug("sendINFO() for: " + dataSource);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeHtmlInfoPage(
                dataSource,
                xdap_accept,
                os,
                erros)){

            response.setHeader("Content-Description", "dods_error");
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(systemPath, context, response);
            log.error(besError.getMessage());
        }

        os.flush();

        log.info("Sent Info.");



    }





    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the writeDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDDX2RDF(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception  {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        String context = request.getContextPath();

        String xmlBase = request.getRequestURL().toString();
        int suffix_start = xmlBase.lastIndexOf("." + requestSuffix);
        xmlBase = xmlBase.substring(0, suffix_start);


        log.debug("sendDDX2RDF() for dataset: " + dataSource);


        String accepts = request.getHeader("Accepts");

        if(accepts!=null && accepts.equalsIgnoreCase("application/rdf+xml"))
            response.setContentType("application/rdf+xml");
        else
            response.setContentType("text/xml");

        Version.setOpendapMimeHeaders(request,response, new BesApi());
        response.setHeader("Content-Description", "text/xml");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");




        String xdap_accept = "3.2";


        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        Document ddx = new Document();


        if(!BesXmlAPI.getDDXDocument(
                dataSource,
                constraintExpression,
                xdap_accept,
                xmlBase,
                ddx)){
            response.setHeader("Content-Description", "dods_error");
            BESError error = new BESError(ddx);
            error.sendErrorResponse(systemPath, context, response);

        }
        else {

            ddx.getRootElement().setAttribute("dataset_id",dataSource);

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            String currentDir = System.getProperty("user.dir");
            String xslDir = systemPath + "/xsl";
            log.debug("Cached working directory: "+currentDir);

            log.debug("Changing working directory to "+ xslDir);
            System.setProperty("user.dir",xslDir);

            String xsltDocName = "dap_3.2_ddxToRdfTriples.xsl";
            SAXBuilder sb = new SAXBuilder();
            Document xsltDoc = sb.build(xsltDocName);



            //xsltDoc.getRootElement().addNamespaceDeclaration(Namespace.getNamespace("att",xmlBase+"/att#"));


            XSLTransformer transformer = new XSLTransformer(xsltDoc);

            Document rdf = null;
            try {
                rdf = transformer.transform(ddx);

            } catch (Exception e) {
                sendRdfErrorResponse(e, dataSource, context, response);
                log.error(e.getMessage());
            }



            if(rdf!=null){
                xmlo.output(rdf,os);
                os.flush();
                log.info("Sent RDF version of DDX.");
            }

            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);

        }

    }
    /***************************************************************************/



    public void sendRdfErrorResponse(Exception e, String dataSource, String context, HttpServletResponse response) throws Exception {

        String errorPageTemplate = systemPath + "/error/error.html.proto";



        String errorMessage =
                        "<p align=\"center\">I'm sorry.</p>\n" +
                        "<p align=\"center\">You requested the RDF representation of the metadata for the dataset:</p>\n" +
                        "<p align=\"center\" class=\"bold\">"+dataSource+" </p>\n" +
                        "<p align=\"center\">The server attempted to transform the metadata in the dataset, " +
                                "represented as a DDX document, into an RDF representation.</p>\n" +
                        "<p align=\"center\">The transform failed, and returned this specific error message:</p>\n" +
                        "<p align=\"center\" class=\"bold\">" + e.getMessage() + "</p>\n";


        HttpResponder.sendHttpErrorResponse(500, errorMessage, errorPageTemplate, context, response);

    }




    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the writeDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @throws Exception When the bad things be happening
     * @see ReqInfo
     */
    public void sendNetcdfFileOut(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String fullSourceName = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);


        log.debug("sendNetcdfFileOut() for dataset: " + dataSource + "?" +
                    constraintExpression);

        String downloadFileName = Scrub.fileName(fullSourceName.substring(fullSourceName.lastIndexOf("/")+1,fullSourceName.length()));
        Pattern startsWithNumber = Pattern.compile("[0-9].*");
        if(startsWithNumber.matcher(downloadFileName).matches())
            downloadFileName = "nc_"+downloadFileName;

        log.debug("sendNetcdfFileOut() downloadFileName: " + downloadFileName );

        String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";

        response.setContentType("application/x-netcdf");
        response.setHeader("Content-Disposition",contentDisposition);

        Version.setOpendapMimeHeaders(request,response, new BesApi());


        String xdap_accept = request.getHeader("XDAP-Accept");

        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeNetcdfFileOut(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){
            response.setHeader("Content-Description", "dods_error");
            String msg = new String(erros.toByteArray());
            log.error(msg);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            os.write(msg.getBytes());

        }

        os.flush();

        log.info("Sent DAP2 data as netCDF file.");


    }
    /***************************************************************************/






}
