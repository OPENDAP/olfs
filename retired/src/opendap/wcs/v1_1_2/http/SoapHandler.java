/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
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
package opendap.wcs.v1_1_2.http;

import opendap.coreServlet.DispatchServlet;
import opendap.namespaces.NS;
import opendap.namespaces.SOAP;
import opendap.wcs.v1_1_2.*;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.util.List;

/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Feb 7, 2009
 * Time: 9:00:41 AM
 * To change this template use File | Settings | File Templates.
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

        if(NS.checkNamespace(soapEnvelope,"Envelope", SOAP.NS)){
            soapBody = soapEnvelope.getChild("Body", SOAP.NS);
            List soapContents = soapBody.getChildren();

            log.debug("Got " + soapContents.size() + " child elements of SOAP body.");


            if(soapContents.size()!=1){
                String msg = "SOAP message body contains "+soapContents.size()+" items. Only one item is allowed.";
                log.error(msg);
                throw new WcsException(msg,
                        WcsException.INVALID_PARAMETER_VALUE,
                        "WCS Request Document");
            }

            Element wcsRequest = (Element) soapContents.get(0);
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
    public Document getCapabilities(GetCapabilitiesRequest wcsRequest, String serviceUrl) throws InterruptedException, WcsException {

        return wrapDocumentInSoapEnvelope(super.getCapabilities(wcsRequest, serviceUrl));

    }


    @Override
    public Document describeCoverage(DescribeCoverageRequest wcsRequest) throws InterruptedException, WcsException {

        return wrapDocumentInSoapEnvelope(super.describeCoverage(wcsRequest));

    }


    @Override
    public Document getStoredCoverage(GetCoverageRequest req) throws InterruptedException, WcsException {

        return wrapDocumentInSoapEnvelope(super.getStoredCoverage(req));

    }

    @Override
    public void sendCoverageResponse(GetCoverageRequest req, HttpServletResponse response) throws InterruptedException, WcsException {

        CoverageRequestProcessor.sendCoverageResponse(req, response, true );

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
