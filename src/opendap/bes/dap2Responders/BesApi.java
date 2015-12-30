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

package opendap.bes.dap2Responders;

import opendap.bes.*;
import opendap.bes.BES;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.Scrub;
import opendap.dap4.QueryParameters;
import opendap.logging.Timer;
import opendap.logging.Procedure;
import opendap.namespaces.*;
import opendap.ppt.PPTException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Subclass BesApi to get different BES behaviors.
 *
 * You may find that it's usefule to:
 *
 *  - override one (or even all!) of the BesApi.write*() methods.
 *
 *  - override BesApi.getRequestDocument()
 *
 *  - override BesApi.besTransaction(*)
 */
public class BesApi {

    public static final String DAP4_DATA  = "dap";
    public static final String DAP4_DMR   = "dmr";

    public static final String DDS        = "dds";
    public static final String DAS        = "das";
    public static final String DDX        = "ddx";
    public static final String DataDDX    = "dataddx";
    public static final String DAP2_DATA  = "dods";
    public static final String STREAM     = "stream";
    public static final String ASCII      = "ascii";
    public static final String CSV        = "csv";
    public static final String HTML_FORM  = "html_form";
    public static final String INFO_PAGE  = "info_page";
    public static final String XML_DATA   = "xml_data";
    public static final String NETCDF_3   = "netcdf";
    public static final String NETCDF_4   = "netcdf-4";
    public static final String GEOTIFF    = "geotiff";
    public static final String GMLJP2     = "jpeg2000";
    public static final String JSON       = "json";
    public static final String IJSON      = "ijson";
    public static final String W10N       = "w10n";
    public static final String W10N_META      = "w10nMeta";
    public static final String W10N_CALLBACK  = "w10nCallback";
    public static final String W10N_FLATTEN   = "w10nFlatten";
    public static final String W10N_TRAVERSE   = "w10nTraverse";


    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;

    public static final String ERRORS_CONTEXT  = "errors";
    public static final String XML_ERRORS      = "xml";
    public static final String DAP2_ERRORS     = "dap2";
    public static final String DAP4_ERRORS     = "dap4";
    public static final String JSON_ERRORS     = "json";
    public static final String XMLBASE_CONTEXT = "xml:base";

    public static final String STORE_RESULT_CONTEXT  = "store_result";


    public static final String XDAP_ACCEPT_CONTEXT = "xdap_accept";
    public static final String DEFAULT_XDAP_ACCEPT = "2.0";

    public static final String EXPLICIT_CONTAINERS_CONTEXT = "dap_explicit_containers";

    public static final String MAX_RESPONSE_SIZE_CONTEXT = "max_response_size";


    public static final String _regexToMatchLastDotSuffixString = "\\.(?=[^.]*$).*$" ;

    /**
     * The name of the BES Exception Element.
     */
    private static String BES_ERROR = "BESError";


    private boolean _initialized = false;

    private Logger log;


    public BesApi() {
        super();
        init();
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }


    /**
     * Initializes logging for the BesApi class.
     */
    public void init() {

        if (_initialized) return;


        _initialized = true;


    }

    public boolean isInitialized(){ return _initialized; }



    public boolean isConfigured() {
        return BESManager.isConfigured();
    }


    public Document getGroupVersionDocument(String path) throws Exception {
        return BESManager.getGroupVersionDocument(path);
    }

    public Document getCombinedVersionDocument() throws Exception {
        return BESManager.getCombinedVersionDocument();
    }

    public String getAdministrator(String path) throws BadConfigurationException, JDOMException, IOException, PPTException, BESError {

        String adminEmail = "support@opendap.org";


        BES bes = getBES(path);

        Document verDoc = bes.getVersionDocument();

        if(verDoc==null)
            return adminEmail;


        Element besElement = verDoc.getRootElement();

        if(besElement==null)
            return adminEmail;


        Element adminElement = besElement.getChild("Administrator", opendap.namespaces.BES.BES_NS);


        if(adminElement!=null)
            adminEmail = adminElement.getTextTrim();

        return adminEmail;
    }

    //public static void configure(OLFSConfig olfsConfig) throws Exception {

    //    BESManager.configure(olfsConfig.getBESConfig());

    //}


    /**
     * Writes an OPeNDAP DDX for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param xmlBase The request URL.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to errors returned by
     *                             the BES..
     * @return False if the BES returns an error, true otherwise.
     * @throws opendap.bes.BadConfigurationException .
     * @throws opendap.bes.BESError              .
     * @throws java.io.IOException               .
     * @throws opendap.ppt.PPTException              .
     */
    public boolean writeDDX(String dataSource,
                                String constraintExpression,
                                String xdap_accept,
                                String xmlBase,
                                OutputStream os,
                                OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDDXRequest(dataSource, constraintExpression, xdap_accept, xmlBase),
                os,
                err);
    }

    /**
     * Writes an OPeNDAP DAP4 DMR for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param qp The DAP4 query string parameters associated with the request.
     * @param xmlBase The request URL.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to errors returned by
     *                             the BES..
     * @return False if the BES returns an error, true otherwise.
     * @throws opendap.bes.BadConfigurationException .
     * @throws opendap.bes.BESError              .
     * @throws java.io.IOException               .
     * @throws opendap.ppt.PPTException              .
     */
    public boolean writeDMR(String dataSource,
                                QueryParameters qp,
                                String xmlBase,
                                OutputStream os,
                                OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDMRRequest(dataSource,qp,xmlBase),
                os,
                err);
    }

    /**
         * Writes an OPeNDAP DAP4 Data response for the dataSource to the passed stream.
         *
         * @param dataSource           The requested DataSource
         * @param qp  A DAP4 QueryParameters object generated by processing the request
         * @param xmlBase The request URL.
         * @param os                   The Stream to which to write the response.
         * @param err                  The Stream to which to errors returned by
         *                             the BES..
         * @return False if the BES returns an error, true otherwise.
         * @throws BadConfigurationException .
         * @throws BESError              .
         * @throws java.io.IOException               .
         * @throws opendap.ppt.PPTException              .
         */
        public boolean writeDap4Data(String dataSource,
                                    QueryParameters qp,
                                    int maxResponseSize,
                                    String xmlBase,
                                    String contentID,
                                    String mimeBoundary,
                                    OutputStream os,
                                    OutputStream err)
                throws BadConfigurationException, BESError, IOException, PPTException {

            return besTransaction(
                    dataSource,
                    getDap4DataRequest(dataSource,
                            qp,
                            maxResponseSize,
                            xmlBase,
                            contentID,
                            mimeBoundary),
                    os,
                    err);
        }


    public boolean getDDXDocument(String dataSource,
                                  String constraintExpression,
                                  String xdap_accept,
                                  String xmlBase,
                                  Document response)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException, BESError {


        ByteArrayOutputStream ddxString = new ByteArrayOutputStream();


        boolean ret = writeDDX(dataSource,constraintExpression,xdap_accept,xmlBase,ddxString,ddxString);


        SAXBuilder sb = new SAXBuilder();



        Document ddx = sb.build(new ByteArrayInputStream(ddxString.toByteArray()));

        response.detachRootElement();

        response.setRootElement(ddx.detachRootElement());


        return ret;


    }

    public boolean getDMRDocument(String dataSource,
                                          QueryParameters qp,
                                          String xmlBase,
                                          Document response)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException, BESError {


        ByteArrayOutputStream ddxString = new ByteArrayOutputStream();


        boolean ret = writeDMR(dataSource,qp,xmlBase,ddxString,ddxString);


        SAXBuilder sb = new SAXBuilder();



        Document ddx = sb.build(new ByteArrayInputStream(ddxString.toByteArray()));

        response.detachRootElement();

        response.setRootElement(ddx.detachRootElement());


        return ret;


    }

    /**
     * Writes an OPeNDAP DDS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request.
     * @param xdap_accept The version of the DAP the BES is to use to package the
     * reponse.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDDS(String dataSource,
                            String constraintExpression,
                            String xdap_accept,
                            OutputStream os,
                            OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getDDSRequest(dataSource, constraintExpression, xdap_accept),
                os,
                err);
    }



    /**
     * Writes the source data (it is often a file, thus the method name) to
     * the passed stream.
     *
     * @param dataSource     The requested DataSource
     * @param os             The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeFile(String dataSource,
                                 OutputStream os,
                                 OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getStreamRequest(dataSource),
                os,
                err);
    }



    /**
     * Writes an OPeNDAP DAS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDAS(String dataSource,
                            String constraintExpression,
                            String xdap_accept,
                            OutputStream os,
                            OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getDASRequest(dataSource,constraintExpression,xdap_accept),
                os,
                err);
    }




    /**
     * Writes an OPeNDAP DAP2 data response for the dataSource to the
     * passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2Data(String dataSource,
                                     String constraintExpression,
                                     String async,
                                     String storeResult,
                                     String xdap_accept,
                                     int maxResponseSize,
                                     OutputStream os,
                                     OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {


        return besTransaction(
                dataSource,
                getDap2RequestDocument(DAP2_DATA, dataSource, constraintExpression, async, storeResult, xdap_accept, maxResponseSize, null, null, null, DAP2_ERRORS),
                os,
                err);
    }

    /**
     * Writes the NetCDF-3 file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsNetcdf3(String dataSource,
                                          String constraintExpression,
                                          String xdap_accept,
                                          int maxResponseSize,
                                          OutputStream os,
                                          OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2DataAsNetcdf3Request(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF-3 file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters associated wih the request.
]     * @param maxResponseSize
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsNetcdf3(String dataSource,
                                          QueryParameters qp,
                                          int maxResponseSize,
                                          OutputStream os,
                                          OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4DataAsNetcdf3Request(dataSource, qp, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF-4 file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsNetcdf4(String dataSource,
                                          String constraintExpression,
                                          String xdap_accept,
                                          int maxResponseSize,
                                          OutputStream os,
                                          OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2DataAsNetcdf4Request(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }

    /**
     * Writes the NetCDF-4 file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param maxResponseSize
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsNetcdf4(String dataSource,
                                          QueryParameters qp,
                                          int maxResponseSize,
                                          OutputStream os,
                                          OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4DataAsNetcdf4Request(dataSource, qp, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsXml(String dataSource,
                                      String constraintExpression,
                                      String xdap_accept,
                                      int maxResponseSize,
                                      String xmlBase,
                                      OutputStream os,
                                      OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getXmlDataRequest(dataSource,constraintExpression,xdap_accept,maxResponseSize,xmlBase),
                os,
                err);
    }



    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsGmlJpeg2000(String dataSource,
                                              String constraintExpression,
                                              String xdap_accept,
                                              int maxResponseSize,
                                              OutputStream os,
                                              OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2DataAsGmlJpeg2000Request(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }

    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsGmlJpeg2000(String dataSource,
                                       QueryParameters qp,
                                       int maxResponseSize,
                                       OutputStream os,
                                       OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4DataAsGmlJpeg2000Request(dataSource, qp, maxResponseSize),
                os,
                err);
    }



    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsJson(String dataSource,
                                       QueryParameters qp,
                                       int maxResponseSize,
                                       OutputStream os,
                                       OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4DataAsJsonRequest(dataSource, qp, maxResponseSize),
                os,
                err);
    }

    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writePathInfoResponse(String dataSource,
                                         OutputStream os,
                                         OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getShowPathInfoRequestDocument(dataSource),
                os,
                err);
    }

    public boolean getPathInfoDocument(String dataSource,Document response)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException, BESError {


        ByteArrayOutputStream pathInfoDocString = new ByteArrayOutputStream();


        boolean ret = writePathInfoResponse(dataSource,pathInfoDocString,pathInfoDocString);


        SAXBuilder sb = new SAXBuilder();

        Document pathInfoResponseDoc = sb.build(new ByteArrayInputStream(pathInfoDocString.toByteArray()));

        response.detachRootElement();

        response.setRootElement(pathInfoResponseDoc.detachRootElement());


        return ret;


    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsGeoTiff(String dataSource,
                                       QueryParameters qp,
                                       int maxResponseSize,
                                       OutputStream os,
                                       OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4DataAsGeoTiffRequest(dataSource, qp, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsIjsn(String dataSource,
                                       QueryParameters qp,
                                       int maxResponseSize,
                                       OutputStream os,
                                       OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4IjsnDataRequest(dataSource, qp, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsXml(String dataSource,
                                       QueryParameters qp,
                                       int maxResponseSize,
                                       String xmlBase,
                                       OutputStream os,
                                       OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {


        return besTransaction(
                dataSource,
                getDap4RequestDocument(XML_DATA, dataSource, qp, maxResponseSize, xmlBase, null, null, XML_ERRORS),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query parameters submitted with the request.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4MetadataAsJson(String dataSource,
                                            QueryParameters qp,
                                            int maxResponseSize,
                                            OutputStream os,
                                            OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4JsonMetadataRequest(dataSource, qp, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query parameters submitted with the request.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4MetadataAsIjsn(String dataSource,
                                            QueryParameters qp,
                                            int maxResponseSize,
                                            OutputStream os,
                                            OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap4IjsnMetadataRequest(dataSource, qp, maxResponseSize),
                os,
                err);
    }



    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsIjsn(String dataSource,
                                            String constraintExpression,
                                            String xdap_accept,
                                            int maxResponseSize,
                                            OutputStream os,
                                            OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2IjsnDataRequest(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2MetadataAsIjsn(String dataSource,
                                           String constraintExpression,
                                           String xdap_accept,
                                           int maxResponseSize,
                                           OutputStream os,
                                           OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2IjsnMetadataRequest(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }

    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsJson(String dataSource,
                                       String constraintExpression,
                                       String xdap_accept,
                                       int maxResponseSize,
                                       OutputStream os,
                                       OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2DataAsJsonRequest(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2MetadataAsJson(String dataSource,
                                           String constraintExpression,
                                           String xdap_accept,
                                           int maxResponseSize,
                                           OutputStream os,
                                           OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2MetadataAsJsonRequest(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsW10nJson(String dataSource,
                                       String constraintExpression,
                                       String w10nMeta,
                                       String w10nCallback,
                                       boolean w10nFlatten,
                                       String xdap_accept,
                                       int maxResponseSize,
                                       OutputStream os,
                                       OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2DataAsW10nJsonRequest(dataSource, constraintExpression, w10nMeta, w10nCallback, w10nFlatten, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2MetadataAsW10nJson(String dataSource,
                                           String constraintExpression,
                                           String w10nMeta,
                                           String w10nCallback,
                                           boolean w10nFlatten,
                                           boolean w10nTraverse,
                                           String xdap_accept,
                                           int maxResponseSize,
                                           OutputStream os,
                                           OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2MetadataAsW10nJsonRequest(dataSource, constraintExpression, w10nMeta, w10nCallback, w10nFlatten, w10nTraverse, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsGeoTiff(String dataSource,
                                          String constraintExpression,
                                          String xdap_accept,
                                          int maxResponseSize,
                                          OutputStream os,
                                          OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getDap2DataAsGeoTiffRequest(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the ASCII representation _rawOS the  OPeNDAP data response for the
     * dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2DataAsAscii(String dataSource,
                                        String constraintExpression,
                                        String xdap_accept,
                                        int maxResponseSize,
                                        OutputStream os,
                                        OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getDap2DataAsAsciiRequest(dataSource, constraintExpression, xdap_accept, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the ASCII representation _rawOS the  OPeNDAP data response for the
     * dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param qp The DAP4 query string parameters associated with the request.
     * @param os                   The Stream to which to write the response.
     * @param err                  The Stream to which to write errors returned
     *                             by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap4DataAsCsv(String dataSource,
                                      QueryParameters qp,
                                      int maxResponseSize,
                                      OutputStream os,
                                      OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getDap4DataAsCsvRequest(dataSource, qp, maxResponseSize),
                os,
                err);
    }


    /**
     * Writes the HTML data request form (aka the I.F.H.) for the OPeNDAP the
     * dataSource to the passed stream.
     *
     * @param dataSource The requested DataSource
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param url The URL to refernence in the HTML form.
     * @param os  The Stream to which to write the response.
     * @param err The Stream to which to write errors returned by the BES.
     * @return True is everything goes well, false if the BES returns an error.
     * @throws BadConfigurationException .
     * @throws PPTException              .
     * @throws IOException              .
     * @throws BESError              .
     */
    public boolean writeDap2DataRequestForm(String dataSource,
                                            String xdap_accept,
                                            String url,
                                            OutputStream os,
                                            OutputStream err)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        return besTransaction(
                dataSource,
                getHtmlFormRequest(dataSource,xdap_accept,url),
                os,
                err);
    }


    /**
     * Writes the OPeNDAP INFO response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param os         The Stream to which to write the response.
     * @param err        The Stream to which to write errors returned
     *                   by the BES.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public boolean writeDap2HtmlInfoPage(String dataSource,
                                         String xdap_accept,
                                         OutputStream os,
                                         OutputStream err)
            throws BadConfigurationException, BESError, IOException, PPTException {

        return besTransaction(
                dataSource,
                getHtmlInfoPageRequest(dataSource,xdap_accept),
                os,
                err);
    }






    /**
     * Returns the BES version document for the BES serving the passed
     * dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param response The document where the response (be it a catalog
     * document or an error) will be placed.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public boolean getBesVersion(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {

        boolean ret =  besTransaction(dataSource, showVersionRequest(),response);

        return ret;
    }





    /**
     * Returns the BES catalog document for the specified dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param response The document where the response (be it a catalog
     * document or an error) will be placed.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public boolean getBesCatalog(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {


        Procedure timedProc = Timer.start();
        try {
            boolean ret;

            String responseCacheKey = this.getClass().getName() + ".getCatalog(\"" + dataSource + "\")";

            log.info("getCatalog(): Looking for cached copy of BES showCatalog response for responseCacheKey=\"" + responseCacheKey + "\"");

            Object o = RequestCache.get(responseCacheKey);

            if (o == null) {

                Document getCatalogRequest = showCatalogRequest(dataSource);

                ret = besTransaction(dataSource, getCatalogRequest, response);

                if (ret) {
                    // Get the root element.
                    Element root = response.getRootElement();

                    // Find the top level dataset Element
                    Element topDataset = root.getChild("showCatalog", BES_NS).getChild("dataset", BES_NS);

                    topDataset.setAttribute("prefix", getBESprefix(dataSource));

                    RequestCache.put(responseCacheKey, response.clone());
                    log.info("getCatalog(): Cached copy of BES showCatalog response for dataSource: \"" + dataSource + "\"   (responseCacheKey=\"" + responseCacheKey + "\")");


                } else {
                    RequestCache.put(responseCacheKey, new NoSuchDatasource((Document) response.clone()));
                    log.info("getInfo():  BES showInfo response failed, cached a NoSuchDatasource object. responseCacheKey=\"" + responseCacheKey + "\"");
                }

                return ret;
            } else {
                log.info("getCatalog(): Using cached copy of BES showCatalog.  responseCacheKey=\"" + responseCacheKey + "\"");


                Document result;

                if (o instanceof NoSuchDatasource) {
                    result = ((NoSuchDatasource) o).getErrDoc();
                    ret = false;
                } else {
                    result = (Document) ((Document) o).clone();
                    ret = true;
                }

                Element root = result.getRootElement();
                root.detach();
                response.setRootElement(root);

                return ret;

            }
        }
        finally {
            Timer.stop(timedProc);

        }

    }


    /**
     * Returns the BES INFO document for the specified dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param response The document where the response (be it datasource
     * information or an error) will be placed.
     * @return True if successful, false if the BES generated an error in
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     */
    public boolean getInfo(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException {


        Procedure timedProc = Timer.start();
        try {
            boolean ret;
            String responseCacheKey = this.getClass().getName() + ".showInfo(\"" + dataSource + "\")";

            log.info("getInfo(): Looking for cached copy of BES showInfo response for data source: \"" + Scrub.urlContent(dataSource) + "\"  (responseCacheKey=\"" + Scrub.urlContent(responseCacheKey) + "\")");

            Object o = RequestCache.get(responseCacheKey);

            if (o == null) {
                log.info("getInfo(): Copy of BES showInfo for  responseCacheKey=\"" + Scrub.urlContent(responseCacheKey) + "\"  not found in cache.");


                Document request = showInfoRequest(dataSource);

                ret = besTransaction(dataSource,
                        request,
                        response);


                if (ret) {
                    // Get the root element.
                    Element responseElement = response.getRootElement();

                    // Find the top level dataset Element
                    Element topDataset = responseElement.getChild("showInfo", BES_NS).getChild("dataset", BES_NS);

                    // Add the prefix attribute for this BES.
                    topDataset.setAttribute("prefix", getBESprefix(dataSource));

                    RequestCache.put(responseCacheKey, response.clone());
                    log.info("getInfo(): Cached copy of BES showInfo response. responseCacheKey=\"" + Scrub.urlContent(responseCacheKey) + "\"");

                } else {
                    RequestCache.put(responseCacheKey, new NoSuchDatasource((Document) response.clone()));
                    log.info("getInfo():  BES showInfo response failed, cached the BES (error) response Document. responseCacheKey=\"" + Scrub.urlContent(responseCacheKey) + "\"");
                }

            } else {
                log.info("getInfo(): Using cached copy of BES showInfo.  responseCacheKey=\"" + Scrub.urlContent(responseCacheKey) + "\" returned an object of type " + o.getClass().getName());


                Document result;

                if (o instanceof NoSuchDatasource) {
                    result = ((NoSuchDatasource) o).getErrDoc();
                    ret = false;
                } else {
                    result = (Document) ((Document) o).clone();
                    ret = true;
                }

                Element root = result.getRootElement();
                root.detach();
                response.setRootElement(root);


            }

            return ret;
        }
        finally {
            Timer.stop(timedProc);
        }

    }

    public static class NoSuchDatasource {
        Document err;
        public NoSuchDatasource(Document besError){
            err = besError;
        }

        public Document getErrDoc(){
            return (Document)err.clone();
        }

    }






    /**
     * Executes a command/response transaction with the BES
     *
     * @param dataSource  The BES datasource that is going to be acccessed. This is used to determine which BES should
     * be used to fulfill the request (In the event that Hyrax is configured to use multiple BESs this string will
     * be used to locate the appropriate BES).
     * @param request   The BES request document.
     * @param response  The document into which the BES response will be placed. If the passed Document object contains
     * conent, then the content will be discarded.
     * @return true if the request is successful, false if there is a problem fulfilling the request.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws JDOMException
     */
    public boolean besTransaction( String dataSource,
                                           Document request,
                                           Document response
                                            )
            throws IOException, PPTException, BadConfigurationException, JDOMException {

        log.debug("besTransaction started.");
        log.debug("besTransaction() request document: \n-----------\n"+showRequest(request)+"-----------\n");

        BES bes = BESManager.getBES(dataSource);
        int bes_timeout_seconds = bes.getTimeout()/1000;
        request.getRootElement().addContent(0,setContextElement("bes_timeout",Integer.toString(bes_timeout_seconds)));
        return bes.besTransaction(request,response);


    }


    /**
     * Executes a command/response transaction with the BES
     *
     * @param dataSource  The BES datasource that is going to be acccessed. This is used to determine which BES should
     * be used to fulfill the request (In the event that Hyrax is configured to use multiple BESs this string will
     * be used to locate the appropriate BES).
     * @param request   The BES request document.
     * @param os   The outputstream to write the BES response to.
     * @param err  The output stream to which BES errors should be written
     * @return true if the request is successful, false if there is a problem fulfilling the request. If false, then
     * any error information will be written to the OutputStream err.
     * @throws BadConfigurationException
     * @throws IOException
     * @throws PPTException
     */
    public boolean besTransaction(String dataSource,
                                             Document request,
                                             OutputStream os,
                                             OutputStream err)
            throws BadConfigurationException, IOException, PPTException {



        log.debug("besTransaction() started.");
        log.debug("besTransaction() request document: \n-----------\n"+showRequest(request)+"-----------\n");


        BES bes = BESManager.getBES(dataSource);
        int bes_timeout_seconds = bes.getTimeout()/1000;
        request.getRootElement().addContent(0,setContextElement("bes_timeout",Integer.toString(bes_timeout_seconds)));
        return bes.besTransaction(request, os, err);
    }




/*##########################################################################*/





    public Element setContainerElement(String name,
                                               String space,
                                               String source,
                                               String type) {

        Element e = new Element("setContainer",BES_NS);
        e.setAttribute("name",name);
        e.setAttribute("space",space);
        if(type.equals(STREAM))
            e.setAttribute("type",type);
        e.setText(source);
        return e;
    }

    public Element defineElement(String name,
                                         String space) {

        Element e = new Element("define",BES_NS);
        e.setAttribute("name",name);
        e.setAttribute("space",space);
        return e;
    }


    public Element containerElement(String name) {
        Element e = new Element("container",BES_NS);
        e.setAttribute("name",name);
        return e;
    }


    public Element constraintElement(String ce) {
        Element e = new Element("constraint",BES_NS);
        e.setText(ce);
        return e;
    }

    public Element dap4ConstraintElement(String ce) {
        Element e = new Element("dap4constraint",BES_NS);
        e.setText(ce);
        return e;
    }

    public Element dap4FunctionElement(String dap4_function) {
        Element e = new Element("dap4function",BES_NS);
        e.setText(dap4_function);
        return e;
    }

    public Element getElement(String type,
                                      String definition,
                                      String url,
                                      String returnAs ) {



        return getElement(type,definition,url,returnAs,null,null);
    }

    public Element getElement(String type,
                                      String definition,
                                      String url,
                                      String returnAs,
                                      String async,
                                      String storeResult ) {

        Element e = new Element("get",BES_NS);

        e.setAttribute("type",type);
        e.setAttribute("definition",definition);
        if(url!=null)
            e.setAttribute("url",url);
        if(returnAs!=null)
            e.setAttribute("returnAs",returnAs);
        if(async!=null)
            e.setAttribute("async",async);
        if(storeResult!=null)
            e.setAttribute("store_result",storeResult);
        return e;
    }


    public Element setContextElement(String name, String value) {
        Element e = new Element("setContext",BES_NS);
        e.setAttribute("name",name);
        e.setText(value);
        return e;
    }



/*##########################################################################*/


    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDX is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the dap that should be used to build the
     * response.
     * @param xmlBase The request URL.
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDDXRequest(String dataSource,
                                  String ce,
                                  String xdap_accept,
                                  String xmlBase)
            throws BadConfigurationException {

        return getDap2RequestDocument(DDX, dataSource, ce, xdap_accept, 0, xmlBase, null, null, XML_ERRORS);

    }

    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDX is being requested
     * @param qp The DAP4 query string parameters associated wih the request..
     * response.
     * @param xmlBase The request URL.
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDMRRequest(String dataSource,
                                         QueryParameters qp,
                                         String xmlBase)
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DMR, dataSource, qp, 0, xmlBase, null, null, XML_ERRORS);

    }

    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDX is being requested
     * @param qp The DAP4 QueryParameters ingested from the client request.
     * response.
     * @param xmlBase The request URL.
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataRequest(String dataSource,
                                       QueryParameters qp,
                                       int maxResponseSize,
                                       String xmlBase,
                                       String contentID,
                                       String mimeBoundary)
            throws BadConfigurationException {

        Document reqDoc =
                getDap4RequestDocument(
                        DAP4_DATA,
                        dataSource,
                        qp,
                        maxResponseSize,
                        xmlBase,
                        null,
                        null,
                        XML_ERRORS);

        Element req = reqDoc.getRootElement();

        Element getReq = req.getChild("get",BES_NS);

        Element e = new Element("contentStartId",BES_NS);
        e.setText(contentID);
        getReq.addContent(e);

        e = new Element("mimeBoundary",BES_NS);
        e.setText(mimeBoundary);
        getReq.addContent(e);

        return reqDoc;

    }

    /**
     *  Returns the DDS request document for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDDSRequest(String dataSource,
                                  String ce,
                                  String xdap_accept)
            throws BadConfigurationException {

        return getDap2RequestDocument(DDS, dataSource, ce, xdap_accept, 0, null, null, null, DAP2_ERRORS);

    }


    public Document getDASRequest(String dataSource,
                                         String ce,
                                         String xdap_accept)
            throws BadConfigurationException {

        return getDap2RequestDocument(DAS, dataSource, ce, xdap_accept, 0, null, null, null, DAP2_ERRORS);

    }


    public Document getDap2DataAsAsciiRequest(String dataSource,
                                              String ce,
                                              String xdap_accept,
                                              int maxResponseSize)
            throws BadConfigurationException {

        // return getDap2RequestDocument(ASCII, dataSource, ce, xdap_accept, maxResponseSize, null, null, null, XML_ERRORS);
        return getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, ASCII, XML_ERRORS);

    }

    public Document getDap4DataAsCsvRequest(String dataSource,
                                            QueryParameters qp,
                                            int maxResponseSize)
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DATA, dataSource, qp, maxResponseSize, null, null, CSV, XML_ERRORS);

    }


    public Document getHtmlFormRequest(String dataSource,
                                              String xdap_accept,
                                              String URL)
            throws BadConfigurationException {

        return getDap2RequestDocument(HTML_FORM, dataSource, null, xdap_accept, 0, null, URL, null, XML_ERRORS);

    }

    public Document getStreamRequest(String dataSource)
            throws BadConfigurationException{

        return getDap2RequestDocument(STREAM, dataSource, null, null, 0, null, null, null, XML_ERRORS);

    }


    public Document getHtmlInfoPageRequest(String dataSource, String xdap_accept)
            throws BadConfigurationException {

        return getDap2RequestDocument(INFO_PAGE, dataSource, null, xdap_accept, 0, null, null, null, XML_ERRORS);

    }

    public Document getDap2DataAsNetcdf3Request(String dataSource, String ce, String xdap_accept, int maxResponseSize)
            throws BadConfigurationException {


        return getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, NETCDF_3, DAP2_ERRORS);


    }


    public Document getDap4DataAsNetcdf3Request(String dataSource, QueryParameters qp, int maxResponseSize)
            throws BadConfigurationException {


        return getDap4RequestDocument(DAP4_DATA, dataSource, qp, maxResponseSize, null, null, NETCDF_3, DAP4_ERRORS);


    }


    public Document getDap2DataAsNetcdf4Request(String dataSource, String ce, String xdap_accept, int maxResponseSize)
            throws BadConfigurationException {


        return getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, NETCDF_4, DAP2_ERRORS);


    }
    public Document getDap4DataAsNetcdf4Request(String dataSource, QueryParameters qp, int maxResponseSize)
            throws BadConfigurationException {


        return getDap4RequestDocument(DAP4_DATA, dataSource, qp, maxResponseSize, null, null, NETCDF_4, DAP4_ERRORS);


    }




    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getXmlDataRequest(String dataSource,
                                         String ce,
                                         String xdap_accept,
                                         int maxResponseSize,
                                         String xmlBase)
            throws BadConfigurationException {

        return getDap2RequestDocument(XML_DATA, dataSource, ce, xdap_accept, maxResponseSize, xmlBase, null, null, XML_ERRORS);

    }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsGmlJpeg2000Request(String dataSource,
                                                    String ce,
                                                    String xdap_accept,
                                                    int maxResponseSize)
            throws BadConfigurationException {

        return getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, GMLJP2, XML_ERRORS);

    }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsGeoTiffRequest(String dataSource,
                                                String ce,
                                                String xdap_accept,
                                                int maxResponseSize)
            throws BadConfigurationException {

        return getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, GEOTIFF, DAP2_ERRORS);

    }

    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataAsGeoTiffRequest(String dataSource,
                                           QueryParameters qp,
                                           int maxResponseSize
                                           )
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DATA, dataSource, qp, maxResponseSize, null, null, GEOTIFF, DAP4_ERRORS);


    }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataAsGmlJpeg2000Request(String dataSource,
                                             QueryParameters qp,
                                             int maxResponseSize
    )
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DATA, dataSource, qp, maxResponseSize, null, null, GMLJP2, DAP4_ERRORS);


    }




    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsJsonRequest(String dataSource,
                                             String ce,
                                             String xdap_accept,
                                             int maxResponseSize)
            throws BadConfigurationException {

        return getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, JSON, JSON_ERRORS);

    }
    /**
     *  Returns the JSON encoded DAP2 Metadata response (DDX) for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The DAP2 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2MetadataAsJsonRequest(String dataSource,
                                                 String ce,
                                                 String xdap_accept,
                                             int maxResponseSize
    )
            throws BadConfigurationException {

        return getDap2RequestDocument(DDX, dataSource, ce, xdap_accept, maxResponseSize, null, null, JSON, JSON_ERRORS);


    }



    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsW10nJsonRequest(String dataSource,
                                           String ce,
                                           String w10nMeta,
                                           String w10nCallback,
                                           boolean w10nFlatten,
                                           String xdap_accept,
                                           int maxResponseSize)
            throws BadConfigurationException {

        Document requestDoc =  getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, W10N, JSON_ERRORS);


        if(w10nMeta!=null)
            requestDoc.getRootElement().addContent(1,setContextElement(W10N_META,w10nMeta));

        if(w10nCallback!=null)
            requestDoc.getRootElement().addContent(1,setContextElement(W10N_CALLBACK,w10nCallback));

        if(w10nFlatten)
            requestDoc.getRootElement().addContent(1,setContextElement(W10N_FLATTEN,"true"));

        return requestDoc;

    }
    /**
     *  Returns the JSON encoded DAP2 Metadata response (DDX) for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The DAP2 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2MetadataAsW10nJsonRequest(String dataSource,
                                                 String ce,
                                                 String w10nMeta,
                                                 String w10nCallback,
                                                 boolean w10nFlatten,
                                                 boolean w10nTraverse,
                                                 String xdap_accept,
                                             int maxResponseSize
    )
            throws BadConfigurationException {

        Document requestDoc = getDap2RequestDocument(DDX, dataSource, ce, xdap_accept, maxResponseSize, null, null, W10N, JSON_ERRORS);


        if(w10nMeta!=null)
            requestDoc.getRootElement().addContent(1,setContextElement(W10N_META,w10nMeta));

        if(w10nCallback!=null)
            requestDoc.getRootElement().addContent(1,setContextElement(W10N_CALLBACK,w10nCallback));

        if(w10nFlatten)
            requestDoc.getRootElement().addContent(1,setContextElement(W10N_FLATTEN,"true"));

        if(w10nTraverse)
            requestDoc.getRootElement().addContent(1,setContextElement(W10N_TRAVERSE,"true"));

        return requestDoc;


    }



    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataAsJsonRequest(String dataSource,
                                             QueryParameters qp,
                                             int maxResponseSize
    )
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DATA, dataSource, qp, maxResponseSize, null, null, JSON, JSON_ERRORS);


    }



    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4IjsnDataRequest(String dataSource,
                                           QueryParameters qp,
                                           int maxResponseSize
                                           )
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DATA, dataSource, qp, maxResponseSize, null, null, IJSON, JSON_ERRORS);


    }




    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4JsonMetadataRequest(String dataSource,
                                               QueryParameters qp,
                                               int maxResponseSize)
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DMR, dataSource, qp, maxResponseSize, null, null, JSON, JSON_ERRORS);
        

    }

    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4IjsnMetadataRequest(String dataSource,
                                               QueryParameters qp,
                                               int maxResponseSize)
            throws BadConfigurationException {

        return getDap4RequestDocument(DAP4_DMR, dataSource, qp, maxResponseSize, null, null, IJSON, JSON_ERRORS);


    }


    /**
      *  Returns the XML data response for the passed dataSource
      *  using the passed constraint expression.
      * @param dataSource The data set whose DDS is being requested
      * @param ce The constraint expression to apply.
      * @param xdap_accept The version of the DAP to use in building the response.
      * @param maxResponseSize Maximum allowable response size.
      * @return The DDS request document.
      * @throws BadConfigurationException When no BES can be found to
      * service the request.
      */
     public Document getDap2IjsnDataRequest(String dataSource,
                                            String ce,
                                            String xdap_accept,
                                            int maxResponseSize)
             throws BadConfigurationException {

         return getDap2RequestDocument(DAP2_DATA, dataSource, ce, xdap_accept, maxResponseSize, null, null, IJSON, JSON_ERRORS);

     }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize Maximum allowable response size.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2IjsnMetadataRequest(String dataSource,
                                               String ce,
                                               String xdap_accept,
                                               int maxResponseSize)
            throws BadConfigurationException {

        return getDap2RequestDocument(DDX, dataSource, ce, xdap_accept, maxResponseSize, null, null, IJSON, JSON_ERRORS);

    }



    /**
     * Returns a BES Request document.
     * @param type
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xdap_accept The version of the DAP to use in building the response.
     * @param maxResponseSize Maximum allowable response size.
     * @param xmlBase
     * @param formURL
     * @param returnAs
     * @param errorContext
     * @return
     * @throws BadConfigurationException
     */
    public  Document getDap2RequestDocument(String type,
                                            String dataSource,
                                            String ce,
                                            String xdap_accept,
                                            int maxResponseSize,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
                throws BadConfigurationException {



        return getDap2RequestDocument(type, dataSource,ce, null, null, xdap_accept, maxResponseSize, xmlBase, formURL, returnAs, errorContext);

    }


    public  Document getDap2RequestDocument(String type,
                                            String dataSource,
                                            String ce,
                                            String async,
                                            String storeResult,
                                            String xdap_accept,
                                            int maxResponseSize,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
                throws BadConfigurationException {


        String besDataSource = getBES(dataSource).trimPrefix(dataSource);


        Element e, request = new Element("request", BES_NS);

        String reqID = Thread.currentThread().getName()+":"+ Thread.currentThread().getId();

        request.setAttribute("reqID",reqID);


        if(xdap_accept!=null)
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT,xdap_accept));
        else
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(maxResponseSize>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,maxResponseSize+""));


        request.addContent(setContainerElement("catalogContainer","catalog",besDataSource,type));

        Element def = defineElement("d1","default");
        e = (containerElement("catalogContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs,async,storeResult);

        request.addContent(e);

        return new Document(request);



    }




    public  Document getDap4RequestDocument(String type,
                                            String dataSource,
                                            QueryParameters qp,
                                            int maxResponseSize,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
                throws BadConfigurationException {


        String besDataSource = getBES(dataSource).trimPrefix(dataSource);


        Element e, request = new Element("request", BES_NS);


        String reqID = Thread.currentThread().getName()+":"+ Thread.currentThread().getId();


        request.setAttribute("reqID",reqID);


        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(maxResponseSize>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,maxResponseSize+""));


        request.addContent(setContainerElement("catalogContainer","catalog",besDataSource,type));

        Element def = defineElement("d1","default");
        e = (containerElement("catalogContainer"));

        if(qp.getCe()!=null && !qp.getCe().equals(""))
            e.addContent(dap4ConstraintElement(qp.getCe()));

        if(qp.getFunc()!=null && !qp.getFunc().equals(""))
            e.addContent(dap4FunctionElement(qp.getFunc()));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs,qp.getAsync(),qp.getStoreResultRequestServiceUrl());

        request.addContent(e);

        return new Document(request);

    }


    public  Document getShowPathInfoRequestDocument(String dataSource)
                throws BadConfigurationException {


        String besDataSource = getBES(dataSource).trimPrefix(dataSource);


        Element request = new Element("request", BES_NS);

        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":bes_request]";

        request.setAttribute("reqID",reqID);

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));
        request.addContent(setContextElement(ERRORS_CONTEXT,XML_ERRORS));
        //request.addContent(w10nRequestElement(besDataSource,queryString,mediaType,maxResponseSize));



        request.addContent(showPathInfoRequestElement(besDataSource));

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        log.debug("getShowPathInfoRequestDocument() - Document\n {}",xmlo.outputString(request));

        return new Document(request);

    }


    public Element showPathInfoRequestElement(String resource) {
        Element e;
        Element spi = new Element("showPathInfo",BES_NS);

        spi.setAttribute("node", resource);

        return spi;
    }




    public Document showVersionRequest()
        throws BadConfigurationException {
        return showRequestDocument("showVersion",null);

    }


    public Document showCatalogRequest(String dataSource)
            throws BadConfigurationException {
        return showRequestDocument("showCatalog",dataSource);

    }


    public Document showInfoRequest(String dataSource)
            throws BadConfigurationException {
        return showRequestDocument("showInfo",dataSource);
    }



    public Document showRequestDocument(String type, String dataSource)
            throws BadConfigurationException {


        Element e, request = new Element("request", BES_NS);
        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":bes_request]";
        request.setAttribute("reqID",reqID);
        request.addContent(setContextElement(ERRORS_CONTEXT,XML_ERRORS));

        e = new Element(type,BES_NS);

        if(dataSource!=null){
            String besDataSource = getBES(dataSource).trimPrefix(dataSource);
            e.setAttribute("node",besDataSource);
        }

        request.addContent(e);

        return new Document(request);

    }






    public BES getBES(String dataSource) throws BadConfigurationException {
        BES bes = BESManager.getBES(dataSource);
        return bes;
    }

    public String getBESprefix(String dataSource) throws BadConfigurationException {
        BES bes = BESManager.getBES(dataSource);
        return bes.getPrefix();
    }


     void showRequest(Document request, OutputStream os) throws IOException{
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(request, os);


    }

    String showRequest(Document request) throws IOException{
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        return xmlo.outputString(request);

    }


    /**
     * The besDataSourceId is the relative (local) URL path of the request, minus any requestSuffixRegex detected. So,
     * if the request is for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset
     * minus the DAP
     * requestSuffixRegex (such as .dds, .das, .dap, etc.). If the request is for a collection, then the dataSourceName
     * is the complete local path.
     * of that collection.
     * <p><b>Examples:</b>
     * <ul><li>If the complete URL were: http://opendap.org:8080/opendap/nc/fnoc1.nc.dds<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = fnoc1.nc </li>
     * <li> besDataSourceId = /opendap/nc/fnoc1.nc </li>
     * <li> requestSuffixRegex = dds </li>
     * </ul>
     *
     * <li>If the complete URL were: http://opendap.org:8080/opendap/nc/<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = null </li>
     * <li> besDataSourceId = /opendap/nc/ </li>
     * <li> requestSuffixRegex = "" </li>
     * </ul>
     * </ul>
     *
     * @param relativeUrl The relative URL of the client request. No Constraint expression (i.e. No query section of
     * the URL - the question mark and everything after it.)
     * @param checkWithBes This boolean value instructs the code to ask the appropriate BES if the resulting
     * besDataSourceID is does in fact represent a valid data source in it's world.
     * @return The DataSourceName
     */
    public String getBesDataSourceID(String relativeUrl, boolean checkWithBes){

        Pattern lastDotSuffixPattern= Pattern.compile(_regexToMatchLastDotSuffixString);

        return getBesDataSourceID(relativeUrl,lastDotSuffixPattern,checkWithBes);

    }







    /**
     * The besDataSourceId is the relative (local) URL path of the request, minus any requestSuffixRegex detected. So,
     * if the request is for a dataset (an atom) then the dataSourceName is the local path and the name of the dataset
     * minus the DAP
     * requestSuffixRegex (such as .dds, .das, .dap, etc.). If the request is for a collection, then the dataSourceName
     * is the complete local path.
     * of that collection.
     * <p><b>Examples:</b>
     * <ul><li>If the complete URL were: http://opendap.org:8080/opendap/nc/fnoc1.nc.dds<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = fnoc1.nc </li>
     * <li> besDataSourceId = /opendap/nc/fnoc1.nc </li>
     * <li> requestSuffixRegex = dds </li>
     * </ul>
     *
     * <li>If the complete URL were: http://opendap.org:8080/opendap/nc/<br/>
     * Then the:</li>
     * <ul>
     * <li> dataSetName = null </li>
     * <li> besDataSourceId = /opendap/nc/ </li>
     * <li> requestSuffixRegex = "" </li>
     * </ul>
     * </ul>
     *
     * @param relativeUrl The relative URL of the client request. No Constraint expression (i.e. No query section of
     * the URL - the question mark and everything after it.)
     * @param matchPattern This parameter provides the method with a regex to us in evaluating what part, if any, of
     * the relative URL must be removed to construct the besDataSourceId/
     * @param checkWithBes This boolean value instructs the code to ask the appropriate BES if the resulting
     * besDataSourceID is does in fact represent a valid data source in it's world.
     * @return The besDataSourceId
     */
    public String getBesDataSourceID(String relativeUrl, Pattern matchPattern, boolean checkWithBes){

        log.debug("getBesDataSourceID() - relativeUrl: " + relativeUrl);

        Matcher suffixMatcher = matchPattern.matcher(relativeUrl);

        boolean suffixMatched = false;


        while(!suffixMatcher.hitEnd()){
            suffixMatched = suffixMatcher.find();
            //log.debug("{}", Util.checkRegex(suffixMatcher, suffixMatched));
        }

        String besDataSourceId = null;

        if(suffixMatched){
            int start =  suffixMatcher.start();
            besDataSourceId = relativeUrl.substring(0,start);

            if(checkWithBes){
                log.debug("Asking BES about resource: {}", besDataSourceId);

                try {
                    ResourceInfo dsi = new BESResource(besDataSourceId, this);
                    if (!dsi.isDataset()) {
                        besDataSourceId = null;
                    }
                } catch (Exception e) {
                    log.debug("matches() failed with an Exception. Msg: '{}'", e.getMessage());
                }

            }
        }

        log.debug("getBesDataSourceID() - besDataSourceId: " + besDataSourceId);

        return besDataSourceId;

    }












}
