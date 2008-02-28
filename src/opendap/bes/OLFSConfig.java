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
import org.jdom.DataConversionException;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.input.SAXBuilder;

import java.io.*;
import java.util.Iterator;

/**
 * This class holds configuration information for the OLFS. It has a main() method and can be run as an application
 * to generate an OLFS config file.
 */
public class OLFSConfig {

    private Element  _besConfig;

    private  boolean   _showTHREDDSDirectoryView;
    private  boolean   _allowDirectDataSourceAccess;
    //private  Document  _OLFSConfigurationDoc;


    OLFSConfig() {
        _besConfig = new Element("BESConfig");
        _showTHREDDSDirectoryView = false;
        _allowDirectDataSourceAccess = false;
    }

    OLFSConfig(Document olfsConfiguration) throws Exception{

        configure(olfsConfiguration);

    }


    OLFSConfig(String filename) throws Exception {

        File confFile = new File(filename);

        if(!confFile.exists()){
            throw new Exception("OLFS configuration file \""+filename+"\" does not exist.");
        }

        if(!confFile.canRead())
            throw new Exception("OLFS configuration file \""+filename+"\" is not readable.");

        FileInputStream fis = new FileInputStream(confFile);

        // Parse the XML doc into a Document object.
        SAXBuilder sb = new SAXBuilder();
        Document doc  = sb.build(fis);
        fis.close();

        configure(doc);


    }


    private void configure(Document olfsConfigurationDoc) throws Exception {

        _besConfig = olfsConfigurationDoc.getRootElement().getChild("BESConfig");


        Element dirView = olfsConfigurationDoc.getRootElement().getChild("DirectoryView");
        if( dirView!=null ){
            String val = dirView.getTextTrim();

            if(val.equalsIgnoreCase("THREDDS")){
                setTHREDDSDirectoryView(true);
            }
            else if(val.equalsIgnoreCase("OPeNDAP")){
                setTHREDDSDirectoryView(false);
            }
            else {
                throw new Exception("In the OLFS Config, The Element <DirectoryView> may only contain content " +
                                    "equal to \"THREDDS\" or \"OPeNDAP\". The value \n" + val + "\" is illegal.");
            }
        }
        else {
            setTHREDDSDirectoryView(false);
        }




        if(olfsConfigurationDoc.getRootElement().getChild("AllowDirectDataSourceAccess") != null)
            setAllowDirectDataSourceAccess(true);
        else
            setAllowDirectDataSourceAccess(false);




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
        Element config = new Element("OLFSConfig");
        Element bes = getBESConfig();

        Element dv = new Element("DirectoryView");
        dv.setText((_showTHREDDSDirectoryView?"THREDDS":"OPeNDAP"));

        config.addContent(bes);
        config.addContent(dv);

        return config;
    }




    public Element getBESConfig(){
        return _besConfig;
    }




    public String toString(){
        String s = "OLFSConfig:\n";
        s+="\n";

        int i = 0;
        String prefix, host, port;
        String maxClients;
        for(Object o : _besConfig.getChildren("BES")){
            Element e = (Element) o;
            host = e.getChild("host").getTextTrim();
            port = e.getChild("port").getTextTrim();
            prefix = e.getChild("prefix").getTextTrim();

            try {
                maxClients = "" + e.getChild("ClientPool").getAttribute("maximum").getIntValue();
            } catch (DataConversionException e1) {
                maxClients = "BAD VALUE!";
            }
            s += "    BES["+ i++ +"]:\n";
            s += "        prefix:      "+prefix+"\n";
            s += "        host:        "+host+"\n";
            s += "        port:        "+port+"\n";
            s += "        MaxClients:  "+maxClients+"\n";
            s+="\n";

        }

        s += "    Directory View:  "+(getTHREDDSDirectoryView()?"THREDDS":"OPeNDAP") + "\n";
        s += "    Direct Data Source Access: "+allowDirectDataSourceAccess()+ "\n";
//        s += "    Use Persistent Content Documentation Directory ('docs'): "+usePersistentContentDocs() + "\n";


        return s;
    }






    public void    setTHREDDSDirectoryView(boolean val){ _showTHREDDSDirectoryView = val; }
    public boolean getTHREDDSDirectoryView(){ return _showTHREDDSDirectoryView; }


    public void    setAllowDirectDataSourceAccess(boolean val){ _allowDirectDataSourceAccess = val; }
    public boolean allowDirectDataSourceAccess(){ return _allowDirectDataSourceAccess; }





    public static void main(String[] args) throws Exception{

        OLFSConfig bc = new OLFSConfig();


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
     * @param oc The OLFSConfig to initialize.
     * @throws IOException
     */

    public static void userConfigure(OLFSConfig oc) throws IOException {
        boolean done = false;
        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));

        while(!done){

            System.out.println("\n\n---------------------------------");
            System.out.println("OLFS Configuration:");
            config(oc);

            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            xmlo.output(oc._besConfig, System.out);


            System.out.println("\n\nYou Configured The OLFS Like This:\n"+oc);
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
                                    oc.writeConfiguration(k);
                                    d2 = true;
                                }
                                else
                                    System.out.println("Hmmmmm... You didn't enter a file name. Try again.");
                            }

                        }
                    }
                    done = true;
                }
            }
            else
                System.out.println("OK then! Try Again!\n\n");

        }
    }

    public static void config(OLFSConfig olfsConfig) throws IOException {

        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));
        boolean done;

        int besCount=1;

        done = false;
        while(!done){
            System.out.print("\nHow Many BES's will you be attaching to your OLFS? (x>=1): ");
            k = kybrd.readLine();
            try {
                besCount  = Integer.decode(k);
                if(besCount>0){
                    done = true;
                }
                else {
                    System.out.println("\n\nThe value must be greater than zero!");
                }

            }
            catch(NumberFormatException e){
                System.out.println("\n\nHey! I need an integer. A number, get it?");
            }


        }
        BESConfig bc = new BESConfig();
        for(int i=0; i<besCount ; i++){
            BESConfig.config(bc);
            olfsConfig.getBESConfig().addContent(bc.getConfigElement());
        }


        done = false;
        while(!done){
            System.out.print("\nDo you want to use the THREDDS catalog as the default directory view?");
            System.out.print("[" + olfsConfig.getTHREDDSDirectoryView() + "]: ");
            k = kybrd.readLine();
            if (k!=null && (k.equalsIgnoreCase("y") || k.equalsIgnoreCase("yes"))){
                olfsConfig.setTHREDDSDirectoryView(true);
                done = true;
            }
            else  {
                olfsConfig.setTHREDDSDirectoryView(false);
                done = true;
            }
        }



        /*
        done = false;
        while(!done){
            System.out.print("\nDo you want to use the 'docs' dir in the persistent content area as the documentation source?");
            System.out.print("[" + olfsConfig.usePersistentContentDocs() + "]: ");
            k = kybrd.readLine();
            if (k.equalsIgnoreCase("y") || k.equalsIgnoreCase("yes")){
                olfsConfig.setUsePersistentContentDocs(true);
                done = true;
            }
            else if(k.equalsIgnoreCase("n") || k.equalsIgnoreCase("no")){
                olfsConfig.setUsePersistentContentDocs(false);
                done = true;
            }
            else
                System.out.println("You must enter say 'yes' or 'no'.\n\n");
        }
        */


    }








}
