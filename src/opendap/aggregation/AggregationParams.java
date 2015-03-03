package opendap.aggregation;

import java.util.Map;
import java.util.StringTokenizer;

/**
 * Manage the Query parameters passed to the Aggregation servlet.
 *
 * This class tests the query parameters to see if they are valid,
 * parses them if needed an can be used to iteratively retrieve
 * their values.
 *
 * The Aggregation servlet accepts three parameters: file, var and bbox.
 * The parameter 'file' must be given; if it appear N times, then the
 * servlet will return N netcdf files, one for each instance of 'file'.
 * The 'var' parameter may appear once or N times. If it appears once,
 * that value is used for every instance of 'file'. The 'bbox' parameter
 * may appear zero, one or N times.
 *
 * Created by jimg on 3/3/15.
 */
public class AggregationParams {

    private final Map<String, String[]> _queryParameters;

    // How many values of 'file' are there? Returned by the validator.
    private int N = 0;

    // These are all set (internally) by the validator
    private boolean _one_var = false;
    private boolean _has_bbox = false;
    private boolean _one_bbox = false;

    /**
     * If the parameters do not contain any instances of 'file', that is an
     * error given that this code will only be used with the /netcdf3 version
     * of the service. The 'file' parameter is not needed when /version is called
     * but this code is not used in that case.
     *
     * @param queryParameters The parameters as extracted from the HTTP request
     *                        object.
     * @throws Exception If 'file' is missing.
     */
    public AggregationParams(Map<String, String[]> queryParameters) throws Exception {
        _queryParameters = queryParameters;

        N = validateNetcdf3Params();
    }

    /**
     * Before we start trying to send back netCDF files, we check to make
     * sure that the parameters passed in are valid. There must be N values
     * for 'file' and either 1 or N values for 'var'. If 'bbox' is used, the
     * number must match 'var'.
     *
     * This method sets some internal 'housekeeping' stuff that will simplify
     * implementing the 'getters'. It also returns the number of values of the
     * 'file' param.
     *
     * @return How many 'file' values are there?
     * @throws Exception Throw if there are any number of fails in the parameter
     * list.
     */
    private int validateNetcdf3Params() throws Exception
    {

        if (_queryParameters.get("file") == null)
            throw new Exception("There must be at least one instance of the 'file' parameter when calling this service.");

        if (_queryParameters.get("var") == null)
            throw new Exception("There must be at least one instance of the 'var' parameter when calling this service.");

        int N = _queryParameters.get("file").length;

         _one_var = _queryParameters.get("var").length == 1;

        if (!(_one_var || _queryParameters.get("var").length == N))
            throw new Exception("Incorrect number of 'var' parameters (found " + N + " instances of 'file' and "
                    + _queryParameters.get("var").length + " of 'var').");

        _has_bbox = _queryParameters.get("bbox") != null;
        _one_bbox = _has_bbox && _queryParameters.get("bbox").length == 1;

        if (_has_bbox && _queryParameters.get("bbox").length != _queryParameters.get("var").length)
            throw new Exception("Incorrect number of 'bbox' parameters (found " + _queryParameters.get("bbox").length
                    + " instances of 'bbox' and " + _queryParameters.get("var").length + " of 'var' - they should match).");

        return N;
    }

    private enum States {
        start,
        lbracket,
        rbracket,
        comma,
        variable,
        min_val,
        max_val,
        error
    }

    /**
     * Given that the parameter list has one or more 'bbox' values, parse the string.
     *
     * The 'bbox' parameter value has the syntax [min_val, variable, max_val] for
     * each variable and this clause translates directly to bbox(variable, min_val, max_val).
     * There can be any number of clauses - they should not repeat variable names.
     * The meaning of including more than one [...] clause is to form the intersection
     * of the bounding boxes they define.
     *
     * The return from this method will be the comma-separated list of bbox() calls
     * wrapped ina bbox_union(..., "intersection") call (e.g.,
     * "bbox_union(bbox(lat,10,30),bbox(lon,70,90),"intersection")").
     *
     * @param bbox The bbox parameter value to be parsed.
     * @return The bbox expression.
     */
    private String parseBBox(String bbox) throws Exception {
        if (bbox.isEmpty())
            return "";

        StringTokenizer tokens = new StringTokenizer(bbox, "[],", true);

        StringBuilder ce = new StringBuilder("");

        States state = States.start;
        String min = "";
        String var = "";
        String max = "";

        // Track the number of bbox() calls. if > 1, wrap them in bbox_union()
        int bboxNumber = 0;
        // the states rbracket and error don't read from the token stream, so
        // run their code even when the stream is empty to ensure the last clause
        // parses.
        while (tokens.hasMoreElements() || state == States.rbracket || state == States.error) {
            String token;

            switch(state) {
                case start:
                    token = tokens.nextToken();
                    if (token.equals("["))
                        state = States.lbracket;
                    else
                        state = States.error;
                  break;

                case lbracket:
                    token = tokens.nextToken();
                    if (token.contains("[],"))
                        state = States.error;
                    else {
                        min = token;
                        state = States.min_val;
                    }
                    break;

                case min_val:
                    token = tokens.nextToken();
                    if (token.equals(","))
                        state = States.comma;
                    else
                        state = States.error;
                    break;

                case comma:
                    token = tokens.nextToken();
                    if (token.contains("[],"))
                        state = States.error;
                    else if (var.equals("")) {
                        var = token;
                        state = States.variable;
                    }
                    else {
                        max = token;
                        state = States.max_val;
                    }
                    break;

                case variable:
                    token = tokens.nextToken();
                    if (token.equals(","))
                        state = States.comma;
                    else
                        state = States.error;
                    break;

                case max_val:
                    token = tokens.nextToken();
                    if (token.equals("]"))
                        state = States.rbracket;
                    else
                        state = States.error;
                    break;

                case error:
                    throw new Exception("The value of the parameter 'bbox' does not parse: '" + bbox + "'");

                case rbracket:
                    ++bboxNumber;// track the number of bbox() calls

                    // Write out the comma as a separator - don't write this with only one bbox()
                    if (bboxNumber > 1)
                        ce.append(",");

                    ce.append("bbox(").append(var).append(",").append(min).append(",").append(max).append(")");

                    state = States.start;
                    min = "";
                    max = "";
                    var= "";
                    break;
            }
        }

        // This code takes two or more bbox() calls and wraps them in bbox_union(... , "intersection")
        if (bboxNumber > 1)
            ce.insert(0, "bbox_union(");
        if (bboxNumber > 1)
            ce.append(",\"intersection\")");

        return ce.toString();
    }

    /**
     * Return the number of files/granules in the current set of query
     * parameters.
     *
     * @return The number of files/granules.
     */
    public int getFileNumber() {
        return N;
    }

    /**
     * Return the name of the granule/file associated with the ith
     * instance of 'file'. This name is one that the BES will use
     * to access the data - it may be a filename or some other token.
     *
     * @param i The instance number
     * @return The file/granule name as a string.
     */
    public String getFilename(int i) {
        return _queryParameters.get("file")[i];
    }

    /**
     * For the ith instance of 'file' build the matching CE to send to
     * the BES. The caller of the aggregation service may not know anything
     * about DAP constraint expressions; take the information from the
     * 'var' and 'bbox' parameters and build the correct CE to pass into
     * the BES.
     *
     * The bbox parameter is optional, but if given must match the number
     * of the 'var' parameter. The 'var' parameter is required, but there
     * may be either one or N instances.
     *
     * If one instance is given for 'var' use that for every CE. if N instances
     * are given, use the ith value for the the ith CE. Note that while the
     * 'var' parameter is intended to be a comma separated list of variables
     * in the granule, it can actually be a DAP CE. A feature that might be
     * useful...
     *
     * TDB bbox code
     * @param i Build the CE for the ith file/granule
     * @return The correct BES/DAP CE
     */
    public String getCE(int i) throws Exception {
        // Simple case first...
        if (!_has_bbox) {
            if (_one_var)
                return _queryParameters.get("var")[0];
            else
                return _queryParameters.get("var")[i];
        }
        else {
            String bboxes;
            if (_one_bbox)
                bboxes = _queryParameters.get("bbox")[0];
            else
                bboxes = _queryParameters.get("bbox")[i];

            bboxes = bboxes.replace("\"", "");// remove surrounding "s
            bboxes = parseBBox(bboxes);

            String vars;
            if (_one_var)
                vars = _queryParameters.get("var")[0];
            else
                vars = _queryParameters.get("var")[i];

            // Here's what we're shooting for:
            // roi( <vars>, <bboxes expr> )
            // where <vars> is the list of variables passed in using 'var'
            // and <bboxes expr> is a list of bbox() calls built by parsing the
            // stuff passed in using 'bbox'. Example:
            // roi ( Lat, Lon, SST, bbox_union( bbox(Lat, 30, 50), bbox(Lon,70, 100), "intersection") )

            return "roi(" + vars + "," + bboxes + ")";
        }
    }
}
