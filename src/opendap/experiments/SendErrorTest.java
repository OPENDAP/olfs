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

import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jan 12, 2009
 * Time: 11:19:03 AM
 * To change this template use File | Settings | File Templates.
 */
public class SendErrorTest extends HttpServlet {



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {
        try {

            String name = "moo<imbedd>spoo";

            javax.xml.namespace.QName n2 = new javax.xml.namespace.QName(name);
            System.out.println("QNAME: "+n2);



            System.out.println("\n*********************************************\n" +
                    "Calling HttpServletResponse.sendError(404)");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("\nHttpServletResponse.sendError(404) returned.\n" +
                    "*********************************************\n");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


}
