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

package opendap.coreServlet;

import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.io.IOException;


/**
 * Holds The type information and a referene to an input stream for the content of a Mutipart
 * MIME attachment.
 * @see MultipartResponse
 * User: ndp
 * Date: Apr 28, 2006
 * Time: 12:13:19 PM
 */
public class Attachment {

    private Logger log;
    String contentTransferEncoding = "binary";
    String contentId;
    String contentType;
    InputStream istream;

    /**
     *
     * @param ctype String containing the value of the HTTP header Content-Type for this attachment.
     * @param cid String containing the value if the HTTP header Content-Id for this attachment.
     * @param is A stream containing the content for this attachment.
     */
    Attachment(String ctype, String cid, InputStream is){
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        contentType = ctype;
        contentId = cid;
        istream = is;
    }

    /**
     * Write the attchment to the indicated stream
     * @param mimeBoundary MIME Boundary for the attachment.
     * @param sos Stream to which to write the attachment.
     * @throws IOException
     */
    void write(String mimeBoundary, ServletOutputStream sos) throws IOException {


        try {
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
            //MIME Attachments need to end with a newline!
            sos.println();
        }
        finally {
            if(istream!=null){
                try {
                    istream.close();
                }
                catch(IOException e){
                    log.error("Failed to close content source InputStream. " +
                            "Error Message: "+e.getMessage());

                }
            }
        }

    }



}
