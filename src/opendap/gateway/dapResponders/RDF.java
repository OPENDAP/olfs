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
package opendap.gateway.dapResponders;

import opendap.bes.BESError;
import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.gateway.BesGatewayApi;
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


public class RDF extends BesDapResponder {
    private Logger log;


    private BesGatewayApi _besGatewayApi;

    private static String defaultRequestSuffixRegex = "\\.rdf";


    public RDF(String sysPath, BesGatewayApi besApi) {
        this(sysPath, null, defaultRequestSuffixRegex, besApi);
    }

    public RDF(String sysPath, String pathPrefix, BesGatewayApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffixRegex, besApi);
    }

    public RDF(String sysPath, String pathPrefix,  String requestSuffixRegex, BesGatewayApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        _besGatewayApi = besApi;
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }



    @Override
    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);


        String dataSourceUrl = _besGatewayApi.getDataSourceUrl(request, getPathPrefix());
        String context = request.getContextPath();

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());


        log.debug("respondToHttpGetRequest() Sending RDF for dataset: " + dataSource);




        String xdap_accept = "3.2";
        Document reqDoc = _besGatewayApi.getRequestDocument(
                                                        BesApi.DDX,
                                                        dataSourceUrl,
                                                        constraintExpression,
                                                        xdap_accept,
                                                        0,
                                                        xmlBase,
                                                        null,
                                                        null,
                                                        BesApi.DAP2_ERRORS);



        log.debug("_besGatewayApi.getRequestDocument() returned:\n "+xmlo.outputString(reqDoc));

        Document ddx = new Document();
        if(!_besGatewayApi.besTransaction(dataSource,reqDoc,ddx)){
            BESError besError = new BESError(xmlo.outputString(ddx));
            besError.sendErrorResponse(_systemPath, context, response);
            log.error("respondToHttpGetRequest() encountered a BESError:\n" + xmlo.outputString(ddx));
            return;
        }

        ddx.getRootElement().setAttribute("dataset_id",dataSource);

        log.debug(xmlo.outputString(ddx));

        ServletOutputStream os = response.getOutputStream();
        StreamSource ddxStreamSource  = new StreamSource(new ByteArrayInputStream(xmlo.outputString(ddx).getBytes()));

        /*
         Because we are going to daisy chain the XSLT's we have to be careful here!
         */

        // Make the first Transform
        String addRdfId2DapTransformFileName = _systemPath + "/xsl/addRdfId2Dap3.2.xsl";
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

        // Set the response headers

        String accepts = request.getHeader("Accepts");

        if(accepts!=null && accepts.equalsIgnoreCase("application/rdf+xml"))
            response.setContentType("application/rdf+xml");
        else
            response.setContentType("text/xml");

        Version.setOpendapMimeHeaders(request,response, _besGatewayApi);
        response.setHeader("Content-Description", "text/xml");


        // run the 1st transform. This will send the result through the 2nd transform and
        // the result of the 2nd transform will then be sent out the response OutputStream


        try {
            addRdfId2DdxTransform.transform(ddxStreamSource);
        } catch (Exception e) {
            sendRdfErrorResponse(e, dataSource, context, response);
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
