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

package opendap.coreServlet;

import org.jdom.input.SAXBuilder;
import org.jdom.*;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.rmi.server.UID;

import opendap.olfs.BesAPI;
import opendap.olfs.BESCrawlableDataset;
import opendap.util.Debug;
import opendap.soap.SoapNamespaces;
import thredds.cataloggen.SimpleCatalogBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Apr 4, 2006
 * Time: 12:50:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class SOAPRequestDispatcher {




    public static void doPost(HttpServletRequest request,
                              HttpServletResponse response,
                              OpendapHttpDispatchHandler odh){

        doPostMulti(request,response,odh);
    }



    public static Document getSOAPDoc(HttpServletRequest req) throws IOException, JDOMException {

        SAXBuilder saxBldr = new SAXBuilder();
        Document doc = saxBldr.build(req.getReader());
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        System.out.println("");
        System.out.println("POST Method got this XML Document:");

        xmlo.output(doc,System.out);
        System.out.println("");

        return doc;
    }


    /**
     *
     *
     * @param request
     * @param response
     */
    public static void doPostSingle(HttpServletRequest request,
                              HttpServletResponse response,
                              OpendapHttpDispatchHandler odh){

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        System.out.println("\n\n\nSOAPHandler.doPost(): Start of POST Handler.");

        try {

            Document doc = getSOAPDoc(request);





            if(qcSOAPDocument(doc)){
                System.out.println("Sending Response...");

                Element soapBody = doc.getRootElement().
                        getChild("Body",SoapNamespaces.getDefaultSoapEnvNamespace());

                List soapContents = soapBody.getChildren();

                Element[] msgs = new Element[soapContents.size()];

                for(int i=0; i<soapContents.size() ;i++){

                    Object o = soapContents.get(i);

                    if(o instanceof Element){
                        Element clientReq = (Element) o;
                        msgs[i] = soapRequestDispatcher(request,clientReq);
                        //xmlo.output(msgElement,System.out);
                    }
                    else if (o instanceof Text){
                        Text t = (Text) o;
                        System.out.println("Got a Text object: \""+t.getText()+"\"");
                    }
                }

                for(int i=0; i<msgs.length ;i++){
                    if(msgs[i] != null)
                    soapBody.addContent(msgs[i]);
                }


                xmlo.output(doc,response.getOutputStream());
                System.out.println("done.");

            }
            else {
                System.out.print("Reflecting Document to client...");

                xmlo.output(doc,response.getOutputStream());

                System.out.println("done.");
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println("SOAPRequestDispatcher.doPost(): End of POST Handler.\n\n\n");
    }


    private static int count = 0;
    private static String getNewMimeBoundary(){
        //Date date = new Date();
        return "----=_Part_"+count++ +"_"+getSuid();
    }

    private static String getSuid(){
        UID uid = new UID();

        byte[] val = uid.toString().getBytes();

        String suid  = "";
        int v;

        for (byte aVal : val) {
            v = aVal;
            suid += Integer.toHexString(v);
        }

        return suid;
    }



    /**
     *
     *
     * @param request
     * @param response
     */
    public static void doPostMulti(HttpServletRequest request,
                              HttpServletResponse response,
                              OpendapHttpDispatchHandler odh){


        System.out.println("\n\n\nSOAPHandler.doPost(): Start of POST Handler.");

        try {

            SAXBuilder saxBldr = new SAXBuilder();
            Document doc = saxBldr.build(request.getReader());
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


            System.out.println("");
            System.out.println("POST Method got this XML Document:");

            xmlo.output(doc,System.out);
            System.out.println("");



            if(qcSOAPDocument(doc)){
                System.out.println("Sending Response...");
                ServletOutputStream os = response.getOutputStream();


                String mimeBoundary = getNewMimeBoundary();
                System.out.println("MIME Boundary: "+mimeBoundary);

                String startID = "<OPeNDAP_SOAP_MSG_RESPONSE_START>";


                response.setContentType("Multipart/related;  "+
                                        "type=\"text/xml\";  "+
                                        "start=\""+startID+"\";  "+
                                        "boundary=\""+mimeBoundary+"\"");

                response.setHeader("XDODS-Server", odh.getXDODSServerVersion());
                response.setHeader("XOPeNDAP-Server", odh.getXOPeNDAPServerVersion());
                response.setHeader("XDAP", odh.getXDAPVersion(request));
                response.setHeader("Content-Description", "OPeNDAP WebServices");


                os.println("--"+mimeBoundary);
                os.println("Content-Type: text/xml; charset=UTF-8");
                os.println("Content-Transfer-Encoding: binary");
                os.println("Content-Id: "+startID);
                os.println();


                Element soapBody = doc.getRootElement().
                        getChild("Body",SoapNamespaces.getDefaultSoapEnvNamespace());

                List soapContents = soapBody.getContent();

                Element[] msgs = new Element[soapContents.size()];

                System.out.println("Got "+soapContents.size()+" SOAP Body Elements.");

                for(int i=0; i<soapContents.size() ;i++){

                    Object o = soapContents.get(i);

                    if(o instanceof Element){
                        Element clientReq = (Element) o;
                        msgs[i] = soapRequestDispatcher(request,clientReq);
                        //xmlo.output(msgElement,System.out);
                    }
                    else if (o instanceof Text){
                        Text t = (Text) o;
                        System.out.println("Got a Text object: \""+t.getText()+"\"");
                    }
                }

                for(int i=0; i<msgs.length ;i++){
                    if(msgs[i] != null)
                    soapBody.addContent(msgs[i]);
                }

                xmlo.output(doc,os);


                os.println("--"+mimeBoundary);
                os.println("Content-Type: text; charset=UTF-8");
                os.println("Content-Transfer-Encoding: binary");
                os.println("Content-Id: <"+getSuid()+">");
                os.println();

                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");
                os.println("This is my attachment bullshit.");



                os.println("--"+mimeBoundary+"--");


                System.out.println("done.");

            }
            else {
                System.out.print("Reflecting Document to client...");

                xmlo.output(doc,response.getOutputStream());

                System.out.println("done.");
            }


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println("SOAPRequestDispatcher.doPost(): End of POST Handler.\n\n\n");
    }



    private static boolean qcSOAPDocument(Document doc){
        boolean result;


        Namespace soapEnvNameSpace = SoapNamespaces.getDefaultSoapEnvNamespace();

        result = false;

        Element se = doc.getRootElement();

        System.out.println("DocRoot: "+se.getName() +"    getOpendapSoapNamespace: "+se.getNamespace().getURI());

        if(se.getName().equals("Envelope")  &&  se.getNamespace().equals(soapEnvNameSpace)){
            Iterator it  = se.getChildren().iterator();

            Element sb = null;
            int i = 0;
            while(it.hasNext()){
                sb = (Element) it.next();
                i++;
            }
            if(i==1 || i==2){
                if(sb.getName().equals("Body") && sb.getNamespace().equals(soapEnvNameSpace)){

                    List reqs = sb.getChildren();
                    result = true;

                    for(int j=0; j< reqs.size() ;j++){
                        Element req = (Element) reqs.get(j);
                        if(!req.getName().equals("Request"))
                            result = false;
                    }
                }
            }
        }


        System.out.println(result?"TRUTH":"CONSEQUENCES");

        return result;

    }





    private static Element soapRequestDispatcher(HttpServletRequest srvReq, Element reqElement){


        Element respElement = null;


        System.out.println("Request ELement: \n"+reqElement.toString());


        try {
            List cmds = reqElement.getChildren();
            if(cmds.size() == 1){

                Element cmd = (Element) cmds.get(0);

                if (cmd.getName().equals("GetMultiPartTest")) {

                    System.out.println("Received GetMultiPartTest reqElement.");


                }
                else if (cmd.getName().equals("GetDDX")) {

                    System.out.println("Received GetDDX reqElement.");

                    Namespace osnms = SoapNamespaces.getOpendapSoapNamespace();

                    Element dataSet = cmd.getChild("DataSet", osnms);

                    System.out.println("Dataset:\n"+dataSet.toString());


                    String datasetname = dataSet.getChild("name",osnms).getTextTrim();
                    String ce = dataSet.getChild("ConstraintExpression",osnms).getTextTrim();

                    System.out.println("Processing DataSet - path: "+datasetname+"   ce: "+ce);

                    respElement = new Element("Response",osnms);
                    String reqID = reqElement.getAttributeValue("reqID",osnms);
                    respElement.setAttribute("reqID",reqID);

                    respElement.addContent(BesAPI.getDDXDocument(datasetname, ce).detachRootElement());

                }
                else if (cmd.getName().equals("GetTHREDDSCatalog")) {

                    System.out.println("Received GetTHREDDSCatalog reqElement.");

                    String path = cmd.getChild("path").getTextTrim();

                    path = BESCrawlableDataset.besPath2ThreddsPath(path);

                    BESCrawlableDataset s4cd = new BESCrawlableDataset(path, null);

                    if (s4cd.isCollection()) {

                        SimpleCatalogBuilder scb = new SimpleCatalogBuilder(
                                "",                                   // CollectionID, which for us needs to be empty.
                                BESCrawlableDataset.getRootDataset(), // Root dataset of this collection
                                "OPeNDAP-Server4",                    // Service Name
                                "OPeNDAP",                            // Service Type Name
                                srvReq.getRequestURI().substring(0, srvReq.getRequestURI().lastIndexOf(srvReq.getPathInfo()) + 1)); // Base URL for this service

                        if (Debug.isSet("showResponse")) {
                            System.out.println("SOAPRequestDispatcher:GetTHREDDSCatalog - Generating catalog");
                        }

                        //System.out.println("\n\nCATALOG:\n"+scb.generateCatalogAsString(s4cd)+"\n");

                        Document catalog = scb.generateCatalogAsDocument(s4cd);

                        if(catalog == null){
                            System.out.println("SimpleCatalogBuilder.generateCatalogAsDocument("+path+") returned null.");
                            respElement =  makeExceptionElement(
                                    "BadSOAPRequest",
                                    "Requested catalog ("+cmd.getChild("path").getTextTrim()+" is not available.",
                                    "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                            );
                        }
                        else {
                            respElement = new Element("Response");
                            respElement.setAttribute("reqID",reqElement.getAttributeValue("reqID"));
                            respElement.addContent(catalog.detachRootElement());
                        }

                    } else {

                        String msg = "ERROR: THREDDS catalogs may only be requested for collections, " +
                                "not for individual data sets. The path: \""+cmd.getChild("path").getTextTrim()+
                                "\" does not resolve to a collection.";

                        respElement =  makeExceptionElement(
                                "BadSOAPRequest",
                                msg,
                                "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                        );

                    }






                }
            }
            else {
                System.out.println("Received Bad SOAP Request. reqID "+reqElement.getAttribute("reqID"));

                        respElement =  makeExceptionElement(
                                "BadSOAPRequest",
                                "Requests must contain one, and only one, command element. Found: " + cmds.size()+" elements.",
                                "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                        );


            }



        } catch (Exception e) {
            respElement = anyExceptionElementBuilder(e);
        }

        if(respElement == null) {
            System.out.println("Received Bad Soap reqElement: "+reqElement.getName());

                    respElement =  makeExceptionElement(
                            "BadSOAPRequest",
                            "Request (reqID: " + reqElement.getAttributeValue("reqID") + ") not recognized by this server.",
                            "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                    );


        }

        return respElement;

    }





    public static Element makeExceptionElement(String type, String msg, String location){


        Element exception = new Element("OPeNDAPException");


        exception.addContent( new Element("Type").setText(type));
        exception.addContent( new Element("Message").setText(msg));
        exception.addContent( new Element("Location").setText(location));

        return exception;


    }


    public static Element anyExceptionElementBuilder(Exception e){
        ByteArrayOutputStream baos =new ByteArrayOutputStream();
        PrintStream ps = new PrintStream( baos);
        e.printStackTrace(ps);

        return makeExceptionElement(e.getClass().getName(),e.getMessage(),baos.toString());

    }







}



