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

import opendap.bes.BESError;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Scrub;
import opendap.dap.User;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * @brief An 'aggregation servlet developed specifically for the EDSC web client
 *
 * This returns a Zip file containing a number of resources read/produced by the Hyrax
 * BES. It can handle a list of resources (typically files) and simply return them,
 * unaltered or translated into netCDF files. In the later case, a constraint expression
 * can be applied to each resource before the transformation takes place, limiting the
 * variables and/or parts of variables in the resulting netCDF file. Note that for the
 * netCDF format response to work, the BES must be able to read the format of the
 * original resource (e.g., HDF4).
 *
 * To methods of interaction are supported: GET and POST. HEAD requests are also supported,
 * although not particularly meaningful.
 *
 * How to call this service:
 * /version: Get the version of this servlet. Includes BES version info too.
 *
 * /file: Given a list of files, return them in a zip archive. Each file is named
 * using &file=<path on the BES>. Both GET and POST are supported. In the case of
 * POST, the &file=<path on the BES> entries may be separated by a newline. The
 * 'file' param must be supplied.
 *
 * /netcdf3: Like 'file' above, but all data are returned in netCDF3 files. In addition,
 * each file has an associated constraint expression, specified using &var=<var1>,
 * <var2>, ..., <varn>. This is required. If only one 'var' param is given, that constraint
 * is applied to every file.  If more than one 'var' param is supplied, the number must
 * match the number of files and they are associated 1-to-1. The /netcdf3 option also
 * accepts an optional &bbox parameter. The format for these is
 * &bbox=<top lat>,<left lon>,<bottom lat>,<right lon>. These values are
 * used as a bounding box for the variables listed in the &var parameter. NB: &var
 * must not be empty if &bbox is used, otherwise, if &var it is empty,
 * the entire file contents will be returned.
 *
 * Example use:
 *
 * Suppose 'hdf4_files.txt' contains:
 *
 * &ce=Sensor_Azimuth,Sensor_Zenith
 * &file=/data/hdf4/MOD04_L2.A2015021.0020.051.NRT.hdf
 * &file=/data/hdf4/MOD04_L2.A2015021.0025.051.NRT.hdf
 * &file=/data/hdf4/MOD04_L2.A2015021.0030.051.NRT.hdf
 *
 * curl -X POST -d @hdf4_files.txt http://localhost:8080/opendap/aggregation/netcdf3 -o data.zip
 *
 * Will call the servlet, using the data in 'hdf4_files.txt' as the contents of the
 * HTTP request document body (i.e., using POST) and save the response to 'data.zip'.
 * Running unizp -t on the response reveals a zip archive with three files:
 *
 * Archive:  data2.zip
 * testing: MOD04_L2.A2015021.0020.051.NRT.hdf.nc   OK
 * testing: MOD04_L2.A2015021.0025.051.NRT.hdf.nc   OK
 * testing: MOD04_L2.A2015021.0030.051.NRT.hdf.nc   OK
 * No errors detected in compressed data of data2.zip.
 *
 * @todo Write a /help response?
 * @todo Add an option to return netCDF4 (using /netcdf4)?
 * @todo (Hard) Make  parallel requests to the BES.
 *
 * @author James Gallagher <jgallagher@opendap.org>
 */
public class AggregationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Logger _log;
    private BesApi _besApi;
    private Set<String> _granuleNames;

    private static final String invocationError = "I expected '/version', '/file', or '/netcdf3', got: ";
    private static final String versionInfo = "Aggregation Interface Version: 1.0";

    @Override
    public void init() throws ServletException {
        super.init();

        _log = LoggerFactory.getLogger(this.getClass());
        _besApi = new BesApi();
        _granuleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        _log.info(versionInfo);

        Runtime runtime = Runtime.getRuntime();

        NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        sb.append("init() - free memory: ").append(format.format(freeMemory / 1024)).append("\n")
            .append("init() - allocated memory: ").append(format.format(allocatedMemory / 1024)).append("\n")
            .append("init() - max memory: ").append(format.format(maxMemory / 1024)).append("\n")
            .append("init() - total free memory: ").append(format.format((freeMemory + maxMemory - allocatedMemory) / 1024)).append("\n");

        _log.info(sb.toString());
    }

    /**
     * Given a pathname, split it into two parts, the basename and the
     * directories leading up to that basename.
     * @param path The path to split up
     * @return A two element String array.
     */
    private static String[] basename(String path) {
        return path.split("/(?=[^/]+$)");
    }

    /**
     * Keep from repeating the same i/o for logging..
     * @param t The thrown object
     * @param msg A message to indicate where this happened. Ex: "in doHead():"
     */
    private void logError(Throwable t, String msg) {
        _log.error("Aggregation: Error {}{}", msg, t.getMessage());

        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        _log.error("Aggregation: Stack trace: ");
        _log.error(writer.toString());
    }

    /**
     * Given a granule name, and return a name that will not collide
     * with a file name already used in the Zip file. This is used
     * because Zip files don't tolerate duplicate entries, it's possible
     * to have the same files used with different CEs and users
     * on machines with case insensitive file systems will have duplicate
     * names even when Unix thinks they are unique...
     *
     * @param granule The name of the current granule, which is about
     *                to be added to the Zip file
     * @return Use this name for the granule in the Zip file
     */
    private String getNameForZip(String granule) {

        if (!_granuleNames.contains(granule)) {
            // In the simple case, don't fiddle with the name, just record that
            // it's been used.
            _granuleNames.add(granule);
            return granule;
        }
        else {
            // In the more complex case, make a new name and try again...
            int i = 1;
            while (_granuleNames.contains(granule + "_" + i))
                ++i;
            return getNameForZip(granule + "_" + i);
        }
    }

    /**
     * Write the Aggregation Service endpoint's version. This was originally
     * written to demonstrate that simple interaction with the BES was working.
     * It may have a use in the future, and I've changed some of the more verbose
     * output so it only happens when logback's DEBUG mode is being used.
     *
     * @param request The HTTP Servlet Request object
     * @param response The HTTP Servlet Response object
     * @param out The Stream
     * @throws java.io.IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws JDOMException
     */
    private void writeAggregationVersion(HttpServletRequest request, HttpServletResponse response, ServletOutputStream out)
            throws IOException, PPTException, BadConfigurationException, JDOMException {

        response.setContentType("text/plain");

        // These should always be true
        if (!_besApi.isConfigured() || !_besApi.isInitialized()) {
            String err = "Aggregation: BES is not configured or not initialized";
            out.println(err);

            _log.error(err);
        }

        // This shows the whole request document
        if (_log.isDebugEnabled()) {
            StringBuilder echoText = new StringBuilder();
            getPlainText(request, echoText);
            _log.debug(echoText.toString());
        }

        // This response, when used in non-debug mode, returns the servlet's version and...
        out.println(versionInfo);

        // ...the bes version info.
        // This is here primarily to show that we are talking to the BES.
        Document version = new Document();
        if (!_besApi.getBesVersion("/", version)) {
            String err = "Aggregation: Error getting version information from the BES";
            out.println(err);

            _log.error(err);
        }

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String besVer = xmlo.outputString(version);
        out.print(besVer);

        _log.debug("Aggregation: The BES Version information:\n");
        _log.debug(besVer);
    }

    /**
     * Helper for writePlainGranules
     *
     * @param granule
     * @param os
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void writeSinglePlainGranule(String granule, OutputStream os)
            throws IOException, PPTException, BadConfigurationException, BESError {

        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        if (!_besApi.writeFile(granule, os, errors)) {
            String msg = new String(errors.toByteArray());
            os.write(msg.getBytes());

            _log.error("Aggregation Error in writeSinglePlainGranule(): {}", msg);
        }
    }

    /**
     * Test getting the BES to write several files to its output stream
     *
     * @param request
     * @param response
     * @param out
     * @throws Exception
     */
    private void writePlainGranules(HttpServletRequest request, HttpServletResponse response, ServletOutputStream out) throws Exception {
        Map<String, String[]> queryParameters = request.getParameterMap();

        response.setContentType("application/x-zip-compressed");
        response.setHeader("Content-Disposition", "attachment; filename=file.zip");

        ZipOutputStream zos = new ZipOutputStream(out);

        int N = queryParameters.get("file").length;
        for (int i = 0; i < N; ++i) {
            String granule = queryParameters.get("file")[i];

            String granuleName = getNameForZip(basename(granule)[1]);
            try {
                zos.putNextEntry(new ZipEntry(granuleName));
                writeSinglePlainGranule(granule, zos);
                zos.closeEntry();
            }
            catch (ZipException ze) {
                out.println("Aggregation Error: " + ze.getMessage());

                logError(ze, "in writePlainGranules():");
            }
        }

        zos.finish();
    }

    /**
     * Are the QueryString parameters sent by the client valid for a
     * 'netcdf3' request? Throw if the params/values are not valid.
     * @param queryParameters The Query Parameters as returned by the
     *                        Http Servlet Request object.
     * @return The number of values for the 'file' parameter.
     */
    private int validateNetcdf3Params(Map<String, String[]> queryParameters) throws Exception
    {
        // Before we start trying to send back netCDF files, we check to make
        // sure that the parameters passed in are valid. There must be N values
        // for 'file' and either 1 or N values for 'var'. If 'bbox' is used, the
        // number must match 'var'.
        int N = queryParameters.get("file").length;
        if (!(queryParameters.get("var").length == 1 || queryParameters.get("var").length == N))
            throw new Exception("Incorrect number of 'var' parameters (found " + N + " instances of 'file' and "
                    + queryParameters.get("var").length + " of 'var').");

        if (queryParameters.get("bbox") != null && queryParameters.get("bbox").length != queryParameters.get("var").length)
            throw new Exception("Incorrect number of 'bbox' parameters (found " + queryParameters.get("bbox").length
                    + " instances of 'bbox' and " + queryParameters.get("var").length + " of 'var' - they should match).");

        return N;
    }

    /**
     * Helper - write a single netCDF3 file to the stream. If an error is
     * returned by the BES, use the value of the error message as the file
     * contents.
     *
     * @param granule The granule name in the BES's data tree
     * @param ce Apply this CE to the granule
     * @param os Write the result to this stream
     * @param maxResponseSize Use a value >0 to indicate an upper limit on
     *                        response sizes.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void writeSingleGranuleAsNetcdf(String granule, String ce, OutputStream os, int maxResponseSize)
            throws IOException, PPTException, BadConfigurationException, BESError {

        String xdap_accept = "3.2";
        ByteArrayOutputStream errors = new ByteArrayOutputStream();

        if (!_besApi.writeDap2DataAsNetcdf3(granule, ce, xdap_accept, maxResponseSize, os, errors)) {
            // Processing errors this way means that if the BES fails to perform
            // some function, e.g., the CE is bad, that error message will be used
            // as the value of the file in the Zip archive. This provides a way for
            // most of a request to work even if some of the files are broken.
            String msg = new String(errors.toByteArray());
            os.write(msg.getBytes());

            _log.error("Aggregation Error in writeSingleGranuleAsNetcdf(): {}", msg);
        }
    }

    /**
     * Write a set of netCDF3 files to the client, wrapped up in a zip file.
     *
     * Each input file in included in the zip archive with the original
     * directory hierarchy information removed (but is protected from
     * name collisions). In addition, since this code will be transforming
     * the original file into a netCDF3, the extension '.nc' is appended,
     * even if the original file was a netCDF file. This provides a primitive
     * indication that the file differs from the original source file.
     *
     * @param request
     * @param response
     * @param out
     * @throws Exception
     */
    private void writeGranulesAsNetcdf(HttpServletRequest request, HttpServletResponse response, ServletOutputStream out)
        throws Exception {

        Map<String, String[]> queryParameters = request.getParameterMap();

        AggregationParams params = new AggregationParams(queryParameters);
        int N = params.getFileNumber();

        // We have valid 'file' and 'var' params...

        response.setContentType("application/x-zip-compressed");
        response.setHeader("Content-Disposition", "attachment; filename=netcdf3.zip");  // TODO Better name?

        User user = new User(request);
        int maxResponse = user.getMaxResponseSize();

        ZipOutputStream zos = new ZipOutputStream(out);

        /*
        String masterCE = "";
        if (queryParameters.get("var").length == 1)
            masterCE = queryParameters.get("var")[0];
*/
        for (int i = 0; i < N; ++i) {
            String granule = params.getFilename(i); //= queryParameters.get("file")[i];
            String ce = params.getCE(i);
            /*
            if (masterCE.equals(""))
                ce = queryParameters.get("var")[i];
            else
                ce = masterCE;
            */
            String granuleName = getNameForZip(basename(granule)[1]) + ".nc";
            try {
                zos.putNextEntry(new ZipEntry(granuleName));
                writeSingleGranuleAsNetcdf(granule, ce, zos, maxResponse);
                zos.closeEntry();
            }
            catch (ZipException ze) {
                out.println("Aggregation Error: " + ze.getMessage());

                logError(ze, "in writeGranulesAsNetcdf():");
            }
        }

        zos.finish();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        _log.debug("doGet() - BEGIN");

        if (request.getPathInfo() == null) {
            response.sendRedirect(Scrub.urlContent(request.getRequestURI() + "/"));
            _log.debug("Aggregation: Sent redirect to make the web page work!");
            return;
        }

        // forget the names used in/by previous requests
        _granuleNames.clear();

        ServletOutputStream out = response.getOutputStream();

        try {
            RequestCache.openThreadCache();

            String requestedResourceId = ReqInfo.getLocalUrl(request);
            _log.debug("Aggregation: The resource ID is: {}", requestedResourceId);

            switch (requestedResourceId) {
                case "/version":
                    writeAggregationVersion(request, response, out);
                    break;
                case "/file":
                    writePlainGranules(request, response, out);
                    break;
                case "/netcdf3":
                    writeGranulesAsNetcdf(request, response, out);
                    break;
                default:
                    throw new Exception(invocationError + requestedResourceId);
            }
		}
        catch (BadConfigurationException | PPTException | JDOMException e) {
			out.println("Aggregation Error: " + e.getMessage());

            logError(e, "in doGet(), caught an BadConfiguration, PPT or JDOM Exception:");
		}
        catch (Exception e) {
            out.println("Aggregation Error: " + e.getMessage());

            logError(e, "in doGet(), caught an Exception:");
        }
        catch (Throwable t) {
            out.println("Aggregation Error: " + t.getMessage());

            logError(t, "in doGet():");
        }
        finally {
        	RequestCache.closeThreadCache();
        }

        out.flush();
        
        _log.debug("doGet() - END");
    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        _log.debug("doHead() - BEGIN");

        ServletOutputStream out = response.getOutputStream();

        try {
            RequestCache.openThreadCache();

            String requestedResourceId = ReqInfo.getLocalUrl(request);

            switch (requestedResourceId) {
                case "/version":
                    response.setContentType("text/plain");
                    break;
                case "/text":
                    response.setContentType("application/x-zip-compressed");
                    response.setHeader("Content-Disposition", "attachment; filename=text.zip");
                    break;
                case "/netcdf3":
                    response.setContentType("application/x-zip-compressed");
                    response.setHeader("Content-Disposition", "attachment; filename=netcdf3.zip");
                    break;
                default:
                    throw new Exception(invocationError + requestedResourceId);
            }
        }
        catch (Throwable t) {
            out.println("Aggregation Error: " + t.getMessage());

            logError(t, "in doHead():");
        }
        finally {
            RequestCache.closeThreadCache();
        }

        out.flush();

        _log.debug("doHead() - END");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        _log.debug("doPost() - BEGIN");

        doGet(request, response);

        _log.debug("doPost() - END");
    }

    /**
     * Copied from the EchoServlet written by Nathan, this was used to debug
     * processing GET and POST requests and is called only when the /version
     * response is requested and logback is in DEBUG mode.
     *
     * @param request
     * @param out
     * @return
     * @throws IOException
     */
    private void getPlainText(HttpServletRequest request, StringBuilder out) throws IOException {

        out.append("\n---------------------------------------------------------------------\n");
        out.append("HTTP Method: ").append(request.getMethod()).append("\n");
        out.append("\n");
        out.append("HTTP Request Headers");
        out.append("\n");
        Enumeration<String> headers = request.getHeaderNames();
        while(headers.hasMoreElements()){
            String headerName = headers.nextElement();
            String headerValue = request.getHeader(headerName);
            out.append("    ").append(headerName).append(": ").append(headerValue).append("\n");
        }

        out.append("\n");

        String queryString = request.getQueryString();
        out.append("Query String and KVP Evaluation\n");
        out.append("\n");
        out.append("  HttpServletRequest.getQueryString(): ").append(queryString).append("\n");
        out.append("\n");
        out.append("  Decoded: ").append(java.net.URLDecoder.decode(queryString == null ? "null" : queryString, "UTF-8")).append("\n");
        out.append("\n");

        int contentLength = request.getContentLength();
        out.append("request.getContentLength(): ").append(contentLength).append("\n");
        out.append("\n");

        String ctype = request.getHeader("Content-Type");

        if(ctype!=null && ctype.equalsIgnoreCase("application/x-www-form-urlencoded")){

            out.append("Content-Type indicates that the request body is form url encoded. Utilizing Servlet API to evaluate parameters.\n");

            out.append("  HttpServletRequest.getParameter()\n");
            out.append("        keyName            value \n");
            Enumeration<String> paramNames = request.getParameterNames();

            while(paramNames.hasMoreElements()){
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                out.append("        ").append(paramName).append(": ").append(paramValue).append("\n");
            }
   
            out.append("  HttpServletRequest.getParameterMap()\n");
            out.append("\n");

            Map paramMap = request.getParameterMap();

            out.append("  ParameterMap is an instance of: ").append(paramMap.getClass().getName()).append("\n");
            out.append("  ParameterMap contains ").append(paramMap.size()).append(" element(s).\n");
            out.append("\n");
            out.append("        keyName            value(s) \n");

            for(Object o:paramMap.keySet()){
                String key = (String) o;

                Object oo =   paramMap.get(key);

                String[] values = (String[]) oo;
                out.append("        ").append(key).append("            ");
                boolean first=true;
                for(String value:values){
                    if(!first)
                        out.append(", ");
                    out.append("'").append(value).append("'");
                    first = false;
                }
                out.append("\n");

            }
            out.append("\n");

        }
        else {
            out.append("Content-Type indicates that the request body is NOT form url encoded.\n");
        }
    }

}
