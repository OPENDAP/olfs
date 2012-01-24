/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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


package opendap.ppt;

import org.slf4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.*;

/**
 * OpenDAPClient is an object that handles the connection to, sending requests
 * to, and receiving response from a specified OpenDAP server running either
 * on this machine or another machine.
 * <p/>
 * Requests to the OpenDAP server can be taken in different ways by the
 * OpenDAPClient object.
 * <UL>
 * <LI>One request, ending with a semicolon.</LI>
 * <LI>Multiple requests, each ending with a semicolon.</LI>
 * <LI>Requests listed in a file, each request can span multiple lines in
 * the file and there can be more than one request per line. Each request
 * ends with a semicolon.</LI>
 * <LI>Interactive mode where the user inputs requests on the command line,
 * each ending with a semicolon, with multiple requests allowed per
 * line.</LI>
 * </UL>
 * <p/>
 * Response from the requests can sent to any File or OutputStream as
 * specified by using the setOutput methods. If no output is specified using
 * the setOutput methods thent he output is ignored.
 * <p/>
 * Thread safety of this object has not yet been determined.
 *
 * @author Patrick West <A * HREF="mailto:pwest@hao.ucar.edu">pwest@hao.ucar.edu</A>
 */

public class OPeNDAPClient {
    private int commandCount;
    private NewPPTClient _client = null;
    private OutputStream _stream = null;
    private boolean _isRunning;
    private Logger log = null;
    private String _id;

    /**
     * Creates a OpenDAPClient to handle OpenDAP requests.
     * <p/>
     * Sets the output of any responses from the OpenDAP server to null,
     * meaning that all responses will be thrown out.
     */
    public OPeNDAPClient() {
        _stream = null;
        _isRunning = false;
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        commandCount = 0;

    }


    public String getID() {
        return _id;
    }

    public void setID(String ID) {
        _id = ID;
    }

    public int getCommandCount() {
        return commandCount;
    }


    public boolean isRunning() {
        return _isRunning;
    }

    /**
     * Connect the OpenDAP client to the OpenDAP server.
     * <p/>
     * Connects to the OpenDAP server on the specified machine listening on
     * the specified port.
     *
     * @param hostStr The name of the host machine where the server is
     *                running.
     * @param portVal The port on which the server on the host hostStr is
     *                listening for requests.
     * @throws PPTException Thrown if unable to connect to the specified host
     *                      machine given the specified port.
     * @see String
     * @see PPTException
     */
    public void startClient(String hostStr, int portVal) throws PPTException {
        _client = new NewPPTClient(hostStr, portVal);
        _client.initConnection();
        _isRunning = true;
    }

    /**
     * Closes the connection to the OpeNDAP server and closes the output stream.
     *
     * @throws PPTException Thrown if unable to close the connection or close
     *                      the output stream.
     *                      machine given the specified port.
     * @see OutputStream
     * @see PPTException
     */
    public void shutdownClient() throws PPTException {

        if(_client!=null)
            _client.closeConnection(true);

        if (_stream != null) {
            try {
                _stream.close();
            }
            catch (IOException e) {
                throw (new PPTException(e.getMessage()));
            }
            finally {
                _isRunning = false;
            }
        }
        _isRunning = false;
    }



    public void killClient() {
        _client.dieNow();
    }



    /**
     * Sends a single OpeNDAP request ending in a semicolon (;) to the
     * OpeNDAP server.
     * <p/>
     * The response is written to the output stream if one is specified,
     * otherwise the output is ignored.
     *
     * @param cmd The OpenDAP request, ending in a semicolon, that is sent to
     *            the OpenDAP server to handle.
     *
     * @param target The target OutputStream for the results of the command.
     * @param error The error OutputStream for errors returned by the server.
     *
     * @return True if successful, false if the server returned an error.
     * @throws PPTException Thrown if there is a problem sending the request
     *                      to the server or a problem receiving the response
     *                      from the server.
     * @see String
     * @see PPTException
     */
    public boolean executeCommand(String cmd,
                                  OutputStream target,
                                  OutputStream error)
            throws PPTException {

        log.debug(cmd);
        _client.sendRequest(cmd);
        boolean success = _client.getResponse(target,error);
        commandCount++;
        return success;
    }

    /**
     * Sends a single XML request document.
     * <p/>
     * The response is written to the output stream if one is specified,
     * otherwise the output is ignored.
     *
     * @param request The XML request that is sent to
     *            the BES to handle.
     *
     *
     * @return True if successful, false if the server returned an error.
     * @throws PPTException Thrown if there is a problem sending the request
     *                      to the server or a problem receiving the response
     *                      from the server.
     * @throws JDOMException if the response fails to parse.
     * @see String
     * @see PPTException
     */
    /*
    public Document sendRequest( Document request)
            throws PPTException, JDOMException {


        _client.sendXMLRequest(request);
        Document doc = _client.getXMLResponse();
        commandCount++;
        return doc;
    }
    */



    /**
     * Sends a single XML request document.
     * <p/>
     * The response is written to the output stream if one is specified,
     * otherwise the output is ignored.
     *
     * @param request The XML request that is sent to
     *            the BES to handle.
     *
     * @param target The target OutputStream for the results of the command.
     * @param error The error OutputStream for errors returned by the server.
     *
     * @return True if successful, false if the server returned an error.
     * @throws PPTException Thrown if there is a problem sending the request
     *                      to the server or a problem receiving the response
     *                      from the server.
     * @see String
     * @see PPTException
     */
    public boolean sendRequest( Document request,
                                OutputStream target,
                                OutputStream error)
            throws PPTException {


        _client.sendXMLRequest(request);
        boolean val = _client.getResponse(target,error);
        commandCount++;
        return val;
    }

    /**
     * Execute each of the commands in the cmd_list, separated by a * semicolon.
     * <p/>
     * The response is written to the output stream if one is specified,
     * otherwise the output is ignored.
     *
     * @param cmd_list The list of OpenDAP requests, separated by semicolons
     *                 and ending in a semicolon, that will be sent to the
     *                 OpenDAP server to handle, one at a time.
     * @param target The target OutputStream for the results of the command.
     * @param error The error OutputStream for errors returned by the server.
     *
     * @return True if successful, false if the server returned an error.
     * @throws PPTException Thrown if there is a problem sending any of the
     *                      request to the server or a problem receiving any
     *                      of the response
     *                      s from the server.
     * @see String
     * @see PPTException
     */
    public boolean executeCommands(String cmd_list,
                                   OutputStream target,
                                   OutputStream error)
            throws PPTException {

        boolean success = true;
        String cmds[] = cmd_list.split(";");
        for (String cmd : cmds) {
            success = executeCommand(cmd + ";", target, error);
            if(!success)
                return success;
        }
        return success;
    }

    /**
     * Sends the requests listed in the specified file to the OpenDAP server,
     * each command ending with a semicolon.
     * <p/>
     * The requests do not have to be one per line but can span multiple
     * lines and there can be more than one command per line.
     * <p/>
     * The response is written to the output stream if one is specified,
     * otherwise the output is ignored.
     *
     * @param inputFile The file holding the list of OpenDAP requests, each
     *                  ending with a semicolon, that will be sent to the
     *                  OpenDAP server to handle.
     * @param target The target OutputStream for the results of the command.
     * @param error The error OutputStream for errors returned by the server.
     *
     * @return True if successful, false if the server returned an error.
     * @throws PPTException Thrown if there is a problem opening the file to
     *                      read, reading the requests from the file, sending
     *                      any of the requests to the server or a problem
     *                      receiving any of the responses from the server.
     * @see File
     * @see PPTException
     */
    public boolean executeCommands(File inputFile,
                                OutputStream target,
                                OutputStream error) throws PPTException {
        BufferedReader reader;

        boolean success = true;


        try {
            reader = new BufferedReader(new FileReader(inputFile));
        }
        catch (FileNotFoundException e) {
            throw (new PPTException(e.getMessage()));
        }

        try {
            String cmd = null;
            boolean done = false;
            while (!done && success) {
                String nextLine = reader.readLine();
                if (nextLine == null) {
                    if (cmd != null) {
                        success = this.executeCommands(cmd,target,error);
                    }
                    done = true;
                } else {
                    if (!nextLine.equals("")) {
                        int i = nextLine.lastIndexOf(';');
                        if (i == -1) {
                            if (cmd == null) {
                                cmd = nextLine;
                            } else {
                                cmd += " " + nextLine;
                            }
                        } else {
                            String sub = nextLine.substring(0, i);
                            if (cmd == null) {
                                cmd = sub;
                            } else {
                                cmd += " " + sub;
                            }
                            success = this.executeCommands(cmd,target,error);
                            if (i == nextLine.length() || i == nextLine.length() - 1) {
                                cmd = null;
                            } else {
                                cmd = nextLine.substring(i + 1, nextLine.length());
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            throw (new PPTException(e.getMessage()));
        }
        finally {
        	try {
                reader.close();
            } catch (IOException e) {
                //Ignore the failure.
            }

        }
        return success;
    }

    /**
     * An interactive OpenDAP client that takes OpenDAP requests on the command
     * line.
     * <p/>
     * There can be more than one command per line, but commands can NOT span
     * multiple lines. The user will be prompted to enter a new OpenDAP request.
     * <p/>
     * OpenDAPClient:
     * <p/>
     * The response is written to the output stream if one is specified,
     * otherwise the output is ignored.
     *
     * @param out The target OutputStream for the results of the command.
     * @param err The error OutputStream for errors returned by the server.
     *
     * @throws PPTException Thrown if there is a problem sending any of the
     *                      requests to the server or a problem receiving any
     *                      of the responses from the server.
     * @see PPTException
     */
    public void interact(OutputStream out, OutputStream err) throws PPTException {
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        try {
            boolean done = false;
            while (!done) {
                System.out.print("OPeNDAP> ");
                String fromUser = stdIn.readLine();
                if (fromUser != null) {
                    if (fromUser.compareTo("exit") == 0) {
                        done = true;
                    } else if (fromUser.compareTo("") == 0) {
                        //continue;
                    } else {
                        this.executeCommands(fromUser,out, err);

                    }
                }
            }
        }
        catch (Exception e) {
            _client.closeConnection(true);
            throw (new PPTException(e.getMessage(), e));
        }
    }
}

