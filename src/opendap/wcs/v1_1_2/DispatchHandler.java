/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2010 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.wcs.v1_1_2;

import net.sf.saxon.s9api.XdmNode;
import opendap.coreServlet.*;
import opendap.bes.Version;
import opendap.bes.BesXmlAPI;
import opendap.bes.BESError;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;
import javax.xml.transform.stream.StreamSource;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.io.*;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:01:59 PM
 */
public class DispatchHandler implements opendap.coreServlet.DispatchHandler {


    private final Logger log;
    private boolean _initialized;
    private HttpServlet dispatchServlet;
    private String _prefix;

    private Element _config;


    private static final String _coveragesTerminus = "/coverages";

    private String _form;
    private String _configurationForm;
    private String _testPath;
    private String _xmlEchoPath;
    private String _coveragesPath;
    private String _describeCoveragePath;
    private String _serviceContentPath;
    private String _resourcePath;
    private String _defaultWcsServiceConfigFilename = "wcs_service.xml";


    private static final int GET_CAPABILITIES   = 0;
    private static final int DESCRIBE_COVERAGE  = 1;
    private static final int GET_COVERAGE       = 2;


    public DispatchHandler() {

        super();

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;

    }



    public void init(HttpServlet servlet, Element config) throws Exception {
        if (_initialized) return;

        String msg;

        dispatchServlet = servlet;
        _config = config;

        ingestPrefix();

        _resourcePath = ServletUtil.getSystemPath(dispatchServlet, "/"+_prefix);
        if(!_resourcePath.endsWith("/"))
            _resourcePath += "/";

        log.debug("_resourcePath: "+_resourcePath);

        _serviceContentPath = ServletUtil.getContentPath(dispatchServlet);
        if(!_serviceContentPath.endsWith("/"))
            _serviceContentPath += "/";
        
        if(!_prefix.equals(""))
            _serviceContentPath += _prefix + "/";

        log.debug("_serviceContentPath: "+_serviceContentPath);



        File f = new File(_serviceContentPath);

        if(f.exists()){
            if(!f.isDirectory())
                throw new Exception("The service content path "+_serviceContentPath+
                        "exists, but it is not directory and cannot be used.");
            if(!f.canWrite())
                throw new Exception("The service content path "+_serviceContentPath+
                        "exists, but the directory is not writable.");
        }
        else {
            log.info("Creating WCS Service content directory: "+_serviceContentPath);
            f.mkdirs();
        }
        File semaphore = new File(_serviceContentPath+".INIT");
        if(!semaphore.exists()){
            String initialContentDir = _resourcePath + "initialContent/";
            log.info("Attempting to copy initial content for WCS from "+initialContentDir+" to "+_serviceContentPath);
            opendap.coreServlet.PersistentContentHandler.copyDirTree(initialContentDir,_serviceContentPath);
            semaphore.createNewFile();
            log.info("WCS Service default configuration and initial content installed.");
        }


        String serviceConfigFilename = _serviceContentPath + _defaultWcsServiceConfigFilename;

        Element serviceConfig = config.getChild("config");
        if(serviceConfig != null){
            serviceConfigFilename = serviceConfig.getTextTrim();
        }

        log.info("Using WCS Service configuration file: "+serviceConfigFilename);

        File configFile = new File(serviceConfigFilename);
        if(!configFile.exists()){
            msg = "Failed to located WCS Service Configuration File '"+serviceConfigFilename+"'";
            log.error(msg);
            throw new Exception(msg);
        }
        if(!configFile.canRead()){
            String userName = System.getProperty("user.name");
            msg = "The WCS Service Configuration File '"+serviceConfigFilename+"' exists but cannot be read." +
                    " Is there a file permission problem? Is the user '"+userName+"' allowed read access on that file?";
            log.error(msg);
            throw new Exception(msg);
        }



        URL serviceConfigUrl = new URL("file://" + serviceConfigFilename);
        SAXBuilder sb = new SAXBuilder();
        serviceConfig = sb.build(serviceConfigUrl).getRootElement();
        

        WcsCatalog catalog;


        Element catalogConfig = serviceConfig.getChild("WcsCatalog");
        if(catalogConfig==null){
            log.info("No WCS catalog implementation provided in the " +
                    "WCS Service configuration "+serviceConfigFilename);
            log.info("Defaulting to opendap.wcs.v1_1_2.LocalFileCatalog.");

            String coveragesDir = _serviceContentPath + _coveragesTerminus;
            String metadataDir  = _serviceContentPath;

            LocalFileCatalog lfc = new LocalFileCatalog();
	        lfc.init(metadataDir,coveragesDir);
            catalog = lfc;

            log.info("A wcs.v1_1_2.LocalFileCatalog has been built.");


        }
        else {
            String catalogClass = catalogConfig.getAttributeValue("className");
            if(catalogClass==null){
                throw new Exception("In the WCS Service configuration (" + serviceConfigFilename +
                        ") the WcsCatalog element is missing the " +
                        "required 'className' attribute which is used to " +
                        "specify the catalog implementation to be used.");
            }

            log.info("Using "+catalogClass+" WCS catalog implementation.");


            try {

                log.debug("Building WCS catalog class: " + catalogClass);
                Class classDefinition = Class.forName(catalogClass);
                catalog = (WcsCatalog) classDefinition.newInstance();


            } catch (ClassNotFoundException e) {
                msg = "Cannot find class: " + catalogClass;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (InstantiationException e) {
                msg = "Cannot instantiate class: " + catalogClass;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (IllegalAccessException e) {
                msg = "Cannot access class: " + catalogClass;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (ClassCastException e) {
                msg = "Cannot cast class: " + catalogClass + " to "+ WcsCatalog.class.getName();
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (Exception e) {
                msg = "Caught an " + e.getClass().getName() + " exception.  msg:" + e.getMessage();
                log.error(msg);
                throw new ServletException(msg, e);

            }

            log.debug("Initializing instance of a WcsCatalog interface implementation:" + catalogClass);


            


            String defautCatalogCacheDir = _serviceContentPath + catalog.getClass().getSimpleName()+"/";


            catalog.init(serviceConfigUrl, defautCatalogCacheDir, _resourcePath);
        }



        CatalogWrapper.init(_serviceContentPath, catalog);

        _initialized = true;
    }


    private void ingestPrefix() throws Exception{

        String msg;

        Element e = _config.getChild("prefix");
        if(e==null)
            throw new Exception("In the configuration file, the <Handler> Element that " +
                    "utilizes "+this.getClass().getName()+" must contain a <prefix> element.");


        _prefix = e.getTextTrim();

/*   
        if(_prefix.equals("/")){
            msg = "Bad Configuration. The <Handler> " +
                    "element that declares " + this.getClass().getName() +
                    " MUST provide 1 <prefix>  " +
                    "child element whose value may not be equal to \"/\"";
            log.error(msg);
            throw new Exception(msg);
        }

*/

        //if(!_prefix.startsWith("/"))
        //    _prefix = "/" + _prefix;

        if(_prefix.startsWith("/"))
            _prefix = _prefix.substring(1, _prefix.length());

        while(_prefix.endsWith("/")){
            _prefix = _prefix.substring(0,_prefix.length()-2);
        }

        /*

        _testPath    = _prefix + "/test";
        _form        = _prefix + "/form";
        _configurationForm   = _prefix + "/config";
        _xmlEchoPath = _prefix + "/echoXML";
        _coveragesPath = _prefix + _coveragesTerminus;

         */

        _testPath              = "test";
        _form                  = "form";
        _configurationForm     = "config";
        _xmlEchoPath           = "echoXML";
        _coveragesPath         = _coveragesTerminus;
        _describeCoveragePath  = "describeCoverage";

        if(!_prefix.equals("")){
            _testPath              = _prefix + "/"+ _testPath;
            _form                  = _prefix + "/"+ _form;
            _configurationForm     = _prefix + "/"+ _configurationForm;
            _xmlEchoPath           = _prefix + "/"+ _xmlEchoPath;
            _coveragesPath         = _prefix + _coveragesPath;
            _describeCoveragePath   = _prefix + "/"+ _describeCoveragePath;
        }


        log.info("Initialized. prefix="+ _prefix);

    }




    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return wcsRequestDispatch(request, null, false);
    }


    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        wcsRequestDispatch(request, response, true);
    }

    public long getLastModified(HttpServletRequest req) {
        return CatalogWrapper.getLastModified();
    }

    public void destroy() {
        CatalogWrapper.destroy();
        log.info("Destroy Complete");
    }


    private boolean wcsRequestDispatch(HttpServletRequest request,
                                       HttpServletResponse response,
                                       boolean sendResponse)
            throws Exception {

        String dataAccessBase = ReqInfo.getServiceUrl(request);
        String relativeURL = ReqInfo.getRelativeUrl(request);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());

        String serviceURL = getServiceUrlString(request,_prefix);
        String query      = request.getQueryString();

        boolean isWcsEndPoint = false;

        if (relativeURL != null) {

            if (relativeURL.startsWith(_prefix)) {

                isWcsEndPoint = true;

                if (sendResponse) {

                    String q = request.getQueryString();

                    if (relativeURL.equals(_prefix+"/")) {
                        response.sendRedirect(ServletUtil.getContextPath(dispatchServlet)+"/"+_prefix);
                        log.debug("Sent redirect from "+_prefix+"/ to "+_prefix);
                    }
                    else if(relativeURL.equals(_prefix) && q==null){
                        sendCapabilitesPresentationPage(request, response);
                        log.info("Sent WCS Capabilities Response Page.");
                    }
                    //else if(relativeURL.equals(_configurationForm) && q==null){
                        //sendConfigurationPage(request, response);
                        //log.info("Sent WCS Configuration Page.");
                    //}
                    else if(relativeURL.equals(_prefix)){
                        response.setContentType("text/xml");
                        ServletOutputStream os = response.getOutputStream();
                        KvpHandler.processKvpWcsRequest(serviceURL, dataAccessBase,query,os);
                        log.info("Sent WCS Response");
                    }
                    else if(relativeURL.startsWith(_testPath)){
                        testWcsRequest(request, response);
                        log.info("Sent WCS Test Page");
                    }
                    else if(relativeURL.startsWith(_xmlEchoPath)){
                        echoWcsRequest(request, response);
                        log.info("Returning KVP request as XML docuemtn.");
                    }
                    /*
                    else if(relativeURL.startsWith(_coveragesPath)){
                        sendNetcdfFileOut(request, response);
                        log.info("Returning NetcdfData.");
                    }
                    */
                    else if(relativeURL.startsWith(_describeCoveragePath)){
                        sendDescribeCoveragePage(request, response);
                        log.info("Returning WCS Describe Coverage Page.");
                    }
                    else if(relativeURL.equals("update()")){
                        log.info("Updating catalog.");
                        update();
                        log.info("Catalog update complete.");
                    }
                    else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }

                }
            }
        }

        return isWcsEndPoint;

    }


    public void update() throws Exception{
        CatalogWrapper.update();
    }




    public void sendWcsHomePage(HttpServletResponse response) throws IOException {

        String name = _serviceContentPath + "index.html";

        File f = new File(name);

        if (f.exists()) {
            log.debug("   Requested item exists.");
            String mType = MimeTypes.getMimeType("html");
            if (mType != null)
                response.setContentType(mType);

            ServletOutputStream sos = null;
            FileInputStream fis = new FileInputStream(f);

            try {


                sos = response.getOutputStream();

                String page = "";
                byte buff[] = new byte[8192];
                int rc;
                boolean doneReading = false;
                while (!doneReading) {
                    rc = fis.read(buff);
                    if (rc < 0) {
                        doneReading = true;
                    } else if (rc > 0) {
                        page += new String(buff);
                    }

                }

                page = page.replace("__PREFIX__",_prefix);
                sos.println(page);

            }
            finally {

                if(fis!=null)
                    fis.close();

                if(sos!=null)
                    sos.flush();
            }

        }

    }

    public void sendDescribeCoveragePage(HttpServletRequest request, HttpServletResponse response) throws Exception {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        String xsltDoc = ServletUtil.getSystemPath(dispatchServlet, _prefix + "/xsl/coverageDescription.xsl");
        log.debug("sendDescribeCoveragePage()  xsltDoc: "+xsltDoc);

        String serviceUrl = getServiceUrlString(request,_prefix);
        log.debug("sendDescribeCoveragePage()  serviceUrl: "+serviceUrl);

        String id = request.getQueryString();
        Element cde = CatalogWrapper.getCoverageDescriptionElement(id);
        if(cde==null)
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        Document  coverageDescription = new Document(cde);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "HTML wcs:DescribeCoverage");




        /*

        XSLTransformer transformer = new XSLTransformer(xsltDoc);

        Document coverageDescriptionPage = transformer.transform(coverageDescription);

        response.setStatus(HttpServletResponse.SC_OK);

        xmlo.output(coverageDescriptionPage, response.getWriter());

        */

        opendap.xml.Transformer t = new   opendap.xml.Transformer(xsltDoc);
        t.setParameter("ServicePrefix",serviceUrl);

        XdmNode descCover = t.build(new StreamSource(new ByteArrayInputStream(xmlo.outputString(coverageDescription).getBytes())));

        t.transform(descCover,response.getOutputStream());






    }

    public void sendCapabilitesPresentationPage(HttpServletRequest request, HttpServletResponse response) throws Exception {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        String xsltDoc = ServletUtil.getSystemPath(dispatchServlet, _prefix + "/xsl/capabilities.xsl");
        log.debug("sendCapabilitesPresentationPage()  xsltDoc: "+xsltDoc);

        String serviceUrl = getServiceUrlString(request,_prefix);
        log.debug("sendCapabilitesPresentationPage()  serviceUrl: "+serviceUrl);

        Document capabilitiesDoc = CapabilitiesRequestProcessor.getFullCapabilitiesDocument(serviceUrl);

        response.setContentType("text/html");
        response.setHeader("Content-Description", "HTML wcs:Capabilities");
        


/*

        XSLTransformer transformer = new XSLTransformer(xsltDoc);

        Document capabilitiesPage = transformer.transform(capabilitiesDoc);

        response.setStatus(HttpServletResponse.SC_OK);

        xmlo.output(capabilitiesPage, response.getWriter());
*/


        opendap.xml.Transformer t = new   opendap.xml.Transformer(xsltDoc);
        t.setParameter("ServicePrefix",serviceUrl);

        XdmNode capDoc = t.build(new StreamSource(new ByteArrayInputStream(xmlo.outputString(capabilitiesDoc).getBytes())));

        t.transform(capDoc,response.getOutputStream());



    }

    public void sendConfigurationPage(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String serviceUrl = getServiceUrlString(request,_prefix);
        String xsltDoc = ServletUtil.getSystemPath(dispatchServlet, _prefix + "/xsl/capabilitiesForm.xsl");

        XSLTransformer transformer = new XSLTransformer(xsltDoc);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());



        Document capabilitiesDoc = CapabilitiesRequestProcessor.getFullCapabilitiesDocument(serviceUrl);
        Document capabilitiesPage = transformer.transform(capabilitiesDoc);

        response.setContentType("text/html");
        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));
        response.setHeader("Content-Description", "Configuration Form");
        response.setStatus(HttpServletResponse.SC_OK);

        xmlo.output(capabilitiesPage, response.getWriter());

    }





    public void echoWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        HashMap<String,String> keyValuePairs = new HashMap<String,String>();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        Document reqDoc;
        String query = request.getQueryString();

        response.setContentType("text/xml");

        try {

            switch(KvpHandler.getRequestType(query,keyValuePairs)){

                case  GET_CAPABILITIES:
                    GetCapabilitiesRequest gcr = new GetCapabilitiesRequest(keyValuePairs);
                    reqDoc = new Document(gcr.getRequestElement());
                    break;

                case  DESCRIBE_COVERAGE:
                    DescribeCoverageRequest dcr = new DescribeCoverageRequest(keyValuePairs);
                    reqDoc = new Document(dcr.getRequestElement());
                    break;

                case GET_COVERAGE:
                    GetCoverageRequest gc = new GetCoverageRequest(keyValuePairs);
                    reqDoc = new Document(gc.getRequestElement());
                    break;

                default:
                    throw new WcsException("INTERNAL ERROR: getRequestType() returned an invalid value.",
                            WcsException.NO_APPLICABLE_CODE);
            }

            xmlo.output(reqDoc,response.getOutputStream());


        }
        catch(WcsException e){
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);
            response.getOutputStream().println(er.toString());
        }



    }







    public void testWcsRequest(HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {

        HashMap<String,String> keyValuePairs = new HashMap<String,String>();
        String serviceUrl = getServiceUrlString(request,_prefix);

        String url = Scrub.completeURL(request.getRequestURL().toString());
        String query = Scrub.completeURL(request.getQueryString());

        response.setContentType("text/html");

        String page = "<html>";
        page += "    <head>";
        page += "        <link rel='stylesheet' href='"+serviceUrl+"/docs/css/contents.css' type='text/css' >";
        page += "        <title>OPeNDAP Hyrax WCS Test</title>";
        page += "    </head>";
        page += "    <body>";
        page += "    <img alt=\"OPeNDAP Logo\" src='"+serviceUrl+"/docs/images/logo.gif'/>";
        page += "    <h2>OPeNDAP WCS Test Harness</h2>";
        page += "    How Nice! You sent a WCS request.";
        page += "    <h3>KVP request: </h3>";
        page += "    <pre>"+url+"?"+query+"</pre>";

        try {

            switch(KvpHandler.getRequestType(query,keyValuePairs)){

                case  GET_CAPABILITIES:
                    page += getCapabilitiesTestPage(serviceUrl, keyValuePairs);
                    break;

                case  DESCRIBE_COVERAGE:
                    page += describeCoverageTestPage(keyValuePairs);
                    break;

                case GET_COVERAGE:
                    page += getCoverageTestPage(request,keyValuePairs);
                    break;

                default:
                    throw new WcsException("INTERNAL ERROR: getRequestType() returned an invalid value.",
                            WcsException.NO_APPLICABLE_CODE);
            }


            page += "    </body>";
            page += "</html>";

            response.getOutputStream().println(page);

        }
        catch(WcsException e){
            log.error(e.getMessage());
            ExceptionReport er = new ExceptionReport(e);

            page += "<h1>ERROR</h1>";
            page += "    After some deliberation we have rejected your request.";
            page += "    <h3>Here's why: </h3>";
            page += "    <pre>";
            page += er.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
            page += "    </pre>";
            page += "    </body>";
            page += "</html>";

            response.getOutputStream().println(page);

        }



    }


    public String getCapabilitiesTestPage(String serviceUrl, HashMap<String,String> keyValuePairs) throws WcsException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        GetCapabilitiesRequest wcsRequest = new GetCapabilitiesRequest(keyValuePairs);
        String page="";

        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += wcsRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";

        Document wcsResponse = KvpHandler.getCapabilities(keyValuePairs,serviceUrl);

        page += xmlo.outputString(wcsResponse).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";


        return page;

    }


    public String describeCoverageTestPage(HashMap<String,String> keyValuePairs) throws WcsException {

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        DescribeCoverageRequest wcsRequest = new DescribeCoverageRequest(keyValuePairs);
        String page = "";

        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += wcsRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";

        Document wcsResponse = KvpHandler.describeCoverage(keyValuePairs);

        page += xmlo.outputString(wcsResponse).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";



        return page;

    }




    public String getCoverageTestPage(HttpServletRequest req, HashMap<String,String> keyValuePairs) throws WcsException {

        GetCoverageRequest getCoverageRequest = new GetCoverageRequest(keyValuePairs);
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        String page = "";


        page += "    <h3>XML request: </h3>";
        page += "    <pre>";
        page += getCoverageRequest.toString().replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";

        page += "    <h3>The WCS response: </h3>";
        page += "    <pre>";

        Document wcsResponse = KvpHandler.getCoverage(keyValuePairs,ReqInfo.getServiceUrl(req));

        page += xmlo.outputString(wcsResponse).replaceAll("<","&lt;").replaceAll(">","&gt;");
        page += "    </pre>";


        return page;
    }


    /**
     * ************************************************************************
     * Default handler for the client's DDS request. Requires the writeDDS() method
     * implemented by each server localization effort.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @throws IOException When the bad things be happening
     * @see ReqInfo
     */
    private void sendNetcdfFileOut(HttpServletRequest request,
                        HttpServletResponse response)
            throws  IOException {

        ServletOutputStream os = response.getOutputStream();
        boolean success;
        ByteArrayOutputStream erros;

        String relativeUrl = ReqInfo.getRelativeUrl(request);
        String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        log.debug("sendNetcdfFileOut() for dataset: " + dataSource + "?" +
                    constraintExpression);


        try {

            try {
                response.setContentType("application/octet-stream");
                response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
                response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
                response.setHeader("XDAP", Version.getXDAPVersion(request));

                response.setStatus(HttpServletResponse.SC_OK);

                String xdap_accept = request.getHeader("XDAP-Accept");


                erros = new ByteArrayOutputStream();

                success = BesXmlAPI.writeNetcdfFileOut( dataSource, constraintExpression, xdap_accept, os, erros);
            }
            catch(Exception e){
                throw new WcsException(e.getMessage(),
                        WcsException.NO_APPLICABLE_CODE);

            }

            if(!success){
                response.setHeader("Content-Description", "dods_error");
                BESError besError = new BESError(new ByteArrayInputStream(erros.toByteArray()));
                log.error(besError.getMessage());

                throw new WcsException(besError.getMessage(), WcsException.NO_APPLICABLE_CODE);

            }

            try {
                os.flush();
            } catch (IOException e) {
                throw new WcsException(e.getMessage(),
                        WcsException.NO_APPLICABLE_CODE);
            }
        }
        catch(WcsException wcse){
            ExceptionReport er = new ExceptionReport(wcse);
            os.println(er.toString());
            log.error(wcse.getMessage());

        }

        log.info("Sent DAP2 data as netCDF file.");


    }
    /***************************************************************************/




    public static String getServiceUrlString(HttpServletRequest request, String prefix){
        String serviceURL = ReqInfo.getServiceUrl(request);

        if (!prefix.equals("")) {
            if (!serviceURL.endsWith("/")) {
                if (prefix.startsWith("/"))
                    serviceURL += prefix;
                else
                    serviceURL += "/" + prefix;

            } else {
                if (prefix.startsWith("/"))
                    serviceURL += serviceURL.substring(0, serviceURL.length() - 1) + prefix;
                else
                    serviceURL += prefix;

            }
        }
        return serviceURL;

    }



}
