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

import opendap.bes.BESResource;
import opendap.bes.BadConfigurationException;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Util;
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
public class BesGatewayApi extends BesApi {


    private Logger log;
    private String _servicePrefix;

    public BesGatewayApi(){
        this("");
    }

    public BesGatewayApi(String servicePrefix){
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
     * @param xdap_accept See opendap.bes.BesXmlAPI
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
     * @see opendap.bes.dapResponders.BesApi
     */
    @Override
    public Document getRequestDocument(String type,
                                                String remoteDataSourceUrl,
                                                String ce,
                                                String xdap_accept,
                                                int maxResponseSize,
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


        if(xdap_accept!=null)
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT,xdap_accept));
        else
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(maxResponseSize>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,maxResponseSize+""));


        request.addContent(setContainerElement("gatewayContainer","gateway",remoteDataSourceUrl,type));

        Element def = defineElement("d1","default");
        e = (containerElement("gatewayContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs);

        request.addContent(e);

        log.debug("Built request for BES gateway_module.");


        return new Document(request);

    }

    private String getDataSourceUrl(HttpServletRequest req, String pathPrefix)  {


        String relativeURL = ReqInfo.getLocalUrl(req);

        return getRemoteDataSourceUrl(relativeURL, pathPrefix, Pattern.compile(_regexToMatchLastDotSuffixString));


    }

    public String getRemoteDataSourceUrl(String relativeURL, String pathPrefix, Pattern suffixMatchPattern )  {



        //String requestSuffix = ReqInfo.getSuffix(relativeURL);



        // Strip leading slash(es)
        while(relativeURL.startsWith("/") && !relativeURL.equals("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        String dataSourceUrl = relativeURL;


        // Strip the path off.
        if(pathPrefix!=null && dataSourceUrl.startsWith(pathPrefix))
            dataSourceUrl = dataSourceUrl.substring(pathPrefix.length());


        if(!dataSourceUrl.equals("")){
            dataSourceUrl = Util.dropSuffixFrom(dataSourceUrl, suffixMatchPattern);
        }


        dataSourceUrl = HexAsciiEncoder.hexToString(dataSourceUrl);

//        URL url = new URL(dataSourceUrl);
        //log.debug(urlInfo(url));

        return dataSourceUrl;


    }


    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDX is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the dap that should be used to build the
     * response.
     * @param xmlBase The request URL.
     * @param contentID contentID of the first MIME part.
     * @param mimeBoundary The MIME boundary to use in the response..
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataRequest(String dataSource,
                                       String ce,
                                       String xdap_accept,
                                       int maxResponseSize,
                                       String xmlBase,
                                       String contentID,
                                       String mimeBoundary)
            throws BadConfigurationException {

        Document reqDoc = getRequestDocument(DataDDX,dataSource,ce,xdap_accept,maxResponseSize,xmlBase,null,null,XML_ERRORS);

        Element req = reqDoc.getRootElement();

        Element getReq = req.getChild("get",BES.BES_NS);

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
     * @param relativeUrl The relative URL of the client request. No Constraint expression (i.e. No query section of
     * the URL - the question mark and everything after it.)
     * @param suffixMatchPattern This parameter provides the method with a suffix regex to use in evaluating what part,
     * if any, of the relative URL must be removed to construct the besDataSourceId/
     * @param checkWithBes This boolean value instructs the code to ask the appropriate BES if the resulting
     * besDataSourceID is does in fact represent a valid data source in it's world. Because the BES gateway_module
     * doesn't have catalog services this parameter is ignored.
     * @return
     */
    @Override
    public String getBesDataSourceID(String relativeUrl, Pattern suffixMatchPattern, boolean checkWithBes){

        log.debug("getBesDataSourceID() - relativeUrl: " + relativeUrl);


        if(Util.matchesSuffixPattern(relativeUrl,suffixMatchPattern)){
            try {

                String remoteDatasourceUrl = getRemoteDataSourceUrl(relativeUrl, _servicePrefix, suffixMatchPattern);

                log.debug("getBesDataSourceID() - besDataSourceId: {}", remoteDatasourceUrl);
                return remoteDatasourceUrl;
            }
            catch (NumberFormatException e){
                log.debug("getBesDataSourceID() - Failed to extract target dataset URL from relative URL '{}'", relativeUrl);
            }
        }

        return null;


    }



    @Override
    public boolean getCatalog(String dataSourceUrl, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {


        // Go get the HEAD for the catalog:
        HttpClient httpClient = new HttpClient();
        HeadMethod headReq = new HeadMethod(dataSourceUrl);

        try {
            int statusCode = httpClient.executeMethod(headReq);

            if (statusCode != HttpStatus.SC_OK) {
                log.error("Unable to HEAD s3 object: " + dataSourceUrl);
                org.opendap.dap4.Error error = new org.opendap.dap4.Error();
                error.setMessage("OLFS: Unable to access requested resource.");
                error.setContext(dataSourceUrl);
                error.setHttpCode(statusCode);

                return false;
            }

            Header lastModifiedHeader = headReq.getResponseHeader("Last-Modified");
            if (lastModifiedHeader == null) {
                return false;
            }
            String lmtString = lastModifiedHeader.getValue();
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            Date lastModified = format.parse(lmtString);

            int size = -1;
            Header contentLengthHeader =  headReq.getResponseHeader("Content-Length");

            if(contentLengthHeader!=null){
                String sizeStr = contentLengthHeader.getValue();
                try {
                    size = Integer.parseInt(sizeStr);
                }
                catch (NumberFormatException nfe){
                    log.warn("Received invalid content length from datasource: {}: ",dataSourceUrl);
                }
            }

            Element catalogElement = getShowCatalogResponseDocForDatasetUrl(dataSourceUrl,size,lastModified);


            response.detachRootElement();
            response.setRootElement(catalogElement);

            return true;

        } catch (Exception e) {
            log.error("Unable to HEAD the s3 resource: {} Error Msg: {}", dataSourceUrl, e.getMessage());
        }


        return false;


    }



    public Element getShowCatalogResponseDocForDatasetUrl(String dataSourceURL, int size, Date lastModified){



        Element root = new Element("response",BES.BES_NS);
        root.addNamespaceDeclaration(BES.BES_NS);
        root.setAttribute("reqID","BesGatewayApi_Construct");


        Element showCatalog = new Element("showCatalog",BES.BES_NS);
        root.addContent(showCatalog);


        Element dataset = new Element("dataset",BES.BES_NS);
        showCatalog.addContent(dataset);

        dataset.setAttribute("name",dataSourceURL);
        dataset.setAttribute("size",""+size);

        SimpleDateFormat sdf = new SimpleDateFormat(BESResource.BESDateFormat);

        dataset.setAttribute("lastModified",sdf.format(lastModified));


        dataset.setAttribute("node","false");



        Element serviceRef = new Element("serviceRef",BES.BES_NS);
        serviceRef.setText("dap");

        dataset.addContent(serviceRef);


        return root;
    }


}
