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
package opendap.nciso;

import opendap.bes.BESError;
import opendap.bes.BESResource;
import opendap.bes.Version;
import opendap.bes.dap2Responders.BesApi;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.http.mediaTypes.TextHtml;
import opendap.http.mediaTypes.TextXml;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * IsoDispatchHandler for Metadata Rubric responses from Hyrax
 *
 * @deprecated
 */
public class RubricDispatchHandler implements opendap.coreServlet.DispatchHandler {



    private Logger log;
    private boolean initialized;

    private String _systemPath;

    private String rubricRequestPatternRegexString;
    private Pattern rubricRequestPattern;

    private Element _config;

    private BesApi _besApi;


    public RubricDispatchHandler(){
        log = LoggerFactory.getLogger(getClass());
    }



    public void init(HttpServlet servlet,Element config) throws Exception {
        init(servlet,config,new BesApi());

    }
    public void init(HttpServlet servlet,Element config, BesApi besApi) throws Exception {

        if(initialized) return;

        _config = config;
        _systemPath = ServletUtil.getSystemPath(servlet,"");
        rubricRequestPatternRegexString = ".*\\.rubric";
        rubricRequestPattern = Pattern.compile(rubricRequestPatternRegexString, Pattern.CASE_INSENSITIVE);
        _besApi = besApi;
        initialized = true;

    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return rubricDispatch(request, null, false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

       if(!rubricDispatch(request, response, true))
           log.debug("FileDispatch request failed inexplicably!");

    }


    /**
     * See the contract for this method in opendap.coreServlet.IsoDispatchHandler
     * @param req The request for which we need to get a last modified date.
     * @return
     */
    public long getLastModified(HttpServletRequest req) {

        String name = ReqInfo.getLocalUrl(req);

        log.debug("Locating LMT for collection: {}", name );


        try {
            ResourceInfo dsi = new BESResource(name,_besApi);
            log.debug("Returning: {}" + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            log.debug("Returning current date/time.");
            return new Date().getTime();
        }


    }



    public void destroy() {
        log.info("Destroy complete.");

    }

    /**
     * Performs dispatching for rubric requests. ]
     *
     * @param request      The HttpServletRequest for this transaction.
     * @param response     The HttpServletResponse for this transaction
     * @param sendResponse If this is true a response will be sent. If it is
     *                     the request will only be evaluated to determine if a response can be
     *                     generated.
     * @return true if the request was serviced as a file request, false
     *         otherwise.
     * @throws Exception .
     */
    private boolean rubricDispatch(HttpServletRequest request,
                               HttpServletResponse response,
                               boolean sendResponse) throws Exception {


        String requestURL = request.getRequestURL().toString();

        boolean isrubricResponse = false;

        if(rubricRequestPattern.matcher(requestURL).matches())   {
            String relativeUrl = ReqInfo.getLocalUrl(request);
            String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
            ResourceInfo dsi = new BESResource(dataSource,_besApi);

            if (dsi.sourceExists() && dsi.isDataset()) {
                isrubricResponse = true;
                if (sendResponse) {
                    sendrubricResponse(request,response);
                }
            }

        }

        return isrubricResponse;

    }


    /**
     * This method is responsible for sending rubric metadata responses to the client.
     * @param request
     * @param response
     * @throws Exception
     */
    private void sendrubricResponse(HttpServletRequest request,
                         HttpServletResponse response)
            throws Exception {


        Request oreq = new Request(null,request);
        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSourceId = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        String xmlBase = request.getRequestURL().toString();
        int suffix_start = xmlBase.lastIndexOf("." + requestSuffix);
        xmlBase = xmlBase.substring(0, suffix_start);

        log.debug("Sending rubric Response() for dataset: " + dataSourceId);

        MediaType responseMediaType = new TextXml();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());

        Version.setOpendapMimeHeaders(request, response);
        response.setHeader("Content-Description", "ISO 19115 Metadata Compliance Score");

        ServletOutputStream os = response.getOutputStream();

        // Doing this insures that the DDX that
        String xdap_accept = "3.2";

        Document ddx = new Document();

        _besApi.getDDXDocument(
                dataSourceId,
                constraintExpression,
                xdap_accept,
                xmlBase,
                ddx);

        ddx.getRootElement().setAttribute("dataset_id",dataSourceId);

        String currentDir = System.getProperty("user.dir");
        log.debug("Cached working directory: "+currentDir);

        String xslDir = _systemPath + "/nciso/xsl";

        log.debug("Changing working directory to "+ xslDir);
        System.setProperty("user.dir",xslDir);

        try {
            String xsltDocName = "OPeNDAPDDCount-HTML.xsl";

            // This Transformer class is an attempt at making the use of the saxon-9 API
            // a little simpler to use. It makes it easy to set input parameters for the stylesheet.
            // See the source code for opendap.xml.Transformer for more.
            Transformer transformer = new Transformer(xsltDocName);
            transformer.setParameter("docsService", oreq.getDocsServiceLocalID());
            transformer.setParameter("HyraxVersion", Version.getHyraxVersionString());

            // Transform the BES  showCatalog response into a HTML page for the browser
            transformer.transform(new JDOMSource(ddx), os);
            os.flush();
            log.info("Sent Rubric version of DDX.");
        }
        finally {
            log.debug("Restoring working directory to "+ currentDir);
            System.setProperty("user.dir",currentDir);
        }

    }


}
