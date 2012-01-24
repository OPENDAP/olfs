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

package opendap.bes;

import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.OpendapSoapDispatchHandler;
import opendap.coreServlet.MultipartResponse;
import opendap.coreServlet.DispatchServlet;
import opendap.soap.XMLNamespaces;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;


import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * Handler for SOAP requests.
 */
public class SoapDispatchHandler implements OpendapSoapDispatchHandler {

    Logger log;

    //DataRootHandler _dataRootHandler;

    /**
     * @param ds  The Servlet that is calling init().
     * @throws ServletException When things go wrong.
     */
    public void init(HttpServlet ds) throws ServletException {

        //_dataRootHandler = ds.getThreddsDispatchHandler().getDataRootHandler();

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        log.info("init() complete.");

    }


    /**
     * Handles a SOAP request for OPeNDAP data. This version places an href attribute in the Response element in the
     * SOAP envelope that references an attachment containing the DDX, which gets added as an attachment. The DDX
     * references (via an href attribute in the dodsBlob element) the data which is added as another attachment to
     * the Multipart MIME message.
     *
     * @param reqID The request ID for this request.
     * @param cmd The GetDATA command Element from the SOAP envelope.
     * @param mpr The MultipartResponse into which to write the response.
     * @throws Exception When the bad things happen
     */
    public void getDATA(String reqID, String xmlBase,  Element cmd, MultipartResponse mpr) throws Exception {

        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        Element dataSet = cmd.getChild("DataSet", osnms);

        log.debug("getDATA() Dataset:\n" + dataSet.toString());


        String datasetname = dataSet.getChild("name", osnms).getTextTrim();
        String ce = dataSet.getChild("ConstraintExpression", osnms).getTextTrim();

        log.debug("getDATA() Processing DataSet - path: " + datasetname + "   ce: " + ce);

        Element respElement = new Element("Response", osnms);
        respElement.setAttribute("reqID", reqID, osnms);
        String contentId = MultipartResponse.newUidString();
        respElement.setAttribute("href", "cid:" + contentId, osnms);


        Document ddxDoc = new Document();


        boolean besError = BesXmlAPI.getDDXDocument(datasetname, ce, null, xmlBase, ddxDoc);


        // Add the returned document to the message. It may be an error!
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(ddxDoc, baos);
        mpr.addAttachment("text/xml",
                contentId,
                new ByteArrayInputStream(baos.toByteArray()));


        // If it's not an error, then try to add the data attachment.
        if(!besError){

            Element ddx = ddxDoc.getRootElement();
            //@todo Fix The BES use of dodsBLOB!

            Element blob = ddx.getChild("dataBLOB", XMLNamespaces.getOpendapDAP2Namespace());

            String blobID = MultipartResponse.newUidString();

            //@todo Add the namespace to the href - first we must add it to the schema!
            blob.setAttribute("href", "cid:" + blobID);

            mpr.addAttachment("application/octet-stream",
                    blobID,
                    BesXmlAPI.getDap2DataStream(datasetname, ce, null));

            mpr.addSoapBodyPart(respElement);
            log.debug("getDATA() completed.");

        }
        else {

        }

    }


    /**
     * Handles a SOAP request for an OPeNDAP DDX.
     *
     * @param reqID
     * @param cmd
     * @param mpr
     * @throws Exception
     */
    public void getDDX(String reqID, String xmlBase, Element cmd, MultipartResponse mpr) throws Exception {


        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();


        Element dataSet = cmd.getChild("DataSet", osnms);

        log.debug("getDDX() Dataset:\n" + dataSet.toString());


        String datasetname = dataSet.getChild("name", osnms).getTextTrim();
        String ce = dataSet.getChild("ConstraintExpression", osnms).getTextTrim();

        log.debug("getDDX() Processing DataSet path: " + datasetname + "   ce: " + ce);

        Element respElement = new Element("Response", osnms);
        respElement.setAttribute("reqID", reqID, osnms);

        // Note that this call does not parse the DDX document into an opendap.dap.DDS, just
        // into a JDOM Document that gets it's root element stuffed into the SOAP envelope.
        Document ddx = new Document();

        // Note that f there is a BESError returned it will be stuffed into the
        // SOAP eveope in lieu of the DDX.
        BesXmlAPI.getDDXDocument(datasetname, ce, null, xmlBase,  ddx);

        respElement.addContent(ddx.detachRootElement());

        mpr.addSoapBodyPart(respElement);


        log.debug("getDDX() completed.");

    }



    /**
     * Handles a SOAP request for a THREDDS catalog.
     *
     * @param srvReq
     * @param reqID
     * @param cmd
     * @param mpr
     * @throws Exception
     */
    public void getTHREDDSCatalog(HttpServletRequest srvReq, String reqID, Element cmd, MultipartResponse mpr) throws Exception {


        log.error("THREDDS catalogs not currently implemented for SOAP interface.");
        throw new Exception("THREDDS catalogs not currently implemented for SOAP interface.");


        /*



        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();
        Element respElement;


        String path = cmd.getChild("path", osnms).getTextTrim();

        log.debug("getTHREDDSCatalog() SOAP message is requesting a THREDDS catalog for path: " + path);

        String catalogName;

        if(path.endsWith("/"))
            catalogName = path + "catalog.xml";
        else
            catalogName = path + "/catalog.xml";


        log.debug("getTHREDDSCatalog() Requesting catalog: " + catalogName);

        URI baseURI = thredds.servlet.ServletUtil.getRequestURI(srvReq);

        InvCatalog catalog = _dataRootHandler.getCatalog(catalogName, baseURI);


        if (catalog == null) {
            log.warn("getTHREDDSCatalog() DataRootHandler.getCatalog(" + path + ","+baseURI+") returned null.");
            respElement = ExceptionElementUtil.makeExceptionElement(
                    "BadSOAPRequest",
                    "Requested catalog \"" + path + "\" is not availableInChunk.",
                    "opendap.bes.SOAPDispatchHandler.getTHREDDSCatalog()"
            );
        } else {
            StringBuffer sb = new StringBuffer();

            if (catalog.check(sb)) {


                InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory(false);
                InvCatalogConvertIF converter = fac.getCatalogConverter(XMLEntityResolver.CATALOG_NAMESPACE_10);
                InvCatalogFactory10 fac10 = (InvCatalogFactory10) converter;

                Document catalogDoc = fac10.writeCatalog((InvCatalogImpl) catalog);


                if (catalogDoc == null) {
                    log.warn("getTHREDDSCatalog()  InvCatalogFactory10.writeCatalog(" + path + ") returned null.");
                    respElement = ExceptionElementUtil.makeExceptionElement(
                            "BadSOAPRequest",
                            "Requested catalog (" + path + " is not availableInChunk.",
                            "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                    );
                } else {
                    respElement = new Element("Response", osnms);
                    respElement.setAttribute("reqID", reqID, osnms);
                    respElement.addContent(catalogDoc.detachRootElement());
                }

            } else {

                String msg = "ERROR: THREDDS InvCatalog.check() failed! " +
                        "The path: \"" + path  +
                        "\" does not appear to resolve to a valid THREDDS catalog. " +
                        "InvCatalog.check() returned: " + sb;
                log.warn("getTHREDDSCatalog()   "+msg);

                respElement = ExceptionElementUtil.makeExceptionElement(
                        "BadSOAPRequest",
                        msg,
                        "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                );

            }
        }
        mpr.addSoapBodyPart(respElement);

        log.debug("getTHREDDSCatalog() completed.");
        */

    }


    public void setOpendapMimeHeaders(HttpServletRequest request,
                                      HttpServletResponse response)
            throws Exception{

        Version.setOpendapMimeHeaders(request,response, new BesApi());
    }



}
