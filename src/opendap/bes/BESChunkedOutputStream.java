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
package opendap.bes;

import opendap.io.Chunk;
import opendap.io.ChunkedOutputStream;
import opendap.ppt.PPTSessionProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

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
        // Logger log = LoggerFactory.getLogger(BES.class);

        if(!isOpen) throw new IOException(closedMsg);
        log.debug("close() - Closing BES Connection.");

        // Send pending data
        flushCache();
        log.debug("close() - Flushed Cache.");


        log.debug("close() - Sending extension chunk with BES/PPT exit command.");

        // send extension chunk with BES/PPT exit command
        setChunkTypeToEXTENSION();
        write(exitMsg.getBytes());
        log.debug("close() - SENT extension chunk with BES/PPT exit command.");
        log.debug("close() - Flushing Cache.");
        flushCache();
        log.debug("close() - Flushed Cache.");

        // Send closing chunk. (The chunk type is always "DATA")
        log.debug("close() - Sending closing chunk. (The chunk type is always \"DATA\")");
        Chunk.writeClosingChunkHeader(_rawOS);
        log.debug("close() - SENT closing chunk.");

        // Close underlying stream
        log.debug("close() - closing underlying stream.");
        _rawOS.close();
        log.debug("close() - closed underlying stream.");

        // Flag this Chunked stream as closed.
        isOpen = false;

        log.debug("close() - Stream is Closed.");

    }


}
