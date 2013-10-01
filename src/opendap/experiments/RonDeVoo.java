/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

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
