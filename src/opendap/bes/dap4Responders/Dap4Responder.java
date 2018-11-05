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

package opendap.bes.dap4Responders;

import opendap.bes.*;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.Util;
import opendap.http.error.*;
import opendap.namespaces.DAP;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/4/12
 * Time: 9:16 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Dap4Responder extends BesDapResponder  {

    Logger _log;

    private MediaType _normativeMediaType;
    private Vector<Dap4Responder> _altResponders;
    private String _combinedRequestSuffixRegex;
    private boolean _addTypeSuffixToDownloadFilename;



    public Dap4Responder(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        _log = LoggerFactory.getLogger(getClass().getName());
        _altResponders =  new Vector<>();
        addTypeSuffixToDownloadFilename(false);
    }

    public void addTypeSuffixToDownloadFilename(boolean value){
        _addTypeSuffixToDownloadFilename = value;
    }


    public boolean addTypeSuffixToDownloadFilename(){
        return _addTypeSuffixToDownloadFilename;
    }


    public void setNormativeMediaType(MediaType mt){
        _normativeMediaType = mt;
        _combinedRequestSuffixRegex = buildRequestMatchingRegex();
        _log.debug("combinedRequestSuffixRegex: {}", _combinedRequestSuffixRegex);
        setRequestMatchRegex(_combinedRequestSuffixRegex);
    }


    public MediaType getNormativeMediaType(){
        return _normativeMediaType;
    }

    public Dap4Responder[] getAltRepResponders(){
        Dap4Responder[] ars = new Dap4Responder[_altResponders.size()];
        return _altResponders.toArray(ars);
    }


    public void addAltRepResponder(Dap4Responder altRepResponder){
        _altResponders.add(altRepResponder);
        _combinedRequestSuffixRegex = buildRequestMatchingRegex();
        _log.debug("combinedRequestSuffixRegex: {}", _combinedRequestSuffixRegex);
        setRequestMatchRegex(_combinedRequestSuffixRegex);
        for(Dap4Responder responder: _altResponders){
            responder._combinedRequestSuffixRegex = _combinedRequestSuffixRegex;
        }
    }


    public void clearAltResponders(){
        _altResponders.clear();
    }



    public String getCombinedRequestSuffixRegex(){
        return _combinedRequestSuffixRegex;
    }

    public void setCombinedRequestSuffixRegex(String regex){
        _combinedRequestSuffixRegex = regex;
    }

    public String buildRequestMatchingRegex() {

        StringBuilder s = new StringBuilder();
        s.append(buildRequestMatchingRegexWorker(this));
        s.append("$");
        _log.debug("Request Match Regex: {}", s.toString());
        return s.toString();

    }

    private String buildRequestMatchingRegexWorker(Dap4Responder responder) {

        StringBuilder s = new StringBuilder();

        if (responder.getNormativeMediaType().getMediaSuffix().startsWith("."))
            s.append("\\");
        s.append(responder.getNormativeMediaType().getMediaSuffix());


        Dap4Responder[] altResponders = responder.getAltRepResponders();


        boolean hasAltRepResponders = altResponders.length > 0;
        if (hasAltRepResponders)
            s.append("(");


        boolean notFirstPass = false;
        for (Dap4Responder altResponder : altResponders) {

            if (notFirstPass)
                s.append("|");

            s.append("(").append("(");

            s.append(buildRequestMatchingRegexWorker(altResponder));

            s.append(")?").append(")");

            notFirstPass = true;
        }

        if (hasAltRepResponders)
            s.append(")?");

        return s.toString();

    }


    /**
     * THis is where we do the Server-driven HTTP Content Negotiation.
     * @param request
     * @return
     * @throws NoSuchElementException
     */
    public Dap4Responder getBestResponderForHttpRequest(HttpServletRequest request) throws NoSuchElementException {


        HashMap<MediaType,Dap4Responder> responderMap = new HashMap<MediaType, Dap4Responder>();

        String acceptsHeaderValue = request.getHeader("Accept");

        _log.debug("Accept: {}", acceptsHeaderValue);

        Vector<MediaType> clientMediaTypes = new Vector<MediaType>();

        if(acceptsHeaderValue!=null){
            String[] mimeTypes = acceptsHeaderValue.split(",");

            for(String mimeType: mimeTypes){
                clientMediaTypes.add(new MediaType(mimeType.trim()));
            }
        }
        else {
            return this;
        }

        for(MediaType mt: clientMediaTypes){
            _log.debug("Clients accepts media type: {}", mt.toString());
        }

        TreeSet<MediaType> matchingTypes = new TreeSet<MediaType>();

        for(MediaType mt: clientMediaTypes){
            if(mt.getMimeType().equalsIgnoreCase(_normativeMediaType.getMimeType())){
                matchingTypes.add(mt);
                responderMap.put(mt,this);
            }
            else if(mt.getPrimaryType().equalsIgnoreCase(_normativeMediaType.getPrimaryType()) &&
                    mt.getSubType().equalsIgnoreCase("*")){
                matchingTypes.add(mt);
                responderMap.put(mt,this);
            }
            else if(mt.getPrimaryType().equalsIgnoreCase("*") &&
                    mt.getSubType().equalsIgnoreCase("*")){
                matchingTypes.add(mt);
                responderMap.put(mt,this);
            }

            for(Dap4Responder altRepResponder : getAltRepResponders()){

                MediaType altType = altRepResponder.getNormativeMediaType();
                if(mt.getMimeType().equalsIgnoreCase(altType.getMimeType())){
                    matchingTypes.add(mt);
                    responderMap.put(mt,altRepResponder);
                }
                else if(mt.getPrimaryType().equalsIgnoreCase(altType.getMimeType()) &&
                        mt.getSubType().equalsIgnoreCase("*")){
                    matchingTypes.add(mt);
                    responderMap.put(mt,altRepResponder);
                }
            }
        }

        if(matchingTypes.isEmpty()){
            return null;
        }

        MediaType bestType = matchingTypes.last();
        Dap4Responder bestResponder = responderMap.get(bestType);

        _log.debug("Best Matching Type:  {}", bestType);
        _log.debug("Worst Matching Type: {}", matchingTypes.first());
        _log.debug("Best Responder:      {}", bestResponder.getClass().getName());

        return bestResponder;

    }





    @Override
    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        _log.debug("respondToHttpGetRequest() - Checking Last-Modified header...");

        if (!response.containsHeader("Last-Modified")) {
            _log.debug("respondToHttpGetRequest() - Last-Modified header has not been set. Setting...");

            Date lmt = new Date(getLastModified(request));
            //Date lmt = new Date((long)-1);
            SimpleDateFormat httpDateFormat = new SimpleDateFormat(HttpDatFormatString);

            response.setHeader("Last-Modified",httpDateFormat.format(lmt));

            _log.debug("respondToHttpGetRequest() - Last-Modified: {}", httpDateFormat.format(lmt));


        } else {
            _log.debug("respondToHttpGetRequest() - Last-Modified header has already been set.");

        }


        String relativeUrl = ReqInfo.getLocalUrl(request);

        for(Dap4Responder altResponder: getAltRepResponders()){

            Pattern p = altResponder.getRequestSuffixMatchPattern();

            if(Util.matchesSuffixPattern(relativeUrl, p)){
                altResponder.respondToHttpGetRequest(request,response);
                return;
            }
        }

        boolean regexMatch = Util.matchesSuffixPattern(relativeUrl,getRequestSuffixMatchPattern());
        if(regexMatch){
            _log.debug("requestedResourceId matches RequestSuffixMatchPattern: {}", regexMatch);
            Dap4Responder targetResponder = getBestResponderForHttpRequest(request);

            if(targetResponder==null){
                //If an Accept header field is present, and if the server cannot send a response
                // which is acceptable according to the combined Accept field value, then the server
                // SHOULD send a 406 (not acceptable) response.

                String msg = "Server-driven content negotiation failed. Returning status 406. Client request 'Accept: "+ Scrub.urlContent(request.getHeader("Accept"))+"'";
                _log.error("respondToHttpGetRequest() - {} ", msg);
                throw new NotAcceptable(msg);
            }
            _log.debug("respondToHttpGetRequest() - Target Responder: {} normative media-type: {}", targetResponder.getClass().getName(), targetResponder.getNormativeMediaType());

            targetResponder.sendNormativeRepresentation(request,response);
            return;
        }
        String msg ="Something Bad Happened. Unable to respond to request for : '" + Scrub.urlContent(relativeUrl) + "'";
        _log.error("respondToHttpGetRequest() - {}",msg);
        throw new opendap.http.error.InternalError(msg);

    }


    /**
     *
     * @param requestedResourceId
     * @return
     */
    @Override
    public boolean matches(String requestedResourceId, boolean checkWithBes) {

        String resourceID = getResourceId(requestedResourceId,checkWithBes);

        boolean result =  resourceID != null;

        return result;

    }
    public String getResourceId(String requestedResource, boolean checkWithBes){

        Pattern suffixPattern = Pattern.compile(_combinedRequestSuffixRegex, Pattern.CASE_INSENSITIVE);
        return getBesApi().getBesDataSourceID(requestedResource, suffixPattern, checkWithBes);

    }


    @Override
    public String getXmlBase(HttpServletRequest req){

        String requestUrl = ReqInfo.getRequestUrlPath(req);
        String xmlBase = Util.dropSuffixFrom(requestUrl, Pattern.compile(getCombinedRequestSuffixRegex()));
        _log.debug("getXmlBase(): @xml:base='{}'", xmlBase);
        return xmlBase;
    }


    @Override
    public long getLastModified(HttpServletRequest request) throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = getResourceId(relativeUrl,true);

        _log.debug("getLastModified(): Determining LastModified time for resource {}", dataSource);

        ResourceInfo ri = getResourceInfo(dataSource);
        return ri.lastModified();

    }






    public Element getServiceElement(String datasetUrl){


        Element service = getServiceElement();

        Element link = getNormativeLink(datasetUrl);

        service.addContent(link);


        for(Dap4Responder altRepResponder: getAltRepResponders()){
            MediaType altMediaType = altRepResponder.getNormativeMediaType();
            String href = datasetUrl + getNormativeMediaType().getMediaSuffix()+altMediaType.getMediaSuffix();
            link = getLinkElement(altMediaType.getMimeType(),href,altRepResponder.getServiceDescription());
            service.addContent(link);
        }

        return service;

    }

    public Element getNormativeLink(String datasetUrl){

        String href = datasetUrl + getNormativeMediaType().getMediaSuffix();

        Element link = getLinkElement(getNormativeMediaType().getMimeType(),href,"The normative form of the "+getServiceTitle());

        Element alt;
        for(Dap4Responder altRepResponder: getAltRepResponders()){
            alt =  new Element("alt",DAP.DAPv40_DatasetServices_NS);
            alt.setAttribute("type",altRepResponder.getNormativeMediaType().getMimeType());
            link.addContent(alt);
        }

        return link;

    }

    public Element getLinkElement(String mediaType, String href, String description ){

        Element link = new Element("link",DAP.DAPv40_DatasetServices_NS);
        link.setAttribute("type",mediaType);
        link.setAttribute("href",href);
        if(description!=null && !description.equals(""))
            link.setAttribute("description",description);

        return link;
    }



    public Element getServiceElement(){
        return getServiceElement(getServiceTitle(),getServiceRoleId(),getServiceDescription(),getServiceDescriptionLink());
    }

    public Element getServiceElement(String title, String role, String descriptionText, String descriptionLink){
        Element service = new Element("Service",DAP.DAPv40_DatasetServices_NS);
        service.setAttribute("title",title);
        service.setAttribute("role",role);
        Element description = getDescriptionElement(descriptionText, descriptionLink);
        if(description!=null)
            service.addContent(description);
        return service;
    }

    public Element getDescriptionElement(String descriptionText, String descriptionLink){
        Element description=null;
        if(descriptionText!=null  ||  descriptionLink!=null){

            description = new org.jdom.Element("Description", DAP.DAPv40_DatasetServices_NS);

            if(descriptionLink!=null)
                description.setAttribute("href",descriptionLink);

            if(descriptionText!=null)
                description.setText(descriptionText);
        }
        return description;
    }


    /**
     * If addTypeSuffixToDownloadFilename() is true, append the value of
     * getRequestSuffix() to the name.
     *
     * {@inheritDoc}
     */
    @Override
    public String getDownloadFileName(String resourceID){
        String name = super.getDownloadFileName(resourceID);

        // old rule: add the suffix - there was no option
        // old-new rule: if addTypeSuffixToDownloadFilename() is true, append getRequestSuffix().
        // new rule: if addType...() is true, then look at 'name' and do one of the following:
        //              file.<ext>: remove '.<ext>' and append the value of getRequestSuffix()
        //              file [no ext at all]: append getRequestSuffix()
        //           else if addType...() is not true, provide the old behavior
        // Assume that all <ext> are no more than three characters long (some are, but this is
        // a reasonable compromise).

        if(addTypeSuffixToDownloadFilename()) {
        	int dotPos = name.lastIndexOf('.');	// -1 if '.' not found
        	int extLength = name.length() - (dotPos + 1);

        	if (dotPos != -1 && (extLength > 0 && extLength < 4)) {
        		name = name.substring(0, dotPos);
        	}
        }

        name += getRequestSuffix();

        return name;
    }




    public abstract void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception;




}


