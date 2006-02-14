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

import opendap.util.Debug;
import opendap.util.Log;
import opendap.ppt.PPTException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.io.*;

import org.jdom.JDOMException;
import org.jdom.Document;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 1, 2006
 * Time: 10:48:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestServletResponse extends ThreddsServlet {


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
     * Server Version Document, Cached at servlet startup
     *
     * @serial
     */
    private Document _serverVersionDocument = null;



    /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from any of the dods code...
     */


    public void init() throws ServletException {

        super.init();

        // debuggering
        //String debugOn = getInitParameter("DebugOn");
        //if (debugOn != null) {
        //    System.out.println("** DebugOn **");
        //    StringTokenizer toker = new StringTokenizer(debugOn);
        //    while (toker.hasMoreTokens()) Debug.set(toker.nextToken(), true);
       // }

        configBES();

        try {
            cacheServerVersionDocument();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Could not get version document from BES.",e);
        }

    }

    private void configBES() throws ServletException {
        String besHost = getInitParameter("BackEndServerHost");
        if (besHost == null)
            throw new ServletException("Servlet configuration must included BackEndServer\n");

        String besPort = getInitParameter("BackEndServerPort");
        if (besPort == null)
            throw new ServletException("Servlet configuration must included BackEndServerPort\n");


        System.out.print("Configuring BES ... ");

        if(BesAPI.configure(besHost, Integer.parseInt(besPort)))
            System.out.println("");
        else
            System.out.println("Odd. It was already done!");

    }

    /**
     *
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
        pw.flush();

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


        System.out.println("rootPath:    " + rootPath);
        System.out.println("contentPath: " + contentPath);
        System.out.println("dataset:     " + rs.getDataset());




        // Why Doesn't this work?
        if (catHandler.processReqForCatalog(this, request, response, contentPath)) {
            System.out.println("Processed Catalog Request");
        } else {
            System.out.println("Rejected Catalog Request");

        }





/*
        String path = rs.getDataset();
        path = path.endsWith("/catalog") ? path.substring(0, path.length() - 8) : path;

        path = S4CrawlableDataset.besPath2ThreddsPath(path);

        S4CrawlableDataset s4cd = new S4CrawlableDataset(path,null);

        if(s4cd.isCollection()){

            if (Debug.isSet("showResponse")){
                System.out.println("doGetCatalog() - Instantiating SimpleCatalogBuilder");
            }


            SimpleCatalogBuilder scb = new SimpleCatalogBuilder(
                        "",                                  // CollectionID, which for us needs to be empty.
                        S4CrawlableDataset.getRootDataset(), // Root dataset of this collection
                        "OPeNDAP-Server4",                   // Service Name
                        "OPeNDAP",                           // Service Type Name
                        request.getRequestURI().substring(0,request.getRequestURI().indexOf(request.getPathInfo())+1)); // Base URL for this service

            if (Debug.isSet("showResponse")){
                System.out.println("doGetCatalog() - Generating catalog");
            }


            pw.print(scb.generateCatalogAsString(s4cd));


        }
        else {
            response.setContentType("text/html");
            String msg = "ERROR: THREDDS catalogs may only be requested for collections, not for individual data sets.";
            throw new DODSException(msg);
        }


    */


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
                String ds = rs.getDataset();
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
                        Log.println("Request dataset: '" + rs.getDataset() + "' suffix: '" + rs.getRequestSuffix() +
                                "' CE: '" + rs.getConstraintExpression() + "'");
                    }
                }
            } // synch

            if (rs != null) {
                String dataSet = rs.getDataset();
                String requestSuffix = rs.getRequestSuffix();


                if (dataSet.endsWith("catalog") && requestSuffix.equalsIgnoreCase("xml")) {
                    doGetCatalog(request, response, rs);
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

    private ReqState getRequestState(HttpServletRequest request) {

        ReqState rs;

        try {
            rs = new ReqState(request, getServletConfig(), getServerName(),_serverVersionDocument);
        } catch (BadURLException bue) {
            rs = null;
        }

        return rs;
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



