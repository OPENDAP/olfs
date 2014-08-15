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

package opendap.async;

import opendap.bes.BES;
import opendap.bes.BESConfig;
import opendap.bes.BESManager;
import opendap.bes.dap2Responders.BesApi;
import opendap.bes.dap2Responders.DapDispatcher;
import opendap.coreServlet.ReqInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IsoDispatchHandler for ISO responses from Hyrax
 */
public class AsyncDispatcher extends DapDispatcher {



    private Logger log;
    private boolean initialized;
    private boolean usePendingAndGoneResponses;
    private ReentrantLock cacheLock;

    private ConcurrentHashMap<String,Date> asyncCache;

    private String _prefix = "async/";

    private int cachePersistTime; // In milliseconds
    private int responseDelay; // In milliseconds


    //private String dapMetadataRegex = ".*\\.xml|.*\\.iso|.*\\.rubric|.*\\.ver|.*\\.ddx|.*\\.dds|.*\\.das|.*\\.info|.*\\.html?";
    //private Pattern dapMetadataPattern = Pattern.compile(dapMetadataRegex, Pattern.CASE_INSENSITIVE);

    private String dap4DataRegex;
    private Pattern dap4DataPattern;

    private String dap2DataRegex;
    private Pattern dap2DataPattern;





    public AsyncDispatcher(){
        log = LoggerFactory.getLogger(getClass());

        asyncCache = new ConcurrentHashMap<String, Date>();
        cacheLock = new ReentrantLock();

        cachePersistTime = 3600000; // In milliseconds
        responseDelay    = 60000;   // In milliseconds

        usePendingAndGoneResponses = true;

        dap4DataRegex = "(.*\\.dap((\\.xml)|(\\.csv)|(\\.nc))?$)";
        dap4DataPattern = Pattern.compile(dap4DataRegex, Pattern.CASE_INSENSITIVE);

        dap2DataRegex = ".*\\.dods|.*\\.asc(ii)?";
        dap2DataPattern = Pattern.compile(dap2DataRegex, Pattern.CASE_INSENSITIVE);

        initialized = false;
    }


    @Override
    public void init(HttpServlet servlet,Element config) throws Exception {


        if(initialized) return;

        BesApi besApi = new BesApi();

        init(servlet, config ,besApi);

        ingestPrefix(config);
        ingestCachePersistTime(config);
        ingestResponseDelay(config);
        ingestUsePendingGone(config);


        // What follows is a hack to get a particular BES, the BES with prefix, to service these requests.
        //
        // We know the that BESManager.getConfig() returns a clone of the config so that we can abuse
        // it as we see fit.
        Element besManagerConfig = BESManager.getConfig();
        List besList = besManagerConfig.getChildren("BES");

        BES bes;
        BESConfig besConfig;
        Element besConfigElement;
        for (Object o : besList) {
            besConfigElement = (Element) o;

            Element prefixElement = besConfigElement.getChild("prefix");
            String prefix = null;
            if(prefixElement!=null)
                prefix = prefixElement.getTextTrim();

            // Find the BESs whose prefix is "/" note that there may be more than one
            // And that's fine. This will just cause the BESManager to form another BESGroup with
            // the new prefix.
            if(prefix!=null && prefix.equals("/")){

                // Change the prefix to be the prefix for our async responder.
                prefixElement.setText(_prefix);

                besConfig = new BESConfig(besConfigElement);

                // Add the new BES to the BESManager
                bes = new BES(besConfig);
                BESManager.addBes(bes);
                log.info("Added BES to service asynchronous responses. BES prefix: '"+_prefix+"'");


                initialized = true;
            }
        }


    }

    private void ingestPrefix(Element config) throws Exception{

        String msg;
        Element e = config.getChild("prefix");
        if(e!=null)
            _prefix = e.getTextTrim();

        if(_prefix.equals("/")){
            msg = "Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <prefix>  " +
                    "child element whose value may not be equal to \"/\"";
            log.error(msg);
            throw new Exception(msg);
        }



        if(_prefix.endsWith("/"))
            _prefix = _prefix.substring(0,_prefix.length()-1);

        if(!_prefix.startsWith("/"))
            _prefix = "/" + _prefix;

        log.info("prefix="+ _prefix);

    }

    private void ingestCachePersistTime(Element config) throws Exception{

        String msg;
        Element e = config.getChild("cachePersistTime");


        if(e!=null)
            cachePersistTime = Integer.parseInt(e.getTextTrim()) * 1000; // Make it into milliseconds

        if(cachePersistTime < 0){
            msg = "Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide a <cachePersistTime>  " +
                    "child element whose value may not be less than 0";
            log.error(msg);
            throw new Exception(msg);
        }
        log.info("cachePersistTime="+ cachePersistTime);

    }



    private void ingestUsePendingGone(Element config) throws Exception{

        String msg;

        Element e = config.getChild("usePendingAndGoneResponses");


        if(e!=null)
            usePendingAndGoneResponses = e.getTextTrim().equalsIgnoreCase("true");

        log.info("usePendingAndGoneResponses="+ usePendingAndGoneResponses);

    }



    private void ingestResponseDelay(Element config) throws Exception{

        String msg;

        Element e = config.getChild("responseDelay");


        if(e!=null)
            responseDelay = Integer.parseInt(e.getTextTrim()) * 1000; // Make it into milliseconds

        if(responseDelay < 0){
            msg = "Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide a <responseDelay>  " +
                    "child element whose value may not be less than 0";
            log.error(msg);
            throw new Exception(msg);
        }
        log.info("responseDelay="+ responseDelay);

    }




    @Override
    public boolean requestDispatch(HttpServletRequest request,
                                    HttpServletResponse response,
                                    boolean sendResponse)
            throws Exception {



        String serviceContext = ReqInfo.getFullServiceContext(request);
        String relativeURL = ReqInfo.getLocalUrl(request);

        log.debug("The client requested this resource: " + relativeURL);

        log.debug("serviceContext: "+serviceContext);
        log.debug("relativeURL:    "+relativeURL);

        if(!relativeURL.startsWith("/"))
            relativeURL = "/" + relativeURL;



        boolean isMyRequest = relativeURL.startsWith(_prefix);



        if (isMyRequest) {
            if(sendResponse){
                return(sendAsyncResponse(request, response));
            }
            else {
                return(super.requestDispatch(request, response, false));
            }
        }

        return isMyRequest;

    }




    public boolean sendAsyncResponse(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {

        log.info("Sending Asynchronous Response");

        // If it's a DAP4 Data request then do the asynchronous dance.
        if(isDap4DataRequest(request))
            return(asyncDap4DataResponse(request, response));

        // If it's a DAP2 Data request then do the asynchronous dance.
        else if(isDap2DataRequest(request))
            return(asyncDap2DataResponse(request, response));

        return(super.requestDispatch(request,response,true));
    }



    public boolean isDap4DataRequest(HttpServletRequest req){
        String relativeURL = ReqInfo.getLocalUrl(req);
        Matcher m = dap4DataPattern.matcher(relativeURL);
        return m.matches();
    }

    public boolean isDap2DataRequest(HttpServletRequest req){
        String relativeURL = ReqInfo.getLocalUrl(req);
        Matcher m = dap2DataPattern.matcher(relativeURL);
        return m.matches();
    }


    /**
     *
     * @param request The request to evaluate for the client headers and CE syntax
     * @return  Returns -1 if the client has not indicated that -1 if it can accept an asynchronous response.
     * Returns 0 if the client has indicated that it will accept any length delay. A return value greater than
     * 0 indicates the time, in milliseconds, that client is willing to wait for a response.
     */
    public long getClientAsyncAcceptVal_ms(HttpServletRequest request){

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
                ceAcceptDelay = Long.parseLong(ceAsyncAccept[0])*1000; // Value comes as seconds, make it milliseconds
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
                    headerAcceptDelay = Long.parseLong(v.get(0))*1000; // Value comes as seconds, make it milliseconds
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




    public boolean asyncDap2DataResponse(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Date now = new Date();

        Date timeAvailable;

        String xmlBase = DocFactory.getXmlBase(request);

        timeAvailable = addResourceToCacheAsNeeded(xmlBase);

        long timeTillReady = timeAvailable.getTime() - now.getTime();

        if(timeTillReady>0){
            log.info("Delaying DAP2 data request for "+timeTillReady+"ms");
            try { Thread.sleep(timeTillReady);}
            catch(InterruptedException e){ log.error("Thread Interrupted. msg: "+e.getMessage());}
        }

        return(super.requestDispatch(request,response,true));


    }


    /**
     *
     * @param id  The resource ID
     * @return  The time at which the resource will be available.
     */
    private Date addResourceToCacheAsNeeded(String id){

        Date now = new Date();
        Date timeAvailable;

        timeAvailable = new Date(now.getTime()+ getResponseDelay_ms());

        Date resourceAvailabilityTime = asyncCache.putIfAbsent(id,timeAvailable);

        if(resourceAvailabilityTime != null)
            return resourceAvailabilityTime;

        return timeAvailable;

    }




    public boolean asyncDap4DataResponse(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Date now = new Date();
        Date startTime;
        String xmlBase = DocFactory.getXmlBase(request);


        long clientAcceptableDelay_ms = getClientAsyncAcceptVal_ms(request);

        if(clientAcceptableDelay_ms<0){
            // There was no indication that the client can accept an asynchronous response, so tell that
            // it's required that they indicate their willingness to accept async in order to get the resource.
            Document asyncResponse = DocFactory.getAsynchronousResponseRequired(request, getResponseDelay_s(), getCachePersistTime_s());
            response.setHeader(HttpHeaders.ASYNC_REQUIRED, "");
            sendDocument(response, asyncResponse, HttpServletResponse.SC_BAD_REQUEST);
            return true;
        }
        else if(clientAcceptableDelay_ms>0 && clientAcceptableDelay_ms < getResponseDelay_ms()){
            // The client indicated that amount of time that they are willing to wait for the
            // asynchronous response is less than the expected delay.
            // So - tell them that the request is REJECTED!
            Document asyncResponse = DocFactory.getAsynchronousResponseRejected(request,
                    DocFactory.reasonCode.TIME,
                    "Acceptable access delay was less than estimated delay.");
            sendDocument(response, asyncResponse, HttpServletResponse.SC_PRECONDITION_FAILED);
            return true;

        }
        else {

            // Looks like the client wants an async response!

            // First, let's figure out if the request is for a pending, or expired resource.
            startTime = asyncCache.get(xmlBase);
            if(startTime!=null) {

                Date endTime = new Date(startTime.getTime()+cachePersistTime);

                if(now.after(startTime)){
                    if(now.before(endTime) ){
                        // The request if for an available resource. I.e. The requested resource is in the cache,
                        // it's start has past and it's end time has not yet arrived. So - send the data.

                        // And because this is hack to add DAP4 functionality to a DAP2 server, make the request
                        // palatable to the underlying DAP2 service.
                        Dap4RequestToDap2Request dap2Request = new Dap4RequestToDap2Request(request);
                        return(super.requestDispatch(dap2Request,response,true));
                    }
                    else if(now.after(endTime)){


                        if(usePendingAndGoneResponses){
                            // Async Response is expired. Return GONE!
                            Document asyncResponse = DocFactory.getAsynchronousResponseGone(request);
                            sendDocument(response, asyncResponse, HttpServletResponse.SC_GONE);
                        }
                        else {
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        }
                        asyncCache.remove(xmlBase, startTime);

                        return true;
                    }
                }
                else {
                    if(usePendingAndGoneResponses){
                        // Async Response is PENDING!
                        Document asyncResponse = DocFactory.getAsynchronousResponsePending(request);
                        sendDocument(response, asyncResponse, HttpServletResponse.SC_CONFLICT);
                    }
                    else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                    return true;

                }
            }

            // The resource is not in the cache, so add the resource to the cache. and then
            // tell the client that the request is accepted.
            addResourceToCacheAsNeeded(xmlBase);
            sendAsyncRequestAccepted(request,response);

            return true;
        }

    }

    public void sendAsyncRequestAccepted(HttpServletRequest request,
                                          HttpServletResponse response)
            throws IOException {


        // Async Request is accepted.
        response.setHeader(HttpHeaders.ASYNC_ACCEPTED, getResponseDelay_s()+"");

        String requestUrl = request.getRequestURL().toString();
        String ce = request.getQueryString();

        if(addAsyncParameterToCE(request))
            ce="async="+ getClientAsyncAcceptVal_ms(request) + "&" + ce;

        String resultLinkUrl = requestUrl + "?" + ce;

        Document asyncResponse =
                DocFactory.getAsynchronousResponseAccepted(
                        request,
                        resultLinkUrl,
                        getResponseDelay_s(),
                        getCachePersistTime_s());


        sendDocument(response, asyncResponse, HttpServletResponse.SC_ACCEPTED);
    }


    public static void sendDocument(HttpServletResponse response, Document doc, int httpStatus) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        response.setStatus(httpStatus);
        response.setContentType("text/xml");
        response.getOutputStream().print(xmlo.outputString(doc));
    }






    private int getResponseDelay_s(){
        return responseDelay/1000;
    }


    private int getCachePersistTime_s(){
        return cachePersistTime/1000;
    }


    private int getResponseDelay_ms(){
        return responseDelay;
    }


    private int getCachePersistTime_ms(){
        return cachePersistTime;
    }





    class Dap4RequestToDap2Request implements HttpServletRequest {

        HttpServletRequest r;

        public Dap4RequestToDap2Request(HttpServletRequest request) {
            r = request;
        }


        
        public String getAuthType() { return r.getAuthType(); }

        public Cookie[] getCookies() {  return r.getCookies(); }

        public long getDateHeader(String s) { return r.getDateHeader(s); }

        public String getHeader(String s) { return r.getHeader(s); }

        public Enumeration getHeaders(String s) { return r.getHeaders(s); }

        public Enumeration getHeaderNames() { return r.getHeaderNames(); }

        public int getIntHeader(String s) { return r.getIntHeader(s); }

        public String getMethod() { return r.getMethod(); }

        public String getPathInfo() { return r.getPathInfo(); }

        public String getPathTranslated() { return r.getPathTranslated(); }

        public String getContextPath() { return r.getContextPath(); }

        public String getQueryString() {

            String query = r.getQueryString();

            log.debug("dap4 query: "+query);

            String dap2Query = convertDap4ceToDap2ce(r);

            log.debug("dap2 query: "+dap2Query);

            return dap2Query;
        }


        /**
         * This prunes the async control by simply ignoring it.
         * Since the DAP4 query string utilizes a regular KVP structure with the DAP4 projection held
         * in a parameter/key named "proj" and the DAP4 selection held in a series of zero or more
         * instances of the parameter/key named "sel". <br/>
         * This gives us the ability to:
         * <ul>
         *     <li>Quickly extract the projection and selection.</li>
         *     <li>utilize other key/parameter names for other purposes.</li>
         * </ul>
         * The async parameter is an example of one of these other keys.
         * @param req The request to use as the source of the DAP4 constraint expression
         * @return A DAP2 constraint expression
         */
        public String convertDap4ceToDap2ce(HttpServletRequest req){

            StringBuilder dap2Query   = new StringBuilder();
            String projection         = req.getParameter("proj");
            String[] selectionClauses = req.getParameterValues("sel");



            if(projection!=null)
                dap2Query.append(projection);

            if(selectionClauses!=null){
                for(String selClause:selectionClauses){
                    if(dap2Query.length()>0)
                        dap2Query.append("&");
                    dap2Query.append(selClause);
                }
            }

            return dap2Query.toString();
        }


        
        public String getRemoteUser() { return r.getRemoteUser(); }

        public boolean isUserInRole(String s) { return r.isUserInRole(s); }

        public Principal getUserPrincipal() { return r.getUserPrincipal(); }

        public String getRequestedSessionId() { return r.getRequestedSessionId(); }

        public String getRequestURI() { return r.getRequestURI(); }

        public StringBuffer getRequestURL() { return r.getRequestURL(); }

        public String getServletPath() { return r.getServletPath(); }

        public HttpSession getSession(boolean b) { return r.getSession(b); }

        public HttpSession getSession() { return r.getSession(); }

        public boolean isRequestedSessionIdValid() { return r.isRequestedSessionIdValid(); }

        public boolean isRequestedSessionIdFromCookie() { return r.isRequestedSessionIdFromCookie(); }

        public boolean isRequestedSessionIdFromURL() { return r.isRequestedSessionIdFromURL(); }

        @Deprecated
        public boolean isRequestedSessionIdFromUrl() { return r.isRequestedSessionIdFromUrl(); }

        public Object getAttribute(String s) { return r.getAttribute(s); }

        public Enumeration getAttributeNames() { return r.getAttributeNames(); }

        public String getCharacterEncoding() { return r.getCharacterEncoding(); }

        public void setCharacterEncoding(String s) throws UnsupportedEncodingException { r.setCharacterEncoding(s); }

        public int getContentLength() { return r.getContentLength(); }

        public String getContentType() { return r.getContentType(); }

        public ServletInputStream getInputStream() throws IOException { return r.getInputStream(); }

        public String getParameter(String s) { return r.getParameter(s); }

        public Enumeration getParameterNames() { return r.getParameterNames(); }

        public String[] getParameterValues(String s) { return r.getParameterValues(s); }

        public Map getParameterMap() { return r.getParameterMap(); }

        public String getProtocol() { return r.getProtocol(); }

        public String getScheme() { return r.getScheme(); }

        public String getServerName() { return r.getServerName(); }

        public int getServerPort() { return r.getServerPort(); }

        public BufferedReader getReader() throws IOException { return r.getReader(); }

        public String getRemoteAddr() { return r.getRemoteAddr(); }

        public String getRemoteHost() { return r.getRemoteHost(); }

        public void setAttribute(String s, Object o) { r.setAttribute(s, o); }

        public void removeAttribute(String s) { r.removeAttribute(s); }

        public Locale getLocale() { return r.getLocale(); }

        public Enumeration getLocales() { return r.getLocales(); }

        public boolean isSecure() { return r.isSecure(); }

        public RequestDispatcher getRequestDispatcher(String s) { return r.getRequestDispatcher(s); }

        @Deprecated
        public String getRealPath(String s) { return r.getRealPath(s); }

        public int getRemotePort() { return r.getRemotePort(); }

        public String getLocalName() { return r.getLocalName(); }

        public String getLocalAddr() { return r.getLocalAddr(); }

        public int getLocalPort() { return r.getLocalPort(); }
    }


}
