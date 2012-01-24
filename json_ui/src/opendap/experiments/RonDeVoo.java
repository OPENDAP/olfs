package opendap.experiments;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 23, 2010
 * Time: 7:19:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class RonDeVoo extends HttpServlet {

    public static ConcurrentHashMap<String,String> table = new ConcurrentHashMap<String,String>();


    public void init(){

        String name = this.getServletName();

        String contextPath = this.getServletContext().getContextPath();
        String contextName = this.getServletContext().getServletContextName();

        System.out.println("Servlet Name: "+name);
        System.out.println("Context Path: "+contextPath);
        System.out.println("Context Name: "+contextName);


        RonDeVoo.table.put(name,contextPath);



    }


    public void doGet(HttpServletRequest req, HttpServletResponse resp){



        try {
        ServletOutputStream os = resp.getOutputStream();


        os.println("<html><body>");

        for(String servlet:table.keySet()){
            os.println("<h3> servlet: "+servlet+"  context: "+table.get(servlet)+"</h3>");
        }

        os.println("</body></html>");


        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }


}
