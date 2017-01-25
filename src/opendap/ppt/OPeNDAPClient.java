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
import opendap.xml.Util;
import org.apache.commons.cli.*;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public boolean isClosed() {
        return _client.isClosed();
    }

    public boolean isConnected() {
        return _client.isConnected();
    }

    public String showConnectionProperties()  {
        return _client.showConnectionProperties();
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
     * @param timeOut The number of milliseconds for the client to wait for the BES
     *                to reply before timing out.
     * @throws PPTException Thrown if unable to connect to the specified host
     *                      machine given the specified port.
     * @see String
     * @see PPTException
     */
    public void startClient(String hostStr, int portVal, int timeOut) throws PPTException {

        int paddedTimeout = (int)(timeOut * 1.5);

        // paddedTimeout = 10000;
        
        _client = new NewPPTClient(hostStr, portVal, paddedTimeout);
        _client.initConnection();
        _isRunning = true;
    }

    /**
     * Closes the connection to the Back End Server and closes the output stream.
     *
     * @throws PPTException Thrown if unable to close the connection or close
     *                      the output stream.
     *                      machine given the specified port.
     * @see OutputStream
     * @see PPTException
     */
    public void shutdownClient() throws PPTException {

        shutdownClient(true);
    }

    /**
     * Closes the connection to the Back End Server and closes the output stream.
     *
     * @throws PPTException Thrown if unable to close the connection or close
     *                      the output stream.
     *                      machine given the specified port.
     * @see OutputStream
     * @see PPTException
     */
    public void shutdownClient(boolean beNice) throws PPTException {

        if(_client!=null)
            _client.closeConnection(beNice);

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


    public int getChunkedReadBufferSize(){
        return _client.getChunkReadBufferSize();
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
            reader = new BufferedReader( new InputStreamReader( new FileInputStream(inputFile), HyraxStringEncoding.getCharset()));
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
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in, HyraxStringEncoding.getCharset()));
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



    private static Options createCmdLineOptions(){

        Options options = new Options();

        options.addOption("r", "reps",    true, "Number of times to send the command. default: 1");
        options.addOption("c", "maxCmds", true, "Number of commands to send before closing the BES connection and opening a new one. default: 1");
        options.addOption("i", "besCmd",  true, "Name of file containing the BES command to use. default: \"bes.cmd\"");
        options.addOption("p", "port",    true, "Port number of BES. default: 10022");
        options.addOption("t", "timeOut", true, "Timeout (in seconds) for the BES connection. (300)");
        options.addOption("n", "host",    true, "Hostname of BES. default \"localhost\"");
        options.addOption("o", "outFile", true, "File into which to log BES responses. default: stdout");
        options.addOption("e", "errFile", true, "File into which to log BES errors. default: stderr");
        options.addOption("h", "help",    false, "Print this usage statement.");

        return options;

    }


    /**
     *
     * @param args  Command line arguments as defined by  createCmdLineOptions()
     */
    public static void main(String[] args) {

        Logger log = LoggerFactory.getLogger("OPeNDAPClient-MAIN");


        String besCmdFileName = "bes.cmd";

        String cmdString;
        int reps = 1;
        int maxCmds = 1;

        OutputStream besOut = System.out;
        OutputStream besErr = System.err;
        String hostName = "localhost";
        int portNum = 10022;
        int timeOut = 300000; // 5 minutes in ms

        try {
            Options options = createCmdLineOptions();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            //---------------------------
            // Command File
            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(120);
                formatter.printHelp( "OPeNDAPClient", options );
                return;
            }

            //---------------------------
            // Command File
            if (cmd.hasOption("i")) {
                besCmdFileName = cmd.getOptionValue("i");
            }
            log.info("BES Command Filename: "+besCmdFileName);
            Document cmdDoc = Util.getDocument(besCmdFileName);
            cmdString = new XMLOutputter(Format.getPrettyFormat()).outputString(cmdDoc);
            log.info("BES command has been read and parsed.");

            //---------------------------
            // Command reps
            if (cmd.hasOption("r")) {
                reps = Integer.parseInt(cmd.getOptionValue("r"));
            }
            log.info("BES command will sent "+reps+" time" + (reps>1?"s.":"."));

            //---------------------------
            // Max commands per client
            if (cmd.hasOption("c")) {
                maxCmds = Integer.parseInt(cmd.getOptionValue("c"));
            }
            if(maxCmds<1){
                log.error("main() - The variable maxCmds has been set (via the -c option) to an unacceptable value of {}. Resetting to 1.",maxCmds);
                maxCmds = 1;
            }
            log.info("The connection to the BES will be dropped and a new one opened after every "
                    + maxCmds+" command" + (maxCmds>1?"s.":"."));

            //---------------------------
            // BES output file
            if (cmd.hasOption("o")) {
                File besOutFile = new File(cmd.getOptionValue("o"));
                besOut = new FileOutputStream(besOutFile);
                log.info("BES output will be written to "+besOutFile.getAbsolutePath());
            }
            else {
                log.info("BES output will be written to stdout");
            }

            //---------------------------
            // BES error file
            if (cmd.hasOption("e")) {
                File besErrFile = new File(cmd.getOptionValue("e"));
                besErr = new FileOutputStream(besErrFile);
                log.info("BES errors will be written to "+besErrFile.getAbsolutePath());
            }
            else {
                log.info("BES errors will be written to stderr");
            }

            //---------------------------
            // Hostname
            if (cmd.hasOption("n")) {
                hostName = cmd.getOptionValue("n");
            }

            //---------------------------
            // Port Number
            if (cmd.hasOption("p")) {
                portNum = Integer.parseInt(cmd.getOptionValue("p"));
            }
            log.info("Using BES at "+hostName+":"+portNum);

            //---------------------------
            // TimeOut
            if (cmd.hasOption("t")) {
                timeOut = Integer.parseInt(cmd.getOptionValue("t")) * 1000;
            }
            log.info("BES timeout set to at "+timeOut/1000+ " seconds");


        }
        catch(Throwable t){
            log.error("OUCH! Caught "+t.getClass().getName()+" Message: "+t.getMessage());
            log.error("STACK TRACE: \n"+org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t));

            try {
                if(besOut!=null){
                    besOut.close();
                }
            } catch (IOException e) {
                log.error("FAILED TO CLOSE OUTPUT LOG.");
            }
            try {
                if(besErr!=null){
                    besErr.close();
                }
            } catch (IOException e) {
                log.error("FAILED TO CLOSE ERROR LOG.");
            }

            return;
        }

        int cmdsSent = 0;
        int connectionsMade = 0;
        try {

            log.info("-------------------------------------------------------------------------------");
            log.info("-------------------------------------------------------------------------------");
            log.info("Starting... \n\n\n");

            OPeNDAPClient oc = new OPeNDAPClient();
            oc.startClient(hostName,portNum,timeOut);
            connectionsMade++;
            for(int r=0; reps==0 || r<reps ;r++){

                if(r>0 && r%maxCmds==0){
                    oc.shutdownClient();

                    boolean done = false;
                    while(!done){
                        oc = new OPeNDAPClient();
                        try {
                            oc.startClient(hostName,portNum,timeOut);
                            done = true;
                        }
                        catch(PPTEndOfStreamException e){
                            log.error("Caught PPTEndOfStreamException - This BES connection is screwed. Retrying...");
                        }

                    }

                    connectionsMade++;
                }
                oc.executeCommand(cmdString,besOut,besErr);
                cmdsSent++;
            }

            if(oc.isRunning()){
                oc.shutdownClient();
            }
        }
        catch(Throwable t){
            log.error("OUCH! Caught "+t.getClass().getName()+" Message: "+t.getMessage());
            try {
                log.error("STACK TRACE: \n"+org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t));
            }
            catch (Throwable tt){
                log.error("**** FAILED TO GENERATE STACK TRACE! ****");
            }
        }
        finally {
            log.info("BES Command Filename: "+besCmdFileName);
            log.info("BES command will sent "+reps+" time" + (reps>1?"s.":"."));
            log.info("The connection to the BES will be dropped and a new one opened after every "
                    + maxCmds+" command" + (maxCmds>1?"s.":"."));
            log.info("Using BES at "+hostName+":"+portNum);

            log.info("Sent a total of "+cmdsSent+" command"+(connectionsMade>1?"s.":"."));
            log.info("Made a total of " + connectionsMade + " connection"+(connectionsMade>1?"s":"")+" to the BES.");

        }

        try {
            if(besOut!=null){
                besOut.close();
            }
        } catch (IOException e) {
            log.error("FAILED TO CLOSE OUTPUT LOG.");
        }
        try {
            if(besErr!=null){
                besErr.close();
            }
        } catch (IOException e) {
            log.error("FAILED TO CLOSE ERROR LOG.");
        }


    }
}
