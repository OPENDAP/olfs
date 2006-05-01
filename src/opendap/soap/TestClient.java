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

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.DOMBuilder;
import org.jdom.output.DOMOutputter;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.IOException;

import opendap.dap.XMLparser.DDSXMLParser;
import opendap.dap.*;
import opendap.servers.ascii.asciiFactory;
import opendap.servers.ascii.toASCII;

/**
 * Simple test driver for our message service.
 */
public class TestClient {


    private static String defaultURL = "http://localhost:8080/opendap/s4/";

    public static void testMsg01(String[] args) throws Exception {

        System.out.println();
        System.out.println("*************************************************************************************");
        System.out.println("                                   testMsg01()");
        System.out.println(". . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .");


        String url = args.length>0?args[0]:defaultURL;


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


        call.setTargetEndpointAddress( new URL(url) );
        //Vector          elems = (Vector) call.invoke( input );
        call.invoke( input );


        SoapUtils.probeMessage(call.getResponseMessage());


        System.out.println("*************************************************************************************");
        System.out.println();


    }






    public static void testMsg02(String[] args) throws Exception {
        System.out.println();
        System.out.println("*************************************************************************************");
        System.out.println("                                   testMsg02()");
        System.out.println(". . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .");


        String url = args.length>0?args[0]:defaultURL;



        //Create the data for the attached file.

        Service service = new Service();

        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new URL(url) );


        SOAPEnvelope se = new SOAPEnvelope();
        //SoapUtils.addDDXRequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u,v");
        SoapUtils.addTHREDDSCatalogRequest((SOAPBody)se.getBody(),"/nc/");
        SoapUtils.addDATARequest((SOAPBody)se.getBody(),"nc/fnoc1.nc","time");
        SoapUtils.addDATARequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u");
        SoapUtils.addDATARequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","v");


        Message msg = new Message(se);


        System.out.println("- - - - - - - - - - - - - - - ");
        System.out.println("Sending this SOAP message:");
        msg.writeTo(System.out);
        System.out.println();
        System.out.println();
        System.out.println("- - - - - - - - - - - - - - - ");

        call.invoke(msg);

        //SoapUtils.probeMessage(call.getResponseMessage());


        handleResponse(call.getResponseMessage());





        System.out.println("*************************************************************************************");
        System.out.println();
    }



    public static void handleResponse(Message responseMsg) throws Exception {



        DOMBuilder db = new DOMBuilder();
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        Element d = db.build(responseMsg.getSOAPEnvelope().getAsDOM());
        d.detach();

        Document respDoc = new Document(d);

        ElementFilter exceptionFilter = new ElementFilter("OPeNDAPException");
        Iterator i = respDoc.getDescendants(exceptionFilter);

        if(i.hasNext())
            handleErrors(respDoc);
        else {

            Element soapBody = respDoc.getRootElement().getChild("Body",XMLNamespaces.getDefaultSoapEnvNamespace());

            List reqs = soapBody.getChildren("Request",XMLNamespaces.getOpendapSoapNamespace());
            List resps = soapBody.getChildren("Response",XMLNamespaces.getOpendapSoapNamespace());


            for(Object r : reqs){

                Element request = (Element) r;
                String reqID = request.getAttributeValue("reqID",osnms);
                System.out.println("\nRequest reqID: "+reqID);

                Element response = null;

                for(Object rp : resps){
                    Element rsp = (Element) rp;
                    String respReqID = rsp.getAttributeValue("reqID",osnms);
                    if(reqID.equals(respReqID)){
                        System.out.println("Found Matching Response: "+respReqID);
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


    public static void handleDATAResponse(Element request, Element response, Message msg) throws Exception {


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        System.out.println("Received DATA response!");
        System.out.println("Request command:");
        xmlo.output(request,System.out);


        String[] s = msg.getMimeHeaders().getHeader("XDODS-Server");
        String serverVersion = s[0];

        Namespace osd2nms = XMLNamespaces.getOpendapDAP2Namespace();

        Element ds = response.getChild("Dataset",osd2nms);
        Element blob = ds.getChild("dodsBLOB",osd2nms);

        String cid = blob.getAttributeValue("href");
        cid = cid.substring(4,cid.length());

        System.out.println("Searching for Attachment with cid: "+cid);

        AttachmentPart data = null;
        Iterator i = msg.getAttachments();

        while(i.hasNext()){
            AttachmentPart ap = (AttachmentPart) i.next();

            String[] attachmentCid = ap.getMimeHeader("Content-ID");

            if(cid.equals(attachmentCid[0])){
                System.out.println("Found it.");
                data = ap;
            }
        }
        System.out.println();


        if(data==null)
            throw new Exception("Error! Server did not return data in conjunction with GetDATA Response.");

        ds.detach();

        Document ddxDoc = new Document(ds);

        DDSXMLParser parser = new DDSXMLParser(XMLNamespaces.OpendapDAP2NamespaceString);

        ServerVersion sv = new ServerVersion(serverVersion);
        DDS dds = new DDS();

        parser.parse(ddxDoc,dds,new asciiFactory(),false);

        dds.deserialize(new DataInputStream((InputStream)data.getContent()), sv, null);


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


    public static void handleDDXResponse(Element request, Element response) throws IOException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element ddx = response.getChild("Dataset",  XMLNamespaces.getOpendapDAP2Namespace());

        System.out.println("Received DDX response!");
        System.out.println("Request command:");
        xmlo.output(request,System.out);
        System.out.println();
        System.out.println();
        System.out.println("DDX Returned:");
        xmlo.output(ddx,System.out);
        System.out.println();
        System.out.println();


    }


    public static void handleTHREDDSCatalogResponse(Element request, Element response) throws IOException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Element catalog = response.getChild("catalog",XMLNamespaces.getThreddsCatalogNamespace());

        System.out.println("Received THREDDS catalog response!");
        System.out.println("Request command:");
        xmlo.output(request,System.out);
        System.out.println();
        System.out.println();
        System.out.println("THREDDS Catalog Returned:");
        xmlo.output(catalog,System.out);
        System.out.println();
        System.out.println();
    }







    private static void handleErrors(Document response) {
        //To change body of created methods use File | Settings | File Templates.
    }


    public static void main(String[] args) throws Exception {
        //TestClient.testMsg01(args);
        TestClient.testMsg02(args);
    }
}
