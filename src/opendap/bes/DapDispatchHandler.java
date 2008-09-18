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
import java.util.Iterator;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import thredds.servlet.ServletUtil;

/**
 * Handler for DAP requests.
 */
public class DapDispatchHandler implements OpendapHttpDispatchHandler {

    private Logger log;
    private boolean initialized;
    private DispatchServlet dispatchServlet;


    public DapDispatchHandler() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;

    }





    /**
     * ************************************************************************
     * Intitializes any state needed for the handler.
     */
    public void init(DispatchServlet ds, Element config) throws Exception {

        if(initialized) return;

        dispatchServlet = ds;
        log.info("Initialized.");
        initialized = true;

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


        try {
            DataSourceInfo dsi = new BESDataSource(dataSource);
            log.debug("getLastModified(): Returning: " + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            log.debug("getLastModified(): Returning: -1");
            return -1;
        }



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
        String requestSuffix = ReqInfo.getRequestSuffix(request);
        DataSourceInfo dsi = null;

        boolean isWCS = WcsCatalog.isWcsDataset(request);
        if(isWCS){
        log.debug("dataSetDispatch() - Request is for WCS dataset.\n" +
                "WCS Request URL: " + WcsCatalog.getWcsRequestURL(request));
        }
        else {
            dsi = getDataSourceInfo(dataSource);
        }


        boolean isDataRequest = false;



        if (isWCS || (dsi!=null && dsi.sourceExists()) ) {

            if (requestSuffix != null && (isWCS || (dsi!=null && dsi.isDataset()))) {
                if ( // DDS Response?
                        requestSuffix.equalsIgnoreCase("dds")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){

                        sendDDS(request, response);
                        log.info("Sent DDS");
                    }

                } else if ( // DAS Response?
                        requestSuffix.equalsIgnoreCase("das")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){
                        sendDAS(request, response);
                        log.info("Sent DAS");
                    }

                } else if (  // DDX Response?
                        requestSuffix.equalsIgnoreCase("ddx")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){
                        sendDDX(request, response);
                        log.info("Sent DDX");
                    }

                } else if ( // DAP2 (aka .dods) Response?
                        requestSuffix.equalsIgnoreCase("dods")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){
                        sendDAP2Data(request, response);
                        log.info("Sent DAP2 Data");
                    }

                } else if (  // ASCII Data Response.
                        requestSuffix.equalsIgnoreCase("asc") ||
                                requestSuffix.equalsIgnoreCase("ascii")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){
                        sendASCII(request, response);
                        log.info("Sent ASCII");
                    }

                } else if (  // Info Response?
                        requestSuffix.equalsIgnoreCase("info")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){
                        sendInfo(request, response);
                        log.info("Sent Info");
                    }

                } else if (  //HTML Request Form (aka The Interface From Hell) Response?
                        requestSuffix.equalsIgnoreCase("html") ||
                                requestSuffix.equalsIgnoreCase("htm")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){
                        sendHTMLRequestForm(request, response);
                        log.info("Sent HTML Request Form");
                    }


                } else if (requestSuffix.equals("")) {
                    isDataRequest = true;
                    if(sendResponse){
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        log.info("Sent BAD URL (missing Suffix)");
                    }
                } else {
                    isDataRequest = true;
                    if(sendResponse){
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        log.info("Sent BAD URL - not an OPeNDAP request suffix.");
                    }
                }

            }
        }


        return isDataRequest;
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

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.writeDAS(
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_FORMAT)){


            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }


        os.flush();

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

        response.setStatus(HttpServletResponse.SC_OK);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.writeDDS(
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_FORMAT)){
            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }

        os.flush();


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

        if(!BesAPI.writeDDX(
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_FORMAT)){
            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }


        os.flush();

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

        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.writeDap2Data(
                dataSource,
                constraintExpression,
                os,
                erros,
                BesAPI.DAP2_FORMAT)){
            String msg = new String(erros.toByteArray());
            log.error(msg);
            os.write(msg.getBytes());

        }

        os.flush();

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

        log.debug("sendASCII(): Data For: " + dataSource +
                    "    CE: '" + request.getQueryString() + "'");



        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!BesAPI.writeASCII(
                dataSource,
                constraintExpression,
                os,
                erros,
//                BesAPI.DAP2_FORMAT)){
                BesAPI.DEFAULT_FORMAT)){

//            String msg = new String(erros.toByteArray());
//            log.error(msg);
//            os.write(msg.getBytes());

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            //besError.setErrorCode(BESError.INTERNAL_ERROR);
            besError.sendErrorResponse(dispatchServlet,response);
            log.error(besError.getMessage());
        }

        os.flush();


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

        log.debug("sendHTMLRequestForm(): Sending HTML Data Request Form For: "
                + dataSource +
                "    CE: '" + request.getQueryString() + "'");


        OutputStream os = response.getOutputStream();

        String url = request.getRequestURL().toString();

        int suffix_start = url.lastIndexOf("." + requestSuffix);

        url = url.substring(0, suffix_start);


        log.debug("sendHTMLRequestForm(): HTML Form URL: " + url);

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.writeHTMLForm(
                dataSource,
                url,
                os,
                erros)){
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(dispatchServlet,response);
            log.error(besError.getMessage());
        }

        os.flush();




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


        log.debug("sendINFO() for: " + dataSource);

        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.writeINFOPage(
                dataSource,
                os,
                erros,
                BesAPI.DEFAULT_FORMAT)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(dispatchServlet,response);
            log.error(besError.getMessage());
        }

        os.flush();



    }


    public void sendFile(HttpServletRequest req,
                         HttpServletResponse response)
            throws Exception {


        String name = req.getPathInfo();


        log.debug("sendFile(): Sending file \"" + name+"\"");

        String suffix = ReqInfo.getRequestSuffix(req);

        if (suffix != null) {
            String mType = MimeTypes.getMimeType(suffix);

            if (mType != null)
                response.setContentType(mType);

            log.debug("   MIME type: " + mType + "  ");
        }

        response.setStatus(HttpServletResponse.SC_OK);


        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if(!BesAPI.writeFile(
                name,
                os,
                erros,
                BesAPI.DEFAULT_FORMAT)){

            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
            besError.sendErrorResponse(dispatchServlet,response);
            log.error(besError.getMessage());

        }


    }











}
