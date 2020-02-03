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
import opendap.dap.User;
import opendap.http.error.BadRequest;
import opendap.http.error.NotFound;
import opendap.http.mediaTypes.TextHtml;
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
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 23, 2010
 * Time: 1:41:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ViewersServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ViewersServlet.class);

    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);

    private static final AtomicInteger REQ_NUMBER = new AtomicInteger(0);
    private static final AtomicBoolean WEB_START_DISABLED = new AtomicBoolean(false);
    private static final BesApi BES_API = new BesApi();

    private static String webStartResourcesDirectory;
    private static String configFilename = "viewers.xml";

    private static String serviceId ="/viewers";

    public static String getServiceId(){
        return serviceId;
    }


    /**
     *
     * @throws ServletException
     */
    @Override
    public void init() throws ServletException {
        // Using this lock prevents thread contention when initializing
        INIT_LOCK.lock();
        try {
            if(IS_INITIALIZED.get())
                return;

            super.init();

            serviceId = this.getServletContext().getContextPath() + "/" + this.getServletName();
            LOG.debug("serviceId: {}", getServiceId());

            String s = getInitParameter("ConfigFileName");
            if (s != null) {
                configFilename = s;
                LOG.info("Servlet configuration included a parameter called 'ConfigFileName' whose value is '{}'",configFilename);
            }

            PersistentConfigurationHandler.installDefaultConfiguration(this, configFilename);


            // We look in the configuration directory first. Because if they are using a localized configuration and have
            // localized the web start options then the JNLP should be available there.
            String webStartDir = ServletUtil.getConfigPath(this) + "WebStart";
            LOG.info("Checking for WebStart resources Directory: {}", webStartDir);
            File f = new File(webStartDir);
            if (f.exists() && f.isDirectory()) {
                webStartResourcesDirectory = webStartDir;
                LOG.info("Found WebStart resources Directory: {}", webStartDir);
            } else {

                LOG.warn("Could not locate WebStart resources Directory: {}", webStartDir);

                // We failed to locate a localized WebStart directory, so we will look in our distribution for it.
                // We find that by asking the servletContext for the real filesystem path of the directory.

                webStartDir = this.getServletContext().getRealPath("WebStart");
                LOG.info("Checking for WebStart resources Directory: {}", webStartDir);
                f = new File(webStartDir);
                if (f.exists() && f.isDirectory()) {
                    webStartResourcesDirectory = webStartDir;
                    LOG.info("Found resources Directory: {}", webStartDir);
                } else {
                    WEB_START_DISABLED.set(true);
                    LOG.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    LOG.error("Could not locate WebStart resources Directory: {}", webStartDir);
                    LOG.error("Java WebStart is DISABLED!");
                    LOG.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

                    webStartResourcesDirectory = null;
                }
            }
            Document configDoc = loadConfig(configFilename);
            buildJwsHandlers(webStartResourcesDirectory, configDoc.getRootElement());
            buildWebServiceHandlers(webStartResourcesDirectory, configDoc.getRootElement());

            IS_INITIALIZED.set(true);
        }
        finally {
            INIT_LOCK.unlock();
        }
    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * ViewersConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private Document loadConfig(String configFileName) throws ServletException {

        String msgBase = "Dataset Viewers Configuration File";
        Document doc;
        configFileName = Scrub.fileName(ServletUtil.getConfigPath(this) + configFileName);
        LOG.debug("Loading {}: {}", msgBase, configFileName);
        try {

            File confFile = new File(configFileName);
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
            String msg = msgBase + " \"" + configFileName + "\" cannot be found.";
            LOG.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = msgBase + " \"" + configFileName + "\" is not readable.";
            LOG.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = msgBase + " \"" + configFileName + "\" cannot be parsed.";
            LOG.error(msg);
            throw new ServletException(msg, e);
        }

        LOG.debug("{} loaded and parsed.",msgBase);
        return doc;

    }



    /**
     * Navigates the config document to instantiate an ordered list of
     * JwsHandler Handlers. Then all of the handlers are initialized by
     * calling their init() methods and passing into them the XML Element
     * that defined them from the config document.
     *
     * @throws javax.servlet.ServletException When things go poorly
     */
    private void  buildJwsHandlers(String resourcesDir, Element webStartConfig)
            throws ServletException {

        String msg;


        LOG.debug("Building JwsHandlers...");

        if(resourcesDir==null){
            LOG.warn("Java WebStart resources directory is 'null'. No Java WebStart service built.");
            return;
        }


        int i = 0;
        for (Object o : webStartConfig.getChildren("JwsHandler")) {
            Element handlerElement = (Element) ((Element) o).clone();
            String className = handlerElement.getAttributeValue("className");
            if(className!=null) {
                JwsHandler jwsHandler;
                try {

                    LOG.debug("Building Handler: {}", className);
                    Class classDefinition = Class.forName(className);
                    jwsHandler = (JwsHandler) classDefinition.newInstance();

                } catch (ClassNotFoundException e) {
                    msg = "Cannot find class: " + className;
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                } catch (InstantiationException e) {
                    msg = "Cannot instantiate class: " + className;
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                } catch (IllegalAccessException e) {
                    msg = "Cannot access class: " + className;
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                } catch (ClassCastException e) {
                    msg = "Cannot cast class: " + className + " to opendap.webstart.JwsHandler";
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                }

                LOG.debug("Initializing Handler: {}", className);
                jwsHandler.init(handlerElement, resourcesDir);

                ServicesRegistry.addService(jwsHandler);
                i++;
            }
            else {
                LOG.error("FAILED to locate the required 'className' attribute in JwsHandler element. SKIPPING.");
            }
        }

        LOG.debug("{} JwsHandlers have been built.", i);

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
        LOG.debug("Building WebServiceHandlers...");
        int i = 0;
        for (Object o : webStartConfig.getChildren("WebServiceHandler")) {
            Element handlerElement = (Element) ((Element) o).clone();
            String className = handlerElement.getAttributeValue("className");
            if(className!=null) {
                WebServiceHandler wsh;
                try {

                    LOG.debug("Building Handler: {}", className);
                    Class classDefinition = Class.forName(className);
                    wsh = (WebServiceHandler) classDefinition.newInstance();

                } catch (ClassNotFoundException e) {
                    msg = "Cannot find class: " + className;
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                } catch (InstantiationException e) {
                    msg = "Cannot instantiate class: " + className;
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                } catch (IllegalAccessException e) {
                    msg = "Cannot access class: " + className;
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                } catch (ClassCastException e) {
                    msg = "Cannot cast class: " + className + " to opendap.webstart.WebServiceHandler";
                    LOG.error(msg);
                    throw new ServletException(msg, e);
                }

                LOG.debug("Initializing Handler: {}", className);
                wsh.init(this, handlerElement);

                ServicesRegistry.addService(wsh);
                i++;
            }
            else {
                LOG.error("FAILED to locate the required 'className' attribute in WebServiceHandler element. SKIPPING.");
            }
        }

        LOG.debug("{} WebServiceHandlers have been built.", i);

    }


    /**
     *
     * @param req
     * @return
     */
    @Override
    public long getLastModified(HttpServletRequest req) {

        long lmt;
        if (WEB_START_DISABLED.get())
            return new Date().getTime();

        String name = Scrub.fileName(getName(req));
        File f = new File(name);
        if (f.exists())
            lmt = f.lastModified();
        else
            lmt = new Date().getTime();

        return lmt;
    }


    /**
     *
     * @param req
     * @return
     */
    private String getName(HttpServletRequest req) {

        String name = req.getPathInfo();
        if (name == null)
            name = "/";

        name = webStartResourcesDirectory + name;
        return name;
    }


    /**
     *
     * @param req
     * @param resp
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        RequestCache.openThreadCache();
        LogUtil.logServerAccessStart(req, LogUtil.HYRAX_ACCESS_LOG_ID, "HTTP-GET", Integer.toString(REQ_NUMBER.incrementAndGet()));
        LOG.debug(ServletUtil.showRequest(req, REQ_NUMBER.get()));

        Request dapRequest = new Request(this,req);
        User user = new User(req);

        String query = Scrub.simpleQueryString(req.getQueryString());
        int requestStatus = HttpServletResponse.SC_OK;
        try {
            String dapService = Scrub.fileName(req.getParameter("dapService"));
            String besDatasetId = Scrub.fileName(req.getParameter("datasetID"));

            if(dapService==null || besDatasetId==null){
                String msg =  "Incorrect parameters sent to '" +  getClass().getName() + "' query: '"+ query +"'";
                LOG.error("{}",msg);
                requestStatus = OPeNDAPException.anyExceptionHandler(new BadRequest(msg),this, resp);
                return;
            }

            URL serviceURL = new URL(ReqInfo.getServiceUrl(req));
            String protocol = serviceURL.getProtocol();
            String host = serviceURL.getHost();
            int port = serviceURL.getPort();
            String serverURL = protocol+"://" + host + ":" + (port==-1 ? "" : port);

            Document ddx = getDDX(user, serverURL, dapService, besDatasetId);

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
                String msg = "No applicationID found in WebStart request.";
                LOG.error("{}", msg);
                requestStatus = OPeNDAPException.anyExceptionHandler(new NotFound(msg),this, resp);
                return;
            }

            if (applicationID.equals("viewers")) {
                DataOutputStream dos = new DataOutputStream(resp.getOutputStream());
                resp.setContentType("text/html");
                sendDatasetPage(getServiceId(),dapRequest.getDocsServiceLocalID(), dapService, besDatasetId, ddx, dos);
                LogUtil.setResponseSize(dos.size());
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
                    DataOutputStream dos = new DataOutputStream(resp.getOutputStream());
                    PrintStream ps = new PrintStream(dos);

                    // Send the jnlp to the client.
                    ps.print(jnlpContent);
                    LogUtil.setResponseSize(dos.size());

                } else {
                    String msg = "Unable to locate a Java WebStart handler to respond to: "+Scrub.simpleString(applicationID)+"?"+query;
                    LOG.error("{}", msg);
                    requestStatus = OPeNDAPException.anyExceptionHandler(new NotFound(msg),this, resp);
                }
            }
        }
        catch (IOException | PPTException | BESError | JDOMException | SaxonApiException | BadConfigurationException e) {
            try {
                requestStatus = OPeNDAPException.anyExceptionHandler(e, this, resp);
            }
            catch (Exception t2) {
                try {
                    String msg = "\n########################################################\n" +
                            "Request proccessing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error log attempt for this request.\n" +
                            "########################################################\n {}";
                    LOG.error(msg,t2);
                }
                catch (Throwable t3) {
                    // It's boned now.. Leave it be.
                }
            }
        }
        finally {
            LogUtil.logServerAccessEnd(requestStatus, LogUtil.HYRAX_ACCESS_LOG_ID);
            RequestCache.closeThreadCache();
            // this.destroy(); // I commented this out because: WTF? Why? - ndp 03/05/2019
        }
    }

    /**
     *
     * @param webStartService
     * @param docsService
     * @param dapService
     * @param datasetID
     * @param ddx
     * @param os
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws SaxonApiException
     * @throws JDOMException
     */
    private void sendDatasetPage(
            String webStartService,
            String docsService,
            String dapService,
            String datasetID,
            Document ddx,
            OutputStream os)
            throws IOException, PPTException, BadConfigurationException, SaxonApiException, JDOMException
    {

        String xsltDoc = ServletUtil.getSystemPath(this, "/xsl/webStartDataset.xsl");

        Transformer transformer = new Transformer(xsltDoc);
        transformer.setParameter("datasetID",datasetID);
        transformer.setParameter("dapService",dapService);
        transformer.setParameter("docsService",docsService);
        transformer.setParameter("webStartService", webStartService);

        String handlers = getWebStartHandlersParam(datasetID, ddx);
        LOG.debug("WebStart Handlers: \n{}",  handlers);
        if(handlers!=null){
            ByteArrayInputStream reader = new ByteArrayInputStream(handlers.getBytes(HyraxStringEncoding.getCharset()));
            XdmNode valueNode = transformer.build(new StreamSource(reader));
            transformer.setParameter("webStartApplications",valueNode);
        }

        handlers = getWebServicesParam(datasetID, ddx);
        LOG.debug("WebServices: \n{}" + handlers);
        if(handlers!=null){
            ByteArrayInputStream reader = new ByteArrayInputStream(handlers.getBytes(HyraxStringEncoding.getCharset()));
            XdmNode valueNode = transformer.build(new StreamSource(reader));
            transformer.setParameter("webServices",valueNode);
        }
        JDOMSource ddxSource = new JDOMSource(ddx);
        transformer.transform(ddxSource, os);
    }


    /**
     *
     * @param datasetId
     * @param ddx
     * @return
     */
    private Vector<JwsHandler> getWebStartApplicationsForDataset(String datasetId, Document ddx){

        Iterator<JwsHandler> e = ServicesRegistry.getJavaWebStartHandlers().values().iterator();
        JwsHandler jwsHandler;
        Vector<JwsHandler> canHandleDataset = new Vector<>();

        while(e.hasNext()){
            jwsHandler = e.next();
            if(jwsHandler.datasetCanBeViewed(datasetId, ddx)){
                canHandleDataset.add(jwsHandler);
            }
        }
        return canHandleDataset;
    }


    /**
     *
     * @param datasetId
     * @param ddx
     * @return
     */
    private TreeMap<String, WebServiceHandler> getWebServicesForDataset(String datasetId, Document ddx){

        Iterator<WebServiceHandler> e = ServicesRegistry.getWebServiceHandlers().values().iterator();
        WebServiceHandler wsHandler;
        TreeMap<String, WebServiceHandler> canHandleDataset = new TreeMap<>();

        while(e.hasNext()){
            wsHandler = e.next();
            if(wsHandler.datasetCanBeViewed(datasetId, ddx)){
                canHandleDataset.put(wsHandler.getName(), wsHandler);
            }
        }
        return canHandleDataset;
    }


    /**
     *
     * @param datsetId
     * @param ddx
     * @return
     */
    private String getWebStartHandlersParam(String datsetId, Document ddx) {

        Vector<JwsHandler> jwsHandlers =  getWebStartApplicationsForDataset(datsetId, ddx);

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


    /**
     *
     * @param datasetId
     * @param ddx
     * @return
     */
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


    /**
     *
     * @param serverURL
     * @param dapService
     * @param datasetID
     * @return
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     * @throws JDOMException
     */
    private Document getDDX(User user, String serverURL, String dapService, String datasetID)
            throws IOException, PPTException, BadConfigurationException, BESError, JDOMException {

        String constraintExpression = "";
        String xmlBase = serverURL+dapService+datasetID;
        Document ddx = new Document();
        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, new TextHtml());
        BES_API.getDDXDocument(
                user,
                datasetID,
                constraintExpression,
                xmlBase,
                ddx);
        return ddx;
    }
}
