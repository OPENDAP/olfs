/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opendap.soap;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.Message;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.commons.cli.*;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Enumeration;
import java.io.*;

import opendap.dap.XMLparser.DDSXMLParser;
import opendap.dap.*;
import opendap.servers.ascii.asciiFactory;
import opendap.servers.ascii.toASCII;

import javax.activation.DataHandler;
import javax.activation.CommandInfo;


/**
 * Simple test driver for our message service.
 */
public class TestClient {


    private static String defaultURL = "http://localhost:8080/opendap/s4/";


    private String hostUrl;
    private String name;
    private String constraint;
    private String fileName;
    private int requestType;

    private boolean verbose;
    private boolean runTests;

    private static final int GET_DDX             = 0;
    private static final int GET_DATA            = 1;
    private static final int GET_THREDDS_CATALOG = 2;



    public TestClient(String[] args){

        Options opts = buildCommandLineOptions();
        processCmdLine(args,opts);

        System.out.println(status());

    }


    private void processCmdLine(String[] args, Options opts){
        CommandLineParser parser = new GnuParser();
        CommandLine line;
        try {
            // parse the command line arguments
            line = parser.parse( opts, args );

            String val;

            verbose = line.hasOption("v");
            runTests = line.hasOption("tests");


            if(line.hasOption("t")){
                val = line.getOptionValue( "t" );
                if(val.equals("x")){
                    requestType = GET_DDX;
                }
                else if(val.equals("d")){
                    requestType = GET_DATA;
                }
                else if(val.equals("c")){
                    requestType = GET_THREDDS_CATALOG;
                }
                else {
                    throw new ParseException("-t must have an argument of \"x\", \"d\", or \"c\".");
                }
            }



            if(line.hasOption("n"))
                name = line.getOptionValue( "n" );
            else
                name = null;

            if(line.hasOption("h"))
                hostUrl = line.getOptionValue( "h" );
            else
                hostUrl = defaultURL;

            if(line.hasOption("ce"))
                constraint = line.getOptionValue( "ce" );
            else
                constraint = "";


            if(line.hasOption("f"))
                fileName = line.getOptionValue( "f" );
            else
                fileName = null;

            if(fileName==null  && name==null && !runTests) {
                throw new ParseException("You must provide the either the name of a data product to request, " +
                        "or the name of a file containing a batch request.");
            }


        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            usage(opts);
            System.exit(1);
        }


    }

    private static Options buildCommandLineOptions(){
        Options opts = new Options();

        opts.addOption("v", false,"Turn on verbose output.");
        opts.addOption("tests", false,"Run canned tests.");


       OptionBuilder.withArgName( " x | d | c " );
       OptionBuilder.hasArg();
       OptionBuilder.withDescription("The type of the request, must be one of the following:\n" +
               "  x - To request a DDX.\n" +
               "  d - To request data.\n" +
               "  c - To request a THREDDS catalog.\n" +
               "(IF A BATCH REQUEST FILE IS NOT SPECIFIED, THEN THIS IS A REQUIRED ARGUMENT.)");
       opts.addOption(OptionBuilder.create( "t" ));


        OptionBuilder.withArgName( "targethost" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("The URL of (http://host:port/servlet/name) of a SOAP enabled OPeNDAP Server. " +
                "If not specified will default to http://localhost:8080/opendap/s4/" );
        opts.addOption(OptionBuilder.create( "h" ));




        OptionBuilder.withArgName( "name" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("The name of the data product to get from the server." +
                "If the request is for a DDX or data, then the name should be the name of a dataset. " +
                "If the request is for a THREDDS catalog then the name should be the name (or path " +
                "if you will) of a collection of datasets.(IF A BATCH REQUEST FILE IS NOT SPECIFIED, " +
                "THEN THIS IS A REQUIRED ARGUMENT.)" );
        opts.addOption(OptionBuilder.create( "n" ));



        OptionBuilder.withArgName( "constraint" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("The constraint expression to use when with a DDX or DATA request " +
                "will be ignored if the request is for a THREDDS catalog.");
         opts.addOption(OptionBuilder.create( "ce" ));





        OptionBuilder.withArgName( "filename" );
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("The name of a file containing one or more XML Request elements to send" +
                "to the server.");
        opts.addOption(OptionBuilder.create( "f" ));

        return opts;

    }

    public String status(){
        String d = "";
        String s = "";

        s += d+"TestClient STATUS: \n";
        s += d+"    Target Host:           " + hostUrl + "\n";

        if(fileName == null){
            switch(requestType){
                case GET_DDX:
                    s += d+"    Request Type:          GetDDX\n";
                    s += d+"    Dataset Name:          "+name+"\n";
                    s += d+"    Constraint Expression: "+constraint+"\n";

                    break;
                case GET_DATA:
                    s += d+"    Request Type:          GetDATA\n";
                    s += d+"    Dataset Name:          "+name+"\n";
                    s += d+"    Constraint Expression: "+constraint+"\n";
                    break;
                case GET_THREDDS_CATALOG:
                    s += d+"    Request Type:          GetTHREDDSCatalog\n";
                    s += d+"    Collection Name:       "+name+"\n";
                    break;
                default:
                    s = "";
                    break;
            }
        }
        else {

            s += d+"    Request File:         " + fileName + "\n";

        }
        return s;

    }




    private static void usage(Options opts){
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TestClient", opts );

    }


    public static void main(String[] args) throws Exception {

        TestClient tc = new TestClient(args);

        if(tc.runTests){
            tc.testMsg01();
            tc.testMsg02();
            tc.testMsg03();

        }
        else {
            if(tc.fileName != null)
                tc.sendRequestFromFile();
            else
                tc.sendCmdLineRequest();
        }



    }



    public void sendRequestFromFile() throws Exception {

        FileInputStream  fis = new FileInputStream(new File(fileName));


        SOAPEnvelope se = new SOAPEnvelope(fis);

        Service service = new Service();

        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new URL(hostUrl) );

        Message msg = new Message(se);


        if(verbose){
            System.out.println("- - - - - - - - - - - - - - - ");
            System.out.println("Sending this SOAP message:");
            msg.writeTo(System.out);
            System.out.println();
            System.out.println();
            System.out.println("- - - - - - - - - - - - - - - ");
        }
        call.invoke(msg);

        //SoapUtils.probeMessage(call.getResponseMessage());


        handleResponse(call.getResponseMessage());

    }

    public void sendCmdLineRequest() throws Exception {




        //Create the data for the attached file.

        Service service = new Service();

        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new URL(hostUrl) );


        SOAPEnvelope se = new SOAPEnvelope();
        //SoapUtils.addDDXRequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u,v");


        switch(requestType){
            case GET_DDX:
                SoapUtils.addDDXRequest((SOAPBody)se.getBody(),name,constraint);
                break;

            case GET_DATA:
                SoapUtils.addDATARequest((SOAPBody)se.getBody(),name,constraint);
                break;

            case GET_THREDDS_CATALOG:
                SoapUtils.addTHREDDSCatalogRequest((SOAPBody)se.getBody(),name);
                break;

            default:
                break;
        }


        Message msg = new Message(se);


        if(verbose){
            System.out.println("- - - - - - - - - - - - - - - ");
            System.out.println("Sending this SOAP message:");
            msg.writeTo(System.out);
            System.out.println();
            System.out.println();
            System.out.println("- - - - - - - - - - - - - - - ");
        }
        call.invoke(msg);

        //SoapUtils.probeMessage(call.getResponseMessage());


        handleResponse(call.getResponseMessage());

    }









    public  void testMsg01() throws Exception {

        System.out.println();
        System.out.println("*************************************************************************************");
        System.out.println("                                   testMsg01()");
        System.out.println(". . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .");




        DOMOutputter domO = new DOMOutputter();


        SOAPBodyElement[] input = new SOAPBodyElement[3];

        Element req = SoapUtils.getDDXRequestElement("/nc/fnoc1.nc","u,v");
        Document doc = new Document(req);
        input[0] = new SOAPBodyElement(domO.output(doc).getDocumentElement());


        req = SoapUtils.getDDXRequestElement("nc/fnoc2.nc","");
        doc = new Document(req);
        input[1] = new SOAPBodyElement(domO.output(doc).getDocumentElement());


        req = SoapUtils.getCatalogRequestElement("nc/Test");
        doc = new Document(req);
        input[2] = new SOAPBodyElement(domO.output(doc).getDocumentElement());

        Service  service = new Service();

        System.out.println("Service class is a: "+service.getClass().getName());
        System.out.println("Service.createCall() returns a: "+service.createCall().getClass().getName());


        Call     call    = (Call) service.createCall();
        System.out.println("Call class is a: "+call.getClass().getName());


        call.setTargetEndpointAddress( new URL(hostUrl) );
        //Vector          elems = (Vector) call.invoke( input );
        call.invoke( input );


        SoapUtils.probeMessage(call.getResponseMessage());


        System.out.println("*************************************************************************************");
        System.out.println();


    }






    public  void testMsg02() throws Exception {
        System.out.println();
        System.out.println("*************************************************************************************");
        System.out.println("                                   testMsg02()");
        System.out.println(". . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .");





        //Create the data for the attached file.


        SOAPEnvelope se = new SOAPEnvelope();
        SoapUtils.addDDXRequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u,v");
        SoapUtils.addTHREDDSCatalogRequest((SOAPBody)se.getBody(),"/nc/");
        SoapUtils.addDDXRequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","time");
        SoapUtils.addDATARequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u");
        SoapUtils.addDATARequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","v");


        Message msg = new Message(se);


        System.out.println("- - - - - - - - - - - - - - - ");
        System.out.println("Sending this SOAP message:");
        msg.writeTo(System.out);
        System.out.println();
        System.out.println();
        System.out.println("- - - - - - - - - - - - - - - ");


        Service service = new Service();
        Call call = (Call) service.createCall();
        call.setTargetEndpointAddress( new URL(hostUrl) );
        call.invoke(msg);

        //SoapUtils.probeMessage(call.getResponseMessage());


        handleResponse(call.getResponseMessage());





        System.out.println("*************************************************************************************");
        System.out.println();
    }




    public  void testMsg03() throws Exception {
        System.out.println();
        System.out.println("*************************************************************************************");
        System.out.println("                                   testMsg03()");
        System.out.println(". . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .");


        DDS dds;


        System.out.println("\n\n\n-----------------------\nUsing the getDDX() method:\n");
        dds = SoapUtils.getDDX(hostUrl,"/nc/fnoc1.nc","",verbose);

        System.out.println("From the returned DDX I get this DDS:\n");
        dds.print(System.out);
        System.out.println("\n\nFrom the returned DDX I get this DDX:\n");
        dds.printXML(System.out);



        System.out.println("\n\n\n-----------------------\nUsing the getDATA() method:\n");

        dds = SoapUtils.getDATA(hostUrl,"/nc/fnoc1.nc","u,v",verbose);
        System.out.println("From the returned DDX I get this DDS:\n");
        dds.print(System.out);
        System.out.println("\n\nFrom the returned DDX I get this DDX:\n");
        dds.printXML(System.out);
        System.out.println("\n\nFrom the returned DDX I get these values:\n");
        dds.printVal(System.out,"");


        System.out.println("\n\n\n-----------------------\nUsing the getTHREDDSCatalog() method:\n");

        Document catalog = SoapUtils.getTHREDDSCatalog(hostUrl,"/nc",verbose);

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        System.out.println("I got this catalog:\n");
        xmlo.output(catalog,System.out);








        System.out.println("*************************************************************************************");
        System.out.println();
    }



    public void handleResponse(Message responseMsg) throws Exception {


        DOMBuilder db = new DOMBuilder();
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        Element d = db.build(responseMsg.getSOAPEnvelope().getAsDOM());
        d.detach();

        Document respDoc = new Document(d);

        ElementFilter exceptionFilter = new ElementFilter("OPeNDAPException");
        Iterator i = respDoc.getDescendants(exceptionFilter);

        if(i.hasNext())
            handleErrors(i);
        else {

            Element soapBody = respDoc.getRootElement().getChild("Body",XMLNamespaces.getDefaultSoapEnvNamespace());

            List reqs = soapBody.getChildren("Request",XMLNamespaces.getOpendapSoapNamespace());
            List resps = soapBody.getChildren("Response",XMLNamespaces.getOpendapSoapNamespace());


            for(Object r : reqs){

                Element request = (Element) r;
                String reqID = request.getAttributeValue("reqID",osnms);
                if(verbose) System.out.println("\nRequest reqID: "+reqID);

                Element response = null;

                for(Object rp : resps){
                    Element rsp = (Element) rp;
                    String respReqID = rsp.getAttributeValue("reqID",osnms);
                    if(reqID.equals(respReqID)){
                        if(verbose) System.out.println("Found Matching Response: "+respReqID);
                        response = rsp;
                        break;
                    }
                }
                if(response == null){
                    throw new Exception("BAD THINGS HAPPENED! " +
                            "The Server did not return an error, or a response to Request <"+reqID+">");
                }


                List cmds = request.getChildren();
                if(cmds.size() == 1){
                    Element cmd = (Element) cmds.get(0);
                    if (cmd.getName().equals("GetDATA")) {
                        handleDATAResponse(request, response, responseMsg);
                    }
                    else if (cmd.getName().equals("GetDDX")) {
                        handleDDXResponse(request,response);
                    }
                    else if (cmd.getName().equals("GetTHREDDSCatalog")) {
                        handleTHREDDSCatalogResponse(request,response);
                    }

                }

            }

        }

    }


    public  void handleDATAResponse_OLD(Element request, Element response, Message msg) throws Exception {


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        if(verbose) {
            System.out.println("Received DATA response!");
            System.out.println("Request command:");
            xmlo.output(request,System.out);
            System.out.println();
        }

        String[] s = msg.getMimeHeaders().getHeader("XDODS-Server");
        String serverVersion = s[0];

        Namespace osd2nms = XMLNamespaces.getOpendapDAP2Namespace();

        Element ds = response.getChild("Dataset",osd2nms);
        Element blob = ds.getChild("dodsBLOB",osd2nms);

        String cid = blob.getAttributeValue("href");
        cid = cid.substring(4,cid.length());

        if(verbose) System.out.println("Searching for Attachment with cid: "+cid);

        AttachmentPart data = null;
        Iterator i = msg.getAttachments();

        while(i.hasNext()){
            AttachmentPart ap = (AttachmentPart) i.next();

            String[] attachmentCid = ap.getMimeHeader("Content-ID");

            if(cid.equals(attachmentCid[0])){
                 if(verbose) System.out.println("Found it.");
                data = ap;
                break;
            }
        }
         if(verbose) System.out.println();


        if(data==null)
            throw new Exception("Error! Server did not return data in conjunction with GetDATA Response.");

        ds.detach();

        Document ddxDoc = new Document(ds);

        DDSXMLParser parser = new DDSXMLParser(XMLNamespaces.OpendapDAP2NamespaceString);

        ServerVersion sv = new ServerVersion(serverVersion);
        DDS dds = new DDS();

        parser.parse(ddxDoc,dds,new asciiFactory(),false);

        dds.deserialize(new DataInputStream(data.getDataHandler().getInputStream()), sv, null);


        System.out.println("DDS:\n"+dds.getDDSText());
        System.out.println("DATA:");
        Enumeration e = dds.getVariables();
        PrintWriter pw = new PrintWriter(System.out);

        while(e.hasMoreElements()){
            BaseType bt = (BaseType)e.nextElement();
            ((toASCII)bt).toASCII(pw,true,null,true);
        }
        pw.flush();

        //System.out.println();

        //dds.printVal(System.out);

        System.out.println();

    }

    public  void handleDATAResponse(Element request, Element response, Message msg) throws Exception {


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        if(verbose) {
            System.out.println("Received DATA response!");
            System.out.println("Request command:");
            xmlo.output(request,System.out);
            System.out.println("\n");
        }

        String[] s = msg.getMimeHeaders().getHeader("XDODS-Server");
        String serverVersion = s[0];

        Namespace osd2nms = XMLNamespaces.getOpendapDAP2Namespace();
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();



        // Find the href that contains the contentID for the attachment containing the DDX
        String cid = response.getAttributeValue("href",osnms);
        cid = cid.substring(4,cid.length());

        if(verbose) System.out.println("Searching for DDX Attachment (cid: "+cid+")");

        AttachmentPart data = null;
        Iterator i = msg.getAttachments();

        while(i.hasNext()){
            AttachmentPart ap = (AttachmentPart) i.next();

            String[] attachmentCid = ap.getMimeHeader("Content-ID");

            if(cid.equals(attachmentCid[0])){
                data = ap;
                break;
            }
        }
        if(verbose) System.out.println();

        if(data==null)
            throw new Exception("Error! Server did not return a DDX Attachment in conjunction with GetDATA Response.");


        if(verbose){
            System.out.println("Found DDX attachment. ContentType: " +
                    data.getContentType() + "\n" +
                    "AttachmentPart.getDataHandler() returns a class of type: " +
                    data.getDataHandler().getClass().getName());
        }

        DataHandler dh = data.getDataHandler();


        if(verbose){
            CommandInfo[] cia = dh.getAllCommands();
            System.out.print("\nDataHandler Command Info: ");
            if(cia.length ==0)
                System.out.println("None Found.");
            else
                System.out.println();

            for (CommandInfo ci : cia) {
                System.out.println("    name: "+ci.getCommandName()+"    class: "+ci.getCommandClass());
            }
            System.out.println();

        }

        SAXBuilder sb = new SAXBuilder();

        Document ddxDoc = sb.build(dh.getInputStream());

        DDSXMLParser parser = new DDSXMLParser(XMLNamespaces.OpendapDAP2NamespaceString);

        ServerVersion sv = new ServerVersion(serverVersion);
        DDS dds = new DDS();

        parser.parse(ddxDoc,dds,new asciiFactory(),false);



        Element ds = ddxDoc.getRootElement();

        Element blob = ds.getChild("dodsBLOB",osd2nms);

        // Find the href that contains the contentID for the attachment containing the DATA        
        cid = blob.getAttributeValue("href");
        cid = cid.substring(4,cid.length());

        if(verbose) System.out.println("Searching for DATA Attachment with cid: "+cid);

        data = null;
        i = msg.getAttachments();

        while(i.hasNext()){
            AttachmentPart ap = (AttachmentPart) i.next();

            String[] attachmentCid = ap.getMimeHeader("Content-ID");

            if(cid.equals(attachmentCid[0])){
                data = ap;
                break;
            }
        }
        if(verbose) System.out.println();


        if(data==null)
            throw new Exception("Error! Server did not return data in conjunction with GetDATA Response.");

        if(verbose){
            System.out.println("Found DATA attachment. ContentType: " +
                data.getContentType() + "\n" +
                "AttachmentPart.getDataHandler() returns a class of type: " +
                data.getDataHandler().getClass().getName() + "\n" +
                "AttachmentPart.getContent() returns a class of type: " +
                data.getContent().getClass().getName());
        }

        dh = data.getDataHandler();

        if(verbose){
            CommandInfo[] cia = dh.getAllCommands();
            System.out.print("\nDataHandler Command Info: ");
            if(cia.length ==0)
                System.out.println("None Found.");
            else
                System.out.println();

            for (CommandInfo ci : cia) {
                System.out.println("    name: "+ci.getCommandName()+"    class: "+ci.getCommandClass());
            }
            System.out.println();

        }

        dds.deserialize(new DataInputStream(dh.getInputStream()), sv, null);


        System.out.println("DDS:\n"+dds.getDDSText());
        System.out.println("DATA:");
        Enumeration e = dds.getVariables();
        PrintWriter pw = new PrintWriter(System.out);

        while(e.hasMoreElements()){
            BaseType bt = (BaseType)e.nextElement();
            ((toASCII)bt).toASCII(pw,true,null,true);
        }
        pw.flush();

        //System.out.println();

        //dds.printVal(System.out);

        System.out.println();

    }



    public  void handleDDXResponse(Element request, Element response) throws IOException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element ddx = response.getChild("Dataset",  XMLNamespaces.getOpendapDAP2Namespace());

        if(verbose) {
            System.out.println("Received DDX response!");
            System.out.println("Request command:");
            xmlo.output(request,System.out);
            System.out.println();
            System.out.println();
            System.out.println("DDX Returned:");
        }
        xmlo.output(ddx,System.out);
        System.out.println();
        System.out.println();


    }


    public  void handleTHREDDSCatalogResponse(Element request, Element response) throws IOException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element catalog = response.getChild("catalog",XMLNamespaces.getThreddsCatalogNamespace());

        if(verbose){
            System.out.println("Received THREDDS catalog response!");
            System.out.println("Request command:");
            xmlo.output(request,System.out);
            System.out.println();
            System.out.println();
            System.out.println("THREDDS Catalog Returned:");
        }

        xmlo.output(catalog,System.out);
        System.out.println();
        System.out.println();
    }







    private static void handleErrors(Iterator i) throws IOException {

        System.out.println("\n\n\n\nOUCH! Errors!");


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        int cnt = 0;
        while(i.hasNext()){
            Element err = (Element) i.next();

            System.out.println("\n--------------- ERROR "+ cnt++ +": ");
            xmlo.output(err,System.out);
            System.out.println("\n---------------");
        }


    }


}
