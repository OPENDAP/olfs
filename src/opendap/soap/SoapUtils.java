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

import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.PrefixedQName;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.MessageElement;

import javax.xml.soap.SOAPException;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.AttachmentPart;

import java.rmi.server.UID;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

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

        System.out.println("SOAP Envelope: ");
        System.out.println(m.getSOAPEnvelope());


        int acount = m.countAttachments();
        int cnt=0;
        System.out.println("\n\nAttachments: "+acount+"\n\n");
        if(acount>0){

            Iterator i = m.getAttachments();
            AttachmentPart ap;
            while(i.hasNext()){

                ap = (AttachmentPart) i.next();
                System.out.println("Attachment["+cnt+"]:");
                Iterator j = ap.getAllMimeHeaders();
                while(j.hasNext()){
                    MimeHeader mhdr = (MimeHeader)j.next();
                    System.out.println("    "+mhdr.getName()+": "+mhdr.getValue());
                }
                System.out.println("    getContent() returns Class: "+ap.getContent().getClass().getName());

                InputStream is = (InputStream) ap.getContent();

                System.out.println("\nAttachment Content ["+is.available()+" bytes]: \n");

                byte[] buf = new byte[is.available()];

                int ret = is.read(buf);

               System.out.println("Read "+ret+" bytes. Content: \n"+new String(buf));

                cnt++;

            }


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


        PrefixedQName bodyName = new PrefixedQName(SoapNamespaces.OpendapSoapNamespaceString,"Request", "ons");
        SOAPBodyElement bodyElement = (SOAPBodyElement) b.addBodyElement(bodyName);

        UID reqID = new UID();

        bodyElement.addAttribute(new PrefixedQName(SoapNamespaces.OpendapSoapNamespaceString,"reqID", "ons"),reqID.toString());

        MessageElement cmd =  (MessageElement) bodyElement.addChildElement("GetDDX");
        MessageElement dataset =  (MessageElement) cmd.addChildElement("DataSet");
        MessageElement name  = (MessageElement) dataset.addChildElement("name");
        name.addTextNode(datasetName);

        MessageElement ce  = (MessageElement) dataset.addChildElement("ConstraintExpression");
        ce.addTextNode(constraintExpression);


    }





    public static void addTHREDDSCatalogRequest(SOAPBody b, String path) throws SOAPException, SOAPException {


        PrefixedQName bodyName = new PrefixedQName(SoapNamespaces.OpendapSoapNamespaceString,"Request", "ons");
        SOAPBodyElement bodyElement = (SOAPBodyElement) b.addBodyElement(bodyName);

        UID reqID = new UID();

        bodyElement.addAttribute(new PrefixedQName(SoapNamespaces.OpendapSoapNamespaceString,"reqID", "ons"),reqID.toString());

        MessageElement cmd =  (MessageElement) bodyElement.addChildElement("GetTHREDDSCatalog");
        MessageElement dpath  = (MessageElement) cmd.addChildElement("path");
        dpath.addTextNode(path);



    }








    public static  Element getDDXRequestElement(String datasetName, String constraintExpression){
        Element req = new Element("Request",SoapNamespaces.getOpendapSoapNamespace());

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString(),SoapNamespaces.getOpendapSoapNamespace());

        Element cmd = new Element("GetDDX",SoapNamespaces.getOpendapSoapNamespace());

        Element dataset = new Element("DataSet",SoapNamespaces.getOpendapSoapNamespace());
        Element dname = new Element("name",SoapNamespaces.getOpendapSoapNamespace());
        Element ce = new Element("ConstraintExpression",SoapNamespaces.getOpendapSoapNamespace());

        dname.addContent(new Text(datasetName));
        ce.addContent(new Text(constraintExpression));

        dataset.addContent(dname);
        cmd.addContent(dataset);
        dataset.addContent(ce);

        req.addContent(cmd);

        return req;

    }

    public static  Element getCatalogRequestElement(String path){
        Element req = new Element("Request");

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString());

        Element cmd = new Element("GetTHREDDSCatalog");

        Element dpath = new Element("path");

        dpath.addContent(new Text(path));

        cmd.addContent(dpath);

        req.addContent(cmd);

        return req;


    }



}
