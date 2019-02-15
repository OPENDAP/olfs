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

package opendap.bes;

import opendap.coreServlet.Scrub;
import opendap.io.HyraxStringEncoding;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.*;

/**
 * Holds a configuarrtion for the BES. The persistent representation of this is an XML element, usually incorporated
 * into a larger document such as the OLFS configuration document.
 * User: ndp
 * Date: Oct 13, 2006
 * Time: 5:00:33 PM
 */
public class BESConfig  {

    private  Logger log;

    private  String  _BesHost;
    private  int     _BesPort;
    private  int     _BesAdminPort;
    private  int     _BesMaxClients;
    private  int     _BesMaxCommands;
    private  int     _BesMaxResponseSize;
    private  String  _BesPrefix;
    private  int     _BesTimeOut;  // in ms

    private  String  _BesNickName;


    //private  boolean   _usePersistentContentDocs;
    //private  Document  _OLFSConfigurationDoc;


    private BESConfig() {
        log = org.slf4j.LoggerFactory.getLogger(getClass());

        _BesHost = "HostNameIsNotSet!";
        _BesPort = -1;
        _BesAdminPort = -1;
        _BesMaxClients = 200;
        _BesMaxCommands = 2000;
        _BesPrefix = "/";
        _BesMaxResponseSize = 0;
        _BesNickName = null;
        _BesTimeOut = 300000; // 5 minutes in ms
    }

    public BESConfig(Document besConfiguration) throws BadConfigurationException {

        this();
        configure(besConfiguration);

    }

    public BESConfig(Element besConfiguration) throws BadConfigurationException {
        this();

        configure(besConfiguration);

    }

    public BESConfig copy() {


        BESConfig copy = new BESConfig();

        copy._BesHost            = _BesHost;
        copy._BesPort            = _BesPort;
        copy._BesAdminPort       = _BesAdminPort;
        copy._BesMaxResponseSize = _BesMaxResponseSize;
        copy._BesMaxClients      = _BesMaxClients;
        copy._BesMaxCommands     = _BesMaxCommands;
        copy._BesPrefix          = _BesPrefix;
        copy._BesNickName        = _BesNickName;
        copy._BesTimeOut         = _BesTimeOut;

        return copy;
    }

    /**
     * Creates a new BESConfig and sets its state according to the values of the persistent representation of the
     * BESConfig fpund in the (XML) file whose name is passed in.
     * @param filename The name of the confguration file
     * @throws Exception When bad things happen.
     */
    public BESConfig(String filename) throws BadConfigurationException, IOException, JDOMException {
        this();

        File confFile = new File(filename);


        if(!confFile.exists()){
            throw new BadConfigurationException("BES configuration file \""+filename+"\" does not exist.");
        }

        if(!confFile.canRead())
            throw new BadConfigurationException("BES configuration file \""+filename+"\" is not readable.");


        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        FileInputStream fis = new FileInputStream(confFile);
        try {
            Document doc  = sb.build(fis);
            configure(doc);
        }
        finally {
            try {fis.close(); }
            catch(IOException e){
                log.error("Failed to close BES configuration file: "+filename+
                "Error Message: "+e.getLocalizedMessage());
            }
        }

    }





    private void configure(Document olfsConfigurationDoc) throws BadConfigurationException {
        Element besConfig = olfsConfigurationDoc.getRootElement();

        configure(besConfig);

    }








    private void configure(Element besConfig) throws BadConfigurationException {

        if( besConfig==null || !besConfig.getName().equals("BES")){
            throw new BadConfigurationException("Configuration Element does not contain neccessary content. " +
                    "Missing <BES> element.");
        }


        Element prefix = besConfig.getChild("prefix");
        if( prefix!=null ){
            setPrefix(prefix.getTextTrim());
            log.info("BES prefix set to '{}'",getPrefix());
        }
        else {
            log.warn("BES prefix not set in configuration. Using default value of: '{}'",getPrefix());
        }


        Element host = besConfig.getChild("host");
        if( host==null ){
            throw new BadConfigurationException("Configuration Element does not contain neccessary content. " +
                    "<BES> Element is missing <host> element.");
        }
        setHost(host.getTextTrim());




        Element port = besConfig.getChild("port");
        if( port==null ){
            throw new BadConfigurationException("Configuration Element does not contain neccessary content. " +
                    "<BES> Element is missing <port> element.");

        }
        setPort(port.getTextTrim());



        Element adminPort = besConfig.getChild("adminPort");
        if( adminPort!=null ){
            setAdminPort(adminPort.getTextTrim());
            log.info("BES '{}' adminPort set to {}",getPrefix(), getAdminPort());
        }



        Element maxResponseSize = besConfig.getChild("maxResponseSize");
        if( maxResponseSize!=null ){
            setMaxResponseSize(maxResponseSize.getTextTrim());
            log.info("BES '{}' maxResponseSize set to {}",getPrefix(), getMaxResponseSize());
        }

        Element timeOut = besConfig.getChild("timeOut");
        if( timeOut!=null ){
            setTimeOut(timeOut.getTextTrim());
            log.info("BES '{}' maxResponseSize set to {}",getPrefix(), getMaxResponseSize());
        }




        //  <ClientPool maximum="10" maxCmds="2000"/>

        Element clientPool = besConfig.getChild("ClientPool");


        if( clientPool!=null ){

            log.debug("Found ClientPool element.");
            Attribute maxClients = clientPool.getAttribute("maximum");

            if(maxClients != null){
                log.debug("@maximum: {}",maxClients.getValue());
                int clients;

                try {
                    clients = maxClients.getIntValue();
                }
                catch (DataConversionException e) {
                    throw new BadConfigurationException("Configuration Element does not " +
                            "contain correct content. The <ClientPool> element's " +
                            "Attribute \"maximum\" must evaluate to an integer value. " +
                            "Found maximum=\""+maxClients.getValue()+"\"");
                }


                if(clients<1){
                    throw new BadConfigurationException("Configuration Element does not " +
                            "contain correct content. The <ClientPool> element " +
                            "MUST contain an Attribute called \"maximum\" whose " +
                            "value MUST be an integer greater than 0 (zero).");
                }
                setMaxClients(clients);
            }
            else {
                log.warn("Configuration of BES ClientPool did not specify a 'maximum' size. Using default value.");
            }
            log.info("BES '{}' client pool will have a maximum size of {}",getPrefix(), getMaxClients());


            Attribute maxCmds = clientPool.getAttribute("maxCmds");

            if(maxCmds != null){
                log.debug("@maxCmds: {}",maxCmds);

                int max;
                try {
                    max = maxCmds.getIntValue();
                }
                catch (DataConversionException e) {
                    throw new BadConfigurationException("Configuration Element does not " +
                            "contain correct content. The <ClientPool> element's " +
                            "Attribute \"maxCmds\" must evaluate to an integer value." +
                            "Found maxCmds=\""+maxCmds.getValue()+"\"");
                }

                if(max<0){
                    throw new BadConfigurationException("Configuration Element does not " +
                            "contain correct content. The <ClientPool> element " +
                            "MAY contain an Attribute called \"maxCmds\" whose " +
                            "value is an integer greater than or equal to 0 (zero).");
                }
                setMaxCommands(max);
            }
            else {
                log.warn("Configuration of BES ClientPool did not specify a 'maxCmds' value. Using default value.");
            }
            log.info("BES '{}' clients be used for at most {} commands",getPrefix(), getMaxCommands());


        }



        String besName = besConfig.getAttributeValue("name");
        if( besName!=null ){
            setBesName(besName);
        }
        else {
            setBesName(_BesPrefix);
        }

        log.info("BES nickName set to {}", getBesName());


    }


    public void setBesName(String nickName){
        _BesNickName = nickName;
    }

    public String getBesName(){
        return _BesNickName;
    }


    public void setMaxCommands(int max){
        _BesMaxCommands = max;
    }

    public int getMaxCommands(){
        return _BesMaxCommands;
    }



    private  void writeConfiguration(String filename) throws IOException {
        OutputStream os = new FileOutputStream(filename);
        try {  writeConfiguration(os); }
        finally {
            try {os.close(); }
            catch(IOException e){
                log.error("Failed to close BES configuration file: "+filename+
                "Error Message: "+e.getLocalizedMessage());
            }

        }
    }




    public void writeConfiguration(OutputStream os) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(getConfigDocument(), os);
    }


    public Document getConfigDocument(){
        return new Document(getConfigElement());
    }



    public Element getConfigElement(){

        Element bes = new Element("BES");

        Element prefix = new Element("prefix");
        prefix.setText(getPrefix());


        Element host = new Element("host");
        host.setText(getHost());

        Element port = new Element("port");
        port.setText(String.valueOf(getPort()));

        Element adminPort = new Element("adminPort");
        adminPort.setText(String.valueOf(getAdminPort()));

        Element maxResponseSize = new Element("maxResponseSize");
        adminPort.setText(String.valueOf(getMaxResponseSize()));


        Element clientPool = new Element("ClientPool");
        clientPool.setAttribute("maximum",Integer.toString(_BesMaxClients));
        clientPool.setAttribute("maxCmds",Integer.toString(_BesMaxCommands));

        bes.addContent(prefix);
        bes.addContent(host);
        bes.addContent(port);
        bes.addContent(adminPort);
        bes.addContent(maxResponseSize);
        bes.addContent(clientPool);

        return bes;
    }





    public void setHost(String host){ _BesHost = host; }
    public String getHost(){ return _BesHost; }


    public void setPort(String port){setPort(Integer.parseInt(port)); }
    public void setPort(int port){ _BesPort = port; }
    public int getPort() { return _BesPort; }

    public void setAdminPort(String port){ setAdminPort(Integer.parseInt(port)); }
    public void setAdminPort(int port){ _BesAdminPort = port; }
    public int getAdminPort() { return _BesAdminPort; }

    public void setMaxResponseSize(String maxResponseSize){ setMaxResponseSize(Integer.parseInt(maxResponseSize)); }
    public void setMaxResponseSize(int maxResponseSize){ _BesMaxResponseSize = maxResponseSize; }
    public int getMaxResponseSize() { return _BesMaxResponseSize; }

    /**
     *
     * @param timeOut  Number of seconds for the client to wait for the BES to respond
     */
    public void setTimeOut(String timeOut){setTimeOut(Integer.parseInt(timeOut)); }

    /**
     *
     * @param timeOut  Number of seconds for the client to wait for the BES to respond
     */
    public void setTimeOut(int timeOut){ _BesTimeOut = timeOut * 1000; }

    /**
     *
     * @return   Number of milliseconds for the client to wait for the BES to respond
     */
    public int getTimeOut() { return _BesTimeOut; }


    public void setPrefix(String prefix){ _BesPrefix = prefix; }
    public String getPrefix() { return _BesPrefix; }



    public void setMaxClients(String i){ setMaxClients(Integer.parseInt(i));   }
    public void setMaxClients(int i){ _BesMaxClients = i;   }
    public int getMaxClients(){ return _BesMaxClients;  }



    public String toString(){

        String s = "";
        s += "    BESConfig:\n";
        s += "        Prefix:     " + getPrefix() + "\n";
        s += "        Host:       " + getHost() + "\n";
        s += "        Port:       " + getPort() + "\n";
        s += "        Timeout:    " + getTimeOut() + " ms\n";
        s += "        adminPort:  " + getAdminPort() + "\n";
        s += "        MaxClients: " + getMaxClients() + "\n";
        s += "        MaxCommands/client: " + getMaxCommands() + "\n";



        return s;
    }






    //public void    setUsePersistentContentDocs(boolean val){ _usePersistentContentDocs = val; }
    //public boolean usePersistentContentDocs(){ return _usePersistentContentDocs; }





    public static void main(String[] args) throws Exception{

        BESConfig bc = new BESConfig();


        bc.userConfigure();

    }







//----------------------------------------------------------------------------------------------------------------------
//
//                    Initialization Routines
//
// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    /**
     * Provides a console interface to initialize (or configure if you will) an OLFSConfig object, which provides
     * BES configuration information to the OLFS.
     * @throws IOException When bad things happen
     */
    public void userConfigure() throws IOException {
        userConfigure(this);
    }






    /**
     * Provides a console interface to initialize (or configure if you will) the passed OLFSConfig object, which provides
     * BES configuration information for the OLFS. After the intialization is complete, the user will be prompted to
     * save the configuration into a file (as an XML document).
     * @param bc The OLFSConfig to initialize.
     * @throws IOException When bad things happen
     */

    public static void userConfigure(BESConfig bc) throws IOException {
        boolean done = false;
        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in, HyraxStringEncoding.getCharset()));

        while(!done){

            System.out.println("\n\n---------------------------------");
            System.out.println("BES Configuration:");
            config(bc);

            System.out.println("\n\nYou Configured The BES Like This:\n"+bc);
            System.out.print("\nIs this acceptable? [Enter Y or N]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                if(k.equalsIgnoreCase("YES") || k.equalsIgnoreCase("Y")){
                    System.out.print("Would you like to save this configuration to a file? [Enter Y or N]: ");
                    k = kybrd.readLine();
                    if (k!=null && !k.equals("")){
                        if(k.equalsIgnoreCase("YES") || k.equalsIgnoreCase("Y")){
                            boolean d2=false;
                            while(!d2){
                                System.out.print("Enter the file name to which to save the configuration: ");
                                k = kybrd.readLine();
                                if (k!=null && !k.equals("")){
                                    bc.writeConfiguration(Scrub.fileName(k));
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

    public static void config(BESConfig bc) throws IOException {

        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in, HyraxStringEncoding.getCharset()));
        boolean done;


        done = false;
        while(!done){
            System.out.print("\nEnter the path prefix that the OLFS will use for the BES "+bc.getHost()+":"+bc.getPort()+"   ");
            System.out.print("[" + bc.getPrefix() + "]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                bc.setPrefix(k);
                done = true;
            }
            else
                done = true;
        }



        done = false;
        while(!done){

            System.out.print("\nEnter the name (or IP address) of the BES host. ");
            System.out.print("[" + bc.getHost() + "]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                bc.setHost(k);
                done = true;
            }
            else if(bc.getHost()==null)
                System.out.println("You must enter a hostname or IP address.\n\n");
            else
                done = true;
        }

        done = false;
        while(!done){
            System.out.print("\nEnter the port number for the BES host "+bc.getHost()+"   ");
            System.out.print("[" + bc.getPort() + "]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                bc.setPort(k);
                done = true;
            }
            else if(bc.getPort()==-1)
                System.out.println("You must enter a port number.\n\n");
            else
                done = true;
        }


        done = false;
        while(!done){
            System.out.print("\nEnter the time out time (in seconds) for the BES host "+bc.getHost()+"   ");
            System.out.print("[" + bc.getTimeOut()/1000 + "]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                bc.setTimeOut(k);
                done = true;
            }
            else if(bc.getTimeOut()==-1)
                System.out.println("You must enter a timeOut number.\n\n");
            else
                done = true;
        }



        done = false;
        while(!done){
            System.out.print("\nEnter the admin port number for the BES host "+bc.getHost()+"   ");
            System.out.print("[" + bc.getAdminPort() + "]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                bc.setAdminPort(k);
                done = true;
            }
            else if(bc.getAdminPort()==-1)
                System.out.println("You must enter a port number.\n\n");
            else
                done = true;
        }



        done = false;
        while(!done){
            System.out.print("\nEnter the max response size for non-authenticated users for the BES host "+bc.getHost()+"   ");
            System.out.print("[" + bc.getMaxResponseSize() + "]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                bc.setMaxResponseSize(k);
                done = true;
            }
            else if(bc.getMaxResponseSize()==-1)
                System.out.println("You must enter a max response size.\n\n");
            else
                done = true;
        }





        done = false;
        while(!done){
            System.out.print("\nEnter the maximum size of the BES client connection pool. ");
            System.out.print("[" + bc.getMaxClients() + "]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")){
                bc.setMaxClients(k);
            }

            if(bc.getMaxClients()<1)
                System.out.println("You must enter an integer greater than 0.\n\n");
            else
                done = true;
        }



    }




}
