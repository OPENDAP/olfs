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

package opendap.wcs.v1_1_2.http;

import opendap.coreServlet.*;
import opendap.http.error.BadRequest;
import opendap.logging.LogUtil;
import opendap.semantics.wcs.StaticRdfCatalog;
import opendap.wcs.v1_1_2.CatalogWrapper;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 21, 2010
 * Time: 9:21:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class Servlet extends HttpServlet {
    private static final String SERVICE_LOG_ID = "WCS_1.2_ACCESS";
    private static final String SIMPLE_ERROR_LOG = "Caught: {}  Message: {}";

    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final AtomicInteger REQ_NUMBER = new AtomicInteger(0);
    private static final Logger LOG = LoggerFactory.getLogger(Servlet.class);

    private static final HttpGetHandler httpGetService = new HttpGetHandler();
    private static final FormHandler formService = new FormHandler();
    private static final XmlRequestHandler wcsPostService = new XmlRequestHandler();
    private static final SoapHandler wcsSoapService = new SoapHandler();



    public void init() throws ServletException {

        INIT_LOCK.lock();
        try {
            if(IS_INITIALIZED.get())
                return;

            super.init();
            LogUtil.initLogging(this);

            String contextPath = ServletUtil.getContextPath(this);
            LOG.debug("contextPath: {}", contextPath);

            String resourcePath = ServletUtil.getSystemPath(this, "/");
            LOG.debug("resourcePath: {}", resourcePath);

            String contentPath = ServletUtil.getConfigPath(this);
            LOG.debug("contentPath: {}", contentPath);

            String configFilename = this.getInitParameter("ConfigFileName");
            LOG.debug("configFilename: {}", configFilename);

            String semanticPreload = this.getInitParameter("SemanticPreload");
            LOG.debug("semanticPreload: {}", semanticPreload);

            boolean enableUpdateUrl = false;
            String s = this.getInitParameter("EnableUpdateUrl");
            enableUpdateUrl = s != null && s.equalsIgnoreCase("true");
            httpGetService.setEnableUpdateUrl(enableUpdateUrl);
            LOG.debug("enableUpdateUrl: {}", enableUpdateUrl);


            String serviceContentPath = contentPath;
            if (!serviceContentPath.endsWith("/"))
                serviceContentPath += "/";
            LOG.debug("_serviceContentPath: {}", serviceContentPath);

            installInitialContent(resourcePath, serviceContentPath);

            initializeSemanticCatalog(resourcePath, serviceContentPath, configFilename, semanticPreload);

            // Build configuration elements
            Element config = new Element("config");
            Element prefix = new Element("prefix");

            config.addContent(prefix);

            try {
                httpGetService.init(this);
                prefix.setText("/form");
                formService.init(this, config);
                prefix.setText("/post");
                wcsPostService.init(this, config);
                prefix.setText("/soap");
                wcsSoapService.init(this, config);

            } catch (Exception e) {
                throw new ServletException(e);
            }

            IS_INITIALIZED.set(true);
        }
        finally {
            INIT_LOCK.unlock();
        }
    }


    private static final String DEFAULT_WCS_SERVICE_CONFIG_FILENAME = "wcs_service.xml";


    public void initializeSemanticCatalog(String resourcePath, String serviceContentPath,  String configFileName, String semanticPreload) throws ServletException {

        LOG.debug("Initializing semantic WCS catalog engine...");

        URL serviceConfigFile = getServiceConfigurationUrl(serviceContentPath,configFileName);

        StaticRdfCatalog semanticCatalog = new StaticRdfCatalog();

        LOG.info("Using {} WCS catalog implementation.",semanticCatalog.getClass().getName());

        String defaultCatalogCacheDir = serviceContentPath + semanticCatalog.getClass().getSimpleName()+"/";

        try {
            semanticCatalog.init(serviceConfigFile, semanticPreload, resourcePath, defaultCatalogCacheDir);
        } catch (Exception e) {
            LOG.error(SIMPLE_ERROR_LOG,e.getClass().getName(),e.getMessage());
            throw new ServletException(e);
        }

        try {
            CatalogWrapper.init(serviceContentPath, semanticCatalog);
        } catch (Exception e) {
            LOG.error(SIMPLE_ERROR_LOG,e.getClass().getName(),e.getMessage());
            throw new ServletException(e);
        }
        LOG.info("Initialized.");
    }




    private URL getServiceConfigurationUrl(String _serviceContentPath, String configFileName) throws ServletException{
        String msg;
        URL serviceConfigUrl;

        String serviceConfigFilename = _serviceContentPath + DEFAULT_WCS_SERVICE_CONFIG_FILENAME;

        if(configFileName!=null){
            serviceConfigFilename = _serviceContentPath + configFileName;
        }

        serviceConfigFilename = Scrub.fileName(serviceConfigFilename);
        
        LOG.info("Using WCS Service configuration file: '{}'", serviceConfigFilename);

        File configFile = new File(serviceConfigFilename);
        if(!configFile.exists()){
            msg = "Failed to located WCS Service Configuration File: '"+serviceConfigFilename+"'";
            LOG.error(msg);
            throw new ServletException(msg);
        }
        if(!configFile.canRead()){
            String userName = System.getProperty("user.name");
            msg = "The WCS Service Configuration File '"+serviceConfigFilename+"' exists but cannot be read." +
                    " Is there a file permission problem? Is the user '"+userName+"' allowed read access on that file?";
            LOG.error(msg);
            throw new ServletException(msg);
        }

        try{
            serviceConfigUrl = new URL("file://" + serviceConfigFilename);
        } catch (MalformedURLException e) {
            LOG.error(SIMPLE_ERROR_LOG,e.getClass().getName(),e.getMessage());
            throw new ServletException(e);
        }

        return  serviceConfigUrl;


    }






    private void installInitialContent(String resourcePath, String serviceContentPath) throws ServletException{

        String msg;
        File f = new File(serviceContentPath);

        if(f.exists()){
            if(!f.isDirectory()) {
                msg = "The service content path "+serviceContentPath+
                        "exists, but it is not directory and cannot be used.";
                LOG.error(msg);
                throw new ServletException(msg);
            }
            if(!f.canWrite()) {
                msg = "The service content path "+serviceContentPath+
                        "exists, but the directory is not writable.";
                LOG.error(msg);
                throw new ServletException(msg);
            }

        }
        else {
            LOG.info("Creating WCS Service content directory: {}", serviceContentPath);
            f.mkdirs();
        }

        File semaphore = new File(serviceContentPath+"wcs_service.xml");
        if(!semaphore.exists()){
            String initialContentDir = resourcePath + "initialContent/";
            LOG.info("Attempting to copy initial content for WCS from {} to {}",initialContentDir,serviceContentPath);
            try {
                PersistentConfigurationHandler.copyDirTree(initialContentDir, serviceContentPath);
                if(semaphore.createNewFile()){
                    LOG.warn("Semapohore file {} already exists! Configuration may not have installed correctly.",semaphore.getAbsolutePath());
                }
            } catch (IOException e) {
                LOG.error("Caught: {} Message: ",e.getClass().getName(),e.getMessage());
                throw new ServletException(e);
            }
            LOG.info("WCS Service default configuration and initial content installed.");
        }
    }


    /**
     *
     * @param req
     * @param resp
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        int request_status = HttpServletResponse.SC_OK;
        try {
            LogUtil.logServerAccessStart(req, SERVICE_LOG_ID, "HTTP-GET", Integer.toString(REQ_NUMBER.incrementAndGet()));
            httpGetService.handleRequest(req, resp);
        }
        catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this, resp);
            }
            catch(Throwable t2) {
            	try {
            		LOG.error("\n########################################################\n" +
                                "Request proccessing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error logging attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
        finally {
            LogUtil.logServerAccessEnd(request_status, SERVICE_LOG_ID);
            RequestCache.closeThreadCache();
        }
    }


    /**
     *
     * @param req
     * @param resp
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp){
        int request_status = HttpServletResponse.SC_OK;
        try {
            LogUtil.logServerAccessStart(req, SERVICE_LOG_ID, "HTTP-POST", Integer.toString(REQ_NUMBER.incrementAndGet()));

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
                LOG.error("doPost() - {}",msg);
                request_status = OPeNDAPException.anyExceptionHandler(new BadRequest(msg), this,  resp);
            }

        }
        catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this,  resp);
            }
            catch(Throwable t2) {
            	try {
            		LOG.error("\n########################################################\n" +
                                "Request proccessing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error logging attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
        finally {
            LogUtil.logServerAccessEnd(request_status, SERVICE_LOG_ID);
            RequestCache.closeThreadCache();

        }
    }

    /**
     *
     * @param req
     * @return
     */
    @Override
    protected long getLastModified(HttpServletRequest req) {

        RequestCache.openThreadCache();
        LogUtil.logServerAccessStart(req, SERVICE_LOG_ID, "LastModified", Long.toString( REQ_NUMBER.incrementAndGet()));

        try {
            return new Date().getTime();
        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, SERVICE_LOG_ID);
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
