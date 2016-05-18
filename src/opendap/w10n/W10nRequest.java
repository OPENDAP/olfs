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
package opendap.w10n;

import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.Scrub;
import opendap.dap.Request;
import opendap.http.error.BadRequest;
import opendap.http.error.NotAcceptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
* Created by ndp on 2/15/15.
*/
class W10nRequest {
    private Logger _log;

    private HttpServletRequest _request;

    private Vector<String> _requestedVariable;
    private String _dap2ArrayConstraint;
    private String _w10nArrayConstraint;


    private String  _output;
    private String  _callback;
    boolean _reCache;
    boolean _flatten;
    boolean _traverse;

    private String _requestedResourceId;


    private String _validResourcePath;
    private String _w10nId;


    boolean _isData;
    boolean _isDir;
    boolean _isFile;

    private MediaType _bestResponseMediaType;


    /**
     * Converts an HttpServletRequest into a semantically meaningful w10n request object.
     *
     * @param request
     */
    public W10nRequest(HttpServletRequest request){

        _log = LoggerFactory.getLogger(this.getClass());

        _request = request;

        _requestedVariable = new Vector<>();

        /**
         * Evaluate the query parameters in a case insensitive manner.
         */
        TreeMap<String,String[]> pmap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        @SuppressWarnings("unchecked")
        Map<String, String[]> foo = _request.getParameterMap();
        pmap.putAll(foo);


        String s = getParamValue("output", pmap);
        _output = s!=null?Scrub.simpleString(s):null;

        s = getParamValue("callback", pmap);
        _callback = s!=null?Scrub.simpleString(s):null;

        _reCache = pmap.containsKey("reCache");
        _flatten = pmap.containsKey("flatten");
        _traverse = pmap.containsKey("traverse");

        Request oreq = new Request(null, request);
        _requestedResourceId = oreq.getRelativeUrl();

        _w10nArrayConstraint = "[]"; // default is to ask for everything

    }

    public String getValidResourcePath() {
        return _validResourcePath;
    }

    public void setValidResourcePath(String validResourcePath) {
        _validResourcePath = validResourcePath;
    }

    public String getW10nId() {
        return _w10nId;
    }

    public void setW10nId(String id) {
        this._w10nId = id;
    }


    public boolean isData() {
        return _isData;
    }

    public void setIsData(boolean _isData) {
        this._isData = _isData;
    }

    public boolean isDir() {
        return _isDir;
    }

    public void setIsDir(boolean _isDir) {
        this._isDir = _isDir;
    }

    public boolean isFile() {
        return _isFile;
    }

    public void setIsFile(boolean _isFile) {
        this._isFile = _isFile;
    }


    public boolean isMetaRequest() {
        return _requestedResourceId.endsWith("/");
    }

    public String getRequestedResourceId(){
        return _requestedResourceId;

    }

    public String output(){
        return _output;
    }

    public String callback(){
        return _callback;
    }

    public boolean flatten(){
        return _flatten;
    }

    public boolean traverse(){
        return _traverse;
    }

    public boolean reCache(){
        return _reCache;
    }


    public String getServiceContextPath(){
        return _request.getContextPath();
    }

    public HttpServletRequest getServletRequest(){
        return _request;
    }


    public String getXmlBase(){

        String forwardRequestUri = (String)_request.getAttribute("javax.servlet.forward.request_uri");
        String requestUrl = _request.getRequestURL().toString();


        if(forwardRequestUri != null){
            String server = _request.getServerName();
            int port = _request.getServerPort();
            String scheme = _request.getScheme();
            requestUrl = scheme + "://" + server + ":" + port + forwardRequestUri;
        }

        String xmlBase = requestUrl;

        _log.debug("@xml:base='{}'", xmlBase);
        return xmlBase;
    }



    /**
     * Worker method to simplify code to ingest the query parakmeters for the request
     * @param name The case-insensitive name of the parameter to retrieve
     * @param pmap The first value of the parameter from the query string, null if the parameter is not present or has
     *             no value.
     * @return
     */
    private String getParamValue(String name, TreeMap<String,String[]> pmap){
        String vals[];
        String param = null;
        vals = pmap.get(name);

        if(vals!=null && vals.length >0){
            param = vals[0];
        }
        return param;
    }

    /**
     *
     * @return  The DAP2 form of any variable the client may have requested.
     */
    private String getRequestedDap2VariableName(){

        StringBuilder reqVar = new StringBuilder();
        boolean first = true;
        for(String var: _requestedVariable){
            if(!first)
                reqVar.append(".");
            reqVar.append(var);
            first = false;

        }
        return reqVar.toString();
    }


    /**
     *
     * @return True if the client asked for a variable in the request, false otherwise.
     */
    public boolean variableWasRequested() {
        return !_requestedVariable.isEmpty();
    }


    /**
     *
     * @return A Vector of name staring each of which is a part of the hierarchical name of a variable
     * request by the client.
     */
    public Vector<String> getRequestedVariableNameVector(){
        return _requestedVariable;
    }


    /**
     * Takes the reminder from the BesApi showPathInfo command ingests it into:
     * <ul>
     *     <li>The w10n array constraint (if present)</li>
     *     <li>The DAP2 array constraint (if present)</li>
     *     <li>The DAP2 variable name (which is "." separated))</li>
     *     <li>The w10n identifier  (which is "/" separated))</li>
     * </ul>
     * These values are cached as part of the objects state for retrieval down the road.
     *
     * @param remainder The reminder from the BesApi showPathInfo command
     * @throws BadRequest
     */
    public void ingestPathRemainder(String remainder) throws BadRequest {

        while (remainder.startsWith("/") && remainder.length() > 0)
            remainder = remainder.substring(1);


        StringBuilder dap2ArrayConstraint = new StringBuilder();


        if (remainder.endsWith("[]")) {
            remainder = remainder.substring(0,remainder.lastIndexOf("["));
        }
        else {
            int lastOpenBracket = remainder.lastIndexOf("[");
            int lastCloseBracket = remainder.lastIndexOf("]");
            int lastSlash = remainder.lastIndexOf("/");

            if (
                    lastOpenBracket > lastSlash &&
                            lastCloseBracket > lastOpenBracket &&
                            lastCloseBracket == (remainder.length() - 1)
                    ) {


                // looks like there's an array constraint!
                _w10nArrayConstraint = remainder.substring(lastOpenBracket);
                String w10nArraySubset = _w10nArrayConstraint.replace("[", "").replace("]", "");


                String dimSubsets[] = w10nArraySubset.split(",");

                for (String dimSubset : dimSubsets) {

                    String sss[] = dimSubset.split(":");

                    switch (sss.length) {
                        case 1:
                            dap2ArrayConstraint.append("[").append(sss[0]).append("]");
                            break;

                        case 2:
                            dap2ArrayConstraint.append("[")
                                    .append(sss[0]).append(":").append("1").append(":").append(sss[1])
                                    .append("]");
                            break;

                        case 3:
                            dap2ArrayConstraint.append("[")
                                    .append(sss[0]).append(":").append(sss[2]).append(":").append(sss[1])
                                    .append("]");
                            break;

                        default:
                            throw new BadRequest("Improper arrays subset syntax " + remainder);
                    }


                }

                remainder = remainder.substring(0, lastOpenBracket);

            }
        }



        if (remainder.length() > 0) {
            String vars[] = remainder.split("/");

            for (String var : vars)
                _requestedVariable.add(var);
        }

        _w10nId = remainder;

        if(!_w10nId.isEmpty() && !_w10nId.startsWith("/"))
            _w10nId = "/" + _w10nId;


        //if(isMetaRequest() && !_w10nId.endsWith("/"))
        //   _w10nId += "/";




        _dap2ArrayConstraint = dap2ArrayConstraint.toString();

    }


    /**
     *
     * @return The w10n "path" (as described in section 3 of the specification) for this request.
     */
    public String getW10nResourcePath(){

        Request oreq = new Request(null, _request);

        String pathId = oreq.getServiceLocalId() + _validResourcePath;
        //if(isMetaRequest() && !pathId.endsWith("/"))
        //    pathId += "/";

        return pathId;

    }



    /**
     *
     * @return  The DAP2 array constraint associated with the request.
     */
    public String getDap2ArrayConstraint() {
        return _dap2ArrayConstraint;
    }


    /**
     *
     * @return  The w10n array constraint associated with the request.
     */
    public String getW10nArrayConstraint() {
        return _w10nArrayConstraint;
    }


    /**
     *
     * @return True if there was an array constraint associated with the request, false otherwise.
     */
    public boolean hasArrayConstraint(){
        return !_dap2ArrayConstraint.isEmpty();

    }


    /**
     *
     * @return   The DAP2 constraint expression associated with this request.
     */
    public String getDap2CE(){
        return getRequestedDap2VariableName() + _dap2ArrayConstraint;
    }


    /**
     * Determines the best media type (from the default and the alternates passed in) for the current request.
     * The best is picked and set as part of the objects state for later retrieval.
     *
     * @param defaultMediaType The default media type for the request
     * @param altMediaTypes  The alternate media types for the request
     * @throws NotAcceptable
     */
    public void setBestMediaType(MediaType defaultMediaType, Map<String, MediaType> altMediaTypes) throws OPeNDAPException {

        MediaType bestMT;

        String outputParam = output();

        if(outputParam!=null){
            _log.debug("getMediaTypeForHttpRequest() - Client provided 'output' query parameter {}",outputParam);

            if(defaultMediaType.getName().equalsIgnoreCase(outputParam)) {
                bestMT = defaultMediaType;
                _log.debug("getMediaTypeForHttpRequest() - 'output' query parameter matches default MediaType of '{}'",bestMT.getMimeType());
            }
            else {
                bestMT = altMediaTypes.get(outputParam);
                if(bestMT == null){



                    String msg = "You have requested an unsupported output media type of '" + outputParam+"'";
                    _log.debug("getBestMediaType() - {}",msg);

                    throw new NotAcceptable(msg);

                }
                _log.debug("getMediaTypeForHttpRequest() - 'output' query parameter matches MediaType of '{}'",bestMT.getMimeType());
            }
        }
        else {
            bestMT = getMediaTypeForHttpRequest(_request, defaultMediaType, altMediaTypes);

        }

        _bestResponseMediaType =  bestMT;


    }

    public MediaType getBestMediaType(){
        return _bestResponseMediaType;
    }



    /**
     * THis is where we do the Server-driven HTTP Content Negotiation.
     * @param request The client's incoming request
     * @param defaultMediaType The default media type for the request
     * @param altMediaTypes  The alternate media types for the request
     * @return  Most appropriate MediaType for the response
     * @throws java.util.NoSuchElementException
     */
    private MediaType getMediaTypeForHttpRequest(HttpServletRequest request, MediaType defaultMediaType, Map<String,MediaType> altMediaTypes) {


        MediaType bestType = defaultMediaType;

        Vector<MediaType> clientMediaTypes = getClientMediaTypes(request);

        if(clientMediaTypes.isEmpty()) {
            _log.debug("getMediaTypeForHttpRequest() - Client did not provide an Accept header, returning default MediaType {}",bestType.getMimeType());
            return bestType;
        }


        for(MediaType mt: clientMediaTypes){
            _log.debug("getMediaTypeForHttpRequest() - Clients accepts media type: {}", mt.toString());
        }

        TreeSet<MediaType> matchingTypes = new TreeSet<>();

        for(MediaType mt: clientMediaTypes){
            if(mt.getMimeType().equalsIgnoreCase(defaultMediaType.getMimeType())){
                matchingTypes.add(defaultMediaType);
            }
            else if(mt.getPrimaryType().equalsIgnoreCase(defaultMediaType.getPrimaryType()) &&
                    mt.getSubType().equalsIgnoreCase("*")){
                matchingTypes.add(defaultMediaType);
            }
            else if(mt.getPrimaryType().equalsIgnoreCase("*") &&
                    mt.getSubType().equalsIgnoreCase("*")){
                matchingTypes.add(defaultMediaType);
            }

            for(MediaType altType : altMediaTypes.values()){

                if(mt.getMimeType().equalsIgnoreCase(altType.getMimeType())){
                    matchingTypes.add(altType);
                }
                else if(mt.getPrimaryType().equalsIgnoreCase(altType.getMimeType()) &&
                        mt.getSubType().equalsIgnoreCase("*")){
                    matchingTypes.add(altType);
                }
            }
        }

        if(!matchingTypes.isEmpty()){
            bestType = matchingTypes.last();
            _log.debug("getMediaTypeForHttpRequest() - Best Matching Type:  {}", bestType);
            _log.debug("getMediaTypeForHttpRequest() - Worst Matching Type: {}", matchingTypes.first());
        }


        _log.debug("getMediaTypeForHttpRequest() - Using Media Type: {}", bestType);


        return bestType;

    }


    /**
     * Returns a collection of MediaTypes that the client has indicated an interest in.
     * @param request The clients request (Mmmmm... headers)
     * @return The clients accepted media types.
     */
    private Vector<MediaType> getClientMediaTypes(HttpServletRequest request){
        String acceptsHeaderValue = request.getHeader("Accept");

        _log.debug("Accept: {}", acceptsHeaderValue);

        Vector<MediaType> clientMediaTypes = new Vector<>();

        if(acceptsHeaderValue!=null){
            String[] mimeTypes = acceptsHeaderValue.split(",");

            for(String mimeType: mimeTypes){
                clientMediaTypes.add(new MediaType(mimeType.trim()));
            }
        }

        return clientMediaTypes;
    }

}
