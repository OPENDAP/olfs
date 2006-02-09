/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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
            throw(new PPTException(msg));
        }

        try {
            _mySock = new Socket(addr, portVal);
        }
        catch (IOException e) {
            String msg = "Could not connect to host " + hostStr + " on port " + portVal + "\n";
            msg += e.getMessage();
            throw(new PPTException(msg));
        }

        try {
            _out = new BufferedOutputStream(_mySock.getOutputStream());
            _in = new BufferedInputStream(_mySock.getInputStream());
        }
        catch (IOException e) {
            String msg = "Couldn't get I/O for the connection to: " + hostStr + "\n";
            msg += e.getMessage();
            throw(new PPTException(msg));
        }
    }

    public boolean initConnection() throws PPTException {
        try {
            this.writeBuffer(PPTSessionProtocol.PPTCLIENT_TESTING_CONNECTION);
        }
        catch (PPTException e) {
            String msg = "Failed to initialize connection to server\n";
            msg += e.getMessage();
            throw(new PPTException(msg));
        }

        try {
            byte[] inBuff = new byte[4096];
            int bytesRead = this.readBuffer(inBuff);
            String status = new String(inBuff, 0, bytesRead);
            if (status.compareTo(PPTSessionProtocol.PPT_PROTOCOL_UNDEFINED) == 0) {
                throw(new PPTException("Could not connect to server, server may be down or busy"));
            }
            if (status.compareTo(PPTSessionProtocol.PPTSERVER_CONNECTION_OK) != 0) {
                throw(new PPTException("Server reported an invalid connection, \"" + status + "\""));
            }
        }
        catch (PPTException e) {
            String msg = "Failed to receive initialization response from server\n";
            msg += e.getMessage();
            throw(new PPTException(msg));
        }

        return true;
    }

    public void closeConnection() {
        try {
            this.writeBuffer(PPTSessionProtocol.PPT_EXIT_NOW);
        }
        catch (PPTException e) {
            System.err.println("Failed to inform server that client is exiting, continuing");
            System.err.println(e.getMessage());
        }

        try {
            _out.close();
        }
        catch (IOException e) {
            System.err.println("Failed to close output stream, continuing");
            System.err.println(e.getMessage());
        }

        try {
            _in.close();
        }
        catch (IOException e) {
            System.err.println("Failed to close input stream, continuing");
            System.err.println(e.getMessage());
        }

        try {
            _mySock.close();
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
            System.out.print("PPTClient writing "+a.length+"  bytes ...");
            _out.write(a, 0, a.length);
            _out.flush();
            System.out.println(" done.");
        }
        catch (IOException e) {
            String msg = "Failed to write to socket\n";
            msg += e.getMessage();
            throw(new PPTException(msg));
        }

        return true;
    }

    public void getResponse(OutputStream strm) throws PPTException {
        PrintStream pstrm = null;
        if (strm != null) {
            pstrm = new PrintStream(strm, true);
        }
        boolean done = false;
        while (!done && pstrm!=null) {
            byte[] inBuff = new byte[4096];
            int bytesRead = this.readBuffer(inBuff);
            if (bytesRead != 0) {
                int termlen = PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION.length();
                int writeBytes = bytesRead;
                if (bytesRead >= termlen) {
                    String inEnd = "";
                    for (int j = 0; j < termlen; j++)
                        inEnd += inBuff[(bytesRead - termlen) + j];
                    System.out.println("inEnd:        "+inEnd+" (length: "+inEnd.length()+")");
                    System.out.println("search value: "+PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION + " (length: "+PPTSessionProtocol.PPT_COMPLETE_DATA_TRANSMITION.length()+") ");
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



    public int readBuffer(byte[] inBuff) throws PPTException {
        int bytesRead;
        try {
            System.out.print("PPTClient reading bytes ...");
            bytesRead = _in.read(inBuff,0,4096);
            System.out.println(" got "+bytesRead+" bytes.");
            System.out.println("Read: "+ new String(inBuff));
        }
        catch (IOException e) {
            String msg = "Failed to read response from server\n";
            msg += e.getMessage();
            throw(new PPTException(msg));
        }

        return bytesRead;
    }
}

