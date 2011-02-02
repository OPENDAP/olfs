/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

import opendap.coreServlet.OPeNDAPException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.IOException;

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
            OPeNDAPException.anyExceptionHandler(t, resp);

        }

    }



}
