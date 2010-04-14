package opendap.wcs.v1_1_2;

import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.OPeNDAPException;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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


    public void init() throws ServletException {
        super.init();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        wcsService = new DispatchHandler();
        form = new FormHandler();
        post = new PostHandler();
        soap = new SoapHandler();

        //Locate config file.
        // parse config File

        Element config  = new Element("config");

        try {
           // wcsService.init(this,config));
           // form.init(this,config);
           // post.init(this,config);
           // soap.init(this,config);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
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
