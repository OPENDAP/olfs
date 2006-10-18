/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
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


import javax.servlet.http.HttpServletRequest;



/**
 * Provides utility methods that perform "analysis" of the user request and return important componet strings
 * for the OPeNDAP servlet.
 *
 * The dataSourceName is the local URL path of the request, minus any requestSuffix detected. So, if the request is
 * for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset minus the
 * requestSuffix. If the request is for a collection, then the dataSourceName is the complete local path.
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
     *
     * @param req The client request.
     * @return The URL of the request minus the last "." suffix. In other words if the requested URL ends
     * with a suffix that is preceeded by a dot (".") then the suffix will removed from this returned URL.
     */

    public static String getRequestURL(HttpServletRequest req){

        String requestURL;

        // Figure out the data set name.
        String requestPath = req.getPathInfo();

        if(Debug.isSet("ReqInfo")) System.out.print("ReqInfo.getRequestURL() - req.getPathInfo(): " + requestPath);

        // Is it a collection?
        if (requestPath == null || requestPath.endsWith("/")) {
            requestURL = req.getRequestURL().toString();
            if(Debug.isSet("ReqInfo")) System.out.println("   requestURL: "+requestURL+" (a collection)");
        } else {
            // It appears to be a dataset.

            // Does it have a request suffix?
            if(requestPath.lastIndexOf("/") < requestPath.lastIndexOf(".")){

                requestURL = req.getRequestURL().substring(0, req.getRequestURL().toString().lastIndexOf("."));

            } else {
                requestURL = req.getRequestURL().toString();
            }
            if(Debug.isSet("ReqInfo")) System.out.println("   requestURL: "+requestURL+" (a dataset)");
        }

        return requestURL;

    }


    /**
     * The collection name is the path leading to requestd dataset, if a dataset was requested. If a
     * collection was requested then that is returned.
     *
     * @param req The client request.
     * @return The name of the collection.
     */
    public static String getCollectionName(HttpServletRequest req){

        String cName, dSrc, dSetName;

        dSrc = getDataSource(req);
        dSetName = getDataSetName(req);

        if(dSetName == null)
            cName = dSrc;
        else
            cName = dSrc.substring(0,dSrc.lastIndexOf(dSetName));

        if(Debug.isSet("ReqInfo")) System.out.println("ReqInfo.getCollectionName(): " + cName);

        return cName;


    }


    /**
     *
     * @param req The client request.
     * @return The suffix of the request. Basically it looks at the last element in the slash "/" seperated list
     * of stuff in the URL and returns everything after the final "." If there is no final "." then null is returned.
     */
    public static String getRequestSuffix(HttpServletRequest req){

        String requestSuffix = null;
        String requestPath = req.getPathInfo();
        if(Debug.isSet("ReqInfo")) System.out.print("ReqInfo.getRequestSuffix() - req.getPathInfo(): " + requestPath);


        // Is it a dataset and not a collection?
        if (requestPath!=null && !requestPath.endsWith("/")) {

            // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
            // and strip it off the dataSetName

            if(requestPath.lastIndexOf("/") < requestPath.lastIndexOf(".")){
                requestSuffix = requestPath.substring(requestPath.lastIndexOf('.') + 1);
            }

        }

        if(Debug.isSet("ReqInfo")) System.out.println("  requestSuffix:  " + requestSuffix);

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
     */
    public static String getDataSetName(HttpServletRequest req){

        String requestPath = req.getPathInfo();
        if(Debug.isSet("ReqInfo")) System.out.print("ReqInfo.getDataSetName()   - req.getPathInfo(): " + requestPath);


        String dataSetName = requestPath;



        // Is it a dataset and not a collection?
        if (requestPath== null ||  requestPath.endsWith("/")) {

            dataSetName = null;

        }
        else{

            // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
            // and strip it off the dataSetName

            if(requestPath.lastIndexOf("/") < requestPath.lastIndexOf(".")){
                   dataSetName = requestPath.substring(requestPath.lastIndexOf("/")+1, requestPath.lastIndexOf('.'));
            }
        }

        if(Debug.isSet("ReqInfo")) System.out.println("  dataSetName:    " + dataSetName);


        return dataSetName;

    }


    /**
     * The dataSourceName is the local URL path of the request, minus any requestSuffix detected. So, if the request is
     * for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset minus the
     * requestSuffix. If the request is for a collection, then the dataSourceName is the complete local path.
     * <p><b>Examples:</b>
     * <ul><li>If the complete URL were: http://opendap.org:8080/opendap/nc/fnoc1.nc.dds<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = fnoc1.nc </li>
     * <li> dataSourceName = /opendap/nc/fnoc1.nc </li>
     * <li> requestSuffix = dds </li>
     * </ul>
     *
     * <li>If the complete URL were: http://opendap.org:8080/opendap/nc/<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = null </li>
     * <li> dataSourceName = /opendap/nc/ </li>
     * <li> requestSuffix = "" </li>
     * </ul>
     * </ul>
     *
     * @param req The client request.
     * @return The DataSourceName
     */
    public static String getDataSource(HttpServletRequest req){

        String requestPath = req.getPathInfo();
        if(Debug.isSet("ReqInfo")) System.out.print("ReqInfo.getDataSource()    - req.getPathInfo(): " + requestPath);


        String dataSourceName;

        // Is it a dataset and not a collection?

        if(requestPath == null){ // If the requestPath is null, then we are at the top level, or "/" as it were.
            dataSourceName = "/";

        }
        else {
            dataSourceName = requestPath;

            if (!dataSourceName.endsWith("/")) { // If it's not a collection then we'll look for a suffix to remove


                // If a dot is found in the last path element take the stuff after the last dot as the OPeNDAP suffix
                // and strip it off the dataSourceName

                if(dataSourceName.lastIndexOf("/") < dataSourceName.lastIndexOf(".")){
                       dataSourceName = dataSourceName.substring(0, dataSourceName.lastIndexOf('.'));
                }
            }
        }
        if(Debug.isSet("ReqInfo")) System.out.println("  dataSourceName: " + dataSourceName);

        return dataSourceName;

    }


    /**
     * Evaluates the request and returns TRUE if it is determined that the request is for an OPeNDAP directory view.
     * @param req The client request.
     * @return True if the request is for an OPeNDAP directory view, False otherwise.
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


    /**
     * ***********************************************************************
     */




}


