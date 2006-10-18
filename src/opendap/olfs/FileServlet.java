/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
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

package opendap.olfs;


import opendap.coreServlet.Debug;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.MimeTypes;
import opendap.coreServlet.ReqInfo;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.IOException;



/**
 * Servers regular files from the BES - used in conjunction with the OPeNDAP directry response.
 * User: ndp
 * Date: Oct 12, 2006
 * Time: 1:32:37 PM
 * @see S4Dir
 */
public class FileServlet extends HttpServlet {

    private MimeTypes mimeTypes;


    public void init() {
        mimeTypes = new MimeTypes();

    }


    public long getLastModified(HttpServletRequest req) {


        String name = req.getPathInfo();


        if (Debug.isSet("showRequest"))
            System.out.print("FileServlet - getlastModified() for dataSource: " + name + "  ");

        String path = BESCrawlableDataset.besPath2ThreddsPath(name);

        try {
            BESCrawlableDataset cd = new BESCrawlableDataset(path, null);
            if (Debug.isSet("showRequest")) System.out.println("Returning: " + cd.lastModified());

            return cd.lastModified().getTime();
        }
        catch (Exception e) {
            if (Debug.isSet("showRequest")) System.out.println("Returning: -1");
            return -1;
        }

    }


    public void doGet(HttpServletRequest req,
                      HttpServletResponse response)
            throws IOException, ServletException {


        String name = req.getPathInfo();


        if (Debug.isSet("showRequest")) System.out.print("FileServlet - The client requested this: " + name);

        String suffix = ReqInfo.getRequestSuffix();

        if(suffix!=null){
            String mType = mimeTypes.getMimeType(suffix);
            if(mType!=null)
                response.setContentType(mType);
            if(Debug.isSet("showRequest")) System.out.print("   MIME type: "+mType+"  ");
        }

        if (Debug.isSet("showRequest")) System.out.println();


        try {
            ServletOutputStream sos = response.getOutputStream();
            BesAPI.writeFile(name, sos);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        catch (Exception e) {
            OPeNDAPException.anyExceptionHandler(e, response);
        }


    }


}
