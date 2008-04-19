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
package opendap.wcs;

import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpClient;

import java.io.IOException;

/**
 * User: ndp
 * Date: Apr 17, 2008
 * Time: 10:23:18 AM
 */
public class Validator {


    private HttpClient httpClient;

    public Validator(){
        httpClient = new HttpClient();
    }




    public static void main(String args[]){

        Validator parser = new Validator();

        for (String url : args) {
            try {
                parser.validateURL(url);
            } catch (Exception e) {
                e.printStackTrace(System.out);

            }
        }
    }


    public Document validateURL(String url) throws Exception {



        System.out.println("requestURI: "+url);

        GetMethod request = new GetMethod(url);

        System.out.println("HttpClient: "+httpClient);


        try {
            // Execute the method.
            int statusCode = httpClient.executeMethod(request);

            if (statusCode != HttpStatus.SC_OK) {
              System.out.println("ERROR: Method failed " + request.getStatusLine());
            }

            // Parse the XML doc into a Document object.

            // get a jdom parser to parse and validate the XML document.
            SAXBuilder parser = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);

            // turn on validation
            parser.setFeature("http://apache.org/xml/features/validation/schema", true);

            Document doc = parser.build(request.getResponseBodyAsStream());
            System.out.println("Got and parsed and validated XML document: "+url);
            //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            //xmlo.output(doc, System.out);

            return doc;


        }
        finally {
            System.out.println("Releasing Http connection.");
            request.releaseConnection();
        }


    }







}
