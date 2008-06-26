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
package opendap.xml;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;
import org.jdom.input.SAXBuilder;

import java.net.URI;
import java.io.*;

/**
 * User: ndp
 * Date: Apr 23, 2008
 * Time: 9:22:11 AM
 */
public class Transformer {


    private HttpClient httpClient;

    public Transformer(){
        httpClient = new HttpClient();
    }



    public void printUsage(PrintStream ps) {
        ps.println("\n");
        ps.println("Usage:");
        ps.println("    "+getClass().getName()+"   SourceXmlURI  XSLTransformURI");
        ps.println("\n");
    }

    public static void main(String args[]){

        Transformer parser = new Transformer();


        if(args.length!=2){
            parser.printUsage(System.err);
            System.exit(-1);
        }



        try {
            parser.transformURI(args[0],args[1]);
        } catch (Exception e) {
            e.printStackTrace(System.err);

        }

    }


    public void transformURI(String srcFile, String xslFile) throws Exception {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Document sourceDoc, xsltDoc;

        sourceDoc = getXMLDoc(srcFile);
        System.err.println("Got and parsed XML document: "+srcFile);
        xmlo.output(sourceDoc, System.err);

        xsltDoc = getXMLDoc(xslFile);
        System.err.println("Got and parsed XSL document: "+xslFile);
        xmlo.output(xsltDoc, System.err);


        System.err.println("Applying transform...");

        XSLTransformer transformer = new XSLTransformer(xsltDoc);
        Document result = transformer.transform(sourceDoc);
        xmlo.output(result, System.out);


    }


    public Document getXMLDoc(String s) throws Exception{


        // get a validating jdom parser to parse and validate the XML document.
        SAXBuilder parser = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
        parser.setFeature("http://apache.org/xml/features/validation/schema", false);

        Document doc;

        if(s.startsWith("http://")){
            System.err.println("Appears to be a URL: "+s);

            GetMethod request = new GetMethod(s);
            InputStream is=null;
            try {
                // Execute the method.
                int statusCode = httpClient.executeMethod(request);

                if (statusCode != HttpStatus.SC_OK) {
                  System.err.println("ERROR: Method failed " + request.getStatusLine());
                  doc = null;
                }
                else {
                	is = request.getResponseBodyAsStream();
                	doc = parser.build(is);
                }

                return doc;

            }
            finally {
            	if(is!=null)
            		is.close();
                System.err.println("Releasing Http connection.");
                request.releaseConnection();
            }

        }
        else {
            File file = new File(s);
            if(!file.exists()){
                throw new IOException("Cannot find file: "+ s);
            }

            if(!file.canRead()){
                throw new IOException("Cannot read file: "+ s);
            }
            doc = parser.build(new FileInputStream(file));

            return doc;

        }

    }










    public static String uriInfo(URI uri){

        String msg = "\n";


        msg += "URI: "+uri.toString()+"\n";
        msg += "  Authority:              "+uri.getAuthority()+"\n";
        msg += "  Host:                   "+uri.getHost()+"\n";
        msg += "  Port:                   "+uri.getPort()+"\n";
        msg += "  Path:                   "+uri.getPath()+"\n";
        msg += "  Query:                  "+uri.getQuery()+"\n";
        msg += "  hashCode:               "+uri.hashCode()+"\n";
        msg += "  Fragment:               "+uri.getFragment()+"\n";
        msg += "  RawAuthority:           "+uri.getRawAuthority()+"\n";
        msg += "  RawFragment:            "+uri.getRawFragment()+"\n";
        msg += "  RawPath:                "+uri.getRawPath()+"\n";
        msg += "  RawQuery:               "+uri.getRawQuery()+"\n";
        msg += "  RawSchemeSpecificPart:  "+uri.getRawSchemeSpecificPart()+"\n";
        msg += "  RawUSerInfo:            "+uri.getRawUserInfo()+"\n";
        msg += "  Scheme:                 "+uri.getScheme()+"\n";
        msg += "  SchemeSpecificPart:     "+uri.getSchemeSpecificPart()+"\n";
        msg += "  UserInfo:               "+uri.getUserInfo()+"\n";
        msg += "  isAbsoulte:             "+uri.isAbsolute()+"\n";
        msg += "  isOpaque:               "+uri.isOpaque()+"\n";
        msg += "  ASCIIString:            "+uri.toASCIIString()+"\n";

        try {
        msg += "  URL:                    "+uri.toURL()+"\n";
        } catch (Exception e) {

            e.printStackTrace();
        }

        return msg;
    }





}
