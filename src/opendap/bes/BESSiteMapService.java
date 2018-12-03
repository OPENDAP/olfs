package opendap.bes;


import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.logging.LogUtil;
import opendap.logging.Procedure;
import opendap.logging.Timer;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This servlet works with the BES system to build site map responses for Hyrax.
 *
 */
public class BESSiteMapService extends HttpServlet {


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

        Request req = new Request(this,request);
        String servicePrefix = req.getWebApplicationUrl();

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

                BESSiteMap besSiteMap = new BESSiteMap();

                if (relativeUrl.equals("/")) {
                    log.debug("Just the service endpoint. {}",request.getRequestURI());
                    sos.println(besSiteMap.getSiteMapEntryForRobotsDotText(request));
                }
                else {
                    // If we are here then the request should be asking for a siteMap sub file.
                    besSiteMap.send_pseudoSiteMapFile(request,sos,relativeUrl);                }
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


}
