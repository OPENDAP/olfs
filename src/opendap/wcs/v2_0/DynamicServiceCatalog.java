/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0;

import opendap.PathBuilder;
import opendap.bes.BESError;
import opendap.bes.BESManager;
import opendap.bes.BadConfigurationException;
import opendap.bes.dap2Responders.BesApi;
import opendap.dap4.QueryParameters;
import opendap.http.Util;
import opendap.ppt.PPTException;
import opendap.services.ServicesRegistry;
import opendap.viewers.WcsService;
import opendap.wcs.srs.SimpleSrs;
import org.apache.commons.codec.binary.Hex;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * This catalog doesn't maintain a catalog persay, but uses incoming requests to access remote services to build a
 * single coverage catalog for the requested
 */
public class DynamicServiceCatalog implements WcsCatalog{

    private Logger _log;
    private boolean _intialized;

    private String _cacheDir;
    private ReentrantReadWriteLock _cacheLock;

   // private ConcurrentHashMap<String,SimpleSrs> _defaultSRS;

    // private CredentialsProvider _credsProvider;

    private ConcurrentHashMap<String,DynamicService> _dynamicServices;



    public DynamicServiceCatalog(){
        _intialized = false;
        _log = LoggerFactory.getLogger(getClass());
        _cacheLock = new ReentrantReadWriteLock();
        _dynamicServices = new ConcurrentHashMap<>();
    }

    /**
     * <pre>
     *    <WcsCatalog className="opendap.wcs.v2_0.DynamicServiceCatalog" >
     *        <DynamicService name="lds" href="http://localhost:8080/opendap/" />
     *    </WcsCatalog>
     * </pre>
     *
     *
     * @param config A URL the when de-referenced will return a document that contains
     * a WcsCatalog configuration element as a child of the root element.
     * @param cacheDir The directory into which the catalog may choose to write persistent content,
     * intermediate files, etc.
     * @param serviceContextPath Service context.
     * @throws Exception
     */
    @Override
    public void init(Element config, String cacheDir, String serviceContextPath) throws Exception {
        if(_intialized)
            return;

        Element e1;
        String msg;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        ///////////////////////////////////////////////////////////////
        // Sort out access credentials for getting things from places
        // that require such...
        /*
        _credsProvider = null;
        e1 = config.getChild("Credentials");
        if(e1!=null){
            // There was a Credentials thing in the config, lets try it...
            String filename = e1.getTextTrim();
            try {
                _credsProvider = opendap.http.Util.getNetRCCredentialsProvider(filename, true);
            }
            catch (IOException ioe){
                _log.error("init() - The file '{}' cannot be processed as a .netrc file. " +
                        "msg: {}",filename,ioe.getMessage());
            }
        }
        if(_credsProvider==null){
            _log.warn("Looking in default location for .netrc");
            try {
                _credsProvider = opendap.http.Util.getNetRCCredentialsProvider();
            } catch (IOException e) {
                msg = "Unable to load authentication credentials from defult location. " +
                        "Try specifying the credentials location if credentials are required.";
                _log.warn(msg);
            }

        }
        */

        e1 = config.getChild("CacheDirectory");
        if(e1==null){

            String defaultCacheDir = cacheDir + this.getClass().getSimpleName();

            File defaultCatDir = new File(defaultCacheDir);

            if(!defaultCatDir.exists()){
                if(!defaultCatDir.mkdirs()){
                    msg = "Default Cache Directory ("+defaultCacheDir+")does not exist and cannot be " +
                            "created. Could not find CoveragesDirectory element in " +
                            "configuration element: "+ xmlo.outputString(config);
                    _log.error(msg);
                    throw new IOException(msg);
                }
            }
            _cacheDir = defaultCacheDir;
        }
        else {
            _cacheDir =  e1.getTextTrim();
        }
        _log.debug("WCS-2.0 Cache Directory: " + _cacheDir);


        List<Element> dynamicServices = config.getChildren("DynamicService");
        for(Element dsElement:dynamicServices) {
            DynamicService dynamicService = new DynamicService(dsElement);
            DynamicService previous = _dynamicServices.put(dynamicService.getPrefix(),dynamicService);
            if(previous!=null){
                //FIXME Do we care that something was in the way? I think so...
                _log.warn("The addition of the DynamicService: {} bumped this instance from the map:{}",
                        dynamicService.toString(),previous.toString());
            }
            if(dynamicService.getDapServiceUrlString().equalsIgnoreCase(Util.BES_PROTOCOL)){
                String serviceId = WcsService.ID+"-"+dynamicService.getPrefix();
                WcsService wcsService = new WcsService(
                        serviceId,
                        dynamicService.getLongName(),
                        serviceContextPath,
                        dynamicService.getPrefix(),
                        dynamicService.getPathMatchRegexString());

                ServicesRegistry.addService(wcsService);
            }

        }

        _intialized = true;
    }

    private String anyId2CacheId(String someId) throws WcsException {
        if(someId==null)
            return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(someId.getBytes(StandardCharsets.UTF_8));
            String cacheId =  Hex.encodeHexString( hash );
            return cacheId;
        } catch (NoSuchAlgorithmException e) {
            throw new WcsException("Oops! No SHA-256 hashing available. msg: "+ e.getMessage(),WcsException.NO_APPLICABLE_CODE, getClass().getClass().getCanonicalName()+".getCacheId()");

        }
    }


    /**
     * Thread safe DMR acquisition and caching.
     * @param coverageId  The cacheId (aka cache file name)
     * @return
     */
    private Element getCachedDMR(String coverageId) throws IOException, JDOMException, InterruptedException, WcsException {

        _log.debug("getCachedDMR() - BEGIN coverageId: {}",coverageId);

        String datasetUrl = getDapDatsetUrl(coverageId);
        _log.debug("getCachedDMR() - DAP Dataset URL: {}",datasetUrl);
        if(datasetUrl==null)
            return null;

        String datasetCacheId = anyId2CacheId(datasetUrl);

        _log.debug("getCachedDMR() - datasetCacheId: {}", datasetCacheId);
        String dmrCacheFileName = datasetCacheId + ".dmr.xml";
        File dmrCacheFile = new File(_cacheDir, dmrCacheFileName);

        // TODO Improve by adding a shared read lock. jhrg 9/18/17
        WriteLock writeLock = _cacheLock.writeLock();

        writeLock.lock();
        try {
            if(dmrCacheFile.exists()){
                _log.debug("getCachedDMR() - Reading cached DMR.");
                Element dmrElement = opendap.xml.Util.getDocumentRoot(dmrCacheFile);
                dmrElement.setAttribute("name",coverageId);
                return dmrElement;
            }
            else {
                _log.debug("getCachedDMR() - Retrieving DMR from DAP service");
                FileOutputStream fos = new FileOutputStream(dmrCacheFile);

                try {
                    if (datasetUrl.startsWith(Util.BES_PROTOCOL)) {
                        String besDatasource = datasetUrl.substring(Util.BES_PROTOCOL.length());
                        if(!BESManager.isInitialized())
                            throw new WcsException("The BESManager has not been configured. " +
                                    "Unable to access BES!",WcsException.NO_APPLICABLE_CODE);
                        BesApi besApi = new BesApi();
                        QueryParameters qp = new QueryParameters();
                        try {
                            besApi.writeDMR(besDatasource, qp, datasetUrl, fos);
                        } catch (BadConfigurationException | PPTException | BESError error) {
                            String msg = "Failed to get DMR from BES! Caught " + error.getClass().getName() +
                                    " Message: " + error.getMessage();
                            _log.error("getCachedDMR() - {}", msg);
                            throw new IOException(msg, error);
                        }

                    } else if (datasetUrl.startsWith(Util.HTTP_PROTOCOL) || datasetUrl.startsWith(Util.HTTPS_PROTOCOL)) {
                        String dmrUrl = datasetUrl + ".dmr.xml";
                        _log.debug("getCachedDMR() - DMR URL: {}",dmrUrl);

                        opendap.http.Util.writeRemoteContent(dmrUrl, WcsServiceManager.getCredentialsProvider(), fos);
                    } else {
                        String msg = "Unrecognized protocol: " + datasetUrl;
                        _log.error("getCachedDMR() - {}", msg);
                        throw new IOException(msg);
                    }
                }
                finally {
                    fos.close();
                }
                // FIXME Add something to limit cache size and to purge the stuff.
                Element dmrElement = opendap.xml.Util.getDocumentRoot(dmrCacheFile);
                // TODO QC the dmrElement to be sure it's not a DAP error object and then maybe uncache it if it's an error.
                dmrElement.setAttribute("name",coverageId);
                return dmrElement;
            }
        }
        finally {
            writeLock.unlock();
        }
    }


    @Override
    public boolean hasCoverage(String coverageId) throws InterruptedException {
        try {
            if(getCachedDMR(coverageId) != null)
                return true;

        } catch (IOException | JDOMException | WcsException e) {
            _log.debug("hasCoverage() - Unable to locate coverage! Caught a(n) "+
                    e.getClass().getName()+" msg: " + e.getMessage());
        }
        return false;
    }

    /**
     * TODO In this method should be set up to utilize a cached instance of the DynamicCoverageDescription object as the getDMR() method does for the DMR document.
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return
     * @throws InterruptedException
     * @throws WcsException
     */
    @Override
    public CoverageDescription getCoverageDescription(String coverageId) throws InterruptedException, WcsException {

        try {
            Element dmr = getCachedDMR(coverageId);

            if(dmr==null)
                return null;

            DynamicService dynamicService = getLongestMatchingDynamicService(coverageId);
            if(dynamicService==null)
                return null;

            DynamicCoverageDescription coverageDescription = new DynamicCoverageDescription(dmr,dynamicService);
            return coverageDescription;

        } catch (JDOMException | IOException e) {
            StringBuilder sb = new StringBuilder("getCoverageDescription() - ");
            sb.append("FAILED to get CoverageDescription for id: '").append(coverageId).append("' ");
            sb.append(" Caught ").append(e.getClass().getName()).append(" message: ").append(e.getMessage());
            _log.error(sb.toString());
        }

        return null;
    }

    @Override
    public Element getCoverageDescriptionElement(String coverageId) throws InterruptedException, WcsException {
        return getCoverageDescription(coverageId).getCoverageDescriptionElement();
    }


    @Override
    public Element getCoverageSummaryElement(String coverageId) throws InterruptedException, WcsException {
        CoverageDescription cDesc = getCoverageDescription(coverageId);
        if(cDesc!=null){

            return cDesc.getCoverageSummary();
            /*
            Element covSum = new Element("CoverageSummary",WCS.WCS_NS);
            Element coverageID = cDesc.getCoverageIdElement();
            covSum.addContent(coverageID);
            Element coverageSubtype = new Element("CoverageSubtype",WCS.WCS_NS);
            covSum.addContent(coverageSubtype);
            coverageSubtype.addContent(cDesc.getCoverageDescriptionElement());
            return covSum;
            */
        }
        return null;
    }

    @Override
    public Collection<Element> getCoverageSummaryElements() throws InterruptedException, WcsException {

        Vector<Element> results = new Vector<>();
        CoverageDescription cDesc = getCoverageDescription("foo");
        if (cDesc != null) {


        }
        return results;
    }

    @Override
    public Collection<Element> getDatasetSeriesSummaryElements() throws InterruptedException, WcsException {
        return new Vector<>();
    }



    /**
     * Compares the passed coverageID with the collection Dynamix services and returns the one,
     * if any, with the lingest matching name.
     *
     * @param coverageId
     * @return
     */
    public DynamicService getLongestMatchingDynamicService(String coverageId){
        String longestMatchingDynamicServiceName=null;
        DynamicService match = null;
        for(DynamicService dynamicService:_dynamicServices.values()){
            String dsName = dynamicService.getPrefix();
            _log.debug("Checking DynamicService '{}'",dsName);

            if(coverageId.startsWith(dsName)){
                _log.debug("CoverageId '{}' matchs DynamicService name '{}'",coverageId,dsName);
                if(longestMatchingDynamicServiceName==null){
                    longestMatchingDynamicServiceName=dsName;
                    match = dynamicService;
                }
                else if(longestMatchingDynamicServiceName.length() < dsName.length()) {
                        longestMatchingDynamicServiceName = dsName;
                        match = dynamicService;
                }
                _log.debug("DynamicService '{}' matched.",match.getPrefix());

            }
        }
        return match;
    }


    @Override
    public String getDapDatsetUrl(String coverageId) throws InterruptedException {
        _log.debug("getDapDatsetUrl() - BEGIN coverageId: {}",coverageId);
        DynamicService dynamicService = getLongestMatchingDynamicService(coverageId);
        _log.debug("getDapDatsetUrl() - DynamicService instance: {}",dynamicService);
        if(dynamicService==null)
            return null;
        String resourceId = coverageId.substring(dynamicService.getPrefix().length());
        PathBuilder pb = new PathBuilder(dynamicService.getDapServiceUrlString().toString());
        pb.pathAppend(resourceId);
        pb.append("");
        return pb.toString();
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void update() throws Exception {

    }

    @Override
    public EOCoverageDescription getEOCoverageDescription(String id) throws WcsException {
        return null;
    }

    @Override
    public EODatasetSeries getEODatasetSeries(String id) throws WcsException {
        return null;
    }

    @Override
    public boolean hasEoCoverage(String id) {
        return false;
    }


    public SimpleSrs getDefaultSrs(String coverageId){
        DynamicService dynamicService = getLongestMatchingDynamicService(coverageId);

        if(dynamicService==null)
            return null;

        return dynamicService.getSrs();
    }


    // public CredentialsProvider getCredentials(){
    //    return _credsProvider;
    //}

    public boolean matches(String coverageId){
        for(DynamicService dynamicService:_dynamicServices.values()){
            if(coverageId.startsWith(dynamicService.getPrefix()))
                return true;
        }
        return false;
    }


}
