package opendap.experiments;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;


public class EchoServlet extends HttpServlet {


    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        ServletOutputStream out = response.getOutputStream();


        out.println("<html>");
        out.println("<head><title>Simple jsp page</title></head>");
        out.println("<body>");


        out.println("<table>");
        out.println("<th colspan=\"2\">HTTP Request Headers</th>");
        Enumeration headers = request.getHeaderNames();
        while(headers.hasMoreElements()){
            String headerName = (String) headers.nextElement();
            String headerValue = request.getHeader(headerName);
            out.println("<tr>");
                out.println("<td style=\"text-align: right;\"><code><strong>"+headerName+"</strong></code></td>");
                out.println("<td><code> "+headerValue+"</code></td>");
            out.println("</tr>");
        }
        out.println("</table>");



        out.println("</body>");
        out.println("</html>");
    }

}
