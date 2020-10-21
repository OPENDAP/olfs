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

package opendap.ngap;

import opendap.auth.EarthDataLoginAccessToken;
import opendap.auth.UserProfile;
import opendap.bes.BESError;
import opendap.bes.BESResource;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Util;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.logging.LogUtil;
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
 * User: dan
 * Date: 2/13/20
 * Time: 1:47 PM
 * Cloned from: opendap.gateway
 * To change this template use File | Settings | File Templates.
 */
public class NgapBesApi extends BesApi implements Cloneable {

    public static final String EDL_AUTH_TOKEN_CONTEXT = "edl_auth_token";
    public static final String EDL_ECHO_TOKEN_CONTEXT = "edl_echo_token";

    private Logger log;
    private String _servicePrefix;

    public NgapBesApi() {
        this("");
    }

    public NgapBesApi(String servicePrefix) {
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
     * @see opendap.bes.dap2Responders.BesApi
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


        log.debug("Building request for BES ngap_module request. remoteDataSourceUrl: "+ remoteDataSourceUrl);
        Element e, request = new Element("request", BES.BES_NS);

        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":ngap_request]";
        request.setAttribute("reqID",reqID);

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        String logEntryForBes = LogUtil.getLogEntryForBesLog();
        if(!logEntryForBes.isEmpty())
            request.addContent(setContextElement(OLFS_LOG_CONTEXT,logEntryForBes));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(user.getMaxResponseSize()>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,user.getMaxResponseSize()+""));

        addEdlAuthToken(request,user);

        request.addContent(setContainerElement("ngapContainer","ngap",remoteDataSourceUrl,type));

        Element def = defineElement("d1","default");
        e = (containerElement("ngapContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs);

        request.addContent(e);

        log.debug("Built request for BES ngap_module.");


        return new Document(request);

    }


    /**
     * Adds the user id and/or the associated EDL auth token to the request
     * element. If either parameter is the empty string it is omitted.
     *
     * Constructs the EDL/URS Echo-Token and Authorization headers for use
     * when connecting to NGAP infrstructure (like cumulus and CMR) The
     * Echo-Token is made from the
     * EDL access_token returned for the user and the server's EDL Application
     * Client-Id.
     *
     *    Echo-Token: µedl_access_token:Client-Id
     *
     * The Authorization header is made of the sting:
     *
     *    Authorization: Bearer edl_access_token
     *
     * From a bes command:
     *   <bes:setContext name="uid">ndp_opendap</bes:setContext>
     *   <bes:setContext name="edl_echo_token">anecho:tokenvalue</bes:setContext>
     *    <bes:setContext name="edl_auth_token">Bearer Abearertokenvalue</bes:setContext>
     *
     * @param request The BES request in which to set the UID_CONTEXT and
     *                EDL_AUTH_TOKEN_CONTEXT from the user object.
     * @param user The instance of User from which to get the uid, the
     *             auth_token, and the EDL Application Client-Id..
     */
    public static void addEdlAuthToken(Element request, User user) {
        UserProfile up = user.profile();
        if (up != null) {
            request.addContent(setContextElement(UID_CONTEXT,user.getUID()==null?"not_logged_in":user.getUID()));

            EarthDataLoginAccessToken oat = up.getEDLAccessToken();
            if (oat != null) {

                // Make and add the @deprecated Echo-Token value
                request.addContent(setContextElement(EDL_ECHO_TOKEN_CONTEXT, oat.getEchoTokenValue()));

                // Add the new service chaining Authorization header value
                request.addContent(setContextElement(EDL_AUTH_TOKEN_CONTEXT, oat.getAuthorizationHeaderValue()));
            }
        }

    }


    /**
     *
     * @param user
     * @param type
     * @param remoteDataSourceUrl
     * @param qp
     * @param xmlBase
     * @param formURL
     * @param returnAs
     * @param errorContext
     * @return
     * @throws BadConfigurationException
     */
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


        log.debug("getDap4RequestDocument() - Building request for BES ngap_module request. remoteDataSourceUrl: {}",remoteDataSourceUrl);
        Element e, request = new Element("request", BES.BES_NS);

        //String besDataSource = getBES(dataSource).trimPrefix(dataSource);

        String reqID = Thread.currentThread().getName()+":"+ Thread.currentThread().getId();


        request.setAttribute("reqID",reqID);

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        String logEntryForBes = LogUtil.getLogEntryForBesLog();
        if(!logEntryForBes.isEmpty())
            request.addContent(setContextElement(OLFS_LOG_CONTEXT,logEntryForBes));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(user.getMaxResponseSize()>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,user.getMaxResponseSize()+""));

        addEdlAuthToken(request,user);

        request.addContent(setContainerElement("ngapContainer","ngap",remoteDataSourceUrl,type));

        Element def = defineElement("d1","default");
        e = (containerElement("ngapContainer"));

        if(qp.getCe()!=null && !qp.getCe().equals(""))
            e.addContent(dap4ConstraintElement(qp.getCe()));

        if(qp.getFunc()!=null && !qp.getFunc().equals(""))
            e.addContent(dap4FunctionElement(qp.getFunc()));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs,qp.getAsync(),qp.getStoreResultRequestServiceUrl());

        request.addContent(e);

        log.debug("getDap4RequestDocument() - Built request for BES ngap_module.");

        return new Document(request);

    }


    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    /**
     * This method defines which "space" (aka catalog) the BES will use to service the request. Here
     * we override the parent class which uses the "space" called "catalog" to use the "space" called "ngap".
     * This is what causes the BES to invoke the ngap handler
     *
     * @return
     */
    @Override
    protected String getBesSpaceName() {
        return "ngap";
    }

    /**
     * This defines the name of the container built by the BES. It's name matters not, it's really an ID, but to keep
     * the BES commands readable and consistent we typically associate it with the "space" name.
     *
     * @return The name of the BES "container" which will be built into teh request document.
     */
    @Override
    protected String getBesContainerName() {
        return "ngapContainer";
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
        //dataSourceUrl = opendap.gateway.HexAsciiEncoder.hexToString(dataSourceUrl);

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

        Document reqDoc = getDap4RequestDocument(user, DataDDX, dataSource, qp, xmlBase, null, null, XML_ERRORS);

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
     * Because the ngap-service doesn't support a catalog we ignore the checkWithBes parameter
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
        root.setAttribute("reqID","NGAP_BesApi_Construct");
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
