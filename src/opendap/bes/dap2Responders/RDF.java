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
package opendap.bes.dap2Responders;

import opendap.PathBuilder;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.http.mediaTypes.TextXml;
import opendap.io.HyraxStringEncoding;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;


public class RDF extends Dap4Responder {

    private Logger log;


    private static String _defaultRequestSuffix = ".rdf";

    public RDF(String sysPath, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        this(sysPath,null, _defaultRequestSuffix,besApi, addTypeSuffixToDownloadFilename);
    }


    public RDF(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi, boolean addTypeSuffixToDownloadFilename) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        setServiceRoleId("http://services.opendap.org/dap4/rdf");
        setServiceTitle("DAP2 RDF");
        setServiceDescription("An RDF representation of the DAP2 Dataset response (DDX) document.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_DDX_Service");

        setNormativeMediaType(new opendap.http.mediaTypes.RDF(getRequestSuffix()));
        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }



    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }


    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl,false);

        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);
        String context = request.getContextPath();

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        log.debug("respondToHttpGetRequest() Sending RDF for dataset: " + resourceID);



        BesApi besApi = getBesApi();



        // Set the response headers

        MediaType responseMediaType =  getNormativeMediaType();
        String accepts = request.getHeader("Accepts");
        if(accepts==null|| !accepts.equalsIgnoreCase("application/rdf+xml"))
            responseMediaType = new TextXml();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());


        Version.setOpendapMimeHeaders(request,response,besApi);
        response.setHeader("Content-Description", "RDF Encoding of DAP2 DDX");

        // response.setHeader("Content-Disposition", " attachment; filename=\"" +getDownloadFileName(resourceID)+"\"");


        String xdap_accept = "3.2";

        Document ddx = new Document();

        besApi.getDDXDocument(resourceID, constraintExpression, xdap_accept, xmlBase, ddx);

        ddx.getRootElement().setAttribute("dataset_id",resourceID);

        log.debug(xmlo.outputString(ddx));

        ServletOutputStream os = response.getOutputStream();
        StreamSource ddxStreamSource  =
                new StreamSource(new ByteArrayInputStream(xmlo.outputString(ddx).getBytes( HyraxStringEncoding.getCharset())));

        /*
         Because we are going to daisy chain the XSLT's we have to be careful here!
         */

        // Make the first Transform
        String addRdfId2DapTransformFileName =
                new PathBuilder(_systemPath).pathAppend("xsl").pathAppend("addRdfId2Dap3.2.xsl").toString();

        Transformer addRdfId2DdxTransform = new Transformer(addRdfId2DapTransformFileName);

        // Grab it's Processor object. All of the XSLT's in the chain must be built
        // using the same Processor
        net.sf.saxon.s9api.Processor proc = addRdfId2DdxTransform.getProcessor();

        // Make the 2nd Transform using the Processor from the first.
        String xml2rdfFileName = _systemPath + "/xsl/anyXml2Rdf.xsl";
        Transformer xml2rdf = new Transformer(proc, xml2rdfFileName);


        // set the destination of the 1st transform to be the 2nd transform
        addRdfId2DdxTransform.setDestination(xml2rdf);

        // Set the destination of the 2nd transform to be the response OutputStream
        xml2rdf.setOutputStream(os);



        // run the 1st transform. This will send the result through the 2nd transform and
        // the result of the 2nd transform will then be sent out the response OutputStream


        try {
            addRdfId2DdxTransform.transform(ddxStreamSource);
        } catch (Exception e) {
            sendRdfErrorResponse(e, resourceID, context, response);
            log.error(e.getMessage());
        }


        log.info("Sent RDF version of DDX.");



    }





    public void sendRdfErrorResponse(Exception e, String dataSource, String docsService, HttpServletResponse response) throws Exception {

        String errorMessage =
                        "<p align=\"center\">I'm sorry.</p>\n" +
                        "<p align=\"center\">You requested the RDF representation of the metadata for the dataset:</p>\n" +
                        "<p align=\"center\" class=\"bold\">"+dataSource+" </p>\n" +
                        "<p align=\"center\">The server attempted to transform the metadata in the dataset, " +
                                "represented as a DDX document, into an RDF representation.</p>\n" +
                        "<p align=\"center\">The transform failed, and returned this specific error message:</p>\n" +
                        "<p align=\"center\" class=\"bold\">" + e.getMessage() + "</p>\n";


        sendHttpErrorResponse(500, errorMessage, docsService, response);

    }



}
