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

package opendap.aws.glacier;

import opendap.async.DocFactory;
import opendap.async.HttpHeaders;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap2Responders.DapDispatcher;
import opendap.coreServlet.ReqInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 9/24/13
 * Time: 1:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierDapDispatcher extends DapDispatcher{

    private Logger log;

    private GlacierBesApi _besApi;

    private long cachePersistTime;  // In seconds
    private long retrievalDelay;    // In seconds


    private boolean _initialized ;

    public GlacierDapDispatcher() {
        super();

        cachePersistTime = 8640000; // 100  days in seconds
        retrievalDelay   = 14400;   //   4 hours in seconds


        log = LoggerFactory.getLogger(this.getClass());
    }




    @Override
    public void init(HttpServlet servlet, Element config) throws Exception {

        if(_initialized)
            return;

        _besApi = new GlacierBesApi();
        init(servlet, config, _besApi);



        _initialized = true;
    }


    private long getCachePersistTime_s(){
        return cachePersistTime;
    }


    private long getResponseDelay(){
        return retrievalDelay;
    }





    @Override
    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        while(relativeUrl.startsWith("/") && relativeUrl.length()>1)
            relativeUrl = relativeUrl.substring(1);

        // String dataSource = getBesApi().getBesDataSourceID(relativeUrl, false);

        log.debug("The client requested this resource: {}",relativeUrl);

        for (Dap4Responder r : getResponders()) {
            log.debug("Checking responder: "+ r.getClass().getSimpleName()+ " (pathPrefix: "+r.getPathPrefix()+")");

            // see if the relative URL matches a responders regex, if so we'll check for it int he resource table.
            if (r.matches(relativeUrl,false)) {

                log.info("The relative URL: " + relativeUrl + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");


                String resourceId = r.removeRequestSuffixFromString(relativeUrl);
                log.debug("Requested resourceId: {}",resourceId);

                GlacierArchive gar = GlacierManager.theManager().getArchiveRecord(resourceId);


                if(gar!=null){

                    String ce = request.getQueryString();


                    if( (ce==null && r.isMetadataResponder()) ||  gar.resourceIsCached()){

                        // If the request is for a metadata object, or if the resource is cached we know we should
                        // be able process the request.



                        if (sendResponse){

                            conditionallySetLastModifiedTime(response,gar);

                            r.respondToHttpGetRequest(request, response);
                        }
                        return true;
                    }

                    // Looks like the resource needs to be retrieved from glacier. Since that is slow we need to
                    // engage the client in the async protocol dance in order to determine:
                    //  1) Is the requesting client able to utilize the async behavior?
                    //  2) Does the requesting client really want to initiate the retrieval?

                    long clientAcceptsAsyncDelay = getClientAsyncAcceptValAsSeconds(request);

                    if(clientAcceptsAsyncDelay<0){
                        // There was no indication that the client can accept an asynchronous response, so tell that
                        // it's required that they indicate their willingness to accept async in order to get the resource.
                        Document asyncResponse = DocFactory.getAsynchronousResponseRequired(request, getResponseDelay(), getCachePersistTime_s());
                        response.setHeader(HttpHeaders.ASYNC_REQUIRED, "");
                        sendDocument(response, asyncResponse, HttpServletResponse.SC_BAD_REQUEST);
                        return true;
                    }

                    if(clientAcceptsAsyncDelay>0 && clientAcceptsAsyncDelay < getResponseDelay()){
                        // The client indicated that amount of time that they are willing to wait for the
                        // asynchronous response is less than the expected delay.
                        // So - tell them that the request is REJECTED!
                        Document asyncResponse = DocFactory.getAsynchronousResponseRejected(request,
                                DocFactory.reasonCode.TIME,
                                "Acceptable access delay was less than estimated delay.");
                        sendDocument(response, asyncResponse, HttpServletResponse.SC_PRECONDITION_FAILED);
                        return true;

                    }

                    // Looks like the client wants to initiate an async retrieval of a glacier resource

                    // Let's see if it's a pending request
                    if(DownloadManager.theManager().alreadyRequested(gar)){
                        // It is already out there - go away and come back later.
                        Document asyncResponse = DocFactory.getAsynchronousResponsePending(request);
                        sendDocument(response, asyncResponse, HttpServletResponse.SC_CONFLICT);
                        return true;

                    }

                    long estimatedRetrievalTime = DownloadManager.theManager().initiateArchiveDownload(gar);

                    if(estimatedRetrievalTime == -1){

                        // @TODO Fix the error handling here.
                        // Request failed. Why?
                        // a) no such resource - which should never happen because
                        // we have an archive record for this resource so it SHOULD BE in glacier.
                        // b) Glacier communication issue.
                        // c) Any other AWS Glacier  or IOException,
                        // Thus: The evil 500 error should be returned. Or not?
                        // MAybe this should be an AsyncResponse with status=rejected and reason code=unavailable

                        // Probably should stop catching the exceptions below and let them propagate up...


                        sendDap4Error(response, "Something's borked in the Async Service", request.getContextPath()+"/", "");
                        return true;

                    }

                    sendAsyncRequestAccepted(request,response,estimatedRetrievalTime);

                    return true;

                }

            }
        }


        return false;

    }







    /**
     *
     * @param request The request to evaluate for the client headers and CE syntax
     * @return  Returns -1 if the client has not indicated that -1 if it can accept an asynchronous response.
     * Returns 0 if the client has indicated that it will accept any length delay. A return value greater than
     * 0 indicates the time, in milliseconds, that client is willing to wait for a response.
     */
    public long getClientAsyncAcceptValAsSeconds(HttpServletRequest request){

        // Get the values of the "async" parameter in the query string.
        String[] ceAsyncAccept     = request.getParameterValues("async");


        // Get the values of the (possibly repeated) DAP Async Accept header
        Enumeration enm = request.getHeaders(HttpHeaders.ASYNC_ACCEPT);
        Vector<String> v  = new Vector<String>();
        while(enm.hasMoreElements())
            v.add((String)enm.nextElement());


        long acceptableDelay;

        long headerAcceptDelay = -1;
        long ceAcceptDelay = -1;


        // Check the constraint expression for the async control parameter
        if(ceAsyncAccept!=null && ceAsyncAccept.length>0){


            try {
                // Only look at the first value.
                ceAcceptDelay = Long.parseLong(ceAsyncAccept[0]); // Value comes as seconds
            }
            catch(NumberFormatException e){
                log.error("Unable to ingest the value of the "+ HttpHeaders.ASYNC_ACCEPT+
                        " header. msg: "+e.getMessage());
                ceAcceptDelay = -1;
            }
        }

        // Check HTTP headers for Async Control
        if(v.size()>0){
                try {
                    // Only look at the first value.
                    headerAcceptDelay = Long.parseLong(v.get(0)); // Value comes as seconds
                }
                catch(NumberFormatException e){
                    log.error("Unable to ingest the value of the "+ HttpHeaders.ASYNC_ACCEPT+
                            " header. msg: "+e.getMessage());
                    headerAcceptDelay = -1;
                }

        }

        // The constraint expression parameter "asnyc=seconds" takes precedence.

        acceptableDelay = headerAcceptDelay;

        if(ceAcceptDelay>=0){
            acceptableDelay = ceAcceptDelay;
        }


        return acceptableDelay;
    }




    /**
     *
     * @param request The client request to evaluate to see if the target URL needs to have the async parameter added
     * the query string
     * @return  Returns -1 if the client has not indicated that -1 if it can accept an asynchronous response.
     * Returns 0 if the client has indicated that it will accept any length delay. A return value greater than
     * 0 indicates the time, in milliseconds, that client is willing to wait for a response.
     */
    public boolean addAsyncParameterToCE(HttpServletRequest request){

        // Get the values of the async parameter in the query string.
        String[] ceAsyncAccept     = request.getParameterValues("async");


        // Get the values of the (possibly repeated) DAP Async Accept header
        Enumeration enm = request.getHeaders(HttpHeaders.ASYNC_ACCEPT);
        Vector<String> acceptAsyncHeaderValues  = new Vector<String>();
        while(enm.hasMoreElements())
            acceptAsyncHeaderValues.add((String)enm.nextElement());


        return ceAsyncAccept==null && acceptAsyncHeaderValues.size()>0;
    }


    /**
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void sendAsyncRequestAccepted(HttpServletRequest request,
                                          HttpServletResponse response,
                                          long estimatedRetrievalTime)
            throws IOException {


        // Async Request is accepted.
        response.setHeader(HttpHeaders.ASYNC_ACCEPTED, getResponseDelay()+"");

        String requestUrl = request.getRequestURL().toString();
        String ce = request.getQueryString();

        if(addAsyncParameterToCE(request))
            ce="async="+ getClientAsyncAcceptValAsSeconds(request) + "&" + ce;

        String resultLinkUrl = requestUrl + "?" + ce;

        Document asyncResponse =
                DocFactory.getAsynchronousResponseAccepted(
                        request,
                        resultLinkUrl,
                        estimatedRetrievalTime,
                        getCachePersistTime_s());


        sendDocument(response, asyncResponse, HttpServletResponse.SC_ACCEPTED);
    }

    public static void sendDocument(HttpServletResponse response, Document doc, int httpStatus) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        response.setStatus(httpStatus);
        response.setContentType("text/xml");
        response.getOutputStream().print(xmlo.outputString(doc));
    }



    public void sendDap4Error( HttpServletResponse response, String message, String context, String otherInfo)
                throws IOException {


            Document error =
                    DocFactory.getDap4ErrorDocument(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message, context, otherInfo);


            sendDocument(response, error, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }




    public long getLastModified(HttpServletRequest req) {

        String relativeUrl = ReqInfo.getLocalUrl(req);


        if(!_initialized)
            return -1;


        for (Dap4Responder r : getResponders()) {
            if (r.matches(relativeUrl,false)) {
                log.info("The relative URL: " + relativeUrl + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                try {

                    long lmt =  r.getLastModified(req);
                    log.debug("getLastModified(): Returning: {}", new Date(lmt));
                    return lmt;

                } catch (Exception e) {
                    log.debug("getLastModified(): Returning: -1");
                    return -1;
                }

            }

        }

        return -1;


    }



    public void conditionallySetLastModifiedTime(HttpServletResponse response, GlacierArchive gar){

        log.debug("conditionallySetLastModifiedTime() - Checking Last-Modified header...");

        if (!response.containsHeader("Last-Modified")) {
            log.debug("conditionallySetLastModifiedTime() - Last-Modified header has not been set. Setting...");

            Date lmt = new Date(gar.getCachedResourceLastModifiedTime());
            //Date lmt = new Date((long)-1);
            SimpleDateFormat httpDateFormat = new SimpleDateFormat(Dap4Responder.HttpDatFormatString);

            response.setHeader("Last-Modified",httpDateFormat.format(lmt));

            log.debug("conditionallySetLastModifiedTime() - Last-Modified: {}",httpDateFormat.format(lmt));


        } else {
            log.debug("conditionallySetLastModifiedTime() - Last-Modified header has already been set.");

        }

    }


}
