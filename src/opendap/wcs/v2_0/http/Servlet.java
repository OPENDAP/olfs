/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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

package opendap.wcs.v2_0.http;

import opendap.PathBuilder;
import opendap.bes.BESManager;
import opendap.coreServlet.*;
import opendap.http.error.BadRequest;
import opendap.logging.LogUtil;
import opendap.wcs.v2_0.WcsServiceManager;
import opendap.wcs.v2_0.WcsException;
import opendap.xml.Util;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 21, 2010
 * Time: 9:21:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class Servlet extends HttpServlet {

    private Logger _log;
    private HttpGetHandler httpGetService = null;

    private FormHandler formService = null;
    private XmlRequestHandler wcsPostService = null;
    private SoapHandler wcsSoapService = null;
    private AtomicInteger reqNumber;

    //private Document configDoc;

    private boolean _initialized;

    private String _defaultWcsServiceConfigFilename = "wcs_service.xml";


    public void init() throws ServletException {
        if(_initialized)
            return;

        super.init();
        reqNumber = new AtomicInteger(0);

        LogUtil.initLogging(this);
        _log = org.slf4j.LoggerFactory.getLogger(getClass());

        String contextPath = ServletUtil.getContextPath(this);
        _log.info("contextPath: "+contextPath);

        String servletName = getServletName();

        contextPath = PathBuilder.pathConcat(contextPath,servletName);

        // _log.info(ServletUtil.probeRequest(this,null));

        String resourcePath = ServletUtil.getSystemPath(this, "/");
        _log.info("resourcePath: "+resourcePath);

        String configPath = ServletUtil.getConfigPath(this);
        _log.info("configPath: "+configPath);

        boolean enableUpdateUrl;
        String s = this.getInitParameter("EnableUpdateUrl");
        enableUpdateUrl = s!=null && s.equalsIgnoreCase("true");
        _log.debug("enableUpdateUrl: "+enableUpdateUrl);

        String serviceConfigPath = configPath;
        if(!serviceConfigPath.endsWith("/"))
            serviceConfigPath += "/";
        _log.debug("serviceConfigPath: {}",serviceConfigPath);


        String wcsConfigFileName = getInitParameter("WCSConfigFileName");
        if (wcsConfigFileName == null) {
            wcsConfigFileName = _defaultWcsServiceConfigFilename;
            String msg = "Servlet configuration (typically in the web.xml file) must include a file name for " +
                    "the WCS service configuration! This on is MISSING. Using default configuration file name.\n";
            _log.warn(msg);
        }
        _log.info("configFilename: "+wcsConfigFileName);
        PersistentConfigurationHandler.installDefaultConfiguration(this, wcsConfigFileName);

        WcsServiceManager.init(contextPath, serviceConfigPath, wcsConfigFileName);

        // Build Handler Objects
        httpGetService = new HttpGetHandler(enableUpdateUrl);
        formService    = new FormHandler();
        wcsPostService = new XmlRequestHandler();
        wcsSoapService = new SoapHandler();

        // Build configuration elements
        Element config  = new Element("config");
        Element prefix  = new Element("prefix");

//        System.out.println(ServletUtil.probeServlet(this));

        // ServletContext sc = this.getServletContext();
        // prefix.setText(sc.getContextPath());
        config.addContent(prefix);

        try {
            httpGetService.init(this);
            prefix.setText("/form");
            formService.init(this,config);
            prefix.setText("/post");
            wcsPostService.init(this,config);
            prefix.setText("/soap");
            wcsSoapService.init(this,config);

        } catch (Exception e) {
            throw new ServletException(e);
        }


        // Now we need to configure a BES

        initBesManager("olfs.xml");



        _initialized = true;
    }
    private void initBesManager(String configFileName) throws ServletException {

        String besConfigFilename = Scrub.fileName(ServletUtil.getConfigPath(this) + configFileName);

        Element config;
        try {
            config = Util.getDocumentRoot(besConfigFilename);
        } catch (IOException | JDOMException e) {
            String msg = "Unable to read BES configuration file. Caught " + e.getClass().getSimpleName() +
                    " message: " + e.getMessage();
            _log.error(msg);
            return;
        }
        Element besManagerElement = config.getChild("BESManager");

        if (besManagerElement == null) {
            String msg = "Invalid BES configuration. Missing required 'BESManager' element. BES was not initialized!";
            _log.error(msg);
            return;
        }

        BESManager besManager  = new BESManager();
        if(!BESManager.isInitialized()) {
            try {
                besManager.init(besManagerElement);
            } catch (Exception e) {
                String msg = "BESManager initialization was an abject failure. BES was not initialized! " +
                        "Caught "+e.getClass().getName()+ " message: "+e.getMessage();
                _log.error(msg);
            }
        }

    }





   /*

    public void initializeCatalog(String serviceContextPath, String serviceConfigPath,  String configFileName) throws ServletException {

        if (_initialized) return;


        WcsServiceManager.init(serviceContextPath, serviceConfigPath,configFileName);

        _initialized = true;
        _log.info("Initialized. ");


        String msg;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        URL serviceConfigFile = getServiceConfigurationUrl(serviceConfigPath,configFileName);
        SAXBuilder sb = new SAXBuilder();
        Document configDoc = null;

        try {
            configDoc = sb.build(serviceConfigFile);
            if(configDoc==null) {
                msg = "The WCS 2.0 servlet is unable to locate the configuration document '"+serviceConfigFile+"'";
                _log.error(msg);
                throw new ServletException(msg);
            }

        } catch (JDOMException e) {
            throw new ServletException(e);
        } catch (IOException e) {
            throw new ServletException(e);
        }

        Element configFileRoot = configDoc.getRootElement();
        if(configFileRoot==null) {
            msg = "The WCS 2.0 servlet is unable to locate the root element of the configuration document '"+serviceConfigFile+"'";
            _log.error(msg);
            throw new ServletException(msg);
        }







        Element catalogConfig = configFileRoot.getChild("WcsCatalog");
        if(catalogConfig==null) {
            msg = "The WCS 2.0 servlet is unable to locate the configuration Directory <WcsCatalog> element " +
                    "in the configuration file: " + serviceConfigFile + "'";
            _log.error(msg);
            throw new ServletException(msg);
        }

        String className =  catalogConfig.getAttributeValue("className");
        if(className==null) {
            msg = "The WCS 2.0 servlet is unable to locate the 'className' attribute of the <WcsCatalog> element"+
                    "in the configuration file: " + serviceConfigFile + "'";
            _log.error(msg);
            throw new ServletException(msg);
        }

        WcsCatalog wcsCatalog = null;
        try {
            _log.debug("Building WcsCatalog implementation: " + className);
            Class classDefinition = Class.forName(className);
            wcsCatalog = (WcsCatalog) classDefinition.newInstance();
        }
        catch ( Exception e){
            msg = "Failed to build WcsCatalog implementation: "+className+
                    " Caught an exception of type "+e.getClass().getName() + " Message: "+ e.getMessage();
            _log.error(msg);
            throw new ServletException(msg, e);
        }

        try {
            wcsCatalog.init(catalogConfig, serviceConfigPath, serviceContextPath);
        } catch (Exception e) {
            _log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
            throw new ServletException(e);
        }

        try {
            CatalogWrapper.init(serviceConfigPath, wcsCatalog);
        } catch (Exception e) {
            _log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
            throw new ServletException(e);
        }


    }

           */

    /*


    public void initializeSemanticCatalog(String resourcePath, String serviceContentPath,  String configFileName, String semanticPreload) throws ServletException {

        if (_initialized) return;

        URL serviceConfigFile = getServiceConfigurationUrl(serviceContentPath,configFileName);

        StaticRdfCatalog semanticCatalog = new StaticRdfCatalog();

        _log.info("Using "+semanticCatalog.getClass().getName()+" WCS catalog implementation.");


        _log.debug("Initializing semantic WCS catalog engine...");


        String defaultCatalogCacheDir = serviceContentPath + semanticCatalog.getClass().getSimpleName()+"/";


        try {
            semanticCatalog.init(serviceConfigFile, semanticPreload, resourcePath, defaultCatalogCacheDir);
        } catch (Exception e) {
            _log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
            throw new ServletException(e);
        }



        try {
            CatalogWrapper.init(serviceContentPath, semanticCatalog);
        } catch (Exception e) {
            _log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
            throw new ServletException(e);
        }

        _initialized = true;
        _log.info("Initialized. ");

    }



    private URL getServiceConfigurationUrl(String _serviceContentPath, String configFileName) throws ServletException{
        String msg;
        URL serviceConfigUrl;

        String serviceConfigFilename = _serviceContentPath + _defaultWcsServiceConfigFilename;

        if(configFileName!=null){
            serviceConfigFilename = _serviceContentPath + configFileName;
        }

        serviceConfigFilename = Scrub.fileName(serviceConfigFilename);

        _log.info("getServiceConfigurationUrl() - Using WCS Service configuration file: "+serviceConfigFilename);

        File configFile = new File(serviceConfigFilename);
        if(!configFile.exists()){
            msg = "Failed to located WCS Service Configuration File '"+serviceConfigFilename+"'";
            _log.error(msg);
            throw new ServletException(msg);
        }
        if(!configFile.canRead()){
            String userName = System.getProperty("user.name");
            msg = "The WCS Service Configuration File '"+serviceConfigFilename+"' exists but cannot be read." +
                    " Is there a file permission problem? Is the user '"+userName+"' allowed read access on that file?";
            _log.error(msg);
            throw new ServletException(msg);
        }

        try{
            serviceConfigUrl = new URL("file://" + serviceConfigFilename);
        } catch (Exception e) {
            _log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
            throw new ServletException(e);
        }

        return  serviceConfigUrl;


    }



                */


     /*
    private void installDefaultConfiguration(String serviceResourcePath, String serviceConfigDir, String semaphoreFileName) throws ServletException{

        String msg;
        File f = new File(serviceConfigDir);

        if(f.exists()){
            if(!f.isDirectory()) {
                msg = "The service content path "+serviceConfigDir+
                        "exists, but it is not directory and cannot be used.";
                _log.error(msg);
                throw new ServletException(msg);
            }
            if(!f.canWrite()) {
                msg = "The service content path "+serviceConfigDir+
                        "exists, but the directory is not writable.";
                _log.error(msg);
                throw new ServletException(msg);
            }

        }
        else {
            _log.info("Creating WCS Service content directory: "+serviceConfigDir);
            f.mkdirs();
        }

        File semaphore = new File(serviceConfigDir+semaphoreFileName);
        if(!semaphore.exists()){
            String confDir = serviceResourcePath + "WEB-INF/conf/";
            _log.info("Attempting to copy default configuration for WCS from "+confDir+" to "+serviceConfigDir);
            try {
                PersistentConfigurationHandler.copyDirTree(confDir, serviceConfigDir);
                semaphore.createNewFile();
            } catch (IOException e) {
                _log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
                throw new ServletException(e);
            }
            _log.info("WCS Service default configuration and initial content installed.");
        }



    }



   */



    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        int request_status = HttpServletResponse.SC_OK;
        try {
            LogUtil.logServerAccessStart(req, "WCS_2.0_ACCESS", "HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));
            httpGetService.handleRequest(req, resp);
        }
        catch (Throwable t) {
            try {
                WcsException myBadThang;
                if(t instanceof WcsException){
                    myBadThang = (WcsException) t;
                }
                else {

                    StringBuilder msg = new StringBuilder();
                    msg.append("doGet() - The bad things have happened in WCS-2.0. Caught ")
                            .append(t.getClass().getName()).append("\n");
                    msg.append("Message: ").append(t.getMessage()).append("\n");

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    t.printStackTrace(new PrintStream(baos));
                    msg.append("StackTrace: ").append(baos.toString()).append("\n");

                    myBadThang = new WcsException(msg.toString(),WcsException.NO_APPLICABLE_CODE);
                    myBadThang.setHttpStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);


                }
                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                Document errDoc = new Document( myBadThang.getExceptionElement());

                if(!resp.isCommitted()){
                    _log.error("doGet() - Encountered ERROR after response committed. Msg: {}",myBadThang.getMessage());
                    resp.setStatus(myBadThang.getHttpStatusCode());
                    xmlo.output(errDoc,resp.getOutputStream());
                }
                else {
                    _log.error("doGet() - Encountered ERROR after response committed. Msg: {}",myBadThang.getMessage());
                    resp.sendError(myBadThang.getHttpStatusCode(),myBadThang.getMessage());
                }


            }
            catch(Throwable t2) {
            	try {
            		_log.error("\n########################################################\n" +
                                "Request processing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error _log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
        finally {
            LogUtil.logServerAccessEnd(request_status, "WCS_2.0_ACCESS");
            RequestCache.closeThreadCache();

        }
    }


    public void doPost(HttpServletRequest req, HttpServletResponse resp){
        int request_status = HttpServletResponse.SC_OK;
        try {
            LogUtil.logServerAccessStart(req, "WCS_2.0_ACCESS", "HTTP-POST", Integer.toString(reqNumber.incrementAndGet()));

            if(wcsPostService.requestCanBeHandled(req)){
                wcsPostService.handleRequest(req,resp);
            }
            else if(wcsSoapService.requestCanBeHandled(req)){
                wcsSoapService.handleRequest(req,resp);
            }
            else if(formService.requestCanBeHandled(req)){
                formService.handleRequest(req,resp);
            }
            else {
                String msg = "The request does not resolve to a WCS service operation that this server supports.";
                _log.error("doPost() - {}",msg);
                throw new BadRequest(msg);
            }

        }
        catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this,  resp);
            }
            catch(Throwable t2) {
            	try {
            		_log.error("\n########################################################\n" +
                                "Request processing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error _log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
        finally {
            LogUtil.logServerAccessEnd(request_status, "WCS_2.0_ACCESS");
            RequestCache.closeThreadCache();

        }
    }
    protected long getLastModified(HttpServletRequest req) {

        RequestCache.openThreadCache();

        long reqno = reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "WCS_2.0_ACCESS", "LastModified", Long.toString(reqno));


        try {
            return -1;

        } catch (Exception e) {
            return -1;
        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, "WCS_2.0_ACCESS");

        }


    }



    public void destroy() {

        LogUtil.logServerShutdown("destroy()");

        httpGetService.destroy();
        formService.destroy();
        wcsPostService.destroy();
        wcsSoapService.destroy();


        super.destroy();
    }



}
