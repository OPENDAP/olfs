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

package opendap.bes.dap4Responders.DatasetServices;

import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.ServiceMediaType;
import opendap.bes.dapResponders.BesApi;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/4/12
 * Time: 9:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class XmlDSR extends Dap4Responder {


    private Logger log;
    private static String defaultRequestSuffix = ".xml";

    NormativeDSR normDSR;

    public XmlDSR(String sysPath, BesApi besApi, NormativeDSR dsr) {
        this(sysPath,null, defaultRequestSuffix,besApi,dsr);
    }

    public XmlDSR(String sysPath, String pathPrefix, BesApi besApi, NormativeDSR dsr) {
        this(sysPath,pathPrefix, defaultRequestSuffix,besApi,dsr);
    }

    public XmlDSR(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi, NormativeDSR dsr) {
        super(sysPath, pathPrefix, requestSuffix, besApi);

        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        log.debug("defaultRequestSuffix: '{}'", defaultRequestSuffix);

        setServiceRoleId("http://services.opendap.org/dap4/dataset-services");
        setServiceTitle("XML Dataset Services Response");
        setServiceDescription("The XML representation of the DSR.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Services_Description_Service");
        //setPreferredServiceSuffix(defaultRequestSuffix);

        setNormativeMediaType(new ServiceMediaType("text","xml", getRequestSuffix()));

        normDSR = dsr;

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }



    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestedResource = request.getRequestURL().toString();
        String baseUrl = opendap.coreServlet.Util.dropSuffixFrom(requestedResource,getRequestSuffixMatchPattern());

        String context = request.getContextPath()+"/";

        Document serviceDescription = new Document();


        log.debug("Sending {} for dataset: {}",getServiceTitle(),baseUrl);

        HashMap<String,String> piMap = new HashMap<String,String>( 2 );
        piMap.put( "type", "text/xsl" );
        piMap.put( "href", context+"xsl/datasetServices.xsl" );
        ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

        serviceDescription.addContent( pi );

        Element datasetServices;

        datasetServices = normDSR.getDatasetServicesElement(baseUrl);

        serviceDescription.setRootElement(datasetServices);

        response.setContentType(getNormativeMediaType().getMimeType());
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        xmlo.output(serviceDescription, response.getOutputStream());

        log.debug("Sent {}",getServiceTitle());

    }


}
