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

package opendap.experiments;

import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.Element;

import java.io.*;


/**
 * User: ndp
 * Date: Apr 2, 2007
 * Time: 11:20:27 AM
 */
public class ThreddsProperty {

    String name;
    String value;




    ThreddsProperty(){
        name = null;
        value = null;
    }

    ThreddsProperty(String n, String v){
        name = n;
        value = v;
    }

    public String toString() {
        String s = "Name: " + name + "\n";
        s += "Value: " + value + "\n";

        return s;

    }

    Element getPropertyElement() {
        Element property = new Element("property");

        property.setAttribute("name", name);
        property.setAttribute("value", value);
        return property;


    }

    public void writeConfiguration(String filename) throws IOException {
        OutputStream os = new FileOutputStream(filename);
        writeConfiguration(os);
        os.close();
    }


    public void writeConfiguration(OutputStream os) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(getPropertyElement(), os);
    }


    /**
     * Provides a console interface to initialize (or configure if you will) the passed OLFSConfig object, which provides
     * BES configuration information for the OLFS. After the intialization is complete, the user will be prompted to
     * save the configuration into a file (as an XML document).
     *
     * @param tp The ThreddsProperty to initialize.
     * @throws java.io.IOException Blah Blah Blah
     */

    public static void userConfigure(ThreddsProperty tp) throws IOException {
        boolean done = false;
        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));

        while (!done) {

            System.out.println("\n\n---------------------------------");
            System.out.println("Property Builder:");

            config(tp);

            System.out.println("\n\nYou Configured The ThreddsProperty Like This:\n" + tp);
            System.out.print("\nIs this acceptable? [Enter Y or N]: ");
            k = kybrd.readLine();
            if (k!=null && !k.equals("")) {
                if (k.equalsIgnoreCase("YES") || k.equalsIgnoreCase("Y")) {
                    System.out.print("Would you like to save this ThreddProperty to a file? [Enter Y or N]: ");
                    k = kybrd.readLine();
                    if (k!=null && !k.equals("")) {
                        if (k.equalsIgnoreCase("YES") || k.equalsIgnoreCase("Y")) {
                            boolean d2 = false;
                            while (!d2) {
                                System.out.print("Enter the file name to which to save the configuration: ");
                                k = kybrd.readLine();
                                if (k!=null && !k.equals("")) {
                                    tp.writeConfiguration(k);
                                    d2 = true;
                                } else
                                    System.out.println("Hmmmmm... You didn't enter a file name. Try again.");
                            }

                        }
                    }
                    done = true;
                }
            } else
                System.out.println("OK then! Try Again!\n\n");

        }
    }

    public static void config(ThreddsProperty tp) throws IOException {

        String k;
        BufferedReader kybrd = new BufferedReader(new InputStreamReader(System.in));
        boolean done;


        done = false;
        while (!done) {
            System.out.print("What is the name of this property(" + tp.name + "): ");
            k = kybrd.readLine();
            if (k!=null && k.equals("")) {
                System.out.println("The name may not be an empty string! Try again...");
            } else {
                tp.name = k;
                done = true;
            }

        }


        done = false;
        while (!done) {
            System.out.print("What is the value of this property(" + tp.value + "): ");
            k = kybrd.readLine();
            if (k!=null && k.equals("")) {
                System.out.println("The value may not be an empty string! Try again...");
            } else {
                tp.value = k;
                done = true;
            }

        }


    }






    public static void main(String[] args) throws Exception{

        ThreddsProperty tp = new ThreddsProperty();


        ThreddsProperty.userConfigure(tp);


    }




}
