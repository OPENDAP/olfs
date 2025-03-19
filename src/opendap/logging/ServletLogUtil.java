/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
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

package opendap.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import opendap.PathBuilder;
import opendap.coreServlet.RequestId;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;


/**
 * User: ndp
 * Date: Feb 6, 2007
 * Time: 3:34:35 PM
 */
public class ServletLogUtil {

    public static final String HYRAX_ACCESS_LOG_ID = "HyraxAccess";

    public static final String HYRAX_LAST_MODIFIED_ACCESS_LOG_ID = "HyraxLastModifiedAccess";

    public static final String DOCS_ACCESS_LOG_ID = "DocsAccess";

    public static final String SITEMAP_ACCESS_LOG_ID = "SiteMapAccess";

    public static final String PDP_SERVICE_ACCESS_LOG_ID = "PDPServiceAccess";
    public static final String PDP_SERVICE_LAST_MODIFIED_LOG_ID = "PDPServiceLastModifiedAccess";

    public static final String GATEWAY_ACCESS_LOG_ID = "HyraxGatewayAccess";
    public static final String GATEWAY_ACCESS_LAST_MODIFIED_LOG_ID = "HyraxGatewayLastModifiedAccess";

    public static final String BUILD_DMRPP_ACCESS_LOG_ID = "HyraxBuildDmrppAccess";
    public static final String BUILD_DMRPP_LAST_MODIFIED_LOG_ID = "HyraxBuildDmrppLastModifiedAccess";

    public static final String ADMIN_ACCESS_LOG_ID = "HyraxAdminAccess";
    public static final String ADMIN_ACCESS_LAST_MODIFIED_LOG_ID = "HyraxAdminLastModifiedAccess";

    public static final String WCS_ACCESS_LOG_ID = "WCSAccess";
    public static final String WCS_LAST_MODIFIED_ACCESS_LOG_ID = "WCSLastModifiedAccess";

    public static final String CLOUDWATCH_REQUEST_LOG = "CloudWatchRequestLog";
    public static final String CLOUDWATCH_RESPONSE_LOG = "CloudWatchResponseLog";





    private static final String REQUEST_ID_KEY = "ID";
    private static final String HTTP_VERB_KEY = "SOURCE";
    private static final String CLIENT_HOST_KEY = "host";
    private static final String SESSION_ID_KEY = "ident";
    private static final String USER_ID_KEY = "userid";
    private static final String USER_AGENT_KEY = "UserAgent";
    private static final String START_TIME_KEY = "startTime";
    private static final String RESOURCE_ID_KEY = "resourceID";
    private static final String QUERY_STRING_KEY = "query";
    private static final String RESPONSE_SIZE_KEY = "size";
    private static final String DURATION_KEY = "duration";
    private static final String HTTP_STATUS_KEY = "http_status";


    private static final AtomicBoolean isLogInit = new AtomicBoolean(false);
    private static final ReentrantLock initLock =  new ReentrantLock();

    private static final AtomicBoolean useCombinedLog = new AtomicBoolean(false);
    public static final AtomicBoolean useDualCloudWatchLogs = new AtomicBoolean(false);

    public static final int MISSING_SIZE_VALUE = -1;


    private static final Logger log;
    static{
        System.out.println("+++LogUtil.static - Instantiating Logger ...");
        try {
            log = org.slf4j.LoggerFactory.getLogger(ServletLogUtil.class);
            //log.info("Logger instantiated. class: " + log.getClass().getCanonicalName());
            System.out.println("Logger instantiated. class: " + log.getClass().getCanonicalName());
        }
        catch(Exception e) {
            System.err.println("\n\n[ERROR]  +++LogUtil.static -  Unable to instantiate Logger. message: "+e.getMessage()+"  [ERROR]\n");
            throw e;
        }
    }

    /**
     * Private constructor prevents inadvertent instantiation of this class which is really a collection
     * of functions (static methods).
     */
    private ServletLogUtil(){}

    /**
     * Initialize logging for the web application context in which the given
     * servlet is running. Two types of logging are supported:
     * </p>
     * 1) Regular logging using the SLF4J API.
     * 2) Performance logging which can write Apache common logging format logs,
     * use the LogUtil.logServerStartup(String) method.
     * <p/>
     * The log directory is determined by the servlet containers content
     * directory. The configuration of logging is controlled by the logback.xml
     * file.
     *
     * @param servlet - The servlet, used to determine the configuration
     *                  location.
     */
    public static void initLogging(HttpServlet servlet) {
        initLogging(servlet.getServletContext());
    }

    /**
     * Initialize logging for the web application context in which the given
     * servlet is running. Two types of logging are supported:
     * <p/>
     * 1) Regular logging using the SLF4J API.
     * 2) Performance logging which can write Apache common logging format logs,
     * use the LogUtil.logServerStartup(String) method.
     * </p>
     * The log directory is determined by the servlet containers content
     * directory. The configuration of logging is controlled by the logback.xml
     * file.
     *
     * @param sc The servlet context used, to determine the configuration
     *           location.
     */
    public static void initLogging(ServletContext sc) {
        initLock.lock();
        try {
            if(isLogInit.get()) {
                return;
            }
            // The config path could resolve to one of several places
            String configPath = ServletUtil.getConfigPath(sc);

            // Make sure the logger has a place to write.
            setupLoggingPath(configPath);

            // The default configuration is always in the distribution:
            // $CATALINA_HOME/webapps/$CONTEXT/WEB-INF/conf directory
            String defaultConfigPath = ServletUtil.getDefaultConfigPath(sc);

            // Find a logback(-test).xml config file in the config path.
            String logbackFile = locateLogbackFile(configPath);
            // Did ya find it?
            if (logbackFile == null) {
                // Nope. Better try the distribution files.
                logbackFile = locateLogbackFile(defaultConfigPath);
            }
            // Ingest the possibly found file (null tolerant)
            ingestLogbackFile(logbackFile);

            isLogInit.set(true);
        }
        finally {
            initLock.unlock();
        }
    }

    /**
     * Locates, if possible, the logback.xml or if that is missing the
     * logback-test.xml in the targetDir. If neither are found, null is
     * returned.
     *
     * @param targetDir The directory to search.
     * @return The logback file path, or null if not located.
     */
    private static String locateLogbackFile(String targetDir){
        String logbackTestConfig="logback-test.xml";
        String logbackConfig="logback.xml";
        String logbackFile = PathBuilder.pathConcat(targetDir,logbackConfig);
        File f = new File(logbackFile);
        if (!f.exists()) {
            log.info("Did not locate logback configuration: {}", logbackFile);
            logbackFile = PathBuilder.pathConcat(targetDir,logbackTestConfig);
            f = new File(logbackFile);
            if (!f.exists()) {
                log.info("Unable to locate logback configuration: {}", logbackFile);
                logbackFile = null;
            }
        }
        return logbackFile;
    }

    /**
     * Make sure we have a valid and operational logging directory as a child
     * directory of the current configuration path.
     *
     * @param currentConfigPath The current configuration path.
     */
    private static void setupLoggingPath(String currentConfigPath){
        // set up the log path
        String logPath = PathBuilder.pathConcat(currentConfigPath ,"logs");
        File logPathFile = new File(logPath);
        if (!logPathFile.exists()) {
            log.info("Creating log dir: {}", logPath);
            if (!logPathFile.mkdirs()) {
                String msg = "Creation of logfile directory failed." + logPath;
                log.error(msg);
                throw new RuntimeException(msg);
            }
        }
        log.info("Using log dir: {}", logPath);
        // read in by Log4J config file
        System.setProperty("logdir", logPath); // variable substitution

    }

    /**
     * Slurp up the logback file (if it's not null)
     *
     * @param logbackFile The name of the file to load.
     */
    private static void ingestLogbackFile(String logbackFile){
        if (logbackFile != null) {
            log.info("Logback configuration using: {}", logbackFile);
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();
            attemptJoranConfiguration(logbackFile, loggerContext);
            if(log.isInfoEnabled()){
                log.info("LoggerContext Follows: {}", logbackFile);
                StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
            }
        } else {
            log.error("Logback configuration using logback default configuration mechanism");
        }
    }

    /**
     * Tries to read and ingest the logback configuration file.
     * @param logbackConfigFile Path to logback configuration file.
     * @param loggerContext Logger context to condition.
     */
    private static void attemptJoranConfiguration(String logbackConfigFile, LoggerContext loggerContext){
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            // the context was probably already configured by default configuration
            // rules
            loggerContext.reset();
            configurator.doConfigure(logbackConfigFile);
            log.info("Configuration via {} successful.",configurator.getClass().getName());
        } catch (JoranException je) {
            log.error("Caught {} Message: {}",je.getClass().getName(),je.getMessage());
            StringWriter sw = new StringWriter();
            je.printStackTrace(new PrintWriter(sw));
            log.error("Stack trace: \n{}",sw);
        }
    }


    /**
     * Gather current thread information for inclusion in regular logging
     * messages. Call this method only for non-request servlet activities, e.g.,
     * during the init() or destroy().
     * <p/>
     * Use the SLF4J API to log a regular logging messages.
     * <p/>
     * This method gathers the following information:
     * 1) "ID" - an identifier for the current thread; and
     * 2) "startTime" - the system time in millis when this method is called.
     * <p/>
     * The appearance of the regular log messages are controlled in the
     * logback.xml configuration file.
     * @param source The source id of who started the logging. Typically an init()
     * method.
     *
     */
    public static void logServerStartup(String source) {
        // Setup context.
        synchronized (ServletLogUtil.class) {
            MDC.put("ID", "Server Startup");
            MDC.put("SOURCE", source);
        }
        MDC.put("startTime", System.currentTimeMillis() + "");
        log.info("Logging started.");
    }


    /**
     * Gather current thread information for inclusion in regular logging
     * messages. Call this method only for non-request servlet activities, e.g.,
     * during the init() or destroy().
     * <p/>
     * Use the SLF4J API to log a regular logging messages.
     * <p/>
     * This method gathers the following information:
     * 1) "ID" - an identifier for the current thread; and
     * 2) "startTime" - the system time in millis when this method is called.
     * <p/>
     * The appearance of the regular log messages are controlled in the
     * logback.xml configuration file.
     * @param source The source id of who started the logging. Typically an init()
     * method.
     *
     */
    public static void logServerShutdown(String source) {
        // Setup context.
        synchronized (ServletLogUtil.class) {
            MDC.put("ID", "Server Startup");
            MDC.put("SOURCE", source);
        }
        MDC.put("startTime", System.currentTimeMillis() + "");
        log.info("Logging started.");
    }



    /**
     * Gather information from the given HttpServletRequest for inclusion in both
     * regular logging messages and THREDDS access log messages. Call this method
     * at start of each doXXX() method (e.g., doGet(), doPut()) in any servlet
     * you implement.
     * <p/>
     * Use the SLF4J API to log a regular logging messages. Use the
     * logServerAccess() method to log a THREDDS access log message.
     * <p/>
     * This method gathers the following information:
     * 1) "ID" - an identifier for the current thread;
     * 2) "host" - the remote host (IP address or host name);
     * 3) "userid" - the reqID of the remote user;
     * 4) "startTime" - the system time in millis when this request is started (i.e., when this method is called); and
     * 5) "request" - The HTTP request, e.g., "GET /index.html HTTP/1.1".
     * <p/>
     * The appearance of the regular log messages and the THREDDS access log
     * messages are controlled in the logback.xml configuration file. For the log
     * messages to look like an Apache server "common" log message, use the
     * following pattern:
     * <p/>
     * "%X{host} %X{ident} %X{userid} [%d{dd/MMM/yyyy:HH:mm:ss}] %X{request} %m%n"
     *
     * @param req     the current HttpServletRequest.
     * @param logName Name of Logger to write stuff.
     * @param httpVerb The HTTP verb initiating the request - GET, POST, LastModifed, etc.
     * @param reqID The request ID, implemented as the request number.
     *
     */
    public static void logServerAccessStart(HttpServletRequest req, String logName,  String httpVerb, RequestId reqID) {

        HttpSession session = req.getSession(false);

        MDC.put(REQUEST_ID_KEY, reqID.logId());
        MDC.put(HTTP_VERB_KEY, httpVerb);
        MDC.put(CLIENT_HOST_KEY, req.getRemoteHost());
        MDC.put(SESSION_ID_KEY, (session == null) ? "-" : session.getId());

        String uid = opendap.auth.Util.getUID(req);
        MDC.put(USER_ID_KEY, uid==null ? "-" : uid  );

        MDC.put(START_TIME_KEY, Long.toString(System.currentTimeMillis()));

        String userAgent = Scrub.simpleString(req.getHeader("User-Agent"));
        MDC.put(USER_AGENT_KEY,  userAgent==null?"-":userAgent);

        String resourceID =  Scrub.urlContent(req.getRequestURI());
        MDC.put(RESOURCE_ID_KEY,resourceID);

        String query = Scrub.simpleQueryString(req.getQueryString());
        query = (query == null || query.isEmpty()) ? "-" : query;
        MDC.put(QUERY_STRING_KEY, query);

        if(log.isInfoEnabled()) {
            String startMsg = "REQUEST START - " +
                    "RemoteHost: '" + LogUtil.scrubEntry(req.getRemoteHost()) + "' " +
                    "RequestedResource: '" + resourceID + "' " +
                    "QueryString: '" + query + "' " +
                    "AccessLog: " + logName;
            log.info(startMsg);
        }
        if(logName.equals(HYRAX_ACCESS_LOG_ID) && useDualCloudWatchLogs.get()){
            Logger cwRequestLog = org.slf4j.LoggerFactory.getLogger(CLOUDWATCH_REQUEST_LOG);
            cwRequestLog.info("");
        }
    }

    /**
     *
     *  Based on the HyraxAccess.log pattern:
     *  <pattern>
     *  [%X{host}]
     *  [%X{UserAgent}]
     *  [%X{ident}]
     *  [%X{userid}]
     *  [%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z}]
     *  [%8X{duration}]
     *  [%X{http_status}]
     *  [%8X{ID}]
     *  [%X{SOURCE}]
     *  [%X{resourceID}]
     *  [%X{query}]
     *  [%X{size}]%n
     *  </pattern>
     *  Example:
     *  [0:0:0:0:0:0:0:1] [curl/7.54.0] [-] [-] [2019-09-16T14:20:58.028 -0700] [       4] [200] [      25]
     *  [LastModified] [/opendap/hyrax/data/nc/fnoc1.nc.dmr] [dap4.ce=lat] []
     *
     * @return The BESlog formatted log line for this request
     */
    public static String getLogEntryForBesLog(){


        StringBuilder alb = new StringBuilder();
        if(useCombinedLog.get()) {
            String sep = "|&|";

            alb.append(MDC.get(CLIENT_HOST_KEY)).append(sep);
            alb.append(MDC.get(USER_AGENT_KEY)).append(sep);
            alb.append(MDC.get(SESSION_ID_KEY)).append(sep);
            alb.append(MDC.get(USER_ID_KEY)).append(sep);
            alb.append(MDC.get(START_TIME_KEY)).append(sep);
            alb.append(MDC.get(REQUEST_ID_KEY)).append(sep);
            alb.append(MDC.get(HTTP_VERB_KEY)).append(sep);
            alb.append(MDC.get(RESOURCE_ID_KEY)).append(sep);


            String ce = MDC.get(QUERY_STRING_KEY);
            if(ce == null || ce.isEmpty()) {
                ce = "-";
            }

            alb.append(ce);
        }

        return alb.toString();
    }


    /**
     * Used in various places in the server to add the response size to the log.
     * Will set the response if it has not already been set or if it's value is empty.
     * @param size The size, in bytes, of the response. A value of -1 indicates that the size is unknown
     */
    public static void setResponseSize(long size){

        String mdc_size = MDC.get(RESPONSE_SIZE_KEY);
        // Was it set? Is it empty??
        if(mdc_size==null || mdc_size.isEmpty())
            MDC.put(RESPONSE_SIZE_KEY, Long.toString(size));
    }


    /**
     * Write log entry to named log.
     *
     * @param httpStatus        - the result code for this request.
     * @param logName the name of the Logger to which to write stuff.
     */
    public static void logServerAccessEnd(int httpStatus, String logName) {
        logServerAccessEnd(httpStatus, MISSING_SIZE_VALUE, logName);
    }

    /**
     * Write log entry to named log.
     *
     * @param httpStatus The HTTP status code for this request.
     * @param size The size of the response.
     * @param logName the name of the Logger to which to write stuff.
     */
    public static void logServerAccessEnd(int httpStatus, int size , String logName) {

        long endTime = System.currentTimeMillis();
        long  duration = -1;
        String sTime = MDC.get("startTime");
        if(sTime!=null) {
            long startTime = Long.parseLong(sTime);
            duration = endTime - startTime;
        }
        MDC.put(DURATION_KEY, (duration>=0)?Long.toString(duration):"unknown" +" ms");

        setResponseSize(size);

        MDC.put(HTTP_STATUS_KEY, Integer.toString(httpStatus));

        // Doesn't matter what we write to the access_log because the access log formatter ignores
        // it in lieu of the stuff in MDC. All that matters is that we write something.
        Logger access_log = org.slf4j.LoggerFactory.getLogger(logName);
        access_log.info("");

        if(logName.equals(HYRAX_ACCESS_LOG_ID) && useDualCloudWatchLogs.get()){
            Logger cwResponseLog = org.slf4j.LoggerFactory.getLogger(CLOUDWATCH_RESPONSE_LOG);
            cwResponseLog.info("");
        }
        log.info("REQUEST COMPLETE - http_status: " + MDC.get(HTTP_STATUS_KEY) + " duration: "+ MDC.get(DURATION_KEY) + "  size: "+MDC.get(RESPONSE_SIZE_KEY));

        cleanupMDC();
    }

    /**
     * This method cleans up the MDC so nothing is left "set" for the next request handled by the current thread.
     * From the LogBack manual ( https://logback.qos.ch/manual/mdc.html ):
     * "Normally, a put() operation should be balanced by the corresponding remove() operation. Otherwise, the
     *  MDC will contain stale values for certain keys."
     * Of note is the fact that they do not recommend using MDC.clear() to do this. I think because MDC.clear() wipes
     * the MDC for all threads while MDC.remove() drops a set value for the current thread.
     */
    public static void cleanupMDC(){

        // -- -- -- -- -- -- -- -- -- -- -- -- -- --
        //
        // These were set in logServerAccessStart()
        //
        MDC.remove(REQUEST_ID_KEY);
        MDC.remove(HTTP_VERB_KEY);
        MDC.remove(CLIENT_HOST_KEY);
        MDC.remove(SESSION_ID_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(START_TIME_KEY);

        MDC.remove(USER_AGENT_KEY);
        MDC.remove(RESOURCE_ID_KEY);

        MDC.remove(QUERY_STRING_KEY);

        // -- -- -- -- -- -- -- -- -- -- -- -- -- --
        //
        // These were set in logServerAccessEnd()
        //
        MDC.remove(DURATION_KEY);
        MDC.remove(RESPONSE_SIZE_KEY);
        MDC.remove(HTTP_STATUS_KEY);

    }


    public static void useCombinedLog(boolean value) {
        useCombinedLog.set(value);
        log.info("Combined OLFS/BES Log Is {}", value ? "ENABLED." : "DISABLED");
    }

    public static void useDualCloudWatchLogs(boolean value) {
        useDualCloudWatchLogs.set(value);
        log.info("CloudWatch Logs Are {}", value ? "ENABLED." : "DISABLED");
    }

}
