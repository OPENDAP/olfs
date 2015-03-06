/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
 * // Author: James Gallagher <jgallagher@opendap.org>
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

package opendap.aggregation;

import java.util.Map;
import java.util.StringTokenizer;

/**
 * Manage the Query parameters passed to the Aggregation servlet.
 *
 * This class tests the query parameters to see if they are valid,
 * parses them if needed and can be used to iteratively retrieve
 * their values.
 *
 * The Aggregation servlet accepts three parameters: file, var and bbox.
 * The parameter 'file' must be given; if it appears N times, then the
 * servlet will return N netcdf files, one for each instance of 'file'.
 * The 'var' parameter may appear one or N times. If it appears once,
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

    // When there is only one 'var' (and optional 'bbox') build the ce once and reuse
    private String _ce = "";

    // Very OO, but I'm not sure it's an improvement over the older way...
    // This does cut down on extra code.
    private final BBoxConstraintBuilder _roiConstraintBuilder = new RoiConstraintBuilder();
    private final BBoxConstraintBuilder _tabularConstraintBuilder = new TabularConstraintBuilder();

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

        N = validateParams();
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
    private int validateParams() throws Exception
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
        // one_bbox = _has_bbox && _queryParameters.get("bbox").length == 1;

        if (_has_bbox && _queryParameters.get("bbox").length != _queryParameters.get("var").length)
            throw new Exception("Incorrect number of 'bbox' parameters (found " + _queryParameters.get("bbox").length
                    + " instances of 'bbox' and " + _queryParameters.get("var").length + " of 'var' - they should match).");

        return N;
    }

    /**
     * These are the states for a simple parser for the bbox syntax we are using
     * to hide the somewhat complex server function syntax from the caller.
     */
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
     * Interface to help modularize the parser so that one state machine
     * can parse the 'bbox expression' and make two different kinds of
     * output, depending on other parameters. This could be done using
     * lambdas or a boolean/enum, but I went with this...
     * (lambdas == Java 8 and I don't want that dependency.)
     */
    private interface BBoxConstraintBuilder {
        void outputClause(int bboxNumber,  String var, String min, String max, StringBuilder ce);
        void completeExpression(int bboxNumber, StringBuilder ce);
    }

    /**
     * Build a little subexpression that the caller can mash into a call to
     * the roi() function. The result is box(var,min,max) or
     * bbox_union(box(var,min,max),bbox(next_var,min,max), ...,"intersection")
     */
    private class RoiConstraintBuilder implements BBoxConstraintBuilder {
        public void outputClause(int bboxNumber, String var, String min, String max, StringBuilder ce) {
            // Write out the comma as a separator - don't write this with only one bbox()
            if (bboxNumber > 1)
                ce.append(",");

            ce.append("bbox(").append(var).append(",").append(min).append(",").append(max).append(")");
        }

        public void completeExpression(int bboxNumber, StringBuilder ce) {
            // This code takes two or more bbox() calls and wraps them in bbox_union(... , "intersection")
            if (bboxNumber > 1)
                ce.insert(0, "bbox_union(");
            if (bboxNumber > 1)
                ce.append(",\"intersection\")");
        }
    }

    /**
     * Build a little subexpression that can be used to get a Sequence/CSV
     * response from the BES. The result is '&var>=min&var<=max' for one
     * var or ''&var1>=min&var1<=max'&var2>=min&var2<=max&...' for N vars
     */
    private class TabularConstraintBuilder implements BBoxConstraintBuilder {
        public void outputClause(int bboxNumber, String var, String min, String max, StringBuilder ce) {
            ce.append("&").append(var).append(">=").append(min);
            ce.append("&").append(var).append("<=").append(max);
        }

        public void completeExpression(int bboxNumber, StringBuilder ce) {
        }
    }

    /**
     * Parse the bounding box parameter value. This is a String that contains
     * one or more bounding box restrictions.
     *
     * The 'bbox' parameter has the syntax (clause+) where each clause looks like
     * [min_val, variable, max_val], where the [] and comma are literal characters.
     * These clauses individually translate into BES server function calls to
     * bbox(variable, min_val, max_val). When there are two or more calls to bbox(),
     * those should be wrapped in a single call to bbox_union(..., "intersection")
     * when the ultimate goal is to call the roi() server function.
     *
     * This method parses the 'bbox' parameter and builds the necessary BES server
     * function call string.
     *
     * The return from this method will be the comma-separated list of bbox() calls
     * optionally wrapped in a bbox_union(..., "intersection") call (e.g.,
     * "bbox_union(bbox(lat,10,30),bbox(lon,70,90),"intersection")").
     *
     * @param bbox The bbox parameter value to be parsed.
     * @return The bbox expression.
     */
    private String parseBBox(String bbox, BBoxConstraintBuilder builder) throws Exception {
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

                    builder.outputClause(bboxNumber, var, min, max, ce);

                    state = States.start;
                    min = "";
                    max = "";
                    var= "";
                    break;
            }
        }

        builder.completeExpression(bboxNumber, ce);

        return ce.toString();
    }

    /**
    /**
     * Return the number of files/granules in the current set of query
     * parameters.
     *
     * @return The number of files/granules.
     */
    public int getNumberOfFiles() {
        return N;
    }

    /**
     * Return the name of the granule/file associated with the ith
     * instance of the 'file' parameter. This name is one that the BES will used
     * to access the data - it may be a pathname or some other token.
     *
     * @param i The instance number
     * @return The file/path/granule name as a string.
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
     * The bbox parameter is optional, but if given, must match the number
     * of the 'var' parameter. The 'var' parameter is required, but there
     * may be either one or N instances.
     *
     * If one instance is given for 'var,' use that for every CE. if N instances
     * are given, use the ith value for the the ith CE. Note that while the
     * 'var' parameter is intended to be a comma separated list of variables
     * in the granule, it can actually be a DAP CE. A feature that might be
     * useful...
     *
     * If the parameter 'bbox' is used, then parse that and build a call to the
     * BES Swath subsetting function (named 'roi()').
     *
     * Note that this code is optimized for the case where there is only one
     * instance of var (and bbox if that's present). It will save the CE after
     * building it the first time and simply reuse that value on subsequent
     * calls
     *
     * @param i Build the CE for the ith file/granule
     * @return The correct BES/DAP CE
     */
    public String getArrayCE(int i) throws Exception {
        // Simple case first...
        if (_one_var) {
            // If the field '_ce' is empty, compute. reuse on subsequent calls
            if (_ce.isEmpty()) {
                if (_has_bbox) {
                    String bboxes = _queryParameters.get("bbox")[0].replace("\"", "");// remove surrounding "s
                    _ce = "roi(" + _queryParameters.get("var")[0] + "," + parseBBox(bboxes, _roiConstraintBuilder) + ")";
                }
                else {
                    _ce = _queryParameters.get("var")[0];
                }
            }

            return _ce;
        }
        else {
            if (_has_bbox) {
                String bboxes = _queryParameters.get("bbox")[i].replace("\"", "");// remove surrounding "s
                return "roi(" + _queryParameters.get("var")[i] + "," + parseBBox(bboxes, _roiConstraintBuilder) + ")";
            }
            else {
                return _queryParameters.get("var")[i];
            }
        }
    }

    /**
     * For the ith instance of 'file' build the matching CE to send to
     * the BES when the caller has requested that the values be returned
     * in a single table of CSV values
     *
     * @see AggregationParams#getArrayCE
     * @param i Build the CE for the ith file/granule
     * @return The correct BES/DAP CE
     */
    public String getTableCE(int i) throws Exception {
        // Simple case first...
        if (_one_var) {
            // If the field '_ce' is empty, compute. reuse on subsequent calls
            if (_ce.isEmpty()) {
                if (_has_bbox) {
                    String bboxes = _queryParameters.get("bbox")[0].replace("\"", "");// remove surrounding "s
                    _ce = "tabular(" + _queryParameters.get("var")[0] + ")" + parseBBox(bboxes, _tabularConstraintBuilder);
                }
                else {
                    _ce = "tabular(" + _queryParameters.get("var")[0] + ")";
                }
            }

            return _ce;
        }
        else {
            if (_has_bbox) {
                String bboxes = _queryParameters.get("bbox")[i].replace("\"", "");// remove surrounding "s
                return "tabular(" + _queryParameters.get("var")[i] + ")" + parseBBox(bboxes, _tabularConstraintBuilder);
            }
            else {
                return "tabular(" + _queryParameters.get("var")[i] + ")";
            }
        }
    }

}
