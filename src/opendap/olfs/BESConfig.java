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

package opendap.olfs;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.io.*;

/**
 * User: ndp
 * Date: Oct 13, 2006
 * Time: 5:00:33 PM
 */
public class BESConfig {

    private  String    _BESHost;
    private  int       _BESPort;
    private  int       _BESMaxClients;
    //private  boolean   _usePersistentContentDocs;
    //private  Document  _OLFSConfigurationDoc;


    BESConfig() {
        _BESHost = "HostNameIsNotSet!";
        _BESPort = -1;
        _BESMaxClients = 1;
    }

    BESConfig(Document besConfiguration) throws Exception{

        this();
        configure(besConfiguration);

    }

    BESConfig(Element besConfiguration) throws Exception{
        this();

        configure(besConfiguration);

    }

    BESConfig(String filename) throws Exception {
        this();

        File confFile = new File(filename);


        if(!confFile.exists()){
            throw new Exception("BES configuration file \""+filename+"\" does not exist.");
        }

        if(!confFile.canRead())
            throw new Exception("BES configuration file \""+filename+"\" is not readable.");


        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc  = sb.build(new FileInputStream(confFile));


        configure(doc);


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



        Element host = besConfig.getChild("host");
        Element port = besConfig.getChild("port");

        if( host==null ){
            throw new Exception("OLFS configuration document does not contain neccessary content. " +
                    "<BES> Element is missing <host> element.");
        }

        if( port==null ){
            throw new Exception("OLFS configuration document does not contain neccessary content. " +
                    "<BES> Element is missing <prt> element.");

        }

        setBESHost(host.getTextTrim());
        setBESPort(port.getTextTrim());



        Element maxClients = besConfig.getChild("MaxClients");


        if( maxClients!=null ){


            int clients = Integer.parseInt(maxClients.getTextTrim());

            if(clients<1){
                throw new Exception("OLFS configuration document does not contain correct content. " +
                        "The <MaxClients> element MUST contain an integer greater than 0 (zero).");
            }
            setBESMaxClients(clients);
        }




    }






    public void writeConfiguration(String filename) throws IOException {
        OutputStream os = new FileOutputStream(filename);
        writeConfiguration(os);
        os.close();
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

        Element host = new Element("host");
        host.setText(getBESHost());

        Element port = new Element("port");
        port.setText(String.valueOf(getBESPort()));


        Element clientPool = new Element("ClientPool");
        clientPool.setAttribute("MaxClients",Integer.toString(_BESMaxClients));

        bes.addContent(host);
        bes.addContent(port);
        bes.addContent(clientPool);

        return bes;
    }





    public void   setBESHost(String host){ _BESHost = host; }
    public String getBESHost(){ return _BESHost; }


    public void setBESPort(String port){ _BESPort = Integer.parseInt(port); }
    public void setBESPort(int port){ _BESPort = port; }
    public int  getBESPort() { return _BESPort; }



    public void setBESMaxClients(String i){ _BESMaxClients = Integer.parseInt(i);   }
    public void setBESMaxClients(int i){ _BESMaxClients = i;   }
    public int  getBESMaxClients(){ return _BESMaxClients;  }



    public String toString(){

        String s = "";
        s += "    BESConfig:\n";
        s += "        Host:       " + getBESHost() + "\n";
        s += "        Port:       " + getBESPort() + "\n";
        s += "        MaxClients: " + getBESMaxClients() + "\n";



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
     * @throws IOException
     */
    public void userConfigure() throws IOException {
        userConfigure(this);
    }






    /**
     * Provides a console interface to initialize (or configure if you will) the passed OLFSConfig object, which provides
     * BES configuration information for the OLFS. After the intialization is complete, the user will be prompted to
     * save the configuration into a file (as an XML document).
     * @param bc The OLFSConfig to initialize.
     * @throws IOException
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
                                    bc.writeConfiguration(k);
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
            System.out.print("\nEnter the name (or IP address) of the BES host. ");
            System.out.print("[" + bc.getBESHost() + "]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                bc.setBESHost(k);
                done = true;
            }
            else if(bc.getBESHost()==null)
                System.out.println("You must enter a hostname or IP address.\n\n");
            else
                done = true;
        }

        done = false;
        while(!done){
            System.out.print("\nEnter the port number for the BES host "+bc.getBESHost()+"   ");
            System.out.print("[" + bc.getBESPort() + "]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                bc.setBESPort(k);
                done = true;
            }
            else if(bc.getBESPort()==-1)
                System.out.println("You must enter a port number.\n\n");
            else
                done = true;
        }



        done = false;
        while(!done){
            System.out.print("\nEnter the maximum allowed number of BES client connections. ");
            System.out.print("[" + bc.getBESMaxClients() + "]: ");
            k = kybrd.readLine();
            if (!k.equals("")){
                bc.setBESMaxClients(k);
            }

            if(bc.getBESMaxClients()<1)
                System.out.println("You must enter an integer greater than 0.\n\n");
            else
                done = true;
        }



    }




}
