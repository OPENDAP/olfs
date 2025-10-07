/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2014 OPeNDAP, Inc.
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

package opendap.dap4;


import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import opendap.dap.Request;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * User: ndp
 * Date: 1/27/14
 * Time: 2:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class QueryParameters {

    public static final String DAP4_STORE_RESULT_KEY = "dap4.store_result";
    public static final String DAP4_CONSTRAINT_EXPRESSION_KEY = "dap4.ce";
    public static final String DAP4_FUNCTION_KEY = "dap4.function";
    public static final String DAP4_ASYNC_KEY = "dap4.async";
    public static final String DAP4_CHECKSUM_KEY = "dap4.checksum";
    public static final String CLOUDY_DAP = "cloudydap";


    private String async;
    private String storeResultServiceUrl;
    private boolean isStoreResultRequest;
    private String func;
    private String ce;
    private boolean d_computeChecksums;
    private String queryRemainder;
    private String cloudyDap;


    public QueryParameters() {
        async = null;
        storeResultServiceUrl = null;
        isStoreResultRequest = false;
        func = null;
        ce = null;
        d_computeChecksums = false;
        queryRemainder = null;
        cloudyDap=null;
    }


    public QueryParameters(HttpServletRequest req) throws IOException {
        this();
        ingestDap4Query(req);
    }


    public String getQueryRemainder(){
        return queryRemainder;
    }



    public void setStoreResultRequestServiceUrl(String  serviceUrl){
        storeResultServiceUrl = serviceUrl;
    }

    public String getStoreResultRequestServiceUrl(){
        return storeResultServiceUrl;
    }

    public void setIsStoreResultRequest(boolean  value){
        isStoreResultRequest = value;
    }

    public boolean isStoreResultRequest(){
        return isStoreResultRequest;
    }


    public void setAsync(String waitTime){
        async = waitTime;
    }

    public String getAsync(){
        return async;
    }


    public void setFunc(String functionalExpression){
        func = functionalExpression;
    }

    public String getFunc(){
        return func;
    }


    public void setCe(String dap4ce){
        ce = dap4ce;
    }

    public String getCe(){
        return ce;
    }

    /**
     * Setter for a checksummed dap4 data response
     * @param state
     */
    public void computeChecksums(String state){
        d_computeChecksums =  (state!=null && state.equalsIgnoreCase("true"));
    }

    /**
     * Getter for checksummed dap4 data response state.
     * @return True to compute checksums, false otherwise
     */
    public boolean computeChecksums(){
        return d_computeChecksums;
    }

    /**  Added for cloudydap experiment - ndp 1/19/17 - - - - - - - - - - - - - - - - - - */
    public void setCloudyDap(String cloudyDapString) { cloudyDap = cloudyDapString; }
    public String getCloudyDap() { return cloudyDap; }
    /** - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


    /**
     * Using the passed HttpServletRequest this method extracts DAP4 query string parameters from the query string,
     * sets their internal values appropriately, and returns the DAP2 constraint expression String sans DAP4 components.
     * This method integrates the infamous GDS {}{}{} syntax content from the path section of the URL into the
     * the returned DAP2 query string.
     * @param req
     * @return Returns a string containing the "remainder" of query string: The original query string with all of
     * the (recognized) dap4 parameters removed.
     */
    public void ingestDap4Query(HttpServletRequest req) throws IOException {

        Map<String, String[]> params = new HashMap<>(req.getParameterMap());

        if(!params.isEmpty()){
            String asyncHttpHeader = req.getHeader("X-DAP-Async-Accept");


            Vector<String> dropList = new Vector<String>();
            for(String key: params.keySet()){

                if(key.equals(DAP4_STORE_RESULT_KEY)){
                    setIsStoreResultRequest(true);
                    setStoreResultRequestServiceUrl(new Request(null, req).getServiceUrl());
                    dropList.add(key);
                }
                if(key.equals(DAP4_CONSTRAINT_EXPRESSION_KEY)){
                    setCe(req.getParameter(key));
                    dropList.add(key);
                }
                if(key.equals(DAP4_CHECKSUM_KEY)){
                    computeChecksums(req.getParameter(key));
                    dropList.add(key);
                }
                if(key.equals(DAP4_FUNCTION_KEY)){
                    setFunc(req.getParameter(key));
                    dropList.add(key);
                }
                
                /**  Added for cloudydap experiment - ndp 1/19/17 */
                if(key.equals(CLOUDY_DAP)){
                    setCloudyDap(req.getParameter(key));
                    dropList.add(key);
                }
                /** - - - - - - - - - - - - - - - - - - - - - - - */

                if(key.equals(DAP4_ASYNC_KEY)){
                    setAsync(req.getParameter(key));
                    dropList.add(key);
                }
                else if (asyncHttpHeader!=null){
                    setAsync(asyncHttpHeader);
                }

            }

            for(String droppedParam: dropList){
                params.remove(droppedParam);

            }
            StringBuilder ce = new StringBuilder();

            for(String key: params.keySet()){
                String values[] =  req.getParameterValues(key);
                if(values!=null){
                    for(String value: values){
                        if(ce.length()>0)
                            ce.append("&");
                        ce.append(key);
                        if(!value.isEmpty()){
                            ce.append("=").append(value);
                        }
                    }
                }
            }

            queryRemainder = ce.toString();

        }
        else {
            String qs = req.getQueryString();
            if(qs==null){
                qs = "";
            }
            queryRemainder = qs;
        }


    }





}
