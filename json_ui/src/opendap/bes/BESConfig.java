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

package opendap.bes;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import java.io.*;

import opendap.coreServlet.Scrub;

/**
 * Holds a configuarrtion for the BES. The persistent representation of this is an XML element, usually incorporated
 * into a larger document such as the OLFS configuration document.
 * User: ndp
 * Date: Oct 13, 2006
 * Time: 5:00:33 PM
 */
public class BESConfig  {

    private  Logger log;

    private  String    _BESHost;
    private  int       _BESPort;
    private  int       _BESAdminPort;
    private  int       _BESMaxClients;
    private  int       _BESMaxCommands;
    private  int       _BESMaxResponseSize;
    private  String    _BESPrefix;
    //private  boolean   _usePersistentContentDocs;
    //private  Document  _OLFSConfigurationDoc;


    private BESConfig() {
        log = org.slf4j.LoggerFactory.getLogger(getClass());

        _BESHost = "HostNameIsNotSet!";
        _BESPort = -1;
        _BESAdminPort = -1;
        _BESMaxClients = 10;
        _BESMaxCommands = 2000;
        _BESPrefix = "/";
        _BESMaxResponseSize = 0;
    }

    public BESConfig(Document besConfiguration) throws Exception{

        this();
        configure(besConfiguration);

    }

    public BESConfig(Element besConfiguration) throws Exception{
        this();

        configure(besConfiguration);

    }

    public BESConfig copy() {


        BESConfig copy = new BESConfig();

        copy._BESHost        = _BESHost;
        copy._BESPort        = _BESPort;
        copy._BESAdminPort   = _BESAdminPort;
        copy._BESMaxResponseSize   = _BESMaxResponseSize;
        copy._BESMaxClients  = _BESMaxClients;
        copy._BESMaxCommands = _BESMaxCommands;
        copy._BESPrefix      = _BESPrefix;

        return copy;
    }

    /**
     * Creates a new BESConfig and sets its state according to the values of the persistent representation of the
     * BESConfig fpund in the (XML) file whose name is passed in.
     * @param filename The name of the confguration file
     * @throws Exception When bad things happen.
     */
    public BESConfig(String filename) throws Exception {
        this();

        File confFile = new File(filename);


        if(!confFile.exists()){
            throw new Exception("BES configuration file \""+filename+"\" does not exist.");
        }

        if(!confFile.canRead())
            throw new Exception("BES configuration file \""+filename+"\" is not readable.");


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





    private void configure(Document olfsConfigurationDoc) throws Exception {
        Element besConfig = olfsConfigurationDoc.getRootElement();

        configure(besConfig);

    }








    private void configure(Element besConfig) throws Exception {

        if( besConfig==null || !besConfig.getName().equals("BES")){
            throw new Exception("BES configuration document does not contain neccessary content. " +
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
            throw new Exception("OLFS configuration document does not contain neccessary content. " +
                    "<BES> Element is missing <host> element.");
        }
        setHost(host.getTextTrim());




        Element port = besConfig.getChild("port");
        if( port==null ){
            throw new Exception("OLFS configuration document does not contain neccessary content. " +
                    "<BES> Element is missing <prt> element.");

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




        //  <ClientPool maximum="10" />

        Element clientPool = besConfig.getChild("ClientPool");


        if( clientPool!=null ){

            log.debug("Found ClientPool element.");
            Attribute maxClients = clientPool.getAttribute("maximum");

            if(maxClients != null){
                log.debug("@maximum: {}",maxClients.getValue());
                int clients = maxClients.getIntValue();

                if(clients<1){
                    throw new Exception("OLFS configuration document does not " +
                            "MAY correct content. The <ClientPool> element " +
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
                int max = maxCmds.getIntValue();

                if(max<0){
                    throw new Exception("OLFS configuration document does not " +
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




    }



    public void setMaxCommands(int max){
        _BESMaxCommands = max;
    }

    public int getMaxCommands(){
        return _BESMaxCommands;
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
        clientPool.setAttribute("maximum",Integer.toString(_BESMaxClients));
        clientPool.setAttribute("maxCmds",Integer.toString(_BESMaxCommands));

        bes.addContent(prefix);
        bes.addContent(host);
        bes.addContent(port);
        bes.addContent(adminPort);
        bes.addContent(maxResponseSize);
        bes.addContent(clientPool);

        return bes;
    }





    public void setHost(String host){ _BESHost = host; }
    public String getHost(){ return _BESHost; }


    public void setPort(String port){ _BESPort = Integer.parseInt(port); }
    public void setPort(int port){ _BESPort = port; }
    public int getPort() { return _BESPort; }

    public void setAdminPort(String port){ _BESAdminPort = Integer.parseInt(port); }
    public void setAdminPort(int port){ _BESAdminPort = port; }
    public int getAdminPort() { return _BESAdminPort; }

    public void setMaxResponseSize(String maxResponseSize){ _BESMaxResponseSize = Integer.parseInt(maxResponseSize); }
    public void setMaxResponseSize(int maxResponseSize){ _BESMaxResponseSize = maxResponseSize; }
    public int getMaxResponseSize() { return _BESMaxResponseSize; }


    public void setPrefix(String prefix){ _BESPrefix = prefix; }
    public String getPrefix() { return _BESPrefix; }



    public void setMaxClients(String i){ _BESMaxClients = Integer.parseInt(i);   }
    public void setMaxClients(int i){ _BESMaxClients = i;   }
    public int getMaxClients(){ return _BESMaxClients;  }



    public String toString(){

        String s = "";
        s += "    BESConfig:\n";
        s += "        Prefix:     " + getPrefix() + "\n";
        s += "        Host:       " + getHost() + "\n";
        s += "        Port:       " + getPort() + "\n";
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
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));

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
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));
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
