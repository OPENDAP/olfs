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


package opendap.coreServlet;


import opendap.dap.Request;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;


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



    private static ConcurrentHashMap<String,String> serviceContexts;

    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(ReqInfo.class);

    }

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
        
        return req.getDapServiceUrl();

    }

    /**
     * Get service context portion of the URL - everything in the URL after the name of the server and before the localID (aka relativeUrl) of the dataset.
     * @param request The client request.
     * @return The URL of the request minus the last "." suffix. In other words if the requested URL ends
     * with a suffix that is preceeded by a dot (".") then the suffix will removed from this returned URL.
     */

    public static String getFullServiceContext(HttpServletRequest request){

        String requestUri = request.getRequestURI().toString();

        String pathInfo = request.getPathInfo();

        String serviceContext = requestUri;
        if(pathInfo != null)
            serviceContext = requestUri.substring(0, requestUri.lastIndexOf(pathInfo));



        return serviceContext;

    }

    public static String getLocalUrl(HttpServletRequest req){

        String name=req.getPathInfo();

        if(name == null){ // If the requestPath is null, then we are at the top level, or "/" as it were.
            name = "/";

        }
        return name;

    }






    /**
     * Returns the OPeNDAP constraint expression.
     * @param req The client request.
     * @return The OPeNDAP constraint expression.
     */
    public static  String getConstraintExpression(HttpServletRequest req) {

        String CE = req.getQueryString();

        if (CE == null) {
            CE = "";
        }

        return CE;
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


        // Is it a dataset and not a collection?
        if (relativeUrl!=null && !relativeUrl.endsWith("/")) {

            // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
            // and strip it off the dataSetName

            if(relativeUrl.lastIndexOf("/") < relativeUrl.lastIndexOf(".")){
                requestSuffix = relativeUrl.substring(relativeUrl.lastIndexOf('.') + 1);
            }

        }

        log.debug("  requestSuffixRegex:  " + requestSuffix);

        return requestSuffix;

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
        s += "getConstraintExpression(): "+ ReqInfo.getConstraintExpression(request) + "\n";
        s += "getDataSetName(): "+ ReqInfo.getDataSetName(request) + "\n";
        s += "getRequestSuffix(): "+ ReqInfo.getRequestSuffix(request) + "\n";
        s += "requestForOpendapContents(): "+ ReqInfo.requestForOpendapContents(request) + "\n";
        s += "requestForTHREDDSCatalog(): "+ ReqInfo.requestForTHREDDSCatalog(request) + "\n";

        return s;
        
    }


    public static boolean isServiceOnlyRequest(HttpServletRequest req){
        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();



        String reqURI = req.getRequestURI();

        String serviceName = contextPath + servletPath;


        if (reqURI.equals(serviceName) && !reqURI.endsWith("/")) {
            return true;
        }
        return false;

    }
}


