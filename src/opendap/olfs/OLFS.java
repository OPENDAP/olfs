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

import java.io.*;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import javax.servlet.*;
import javax.servlet.http.*;

import opendap.dap.*;
import opendap.dap.parser.ParseException;
import opendap.util.*;
import opendap.ppt.PPTException;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import thredds.cataloggen.SimpleCatalogBuilder;

/**
 * OLFS is the base servlet class for all OPeNDAP
 * servers. (Well, all OPeNDAP servers running as java servlets)
 * Default handlers for all of the acceptable OPeNDAP client
 * requests are here.
 * <p/>
 * Each of the request handlers appears as an adjunct method to
 * the doGet() method of the base servlet class. In order to
 * reduce the bulk of this file, many of these methods have been
 * in wrapper classes in this package (opendap.servlet).
 * <p/>
 * This is an abstract class because it is left to the individual
 * server development efforts to write the getDDS() and
 * getXDODSServer() methods. The getDDS() method is intended to
 * be where the server specific OPeNDAP server data types are
 * used via their associated class factory.
 * <p/>
 * This code relies on the <code>javax.servlet.ServletConfig</code>
 * interface (in particular the <code>getInitParameter()</code> method)
 * to record detailed configuration information used by
 * the servlet and it's children.
 * <p/>
 * The servlet should be started in the servlet engine with the following
 * initParameters for the tomcat servlet engine:
 * <pre>
 *    &lt;servlet&gt;
 *        &lt;servlet-name&gt;
 *            dts
 *        &lt;/servlet-name&gt;
 *        &lt;servlet-class&gt;
 *            opendap.servers.test.dts
 *        &lt;/servlet-class&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;INFOcache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/info&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;DDScache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/dds&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;DAScache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/das&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *        &lt;init-param&gt;
 *            &lt;param-name&gt;DDXcache&lt;/param-name&gt;
 *            &lt;param-value&gt;/home/Datasets/ddx&lt;/param-value&gt;
 *        &lt;/init-param&gt;
 *    &lt;/servlet&gt;
 * <p/>
 * </pre>
 * <p/>
 * Obviously the actual values of these parameters will depend on your particular
 * file system.
 * <h3>See the file <i>SERVLETS</i> in the top level directory of the
 * software distribution for more detailed information about servlet
 * configuration. </h3>
 * Also, the method <code>processDodsURL()</code> could be overloaded
 * if some kind of special processing of the incoming request is needed
 * to ascertain the OPeNDAP URL information.
 *
 * @author Nathan David Potter
 */


public class OLFS extends ThreddsServlet {



    /**
     * ************************************************************************
     * Debugging
     */
    private boolean track = false;


    /**
     * ************************************************************************
     * Used for thread syncronization.
     *
     * @serial
     */
    private static final Object syncLock = new Object();


    /**
     * ************************************************************************
     * Count "hits" on the server...
     *
     * @serial
     */
    private int HitCounter = 0;



    /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from any of the dods code...
     */


    public void init() throws ServletException {

        super.init();

        String besHost = getInitParameter("BackEndServer");
        if (besHost == null)
            throw new ServletException("Servlet configuration must included BackEndServer\n");

        String besPort = getInitParameter("BackEndServerPort");
        if (besPort == null)
            throw new ServletException("Servlet configuration must included BackEndServerPort\n");


        System.out.print("Configuring BES ... ");
        boolean result;

        synchronized (syncLock) {
            result = BesAPI.configure(besHost, Integer.parseInt(besPort));
        }

        if(result)
            System.out.println("");
        else
            System.out.println("Odd. It was already done!");


        // debuggering
        //String debugOn = getInitParameter("DebugOn");
        //if (debugOn != null) {
        //    System.out.println("** DebugOn **");
        //    StringTokenizer toker = new StringTokenizer(debugOn);
        //    while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
        //}



    }



    /***************************************************************************/



    /**
     * ************************************************************************
     * Sends a OPeNDAP error (type UNKNOWN ERROR) to the client and displays a
     * message on the server console.
     *
     * @param request   The client's <code> HttpServletRequest</code> request object.
     * @param response  The server's <code> HttpServletResponse</code> response object.
     * @param clientMsg Error message <code>String</code> to send to the client.
     * @param serverMsg Error message <code>String</code> to display on the server console.
     */
    public void sendOPeNDAPError(HttpServletRequest request,
                                 HttpServletResponse response,
                                 ReqState rs,
                                 String clientMsg,
                                 String serverMsg)
            throws IOException, ServletException {

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_error");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "none");

        ServletOutputStream Out = response.getOutputStream();

        DODSException de = new DODSException(DODSException.UNKNOWN_ERROR, clientMsg);

        de.print(Out);

        response.setStatus(HttpServletResponse.SC_OK);

        System.out.println(serverMsg);


    }
    /***************************************************************************/


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
    public void doGetDAS(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws IOException, ServletException {

        if (Debug.isSet("showResponse"))
            System.out.println("doGetDAS for dataset: " + rs.getDataSet());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_das");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());
        try {
            BesAPI.getDAS(rs.getDataSet(), rs.getConstraintExpression(), Out);

        } catch (DODSException de) {
            Util.dodsExceptionHandler(de, response);
        } catch (PPTException e) {
            DODSException de = new DODSException(e.getMessage() + e.getStackTrace());
            Util.dodsExceptionHandler(de, response);
        } finally {
            Out.flush();
        }
        response.setStatus(HttpServletResponse.SC_OK);

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the getDDS() method
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
    public void doGetDDS(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws IOException, ServletException {

        System.out.println("Flow in doGetDDS()");


        if (Debug.isSet("showResponse"))
            System.out.println("doGetDDS for dataset: " + rs.getDataSet());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_dds");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        OutputStream Out = new BufferedOutputStream(response.getOutputStream());
        try {
            BesAPI.getDDS(rs.getDataSet(), rs.getConstraintExpression(), Out);

        } catch (DODSException de) {
            Util.dodsExceptionHandler(de, response);
        } catch (PPTException e) {
            DODSException de = new DODSException(e.getMessage() + e.getStackTrace());
            Util.dodsExceptionHandler(de, response);
        } finally {
            Out.flush();
        }

        System.out.println("Flow returned to doGetDDS()");

        response.setStatus(HttpServletResponse.SC_OK);

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's data request. Requires the getDDS()
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
    public void doGetDODS(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
            throws IOException, ServletException {


        if (Debug.isSet("showResponse"))
            System.out.println("doGetOPeNDAP For: " + rs.getDataSet());

        response.setContentType("application/octet-stream");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
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
            BesAPI.getDODS(rs.getDataSet(), rs.getConstraintExpression(), bOut);

        } catch (DODSException de) {
            Util.dodsExceptionHandler(de, response);
        } catch (PPTException e) {
            DODSException de = new DODSException(e.getMessage() + e.getStackTrace());
            Util.dodsExceptionHandler(de, response);
        } finally {
            bOut.flush();
        }
        response.setStatus(HttpServletResponse.SC_OK);

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's directory request.
     * <p/>
     * Returns an html document to the client showing (a possibly pseudo)
     * listing of the datasets available on the server in a directory listing
     * format.
     * <p/>
     * The bulk of this code resides in the class opendap.servlet.S4Dir and
     * documentation may be found there.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see S4Dir
     */
    public void doGetDIR(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws IOException, ServletException {


        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_directory");

        try {
            S4Dir.sendDIR(request, response, rs);
        } catch (ParseException pe) {
            Util.parseExceptionHandler(pe, response);
        } catch (DODSException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

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
    public void doGetVER(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws IOException, ServletException, DODSException {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending Version Document:");

        response.setContentType("text/xml");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_version");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintStream ps = new PrintStream(response.getOutputStream());

        Document vdoc = rs.getVersionDocument();
        if(vdoc == null){
            throw new DODSException("Internal Error: Version Document not initialized.");
        }
        //XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        XMLOutputter xout = new XMLOutputter();
        xout.output(rs.getVersionDocument(),ps);
        ps.flush();

        if (Debug.isSet("showResponse")){
            xout.output(rs.getVersionDocument(),System.out);
            System.out.println("Document Sent.");
            System.out.println("\nMIME Headers:");
            System.out.println("    XDODS-Server: "+rs.getXDODSServer());
            System.out.println("    XOPeNDAP-Server: "+rs.getXOPeNDAPServer());
            System.out.println("    XDAP: "+rs.getXDAP(request));
            System.out.println("\nEnd Response.");
        }

        response.setStatus(HttpServletResponse.SC_OK);

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for the client's help request.
     * <p/>
     * <p> Returns an html page of help info for the server
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void doGetHELP(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
            throws IOException, ServletException {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending Help Page.");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_help");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));


        printHelpPage(pw);
        pw.flush();

        response.setStatus(HttpServletResponse.SC_OK);


    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Sends an html document to the client explaining that they have used a
     * poorly formed URL and then the help page...
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void badURL(HttpServletRequest request,
                       HttpServletResponse response,
                       ReqState rs)
            throws IOException, ServletException {

        if (Debug.isSet("showResponse"))
            System.out.println("Sending Bad URL Page.");

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "BadURL");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));

        printBadURLPage(pw);
        printHelpPage(pw);
        pw.flush();

        response.setStatus(HttpServletResponse.SC_OK);


    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for OPeNDAP ascii data requests. Returns the request data as
     * a comma delimited ascii file. Note that this means that the more complex
     * OPeNDAP structures such as Grids get flattened...
     * <p/>
     * The bulk of this code resides in the class opendap.servlet.dodsASCII and
     * documentation may be found there.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see S4Ascii
     */
    public void doGetASC(HttpServletRequest request,
                         HttpServletResponse response,
                         ReqState rs)
            throws IOException, ServletException, PPTException, DODSException, ParseException {


        if (Debug.isSet("showResponse"))
            System.out.println("doGetASC For: " + rs.getDataSet());

        response.setContentType("text/plain");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_ascii");

        System.out.println("Flow in doGetASC()");
        S4Ascii.sendASCII(request, response, rs);
        response.setStatus(HttpServletResponse.SC_OK);

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Default handler for OPeNDAP info requests. Returns an html document
     * describing the contents of the servers datasets.
     * <p/>
     * The bulk of this code resides in the class opendap.servlet.S4Info and
     * documentation may be found there.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see S4Info
     */
    public void doGetINFO(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
            throws IOException, ServletException, PPTException, ParseException, DODSException {

        S4Info.sendINFO(request,response, rs);
        response.setStatus(HttpServletResponse.SC_OK);




    }
    /**************************************************************************/


    /**
     * ************************************************************************
     * Default handler for OPeNDAP .html requests. Returns the OPeNDAP Web Interface
     * (aka The Interface From Hell) to the client.
     * <p/>
     * The bulk of this code resides in the class opendap.servlet.S4Html and
     * documentation may be found there.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see S4Html
     */
    public void doGetHTML(HttpServletRequest request,
                          HttpServletResponse response,
                          ReqState rs)
            throws IOException, ServletException {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_form");
/*
        try {


            //ds = getDataset(rs);

            // Utilize the getDDS() method to get	a parsed and populated DDS
            // for this server.
            //ServerDDS myDDS = ds.getDDS();
            //DAS das = ds.getDAS();
            //S4Html di = new S4Html();
            //di.sendDataRequestForm(request, response, rs.getDataSet(), myDDS, das);
            //response.setStatus(HttpServletResponse.SC_OK);
        } catch (OPeNDAPException de) {
            Util.dodsExceptionHandler(de, response);
        } catch (IOException pe) {
            Util.IOExceptionHandler(pe, response, rs);
        } catch (ParseException pe) {
            Util.parseExceptionHandler(pe, response);
        }
*/

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
     * @see S4Html
     */
    public void doGetCatalog(HttpServletRequest request,
                             HttpServletResponse response,
                             ReqState rs)
            throws IOException, ServletException, BadConfigurationException, PPTException, JDOMException {


        response.setContentType("text/xml");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_catalog");

        PrintWriter pw = new PrintWriter(response.getOutputStream());




        System.out.println("rootPath:    "+rootPath);
        System.out.println("contentPath: "+contentPath);
        System.out.println("myPath:      "+"/"+rs.getDataSet());



/*
        if(catHandler.processReqForCatalog(this,request,response, contentPath)){
            System.out.println("Processed Catalog Request");
        }
        else {
            System.out.println("Rejected Catalog Request");
        }


        if (Debug.isSet("showResponse")){
            System.out.println("doGetCatalog() - Instantiating S4CrawlableDataset object (a CrawlableDataset)");
        }

*/

        S4CrawlableDataset s4c = new S4CrawlableDataset( "/"+rs.getDataSet(),null);

        if (Debug.isSet("showResponse")){
            System.out.println("doGetCatalog() - Instantiating SimpleCatalogBuilder");
        }


        SimpleCatalogBuilder scb = new SimpleCatalogBuilder(
                    "wingnut",
                    s4c,
                    "THREDDS",
                    "OPENDAP",
                    rs.getRequestURL());

        if (Debug.isSet("showResponse")){
            System.out.println("doGetCatalog() - Generating catalog");
        }


        pw.print(scb.generateCatalogAsString(s4c));






        //printCatalog(request, pw);
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }

    // to be overridden by servers that implement catalogs
    protected void printCatalog(HttpServletRequest request, PrintWriter os) throws IOException {







        os.println("Catalog not available for this server");
    }

    /***************************************************************************/

    /**
     * ************************************************************************
     * Default handler for debug requests;
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response object.
     */
    public void doDebug(HttpServletRequest request,
                        HttpServletResponse response,
                        ReqState rs) throws IOException {


        response.setContentType("text/html");
        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setHeader("Content-Description", "dods_debug");

        PrintStream pw = new PrintStream(response.getOutputStream());
        pw.println("<title>Debugging</title>");
        pw.println("<body><pre>");

        StringTokenizer tz = new StringTokenizer(rs.getConstraintExpression(), "=;");
        while (tz.hasMoreTokens()) {
            String cmd = tz.nextToken();
            pw.println("Cmd= " + cmd);

            if (cmd.equals("help")) {
                pw.println(" help;log;logEnd;logShow");
                pw.println(" showFlags;showInitParameters;showRequest");
                pw.println(" on|off=(flagName)");
                doDebugCmd(cmd, tz, pw); // for subclasses
            } else if (cmd.equals("log")) {
                Log.reset();
                pw.println(" logging started");
            } else if (cmd.equals("logEnd")) {
                Log.close();
                pw.println(" logging ended");
            } else if (cmd.equals("logShow")) {
                pw.println(Log.getContents());
                pw.println("-----done logShow");
            } else if (cmd.equals("on"))
                Debug.set(tz.nextToken(), true);

            else if (cmd.equals("off"))
                Debug.set(tz.nextToken(), false);

            else if (cmd.equals("showFlags")) {
                Iterator iter = Debug.keySet().iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    pw.println("  " + key + " " + Debug.isSet(key));
                }
            } else if (cmd.equals("showInitParameters")) {
                pw.println(rs.toString());
            } else if (cmd.equals("showRequest")) {
                Util.probeRequest(pw, request, getServletContext(), getServletConfig());
            }

            else if (!doDebugCmd(cmd, tz, pw)) { // for subclasses
                pw.println("  unrecognized command");
            }
        }

        pw.println("--------------------------------------");
        pw.println("Logging is " + (Log.isOn() ? "on" : "off"));
        Iterator iter = Debug.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            boolean val = Debug.isSet(key);
            if (val)
                pw.println("  " + key + " " + Debug.isSet(key));
        }

        pw.println("</pre></body>");
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }

    protected boolean doDebugCmd(String cmd, StringTokenizer tz, PrintStream pw) {
        return false;
    }

    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see S4Html
     */
    public void doGetSystemProps(HttpServletRequest request,
                                 HttpServletResponse response,
                                 ReqState rs)
            throws IOException, ServletException {


        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println("<html>");
        pw.println("<title>System Properties</title>");
        pw.println("<hr>");
        pw.println("<body><h2>System Properties</h2>");
        pw.println("<h3>Date: " + new Date() + "</h3>");

        Properties sysp = System.getProperties();
        Enumeration e = sysp.propertyNames();

        pw.println("<ul>");
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();

            String value = System.getProperty(name);

            pw.println("<li>" + name + ": " + value + "</li>");
        }
        pw.println("</ul>");

        pw.println("<h3>Runtime Info:</h3>");

        Runtime rt = Runtime.getRuntime();
        pw.println("JVM Max Memory:   " + (rt.maxMemory() / 1024) / 1000. + " MB (JVM Maximum Allowable Heap)<br>");
        pw.println("JVM Total Memory: " + (rt.totalMemory() / 1024) / 1000. + " MB (JVM Heap size)<br>");
        pw.println("JVM Free Memory:  " + (rt.freeMemory() / 1024) / 1000. + " MB (Unused part of heap)<br>");
        pw.println("JVM Used Memory:  " + ((rt.totalMemory() - rt.freeMemory()) / 1024) / 1000. + " MB (Currently active memory)<br>");

        pw.println("<hr>");
        pw.println("</body>");
        pw.println("</html>");
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }


    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see S4Html
     */
    public void doGetStatus(HttpServletRequest request,
                            HttpServletResponse response,
                            ReqState rs)
            throws IOException, ServletException {


        response.setHeader("XDODS-Server", rs.getXDODSServer());
        response.setHeader("XOPeNDAP-Server", rs.getXOPeNDAPServer());
        response.setHeader("XDAP", rs.getXDAP(request));
        response.setContentType("text/html");
        response.setHeader("Content-Description", "dods_status");

        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println("<title>Server Status</title>");
        pw.println("<body><ul>");
        printStatus(pw);
        pw.println("</ul></body>");
        pw.flush();
        response.setStatus(HttpServletResponse.SC_OK);

    }


    // to be overridden by servers that implement status report
    protected void printStatus(PrintWriter os) throws IOException {
        os.println("<h2>Number of Requests Received = " + HitCounter + "</h2>");
        if (track) {
            int n = prArr.size();
            int pending = 0;
            String preqs = "";
            for (int i = 0; i < n; i++) {
                ReqState rs = (ReqState) prArr.get(i);
                RequestDebug reqD = (RequestDebug) rs.getUserObject();
                if (!reqD.done) {
                    preqs += "<pre>-----------------------\n";
                    preqs += "Request[" + reqD.reqno + "](" + reqD.threadDesc + ") is pending.\n";
                    preqs += rs.toString();
                    preqs += "</pre>";
                    pending++;
                }
            }
            os.println("<h2>" + pending + " Pending Request(s)</h2>");
            os.println(preqs);
        }
    }

    /***************************************************************************/


    /**
     * ************************************************************************
     * <p/>
     * In this (default) implementation of the getServerName() method we just get
     * the name of the servlet and pass it back. If something different is
     * required, override this method when implementing the getDDS() and
     * getXDODSServer() methods.
     * <p/>
     * This is typically used by the getINFO() method to figure out if there is
     * information specific to this server residing in the info directory that
     * needs to be returned to the client as part of the .info response.
     *
     * @return A string containing the name of the servlet class that is running.
     */
    public String getServerName() {

        // Ascertain the name of this server.
        String servletName = this.getClass().getName();

        return (servletName);
    }


    /**
     * ***********************************************************************
     * Handles incoming requests from clients. Parses the request and determines
     * what kind of OPeNDAP response the cleint is requesting. If the request is
     * understood, then the appropriate handler method is called, otherwise
     * an error is returned to the client.
     * <p/>
     * This method is the entry point for <code>OLFS</code>. It uses
     * the methods <code>processDodsURL</code> to extract the OPeNDAP URL
     * information from the incoming client request. This OPeNDAP URL information
     * is cached and made accessible through get and set methods.
     * <p/>
     * After  <code>processDodsURL</code> is called <code>loadIniFile()</code>
     * is called to load configuration information from a .ini file,
     * <p/>
     * If the standard behaviour of the servlet (extracting the OPeNDAP URL
     * information from the client request, or loading the .ini file) then
     * you should overload <code>processDodsURL</code> and <code>loadIniFile()
     * </code>. <b> We don't recommend overloading <code>doGet()</code> beacuse
     * the logic contained there may change in our core and cause your server
     * to behave unpredictably when future releases are installed.</b>
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see opendap.servlet.ReqState
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        // response.setHeader("Last-Modified", (new Date()).toString() );

        boolean isDebug = false;
        ReqState rs = null;
        RequestDebug reqD = null;
        try {
            if (Debug.isSet("probeRequest"))
                Util.probeRequest(System.out, request, getServletContext(), getServletConfig());

            rs = getRequestState(request);
            if (rs != null) {
                String ds = rs.getDataSet();
                String suff = rs.getRequestSuffix();
                isDebug = ((ds != null) && ds.equals("debug") && (suff != null) && suff.equals(""));
            }

            synchronized (syncLock) {

                if (!isDebug) {
                    long reqno = HitCounter++;
                    if (track) {
                        reqD = new RequestDebug(reqno, Thread.currentThread().toString());
                        rs.setUserObject(reqD);
                        if (prArr == null) prArr = new ArrayList(10000);
                        prArr.add((int) reqno, rs);
                    }

                    if (Debug.isSet("showRequest")) {
                        System.out.println("-------------------------------------------");
                        System.out.println("Server: " + getServerName() + "   Request #" + reqno);
                        System.out.println("Client: " + request.getRemoteHost());
                        System.out.println(rs.toString());
                        Log.println("Request dataset: '" + rs.getDataSet() + "' suffix: '" + rs.getRequestSuffix() +
                                "' CE: '" + rs.getConstraintExpression() + "'");
                    }
                }
            } // synch

            if (rs != null) {
                String dataSet = rs.getDataSet();
                String requestSuffix = rs.getRequestSuffix();

                if (dataSet == null) {
                    doGetDIR(request, response, rs);
                } else if (dataSet.equals("/")) {
                    doGetDIR(request, response, rs);
                } else if (dataSet.equals("")) {
                    doGetDIR(request, response, rs);
                } else if (dataSet.equalsIgnoreCase("/version") || dataSet.equalsIgnoreCase("/version/")) {
                    doGetVER(request, response, rs);
                } else if (dataSet.equalsIgnoreCase("/help") || dataSet.equalsIgnoreCase("/help/")) {
                    doGetHELP(request, response, rs);
                } else if (dataSet.equalsIgnoreCase("/" + requestSuffix)) {
                    doGetHELP(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("dds")) {
                    doGetDDS(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("das")) {
                    doGetDAS(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("ddx")) {
                    //doGetDDX(request, response, rs);
                    badURL(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("blob")) {
                    //doGetBLOB(request, response, rs);
                    badURL(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("dods")) {
                    doGetDODS(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("asc") ||
                        requestSuffix.equalsIgnoreCase("ascii")) {

                    doGetASC(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("info")) {
                    doGetINFO(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("html") || requestSuffix.equalsIgnoreCase("htm")) {
                    doGetHTML(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("ver") || requestSuffix.equalsIgnoreCase("version")) {
                    doGetVER(request, response, rs);
                } else if (requestSuffix.equalsIgnoreCase("help")) {
                    doGetHELP(request, response, rs);
                }

                // JC added
                //else if (dataSet.equalsIgnoreCase("catalog") && requestSuffix.equalsIgnoreCase("xml")) {
                else if (dataSet.endsWith("catalog") && requestSuffix.equalsIgnoreCase("xml")) {
                    doGetCatalog(request, response, rs);
                } else if (dataSet.equalsIgnoreCase("status")) {
                    doGetStatus(request, response, rs);
                } else if (dataSet.equalsIgnoreCase("systemproperties")) {
                    doGetSystemProps(request, response, rs);
                } else if (isDebug) {
                    doDebug(request, response, rs);
                } else if (requestSuffix.equals("")) {
                    badURL(request, response, rs);
                } else {
                    badURL(request, response, rs);
                }
            } else {
                badURL(request, response, rs);
            }

            if (reqD != null) reqD.done = true;
        } catch (Throwable e) {
            Util.anyExceptionHandler(e, response, rs);
        }

    }

    private ReqState getRequestState(HttpServletRequest request)
            throws IOException, PPTException, BadConfigurationException, JDOMException {

        ReqState rs;

        try {
            rs = new ReqState(request, getServletConfig(), getServerName());
        } catch (BadURLException bue) {
            rs = null;
        }

        return rs;
    }
    //**************************************************************************

    void showMemUsed(String from) {
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        //long maxMemory = Runtime.getRuntime ().maxMemory ();
        long usedMemory = (totalMemory - freeMemory);

        System.out.println("****showMemUsed " + from);
        System.out.println(" totalMemory " + totalMemory);
        System.out.println(" freeMemory " + freeMemory);
        //System.out.println(" maxMemory "+maxMemory);
        System.out.println(" usedMemory " + usedMemory);
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


    /**
     * ************************************************************************
     * Prints the Bad URL Page page to the passed PrintWriter
     *
     * @param pw PrintWriter stream to which to dump the bad URL page.
     */
    private void printBadURLPage(PrintWriter pw) {

        pw.println("<h3>Error in URL</h3>");
        pw.println("The URL extension did not match any that are known by this");
        pw.println("server. Below is a list of the five extensions that are be recognized by");
        pw.println("all OPeNDAP servers. If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a><p>");

    }
    //**************************************************************************

    // debug
    private ArrayList prArr = null;

    private class RequestDebug {
        long reqno;
        String threadDesc;
        boolean done = false;

        RequestDebug(long reqno, String threadDesc) {
            this.reqno = reqno;
            this.threadDesc = threadDesc;
        }
    }


}





