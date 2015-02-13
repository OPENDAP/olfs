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

import net.sf.saxon.s9api.SaxonApiException;
import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.BesDapDispatcher;
import opendap.bes.Version;
import opendap.bes.dap2Responders.BesApi;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.MimeTypes;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.dap.Request;
import opendap.dap.User;
import opendap.http.error.BadRequest;
import opendap.http.error.HttpError;
import opendap.http.error.NotAcceptable;
import opendap.http.mediaTypes.*;
import opendap.namespaces.BES;
import opendap.ppt.PPTException;
import opendap.viewers.ViewersServlet;
import opendap.xml.Transformer;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMSource;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Handles w10n services. Performs  HTTP server/client content negotiation, evaluates w10n requests
 * and formulates BES commands to produce the desired results.
 *
 * Created by ndp on 1/22/15.
 */
public class W10nResponder  {

    private Logger _log;

    TreeMap<String,MediaType> _supportedMetaMediaTypes;
    TreeMap<String,MediaType> _supportedDataMediaTypes;

    MediaType _defaultMetaMediaType;
    MediaType _defaultDataMediaType;

    BesApi _besApi;

    String _systemPath;



    public W10nResponder(String systemPath){


        _log = LoggerFactory.getLogger(this.getClass());

        _systemPath = systemPath;

        _besApi = new BesApi();

        MediaType mt;




        mt = new Json();
        _defaultMetaMediaType = mt;


        _supportedMetaMediaTypes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        mt = new Html();
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


    private MediaType getBestMediaType(HttpServletRequest request, MediaType defaultMediaType, Map<String,MediaType> altMediaTypes) throws HttpError {

        MediaType bestMT;

        String outputParam = request.getParameter("output");

        if(outputParam!=null){
            _log.debug("getMediaTypeForHttpRequest() - Client provided 'output' query parameter {}",outputParam);

            if(defaultMediaType.getName().equalsIgnoreCase(outputParam)) {
                bestMT = defaultMediaType;
                _log.debug("getMediaTypeForHttpRequest() - 'output' query parameter matches default MediaType of '{}'",bestMT.getMimeType());
            }
            else {
                bestMT = altMediaTypes.get(outputParam);
                if(bestMT == null){
                    String msg = "Client requested an unsupported output type of " + outputParam;
                    _log.debug("getBestMediaType() - {}",msg);
                    throw new NotAcceptable(msg);

                }
                _log.debug("getMediaTypeForHttpRequest() - 'output' query parameter matches MediaType of '{}'",bestMT.getMimeType());
            }
        }
        else {
            bestMT = getMediaTypeForHttpRequest(request, defaultMediaType, altMediaTypes);

        }

        return bestMT;


    }



    /**
     * THis is where we do the Server-driven HTTP Content Negotiation.
     * @param request The client's incoming request
     * @return  Most appropriate MediaType for the response
     * @throws java.util.NoSuchElementException
     */
    public MediaType getMediaTypeForHttpRequest(HttpServletRequest request, MediaType defaultMediaType, Map<String,MediaType> altMediaTypes) {


        MediaType bestType = defaultMediaType;

        Vector<MediaType> clientMediaTypes = getClientMediaTypes(request);

        if(clientMediaTypes.isEmpty()) {
            _log.debug("getMediaTypeForHttpRequest() - Client did not provide an Accept header, returning default MediaType {}",bestType.getMimeType());
            return bestType;
        }


        for(MediaType mt: clientMediaTypes){
            _log.debug("getMediaTypeForHttpRequest() - Clients accepts media type: {}", mt.toString());
        }

        TreeSet<MediaType> matchingTypes = new TreeSet<>();

        for(MediaType mt: clientMediaTypes){
            if(mt.getMimeType().equalsIgnoreCase(defaultMediaType.getMimeType())){
                matchingTypes.add(defaultMediaType);
            }
            else if(mt.getPrimaryType().equalsIgnoreCase(defaultMediaType.getPrimaryType()) &&
                    mt.getSubType().equalsIgnoreCase("*")){
                matchingTypes.add(defaultMediaType);
            }
            else if(mt.getPrimaryType().equalsIgnoreCase("*") &&
                    mt.getSubType().equalsIgnoreCase("*")){
                matchingTypes.add(defaultMediaType);
            }

            for(MediaType altType : altMediaTypes.values()){

                if(mt.getMimeType().equalsIgnoreCase(altType.getMimeType())){
                    matchingTypes.add(altType);
                }
                else if(mt.getPrimaryType().equalsIgnoreCase(altType.getMimeType()) &&
                        mt.getSubType().equalsIgnoreCase("*")){
                    matchingTypes.add(altType);
                }
            }
        }

        if(!matchingTypes.isEmpty()){
            bestType = matchingTypes.last();
            _log.debug("getMediaTypeForHttpRequest() - Best Matching Type:  {}", bestType);
            _log.debug("getMediaTypeForHttpRequest() - Worst Matching Type: {}", matchingTypes.first());
        }


        _log.debug("getMediaTypeForHttpRequest() - Using Media Type: {}", bestType);


        return bestType;

    }

    public Vector<MediaType> getClientMediaTypes(HttpServletRequest request){
        String acceptsHeaderValue = request.getHeader("Accept");

        _log.debug("Accept: {}", acceptsHeaderValue);

        Vector<MediaType> clientMediaTypes = new Vector<>();

        if(acceptsHeaderValue!=null){
            String[] mimeTypes = acceptsHeaderValue.split(",");

            for(String mimeType: mimeTypes){
                clientMediaTypes.add(new MediaType(mimeType.trim()));
            }
        }

        return clientMediaTypes;
    }



    private void setResponseHeaders(HttpServletRequest request, String requestedResourceId, MediaType mt, HttpServletResponse response) throws Exception {

        response.setContentType(mt.getMimeType());
        response.setHeader("Content-Description",mt.getMimeType());
        Version.setOpendapMimeHeaders(request, response, _besApi);


        // If they aren't asking for html then set the Content-Disposition header which will trigger a browser
        // to download the response and save it to a file.
        if(!mt.getName().equalsIgnoreCase(Html.NAME) && !mt.getName().equalsIgnoreCase(Json.NAME)) {
            while(requestedResourceId.endsWith("/") && requestedResourceId.length()>=0){
                requestedResourceId = requestedResourceId.substring(0,requestedResourceId.lastIndexOf("/"));
            }


            String downloadFileName = requestedResourceId.substring(requestedResourceId.lastIndexOf("/") + 1,
                                      requestedResourceId.length());


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


    public void send_w10n_response(HttpServletRequest request, HttpServletResponse response) throws Exception {


        _log.debug("send_w10n_response() - BEGIN");

        Request oreq = new Request(null, request);
        String requestedResourceId = oreq.getRelativeUrl();





        User user = new User(request);


        Document pathInfoDoc =  new Document();
        boolean result = _besApi.getPathInfoDocument(requestedResourceId, pathInfoDoc);
        if(!result){
            BESError besError = new BESError(pathInfoDoc);
            _log.error("send_w10n_response() encountered a BESError: {}"+besError.getErrorMessage());

            sendErrorResponse(response,besError, _defaultMetaMediaType);
            return; // Because it broke already....
        }

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        _log.debug("send_w10n_response() - getPathInfo response: \n {}",xmlo.outputString(pathInfoDoc));

        Element besResponse = pathInfoDoc.getRootElement();
        Element showPathInfo =  besResponse.getChild("showPathInfo", BES.BES_NS);
        Element pathInfo =  showPathInfo.getChild("PathInfo", BES.BES_NS);

        Element vpE =  pathInfo.getChild("validPath", BES.BES_NS);
        Element remE =  pathInfo.getChild("remainder", BES.BES_NS);


        String validPath = vpE.getTextTrim();
        boolean isData   = vpE.getAttributeValue("isData").equalsIgnoreCase("true");
        boolean isDir    = vpE.getAttributeValue("isDir").equalsIgnoreCase("true");
        boolean isFile   = vpE.getAttributeValue("isFile").equalsIgnoreCase("true");

        String remainder = remE.getTextTrim();

        boolean isMetaRequest = requestedResourceId.endsWith("/");








        String w10nPathIdentifier = oreq.getServiceLocalId() + validPath + "/" +remainder;
        if(isMetaRequest && !w10nPathIdentifier.endsWith("/"))
            w10nPathIdentifier += "/";


        // We know that the resourceId is a proper dataset in the BES, so now we know we need to formulate
        // a DAP constraint expression from the remainder (if there is one)

        W10nDap2Constraint dapCE = makeDap2ConstraintFromRemainderString(remainder);


        // Is this a w10n meta request?
        if(isMetaRequest){
            // yup - build a meta response.
            MediaType mt = getBestMediaType(request, _defaultMetaMediaType,_supportedMetaMediaTypes);

            setResponseHeaders(request,requestedResourceId, mt, response);

            _log.debug("send_w10n_response() - Sending w10n meta response for resource: {} Response type: {}",requestedResourceId,mt.getMimeType());


            // First we QC the request

            // Is the thing they asked for some thing the BES sees as a dataset?
            if(isData){

                // OK, the BES thinks it's data, but just to be sure something is not broken,
                // Let's see if it's a file too.
                if(isFile) {

                    // And then we to send the response, using the MediaType to determine what to
                    // send back.
                    sendW10nMetaResponseForDap2Metadata(request, validPath, dapCE, w10nPathIdentifier, mt, user.getMaxResponseSize(), response);

                }
                else {
                    // It's not a file! That's a BAD THING.
                    // It's not possible for a directory to be data.
                    // Since that indicates something serious is busted somewhere: Internal ERROR.
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                // The BES doesn't think the valid part of the path is data

                //Is there a remainder?
                if(remainder.length()>0){
                    // Dang - given that the BES doesn't see this thing as data
                    // and there is a path remainder we're in a NotFound situation
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                else {

                    // It doesn't matter if it's a directory or a file, we'll get the show catalog response
                    // as an XML document and we'll return a version appropriate to the negotiated media type.
                    sendW10nMetaResponseForFileOrDir(request, validPath, mt, response);
                }

            }


        }
        else {
            // It's a data request

            MediaType mt = getBestMediaType(request, _defaultDataMediaType,_supportedDataMediaTypes);
            _log.debug("send_w10n_response() - Sending w10n data response for resource: {} Response type: {}",requestedResourceId,mt.getMimeType());


            if(!isData){
                // The BES doesn't think this thing is data



                if(isDir){
                    // Data request are not valid for nodes (datasets or directories)
                    // So if it's a directory, that's a fail.
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);

                }
                // It's not data. But did they try to w10n access it anyway?
                else if(remainder.length()>0 && !isDir){
                    // Yup, but that's a fail so - 400!
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
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
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);

            }
            else {


                sendW10nDataResponse(validPath, dapCE, w10nPathIdentifier, mt, user.getMaxResponseSize(), response);


            }

        }





        _log.debug("send_w10n_response() - END. Sent w10n Response");



    }




    private void sendBadMediaTypeError(MediaType mt, HttpServletResponse response) throws IOException {
        if(!response.isCommitted())
            response.reset();
        response.setContentType(new Html().getMimeType());
        response.setHeader("Content-Description","ERROR");

        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);

        ServletOutputStream sos = response.getOutputStream();

        sos.println("<html>");
        sos.println("<head><title>ERROR: Unsupported media type</title></head>");
        sos.println("<body>");
        sos.println("<h2>ERROR</h2>");
        sos.println("You have requested an unsupported return type of "+ mt.getMimeType());
        sos.println("</body></html>");

    }


    public void sendErrorResponse(HttpServletResponse response, BESError besError, MediaType mt) throws IOException {


        if(!response.isCommitted())
            response.reset();
        response.setContentType(new Html().getMimeType());
        response.setHeader("Content-Description","BES ERROR");

        response.sendError(besError.getHttpStatus());




    }


    private W10nDap2Constraint makeDap2ConstraintFromRemainderString(String remainder) throws BadRequest {

        while(remainder.startsWith("/") && remainder.length()>0)
            remainder = remainder.substring(1);


        int lastOpenBracket = remainder.lastIndexOf("[");
        int lastCloseBracket = remainder.lastIndexOf("]");
        int lastSlash = remainder.lastIndexOf("/");

        StringBuilder arrayConstraint = new StringBuilder();
        if(
                lastOpenBracket > lastSlash  &&
                lastCloseBracket > lastOpenBracket &&
                lastCloseBracket == (remainder.length()-1)
                ){


            // looks like there's an array constraint!
            String arraySubset = remainder.substring(lastOpenBracket);
            arraySubset = arraySubset.replace("[","").replace("]","");


            String dimSubsets[] = arraySubset.split(",");

            for(String dimSubset: dimSubsets){

                String sss[] = dimSubset.split(":");

                switch(sss.length){
                    case 1:
                        arrayConstraint.append("[").append(sss[0]).append("]");
                        break;

                    case 2:
                        arrayConstraint.append("[")
                                .append(sss[0]).append(":").append("1").append(":").append(sss[1])
                                .append("]");
                        break;

                    case 3:
                        arrayConstraint.append("[")
                                .append(sss[0]).append(":").append(sss[2]).append(":").append(sss[1])
                                .append("]");
                        break;

                    default:
                        throw new BadRequest("Improper arrays subset syntax "+remainder);
                }


            }


            remainder = remainder.substring(0,lastOpenBracket);




        }


        W10nDap2Constraint dapCe = new W10nDap2Constraint();


        if(remainder.length()>0) {
            String vars[] = remainder.split("/");

            for (String var : vars)
                dapCe.requestedVariable.add(var);
        }

        dapCe.arrayConstraint = arrayConstraint.toString();


        return dapCe;

    }


    private void sendW10nMetaResponseForFileOrDir(HttpServletRequest request, String resourceId, MediaType mediaType, HttpServletResponse response) throws JDOMException, BadConfigurationException, PPTException, IOException, SaxonApiException {

        String context = request.getContextPath();



        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());



        Document showCatalogDoc = new Document();

        if(_besApi.getBesCatalog(resourceId, showCatalogDoc)){

            _log.debug("sendMetaResponseForFileOrDir() - Catalog from BES:\n"+xmlo.outputString(showCatalogDoc));


            if(mediaType.getName().equalsIgnoreCase(Json.NAME)){
                _log.debug("sendMetaResponseForFileOrDir() - Sending as JSON");
                sendBesCatalogAsJson(request, showCatalogDoc, response);

            }
            else if(mediaType.getName().equalsIgnoreCase(Html.NAME)){
                _log.debug("sendMetaResponseForFileOrDir() - Sending as HTML");
                sendBesCatalogAsHtml(request, showCatalogDoc, response);
            }
            else {
                response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "You have requested an unsupported response type of " + mediaType.getMimeType());
            }

        }
        else {
            BESError besError = new BESError(showCatalogDoc);
            besError.sendErrorResponse(_systemPath, context, response);
            _log.error(besError.getMessage());

        }

    }


    private void sendBesCatalogAsJson(HttpServletRequest request, Document showCatalogDoc, HttpServletResponse response) throws JDOMException, IOException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        // response.getOutputStream().print(xmlo.outputString(showCatalogDoc));


        Request oreq = new Request(null, request);

        String relUrl = oreq.getRelativeUrl();
        String w10nPathIdentifier = oreq.getServiceLocalId() + relUrl;


        Element showCatalogElement = showCatalogDoc.getRootElement().getChild("showCatalog",BES.BES_NS);

        Element topDataset = showCatalogElement.getChild("dataset",BES.BES_NS);

        String type = getW10nTypeStringForBesCatalogDataset(topDataset);


        boolean isNode = isDatasetW10nNode(topDataset);

        HashMap<String,Object> jsonBesCatalogResponse = getJsonForDatasetElement(topDataset);

        if(isNode) {
            ArrayList<Object> nodes = new ArrayList<>();
            ArrayList<Object> leaves = new ArrayList<>();

            @SuppressWarnings("unchecked")
            List<Element> childDatasets = (List<Element>) topDataset.getChildren("dataset", BES.BES_NS);

            for(Element childDataset: childDatasets){
                HashMap<String,Object> jsonDataset = getJsonForDatasetElement(childDataset);

                if(isDatasetW10nNode(childDataset)){
                    nodes.add(jsonDataset);
                }
                else {
                    leaves.add(jsonDataset);
                }
            }

            jsonBesCatalogResponse.put("nodes", nodes);
            jsonBesCatalogResponse.put("leaves", leaves);
            jsonBesCatalogResponse.put("w10n", getW10nMeta(type, "", w10nPathIdentifier,_defaultMetaMediaType,_supportedMetaMediaTypes));
        }
        else {
            jsonBesCatalogResponse.put("w10n", getW10nMeta(type, w10nPathIdentifier, "/",_defaultMetaMediaType,_supportedMetaMediaTypes));

        }

        ServletOutputStream sos =  response.getOutputStream();

        sos.print(JSONValue.toJSONString(jsonBesCatalogResponse));

        sos.println();

    }


    private boolean isDatasetW10nNode(Element dataset){

        boolean isNode;

        isNode = dataset.getAttributeValue("node").equalsIgnoreCase("true");

        Element serviceRef = dataset.getChild("serviceRef",BES.BES_NS);
        if(serviceRef!=null &&  serviceRef.getTextTrim().equalsIgnoreCase("dap")){
            isNode = true;
        }

        return isNode;

    }


    private HashMap<String,Object> getJsonForDatasetElement(Element dataset){

        String nodeName = dataset.getAttributeValue("name");

        String name = nodeName;

        int lastIndexOfSlash = nodeName.lastIndexOf('/');
        if(lastIndexOfSlash > 0) {
            name = nodeName.substring(lastIndexOfSlash);
            while (name.startsWith("/") && name.length() > 1)
                name = name.substring(1);
        }

        long   size = Integer.parseInt(dataset.getAttributeValue("size"));
        String  lmt = dataset.getAttributeValue("lastModified");


        HashMap<String,Object> jsonObject = new HashMap<>();

        jsonObject.put("name",name);

        ArrayList<Object> attributes = new ArrayList<>();

        attributes.add(getW10nAttribute("last_modified",lmt));

        attributes.add(getW10nAttribute("size", size));

        jsonObject.put("attributes",attributes);



        return jsonObject;



    }





    private  ArrayList<Object> getW10nMeta(String type, String path, String id, MediaType defaultMT, Map<String,MediaType> altMediaTypes){

        ArrayList<Object> w10n = new ArrayList<>();

        w10n.add(getW10nAttribute("spec","draft-20091228"));
        w10n.add(getW10nAttribute("application", "Hyrax-"+Version.getHyraxVersionString()));
        w10n.add(getW10nAttribute("type", type));
        w10n.add(getW10nAttribute("path", path));
        w10n.add(getW10nAttribute("identifier", id));
        w10n.add(getW10nAttribute("output", getW10nOutputTypes(defaultMT,altMediaTypes)));

        return w10n;


    }



    private ArrayList<Object> getW10nOutputTypes(MediaType defaultMT, Map<String,MediaType> altMediaTypes ) {

        ArrayList<Object> outputTypes = new ArrayList<>();

        outputTypes.add(getW10nAttribute("type",defaultMT.getName(),defaultMT.getMimeType()));

        for(MediaType mt : altMediaTypes.values()){
            outputTypes.add(getW10nAttribute("type",mt.getName(),mt.getMimeType()));
        }

        return outputTypes;


    }







    private JSONObject getW10nAttribute(String name, Object value){

        HashMap<String,Object> w10nAttribute = new HashMap<>();
        w10nAttribute.put("name", name);
        w10nAttribute.put("value", value);

        return new JSONObject(w10nAttribute);

    }

    private JSONObject getW10nAttribute(String name, Object value, String description){

        HashMap<String,Object> w10nAttribute = new HashMap<>();
        w10nAttribute.put("name", name);
        w10nAttribute.put("value", value);
        w10nAttribute.put("mime-type", description);

        return new JSONObject(w10nAttribute);

    }


    private String getW10nTypeStringForBesCatalogDataset(Element dataset){

        String type;

        boolean isNode;


        isNode = dataset.getAttributeValue("node").equalsIgnoreCase("true");

        if(isNode){
            type = "fs.dir";
        }
        else {
            Element serviceRef = dataset.getChild("serviceRef",BES.BES_NS);
            if(serviceRef!=null){
                type  = serviceRef.getTextTrim();
            }
            else {
                type = "fs.file";
            }

        }
        return type;



    }



    private void sendBesCatalogAsHtml(HttpServletRequest request, Document showCatalogDoc, HttpServletResponse response) throws SaxonApiException, IOException {


        Request oreq = new Request(null,request);

        JDOMSource besCatalog = new JDOMSource(showCatalogDoc);

        String xsltDoc = _systemPath + "/xsl/w10nCatalog.xsl";


        Transformer transformer = new Transformer(xsltDoc);

        transformer.setParameter("dapService",oreq.getServiceLocalId());
        transformer.setParameter("docsService",oreq.getDocsServiceLocalID());
        transformer.setParameter("viewersService", ViewersServlet.getServiceId());
        if(BesDapDispatcher.allowDirectDataSourceAccess())
            transformer.setParameter("allowDirectDataSourceAccess","true");

        // Transform the BES  showCatalog response into a HTML page for the browser
        transformer.transform(besCatalog, response.getOutputStream());
        // transformer.transform(besCatalog, System.out);



    }

    private void sendBesCatalogAsThredds(Document showCatalogDoc, HttpServletResponse response){

    }




    private void sendW10nDataResponse(String resourceId,
                                                     W10nDap2Constraint dapCe,
                                                     String w10nPathIdentifier,
                                                     MediaType mt,
                                                     int maxResponseSize,
                                                     HttpServletResponse response)
                throws IOException, PPTException, BadConfigurationException, BESError {



        if(mt.getName().equalsIgnoreCase(Json.NAME)) {
            sendW10nDataResponseForDap2Data(resourceId, dapCe, w10nPathIdentifier, mt, maxResponseSize, response);
            return;
        }

        if(mt.getName().equalsIgnoreCase(Dap2Data.NAME)) {
            sendDap2Data(resourceId, dapCe, mt, maxResponseSize, response);
            return;
        }

        if(mt.getName().equalsIgnoreCase(Netcdf3.NAME)) {
            sendNetCDF_3(resourceId, dapCe, mt, maxResponseSize, response);
            return;
        }

        if(mt.getName().equalsIgnoreCase(Netcdf4.NAME)) {
            sendNetCDF_4(resourceId, dapCe, mt, maxResponseSize, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "You have requested an unsupported response type of " + mt.getMimeType());

        return;


    }

    public void sendNetCDF_3(String resourceID,
                             W10nDap2Constraint dapCe,
                             MediaType mt,
                             int maxRS,
                             HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {




        _log.debug("Sending NetCDF-3 for dataset: {}",resourceID);

        response.setContentType(mt.getMimeType());
        //Version.setOpendapMimeHeaders(request, response, _besApi);
        response.setHeader("Content-Description", mt.getMimeType());



        String xdap_accept = "3.2";


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();



        if(!_besApi.writeDap2DataAsNetcdf3(resourceID, dapCe.ce(), xdap_accept, maxRS, os, erros)){
            String msg = new String(erros.toByteArray());
            _log.error("respondToHttpGetRequest() encountered a BESError: " + msg);
            os.write(msg.getBytes());

        }


        os.flush();
        _log.debug("Sent NetCDF-3 for {}",resourceID);



    }


    public void sendNetCDF_4(String resourceID,
                                 W10nDap2Constraint dapCe,
                                 MediaType mt,
                                 int maxRS,
                                 HttpServletResponse response)
                throws IOException, PPTException, BadConfigurationException, BESError {





        _log.debug("Sending NetCDF-4 for dataset: {}",resourceID);

        response.setContentType(mt.getMimeType());
        // Version.setOpendapMimeHeaders(request, response, _besApi);
        response.setHeader("Content-Description", mt.getMimeType());



        String xdap_accept = "3.2";



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();



        if(!_besApi.writeDap2DataAsNetcdf4(resourceID, dapCe.ce(), xdap_accept, maxRS, os, erros)){
            String msg = new String(erros.toByteArray());
            _log.error("respondToHttpGetRequest() encountered a BESError: " + msg);
            os.write(msg.getBytes());

        }


        os.flush();
        _log.debug("Sent NetCDF-4 for dataset: {}",resourceID);



    }




    private void sendW10nDataResponseForDap2Data(String resourceId,
                                                 W10nDap2Constraint dapCe,
                                                 String w10nPathIdentifier,
                                                 MediaType mt,
                                                 int maxResponseSize,
                                                 HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {


        String w10nMeta = "\"w10n\":"+ JSONValue.toJSONString(getW10nMeta("dap",w10nPathIdentifier,"/",_defaultDataMediaType,_supportedDataMediaTypes));

        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if (!_besApi.writeDap2DataAsW10nJson(resourceId, dapCe.ce(), w10nMeta,"3.2", maxResponseSize, os, erros)) {
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()),mt);
            besError.sendErrorResponse(_systemPath, "opendap", response);
            return;
        }



        os.flush();

    }

    private void sendDap2Data(String resourceId,
                              W10nDap2Constraint dapCe,
                              MediaType mt,
                              int maxResponseSize,
                              HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {



        ServletOutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if (!_besApi.writeDap2Data(resourceId, dapCe.ce(), null, null,"3.2", maxResponseSize, os, erros)) {
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()),mt);
            besError.sendErrorResponse(_systemPath, "opendap", response);
            return;
        }


        os.flush();

    }




    public void sendFile(HttpServletRequest req,
                         HttpServletResponse response)
            throws Exception {


        String name = ReqInfo.getLocalUrl(req);


        _log.debug("sendFile(): Sending file \"" + name + "\"");

        String downloadFileName = Scrub.fileName(name.substring(name.lastIndexOf("/")+1));

        _log.debug("sendFile() downloadFileName: " + downloadFileName );

        // I commented these two lines  out because it was incorrectly causing browsers to downloadJobOutput
        // (as opposed to display) EVERY file retrieved.
        //String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";
        //response.setHeader("Content-Disposition",contentDisposition);


        String suffix = ReqInfo.getRequestSuffix(req);

        if (suffix != null) {
            String mType = MimeTypes.getMimeType(suffix);

            if (mType != null)
                response.setContentType(mType);

            _log.debug("   MIME type: " + mType + "  ");
        }





        response.setStatus(HttpServletResponse.SC_OK);
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        ServletOutputStream sos = response.getOutputStream();
        if(!_besApi.writeFile(name, sos, erros)){

            BESError berr = new BESError(new ByteArrayInputStream(erros.toByteArray()));

            _log.error("sendFile() - ERROR. msg: "+ berr.getErrorMessage());
            berr.sendErrorResponse(_systemPath, req.getContextPath(), response);

        }


    }



    public String getXmlBase(HttpServletRequest req){

        String forwardRequestUri = (String)req.getAttribute("javax.servlet.forward.request_uri");
        String requestUrl = req.getRequestURL().toString();


        if(forwardRequestUri != null){
            String server = req.getServerName();
            int port = req.getServerPort();
            String scheme = req.getScheme();
            requestUrl = scheme + "://" + server + ":" + port + forwardRequestUri;
        }



        String xmlBase = requestUrl;



        _log.debug("@xml:base='{}'", xmlBase);
        return xmlBase;
    }



    class W10nDap2Constraint {

        private Vector<String> requestedVariable;
        private String arrayConstraint;
        public W10nDap2Constraint(){
            requestedVariable = new Vector<>();
        }

        public String getRequestedVariable(){

            StringBuilder reqVar = new StringBuilder();
            boolean first = true;
            for(String var: requestedVariable){
                if(!first)
                    reqVar.append(".");
                reqVar.append(var);
                first = false;

            }
            return reqVar.toString();
        }

        public String ce(){
            return getRequestedVariable() + arrayConstraint;
        }
        public String toString(){
            return getRequestedVariable() + arrayConstraint;
        }
    }



    private void sendW10nMetaResponseForDap2Metadata(HttpServletRequest request,
                                                     String resourceId,
                                                     W10nDap2Constraint dap2Constraint,
                                                    String w10nPathIdentifier,
                                                    MediaType mt,
                                                    int maxResponseSize,
                                                    HttpServletResponse response)
            throws BESError, BadConfigurationException, PPTException, IOException, JDOMException, SaxonApiException {



        if(mt.getName().equalsIgnoreCase(Html.NAME)){
            sendDap2MetadataAsW10nHtml(request, resourceId, dap2Constraint, w10nPathIdentifier, mt, response);
            return;
        }


        if(mt.getName().equalsIgnoreCase(Json.NAME)){
            sendDap2MetadataAsW10nJson(resourceId,dap2Constraint,w10nPathIdentifier,mt,maxResponseSize,response);
            return;
        }




        sendBadMediaTypeError(mt,response);

    }
    private void sendDap2MetadataAsW10nJson(String resourceId,
                                            W10nDap2Constraint dapCe,
                                            String w10nPathIdentifier,
                                            MediaType mt,
                                            int maxResponseSize,
                                            HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError {

        String w10nMeta = "\"w10n\":"+ JSONValue.toJSONString(getW10nMeta("dap",w10nPathIdentifier,"/",_defaultMetaMediaType,_supportedMetaMediaTypes));

        ServletOutputStream os = response.getOutputStream();

        ByteArrayOutputStream erros = new ByteArrayOutputStream();

        if (!_besApi.writeDap2MetadataAsW10nJson(resourceId, dapCe.ce(), w10nMeta, "3.2", maxResponseSize, os, erros)) {
            BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()),mt);
            besError.sendErrorResponse(_systemPath, "opendap", response);
            return;
        }

        os.flush();

    }


    private void sendDap2MetadataAsW10nHtml(HttpServletRequest request,
                                            String resourceId,
                                            W10nDap2Constraint dapCe,
                                            String w10nPathIdentifier,
                                            MediaType mt,
                                            HttpServletResponse response)
            throws IOException, PPTException, BadConfigurationException, BESError, JDOMException, SaxonApiException {

        String context = request.getContextPath();

        String xmlBase = getXmlBase(request);

        //Request oreq = new Request(null,request);

       // String serviceId = oreq.getServiceLocalId();

        //String requestedItem = w10nPathIdentifier.substring(serviceId.length());

        Document besResponse = new Document();

        if (!_besApi.getDDXDocument(resourceId, dapCe.ce(), "3.2",xmlBase,besResponse)) {
            BESError besError = new BESError(besResponse,mt);
            besError.sendErrorResponse(_systemPath, "opendap", response);
            return;
        }


        boolean isNode = true;

        if(dapCe.requestedVariable.size()>0) {
            Element dataset = besResponse.getRootElement();

            Iterator<String> reqVarIter = dapCe.requestedVariable.iterator();

            Element requestedVariableElement = childSearchWorker(dataset, reqVarIter);


            if( requestedVariableElement.getName().equalsIgnoreCase("Grid") ||
                    requestedVariableElement.getName().equalsIgnoreCase("Structure") ||
                    requestedVariableElement.getName().equalsIgnoreCase("Sequence")){


                dataset.removeContent();

                @SuppressWarnings("unchecked")
                List<Element> varsAndAttrs = requestedVariableElement.getChildren();

                Vector<Element> containerContents = new Vector<>();
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

            _log.debug("sendDap2MetadataAsW10nHtml() - Transforming modified dataset document: \n{}",xmlo.outputString(besResponse));
        }




        JDOMSource datasetDocumentSource = new JDOMSource(besResponse);

        String xsltDoc = _systemPath + "/xsl/w10nDataset.xsl";


        Transformer transformer = new Transformer(xsltDoc);

        transformer.setParameter("serviceContext", context);
        transformer.setParameter("w10nName", w10nPathIdentifier);
        transformer.setParameter("w10nType", isNode?"node":"leaf");

        // Transform the BES  showCatalog response into a HTML page for the browser
        transformer.transform(datasetDocumentSource, response.getOutputStream());
        // transformer.transform(besCatalog, System.out);




    }


    Element childSearchWorker(Element e, Iterator<String> reqVarIter){


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







}
