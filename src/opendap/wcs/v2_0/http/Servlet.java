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
import opendap.io.HyraxStringEncoding;
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
    private HttpGetHandler httpGetService;
    private FormHandler formService;
    private XmlRequestHandler wcsPostService ;
    private SoapHandler wcsSoapService;
    private AtomicInteger reqNumber;

    //private Document configDoc;
    private String _defaultWcsServiceConfigFilename;

    private boolean _initialized;

    public Servlet(){
        httpGetService = null;
        formService = null;
        wcsPostService = null;
        wcsSoapService = null;
        reqNumber = new AtomicInteger(0);
        _defaultWcsServiceConfigFilename = "wcs_service.xml";
    }



    public void init() throws ServletException {
        if(_initialized)
            return;

        super.init();

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
            String msg = "Unable to read BES configuration file: '"+besConfigFilename+"' Caught " + e.getClass().getSimpleName() +
                    " message: " + e.getMessage();
            _log.error(msg);
            return;
        }
        if(config==null) {
            _log.error("Failed to get BES configuration document: '{}'", besConfigFilename);
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
                    t.printStackTrace(new PrintStream(baos,true,HyraxStringEncoding.getCharset().name()));
                    msg.append("StackTrace: ").append(baos.toString(HyraxStringEncoding.getCharset().name())).append("\n");

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