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

package opendap.bes;


import opendap.coreServlet.*;

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
 */
public class DapDispatchHandler implements OpendapHttpDispatchHandler {

    private Logger log;
    private boolean initialized;
    private DispatchServlet _servlet;


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
    public void init(DispatchServlet ds, Element config) throws Exception {

        if(initialized) return;

        _servlet = ds;

        dispatchMethods = new HashMap<Pattern,Method>();


        registerDispatchMethod(".*.ddx",     "sendDDX");
        registerDispatchMethod(".*.dds",     "sendDDS");
        registerDispatchMethod(".*.das",     "sendDAS");
        registerDispatchMethod(".*.dods",    "sendDAP2Data");
        registerDispatchMethod(".*.info",    "sendInfo");
        registerDispatchMethod(".*.html?",   "sendHTMLRequestForm");
        registerDispatchMethod(".*.asc(ii)?","sendASCII");
        registerDispatchMethod(".*.nc",      "sendNetcdfFileOut");
        registerDispatchMethod(".*.rdf",     "sendDDX2RDF");


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


        String dataSource = ReqInfo.getDataSource(req);


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


        String dataSource = ReqInfo.getDataSource(request);
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
        return new BESDataSource(dataSourceName);
    }




    /**
     * ************************************************************************
     * Default handler for the client's DAS request. Operates on the assumption
     * that the DAS information is cached on a disk local to the server. If you
     * don't like that, then you better override it in your server :)
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


        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDAS() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_das");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        String xdap_accept = request.getHeader("XDAP-Accept");
        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeDAS(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){


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

        String xdap_accept = request.getHeader("XDAP-Accept");

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeDDS(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){
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

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = request.getRequestURL().toString();

        log.debug("sendDDX() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_ddx");

        // This hedaer indicates to the client that the content of this response
        // is dependant on the HTTP request header XDAP-Accept
        response.setHeader("Vary", "XDAP-Accept");

        // Because the content of this response is dependant on a client provided
        // HTTP header (XDAP-Accept) it is useful to include this Cach-Control
        // header to make caching work correctly...
        response.setHeader("Cache-Control", "public");


        response.setStatus(HttpServletResponse.SC_OK);

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

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendDAP2Data() For: " + dataSource);

        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_data");

        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");

        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeDap2Data(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){
            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }

        os.flush();
        log.info("Sent DAP2 dta response.");

    }

    /***************************************************************************/



    public void sendASCII(HttpServletRequest request,
                          HttpServletResponse response)
            throws Exception {


        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_ascii");

        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");


        log.debug("sendASCII(): Data For: " + dataSource +
                    "    CE: '" + request.getQueryString() + "'");



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

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            //besError.setErrorCode(BESError.INTERNAL_ERROR);
            besError.sendErrorResponse(_servlet,response);
            log.error(besError.getMessage());
        }

        os.flush();
        log.info("Sent ASCII.");


    }




    public void sendHTMLRequestForm(HttpServletRequest request,
                                    HttpServletResponse response)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
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

        if(!BesXmlAPI.writeHTMLForm(
                dataSource,
                xdap_accept,
                url,
                os,
                erros)){
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(_servlet,response);
            log.error(besError.getMessage());
        }

        os.flush();


        log.info("Sent HTML data request form.");


    }


    public void sendInfo(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception {


        String dataSource = ReqInfo.getDataSource(request);

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_description");

        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");

        log.debug("sendINFO() for: " + dataSource);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeHtmlInfoPage(
                dataSource,
                xdap_accept,
                os,
                erros)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(_servlet,response);
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
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        String xmlBase = request.getRequestURL().toString();
        int suffix_start = xmlBase.lastIndexOf("." + requestSuffix);
        xmlBase = xmlBase.substring(0, suffix_start);


        log.debug("sendDDX2RDF() for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "text/xml");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        response.setStatus(HttpServletResponse.SC_OK);


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
            String msg = new String(erros.toByteArray());
            log.error("BES Error. Message: \n"+msg);
            os.write(msg.getBytes());

        }
        else {

            ddx.getRootElement().setAttribute("dataset_id",dataSource);

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            String xsltDocName = ServletUtil.getPath(_servlet, "/docs/xsl/dap_3.2_ddxToRdfTriples.xsl");
            SAXBuilder sb = new SAXBuilder();
            Document xsltDoc = sb.build(xsltDocName);


            //xsltDoc.getRootElement().addNamespaceDeclaration(Namespace.getNamespace("att",xmlBase+"/att#"));


            XSLTransformer transformer = new XSLTransformer(xsltDoc);

            Document rdf = transformer.transform(ddx);

            xmlo.output(rdf,os);

            os.flush();
            log.info("Sent RDF version of DDX.");
        }

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
     * @throws Exception When the bad things be happening
     * @see ReqInfo
     */
    public void sendNetcdfFileOut(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);


        log.debug("sendNetcdfFileOut() for dataset: " + dataSource + "?" +
                    constraintExpression);


        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));

        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");

        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesXmlAPI.writeNetcdfFileOut(
                dataSource,
                constraintExpression,
                xdap_accept,
                os,
                erros)){
            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }

        os.flush();

        log.info("Sent DAP2 data as netCDF file.");


    }
    /***************************************************************************/






}
