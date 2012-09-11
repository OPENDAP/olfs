/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2012 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
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
