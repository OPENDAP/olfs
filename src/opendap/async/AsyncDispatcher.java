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

package opendap.async;

import opendap.bes.*;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.http.error.NotFound;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IsoDispatchHandler for ISO responses from Hyrax
 */
public class AsyncDispatcher extends BesDapDispatcher {



    private Logger log;
    private boolean initialized;
    private boolean usePendingAndGoneResponses;

    private ConcurrentHashMap<String,Date> asyncCache;

    private String prefix = "async/";

    private int cachePersistTime; // In milliseconds
    private int responseDelay; // In milliseconds

    private String dap4DataRegex;
    private Pattern dap4DataPattern;

    private String dap2DataRegex;
    private Pattern dap2DataPattern;

    private static final String BAD_CONF_MSG_START = "Bad Configuration. The <Handler> element that declares ";





    public AsyncDispatcher(){
        log = LoggerFactory.getLogger(getClass());

        asyncCache = new ConcurrentHashMap<>();
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
            String configPrefix = null;
            if(prefixElement!=null)
                configPrefix = prefixElement.getTextTrim();

            // Find the BESs whose prefix is "/" note that there may be more than one
            // And that's fine. This will just cause the BESManager to form another BESGroup with
            // the new prefix.
            if(configPrefix!=null && configPrefix.equals("/")){

                // Change the prefix to be the prefix for our async responder.
                prefixElement.setText(this.prefix);

                besConfig = new BESConfig(besConfigElement);

                // Add the new BES to the BESManager
                bes = new BES(besConfig);
                BESManager.addBes(bes);
                log.info("Added BES to service asynchronous responses. BES prefix: '{}'", this.prefix );

                initialized = true;
            }
        }


    }


    private void ingestPrefix(Element config) throws BadConfigurationException{

        Element e = config.getChild("prefix");
        if(e!=null)
            prefix = e.getTextTrim();

        if(prefix.equals("/")){
            String msg = BAD_CONF_MSG_START + this.getClass().getName();
            msg += " MUST provide 1 <prefix> child element whose value may not be equal to \"/\"";
            log.error("ingestPrefix() - {}",msg);
            throw new BadConfigurationException(msg);
        }

        if(prefix.endsWith("/"))
            prefix = prefix.substring(0, prefix.length()-1);

        if(!prefix.startsWith("/"))
            prefix = "/" + prefix;

        log.info("prefix={}", prefix);
    }

    private void ingestCachePersistTime(Element config) throws BadConfigurationException{

        Element e = config.getChild("cachePersistTime");
        if(e!=null)
            cachePersistTime = Integer.parseInt(e.getTextTrim()) * 1000; // Make it into milliseconds

        if(cachePersistTime < 0){
            String msg = BAD_CONF_MSG_START + this.getClass().getName();
            msg += " MUST provide a <cachePersistTime> child element whose value may not be less than 0";
            log.error("ingestCachePersistTime() - {}",msg);
            throw new BadConfigurationException(msg);
        }
        log.info("cachePersistTime={}", cachePersistTime);

    }



    private void ingestUsePendingGone(Element config) {

        Element e = config.getChild("usePendingAndGoneResponses");
        if(e!=null)
            usePendingAndGoneResponses = e.getTextTrim().equalsIgnoreCase("true");

        log.info("usePendingAndGoneResponses={}",usePendingAndGoneResponses);
    }



    private void ingestResponseDelay(Element config) throws BadConfigurationException{

        Element e = config.getChild("responseDelay");
        if(e!=null)
            responseDelay = Integer.parseInt(e.getTextTrim()) * 1000; // Make it into milliseconds

        if(responseDelay < 0){
            String msg = BAD_CONF_MSG_START + this.getClass().getName();
            msg += " MUST provide a <responseDelay> child element whose value may not be less than 0";
            log.error("ingestResponseDelay() - {}",msg);
            throw new BadConfigurationException(msg);
        }
        log.info("responseDelay={}ms", responseDelay);
    }




    @Override
    public boolean requestDispatch(HttpServletRequest request,
                                    HttpServletResponse response,
                                    boolean sendResponse)
            throws Exception {

        String serviceContext = ReqInfo.getFullServiceContext(request);
        String relativeURL = ReqInfo.getLocalUrl(request);

        log.debug("The client requested this resource: {}", relativeURL);
        log.debug("serviceContext: {}",serviceContext);
        log.debug("relativeURL:    {}",relativeURL);

        if(!relativeURL.startsWith("/"))
            relativeURL = "/" + relativeURL;

        boolean isMyRequest = relativeURL.startsWith(prefix);
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
    public long getClientAsyncAcceptValMilliseconds(HttpServletRequest request){

        // Get the values of the "async" parameter in the query string.
        String[] ceAsyncAccept     = request.getParameterValues("async");


        // Get the values of the (possibly repeated) DAP Async Accept header
        Enumeration enm = request.getHeaders(HttpHeaders.ASYNC_ACCEPT);
        ArrayList<String> asyncHeaders  = new ArrayList<>();
        while(enm.hasMoreElements())
            asyncHeaders.add((String)enm.nextElement());

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
        if(!asyncHeaders.isEmpty()){
                try {
                    // Only look at the first value.
                    headerAcceptDelay = Long.parseLong(asyncHeaders.get(0))*1000; // Value comes as seconds, make it milliseconds
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
        ArrayList<String> acceptAsyncHeaderValues  = new ArrayList<>();
        while(enm.hasMoreElements())
            acceptAsyncHeaderValues.add((String)enm.nextElement());


        return ceAsyncAccept==null && !acceptAsyncHeaderValues.isEmpty();
    }




    public boolean asyncDap2DataResponse(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Date now = new Date();
        Date timeAvailable;
        String xmlBase = DocFactory.getXmlBase(request);
        timeAvailable = addResourceToCacheAsNeeded(xmlBase);

        long timeTillReady = timeAvailable.getTime() - now.getTime();

        if(timeTillReady>0){
            log.info("Delaying DAP2 data request for {}ms",timeTillReady);
            try { Thread.sleep(timeTillReady);}
            catch(InterruptedException e){
                log.error("Thread Interrupted. msg: {}",e.getMessage());
                throw e;
            }
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

        timeAvailable = new Date(now.getTime()+ getResponseDelayMilliseconds());
        Date resourceAvailabilityTime = asyncCache.putIfAbsent(id,timeAvailable);

        if(resourceAvailabilityTime != null)
            return resourceAvailabilityTime;

        return timeAvailable;
    }




    public boolean asyncDap4DataResponse(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Date now = new Date();
        Date startTime;
        String xmlBase = DocFactory.getXmlBase(request);

        long clientAcceptableDelayMs = getClientAsyncAcceptValMilliseconds(request);

        if(clientAcceptableDelayMs<0){
            // There was no indication that the client can accept an asynchronous response, so tell that
            // it's required that they indicate their willingness to accept async in order to get the resource.
            Document asyncResponse = DocFactory.getAsynchronousResponseRequired(request, getResponseDelaySeconds(), getCachePersistTimeSeconds());
            response.setHeader(HttpHeaders.ASYNC_REQUIRED, "");
            sendDocument(response, asyncResponse, HttpServletResponse.SC_BAD_REQUEST);
            return true;
        }

        if(clientAcceptableDelayMs>0 && clientAcceptableDelayMs < getResponseDelayMilliseconds()){
            // The client indicated that amount of time that they are willing to wait for the
            // asynchronous response is less than the expected delay.
            // So - tell them that the request is REJECTED!
            Document asyncResponse = DocFactory.getAsynchronousResponseRejected(request,
                    DocFactory.reasonCode.TIME,
                    "Acceptable access delay was less than estimated delay.");
            sendDocument(response, asyncResponse, HttpServletResponse.SC_PRECONDITION_FAILED);
            return true;

        }

        // Looks like the client wants an async response!

        // First, let's figure out if the request is for a pending, or expired resource.
        startTime = asyncCache.get(xmlBase);
        if(startTime!=null) {

            Date endTime = new Date(startTime.getTime()+cachePersistTime);
            if(now.after(startTime)){
                if(now.before(endTime) ){
                    // The request is for an available resource. I.e. The requested resource is in the cache,
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
                        asyncCache.remove(xmlBase, startTime);
                    }
                    else {
                        asyncCache.remove(xmlBase, startTime);
                        throw new NotFound("The requested resource is no longer available.");
                    }

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
                    throw new NotFound("The requested resource is not yet available.");
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

    public void sendAsyncRequestAccepted(HttpServletRequest request,
                                          HttpServletResponse response)
            throws IOException {


        // Async Request is accepted.
        response.setHeader(HttpHeaders.ASYNC_ACCEPTED, getResponseDelaySeconds()+"");

        String requestUrl = request.getRequestURL().toString();
        String ce = request.getQueryString();

        if(addAsyncParameterToCE(request))
            ce="async="+ getClientAsyncAcceptValMilliseconds(request) + "&" + ce;

        String resultLinkUrl = requestUrl + "?" + ce;

        Document asyncResponse =
                DocFactory.getAsynchronousResponseAccepted(
                        request,
                        resultLinkUrl,
                        getResponseDelaySeconds(),
                        getCachePersistTimeSeconds());


        sendDocument(response, asyncResponse, HttpServletResponse.SC_ACCEPTED);
    }


    public static void sendDocument(HttpServletResponse response, Document doc, int httpStatus) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        response.setStatus(httpStatus);
        response.setContentType("text/xml");
        response.getOutputStream().print(xmlo.outputString(doc));
    }






    private int getResponseDelaySeconds(){
        return responseDelay/1000;
    }


    private int getCachePersistTimeSeconds(){
        return cachePersistTime/1000;
    }


    private int getResponseDelayMilliseconds(){
        return responseDelay;
    }



    class Dap4RequestToDap2Request implements HttpServletRequest {

        HttpServletRequest wrappedRequest;

        public Dap4RequestToDap2Request(HttpServletRequest request) {
            wrappedRequest = request;
        }

        
        public String getAuthType() { return wrappedRequest.getAuthType(); }

        public Cookie[] getCookies() {  return wrappedRequest.getCookies(); }

        public long getDateHeader(String s) { return wrappedRequest.getDateHeader(s); }

        public String getHeader(String s) { return wrappedRequest.getHeader(s); }

        public Enumeration getHeaders(String s) { return wrappedRequest.getHeaders(s); }

        public Enumeration getHeaderNames() { return wrappedRequest.getHeaderNames(); }

        public int getIntHeader(String s) { return wrappedRequest.getIntHeader(s); }

        public String getMethod() { return wrappedRequest.getMethod(); }

        public String getPathInfo() { return wrappedRequest.getPathInfo(); }

        public String getPathTranslated() { return wrappedRequest.getPathTranslated(); }

        public String getContextPath() { return wrappedRequest.getContextPath(); }

        public String getQueryString() {

            String query = wrappedRequest.getQueryString();
            log.debug("getQueryString() - dap4 query: {}",query);

            String dap2Query = convertDap4ceToDap2ce(wrappedRequest);
            log.debug("getQueryString() - dap2 query: {}", dap2Query);

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


        
        public String getRemoteUser() { return wrappedRequest.getRemoteUser(); }

        public boolean isUserInRole(String s) { return wrappedRequest.isUserInRole(s); }

        public Principal getUserPrincipal() { return wrappedRequest.getUserPrincipal(); }

        public String getRequestURI() { return wrappedRequest.getRequestURI(); }

        public StringBuffer getRequestURL() { return wrappedRequest.getRequestURL(); }

        public String getServletPath() { return wrappedRequest.getServletPath(); }

        public HttpSession getSession(boolean b) { return wrappedRequest.getSession(b); }

        public HttpSession getSession() { return wrappedRequest.getSession(); }

        public boolean isRequestedSessionIdValid() { return wrappedRequest.isRequestedSessionIdValid(); }

        public boolean isRequestedSessionIdFromCookie() { return wrappedRequest.isRequestedSessionIdFromCookie(); }

        public boolean isRequestedSessionIdFromURL() { return wrappedRequest.isRequestedSessionIdFromURL(); }

        /**
         *
         * @return
         * @deprecated Not needed.
         */
        @Deprecated
        public boolean isRequestedSessionIdFromUrl() { return wrappedRequest.isRequestedSessionIdFromUrl(); }

        @Override
        public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
            return wrappedRequest.authenticate(httpServletResponse);
        }

        @Override
        public void login(String s, String s1) throws ServletException {

            wrappedRequest.login(s,s1);
        }

        @Override
        public void logout() throws ServletException {

            wrappedRequest.logout();
        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            return wrappedRequest.getParts();
        }

        @Override
        public Part getPart(String s) throws IOException, ServletException {
            return wrappedRequest.getPart(s);
        }
        /*
        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException
            return wrappedRequest.upgrade(aClass)

        @Override
        public String changeSessionId()
            return wrappedRequest.changeSessionId()

        @Override
        public long getContentLengthLong()
            return wrappedRequest.getContentLengthLong()

        */
        public String getRequestedSessionId() { return null; }

        public Object getAttribute(String s) { return wrappedRequest.getAttribute(s); }

        public Enumeration getAttributeNames() { return wrappedRequest.getAttributeNames(); }

        public String getCharacterEncoding() { return wrappedRequest.getCharacterEncoding(); }

        public void setCharacterEncoding(String s) throws UnsupportedEncodingException { wrappedRequest.setCharacterEncoding(s); }

        public int getContentLength() { return wrappedRequest.getContentLength(); }


        public String getContentType() { return wrappedRequest.getContentType(); }

        public ServletInputStream getInputStream() throws IOException { return wrappedRequest.getInputStream(); }

        public String getParameter(String s) { return wrappedRequest.getParameter(s); }

        public Enumeration getParameterNames() { return wrappedRequest.getParameterNames(); }

        public String[] getParameterValues(String s) { return wrappedRequest.getParameterValues(s); }

        public Map getParameterMap() { return wrappedRequest.getParameterMap(); }

        public String getProtocol() { return wrappedRequest.getProtocol(); }

        public String getScheme() { return wrappedRequest.getScheme(); }

        public String getServerName() { return wrappedRequest.getServerName(); }

        public int getServerPort() { return wrappedRequest.getServerPort(); }

        public BufferedReader getReader() throws IOException { return wrappedRequest.getReader(); }

        public String getRemoteAddr() { return wrappedRequest.getRemoteAddr(); }

        public String getRemoteHost() { return wrappedRequest.getRemoteHost(); }

        public void setAttribute(String s, Object o) { wrappedRequest.setAttribute(s, o); }

        public void removeAttribute(String s) { wrappedRequest.removeAttribute(s); }

        public Locale getLocale() { return wrappedRequest.getLocale(); }

        public Enumeration getLocales() { return wrappedRequest.getLocales(); }

        public boolean isSecure() { return wrappedRequest.isSecure(); }

        public RequestDispatcher getRequestDispatcher(String s) { return wrappedRequest.getRequestDispatcher(s); }

        /**
         *
         *
         * @param s
         * @return
         * @deprecated No longer used...
         */
        @Deprecated
        public String getRealPath(String s) { return wrappedRequest.getRealPath(s); }

        public int getRemotePort() { return wrappedRequest.getRemotePort(); }

        public String getLocalName() { return wrappedRequest.getLocalName(); }

        public String getLocalAddr() { return wrappedRequest.getLocalAddr(); }

        public int getLocalPort() { return wrappedRequest.getLocalPort(); }

        @Override
        public ServletContext getServletContext() {
            return wrappedRequest.getServletContext();
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return wrappedRequest.startAsync();
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
            return wrappedRequest.startAsync(servletRequest,servletResponse);
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return wrappedRequest.getAsyncContext();
        }

        @Override
        public DispatcherType getDispatcherType() {
            return wrappedRequest.getDispatcherType();
        }
    }


}
