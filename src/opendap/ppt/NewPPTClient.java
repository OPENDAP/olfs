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
package opendap.ppt;

import opendap.io.ChunkProtocol;
import org.slf4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import opendap.bes.BESChunkedOutputStream;
import opendap.io.ChunkedInputStream;

/**
 * User: ndp
 * Date: Jan 7, 2008
 * Time: 3:28:18 PM
 */
public class NewPPTClient {
    private Socket _mySock = null;
    private BESChunkedOutputStream _out = null;
    private ChunkedInputStream _in = null;
    private OutputStream _rawOut = null;
    private InputStream _rawIn = null;

    private Logger log;

    NewPPTClient(String hostStr, int portVal) throws PPTException {

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
            _mySock.setSoTimeout(300000);
        }
        catch (IOException e) {
            String msg = "Could not connect to host " + hostStr + " on port " + portVal + ".  ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        try {
            _rawOut = _mySock.getOutputStream();
            _rawIn = _mySock.getInputStream();
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



    public void dieNow(){
        try{
            _mySock.close();
        }
        catch(Throwable t){
            log.error(t.getMessage());
        }
    }




    public boolean initConnection() throws PPTException {

        try {
            _rawOut.write(PPTSessionProtocol.PPTCLIENT_TESTING_CONNECTION.getBytes());
            _rawOut.flush();
        }
        catch (IOException e) {
            String msg = "Failed to initialize connection to server. ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        try {
            byte[] inBuff = new byte[4096];
            int bytesRead = _rawIn.read(inBuff);
            String status = new String(inBuff, 0, bytesRead);
            if (status.compareTo(PPTSessionProtocol.PPT_PROTOCOL_UNDEFINED) == 0) {
                throw new PPTException("Could not connect to server, server may be down or busy");
            }
            if (status.compareTo(PPTSessionProtocol.PPTSERVER_CONNECTION_OK) != 0) {
                throw new PPTException("Server reported an invalid connection, \"" + status + "\"");
            }

            if(_rawIn.available()>0){
                long skipped = _rawIn.skip(_rawIn.available());
                log.debug("Skipped "+skipped+" bytes in the input stream.");
            }



        }
        catch (IOException e) {
            String msg = "Failed to receive initialization response from server.  ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        _out = new BESChunkedOutputStream(_rawOut);


        _in = new ChunkedInputStream(_rawIn, new PPTSessionProtocol());

        return true;
    }

    /**
     * Attempts to gracefully close the connection to the Server.
     * @param informServer A true value will result in an attempt to inform the Server that the client is disconnecting.
     * A false value will simple cause the client to close connections with out informing the server.
     */
    public void closeConnection(boolean informServer) {
        try {
            if(informServer && _out != null)
                _out.close();
            _out = null;
        }
        catch (IOException e) {
            log.error("closeConnection(): Unable to inform server that client is exiting, continuing.");
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
        try {
            //System.out.println("Sending: "+buffer);
            _out.write(buffer.getBytes());
            _out.finish();
            _out.flush();
        } catch (IOException e) {
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
     * @param errorStream The stream to which to write any errors sent from
     * the server.
     * @return False if the server returns an error extension in the message,
     * true otherwise.
     * @throws PPTException Stuff happens
     */
    public boolean getResponse(OutputStream strm,
                               OutputStream errorStream)
            throws PPTException {

        try {
            if (strm == null)
                throw new PPTException("Cannot write response to a \"null\" " +
                        "OutputStream. ");

            return _in.readChunkedMessage(strm,errorStream);

        }
        catch (IOException e) {
            closeConnection(true);
            throw new PPTException("Cannot read response from designated " +
                    "stream. ", e);
        }
    }








    public boolean sendXMLRequest(Document req) throws PPTException {
        try {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            log.debug("\n"+xmlo.outputString(req));
            xmlo.output(req,_out);
            _out.finish();
            _out.flush();
        } catch (IOException e) {
            String msg = "Failed to write to socket:  ";
            msg += e.getMessage();
            closeConnection(false);
            throw new PPTException(msg, e);
        }

        return true;
    }



    /*



    public Document getXMLResponse() throws JDOMException, PPTException {

        try {

            SAXBuilder parser = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);

           return  parser.build(_in);

        }
        catch (IOException e) {
            closeConnection(true);
            throw new PPTException("Cannot read response to designated " +
                    "stream. ", e);
        }
    }
    */






















}
