package opendap.bes;


import opendap.PathBuilder;
import opendap.coreServlet.*;
import opendap.dap.Request;
import opendap.logging.LogUtil;
import opendap.logging.Procedure;
import opendap.logging.Timer;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final AtomicInteger REQ_NUMBER = new AtomicInteger(0);
    private static final Logger LOG = LoggerFactory.getLogger(BESSiteMapService.class);

    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);

    private static String _dapServiceContext;

    /**
     * ************************************************************************
     * Intitializes the servlet. Init (at this time) basically sets up
     * the object opendap.util.Debug from the debuggery flags in the
     * servlet InitParameters. The Debug object can be referenced (with
     * impunity) from any of the dods code...
     *
     * @throws javax.servlet.ServletException
     */
    @Override
    public void init() throws ServletException {

        INIT_LOCK.lock();
        try {
            if(IS_INITIALIZED.get())
                return;

            super.init();

            LogUtil.initLogging(this);
            LOG.debug("init() start");
            // Timer.enable();
            RequestCache.openThreadCache();


            _dapServiceContext = getInitParameter("DapServiceContext");
            if (_dapServiceContext == null)
                _dapServiceContext = "opendap/";

            if (!_dapServiceContext.isEmpty() && !_dapServiceContext.endsWith("/"))
                _dapServiceContext += "/";


            if (!BESManager.isInitialized()) {
                String filename = PathBuilder.pathConcat(ServletUtil.getConfigPath(this), "olfs.xml");
                Element configElement;
                try {
                    configElement = opendap.xml.Util.getDocumentRoot(filename);

                } catch (Exception e) {
                    throw new ServletException("Failed to read configuration file " + filename + " message: " + e.getMessage());
                }

                try {
                    Element besManagerElement = configElement.getChild(BESManager.BES_MANAGER_CONFIG_ELEMENT);
                    BESManager.init(getServletContext(), besManagerElement);

                } catch (Exception e) {
                    throw new ServletException("Failed to initialize BES.  message: " + e.getMessage());
                }

            }
            IS_INITIALIZED.set(true);

            LOG.info("init() complete.");
            RequestCache.closeThreadCache();
        }
        finally {
            INIT_LOCK.unlock();
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
        Request req = new Request(this,request);
        String siteMapServicePrefix = req.getServiceUrl();

        String webapp = req.getWebApplicationUrl();

        String dapService;
        if(getServletContext().getContextPath().isEmpty()){
            // If we are running in the ROOT context (no contextPath) then we make the assumption that the DAP
            // service is located at the _dapServiceContext as set in the configuration parameter DapServiceContext.
            dapService = PathBuilder.pathConcat(webapp,_dapServiceContext);
        }
        else {
            dapService = webapp;
        }


        int request_status = HttpServletResponse.SC_OK;
        int response_size = -1;
        try {
            Procedure timedProcedure = Timer.start();
            RequestCache.openThreadCache();
            try {

                int reqno = REQ_NUMBER.incrementAndGet();
                LogUtil.logServerAccessStart(request, LogUtil.SITEMAP_ACCESS_LOG_ID, "HTTP-GET", Long.toString(reqno));

                LOG.debug(Util.getMemoryReport());
                LOG.debug(ServletUtil.showRequest(request, reqno));
                LOG.debug(ServletUtil.probeRequest(this, request));

                if (ReqInfo.isServiceOnlyRequest(request)) {
                    String reqURI = request.getRequestURI();
                    String newURI = reqURI+"/";
                    response.sendRedirect(Scrub.urlContent(newURI));
                    LOG.debug("Sent redirectForServiceOnlyRequest to map the servlet " +
                            "context to a URL that ends in a '/' character!");
                    return;
                }

                String msg = "Requested relative URL: '" + relativeUrl +
                        "' suffix: '" + ReqInfo.getRequestSuffix(request) +
                        "' CE: '" + ReqInfo.getConstraintExpression(request) + "'";
                LOG.debug(msg);

                DataOutputStream dos = new DataOutputStream(response.getOutputStream());
                PrintStream sos = new PrintStream(dos);

                BESSiteMap besSiteMap = new BESSiteMap(dapService);

                if (relativeUrl.equals("/")) {
                    LOG.debug("Just the service endpoint. {}",request.getRequestURI());
                    sos.println(besSiteMap.getSiteMapEntryForRobotsDotText(siteMapServicePrefix));
                }
                else {
                    // If we are here then the request should be asking for a siteMap sub file.
                    besSiteMap.send_pseudoSiteMapFile(siteMapServicePrefix,sos,relativeUrl);
                }
                response_size = dos.size();
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
                    LOG.error("\n########################################################\n" +
                            "Request processing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error logging attempt for this request.\n" +
                            "########################################################\n", t2);
                }
                catch(Throwable t3){
                    // It's boned now.. Leave it be.
                }
            }
        }
        finally {
            LogUtil.logServerAccessEnd(request_status, response_size, LogUtil.SITEMAP_ACCESS_LOG_ID);
            RequestCache.closeThreadCache();
            LOG.info("Response completed.\n");
        }
        LOG.info("Timing Report: \n{}", Timer.report());
        Timer.reset();
    }

    @Override
    protected long getLastModified(HttpServletRequest req) {
        return new Date().getTime();
    }



}
