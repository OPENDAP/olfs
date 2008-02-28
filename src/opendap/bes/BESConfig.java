/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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

import java.io.*;

/**
 * Holds a configuarrtion for the BES. The persistent representation of this is an XML element, usually incorporated
 * into a larger document such as the OLFS configuration document.
 * User: ndp
 * Date: Oct 13, 2006
 * Time: 5:00:33 PM
 */
public class BESConfig  {

    private  String    _BESHost;
    private  int       _BESPort;
    private  int       _BESMaxClients;
    private  String    _BESPrefix;
    //private  boolean   _usePersistentContentDocs;
    //private  Document  _OLFSConfigurationDoc;


    BESConfig() {
        _BESHost = "HostNameIsNotSet!";
        _BESPort = -1;
        _BESMaxClients = 10;
        _BESPrefix = "/";
    }

    BESConfig(Document besConfiguration) throws Exception{

        this();
        configure(besConfiguration);

    }

    BESConfig(Element besConfiguration) throws Exception{
        this();

        configure(besConfiguration);

    }

    public BESConfig copy() {


        BESConfig copy = new BESConfig();

        copy._BESHost        = _BESHost;
        copy._BESPort        = _BESPort;
        copy._BESMaxClients  = _BESMaxClients;
        copy._BESPrefix      = _BESPrefix;

        return copy;
    }

    /**
     * Creates a new BESConfig and sets its state according to the values of the persistent representation of the
     * BESConfig fpund in the (XML) file whose name is passed in.
     * @param filename The name of the confguration file
     * @throws Exception When bad things happen.
     */
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
        FileInputStream fis = new FileInputStream(confFile);
        Document doc  = sb.build(fis);

        fis.close();


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



        Element prefix = besConfig.getChild("prefix");
        if( prefix!=null ){
            setPrefix(prefix.getTextTrim());
        }

        Element clientPool = besConfig.getChild("ClientPool");


        if( clientPool!=null ){

            Attribute maxClients = clientPool.getAttribute("maximum");

            int clients = maxClients.getIntValue();

            if(clients<1){
                throw new Exception("OLFS configuration document does not " +
                        "contain correct content. The <ClientPool> element " +
                        "MUST contain an Attribute called \"maximum\" whose " +
                        "value is an integer greater than 0 (zero).");
            }
            setMaxClients(clients);
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

        Element prefix = new Element("prefix");
        prefix.setText(getPrefix());


        Element host = new Element("host");
        host.setText(getHost());

        Element port = new Element("port");
        port.setText(String.valueOf(getPort()));


        Element clientPool = new Element("ClientPool");
        clientPool.setAttribute("maximum",Integer.toString(_BESMaxClients));

        bes.addContent(prefix);
        bes.addContent(host);
        bes.addContent(port);
        bes.addContent(clientPool);

        return bes;
    }





    public void setHost(String host){ _BESHost = host; }
    public String getHost(){ return _BESHost; }


    public void setPort(String port){ _BESPort = Integer.parseInt(port); }
    public void setPort(int port){ _BESPort = port; }
    public int getPort() { return _BESPort; }


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
        s += "        MaxClients: " + getMaxClients() + "\n";



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
