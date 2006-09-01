/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2005 OPeNDAP, Inc.
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

package opendap.niotest;

import java.io.*;
import java.util.*;
import java.net.*;
import java.text.DecimalFormat;

import gnu.getopt.*;

/**
 * <p>The IPLogger class logs data collected from a TCP/IP socket connection to a file.</p>
 * <h3> This class is not thread safe.</h3>
 * In general this class can be used as a simple command line tool to log data to a file over a TCP/IP
 * socket. Additional API methods are available to allow it to be used as a logging thread in a larger application.
 * When using IPLogger in such a manner it is important to carefully set all of the configuration parameters prior
 * to starting the logging process. In a threaded world the logging process would be started like so:
 * <pre>
 *      IPlogger ipl = new IPLogger("some.ip.address",port,file,mode);
 *      new Thread(ipl).start;
 * </pre>
 * Which will cause the IPLogger.run() method to be called. You can, if you wish, attempt to manipulate the
 * other methods to get it running, but at that point I suggest you read the source code for IPLogger.run() so you don't
 * break things.
 * <br> The use of the IPLConfig object for construction and configuration is also encouraged.
 *
 */
public class Receiver implements Runnable, IPLConfigReader {


    /**
     * Use to indicate the RAW logging mode.
     */
    public static final int RAW = 0;

    /**
     * Use to indicate the INTERPRETED logging mode.
     */
    public static final int INTERPRETED = 1;

    /**
     * Use to indicate tan integer value is not set logging mode.
     */
    public static final int NOTSET = -1;


    private Socket          targetSocket;
    private DataInputStream targetData;
    private PrintWriter     targetControl;

    //private int recordCount = 0;
    DecimalFormat dformat = null;

    private boolean runnning;

    private IPLConfig iplc;
    private int syncIndex = 0;







    /**
     * Default constructor made private so lunk heads can't break things.
     */
    private IPLogger() {

        iplc = new IPLConfig();

        dformat = new DecimalFormat();
        dformat.applyPattern("#00000");

        runnning = false;
    }


    /**
     * This constructor is utilized by the command line invocation of this class.
     * It expects it's configuration information
     * to be passed in via command line arguments.
     * @param args Command line arguments from the method main().
     * @throws BadArgException
     */
    public IPLogger(String args[]) throws BadArgException, IOException {

        this();


        if(!processCmdLine(args))
            throw new BadArgException("Bad command line argument!\n");


        if(getOutputMode()== NOTSET)
            iplc.setOutputMode(INTERPRETED);

        if(getOutputMode()== INTERPRETED || getEndOfRecordBlock()==null){
            byte b[] = {(byte) 0x0d, (byte) 0x0a};
            iplc.setEndOfRecordBlock( b );
        }

        if(getLogFileName()==null)
            iplc.setLogFileName(autoFileName());



    }

    /**
     * This is a most useful constructor. It takes as it's parameters all of the configuration information
     * necessary to run the the logger.
     * @param ipAddr A string containing the hostname or ip address of the target system from which to log data.
     * @param portNum An int containing the port number to connect to on the target system.
     * @param mode The logging mode, either <code>RAW or INTERPRETED</code>
     * @param fname The name of the file to which to log data.
     * @param eorBlock The End Of Record block to use if logging data in INTERPRETED mode. Ignored (may be null)
     * if using raw mode.
     * @throws BadArgException
     * @see #RAW
     * @see #INTERPRETED
     */
    public IPLogger(String ipAddr, int portNum, int mode, String fname, byte[] eorBlock) throws BadArgException {
        this();
        iplc.setTargetIP(ipAddr);
        iplc.setTargetPort(portNum);
        iplc.setOutputMode(mode);
        iplc.setLogFileName(fname);
        iplc.setEndOfRecordBlock(eorBlock);
    }



    /**
     * This is a most useful constructor. It takes as it's parameter an already configured IPLConfig object.
     * The configuration is copied from passed parameter into a private instance of the configuration. Thus the
     * passed parameter may be reused by the calling code without impact on a particular instance of IPLogger.
     * Assuming that the passed config object contains a valid config, the returned IPLogger should be ready to "run()"
     * @param iplconfig A pre-configured configuration for this logger.
     * #see IPLConfig
     */
    public IPLogger(IPLConfig iplconfig){
        this();
        try {
            iplc = (IPLConfig) iplconfig.clone();
        } catch (CloneNotSupportedException cnse) {
            System.err.println("We should never get here because we know the IPLConfig supports clones!");
            cnse.printStackTrace(System.err);
        }
    }



    /**
     * This is a most useful constructor. It takes as it's parameter a file name that references the persistent
     * representation (an XML JavaBean) of an IPLConfig object. It attempts to the IPLConfig object from the file
     * indicated. If successful, then the IPLogger is configured and ready to use.
     *
     * @param fname The name of a file containing the persistent representation (an XML JavaBean) of an
     * IPLConifg object.
     * #see IPLConfig
     */
    public IPLogger(String fname) throws IOException {
        this();
        iplc = IPLConfig.loadConfig(fname);
    }





    /**
     *
     * @return A String containing an ID for an instance of IPLogger.
     */
    public String getLoggerID(){
        return iplc.getLoggerID();
    }


    /**
     * Returns the name of the file to which this IPLogger will be writing data. If the value is null then the
     * setLogFileName() method needs to be called.
     * @return The log file name.
     */
    public String getLogFileName(){
        return iplc.getLogFileName();
    }


    /**
     * Returns the output mode that this instance of IPLogger will use.
     *
     * @return The output mode, NOTSET if the mode has not been set.
     * @see #RAW
     * @see #INTERPRETED
     * @see #NOTSET
     */
    public int getOutputMode(){
        return iplc.getOutputMode();
    }


    /**
     * Returns the IP (or hostname) address of the system to which IPLogger will connect to log data.
     * @return The IP address of the target system.
     */
    public String getTargetIP(){
        return iplc.getTargetIP();
    }


    /**
     *
     * @return Returns the port number on the target system to which IPLogger will connect to.
     */
    public int getTargetPort(){
        return iplc.getTargetPort();
    }

    /**
     * Generates a file name based on the date and time.
     * @return The file name.
     */
    public static String autoFileName(){
        SimpleTimeZone tz = new SimpleTimeZone(0,"GMT");
        System.err.println("Operational TimeZone: " + tz.getID());
        DecimalFormat cf = new DecimalFormat();
        cf.applyPattern("00");

        Calendar c = Calendar.getInstance(tz);
        String s =
                cf.format(c.get(Calendar.YEAR)) +
                cf.format(c.get(Calendar.MONTH) + 1) +
                cf.format(c.get(Calendar.DAY_OF_MONTH)) +
                cf.format(c.get(Calendar.HOUR_OF_DAY)) +
                cf.format(c.get(Calendar.MINUTE)) +
                cf.format(c.get(Calendar.SECOND)) + "-IPLogger.txt";
        return(s);
    }
    /**
     *
     * @return A copy of the current End Of Record block.
     */
    public byte [] getEndOfRecordBlock(){
        return iplc.getEndOfRecordBlock();
    }


    /**
     * Use this method to stop the IPLogger once it's running. Calling this method will stop logging,
     * close the socket connection and close the file. Once the IPLogger has been stopped it cannot be
     * restarted. If you want to log some more stuff, make a new IPLogger.
     */
    public  void stop(){ runnning = false; }

    /**
     * This private method sets the running flag. Once set logging will continue until the stop() method
     * is called.
     */
    private  void start(){ runnning = true; }

    /**
     *
     * @return A boolean indicating the run state of the IPLogger instance.
     */
    public boolean isRunning(){ return runnning; }


    /**
     * The run method will start the logger. Calling this will:
     * <ul>
     * <li> Open the data file (File name from IPLConfig)</li>
     * <li> Connect to the target system (hostname/ip_address  and port from IPLConfig)</li>
     * <li> Cause data to be collected from the target system and written to the log file using either
     * RAW or INTERPRETED mode (determined by IPLConfig)
     * </ul>
     * Once running the logger can be stopped by calling the stop() method. The running state of the IPLogger
     * can be checked using the isRunning() method.
     *
     * @see #stop()
     * @see #isRunning()
     */
    public void run(){

        try {
            System.err.println(getLoggerID()+"Has Started.");

            if(getOutputMode() == RAW){

                FileOutputStream fos = new FileOutputStream(new File(getLogFileName()));
                try {
                    connect();
                    logRawData(fos);
                }
                finally {
                    fos.flush();
                    fos.close();
                    disconnect();
                }
            }
            else{

                PrintStream ps;
                if(getLogFileName().equalsIgnoreCase("stdout") || getLogFileName().equals("-")){
                    ps = System.out;
                }
                else {
                    ps = new PrintStream(new FileOutputStream(new File(getLogFileName())));
                }

                try {
                    connect();
                    logInterpretedData(ps);
                }
                finally {
                    ps.flush();
                    ps.close();
                    disconnect();
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);

        }
        finally{
            System.err.println(getLoggerID()+"Has Stopped.");
        }


    }


    /**
     * Connects the logger to the target system. This MUST be called prior to attempting to read data.
     *
     * @throws IOException
     */
    public void connect() throws IOException {
        System.err.print(getLoggerID()+"Connecting to " + getTargetIP() + ":" + getTargetPort()+"... ");

        InetAddress ia = InetAddress.getByName(getTargetIP());

        targetSocket = new Socket(ia, getTargetPort());

        if(targetSocket.isConnected())
            System.err.println("Done.");
        else
            System.err.println("Connection Failed!\n\n");


        targetData = new DataInputStream(targetSocket.getInputStream());
        targetControl = new PrintWriter(targetSocket.getOutputStream());

    }

    /**
     * Disconects the logger from the target system. This should ALWAYS be called when logging is complete.
     */
    public void disconnect() {
        System.err.println(getLoggerID()+"Disconnecting from " + getTargetIP() + ":" + getTargetPort());

        try {
            targetData.close();
            targetControl.flush();
            targetControl.close();
            targetSocket.close();
        } catch (Exception e) {
            System.err.println("Problem disconnecting from IPLogger.");
            e.printStackTrace(System.err);
        }
    }


    /**
     * Logs the data from the target in RAW mode. This will pass the data to the log
     * OutputStream with no modifcations.
     *
     * @param os The OutputStream to which to write the data.
     * @throws IOException
     * @throws InterruptedException
     */

    public void logRawData(OutputStream os) throws IOException, InterruptedException{
        byte b;

        System.err.println(getLoggerID()+"Logging in RAW mode.");

        start();
        while(isRunning()){
            while (targetData.available() < 1) {
                Thread.sleep(1);
            }
            b = targetData.readByte();
            os.write(b);
        }

    }


    /**
     * Logs the data from the target in INTERPRETED mode. This will change byte values that are not printable
     * as ASCII (values less than 32 and greater than 126) to an ASCII representation of their hexadecimal values
     * encased in square brackets. Additionally, if a CR/LF (0x0d0a) pair is detected then the logger will inject
     * a localized new line ("\n") into the file.
     *
     * @param ps
     * @throws IOException
     * @throws InterruptedException
     */
    public void logInterpretedData(PrintStream ps) throws IOException, InterruptedException, BadArgException {

        byte val;

        System.err.println(getLoggerID()+"Logging in INTERPRETED mode.");

        if(getEndOfRecordBlock() == null)
            throw new BadArgException("You Cannot log data in INTERPRETED mode and have a null End Of Record block!");

        start();
        while (isRunning()) {

            while (targetData.available() < 1) {

                Thread.sleep(1);
            }
            val = targetData.readByte();

            if( (val<32 ) || (val > 126)){
                ps.print("[" + Integer.toHexString(0x00ff & val) + "]");
            }
            else
                ps.print(String.valueOf((char)val));

            if(instrumentSyncCheck(val)){
                //ps.print("\n"+ ++recordCount + " ");
                ps.print("\n");
            }
        }


    }


    /**
     * Provides frame synchronization checking for data coming from an Instrument
     *
     * @return True if the End Of Record mark has been detected, False otherwise.
     */

    private boolean instrumentSyncCheck(byte b) {

        //System.err.println("syncIndecx: "+syncIndex);
        if (getEndOfRecordBlock()[syncIndex] == b) {
            syncIndex++;
            if (syncIndex == getEndOfRecordBlock().length) {
                syncIndex = 0;
                return (true);
            }
        } else {
            syncIndex = 0;
        }
        return false;
    }




    private boolean processCmdLine(String args[]) throws BadArgException, IOException {

        Getopt g = new Getopt("IPLogger command line: ", args, "ro:i:p:c:");
        //
        int c;
        String arg;
        while ((c = g.getopt()) != -1) {

            //System.err.println("getopt() returned " + c);
            switch (c) {

                case 'c':
                    iplc = IPLConfig.loadConfig(g.getOptarg());
                    break;

                case 'r':
                    System.err.println("Using RAW output format!");
                    iplc.setOutputMode(RAW);
                    break;

                case 'o':
                    arg = g.getOptarg();
                    System.err.println("Log File Name: "+ arg);
                    iplc.setLogFileName(arg);
                    break;

                case 'i':
                    arg = g.getOptarg();
                    System.err.println("Target Host IP Address: "+ arg);
                    iplc.setTargetIP(arg);
                    break;

                case 'p':
                    arg = g.getOptarg();
                    System.err.println("Target Host Port NUmber: "+ arg);
                    iplc.setTargetPort(Integer.parseInt(arg));
                    break;

                case '?':
                default:
                    usage();
                    return false;
            }
        }
        return true;

    }


    private void usage(){
        System.err.println("");
        System.err.println("");
        System.err.println("NAME");
        System.err.println("    ipl.jar -- An excecutable jar file that is used to log data collected");
        System.err.println("                from a socket connection using the IPLogger class.");
        System.err.println("");
        System.err.println("SYNOPSIS");
        System.err.println("    ipl.jar [[-r] [-o fname] -i ip -p pnum ] || [-c fname]");
        System.err.println("");
        System.err.println("DESCRIPTION:");
        System.err.println("    Reads data from a given port at a given IP address and logs the data to");
        System.err.println("    a file on disk. Two logging modes ara supported an INTERPRETED mode and a");
        System.err.println("    RAW mode.");
        System.err.println("");
        System.err.println("    The default mode is INTERPRETED. In INTERPRETED mode the code operates under");
        System.err.println("    the assumption that the received data is ASCII. It prints out the ASCII char-");
        System.err.println("    acters whose byte values are between 32 and 126. For all other byte values the ");
        System.err.println("    base 16 numerical value is printed inside of a pair of square brackets. For");
        System.err.println("    example if a byte value of 10 is received, then it would be logged as \"[a]\"");
        System.err.println("    Additionally the interpreted mode assumes that when it detects a CR LF pair,");
        System.err.println("    (byte values 13 and 10) that it should log them as [d][a] and print a localized");
        System.err.println("    new line character to the log file.");
        System.err.println("    ");
        System.err.println("    In RAW mode the content of the stream from the socket is logged directly to the");
        System.err.println("    disk file. No modifications are made.");
        System.err.println("    ");
        System.err.println("    Launching the IPLogger can be done in two ways:");
        System.err.println("        1) Pedantically - by specifing on the command line the hostname/ip address, port");
        System.err.println("           number, logging mode and output file name.");
        System.err.println("        2) By specifying on the command line the name of a file containing the peristent");
        System.err.println("           representation (as an XML JavaBean) of an IPLConfig object.");
        System.err.println("    ");
        System.err.println("    The two methods are not mutually exclusive. The command line arguments are processed");
        System.err.println("    in the order that they occur on the command line. Thus if an option occurs twice, or");
        System.err.println("    a later option overrides an earlier one the last one processed is the one that takes");
        System.err.println("    effect. SO BE CAREFUL!");
        System.err.println("    ");
        System.err.println("    ");
        System.err.println("    ");
        System.err.println("    The following options are available:");
        System.err.println("    ");
        System.err.println("    -r    Log data in RAW mode. If not present data will be logged in INTERPRETED mode.");
        System.err.println("    ");
        System.err.println("    -o    Use to specify an output file name. If the name specifed is equal to \"stdout\"");
        System.err.println("          or to \"-\" then the log output will be directed to the terminal via stdout.");
        System.err.println("          If no name is specified a name will be automatically generated using the date");
        System.err.println("          and time.");
        System.err.println("");
        System.err.println("    -i ip      Where ip is the hostname or ip address of the target system from which data");
        System.err.println("               will be logged.");
        System.err.println("");
        System.err.println("    -p pnum    Where pnum is the port number on the target system to which the logger will");
        System.err.println("               connect to receive data.");
        System.err.println("");
        System.err.println("    -c fname   Where fname is the name of a file containing the peristent representation");
        System.err.println("               (as an XML JavaBean) of an IPLConfig object.");
        System.err.println("");
        System.err.println("");
        System.err.println("");
        System.err.println("");
        System.err.println("");


    }

    public static void main(String args[]) {

        try {
            IPLogger ipl = new IPLogger(args);
            Thread loggerThread =new Thread(ipl);

            loggerThread.start();

            Thread.sleep(1000);

            IPLogger.console(ipl);

        }

        catch (Exception e){
            System.err.println(e.getMessage());
        }

    }


    public static void console(IPLogger ipl) throws IOException {

        byte b[] = new byte[256];
        int count;
        boolean done = false;

        while(!done){

            System.err.println("");
            System.err.println("");
            System.err.println("");
            System.err.println("-------------------------------------------------------------------------------------");
            System.err.println(ipl.getLoggerID()+" is "+ (ipl.isRunning()?"running":"not running."));
            System.err.println("");
            System.err.println("");
            System.err.println("Enter q/Q and press Enter/Return to stop logging and quit");
            System.err.println("");
            System.err.println("");
            System.err.print  (">> ");

            count = System.in.read(b);

            for(int i=0; i<count; i++){
                switch(b[i]){
                    case 'q':
                    case 'Q':
                        ipl.stop();
                        done = true;
                        break;
                    default:
                        break;
                }
            }

        }


    }



    public String toString(){
        String s;

        s = "\n-------------------------------------------------------------------------------------------------\n";
        s += "IPLogger:\n";
        s += "\n";
        s += "     Target Host Name:    " + getTargetIP() + "\n";
        s += "     Target Host Port:    " + getTargetPort() + "\n";
        s += "\n";
        s += "     Logger Output Mode:  " + iplc.getModeString() + "\n";
        s += "     End Of Record Block: ";

        if(getOutputMode() == IPLogger.INTERPRETED){
            s+= "{ ";
            byte block[] = getEndOfRecordBlock();
            for(int i=0; i<block.length ;i++){
                if(i>0)
                    s+= ", ";
                s+= block[i];
            }
            s+= " }\n";
        }
        else
            s+= "Not Used In RAW mode.\n";

        s += "\n";
        s += "     Log File Name:       " + getLogFileName()+"\n";
        s += "\n";
        s += "     LoggerID:            " + getLoggerID() + "\n";

        s += "-------------------------------------------------------------------------------------------------\n";

        return (s);

    }






}