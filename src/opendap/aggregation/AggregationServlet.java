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
import opendap.coreServlet.*;
import opendap.dap.User;
import opendap.http.error.BadRequest;
import opendap.http.mediaTypes.Netcdf3;
import opendap.http.mediaTypes.Netcdf4;
import opendap.http.mediaTypes.TextPlain;
import opendap.io.HyraxStringEncoding;
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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * An 'aggregation servlet developed specifically for the EDSC web client
 *
 * This returns a Zip file containing a number of resources read/produced by the Hyrax
 * BES. The ZIP64(tm) format extensions are used to overcome the size limitations of 
 * the original ZIP format. It can handle a list of resources (typically files) and simply
 * return them, unaltered or translated into netCDF files. In the later case, a 
 * constraint expression can be applied to each resource before the transformation takes
 * place, limiting the variables and/or parts of variables in the resulting netCDF file.
 * Note that for the netCDF format response to work, the BES must be able to read the 
 * format of the original resource (e.g., HDF4). 
 *
 * To methods of interaction are supported: GET and POST. HEAD requests are also supported,
 * although not particularly meaningful.
 *
 * How to call this service:
 * &operation=version: Get the version of this servlet. Includes BES version info too.
 *
 * &operation=file: Given a list of files, return them in a zip archive. Each file is named
 * using &file=<path on the BES>. Both GET and POST are supported. In the case of
 * POST, the &file=<path on the BES> entries may be separated by a newline. The
 * 'file' param must be supplied.
 *
 * &operation=netcdf3: Like 'file' above, but all data are returned in netCDF3 files. In addition,
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
 * &operation=netcdf4: Like 'netcdf3', but the individual response files use netCDF4
 * &operation=ascii: Like 'netcdf4', but using the DAP ASCII format - this is not the
 * tabular data option.
 *
 * &operation=csv: Convert the arrays listed in 'var' into a table, and optionally restrict
 * the values to those defined by the bounding box(es) given using the bbox parameter.
 * All of the values are returned in a single table.
 *
 * Example use:
 *
 * Suppose 'hdf4_files.txt' contains:
 *
 * &operation=netcdf3
 * &ce=Sensor_Azimuth,Sensor_Zenith
 * &file=/data/hdf4/MOD04_L2.A2015021.0020.051.NRT.hdf
 * &file=/data/hdf4/MOD04_L2.A2015021.0025.051.NRT.hdf
 * &file=/data/hdf4/MOD04_L2.A2015021.0030.051.NRT.hdf
 *
 * curl -X POST -d @hdf4_files.txt http://localhost:8080/opendap/aggregation -o data.zip
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
 * TODO Write a /help response?
 * TODO (Hard) Make  parallel requests to the BES.
 * TODO Add an option to return tar.gz: 
 * http://www.selikoff.net/2010/07/28/creating-a-tar-gz-file-in-java/
 *
 * @author James Gallagher <jgallagher@opendap.org>
 */
public class AggregationServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(AggregationServlet.class);
    private static BesApi besApi = new BesApi();
    private static ConcurrentSkipListSet<String> granuleNames = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);

    private static final String INVOCATION_ERROR =
            "I expected the operation to be one of: version, file, netcdf3, netcdf4, ascii or csv but got: ";

    private static final String VERSION_INFO = "Aggregation Interface Version: 1.1";

    private static final String TEXT_PLAIN = "text/plain";
    private static final String APPLICATION_X_ZIP_COMPRESSED = "application/x-zip-compressed";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    private enum ResponseFormat {
        NETCDF_3,
        NETCDF_4,
        ASCII,
        PLAIN
    }

    @Override
    public void init() throws ServletException {
        super.init();


        log.info(VERSION_INFO);

        Runtime runtime = Runtime.getRuntime();

        NumberFormat format = NumberFormat.getInstance();

        StringBuilder sb = new StringBuilder();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        if(log.isInfoEnabled()) {
            sb.append("init() - free memory: ").append(format.format(freeMemory / 1024)).append("\n")
                    .append("init() - allocated memory: ").append(format.format(allocatedMemory / 1024)).append("\n")
                    .append("init() - max memory: ").append(format.format(maxMemory / 1024)).append("\n")
                    .append("init() - total free memory: ").append(format.format((freeMemory + maxMemory - allocatedMemory) / 1024)).append("\n");

            log.info(sb.toString());
        }
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

        if(log.isErrorEnabled()) {
            log.error("{}{}", msg, t.getMessage());

            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));

            log.error("Stack trace: \n{}", writer);
        }
    }

    /**
     * Given a granule name, return a name that will not collide
     * with a file name already used in the Zip file. This is used
     * because Zip files don't tolerate duplicate entries. It's possible
     * to have the same files used with different CEs and users
     * on machines with case insensitive file systems will have duplicate
     * names even when Unix thinks they are unique...
     *
     * @param granule The name of the current granule, which is about
     *                to be added to the Zip file
     * @return Use this name for the granule in the Zip file
     */
    private String getNameForZip(String granule, ResponseFormat format) {

        if (!granuleNames.contains(granule)) {
            // In the simple case, don't fiddle with the name, just record that
            // it's been used.
            granuleNames.add(granule);

            switch (format) {
                case NETCDF_3:
                    granule = granule + ".nc";
                    break;
                case NETCDF_4:
                    granule =  granule + ".nc4";
                    break;
                case ASCII:
                    granule =  granule + ".txt";
                    break;
                case PLAIN:
                    // No change to the name in this case - this WcsResponseFormat is
                    // used by the /file service that simply reads files and dumps
                    // them into the zip output stream.
                    break;
            }

            return granule;
        }
        else {
            // In the more complex case, make a new name and try again...
            int i = 1;
            while (granuleNames.contains(granule + "_" + i))
                ++i;
            return getNameForZip(granule + "_" + i, format);
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
    private void writeAggregationVersion(HttpServletRequest request, HttpServletResponse response,
                                         ServletOutputStream out)
            throws IOException, PPTException, BadConfigurationException, JDOMException, BESError {

        response.setContentType(TEXT_PLAIN);

        // These should always be true
        if (!besApi.isConfigured() || !besApi.isInitialized()) {
            String err = "Aggregation: BES is not configured or not initialized";
            out.println(err);

            log.error(err);
        }

        // This shows the whole request document
        if (log.isDebugEnabled()) {
            StringBuilder echoText = new StringBuilder();
            getPlainText(request, echoText);
            log.debug(echoText.toString());
        }

        // This response, when used in non-debug mode, returns the servlet's version and...
        out.println(VERSION_INFO);

        // ...the bes version info.
        // This is here primarily to show that we are talking to the BES.
        Document version = new Document();
        besApi.getBesVersion("/", version);

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String besVer = xmlo.outputString(version);
        out.print(besVer);

        log.debug("Aggregation: The BES Version information:\n");
        log.debug(besVer);
    }

    /**
     * Helper for writePlainGranules
     *
     * @param granule The granule pathname
     * @param os Write the information to this stream
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void writeSinglePlainGranule(User user, String granule, OutputStream os)
            throws IOException, BadConfigurationException {
    	
        try {
            besApi.writeFile(user, granule, os);
        }
        catch (BESError | PPTException | IOException | BadConfigurationException e) {
            String msg = e.getMessage();
            os.write(msg.getBytes(HyraxStringEncoding.getCharset()));
            log.error("Aggregation Error in writeSinglePlainGranule(): {}", msg);
        }
    }

    /**
     * Use the BES to write several files to its output stream
     *
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param out The ServletOutputStream
     * @throws Exception
     */
    private void writePlainGranules(HttpServletRequest request, HttpServletResponse response, ServletOutputStream out)
            throws IOException, BadConfigurationException {

        Map<String, String[]> queryParameters = request.getParameterMap();

        User user = new User(request);

        response.setContentType(APPLICATION_X_ZIP_COMPRESSED);
        response.setHeader(CONTENT_DISPOSITION, "attachment; filename=file.zip");

        ZipOutputStream zos = new ZipOutputStream(out);

        int numParams = queryParameters.get("file").length;
        for (int i = 0; i < numParams; ++i) {
            String granule = queryParameters.get("file")[i];

            String granuleName = getNameForZip(basename(granule)[1], ResponseFormat.PLAIN);
            try {
                zos.putNextEntry(new ZipEntry(granuleName));
                writeSinglePlainGranule(user, granule, zos);
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
     * Helper - write a single netCDF3 file to the stream. If an error is
     * returned by the BES, use the value of the error message as the file
     * contents.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param granule The granule name in the BES's data tree
     * @param ce Apply this CE to the granule
     * @param os Write the result to this stream
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void writeSingleFormattedGranule(
            User user,
            String granule,
            String ce,
            OutputStream os,
            ResponseFormat format)
            throws IOException, PPTException, BadConfigurationException, BESError {


        String cfHistoryEntry = granule + "?" + ce;

        switch (format) {
            case NETCDF_3:
                // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
                RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, new Netcdf3());
                besApi.writeDap2DataAsNetcdf3(user, granule,  ce, cfHistoryEntry, os);
                break;
            case NETCDF_4:
                // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
                RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, new Netcdf4());
                besApi.writeDap2DataAsNetcdf4(user, granule, ce, cfHistoryEntry, os);
                break;
            case ASCII:
                // Stash the Media type in case there's an error. That way the error handler will know how to encode the error.
                RequestCache.put(OPeNDAPException.ERROR_RESPONSE_MEDIA_TYPE_KEY, new TextPlain());
                besApi.writeDap2DataAsAscii(user, granule, ce, os);
                break;
            default:
            	break;
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
     * @param request The HttpServletRequest
     * @param response The HttpServletResponse
     * @param out The ServletOutputStream
     * @param format
     * @throws BadRequest
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws BESError
     */
    private void writeFormattedGranules(HttpServletRequest request, HttpServletResponse response,
                                        ServletOutputStream out, ResponseFormat format)
            throws BadRequest, IOException, PPTException, BadConfigurationException, BESError {

        // This ctor vets the params and throws an Exception if there are problems
        AggregationParams params = new AggregationParams(request.getParameterMap());
        int N = params.getNumberOfFiles();

        response.setContentType(APPLICATION_X_ZIP_COMPRESSED);
        response.setHeader(CONTENT_DISPOSITION, "attachment; filename=netcdf3.zip");

        User user = new User(request);

        ZipOutputStream zos = new ZipOutputStream(out);

        for (int i = 0; i < N; ++i) {
            String granule = params.getFilename(i);
            String ce = params.getArrayCE(i);

            try {
                zos.putNextEntry(new ZipEntry(getNameForZip(basename(granule)[1], format)));
                writeSingleFormattedGranule(user, granule, ce, zos, format);
                zos.closeEntry();
            } catch (ZipException ze) {
                out.println("Aggregation Error: " + ze.getMessage());

                logError(ze, "in writeFormattedGranules():");
            }
        }

        zos.finish();
    }

    /**
     * Get the values from a number of data files in a single CSV-format table.
     *
     * @param request The HttpServletRequest object
     * @param response The HttpServletResponse object
     * @param out where to write the response
     * @throws Exception
     */
    private void writeGranulesSingleTable(
            HttpServletRequest request,
            HttpServletResponse response,
            ServletOutputStream out)
            throws BadRequest, PPTException, BadConfigurationException, BESError, IOException {

        // This ctor vets the params and throws an Exception if there are problems
        AggregationParams params = new AggregationParams(request.getParameterMap());
        int numFiles = params.getNumberOfFiles();

        response.setContentType(TEXT_PLAIN);

        User user = new User(request);

        FilterAsciiHeaderStream filter = new FilterAsciiHeaderStream(out);
        filter.set(false);// let the first set of header lines through

        for (int i = 0; i < numFiles; ++i) {
            String granule = params.getFilename(i);
            String ce = params.getTableCE(i);

            try {
                writeSingleFormattedGranule(user, granule, ce, filter, ResponseFormat.ASCII);
                filter.set(true);// filter out all the remaining header lines
            } catch (IOException ioe) {
                out.println("Aggregation error building table of values: " + ioe.getMessage());
                logError(ioe, "in writeGranulesSingleTable():");
            }
        }
    }

    /**
     * Do the work of processing the request and returning a response. This code
     * is broken out so that the doGet() method can handle redirecting URLs that
     * don't end in a slash, which will break web pages that might be associated
     * with this service. The doPut(), on the other hand does not care and should
     * not be issuing redirects (doing so will break curl, e.g., when it is used
     * with -X POST.
     *
     * @param request The HttpServletRequest object
     * @param response The HttpServletResponse object
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response) {
        // forget the names used in/by previous requests
        granuleNames.clear();


        try {
            ServletOutputStream out = response.getOutputStream();

            RequestCache.openThreadCache();

            String requestKind = request.getParameter("operation");
            if (requestKind == null)
                requestKind = "nothing - the operation parameter was not supplied.";

            log.debug("Aggregation: The requested operation is: {}", requestKind);

            switch (requestKind) {
                case "version":
                    writeAggregationVersion(request, response, out);
                    break;
                case "file":
                    writePlainGranules(request, response, out);
                    break;
                case "netcdf3":
                    writeFormattedGranules(request, response, out, ResponseFormat.NETCDF_3);
                    break;
                case "netcdf4":
                    writeFormattedGranules(request, response, out, ResponseFormat.NETCDF_4);
                    break;
                case "ascii":
                    writeFormattedGranules(request, response, out, ResponseFormat.ASCII);
                    break;
                case "csv":
                    writeGranulesSingleTable(request, response, out);
                    break;
                default:
                    throw new BadRequest(INVOCATION_ERROR + requestKind);
            }
            out.flush();
        }
        catch (Throwable t) {
            OPeNDAPException.anyExceptionHandler(t, this,  response);
            logError(t, "in doGet():");
        }
        finally {
            RequestCache.closeThreadCache();
        }

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {

        log.debug("doGet() - BEGIN");

        if (request.getPathInfo() == null) {
            try {
                response.sendRedirect(Scrub.urlContent(request.getRequestURI() + "/"));
                log.debug("Aggregation: Sent redirect to make the web page work!");
            }
            catch (IOException e) {
                OPeNDAPException.anyExceptionHandler(e, this,  response);
                logError(e, "in doGet():");
            }
            return;
        }

        processRequest(request, response);


        log.debug("doGet() - END");
    }

    @Override
    public void doHead(HttpServletRequest request, HttpServletResponse response) {

        log.debug("doHead() - BEGIN");

        try {
            RequestCache.openThreadCache();
            ServletOutputStream out = response.getOutputStream();

            String requestKind = request.getParameter("operation");
            log.debug("Aggregation: The requested operation is: {}", requestKind);
            if (requestKind == null)
                requestKind = "nothing - the operation parameter was not supplied.";

            switch (requestKind) {
                case "version":
                    response.setContentType(TEXT_PLAIN);
                    break;
                case "file":
                    response.setContentType(APPLICATION_X_ZIP_COMPRESSED);
                    response.setHeader(CONTENT_DISPOSITION, "attachment; filename=file.zip");
                    break;
                case "netcdf3":
                    response.setContentType(APPLICATION_X_ZIP_COMPRESSED);
                    response.setHeader(CONTENT_DISPOSITION, "attachment; filename=netcdf3.zip");
                    break;
                case "netcdf4":
                    response.setContentType(APPLICATION_X_ZIP_COMPRESSED);
                    response.setHeader(CONTENT_DISPOSITION, "attachment; filename=netcdf4.zip");
                    break;
                case "ascii":
                    response.setContentType(APPLICATION_X_ZIP_COMPRESSED);
                    response.setHeader(CONTENT_DISPOSITION, "attachment; filename=ascii.zip");
                    break;
                case "csv":
                    response.setContentType(TEXT_PLAIN);
                    break;

                default:
                    throw new ServletException(INVOCATION_ERROR + requestKind);
            }
            out.flush();
        }
        catch (Throwable t) {
            OPeNDAPException.anyExceptionHandler(t, this,  response);
            logError(t, "in doHead():");
        }
        finally {
            RequestCache.closeThreadCache();
        }


        log.debug("doHead() - END");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {

        log.debug("doPost() - BEGIN");

        processRequest(request, response);


        log.debug("doPost() - END");
    }

    /**
     * Copied from the EchoServlet written by Nathan, this was used to debug
     * processing GET and POST requests and is called only when the 'version'
     * response is requested and logback is in DEBUG mode.
     *
     * @param request The HttpServletRequest
     * @param out Write to this value-result parameter.
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
