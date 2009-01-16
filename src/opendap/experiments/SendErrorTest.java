package opendap.experiments;

import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jan 12, 2009
 * Time: 11:19:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class SendErrorTest extends HttpServlet {



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {
        try {

            String name = "wcs<imbedd>assbite";

            javax.xml.namespace.QName n2 = new javax.xml.namespace.QName(name);
            System.out.println("QNAME: "+n2);



            System.out.println("\n*********************************************\n" +
                    "Calling HttpServletResponse.sendError(404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("\nHttpServletResponse.sendError(404) returned.\n" +
                    "*********************************************\n");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


}
