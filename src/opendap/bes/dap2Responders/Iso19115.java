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
import opendap.bes.BesApi;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.dap.User;
import opendap.http.mediaTypes.TextXml;
import opendap.logging.ServletLogUtil;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;

/**
 * Responder that transmits JSON encoded DAP2 data to the client.
 */
public class Iso19115 extends Dap4Responder {

    private Logger log;
    private static String defaultRequestSuffix = ".iso";

    public Iso19115(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public Iso19115(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public Iso19115(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("ISO-19115 Metadata");
        setServiceDescription("ISO-19115 metadata extracted form the normative DMR.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_DDX_Service");

        setNormativeMediaType(new TextXml(getRequestSuffix()));

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());
    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }


    @Override
    public boolean matches(String relativeUrl, boolean checkWithBes){
        return super.matches(relativeUrl,checkWithBes);
    }


    @Override
    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // String context = request.getContextPath();
        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);

        String resourceID = getResourceId(requestedResourceId, false);


        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        MediaType responseMediaType =  getNormativeMediaType();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        Version.setOpendapMimeHeaders(request, response);
        response.setHeader("Content-Description","ISO 19115 Metadata");

        Document ddx = new Document();
        User user = new User(request);
        besApi.getDDXDocument(
                user,
                resourceID,
                constraintExpression,
                xmlBase,
                ddx);

        ddx.getRootElement().setAttribute("dataset_id",resourceID);

        String currentDir = System.getProperty("user.dir");
        log.debug("Cached working directory: "+currentDir);


        String xslDir = new PathBuilder(_systemPath).pathAppend("nciso").pathAppend("xsl").toString();


        log.debug("Changing working directory to "+ xslDir);
        System.setProperty("user.dir",xslDir);

        try {
            String xsltDocName = "ddx2iso.xsl";


            // This Transformer class is an attempt at making the use of the saxon-9 API
            // a little simpler to use. It makes it easy to set input parameters for the stylesheet.
            // See the source code for opendap.xml.Transformer for more.
            Transformer transformer = new Transformer(xsltDocName);

            DataOutputStream os = new DataOutputStream(response.getOutputStream());

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform(new JDOMSource(ddx), os);


            os.flush();
            ServletLogUtil.setResponseSize(os.size());
            log.debug("Sent {} size:{}",getServiceTitle(),os.size());
        }
        finally {
            log.debug("Restoring working directory to " + currentDir);
            System.setProperty("user.dir", currentDir);
        }



    }



}
