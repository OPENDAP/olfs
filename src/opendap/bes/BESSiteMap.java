package opendap.bes;


import opendap.bes.dap2Responders.BesApi;
import opendap.io.HyraxStringEncoding;
import opendap.ppt.PPTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import java.io.*;
import java.util.Date;
import java.util.TreeSet;

/**
 * This servlet works with the BES system to build site map responses for Hyrax.
 *
 */
public class BESSiteMap {

    private static final String PseudoFileOpener ="smap_";
    private static final String PseudoFileCloser =".txt";

    public static final long SITE_MAP_FILE_MAX_ENTRIES = 50000;  // Fifty Thousand entries per file.
    public static final long SITE_MAP_FILE_MAX_BYTES = 52428800; // 50MB per file.
    //public static final long SITE_MAP_FILE_MAX_ENTRIES = 100;
    //public static final long SITE_MAP_FILE_MAX_BYTES = 500;



    /**
     */
    private org.slf4j.Logger _log;
    private TreeSet<String> _siteMap;
    private int _siteMapFileCount;
    private String _dapServicePrefix;
    private long _siteMapCharCount;
    private Date _creation;


    public BESSiteMap(String dapServicePrefix) throws BESError, BadConfigurationException, PPTException, IOException {
        _log = org.slf4j.LoggerFactory.getLogger(getClass());
        _siteMap = new TreeSet<>();
        _siteMapFileCount = 1;
        _dapServicePrefix = dapServicePrefix;
        _siteMapCharCount = getSiteMapFromBes();
        _creation = new Date();
    }

    /**
     * Retrieves the entire site map from the BES by populating a TreeSet<String>.
     * @return The number of characters in the site map response from the BES.
     * @throws BadConfigurationException
     * @throws PPTException
     * @throws IOException
     * @throws BESError
     */
    private long getSiteMapFromBes() throws BadConfigurationException, PPTException, IOException, BESError {


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BesApi besApi = new BesApi();

        besApi.writeCombinedSiteMapResponse(_dapServicePrefix,baos);

        _log.debug("getSiteMapFromBes() - BES returned {} bytes in the getSiteMap response", baos.size());

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BufferedReader bfr = new  BufferedReader (new InputStreamReader(bais,HyraxStringEncoding.getCharset()));

        _siteMapFileCount = 1;
        long charsInSiteMapPseudoFile = 0;
        long linesInSiteMapPseudoFile = 0;

        int i=0;
        String line = bfr.readLine();
        long char_count=0;
        while(line != null) {
            i++;
            _siteMap.add(line);
            char_count += line.length();
            linesInSiteMapPseudoFile++;
            charsInSiteMapPseudoFile += line.length();
            if( charsInSiteMapPseudoFile > SITE_MAP_FILE_MAX_BYTES || linesInSiteMapPseudoFile > SITE_MAP_FILE_MAX_ENTRIES){
                _siteMapFileCount++;
                charsInSiteMapPseudoFile = 0;
                linesInSiteMapPseudoFile = 0;
            }
            line = bfr.readLine();
        }
        _log.debug("getSiteMapFromBes() - Processed {} lines.", i);
        _log.debug("getSiteMapFromBes() - siteMap has {} entries, {} characters.", _siteMap.size(),char_count);

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
        return sb.toString();
    }



    /**
     * Sends a partial siteMap response as a pseudo file .
     *  If we are here then the request should be asking for a siteMap sub file.
     *   If not then we return the top level site map response...
     *   We look at the total number of siteMapfiles in this siteMap (computed)
     *    and then form the ith file based on their URL path.
     * @param siteMapServicePrefix
     * @param sos
     * @param relativeUrl
     * @throws IOException
     */
    public void send_pseudoSiteMapFile(String siteMapServicePrefix , ServletOutputStream sos, String relativeUrl ) throws IOException  {

        // We try to "parse" the request URL to see if it's a site map sub file.
        String pseudoFilename = relativeUrl;
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
            sos.println(getSiteMapEntryForRobotsDotText(siteMapServicePrefix));
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
                sos.println(line);
            }
            if(currentPseudoFile > targetFileIndex)
                    break;
        }

    }

    public Date created(){ return _creation; }


}
