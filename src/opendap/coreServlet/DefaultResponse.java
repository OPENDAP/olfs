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

import opendap.dap.DODSException;
import opendap.dap.DAS;
import opendap.dap.Server.ServerDDS;
import opendap.dap.parser.ParseException;
import opendap.ppt.PPTException;

import java.io.PrintWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Mar 31, 2006
 * Time: 3:02:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultResponse {


    public static void sendAsciiResponse(PrintWriter pw,
                                         ReqState rs,
                                         InputStream is)
            throws DODSException, ParseException, IOException {

        AsciiResponse.sendASCII(pw, rs, is);

    }

    public static void sendHtmlResponse(PrintWriter pw,
                                        ReqState rs,
                                        ServerDDS dds,
                                        DAS das)
            throws DODSException, ParseException {


        HtmlResponse.sendDataRequestForm(pw, rs, dds, das);


    }

    public static void sendInfoResponse(PrintStream pw,
                                        ReqState rs, ServerDDS dds, DAS das)
            throws DODSException {

        InfoResponse.sendINFO(pw, rs, dds, das);

    }

}
