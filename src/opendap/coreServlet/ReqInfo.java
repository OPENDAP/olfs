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
 * User requests get cached here so that downstream code can access
 * the details of the request information.
 *
 * @author Nathan Potter
 */

public class ReqInfo {





    public static  String getConstraintExpression(HttpServletRequest req) {
        String CE = req.getQueryString();

        // If there was simply no constraint then prepCE() should have returned
        // a CE equal "", the empty string. A null return indicates an error.
        if (CE == null) {
            CE = "";
        }

        return CE;
    }




    /**
     *
     * @param req
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
     * @param req
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
       * *************************************************************************
       * Evaluates the (private) request object to determine if the client that
       * sent the request accepts compressed return documents.
       *
       * @return True is the client accpets a compressed return document.
       *         False otherwise.
       */

      public static boolean getAcceptsCompressed(HttpServletRequest req) {

          boolean isTiny;

          String Encoding = req.getHeader("Accept-Encoding");

          if (Encoding != null)
              isTiny = Encoding.equalsIgnoreCase("deflate");
          else
              isTiny = false;

          return (isTiny);
      }











    /**
     * ***********************************************************************
     */




}


