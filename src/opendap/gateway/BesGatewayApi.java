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

package opendap.gateway;

import opendap.bes.BESError;
import opendap.bes.BESResource;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesApi;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Util;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.logging.ServletLogUtil;
import opendap.namespaces.BES;
import opendap.ppt.PPTException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 11/28/11
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class BesGatewayApi extends BesApi implements Cloneable {


    private Logger log;
    private String _servicePrefix;

    public BesGatewayApi() {
        this("");
    }

    public BesGatewayApi(String servicePrefix) {
        super();
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        _servicePrefix = servicePrefix;
    }

    /**
     * This child class of opendap.bes.BesXmlAPI provides an implementation of the
     * getRequestDocument method that utilizes the BES wcs_gateway_module.
     * @param type The type of thing being requested. For example a DDX would be
     * opendap.bes.BesXmlAPI.DDX
     * @param remoteDataSourceUrl See opendap.bes.BesXmlAPI.DDX
     * @param ce See opendap.bes.BesXmlAPI
     * @param xmlBase See opendap.bes.BesXmlAPI
     * @param formURL See opendap.bes.BesXmlAPI
     * @param returnAs See opendap.bes.BesXmlAPI
     * @param errorContext See opendap.bes.BesXmlAPI
     * @return The request Document
     * @throws opendap.bes.BadConfigurationException When the bad things happen.
     *
     *
     *
     *
     *
     *     public  Document getRequestDocument(String type,
                                                String dataSource,
                                                String ce,
                                                String xdap_accept,
                                                int maxResponseSize,
                                                String xmlBase,
                                                String formURL,
                                                String returnAs,
                                                String errorContext)
                throws BadConfigurationException {

     *
     *
     *
     *
     * @see BesApi
     */
    @Override
    public Document getDap2RequestDocumentAsync(User user,
                                                String type,
                                           String remoteDataSourceUrl,
                                           String ce,
                                           String async,
                                           String storeResult,
                                           String xmlBase,
                                           String formURL,
                                           String returnAs,
                                           String errorContext)
                throws BadConfigurationException {


        log.debug("Building request for BES gateway_module request. remoteDataSourceUrl: "+ remoteDataSourceUrl);
        Element e, request = new Element("request", BES.BES_NS);

        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":gateway_request]";
        request.setAttribute("reqID",reqID);


        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        String logEntryForBes = ServletLogUtil.getLogEntryForBesLog();
        if(!logEntryForBes.isEmpty())
            request.addContent(setContextElement(OLFS_LOG_CONTEXT,logEntryForBes));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(user.getMaxResponseSize()>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,user.getMaxResponseSize()+""));

        if(user.getMaxVariableSize()>=0)
            request.addContent(setContextElement(MAX_VARIABLE_SIZE_CONTEXT,user.getMaxVariableSize()+""));



        request.addContent(setContainerElement("gatewayContainer","gateway",remoteDataSourceUrl,type));

        Element def = defineElement("d1","default");
        e = (containerElement("gatewayContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(dap2ConstraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs);

        request.addContent(e);

        log.debug("Built request for BES gateway_module.");


        return new Document(request);

    }




    @Override
    public  Document getDap4RequestDocument(User user,
                                            String type,
                                            String remoteDataSourceUrl,
                                            QueryParameters qp,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
            throws BadConfigurationException {


        log.debug("getDap4RequestDocument() - Building request for BES gateway_module request. remoteDataSourceUrl: {}",remoteDataSourceUrl);
        Element e, request = new Element("request", BES.BES_NS);

        //String besDataSource = getBES(dataSource).trimPrefix(dataSource);

        String reqID = Thread.currentThread().getName()+":"+ Thread.currentThread().getId();


        request.setAttribute("reqID",reqID);

        /**----------------------------------------------------------------------
         * Added this bit for the cloudy dap experiment - ndp 1/19/17
         */
        String cloudyDap = qp.getCloudyDap();
        if(cloudyDap!=null){
            request.addContent(setContextElement(CLOUDY_DAP_CONTEXT,cloudyDap));
        }
        /**----------------------------------------------------------------------*/

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        String logEntryForBes = ServletLogUtil.getLogEntryForBesLog();
        if(!logEntryForBes.isEmpty())
            request.addContent(setContextElement(OLFS_LOG_CONTEXT,logEntryForBes));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(user.getMaxResponseSize()>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,user.getMaxResponseSize()+""));

        if(user.getMaxVariableSize()>=0)
            request.addContent(setContextElement(MAX_VARIABLE_SIZE_CONTEXT,user.getMaxVariableSize()+""));


        request.addContent(setContainerElement("gatewayContainer","gateway",remoteDataSourceUrl,type));

        Element def = defineElement("d1","default");
        e = (containerElement("gatewayContainer"));

        if(qp.getCe()!=null && !qp.getCe().equals(""))
            e.addContent(dap4ConstraintElement(qp.getCe()));

        if(qp.getFunc()!=null && !qp.getFunc().equals(""))
            e.addContent(dap4FunctionElement(qp.getFunc()));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs,qp.getAsync(),qp.getStoreResultRequestServiceUrl());

        request.addContent(e);

        log.debug("getDap4RequestDocument() - Built request for BES gateway_module.");

        return new Document(request);

    }


    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }









    /**
     * This method defines which "space" (aka catalog) the BES will use to service the request. Here
     * we override the parent class which uses the "space" called "catalog" to use the "space" called "gateway".
     * This is what causes the BES to invoke the gateway handler
     *
     * @return
     */
    @Override
    protected String getBesSpaceName() {
        return "gateway";
    }

    /**
     * This defines the name of the container built by the BES. It's name matters not, it's really an ID, but to keep
     * the BES commands readable and consistent we typically associate it with the "space" name.
     *
     * @return The name of the BES "container" which will be built into teh request document.
     */
    @Override
    protected String getBesContainerName() {
        return "gatewayContainer";
    }



    private String getDataSourceUrl(HttpServletRequest req, String pathPrefix) {

        String relativeURL = ReqInfo.getLocalUrl(req);

        return getRemoteDataSourceUrl(relativeURL, pathPrefix, Pattern.compile(MATCH_LAST_DOT_SUFFIX_REGEX_STRING));

    }


    public String getRemoteDataSourceUrl(String relativeURL, String pathPrefix, Pattern suffixMatchPattern) {

        // Strip leading slash(es)
        while (relativeURL.startsWith("/") && !relativeURL.equals("/"))
            relativeURL = relativeURL.substring(1, relativeURL.length());

        String dataSourceUrl = relativeURL;

        // Strip the path off.
        if (pathPrefix != null && dataSourceUrl.startsWith(pathPrefix))
            dataSourceUrl = dataSourceUrl.substring(pathPrefix.length());

        if (!dataSourceUrl.equals("")) {
            dataSourceUrl = Util.dropSuffixFrom(dataSourceUrl, suffixMatchPattern);
        }
        dataSourceUrl = HexAsciiEncoder.hexToString(dataSourceUrl);

        return dataSourceUrl;
    }


    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDX is being requested
     * @param xmlBase The request URL.
     * @param contentID contentID of the first MIME part.
     * @param mimeBoundary The MIME boundary to use in the response..
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    @Override
    public Document getDap4DataRequest(User user,
                                       String dataSource,
                                       QueryParameters qp,
                                       String xmlBase,
                                       String contentID,
                                       String mimeBoundary)
            throws BadConfigurationException {

        Document reqDoc = getDap4RequestDocument(user, DAP4_DATA, dataSource, qp, xmlBase, null, null, XML_ERRORS);

        Element req = reqDoc.getRootElement();
        if(req==null)
            throw new BadConfigurationException("Request document is corrupt! Missing root element!");

        Element getReq = req.getChild("get",BES.BES_NS);
        if(getReq==null)
            throw new BadConfigurationException("Request document is corrupt! Missing 'get' element!");

        Element e = new Element("contentStartId",BES.BES_NS);
        e.setText(contentID);
        getReq.addContent(e);

        e = new Element("mimeBoundary",BES.BES_NS);
        e.setText(mimeBoundary);
        getReq.addContent(e);

        return reqDoc;

    }

    /*

    @Override
    public boolean getInfo(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {


        String besDataSourceId = getBesDataSourceID(dataSource);

        return super.getInfo(besDataSourceId, response);

    }
    */


    String stripPrefix(String dataSource){


        while(dataSource.startsWith("/") && !dataSource.equals("/"))
            dataSource = dataSource.substring(1,dataSource.length());


        if(dataSource.startsWith(_servicePrefix))
            return dataSource.substring(_servicePrefix.length(),dataSource.length());

        return dataSource;

    }

    /**
     * Because the gateway doesn't support a catalog we ignore the checkWithBes parameter
     *
     * @param relativeUrl        The relative URL of the client request. No Constraint expression (i.e. No query section of
     *                           the URL - the question mark and everything after it.)
     * @param suffixMatchPattern This parameter provides the method with a suffix regex to use in evaluating what part,
     *                           if any, of the relative URL must be removed to construct the besDataSourceId.
     * @param checkWithBes       This boolean value instructs the code to ask the appropriate BES if the resulting
     *                           besDataSourceID is does in fact represent a valid data source in it's world. Because the BES gateway_module
     *                           doesn't have catalog services this parameter is ignored.
     * @return
     */
    @Override
    public String getBesDataSourceID(String relativeUrl, Pattern suffixMatchPattern, boolean checkWithBes) {
        log.debug("getBesDataSourceID() - relativeUrl: " + relativeUrl);
        if (Util.matchesSuffixPattern(relativeUrl, suffixMatchPattern)) {
            try {

                String remoteDatasourceUrl = getRemoteDataSourceUrl(relativeUrl, _servicePrefix, suffixMatchPattern);

                log.debug("getBesDataSourceID() - besDataSourceId: {}", remoteDatasourceUrl);
                return remoteDatasourceUrl;
            } catch (NumberFormatException e) {
                log.debug("getBesDataSourceID() - Failed to extract target dataset URL from relative URL '{}'", relativeUrl);
            }
        }

        return null;


    }

    @Override
    public void getBesNode(String dataSource, Document response)
            throws BadConfigurationException, PPTException, JDOMException, IOException, BESError {

        // Returns a dummied up BesResource object
        getBesNodeDummy(dataSource, response);

        // While this on the other hand runs out on the web and asks for the information
        // directly from the remote service. Not a whitelisted task so not production
        // getBesNodeRemote(dataSource, response);
    }

    public void getBesNodeDummy(String dataSource, Document response) {
        Element rootElement = new Element("response",BES.BES_NS);
        response.setRootElement(rootElement);
        Element showNode = new Element("showNode",BES.BES_NS);
        rootElement.addContent(showNode);
        Element item = new Element("item",BES.BES_NS);
        showNode.addContent(item);

        SimpleDateFormat fdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
        item.setAttribute("isData", "true");
        item.setAttribute("lastModified", fdf.format(new Date()));
        item.setAttribute("name", dataSource);
        item.setAttribute("type", "leaf");
    }



    public void getBesNodeRemote(String dataSourceUrl, Document response) throws IOException {
        // Go get the HEAD for the catalog
        // FIXME: This DOES NOT utilize the whitelist in the BES and this should to be MOVED to the BES
        HttpClient httpClient = new HttpClient();
        HeadMethod headReq = new HeadMethod(dataSourceUrl);

        try {
            int statusCode = httpClient.executeMethod(headReq);

            if (statusCode != HttpStatus.SC_OK) {
                log.error("Unable to HEAD remote resource: " + dataSourceUrl);
                String msg = "OLFS: Unable to access requested resource: " + dataSourceUrl;
                throw new OPeNDAPException(statusCode, msg);
            }

            Header lastModifiedHeader = headReq.getResponseHeader("Last-Modified");
            Date lastModified = new Date();

            if (lastModifiedHeader != null) {
                String lmtString = lastModifiedHeader.getValue();
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                try {
                    lastModified = format.parse(lmtString);
                } catch (ParseException e) {
                    log.warn("Failed to parse last modified time. LMT String: {}, resource URL: {}", lmtString, dataSourceUrl);
                }
            }

            int size = -1;
            Header contentLengthHeader = headReq.getResponseHeader("Content-Length");

            if (contentLengthHeader != null) {
                String sizeStr = contentLengthHeader.getValue();
                try {
                    size = Integer.parseInt(sizeStr);
                } catch (NumberFormatException nfe) {
                    log.warn("Received invalid content length from datasource: {}: ", dataSourceUrl);
                }
            }
            Element catalogElement = getShowNodeResponseDocForDatasetUrl(dataSourceUrl, size, lastModified);
            response.detachRootElement();
            response.setRootElement(catalogElement);

        }

        catch (Exception e) {
            StringBuilder s = new StringBuilder();
            s.append("Unable to HEAD the remote resource: '").append(dataSourceUrl).append("' ");
            s.append("Caught ").append(e.getClass().getName()).append("  Error Msg: ").append(e.getMessage());
            throw new IOException(s.toString(), e);
        }
    }

    public Element getShowNodeResponseDocForDatasetUrl(String dataSourceURL, int size, Date lastModified) throws IOException {

        Element root = new Element("response",BES.BES_NS);
        root.addNamespaceDeclaration(BES.BES_NS);
        root.setAttribute("reqID","BesGatewayApi_Construct");
        Element showCatalog = new Element("showNode",BES.BES_NS);
        root.addContent(showCatalog);

        if(dataSourceURL!=null && dataSourceURL.length()>0){
            Element item = new Element("item",BES.BES_NS);
            showCatalog.addContent(item);
            item.setAttribute("name",dataSourceURL);
            item.setAttribute("size",""+size);

            SimpleDateFormat sdf = new SimpleDateFormat(BESResource.BESDateFormat);
            item.setAttribute("lastModified",sdf.format(lastModified));
            item.setAttribute("isData", "true");
            item.setAttribute("type", "leaf");
        }
        return root;
    }
}
