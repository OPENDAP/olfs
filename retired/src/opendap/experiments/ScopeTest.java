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

import opendap.coreServlet.OPeNDAPException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * User: ndp
 * Date: Apr 23, 2007
 * Time: 12:03:22 PM
 */
public class ScopeTest extends HttpServlet {



    public void init(){

        String val = getInitParameter("ScopeTest");

        ScopeVar.init(val);



    }


    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {


        try {

            resp.setContentType("text/html");

            PrintWriter pw = resp.getWriter();




            pw.println("<html>");
            pw.println("<body>");
            pw.println("<h1>Scope Variable: "+ScopeVar.getVar()+"</h1>");
            pw.println("</body>");
            pw.println("</html>");
        }
        catch(Throwable t){
            OPeNDAPException.anyExceptionHandler(t, this, resp);

        }

    }



}
