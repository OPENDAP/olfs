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
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.rmi.server.UID;

/**
 * Holds the configuration information for the IPLogger class. This implementation provides a command line (console)
 * interface for build a config and saving it as an XML JavaBEan to a disk file. Additionally, a complete API is
 * available for programmatically building a configuration.
 */
public class IPLConfig implements IPLConfigReader, Serializable, Cloneable {

    private String targetIP;
    private int    targetPort;
    private int    outputMode;
    private byte[] endOfRecordBlock;
    private String fileName;
    private UID    uid;




    public IPLConfig(){
        uid = new UID();
        setTargetIP(null);
        setTargetPort(IPLogger.NOTSET);
        outputMode = IPLogger.NOTSET; // Using direct access to avoid a BadArgException.
        setEndOfRecordBlock( null );
        setLogFileName(null);
    }


    /**
     * Uses the passed IPLConfigReader to configur a new instance of IPLConfig.
     * @param iplcr
     */
    public IPLConfig(IPLConfigReader iplcr){
        uid = new UID();
        setTargetIP(iplcr.getTargetIP());
        setTargetPort(iplcr.getTargetPort());
        try {
            setOutputMode(iplcr.getOutputMode());
        } catch (BadArgException e) {
            e.printStackTrace();
        }
        if(getOutputMode()==IPLogger.INTERPRETED)
            setEndOfRecordBlock(iplcr.getEndOfRecordBlock());
        setLogFileName(iplcr.getLogFileName());
    }

    /**
     * Loads a IPLConfig (stored as a XML JavaBean) from a file.
     * @param fname The file containing the XML JavaBEan representation of the IPLConfig.
     * @throws IOException
     */
    public IPLConfig(String fname) throws IOException {
        uid = new UID();
        loadConfig(fname);
    }


    /**
     * Mmmmmmm... Clones...
     * @return A clone of this instance of IPLConfig
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException {

        IPLConfig copy = null;
        try {
            copy             = (IPLConfig) super.clone();
            copy.uid         = new UID();
            copy.targetIP    = this.getTargetIP();
            copy.targetPort  = this.getTargetPort();
            copy.fileName    = this.getLogFileName();
            copy.outputMode  = this.getOutputMode();
            copy.setEndOfRecordBlock(this.getEndOfRecordBlock()); // Copies the array content and not the reference.
        }
        catch (CloneNotSupportedException cnse){
            System.err.println("We should never get here because we support clones!");
            cnse.printStackTrace(System.err);

        }
        return copy;
    }


    /**
     * @return A string representation of the current logging mode.
     */
    public String getModeString(){
        String mode;
        switch(outputMode){
            case IPLogger.RAW:
                mode = "RAW";
                break;
            case IPLogger.INTERPRETED:
                mode = "INTERPRETED";
                break;
            default:
                mode = "NOT SET";
                break;
        }
        return mode;
    }

    /**
     * @return An ID string for the logger.
     */
    public String getLoggerID(){
        return "[ IPLogger ("+uid+") ("+targetIP+":"+targetPort+"-->"+fileName+") mode="+getModeString() +" ] ";
    }



    /**
     * Returns the name of the file to which this IPLogger will be writing data. If the value is null then the
     * setLogFileName() method needs to be called.
     * @return The log file name.
     */
    public String getLogFileName(){
        return fileName;
    }

    /**
     * Sets the name of the file to which data should be logged.
     * @param name The name of the log file.
     */
    public void setLogFileName(String name)  {

        fileName = name;
    }

    /**
     * Returns the output mode that this instance of IPLogger will use.
     *
     * @return The output mode, -1 if the mode has not been set.
     * @see IPLogger.RAW
     * @see IPLogger.INTERPRETED
     */
    public int getOutputMode(){
        return outputMode;
    }

    /**
     * Sets the output mode. The only acceptable values are: RAW or INTERPRETED
     * @param mode The output mode.
     * @throws BadArgException If mode has already been set, or if it is set to value other than 0 or 1.
     * @see IPLogger.RAW
     * @see IPLogger.INTERPRETED
     */
    public void setOutputMode(int mode) throws BadArgException {

        if(mode==IPLogger.RAW || mode==IPLogger.INTERPRETED) outputMode = mode;
        else throw new BadArgException("Output Mode May Only Be Set to 0 (zero) or 1 (one)! RTFM!");
    }

    /**
     * Returns the IP (or hostname) address of the system to which IPLogger will connect to log data.
     * @return The IP address of the target system.
     */
    public String getTargetIP(){
        return targetIP;
    }

    /**
     * Sets the address of the system to which IPLogger will connect and log data.
     * @param ip The address. It may be a hostname or IP address.
     */
    public void setTargetIP(String ip)  {
        targetIP = ip;
    }

    /**
     *
     * @return Returns the port number on the target system to which IPLogger will connect to.
     */
    public int getTargetPort(){
        return targetPort;
    }

    /**
     * Sets the port number on the target system to which IPLogger will connect to.
     * @param port The port number. Choose wisely.
     */
    public void setTargetPort(int port)  {
        targetPort = port;
    }


    /**
     * DANGER! Do not alter the contents of the returned array. It is not a copy.
     * @return A reference to the current End Of Record block.
     */
    public byte[] getEndOfRecordBlock(){
        return endOfRecordBlock;
    }

    /**
     * Copies the values of the passed array into a new End Of Record block.
     * @param block The values for the new End Of Record block.
     */
    public void setEndOfRecordBlock(byte block[]){
        if( block!= null) {
            endOfRecordBlock = new byte[block.length];
            System.arraycopy(block,0,endOfRecordBlock,0,endOfRecordBlock.length);
        }
        else
            endOfRecordBlock = null;
    }

/*
    public byte getEndOfRecordBlock(int index){
        if(endOfRecordBlock != null && index<endOfRecordBlock.length)
            return endOfRecordBlock[index];
        return -1;
    }

    public void setEndOFRecordBlock(int index, byte newValue){
        endOfRecordBlock[index] = newValue;
    }

*/


//----------------------------------------------------------------------------------------------------------------------
//
//                          Serialization (Persistence) Routines
//
// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    /**
     * Load a <code>IPLConfig</code> from the filen name passed
     * @param fname A <code>String</code> containing the file name
     * of (a XML representation of) a <code>IPLConfig</code> object.
     */
    public static IPLConfig loadConfig(String fname) throws IOException {

        XMLDecoder d = new XMLDecoder(
                       new BufferedInputStream(
                       new FileInputStream(fname)));

        Object result = d.readObject();
        d.close();
        //System.out.println("It built a "+result.getClass().getName());

        return (IPLConfig) result;

    }

    /**
     * Load an array of <code>IPLConfig</code> objects from the filen name passed
     * @param fname A <code>String</code> containing the file name
     * of (a XML representation of) an array of <code>IPLConfig</code> objects.
     */
    public static IPLConfig[] loadConfigArray(String fname) throws IOException {

        XMLDecoder d = new XMLDecoder(
                       new BufferedInputStream(
                       new FileInputStream(fname)));

        Object result = d.readObject();
        d.close();
        //System.out.println("It built a "+result.getClass().getName());

        return (IPLConfig[]) result;

    }


    /**
     * Parse a <code>IPLConfig</code> from the passed <code>String</code>
     * @param config A <code>String</code> containing an XML representation of
     * a <code>IPLConfig</code> object.
     */
    public static IPLConfig parseConfig(String config)  {

        XMLDecoder d =  new XMLDecoder(
                        new BufferedInputStream(
                        new ByteArrayInputStream(config.getBytes())));

        Object result = d.readObject();
        d.close();
        //System.out.println("It built a "+result.getClass().getName());

        IPLConfig dcc = (IPLConfig) result;

        return (dcc);

    }


    /**
     * Save this <code>IPLConfig</code> (as XML) to a file named after
     * the passed <code>String</code>
     * @param fname A <code>String</code> containing the name of the file in which
     * to write the XML representation of this <code>IPLConfig</code>
     */
    public void saveConfig(String fname) throws IOException {

        saveConfig(this,fname);
    }


    /**
     * Save the passed <code>IPLConfig</code> (as XML) to a file named after
     * the passed <code>String</code>
     * @param cc A <code>IPLConfig</code> to be saved (as XML).
     * @param fname A <code>String</code> containing the name of the file in which
     * to write the XML representation of this <code>IPLConfig</code>
     */
    public static void saveConfig(IPLConfig cc, String fname) throws IOException {

        System.out.println("\n\nWriting config to file: " + fname);

        // System.out.println(this);

        XMLEncoder e = new XMLEncoder(
                       new BufferedOutputStream(
                       new FileOutputStream(fname,false)));
        e.writeObject(cc);
        e.close();

    }

    /**
     * Save the passed array of <code>IPLConfig</code> objects (as XML) to a file named after
     * the passed <code>String</code>
     * @param cc An array of <code>IPLConfig</code> objects to be saved (as XML).
     * @param fname A <code>String</code> containing the name of the file in which
     * to write the XML representation of this <code>IPLConfig</code>
     */
    public static void saveConfigArray(IPLConfig[] cc, String fname) throws IOException {

        System.out.println("\n\nWriting config to file: " + fname);

        // System.out.println(this);

        XMLEncoder e = new XMLEncoder(
                       new BufferedOutputStream(
                       new FileOutputStream(fname,false)));
        e.writeObject(cc);
        e.close();

    }











//----------------------------------------------------------------------------------------------------------------------


//----------------------------------------------------------------------------------------------------------------------
//
//                    Initialization Routines
//
// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    /**
     * Provides a console interface to initialize (or configure if you will) an IPLConfig object, which provides
     * configuration information for an IPLogger.
     * @throws IOException
     */
    public void configure() throws IOException {
        configure(this);
    }

    /**
     * Provides a console interface to initialize (or configure if you will) the passed IPLConfig object, which provides
     * configuration information for an IPLogger. After the intialization is complete, the user will be prompted to
     * save the configuration into a file (as an XML java bean).
     * @param iplc The IPLConfig to initialize.
     * @throws IOException
     */
    public static void configure(IPLConfig iplc) throws IOException {
        boolean done = false;
        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));

        while(!done){

            System.out.println("\n\n---------------------------------");
            System.out.println("IPLogger Configuration:");
            config(iplc);

            System.out.println("\n\nYou Configured The IPLogger Like This:\n"+iplc);
            System.out.print("\nIs this acceptable? [Enter Y or N]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                if(k.equalsIgnoreCase("YES") || k.equalsIgnoreCase("Y")){
                    System.out.print("Would you like to save this configuration to a file? [Enter Y or N]: ");
                    k = kybrd.readLine();
                    if (!k.equals("")){
                        if(k.equalsIgnoreCase("YES") || k.equalsIgnoreCase("Y")){
                            boolean d2=false;
                            while(!d2){
                                System.out.print("Enter the file name to which to save the configuration: ");
                                k = kybrd.readLine();
                                if (!k.equals("")){
                                    iplc.saveConfig(k);
                                    d2 = true;
                                }
                                else
                                    System.out.println("Hmmmmm... You didn't enter a file name. Try again.");
                            }

                        }
                    }
                }
                done = true;
            }
            else
                System.out.println("OK then! Try Again!\n\n");

        }
    }


    /**
     * Provides a console interface to initialize (or configure if you will) the passed IPLConfig object, which provides
     * configuration information for an IPLogger.
     * @param iplc
     * @throws IOException
     */
    public static void config(IPLConfig iplc) throws IOException {


        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));
        boolean done;

        done = false;
        while(!done){
            System.out.print("\nEnter the name (or IP address) of the target host. ");
            System.out.print("[" + iplc.getTargetIP() + "]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                iplc.setTargetIP(k);
                done = true;
            }
            else if(iplc.getTargetIP()==null)
                System.out.println("You must enter a hostname or IP address.\n\n");
            else
                done = true;
        }

        done = false;
        while(!done){
            System.out.print("\nEnter the port number on the target host. ");
            System.out.print("[" + iplc.getTargetPort() + "]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                iplc.setTargetPort(Integer.valueOf(k).intValue());
                done = true;
            }
            else if(iplc.getTargetPort()==IPLogger.NOTSET)
                System.out.println("You must enter a port number.\n\n");
            else
                done = true;
        }

        done = false;
        while(!done){
            System.out.print("\nEnter the output mode for this logger, R for RAW or I for INTERPRETED.   ");
            System.out.print("[" + iplc.getModeString() + "]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                if(k.equalsIgnoreCase("RAW") || k.equalsIgnoreCase("R")){
                    try {
                        iplc.setOutputMode(IPLogger.RAW);
                    } catch (BadArgException e) {
                        e.printStackTrace();
                    }
                    done = true;
                }
                else if(k.equalsIgnoreCase("INTERPRETED") || k.equalsIgnoreCase("I")){
                    try {
                        iplc.setOutputMode(IPLogger.INTERPRETED);
                    } catch (BadArgException e) {
                        e.printStackTrace();
                    }
                    done = true;
                }
            }
            else if(iplc.getOutputMode() == IPLogger.NOTSET){
                    System.out.println("Your choice of \""+k+"\"  is invalid.\n" +
                                       "Please respond with either an \"R\" or an \"I\"");
            }
            else
                done = true;

        }

        if(iplc.getOutputMode() == IPLogger.INTERPRETED){
            System.out.println("\nThis IPLogger will be using "+iplc.getModeString()+" mode.\n" +
                               "You need to specify the length and content of the End Of Record Block (EORB).");

            int bsize = 0;
            done = false;
            while(!done){
                if(iplc.getEndOfRecordBlock()!=null)
                    bsize = iplc.getEndOfRecordBlock().length;
                System.out.print("Enter the number of bytes in the EORB. ");
                System.out.print("["+bsize+"]: ");
                k = kybrd.readLine();
                if (!k.equals("")){
                    bsize = Integer.valueOf(k).intValue();
                    done = true;
                }
                else if(bsize <= 0)
                    System.out.println("You must enter a size (larger than zero) for the EORB!");
                else
                    done = true;

            }
            byte newBlock[] = new byte[bsize];

            byte oldBlock[] = iplc.getEndOfRecordBlock();
            byte oldByte;

            for(int i=0; i<newBlock.length ;i++){
                oldByte = 0;
                if(oldBlock!=null && i<oldBlock.length)
                    oldByte = oldBlock[i];

                System.out.print("Byte "+i+" ["+oldByte+"]: ");
                k = kybrd.readLine();
                if (!k.equals(""))
                    newBlock[i] = Byte.valueOf(k).byteValue();
                else
                    newBlock[i] = oldByte;
            }
            iplc.setEndOfRecordBlock(newBlock);
        }

        done = false;
        while(!done){
            System.out.print("\nEnter the log file name to which to log the data. " +
                    "(You may use stdout or \"-\" for standard output.   ");
            System.out.print("[" + iplc.getLogFileName() + "]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                iplc.setLogFileName(k);
                done = true;
            }
            else if(iplc.getLogFileName() == null){
                    System.out.println("You MUST identify a log file name!");
            }
            else
                done = true;

        }






    }


//----------------------------------------------------------------------------------------------------------------------

    /**
     * @return A string representation of the IPLConfig object.
     */
    public String toString(){
        String s;

        s = "-------------------------------------------------------------------------------------------------\n";
        s += "IPLConfig:\n";
        s += "\n";
        s += "     Target Host Name:    " + getTargetIP() + "\n";
        s += "     Target Host Port:    " + getTargetPort() + "\n";
        s += "\n";
        s += "     Logger Output Mode:  " + getModeString() + "\n";
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
        s += "     uid:                 <" + uid + ">\n";

        s += "-------------------------------------------------------------------------------------------------\n";

        return (s);

    }

    /**
     * Uses the console (keyboard/monitor) interface to build and save an IPLConfig object.
     * The saved object can be used to run an IPLogger.
     * @param args
     */
    public static void main(String args[]){
        IPLConfig i = new IPLConfig();

        try {
            i.configure();
            System.out.println("\n\nFINAL CONFIGURATION:\n"+i);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



}
