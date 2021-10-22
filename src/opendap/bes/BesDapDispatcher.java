/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) $year OPeNDAP, Inc.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.bes;

import opendap.bes.dap2Responders.*;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.DataResponse.NormativeDR;
import opendap.bes.dap4Responders.DatasetMetadata.NormativeDMR;
import opendap.bes.dap4Responders.DatasetServices.NormativeDSR;
import opendap.bes.dap4Responders.FileAccess;
import opendap.bes.dap4Responders.Iso19115.IsoDMR;
import opendap.bes.dap4Responders.Iso19115.IsoRubricDMR;
import opendap.bes.dap4Responders.Version;
import opendap.coreServlet.DispatchHandler;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ServletUtil;
import opendap.dap.Dap2Service;
import opendap.dap4.Dap4Service;
import opendap.services.ServicesRegistry;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Vector;

import static opendap.bes.DatasetUrlResponseAction.*;
import static opendap.bes.DataRequestFormType.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/11/11
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class BesDapDispatcher implements DispatchHandler {

    private Logger _log;
    private boolean _initialized;
    private HttpServlet _dispatchServlet;

    private String _systemPath;
    private Element _config;
    private Vector<Dap4Responder> _responders;
    private static boolean _allowDirectDataSourceAccess = false;
    private static boolean _useDAP2ResourceUrlResponse = false;
    private static boolean _addFileoutTypeSuffixToDownloadFilename = false;
    private static boolean _enforceRequiredUserSelection = false;

    private static DataRequestFormType d_dataRequestFormType = dap4;
    private static DatasetUrlResponseAction d_datasetUrlResponse = requestForm;

    private static boolean d_forceDataRequestFormLinkToHttps = false;

    private BesApi _besApi;

    public BesDapDispatcher() {
        _log = LoggerFactory.getLogger(getClass());
        _responders = new Vector<>();
        d_dataRequestFormType = dap4;
        d_datasetUrlResponse = requestForm;
    }


    public String getSystemPath(){ return _systemPath; }

    public static boolean allowDirectDataSourceAccess() {
        return _allowDirectDataSourceAccess;
    }
    public static boolean useDAP2ResourceUrlResponse() { return _useDAP2ResourceUrlResponse; }

    public static DataRequestFormType dataRequestFormType() { return d_dataRequestFormType; }

    public static DatasetUrlResponseAction datasetUrlResponseAction() { return d_datasetUrlResponse; }
    public static String datasetUrlResponseActionStr() { return d_datasetUrlResponse.toString(); }

    public static boolean forceDataRequestFormLinkToHttps() { return d_forceDataRequestFormLinkToHttps; }

    protected Vector<Dap4Responder> getResponders() {
        return _responders;
    }

    protected void addResponder(Dap4Responder r) {
        _responders.add(r);
    }


    public BesApi getBesApi(){
        return _besApi;
    }

    public void setBesApi(BesApi besApi){
        _besApi = besApi;
    }


    private void ingestConfig(Element config) {

        if(config!=null){
            _config = config;

            _log.info("ingestConfig() - Using BES API implementation: "+getBesApi().getClass().getName());

            _allowDirectDataSourceAccess = false;
            Element dv = _config.getChild("AllowDirectDataSourceAccess");
            if (dv != null) {
                _allowDirectDataSourceAccess = true;
            }
            _log.info("ingestConfig() - AllowDirectDataSourceAccess: {}",_allowDirectDataSourceAccess);

            _enforceRequiredUserSelection = false;
            dv = _config.getChild("RequireUserSelection");
            if (dv != null) {
                _enforceRequiredUserSelection = true;
            }
            _log.info("ingestConfig() - RequireUserSelection: {}",_enforceRequiredUserSelection);

            _useDAP2ResourceUrlResponse = false;
            dv = _config.getChild("UseDAP2ResourceUrlResponse");
            if (dv != null) {
                _useDAP2ResourceUrlResponse = true;
            }
            _log.info("ingestConfig() - UseDAP2ResourceUrlResponse: {}",_useDAP2ResourceUrlResponse);

            d_dataRequestFormType = dap4;
            dv = _config.getChild("DataRequestForm");
            if (dv != null) {
                String drfTypeStr = dv.getAttributeValue("type");
                if(drfTypeStr!=null && drfTypeStr.equalsIgnoreCase("dap2")) {
                    d_dataRequestFormType = dap2;
                }
            }
            _log.info("ingestConfig() - DataRequestForm: {}",d_dataRequestFormType.toString());

            d_forceDataRequestFormLinkToHttps = false;
            dv = _config.getChild("ForceDataRequestFormLinkToHttps");
            d_forceDataRequestFormLinkToHttps = dv != null;
            _log.info("ingestConfig() - ForceDataRequestFormLinkToHttps: {}",(d_forceDataRequestFormLinkToHttps?"true":"false"));

            d_datasetUrlResponse = requestForm;
            dv = _config.getChild("DatasetUrlResponse");
            if (dv != null) {
                String drfTypeStr = dv.getAttributeValue("type");
                if(drfTypeStr!=null) {
                    if (drfTypeStr.equalsIgnoreCase("dsr")) {
                        d_datasetUrlResponse = DatasetUrlResponseAction.dsr;
                    } else if (drfTypeStr.equalsIgnoreCase("download")) {
                        d_datasetUrlResponse = download;
                    } else if (drfTypeStr.equalsIgnoreCase("requestForm")) {
                        d_datasetUrlResponse = requestForm;
                    }
                }
            }
            _log.info("ingestConfig() - DatasetUrlResponse: {}",d_datasetUrlResponse.toString());

            _addFileoutTypeSuffixToDownloadFilename = false;
            dv = _config.getChild("AddFileoutTypeSuffixToDownloadFilename");
            if (dv != null) {
                _addFileoutTypeSuffixToDownloadFilename = true;
            }
            _log.info("ingestConfig() - AddFileoutTypeSuffixToDownloadFilename: {}",_addFileoutTypeSuffixToDownloadFilename);

            dv = _config.getChild("HttpPost");
            if (dv != null) {

                String max = dv.getAttributeValue("max");
                if(max!=null) {
                    try {
                        int maxLength = Integer.parseInt(max);
                        ReqInfo.setMaxPostBodyLength(maxLength);
                    } catch (NumberFormatException e) {
                        _log.warn("HttpPost - Unable to parse the value of max! Value: {} ", max);

                    }
                }
            }
            _log.info("ingestConfig() - HTTP POST max body length is set to: {}", ReqInfo.getPostBodyMaxLength());
        }
    }


    /**
     *  This method is where the behavior of the BesDapDispatcher is defined. In here the various Responder classes
     *  are instantiated and loaded in to an ordered list. The types of the responders and their order defines the
     *  behaviour of the DAP dispatch activity.
     * @param servlet    The Servlet instance that this dispatcher is running in.
     * @param config  The configuration element loaded from the olfs.xml file for this dispatcher
     * @throws Exception  When the bad things happen.
     */
    public void init(HttpServlet servlet, Element config) throws Exception {
        BesApi besApi = new BesApi();
        init(servlet, config, besApi);
    }


    /**
     *  This method is where the behavior of the BesDapDispatcher is defined. In here the various Responder classes
     *  are instantiated and loaded in to an ordered list. The types of the responders and their order defines the
     *  behaviour of the DAP dispatch activity.
     * @param servlet    The Servlet instance that this dispatcher is running in.
     * @param config  The configuration element loaded from the olfs.xml file for this dispatcher
     * @param besApi    The BesApi instance to use when servicing requests.
     * @throws Exception  When the bad things happen.
     */
     public void init(HttpServlet servlet, Element config, BesApi besApi) throws Exception {

        if (_initialized) return;

        setBesApi(besApi);

        ingestConfig(config);

        _log.debug("Using BesApi implementation: {}", getBesApi().getClass().getName());

        _dispatchServlet = servlet;

        _systemPath = ServletUtil.getSystemPath(_dispatchServlet, "");

        // DAP4 Responses

        NormativeDSR ndsr = new NormativeDSR(_systemPath, null, ".dsr", besApi,_responders);
        _responders.add(ndsr);

        _responders.add(new NormativeDR(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename));
        _responders.add(new NormativeDMR(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename, _enforceRequiredUserSelection));
        _responders.add(new IsoDMR(_systemPath, besApi));
        _responders.add(new Version(_systemPath, besApi));


        // DAP2 Data Responses
        _responders.add(new Dap2Data(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename));
        _responders.add(   new Ascii(_systemPath, besApi));
        //responders.add(new Ascii(systemPath, null, ".asc", besApi)); // We can uncomment this if we want to support both the dap2 ".ascii" suffix and ".asc"
        _responders.add( new CsvData(_systemPath, besApi));
        _responders.add( new Netcdf3(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename));
        _responders.add( new Netcdf4(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename));
        _responders.add( new XmlData(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename));

        // DAP2 GeoTIFF Response
        Dap4Responder geoTiff = new GeoTiff(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename);
        _responders.add(geoTiff);

        // DAP2 JPEG2000 Response
        Dap4Responder jp2 = new GmlJpeg2000(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename);
        _responders.add(jp2);

        // DAP2 w10n JSON Response
        Dap4Responder json = new Json(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename);
        _responders.add(json);

        // DAP2 Instance Object JSON Response
        Dap4Responder ijsn = new Ijson(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename);
        _responders.add(ijsn);

        // DAP2 Cov-JSON Response
        Dap4Responder covjson = new CovJson(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename);
        _responders.add(covjson);

        // DAP2 Metadata responses
        Dap4Responder d4r = new DDX(_systemPath, besApi);
        _responders.add(d4r);
        _responders.add(new DDS(_systemPath, besApi));
        _responders.add(new DAS(_systemPath, besApi));
        _responders.add(new RDF(_systemPath, besApi, _addFileoutTypeSuffixToDownloadFilename));
        _responders.add(new DatasetInfoHtmlPage(_systemPath, besApi));

        Dap4Responder iso = new Iso19115(_systemPath, besApi);
        _responders.add(iso);

        Dap4Responder rubric = new IsoRubricDMR(_systemPath, null, ".rubric", besApi);
        rubric.clearAltResponders();
        rubric.setCombinedRequestSuffixRegex(rubric.buildRequestMatchingRegex());
        _responders.add(rubric);

/*
    if (_useDAP2ResourceUrlResponse) {


        // Add the HTML form conditionally because the ".html" suffix is used
        // by the NormativeDSR's HTML representation. Since we aren't using the DSR response
        // We should make sure that the old HTML ".html" response is available.
        Dap4Responder ifh = new Dap2IFH(_systemPath, besApi, _enforceRequiredUserSelection);
        _responders.add(ifh);

        // Add the "old" Data Request Form - the one that is generated by the BES.
        Dap4Responder htmlForm = new DatasetHtmlForm(_systemPath, besApi);
        _responders.add(htmlForm);

        // If we are running a dap2 centric server then we need to install the
        // FileAccess so it responds to <dataset_url> alone.
        FileAccess d2fa = new FileAccess(_systemPath, null, "", besApi);
        d2fa.clearAltResponders();
        d2fa.setCombinedRequestSuffixRegex(d2fa.buildRequestMatchingRegex());
        d2fa.setAllowDirectDataSourceAccess(_allowDirectDataSourceAccess);
        _responders.add(d2fa);
    } else {

        // If we are running a dap4 centric server then we need to install the
        // FileAccess handler so the service responds to <dataset_url>.file to
        // retrieve the source data file.
        FileAccess d4fa = new FileAccess(_systemPath, besApi);
        d4fa.setAllowDirectDataSourceAccess(_allowDirectDataSourceAccess);

        _responders.add(d4fa);

        // This call maps the DSR response to the <dataset_url> ala DAP4
        // And also causes the URL <dataset_url>.html to be a specific
        // client request for the HTML encoded DSR.
        _responders.add(new NormativeDSR(_systemPath, besApi, _responders));

    }
*/


         if(d_datasetUrlResponse == download || d_datasetUrlResponse == requestForm){
             // Either the download or the requestForm option imply that there
             // can be no DSR response associated with the dataset URL
             // with these options we need a DAP2 Data Request Form response
             // (DSR response space collides with the DAP2 Data Request Form URL)
             Dap2IFH d2ifh = new Dap2IFH(_systemPath, besApi, _enforceRequiredUserSelection);
             _responders.add(d2ifh);
         }
         // We install the FileAccess handler so the service
         // responds to <dataset_url>.file to retrieve the source data file.
         // This is the default behavior of FileAccess(String, BesApi)
         FileAccess d4fa = new FileAccess(_systemPath, besApi);
         d4fa.setDatasetUrlResponseAction(d_datasetUrlResponse);
         d4fa.setDatasetRequestFormType(d_dataRequestFormType);
         _responders.add(d4fa);

         if(d_datasetUrlResponse == download) {
             // We install and configure the FileAccess handler so that the FileAccess
             // service will respond to the unmodified <dataset_url> to retrieve the
             // source data file.
             FileAccess d2fa = new FileAccess(_systemPath, null, "", besApi);
             d2fa.clearAltResponders();
             d2fa.setDatasetUrlResponseAction(d_datasetUrlResponse);
             d2fa.setDatasetRequestFormType(d_dataRequestFormType);
             d2fa.setCombinedRequestSuffixRegex(d2fa.buildRequestMatchingRegex());
             _responders.add(d2fa);
         }
         if(d_datasetUrlResponse == dsr){
             // This call maps the DSR response to the <dataset_url> ala DAP4
             // And also causes the URL <dataset_url>.html to be a specific
             // client request for the HTML encoded DSR.
             _responders.add(new NormativeDSR(_systemPath, besApi, _responders));
         }


         /*

         switch (d_datasetUrlResponse) {
        case dsr: {
            // We don't install a DAP2 Data Request Form response
            // because the DSR URL space collides with the DAP2 Data
            // Request Form URL.

            // We install the FileAccess handler so the service
            // responds to <dataset_url>.file to retrieve the source data file.
            // This is the default behavior of FileAccess(String, BesApi)
            FileAccess d4fa = new FileAccess(_systemPath, besApi);
            d4fa.setDatasetUrlResponseAction(d_datasetUrlResponse);
            d4fa.setDatasetRequestFormType(d_dataRequestFormType);
            _responders.add(d4fa);

            // This call maps the DSR response to the <dataset_url> ala DAP4
            // And also causes the URL <dataset_url>.html to be a specific
            // client request for the HTML encoded DSR.
            _responders.add(new NormativeDSR(_systemPath, besApi, _responders));
            break;
        }

        case download:
        {
            // Because there is no DSR response with this option we need a DAP2 Data
            // Request Form response (DSR response space collides with the DAP2 Data
            // Request Form URL)
            Dap2IFH d2ifh = new Dap2IFH(_systemPath, besApi, _enforceRequiredUserSelection);
            _responders.add(d2ifh);

            // We install the FileAccess handler so the service
            // responds to <dataset_url>.file to retrieve the source data file.
            // This is the default behavior of FileAccess(String, BesApi)
            FileAccess d4fa = new FileAccess(_systemPath, besApi);
            d4fa.setDatasetUrlResponseAction(d_datasetUrlResponse);
            d4fa.setDatasetRequestFormType(d_dataRequestFormType);
            _responders.add(d4fa);

            // We install and configure the FileAccess handler so that the FileAccess
            // service will respond to the unmodified <dataset_url> to retrieve the
            // source data file.
            FileAccess d2fa = new FileAccess(_systemPath, null, "", besApi);
            d2fa.clearAltResponders();
            d2fa.setDatasetUrlResponseAction(d_datasetUrlResponse);
            d2fa.setDatasetRequestFormType(d_dataRequestFormType);
            d2fa.setCombinedRequestSuffixRegex(d2fa.buildRequestMatchingRegex());
            _responders.add(d2fa);

            break;
        }
        case requestForm:
        default: {
            // Because there is no DSR response with this option we need a DAP2 Data
            // Request Form response (DSR response space collides with the DAP2 Data
            // Request Form URL)
            Dap2IFH d2ifh = new Dap2IFH(_systemPath, besApi, _enforceRequiredUserSelection);
            _responders.add(d2ifh);


            // We install the FileAccess handler so the service
            // responds to <dataset_url>.file to retrieve the source data file.
            // This is the default behavior of FileAccess(String, BesApi)
            FileAccess d4fa = new FileAccess(_systemPath, besApi);
            d4fa.setDatasetUrlResponseAction(d_datasetUrlResponse);
            d4fa.setDatasetRequestFormType(d_dataRequestFormType);
            _responders.add(d4fa);
            break;
        }
    }
*/


        _log.info("Initialized. " +
                "Direct Data Source Access: " + (_allowDirectDataSourceAccess ? "Enabled" : "Disabled") +
                " d_datasetUrlResponse: " + (d_datasetUrlResponse.toString()) +
                " d_dataRequestFormType: " + (d_dataRequestFormType.toString())
        );


        Dap2Service dap2Service = new Dap2Service();
        Dap4Service dap4Service = new Dap4Service();

        dap2Service.init(servlet,null);
        dap4Service.init(servlet,null);

        ServicesRegistry.addService(dap2Service);
        ServicesRegistry.addService(dap4Service);

         _initialized = true;
     }


    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {

        _log.debug("************************************************************");
        if (requestDispatch(request, null, false)) {
            _log.debug("Request can be handled.");
            return true;
        }
        _log.debug("Request can not be handled.");
        _log.debug("************************************************************");
        return false;
    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        if (!requestDispatch(request, response, true)) {
            _log.error("Unable to service request.");
        }


    }


    public boolean requestDispatch(HttpServletRequest request,
                                   HttpServletResponse response,
                                   boolean sendResponse)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        // String dataSource = getBesApi().getBesDataSourceID(relativeUrl, false);

        _log.debug("The client requested this resource: {}", relativeUrl);
        if(relativeUrl.endsWith("contents.html") ||
                relativeUrl.endsWith("catalog.html") ||
                relativeUrl.endsWith("catalog.xml") ||
                relativeUrl.endsWith("/"))
            return false;


        for (HttpResponder r : _responders) {
            _log.debug("Checking responder: " + r.getClass().getSimpleName() + " (pathPrefix: " + r.getPathPrefix() + ")");
            if (r.matches(relativeUrl)) {

                _log.info("The relative URL: " + relativeUrl + " matches " +
                        "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                if (sendResponse){

                    r.respondToHttpGetRequest(request, response);

                }

                return true;
            }
        }


        return false;

    }


    public long getLastModified(HttpServletRequest req) {

         String relativeUrl = ReqInfo.getLocalUrl(req);

        if(!_initialized)
            return new Date().getTime();

        for (HttpResponder r : _responders) {
            if (r.matches(relativeUrl)) {
                if(_log.isInfoEnabled()) {
                    String msg = "The relative URL: " + relativeUrl +
                            " matches the pattern: \"" +
                            r.getRequestMatchRegexString() +
                            "\" (responder: " + r.getClass().getName() +
                            ")";
                    _log.info(msg);
                }
                try {

                    long lmt =  r.getLastModified(req);
                    _log.debug("getLastModified(): Returning: {}", new Date(lmt));
                    return lmt;

                } catch (Exception e) {
                    _log.debug("getLastModified(): Returning: -1");
                    return new Date().getTime();
                }

            }

        }

        return new Date().getTime();


    }



    public void destroy() {
        _log.info("Destroy complete.");

    }


}
