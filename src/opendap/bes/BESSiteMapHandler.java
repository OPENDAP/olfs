package opendap.bes;


import opendap.bes.dap2Responders.BesApi;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.io.HyraxStringEncoding;
import opendap.logging.LogUtil;
import opendap.logging.Procedure;
import opendap.logging.Timer;
import opendap.ppt.PPTException;
import org.jdom.Document;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class BESSiteMapHandler extends HttpServlet {

    private static final String PseudoFileOpener ="smap_";
    private static final String PseudoFileCloser =".txt";

    public static final long SITE_MAP_FILE_MAX_ENTRIES = 50000;  // Fifty Thousand entries per file.
    public static final long SITE_MAP_FILE_MAX_BYTES = 52428800; // 50MB per file.
    //public static final long SITE_MAP_FILE_MAX_ENTRIES = 100;
    //public static final long SITE_MAP_FILE_MAX_BYTES = 500;

    /**
     * ************************************************************************
     * A thread safe hit counter
     *
     * @serial
     */
    private AtomicInteger reqNumber;
    private org.slf4j.Logger log;

    /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from any of the dods code...
     *
     * @throws javax.servlet.ServletException
     */
    public void init() throws ServletException {

        super.init();

        LogUtil.initLogging(this);
        log = org.slf4j.LoggerFactory.getLogger(getClass());

        // Timer.enable();
        RequestCache.openThreadCache();

        reqNumber = new AtomicInteger(0);

        log.debug("init() start");

        log.info("init() complete.");
        RequestCache.closeThreadCache();
    }

    private long getSiteMapFromBes(HttpServletRequest request, TreeSet<String> siteMap) throws BadConfigurationException, PPTException, IOException, BESError {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BesApi besApi = new BesApi();

        Request dapReq = new Request(this,request);
        String dapService = dapReq.getWebApplicationUrl();
        besApi.writeSiteMapResponse(dapService,baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        BufferedReader bfr = new  BufferedReader (new InputStreamReader(bais,HyraxStringEncoding.getCharset()));

        int i=0;
        String line = bfr.readLine();
        long byte_count=0;
        while(line != null) {
            i++;
            siteMap.add(line);
            byte_count += line.length();
            line = bfr.readLine();
        }
        log.debug("i: {}", i);
        log.debug("siteMap has {} entries, {} bytes.", siteMap.size(),byte_count);

        return byte_count;
    }


    /**
     * Sends the top level site map. If the siteMap fits in a single file then the siteMap is sent in total. If the site
     * map requires multiple files then the list of site map files is sent.
     *
     * @param request
     * @param sos
     * @param siteMapFileCount
     * @param siteMap
     * @throws IOException
     */
    private void sendTopSiteMap(HttpServletRequest request, ServletOutputStream sos, long siteMapFileCount, TreeSet<String> siteMap ) throws IOException {
        if(siteMapFileCount==1){
            // Here we send the entire siteMap if the size is cool
            for(String aline: siteMap) {
                sos.println(aline);
            }
        }
        else {
            Request dapReq = new Request(this,request);
            String siteMapService = dapReq.getServiceUrl();
            log.debug("Building siteMap files index response.");
            for(long i = 0; i < siteMapFileCount ; i++){
                String pfn = siteMapService+"/"+ PseudoFileOpener +Long.toString(i)+ PseudoFileCloser;
                sos.println(pfn);
            }
        }
    }

    /**
     * ***********************************************************************
     * Handles incoming requests from clients. Parses the request and determines
     * what kind of OPeNDAP response the cleint is requesting. If the request is
     * understood, then the appropriate handler method is called, otherwise
     * an error is returned to the client.
     * <p/>
     * This method is the entry point for <code>OLFS</code>. It uses
     * the methods <code>processOpendapURL</code> to extract the OPeNDAP URL
     * information from the incoming client request. This OPeNDAP URL information
     * is cached and made accessible through get and set methods.
     * <p/>
     * After  <code>processOpendapURL</code> is called <code>loadIniFile()</code>
     * is called to load configuration information from a .ini file,
     * <p/>
     * If the standard behaviour of the servlet (extracting the OPeNDAP URL
     * information from the client request, or loading the .ini file) then
     * you should overload <code>processOpendapURL</code> and <code>loadIniFile()
     * </code>. <b> We don't recommend overloading <code>doGet()</code> beacuse
     * the logic contained there may change in our core and cause your server
     * to behave unpredictably when future releases are installed.</b>
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        String relativeUrl = ReqInfo.getLocalUrl(request);

        int request_status = HttpServletResponse.SC_OK;

        try {
            Procedure timedProcedure = Timer.start();
            RequestCache.openThreadCache();
            try {

                int reqno = reqNumber.incrementAndGet();
                LogUtil.logServerAccessStart(request, "HyraxAccess", "HTTP-GET", Long.toString(reqno));

                log.debug(Util.getMemoryReport());
                log.debug(ServletUtil.showRequest(request, reqno));
                log.debug(ServletUtil.probeRequest(this, request));

                if (ReqInfo.isServiceOnlyRequest(request)) {
                    String reqURI = request.getRequestURI();
                    String newURI = reqURI+"/";
                    response.sendRedirect(Scrub.urlContent(newURI));
                    log.debug("Sent redirectForServiceOnlyRequest to map the servlet " +
                            "context to a URL that ends in a '/' character!");
                    return;
                }

                String msg = "Requested relative URL: '" + relativeUrl +
                        "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                        "' CE: '" + ReqInfo.getConstraintExpression(request) + "'";
                log.debug(msg);

                ServletOutputStream sos = response.getOutputStream();

                TreeSet<String> siteMap = new TreeSet<>();
                long byte_count = getSiteMapFromBes(request, siteMap);
                long siteMapFileCount = siteMapFileCount(byte_count,siteMap.size());
                log.debug("siteMapFileCount: {}",siteMapFileCount);

                if (relativeUrl.equals("/")) {
                    log.debug("Just the service endpoint. {}",request.getRequestURI());
                    sendTopSiteMap(request,sos,siteMapFileCount,siteMap);
                }
                else {
                    // If we are here then the request should be asking for a siteMap sub file.
                    send_pseudoSiteMapFile(request,sos,relativeUrl,siteMapFileCount,siteMap);                }
            }
            finally {
                Timer.stop(timedProcedure);
            }
        }
        catch (Throwable t) {
            try {
                request_status = OPeNDAPException.anyExceptionHandler(t, this, response);
            }
            catch(Throwable t2) {
                try {
                    log.error("\n########################################################\n" +
                            "Request processing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error log attempt for this request.\n" +
                            "########################################################\n", t2);
                }
                catch(Throwable t3){
                    // It's boned now.. Leave it be.
                }
            }
        }
        finally {
            LogUtil.logServerAccessEnd(request_status, "HyraxAccess");
            RequestCache.closeThreadCache();
            log.info("doGet(): Response completed.\n");
        }
        log.info("doGet() - Timing Report: \n{}", Timer.report());
        Timer.reset();
    }

    /**
     * Determine the number of files into which the siteMap must be broken based on the rules at
     * https://www.sitemaps.org/protocol.html
     *
     * @param byte_count Number of bytes in total site map.
     * @param siteMap_size Number of entries in total site map.
     * @return
     */
    long siteMapFileCount(long byte_count, int siteMap_size){
        long siteMapFileCount = 1 + siteMap_size / SITE_MAP_FILE_MAX_ENTRIES;
        long bytesPerFile = byte_count/siteMapFileCount;
        long newFileCount = siteMapFileCount;
        boolean done = false;

        // Here we try to allow for long lines in the files, but only up to a point...
        while(bytesPerFile > SITE_MAP_FILE_MAX_BYTES && !done){
            newFileCount++;
            bytesPerFile = byte_count/newFileCount;
            if(newFileCount >= SITE_MAP_FILE_MAX_ENTRIES) {
                done = true;
            }
        }
        siteMapFileCount = newFileCount;
        return siteMapFileCount;
    }


    /**
     * Sends a partial siteMap response as a pseudo file .
     *  If we are here then the request should be asking for a siteMap sub file.
     *   If not then we return the top level site map response...
     *   We look at the total number of siteMapfiles in this siteMap (computed)
     *    and then form the ith file based on their URL path.
     * @param request
     * @param sos
     * @param relativeUrl
     * @param siteMapFileCount
     * @param siteMap
     * @throws IOException
     */
    private void send_pseudoSiteMapFile(HttpServletRequest request, ServletOutputStream sos, String relativeUrl, long siteMapFileCount, TreeSet<String> siteMap) throws IOException  {

        // We try to "parse" the request URL to see if it's a site map sub file.
        String pseudoFilename = relativeUrl;
        int indx = pseudoFilename.indexOf(PseudoFileOpener);

        int file_index = -1;
        if(indx==0 || indx==1){
            String s = pseudoFilename.substring(indx+ PseudoFileOpener.length());
            indx = s.indexOf(PseudoFileCloser);
            if(indx>0){
                s = s.substring(0,indx);
                try {
                    file_index = Integer.parseInt(s);
                }
                catch (NumberFormatException nfe){
                    log.error("Failed to parse integer file number in string '{}'",pseudoFilename);
                }
            }
        }

        // Did the parse effort succeed?
        if(file_index <0 || file_index >= siteMapFileCount) {
            // If the parse effort failed we just return the top level file index.
            sendTopSiteMap(request,sos,siteMapFileCount,siteMap);
            return;
        }

        // Now we need to transmit a range from the site map that corresponds to their selected
        // pseudo file number. The following is crude, but required due to the nature of the
        // accessor methods in the underlying TreeSet. We could change up to Vector but at the cost
        // of ordering...
        long linksPerFile = siteMap.size()/siteMapFileCount;
        long start = file_index * linksPerFile;
        long stop = start + linksPerFile;
        long index = 0;
        long bytes = 0;
        for(String line: siteMap){
            if(index >= start && index < stop && bytes < SITE_MAP_FILE_MAX_BYTES){
                sos.println(line);
                bytes += line.length();
            }
            index++;
        }

    }

}
