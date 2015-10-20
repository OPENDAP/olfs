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

import opendap.bes.BES;
import opendap.bes.BESChunkedOutputStream;
import opendap.io.ChunkedInputStream;
import opendap.io.HyraxStringEncoding;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

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

    NewPPTClient(String hostStr, int portVal, int timeOut) throws PPTException {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        InetAddress host;
        InetSocketAddress address;

        try {
            host = InetAddress.getByName(hostStr);
            address = new InetSocketAddress(host,portVal);
        }
        catch (UnknownHostException e) {
            String msg = "Don't know about host: " + hostStr + "\n";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        try {
            _mySock = new Socket();
            _mySock.connect(address,timeOut);
            _mySock.setSoTimeout(timeOut);
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

            log.debug("NewPPTClient() -  Using '"+_rawIn.getClass().getName()+"' InputStream implementation.");
            log.debug("NewPPTClient() -  Using '"+_rawOut.getClass().getName()+"' OutputStream implementation.");


        }
        catch (IOException e) {
            String msg = "Couldn't get I/O for the connection to: " + hostStr + ".  ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

    }


    public String showConnectionProperties() {

        StringBuilder msg = new StringBuilder();

        msg.append("\nshowConnectionProperties():\n");
        msg.append("    Socket isBound():          ").append(_mySock.isBound()).append("\n");
        msg.append("    Socket isClosed():         ").append(_mySock.isClosed()).append("\n");
        msg.append("    Socket isConnected():      ").append(_mySock.isConnected()).append("\n");
        msg.append("    Socket isInputShutdown():  ").append(_mySock.isInputShutdown()).append("\n");
        msg.append("    Socket isOutputShutdown(): ").append(_mySock.isOutputShutdown()).append("\n");

        try {
            msg.append("    Socket getKeepAlive():     ").append(_mySock.getKeepAlive()).append("\n");
        } catch (SocketException e) {
            msg.append("Caught SocketException! Msg: ").append(e.getMessage()).append("\n");
        }

        try {
            msg.append("    Socket getOOBInline():     ").append(_mySock.getOOBInline()).append("\n");
        } catch (SocketException e) {
            msg.append("Caught SocketException! Msg: ").append(e.getMessage()).append("\n");
        }

        try {
            msg.append("    Socket getReuseAddress():  ").append(_mySock.getReuseAddress()).append("\n");
        } catch (SocketException e) {
            msg.append("Caught SocketException! Msg: ").append(e.getMessage()).append("\n");
        }

        try {
            msg.append("    Socket getSoLinger():      ").append(_mySock.getSoLinger()).append("\n");
        } catch (SocketException e) {
            msg.append("Caught SocketException! Msg: ").append(e.getMessage()).append("\n");
        }

        try {
            msg.append("    Socket getSoTimeout():     ").append(_mySock.getSoTimeout()).append("\n");
        } catch (SocketException e) {
            msg.append("Caught SocketException! Msg: ").append(e.getMessage()).append("\n");
        }



        return msg.toString();

    }

    public boolean isClosed(){
        return _mySock.isClosed();
    }

    public boolean isConnected(){
        return _mySock.isConnected();
    }


    public int getChunkReadBufferSize(){

        return _in.getChunkedReadBufferSize();

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

        //Logger log = LoggerFactory.getLogger(BES.class);
        log.debug("initConnection() -  START");

        try {
            _rawOut.write(PPTSessionProtocol.PPTCLIENT_TESTING_CONNECTION.getBytes(HyraxStringEncoding.getCharset()));
            _rawOut.flush();
        }
        catch (IOException e) {
            String msg = "Failed to initialize connection to server. ";
            msg += e.getMessage();
            closeConnection(true);
            throw new PPTException(msg, e);
        }

        log.debug("initConnection() -  Sent '"+PPTSessionProtocol.PPTCLIENT_TESTING_CONNECTION+"' to server.");

        byte[] inBuff = new byte[4096];
        int bytesRead;

        try {

            bytesRead = _rawIn.read(inBuff);
            /*
            int lapCounter = 1;
            while(bytesRead<0 && lapCounter <= 10){
                log.debug("Reached End Of Stream when attempting to retrieve the PPT handshake response. Attempt: "+lapCounter);
                log.debug(showConnectionProperties());
                log.debug("Sleeping for 1 second");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Something woke me up!!! Msg: "+e.getMessage());
                }
                bytesRead = _rawIn.read(inBuff);
                lapCounter++;
            }
            */
        }
        catch (IOException e) {
           String msg = "Caught "+e.getClass().getSimpleName()+" attempting to read initialization response from server.  Message: ";
           msg += e.getMessage();
           log.error(msg);
           closeConnection(false);
           throw new PPTException(msg, e);
        }

        if(bytesRead<0){
            log.error("initConnection() -  Encountered End Of Stream when attempting to read server handshake response!");
            throw new PPTEndOfStreamException("PPT Connection encounter a premature End Of Stream - The connection appears to have been prematurely closed.");
        }


        try {
            String status = new String(inBuff, 0, bytesRead, HyraxStringEncoding.getCharset());
            if (status.compareTo(PPTSessionProtocol.PPT_PROTOCOL_UNDEFINED) == 0) {
                log.error("initConnection() -  Received '"+PPTSessionProtocol.PPT_PROTOCOL_UNDEFINED+"' from server. That's a bad thing!");
                throw new PPTException("Could not connect to server, server may be down or busy");
            }
            if (status.compareTo(PPTSessionProtocol.PPTSERVER_CONNECTION_OK) != 0) {
                log.error("initConnection() -  Received unrecognized status '"+status+"' from server. That's a bummer man...");
                throw new PPTException("Server reported an invalid connection status , \"" + status + "\"");
            }
            log.debug("initConnection() -  Received '"+PPTSessionProtocol.PPTSERVER_CONNECTION_OK+"' from server.");

            if(_rawIn.available()>0){
                long skipped = _rawIn.skip(_rawIn.available());
                log.warn("Skipped "+skipped+" bytes in the input stream.");
            }

        }
        catch (IOException e) {
            String msg = "Failed to receive initialization response from server.  ";
            msg += e.getMessage();
            closeConnection(false);
            throw new PPTException(msg, e);
        }

        _out = new BESChunkedOutputStream(_rawOut);


        _in = new ChunkedInputStream(_rawIn, new PPTSessionProtocol());

        log.debug("initConnection() -  END");

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
            log.error("closeConnection(): Unable to inform server that client is exiting, continuing. Base message: '" + e.getMessage()+"'");
        }

        try {
            if (_in != null)
                _in.close();
            _in = null;
        }
        catch (IOException e) {
            log.error("closeConnection(): Unable to close input stream, continuing. Base message: '" + e.getMessage()+"'");
        }

        try {
            if (_mySock != null)
                _mySock.close();
            _mySock = null;
        }
        catch (IOException e) {
            log.error("closeConnection(): Unable to close socket, continuing. Base message: '" + e.getMessage()+"'");
        }
    }

    public boolean sendRequest(String buffer) throws PPTException {
        try {
            //System.out.println("Sending: "+buffer);
            _out.write(buffer.getBytes(HyraxStringEncoding.getCharset()));
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

            return _in.readChunkedMessage(strm, errorStream);

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
