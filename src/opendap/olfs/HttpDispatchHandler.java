/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.olfs;

import opendap.dap.DODSException;
import opendap.dap.DAS;
import opendap.dap.DefaultFactory;
import opendap.dap.DDS;
import opendap.dap.Server.ServerDDS;
import opendap.ppt.PPTException;
import opendap.util.Debug;
import opendap.coreServlet.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.StringTokenizer;
import java.util.Iterator;

import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import thredds.cataloggen.SimpleCatalogBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 28, 2006
 * Time: 12:42:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpDispatchHandler implements OpendapHttpDispatchHandler {


    /**
     * ************************************************************************
     * Server Version Document, Cached by init()
     *
     * @serial
     */
    private Document _serverVersionDocument = null;


    public HttpDispatchHandler() {
        super();
    }

    /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from any of the dods code...
     */
    public void init(DispatchServlet ds) throws ServletException {

        // debuggering
        String debugOn = ds.getInitParameter("DebugOn");
        if (debugOn != null) {
            System.out.println("** DebugOn **");
            StringTokenizer toker = new StringTokenizer(debugOn);
            while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
        }

        configBES(ds);

        try {
            cacheServerVersionDocument();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Could not get version document from BES.", e);
        }

    }

    private void configBES(HttpServlet ds) throws ServletException {
        String besHost = ds.getInitParameter("BackEndServerHost");
        if (besHost == null)
            throw new ServletException("Servlet configuration must included BackEndServer\n");

        String besPort = ds.getInitParameter("BackEndServerPort");
        if (besPort == null)
            throw new ServletException("Servlet configuration must included BackEndServerPort\n");


        System.out.print("Configuring BES ... ");

        if (BesAPI.configure(besHost, Integer.parseInt(besPort)))
            System.out.println("");
        else
            System.out.println("That's odd, it was already done...");

    }


    /**
     * Caches the OLFS version Document object. Calling this method ccauses the OLFS to query
     * the BES to determine the various version components located there.
     */
    private void cacheServerVersionDocument() throws IOException,
            PPTException,
            BadConfigurationException,
            JDOMException, BESException {

        System.out.println("Getting Server Version Document.");

        //UID reqid = new UID();
        //System.out.println("    RequestID: "+reqid);

        // Get the Version info from the BES.
        Document doc = BesAPI.showVersion();

        // Add a version element for this, the OLFS server
        doc.getRootElement().addContent(opendap.olfs.Version.getVersionElement());


        _serverVersionDocument = doc;


    }

    private Document getVersionDocument() {
        return _serverVersionDocument;
    }


    /**
     * @return A string containing the value of the XDODS-Server MIME header as ascertained
     *         by querying the BES.
     */
    public String getXDODSServerVersion() {

        if (getVersionDocument() != null) {
            Iterator i = getVersionDocument().getRootElement().getChild("BES").getChildren("lib").iterator();

            while (i.hasNext()) {
                Element e = (Element) i.next();
                if (e.getChildTextTrim("name").equalsIgnoreCase("libdap")) {
                    return ("dods/" + e.getChildTextTrim("version"));
                }
            }
        }

        return ("Server-Version-Unknown");

    }

    /**
     * @return A String containing the value of the XOPeNDAP-Server MIME header ascertained by querying
     *         the BES and conforming to the DAP4 specification.
     */
    public String getXOPeNDAPServerVersion() {
        if (getVersionDocument() != null) {

            String opsrv = "";

            Iterator i = getVersionDocument().getRootElement().getChildren().iterator();

            while (i.hasNext()) {
                Element pkg = (Element) i.next();
                Iterator j = pkg.getChildren("lib").iterator();
                while (j.hasNext()) {
                    Element lib = (Element) j.next();
                    opsrv += " " + lib.getChildTextTrim("name") + "/" + lib.getChildTextTrim("version");
                }
            }
            return (opsrv);
        }
        return ("Server-Version-Unknown");

    }


    /**
     * @return A String containing the XDAP MIME header value that describes the DAP specifcation that
     *         the server response conforms to.
     */
    public String getXDAPVersion(HttpServletRequest request) {
        double hval = 0.0;
        String hver = "";

        String clientDapVer = null;

        if (request != null)
            clientDapVer = request.getHeader("XDAP");

        if (getVersionDocument() != null) {

            String responseDAP = null;

            Iterator i = getVersionDocument().getRootElement().getChild("DAP").getChildren("version").iterator();

            while (i.hasNext()) {
                Element v = (Element) i.next();
                String ver = v.getTextTrim();
                double vval = Double.parseDouble(ver);
                if (hval < vval) {
                    hval = vval;
                    hver = ver;
                }

                if (clientDapVer != null && clientDapVer.equals(ver))
                    responseDAP = ver;
            }
            if (responseDAP == null)
                return (hver);
            return (responseDAP);
        }

        return ("DAP-Version-Unknown");


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
     * @param rs       The ReqState of this client request. Contains all kinds of
     *                 important stuff.
     * @see ReqState
     */
    public void sendDAS(HttpServletRequest request,
                        HttpServletResponse response,
                        ReqState rs)
            throws IOException, ServletException {

        if (Debug.isSet("showResponse"))
            System.out.println("doGetDAS for dataset: " + rs.getDataset());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_das");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());
        try {
            DDS ddx = BesAPI.getDDX(rs.getDataset(), rs.getConstraintExpression());
            ddx.getDAS().print(Out);
            //BesAPI.writeDAS(rs.getDataset(), rs.getConstraintExpression(), Out);

        } catch (DODSException de) {
            Util.opendapExceptionHandler(de, response);
        } catch (PPTException e) {
            DODSException de = new DODSException(e.getMessage() + e.getStackTrace());
            Util.opendapExceptionHandler(de, response);
        } finally {
            Out.flush();
        }
        response.setStatus(HttpServletResponse.SC_OK);

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
     * @param rs       The ReqState of this client request. Contains all kinds of
     *                 important stuff.
     * @see ReqState
     */
    public void sendDDS(HttpServletRequest request,
                        HttpServletResponse response,
                        ReqState rs)
            throws IOException, ServletException {

        System.out.println("Flow in doGetDDS()");


        if (Debug.isSet("showResponse"))
            System.out.println("doGetDDS for dataset: " + rs.getDataset());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());
        try {

            // We get a DDX from the BES and then parse it. We use the parsed
            // one to send a DDS representation. Since the BES applies the
            // constraint expression, we don't need to get a ServerDDS (full
            // of Server BaseTypes) with whic we could apply the conststraint
            //expression - it's already done.
            DDS ddx = BesAPI.getDDX(rs.getDataset(), rs.getConstraintExpression());
            ddx.print(Out);

            // Earlier method - asks BES directly for a DDS and then writes it
            // out to the response stream.
            //BesAPI.writeDDS(rs.getDataset(), rs.getConstraintExpression(), Out);

        } catch (DODSException de) {
            Util.opendapExceptionHandler(de, response);
        } catch (PPTException e) {
            DODSException de = new DODSException(e.getMessage() + e.getStackTrace());
            Util.opendapExceptionHandler(de, response);
        } finally {
            Out.flush();
        }

        System.out.println("Flow returned to doGetDDS()");

        response.setStatus(HttpServletResponse.SC_OK);

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
     * @param rs       The ReqState of this client request. Contains all kinds of
     *                 important stuff.
     * @see ReqState
     */
    public void sendDDX(HttpServletRequest request,
                        HttpServletResponse response,
                        ReqState rs)
            throws IOException, ServletException {

        System.out.println("Flow in doGetDDX()");


        if (Debug.isSet("showResponse"))
            System.out.println("doGetDDX for dataset: " + rs.getDataset());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());
        try {
            BesAPI.writeDDX(rs.getDataset(), rs.getConstraintExpression(), Out);

        } catch (DODSException de) {
            Util.opendapExceptionHandler(de, response);
        } catch (PPTException e) {
            DODSException de = new DODSException(e.getMessage() + e.getStackTrace());
            Util.opendapExceptionHandler(de, response);
        } finally {
            Out.flush();
        }

        System.out.println("Flow returned to doGetDDX()");

        response.setStatus(HttpServletResponse.SC_OK);

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
     * @param rs       The ReqState of this client request. Contains all kinds of
     *                 important stuff.
     * @see ReqState
     */
    public void sendDODS(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws IOException, ServletException {


        if (Debug.isSet("showResponse"))
            System.out.println("doGetOPeNDAP For: " + rs.getDataset());

        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_data");

        ServletOutputStream sOut = response.getOutputStream();
        OutputStream bOut;

        System.out.println("Flow in doGetOPeNDAP()");


        if (rs.getAcceptsCompressed()) {
            response.setHeader("Content-Encoding", "deflate");
            bOut = new DeflaterOutputStream(sOut);
        } else {
            // Commented out because of a bug in the OPeNDAP C++ stuff...
            //response.setHeader("Content-Encoding", "plain");
            bOut = new BufferedOutputStream(sOut);
        }

        try {
            BesAPI.writeDapData(rs.getDataset(), rs.getConstraintExpression(), bOut);

        } catch (DODSException de) {
            Util.opendapExceptionHandler(de, response);
        } catch (PPTException e) {
            DODSException de = new DODSException(e.getMessage() + e.getStackTrace());
            Util.opendapExceptionHandler(de, response);
        } finally {
            bOut.flush();
        }
        response.setStatus(HttpServletResponse.SC_OK);

    }

    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's version request.
     * <p/>
     * <p>Returns a plain text document with server version and OPeNDAP core
     * version #'s
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void sendVersion(HttpServletRequest request,
                            HttpServletResponse response,
                            ReqState rs)
            throws IOException, ServletException, DODSException {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending Version Document:");

        response.setContentType("text/xml");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_version");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintStream ps = new PrintStream(response.getOutputStream());

        Document vdoc = rs.getVersionDocument();
        if (vdoc == null) {
            throw new DODSException("Internal Error: Version Document not initialized.");
        }
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        //XMLOutputter xout = new XMLOutputter();
        xout.output(rs.getVersionDocument(), ps);
        ps.flush();

        if (Debug.isSet("showResponse")) {
            xout.output(rs.getVersionDocument(), System.out);
            System.out.println("Document Sent.");
            System.out.println("\nMIME Headers:");
            System.out.println("    XDODS-Server: " + rs.getXDODSServer());
            System.out.println("    XOPeNDAP-Server: " + rs.getXOPeNDAPServer());
            System.out.println("    XDAP: " + rs.getXDAP(request));
            System.out.println("\nEnd Response.");
        }

        response.setStatus(HttpServletResponse.SC_OK);

    }

    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for OPeNDAP catalog.xml requests.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void sendCatalog(HttpServletRequest request,
                            HttpServletResponse response,
                            ReqState rs)
            throws IOException, ServletException, DODSException, PPTException, JDOMException, BESException {


        response.setContentType("text/xml");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_catalog");

        PrintWriter pw = new PrintWriter(response.getOutputStream());

//        System.out.println("rootPath:    "+rootPath);
//        System.out.println("contentPath: "+contentPath);
//        System.out.println("myPath:      "+"/"+rs.getDataset());

/*
        if(catHandler.processReqForCatalog(this,request,response, contentPath)){
            System.out.println("Processed Catalog Request");
        }
        else {
            System.out.println("Rejected Catalog Request");
        }


        if (Debug.isSet("showResponse")){
            System.out.println("doGetCatalog() - Instantiating BESCrawlableDataset object (a CrawlableDataset)");
        }

*/

        // Strip off the catalog request

        String path = rs.getDataset();
        path = path.endsWith("/catalog") ? path.substring(0, path.length() - 8) : path;

        path = BESCrawlableDataset.besPath2ThreddsPath(path);

        BESCrawlableDataset s4cd = new BESCrawlableDataset(path, null);

        if (s4cd.isCollection()) {

            if (Debug.isSet("showResponse")) {
                System.out.println("doGetCatalog() - Instantiating SimpleCatalogBuilder");
            }


            SimpleCatalogBuilder scb = new SimpleCatalogBuilder(
                    "",                                   // CollectionID, which for us needs to be empty.
                    BESCrawlableDataset.getRootDataset(), // Root dataset of this collection
                    "OPeNDAP-Server4",                    // Service Name
                    "OPeNDAP",                            // Service Type Name
                    request.getRequestURI().substring(0, request.getRequestURI().lastIndexOf(request.getPathInfo()) + 1)); // Base URL for this service

            if (Debug.isSet("showResponse")) {
                System.out.println("doGetCatalog() - Generating catalog");
            }


            pw.print(scb.generateCatalogAsString(s4cd));


        } else {
            response.setContentType("text/html");
            String msg = "ERROR: THREDDS catalogs may only be requested for collections, not for individual data sets.";
            throw new DODSException(msg);
        }

        //printCatalog(request, pw);
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }

    /**
     * ***********************************************************************
     */


    public void sendDir(HttpServletRequest request,
                        HttpServletResponse response,
                        ReqState rs)
            throws Exception {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_directory");

        S4Dir.sendDIR(request, response, rs);

        response.setStatus(HttpServletResponse.SC_OK);
    }


    /**
     * @param request
     * @param response
     * @param rs
     * @throws Exception
     */
    public void sendHTMLRequestForm(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ReqState rs)
            throws Exception {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_form");

        System.out.println("Sending OPeNDAP Data Request Form For: " + rs.getDataset() +
                "    CE: '" + request.getQueryString() + "'");

        ServerDDS ddx = BesAPI.getDDX(rs.getDataset(), rs.getConstraintExpression());
        DAS das = ddx.getDAS();

        PrintWriter pw = new PrintWriter(response.getOutputStream());

        DefaultResponse.sendHtmlResponse(pw, rs, ddx, das);

        response.setStatus(HttpServletResponse.SC_OK);

    }


    /**
     * @param request
     * @param response
     * @param rs
     * @throws Exception
     */
    public void sendInfo(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws Exception {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_description");

        if (Debug.isSet("showResponse"))
            System.out.println("doGetINFO For: " + rs.getDataset());


        ServerDDS ddx = BesAPI.getDDX(rs.getDataset(), rs.getConstraintExpression());

        DAS das = ddx.getDAS();

        PrintStream pw = new PrintStream(response.getOutputStream());

        DefaultResponse.sendInfoResponse(pw, rs, ddx, das);

        response.setStatus(HttpServletResponse.SC_OK);


    }


    public void sendASCII(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
            throws Exception {


        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_ascii");

        if (Debug.isSet("showResponse"))
            System.out.println("Sending OPeNDAP ASCII Data For: " + rs.getDataset() +
                    "    CE: '" + request.getQueryString() + "'");


        String requestURL, ce;

        if (request.getQueryString() == null) {
            ce = "";
        } else {
            ce = "?" + request.getQueryString();
        }


        requestURL = rs.getRequestURL();

        if (Debug.isSet("showResponse")) {
            System.out.println("New Request URL Resource: '" + requestURL + "'");
            System.out.println("New Request Constraint Expression: '" + ce + "'");
        }


        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BesAPI.writeDapData(rs.getDataset(), rs.getConstraintExpression(), os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        PrintWriter pw = new PrintWriter(response.getOutputStream());


        DefaultResponse.sendAsciiResponse(pw, rs, is);

        response.setStatus(HttpServletResponse.SC_OK);

    }


    public void sendHelpPage(HttpServletRequest request, HttpServletResponse response, ReqState rs) throws Exception {
        if (Debug.isSet("showResponse"))
            System.out.println("Sending Help Page.");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_help");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));


        printHelpPage(pw);
        pw.flush();

        response.setStatus(HttpServletResponse.SC_OK);

    }


    /**
     * ************************************************************************
     * Prints the OPeNDAP Server help page to the passed PrintWriter
     *
     * @param pw PrintWriter stream to which to dump the help page.
     */
    private void printHelpPage(PrintWriter pw) {

        pw.println("<h3>OPeNDAP Server Help</h3>");
        pw.println("To access most of the features of this OPeNDAP server, append");
        pw.println("one of the following a eight suffixes to a URL: .das, .dds, .dods, .ddx, .blob, .info,");
        pw.println(".ver or .help. Using these suffixes, you can ask this server for:");
        pw.println("<dl>");
        pw.println("<dt> das  </dt> <dd> Dataset Attribute Structure (DAS)</dd>");
        pw.println("<dt> dds  </dt> <dd> Dataset Descriptor Structure (DDS)</dd>");
        pw.println("<dt> dods </dt> <dd> DataDDS object (A constrained DDS populated with data)</dd>");
        pw.println("<dt> ddx  </dt> <dd> XML version of the DDS/DAS</dd>");
        pw.println("<dt> blob </dt> <dd> Serialized binary data content for requested data set, " +
                "with the constraint expression applied.</dd>");
        pw.println("<dt> info </dt> <dd> info object (attributes, types and other information)</dd>");
        pw.println("<dt> html </dt> <dd> html form for this dataset</dd>");
        pw.println("<dt> ver  </dt> <dd> return the version number of the server</dd>");
        pw.println("<dt> help </dt> <dd> help information (this text)</dd>");
        pw.println("</dl>");
        pw.println("For example, to request the DAS object from the FNOC1 dataset at URI/GSO (a");
        pw.println("test dataset) you would appand `.das' to the URL:");
        pw.println("http://opendap.gso.uri.edu/cgi-bin/nph-nc/data/fnoc1.nc.das.");

        pw.println("<p><b>Note</b>: Many OPeNDAP clients supply these extensions for you so you don't");
        pw.println("need to append them (for example when using interfaces supplied by us or");
        pw.println("software re-linked with a OPeNDAP client-library). Generally, you only need to");
        pw.println("add these if you are typing a URL directly into a WWW browser.");
        pw.println("<p><b>Note</b>: If you would like version information for this server but");
        pw.println("don't know a specific data file or data set name, use `/version' for the");
        pw.println("filename. For example: http://opendap.gso.uri.edu/cgi-bin/nph-nc/version will");
        pw.println("return the version number for the netCDF server used in the first example. ");

        pw.println("<p><b>Suggestion</b>: If you're typing this URL into a WWW browser and");
        pw.println("would like information about the dataset, use the `.info' extension.");

        pw.println("<p>If you'd like to see a data values, use the `.html' extension and submit a");
        pw.println("query using the customized form.");

    }
    //**************************************************************************


}
