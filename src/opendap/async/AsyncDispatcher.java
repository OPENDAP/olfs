/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.async;

import opendap.bes.BES;
import opendap.bes.BESConfig;
import opendap.bes.BESManager;
import opendap.bes.dapResponders.BesApi;
import opendap.bes.dapResponders.DapDispatcher;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IsoDispatchHandler for ISO responses from Hyrax
 */
public class AsyncDispatcher extends DapDispatcher {



    private Logger log;
    private boolean initialized;

    private ConcurrentHashMap<String,Date> asyncCache;

    private String _prefix = "async/";

    private int cachePersistTime; // In milliseconds
    private int responseDelay; // In milliseconds


    public AsyncDispatcher(){
        log = LoggerFactory.getLogger(getClass());

        asyncCache = new ConcurrentHashMap<String, Date>();

        cachePersistTime = 3600000; // In milliseconds
        responseDelay    = 60000;   // In milliseconds

        initialized = false;
    }


    @Override
    public void init(HttpServlet servlet,Element config) throws Exception {


        if(initialized) return;

        BesApi besApi = new BesApi();

        init(servlet, config ,besApi);

        ingestPrefix();
        ingestCachePersistTime();
        ingestResponseDelay();



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

    private void ingestPrefix() throws Exception{

        String msg;

        Element config = getConfig();

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

    private void ingestCachePersistTime() throws Exception{

        String msg;

        Element config = getConfig();

        Element e = config.getChild("cachePersistTime");


        if(e!=null)
            cachePersistTime = Integer.parseInt(e.getTextTrim());

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



    private void ingestResponseDelay() throws Exception{

        String msg;

        Element config = getConfig();

        Element e = config.getChild("responseDelay");


        if(e!=null)
            responseDelay = Integer.parseInt(e.getTextTrim());

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




    private String dap4DataRegex = ".*\\.dap|.*\\.xdods";
    private String dap4MetadataRegex = ".*\\.xml|.*\\.iso|.*\\.rubric|.*\\.ver|.*\\.ddx|.*\\.dds|.*\\.das|.*\\.info|.*\\.html?";
    private String dap2Regex = ".*\\.dods|.*\\.asc(ii)?";

    private String servicesRegex = dap4DataRegex + "|" + dap4MetadataRegex + "|" + dap2Regex;

    private Pattern dap4DataPattern = Pattern.compile(dap4DataRegex, Pattern.CASE_INSENSITIVE);
    private Pattern dap4MetadataPattern = Pattern.compile(dap4MetadataRegex, Pattern.CASE_INSENSITIVE);
    private Pattern dap2Pattern = Pattern.compile(dap2Regex, Pattern.CASE_INSENSITIVE);

    private Pattern servicesPattern = Pattern.compile(servicesRegex, Pattern.CASE_INSENSITIVE);



    public boolean sendAsyncResponse(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {

        log.info("Sending Asynchronous Response");



        String relativeURL = ReqInfo.getLocalUrl(request);

        Matcher m;

        m = dap4DataPattern.matcher(relativeURL);
        if(m.matches()){
            return(asyncResponse(request, response, false));
        }

        m = dap2Pattern.matcher(relativeURL);
        if(m.matches()){
            return(asyncResponse(request, response, true));
        }

        return(super.requestDispatch(request,response,true));


    }


    /**
     *
     * @param ceAsyncAccept The value (possibly null) of the "async=xx" term of the constraint expression
     * @param headerAsyncAccept The value (possibly null) of the X-DAP-Async-Accept HTTP header.
     * @return  Returns -1 if the client has not indicated that -1 if it can accept an asynchronous response.
     * Returns 0 if the client has indicated that it will accept any length delay. A return value greater than
     * 0 indicates the time, in milliseconds, that client is willing to wait for a response.
     */
    public long clientAcceptsAsync(String[] ceAsyncAccept, String[] headerAsyncAccept){

        long acceptableDelay;

        long headerAcceptDelay = -1;
        long ceAcceptDelay = -1;


        // Check the constraint expression for the async control
        // @todo Prune this parameter from the query String! OMFG!
        if(ceAsyncAccept!=null && ceAsyncAccept.length>0){


            try {
                // Only look at the first value.
                ceAcceptDelay = Long.parseLong(ceAsyncAccept[0]);
            }
            catch(NumberFormatException e){
                log.error("Unable to ingest the value of the "+ HttpHeaders.ASYNC_ACCEPT+
                        " header. msg: "+e.getMessage());
                ceAcceptDelay = -1;
            }
        }

        // Check HTTP headers for Async Control
        if(headerAsyncAccept!=null && headerAsyncAccept.length>0){

                try {
                    // Only look at the first value.
                    headerAcceptDelay = Long.parseLong(headerAsyncAccept[0]);
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







    public boolean asyncResponse(HttpServletRequest request, HttpServletResponse response, boolean isDap2Request) throws Exception {

        Date now = new Date();

        Date startTime;

        String xmlBase = DocFactory.getXmlBase(request);

        String[] ceAsyncAccept     = request.getParameterValues("async");
        Enumeration enm = request.getHeaders(HttpHeaders.ASYNC_ACCEPT);
        Vector<String> v  = new Vector<String>();
        while(enm.hasMoreElements()){
            v.add((String)enm.nextElement());
        }
        String[] headerAsyncAccept = new String[v.size()];

        headerAsyncAccept = v.toArray(headerAsyncAccept);

        long clientsAsyncVal = clientAcceptsAsync(ceAsyncAccept,headerAsyncAccept);

        if(clientsAsyncVal<0){
            Document asyncResponse = DocFactory.getAsynchronousResponseRequired(request, getResponseDelay(), getCachePersistTime());
            response.setHeader(HttpHeaders.ASYNC_REQUIRED, "");
            sendDocument(response, asyncResponse, HttpServletResponse.SC_BAD_REQUEST);
            return true;
        }
        else if(clientsAsyncVal>0 && clientsAsyncVal < getResponseDelay()){
            Document asyncResponse = DocFactory.getAsynchronousResponseRejected(request, DocFactory.reasonCode.TIME,"Acceptable access delay was less than estimated delay.");
            sendDocument(response, asyncResponse, HttpServletResponse.SC_PRECONDITION_FAILED);
            return true;

        }
        else {

            boolean cacheIsReady = false;

            if(asyncCache.containsKey(xmlBase)) {

                startTime = asyncCache.get(xmlBase);
                Date endTime = new Date(startTime.getTime()+cachePersistTime);

                if(now.after(startTime)){
                    if(now.before(endTime) ){
                        cacheIsReady = true;
                    }
                    else if(now.after(endTime)){
                        // Response is GONE!
                        Document asyncResponse = DocFactory.getAsynchronousResponseGone(request);
                        sendDocument(response, asyncResponse, HttpServletResponse.SC_GONE);
                        asyncCache.remove(xmlBase);
                        return true;
                    }
                }
                else {
                    // Response is PENDING!
                    Document asyncResponse = DocFactory.getAsynchronousResponsePending(request);
                    sendDocument(response, asyncResponse, HttpServletResponse.SC_CONFLICT);
                    return true;

                }
            }


            if(!asyncCache.containsKey(xmlBase)) {
                startTime = new Date(now.getTime()+getResponseDelay());
                asyncCache.put(xmlBase,startTime);
            }


            startTime = asyncCache.get(xmlBase);

            if(cacheIsReady){


                Dap4RequestToDap2Request myReq = new Dap4RequestToDap2Request(request);



                return(super.requestDispatch(myReq,response,true));
            }
            else {

                if(isDap2Request){

                    long timeTillReady = startTime.getTime() - now.getTime();

                    if(timeTillReady>0){
                        log.info("Delaying DAP2 data request for "+timeTillReady+"ms");
                        try { Thread.sleep(timeTillReady);}
                        catch(InterruptedException e){ log.error("Thread Interrupted. msg: "+e.getMessage());}
                    }

                    return(super.requestDispatch(request,response,true));

                }
                else {

                    response.setHeader(HttpHeaders.ASYNC_ACCEPTED, getResponseDelay()+"");

                    String requestUrl = request.getRequestURL().toString();
                    String ce = request.getQueryString();

                    if(ceAsyncAccept==null && headerAsyncAccept!=null){

                        ce="async="+ clientsAsyncVal + "&" + ce;
                    }

                    String resultLinkUrl = requestUrl + "?" + ce;

                    Document asyncResponse = DocFactory.getAsynchronousResponseAccepted(request, resultLinkUrl, getResponseDelay(),getCachePersistTime());
                    sendDocument(response, asyncResponse, HttpServletResponse.SC_ACCEPTED);


                    return true;
                }
            }
        }

    }


    public static void sendDocument(HttpServletResponse response, Document doc, int httpStatus) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        response.setStatus(httpStatus);
        response.setContentType("text/xml");

        response.getOutputStream().print(xmlo.outputString(doc));

    }






    private int getResponseDelay(){


        return responseDelay;
    }


    private int getCachePersistTime(){


        return cachePersistTime;
    }







    /* Mothballed First version

    public Document getAsynchronousResponseDoc(HttpServletRequest request, Date firstTimeAvailable, Date lastTimeAvailable){


        String context  = request.getContextPath()+"/";

        Element dataset = new Element("Dataset", DAP.DAPv40_NS);
        Element async   = new Element("async",DAP.DAPv40_NS);
        Element beginAccess  = new Element("beginAccess", DAP.DAPv40_NS);
        Element endAccess    = new Element("endAccess", DAP.DAPv40_NS);

        async.addContent(beginAccess);
        async.addContent(endAccess);
        dataset.addContent(async);
        dataset.addNamespaceDeclaration(DublinCore.NS);
        dataset.addNamespaceDeclaration(XLINK.NS);

        String xmlBase =  DocFactory.getXmlBase(request);
        String requestUrl = request.getRequestURL().toString();
        String ce = request.getQueryString();

        dataset.setAttribute("base",xmlBase, XML.NS);
        async.setAttribute("href",requestUrl+"?"+ce, XLINK.NS);


        log.debug("firstTime: "+firstTimeAvailable.getTime());
        log.debug("lastTime:  "+lastTimeAvailable.getTime());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        StringBuffer startTime = new StringBuffer();

        startTime = sdf.format(firstTimeAvailable,startTime, new FieldPosition(DateFormat.YEAR_FIELD));

        StringBuffer endTime = new StringBuffer();

        endTime = sdf.format(lastTimeAvailable,endTime,new FieldPosition(DateFormat.YEAR_FIELD));

        beginAccess.setText(startTime.toString());
        endAccess.setText(endTime.toString());

        HashMap<String,String> piMap = new HashMap<String,String>( 2 );
        piMap.put( "type", "text/xsl" );
        piMap.put( "href", context+"xsl/asyncResponse.xsl" );
        ProcessingInstruction pi = new ProcessingInstruction( "xml-stylesheet", piMap );

        Document asyncResponse = new Document() ;
        asyncResponse.addContent( pi );

        asyncResponse.setRootElement(dataset);


        return asyncResponse;

    }



    */

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
