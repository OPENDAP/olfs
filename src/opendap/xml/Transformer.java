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



import net.sf.saxon.s9api.*;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.transform.XSLTransformer;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import java.net.URI;
import java.io.*;

/**
 * User: ndp
 * Date: Apr 23, 2008
 * Time: 9:22:11 AM
 */
public class Transformer {


    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(Transformer.class);
    }

    private Transformer(){
    }



    public void printUsage(PrintStream ps) {
        ps.println("\n");
        ps.println("Usage:");
        ps.println("    "+getClass().getName()+"   SourceXmlURI  XSLTransformURI");
        ps.println("\n");
    }

    public static void main(String args[]){

        Transformer  t = new Transformer();

         if(args.length!=2){
            t.printUsage(System.err);
            System.exit(-1);
        }
        try {
            jdomXsltTransfom(args[0],args[1],System.out);
            saxonXsltTransform(args[0],args[1],System.out);

        } catch (Exception e) {
            e.printStackTrace(System.err);

        }

    }


    public static void jdomXsltTransfom(String srcDocUri, String xslDocUri, OutputStream os) throws Exception {



        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Document sourceDoc, xsltDoc;

        sourceDoc = getXMLDoc(srcDocUri);
        log.debug("Got and parsed XML document: "+srcDocUri);
        log.debug(xmlo.outputString(sourceDoc));

        xsltDoc = getXMLDoc(xslDocUri);
        log.debug("Got and parsed XSL document: "+xslDocUri);
        log.debug(xmlo.outputString(xsltDoc));


        System.err.println("Applying transform...");

        XSLTransformer transformer = new XSLTransformer(xsltDoc);

        System.err.println("Transformer is an instance of  "+transformer.getClass().getName());



        Document result = transformer.transform(sourceDoc);
        xmlo.output(result, os);
        log.debug(xmlo.outputString(result));








    }


    public static Document getXMLDoc(String s) throws Exception{


        // get a validating jdom parser to parse and validate the XML document.
        SAXBuilder parser = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
        parser.setFeature("http://apache.org/xml/features/validation/schema", false);

        Document doc;

        if(s.startsWith("http://")){
            log.debug("Appears to be a URL: "+s);

            GetMethod request = new GetMethod(s);
            InputStream is=null;
            try {

                HttpClient httpClient = new HttpClient();
                // Execute the method.
                int statusCode = httpClient.executeMethod(request);

                if (statusCode != HttpStatus.SC_OK) {
                    log.error("HttpClient failed to executeMethod(). Status: " + request.getStatusLine());
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
                log.debug("Releasing Http connection.");
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



    public static String getXSLTImpl(){

        String str = "SystemProperty javax.xml.transform.TransformerFactory: \n";


        try {

            String impl = System.getProperty("javax.xml.transform.TransformerFactory");

            if(impl!=null){

                Class classDefinition = Class.forName(impl);
                javax.xml.transform.TransformerFactory s = (javax.xml.transform.TransformerFactory) classDefinition.newInstance();

                str += "    TransformerFactory class  = "+s.getClass().getName() +"\n";
                str += "    Transformer class         = "+s.newTransformer().getClass().getName()+"\n";
            }
            else {
                str += "    Java System Property Not Set.\n";
            }
        }
        catch(Exception e){
            e.printStackTrace(System.out);
            System.exit(1);
        }

        return str;

    }

    public static void saxonXsltTransform(String srcDocUri, String xslTransformUri, OutputStream os) throws IOException, SaxonApiException {


        log.debug("Performing transform using Saxon");

        Processor proc = new Processor(false);

        XsltTransformer trans = Transformer.getXsltTransformer(proc, xslTransformUri);

        XdmNode source = Transformer.getXdmNode(proc,srcDocUri);

        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputStream(os);

        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();

        log.debug("Output written to: "+os);

    }


    public static void saxonXsltTransform(InputStream srcDoc, String xslTransformUri, OutputStream os) throws IOException, SaxonApiException {


        log.debug("Performing transform using Saxon");

        Processor proc = new Processor(false);

        XsltTransformer trans = Transformer.getXsltTransformer(proc, xslTransformUri);

        XdmNode source = proc.newDocumentBuilder().build(new StreamSource(srcDoc));

        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputStream(os);

        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();

        log.debug("Output written to: "+os);

    }


    public static void saxonXsltTransform(InputStream srcDoc, InputStream xslTransform, OutputStream os) throws IOException, SaxonApiException {


        log.debug("Performing transform using Saxon");

        Processor proc = new Processor(false);

        XsltCompiler comp = proc.newXsltCompiler();

        XsltExecutable exp = comp.compile(new StreamSource(xslTransform));
        XsltTransformer trans = exp.load();

        XdmNode source = proc.newDocumentBuilder().build(new StreamSource(srcDoc));

        Serializer out = new Serializer();
        out.setOutputProperty(Serializer.Property.METHOD, "xml");
        out.setOutputProperty(Serializer.Property.INDENT, "yes");
        out.setOutputStream(os);

        trans.setInitialContextNode(source);
        trans.setDestination(out);
        trans.transform();

        log.debug("Output written to: "+os);

    }






    public static XdmNode getXdmNode(Processor proc, String srcDocUri) throws IOException, SaxonApiException {

        XdmNode source;

        if(srcDocUri.startsWith("http://")){
            log.debug("Appears to be a URL: "+srcDocUri);

            GetMethod request = new GetMethod(srcDocUri);
            InputStream is = null;
            try {

                HttpClient httpClient = new HttpClient();
                // Execute the method.
                int statusCode = httpClient.executeMethod(request);

                if (statusCode != HttpStatus.SC_OK) {
                    log.error("HttpClient failed to executeMethod(). Status: " + request.getStatusLine());
                  source = null;
                }
                else {
                	is = request.getResponseBodyAsStream();
                    source = proc.newDocumentBuilder().build(new StreamSource(is));
                }

                return source;

            }
            finally {
            	if(is!=null)
            		is.close();
                log.debug("Releasing Http connection.");
                request.releaseConnection();
            }

        }
        else {
            File file = new File(srcDocUri);
            if(!file.exists()){
                throw new IOException("Cannot find file: "+ srcDocUri);
            }

            if(!file.canRead()){
                throw new IOException("Cannot read file: "+ srcDocUri);
            }
            source = proc.newDocumentBuilder().build(new StreamSource(file));

            return source;

        }

    }






    public static XsltTransformer getXsltTransformer(Processor proc, String xslTransformUri) throws IOException, SaxonApiException {

        XsltCompiler comp = proc.newXsltCompiler();
        XsltExecutable exp;
        XsltTransformer trans;


        if(xslTransformUri.startsWith("http://")){
            log.debug("Appears to be a URL: "+xslTransformUri);

            GetMethod request = new GetMethod(xslTransformUri);
            InputStream is = null;
            try {

                HttpClient httpClient = new HttpClient();

                // Execute the method.
                int statusCode = httpClient.executeMethod(request);

                if (statusCode != HttpStatus.SC_OK) {
                    log.error("HttpClient failed to executeMethod(). Status: " + request.getStatusLine());
                  trans = null;
                }
                else {
                	is = request.getResponseBodyAsStream();
                    exp = comp.compile(new StreamSource(is));
                    trans = exp.load();

                }

                return trans;

            }
            finally {
            	if(is!=null)
            		is.close();
                log.debug("Releasing Http connection.");
                request.releaseConnection();
            }

        }
        else {
            File file = new File(xslTransformUri);
            if(!file.exists()){
                throw new IOException("Cannot find file: "+ xslTransformUri);
            }

            if(!file.canRead()){
                throw new IOException("Cannot read file: "+ xslTransformUri);
            }
            exp = comp.compile(new StreamSource(file));
            trans = exp.load();

            return trans;

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
