/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
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

package opendap.olfs;

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

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 *
 * Handler for SOAP requests.
 *
 */
public class SoapDispatchHandler implements OpendapSoapDispatchHandler {

    Logger log;

    /**
     *
     * @param ds The Servlet that is aclling init().
     * @throws ServletException
     */
    public void init(HttpServlet ds) throws ServletException{
        log = org.slf4j.LoggerFactory.getLogger(getClass());


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
    public void getDATA( String reqID,  Element cmd, MultipartResponse mpr) throws Exception {

        Namespace osnms = XMLNamespaces.getOpendapSoapNamespace();

        log.debug("Received GetDATA reqElement.");
        Element dataSet = cmd.getChild("DataSet", osnms);

        log.debug("Dataset:\n"+dataSet.toString());


        String datasetname = dataSet.getChild("name",osnms).getTextTrim();
        String ce = dataSet.getChild("ConstraintExpression",osnms).getTextTrim();

        log.debug("Processing DataSet - path: "+datasetname+"   ce: "+ce);

        Element respElement = new Element("Response",osnms);
        respElement.setAttribute("reqID",reqID,osnms);
        String contentId = MultipartResponse.newUidString();
        respElement.setAttribute("href","cid:"+contentId,osnms);


        Document ddxDoc =  BesAPI.getDDXDocument(datasetname, ce);
        Element ddx = ddxDoc.getRootElement();
        //@todo Fix The BES use of dodsBLOB!

        Element blob = ddx.getChild("dataBLOB", XMLNamespaces.getOpendapDAP2Namespace());

        String blobID = MultipartResponse.newUidString();

        //@todo Add the namespace to the href - first we must add it to the schema!
        blob.setAttribute("href", "cid:" + blobID);



        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        xmlo.output(ddxDoc,baos);


        mpr.addAttachment("text/xml",
                contentId,
                new ByteArrayInputStream(baos.toByteArray()));


        mpr.addAttachment("application/octet-stream",
                blobID,
                BesAPI.getDap2DataStream(datasetname, ce,BesAPI.XML_ERRORS));

        mpr.addSoapBodyPart(respElement);
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
        log.debug("Received GetDDX reqElement.");


        Element dataSet = cmd.getChild("DataSet", osnms);

        log.debug("Dataset:\n"+dataSet.toString());


        String datasetname = dataSet.getChild("name",osnms).getTextTrim();
        String ce = dataSet.getChild("ConstraintExpression",osnms).getTextTrim();

        log.debug("Processing DataSet - path: "+datasetname+"   ce: "+ce);

        Element respElement = new Element("Response",osnms);
        respElement.setAttribute("reqID",reqID,osnms);

        // Note that this call does not parse the DDX document into an opendap.dap.DDS, just
        // into a jdom.Document that gets it's root element stuffed into the SOAP envelope.
        respElement.addContent(BesAPI.getDDXDocument(datasetname, ce).detachRootElement());

        mpr.addSoapBodyPart(respElement);

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



        log.debug("Received GetTHREDDSCatalog reqElement.");

        String path = cmd.getChild("path",osnms).getTextTrim();

        path = BESCrawlableDataset.besPath2ThreddsPath(path);

        BESCrawlableDataset s4cd = new BESCrawlableDataset(path, null);

        String reqURI   = srvReq.getRequestURI();
        String pathInfo = srvReq.getPathInfo();

/*
        respElement =  ExceptionElementUtil.makeExceptionElement(
                "CatalogGenError",

                        "getRequestURI(): "+srvReq.getRequestURI()+"     " +
                        "getPathInfo(): "+srvReq.getPathInfo()+"\n",
                "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
        );
        mpr.addSoapBodyPart(respElement);
*/

//@todo Why did I have to do this? doPost() and doGet() have different behaviours relative to reqURI and pathINFO
        String baseURL;
        if(pathInfo != null)
            baseURL = reqURI.substring(0, reqURI.lastIndexOf(pathInfo) + 1);
        else
            baseURL = reqURI+"/";


        if (s4cd.isCollection()) {

            SimpleCatalogBuilder scb = new SimpleCatalogBuilder(
                    "",                                   // CollectionID, which for us needs to be empty.
                    BESCrawlableDataset.getRootDataset(), // Root dataset of this collection
                    "OPeNDAP-Server4",                    // Service Name
                    "OPeNDAP",                            // Service Type Name
                    baseURL ); // Base URL for this service

            log.debug("SOAPRequestDispatcher:GetTHREDDSCatalog - " +
                    "Generating catalog using SimpleCatalogBuilder");



            Document catalog = scb.generateCatalogAsDocument(s4cd);

            if(catalog == null){
                log.debug("SimpleCatalogBuilder.generateCatalogAsDocument("+path+") returned null.");
                respElement =  ExceptionElementUtil.makeExceptionElement(
                        "BadSOAPRequest",
                        "Requested catalog ("+cmd.getChild("path").getTextTrim()+" is not available.",
                        "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
                );
            }
            else {
                respElement = new Element("Response",osnms);
                respElement.setAttribute("reqID",reqID,osnms);
                respElement.addContent(catalog.detachRootElement());
            }

        } else {

            String msg = "ERROR: THREDDS catalogs may only be requested for collections, " +
                    "not for individual data sets. The path: \""+cmd.getChild("path").getTextTrim()+
                    "\" does not resolve to a collection.";

            respElement =  ExceptionElementUtil.makeExceptionElement(
                    "BadSOAPRequest",
                    msg,
                    "opendap.coreServlet.SOAPRequestDispatcher.soapDispatcher()"
            );

        }
        mpr.addSoapBodyPart(respElement);


    }
}
