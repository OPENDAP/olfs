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


package opendap.coreServlet;


import opendap.PathBuilder;
import opendap.dap.Request;
import opendap.io.HyraxStringEncoding;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides utility methods that perform "analysis" of the user request and return important componet strings
 * for the OPeNDAP servlet.
 *
 * The dataSourceName is the local URL path of the request, minus any requestSuffixRegex detected. So, if the request is
 * for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset minus the
 * requestSuffixRegex. If the request is for a collection, then the dataSourceName is the complete local path.
 * <p><b>Examples:</b>
 * <ul><li>If the complete URL were: http://opendap.org:8080/opendap/nc/fnoc1.nc.dds?lat,lon,time&lat>72.0<br/>
 * Then the:</li>
 * <ul>
 * <li> RequestURL = http://opendap.org:8080/opendap/nc/fnoc1.nc </li>
 * <li> CollectionName = /opendap/nc/ </li>
 * <li> DataSetName = fnoc1.nc </li>
 * <li> DataSourceName = /opendap/nc/fnoc1.nc </li>
 * <li> RequestSuffix = dds </li>
 * <li> ConstraintExpression = lat,lon,time&lat>72.0 </li>
 * </ul>
 *
 * <li>If the complete URL were: http://opendap.org:8080/opendap/nc/<br/>
 * Then the:</li>
 * <ul>
 * <li> RequestURL = http://opendap.org:8080/opendap/nc/ </li>
 * <li> CollectionName = /opendap/nc/ </li>
 * <li> DataSetName = null </li>
 * <li> DataSourceName = /opendap/nc/ </li>
 * <li> RequestSuffix = "" </li>
 * <li> ConstraintExpression = "" </li>
 * </ul>
 * </ul>
 * @author Nathan Potter
 */

public class ReqInfo {

    public static final int DEFAULT_POST_BODY_MAX_LENGTH  =  2000000;
    public static final int ABSOLUTE_MAX_POST_BODY_LENGTH = 10000000;

    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(ReqInfo.class);

    }

    private static AtomicInteger maxPostBodyLength;
    static {
        maxPostBodyLength = new AtomicInteger(DEFAULT_POST_BODY_MAX_LENGTH);
    }

    private static String pathFunctionSyntaxRegEx = "_expr_\\{.*\\}\\{.*\\}(\\{.*\\})?";
    private static Pattern pathFunctionSyntaxPattern;
    static {
         pathFunctionSyntaxPattern = Pattern.compile(pathFunctionSyntaxRegEx, Pattern.CASE_INSENSITIVE);
    }


    public static void setMaxPostBodyLength(int maxLength){
        if(maxLength > ABSOLUTE_MAX_POST_BODY_LENGTH){
            log.error("Submitted max POST body length ({}) is too large. Setting to max allowable POST body length of {}.",maxLength,ABSOLUTE_MAX_POST_BODY_LENGTH);
            maxPostBodyLength.set(ABSOLUTE_MAX_POST_BODY_LENGTH);
        }
        else {
            maxPostBodyLength.set(maxLength);
        }
    }

    public static int getPostBodyMaxLength(){
        return maxPostBodyLength.get();
    }

    private static ConcurrentHashMap<String,String> serviceContexts;


    public static boolean addService(String serviceName, String serviceLocalID){
        serviceContexts.put(serviceName,serviceLocalID);
        return true;
    }

    public static boolean dropService(String serviceName){
        serviceContexts.remove(serviceName);
        return true;
    }

    public static Enumeration<String> getServiceNames(){
        return serviceContexts.keys();
    }



    /**
     * Get service portion of the URL - everything in the URL before the localID (aka relativeUrl) of the dataset.
     * @param request The client request.
     * @return The URL of the request minus the last "." suffix. In other words if the requested URL ends
     * with a suffix that is preceeded by a dot (".") then the suffix will removed from this returned URL.
     */

    public static String getServiceUrl(HttpServletRequest request){
        Request req = new Request(null,request);
        return req.getServiceUrl();
    }

    /**
     * Get service context portion of the URL - everything in the URL after the name of the server and before the
     * localID (aka relativeUrl) of the dataset.
     * @param request The client request.
     * @return The URL of the request minus the last "." suffix. In other words if the requested URL ends
     * with a suffix that is preceeded by a dot (".") then the suffix will removed from this returned URL.
     */

    public static String getFullServiceContext(HttpServletRequest request){
        String requestUri = request.getRequestURI();
        String pathInfo = request.getPathInfo();
        String serviceContext = requestUri;
        if(pathInfo != null)
            serviceContext = requestUri.substring(0, requestUri.lastIndexOf(pathInfo));
        return serviceContext;
    }


    /**
     * This finds the name of the requested local resource. It has been hacked to remove any usages of path based
     * functions as used by the GDS, TDS, Ferret, or LAS.
     *
     * @todo Change the name of this method to getResourceId().
     * @param req
     * @return
     */
    public static String getLocalUrl(HttpServletRequest req){
        String requestPath=req.getPathInfo();
        if(requestPath == null){ // If the requestPath is null, then we are at the top level, or "/" as it were.
            requestPath = "/";

        }
        Pattern pathFunctionSyntaxPattern = Pattern.compile(pathFunctionSyntaxRegEx, Pattern.CASE_INSENSITIVE);
        Matcher pathFunctionMatcher = pathFunctionSyntaxPattern.matcher(requestPath);

        boolean foundFunctionSyntax = false;
        while(!pathFunctionMatcher.hitEnd()){
            foundFunctionSyntax = pathFunctionMatcher.find();
            log.debug("{}", opendap.coreServlet.Util.checkRegex(pathFunctionMatcher, foundFunctionSyntax));
        }
        String resourceId = requestPath;
        if(foundFunctionSyntax){
            int start =  pathFunctionMatcher.start();
            int end = pathFunctionMatcher.end();
            resourceId = requestPath.substring(0,start) + requestPath.substring(end,requestPath.length());
        }
        return resourceId;
    }









    /**
     * This has been hacked to collect path side functional expressions (server side functions expressed in the path of
     * the URL and place them on the beginning of the CE returned.
     *
     * Returns the OPeNDAP constraint expression.
     * @param req The client request.
     * @return The OPeNDAP constraint expression.
     * @throws java.io.IOException When the body of a POST request cannot be read.
     */
    public static  String getConstraintExpression(HttpServletRequest req) throws IOException {
        StringBuilder CE;
        String ceCacheKey = ReqInfo.class.getName()+".getConstraintExpression()";
        Object o  = RequestCache.get(ceCacheKey);

        if(o == null){
            CE = new StringBuilder();
            StringBuilder pathFunctionCE = getPathFunctionCE(req.getPathInfo());
            CE.append(pathFunctionCE);
            String queryString = req.getQueryString();
            if(queryString != null){
                if(CE.length() != 0){
                    CE.append(",");
                }
                CE.append(queryString);
            }
            StringBuilder bodyCE = getPostBodyCE(req);
            if(CE.length()!=0 && bodyCE.length() != 0){
                CE.append(",");
            }
            CE.append(bodyCE);
            RequestCache.put(ceCacheKey,CE);
        }
        else {
            CE = (StringBuilder)o;
        }
        return CE.toString();
    }


    private static StringBuilder getPostBodyCE(HttpServletRequest req) throws IOException {

        StringBuilder bodyCE = new StringBuilder();
        if(req.getMethod().equalsIgnoreCase("POST")){
            if(req.getContentLength()> getPostBodyMaxLength()) {
                throw new IOException("POST body content length is longer than maximum allowed by service.");
            }
            String contentType = req.getHeader("Content-Type");
            if(contentType!=null && contentType.equalsIgnoreCase("application/x-www-form-urlencoded")){
                Map paramMap = req.getParameterMap();
                if(paramMap!=null){
                    String[] bodyCEValues = (String[]) paramMap.get("ce");
                    if(bodyCEValues!=null){
                        for(String ceValue: bodyCEValues){
                            if(bodyCE.length()!=0){
                                bodyCE.append(",");
                            }
                            bodyCE.append(ceValue);
                        }
                    }
                    bodyCEValues = (String[]) paramMap.get("dap4:ce");
                    if(bodyCEValues!=null){
                        for(String ceValue: bodyCEValues){
                            if(bodyCE.length()!=0){
                                bodyCE.append(",");
                            }
                            bodyCE.append(ceValue);
                        }
                    }
                }
            }
            else {
                bodyCE.append(convertStreamToString(req.getInputStream()));
            }
        }
        return bodyCE;
    }


    /**
     * Sucks an InputStream dry and turns the extracted content into a Java String.
     *
     * Lifted from Pat Niemeyer's 2004 Stupid Scanner Tricks Article
     * Orig Source: https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
     * Archive: http://web.archive.org/web/20140531042945/https://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
     *
     * @author Pat Niemeyer -  http://pat.net/Pat%20Niemeyer/Welcome.html
     * @param is InputStream to drain
     * @return A String formed from the content of InputStream is;
     * @throws IOException When the bad things happen.
     *
     */
    private static String convertStreamToString(java.io.InputStream is) throws IOException {
        // Using the scanner with the \A delimiter basically says "from the beginning of the input"
        // So then we get one big token from teh scanner and (see comment below)
        java.util.Scanner s = new java.util.Scanner(is, HyraxStringEncoding.getCharset().name()).useDelimiter("\\A");
        // Since the Scanner is going to make one token from the whole shebang, either the  InputStream
        // is empty (in which case we return an empty string, or it's not empty and we return the single token.
        return s.hasNext() ? s.next() : "";
    }





    private static StringBuilder getPathFunctionCE(String requestPath){
        if(requestPath == null){ // If the requestPath is null, then we are at the top level, or "/" as it were.
            requestPath = "/";

        }
        Pattern pathFunctionSyntaxPattern = Pattern.compile(pathFunctionSyntaxRegEx, Pattern.CASE_INSENSITIVE);
        Matcher pathFunctionMatcher = pathFunctionSyntaxPattern.matcher(requestPath);
        boolean foundFunctionSyntax = false;
        while(!pathFunctionMatcher.hitEnd()){
            foundFunctionSyntax = pathFunctionMatcher.find();
            log.debug("{}", opendap.coreServlet.Util.checkRegex(pathFunctionMatcher, foundFunctionSyntax));
        }
        String pathFunction = null;
        if(foundFunctionSyntax){
            int start =  pathFunctionMatcher.start();
            int end = pathFunctionMatcher.end();
            pathFunction = requestPath.substring(start,end);

        }
        StringBuilder serverSideFunctionCalls = new StringBuilder();
        if(pathFunction!=null){
            int firstCurlyBrace = pathFunction.indexOf("{");
            if(firstCurlyBrace>=0){
                int secondCurlyBrace = pathFunction.indexOf("{",firstCurlyBrace+1);
                if(secondCurlyBrace>=0){
                    int endSecondCurlyBrace = pathFunction.indexOf("}",secondCurlyBrace+1);
                    if(endSecondCurlyBrace> secondCurlyBrace)
                        serverSideFunctionCalls.append(pathFunction.substring(secondCurlyBrace+1,endSecondCurlyBrace));
                }
            }
        }
        return serverSideFunctionCalls;
    }




    /**
     * The collection name is the path leading to requested dataset, if a dataset was requested. If a
     * collection was requested then that is returned.
     *
     * @param req The client request.
     * @return The name of the collection.
     * @todo Replace this method with one that takes a the result of getRelativeURl() as it's parameter
     */
    public static String getCollectionName(HttpServletRequest req){
        String cName, dSrc, dSetName;
        dSrc = getBesDataSourceID(getLocalUrl(req));
        dSetName = getDataSetName(req);

        if(dSetName == null)
            cName = dSrc;
        else
            cName = dSrc.substring(0,dSrc.lastIndexOf(dSetName));

        if(cName.endsWith("/") && !cName.equalsIgnoreCase("/"))
            cName = cName.substring(0,cName.length()-1);

        log.debug("getCollectionName(): " + cName);
        return cName;
    }

    public static String getCollectionUrl(HttpServletRequest req){
        String collectioName = getCollectionName(req);
        String serviceUrl = getServiceUrl(req);
        return PathBuilder.pathConcat(serviceUrl,collectioName);
    }


    /**
     *
     * @param req The client request.
     * @return The suffix of the request. Basically it looks at the last element in the slash "/" seperated list
     * of stuff in the URL and returns everything after the final "." If there is no final "." then null is returned.
     * @todo Replace this method with one that takes a the result of getRelativeURl() as it's parameter
     */
    public static String getRequestSuffix(HttpServletRequest req){
        String requestSuffix = null;
        String relativeUrl = getLocalUrl(req);
        log.debug("getRequestSuffix() - relativeUrl(request): " + relativeUrl);

        requestSuffix = getSuffix(relativeUrl);
        log.debug("  requestSuffixRegex:  " + requestSuffix);
        return requestSuffix;
    }


    /**
     * If a dot is found in the last path element take the stuff after the last dot as the file suffix
     *
     *
     * @param s
     * @return
     */
    public static String getSuffix(String s){
        //String _regexToMatchLastDotSuffixString = "\\.(?=[^.]*$).*$";
        //Matcher m = Pattern.compile(_regexToMatchLastDotSuffixString).matcher(s);
        String suffix="";
        if (s!=null && !s.endsWith("/")) {
            // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
            // and strip it off the dataSetName
            if(s.lastIndexOf("/") < s.lastIndexOf(".")){

                suffix = s.substring(s.lastIndexOf('.') + 1);
            }
        }
        return suffix;
    }


    /**
     * The dataset is an "atom" in the OPeNDAP URL lexicon. Thus, the dataset is the last thing in the URL prior to
     * the constraint expression (query string) and after the last slash. If the last item in the URL path is a
     * collection, than the dataset name may be null.
     *
     *
     * @param req The client request.
     * @return The dataset name, null if the request is for a collection.
     * @todo Replace this method with one that takes a the result of getRelativeURl() as it's parameter
     */
    public static String getDataSetName(HttpServletRequest req){
        String localUrl = getLocalUrl(req);
        log.debug("getDataSetName()   - req.getLocalUrl(): " + localUrl);

        String dataSetName = localUrl;
        // Is it a collection?
        if (localUrl== null ||  localUrl.endsWith("/")) {
            dataSetName = null;
        }
        else{  // Must be a dataset...
            // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
            // and strip it off the dataSetName
            if(localUrl.lastIndexOf("/") < localUrl.lastIndexOf(".")){
                   dataSetName = localUrl.substring(localUrl.lastIndexOf("/")+1, localUrl.lastIndexOf('.'));
            }
        }
        log.debug("  dataSetName:    " + dataSetName);
        return dataSetName;
    }





    /**
     * The dataSourceName is the local URL path of the request, minus any requestSuffixRegex detected. So, if the request is
     * for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset minus the
     * requestSuffixRegex. If the request is for a collection, then the dataSourceName is the complete local path.
     * <p><b>Examples:</b>
     * <ul><li>If the complete URL were: http://opendap.org:8080/opendap/nc/fnoc1.nc.dds<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = fnoc1.nc </li>
     * <li> dataSourceName = /opendap/nc/fnoc1.nc </li>
     * <li> requestSuffixRegex = dds </li>
     * </ul>
     *
     * <li>If the complete URL were: http://opendap.org:8080/opendap/nc/<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = null </li>
     * <li> dataSourceName = /opendap/nc/ </li>
     * <li> requestSuffixRegex = "" </li>
     * </ul>
     * </ul>
     *
     * @param req The client request.
     * @return The DataSourceName
     * @deprecated 
     */
    public static String getBesDataSourceID(HttpServletRequest req){
        String requestPath = getLocalUrl(req);
        log.debug("getBesDataSourceID()    - req.getPathInfo(): " + requestPath);
        String dataSourceName;
        // Is it a dataset and not a collection?
        dataSourceName = requestPath;
        if (!dataSourceName.endsWith("/")) { // If it's not a collection then we'll look for a suffix to remove
            // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
            // and strip it off the dataSourceName
            if(dataSourceName.lastIndexOf("/") < dataSourceName.lastIndexOf(".")){
                   dataSourceName = dataSourceName.substring(0, dataSourceName.lastIndexOf('.'));
            }
        }
        log.debug("  dataSourceName: " + dataSourceName);
        return dataSourceName;
    }




    /**
     * The dataSourceName is the local URL path of the request, minus any requestSuffixRegex detected. So, if the request is
     * for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset minus the
     * requestSuffixRegex. If the request is for a collection, then the dataSourceName is the complete local path.
     * <p><b>Examples:</b>
     * <ul><li>If the complete URL were: http://opendap.org:8080/opendap/nc/fnoc1.nc.dds<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = fnoc1.nc </li>
     * <li> dataSourceName = /opendap/nc/fnoc1.nc </li>
     * <li> requestSuffixRegex = dds </li>
     * </ul>
     *
     * <li>If the complete URL were: http://opendap.org:8080/opendap/nc/<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = null </li>
     * <li> dataSourceName = /opendap/nc/ </li>
     * <li> requestSuffixRegex = "" </li>
     * </ul>
     * </ul>
     *
     * @param relativeUrl The relative URL of the client request. No Constraint expression (i.e. No query section of
     * the URL - the question mark and everything after it.)
     * @return The DataSourceName
     */
    @Deprecated
    public static String getBesDataSourceID(String relativeUrl){
        String requestPath = relativeUrl;
        log.debug("getBesDataSourceID()    - req.getPathInfo(): " + requestPath);
        String dataSourceName;
        // Is it a dataset and not a collection?
        dataSourceName = requestPath;
        if (!dataSourceName.endsWith("/")) { // If it's not a collection then we'll look for a suffix to remove

            // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
            // and strip it off the dataSourceName

            if(dataSourceName.lastIndexOf("/") < dataSourceName.lastIndexOf(".")){
                   dataSourceName = dataSourceName.substring(0, dataSourceName.lastIndexOf('.'));
            }
        }
        log.debug("  dataSourceName: " + dataSourceName);
        return dataSourceName;
    }


    /**
     * Evaluates the request and returns TRUE if it is determined that the request is for an OPeNDAP directory view.
     * @param req The client request.
     * @return True if the request is for an OPeNDAP directory view, False otherwise.
     * @todo Replace this method with one that takes a the result of getRelativeURl() as it's parameter
     */
    public static boolean requestForOpendapContents(HttpServletRequest req){

        boolean test = false;
        String dsName  = ReqInfo.getDataSetName(req);
        String rSuffix = ReqInfo.getRequestSuffix(req);
        if(     dsName!=null                        &&
                dsName.equalsIgnoreCase("contents") &&
                rSuffix!=null                       &&
                rSuffix.equalsIgnoreCase("html") ){
            test = true;
        }
        return test;
    }


    /**
     * Evaluates the request and returns TRUE if it is determined that the request is for an THREDDS directory view.
     * @param req The client request.
     * @return True if the request is for an THREDDS directory view, False otherwise.
     * @todo Replace this method with one that takes a the result of getRelativeURl() as it's parameter
     */
    public static boolean requestForTHREDDSCatalog(HttpServletRequest req){
        boolean test = false;
        String dsName  = ReqInfo.getDataSetName(req);
        String rSuffix = ReqInfo.getRequestSuffix(req);

        if(     dsName!=null                        &&
                dsName.equalsIgnoreCase("catalog")  &&
                rSuffix!=null                       &&
                (rSuffix.equalsIgnoreCase("html") || rSuffix.equalsIgnoreCase("xml"))  ){
            test = true;
        }
        return test;
    }


    
    // @todo Replace this method with one that takes a the result of getRelativeURl() as it's parameter
    public static String toString(HttpServletRequest request){
        String s = "";
        
        s += "getLocalUrl(): "+ getLocalUrl(request) + "\n";
        s += "getBesDataSourceID(): "+ getBesDataSourceID(getLocalUrl(request)) + "\n";
        s += "getServiceUrl(): "+ getServiceUrl(request) + "\n";
        s += "getCollectionName(): "+ ReqInfo.getCollectionName(request) + "\n";

        s += "getConstraintExpression(): ";
        try {
            s += ReqInfo.getConstraintExpression(request) + "\n";
        } catch (IOException e) {
            s += "Encountered IOException when attempting get the constraint expression! Msg: " + e.getMessage() + "\n";
        }
        s += "getDataSetName(): "+ ReqInfo.getDataSetName(request) + "\n";
        s += "getRequestSuffix(): "+ ReqInfo.getRequestSuffix(request) + "\n";
        s += "requestForOpendapContents(): "+ ReqInfo.requestForOpendapContents(request) + "\n";
        s += "requestForTHREDDSCatalog(): "+ ReqInfo.requestForTHREDDSCatalog(request) + "\n";

        return s;
        
    }


    public static boolean isServiceOnlyRequest(HttpServletRequest req){
        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();

        //String preq = ServletUtil.probeRequest(null,req);
        //System.out.println(preq);
        String reqURI = req.getRequestURI();
        String serviceName = contextPath + servletPath;
        boolean stringsMatch =  reqURI.equals(serviceName);

        if (stringsMatch && !reqURI.endsWith("/")) {
            return true;
        }
        return false;
    }

    private static final String CF_History_Entry_Date_Format = "yyyy-MM-dd HH:mm:ss z";

    public  static String getCFHistoryEntry(HttpServletRequest request) throws IOException {
        StringBuilder cf_history_entry = new StringBuilder();
        // Add the date
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(CF_History_Entry_Date_Format);
        sdf.setTimeZone(new SimpleTimeZone(0,"GMT"));
        cf_history_entry.append(sdf.format(now,new StringBuffer(),new FieldPosition(0)));
        cf_history_entry.append(" ");

        // Add the Hyrax Version
        cf_history_entry.append("Hyrax-").append(opendap.bes.Version.getHyraxVersionString());
        cf_history_entry.append(" ");

        // Add the complete request URL
        cf_history_entry.append(getRequestUrlPath(request));
        cf_history_entry.append("?");
        cf_history_entry.append(ReqInfo.getConstraintExpression(request));
        cf_history_entry.append("\n");

        return cf_history_entry.toString();
    }




    public static String getRequestUrlPath(HttpServletRequest req) {
        String forwardRequestUri = (String)req.getAttribute("javax.servlet.forward.request_uri");
        StringBuilder requestUrl = new StringBuilder();

        if(forwardRequestUri == null) {
            requestUrl.append(req.getRequestURL().toString());
        }
        else {
            String serverName = req.getServerName();
            int serverPort = req.getServerPort();
            String transport = req.getScheme();
            requestUrl.append(transport).append("://").append(serverName);
            if( transport.equalsIgnoreCase("http") && serverPort != 80) {
                requestUrl.append(":").append(serverPort);
            }
            else if( transport.equalsIgnoreCase("https") && serverPort != 443) {
                requestUrl.append(":").append(serverPort);
            }
            requestUrl.append(forwardRequestUri);
        }

        return requestUrl.toString();
    }


}                                                          


