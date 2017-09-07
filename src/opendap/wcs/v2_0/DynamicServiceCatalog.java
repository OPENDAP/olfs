package opendap.wcs.v2_0;

import org.apache.http.HttpEntity;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import org.apache.commons.codec.binary.Hex;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class DynamicServiceCatalog implements WcsCatalog{
    private Logger _log;
    private boolean _intiialized;

    private String _cacheDir;
    private ReentrantReadWriteLock _cacheLock;
    private ConcurrentHashMap<String,String> _dynamicServicesMap;

    private CredentialsProvider _credsProvider;


    public DynamicServiceCatalog(){
        _intiialized = false;
        _log = LoggerFactory.getLogger(getClass());
        _dynamicServicesMap = new ConcurrentHashMap<>();
        _cacheLock = new ReentrantReadWriteLock();
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
     * @param resourcePath The path to the resource bundle delivered with the software.
     * @throws Exception
     */
    @Override
    public void init(Element config, String cacheDir, String resourcePath) throws Exception {
        if(_intiialized)
            return;

        Element e1;
        String msg;
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

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
        for(Element dservice:dynamicServices) {
            String name = dservice.getAttributeValue("name");
            String href = dservice.getAttributeValue("href");
            if (name != null && href != null) {
                _dynamicServicesMap.put(name, href);
                _log.info("WCS-2.0 DynamicService Loaded! name: {} href: {} ",name,href);
            }
        }

        _credsProvider = null;
        try {
            _credsProvider = opendap.http.Util.getNetRCCredentialsProvider();
        } catch (IOException e) {
            msg = "Unable to load authentication credentials from defult location. " +
                    "Try specifying the credentials location if credentials are required.";
            _log.warn(msg);
        }
        _intiialized = true;
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

    private void writeRemoteContent(String url, OutputStream os) throws IOException, JDOMException {

        _log.debug("writeRemoteContent() - URL: {}",url);

        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(_credsProvider)
                .build();

        HttpGet httpGet = new HttpGet(url);
        CloseableHttpResponse resp = httpclient.execute(httpGet);
        try {
            _log.debug("writeRemoteContent() - HTTP STATUS: {}",resp.getStatusLine());
            HttpEntity entity1 = resp.getEntity();
            entity1.writeTo(os);
            EntityUtils.consume(entity1);
        } finally {
            resp.close();
        }
    }


    /**
     * Thread safe DMR acquisition and caching.
     * @param coverageId  The cacheId (aka cache file name)
     * @return
     */
    private Element getCachedDMR(String coverageId) throws IOException, JDOMException, InterruptedException, WcsException {

        _log.debug("getCachedDMR() - BEGIN coverageId: {}",coverageId);

        String datasetUrl = getDataAccessUrl(coverageId);
        _log.debug("getCachedDMR() - DAP Dataset URL: {}",datasetUrl);
        if(datasetUrl==null)
            return null;

        String dmrUrl = datasetUrl + ".dmr.xml";
        _log.debug("getCachedDMR() - DMR URL: {}",dmrUrl);
        String cacheId = anyId2CacheId(dmrUrl);
        _log.debug("getCachedDMR() - cacheId: {}",cacheId);
        File cacheFile = new File(_cacheDir,cacheId);


        WriteLock writeLock = _cacheLock.writeLock();

        writeLock.lock();
        try {
            if(cacheFile.exists()){
                _log.debug("getCachedDMR() - Reading cached DMR.");
                Element dmrElement = opendap.xml.Util.getDocumentRoot(cacheFile);
                dmrElement.setAttribute("name",coverageId);
                return dmrElement;
            }
            else {
                _log.debug("getCachedDMR() - Retrieving DMR from DAP service");
                FileOutputStream fos = new FileOutputStream(cacheFile);
                writeRemoteContent(dmrUrl,fos);
                fos.close();
                Element dmrElement = opendap.xml.Util.getDocumentRoot(cacheFile);
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

    @Override
    public CoverageDescription getCoverageDescription(String coverageId) throws InterruptedException, WcsException {

        try {
            Element dmr = getCachedDMR(coverageId);
            if(dmr==null)
                return null;
            CoverageDescription coverageDescription = new DynamicCoverageDescription(dmr);
            return coverageDescription;

        } catch (JDOMException | IOException e) {
            _log.error("getCoverageDescription() - FAILED to get CoverageDescription for id: {} msg: {}"+
            coverageId, e.getMessage());
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
            Element covSum = new Element("CoverageSummary",WCS.WCS_NS);
            Element coverageID = cDesc.getCoverageIdElement();
            covSum.addContent(coverageID);
            Element coverageSubtype = new Element("CoverageSubtype",WCS.WCS_NS);
            covSum.addContent(coverageSubtype);
            coverageSubtype.addContent(cDesc.getCoverageDescriptionElement());
            return covSum;
        }
        return null;
    }

    @Override
    public Collection<Element> getCoverageSummaryElements() throws InterruptedException, WcsException {
        return new Vector<>();
    }

    @Override
    public Collection<Element> getDatasetSeriesSummaryElements() throws InterruptedException, WcsException {
        return new Vector<>();
    }

    public String getDmrUrl(String coverageId) throws InterruptedException {
        String datasetUrl =  getDataAccessUrl(coverageId);
        if(datasetUrl==null)
            return null;
        return datasetUrl + ".dmr.xml";
    }

    @Override
    public String getDataAccessUrl(String coverageId) throws InterruptedException {
        String longestMatchingDynamicServiceName=null;
        for(String dsName:_dynamicServicesMap.keySet()){
            if(coverageId.startsWith(dsName)){
                if(longestMatchingDynamicServiceName==null){
                    longestMatchingDynamicServiceName=dsName;
                }
                else {
                    if(longestMatchingDynamicServiceName.length() < dsName.length()) {
                        longestMatchingDynamicServiceName = dsName;
                    }
                }
            }
        }
        if(longestMatchingDynamicServiceName==null)
            return null;
        String dapServer = _dynamicServicesMap.get(longestMatchingDynamicServiceName);
        String datasetUrl = coverageId.replace(longestMatchingDynamicServiceName,dapServer);
        return datasetUrl;
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
}
