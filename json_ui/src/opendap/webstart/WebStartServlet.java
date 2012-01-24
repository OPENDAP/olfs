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
package opendap.webstart;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.bes.BadConfigurationException;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.logging.LogUtil;
import opendap.ppt.PPTException;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 23, 2010
 * Time: 1:41:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebStartServlet extends HttpServlet {

    private Logger log;

    private boolean disabled = false;
    private String resourcesDirectory;
    private Document configDoc;

    private BesApi _besApi;


    private ConcurrentHashMap<String, JwsHandler> jwsHandlers = null;

    //private Document configDoc;
    private AtomicInteger reqNumber;


    public void init() throws ServletException {
        super.init();
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        reqNumber = new AtomicInteger(0);

        String dir = ServletUtil.getContentPath(this) + "WebStart";

        File f = new File(dir);

        log.info("Checking for resources Directory: " + dir);
        if (f.exists() && f.isDirectory()) {
            resourcesDirectory = dir;
            log.info("Found resources Directory: " + dir);
        } else {
            log.warn("Could not locate resources Directory: " + dir);
            dir = this.getServletContext().getRealPath("WebStart");
            f = new File(dir);
            log.info("Checking for resources Directory: " + dir);
            if (f.exists() && f.isDirectory()) {
                resourcesDirectory = dir;
                log.info("Found resources Directory: " + dir);
            } else {
                disabled = true;
                log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                log.error("Could not locate resources Directory: " + dir);
                log.error("Java WebStart Disabled!");
                log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");


            }

        }

        if (!disabled) {
            log.info("resourcesDirectory: " + resourcesDirectory);

            configDoc = loadConfig();


            // Build Handler Objects
            jwsHandlers = buildJwsHandlers(resourcesDirectory, configDoc.getRootElement());

            _besApi = new BesApi();
        }


    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * OLFSConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private Document loadConfig() throws ServletException {

        Document doc;

        String filename = getInitParameter("WebStartConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the WebStart configuration!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }

        filename = Scrub.fileName(ServletUtil.getContentPath(this) + filename);

        log.debug("Loading Configuration File: " + filename);


        try {

            File confFile = new File(filename);
            FileInputStream fis = new FileInputStream(confFile);

            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                doc = sb.build(fis);
            }
            finally {
                fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "WebStart configuration file \"" + filename + "\" cannot be found.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "WebStart configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "WebStart configuration file \"" + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
        }

        log.debug("WebStart Configuration loaded and parsed.");
        return doc;

    }

    /**
     * Navigates the config document to instantiate an ordered list of
     * JwsHandler Handlers. Then all of the handlers are initialized by
     * calling their init() methods and passing into them the XML Element
     * that defined them from the config document.
     *
     * @return A VEector of JwsHandlers that have been intialized and are ready to use.
     * @throws ServletException When things go poorly
     */
    private ConcurrentHashMap<String, JwsHandler> buildJwsHandlers(String resourcesDir, Element webStartConfig) throws ServletException {

        String msg;

        ConcurrentHashMap<String, JwsHandler> jwsHandlers = new ConcurrentHashMap<String, JwsHandler>();

        log.debug("Building JwsHandlers");


        for (Object o : webStartConfig.getChildren("JwsHandler")) {
            Element handlerElement = (Element) o;
            String className = handlerElement.getAttribute("className").getValue();
            JwsHandler dh;
            try {

                log.debug("Building Handler: " + className);
                Class classDefinition = Class.forName(className);
                dh = (JwsHandler) classDefinition.newInstance();

            } catch (ClassNotFoundException e) {
                msg = "Cannot find class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (InstantiationException e) {
                msg = "Cannot instantiate class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (IllegalAccessException e) {
                msg = "Cannot access class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (ClassCastException e) {
                msg = "Cannot cast class: " + className + " to opendap.coreServlet.IsoDispatchHandler";
                log.error(msg);
                throw new ServletException(msg, e);
            }

            log.debug("Initializing Handler: " + className);
            dh.init(handlerElement, resourcesDir);


            jwsHandlers.put(dh.getServiceId(), dh);
        }

        log.debug("JwsHandlers have been built.");
        return jwsHandlers;

    }


    public long getLastModified(HttpServletRequest req) {

        long lmt;

        if (disabled)
            return -1;

        String name = Scrub.fileName(getName(req));


        File f = new File(name);

        if (f.exists())
            lmt = f.lastModified();
        else
            lmt = -1;


        //log.debug("getLastModified() - Tomcat requested lastModified for: " + name + " Returning: " + new Date(lmt));

        return lmt;


    }


    private String getName(HttpServletRequest req) {

        String name = req.getPathInfo();

        if (name == null)
            name = "/";

        name = resourcesDirectory + name;
        return name;
    }



    private HashMap<String, String> parseQuery(String query){

        HashMap<String, String> params = new HashMap<String, String>();

        if(query==null){
            log.error("Incorrect parameters sent to '{}' query:{}", getClass().getName(),Scrub.simpleQueryString(query));
            return params;
        }

        String args[] = query.split("&");
        if (args == null) {
            log.error("Incorrect parameters sent to '{}' query:{}", getClass().getName(),Scrub.simpleQueryString(query));
            return params;
        }

        String[] pairs;
        for (String arg : args) {
            pairs = arg.split("=");
            if (pairs != null) {
                if (pairs.length == 2) {
                    params.put(pairs[0], pairs[1]);
                } else {
                    log.error("Parse failed for argument: " + Scrub.simpleQueryString(arg));
                }
            }
        }
        return params;
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        LogUtil.logServerAccessStart(req, "WebStartServletAccess", "GET", Integer.toString(reqNumber.incrementAndGet()));


        log.debug(opendap.coreServlet.Util.showRequest(req, reqNumber.get()));
        //log.debug(opendap.coreServlet.Util.probeRequest(this, req));


        Request dapRequest = new Request(this,req);


        String query = req.getQueryString();
        HashMap<String, String> params = new HashMap<String, String>();

        try {


            if (disabled) {
                log.error("Java WebStart is disabled!");
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;

            }

            params = parseQuery(query);

            String dapService = Scrub.fileName(params.get("dapService"));
            String besDatasetId = Scrub.fileName(params.get("datasetID"));

            if(dapService==null || besDatasetId==null){
                log.error("Incorrect parameters sent to '{}' query:{}", getClass().getName(),Scrub.simpleQueryString(query));
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;

            }


            URL serviceURL = new URL(ReqInfo.getServiceUrl(req));
            String protocol = serviceURL.getProtocol();
            String host = serviceURL.getHost();
            int port = serviceURL.getPort();
            String serverURL = protocol+"://" + host + ":" + (port==-1 ? "" : port);




            Document ddx = getDDX(serverURL, dapService, besDatasetId);

            if(ddx == null){
                log.error("Failed to locate dataset: "+ besDatasetId);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }


            String applicationID = req.getPathInfo();
                 

            // Condition applicationID.
            if (applicationID != null)
            {
                while (applicationID != null && applicationID.startsWith("/")) { // Strip leading slashes
                    applicationID = applicationID.substring(1, applicationID.length());
                }
                if (applicationID.equals(""))
                    applicationID = null;
            }

            if(applicationID == null){
                log.error("No applicationID found in WebStart request.");
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (applicationID.equals("viewers")) {

                resp.setContentType("text/html");
                sendDatasetPage(dapRequest.getWebStartServiceLocalID(),dapRequest.getDocsServiceLocalID(), dapService, besDatasetId, ddx, resp.getOutputStream());
                
            } else {

                String dataAccessURL = serverURL+dapService+besDatasetId;

                // Attempt to locate the application...
                JwsHandler jwsHandler = jwsHandlers.get(applicationID);

                if (jwsHandler != null) {

                    // get the jnlp content
                    String jnlpContent = jwsHandler.getJnlpForDataset(dataAccessURL);

                    //set the mime type for the return document
                    String mType = MimeTypes.getMimeType("jnlp");
                    if (mType != null)
                        resp.setContentType(mType);

                    // Get the sink
                    PrintWriter pw = resp.getWriter();

                    // Send the jnlp to the client.
                    pw.print(jnlpContent);

                } else {
                    log.error("Unable to locate a Java WebStart handler to respond to: '{}?{}'",Scrub.simpleString(applicationID), Scrub.simpleQueryString(query));
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }


            }

        }

        catch (
                Throwable t
                )

        {
            try {
                OPeNDAPException.anyExceptionHandler(t, resp);
            }
            catch (Throwable t2) {
                try {
                    log.error("\n########################################################\n" +
                            "Request proccessing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error log attempt for this request.\n" +
                            "########################################################\n", t2);
                }
                catch (Throwable t3) {
                    // It's boned now.. Leave it be.
                }
            }
        }

        finally

        {
            this.destroy();
        }

    }








    private void sendDatasetPage(String webStartService, String docsService, String dapService, String datasetID, Document ddx, OutputStream os) throws IOException, PPTException, BadConfigurationException, SaxonApiException, JDOMException
    {

        String xsltDoc = ServletUtil.getSystemPath(this, "/xsl/webStartDataset.xsl");

        Transformer transformer = new Transformer(xsltDoc);





        transformer.setParameter("datasetID",datasetID);
        transformer.setParameter("dapService",dapService);
        transformer.setParameter("docsService",docsService);
        transformer.setParameter("webStartService", webStartService);



        String handlers = getHandlersParam(ddx);
        log.debug("Handlers: "+handlers);
        if(handlers!=null){
            ByteArrayInputStream reader = new ByteArrayInputStream(handlers.getBytes());
            XdmNode valueNode = transformer.build(new StreamSource(reader));
            transformer.setParameter("webStartApplications",valueNode);
        }


        JDOMSource ddxSource = new JDOMSource(ddx);
        transformer.transform(ddxSource, os);
    }



    private Vector<JwsHandler> getApplicationsforDataset(Document ddx){
        Enumeration<JwsHandler> e = jwsHandlers.elements();
        JwsHandler jwsHandler;

        Vector<JwsHandler> canHandleDataset = new Vector<JwsHandler>();


        while(e.hasMoreElements()){
            jwsHandler = e.nextElement();
            if(jwsHandler.datasetCanBeViewed(ddx)){
                canHandleDataset.add(jwsHandler);
            }

        }

        return canHandleDataset;

    }

    private String getHandlersParam(Document ddx) {


        String nodeString = null;

        Vector<JwsHandler> jwsHandlers =  getApplicationsforDataset(ddx);

        for(JwsHandler jwsh : jwsHandlers){
            if(nodeString==null)
                nodeString = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><WebStartApplications>";

            nodeString +=  "<wsApp id=\"" +jwsh.getServiceId()+"\" applicationName=\""+jwsh.getApplicationName()+"\" />";
        }

        if(nodeString!=null){
            nodeString +=  "</WebStartApplications>";

        }

        return nodeString;


    }




    public Document getDDX(String serverURL, String dapService, String datasetID) throws IOException, PPTException, BadConfigurationException, SaxonApiException, JDOMException {



        String constraintExpression = "";

        String xdap_accept = "3.2";

        String xmlBase = serverURL+dapService+datasetID;

        Document ddx = new Document();

        if(!_besApi.getDDXDocument(
                datasetID,
                constraintExpression,
                xdap_accept,
                xmlBase,
                ddx)){
            log.error("Unable unable to load DDX dataset: '" + datasetID + "'");
            return null;
        }


        return ddx;



    }






}
