package opendap.ncml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Oct 7, 2010
 * Time: 3:57:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class NcmlServlet extends HttpServlet {

    Logger log;

    public void init(){
        log = LoggerFactory.getLogger(this.getClass());
    }




    public void doPost(HttpServletRequest request, HttpServletResponse response){


        try {

            request.getInputStream();

        } catch (Exception e) {
            log.error("Caught "+e.getClass().getName()+" Message: "+e.getMessage());
        }
    }



}
