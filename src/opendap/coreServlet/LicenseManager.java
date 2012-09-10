package opendap.coreServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 */
public class LicenseManager {

    private static Logger log;
    private static Date expirationDate;


    static {
        log = LoggerFactory.getLogger(LicenseManager.class.getName());
        GregorianCalendar expires = new GregorianCalendar(2012,8,1);   // 2012,8,1 is September 1, 2012
        expirationDate = expires.getTime();

    }








    public static boolean isExpired(HttpServletRequest request){

        Date now = new Date();

        long timeRemaining = (expirationDate.getTime() - now.getTime())/1000;

        log.debug("License expires in {} seconds.",timeRemaining);

        //return now.after(expirationDate);
        return false;
    }






    public static void sendLicenseExpiredPage(HttpServletRequest request, HttpServletResponse response) throws IOException {

        log.debug("sendLicenseExpiredPage() Sending License Expired page");

        response.setContentType("text/html");
        response.setHeader("Content-Description", "text/html");

        String pageContent = getPageContent(request.getContextPath());
        response.getOutputStream().print(pageContent.toString());

        log.error("Hyrax License Expired.");






    }


    private static String getPageContent(String contextPath) {

        StringBuilder pageContent = new StringBuilder();


        pageContent.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        pageContent.append("<head>");
        pageContent.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\" />");
        pageContent.append("    <link rel='stylesheet' href='").append(contextPath).append("/docs/css/contents.css' type='text/css' />");
        pageContent.append("<title>Hyrax:  License Expired</title>");
        pageContent.append("</head>");
        pageContent.append("<body>");
        pageContent.append("<p align=\"left\">&nbsp;</p>");
        pageContent.append("<h1 align=\"center\">Hyrax : License Expired </h1>");
        pageContent.append("<hr align=\"left\" size=\"1\" noshade=\"noshade\" />");
        pageContent.append("<table width=\"100%\" border=\"0\">");
        pageContent.append("  <tr>");
        pageContent.append("    <td><img src=\"").append(contextPath).append("/docs/images/forbidden.png\" alt=\"Forbidden!\" width=\"350\" height=\"313\" /></td>");
        pageContent.append("    <td align=\"center\"><strong>The license to use this copy of Hyrax has expired.</strong>");
        pageContent.append("      <p align=\"left\">&nbsp;</p>");
        pageContent.append("      <p align=\"left\">&nbsp;</p>");
        pageContent.append("    </td>");
        pageContent.append("  </tr>");
        pageContent.append("</table>");
        pageContent.append("<hr align=\"left\" size=\"1\" noshade=\"noshade\" />");
        pageContent.append("<h1 align=\"center\">Hyrax : License Expired </h1>");
        pageContent.append("</body>");
        pageContent.append("</html>");


        return pageContent.toString();

    }

}
