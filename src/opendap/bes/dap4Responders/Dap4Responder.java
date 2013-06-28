/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.bes.dap4Responders;

import opendap.bes.BesDapResponder;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.Util;
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

    Logger log;

    private MediaType normativeMediaType;
    private Vector<Dap4Responder> altResponders;
    private String combinedRequestSuffixRegex;



    public Dap4Responder(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = LoggerFactory.getLogger(getClass().getName());
        altResponders =  new Vector<Dap4Responder>();
    }



    public void setNormativeMediaType(MediaType mt){
        normativeMediaType = mt;
        combinedRequestSuffixRegex = buildRequestMatchingRegex();
        log.debug("combinedRequestSuffixRegex: {}",combinedRequestSuffixRegex);
        setRequestMatchRegex(combinedRequestSuffixRegex);
    }


    public MediaType getNormativeMediaType(){
        return normativeMediaType;
    }

    public Dap4Responder[] getAltRepResponders(){
        Dap4Responder[] ars = new Dap4Responder[altResponders.size()];
        return altResponders.toArray(ars);
    }


    public void addAltRepResponder(Dap4Responder altRepResponder){
        altResponders.add(altRepResponder);
        combinedRequestSuffixRegex = buildRequestMatchingRegex();
        log.debug("combinedRequestSuffixRegex: {}",combinedRequestSuffixRegex);
        setRequestMatchRegex(combinedRequestSuffixRegex);
        for(Dap4Responder responder:altResponders){
            responder.combinedRequestSuffixRegex = combinedRequestSuffixRegex;
        }
    }


    public void clearAltResponders(){
        altResponders.clear();
    }



    public String getCombinedRequestSuffixRegex(){
        return combinedRequestSuffixRegex;
    }

    public void setCombinedRequestSuffixRegex(String regex){
        combinedRequestSuffixRegex = regex;
    }

    public String buildRequestMatchingRegex() {

        StringBuilder s = new StringBuilder();
        s.append(buildRequestMatchingRegexWorker(this));
        s.append("$");
        log.debug("Request Match Regex: {}", s.toString());
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

        log.debug("Accept: {}",acceptsHeaderValue);

        Vector<MediaType> clientMediaTypes = new Vector<MediaType>();

        if(acceptsHeaderValue!=null){
            String[] mimeTypes = acceptsHeaderValue.split(",");

            for(String mimeType: mimeTypes){
                clientMediaTypes.add(new ClientMediaType(mimeType));
            }
        }
        else {
            return this;
        }

        for(MediaType mt: clientMediaTypes){
            log.debug("Clients accepts media type: {}",mt.toString());
        }

        TreeSet<MediaType> matchingTypes = new TreeSet<MediaType>();

        for(MediaType mt: clientMediaTypes){
            if(mt.getMimeType().equalsIgnoreCase(normativeMediaType.getMimeType())){
                matchingTypes.add(mt);
                responderMap.put(mt,this);
            }
            else if(mt.getPrimaryType().equalsIgnoreCase(normativeMediaType.getPrimaryType()) &&
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

        log.debug("Best Matching Type:  {}", bestType);
        log.debug("Worst Matching Type: {}", matchingTypes.first());
        log.debug("Best Responder:      {}", bestResponder.getClass().getName());

        return bestResponder;

    }





    @Override
    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {


        log.debug("respondToHttpGetRequest() - Checking Last-Modified header...");

        if (!response.containsHeader("Last-Modified")) {
            log.debug("respondToHttpGetRequest() - Last-Modified header has not been set. Setting...");

            Date lmt = new Date(getLastModified(request));
            SimpleDateFormat httpDateFormat = new SimpleDateFormat(HttpDatFormatString);

            response.setHeader("Last-Modified",httpDateFormat.format(lmt));

            log.debug("respondToHttpGetRequest() - Last-Modified: {}",httpDateFormat.format(lmt));


        } else {
            String lastModified =  response.toString();
            log.debug("respondToHttpGetRequest() - Last-Modified header has already been set.");

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
            log.debug("requestedResourceId matches RequestSuffixMatchPattern: {}",regexMatch);
            Dap4Responder targetResponder = getBestResponderForHttpRequest(request);

            if(targetResponder==null){
                //If an Accept header field is present, and if the server cannot send a response
                // which is acceptable according to the combined Accept field value, then the server
                // SHOULD send a 406 (not acceptable) response.
                log.error("Server-driven content negotiation failed. Returning status 406. Client request 'Accept: {}'",
                        Scrub.urlContent(request.getHeader("Accept")));
                response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return;
            }
            log.debug("Target Responder: {} normative media-type: {}",targetResponder.getClass().getName(),targetResponder.getNormativeMediaType());

            targetResponder.sendNormativeRepresentation(request,response);
            return;
        }
        log.error("Something Bad Happened. Unable to respond to request for : {}'",Scrub.urlContent(relativeUrl));
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);



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

        Pattern suffixPattern = Pattern.compile(combinedRequestSuffixRegex, Pattern.CASE_INSENSITIVE);
        return getBesApi().getBesDataSourceID(requestedResource, suffixPattern, checkWithBes);

    }

    /*

    public String getResourceId(String requestedResource, boolean checkWithBes){

        Pattern suffixPattern = Pattern.compile(combinedRequestSuffixRegex, Pattern.CASE_INSENSITIVE);
        Matcher suffixMatcher = suffixPattern.matcher(requestedResource);

        boolean suffixMatched = false;


        while(!suffixMatcher.hitEnd()){
            suffixMatched = suffixMatcher.find();
            log.debug("{}", Util.checkRegex(suffixMatcher, suffixMatched));
        }

        String besDataSourceId = null;

        if(suffixMatched){
            int start =  suffixMatcher.start();
            besDataSourceId = requestedResource.substring(0,start);

            if(checkWithBes){
                log.debug("Asking BES about resource: {}", besDataSourceId);

                try {
                    ResourceInfo dsi = new BESResource(besDataSourceId, getBesApi());
                    if (!dsi.isDataset()) {
                        besDataSourceId = null;
                    }
                } catch (Exception e) {
                    log.debug("matches() failed with an Exception. Msg: '{}'", e.getMessage());
                }

            }
        }





        return besDataSourceId;
    }

    */

    @Override
    public String getXmlBase(HttpServletRequest req){

        String forwardRequestUri = (String)req.getAttribute("javax.servlet.forward.request_uri");
        String requestUrl = req.getRequestURL().toString();


        if(forwardRequestUri != null){
            String server = req.getServerName();
            int port = req.getServerPort();
            String scheme = req.getScheme();
            requestUrl = scheme + "://" + server + ":" + port + forwardRequestUri;
        }



        String xmlBase = Util.dropSuffixFrom(requestUrl, Pattern.compile(getCombinedRequestSuffixRegex()));



        log.debug("@xml:base='{}'",xmlBase);
        return xmlBase;
    }


    @Override
    public long getLastModified(HttpServletRequest request) throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = getResourceId(relativeUrl,true);

        log.debug("getLastModified(): Determining LastModified time for resource {}",dataSource );

        ResourceInfo ri = getResourceInfo(dataSource);
        return ri.lastModified();

    }








    @Override
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



    public abstract void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response)throws Exception;



}


