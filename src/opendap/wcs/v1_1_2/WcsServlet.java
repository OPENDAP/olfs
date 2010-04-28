package opendap.wcs.v1_1_2;

import opendap.coreServlet.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Apr 6, 2010
 * Time: 8:43:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class WcsServlet extends HttpServlet {


    private Logger log;
    private DispatchHandler wcsService = null;

    private FormHandler form = null;
    private PostHandler post = null;
    private SoapHandler soap = null;

    private Document configDoc;


    public void init() throws ServletException {
        super.init();
        PerfLog.initLogging(this);
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        
        wcsService = new DispatchHandler();
        form = new FormHandler();
        post = new PostHandler();
        soap = new SoapHandler();

        //Locate config file.
        // parse config File

        Element config  = new Element("config");
        Element prefix = new Element("prefix");




        System.out.println(ServletUtil.probeServlet(this));

        ServletContext sc = this.getServletContext();

        
        prefix.setText(sc.getContextPath());
        config.addContent(prefix);

        try {
            prefix.setText("/");
            wcsService.init(this,config);
            prefix.setText("/form");
            form.init(this,config);
            prefix.setText("/post");
            post.init(this,config);
            prefix.setText("/soap");
            soap.init(this,config);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }




    /**
     * Loads the configuration file specified in the servlet parameter
     * OLFSConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private void loadConfig() throws ServletException {

        String filename = getInitParameter("OLFSConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the OLFS configuration!\n";
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
                configDoc = sb.build(fis);
            }
            finally {
            	fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "OLFS configuration file \"" + filename + "\" cannot be found.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "OLFS configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "OLFS configuration file \"" + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
        }

        log.debug("Configuration loaded and parsed.");

    }


    public void doGet(HttpServletRequest req, HttpServletResponse resp){
        try {
            wcsService.handleRequest(req, resp);
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
        finally{
            this.destroy();
        }
    }



}
