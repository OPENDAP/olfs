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

import org.apache.axis.Message;
import org.apache.axis.message.SOAPBody;
import org.apache.axis.message.SOAPBodyElement;
import org.apache.axis.message.PrefixedQName;
import org.apache.axis.message.MessageElement;


import org.jdom.Element;
import org.jdom.Text;
import org.jdom.Document;
import org.jdom.output.DOMOutputter;


import javax.xml.soap.*;
import java.rmi.server.UID;

/**
 * Simple test driver for our message service.
 */
public class TestClient {


    public String testAttachmentResponse(String[] args) throws Exception {

        DOMOutputter domO = new DOMOutputter();







        MessageFactory factory = MessageFactory.newInstance();
        System.out.println("MessageFactory is a "+factory.getClass().getName());


        SOAPMessage msg = factory.createMessage();
        System.out.println("SOAPMessage is a "+msg.getClass().getName());


        msg.setContentDescription("OPeNDAP SOAP Protocol GetDDX Request");

        System.out.println("SOAPMessage is a "+msg.getClass().getName());
        System.out.println("    DEFAULT_ATTACHMNET_IMPL:  "+Message.getAttachmentImplClassName());
        System.out.println("    Message Desc:  "+msg.getContentDescription());


        //Element req = getDDXRequestElement("/nc/fnoc1.nc","u,v");
        //Document doc = new Document(req);
        //new SOAPBodyElement(domO.output(doc).getDocumentElement());

        SOAPBody body = (org.apache.axis.message.SOAPBody) msg.getSOAPBody();
        System.out.println("SOAPBody is a "+body.getClass().getName());


        addDDXRequest(body,"/nc/fnoc1.nc","u,v");

        msg.writeTo(System.out);

        SOAPConnection soapConn = SOAPConnectionFactory.newInstance().createConnection();
        System.out.println("SOAPConnection is a "+soapConn.getClass().getName());
        Message reply =  (Message) soapConn.call(msg,"http://localhost:3001/opendap/s4/");


        System.out.println("\n\n\nMESSAGE:");
        System.out.println("SOAPMessage is a "+reply.getClass().getName());
        System.out.println("    DEFAULT_ATTACHMNET_IMPL:  "+Message.getAttachmentImplClassName());
        System.out.println("    Message Desc:  "+reply.getContentDescription());

        System.out.println("    Attachments: "+msg.countAttachments());
        System.out.println("\n\n\n");


        return( "foo" );
    }







    /*
    public String symmetricMsg(String[] args) throws Exception {


        Options opts = new Options(args);
        //opts.setDefaultURL("http://localhost:8080/axis/services/MessageService");
        opts.setDefaultURL("http://localhost:3001/opendap/s4/");


        DOMOutputter domO = new DOMOutputter();


        SOAPBodyElement[] input = new SOAPBodyElement[3];

        Element req = getDDXRequestElement("/nc/fnoc1.nc","u,v");
        Document doc = new Document(req);
        input[0] = new SOAPBodyElement(domO.output(doc).getDocumentElement());


        req = getDDXRequestElement("nc/fnoc2.nc","v");
        doc = new Document(req);
        input[1] = new SOAPBodyElement(domO.output(doc).getDocumentElement());


        req = getCatalogRequestElement("nc/Test");
        doc = new Document(req);
        input[2] = new SOAPBodyElement(domO.output(doc).getDocumentElement());

        Service  service = new Service();

        System.out.println("Service class is a: "+service.getClass().getName());
        System.out.println("Service.createCall() returns a: "+service.createCall().getClass().getName());


        Call     call    = (Call) service.createCall();
        System.out.println("Call class is a: "+call.getClass().getName());


        call.setTargetEndpointAddress( new URL(opts.getURL()) );
        //Vector          elems = (Vector) call.invoke( input );
        call.invoke( input );




        String str = "";

        for(int i=0; i<elems.size() ;i++){
            SOAPBodyElement elem = (SOAPBodyElement) elems.get(i);
            Element e            = domB.build(elem.getAsDOM());

             str += "\nRes elem["+i+"]:\n" + xmlo.outputString(e) +"\n";

        }



        System.out.println("\n\n\nMESSAGE:");
        System.out.println("    Attachments: "+call.getResponseMessage().countAttachments());
        System.out.println("\n\n\n");


        return( str );
    }

*/





    public static void addDDXRequest(SOAPBody b, String datasetName, String constraintExpression) throws SOAPException, SOAPException {

        String osanms = "http://www.opendap.org/xml/soapAPI";

        PrefixedQName bodyName = new PrefixedQName(null,"Request", null);
        SOAPBodyElement bodyElement = (SOAPBodyElement) b.addBodyElement(bodyName);

        UID reqID = new UID();

        bodyElement.addAttribute(new PrefixedQName(null,"reqID", null),"<"+reqID.toString()+">");

        MessageElement cmd =  (MessageElement) bodyElement.addChildElement("GetDDX");
        MessageElement dataset =  (MessageElement) cmd.addChildElement("DataSet");
        MessageElement name  = (MessageElement) dataset.addChildElement("name");
        name.addTextNode(datasetName);

        MessageElement ce  = (MessageElement) dataset.addChildElement("ConstraintExpression");
        ce.addTextNode(constraintExpression);


    }





    public static  Element getDDXRequestElement(String datasetName, String constraintExpression){
        Element req = new Element("Request");

        UID reqID = new UID();

        req.setAttribute("reqID",reqID.toString());

        Element cmd = new Element("GetDDX");

        Element dataset = new Element("DataSet");
        Element dname = new Element("name");
        Element ce = new Element("ConstraintExpression");

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





    public static void main(String[] args) throws Exception {
        TestClient testMsg = new TestClient();
        System.out.println(testMsg.testAttachmentResponse(args));
        //testMsg.testEnvelope(args);
    }
}
