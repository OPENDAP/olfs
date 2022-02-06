/*
 * /////////////////////////////////////////////////////////////////////////////
 * This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * Copyright (c) 2022 OPeNDAP, Inc.
 * Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * //
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * //
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */


package opendap.coreServlet;

import opendap.PathBuilder;
import opendap.bes.BesDapDispatcher;
import opendap.dap.Request;
import opendap.dap4.QueryParameters;
import opendap.io.HyraxStringEncoding;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
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

import static opendap.http.Util.PROTOCOL_TERMINATON;


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
    public static final String  HTTP_POST = "POST";

    private static final String CLOUD_FRONT_FORWARDED_PROTOCOL = "CloudFront-Forwarded-Proto";
    private static final String X_FORWARDED_PROTOCOL = "X-Forwarded-Proto";
    private static final String X_FORWARDED_PORT = "X-Forwarded-Port";

    private static final String JAVAX_SERVLET_FORWARD_REQUEST_URI  = "javax.servlet.forward.request_uri";
    private static final String JAVAX_SERVLET_FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";
    private static final String JAVAX_SERVLET_FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";
    private static final String JAVAX_SERVLET_FORWARD_PATH_INFO    = "javax.servlet.forward.path_info";
    private static final String JAVAX_SERVLET_FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";
    private static final String MISSING = "MISSING";

    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(ReqInfo.class);

    }

    private static String show_javax_servlet_forward(HttpServletRequest req){
        String forwardRequestUri = (String)req.getAttribute(JAVAX_SERVLET_FORWARD_REQUEST_URI);
        String context_path = (String)req.getAttribute(JAVAX_SERVLET_FORWARD_CONTEXT_PATH);
        String servlet_path = (String)req.getAttribute(JAVAX_SERVLET_FORWARD_SERVLET_PATH);
        String path_info = (String)req.getAttribute(JAVAX_SERVLET_FORWARD_PATH_INFO);
        String query_string  = (String)req.getAttribute(JAVAX_SERVLET_FORWARD_QUERY_STRING);

        String sb = JAVAX_SERVLET_FORWARD_REQUEST_URI + ": " +
                (forwardRequestUri != null ? forwardRequestUri : MISSING) + '\n' +
                JAVAX_SERVLET_FORWARD_CONTEXT_PATH + ": " +
                (context_path != null ? context_path : MISSING) + '\n' +
                JAVAX_SERVLET_FORWARD_SERVLET_PATH + ": " +
                (servlet_path != null ? servlet_path : MISSING) + '\n' +
                JAVAX_SERVLET_FORWARD_PATH_INFO + ": " +
                (path_info != null ? path_info : MISSING) + '\n' +
                JAVAX_SERVLET_FORWARD_QUERY_STRING + ": " +
                (query_string != null ? query_string : MISSING) + '\n';
        return sb;
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

    private static ConcurrentHashMap<String,String> serviceContexts = new ConcurrentHashMap<>();


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
        if(req.getMethod().equalsIgnoreCase(HTTP_POST)){
            if(req.getContentLength()> getPostBodyMaxLength()) {
                throw new IOException("POST body content length is longer than maximum allowed by service.");
            }
            String contentType = req.getHeader("Content-Type");
            if(contentType!=null && contentType.equalsIgnoreCase("application/x-www-form-urlencoded")){
                Map paramMap = req.getParameterMap();
                if(paramMap!=null){

                    // Check for DAP4 CE in the body.
                    String[] bodyCEValues = (String[]) paramMap.get(QueryParameters.DAP4_CONSTRAINT_EXPRESSION_KEY);
                    if(bodyCEValues!=null){
                        for(String ceValue: bodyCEValues){
                            if(bodyCE.length()!=0){
                                bodyCE.append(";");
                            }
                            else {
                                bodyCE.append(QueryParameters.DAP4_CONSTRAINT_EXPRESSION_KEY + "=");
                            }
                            bodyCE.append(ceValue);
                        }
                    }
                    else {
                        // Check for DAP2 CE in the body.
                        bodyCEValues = (String[]) paramMap.get("ce");
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
        //String MATCH_LAST_DOT_SUFFIX_REGEX_STRING = "\\.(?=[^.]*$).*$";
        //Matcher m = Pattern.compile(MATCH_LAST_DOT_SUFFIX_REGEX_STRING).matcher(s);
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
    private static final String History_Json_Entry_Date_Format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public  static String getCFHistoryEntry(HttpServletRequest request) throws IOException {
        StringBuilder cf_history_entry = new StringBuilder();
        // Add the date
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(CF_History_Entry_Date_Format);
        sdf.setTimeZone(new SimpleTimeZone(0,"GMT"));
        cf_history_entry.append(sdf.format(now,new StringBuffer(),new FieldPosition(0)));
        cf_history_entry.append(" ");

        // Add the Hyrax Version
        cf_history_entry.append("hyrax-").append(opendap.bes.Version.getHyraxVersionString());
        cf_history_entry.append(" ");

        // Add the complete request URL
        cf_history_entry.append(getRequestUrlPath(request));
        if(!ReqInfo.getConstraintExpression(request).isEmpty()) {
            cf_history_entry.append("?");
            cf_history_entry.append(ReqInfo.getConstraintExpression(request));
        }
        cf_history_entry.append("\n");

        return cf_history_entry.toString();
    }

    /**
       Creates JSON array for history_json
    */
    public  static String getHistoryJsonEntry(HttpServletRequest request) throws IOException {

        // Add the date
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(History_Json_Entry_Date_Format);
        sdf.setTimeZone(new SimpleTimeZone(0,"GMT"));
        String timestamp = sdf.format(now);
        String schema = "https://harmony.earthdata.nasa.gov/schemas/history/0.1.0/history-0.1.0.json";
        String program = "hyrax";
        String version = opendap.bes.Version.getHyraxVersionString();
        String request_url = getRequestUrlPath(request);

        JSONObject param = new JSONObject();
        param.put("request_url",request_url);
        JSONArray parameters = new JSONArray();
        parameters.add(param);

        JSONObject history_json_obj = new JSONObject();

        history_json_obj.put("$schema", schema);
        history_json_obj.put("date_time", timestamp);
        history_json_obj.put("program", program);
        history_json_obj.put("version", version);
        history_json_obj.put("parameters", parameters);

        StringWriter out = new StringWriter();
        history_json_obj.writeJSONString(out);

        log.debug("history_json entry: {}", out.toString());
        return out.toString();
    }


    /**
     * -----------------------------------------------------------------------------------
     *
     * - BEGIN: From javaee6 javadoc ---------------------
     *
     * Interface HttpServletRequest
     *
     * getRequestURL()
     *
     * java.lang.StringBuffer getRequestURL()
     * Reconstructs the URL the client used to make the request. The returned
     * URL contains a protocol, server name, port number, and server path,
     * but it does not include query string parameters.
     *
     * If this request has been forwarded using:
     *    RequestDispatcher.forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     * the server path in the reconstructed URL must reflect the path used to
     * obtain the RequestDispatcher, and not the server path specified by the client.
     *
     * Because this method returns a StringBuffer, not a string, you can modify the URL
     * easily, for example, to append query parameters.
     *
     * This method is useful for creating redirect messages and for reporting errors.
     *
     * Returns:
     * a StringBuffer object containing the reconstructed URL
     *
     * - END: From javaee6 javadoc ---------------------
     *
     * -----------------------------------------------------------------------------------
     * 
     * public static String getRequestUrlPath(HttpServletRequest req);
     * 
     * 
     * This method returns the client issued request URL. This can be a bit difficult to
     * determined because of CDN, Firewall, or server internal server redirect URL rewriting.
     *
     * The method checks for the presence of the request headers: CLOUD_FRONT_FORWARDED_PROTOCOL
     * and X_FORWARDED_PROTOCOL to determine f the protocol of the request was rewritten by
     * some forwardning agent (such as the AWS CloudFront Content Delivery Network).
     *
     * It can often be the case that the forwarding entity is supporting TLS on an outward facing
     * endpoint but the OLFS/Tomcat instance is not. The forwarding entity may then rewrite the
     * URL by replacing the https:// protocol with the http:// protocol, and if the forwarding
     * entity is well behaved the original request protocol will be noted in an injected
     * X_FORWARDED_PROTOCOL request header in an injected  CLOUD_FRONT_FORWARDED_PROTOCOL if
     * the forwarding entity is an instance of AWS CloudFront.
     *
     * If we allow the value of CLOUD_FRONT_FORWARDED_PROTOCOL or X_FORWARDED_PROTOCOL to simply
     * dictate the protocol of the returned URL then the case in which the protocol is rewritten
     * from http:// to https:// may be encountered. The use-case for this scenario escapes me,
     * and I think that while possible it does not regularly happen.
     *
     * If instead we implement this so that https:// is favored then we might say that:
     *
     * Client   to ReWrite  -> RESULT
     * http://  to http://  -> http://
     * http://  to https:// -> https://
     * https:// to http://  -> https://
     * https:// to https:// -> https://
     *
     * I think that since the rewrite case of http ->https is rare and potentially useless,
     * I will implement this so that it slavishly utilizes the value of the CLOUD_FRONT_FORWARDED_PROTOCOL
     * or X_FORWARDED_PROTOCOL request headers to determine the protocol for the returned URL.
     * Client   to ReWrite  -> RESULT
     * http://  to http://  -> http://
     * http://  to https:// -> http://
     * https:// to http://  -> https://
     * https:// to https:// -> https://
     *
     * Additonallu, if the OLFS configuration parameter <ForceServiceLinksToHttps /> is
     * present this will override all of the above and force the protocol for the
     * returned URL to https://
     * 
     * @param req The request to assess.
     * @return The client issued URL
     */
    public static String getRequestUrlPath(HttpServletRequest req) {

        // We assume the scheme in the request is correct as a starting place.
        String client_request_protcol = req.getScheme();

        // Are we forcing to HTTPS?
        if(BesDapDispatcher.forceLinksToHttps()) {
            // Yes, then do so...
            client_request_protcol = opendap.http.Util.HTTPS_PROTOCOL;
        }
        else {
            // We are not forcing so we, navigate the protocol determination
            // See if this was a forward from AWS CloudFront
            String cf_client_proto = req.getHeader(CLOUD_FRONT_FORWARDED_PROTOCOL);
            log.debug("{}: {}",CLOUD_FRONT_FORWARDED_PROTOCOL,(cf_client_proto!=null?cf_client_proto:MISSING));

            // See if this was a forward from someplace willing to admit it.
            String xfp_client_proto = req.getHeader(X_FORWARDED_PROTOCOL);
            log.debug("{}: {}",X_FORWARDED_PROTOCOL,(xfp_client_proto!=null?xfp_client_proto:MISSING));

            if(cf_client_proto != null){
                // It's a CloudFront redirect, use the indicated protocol
                client_request_protcol = cf_client_proto;
            }
            else if(xfp_client_proto!=null){
                // It's a from something, use the indicated protocol
                client_request_protcol = xfp_client_proto;
            }
        }
        // We know that the values of the request headers CLOUD_FRONT_FORWARDED_PROTOCOL
        // and X_FORWARDED_PROTOCOL don't end with the PROTOCOL_TERMINATON (aka "://")
        // zso we check for the abscence of a trailing PROTOCOL_TERMINATON and add it
        // as needed.
        if(!client_request_protcol.endsWith(PROTOCOL_TERMINATON)){
            client_request_protcol += PROTOCOL_TERMINATON;
        }

        // Determine which server port the client was accessing.
        // Start with request's ServerPort.
        String serverRequestPort = Integer.toString(req.getServerPort());

        // Check for a forwarded port header and use it if present. It
        // looks like AWS supports this as well.
        // See: https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/x-forwarded-headers.html
        String xfp_client_port = req.getHeader(X_FORWARDED_PORT);
        log.debug("{}: {}",X_FORWARDED_PORT, xfp_client_port!=null?xfp_client_port:MISSING);
        if(xfp_client_port!=null){
            serverRequestPort = xfp_client_port;
        }

        // Now we have a peek to see if the request was an internal forward from some
        // our servlet
        log.debug("show_javax_servlet_forward():\n {}", show_javax_servlet_forward(req));
        String forwardRequestUri = (String)req.getAttribute(JAVAX_SERVLET_FORWARD_REQUEST_URI);
        log.debug("{}: {}",JAVAX_SERVLET_FORWARD_REQUEST_URI, forwardRequestUri!=null?forwardRequestUri:MISSING);

        String requestUrlStr;
        if(forwardRequestUri == null) {
            requestUrlStr = req.getRequestURL().toString();
            if(!requestUrlStr.startsWith(client_request_protcol)){
                // If protocols do not match then update the requestUrlStr to match client_request_protcol
                int index = requestUrlStr.indexOf(PROTOCOL_TERMINATON) + PROTOCOL_TERMINATON.length();
                requestUrlStr = client_request_protcol + requestUrlStr.substring(index);
            }
        }
        else {
            // Read the javadoc entry for HttpServletRequest.getREquestUrl() in the header
            // comment for this method.
            //
            // Because javax.servlet.forward.request_uri does not contain the request
            // protocol or the port number of the service we have to determine this
            // by examining the HttpServletRequest objects state.
            String serverName = req.getServerName();
            int serverPort = Integer.parseInt(serverRequestPort);
            requestUrlStr = client_request_protcol + serverName;

            // If the port used is "unusual" then we make sure to include it in the URL.
            if( client_request_protcol.equalsIgnoreCase(opendap.http.Util.HTTP_PROTOCOL) && serverPort != 80) {
                requestUrlStr += ":";
                requestUrlStr += serverPort;
            }
            else if( client_request_protcol.equalsIgnoreCase(opendap.http.Util.HTTPS_PROTOCOL) && (serverPort != 443 && serverPort != 80) ) {
                requestUrlStr += ":";
                requestUrlStr += serverPort;
            }
            requestUrlStr += forwardRequestUri;
        }

        return requestUrlStr;
    }


}


