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

package opendap.coreServlet;

import opendap.util.Debug;
import opendap.util.Log;
import opendap.dap.DODSException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedOutputStream;
import java.util.StringTokenizer;

/**
 * This servlet provides the dispatching for all OPeNDAP requests.
 *
 * <p>This server will respond to both HTTP GET and POST requests. The GET dispatching is
 * done in this class, and the POST dispatching (which is in fact the SOAP inrterface)
 * is done in <code>SOAPRequestDispatcher</code></p>
 *
 *
 * <p>This server is built designed so that the actual handling of the dispatchs is done
 * through code that is identified at run time through the web.xml configuration of the
 * servlet. In particular the HTTP GET request are handled by a class the implements the
 * OpendapHttpDispatchHandler interface. The SOAP requests (via HTTP POST) are handled by a
 * class the implements the OpendapSOAPDispatchHandler interface.<p>
 *
 * <p>The web.xml file used to configure this servlet must contain servlet parameters identifying
 * an implmentation clas for both these interfaces.</p>
 *
 *
 *
 * User: ndp
 * Date: Mar 17, 2006
 * Time: 2:23:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class DispatchServlet extends HttpServlet {


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


    private OpendapHttpDispatchHandler odh = null;
    private OpendapSoapDispatchHandler sdh = null;


    /**
     * ************************************************************************
     * Intitializes the servlet.
     *
     * @throws ServletException
     */
    public void init() throws ServletException {

        super.init();

        initDebug();

        // Identify and make the HTTP GET request handler
        String className = getInitParameter("OpendapHttpDispatchHandlerImplementation");

        if(className == null)
            throw new ServletException("Missing servlet parameter \"OpendapHttpDispatchHandlerImplementation\"." +
                    "A class that implements the opendap.coreServlet.OpendapHttpDispatchHandler interface must" +
                    "be identified in this (missing) servlet parameter.");

        System.out.println("\n\nOpendapHttpDispatchHandlerImplementation: " + className);

        try {
            Class classDefinition = Class.forName(className);
            odh = (OpendapHttpDispatchHandler) classDefinition.newInstance();
        } catch (InstantiationException e) {
            throw new ServletException("Cannot instantiate class: " + className,e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Cannot access class: " + className,e);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Cannot find class: " + className, e);
        }


        odh.init(this);


        // Identify and make the SOAP (via HTTP POST) request handler

        className = getInitParameter("OpendapSoapDispatchHandlerImplementation");

        if(className == null)
            throw new ServletException("Missing servlet parameter \"OpendapSoapDispatchHandlerImplementation\"." +
                    "A class that implements the opendap.coreServlet.OpendapSoapDispatchHandler interface must" +
                    "be identified in this (missing) servlet parameter.");

        System.out.println("\n\nOpendapSoapDispatchHandlerImplementation: " + className);

        try {
            Class classDefinition = Class.forName(className);
            sdh = (OpendapSoapDispatchHandler) classDefinition.newInstance();
        } catch (InstantiationException e) {
            throw new ServletException("Cannot instantiate class: " + className,e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Cannot access class: " + className,e);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Cannot find class: " + className, e);
        }


        sdh.init(this);



    }
    /***************************************************************************/


    /**
     * ************************************************************************
     *
     * Process the DebugOn initParameter and turn on the requested debugging
     * states in the Debug object.
     *
     */
    private void initDebug(){
        // Turn on debugging.
        String debugOn = getInitParameter("DebugOn");
        if (debugOn != null) {
            System.out.println("** DebugOn **");
            StringTokenizer toker = new StringTokenizer(debugOn);
            while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
        }

    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * <p/>
     * In this (default) implementation of the getServerName() method we just get
     * the name of the servlet and pass it back. If something different is
     * required, override this method when implementing the writeDDS() and
     * getXDODSServerVersion() methods.
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
     * the methods <code>processOpendapURL</code> to extract the OPeNDAP URL
     * information from the incoming client request. This OPeNDAP URL information
     * is cached and made accessible through get and set methods.
     * <p/>
     * After  <code>processOpendapURL</code> is called <code>loadIniFile()</code>
     * is called to load configuration information from a .ini file,
     * <p/>
     * If the standard behaviour of the servlet (extracting the OPeNDAP URL
     * information from the client request, or loading the .ini file) then
     * you should overload <code>processOpendapURL</code> and <code>loadIniFile()
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
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        // response.setHeader("Last-Modified", (new Date()).toString() );



        boolean isDebug = false;
        ReqState rs = null;

        try {
            if (Debug.isSet("probeRequest"))
                Util.probeRequest(System.out, this, request, getServletContext(), getServletConfig());

            rs = getRequestState(request);
            if (rs != null) {
                String ds = rs.getDataset();
                String suff = rs.getRequestSuffix();
                isDebug = ((ds != null) && ds.equals("debug") && (suff != null) && suff.equals(""));
            }

            synchronized (syncLock) {
                long reqno = HitCounter++;

                if (!isDebug) {
                    if (Debug.isSet("showRequest")) {
                        System.out.println("-------------------------------------------");
                        System.out.println("Server: " + getServerName() + "   Request #" + reqno);
                        System.out.println("Client: " + request.getRemoteHost());
                        System.out.println(rs.toString());
                        Log.println("Request dataset: '" + rs.getDataset() + "' suffix: '" + rs.getRequestSuffix() +
                                "' CE: '" + rs.getConstraintExpression() + "'");
                    }
                }

            } // synch

            if (rs != null) {
                String dataSet = rs.getDataset();
                String requestSuffix = rs.getRequestSuffix();



                if ( // Version Response?
                        dataSet.equalsIgnoreCase("/version") ||
                                dataSet.equalsIgnoreCase("/version/") ||
                                requestSuffix.equalsIgnoreCase("ver") ||
                                requestSuffix.equalsIgnoreCase("version")
                        ) {
                    odh.sendVersion(request, response);

                } else if ( // Directory response?
                        dataSet == null ||
                                dataSet.equals("/") ||
                                dataSet.equals("") ||
                                dataSet.endsWith("/") ||
                                requestSuffix.equals("")
                        ) {
                    odh.sendDir(request, response, rs);


                }else if ( // Help Response?
                        dataSet.equalsIgnoreCase("/help") ||
                                dataSet.equalsIgnoreCase("/help/") ||
                                dataSet.equalsIgnoreCase("/" + requestSuffix) ||
                                requestSuffix.equalsIgnoreCase("help")
                        ) {
                    odh.sendHelpPage(request, response, rs);

                } else if ( // DDS Response?
                        requestSuffix.equalsIgnoreCase("dds")
                        ) {
                    odh.sendDDS(request, response, rs);

                } else if ( // DAS Response?
                        requestSuffix.equalsIgnoreCase("das")
                        ) {
                    odh.sendDAS(request, response, rs);

                } else if (  // DDX Response?
                        requestSuffix.equalsIgnoreCase("ddx")
                        ) {
                    odh.sendDDX(request, response, rs);

                } else if ( // Blob Response?
                        requestSuffix.equalsIgnoreCase("blob")
                        ) {
                    //doGetBLOB(request, response, rs);
                    badURL(request, response, rs);

                } else if ( // DataDDS (aka .dods) Response?
                        requestSuffix.equalsIgnoreCase("dods")
                        ) {
                    odh.sendDODS(request, response, rs);

                } else if (  // ASCII Data Response.
                        requestSuffix.equalsIgnoreCase("asc") ||
                                requestSuffix.equalsIgnoreCase("ascii")
                        ) {
                    odh.sendASCII(request, response, rs);

                } else if (  // Info Response?
                        requestSuffix.equalsIgnoreCase("info")
                        ) {
                    odh.sendInfo(request, response, rs);

                } else if (  //HTML Request Form (aka The Interface From Hell) Response?
                        requestSuffix.equalsIgnoreCase("html") ||
                                requestSuffix.equalsIgnoreCase("htm")
                        ) {
                    odh.sendHTMLRequestForm(request, response, rs);


                } else if (  // THREDDS Catalog Response?
                        dataSet.endsWith("catalog") &&
                                requestSuffix.equalsIgnoreCase("xml")
                        ) {
                    odh.sendCatalog(request, response, rs);


                } else if (  // Status Response?
                        dataSet.equalsIgnoreCase("status")
                        ) {
                    doGetStatus(request, response);

                } else if ( // System Properties Response?
                        dataSet.equalsIgnoreCase("systemproperties")
                        ) {
                    Util.sendSystemProperties(request, response, odh);

                } else if (isDebug) {
                    DebugHandler.doDebug(this, request, response, odh, rs);


                } else if (requestSuffix.equals("")) {
                    badURL(request, response, rs);
                } else {
                    badURL(request, response, rs);
                }
            } else {
                badURL(request, response, rs);
            }

        } catch (Throwable e) {
            anyExceptionHandler(e, response, rs);
        }

    }

    private ReqState getRequestState(HttpServletRequest request) {

        ReqState rs;

        try {
            rs = new ReqState(request, getServletConfig(), getServerName());
        } catch (BadURLException bue) {
            rs = null;
        }

        return rs;
    }
    //**************************************************************************


    /**
     * ************************************************************************
     * Default handler for OPeNDAP status requests; not publically available,
     * used only for debugging
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void doGetStatus(HttpServletRequest request,
                            HttpServletResponse response)
            throws IOException, ServletException {


        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
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
        response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
        response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
        response.setHeader("XDAP", odh.getXDAPVersion(request));
        response.setHeader("Content-Description", "BadURL");
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));


        pw.println("<h3>Error in URL</h3>");
        pw.println("The URL extension did not match any that are known by this");
        pw.println("server. Below is a list of the five extensions that are be recognized by");
        pw.println("all OPeNDAP servers. If you think that the server is broken (that the URL you");
        pw.println("submitted should have worked), then please contact the");
        pw.println("OPeNDAP user support coordinator at: ");
        pw.println("<a href=\"mailto:support@unidata.ucar.edu\">support@unidata.ucar.edu</a><p>");

        pw.flush();

        response.setStatus(HttpServletResponse.SC_OK);


    }
    /***************************************************************************/


    /**
     * ************************************************************************
     * Sends an error to the client.
     *
     * @param e        The exception that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public static void anyExceptionHandler(Throwable e, HttpServletResponse response, ReqState rs) {

        try {
            System.out.println("DODServlet ERROR (anyExceptionHandler): " + e);
            System.out.println(rs);

            e.printStackTrace();
            Log.printThrowable(e);

            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());
            response.setHeader("Content-Description", "dods_error");

            // This should probably be set to "plain" but this works, the
            // C++ slients don't barf as they would if I sent "plain" AND
            // the C++ don't expect compressed data if I do this...
            response.setHeader("Content-Encoding", "");

            // Strip any double quotes out of the parser error message.
            // These get stuck in auto-magically by the javacc generated parser
            // code and they break our error parser (bummer!)
            String msg = e.getMessage();
            if (msg != null)
                msg = msg.replace('\"', '\'');

            DODSException de2 = new DODSException(DODSException.UNDEFINED_ERROR, msg);
            de2.print(eOut);

        } catch (IOException ioe) {
            System.out.println("Cannot respond to client! IO Error: " + ioe.getMessage());
        }


    }
    /***************************************************************************/


    /**
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {


        SOAPRequestDispatcher.doPost(request,response, odh, sdh);
    }





}
