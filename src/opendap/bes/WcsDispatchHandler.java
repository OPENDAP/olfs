/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
//
// Copyright (c) 2006 OPeNDAP, Inc.
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
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.util.Date;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

/**
 * Handler for DAP requests.
 */
public class WcsDispatchHandler implements OpendapHttpDispatchHandler {

    private MimeTypes mimeTypes;
    private Logger log;
    private boolean initialized;


    public WcsDispatchHandler() {

        super();

        mimeTypes = new MimeTypes();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        initialized = false;
    }





    /**
     * ************************************************************************
     * Intitializes any state needed for the handler.
     */
    public void init(DispatchServlet ds, Element config) throws Exception {

        if(initialized) return;

        log.info("Initialized.");
        initialized = true;

    }


    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {

       return dataSetDispatch(request,null,false);

    }

    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        dataSetDispatch(request,response,true);


    }


    public long getLastModified(HttpServletRequest req) {


        String dataSource = ReqInfo.getDataSource(req);





        log.debug("getLastModified(): Tomcat requesting getlastModified() for collection: " + dataSource );


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

        DataSourceInfo dsi = getDataSourceInfo(dataSource);

        boolean isDataRequest = false;

        if (dsi.sourceExists()) {

            if (requestSuffix != null && dsi.isDataset()) {

                if ( // DDS Response?
                        requestSuffix.equalsIgnoreCase("wcs")
                        ) {
                    isDataRequest = true;
                    if(sendResponse){
                        sendDDS(request, response);
                        log.info("Sent DDS");
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

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        BesAPI.writeDAS(
                dataSource,
                constraintExpression,
                Out,
                BesAPI.DAP2_ERRORS);


        Out.flush();

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

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        BesAPI.writeDDS(
                dataSource,
                constraintExpression,
                Out,
                BesAPI.DAP2_ERRORS);




        Out.flush();


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


        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        BesAPI.writeDDX(
                dataSource,
                constraintExpression,
                Out,
                BesAPI.DAP2_ERRORS);


        Out.flush();

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

        ServletOutputStream sOut = response.getOutputStream();
        OutputStream bOut;

        //boolean compress = false;
        /*
        if (ReqInfo.getAcceptsCompressed(request)) {
            //compress = true;
            response.setHeader("Content-Encoding", "gzip");
            //DeflaterOutputStream dos = new DeflaterOutputStream(sOut);
            DeflaterOutputStream dos = new GZIPOutputStream(sOut);
            BesAPI.writeDap2Data(dataSource, constraintExpression, dos);
            //dos.finish();
            //dos.flush();
            dos.close();
            response.setStatus(HttpServletResponse.SC_OK);

        } else {
            // Commented out because of a bug in the OPeNDAP C++ stuff...
            //response.setHeader("Content-Encoding", "plain");
            bOut = new BufferedOutputStream(sOut);
            BesAPI.writeDap2Data(dataSource, constraintExpression, bOut);
            response.setStatus(HttpServletResponse.SC_OK);
            bOut.flush();
        }
*/

        bOut = new BufferedOutputStream(sOut);

        BesAPI.writeDap2Data(
                dataSource,
                constraintExpression,
                bOut,
                BesAPI.DAP2_ERRORS);



        bOut.flush();

    }

    /***************************************************************************/



    /**
     * ***********************************************************************
     */


    public void sendDir(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {

        log.debug("sendDir()");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_directory");

        response.setStatus(HttpServletResponse.SC_OK);

        S4Dir.sendDIR(request, response);

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


        OutputStream os = new BufferedOutputStream(response.getOutputStream());

        String url = request.getRequestURL().toString();

        int suffix_start = url.lastIndexOf("." + requestSuffix);

        url = url.substring(0, suffix_start);


        log.debug("sendHTMLRequestForm(): HTML Form URL: " + url);

        BesAPI.writeHTMLForm(dataSource, url, os);

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

        OutputStream os = new BufferedOutputStream(response.getOutputStream());

        BesAPI.writeINFOPage(
                dataSource,
                os,
                BesAPI.DAP2_ERRORS);

        os.flush();



    }


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

        OutputStream bOut;


        ServletOutputStream sOut = response.getOutputStream();

        /*
        //boolean compress = false;
        if (ReqInfo.getAcceptsCompressed(request)) {
            //compress = true;
            response.setHeader("Content-Encoding", "deflate");
            DeflaterOutputStream dos = new DeflaterOutputStream(sOut);
            //DeflaterOutputStream dos = new GZIPOutputStream(sOut);
            BesAPI.writeASCII(dataSource, constraintExpression, dos);
            dos.finish();
            dos.flush();
            response.setStatus(HttpServletResponse.SC_OK);

        } else {
            // Commented out because of a bug in the OPeNDAP C++ stuff...
            //response.setHeader("Content-Encoding", "plain");
            bOut = new BufferedOutputStream(sOut);
            BesAPI.writeASCII(dataSource, constraintExpression, bOut);
            response.setStatus(HttpServletResponse.SC_OK);
            bOut.flush();
        }

*/

        bOut = new BufferedOutputStream(sOut);

        BesAPI.writeASCII(
                dataSource,
                constraintExpression,
                bOut,
                BesAPI.DAP2_ERRORS);

        bOut.flush();


    }


    public void sendFile(HttpServletRequest req,
                         HttpServletResponse response)
            throws Exception {


        String name = req.getPathInfo();


        log.debug("sendFile(): Sending file \"" + name+"\"");

        String suffix = ReqInfo.getRequestSuffix(req);

        if (suffix != null) {
            String mType = mimeTypes.getMimeType(suffix);

            if (mType != null)
                response.setContentType(mType);

            log.debug("   MIME type: " + mType + "  ");
        }

        response.setStatus(HttpServletResponse.SC_OK);


        ServletOutputStream sos = response.getOutputStream();
        BesAPI.writeFile(name, sos, BesAPI.DAP2_ERRORS);


    }



    /**
     * Sends an html document to the client explaining that they have used a
     * poorly formed URL and then the help page...
     *
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @throws IOException If it can't right the response.
     */
    private void badURL(HttpServletResponse response) throws Exception {

        log.debug("Sending Bad URL Page.");

        response.setContentType("text/html");

        // OPeNDAP Headers not needed (James said so!)
        //response.setHeader("XDODS-Server", odh.getXDODSServerVersion(request));
        //response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion(request));
        //response.setHeader("XDAP", odh.getXDAPVersion(request));

        response.setHeader("Content-Description", "BadURL");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        pw.println("<h3>Error in URL</h3>");
        pw.println("<p>The URL extension did not match any that are known by this");
        pw.println("server. Here is a list of the five extensions that are be recognized by");
        pw.println("all OPeNDAP servers:</p>");
        pw.println("<ui>");
        pw.println("    <li>ddx</li>");
        pw.println("    <li>dds</li>");
        pw.println("    <li>das</li>");
        pw.println("    <li>dods</li>");
        pw.println("    <li>info</li>");
        pw.println("    <li>html</li>");
        pw.println("    <li>ascii</li>");
        pw.println("</ui>");
        pw.println("<p>If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a></p>");

        pw.flush();


    }













}
