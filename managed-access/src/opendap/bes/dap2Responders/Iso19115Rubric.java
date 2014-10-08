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

import opendap.bes.BESError;
import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.dap.Request;
import opendap.dap.User;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Responder that transmits JSON encoded DAP2 data to the client.
 */
public class Iso19115Rubric extends Dap4Responder {

    private Logger log;
    private static String defaultRequestSuffix = ".rubric";

    public Iso19115Rubric(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public Iso19115Rubric(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public Iso19115Rubric(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/dataset-metadata");
        setServiceTitle("ISO-19115 Metadata");
        setServiceDescription("ISO-19115 metadata extracted form the normative DMR.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4:_Specification_Volume_2#DAP2:_DDX_Service");

        setNormativeMediaType(new MediaType("text","xml", getRequestSuffix()));

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

        String context = request.getContextPath();
        String requestedResourceId = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);

        String resourceID = getResourceId(requestedResourceId, false);

        Request oreq = new Request(null,request);


        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}", getServiceTitle(), resourceID);

        response.setContentType(getNormativeMediaType().getMimeType());
        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");


        OutputStream os = response.getOutputStream();


        String xdap_accept = "3.2";



        Document ddx = new Document();


        if(!besApi.getDDXDocument(
                resourceID,
                constraintExpression,
                xdap_accept,
                xmlBase,
                ddx)){
            response.setHeader("Content-Description", "application/vnd.opendap.dap2.error");

            BESError error = new BESError(ddx);
            error.sendErrorResponse(_systemPath,context, response);
        }
        else {

            ddx.getRootElement().setAttribute("dataset_id",resourceID);

            String currentDir = System.getProperty("user.dir");
            log.debug("Cached working directory: "+currentDir);


            String xslDir = _systemPath + "/nciso/xsl";


            log.debug("Changing working directory to "+ xslDir);
            System.setProperty("user.dir",xslDir);

            String xsltDocName = "OPeNDAPDDCount-HTML.xsl";


            // This Transformer class is an attempt at making the use of the saxon-9 API
            // a little simpler to use. It makes it easy to set input parameters for the stylesheet.
            // See the source code for opendap.xml.Transformer for more.
            Transformer transformer = new Transformer(xsltDocName);


            transformer.setParameter("docsService",oreq.getDocsServiceLocalID());
            transformer.setParameter("HyraxVersion",Version.getHyraxVersionString());

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform( new JDOMSource(ddx),os);




            os.flush();
            log.info("Sent {}",getServiceTitle());
            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);
        }



    }



}
