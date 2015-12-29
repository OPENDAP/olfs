/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.viewers;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.io.HyraxStringEncoding;
import opendap.logging.LogUtil;
import opendap.ppt.PPTException;
import opendap.services.ServicesRegistry;
import opendap.services.WebServiceHandler;
import opendap.webstart.JwsHandler;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 23, 2010
 * Time: 1:41:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ViewersServlet extends HttpServlet {

    private Logger _log;

    private boolean _webStartDisabled = false;
    private String _webStartResourcesDirectory;
    private Document _configDoc;

    private BesApi _besApi;

    private static String _serviceId ="/viewers";

    public static String getServiceId(){
        return _serviceId;
    }




    //private Document configDoc;
    private AtomicInteger reqNumber;


    public void init() throws ServletException {
        super.init();

        PersistentConfigurationHandler.installDefaultConfiguration(this);


        _log = org.slf4j.LoggerFactory.getLogger(getClass());

        _serviceId = this.getServletContext().getContextPath() + "/" + this.getServletName();

        _log.debug(getClass().getSimpleName() + " - serviceId: " + getServiceId());


        // log.debug(ServletUtil.probeServlet(this));

        reqNumber = new AtomicInteger(0);

        String webStartDir = ServletUtil.getConfigPath(this) + "WebStart";

        File f = new File(webStartDir);

        _log.info("Checking for WebStart resources Directory: " + webStartDir);
        if (f.exists() && f.isDirectory()) {
            _webStartResourcesDirectory = webStartDir;
            _log.info("Found WebStart resources Directory: " + webStartDir);
        } else {
            _log.warn("Could not locate WebStart resources Directory: " + webStartDir);
            webStartDir = this.getServletContext().getRealPath("WebStart");
            f = new File(webStartDir);
            _log.info("Checking for WebStart resources Directory: " + webStartDir);
            if (f.exists() && f.isDirectory()) {
                _webStartResourcesDirectory = webStartDir;
                _log.info("Found resources Directory: " + webStartDir);
            } else {
                _webStartDisabled = true;
                _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                _log.error("Could not locate WebStart resources Directory: " + webStartDir);
                _log.error("Java WebStart Disabled!");
                _log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

                _webStartResourcesDirectory = null;
            }

        }

        _configDoc = loadConfig();

        buildJwsHandlers(_webStartResourcesDirectory,_configDoc.getRootElement());
        buildWebServiceHandlers(_webStartResourcesDirectory, _configDoc.getRootElement());

        _besApi = new BesApi();



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

        String filename = getInitParameter("ViewersConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the Dataset Viewers configuration!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }

        filename = Scrub.fileName(ServletUtil.getConfigPath(this) + filename);

        _log.debug("Loading Dataset Viewers Configuration File: " + filename);


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
            String msg = "Dataset Viewers Configuration File \"" + filename + "\" cannot be found.";
            _log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "Dataset Viewers Configuration File \"" + filename + "\" is not readable.";
            _log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "Dataset Viewers Configuration File \"" + filename + "\" cannot be parsed.";
            _log.error(msg);
            throw new ServletException(msg, e);
        }

        _log.debug("Dataset Viewers Configuration loaded and parsed.");
        return doc;

    }



    /**
     * Navigates the config document to instantiate an ordered list of
     * JwsHandler Handlers. Then all of the handlers are initialized by
     * calling their init() methods and passing into them the XML Element
     * that defined them from the config document.
     *
     * @return A VEector of JwsHandlers that have been intialized and are ready to use.
     * @throws javax.servlet.ServletException When things go poorly
     */
    private void  buildJwsHandlers(String resourcesDir, Element webStartConfig) throws ServletException {

        String msg;


        _log.debug("Building JwsHandlers...");

        if(resourcesDir==null){
            _log.warn("Java WebStart resources directory is 'null'. No Java WebStart service built.");
            return;
        }


        int i = 0;
        for (Object o : webStartConfig.getChildren("JwsHandler")) {
            Element handlerElement = (Element) ((Element) o).clone();
            String className = handlerElement.getAttributeValue("className");
            if(className!=null) {
                JwsHandler jwsHandler;
                try {

                    _log.debug("Building Handler: " + className);
                    Class classDefinition = Class.forName(className);
                    jwsHandler = (JwsHandler) classDefinition.newInstance();

                } catch (ClassNotFoundException e) {
                    msg = "Cannot find class: " + className;
                    _log.error(msg);
                    throw new ServletException(msg, e);
                } catch (InstantiationException e) {
                    msg = "Cannot instantiate class: " + className;
                    _log.error(msg);
                    throw new ServletException(msg, e);
                } catch (IllegalAccessException e) {
                    msg = "Cannot access class: " + className;
                    _log.error(msg);
                    throw new ServletException(msg, e);
                } catch (ClassCastException e) {
                    msg = "Cannot cast class: " + className + " to opendap.webstart.JwsHandler";
                    _log.error(msg);
                    throw new ServletException(msg, e);
                }

                _log.debug("Initializing Handler: " + className);
                jwsHandler.init(handlerElement, resourcesDir);

                ServicesRegistry.addService(jwsHandler);
                i++;
            }
            else {
                _log.error("buildJwsHandlers() - FAILED to locate the required 'className' attribute in JwsHandler element. SKIPPING.");
            }
        }

        _log.debug(i + " JwsHandlers have been built.");

    }


    /**
     * Navigates the config document to instantiate an ordered list of
     * JwsHandler Handlers. Then all of the handlers are initialized by
     * calling their init() methods and passing into them the XML Element
     * that defined them from the config document.
     *
     * @return A VEector of JwsHandlers that have been intialized and are ready to use.
     * @throws javax.servlet.ServletException When things go poorly
     */
    private void buildWebServiceHandlers(String resourcesDir, Element webStartConfig) throws ServletException {

        String msg;


        _log.debug("Building WebServiceHandlers...");


        int i = 0;
        for (Object o : webStartConfig.getChildren("WebServiceHandler")) {
            Element handlerElement = (Element) ((Element) o).clone();
            String className = handlerElement.getAttributeValue("className");
            if(className!=null) {
                WebServiceHandler wsh;
                try {

                    _log.debug("Building Handler: " + className);
                    Class classDefinition = Class.forName(className);
                    wsh = (WebServiceHandler) classDefinition.newInstance();

                } catch (ClassNotFoundException e) {
                    msg = "Cannot find class: " + className;
                    _log.error(msg);
                    throw new ServletException(msg, e);
                } catch (InstantiationException e) {
                    msg = "Cannot instantiate class: " + className;
                    _log.error(msg);
                    throw new ServletException(msg, e);
                } catch (IllegalAccessException e) {
                    msg = "Cannot access class: " + className;
                    _log.error(msg);
                    throw new ServletException(msg, e);
                } catch (ClassCastException e) {
                    msg = "Cannot cast class: " + className + " to opendap.webstart.WebServiceHandler";
                    _log.error(msg);
                    throw new ServletException(msg, e);
                }

                _log.debug("Initializing Handler: " + className);
                wsh.init(this, handlerElement);

                ServicesRegistry.addService(wsh);
                i++;
            }
            else {
                _log.error("buildWebServiceHandlers() - FAILED to locate the required 'className' attribute in WebServiceHandler element. SKIPPING.");
            }
        }

        _log.debug(i + " WebServiceHandlers have been built.");

    }





    public long getLastModified(HttpServletRequest req) {

        long lmt;

        if (_webStartDisabled)
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

        name = _webStartResourcesDirectory + name;
        return name;
    }



    private HashMap<String, String> parseQuery(String query){

        HashMap<String, String> params = new HashMap<String, String>();

        if(query==null){
            _log.error("Incorrect parameters sent to '{}' query:{}", getClass().getName(), Scrub.simpleQueryString(query));
            return params;
        }

        String args[] = query.split("&");
        if (args == null) {
            _log.error("Incorrect parameters sent to '{}' query:{}", getClass().getName(), Scrub.simpleQueryString(query));
            return params;
        }

        String[] pairs;
        for (String arg : args) {
            pairs = arg.split("=");
            if (pairs != null) {
                if (pairs.length == 2) {
                    params.put(pairs[0], pairs[1]);
                } else {
                    _log.error("Parse failed for argument: " + Scrub.simpleQueryString(arg));
                }
            }
        }
        return params;
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        RequestCache.openThreadCache();

        LogUtil.logServerAccessStart(req, "WebStartServletAccess", "GET", Integer.toString(reqNumber.incrementAndGet()));


        _log.debug(ServletUtil.showRequest(req, reqNumber.get()));
        //log.debug(opendap.coreServlet.AwsUtil.probeRequest(this, req));


        Request dapRequest = new Request(this,req);


        String query = req.getQueryString();
        HashMap<String, String> params;

        try {



            //if (_webStartDisabled) {
            //    log.error("Java WebStart is disabled!");
            //    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            //    return;

            //}

            params = parseQuery(query);

            String dapService = Scrub.fileName(params.get("dapService"));
            String besDatasetId = Scrub.fileName(params.get("datasetID"));

            if(dapService==null || besDatasetId==null){
                _log.error("Incorrect parameters sent to '{}' query:{}", getClass().getName(), Scrub.simpleQueryString(query));
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
                _log.error("Failed to locate dataset: " + besDatasetId);
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }


            String applicationID = req.getPathInfo();
                 

            // Condition applicationID.
            if (applicationID != null)
            {
                while (applicationID.startsWith("/")) { // Strip leading slashes
                    applicationID = applicationID.substring(1, applicationID.length());
                }
                if (applicationID.equals(""))
                    applicationID = null;
            }

            if(applicationID == null){
                _log.error("No applicationID found in WebStart request.");
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (applicationID.equals("viewers")) {

                resp.setContentType("text/html");
                sendDatasetPage(getServiceId(),dapRequest.getDocsServiceLocalID(), dapService, besDatasetId, ddx, resp.getOutputStream());
                
            } else {

                String dataAccessURL = serverURL+dapService+besDatasetId;

                // Attempt to locate the application...
                JwsHandler jwsHandler = ServicesRegistry.getJwsHandlerById(applicationID);

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
                    _log.error("Unable to locate a Java WebStart handler to respond to: '{}?{}'", Scrub.simpleString(applicationID), Scrub.simpleQueryString(query));
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }


            }

        }
        catch (Throwable t){
            try {
                OPeNDAPException.anyExceptionHandler(t, this, req.getContextPath(), resp);
            }
            catch (Throwable t2) {
                try {
                    _log.error("\n########################################################\n" +
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
        finally {
            RequestCache.closeThreadCache();
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



        String handlers = getWebStartHandlersParam(ddx);
        _log.debug("WebStart Handlers: " + handlers);
        if(handlers!=null){
            ByteArrayInputStream reader = new ByteArrayInputStream(handlers.getBytes(HyraxStringEncoding.getCharset()));
            XdmNode valueNode = transformer.build(new StreamSource(reader));
            transformer.setParameter("webStartApplications",valueNode);
        }

        handlers = getWebServicesParam(datasetID, ddx);
        _log.debug("WebServices: \n" + handlers);
        if(handlers!=null){
            ByteArrayInputStream reader = new ByteArrayInputStream(handlers.getBytes(HyraxStringEncoding.getCharset()));
            XdmNode valueNode = transformer.build(new StreamSource(reader));
            transformer.setParameter("webServices",valueNode);
        }


        JDOMSource ddxSource = new JDOMSource(ddx);
        transformer.transform(ddxSource, os);
    }



    private Vector<JwsHandler> getWebStartApplicationsForDataset(Document ddx){
        Iterator<JwsHandler> e = ServicesRegistry.getJavaWebStartHandlers().values().iterator();
        JwsHandler jwsHandler;

        Vector<JwsHandler> canHandleDataset = new Vector<JwsHandler>();

        while(e.hasNext()){
            jwsHandler = e.next();
            if(jwsHandler.datasetCanBeViewed(ddx)){
                canHandleDataset.add(jwsHandler);
            }

        }

        return canHandleDataset;

    }

    private TreeMap<String, WebServiceHandler> getWebServicesForDataset(String datasetId, Document ddx){

        Iterator<WebServiceHandler> e = ServicesRegistry.getWebServiceHandlers().values().iterator();
        WebServiceHandler wsHandler;

        TreeMap<String, WebServiceHandler> canHandleDataset = new TreeMap<>();


        while(e.hasNext()){
            wsHandler = e.next();
            if(wsHandler.datasetCanBeViewed(ddx)){
                canHandleDataset.put(wsHandler.getName(), wsHandler);
            }

        }

        return canHandleDataset;

    }

    private String getWebStartHandlersParam(Document ddx) {


        Vector<JwsHandler> jwsHandlers =  getWebStartApplicationsForDataset(ddx);

        if(jwsHandlers.isEmpty())
            return null;

        Element webStartAppsElement = new Element("WebStartApplications");
        Element wsElement;

        for(JwsHandler jwsh : jwsHandlers){

            wsElement = new Element("webStartApp");
            wsElement.setAttribute("id",jwsh.getServiceId());
            wsElement.setAttribute("applicationName",jwsh.getName());

            webStartAppsElement.addContent(wsElement);
        }

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        return xmlo.outputString(new Document(webStartAppsElement));


    }

    private String getWebServicesParam(String datasetId, Document ddx) {

        TreeMap<String, WebServiceHandler> webServicesForDataset =  getWebServicesForDataset(datasetId, ddx);

        if(webServicesForDataset.isEmpty())
            return null;

        Element webServicesElement = new Element("WebServices");
        Element wsElement;


        for(WebServiceHandler wsh: webServicesForDataset.values()){
            wsElement = new Element("webService");
            wsElement.setAttribute("id",wsh.getServiceId());
            wsElement.setAttribute("applicationName",wsh.getName());
            wsElement.setAttribute("serviceUrl",wsh.getServiceLink(datasetId));

            webServicesElement.addContent(wsElement);

        }

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        return xmlo.outputString(new Document(webServicesElement));


    }




    public Document getDDX(String serverURL, String dapService, String datasetID) throws IOException, PPTException, BadConfigurationException, BESError, SaxonApiException, JDOMException {



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
            _log.error("Unable unable to load DDX dataset: '" + datasetID + "'");
            return null;
        }


        return ddx;



    }






}
