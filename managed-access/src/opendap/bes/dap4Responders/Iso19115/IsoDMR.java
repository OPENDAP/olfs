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

package opendap.bes.dap4Responders.Iso19115;

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.dap4.QueryParameters;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/5/12
 * Time: 8:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class IsoDMR extends Dap4Responder {

    private Logger log;
    private static String defaultRequestSuffix = ".dmr.iso";


    public IsoDMR(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public IsoDMR(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public IsoDMR(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("ISO-19115 Metadata");
        setServiceDescription("ISO-19115 metadata extracted form the normative DMR.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#Dataset_Metadata_Response");

        setNormativeMediaType(new MediaType("text","xml", getRequestSuffix()));

        IsoRubricDMR rubric =  new IsoRubricDMR(sysPath, pathPrefix, besApi);

        addAltRepResponder(rubric);

        //rubric.setRequestSuffix(buildRequestMatchingRegex());

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return false; }
    public boolean isMetadataResponder(){ return true; }


    @Override
    public Element getServiceElement(String datasetUrl){
        Element service = getServiceElement();

        Element link = getNormativeLink(datasetUrl);

        service.addContent(link);

        for(Dap4Responder altRepResponder: getAltRepResponders()){
            MediaType altMediaType = altRepResponder.getNormativeMediaType();
            String href = datasetUrl + altMediaType.getMediaSuffix();
            link = getLinkElement(altMediaType.getMimeType(),href,altRepResponder.getServiceDescription());
            service.addContent(link);
        }

        return service;

    }


    @Override
    public String buildRequestMatchingRegex() {

        StringBuilder s = new StringBuilder();
        s.append(buildRequestMatchingRegexWorker(this));
        s.append("$");
        log.debug("Request Match Regex: {}", s.toString());
        return s.toString();

    }

    private String buildRequestMatchingRegexWorker(Dap4Responder responder) {

        StringBuilder s = new StringBuilder();

        Dap4Responder[] altResponders = responder.getAltRepResponders();
        boolean hasAltRepResponders = altResponders.length > 0;

        if (hasAltRepResponders)
            s.append("((");

        if (responder.getNormativeMediaType().getMediaSuffix().startsWith("."))
            s.append("\\");
        s.append(responder.getNormativeMediaType().getMediaSuffix());

        if (hasAltRepResponders)
            s.append(")");

        for (Dap4Responder altResponder : altResponders) {

            s.append("|");

            s.append("(");

            s.append(buildRequestMatchingRegexWorker(altResponder));

            s.append(")");

        }

        if (hasAltRepResponders)
            s.append(")");

        return s.toString();

    }


    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String context = request.getContextPath();
        String requestedResourceId = ReqInfo.getLocalUrl(request);

        QueryParameters qp = new QueryParameters(request);
        String xmlBase = getXmlBase(request);

        String resourceID = getResourceId(requestedResourceId, false);


        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        response.setContentType(getNormativeMediaType().getMimeType());
        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());


        OutputStream os = response.getOutputStream();


        Document dmr = new Document();


        if(!besApi.getDMRDocument(
                resourceID,
                qp,
                xmlBase,
                dmr)){
            response.setHeader("Content-Description", "application/vnd.opendap.dap4.error+xml");

            BESError error = new BESError(dmr);
            error.sendErrorResponse(_systemPath, context, response);
        }
        else {

            dmr.getRootElement().setAttribute("dataset_id",resourceID);

            String currentDir = System.getProperty("user.dir");
            log.debug("Cached working directory: "+currentDir);


            String xslDir = _systemPath + "/nciso/xsl";


            log.debug("Changing working directory to "+ xslDir);
            System.setProperty("user.dir",xslDir);

            String xsltDocName = "ddx2iso.xsl";


            // This Transformer class is an attempt at making the use of the saxon-9 API
            // a little simpler to use. It makes it easy to set input parameters for the stylesheet.
            // See the source code for opendap.xml.Transformer for more.
            Transformer transformer = new Transformer(xsltDocName);

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform( new JDOMSource(dmr),os);




            os.flush();
            log.info("Sent {}",getServiceTitle());
            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);
        }



    }

}
