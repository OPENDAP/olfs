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
    private PPTClient _client = null;
    private OutputStream _stream = null;

    /**
     * Creates a OpenDAPClient to handle OpenDAP requests.
     * <p/>
     * Sets the output of any responses from the OpenDAP server to null,
     * meaning that all responses will be thrown out.
     */
    public OPeNDAPClient() {
        _stream = null;
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
        _client = new PPTClient(hostStr, portVal);
        _client.initConnection();
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
        _client.closeConnection();
        if (_stream != null) {
            try {
                _stream.close();
            }
            catch (IOException e) {
                throw(new PPTException(e.getMessage()));
            }
        }
    }

    /**
     * Set the output stream for responses from the OpenDAP server.
     * <p/>
     * Specify where the response output from your OpenDAP request will be
     * sent. Set to null if you wish to ignore the response from the OpenDAP
     * server.
     *
     * @param strm an OutputStream specifying where to send responses from
     *             the OpenDAP server. If null then the output will not be
     *             output but will be thrown away.
     * @throws PPTException catches any problems with opening or writing to
     *                      the output stream and creates a PPTException
     * @see OutputStream
     * @see PPTException
     */
    public void setOutput(OutputStream strm, boolean nice) throws PPTException {
        if(nice){
            if (strm != null) {
                try {
                    if (_stream != null) {
                        _stream.close();
                    }
                    _stream = strm;
                }
                catch (IOException e) {
                    throw(new PPTException(e.getMessage()));
                }
            } else {
                try {
                    if (_stream != null) {
                        _stream.close();
                    }
                    _stream = null;
                }
                catch (IOException e) {
                    throw(new PPTException(e.getMessage()));
                }
            }
        }
        else {
            _stream  = strm;

        }
    }

     /**
      * @deprecated
      * @param os
      * @throws PPTException
      */
     public void setOutput(OutputStream os) throws PPTException {
         setOutput(os,true);
     }




    /**
     * Set the output to the specified file for responses from the OpenDAP
     * server.
     * <p/>
     * Specify the file where the response output from your OpenDAP request
     * will be sent. Set to null if you wish to ignore the response from the
     * OpenDAP server.
     *
     * @param outputFile File where responses from the OpenDAP server will
     *                   be written. If null then the output will not be
     *                   output but will be thrown away.
     * @throws PPTException catches any problems with opening or writing to
     *                      the output file and creates a PPTException
     * @see File
     * @see PPTException
     */
    public void setOutput(File outputFile) throws PPTException {
        if (outputFile != null) {
            try {
                if (_stream != null) {
                    _stream.close();
                }
                _stream = new FileOutputStream(outputFile);
            }
            catch (FileNotFoundException e) {
                throw(new PPTException(e.getMessage()));
            }
            catch (IOException e) {
                throw(new PPTException(e.getMessage()));
            }
        } else {
            try {
                if (_stream != null) {
                    _stream.close();
                }
                _stream = null;
            }
            catch (IOException e) {
                throw(new PPTException(e.getMessage()));
            }
        }
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
     * @throws PPTException Thrown if there is a problem sending the request
     *                      to the server or a problem receiving the response
     *                      from the server.
     * @see String
     * @see PPTException
     */
    public void executeCommand(String cmd) throws PPTException {
        _client.sendRequest(cmd);
        _client.getResponse(_stream);
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
     * @throws PPTException Thrown if there is a problem sending any of the
     *                      request to the server or a problem receiving any
     *                      of the responses from the server.
     * @see String
     * @see PPTException
     */
    public void executeCommands(String cmd_list) throws PPTException {
        String cmds[] = cmd_list.split(";");
        for (int i = 0; i < cmds.length; i++) {
            executeCommand(cmds[i] + ";");
        }
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
     * @throws PPTException Thrown if there is a problem opening the file to
     *                      read, reading the requests from the file, sending
     *                      any of the requests to the server or a problem
     *                      receiving any of the responses from the server.
     * @see File
     * @see PPTException
     */
    public void executeCommands(File inputFile) throws PPTException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(inputFile));
        }
        catch (FileNotFoundException e) {
            throw(new PPTException(e.getMessage()));
        }

        try {
            String cmd = null;
            boolean done = false;
            while (!done) {
                String nextLine = reader.readLine();
                if (nextLine == null) {
                    if (cmd != null) {
                        this.executeCommands(cmd);
                    }
                    done = true;
                } else {
		    if( !nextLine.equals("") ) {
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
			    this.executeCommands(cmd);
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
            throw(new PPTException(e.getMessage()));
        }
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
     * @throws PPTException Thrown if there is a problem sending any of the
     *                      requests to the server or a problem receiving any
     *                      of the responses from the server.
     * @see PPTException
     */
    public void interact() throws PPTException {
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        try {
            boolean done = false;
            while (!done) {
                System.out.print("OPeNDAP> ");
                String fromUser = stdIn.readLine();
                if (fromUser.compareTo("exit") == 0) {
                    done = true;
                } else if (fromUser.compareTo("") == 0) {
                    //continue;
                } else {
                    this.executeCommands(fromUser);
                }
            }
        }
        catch (Exception e) {
            _client.closeConnection();
            throw(new PPTException(e.getMessage(),e));
        }
    }
}

