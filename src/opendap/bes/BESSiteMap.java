package opendap.bes;


import opendap.bes.dap2Responders.BesApi;
import opendap.io.HyraxStringEncoding;
import opendap.ppt.PPTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This servlet works with the BES system to build site map responses for Hyrax.
 *
 */
public class BESSiteMap {

    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private static final String PseudoFileOpener ="smap_";
    private static final String PseudoFileCloser =".txt";

    public static final long SITE_MAP_FILE_MAX_ENTRIES = 50000;  // Fifty Thousand entries per file.
    public static final long SITE_MAP_FILE_MAX_BYTES = 52428800; // 50MB per file.
    //public static final long SITE_MAP_FILE_MAX_ENTRIES = 100;
    //public static final long SITE_MAP_FILE_MAX_BYTES = 500;

    // @FIXME Make this a configuration option!!!!
    private static String SiteMapCacheFileName = "/etc/olfs/cache/SiteMap.cache";

    // @FIXME Make this a configuration option!!!!
    private static AtomicLong cacheRefreshInterval_ms = new AtomicLong(180 * 1000); // Ten minutes of ms

    static void setSiteMapCacheFileName(String fname){
        cacheLock.writeLock().lock();
        try{
            SiteMapCacheFileName = fname;
        }
        finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Set the SiteMap cache refresh interval
     * @param interval The number seconds in the refresh interval.
     */
    static void setCacheRefreshInterval(long interval){

        cacheLock.writeLock().lock();
        try{
            cacheRefreshInterval_ms.set(interval);
        }
        finally {
            cacheLock.writeLock().unlock();
        }

    }

    /**
     */
    private Logger _log;
    private TreeSet<String> _siteMap;
    private int _siteMapFileCount;
    private String _dapServicePrefix;
    private long _siteMapCharCount;
    private Date _creation;


    /**
     *
     * @param dapServicePrefix
     * @throws BESError
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     */
    public BESSiteMap(String dapServicePrefix)
            throws BESError, BadConfigurationException, PPTException, IOException {
        _log = LoggerFactory.getLogger(getClass());
        _siteMap = new TreeSet<>();
        _siteMapFileCount = 1;
        _dapServicePrefix = dapServicePrefix;
        _siteMapCharCount = getSiteMap();
        _creation = new Date();
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
        _log.debug(msg);
        return expired;
    }


    /**
     *
     * @return The number of items in the site map.
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
                    _log.debug("UPDATING SiteMap file: {}", smcFile.getAbsolutePath());
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
            _log.debug("Ingesting SiteMap file: {}", smcFile.getAbsolutePath());
            try (FileInputStream smcIs = new FileInputStream(smcFile)) {
                return ingestSiteMap(smcIs);
            }
        }
        finally {
            cacheLock.readLock().unlock();
        }

    }

    /**
     * Creates the cache file (and any required parent directories)
     *
     * @throws IOException If the parent directories or or the cache file cannot
     * be created.
     */
    private void makeCacheFileAsRequired() throws IOException{

        File cacheFile = new File(SiteMapCacheFileName);

        if(cacheFile.exists())
            return;

        if(cacheFile.getParentFile().mkdirs()){
            _log.debug("Created cache dir: '{}'",cacheFile.getParentFile().getAbsolutePath());
        }
        else {
            _log.debug("Cache dir: '{}' is present.",cacheFile.getParentFile().getAbsolutePath());
        }

        if (cacheFile.createNewFile()) {
            _log.debug("Created new SiteMap cache file: {}", cacheFile.getAbsolutePath());
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
        besApi.writeCombinedSiteMapResponse(_dapServicePrefix,os);
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

        _log.debug("Ingesting SiteMap from Stream");

        BufferedReader bfr = new BufferedReader(new InputStreamReader(smcIs, HyraxStringEncoding.getCharset()));
        _siteMapFileCount = 1;
        long charsInSiteMapPseudoFile = 0;
        long linesInSiteMapPseudoFile = 0;

        int i = 0;
        String line = bfr.readLine();
        long char_count = 0;
        while (line != null) {
            i++;
            _siteMap.add(line);
            char_count += line.length();
            linesInSiteMapPseudoFile++;
            charsInSiteMapPseudoFile += line.length();
            if (charsInSiteMapPseudoFile > SITE_MAP_FILE_MAX_BYTES || linesInSiteMapPseudoFile > SITE_MAP_FILE_MAX_ENTRIES) {
                _siteMapFileCount++;
                charsInSiteMapPseudoFile = 0;
                linesInSiteMapPseudoFile = 0;
            }
            line = bfr.readLine();
        }
        _log.debug("getSiteMapFromBes() - Processed {} lines.", i);
        _log.debug("getSiteMapFromBes() - siteMap has {} entries, {} characters.", _siteMap.size(), char_count);

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

        StringBuilder sb = new StringBuilder();
        _log.debug("Building siteMap files index response.");
        for(long i = 0; i < _siteMapFileCount ; i++){
            sb.append("sitemap: ").append(siteMapServicePrefix).append("/").append(PseudoFileOpener).append(Long.toString(i)).append(PseudoFileCloser).append("\n");
        }
        _log.debug("siteMap files response content:\n{}",sb.toString());
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
                    _log.error("Failed to parse integer file number in string '{}'",pseudoFilename);
                }
            }
        }

        // Did the parse effort succeed?
        if(targetFileIndex <0 || targetFileIndex >= _siteMapFileCount) {
            // If the parse effort failed we just return the top level file index.
            ps.println(getSiteMapEntryForRobotsDotText(siteMapServicePrefix));
            return;
        }

        int i=0;
        long char_count=0;
        int currentPseudoFile = 0;

        for(String line: _siteMap){
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

    public Date created(){ return _creation; }


}
