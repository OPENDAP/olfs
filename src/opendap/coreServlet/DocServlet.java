/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.coreServlet;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;


import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;

import thredds.servlet.ServletUtil;

/**
 * User: ndp
 * Date: Oct 5, 2006
 * Time: 2:54:35 PM
 */
public class DocServlet extends HttpServlet {




    private String documentsDirectory;



    public void init(){

        String dir = ServletUtil.getContentPath(this) + "docs";

        File f = new File(dir);

        if(f.exists() && f.isDirectory())
            documentsDirectory = dir;
        else {

            documentsDirectory = this.getServletContext().getRealPath("docs");

        }



    }



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {


        if(Debug.isSet("DocServlet")) System.out.println("DocServlet - documentsDirectory: "+documentsDirectory);


        String name = request.getPathInfo();



        if(name==null || name.equals("/"))
             name = "/index.html";


        name = documentsDirectory + name;


        if(Debug.isSet("DocServlet")) System.out.println("DocServlet - I think the client requested this: "+name);


        File f = new File(name);

        if(f.exists()){
            if(Debug.isSet("DocServlet")) System.out.println("Requested item EXISTS!");


            FileInputStream fis = new FileInputStream(f);

            ServletOutputStream sos = response.getOutputStream();

            byte buff[] = new byte[8192];
            int rc;
            boolean doneReading = false;
            while(!doneReading){
                rc = fis.read(buff);
                if(rc<0){
                    doneReading = true;
                }
                else if(rc>0){
                    sos.write(buff,0,rc);
                }

            }

            response.setStatus(HttpServletResponse.SC_OK);

            sos.flush();

        }
        else {
            System.out.println("Requested item Does Not Exist.");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        }



    }

}
