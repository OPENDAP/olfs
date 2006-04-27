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

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.DOMOutputter;

import java.net.URL;

/**
 * Simple test driver for our message service.
 */
public class TestClient {


    private static String defaultURL = "http://localhost:3001/opendap/s4/";

    public static void testMsg01(String[] args) throws Exception {

        String url = args.length>0?args[0]:defaultURL;


        DOMOutputter domO = new DOMOutputter();


        SOAPBodyElement[] input = new SOAPBodyElement[3];

        Element req = SoapUtils.getDDXRequestElement("/nc/fnoc1.nc","u,v");
        Document doc = new Document(req);
        input[0] = new SOAPBodyElement(domO.output(doc).getDocumentElement());


        req = SoapUtils.getDDXRequestElement("nc/fnoc2.nc","v");
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


    }






    public static void testMsg02(String[] args) throws Exception {

        String url = args.length>0?args[0]:defaultURL;



        //Create the data for the attached file.

        Service service = new Service();

        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new URL(url) );


        SOAPEnvelope se = new SOAPEnvelope();
        SoapUtils.addDDXRequest((SOAPBody)se.getBody(),"/nc/fnoc1.nc","u,v");
        SoapUtils.addTHREDDSCatalogRequest((SOAPBody)se.getBody(),"/nc/Test");

        Message msg = new Message(se);


        System.out.println("Sending this SOAP message:");
        msg.writeTo(System.out);
        System.out.println();

        call.setProperty(Call.ATTACHMENT_ENCAPSULATION_FORMAT,Call.ATTACHMENT_ENCAPSULATION_FORMAT_MIME);
        call.invoke(msg);

        SoapUtils.probeMessage(call.getResponseMessage());
    }





    public static void main(String[] args) throws Exception {
        TestClient.testMsg01(args);
    }
}
