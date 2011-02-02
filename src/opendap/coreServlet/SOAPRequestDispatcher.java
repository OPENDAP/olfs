/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Iterator;

import opendap.soap.XMLNamespaces;
import opendap.soap.ExceptionElementUtil;

/**
 * Handles SOAP requests that arrive via HTTP POST.
 */
public class SOAPRequestDispatcher implements DispatchHandler {


    private org.slf4j.Logger log;
    private HttpServlet servlet;
    private boolean initialized;
    private OpendapSoapDispatchHandler sdh;

    public SOAPRequestDispatcher() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        servlet = null;
        initialized = false;

    }

    public void init(HttpServlet s, Element config) throws Exception {

        if (initialized) return;


        servlet = s;




        String className = config.getChild("OpendapSoapDispatchHandler").getTextTrim();
        if (className == null)
            throw new ServletException("Missing configuration parameter \"OpendapSoapDispatchHandlerImplementation\"." +
                    "A class that implements the opendap.coreServlet.OpendapSoapDispatchHandler interface must" +
                    "be identified in this (missing) servlet configuration.");

        log.info("OpendapSoapDispatchHandlerImplementation is " + className);

        try {
            Class classDefinition = Class.forName(className);
            sdh = (OpendapSoapDispatchHandler) classDefinition.newInstance();
        } catch (InstantiationException e) {
            throw new ServletException("Cannot instantiate class: " + className, e);
        } catch (IllegalAccessException e) {
            throw new ServletException("Cannot access class: " + className, e);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Cannot find class: " + className, e);
        }

        sdh.init(servlet);



        initialized = true;

        log.info("Initialized.");
    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return true;

    }
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        doPost(request,response);

    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }


    public void destroy() {
        log.info("Destroy complete.");

    }



    /**
     * Handles SOAP requests that arrive via HTTP POST. No other POST functions supported.
     *
     * @param request .
     * @param response .
     */
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response) {

        try {

            log.debug("\n\n\nSOAPHandler.doPost(): Start of POST Handler.");



            Document doc = getSOAPDoc(request);

            if (qcSOAPDocument(doc)) {


                log.debug("Building Multipart Response...");

                MultipartResponse mpr = new MultipartResponse(request, response, sdh);

                Element soapEnvelope = doc.getRootElement();

                mpr.setSoapEnvelope(soapEnvelope);

                List soapContents = soapEnvelope.getChild("Body", XMLNamespaces.getDefaultSoapEnvNamespace()).getChildren();

                log.debug("Got " + soapContents.size() + " SOAP Body Elements.");

                for (Object soapContent : soapContents) {

                    Element clientReq = (Element) soapContent;
                    requestDispatcher(request, clientReq, mpr, sdh);
                }

                mpr.send();


                log.debug("done.");

            } else {
                log.debug("Reflecting Document to client...");

                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
                xmlo.output(doc, response.getOutputStream());

                log.debug("done.");
            }


            log.debug("SOAPRequestDispatcher.doPost(): End of POST Handler.\n\n\n");
        }
        catch(Throwable t){
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            }
            catch(Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        }
    }


    /**
     *
     * @param srvReq
     * @param reqElement
     * @param mpr
     * @param sdh
     */
    private  void requestDispatcher(HttpServletRequest srvReq,
                                             Element reqElement,
                                             MultipartResponse mpr,
                                             OpendapSoapDispatchHandler sdh) {


        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        String reqID = reqElement.getAttributeValue("reqID", osnms);

        String xmlBase = srvReq.getRequestURL().toString();



        try {
            List cmds = reqElement.getChildren();
            if (cmds.size() == 1) {

                Element cmd = (Element) cmds.get(0);

                if (cmd.getName().equals("GetDATA")) {
                    log.debug("requestDispatcher(): <GetDATA> Element.");

                    sdh.getDATA( reqID, xmlBase, cmd, mpr);

                } else if (cmd.getName().equals("GetDDX")) {
                    log.debug("requestDispatcher(): <GetDDX> Element.");

                    sdh.getDDX( reqID, xmlBase, cmd, mpr);

                } else if (cmd.getName().equals("GetTHREDDSCatalog")) {
                    log.debug("requestDispatcher(): <GetTHREDDSCatalog> Element.");

                    sdh.getTHREDDSCatalog(srvReq, reqID, cmd, mpr);

                }
                else {
                    log.error("Received Bad Soap reqElement: " + reqElement.getName());

                    Element err = ExceptionElementUtil.makeExceptionElement(
                            "BadSOAPRequest",
                            "Request (reqID: " + reqID + ") not recognized by this server.",
                            "opendap.coreServlet.SOAPRequestDispatcher.requestDispatcher()"
                    );
                    mpr.addSoapBodyPart(err);

                }
            } else {
                log.error("Received Bad SOAP Request. reqID :" + reqID);

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
            log.error("Something Bad Happened while processing a SOAP request (reqID :" + reqID+")");
            ByteArrayOutputStream baos =new ByteArrayOutputStream();
            PrintStream ps = new PrintStream( baos);
            //e.printStackTrace(ps);
            log.debug(baos.toString());

            mpr.addSoapBodyPart(ExceptionElementUtil.makeExceptionElement(e));
        }


    }


    /**
     *
     * @param req
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    private  Document getSOAPDoc(HttpServletRequest req) throws IOException, JDOMException {

        SAXBuilder saxBldr = new SAXBuilder();
        Document doc = saxBldr.build(req.getReader());
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        log.debug("POST Method got this XML Document:" +
                xmlo.outputString(doc) );

        return doc;
    }

    /**
     *
     * @param doc
     * @return  Returns true if the SOAP document looks good, false if not.
     */
    private  boolean qcSOAPDocument(Document doc) {
        boolean result;


        Namespace soapEnvNameSpace = XMLNamespaces.getDefaultSoapEnvNamespace();

        result = false;

        Element se = doc.getRootElement();

        log.debug("DocRoot: " + se.getName() + "    getOpendapSoapNamespace: " + se.getNamespace().getURI());

        if (se.getName().equals("Envelope") && se.getNamespace().equals(soapEnvNameSpace)) {
            Iterator it = se.getChildren().iterator();

            Element sb = null;
            int i = 0;
            while (it.hasNext()) {
                sb = (Element) it.next();
                i++;
            }
            if (sb!=null && (i == 1 || i == 2)) {
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



