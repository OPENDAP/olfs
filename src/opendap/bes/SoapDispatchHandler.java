/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
//
// Copyright (c) 2006 OPeNDAP, Inc.
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

import opendap.coreServlet.OpendapSoapDispatchHandler;
import opendap.coreServlet.MultipartResponse;
import opendap.soap.XMLNamespaces;
import opendap.soap.ExceptionElementUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;


import thredds.cataloggen.SimpleCatalogBuilder;
import thredds.servlet.DataRootHandler;
import thredds.catalog.*;
import thredds.catalog.parser.jdom.InvCatalogFactory10;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URI;

/**
 * Handler for SOAP requests.
 */
public class SoapDispatchHandler implements OpendapSoapDispatchHandler {

    Logger log;

    DataRootHandler _dataRootHandler;

    /**
     * @param ds  The Servlet that is calling init().
     * @param drh The DataRootHandler that will be used to handle THREDDS requests.
     * @throws ServletException When things go wrong.
     */
    public void init(HttpServlet ds, DataRootHandler drh) throws ServletException {

        _dataRootHandler = drh;

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
     * @param cmd
     * @param mpr
     * @throws Exception
     */
    public void getDATA(String reqID, Element cmd, MultipartResponse mpr) throws Exception {

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


        Document ddxDoc = BesAPI.getDDXDocument(datasetname, ce);
        Element ddx = ddxDoc.getRootElement();
        //@todo Fix The BES use of dodsBLOB!

        Element blob = ddx.getChild("dataBLOB", XMLNamespaces.getOpendapDAP2Namespace());

        String blobID = MultipartResponse.newUidString();

        //@todo Add the namespace to the href - first we must add it to the schema!
        blob.setAttribute("href", "cid:" + blobID);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        xmlo.output(ddxDoc, baos);


        mpr.addAttachment("text/xml",
                contentId,
                new ByteArrayInputStream(baos.toByteArray()));


        mpr.addAttachment("application/octet-stream",
                blobID,
                BesAPI.getDap2DataStream(datasetname, ce, BesAPI.XML_ERRORS));

        mpr.addSoapBodyPart(respElement);
        log.debug("getDATA() completed.");
    }


    /**
     * Handles a SOAP request for an OPeNDAP DDX.
     *
     * @param reqID
     * @param cmd
     * @param mpr
     * @throws Exception
     */
    public void getDDX(String reqID, Element cmd, MultipartResponse mpr) throws Exception {
        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();


        Element dataSet = cmd.getChild("DataSet", osnms);

        log.debug("getDDX() Dataset:\n" + dataSet.toString());


        String datasetname = dataSet.getChild("name", osnms).getTextTrim();
        String ce = dataSet.getChild("ConstraintExpression", osnms).getTextTrim();

        log.debug("getDDX() Processing DataSet path: " + datasetname + "   ce: " + ce);

        Element respElement = new Element("Response", osnms);
        respElement.setAttribute("reqID", reqID, osnms);

        // Note that this call does not parse the DDX document into an opendap.dap.DDS, just
        // into a jdom.Document that gets it's root element stuffed into the SOAP envelope.
        respElement.addContent(BesAPI.getDDXDocument(datasetname, ce).detachRootElement());

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
                    "Requested catalog \"" + path + "\" is not available.",
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
                            "Requested catalog (" + path + " is not available.",
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

    }


}
