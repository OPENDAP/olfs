package opendap.wcs.v1_1_2.http;

import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.logging.LogUtil;
import opendap.semantics.wcs.StaticRdfCatalog;
import opendap.wcs.v1_1_2.*;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 21, 2010
 * Time: 9:21:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class Servlet extends HttpServlet {

    private Logger log;
    private HttpGetHandler httpGetService = null;

    private FormHandler formService = null;
    private PostHandler wcsPostService = null;
    private SoapHandler wcsSoapService = null;

    //private Document configDoc;


    public void init() throws ServletException {
        super.init();
        LogUtil.initLogging(this);
        log = org.slf4j.LoggerFactory.getLogger(getClass());

        String contextPath = ServletUtil.getContextPath(this);
        log.debug("contextPath: "+contextPath);

        String resourcePath = ServletUtil.getSystemPath(this, "/");
        log.debug("resourcePath: "+resourcePath);

        String contentPath = ServletUtil.getContentPath(this);
        log.debug("contentPath: "+contentPath);

        String configFilename = this.getInitParameter("ConfigFileName");
        log.debug("configFilename: "+configFilename);

        String semanticPreload = this.getInitParameter("SemanticPreload");
        log.debug("semanticPreload: "+semanticPreload);

        boolean enableUpdateUrl = false;
        String s = this.getInitParameter("EnableUpdateUrl");
        enableUpdateUrl = s!=null && s.equalsIgnoreCase("true");
        log.debug("enableUpdateUrl: "+enableUpdateUrl);



        String serviceContentPath = contentPath;
        if(!serviceContentPath.endsWith("/"))
            serviceContentPath += "/";
        log.debug("_serviceContentPath: "+serviceContentPath);

        installInitialContent(resourcePath, serviceContentPath);

        initializeSemanticCatalog(resourcePath, serviceContentPath, configFilename, semanticPreload);



        // Build Handler Objects
        httpGetService = new HttpGetHandler(enableUpdateUrl);
        formService = new FormHandler();
        wcsPostService = new PostHandler();
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
    }




    private boolean _initialized;

    private String _defaultWcsServiceConfigFilename = "wcs_service.xml";






    public void initializeSemanticCatalog(String resourcePath, String serviceContentPath,  String configFileName, String semanticPreload) throws ServletException {

        if (_initialized) return;

        URL serviceConfigFile = getServiceConfigurationUrl(serviceContentPath,configFileName);

        StaticRdfCatalog semanticCatalog = new StaticRdfCatalog();

        log.info("Using "+semanticCatalog.getClass().getName()+" WCS catalog implementation.");


        log.debug("Initializing semantic WCS catalog engine...");


        String defaultCatalogCacheDir = serviceContentPath + semanticCatalog.getClass().getSimpleName()+"/";


        try {
            semanticCatalog.init(serviceConfigFile, semanticPreload, resourcePath, defaultCatalogCacheDir);
        } catch (Exception e) {
            log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
            throw new ServletException(e);
        }



        try {
            CatalogWrapper.init(serviceContentPath, semanticCatalog);
        } catch (Exception e) {
            log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
            throw new ServletException(e);
        }

        _initialized = true;
        log.info("Initialized. ");

    }




    private URL getServiceConfigurationUrl(String _serviceContentPath, String configFileName) throws ServletException{
        String msg;
        URL serviceConfigUrl;

        String serviceConfigFilename = _serviceContentPath + _defaultWcsServiceConfigFilename;

        if(configFileName!=null){
            serviceConfigFilename = _serviceContentPath + configFileName;
        }

        serviceConfigFilename = Scrub.fileName(serviceConfigFilename);
        
        log.info("Using WCS Service configuration file: "+serviceConfigFilename);

        File configFile = new File(serviceConfigFilename);
        if(!configFile.exists()){
            msg = "Failed to located WCS Service Configuration File '"+serviceConfigFilename+"'";
            log.error(msg);
            throw new ServletException(msg);
        }
        if(!configFile.canRead()){
            String userName = System.getProperty("user.name");
            msg = "The WCS Service Configuration File '"+serviceConfigFilename+"' exists but cannot be read." +
                    " Is there a file permission problem? Is the user '"+userName+"' allowed read access on that file?";
            log.error(msg);
            throw new ServletException(msg);
        }

        try{
            serviceConfigUrl = new URL("file://" + serviceConfigFilename);
        } catch (Exception e) {
            log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
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
                log.error(msg);
                throw new ServletException(msg);
            }
            if(!f.canWrite()) {
                msg = "The service content path "+serviceContentPath+
                        "exists, but the directory is not writable.";
                log.error(msg);
                throw new ServletException(msg);
            }

        }
        else {
            log.info("Creating WCS Service content directory: "+serviceContentPath);
            f.mkdirs();
        }

        File semaphore = new File(serviceContentPath+".INIT");
        if(!semaphore.exists()){
            String initialContentDir = resourcePath + "initialContent/";
            log.info("Attempting to copy initial content for WCS from "+initialContentDir+" to "+serviceContentPath);
            try {
                opendap.coreServlet.PersistentContentHandler.copyDirTree(initialContentDir,serviceContentPath);
                semaphore.createNewFile();
            } catch (IOException e) {
                log.error("Caught "+e.getClass().getName()+"  Msg: "+e.getMessage());
                throw new ServletException(e);
            }
            log.info("WCS Service default configuration and initial content installed.");
        }



    }







    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            httpGetService.handleRequest(req, resp);
        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, resp);
            }
            catch(Throwable t2) {
            	try {
            		log.error("\n########################################################\n" +
                                "Request proccessing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
        }
    }


    public void doPost(HttpServletRequest req, HttpServletResponse resp){
        try {

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
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, resp);
            }
            catch(Throwable t2) {
            	try {
            		log.error("\n########################################################\n" +
                                "Request proccessing failed.\n" +
                                "Normal Exception handling failed.\n" +
                                "This is the last error log attempt for this request.\n" +
                                "########################################################\n", t2);
            	}
            	catch(Throwable t3){
                    // It's boned now.. Leave it be.
            	}
            }
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
