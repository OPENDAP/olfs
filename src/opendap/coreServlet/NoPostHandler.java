package opendap.coreServlet;

import org.jdom.Element;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 7/27/12
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoPostHandler implements DispatchHandler {
    public void init(HttpServlet servlet, Element config) throws Exception {
        // Do nothing
    }

    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return true;  // Always respond
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletOutputStream sos = response.getOutputStream();
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setHeader("Allow","GET");
        sos.println("POST is not supported for this resource on this server.");
    }

    public long getLastModified(HttpServletRequest req) {
        return -1;  // punt...
    }

    public void destroy() {
        // Do nothing
    }

}
