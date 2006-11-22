/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
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

package opendap.olfs;

import opendap.ppt.PPTException;

import opendap.coreServlet.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import thredds.cataloggen.SimpleCatalogBuilder;
import thredds.servlet.ServletUtil;

/**
 * Handler fo HTTP GET requests.
 */
public class HttpDispatchHandler implements OpendapHttpDispatchHandler {

    private MimeTypes mimeTypes;






    /**
     * ************************************************************************
     * Server Version Document, Cached by init()
     *
     * @serial
     */
    private Document _serverVersionDocument = null;

    /**
     * ************************************************************************
     * Configuration, Cached by init()
     *
     * @serial
     */
    private OLFSConfig _olfsConfig = null;


    public HttpDispatchHandler() {
        super();
    }







    /**
     * ************************************************************************
     * Intitializes any state needed for the handler.
     */
    public void init(HttpServlet ds) throws ServletException {

        mimeTypes = new MimeTypes();

        System.out.println("HttpDispatchHandler.init()");

        configure(ds);

        try {
            cacheServerVersionDocument();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Could not get version document from BES.", e);
        }

    }

    public void destroy(){
        BesAPI.shutdownBES();
    }

    public DataSourceInfo getDataSourceInfo(String dataSourceName) throws Exception {
        return new BESDataSource(dataSourceName);
    }


    public String getVersionStringForTHREDDSCatalog(){
        return "OPeNDAP Server4 ("+ Version.getVersionString()+")" +
                "<font size='-5' color='#5A647E'>" +
                "ServerUUID="+Version.getServerUUID()+"-catalog" +
                "</font><br />";

    }



    /**
     * @param ds
     * @throws ServletException
     */
    private void configure(HttpServlet ds) throws ServletException {


        String filename = ds.getInitParameter("OLFSConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for the OLFS configuration!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }


        System.out.print("Configuring OLFS ... ");


        try {
            _olfsConfig = new OLFSConfig(ServletUtil.getContentPath(ds) + filename);

            if (BesAPI.configure(_olfsConfig.getBESConfig()))
                System.out.println("");
            else
                System.out.println("That's odd, it was already done...");

        }
        catch (Exception e) {
            throw new ServletException(e);
        }


    }


    public boolean useOpendapDirectoryView() {
        return !_olfsConfig.getTHREDDSDirectoryView();
    }




    public boolean allowDirectDataSourceAccess() {
        return _olfsConfig.allowDirectDataSourceAccess();
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

            for (Object o : getVersionDocument().getRootElement().getChild("BES").getChildren("lib")) {
                Element e = (Element) o;
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

            for (Object o : getVersionDocument().getRootElement().getChildren()) {
                Element pkg = (Element) o;
                boolean first = true;
                for (Object o1 : pkg.getChildren("lib")) {
                    Element lib = (Element) o1;
                    if(!first)
                        opsrv += ",";
                    opsrv += " " + lib.getChildTextTrim("name") + "/" + lib.getChildTextTrim("version");
                    first = false;
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

            for (Object o : getVersionDocument().getRootElement().getChild("Handlers").getChild("DAP").getChildren("version")) {
                Element v = (Element) o;
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










    public long getLastModified(HttpServletRequest req){

        String name;


        // Check to see if they are requesting a catalog/contents view of a collection
        if(    ReqInfo.requestForOpendapContents(req) || ReqInfo.requestForTHREDDSCatalog(req)){

            name = ReqInfo.getCollectionName(req);
            if(Debug.isSet("showRequest")) System.out.print("\nTomcat requesting getlastModified() for collection: "+name+"  ");
        }
        else { // Otherwise they are looking for data...
            name = ReqInfo.getDataSource(req);
            if(Debug.isSet("showRequest")) System.out.print("\nTomcat requesting getlastModified() for dataSource: "+name+"  ");
        }

        String path = BESCrawlableDataset.besPath2ThreddsPath(name);

        try {
            BESCrawlableDataset cd = new BESCrawlableDataset(path, null);
            if(Debug.isSet("showRequest")) System.out.println("Returning: "+cd.lastModified());

            return cd.lastModified().getTime();
        }
        catch (Exception e){
            if(Debug.isSet("showRequest")) System.out.println("Returning: -1");
            return -1;
        }



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
        String constraintExpression =  ReqInfo.getConstraintExpression(request);

        if (Debug.isSet("showResponse"))
            System.out.println("doGetDAS for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_das");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        BesAPI.writeDAS(dataSource, constraintExpression, Out);
        response.setStatus(HttpServletResponse.SC_OK);

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
        String constraintExpression =  ReqInfo.getConstraintExpression(request);

        if (Debug.isSet("showResponse"))
            System.out.println("doGetDDS for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        BesAPI.writeDDS(dataSource, constraintExpression, Out);
        response.setStatus(HttpServletResponse.SC_OK);


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
        String constraintExpression =  ReqInfo.getConstraintExpression(request);

        if (Debug.isSet("showResponse"))
            System.out.println("doGetDDX for dataset: " + dataSource);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());

        BesAPI.writeDDX(dataSource, constraintExpression, Out);
        response.setStatus(HttpServletResponse.SC_OK);

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
    public void sendDODS(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception {

        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression =  ReqInfo.getConstraintExpression(request);

        if (Debug.isSet("showResponse"))
            System.out.println("doGetOPeNDAP For: " + dataSource);

        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_data");

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

        BesAPI.writeDap2Data(dataSource, constraintExpression, bOut);
        response.setStatus(HttpServletResponse.SC_OK);


        bOut.flush();

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
                            HttpServletResponse response)
            throws Exception {

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

        Document vdoc = getVersionDocument();
        if (vdoc == null) {
            throw new ServletException("Internal Error: Version Document not initialized.");
        }
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        //XMLOutputter xout = new XMLOutputter();
        xout.output(getVersionDocument(), ps);
        ps.flush();

        if (Debug.isSet("showResponse")) {
            xout.output(getVersionDocument(), System.out);
            System.out.println("Document Sent.");
            System.out.println("\nMIME Headers:");
            System.out.println("    XDODS-Server: " + getXDODSServerVersion());
            System.out.println("    XOPeNDAP-Server: " + getXOPeNDAPServerVersion());
            System.out.println("    XDAP: " + getXDAPVersion(request));
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
                            HttpServletResponse response)
            throws Exception {



        response.setContentType("text/xml");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_catalog");

        PrintWriter pw = new PrintWriter(response.getOutputStream());


        // Strip off the catalog request

        String path = ReqInfo.getDataSource(request);
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
            throw new OPeNDAPException(msg);
        }

        //printCatalog(request, pw);
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }

    /**
     * ***********************************************************************
     */




    public void sendDir(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_directory");

        S4Dir.sendDIR(request, response);

        response.setStatus(HttpServletResponse.SC_OK);
    }


    /**
     * @param request
     * @param response
     */
    public void sendHTMLRequestForm(HttpServletRequest request,
                                    HttpServletResponse response)
            throws Exception  {

        String dataSource = ReqInfo.getDataSource(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_form");

        if (Debug.isSet("showResponse"))
            System.out.println("Sending OPeNDAP Data Request Form For: " + dataSource +
                    "    CE: '" + request.getQueryString() + "'");


            OutputStream os = new BufferedOutputStream(response.getOutputStream());

            String url = request.getRequestURL().toString();

            int suffix_start = url.lastIndexOf("." + requestSuffix);

            url = url.substring(0, suffix_start);


            if (Debug.isSet("showResponse"))
                System.out.println("HTML Form URL: " + url);

            BesAPI.writeHTMLForm(dataSource, url, os);

                    os.flush();


        response.setStatus(HttpServletResponse.SC_OK);


    }


    /**
     * @param request
     * @param response
     */
    public void sendInfo(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception  {


        String dataSource = ReqInfo.getDataSource(request);

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_description");

        if (Debug.isSet("showResponse"))
            System.out.println("doGetINFO For: " + dataSource);

        OutputStream os = new BufferedOutputStream(response.getOutputStream());

            BesAPI.writeINFOPage(dataSource, os);

                    os.flush();


        response.setStatus(HttpServletResponse.SC_OK);

    }


    public void sendASCII(HttpServletRequest request,
                          HttpServletResponse response)
            throws Exception  {


        String dataSource = ReqInfo.getDataSource(request);
        String constraintExpression =  ReqInfo.getConstraintExpression(request);

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_ascii");

        if (Debug.isSet("showResponse"))
            System.out.println("Sending OPeNDAP ASCII Data For: " + dataSource +
                    "    CE: '" + request.getQueryString() + "'");

        OutputStream bOut ;



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
        BesAPI.writeASCII(dataSource, constraintExpression, bOut);
        response.setStatus(HttpServletResponse.SC_OK);
        bOut.flush();



    }


    public void sendHelpPage(HttpServletRequest request,
                             HttpServletResponse response)
            throws Exception  {


        if (Debug.isSet("showResponse"))
            System.out.println("Sending Help Page.");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion());
        response.setHeader("XDAP", getXDAPVersion(request));
        response.setHeader("Content-Description", "dods_help");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");


        PrintWriter pw  = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

            printHelpPage(pw);
            pw.flush();


                    pw.flush();

        response.setStatus(HttpServletResponse.SC_OK);

    }





    public void sendFile(HttpServletRequest req,
                             HttpServletResponse response)
            throws Exception  {


        String name = req.getPathInfo();


        if (Debug.isSet("showResponse")) System.out.print("Sending File: " + name);

        String suffix = ReqInfo.getRequestSuffix(req);

        if(suffix!=null){
            String mType = mimeTypes.getMimeType(suffix);
            if(mType!=null)
                response.setContentType(mType);
            if(Debug.isSet("showResponse")) System.out.print("   MIME type: "+mType+"  ");
        }

        if (Debug.isSet("showResponse")) System.out.println();


            ServletOutputStream sos = response.getOutputStream();
            BesAPI.writeFile(name, sos);
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
        pw.println("one of the following a eight suffixes to a URL: .das, .dds, .dods, .ddx, .info,");
        pw.println(".ver or .help. Using these suffixes, you can ask this server for:");
        pw.println("<dl>");
        pw.println("<dt> das  </dt> <dd> Dataset Attribute Structure (DAS)</dd>");
        pw.println("<dt> dds  </dt> <dd> Dataset Descriptor Structure (DDS)</dd>");
        pw.println("<dt> dods </dt> <dd> DataDDS object (A constrained DDS populated with data)</dd>");
        pw.println("<dt> ddx  </dt> <dd> XML version of the DDS/DAS</dd>");
        pw.println("<dt> info </dt> <dd> info object (attributes, types and other information)</dd>");
        pw.println("<dt> html </dt> <dd> html form for this dataset</dd>");
        pw.println("<dt> ver  </dt> <dd> return the version number of the server</dd>");
        pw.println("<dt> help </dt> <dd> help information (this text)</dd>");
        pw.println("</dl>");
        pw.println("For example, to request the DAS object from the FNOC1 dataset at URI/GSO (a");
        pw.println("experiments dataset) you would appand `.das' to the URL:");
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
