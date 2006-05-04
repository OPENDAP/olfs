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
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

import opendap.soap.XMLNamespaces;
import opendap.soap.ExceptionElementUtil;

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
                              OpendapHttpDispatchHandler odh,
                              OpendapSoapDispatchHandler sdh) {

        doPost03(request, response, odh, sdh);
    }




    /**
     *
     * @param request
     * @param response
     * @param odh
     * @param sdh
     */
    public static void doPost03(HttpServletRequest request,
                                HttpServletResponse response,
                                OpendapHttpDispatchHandler odh,
                                OpendapSoapDispatchHandler sdh) {


        System.out.println("\n\n\nSOAPHandler.doPost(): Start of POST Handler.");


        try {

            Document doc = getSOAPDoc(request);

            if (qcSOAPDocument(doc)) {


                System.out.println("Building Multipart Response...");

                MultipartResponse mpr = new MultipartResponse(request, response, odh);

                Element soapEnvelope = doc.getRootElement();

                mpr.setSoapEnvelope(soapEnvelope);

                List soapContents = soapEnvelope.getChild("Body", XMLNamespaces.getDefaultSoapEnvNamespace()).getChildren();

                System.out.println("Got " + soapContents.size() + " SOAP Body Elements.");

                for (Object soapContent : soapContents) {

                    Element clientReq = (Element) soapContent;
                    requestDispatcher(request, clientReq, mpr, sdh);
                }

                mpr.send();


                System.out.println("done.");

            } else {
                System.out.print("Reflecting Document to client...");

                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                xmlo.output(doc, response.getOutputStream());

                System.out.println("done.");
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        System.out.println("SOAPRequestDispatcher.doPost(): End of POST Handler.\n\n\n");
    }


    /**
     *
     * @param srvReq
     * @param reqElement
     * @param mpr
     * @param sdh
     */
    private static void requestDispatcher(HttpServletRequest srvReq,
                                             Element reqElement,
                                             MultipartResponse mpr,
                                             OpendapSoapDispatchHandler sdh) {


        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        String reqID = reqElement.getAttributeValue("reqID", osnms);


        System.out.println("Request ELement: \n" + reqElement.toString());


        try {
            List cmds = reqElement.getChildren();
            if (cmds.size() == 1) {

                Element cmd = (Element) cmds.get(0);

                if (cmd.getName().equals("GetDATA")) {

                    sdh.getDATA( reqID, cmd, mpr);

                } else if (cmd.getName().equals("GetDDX")) {

                    sdh.getDDX( reqID, cmd, mpr);

                } else if (cmd.getName().equals("GetTHREDDSCatalog")) {

                    sdh.getTHREDDSCatalog(srvReq, reqID, cmd, mpr);

                }
                else {
                    System.out.println("Received Bad Soap reqElement: " + reqElement.getName());

                    Element err = ExceptionElementUtil.makeExceptionElement(
                            "BadSOAPRequest",
                            "Request (reqID: " + reqID + ") not recognized by this server.",
                            "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                    );
                    mpr.addSoapBodyPart(err);

                }
            } else {
                System.out.println("Received Bad SOAP Request. reqID :" + reqID);

                Element err = ExceptionElementUtil.makeExceptionElement(
                        "BadSOAPRequest",
                        "Requests must contain one, and only one, command element. Found: " +
                                cmds.size() +
                                " elements.",
                        "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                );
                mpr.addSoapBodyPart(err);


            }
        } catch (Exception e) {
            mpr.addSoapBodyPart(ExceptionElementUtil.anyExceptionElementBuilder(e));
        }


    }


    /**
     *
     * @param req
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private static Document getSOAPDoc(HttpServletRequest req) throws IOException, JDOMException {

        SAXBuilder saxBldr = new SAXBuilder();
        Document doc = saxBldr.build(req.getReader());
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        System.out.println("");
        System.out.println("POST Method got this XML Document:");

        xmlo.output(doc, System.out);
        System.out.println("");

        return doc;
    }

    /**
     *
     * @param doc
     * @return  Returns true if the SOAP document looks good, false if not.
     */
    private static boolean qcSOAPDocument(Document doc) {
        boolean result;


        Namespace soapEnvNameSpace = XMLNamespaces.getDefaultSoapEnvNamespace();

        result = false;

        Element se = doc.getRootElement();

        System.out.println("DocRoot: " + se.getName() + "    getOpendapSoapNamespace: " + se.getNamespace().getURI());

        if (se.getName().equals("Envelope") && se.getNamespace().equals(soapEnvNameSpace)) {
            Iterator it = se.getChildren().iterator();

            Element sb = null;
            int i = 0;
            while (it.hasNext()) {
                sb = (Element) it.next();
                i++;
            }
            if (i == 1 || i == 2) {
                if (sb.getName().equals("Body") && sb.getNamespace().equals(soapEnvNameSpace)) {

                    List reqs = sb.getChildren();
                    result = true;

                    for (Object req1 : reqs) {
                        Element req = (Element) req1;
                        if (!req.getName().equals("Request"))
                            result = false;
                    }
                }
            }
        }


        //System.out.println(result ? "TRUTH" : "CONSEQUENCES");

        return result;

    }








}



