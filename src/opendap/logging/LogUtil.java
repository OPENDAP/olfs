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
public class LogUtil {

    public static final String HYRAX_ACCESS_LOG_ID = "HyraxAccess";
    public static final String HYRAX_LAST_MODIFIED_ACCESS_LOG_ID = "HyraxLastModifiedAccess";

    public static final String DOCS_ACCESS_LOG_ID = "DocsAccess";

    public static final String SITEMAP_ACCESS_LOG_ID = "SiteMapAccess";

    public static final String PDP_SERVICE_ACCESS_LOG_ID = "PDPServiceAccess";
    public static final String PDP_SERVICE_LAST_MODIFIED_LOG_ID = "PDPServiceLastModifiedAccess";

    public static final String GATEWAY_ACCESS_LOG_ID = "HyraxGatewayAccess";
    public static final String GATEWAY_ACCESS_LAST_MODIFIED_LOG_ID = "HyraxGatewayLastModifiedAccess";

    public static final String ADMIN_ACCESS_LOG_ID = "HyraxAdminAccess";
    public static final String ADMIN_ACCESS_LAST_MODIFIED_LOG_ID = "HyraxAdminLastModifiedAccess";

    public static final String S3_SERVICE_ACCESS_LOG_ID = "S3ServiceAccess";
    public static final String S3_SERVICE_LAST_MODIFIED_LOG_ID = "S3ServiceLastModifiedAccess";

    public static final String WCS_ACCESS_LOG_ID = "WCSAccess";
    public static final String WCS_LAST_MODIFIED_ACCESS_LOG_ID = "WCSLastModifiedAccess";

    private static final String ID_KEY = "ID";
    private static final String SOURCE_KEY = "SOURCE";
    private static final String HOST_KEY = "host";
    private static final String IDENT_KEY = "ident";
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

    private static Logger log;
    static{
        System.out.print("+++LogUtil.static - Instantiating Logger ... \n");

        try {
            log = org.slf4j.LoggerFactory.getLogger(LogUtil.class);
            log.info("Logger instantiated. class: {}",log.getClass().getCanonicalName());
        }
        catch(NoClassDefFoundError e) {
            System.err.println("\n\n[ERROR]  +++LogUtil.initLogging() -  Unable to instantiate Logger. java.lang.NoClassDefFoundError: "+e.getMessage()+"  [ERROR]\n");
            throw e;
        }
    }

    /**
     * Private constructor prevents inadvertent instantiation of this class which is really a collection
     * of functions (static methods).
     */
    private LogUtil(){}

    /**
     * Initialize logging for the web application context in which the given
     * servlet is running. Two types of logging are supported:
     * <p/>
     * 1) Regular logging using the SLF4J API.
     * 2) Performance logging which can write Apache common logging format logs,
     * use the LogUtil.logServerStartup(String) method.
     * <p/>
     * The log directory is determined by the servlet containers content
     * directory. The configuration of logging is controlled by the log4j.xml
     * file.
     *
     * @param servlet - the servlet.
     */
    public static void initLogging(HttpServlet servlet) {

        initLock.lock();
        try {
            // Initialize logging if not already done.
            if (isLogInit.get())
                return;

            log.info("BEGIN servlet name: {}  class: {}", servlet.getServletName(), servlet.getClass().getCanonicalName());
            ServletContext servletContext = servlet.getServletContext();

            String configPath = ServletUtil.getConfigPath(servlet);

            // set up the log path
            String logPath = configPath + "logs";
            File logPathFile = new File(logPath);
            if (!logPathFile.exists() && !logPathFile.mkdirs()) {
                throw new RuntimeException("Creation of logfile directory failed." + logPath);
            }


            // read in Logback config file
            System.setProperty("logdir", logPath); // variable substitution

            String logbackConfig = servletContext.getInitParameter("logbackConfig");
            if (logbackConfig == null) {
                logbackConfig = configPath + "logback-test.xml";
                File f = new File(logbackConfig);
                if (!f.exists()) {
                    logbackConfig = configPath + "logback.xml";
                    f = new File(logbackConfig);
                    if (!f.exists()) {
                        // Try to use the one that shipped with the webapp
                        String defaultLogbackConfig = ServletUtil.getSystemPath(servlet, "WEB-INF/logback.xml");
                        f = new File(defaultLogbackConfig);
                        if (!f.exists())
                            logbackConfig = null;
                        else
                            logbackConfig = defaultLogbackConfig;
                    }

                }
            }

            if (logbackConfig != null) {
                log.info("Logback configuration using: {}", logbackConfig);
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                attemptJoranConfiguration(logbackConfig, lc);
                StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
            } else {
                log.info("Logback configuration using logback's default configuration mechanism");
            }

            isLogInit.set(true);
            log.info("END Logback is configured.");
        }
        finally {
            initLock.unlock();
        }
    }


    /**
     * Initialize logging for the web application context in which the given
     * servlet is running. Two types of logging are supported:
     * <p/>
     * 1) Regular logging using the SLF4J API.
     * 2) Performance logging which can write Apache common logging format logs,
     * use the LogUtil.logServerStartup(String) method.
     * <p/>
     * The log directory is determined by the servlet containers content
     * directory. The configuration of logging is controlled by the log4j.xml
     * file.
     *
     * @param path - the path to the log4j.xml file
     */
    public static void initLogging(String path) {

        initLock.lock();
        try {
            // Initialize logging if not already done.
            if (isLogInit.get())
                return;

            log.info("BEGIN path='{}'", path);

            // set up the log path
            if (!path.endsWith("/"))
                path += "/";
            String logPath = path + "logs";
            File logPathFile = new File(logPath);
            if (!logPathFile.exists()) {
                log.info("Creating log dir: {}", logPath);
                if (!logPathFile.mkdirs()) {
                    throw new RuntimeException("Creation of logfile directory failed." + logPath);
                }
            } else {
                log.info("Found log dir: {} ", logPath);

            }

            log.info("Using log dir: {}", logPath);

            // read in Log4J config file
            System.setProperty("logdir", logPath); // variable substitution

            String logbackConfig = path + "logback-test.xml";
            File f = new File(logbackConfig);
            if (!f.exists()) {
                log.info("Unable to locate logback configuration: {}", logbackConfig);
                logbackConfig = path + "logback.xml";
                f = new File(logbackConfig);
                if (!f.exists()) {
                    log.info("Unable to locate logback configuration: {}", logbackConfig);
                    logbackConfig = null;
                }

            }

            if (logbackConfig != null) {
                log.info("Logback configuration using: {}", logbackConfig);
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                attemptJoranConfiguration(logbackConfig, lc);
                StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

            } else {
                log.info("Logback configuration using logback default configuration mechanism");
            }

            isLogInit.set(true);
            log.info("END Logback is configured.");
        }
        finally {
            initLock.unlock();
        }
    }



    /**
     *
     * @param logbackConfig Path to logback configuration file.
     * @param lc Logger context to condition.
     */
    private static void attemptJoranConfiguration(String logbackConfig, LoggerContext lc){
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(lc);
            // the context was probably already configured by default configuration
            // rules
            lc.reset();
            configurator.doConfigure(logbackConfig);
            log.info("Configuration via {} successful.",configurator.getClass().getName());
        } catch (JoranException je) {
            log.error("Caught {} Messge: ",je.getClass().getName(),je.getMessage());
            StringWriter sw = new StringWriter();
            je.printStackTrace(new PrintWriter(sw));
            log.error("Stack trace: \n{}",sw.toString());
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
     * log4j.xml configuration file.
     * @param source The source id of who started the logging. Typically an init()
     * method.
     *
     */
    public static void logServerStartup(String source) {
        // Setup context.
        synchronized (LogUtil.class) {
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
     * log4j.xml configuration file.
     * @param source The source id of who started the logging. Typically an init()
     * method.
     *
     */
    public static void logServerShutdown(String source) {
        // Setup context.
        synchronized (LogUtil.class) {
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
     * messages are controlled in the log4j.xml configuration file. For the log
     * messages to look like an Apache server "common" log message, use the
     * following log4j pattern:
     * <p/>
     * "%X{host} %X{ident} %X{userid} [%d{dd/MMM/yyyy:HH:mm:ss}] %X{request} %m%n"
     *
     * @param req     the current HttpServletRequest.
     * @param logName Name of Logger to write stuff.
     */
    public static void logServerAccessStart(HttpServletRequest req, String logName,  String reqSource, String reqID) {

        HttpSession session = req.getSession(false);


        MDC.put(ID_KEY, reqID);
        MDC.put(SOURCE_KEY, reqSource);
        MDC.put(HOST_KEY, req.getRemoteHost());
        MDC.put(IDENT_KEY, (session == null) ? "-" : session.getId());
        MDC.put(USER_ID_KEY, req.getRemoteUser() == null ? "-" : req.getRemoteUser() );
        MDC.put(START_TIME_KEY, System.currentTimeMillis() + "");

        String userAgent = req.getHeader("User-Agent");
        MDC.put(USER_AGENT_KEY,  userAgent==null?"-":userAgent);

        String resourceID =  req.getRequestURI();
        MDC.put(RESOURCE_ID_KEY,resourceID);

        String query = req.getQueryString();
        query = (query == null) ? "" : query;
        MDC.put(QUERY_STRING_KEY, query);

        if(log.isInfoEnabled()) {
            StringBuilder startMsg = new StringBuilder();
            startMsg.append("REQUEST START - ");
            startMsg.append("RemoteHost: '").append(LogUtil.scrubEntry(req.getRemoteHost())).append("' ");
            startMsg.append("RequestedResource: '").append(resourceID).append("' ");
            startMsg.append("QueryString: '").append(query).append("' ");
            startMsg.append("AccessLog: ").append(logName);
            log.info(startMsg.toString());
        }
    }


    /**
     * Used in various places in the server to add the response size to the log.
     * @param size The size, in bytes, of the response. Values less than 0 will be ignored.
     */
    public static void setResponseSize(long size){
        // Only set the size if it's not equal to the missing value (-1)
        if(size>=0) {
            MDC.put(RESPONSE_SIZE_KEY, Long.toString(size) + " bytes");
        }
    }


    /**
     * Write log entry to named log.
     *
     * @param httpStatus        - the result code for this request.
     * @param logName the name of the Logger to which to write stuff.
     */
    public static void logServerAccessEnd(int httpStatus, String logName) {
        logServerAccessEnd(httpStatus, -1, logName);
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
            long startTime = Long.valueOf(sTime);
            duration = endTime - startTime;
        }
        MDC.put(DURATION_KEY, (duration>=0)?Long.toString(duration):"unknown" +" ms");

        setResponseSize(size);

        MDC.put(HTTP_STATUS_KEY, Integer.toString(httpStatus));

        // Doesn't matter what we write to the access_log because the access log formatter ignores
        // it in lieu of the stuff in MDC. All that matters is that we write something.
        Logger access_log = org.slf4j.LoggerFactory.getLogger(logName);
        access_log.info("");

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
    private static void cleanupMDC(){

        // -- -- -- -- -- -- -- -- -- -- -- -- -- --
        //
        // These were set in logServerAccessStart()
        //
        MDC.remove(ID_KEY);
        MDC.remove(SOURCE_KEY);
        MDC.remove(HOST_KEY);
        MDC.remove(IDENT_KEY);
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

    /**
     * https://affinity-it-security.com/how-to-prevent-log-injection/
     * @param s String to prep for log.
     * @return String ready for log.
     */
    public static String scrubEntry(String s){
        char[] disallowedChars = {'\r','\n', 0x08, '<', '>', '&', '\"', '\''} ;
        // Grind out a char by char replacement.
        for(char badChar: disallowedChars){
            s = s.replace(badChar,'_');
        }
        return s;
    }



}
