package opendap.gateway.dapResponders;

import opendap.gateway.HttpResponder;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/31/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class NetcdfFileOut extends HttpResponder {
    private Logger log;


    public NetcdfFileOut(String sysPath) {
        super(sysPath, ".*\\.nc");
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    }

    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        sendSomeStuff(response);


    }

    private void sendSomeStuff(HttpServletResponse response) throws Exception {

        response.setContentType("text/html");

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream()));
        XMLOutputter xmlo = new XMLOutputter();


        pw.println("<h2>NetCDF FileOut Response</h2>");
        pw.println("<p>This request is being handled by: "+getClass().getName()+"</p>");

        pw.flush();


    }




}
