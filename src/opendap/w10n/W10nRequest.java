package opendap.w10n;

import opendap.bes.dap4Responders.MediaType;
import opendap.dap.Request;
import opendap.http.error.BadRequest;
import opendap.http.error.HttpError;
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

    HttpServletRequest _request;

    private Vector<String> _requestedVariable;
    private String _dap2ArrayConstraint;
    private String _w10nArrayConstraint;


    String  _output;
    String  _callback;
    boolean _reCache;
    boolean _flatten;
    boolean _traverse;

    String _requestedResourceId;


    String _validResourcePath;
    String _w10nId;


    boolean _isData;
    boolean _isDir;
    boolean _isFile;

    MediaType _bestResponseMediaType;


    public W10nRequest(HttpServletRequest request){

        _log = LoggerFactory.getLogger(this.getClass());

        _request = request;

        _requestedVariable = new Vector<>();

        TreeMap<String,String[]> pmap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        @SuppressWarnings("unchecked")
        Map<String, String[]> foo = _request.getParameterMap();
        pmap.putAll(foo);


        _output = getParamValue("output", pmap);
        _callback = getParamValue("callback", pmap);
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






    private String getParamValue(String name, TreeMap<String,String[]> pmap){
        String vals[];
        String param = null;
        vals = pmap.get(name);

        if(vals!=null && vals.length >0){
            param = vals[0];
        }
        return param;
    }

    public String getRequestedVariableName(){

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


    public boolean variableWasRequested() {
        return !_requestedVariable.isEmpty();
    }

    public Vector<String> getRequestedVariableNameVector(){
        return _requestedVariable;
    }


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


    public String getW10nPathIdenitifier(){

        Request oreq = new Request(null, _request);

        String pathId = oreq.getServiceLocalId() + _validResourcePath;
        //if(isMetaRequest() && !pathId.endsWith("/"))
        //    pathId += "/";

        return pathId;

    }


    public String getDap2ArrayConstraint() {
        return _dap2ArrayConstraint;
    }


    public String getW10nArrayConstraint() {
        return _w10nArrayConstraint;
    }


    public boolean hasArrayConstraint(){
        return !_dap2ArrayConstraint.isEmpty();

    }


    public String getDap2CE(){
        return getRequestedVariableName() + _dap2ArrayConstraint;
    }






    public void setBestMediaType(MediaType defaultMediaType, Map<String, MediaType> altMediaTypes) throws HttpError {

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
                    String msg = "Client requested an unsupported output type of " + outputParam;
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
