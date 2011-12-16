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
package opendap.bes;

import opendap.io.ChunkedOutputStream;
import opendap.io.Chunk;
import opendap.ppt.PPTSessionProtocol;

import java.io.OutputStream;
import java.io.IOException;

import org.slf4j.Logger;

/**
 * User: ndp
 * Date: Jan 3, 2008
 * Time: 9:38:12 AM
 */
public class BESChunkedOutputStream  extends ChunkedOutputStream {


    private Logger log;

    private String exitMsg = "status="+PPTSessionProtocol.PPT_EXIT_NOW+";";

    public BESChunkedOutputStream(OutputStream stream){
        super(stream);
        log = org.slf4j.LoggerFactory.getLogger(getClass());
    }

    public BESChunkedOutputStream(OutputStream stream, int minChunkSize) throws Exception {

        super(stream,minChunkSize);
        log = org.slf4j.LoggerFactory.getLogger(getClass());
    }

    /**
     * Finishes writing to the underlying stream. After sending any pending data
     * the command is sent that informs the BES that the session is over and that
     * the connection should be closed. The underlying stream is then closed.
     *
     *
     * @throws java.io.IOException When the wrapped OutputStream encounters a problem.
     */
    @Override
    public void close() throws IOException {



        if(!isOpen) throw new IOException(closedMsg);
        log.debug("closing chunked stream and underlying stream.");

        // Send pending data
        flushCache();

        // send extension chunk with BES/PPT exit command
        setChunkTypeToEXTENSION();
        write(exitMsg.getBytes());
        flushCache();

        // Send closing chunk. (The chunk type is always "DATA")
        Chunk.writeClosingChunkHeader(_rawOS);

        // Close underlying stream
        _rawOS.close();

        // Flag this Chunked stream as closed.
        isOpen = false;

    }


}
