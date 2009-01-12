package opendap.experiments;

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
            response.sendError(404);
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }



}
