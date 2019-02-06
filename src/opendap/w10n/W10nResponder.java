/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.w10n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.sf.saxon.s9api.SaxonApiException;
import opendap.PathBuilder;
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesDapDispatcher;
import opendap.bes.Version;
import opendap.bes.dap2Responders.BesApi;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.dap.User;
import opendap.http.error.*;
import opendap.http.mediaTypes.*;
import opendap.namespaces.BES;
import opendap.ppt.PPTException;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;
import org.owasp.encoder.Encode;

/**
 * Handles w10n services. Performs  HTTP server/client content negotiation, evaluates w10n requests
 * and formulates BES commands to produce the desired results.
 *
 * Created by ndp on 1/22/15.
 */
public class W10nResponder {

    private Logger _log;

    private static final String DAP2_TYPE = "dap.2";

    private static final String dapLastModified ="lastModified";
    private static final String nodeLastModified ="lastModified";
    private static final String nodeCount ="count";
    private static final String nodeItem ="item";

    private static final String keyLastModified ="last-modified";
    private static final String keyAttributes ="attributes";
    private static final String keySize ="size";
    private static final String keyName ="name";
    private static final String keyIsData ="isData";
    private static final String keyIsDir ="isDir";
    private static final String keyIsFile ="isFile";

    private TreeMap<String,MediaType> _supportedMetaMediaTypes;
    private TreeMap<String,MediaType> _supportedDataMediaTypes;

    private MediaType _defaultMetaMediaType;
    private MediaType _defaultDataMediaType;

    private BesApi _besApi;

    private String _systemPath;



    public W10nResponder(String systemPath){
        super();

        _log = LoggerFactory.getLogger(this.getClass());

        _systemPath = systemPath;

        _besApi = new BesApi();

        MediaType mt;

        mt = new Json();
        _defaultMetaMediaType = mt;

        _supportedMetaMediaTypes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        mt = new TextHtml();
        _supportedMetaMediaTypes.put(mt.getName(), mt);

        mt = new Json();
        _defaultDataMediaType = mt;

        _supportedDataMediaTypes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        mt = new Dap2Data();
        _supportedDataMediaTypes.put(mt.getName(), mt);

        mt = new Netcdf3();
        _supportedDataMediaTypes.put(mt.getName(), mt);

        mt = new Netcdf4();
        _supportedDataMediaTypes.put(mt.getName(), mt);
    }


    /**
     * Sets the response headers based on the passed media type and incoming request. The request is need to
     * discover which BES is servicing the request so that it's version information can be included in the
     * headers.
     * @param request Clients request.
     * @param requestedResourceId   The thing they asked for.
     * @param mt The MediaType of the outgoing response.
     * @param response The response in which the headers will be set.
     * @throws JDOMException
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws BESError
     * @throws IOException
     */
    private void setResponseHeaders(HttpServletRequest request, String requestedResourceId, MediaType mt, HttpServletResponse response) throws JDOMException, BadConfigurationException, PPTException, BESError, IOException {

        response.setContentType(mt.getMimeType());
        response.setHeader("Content-Description",mt.getMimeType());
        Version.setOpendapMimeHeaders(request, response);


        // If they aren't asking for html then set the Content-Disposition header which will trigger a browser
        // to download the response and save it to a file.
        if(!mt.getName().equalsIgnoreCase(TextHtml.NAME) && !mt.getName().equalsIgnoreCase(Json.NAME)) {
            while(requestedResourceId.endsWith("/") && requestedResourceId.length()>=0){
                requestedResourceId = requestedResourceId.substring(0,requestedResourceId.lastIndexOf("/"));
            }

            String downloadFileName = getDownloadFileName(requestedResourceId);
            String suffix = "";
            int lastIndexOfDot = downloadFileName.lastIndexOf(".");
            if(lastIndexOfDot>=0){
                suffix = downloadFileName.substring(downloadFileName.lastIndexOf("."));
                while(suffix.startsWith(".") && suffix.length() >0)
                    suffix = suffix.substring(1);
            }
            if(!suffix.equalsIgnoreCase(mt.getName()))
                downloadFileName += "." + mt.getName();

            downloadFileName = Scrub.fileName(downloadFileName);
            String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";
            response.setHeader("Content-Disposition", contentDisposition);
        }




    }


    /**
     * Handles all w10n service activity.
     * @param request Incoming client request
     * @param response Outbound response.
     * @throws JDOMException
     * @throws OPeNDAPException
     * @throws IOException
     * @throws SaxonApiException
     */
    public void send_w10n_response(HttpServletRequest request, HttpServletResponse response) throws JDOMException, OPeNDAPException, IOException, SaxonApiException {
        _log.debug("send_w10n_response() - BEGIN");

        W10nRequest w10nRequest = new W10nRequest(request);
        User user = new User(request);

        /**
         * This section asks the BES to evaluate the requested resource and return a report that indicates what
         * part (if any) can be mapped to an actual thing in the BES catalog and what part (if any) cannot.
         * The BES also supplies information regarding the matching part (aka validResourcePath) such is
         * if it's a file, a directory, and if the BES thinks it's a dataset.
         *
         */
        Document pathInfoDoc =  new Document();
        _besApi.getPathInfoDocument(w10nRequest.getRequestedResourceId(), pathInfoDoc);
        if(_log.isDebugEnabled()) {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            _log.debug("send_w10n_response() - getPathInfo response: \n {}", xmlo.outputString(pathInfoDoc));
        }

        Element besResponse = pathInfoDoc.getRootElement();
        if(besResponse==null)
            throw new IOException("BES failed to include a root element in the PathInfoDocument.");

        Element showW10nPathInfo =  besResponse.getChild("showW10nPathInfo", BES.BES_NS);
        if(showW10nPathInfo==null)
            throw new IOException("BES failed to include a 'showW10nPathInfo' element in the PathInfoDocument.");

        Element w10nPathInfo =  showW10nPathInfo.getChild("W10nPathInfo", BES.BES_NS);
        if(w10nPathInfo==null)
            throw new IOException("BES failed to include a 'W10nPathInfo' element in the PathInfoDocument.");

        Element vpE =  w10nPathInfo.getChild("validPath", BES.BES_NS);
        if(vpE==null)
            throw new IOException("BES failed to include a 'validPath' element in the PathInfoDocument.");

        Element remE =  w10nPathInfo.getChild("remainder", BES.BES_NS);
        if(remE==null)
            throw new IOException("BES failed to include a 'remainder' element in the PathInfoDocument.");

        String validResourcePath = vpE.getTextTrim();
        boolean isData   = vpE.getAttributeValue(keyIsData).equalsIgnoreCase("true");
        boolean isDir    = vpE.getAttributeValue(keyIsDir).equalsIgnoreCase("true");
        boolean isFile   = vpE.getAttributeValue(keyIsFile).equalsIgnoreCase("true");
        String remainder = remE.getTextTrim();

        w10nRequest.setValidResourcePath(validResourcePath);

        /**
         * We know that the resourceId is a proper dataset in the BES, so now we know we need to formulate
         *  a DAP constraint expression from the remainder (if there is one)
         */
        w10nRequest.ingestPathRemainder(remainder);
        // Is this a w10n meta request?
        if(w10nRequest.isMetaRequest()){
            // yup - build a meta response.
            w10nRequest.setBestMediaType(_defaultMetaMediaType, _supportedMetaMediaTypes);
            MediaType responseMediaType = w10nRequest.getBestMediaType();

            // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
            RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);
            setResponseHeaders(request,w10nRequest.getRequestedResourceId(), responseMediaType, response);

            _log.debug("send_w10n_response() - Sending w10n meta response for resource: {} Response type: {}", w10nRequest.getRequestedResourceId(), responseMediaType.getMimeType());
            // First we QC the request
            // Is the thing they asked for some thing the BES sees as a dataset?
            if(isData){
                // OK, the BES thinks it's data, but just to be sure something is not broken,
                // Let's see if it's a file too.
                if(isFile) {
                    // And then we to send the response, using the MediaType to determine what to
                    // send back.
                    sendW10nMetaResponseForDap2Metadata(w10nRequest, user.getMaxResponseSize(), response);
                }
                else {
                    // It's not a file! That's a BAD THING.
                    // It's not possible for a directory to be data.
                    // Since that indicates something serious is busted somewhere: Internal ERROR.
                    throw new opendap.http.error.InternalError("The request for data identifies a resource which is NOT a file. Only files can contain data.");
                }
            }
            else {
                // The BES doesn't think the valid part of the path is data
                //Is there a remainder?
                if(remainder.length()>0){
                    // Dang - given that the BES doesn't see this thing as data
                    // and there is a path remainder we're in a NotFound situation
                    throw new NotFound("The request for data identifies a resource which the server identifies as data.");
                }
                else {
                    // It doesn't matter if it's a directory or a file, we'll get the show catalog response
                    // as an XML document and we'll return a version appropriate to the negotiated media type.
                    sendW10nMetaResponseForFileOrDir(w10nRequest, response);
                }
            }
        }
        else {
            // It's a data request
            if(!isData){
                // The BES doesn't think this thing is data
                if(isDir){
                    // Data request are not valid for nodes (datasets or directories)
                    // So if it's a directory, that's a fail.
                    throw new BadRequest("In w10n data requests are only valid for 'leaves', the requested resource is a node or is not data.");
                }
                // It's not data. But did they try to w10n access it anyway?
                else if(remainder.length()>0){
                    // Yup, but that's a fail so - 400!
                    throw new BadRequest("The requested resource is not a data w10n leaf (data) object.");
                }
                else {
                    // Fine, let's just send them the thang.
                    sendFile(request,response);
                }
            }
            else if(remainder.length()==0 ){
                // The BES thinks this thing is data.
                // BUT, if remainder is empty the user  didn't specify a
                // variable within the dataset. And that's verboten.
                throw new BadRequest("The requested resource does not identify a w10n leaf (data) object and thus cannot be retrieved as data.");
            }
            else {
                w10nRequest.setBestMediaType(_defaultDataMediaType, _supportedDataMediaTypes);
                _log.debug("send_w10n_response() - Sending w10n data response for resource: {} Response type: {}",w10nRequest.getRequestedResourceId(),w10nRequest.getBestMediaType().getMimeType());
                setResponseHeaders(request,w10nRequest.getRequestedResourceId(), w10nRequest.getBestMediaType(), response);
                sendW10nDataResponse(w10nRequest, user.getMaxResponseSize(), response);
            }
        }
        _log.debug("send_w10n_response() - END.");
    }

    /**
     *
     * Transmits the w10n meta response for a catalog directory/collection or file/granule using the correct media type.
     *
     * @param w10nRequest The w10n request to be serviced
     * @param response The outbound response
     * @throws JDOMException
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     * @throws SaxonApiException
     */
    private void sendW10nMetaResponseForFileOrDir(W10nRequest w10nRequest,
                                                  HttpServletResponse response)
            throws OPeNDAPException, JDOMException, IOException, SaxonApiException {

        MediaType mt = w10nRequest.getBestMediaType();
        Document besNode = new Document();
        _besApi.getBesNode(w10nRequest.getValidResourcePath(), besNode);
        if(_log.isDebugEnabled()) {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            _log.debug("sendMetaResponseForFileOrDir() - Catalog from BES: {}\n", xmlo.outputString(besNode));
        }
        if(mt.getName().equalsIgnoreCase(Json.NAME)){
            _log.debug("sendMetaResponseForFileOrDir() - Sending as JSON");
            sendBesNodeAsJson(w10nRequest, besNode, response);
        }
        else if(mt.getName().equalsIgnoreCase(TextHtml.NAME)){
            _log.debug("sendMetaResponseForFileOrDir() - Sending as HTML");
            sendBesNodeAsHtml(w10nRequest, besNode, response);
        }
        else {
            throw  new NotAcceptable("Unsupported response encoding! You have requested an unsupported return type of"+ mt.getMimeType());
        }
    }

    /**
     * Transmits a BES showNode response in a w10n JSON encoding.
     *
     * @param node The showNode response document.
     * @param w10nRequest  The w10n request to be service.
     * @throws JDOMException
     * @throws IOException
     */
    private void sendBesNodeAsJson(W10nRequest w10nRequest, Document node, HttpServletResponse response) throws JDOMException, IOException, BESError {

        Element root = node.getRootElement();
        if(root==null)
            throw new IOException("BES showNode response document is missing the root (response) element!");

        Element showNodeElement = root.getChild("showNode",BES.BES_NS);
        if(showNodeElement==null)
            throw new IOException("BES showNode response document is missing the showNode element!");

        boolean isNode = false;
        Element topElement = showNodeElement.getChild("node",BES.BES_NS);
        if(topElement==null) {
            topElement = showNodeElement.getChild("item",BES.BES_NS);
            if(topElement==null)
                throw new IOException("BES showNode response document is missing the expected 'node' or 'item' element!");
            isNode = isItemW10nNode(topElement);
        }
        else {
            isNode = true;
        }
        String type = getW10nTypeStringForBesNode(topElement);
        HashMap<String,Object> jsonBesCatalogResponse = getJsonForBesItemOrNodeElement(topElement);
        if(isNode) {
            if(topElement.getName().equals("node")) {
                ArrayList<Object> nodes = new ArrayList<>();
                ArrayList<Object> leaves = new ArrayList<>();

                @SuppressWarnings("unchecked")
                List<Element> items = (List<Element>) topElement.getChildren("item", BES.BES_NS);

                for (Element item : items) {
                    HashMap<String, Object> jsonDataset = getJsonForBesItemOrNodeElement(item);

                    if (isItemW10nNode(item)) {
                        nodes.add(jsonDataset);
                    } else {
                        leaves.add(jsonDataset);
                    }
                }

                jsonBesCatalogResponse.put("nodes", nodes);
                jsonBesCatalogResponse.put("leaves", leaves);
                jsonBesCatalogResponse.put("w10n",
                        getW10nMetaObject(
                                type,
                                w10nRequest.getW10nResourcePath(),
                                w10nRequest.getW10nId(),
                                _defaultMetaMediaType,
                                _supportedMetaMediaTypes)
                );
            }
            else {
                jsonBesCatalogResponse.put("w10n",
                        getW10nMetaObject(
                                type,
                                w10nRequest.getW10nResourcePath(),
                                w10nRequest.getW10nId(),
                                _defaultMetaMediaType,
                                _supportedMetaMediaTypes));

            }

        }
        else {
            jsonBesCatalogResponse.put("w10n",
                    getW10nMetaObject(
                            type,
                            w10nRequest.getW10nResourcePath(),
                            w10nRequest.getW10nId(),
                            _defaultMetaMediaType,
                            _supportedMetaMediaTypes));

        }

        ServletOutputStream sos =  response.getOutputStream();
        if(w10nRequest.callback()!=null){
            sos.print(w10nRequest.callback()+"(");
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        sos.print(gson.toJson(jsonBesCatalogResponse));
        if(w10nRequest.callback()!=null){
            sos.print(")");
        }
        sos.println();
    }





    /**
     * Determines is a bes:dataset object is a w10n node.  It must either have child datasets, or
     * the BES must have provided a child serviceRef element whose value is "dap".
     * @param dataset The bes:dataset object
     *
     * @return  True if the dataset is a w10n node, false otherwise.
     */
    private boolean isDatasetW10nNode(Element dataset){

        boolean isNode;
        isNode = dataset.getAttributeValue("node").equalsIgnoreCase("true");
        Element serviceRef = dataset.getChild("serviceRef", BES.BES_NS);
        if(serviceRef!=null &&  serviceRef.getTextTrim().equalsIgnoreCase(BES.DAP_SERVICE_ID)){
            isNode = true;
        }
        return isNode;
    }


    /**
     * Determines if a bes:item object is a w10n node.  It must either have type "node", or
     * it must be type "leaf" and the value of the isData attribute must be "true".
     * @param item The bes:item object
     *
     * @return  True if the dataset is a w10n node, false otherwise.
     */
    private boolean isItemW10nNode(Element item){

        boolean isNode = false;
        String type = item.getAttributeValue("type");
        if(type!=null && type.equals("node")){
            isNode = true;
        }
        else {
            // It's a leaf, but if it's data it's a node
            String isData = item.getAttributeValue(keyIsData);
            if(isData!=null && isData.equals("true"))
                isNode=true;
        }
        return isNode;
    }


    /**
     * Returns a w10n JSON object representation of a bes:dataset element.
     * @param dataset The bes:dataset element
     * @return  The JSON representation (as a HashMap<String,Object>)
     */
    private HashMap<String,Object> getJsonForCatalogDatasetElement(Element dataset){

        String nodeName = dataset.getAttributeValue(keyName);
        String name = nodeName;

        int lastIndexOfSlash = nodeName.lastIndexOf('/');
        if(lastIndexOfSlash > 0) {
            name = nodeName.substring(lastIndexOfSlash);
            while (name.startsWith("/") && name.length() > 1)
                name = name.substring(1);
        }
        long   size = Long.parseLong(dataset.getAttributeValue(keySize));
        String  lmt = dataset.getAttributeValue(dapLastModified);
        HashMap<String,Object> jsonObject = new HashMap<>();
        jsonObject.put(keyName,name);
        ArrayList<Object> attributes = new ArrayList<>();
        attributes.add(getW10nAttribute(keyLastModified,lmt));
        attributes.add(getW10nAttribute(keySize, size));

        jsonObject.put(keyAttributes,attributes);
        return jsonObject;
    }


    /**
     * Returns a w10n JSON object representation of a bes:item element.
     * @param item The bes:item element
     * @return  The JSON representation (as a HashMap<String,Object>)
     */
    private HashMap<String,Object> getJsonForBesItemOrNodeElement(Element item){

        ArrayList<Object> attributes = new ArrayList<>();
        HashMap<String,Object> jsonObject = new HashMap<>();

        if(item.getName().equals(nodeItem)){

            String itemName = item.getAttributeValue(keyName);

            String name = PathBuilder.basename(itemName);
            jsonObject.put(keyName,name);

            long size = -1;
            String s = item.getAttributeValue(keySize);
            if(s!=null) {
                size = Long.parseLong(s);
            }
            attributes.add(getW10nAttribute(keySize, size));

            String  lmt = item.getAttributeValue(nodeLastModified);
            attributes.add(getW10nAttribute(keyLastModified,lmt));

            jsonObject.put(keyAttributes,attributes);

        }
        else {
            // It must be a node.
            Element node = item;
            String nodeName = node.getAttributeValue(keyName);

            String name = PathBuilder.basename(nodeName);
            jsonObject.put(keyName,name);

            long size = -1;
            String s = node.getAttributeValue(nodeCount);
            if(s!=null) {
                size = Long.parseLong(s);
            }
            attributes.add(getW10nAttribute(keySize, size));

            String  lmt = item.getAttributeValue(nodeLastModified);
            attributes.add(getW10nAttribute(keyLastModified,lmt));

            jsonObject.put(keyAttributes,attributes);
        }
        return jsonObject;
    }


    /**
     * Returns the w10n meta object used in all w10n JSON encoded responses
     * @param type The tyt]pe string
     * @param path
     * @param id
     * @param defaultMT
     * @param altMediaTypes
     * @return
     */
    private  ArrayList<Object> getW10nMetaObject(
            String type,
            String path,
            String id,
            MediaType defaultMT,
            Map<String, MediaType> altMediaTypes){

        ArrayList<Object> w10n = new ArrayList<>();
        w10n.add(getW10nAttribute("spec","draft-20091228"));
        w10n.add(getW10nAttribute("application", "Hyrax-"+Version.getHyraxVersionString()));
        w10n.add(getW10nAttribute("type", type));
        w10n.add(getW10nAttribute("path", path));
        w10n.add(getW10nAttribute("identifier", id));
        w10n.add(getW10nAttribute("output", getW10nOutputTypes(defaultMT,altMediaTypes)));
        return w10n;
    }


    /**
     * Constructs list of available output MediaTypes.
     * @param defaultMT default MediaTypes
     * @param altMediaTypes  AlternateMediaType
     * @return List of available output MediaTypes
     */
    private ArrayList<Object> getW10nOutputTypes(MediaType defaultMT, Map<String,MediaType> altMediaTypes ) {
        ArrayList<Object> outputTypes = new ArrayList<>();
        outputTypes.add(getW10nAttribute(defaultMT.getName(), defaultMT.getMimeType()));
        for(MediaType mt : altMediaTypes.values()){
            outputTypes.add(getW10nAttribute(mt.getName(), mt.getMimeType()));
        }
        return outputTypes;
    }


    /**
     * Forms a simple attribute for use in JSON
     * @param name Attribute name
     * @param value  Attribute value
     * @return Attribute as a JSONObject
     */
    private HashMap<String,Object> getW10nAttribute(String name, Object value){
        HashMap<String,Object> w10nAttribute = new HashMap<>();
        w10nAttribute.put("name", name);
        w10nAttribute.put("value", value);
        return w10nAttribute;
    }


    /**
     * Determines the w10n "type" of a BES catalog node element.
     * @param node A bes:node element from a BES showCatalog response.
     * @return The w10n "type" of a BES catalog node element
     */
    private String getW10nTypeStringForBesNode(Element node) throws BESError {

        String type;
        switch (node.getName()) {
            case "node":
                type = "fs.dir";
                break;
            case "item":
                String nodeType = node.getAttributeValue("type");
                if (nodeType.equals("node")) {
                    type = "fs.dir";
                } else {
                    // Axiom: If an item is not a node, then it is a leaf.
                    // It's a leaf, but is it data?
                    String isData = node.getAttributeValue(keyIsData);
                    if (isData != null && isData.equals("true")) {
                        type = "dap";
                    } else {
                        type = "fs.file";
                    }
                }
                break;
            default:
                throw new BESError("The bes:" + node.getName() + " element contains unanticipated content. expected " +
                        "bes:node or bes:item. Received " + node.getName());
        }
        return type;
    }

    /**
     * Transmits a BES catalog document as a w10n meta response encoded as HTML (srsly.)
     *
     * @param w10nRequest The w10n request to be serviced.
     * @param showNodeDoc The BES showNode response document.
     * @param response The outgoing response.
     * @throws SaxonApiException
     * @throws IOException
     */
    private void sendBesNodeAsHtml(W10nRequest w10nRequest, Document showNodeDoc, HttpServletResponse response)
            throws SaxonApiException, IOException {

        Request oreq = new Request(null,w10nRequest.getServletRequest());
        JDOMSource besCatalog = new JDOMSource(showNodeDoc);
        String xsltDoc = _systemPath + "/xsl/showNodeToW10nCatalog.xsl";

        Transformer transformer = new Transformer(xsltDoc);
        transformer.setParameter("dapService",oreq.getServiceLocalId());
        transformer.setParameter("docsService",oreq.getDocsServiceLocalID());
        if(BesDapDispatcher.allowDirectDataSourceAccess())
            transformer.setParameter("allowDirectDataSourceAccess","true");

        // Transform the BES  showCatalog response into a HTML page for the browser
        transformer.transform(besCatalog, response.getOutputStream());
    }


    /**
     *
     *
     * Sends the w10n data response using the client/server negotiated media type.
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param maxResponseSize  Max response size.
     * @param response The outgoing response.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void sendW10nDataResponse(
            W10nRequest w10nRequest,
            int maxResponseSize,
            HttpServletResponse response)
            throws IOException, OPeNDAPException {

        // Handle Response Media Type...
        MediaType mt = w10nRequest.getBestMediaType();
        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, mt);
        response.setContentType(mt.getMimeType());
        response.setHeader("Content-Description", mt.getMimeType());

        // Call the best responder for the requested media type
        if(mt.getName().equalsIgnoreCase(Json.NAME)) {
            sendDap2DataAsW10nJson(w10nRequest, maxResponseSize, response);
            return;
        }

        if(mt.getName().equalsIgnoreCase(Dap2Data.NAME)) {
            sendDap2Data(w10nRequest, maxResponseSize, response);
            return;
        }

        if(mt.getName().equalsIgnoreCase(Netcdf3.NAME)) {
            sendNetCDF_3(w10nRequest, maxResponseSize, response);
            return;
        }

        if(mt.getName().equalsIgnoreCase(Netcdf4.NAME)) {
            sendNetCDF_4(w10nRequest, maxResponseSize, response);
            return;
        }
        throw new NotAcceptable("Unsupported response encoding! You have requested an unsupported return type of "+ mt.getMimeType());
    }

    /**
     *
     * Utilizes the BesApi and the BES fileout_netcdf handler to transmit the requested variable
     * as NetCDF-3 encoded data.
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param maxResponseSize  Max response size.
     * @param response The outgoing response.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    public void sendNetCDF_3(W10nRequest w10nRequest,
                             int maxResponseSize,
                             HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {

        _log.debug("sendNetCDF_3() - Sending NetCDF-3 for dataset: {}",w10nRequest.getValidResourcePath());

        String xdap_accept = "3.2";

        String resourceID = w10nRequest.getRequestedResourceId();
        MediaType responseMediaType =  new Netcdf3();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());

        String downloadFileName = getDownloadFileName(resourceID);
        downloadFileName += ".nc";

        _log.debug("sendNetCDF_3() - NetCDF file downloadFileName: {}", downloadFileName );

        String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";
        response.setHeader("Content-Disposition", contentDisposition);

        OutputStream os = response.getOutputStream();
        _besApi.writeDap2DataAsNetcdf3(
                w10nRequest.getValidResourcePath(),
                w10nRequest.getDap2CE(),
                w10nRequest.getXmlBase(),
                xdap_accept,
                maxResponseSize,
                os);
        os.flush();
        _log.debug("sendNetCDF_3() - Sent NetCDF-3 for {}",w10nRequest.getValidResourcePath());
    }


    /**
     *
     * Utilizes the BesApi and the BES fileout_netcdf handler to transmit the requested variable
     * as NetCDF-4 encoded data.
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param maxResponseSize  Max response size.
     * @param response The outgoing response.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    public void sendNetCDF_4(W10nRequest w10nRequest,
                                 int maxResponseSize,
                                 HttpServletResponse response)
                throws IOException, PPTException, BadConfigurationException, BESError {

        _log.debug("sendNetCDF_4() - Sending NetCDF-4 for dataset: {}",w10nRequest.getValidResourcePath());

        String xdap_accept = "3.2";
        String resourceID = w10nRequest.getRequestedResourceId();
        MediaType responseMediaType = new Netcdf4();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());

        String downloadFileName = getDownloadFileName(resourceID);
        Pattern startsWithNumber = Pattern.compile("[0-9].*");
        if(startsWithNumber.matcher(downloadFileName).matches())
            downloadFileName = "nc_"+downloadFileName;

        downloadFileName = downloadFileName+".nc4";
        _log.debug("sendNetCDF_4() - NetCDF file downloadFileName: {}", downloadFileName);

        String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";
        response.setHeader("Content-Disposition", contentDisposition);

        OutputStream os = response.getOutputStream();
        _besApi.writeDap2DataAsNetcdf4(
                w10nRequest.getValidResourcePath(),
                w10nRequest.getDap2CE(),
                w10nRequest.getXmlBase(),
                xdap_accept,
                maxResponseSize,
                os);
        os.flush();

        _log.debug("sendNetCDF_4() - Sent NetCDF-4 for dataset: {}", w10nRequest.getValidResourcePath());
    }


    /**
     * Utilizes the BesApi and the BES w10n_handler to transmit the DAP2 data encoded as w10n JSON.
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param maxResponseSize  Max response size.
     * @param response The outgoing response.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void sendDap2DataAsW10nJson(W10nRequest w10nRequest,
                                        int maxResponseSize,
                                        HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {

        String resourceID = w10nRequest.getRequestedResourceId();
        _log.debug("Sending w10n JSON data response for dataset: {}",w10nRequest.getValidResourcePath());

        MediaType responseMediaType =  new Json();
        RequestCache.put("ResponseMediaType", responseMediaType);

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());
        response.setHeader("Content-Description", "w10n (json)  data");

        String downloadFileName = getDownloadFileName(resourceID)+".json";
        response.setHeader("Content-Disposition", " attachment; filename=\"" +downloadFileName+"\"");
        _log.debug("sendDap2DataAsW10nJson() - JSON file downloadFileName: {}", downloadFileName);

        Gson gson = new Gson();
        String w10nMetaObject = "\"w10n\":"+ gson.toJson(
                getW10nMetaObject(
                        DAP2_TYPE,
                        w10nRequest.getW10nResourcePath(),
                        w10nRequest.getW10nId(),
                        _defaultDataMediaType,
                        _supportedDataMediaTypes));

        ServletOutputStream os = response.getOutputStream();
        _besApi.writeDap2DataAsW10nJson(
                w10nRequest.getValidResourcePath(),
                w10nRequest.getDap2CE(),
                w10nMetaObject,
                w10nRequest.callback(),
                w10nRequest.flatten(),
                "3.2",
                maxResponseSize,
                os);
        os.flush();
    }

    /**
     *
     * Sends a DAP2 data response for the requested variable.
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param maxResponseSize  Max response size.
     * @param response The outgoing response.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void sendDap2Data(W10nRequest w10nRequest,
                              int maxResponseSize,
                              HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {

        _log.debug("sendDap2Data() - Sending DAP2 data response for dataset: {}",w10nRequest.getValidResourcePath());

        ServletOutputStream os = response.getOutputStream();

        String resourceID = w10nRequest.getRequestedResourceId();
        MediaType responseMediaType = new Dap2Data();

        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, responseMediaType);

        response.setContentType(responseMediaType.getMimeType());

        String downloadFileName = getDownloadFileName(resourceID)+".dods";
        response.setHeader("Content-Disposition", " attachment; filename=\"" +downloadFileName+"\"");
        _log.debug("sendDap2Data() - DAP2 Data file downloadFileName: {}", downloadFileName);

        response.setHeader("Content-Description", "DAP2 Data Response");
        _besApi.writeDap2Data(w10nRequest.getValidResourcePath(), w10nRequest.getDap2CE(), null, null, "3.2", maxResponseSize, os);

        os.flush();
    }


    /**
     * Transmits a non-data file from the BES to the requesting client.
     *
     * @param req
     * @param response
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    public void sendFile(HttpServletRequest req,
                         HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {

        String name = ReqInfo.getLocalUrl(req);
        _log.debug("sendFile(): Sending file \"{}\"", name);

        String downloadFileName = Scrub.fileName(name.substring(name.lastIndexOf("/")+1));
        _log.debug("sendFile() downloadFileName: {}", downloadFileName);

        String suffix = ReqInfo.getRequestSuffix(req);
        if (suffix != null) {
            MediaType responseMediaType = MimeTypes.getMediaType(suffix);
            if (responseMediaType != null) {
                response.setContentType(responseMediaType.getMimeType());
                _log.debug("sendFile() - MIME type: {}", responseMediaType.getMimeType());
            }
        }
        ServletOutputStream sos = response.getOutputStream();
        _besApi.writeFile(name, sos);
        sos.flush();
    }


    /**
     *
     * Sends the w10n meta response using the client/server negotiated media type.
     *
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param maxResponseSize  Max response size.
     * @param response The outgoing response.
     * @throws BESError
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     * @throws JDOMException
     * @throws SaxonApiException
     */
    private void sendW10nMetaResponseForDap2Metadata(W10nRequest w10nRequest,
                                                    int maxResponseSize,
                                                    HttpServletResponse response)
            throws OPeNDAPException, IOException, JDOMException, SaxonApiException {

        // Handle Response Media Type...
        MediaType mt = w10nRequest.getBestMediaType();
        // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
        RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, mt);
        response.setContentType(mt.getMimeType());
        response.setHeader("Content-Description", mt.getMimeType());
        response.setContentType(mt.getMimeType());

        if(mt.getName().equalsIgnoreCase(TextHtml.NAME)){
            sendDap2MetadataAsW10nHtml(w10nRequest, response);
            return;
        }

        if(mt.getName().equalsIgnoreCase(Json.NAME)){
            sendDap2MetadataAsW10nJson(w10nRequest,maxResponseSize,response);
            return;
        }
        throw  new NotAcceptable("Unsupported response encoding! You have requested an unsupported return type of"+ mt.getMimeType());
    }


    /**
     *
     * Utilizes the BesApi and the BES w10n_handler to transmit the DAP2 metadata encoded as w10n JSON.
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param maxResponseSize  Max response size.
     * @param response The outgoing response.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void sendDap2MetadataAsW10nJson(W10nRequest w10nRequest, int maxResponseSize, HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {

        Gson gs = new Gson();
        String w10nMetaObject = "\"w10n\":"+ gs.toJson(
                getW10nMetaObject(
                        DAP2_TYPE,
                        w10nRequest.getW10nResourcePath(),
                        w10nRequest.getW10nId(),
                        _defaultMetaMediaType,
                        _supportedMetaMediaTypes));

        ServletOutputStream os = response.getOutputStream();

        _besApi.writeDap2MetadataAsW10nJson(
                w10nRequest.getValidResourcePath(),
                w10nRequest.getDap2CE(),
                w10nMetaObject,
                w10nRequest.callback(),
                w10nRequest.flatten(),
                w10nRequest.traverse(),
                "3.2",
                maxResponseSize,
                os);

        os.flush();

    }


    /**
     * Retrieves a DDX from the server and produces a html view consistent with a w10n meta response using some hacking
     * of the DDX document and an XSLT.
     *
     * @param w10nRequest The w10nRequest object for the request to be serviced.
     * @param response The outgoing response.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     * @throws JDOMException
     * @throws SaxonApiException
     */
    private void sendDap2MetadataAsW10nHtml(W10nRequest w10nRequest,  HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError, JDOMException, SaxonApiException {

        Document besResponse = new Document();

        _besApi.getDDXDocument(w10nRequest.getValidResourcePath(), w10nRequest.getDap2CE(), "3.2", w10nRequest.getXmlBase(),  besResponse);

        boolean isNode = true;

        /**
         * If the requested a variable then we are going to hack the DDX document so that
         * only the stuff we want gets sent into the XSLT. Why? Because w10n doesn't care about parent containers
         * and such, only about the target variable - which may be a node or a leaf in w10n parlance.
         */
        if(w10nRequest.variableWasRequested()) {
            Element dataset = besResponse.getRootElement();
            Iterator<String> reqVarIter = w10nRequest.getRequestedVariableNameVector().iterator();
            Element requestedVariableElement = childSearchWorker(dataset, reqVarIter);

            if( requestedVariableElement.getName().equalsIgnoreCase("Grid") ||
                    requestedVariableElement.getName().equalsIgnoreCase("Structure") ||
                    requestedVariableElement.getName().equalsIgnoreCase("Sequence")){

                dataset.removeContent();
                @SuppressWarnings("unchecked")
                List<Element> varsAndAttrs = requestedVariableElement.getChildren();
                ArrayList<Element> containerContents = new ArrayList<>();
                containerContents.addAll(varsAndAttrs);
                for(Element e: containerContents){
                    e.detach();
                    dataset.addContent(e);
                }
                isNode = true;
            }
            else {
                dataset.removeContent();
                requestedVariableElement.detach();
                dataset.addContent(requestedVariableElement);
                isNode = false;
            }
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            if(_log.isDebugEnabled())
                _log.debug("sendDap2MetadataAsW10nHtml() - Transforming modified dataset document: \n{}",xmlo.outputString(besResponse));
        }

        String s;

        JDOMSource datasetDocumentSource = new JDOMSource(besResponse);

        String xsltDoc = _systemPath + "/xsl/w10nDataset.xsl";


        Transformer transformer = new Transformer(xsltDoc);
        transformer.setParameter("serviceContext", w10nRequest.getServiceContextPath());

        s = w10nRequest.getW10nResourcePath() + w10nRequest.getW10nId();
        transformer.setParameter("w10nName", s);
        transformer.setParameter("w10nType", isNode?"node":"leaf");

        // Since the array constraint will most surely contain '[' and ']' we need to URI encode it.
        s =  Encode.forUriComponent(w10nRequest.getW10nArrayConstraint());
        transformer.setParameter("arrayConstraint",s);

        // Transform the BES  showCatalog response into a HTML page for the browser
        transformer.transform(datasetDocumentSource, response.getOutputStream());
    }


    /**
     * Worker method to recursively locate the requested variable whose name is held in the iterator as a series
     * of name component strings.
     *
     * @param e The DDX Element to search
     * @param reqVarIter  The name component iterator.
     * @return
     */
    private Element childSearchWorker(Element e, Iterator<String> reqVarIter){
        Element requestedVariableElement = e;
        if(reqVarIter.hasNext()) {
            String requestedVarName = reqVarIter.next();
            @SuppressWarnings("unchecked")
            List<Element> varsAndAttrs = e.getChildren();
            for (Element vOrA : varsAndAttrs) {
                String typeName = vOrA.getName();
                // if it's not an attribute or the blob
                if (!typeName.equalsIgnoreCase("Attribute") && !typeName.equalsIgnoreCase("blob")) {
                    String name = vOrA.getAttributeValue("name");
                    // Does the name match?
                    if (name.equals(requestedVarName)) {
                        requestedVariableElement = vOrA;
                        return childSearchWorker(requestedVariableElement,reqVarIter);
                    }
                }
            }
        }
        return requestedVariableElement;
    }


    /**
     *
     * @param resourceID
     * @return
     */
    public String getDownloadFileName(String resourceID){
        String downloadFileName = (resourceID.substring(resourceID.lastIndexOf('/') + 1, resourceID.length()));
        int lastOpenBracketIndex = downloadFileName.lastIndexOf('[');
        int lastSlashIndex = downloadFileName.lastIndexOf('/');
        if ( lastOpenBracketIndex>0 && lastSlashIndex < lastOpenBracketIndex)
            downloadFileName = downloadFileName.substring(0,lastOpenBracketIndex);

        _log.debug("getDownloadFileName() - input: {} output: {}",resourceID,downloadFileName );

        return downloadFileName;
    }


}
