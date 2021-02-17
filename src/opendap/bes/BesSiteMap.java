package opendap.bes;


import opendap.bes.dap2Responders.BesApi;
import opendap.io.HyraxStringEncoding;
import opendap.ppt.PPTException;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This servlet works with the BES system to build site map responses for Hyrax.
 *
 */
public class BesSiteMap {
    private static final Logger LOG = LoggerFactory.getLogger(BesSiteMap.class);

    public static final String SITE_MAP_CACHE_ELEMENT_NAME = "SiteMapCache";

    public static final String REFRESH_INTERVAL_ATTRIBUTE_NAME = "refreshInterval";
    public static final String CACHE_FILE_ATTRIBUTE_NAME = "cacheFile";
    public static final String ROBOTS_BASE_ATTRIBUTE_NAME = "robotsBaseFile";

    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private static final AtomicBoolean ENABLED = new AtomicBoolean(false);

    private static final String PseudoFileOpener ="smap_";
    private static final String PseudoFileCloser =".txt";

    public static final long SITE_MAP_FILE_MAX_ENTRIES = 49500;  // Fifty Thousand entries per file is the specification max
    public static final long SITE_MAP_FILE_MAX_BYTES = 50000000; // 50MB per file is the specification max
    //public static final long SITE_MAP_FILE_MAX_ENTRIES = 100;
    //public static final long SITE_MAP_FILE_MAX_BYTES = 500;

    public static final String  DEFAULT_CACHE_FILE = "/etc/olfs/cache/SiteMap.cache"; // this is a punt
    private static String SiteMapCacheFileName = DEFAULT_CACHE_FILE;

    public static final String  DEFAULT_ROBOTS_BASE_FILE = "/etc/olfs/robots.base"; // this is a punt
    private static String RobotsBaseText = "";


    public static final long DEFAULT_CACHE_REFRESH_INTERVAL = 600; // Ten minutes worth of seconds.
    private static AtomicLong cacheRefreshInterval_ms = new AtomicLong(DEFAULT_CACHE_REFRESH_INTERVAL * 1000);

    private Logger log;
    private TreeSet<String> siteMap;
    private int siteMapFileCount;
    private String dapServicePrefix;
    private long siteMapCharCount;
    private Date creation;


    /**
     *
     * @param dapServicePrefix
     * @throws BESError
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     */
    public BesSiteMap(String dapServicePrefix)
            throws BESError, BadConfigurationException, PPTException, IOException {
        if(!ENABLED.get()){
            throw new BadConfigurationException("BESSiteMap has not been initialized.");
        }
        log = LoggerFactory.getLogger(getClass());
        siteMap = new TreeSet<>();
        siteMapFileCount = 1;
        this.dapServicePrefix = dapServicePrefix;
        siteMapCharCount = getSiteMap();
        creation = new Date();
    }

    /**
     * Initialize the Node Cache using an XML Element.
     * @param config The "NodeCache" configuration element
     * @throws BadConfigurationException When the configuration is broken.
     */
    public static void init(Element config)
            throws BadConfigurationException {

        if(ENABLED.get())
            return;

        if (config == null || !config.getName().equals(SITE_MAP_CACHE_ELEMENT_NAME))
            throw new BadConfigurationException("BESSiteMap must be passed a non-null configuration " +
                    "element named " + SITE_MAP_FILE_MAX_ENTRIES);

        long refreshIntervalSeconds = DEFAULT_CACHE_REFRESH_INTERVAL;
        String refreshIntervalString = config.getAttributeValue(REFRESH_INTERVAL_ATTRIBUTE_NAME);
        if(refreshIntervalString!= null) {
            try {
                refreshIntervalSeconds = Long.parseLong(refreshIntervalString);
                if (refreshIntervalSeconds <= 0) {
                    refreshIntervalSeconds = DEFAULT_CACHE_REFRESH_INTERVAL;
                    String msg = "Failed to parse value of " +
                            SITE_MAP_CACHE_ELEMENT_NAME + "@" +
                            REFRESH_INTERVAL_ATTRIBUTE_NAME + "! " +
                            "Value must be an integer > 0. Using default value: " +
                            refreshIntervalSeconds;
                    LOG.error(msg);
                }
            } catch (NumberFormatException nfe) {
                String errMsg = "Failed to parse value of " +
                        SITE_MAP_CACHE_ELEMENT_NAME + "@" +
                        REFRESH_INTERVAL_ATTRIBUTE_NAME + "! Value must be an integer." +
                        " Using default value: " + refreshIntervalSeconds;
                LOG.error(errMsg);
            }
        }

        String cacheFileName = DEFAULT_CACHE_FILE;
        String cacheFileNameString = config.getAttributeValue(CACHE_FILE_ATTRIBUTE_NAME);
        if(cacheFileNameString!=null){
            cacheFileName = cacheFileNameString;
        }

        String rbFileName = DEFAULT_ROBOTS_BASE_FILE;
        String robotsBaseAttrValue = config.getAttributeValue(ROBOTS_BASE_ATTRIBUTE_NAME);
        if(robotsBaseAttrValue!=null){
            rbFileName = robotsBaseAttrValue;
        }

        init(rbFileName, cacheFileName, refreshIntervalSeconds);
    }


    /**
     * The _actual_ init method that sets up the cache. This must be called
     * prior to using the cache.
     * @param cacheFileName The maximum number of entries in the cache
     * @param refreshIntervalSeconds The time that a site map is considered
     *                               valid, after which it must be refreshed.
     */
    public static void init(String robotsBaseFilename, String cacheFileName, long refreshIntervalSeconds)
            throws BadConfigurationException {
        cacheLock.writeLock().lock();
        try {
            if (ENABLED.get()) {
                LOG.error("BESSiteMap has already been initialized!  " +
                                "SiteMapCacheFile: {}  RefreshInterval: {} s",
                        SiteMapCacheFileName,
                        cacheRefreshInterval_ms.get()/1000);
                return;
            }

            // convert to milliseconds
            cacheRefreshInterval_ms.set(refreshIntervalSeconds * 1000);

            SiteMapCacheFileName = cacheFileName;
            checkSiteMapFileLocation();

            // Load the robots.txt base file form the configuration, if possible.
            try {
                byte[] allTheBytes = Files.readAllBytes(Paths.get(robotsBaseFilename));
                RobotsBaseText = new String(allTheBytes, HyraxStringEncoding.getCharset());
            }
            catch (IOException e) {
                LOG.error("Failed to read robots base file: {} , Message: {} SKIPPING.",robotsBaseFilename,e.getMessage());
            }
            RobotsBaseText += "\n";
            ENABLED.set(true);
            LOG.debug("INITIALIZED  SiteMapCacheFile: {}  RefreshInterval: {} s",
                    SiteMapCacheFileName,
                    cacheRefreshInterval_ms.get()/(1000));
        }
        finally {
            cacheLock.writeLock().unlock();
        }
    }




    /**
     * Checks to see if the cache file has expired by comparing the file's LMT
     * to the current time.
     *
     * @return Returns true is the cache file is expired, false otherwise.
     */
    private boolean cacheExpired(){

        File cacheFile = new File(SiteMapCacheFileName);
        boolean expired;
        String msg;
        if(cacheFile.exists()) {
            long cacheLMT_ms = cacheFile.lastModified();
            long now_ms = new Date().getTime();
            long cacheAge_ms = now_ms - cacheLMT_ms;
            expired =  cacheAge_ms > cacheRefreshInterval_ms.get();
            msg = "SiteMap Cache is "+(expired?"EXPIRED":"READY")+
                    ". (cacheAge: "+cacheAge_ms+" ms "+ " refreshInterval: "+
                    cacheRefreshInterval_ms.get()+" ms)";
        }
        else {
            expired = true;
            msg = "SiteMap Cache is EXPIRED. (No Cache File Found: "+
                    SiteMapCacheFileName+")";
        }
        log.debug(msg);
        return expired;
    }


    /**
     * Loads the SiteMap. If the stiemap cache file is missing/empty/expired
     * then it will be rebuilt.
     * @return The number of bytes in the site map.
     * @throws BESError
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     */
    private long getSiteMap() throws BESError, BadConfigurationException, PPTException, IOException {

        File smcFile = new File(SiteMapCacheFileName);

        cacheLock.readLock().lock();
        if(cacheExpired()){
            cacheLock.readLock().unlock(); // dump read lock
            cacheLock.writeLock().lock();  // grab write lock
            // Lock upgraded to write lock.
            try {
                // Recheck expired condition as it may have changed before we
                // got the writeLock
                if(cacheExpired()){
                    makeCacheFileAsRequired();
                    log.debug("UPDATING SiteMap file: {}", smcFile.getAbsolutePath());
                    try (FileOutputStream fos = new FileOutputStream(smcFile)) {
                        writeBesSiteMap(fos);
                    }
                }
                // Downgrade lock to readLock
                cacheLock.readLock().lock();
            }
            finally {
                cacheLock.writeLock().unlock();
            }
        }

        try {
            log.debug("Ingesting SiteMap file: {}", smcFile.getAbsolutePath());
            try (FileInputStream smcIs = new FileInputStream(smcFile)) {
                return ingestSiteMap(smcIs);
            }
        }
        finally {
            cacheLock.readLock().unlock();
        }

    }

    /**
     * This checks to make sure that a site map file can be placed in the
     * location indicated by SiteMapCacheFileName.
     *
     * If the site map files exists and can be written to and read from then the
     * check returns.
     *
     * @throws BadConfigurationException When the cache file is unusable or the
     * parent directory coannot be created or does not exist.
     **/
    private static void checkSiteMapFileLocation()
            throws BadConfigurationException {

        File cacheFile = new File(SiteMapCacheFileName);
        if(cacheFile.exists()){
            if(!cacheFile.canRead() || !cacheFile.canWrite()) {
                throw new BadConfigurationException("Unable to read and write " +
                        "to the SiteMap cache file: "+SiteMapCacheFileName);
            }
            LOG.debug("Cache file: '{}' exists and rw privileges confirmed",
                    cacheFile.getAbsolutePath());
            return;
        }

        File parent = cacheFile.getParentFile();

        if(parent.mkdirs()){
            LOG.debug("Created cache dir: '{}'",
                    cacheFile.getParentFile().getAbsolutePath());
        }
        else if(parent.exists()){
            LOG.debug("Cache dir: '{}' is present.",
                    cacheFile.getParentFile().getAbsolutePath());
        }
        else {
            throw new BadConfigurationException("Unable to create cache " +
                    "directory: "+parent.getAbsolutePath());
        }


    }


    /**
     * Creates the cache file (and any required parent directories)
     *
     * @throws IOException If the parent directories or or the cache file cannot
     * be created.
     */
    private static void makeCacheFileAsRequired() throws IOException{

        File cacheFile = new File(SiteMapCacheFileName);

        if(cacheFile.exists()) {
            return;
        }
        File parent = cacheFile.getParentFile();
        if(parent.mkdirs()){
            LOG.debug("Created cache dir: '{}'",cacheFile.getParentFile().getAbsolutePath());
        }
        else if(parent.exists()){
            LOG.debug("Cache dir: '{}' is present.",cacheFile.getParentFile().getAbsolutePath());
        }
        else {
            throw new IOException("Unable to find or create the cache " +
                    "directory: "+parent.getAbsolutePath());
        }

        if (cacheFile.createNewFile()) {
            LOG.debug("Created new SiteMap cache file: {}", cacheFile.getAbsolutePath());
        }
    }


    /**
     * Writes the BES SiteMap to the passed InputStream;
     * @param os
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     * @throws BESError
     */
    private void writeBesSiteMap(OutputStream os) throws BadConfigurationException, PPTException, IOException, BESError {
        BesApi besApi = new BesApi();
        besApi.writeCombinedSiteMapResponse(dapServicePrefix,os);
    }

    /**
     * Retrieves the entire site map from the BES by populating a TreeSet<String>.
     * @return The number of characters in the site map response from the BES.
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     * @throws BESError
     */
    private long ingestSiteMap(InputStream smcIs) throws IOException {

        log.debug("Ingesting SiteMap from Stream");

        BufferedReader bfr = new BufferedReader(new InputStreamReader(smcIs, HyraxStringEncoding.getCharset()));
        siteMapFileCount = 1;
        long charsInSiteMapPseudoFile = 0;
        long linesInSiteMapPseudoFile = 0;

        int i = 0;
        String line = bfr.readLine();
        long char_count = 0;
        while (line != null) {
            i++;
            siteMap.add(line);
            char_count += line.length();
            linesInSiteMapPseudoFile++;
            charsInSiteMapPseudoFile += line.length();
            if (charsInSiteMapPseudoFile > SITE_MAP_FILE_MAX_BYTES || linesInSiteMapPseudoFile > SITE_MAP_FILE_MAX_ENTRIES) {
                siteMapFileCount++;
                charsInSiteMapPseudoFile = 0;
                linesInSiteMapPseudoFile = 0;
            }
            line = bfr.readLine();
        }
        log.debug("getSiteMapFromBes() - Processed {} lines.", i);
        log.debug("getSiteMapFromBes() - siteMap has {} entries, {} characters.", siteMap.size(), char_count);

        return char_count;
    }




    /**
     * Sends the top level site map. If the siteMap fits in a single file then the siteMap is sent in total. If the site
     * map requires multiple files then the list of site map files is sent.
     *
     * @param siteMapServicePrefix
     * @throws IOException
     */
    public String getSiteMapEntryForRobotsDotText(String siteMapServicePrefix ) throws IOException {

        StringBuilder sb = new StringBuilder(RobotsBaseText);
        log.debug("Building siteMap files index response.");
        for(long i = 0; i < siteMapFileCount; i++){
            sb.append("Sitemap: ").append(siteMapServicePrefix).append("/").append(PseudoFileOpener).append(Long.toString(i)).append(PseudoFileCloser).append("\n");
        }
        log.debug("siteMap files response content:\n{}",sb.toString());
        return sb.toString();
    }



    /**
     * Sends a partial siteMap response as a pseudo file .
     *  If we are here then the request should be asking for a siteMap sub file.
     *   If not then we return the top level site map response...
     *   We look at the total number of siteMapfiles in this siteMap (computed)
     *    and then form the ith file based on their URL path.
     * @param siteMapServicePrefix
     * @param ps
     * @param pseudoFilename
     * @throws IOException
     */
    public void send_pseudoSiteMapFile(String siteMapServicePrefix , PrintStream ps, String pseudoFilename ) throws IOException  {

        // We try to "parse" the request URL to see if it's a site map sub file.
        int indx = pseudoFilename.indexOf(PseudoFileOpener);

        int targetFileIndex = -1;
        if(indx==0 || indx==1){
            String s = pseudoFilename.substring(indx+ PseudoFileOpener.length());
            indx = s.indexOf(PseudoFileCloser);
            if(indx>0){
                s = s.substring(0,indx);
                try {
                    targetFileIndex = Integer.parseInt(s);
                }
                catch (NumberFormatException nfe){
                    log.error("Failed to parse integer file number in string '{}'",pseudoFilename);
                }
            }
        }

        // Did the parse effort succeed?
        if(targetFileIndex <0 || targetFileIndex >= siteMapFileCount) {
            // If the parse effort failed we just return the top level file index.
            ps.println(getSiteMapEntryForRobotsDotText(siteMapServicePrefix));
            return;
        }

        int i=0;
        long char_count=0;
        int currentPseudoFile = 0;

        for(String line: siteMap){
            i++;
            char_count += line.length();
            if( char_count > SITE_MAP_FILE_MAX_BYTES || i > SITE_MAP_FILE_MAX_ENTRIES){
                currentPseudoFile++;
                char_count = line.length();
                i = 1;
            }
            if(currentPseudoFile == targetFileIndex) {
                ps.println(line);
            }
            if(currentPseudoFile > targetFileIndex)
                    break;
        }

    }

    public Date created(){ return creation; }


}
