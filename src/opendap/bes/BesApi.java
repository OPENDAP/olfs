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

package opendap.bes;

import opendap.PathBuilder;
import opendap.auth.EarthDataLoginAccessToken;
import opendap.auth.UserProfile;
import opendap.bes.caching.BesNodeCache;
import opendap.coreServlet.ByteArrayOutputStreamTransmitCoordinator;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.TransmitCoordinator;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import opendap.logging.ServletLogUtil;
import opendap.logging.Procedure;
import opendap.logging.Timer;
import opendap.ppt.PPTException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class BesApi implements Cloneable {

    private static Logger staticLog = LoggerFactory.getLogger(BesApi.class);

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
    public static final String COVJSON   = "covjson";
    public static final String IJSON      = "ijson";
    public static final String W10N       = "w10n";
    public static final String W10N_META      = "w10nMeta";
    public static final String W10N_CALLBACK  = "w10nCallback";
    public static final String W10N_FLATTEN   = "w10nFlatten";
    public static final String W10N_TRAVERSE   = "w10nTraverse";
    public static final String SHOW_BES_KEY    = "showBesKey";
    public static final String VALUE           = "value";
    public static final String BES_SUPPORT_EMAIL_KEY = "SupportEmail";
    public static final String BES_SERVER_ADMINISTRATOR_KEY = "BES.ServerAdministrator";
    public static final String DEFAULT_SUPPORT_EMAIL_ADDRESS   = "support@opendap.org";

    public static final String REQUEST_ID      = "reqID";


    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;

    public static final String OLFS_LOG_CONTEXT = "olfsLog";
    public static final String ERRORS_CONTEXT  = "errors";
    public static final String XML_ERRORS      = "xml";

    public static final String XMLBASE_CONTEXT = "xml:base";

    public static final String STORE_RESULT_CONTEXT  = "store_result";
    public static final String UID_CONTEXT  = "uid";


    public static final String XDAP_ACCEPT_CONTEXT = "xdap_accept";
    public static final String DEFAULT_XDAP_ACCEPT = "3.2";

    public static final String EXPLICIT_CONTAINERS_CONTEXT = "dap_explicit_containers";

    // Added for cloudydap experiment
    public static final String CLOUDY_DAP_CONTEXT = "cloudydap";

    public static final String MAX_RESPONSE_SIZE_CONTEXT = "max_response_size";
    public static final String MAX_VARIABLE_SIZE_CONTEXT = "max_variable_size";
    public static final String CF_HISTORY_ENTRY_CONTEXT = "cf_history_entry";
    public static final String HISTORY_JSON_ENTRY_CONTEXT = "history_json_entry";

    // public static final String EDL_AUTH_TOKEN_CONTEXT = "edl_auth_token";


    /**
     * This specifies that the default BES "space" name is "catalog".
     * In more common parlance it's "the catalog called catalog" the utilizes
     * the BES.Catalog.catalog.RootDirectory filesystem as the catalog.
     */
    public static final String DEFAULT_BES_CATALOG_NAME = "catalog";
    public static final String DEFAULT_BES_CATALOG_TYPE_MATCH_KEY = "BES.Catalog." + DEFAULT_BES_CATALOG_NAME + ".TypeMatch";


    /**
     * This specifes the sdeafult BES "container" name. While this name could
     * pretty much be "foo" or some nonsensical string for the sake of BES
     * command readability a value should be chosen that relates to the BES "space"
     * name that is being used.
     */
    public static final String DEFAULT_BES_CONTAINER = DEFAULT_BES_CATALOG_NAME + "Container";


    public static final String MATCH_LAST_DOT_SUFFIX_REGEX_STRING = "\\.(?=[^.]*$).*$" ;



    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

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

    public AdminInfo getAdminInfo(String path)
            throws BadConfigurationException, JDOMException, IOException, PPTException, BESError {
        BES bes = getBES(path);
        return bes.getAdministratorInfo();
    }


    /**
     * Returns the support email held by the BES associated with the path.
     * @param path
     * @return
     */
    public String getSupportEmail(String path) throws BadConfigurationException {
        BES bes = getBES(path);
        return bes.getSupportEmail();
    }



    /**
     * Writes an OPeNDAP DDX for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xmlBase The request URL.
     * @param os                   The Stream to which to write the response.
     * @throws opendap.bes.BadConfigurationException .
     * @throws opendap.bes.BESError              .
     * @throws java.io.IOException               .
     * @throws opendap.ppt.PPTException              .
     */
    public void writeDDX(User user,
                         String dataSource,
                                String constraintExpression,
                                String xmlBase,
                                OutputStream os,
                         TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDDXRequest(user, dataSource, constraintExpression, xmlBase),
                os, tc);
    }

    /**
     * Writes an OPeNDAP DAP4 DMR for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param qp The DAP4 query string parameters associated with the request.
     * @param xmlBase The request URL.
     * @param os                   The Stream to which to write the response.
     * @throws opendap.bes.BadConfigurationException .
     * @throws opendap.bes.BESError              .
     * @throws java.io.IOException               .
     * @throws opendap.ppt.PPTException              .
     */
    public void writeDMR(User user,
                        String dataSource,
                        QueryParameters qp,
                        String xmlBase,
                        OutputStream os,
                         TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDMRRequest(user, dataSource,qp,xmlBase),
                os, tc);
    }

    /**
     * Writes an OPeNDAP DAP4 Data response for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param qp  A DAP4 QueryParameters object generated by processing the request
     * @param xmlBase The request URL.
     * @param os                   The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws java.io.IOException               .
     * @throws opendap.ppt.PPTException              .
     */
    public void writeDap4Data(User user,
                              String dataSource,
                                 QueryParameters qp,
                                 String xmlBase,
                                 String contentID,
                                 String mimeBoundary,
                                 OutputStream os,
                              TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
            dataSource,
            getDap4DataRequest(
                    user,
                    dataSource,
                    qp,
                    xmlBase,
                    contentID,
                    mimeBoundary),
                os,
                tc);
    }


    public void getDDXDocument(User user,
                               String dataSource,
                                  String constraintExpression,
                                  String xmlBase,
                                  Document response)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException, BESError {


        ByteArrayOutputStream ddxString = new ByteArrayOutputStream();
        ByteArrayOutputStreamTransmitCoordinator baostc = new ByteArrayOutputStreamTransmitCoordinator(ddxString);

        writeDDX(user, dataSource,constraintExpression,xmlBase,ddxString,baostc);


        SAXBuilder sb = new SAXBuilder();



        Document ddx = sb.build(new ByteArrayInputStream(ddxString.toByteArray()));

        response.detachRootElement();

        response.setRootElement(ddx.detachRootElement());




    }

    /**
     *
     * @param dataSource  The requested DataSource
     * @param qp  The DAP4 query parameters
     * @param xmlBase The request URL.
     * @param response
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws IOException
     * @throws JDOMException
     * @throws BESError
     */
    public void getDMRDocument(User user, String dataSource,
                                          QueryParameters qp,
                                          String xmlBase,
                                          Document response)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException, BESError {


        ByteArrayOutputStream ddxString = new ByteArrayOutputStream();
        ByteArrayOutputStreamTransmitCoordinator baostc = new ByteArrayOutputStreamTransmitCoordinator(ddxString);

        writeDMR(user, dataSource,qp,xmlBase,ddxString, baostc);


        SAXBuilder sb = new SAXBuilder();

        Document ddx = sb.build(new ByteArrayInputStream(ddxString.toByteArray()));

        response.detachRootElement();

        response.setRootElement(ddx.detachRootElement());




    }

    /**
     * Writes an OPeNDAP DDS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request.
     * reponse.
     * @param os                   The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDDS(User user,
            String dataSource,
            String constraintExpression,
            OutputStream os,
            TransmitCoordinator tc)
            throws
            BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        besTransaction(
                dataSource,
                getDDSRequest(user, dataSource, constraintExpression),
                os, tc);
    }



    /**
     * Writes the source data (it is often a file, thus the method name) to
     * the passed stream.
     *
     * @param dataSource     The requested DataSource
     * @param os             The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeFile(User user,
                          String dataSource,
                             OutputStream os,
                          TransmitCoordinator tc)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        besTransaction(
                dataSource,
                getStreamRequest(user,dataSource),
                os,
                tc);
    }



    /**
     * Writes an OPeNDAP DAS for the dataSource to the passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDAS(User user,
                         String dataSource,
                            String constraintExpression,
                            OutputStream os,
                         TransmitCoordinator tc)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        besTransaction(
                dataSource,
                getDASRequest(user,dataSource,constraintExpression),
                os,
                tc);
    }




    /**
     * Writes an OPeNDAP DAP2 data response for the dataSource to the
     * passed stream.
     *
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2Data(User user,
                              String dataSource,
                                     String constraintExpression,
                                     String async,
                                     String storeResult,
                                     OutputStream os,
                              TransmitCoordinator tc)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {


        besTransaction(
                dataSource,
                getDap2RequestDocumentAsync(user, DAP2_DATA, dataSource, constraintExpression, async, storeResult,  null, null, null, XML_ERRORS),
                os,
                tc);
    }

    /**
     * Writes the NetCDF-3 file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param cf_history_entry The entry to add to the CF "history" attribute in the resulting NetCDF file..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsNetcdf3(User user,
                                       String dataSource,
                                       String constraintExpression,
                                       String cf_history_entry,
                                       String history_json_entry,
                                       OutputStream os,
                                       TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap2DataAsNetcdf3Request(user, dataSource, constraintExpression, cf_history_entry, history_json_entry),
                os,
                tc);
    }


    /**
     * Writes the NetCDF-3 file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsNetcdf3(User user,
                                       String dataSource,
                                       QueryParameters qp,
                                       String cf_history_entry,
                                       String history_json_entry,
                                       OutputStream os,
                                       TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap4DataAsNetcdf3Request(user, dataSource, qp, cf_history_entry, history_json_entry),
                os,
                tc);
    }


    /**
     * Writes the NetCDF-4 file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsNetcdf4(User user,
                                       String dataSource,
                                       String constraintExpression,
                                       String cf_history_entry,
                                       String history_json_entry,
                                       OutputStream os,
                                       TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap2DataAsNetcdf4Request(user, dataSource, constraintExpression, cf_history_entry, history_json_entry),
                os,
                tc);
    }

    /**
     * Writes the NetCDF-4 file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters associated wih the request.
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsNetcdf4(User user,
                                       String dataSource,
                                       QueryParameters qp,
                                       String cf_history_entry,
                                       String history_json_entry,
                                       OutputStream os,
                                       TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap4DataAsNetcdf4Request(user, dataSource, qp, cf_history_entry, history_json_entry),
                os,
                tc);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param xmlBase The request URL.
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsXml(User user,
                                   String dataSource,
                                      String constraintExpression,
                                      String xmlBase,
                                      OutputStream os,
                                   TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getXmlDataRequest(user,dataSource,constraintExpression,xmlBase),
                os,
                tc);
    }



    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsGmlJpeg2000(User user,
                                           String dataSource,
                                              String constraintExpression,
                                              OutputStream os,
                                           TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap2DataAsGmlJpeg2000Request(user, dataSource, constraintExpression),
                os,
                tc);
    }

    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsGmlJpeg2000(User user,
                                           String dataSource,
                                           QueryParameters qp,
                                           OutputStream os,
                                           TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap4DataAsGmlJpeg2000Request(user, dataSource, qp),
                os,
                tc);
    }



    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsJson(User user, String dataSource,
                                    QueryParameters qp,
                                    OutputStream os,
                                    TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap4DataAsJsonRequest(user, dataSource, qp),
                os,
                tc);
    }

    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsCovJson(User user,
                                       String dataSource,
                                        QueryParameters qp,
                                        OutputStream os,
                                       TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap4DataAsCovJsonRequest(user, dataSource, qp),
                os,
                tc);
    }


    /**
     * Writes the combined site map for the server to the passed stream. This means that one BES from each
     * BesGroup (assumed to be identical to other BesGroup members) is queried for its site map and all of
     * resulting BESGroup site maps are combined into a single server site map. woot.
     *
     *
     * @param sitePrefix The requested DataSource
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeCombinedSiteMapResponse(String sitePrefix,
                                             OutputStream os,
                                             TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        Iterator<BesGroup> bgIt = BESManager.getBesGroups();
        while(bgIt.hasNext()){
            BesGroup bg = bgIt.next();
            String besPrefix = bg.getGroupPrefix();
            String siteMapPrefix = PathBuilder.pathConcat(sitePrefix,besPrefix);

            besTransaction(
                    besPrefix,
                    getSiteMapRequestDocument(siteMapPrefix),
                    os,
                    tc);
        }
    }



    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param dataSource The requested DataSource
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writePathInfoResponse(String dataSource,
                                         OutputStream os,
                                      TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
            dataSource,
            getShowPathInfoRequestDocument(dataSource),
            os, tc);
    }

    public void getPathInfoDocument(String dataSource, Document response)
            throws PPTException,
            BadConfigurationException,
            IOException,
            JDOMException, BESError {

        ByteArrayOutputStream pathInfoDocString = new ByteArrayOutputStream();
        ByteArrayOutputStreamTransmitCoordinator baostc = new ByteArrayOutputStreamTransmitCoordinator(pathInfoDocString);
        writePathInfoResponse(dataSource,pathInfoDocString, baostc);

        SAXBuilder sb = new SAXBuilder();
        Document pathInfoResponseDoc = sb.build(new ByteArrayInputStream(pathInfoDocString.toByteArray()));
        response.detachRootElement();
        response.setRootElement(pathInfoResponseDoc.detachRootElement());
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void  writeDap4DataAsGeoTiff(User user,
                                        String dataSource,
                                       QueryParameters qp,
                                       OutputStream os,
                                        TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap4DataAsGeoTiffRequest(user, dataSource, qp),
                os,
                tc);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsIjsn(User user,
                                    String dataSource,
                                       QueryParameters qp,
                                       OutputStream os,
                                    TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
            dataSource,
            getDap4IjsnDataRequest(user, dataSource, qp),
            os, tc);
    }


    /**
     * Writes the DAP4 XML data response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query string parameters
     * @param xmlBase The request URL.
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsXml(User user,
                                   String dataSource,
                                       QueryParameters qp,
                                       String xmlBase,
                                       OutputStream os,
                                   TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {


         besTransaction(
                dataSource,
                getDap4RequestDocument(user,XML_DATA, dataSource, qp, xmlBase, null, null, XML_ERRORS),
                os, tc);
    }


    /**
     * Writes the DAP4 json metadata response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query parameters submitted with the request.
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4MetadataAsJson(User user,
                                        String dataSource,
                                            QueryParameters qp,
                                            OutputStream os,
                                        TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
            dataSource,
            getDap4JsonMetadataRequest(user, dataSource, qp),
            os, tc);
    }


    /**
     * Writes the DAP4 IJson metadata response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param qp The DAP4 query parameters submitted with the request.
     * @param os  The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void  writeDap4MetadataAsIjsn(User user,
                                         String dataSource,
                                            QueryParameters qp,
                                            OutputStream os,
                                         TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

         besTransaction(
                dataSource,
                getDap4IjsnMetadataRequest(user, dataSource, qp),
                os, tc);
    }



    /**
     * Writes the DAP2 IJson data response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsIjsn(User user,
                                    String dataSource,
                                            String constraintExpression,
                                            OutputStream os,
                                    TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap2IjsnDataRequest(user, dataSource, constraintExpression),
            os, tc);
    }


    /**
     * Writes the DAP2 IJson metadata response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2MetadataAsIjsn(User user,
                                        String dataSource,
                                           String constraintExpression,
                                           OutputStream os,
                                        TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

         besTransaction(
                dataSource,
                getDap2IjsnMetadataRequest(user, dataSource, constraintExpression),
                os, tc);
    }

    /**
     * Writes the DAP2 JSON data response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsJson(User user,
                                    String dataSource,
                                       String constraintExpression,
                                       OutputStream os,
                                    TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap2DataAsJsonRequest(user, dataSource, constraintExpression),
                os,
                tc);
    }


    /**
     * Writes the DAP2 CovJSON data response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsCovJson(User user,
                                       String dataSource,
                                    String constraintExpression,
                                    OutputStream os,
                                       TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
                dataSource,
                getDap2DataAsCovJsonRequest(
                        user,
                        dataSource,
                        constraintExpression),
                        os, tc);
    }


    /**
     * Write DAP2 metadata as JSON for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2MetadataAsJson(User user,
                                        String dataSource,
                                           String constraintExpression,
                                           OutputStream os,
                                        TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

         besTransaction(
                dataSource,
                getDap2MetadataAsJsonRequest(user, dataSource, constraintExpression),
                os, tc);
    }


    /**
     * Writes the w10n Json response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsW10nJson(User user,
                                        String dataSource,
                                       String constraintExpression,
                                       String w10nMeta,
                                       String w10nCallback,
                                       boolean w10nFlatten,
                                       OutputStream os,
                                        TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

         besTransaction(
                dataSource,
                getDap2DataAsW10nJsonRequest(user, dataSource, constraintExpression, w10nMeta, w10nCallback, w10nFlatten),
                os, tc);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2MetadataAsW10nJson(User user,
                                            String dataSource,
                                           String constraintExpression,
                                           String w10nMeta,
                                           String w10nCallback,
                                           boolean w10nFlatten,
                                           boolean w10nTraverse,
                                           OutputStream os,
                                            TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

         besTransaction(
                dataSource,
                getDap2MetadataAsW10nJsonRequest(user, dataSource, constraintExpression, w10nMeta, w10nCallback, w10nFlatten, w10nTraverse),
                os, tc);
    }


    /**
     * Writes the NetCDF file out response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os         The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsGeoTiff(User user,
                                       String dataSource,
                                          String constraintExpression,
                                          OutputStream os,
                                       TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

        besTransaction(
            dataSource,
            getDap2DataAsGeoTiffRequest(user, dataSource, constraintExpression),
            os, tc);
    }


    /**
     * Writes the ASCII representation _rawOS the  OPeNDAP data response for the
     * dataSource to the passed stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource           The requested DataSource
     * @param constraintExpression The constraintElement expression to be applied to
     *                             the request..
     * @param os                   The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2DataAsAscii(User user,
                                     String dataSource,
                                        String constraintExpression,
                                        OutputStream os,
                                     TransmitCoordinator tc)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

         besTransaction(
                dataSource,
                getDap2DataAsAsciiRequest(user, dataSource, constraintExpression),
                os, tc);
    }


    /**
     * Writes the ASCII representation _rawOS the  OPeNDAP data response for the
     * dataSource to the passed stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource           The requested DataSource
     * @param qp The DAP4 query string parameters associated with the request.
     * @param os                   The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws BESError              .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap4DataAsCsv(User user,
                                   String dataSource,
                                      QueryParameters qp,
                                      OutputStream os,
                                   TransmitCoordinator tc)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

        besTransaction(
                dataSource,
                getDap4DataAsCsvRequest(user, dataSource, qp),
                os, tc);
    }


    /**
     * Writes the HTML data request form (aka the I.F.H.) for the OPeNDAP the
     * dataSource to the passed stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param url The URL to refernence in the HTML form.
     * @param os  The Stream to which to write the response.
     * @throws BadConfigurationException .
     * @throws PPTException              .
     * @throws IOException              .
     * @throws BESError              .
     */
    public void writeDap2DataRequestForm(User user,
                                         String dataSource,
                                            String url,
                                            OutputStream os,
                                         TransmitCoordinator tc)
            throws BadConfigurationException,
            BESError,
            IOException,
            PPTException {

         besTransaction(
                 dataSource,
                 getHtmlFormRequest(user, dataSource,url),
                 os, tc);
    }


    /**
     * Writes the OPeNDAP INFO response for the dataSource to the passed
     * stream.
     *
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The requested DataSource
     * @param os         The Stream to which to write the response.
     * @return False if the BES returns an error, true otherwise.
     * @throws BadConfigurationException .
     * @throws BESError                  .
     * @throws IOException               .
     * @throws PPTException              .
     */
    public void writeDap2HtmlInfoPage(User user,
                                      String dataSource,
                                         OutputStream os,
                                      TransmitCoordinator tc)
            throws BadConfigurationException, BESError, IOException, PPTException {

         besTransaction(
                dataSource,
                getHtmlInfoPageRequest(user,dataSource),
                os, tc);
    }






    /**
     * Returns the BES version document for the BES serving the passed
     * dataSource.
     *
     * @param dataSource The data source whose information is to be retrieved
     * @param response The document where the response (be it a catalog
     * document or an error) will be placed.
     * while servicing the request.
     * @throws PPTException              .
     * @throws BadConfigurationException .
     * @throws IOException               .
     * @throws JDOMException             .
     * @throws BESError             .
     */
    public void getBesVersion(String dataSource, Document response) throws
            PPTException,
            BadConfigurationException,
            IOException,
            JDOMException,
            BESError {

        besTransaction(dataSource, getShowVersionRequestDocument(),response);

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
    public void getBesNode(String dataSource, Document response)
            throws BadConfigurationException, PPTException, JDOMException, IOException, BESError {

        if(!dataSource.startsWith("/"))
            dataSource = "/" + dataSource;

        Procedure timedProc = Timer.start();
        try {

            if (BesNodeCache.isInitialized()) {
                log.info("Using BesNodeCache to acquire showNode response for dataSource \"{}\"", dataSource);
                BesNodeCache.getNode(this, dataSource, response);
            }
            else {
                log.info("BesNodeCache DISABLED. Acquiring BES showNode response for dataSource \"{}\"",dataSource);
                getBesNodeNoCache(dataSource, response);
            }
        }
        finally {
            Timer.stop(timedProc);

        }

    }

    public void getBesNodeNoCache(String dataSource, Document response)
            throws JDOMException, BadConfigurationException, PPTException, BESError, IOException {

        Document showNodeRequestDoc = getShowNodeRequestDocument(dataSource);

        besTransaction(dataSource, showNodeRequestDoc, response);
        // Get the root element.
        Element root = response.getRootElement();
        if(root==null)
            throw new IOException("BES showNode response for "+dataSource+" was empty! No root element");

        Element showNode  = root.getChild("showNode", BES_NS);
        if(showNode==null)
            throw new IOException("BES showNode response for "+dataSource+" was malformed! No showNode element");

        showNode.setAttribute("prefix", getBESprefix(dataSource));
    }





    /**
     * Executes a command/response transaction with the BES
     *
     * @param dataSource  The BES datasource that is going to be acccessed. This is used to determine which BES should
     * be used to fulfill the request (In the event that Hyrax is configured to use multiple BESs this string will
     * be used to locate the appropriate BES).
     * @param request   The BES request document.
     * @param response  The document into which the BES response will be placed. If the passed Document object contains
     *                   content, then the content will be discarded.
     * @return true if the request is successful, false if there is a problem fulfilling the request.
     * @throws IOException
     * @throws PPTException
     * @throws BadConfigurationException
     * @throws JDOMException
     */
    public void besTransaction( String dataSource,
                                           Document request,
                                           Document response
                                            )
            throws IOException, PPTException, BadConfigurationException, JDOMException, BESError {

        log.debug("besTransaction started.");
        log.debug("besTransaction() request document: \n-----------\n"+ getDocumentAsString(request)+"-----------\n");

        BES bes = BESManager.getBES(dataSource);
        int bes_timeout_seconds = bes.getTimeout()/1000;
        request.getRootElement().addContent(0,setContextElement("bes_timeout",Integer.toString(bes_timeout_seconds)));
        bes.besTransaction(request,response);


    }


    /**
     * Executes a command/response transaction with the BES
     *
     * @param dataSource  The BES datasource that is going to be acccessed. This is used to determine which BES should
     * be used to fulfill the request (In the event that Hyrax is configured to use multiple BESs this string will
     * be used to locate the appropriate BES).
     * @param request   The BES request document.
     * @param os   The OutputStream to write the BES response to.
     * any error information will be written to the OutputStream err.
     * @throws BadConfigurationException
     * @throws IOException
     * @throws PPTException
     */
    public void besTransaction(String dataSource, Document request, OutputStream os, TransmitCoordinator tc)
            throws BadConfigurationException, IOException, PPTException, BESError {

        log.debug("besTransaction() started.");
        log.debug("besTransaction() request document: \n-----------\n"+ getDocumentAsString(request)+"-----------\n");

        BES bes = BESManager.getBES(dataSource);
        int bes_timeout_seconds = bes.getTimeout()/1000;
        request.getRootElement().addContent(0,setContextElement("bes_timeout",Integer.toString(bes_timeout_seconds)));
        bes.besTransaction(request, os, tc);
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




    public Element dap2ConstraintElement(String ce) {
        Element e = new Element("constraint",BES_NS);
        // We replace the space characters in the CE with %20
        // so the libdap ce parsers don't blow a gasket.
        String encoded_ce = ce.replaceAll(" ","%20");
        e.setText(encoded_ce);
        return e;
    }

    public Element dap4ConstraintElement(String ce) {
        Element e = new Element("dap4constraint",BES_NS);
        // We replace the space characters in the CE with %20
        // so the libdap ce parsers don't blow a gasket.
        String encoded_ce = ce.replaceAll(" ","%20");
        e.setText(encoded_ce);
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


    public static Element setContextElement(String name, String value) {
        Element e = new Element("setContext",BES_NS);
        e.setAttribute("name",name);
        e.setText(value);
        return e;
    }



/*##########################################################################*/


    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDX is being requested
     * @param ce The constraint expression to apply.
     * @param xmlBase The request URL.
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDDXRequest(User user,
                                  String dataSource,
                                  String ce,
                                  String xmlBase)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, DDX, dataSource, ce, xmlBase, null, null, XML_ERRORS);

    }

    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDX is being requested
     * @param qp The DAP4 query string parameters associated wih the request..
     * response.
     * @param xmlBase The request URL.
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDMRRequest(User user,
                                  String dataSource,
                                         QueryParameters qp,
                                         String xmlBase)
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DMR, dataSource, qp, xmlBase, null, null, XML_ERRORS);

    }

    /**
     *  Returns the DDX request document for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDX is being requested
     * @param qp The DAP4 QueryParameters ingested from the client request.
     * response.
     * @param xmlBase The request URL.
     * @return The DDX request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataRequest(User user,
                                       String dataSource,
                                       QueryParameters qp,
                                       String xmlBase,
                                       String contentID,
                                       String mimeBoundary)
            throws BadConfigurationException {

        Document reqDoc =
                getDap4RequestDocument(
                        user,
                        DAP4_DATA,
                        dataSource,
                        qp,
                        xmlBase,
                        null,
                        null,
                        XML_ERRORS);

        Element req = reqDoc.getRootElement();
        if(req==null)
            throw new BadConfigurationException("Request document is corrupt! No root element!");

        Element getReq = req.getChild("get",BES_NS);
        if(getReq==null)
            throw new BadConfigurationException("Request document is corrupt! No 'get' element!");

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
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDDSRequest(User user, String dataSource, String ce)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, DDS, dataSource, ce, null, null, null, XML_ERRORS);

    }


    public Document getDASRequest(User user, String dataSource, String ce) throws BadConfigurationException {

        return getDap2RequestDocument(user, DAS, dataSource, ce, null, null, null, XML_ERRORS);

    }


    public Document getDap2DataAsAsciiRequest(User user,
                                              String dataSource,
                                              String ce)
            throws BadConfigurationException {

        return getDap2RequestDocument(user,DAP2_DATA, dataSource, ce, null, null, ASCII, XML_ERRORS);

    }

    public Document getDap4DataAsCsvRequest(User user,
                                            String dataSource,
                                            QueryParameters qp)
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DATA, dataSource, qp, null, null, CSV, XML_ERRORS);

    }


    public Document getHtmlFormRequest(User user, String dataSource, String URL) throws BadConfigurationException {

        return getDap2RequestDocument(user, HTML_FORM, dataSource, null, null, URL, null, XML_ERRORS);

    }

    public Document getStreamRequest(User user, String dataSource) throws BadConfigurationException {

        return getDap2RequestDocument(user,STREAM, dataSource, null, null, null, null, XML_ERRORS);

    }


    public Document getHtmlInfoPageRequest(User user, String dataSource)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, INFO_PAGE, dataSource, null, null, null, null, XML_ERRORS);

    }

    public Document getDap2DataAsNetcdf3Request(User user, String dataSource, String ce, String cf_history_entry, String history_json_entry)
            throws BadConfigurationException {

        Document besRequest = getDap2RequestDocument(user,DAP2_DATA, dataSource, ce, null, null, NETCDF_3, XML_ERRORS);

        if(cf_history_entry!=null) {
            Element root = besRequest.getRootElement();
            root.addContent(0, setContextElement(CF_HISTORY_ENTRY_CONTEXT, cf_history_entry));
        }

        if(history_json_entry!=null) {
            Element root = besRequest.getRootElement();
            root.addContent(0, setContextElement(HISTORY_JSON_ENTRY_CONTEXT, history_json_entry));
        }

        return besRequest;

    }


    public Document getDap4DataAsNetcdf3Request(User user, String dataSource, QueryParameters qp, String cf_history_entry, String history_json_entry)
            throws BadConfigurationException {

        Document besRequest = getDap4RequestDocument(user, DAP4_DATA, dataSource, qp, null, null, NETCDF_3, XML_ERRORS);

        if(cf_history_entry!=null) {
            Element root = besRequest.getRootElement();
            root.addContent(0, setContextElement(CF_HISTORY_ENTRY_CONTEXT, cf_history_entry));
        }

        if(history_json_entry!=null) {
            Element root = besRequest.getRootElement();
            root.addContent(0, setContextElement(HISTORY_JSON_ENTRY_CONTEXT, history_json_entry));
        }

        return besRequest;



    }


    public Document getDap2DataAsNetcdf4Request(User user, String dataSource, String ce, String cf_history_entry, String history_json_entry)
            throws BadConfigurationException {

        Document besRequest = getDap2RequestDocument(user, DAP2_DATA, dataSource, ce, null, null, NETCDF_4, XML_ERRORS);

        Element root = besRequest.getRootElement();
        if(cf_history_entry!=null) {
            root.addContent(0, setContextElement(CF_HISTORY_ENTRY_CONTEXT, cf_history_entry));
        }

        if(history_json_entry!=null) {
            root.addContent(0, setContextElement(HISTORY_JSON_ENTRY_CONTEXT, history_json_entry));
        }

        return besRequest;

    }

    public Document getDap4DataAsNetcdf4Request(User user, String dataSource, QueryParameters qp, String cf_history_entry, String history_json_entry)
            throws BadConfigurationException {

        Document besRequest = getDap4RequestDocument(user, DAP4_DATA, dataSource, qp, null, null, NETCDF_4, XML_ERRORS);
        Element root = besRequest.getRootElement();

        if(cf_history_entry!=null) {
            root.addContent(0, setContextElement(CF_HISTORY_ENTRY_CONTEXT, cf_history_entry));
        }

        if(history_json_entry!=null) {
            root.addContent(0, setContextElement(HISTORY_JSON_ENTRY_CONTEXT, history_json_entry));
        }

        return besRequest;

    }




    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getXmlDataRequest(User user,
                                      String dataSource,
                                         String ce,
                                         String xmlBase)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, XML_DATA, dataSource, ce, xmlBase, null, null, XML_ERRORS);

    }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsGmlJpeg2000Request(User user,
                                                    String dataSource,
                                                    String ce)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, DAP2_DATA, dataSource, ce, null, null, GMLJP2, XML_ERRORS);

    }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsGeoTiffRequest(User user,
                                                String dataSource,
                                                String ce)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, DAP2_DATA, dataSource, ce, null, null, GEOTIFF, XML_ERRORS);

    }

    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataAsGeoTiffRequest(User user,
                                                String dataSource,
                                           QueryParameters qp)
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DATA, dataSource, qp, null, null, GEOTIFF, XML_ERRORS);


    }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataAsGmlJpeg2000Request(User user,
                                                    String dataSource,
                                             QueryParameters qp)
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DATA, dataSource, qp, null, null, GMLJP2, XML_ERRORS);
    }




    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsJsonRequest(User user,
                                             String dataSource,
                                             String ce)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, DAP2_DATA, dataSource, ce, null, null, JSON, XML_ERRORS);

    }
    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsCovJsonRequest(User user,
                                                String dataSource,
                                             String ce)
            throws BadConfigurationException {

        return getDap2RequestDocument(user, DAP2_DATA, dataSource, ce, null, null, COVJSON, XML_ERRORS);

    }
    /**
     *  Returns the JSON encoded DAP2 Metadata response (DDX) for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The DAP2 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2MetadataAsJsonRequest(User user,
                                                 String dataSource,
                                                 String ce
    )
            throws BadConfigurationException {

        return getDap2RequestDocument(user,DDX, dataSource, ce, null, null, JSON, XML_ERRORS);


    }



    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2DataAsW10nJsonRequest(User user,
                                                 String dataSource,
                                           String ce,
                                           String w10nMeta,
                                           String w10nCallback,
                                           boolean w10nFlatten)
            throws BadConfigurationException {

        Document requestDoc =  getDap2RequestDocument(user, DAP2_DATA, dataSource, ce, null, null, W10N, XML_ERRORS);


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
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The DAP2 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2MetadataAsW10nJsonRequest(User user,
                                                     String dataSource,
                                                 String ce,
                                                 String w10nMeta,
                                                 String w10nCallback,
                                                 boolean w10nFlatten,
                                                 boolean w10nTraverse
    )
            throws BadConfigurationException {

        Document requestDoc = getDap2RequestDocument(user, DDX, dataSource, ce, null, null, W10N, XML_ERRORS);


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
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataAsJsonRequest(User user,
                                             String dataSource,
                                             QueryParameters qp
    )
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DATA, dataSource, qp,  null, null, JSON, XML_ERRORS);


    }

    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4DataAsCovJsonRequest(User user,
                                                String dataSource,
                                             QueryParameters qp
    )
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DATA, dataSource, qp,null, null, COVJSON, XML_ERRORS);


    }



    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4IjsnDataRequest(User user,
                                           String dataSource,
                                           QueryParameters qp)
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DATA, dataSource, qp, null, null, IJSON, XML_ERRORS);


    }




    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4JsonMetadataRequest(User user,
                                               String dataSource,
                                               QueryParameters qp
                                               )
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DMR, dataSource, qp, null, null, JSON, XML_ERRORS);


    }

    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param qp The DAP4 query string parameters associated wih the request.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap4IjsnMetadataRequest(User user,
                                               String dataSource,
                                               QueryParameters qp)
            throws BadConfigurationException {

        return getDap4RequestDocument(user, DAP4_DMR, dataSource, qp, null, null, IJSON, XML_ERRORS);


    }


    /**
      *  Returns the XML data response for the passed dataSource
      *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
      * @param ce The constraint expression to apply.
      * @return The DDS request document.
      * @throws BadConfigurationException When no BES can be found to
      * service the request.
      */
     public Document getDap2IjsnDataRequest(User user,
                                            String dataSource,
                                            String ce)
             throws BadConfigurationException {

         return getDap2RequestDocument(user, DAP2_DATA, dataSource, ce, null, null, IJSON, XML_ERRORS);

     }


    /**
     *  Returns the XML data response for the passed dataSource
     *  using the passed constraint expression.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @return The DDS request document.
     * @throws BadConfigurationException When no BES can be found to
     * service the request.
     */
    public Document getDap2IjsnMetadataRequest(User user,
                                               String dataSource,
                                               String ce)
            throws BadConfigurationException {

        return getDap2RequestDocument(user,  DDX, dataSource, ce, null, null, IJSON, XML_ERRORS);

    }



    /**
     * Returns a BES Request document.
     * @param user The User profile and tokens that may be required to complete
     *            downstream transactions.
     * @param type
     * @param dataSource The data set whose DDS is being requested
     * @param ce The constraint expression to apply.
     * @param xmlBase
     * @param formURL
     * @param returnAs
     * @param errorContext
     * @return
     * @throws BadConfigurationException
     */
    public  Document getDap2RequestDocument(User user,
                                            String type,
                                            String dataSource,
                                            String ce,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
                throws BadConfigurationException {



        return getDap2RequestDocumentAsync(
                user,
                type,
                dataSource,
                ce,
                null,
                null,
                xmlBase,
                formURL,
                returnAs,
                errorContext);

    }


    public  Document getDap2RequestDocumentAsync(User user,
                                                 String type,
                                            String dataSource,
                                            String ce,
                                            String async,
                                            String storeResult,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
                throws BadConfigurationException {


        String besDataSource = getBES(dataSource).trimPrefix(dataSource);


        Element e, request = new Element("request", BES_NS);
        request.setAttribute(REQUEST_ID,getRequestIdBase());


        request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        String logEntryForBes = ServletLogUtil.getLogEntryForBesLog();
        if(!logEntryForBes.isEmpty())
            request.addContent(setContextElement(OLFS_LOG_CONTEXT,logEntryForBes));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(user.getMaxResponseSize()>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,user.getMaxResponseSize()+""));

        if(user.getMaxVariableSize()>=0)
            request.addContent(setContextElement(MAX_VARIABLE_SIZE_CONTEXT,user.getMaxVariableSize()+""));

        request.addContent(setContextElement(UID_CONTEXT,user.getUID()==null?"not_logged_in":user.getUID()));

        UserProfile profile = user.profile();
        if (profile != null) {
            EarthDataLoginAccessToken oat = profile.getEDLAccessToken();
            if (oat != null) {
                opendap.ngap.NgapBesApi.addEdlAuthToken(request,user);
            }
        }


        request.addContent(setContainerElement(getBesContainerName(),getBesSpaceName(),besDataSource,type));

        Element def = defineElement("d1","default");
        e = (containerElement(getBesContainerName()));

        if(ce!=null && !ce.equals(""))
            e.addContent(dap2ConstraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs,async,storeResult);

        request.addContent(e);

        return new Document(request);



    }


    /**
     * This method defines which "space" (aka catalog) the BES will use to service a request.
     * THis method in order to simplify the implementations of the the BesAPI (child classes thereof)
     * that need only  modify the catalog name to achieve their goals.
     *
     * @return The name os the BES "space" (aka catalog) which will be used to service the request.
     */
    protected String getBesSpaceName(){ return DEFAULT_BES_CATALOG_NAME; }

    /**
     * This defines the name of the container built by the BES. It's name matters not, it's really an ID, but to keep
     * the BES commands readable and consistent we typically associate it with the "space" name.
     * @return The name of the BES "container" which will be built into teh request document.
     */
    protected String getBesContainerName(){
        return DEFAULT_BES_CONTAINER;
    }




    public  Document getDap4RequestDocument(User user,
                                            String type,
                                            String dataSource,
                                            QueryParameters qp,
                                            String xmlBase,
                                            String formURL,
                                            String returnAs,
                                            String errorContext)
                throws BadConfigurationException {


        String besDataSource = getBES(dataSource).trimPrefix(dataSource);


        Element e, request = new Element("request", BES_NS);

        request.setAttribute(REQUEST_ID,getRequestIdBase());

        //----------------------------------------------------------------------
        // Added this bit for the cloudy dap experiment - ndp 1/19/17
        //
        String cloudyDap = qp.getCloudyDap();
        if(cloudyDap!=null){
            request.addContent(setContextElement(CLOUDY_DAP_CONTEXT,cloudyDap));
        }
        //----------------------------------------------------------------------

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        String logEntryForBes = ServletLogUtil.getLogEntryForBesLog();
        if(!logEntryForBes.isEmpty())
            request.addContent(setContextElement(OLFS_LOG_CONTEXT,logEntryForBes));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        if(user.getMaxResponseSize()>=0)
            request.addContent(setContextElement(MAX_RESPONSE_SIZE_CONTEXT,user.getMaxResponseSize()+""));

        if(user.getMaxVariableSize()>=0)
            request.addContent(setContextElement(MAX_VARIABLE_SIZE_CONTEXT,user.getMaxVariableSize()+""));

        request.addContent(setContextElement(UID_CONTEXT,user.getUID()==null?"not_logged_in":user.getUID()));

        UserProfile profile = user.profile();
        if (profile != null) {
            EarthDataLoginAccessToken oat = profile.getEDLAccessToken();
            if (oat != null) {
                opendap.ngap.NgapBesApi.addEdlAuthToken(request,user);
            }
        }



        request.addContent(setContainerElement(getBesContainerName(),getBesSpaceName(),besDataSource,type));

        Element def = defineElement("d1","default");
        e = (containerElement(getBesContainerName()));

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


    public  Document getSiteMapRequestDocument(String sitePrefix) {

        Element request = new Element("request", BES_NS);
        request.setAttribute(REQUEST_ID,getRequestIdBase());

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));
        request.addContent(setContextElement(ERRORS_CONTEXT,XML_ERRORS));

        request.addContent(getSiteMapRequestElement(sitePrefix,"contents.html", ".html"));

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        log.debug("getSiteMapRequestDocument() - Document\n {}",xmlo.outputString(request));

        return new Document(request);

    }


    /**
     *     <buildSiteMap prefix="http://machine/opendap"
     *     nodeSuffix="contents.html" leafSuffix=""
     *     filename="node_site_map.txt"/>
     *
     * @param prefix
     * @return
     */

    public Element getSiteMapRequestElement(String prefix, String nodeSuffix, String leafSuffix ) {
        Element spi = new Element("buildSiteMap",BES_NS);

        if(prefix!=null)
            spi.setAttribute("prefix", prefix);

        if(nodeSuffix!=null)
            spi.setAttribute("nodeSuffix", nodeSuffix);

        if(leafSuffix!=null)
            spi.setAttribute("leafSuffix", leafSuffix);

        return spi;
    }



    public  Document getShowPathInfoRequestDocument(String dataSource)
            throws BadConfigurationException {


        String besDataSource = getBES(dataSource).trimPrefix(dataSource);


        Element request = new Element("request", BES_NS);
        request.setAttribute(REQUEST_ID,getRequestIdBase());

        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));
        request.addContent(setContextElement(ERRORS_CONTEXT,XML_ERRORS));

        request.addContent(showPathInfoRequestElement(besDataSource));

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        log.debug("getShowPathInfoRequestDocument() - Document\n {}",xmlo.outputString(request));

        return new Document(request);

    }


    public Element showPathInfoRequestElement(String resource) {
        Element e;
        Element spi = new Element("showW10nPathInfo",BES_NS);

        spi.setAttribute("node", resource);

        return spi;
    }




    public Document getShowVersionRequestDocument()
        throws BadConfigurationException {
        return getShowRequestDocument("showVersion", null);

    }



    public Document getShowNodeRequestDocument(String dataSource)
            throws BadConfigurationException {

        return getShowRequestDocument("showNode", dataSource);
    }


    public Document getShowInfoRequestDocument(String dataSource)
            throws BadConfigurationException {
        return getShowRequestDocument("showInfo", dataSource);
    }



    public Document getShowRequestDocument(String type, String dataSource)
            throws BadConfigurationException {


        Element e, request = new Element("request", BES_NS);
        request.setAttribute(REQUEST_ID,getRequestIdBase());
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
        return BESManager.getBES(dataSource);
    }

    public String getBESprefix(String dataSource) throws BadConfigurationException {
        BES bes = BESManager.getBES(dataSource);
        return bes.getPrefix();
    }



    private  String getDocumentAsString(Document request) throws IOException{
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

        Pattern lastDotSuffixPattern= Pattern.compile(MATCH_LAST_DOT_SUFFIX_REGEX_STRING);

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
                    // Q: Why this test and not dsi.sourceExists()??
                    // A: Because this check is only for things the BES views as data. Regular (non data)
                    //    files are handled by the "FileDispatchHandler"
                    if (!dsi.isDataset()) {
                        log.debug("getBesDataSourceID() The thing that was requested is not a Dataset.");
                        besDataSourceId = null;
                    }
                } catch (Exception e) {
                    log.debug("getBesDataSourceID() failed with an Exception. Msg: '{}'", e.getMessage());
                }

            }
        }

        log.debug("getBesDataSourceID() - besDataSourceId: " + besDataSourceId);

        return besDataSourceId;

    }

    private static String getRequestIdBase(){
        return "[thread:"+Thread.currentThread().getName()+"-"+ Thread.currentThread().getId()+"]";
    }



    public String getBesCombinedTypeMatch()
            throws JDOMException, BadConfigurationException, PPTException, IOException, BESError {
        return getDefaultBesCombinedTypeMatchPattern("/");
    }


    /**
     * Retrives a BES Key that holds a Map stored in the values of the key and formatted as key:value
     * @param besPath
     * @param mapName
     * @return
     * @throws BadConfigurationException
     * @throws JDOMException
     * @throws IOException
     * @throws PPTException
     * @throws BESError
     */
    public Map<String,String> getBESConfigParameterMap(String besPath, String mapName)
            throws BadConfigurationException, JDOMException, IOException, PPTException, BESError {

        BES bes = getBES(besPath);
        Element besParamMap = showBesKey(bes.getPrefix(), mapName);
        if(log.isInfoEnabled()) {
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            log.info("BES map {}:\n{}",mapName,xmlo.outputString(besParamMap));
        }
        return processBesParameterMap(besParamMap);
    }

    /**
     * Retrieves a BES Key that holds a Map stored in the values of the key and formatted as key:value
     * @param map
     * @return
     */
    public static Map<String,String> processBesParameterMap(Element map) {

        HashMap<String,String> pmap = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Element> values = map.getChildren("value", BES_NS);
        for(Element v: values){
            String s = v.getTextTrim();
            staticLog.debug("getBESConfigParameterMap() - Processing map string: {}",s);
            int markIndex = s.indexOf(":");
            if(markIndex < 0){
                staticLog.error("getBESConfigParameterMap() The BES returned an incorrectly formatted value for the key. raw value: '{}' SKIPPING",v);
            }
            else {
                String key = s.substring(0,markIndex ).toLowerCase();
                String value = s.substring(markIndex + 1);
                pmap.put(key, value);
            }
        }
        return pmap;
    }





    public String getDefaultBesCombinedTypeMatchPattern(String besPrefix) throws JDOMException, BadConfigurationException, PPTException, BESError, IOException {

        StringBuilder combinedTypeMatch = new StringBuilder();
        Element typeMatchKey = showBesKey(besPrefix, DEFAULT_BES_CATALOG_TYPE_MATCH_KEY);
        if(typeMatchKey == null){
            throw new BadConfigurationException("Failed to get BES Key '"+ DEFAULT_BES_CATALOG_TYPE_MATCH_KEY +"'");
        }
        @SuppressWarnings("unchecked")
        List<Element> values = (List<Element>)typeMatchKey.getChildren("value",BES_NS);

        for(Element value: values){

            String s = value.getTextTrim();
            log.debug("getBesCombinedTypeMatch() - Processing TypeMatch String: {}",s);

            String regex = s.substring(s.indexOf(":")+1);
            while(regex.length()>0 && regex.endsWith(";"))
                regex = regex.substring(0,regex.length()-1);

           // regex =  regex.replaceAll("\\/","\\\\/");

            log.debug("getBesCombinedTypeMatch() - regex: {}",regex);

            if(combinedTypeMatch.length()>0)
                combinedTypeMatch.append("|");
            combinedTypeMatch.append(regex);

        }
        log.debug("getBesCombinedTypeMatch() -  Combined TypeMatch Regex String: {}",combinedTypeMatch.toString());
        return combinedTypeMatch.toString();
    }


    public Element showBesKey(String besPrefix, String besKey) throws JDOMException, BadConfigurationException, PPTException, BESError, IOException {
        Document showBesKeyCmd = getShowBesKeyRequestDocument(besKey);
        Document response = new Document();
        besTransaction(besPrefix,showBesKeyCmd,response);
        Element showBesKey = response.getRootElement().getChild("showBesKey",BES_NS);
        showBesKey.detach();
        return showBesKey;
    }


    public static Document getShowBesKeyRequestDocument(String besKey) {

        Element request = new Element("request", BES_NS);
        request.setAttribute(REQUEST_ID,getRequestIdBase());
        request.addContent(setContextElement(EXPLICIT_CONTAINERS_CONTEXT,"no"));
        request.addContent(setContextElement(ERRORS_CONTEXT,XML_ERRORS));
        request.addContent(showBesKeyRequestElement(besKey));

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        staticLog.debug("Document\n {}",xmlo.outputString(request));

        return new Document(request);

    }
    public static Element showBesKeyRequestElement(String besKey) {
        Element spi = new Element(SHOW_BES_KEY,BES_NS);
        spi.setAttribute("key", besKey);
        return spi;
    }



}
