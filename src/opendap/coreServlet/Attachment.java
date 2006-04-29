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

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Apr 28, 2006
 * Time: 12:13:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class Attachment {
    String contentTransferEncoding = "binary";
    String contentId;
    String contentType;
    InputStream istream;
    Attachment(String ctype, String cid, InputStream is){
        contentType = ctype;
        contentId = cid;
        istream = is;
    }

    void write(String mimeBoundary, ServletOutputStream sos) throws IOException {


        sos.println("--"+mimeBoundary);
        sos.println("Content-Type: "+contentType);
        sos.println("Content-Transfer-Encoding: "+contentTransferEncoding);
        sos.println("Content-Id: <"+contentId+">");
        sos.println();

        int val;
        boolean done = false;
        while(!done){
            val = istream.read();
            if(val == -1)
                done = true;
            else {
                sos.write(val);
            }
        }
        sos.println();


    }



}
