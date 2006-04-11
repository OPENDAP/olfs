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

import java.io.*;
import java.util.*;

import opendap.dap.parser.ParseException;
import opendap.dap.DataDDS;
import opendap.dap.ServerVersion;
import opendap.dap.DODSException;
import opendap.dap.BaseType;
import opendap.util.*;
import opendap.servers.ascii.toASCII;
import opendap.servers.ascii.asciiFactory;


/**
 * Default handler for OPeNDAP ascii requests. This class is used
 * by OLFS. This code exists as a seperate class in order to alleviate
 * code bloat in the OLFS class. As such, it contains virtually no
 * state, just behaviors.
 *
 * @author Nathan David Potter
 */

class AsciiResponse {


    /**--------------------------------------------------------------------------------
     *
     * Default handler for OPeNDAP ascii requests. Returns OPeNDAP data in
     * comma delimited ascii columns for ingestion into some not so
     * OPeNDAP enabled application such as MS-Excel. 
     */
    static void sendASCII(PrintWriter pw,
                          ReqState rs,
                          InputStream is)
            throws DODSException, ParseException, IOException {


        if (Debug.isSet("showResponse"))
            System.out.println(" Flow in sendASCII()");

        ServerVersion sv = new ServerVersion(rs.getXDODSServer());

        if (Debug.isSet("asciiResponse")) {
            System.out.println("    Major Server Version: " + sv.getMajor());
            System.out.println("    Minor Server Version: " + sv.getMinor());
        }

        DataDDS dds = new DataDDS(sv, new asciiFactory());
        dds.parse(new opendap.dap.HeaderInputStream(is));
        dds.readData(is, null);


        if (Debug.isSet("asciiResponse")) System.out.println(" ASCII DDS: ");
        if (Debug.isSet("asciiResponse")) dds.print(System.out);

        PrintWriter pwDebug = new PrintWriter(System.out);

        //pw.println("<pre>");
        dds.print(pw);
        pw.println("---------------------------------------------");


        Enumeration e = dds.getVariables();

        while (e.hasMoreElements()) {
            BaseType bt = (BaseType) e.nextElement();
            if (Debug.isSet("asciiResponse")) ((toASCII) bt).toASCII(pwDebug, true, null, true);
            //bt.toASCII(pw,addName,getNAme(),true);
            ((toASCII) bt).toASCII(pw, true, null, true);
        }

        //pw.println("</pre>");
        pw.flush();
        if (Debug.isSet("asciiResponse")) pwDebug.flush();


        if (Debug.isSet("asciiResponse")) System.out.println(" dodsASCII done");


    }
    /**--------------------------------------------------------------------------------*/

}





