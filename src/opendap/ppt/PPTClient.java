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

package opendap.ppt;


import opendap.io.HyraxStringEncoding;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @deprecated
 */
class PPTClient {

    private Socket _mySock = null;
    private BufferedOutputStream _out = null;
    private BufferedInputStream _in = null;

    private Logger log;

    PPTClient(String hostStr, int portVal) throws PPTException {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        InetAddress addr;

        try {
            addr = InetAddress.getByName(hostStr);
        }
        catch (UnknownHostException e) {
            String msg = "Don't know about host: " + hostStr + "\n";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        try {
            _mySock = new Socket(addr, portVal);
        }
        catch (IOException e) {
            String msg = "Could not connect to host " + hostStr + " on port " + portVal + ".  ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        try {
            _out = new BufferedOutputStream(_mySock.getOutputStream());
            _in = new BufferedInputStream(_mySock.getInputStream());
        }
        catch (IOException e) {
            String msg = "Couldn't get I/O for the connection to: " + hostStr + ".  ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

    }


    public String showConnectionProperties() throws SocketException {

        String msg = "\nshowConnectionProperties():\n";
        msg += "    Socket isBound():          " + _mySock.isBound();
        msg += "    Socket isClosed():         " + _mySock.isClosed();
        msg += "    Socket isConnected():      " + _mySock.isConnected();
        msg += "    Socket isInputShutdown():  " + _mySock.isInputShutdown();
        msg += "    Socket isOutputShutdown(): " + _mySock.isOutputShutdown();
        msg += "    Socket getKeepAlive():     " + _mySock.getKeepAlive();
        msg += "    Socket getOOBInline():     " + _mySock.getOOBInline();
        msg += "    Socket getReuseAddress():  " + _mySock.getReuseAddress();
        msg += "    Socket getSoLinger():      " + _mySock.getSoLinger();
        msg += "    Socket getSoTimeout():     " + _mySock.getSoTimeout();


        return msg;

    }


    public void dieNow() {
        try {
            _mySock.close();
        }
        catch (Throwable t) {
            log.error(t.getMessage());
        }
    }


    public boolean initConnection() throws PPTException {
        try {
            this.writeBuffer(PPTSessionProtocol.PPT_CLIENT_TESTING_CONNECTION);
        }
        catch (PPTException e) {
            String msg = "Failed to initialize connection to server. ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        try {
            byte[] inBuff = new byte[4096];
            int bytesRead = this.readBuffer(inBuff);
            String status = new String(inBuff, 0, bytesRead,HyraxStringEncoding.getCharset());
            if (status.compareTo(PPTSessionProtocol.PPT_PROTOCOL_UNDEFINED) == 0) {
                throw new PPTException("Could not connect to server, server may be down or busy");
            }
            if (status.compareTo(PPTSessionProtocol.PPT_SERVER_CONNECTION_OK) != 0) {
                throw new PPTException("Server reported an invalid connection, \"" + status + "\"");
            }
        }
        catch (PPTException e) {
            String msg = "Failed to receive initialization response from server.  ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        return true;
    }

    /**
     * Attempts to gracefully close the connection to the Server.
     *
     * @param informServer A true value will result in an attempt to inform the Server that the client is disconnecting.
     *                     A false value will simple cause the client to close connections with out informing the server.
     */
    public void closeConnection(boolean informServer) {
        try {
            if (informServer && _out != null)
                this.writeBuffer(PPTSessionProtocol.PPT_EXIT_NOW);
        }
        catch (PPTException e) {
            log.error("closeConnection(): Unable to inform server that client is exiting, continuing.");
            log.error(e.getMessage());
        }

        try {
            if (_out != null)
                _out.close();
            _out = null;
        }
        catch (IOException e) {
            log.error("closeConnection(): Unable to close output stream, continuing");
            log.error(e.getMessage());
        }

        try {
            if (_in != null)
                _in.close();
            _in = null;
        }
        catch (IOException e) {
            log.error("closeConnection(): Unable to close input stream, continuing");
            log.error(e.getMessage());
        }

        try {
            if (_mySock != null)
                _mySock.close();
            _mySock = null;
        }
        catch (IOException e) {
            log.error("closeConnection(): Unable to close socket, continuing");
            log.error(e.getMessage());
        }
    }

    public boolean sendRequest(String buffer) throws PPTException {
        this.writeBuffer(buffer);
        this.writeBuffer(PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMISSION);

        return true;
    }

    public boolean writeBuffer(String buffer) throws PPTException {
        try {
            byte[] a = buffer.getBytes(HyraxStringEncoding.getCharset());
            log.debug("Writing " + a.length + "  bytes ...");
            _out.write(a, 0, a.length);
            _out.flush();
            log.debug(" done.");
        }
        catch (IOException e) {
            String msg = "Failed to write to socket:  ";
            msg += e.getMessage();
            closeConnection(false);
            throw new PPTException(msg, e);
        }
        return true;
    }


    /**
     * Get the response from the BES and write it to the passed OutputStream
     *
     * @param strm The stream to which to write the response.
     * @throws PPTException
     */
    public void getResponse(OutputStream strm) throws PPTException {

        try {
            if (strm == null)
                throw new PPTException("Cannot write response to \"null\" OutputStream. ");

            int bytesRead, markBufBytes, i;

            MarkFinder mfinder = new MarkFinder(PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMISSION.getBytes(HyraxStringEncoding.getCharset()));
            byte[] markBuffer = new byte[PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMISSION.length()];
            markBufBytes = 0; // zero byte count in the mark buffer

            BufferedOutputStream bstrm = new BufferedOutputStream(strm);
            byte[] inBuff = new byte[4096];

            boolean done = false;
            while (!done) {
                bytesRead = this.readBuffer(inBuff);                          // Read the response.
                if (bytesRead != 0) {                                         // Got something?

                    for (i = 0; i < bytesRead && !done; i++)
                    {                // look at what we got...
                        done = mfinder.markCheck(inBuff[i]);                  // check for the mark
                        if (!done) {                                          // didn't find the mark?
                            if (mfinder.getMarkIndex() > 0) {                 // did ya find part of it?
                                markBuffer[markBufBytes++] = inBuff[i];       // cache it in case this fragment
                                                                              // isn't part of the whole mark.
                            } else {
                                if (markBufBytes > 0) {                       // if we found part of the mark
                                                                              // (but got fooled) then

                                    markBuffer[markBufBytes++] = inBuff[i];   // cache current char so we don't
                                                                              // have to worry about it.
                                    boolean isdone = false;
                                    while (!isdone) {
                                        bstrm.write(markBuffer[0]);           // write the first character
                                        for (int j = 1; j < markBufBytes; j++)// shift the rest of the
                                        {                                     // characters in markBuffer
                                            markBuffer[j - 1] = markBuffer[j];// to the left one
                                        }
                                        markBufBytes--;                       // we have one less in markBuffer

                                        boolean partof = true;                // start checking the rest of the
                                                                              // markBuffer against the marker

                                        for (int j = 0; j < markBufBytes && partof; j++)
                                        {
                                            mfinder.markCheck(markBuffer[j]); // we won't find the whole
                                                                              // marker so dont' worry about
                                                                              // return value of markCheck

                                            if (mfinder.getMarkIndex() == 0)  // if 0 then char not in marker
                                            {
                                                partof = false;
                                            }
                                        }

                                        if (partof == true) {
                                            isdone = true;
                                        }
                                    }
                                } else {
                                    bstrm.write(inBuff[i]);                   // send this byte that's not part
                                                                              // of a mark.
                                }
                            }
                        }

                    }
                } else {
                    done = true;
                }
            }

            bstrm.flush();
        }
        catch (IOException e) {
            closeConnection(true);
            throw new PPTException("Cannot read response to designated stream. ", e);
        }
    }


    public int readBuffer(byte[] inBuff) throws PPTException {

        int bytesRead;
        try {
            log.debug("Reading bytes ...");
            bytesRead = _in.read(inBuff);
            log.debug("Got " + bytesRead + " bytes.");

            if (bytesRead == -1)
                throw new PPTException("Failed to read response from server. End Of Stream reached prematurely.  ");


            log.debug("Read: " + new String(inBuff,HyraxStringEncoding.getCharset()));
        }
        catch (IOException e) {
            String msg = "Failed to read response from server.  ";
            msg += e.getMessage();
            closeConnection(true);

            throw new PPTException(msg, e);
        }


        return bytesRead;
    }
}

