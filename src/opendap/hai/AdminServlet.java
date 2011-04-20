package opendap.hai;

import opendap.coreServlet.ServletUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Nov 12, 2010
 * Time: 2:35:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdminServlet extends HttpServlet {

    private Logger log;


    public void init(){
        log = LoggerFactory.getLogger(getClass());

    }



    public void doGet(HttpServletRequest req, HttpServletResponse rsp) throws ServletException {


        try {
            ServletOutputStream sos = rsp.getOutputStream();

            String usersFile = ServletUtil.getSystemPath(this, "../../conf/tomcat-users.xml");



            String tomcatUsers = StringEscapeUtils.escapeHtml(readFileAsString(usersFile));



            rsp.setContentType("text/html");
            sos.println("<html>");
            sos.println("<body>");
            sos.println("<hr/>");
            sos.println("<ul><li>");
            sos.println("req.getProtcol(): "+ req.getProtocol());
            sos.println("</li><li>");
            sos.println("req.getScheme(): "+ req.getScheme());
            sos.println("</li><li>");
            sos.println("req.isSecure(): "+ req.isSecure());
            sos.println("</li><li>");
            sos.println("req.isRequestedSessionIdFromCookie(): "+ req.isRequestedSessionIdFromCookie());
            sos.println("</li><li>");
            sos.println("req.isRequestedSessionIdFromURL(): "+ req.isRequestedSessionIdFromURL());
            sos.println("</li><li>");
            sos.println("req.isRequestedSessionIdValid(): "+ req.isRequestedSessionIdValid());
            sos.println("</li><li>");
            sos.println("req.getRemoteUser(): "+ req.getRemoteUser());
            sos.println("</li><li>");
            sos.println("req.isUserInRole('manager'): "+ req.isUserInRole("manager"));
            sos.println("</li><li>");
            sos.println("req.isUserInRole('admin'): "+ req.isUserInRole("admin"));
            sos.println("</li></ul>");
            sos.println("<hr/>");
            sos.println("usersFile: "+ usersFile+"<br/><br/>");

            sos.println("<pre>"+tomcatUsers+"</pre>");


            sos.println("<hr/>");

            Document tusers = opendap.xml.Util.getDocument(usersFile);
            Element root = tusers.getRootElement();
            Comment test = new Comment("Testing to see if we can write to the tomcat users file..."+new Date());
            root.addContent(test);

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            OutputStream os = new FileOutputStream(usersFile);
            xmlo.output(tusers,os);


            sos.print("Added this line to tomcat-users.xml: ");
            sos.println("<pre>"+ StringEscapeUtils.escapeHtml(xmlo.outputString(test))+"</pre>");

            sos.println("<hr/>");

            sos.println("</body>");
            sos.println("</html>");



        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (JDOMException e) {
            log.error(e.getMessage());
        }


    }


private static String readFileAsString(String filePath) throws java.io.IOException{
    byte[] buffer = new byte[(int) new File(filePath).length()];
    BufferedInputStream f = null;
    try {
        f = new BufferedInputStream(new FileInputStream(filePath));
        f.read(buffer);
    } finally {
        if (f != null) try { f.close(); } catch (IOException ignored) { }
    }
    return new String(buffer);
}


}
