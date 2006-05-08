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

package opendap.soap;

import org.jdom.Element;
import org.jdom.Text;
import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.filter.ElementFilter;
import org.jdom.input.DOMBuilder;

import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Service;
import org.apache.axis.client.Call;
import org.apache.axis.message.*;

import javax.xml.soap.SOAPException;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.AttachmentPart;

import java.rmi.server.UID;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.net.URL;

import opendap.dap.XMLparser.DDSXMLParser;
import opendap.dap.ServerVersion;
import opendap.dap.DDS;
import opendap.dap.BaseTypeFactory;
import opendap.dap.DefaultFactory;
import opendap.servers.ascii.asciiFactory;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Apr 11, 2006
 * Time: 12:04:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class SoapUtils {

    public static void probeMessage(Message m) throws IOException, SOAPException {
        System.out.println("\n\n\nMESSAGE Probe:");
        System.out.println("    SOAPMessage:    " + m.getClass().getName());
        System.out.println("    getMessageType():   " + m.getMessageType());
        System.out.println("    getContentDescription():   " + m.getContentDescription());
        System.out.println("    getContentLength(): " + m.getContentLength());
        System.out.println("    getAttachmentImplClassName(): " + Message.getAttachmentImplClassName());
        System.out.println("");
        javax.xml.soap.MimeHeaders mh = m.getMimeHeaders();
        System.out.print("    MIME Headers: ");
        if(mh != null){
            Iterator mhi = mh.getAllHeaders();
            if(mhi.hasNext()){
                System.out.println();
                while(mhi.hasNext()){
                    MimeHeader mhdr = (MimeHeader)mhi.next();
                    System.out.println("        "+mhdr.getName()+": "+mhdr.getValue());
                }
            }
            else
                System.out.println("none.");
        }
        else
            System.out.println("none.");
        System.out.println("");



        probeMessageContext(m.getMessageContext(), "    ");


        System.out.println("");

        org.apache.axis.message.SOAPEnvelope se = m.getSOAPEnvelope();
        org.apache.axis.message.SOAPBody sb = (org.apache.axis.message.SOAPBody) m.getSOAPBody();
        org.apache.axis.message.SOAPHeader sh = (org.apache.axis.message.SOAPHeader) m.getSOAPHeader();
        SOAPPart sp = m.getSOAPPart();

        System.out.println("    SOAPEnvelope:   " + se.getClass().getName());
        System.out.println("    SOAPBody:       " + sb.getClass().getName());
        System.out.println("    SOAPHeader:     " + sh.getClass().getName());
        System.out.println("    SOAPPart:       " + sp.getClass().getName());
        System.out.println("");

        System.out.println("-----------------  SOAP Envelope (START) ---------------- ");
        System.out.println(m.getSOAPEnvelope());
        System.out.println("------------------- SOAP Envelope (END) ----------------- ");


        int acount = m.countAttachments();
        int cnt=0;
        System.out.println("\n\n--------------------- Attachments ("+acount+") ----------------------");
        if(acount>0){

            Iterator i = m.getAttachments();
            AttachmentPart ap;
            while(i.hasNext()){

                ap = (AttachmentPart) i.next();
                System.out.println("--- Attachment["+cnt+"]:\n");
                Iterator j = ap.getAllMimeHeaders();
                while(j.hasNext()){
                    MimeHeader mhdr = (MimeHeader)j.next();
                    System.out.println("    "+mhdr.getName()+": "+mhdr.getValue());
                }
                System.out.println("    getContent() returns Class: "+ap.getContent().getClass().getName());

                InputStream is = (InputStream) ap.getContent();


                byte[] buf = new byte[is.available()];

                int ret = is.read(buf);


                System.out.println("\nAttachment Content ["+is.available()+" bytes]:");
                System.out.println("buf[0]: "+buf[0]+"   b[1]: "+buf[1]+"   b[2]: "+buf[2]+"   buf["+(buf.length-1)+"]: "+buf[buf.length-1]);
                System.out.print("-->");
                System.out.println(new String(buf));
                System.out.println("<--");

                cnt++;


            }

            System.out.println("\n--------------------- Attachments (END) ----------------------");

        }

        System.out.println("\n\n\n");
    }




    public static void probeMessageContext(MessageContext mc, String n){
        System.out.print(n+"MessageContext: ");
        if(mc == null){
            System.out.println("null");
            return;
        }
        System.out.println();


        System.out.println(n+n+"getEncodingStyle(): "+mc.getEncodingStyle());
        System.out.println(n+n+"getMaintainSession(): "+mc.getMaintainSession());



        Iterator  i = mc.getAllPropertyNames();
        System.out.println(n+n+"Property Names:   "+ (i==null?"none":"") );
        String prop;
        Object val;
        while(i!=null && i.hasNext()){
            prop = (String) i.next();
            val  = mc.getProperty(prop);
            System.out.println(n+n+n+prop+" = "+val);
        }

        System.out.println(n+n+"getSOAPActionURI(): "+mc.getSOAPActionURI());
        System.out.println(n+n+"getTargetService(): "+mc.getTargetService());
        System.out.println(n+n+"getTransportName(): "+mc.getTransportName());


        System.out.println(n+n+"getUsername(): "+mc.getUsername());

        System.out.println(n+n+"isClient(): "+mc.isClient());
        System.out.println(n+n+"isEncoded(): "+mc.isEncoded());
        System.out.println(n+n+"isHighFidelity(): "+mc.isHighFidelity());


    }













    public static void addDDXRequest(SOAPBody b, String datasetName, String constraintExpression) throws SOAPException, SOAPException {


        PrefixedQName bodyName = new PrefixedQName(XMLNamespaces.OpendapSoapNamespaceString,"Request", "ons");
        SOAPBodyElement bodyElement = (SOAPBodyElement) b.addBodyElement(bodyName);

        UID reqID = new UID();

        bodyElement.addAttribute(new PrefixedQName(XMLNamespaces.OpendapSoapNamespaceString,"reqID", "ons"),reqID.toString());

        MessageElement cmd =  (MessageElement) bodyElement.addChildElement("GetDDX");
        MessageElement dataset =  (MessageElement) cmd.addChildElement("DataSet");
        MessageElement name  = (MessageElement) dataset.addChildElement("name");
        name.addTextNode(datasetName);

        MessageElement ce  = (MessageElement) dataset.addChildElement("ConstraintExpression");
        ce.addTextNode(constraintExpression);


    }



    public static void addDATARequest(SOAPBody b, String datasetName, String constraintExpression) throws SOAPException, SOAPException {


        PrefixedQName bodyName = new PrefixedQName(XMLNamespaces.OpendapSoapNamespaceString,"Request", "ons");
        SOAPBodyElement bodyElement = (SOAPBodyElement) b.addBodyElement(bodyName);

        UID reqID = new UID();

        bodyElement.addAttribute(new PrefixedQName(XMLNamespaces.OpendapSoapNamespaceString,"reqID", "ons"),reqID.toString());

        MessageElement cmd =  (MessageElement) bodyElement.addChildElement("GetDATA");
        MessageElement dataset =  (MessageElement) cmd.addChildElement("DataSet");
        MessageElement name  = (MessageElement) dataset.addChildElement("name");
        name.addTextNode(datasetName);

        MessageElement ce  = (MessageElement) dataset.addChildElement("ConstraintExpression");
        ce.addTextNode(constraintExpression);


    }





    public static void addTHREDDSCatalogRequest(SOAPBody b, String path) throws SOAPException, SOAPException {


        PrefixedQName bodyName = new PrefixedQName(XMLNamespaces.OpendapSoapNamespaceString,"Request", "ons");
        SOAPBodyElement bodyElement = (SOAPBodyElement) b.addBodyElement(bodyName);

        UID reqID = new UID();

        bodyElement.addAttribute(new PrefixedQName(XMLNamespaces.OpendapSoapNamespaceString,"reqID", "ons"),reqID.toString());

        MessageElement cmd =  (MessageElement) bodyElement.addChildElement("GetTHREDDSCatalog");
        MessageElement dpath  = (MessageElement) cmd.addChildElement("path");
        dpath.addTextNode(path);



    }








    public static  Element getDDXRequestElement(String datasetName, String constraintExpression){
        Element req = new Element("Request",XMLNamespaces.getOpendapSoapNamespace());

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString(),XMLNamespaces.getOpendapSoapNamespace());

        Element cmd = new Element("GetDDX",XMLNamespaces.getOpendapSoapNamespace());

        Element dataset = new Element("DataSet",XMLNamespaces.getOpendapSoapNamespace());
        Element dname = new Element("name",XMLNamespaces.getOpendapSoapNamespace());
        Element ce = new Element("ConstraintExpression",XMLNamespaces.getOpendapSoapNamespace());

        dname.addContent(new Text(datasetName));
        ce.addContent(new Text(constraintExpression));

        dataset.addContent(dname);
        cmd.addContent(dataset);
        dataset.addContent(ce);

        req.addContent(cmd);

        return req;

    }

    public static  Element getDATARequestElement(String datasetName, String constraintExpression){
        Element req = new Element("Request",XMLNamespaces.getOpendapSoapNamespace());

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString(),XMLNamespaces.getOpendapSoapNamespace());

        Element cmd = new Element("GetDATA",XMLNamespaces.getOpendapSoapNamespace());

        Element dataset = new Element("DataSet",XMLNamespaces.getOpendapSoapNamespace());
        Element dname = new Element("name",XMLNamespaces.getOpendapSoapNamespace());
        Element ce = new Element("ConstraintExpression",XMLNamespaces.getOpendapSoapNamespace());

        dname.addContent(new Text(datasetName));
        ce.addContent(new Text(constraintExpression));

        dataset.addContent(dname);
        cmd.addContent(dataset);
        dataset.addContent(ce);

        req.addContent(cmd);

        return req;

    }

    public static  Element getCatalogRequestElement(String path){
        Element req = new Element("Request",XMLNamespaces.getOpendapSoapNamespace());

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString(),XMLNamespaces.getOpendapSoapNamespace());

        Element cmd = new Element("GetTHREDDSCatalog");

        Element dpath = new Element("path",XMLNamespaces.getOpendapSoapNamespace());

        dpath.addContent(new Text(path));

        cmd.addContent(dpath);

        req.addContent(cmd);

        return req;


    }



    public static Element makeExceptionElement(String type, String msg, String location){

        Namespace ns = XMLNamespaces.getOpendapSoapNamespace();

        Element exception = new Element("OPeNDAPException",ns);


        exception.addContent( new Element("Type",ns).setText(type));
        exception.addContent( new Element("Message",ns).setText(msg));
        exception.addContent( new Element("Location",ns).setText(location));

        return exception;


    }


    public static Element anyExceptionElementBuilder(Exception e){
        ByteArrayOutputStream baos =new ByteArrayOutputStream();
        PrintStream ps = new PrintStream( baos);
        e.printStackTrace(ps);

        return makeExceptionElement(e.getClass().getName(),e.getMessage(),baos.toString());

    }



    public static DDS getDDX(String host,
                              String datasetName,
                              String constraintExpression,
                              boolean verbose)
            throws Exception {


        Service service = new Service();

        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new URL(host) );


        SOAPEnvelope se = new SOAPEnvelope();
        //SoapUtils.addDDXRequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u,v");


        SoapUtils.addDDXRequest((SOAPBody)se.getBody(),datasetName,constraintExpression);


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

        return handleSingleDDXResponse(call.getResponseMessage(),verbose);

    }




    public static DDS handleSingleDDXResponse(Message responseMsg, boolean verbose) throws Exception {
        return handleSingleDDXResponse(responseMsg,new DefaultFactory(),verbose);
    }






    public static DDS handleSingleDDXResponse(Message responseMsg, BaseTypeFactory btf, boolean verbose) throws Exception {


        DOMBuilder db = new DOMBuilder();
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();
        Namespace osd2nms = XMLNamespaces.getOpendapDAP2Namespace();

        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        Element d = db.build(responseMsg.getSOAPEnvelope().getAsDOM());
        d.detach();

        Document respDoc = new Document(d);

        //xmlo.output(respDoc,System.out);

        ElementFilter exceptionFilter = new ElementFilter("OPeNDAPException");
        Iterator i = respDoc.getDescendants(exceptionFilter);

        if(i.hasNext())
            throw new Exception(getServerErrorMsgs(i));



        Element soapBody = respDoc.getRootElement().getChild("Body",XMLNamespaces.getDefaultSoapEnvNamespace());


        //xmlo.output(soapBody,System.out);



        List resps = soapBody.getChildren("Response",XMLNamespaces.getOpendapSoapNamespace());
        List reqs = soapBody.getChildren("Request",XMLNamespaces.getOpendapSoapNamespace());

        if(verbose) System.out.println("Requests: "+reqs.size() +"   Responses: "+resps.size());

        if(reqs.size()>1 ){
            throw new Exception("More than one Request Element found in SOAP envelope. " +
                    "The method \"SoapUtils.handleSingleDDXResponse()\" is not an appropriate way to handle " +
                    "this SOAP transaction.");
        }


        if(reqs.size()<1 ){
            throw new Exception("No Request Elements found in SOAP envelope. ");
        }


        if(resps.size()>1){
            throw new Exception("More than one Response Element found in SOAP envelope. " +
                    "The method \"SoapUtils.handleSingleDDXResponse()\" is not an appropriate way to handle " +
                    "this SOAP transaction.");
        }


        if(resps.size()<1){
            throw new Exception("No Response Element found in SOAP envelope. ");
        }


        Element request = (Element) reqs.get(0);
        Element response = (Element) resps.get(0);

        String reqID = request.getAttributeValue("reqID",osnms);
        String respReqID = response.getAttributeValue("reqID",osnms);

        if(verbose) System.out.println("\nRequest reqID: "+reqID+"  Response reqID: "+reqID);


        if(reqID.equals(respReqID)){
        }
        else{
            throw new Exception("BAD THINGS HAPPENED! " +
                    "The Server returned a Response <"+respReqID+"> that is not related to Request <"+reqID+">");
        }


        Element ds = response.getChild("Dataset",osd2nms);
        ds.detach();

        Document ddxDoc = new Document(ds);

        DDSXMLParser parser = new DDSXMLParser(XMLNamespaces.OpendapDAP2NamespaceString);

        DDS dds = new DDS();

        parser.parse(ddxDoc,dds,btf,false);

        return dds;
    }


    public static DDS getDATA(String host,
                              String datasetName,
                              String constraintExpression,
                              boolean verbose)
            throws Exception {


        Service service = new Service();

        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new URL(host) );


        SOAPEnvelope se = new SOAPEnvelope();

        SoapUtils.addDATARequest((SOAPBody)se.getBody(),datasetName,constraintExpression);


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

        return handleSingleDATAResponse(call.getResponseMessage(),verbose);

    }



    public static DDS handleSingleDATAResponse(Message responseMsg, boolean verbose) throws Exception {
        return handleSingleDATAResponse(responseMsg,new DefaultFactory(),verbose);
    }



    public static DDS handleSingleDATAResponse(Message responseMsg, BaseTypeFactory btf, boolean verbose) throws Exception {


        DOMBuilder db = new DOMBuilder();
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();
        Namespace osd2nms = XMLNamespaces.getOpendapDAP2Namespace();

        Element d = db.build(responseMsg.getSOAPEnvelope().getAsDOM());
        d.detach();


        Document respDoc = new Document(d);

        ElementFilter exceptionFilter = new ElementFilter("OPeNDAPException");
        Iterator i = respDoc.getDescendants(exceptionFilter);

        if(i.hasNext())
            throw new Exception(getServerErrorMsgs(i));



        Element soapBody = respDoc.getRootElement().getChild("Body",XMLNamespaces.getDefaultSoapEnvNamespace());

        List reqs = soapBody.getChildren("Request",XMLNamespaces.getOpendapSoapNamespace());
        List resps = soapBody.getChildren("Response",XMLNamespaces.getOpendapSoapNamespace());


        if(reqs.size()>1 ){
            throw new Exception("More than one Request Element found in SOAP envelope. " +
                    "The method \"SoapUtils.handleSingleDDXResponse()\" is not an appropriate way to handle " +
                    "this SOAP transaction.");
        }


        if(reqs.size()<1 ){
            throw new Exception("No Request Elements found in SOAP envelope. ");
        }


        if(resps.size()>1){
            throw new Exception("More than one Response Element found in SOAP envelope. " +
                    "The method \"SoapUtils.handleSingleDDXResponse()\" is not an appropriate way to handle " +
                    "this SOAP transaction.");
        }


        if(resps.size()<1){
            throw new Exception("No Response Element found in SOAP envelope. ");
        }

        Element request = (Element) reqs.get(0);
        Element response = (Element) resps.get(0);

        String reqID = request.getAttributeValue("reqID",osnms);
        String respReqID = response.getAttributeValue("reqID",osnms);

        if(verbose) System.out.println("\nRequest reqID: "+reqID+"  Response reqID: "+reqID);


        if(reqID.equals(respReqID)){
        }
        else{
            throw new Exception("BAD THINGS HAPPENED! " +
                    "The Server returned a Response <"+respReqID+"> that is not related to Request <"+reqID+">");
        }


        Element ds = response.getChild("Dataset",osd2nms);
        ds.detach();

        //new XMLOutputter(Format.getPrettyFormat()).output(ds,System.out);


        Element blob = ds.getChild("dodsBLOB",osd2nms);


        //@todo When the schema is updated we must add the namespace to the this getAttribute call
        String cid = blob.getAttributeValue("href");

        if(cid==null)
            throw new Exception("\n\nThe returned DDX has an incorrectly structured blob reference.\n" +
                    "It is missing an href attribute that points to the Content-ID of the binary data attachment.\n" +
                    "Did you mistakenly ask for a DDX when you meant to ask for DATA?\n");


        cid = cid.substring(4,cid.length());


        AttachmentPart data = getAttachment(responseMsg,cid,verbose);






        Document ddxDoc = new Document(ds);

        DDSXMLParser parser = new DDSXMLParser(XMLNamespaces.OpendapDAP2NamespaceString);

        DDS dds = new DDS();

        parser.parse(ddxDoc,dds,btf,false);





        String[] s = responseMsg.getMimeHeaders().getHeader("XDODS-Server");
        String serverVersion = s[0];
        ServerVersion sv = new ServerVersion(serverVersion);


        dds.deserialize(new DataInputStream((InputStream)data.getContent()), sv, null);



        return dds;
    }



    public static AttachmentPart getAttachment(Message msg, String contentID, boolean verbose) throws Exception {

        if(verbose) System.out.println("Searching for Attachment with cid: "+contentID);

        AttachmentPart data = null;
        Iterator i = msg.getAttachments();

        while(i.hasNext()){
            org.apache.axis.attachments.AttachmentPart ap = (org.apache.axis.attachments.AttachmentPart) i.next();

            String[] attachmentCid = ap.getMimeHeader("Content-ID");

            if(contentID.equals(attachmentCid[0])){
                 if(verbose) System.out.println("Found it.");
                data = ap;
                break;
            }
        }
         if(verbose) System.out.println();


        if(data==null)
            throw new Exception("Error! Message does not contain an attachment " +
                    "with a ContentID of "+contentID);


        return data;
    }


    public static Document getTHREDDSCatalog(String host,
                              String path,
                              boolean verbose)
            throws Exception {


        Service service = new Service();

        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new URL(host) );


        SOAPEnvelope se = new SOAPEnvelope();
        //SoapUtils.addDDXRequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u,v");


        SoapUtils.addTHREDDSCatalogRequest((SOAPBody)se.getBody() ,path);


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

        return handleSingleTHREDDSCatalogResponse(call.getResponseMessage(),verbose);

    }

    private static Document handleSingleTHREDDSCatalogResponse(Message responseMsg, boolean verbose) throws Exception{

        DOMBuilder db = new DOMBuilder();
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        Element d = db.build(responseMsg.getSOAPEnvelope().getAsDOM());
        d.detach();

        Document respDoc = new Document(d);

        ElementFilter exceptionFilter = new ElementFilter("OPeNDAPException");
        Iterator i = respDoc.getDescendants(exceptionFilter);

        if(i.hasNext())
            throw new Exception(getServerErrorMsgs(i));



        Element soapBody = respDoc.getRootElement().getChild("Body",XMLNamespaces.getDefaultSoapEnvNamespace());

        List reqs = soapBody.getChildren("Request",XMLNamespaces.getOpendapSoapNamespace());
        List resps = soapBody.getChildren("Response",XMLNamespaces.getOpendapSoapNamespace());


        if(reqs.size()>1 ){
            throw new Exception("More than one Request Element found in SOAP envelope. " +
                    "The method \"SoapUtils.handleSingleDDXResponse()\" is not an appropriate way to handle " +
                    "this SOAP transaction.");
        }


        if(reqs.size()<1 ){
            throw new Exception("No Request Elements found in SOAP envelope. ");
        }


        if(resps.size()>1){
            throw new Exception("More than one Response Element found in SOAP envelope. " +
                    "The method \"SoapUtils.handleSingleDDXResponse()\" is not an appropriate way to handle " +
                    "this SOAP transaction.");
        }


        if(resps.size()<1){
            throw new Exception("No Response Element found in SOAP envelope. ");
        }


        Element request = (Element) reqs.get(0);
        Element response = (Element) resps.get(0);

        String reqID = request.getAttributeValue("reqID",osnms);
        String respReqID = response.getAttributeValue("reqID",osnms);

        if(verbose) System.out.println("\nRequest reqID: "+reqID+"  Response reqID: "+reqID);


        if(reqID.equals(respReqID)){
        }
        else{
            throw new Exception("BAD THINGS HAPPENED! " +
                    "The Server returned a Response <"+respReqID+"> that is not related to Request <"+reqID+">");
        }


        List c = response.getChildren();

        if(c.size() != 1){
            throw new Exception("Badly formed response! Response Element MUST contain " +
                    "one and opnly one child Element.");
        }


        Element catalog = (Element) c.get(0);

        catalog.detach();

        return new Document(catalog);




    }


    private static String getServerErrorMsgs(Iterator i) {

       String eMsg = "\"The Server Returned One Or More Errors.  Messages: \n";


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        int cnt = 0;
        while(i.hasNext()){
            Element err = (Element) i.next();

            eMsg +="\n--------------- ERROR "+ cnt++ +": ";
            eMsg += xmlo.outputString(err);
            eMsg +="\n---------------";
        }

        return eMsg;
    }


}
