/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
// Author:  Patrick West <pwest@hao.ucar.edu>
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

package opendap.ppt ;

import opendap.coreServlet.Debug;

import java.io.*;
import java.net.*;

class PPTClient {

    private Socket _mySock = null;
    private BufferedOutputStream _out = null;
    private BufferedInputStream _in = null;

    PPTClient(String hostStr, int portVal) throws PPTException {
        InetAddress addr;

        try {
            addr = InetAddress.getByName(hostStr);
        }
        catch (UnknownHostException e) {
            String msg = "Don't know about host: " + hostStr + "\n";
            msg += e.getMessage();
            closeConnection();
            throw new PPTException(msg, e);
        }

        try {
            _mySock = new Socket(addr, portVal);
        }
        catch (IOException e) {
            String msg = "Could not connect to host " + hostStr + " on port " + portVal + ".  ";
            msg += e.getMessage();
            closeConnection();
            throw new PPTException(msg, e);
        }

        try {
            _out = new BufferedOutputStream(_mySock.getOutputStream());
            _in = new BufferedInputStream(_mySock.getInputStream());
        }
        catch (IOException e) {
            String msg = "Couldn't get I/O for the connection to: " + hostStr + ".  ";
            msg += e.getMessage();
            closeConnection();
            throw new PPTException(msg, e);
        }

    }

    public boolean initConnection() throws PPTException {
        try {
            this.writeBuffer(PPTSessionProtocol.PPTCLIENT_TESTING_CONNECTION);
        }
        catch (PPTException e) {
            String msg = "Failed to initialize connection to server. ";
            msg += e.getMessage();
            closeConnection();
            throw new PPTException(msg, e);
        }

        try {
            byte[] inBuff = new byte[4096];
            int bytesRead = this.readBuffer(inBuff);
            String status = new String(inBuff, 0, bytesRead);
            if (status.compareTo(PPTSessionProtocol.PPT_PROTOCOL_UNDEFINED) == 0) {
                throw new PPTException("Could not connect to server, server may be down or busy");
            }
            if (status.compareTo(PPTSessionProtocol.PPTSERVER_CONNECTION_OK) != 0) {
                throw new PPTException("Server reported an invalid connection, \"" + status + "\"");
            }
        }
        catch (PPTException e) {
            String msg = "Failed to receive initialization response from server.  ";
            msg += e.getMessage();
            closeConnection();
            throw new PPTException(msg, e);
        }

        return true;
    }

    public void closeConnection() {
        try {
            if(_out != null)
                this.writeBuffer(PPTSessionProtocol.PPT_EXIT_NOW);
        }
        catch (PPTException e) {
            System.err.println("Failed to inform server that client is exiting, continuing.");
            System.err.println(e.getMessage());
        }

        try {
            if (_out != null)
                _out.close();
            _out = null;
        }
        catch (IOException e) {
            System.err.println("Failed to close output stream, continuing");
            System.err.println(e.getMessage());
        }

        try {
            if (_in != null)
                _in.close();
            _in = null;
        }
        catch (IOException e) {
            System.err.println("Failed to close input stream, continuing");
            System.err.println(e.getMessage());
        }

        try {
            if (_mySock != null)
                _mySock.close();
            _mySock = null;
        }
        catch (IOException e) {
            System.err.println("Failed to close socket, continuing");
            System.err.println(e.getMessage());
        }
    }

    public boolean sendRequest(String buffer) throws PPTException {
        this.writeBuffer(buffer);
        this.writeBuffer(PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION);

        return true;
    }

    public boolean writeBuffer(String buffer) throws PPTException {
        try {
            byte[] a = buffer.getBytes();
            if (Debug.isSet("PPTClient")) System.out.print("PPTClient writing " + a.length + "  bytes ...");
            _out.write(a, 0, a.length);
            _out.flush();
            if (Debug.isSet("PPTClient")) System.out.println(" done.");
        }
        catch (IOException e) {
            String msg = "Failed to write to socket\n";
            msg += e.getMessage();
            closeConnection();
            throw new PPTException(msg, e);
        }
        return true;
    }

    public void getResponseOld(OutputStream strm) throws PPTException {
        PrintStream pstrm = null;
        if (strm != null) {
            pstrm = new PrintStream(strm, true);
        }
        boolean done = false;
        while (!done && pstrm != null) {
            byte[] inBuff = new byte[4096];
            int bytesRead = this.readBuffer(inBuff);
            if (bytesRead != 0) {
                int termlen = PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION.length();
                int writeBytes = bytesRead;
                if (bytesRead >= termlen) {
                    String inEnd = "";
                    for (int j = 0; j < termlen; j++)
                        inEnd += inBuff[(bytesRead - termlen) + j];
                    System.out.println("inEnd:        " + inEnd + " (length: " + inEnd.length() + ")");
                    System.out.println("search value: " + PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION + " (length: " + PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION.length() + ") ");
                    if (inEnd.equals(PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION)) {
                        done = true;
                        writeBytes = bytesRead - termlen;
                    }
                }
                for (int j = 0; j < writeBytes; j++)
                    pstrm.write(inBuff[j]);
            } else {
                done = true;
            }
        }
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

            MarkFinder mfinder = new MarkFinder(PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION.getBytes());
            byte[] markBuffer = new byte[PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION.length()];
            markBufBytes = 0; // zero byte count in the mark buffer

            BufferedOutputStream bstrm = new BufferedOutputStream(strm);
            byte[] inBuff = new byte[4096];

            boolean done = false;
            while (!done) {
                bytesRead = this.readBuffer(inBuff);                          // Read the response.
                if (bytesRead != 0) {                                         // Got something?

                    for (i = 0; i < bytesRead && !done; i++) {                // look at what we got...
                        done = mfinder.markCheck(inBuff[i]);                  // check for the mark
                        if (!done) {                                          // didn't find the mark?
                            if (mfinder.getMarkIndex() > 0) {                 // did ya find part of it?
                                markBuffer[markBufBytes++] = inBuff[i];       // cache it in case this fragment
                                                                              // isn't part of the whole mark.
                            } else {
                                if (markBufBytes > 0) {                       // if we found part of the mark
                                                                              // (but got fooled) then

                                    bstrm.write(markBuffer, 0, markBufBytes); // send the fragment.
                                    markBufBytes = 0;
                                }

                                bstrm.write(inBuff[i]);                       // send this byte that's not part
                                                                              // of a mark.
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
            closeConnection();
            throw new PPTException("Cannot read response to designated stream. ", e);
        }
    }


    public int readBuffer(byte[] inBuff) throws PPTException {

        int bytesRead;
        try {
            if (Debug.isSet("PPTClient")) System.out.print("PPTClient reading bytes ...");
            bytesRead = _in.read(inBuff);
            if (Debug.isSet("PPTClient")) System.out.println(" got " + bytesRead + " bytes.");

            if(bytesRead == -1)
                throw new PPTException("Failed to read response from server. End Of Stream reached prematurely.  ");


            if (Debug.isSet("PPTClient")) System.out.println("Read: " + new String(inBuff));
        }
        catch (IOException e) {
            String msg = "Failed to read response from server.  ";
            msg += e.getMessage();
            closeConnection();

            throw new PPTException(msg, e);
        }


        return bytesRead;
    }
}

