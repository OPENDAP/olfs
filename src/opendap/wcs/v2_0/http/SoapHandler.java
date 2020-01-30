/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.wcs.v2_0.http;

import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.coreServlet.DispatchServlet;
import opendap.dap.User;
import opendap.namespaces.NS;
import opendap.namespaces.SOAP;
import opendap.ppt.PPTException;
import opendap.wcs.v2_0.*;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 *
 *  This class extends the XmlRequestHandler to deal with the
 *  SOAP wrapper and all that truck.
 *
 */
public class SoapHandler extends XmlRequestHandler {


    public SoapHandler() {
        super();
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }
    public void init(DispatchServlet servlet, Element config) throws Exception {
        super.init(servlet,config);
    }


    @Override
    public Document parseWcsRequest(BufferedReader sis, String encoding) throws WcsException {

        Document soapRequestDocument;
        Element soapEnvelope;
        Element soapBody;

        soapRequestDocument = super.parseWcsRequest(sis,encoding);

        soapEnvelope = soapRequestDocument.getRootElement();
        if(soapEnvelope==null)
            throw new WcsException("Root element located in request document! ",
                    WcsException.INVALID_PARAMETER_VALUE);

        if(NS.checkNamespace(soapEnvelope,"Envelope", SOAP.NS)){
            soapBody = soapEnvelope.getChild("Body", SOAP.NS);
            if(soapBody==null)
                throw new WcsException("SOAP Envelope is missing SOAP Body element.",
                        WcsException.INVALID_PARAMETER_VALUE);

            List<Element> soapContents = soapBody.getChildren();
            log.debug("Got " + soapContents.size() + " child elements of SOAP body.");

            if(soapContents.size()!=1){
                String msg = "SOAP message body contains "+soapContents.size()+" items. Only one item is allowed.";
                log.error(msg);
                throw new WcsException(msg,
                        WcsException.INVALID_PARAMETER_VALUE,
                        "WCS Request Document");
            }
            Element wcsRequest = soapContents.get(0);
            wcsRequest.detach();
            return new Document(wcsRequest);
        }
        else {
            String msg = "Request document is not a SOAP envelope.";
            log.error(msg);
            throw new WcsException(msg,
                    WcsException.INVALID_PARAMETER_VALUE,
                    "WCS Request Document");
        }
    }


    @Override
    public Document getCapabilities(User user, GetCapabilitiesRequest wcsRequest, String serviceUrl) throws InterruptedException, WcsException {

        return wrapDocumentInSoapEnvelope(super.getCapabilities(user, wcsRequest, serviceUrl));

    }


    @Override
    public Document describeCoverage(User user, DescribeCoverageRequest wcsRequest) throws InterruptedException, WcsException {

        return wrapDocumentInSoapEnvelope(super.describeCoverage(user,wcsRequest));

    }



    @Override
    public void sendCoverageResponse(User user, GetCoverageRequest req, HttpServletResponse response) throws InterruptedException, WcsException, IOException, PPTException, BadConfigurationException, BESError {

        GetCoverageRequestProcessor.sendCoverageResponse(user, req, response, true );

    }


    public static  Document wrapDocumentInSoapEnvelope(Document doc){

        Element soapEnvelope = new Element("Envelope",SOAP.NS);
        Element soapBody = new Element("Body",SOAP.NS);

        soapEnvelope.addContent(soapBody);


        Element rootElem = doc.getRootElement();
        rootElem.detach();

        soapBody.addContent(rootElem);

        return new Document(soapEnvelope);

    }




}
