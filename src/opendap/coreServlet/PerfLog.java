/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
//
// Copyright (c) 2006 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.coreServlet;

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.log4j.MDC;
import org.slf4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.xml.parsers.FactoryConfigurationError;
import java.io.File;

import thredds.servlet.ServletUtil;

/**
 * User: ndp
 * Date: Feb 6, 2007
 * Time: 3:34:35 PM
 */
public class PerfLog {

    private static boolean isLogInit = false;
    private static volatile long logID = 0;

    private static Logger log;

    /**
     * Initialize logging for the web application context in which the given
     * servlet is running. Two types of logging are supported:
     * <p/>
     * 1) Regular logging using the SLF4J API.
     * 2) Performance logging which can write Apache common logging format logs,
     * use the PerfLog.logServerSetup(String) method.
     * <p/>
     * The log directory is determined by the servlet containers content
     * directory. The configuration of logging is controlled by the log4j.xml
     * file.
     *
     * @param servlet - the servlet.
     */
    public static void initLogging(HttpServlet servlet) {
        // Initialize logging if not already done.
        if (isLogInit)
            return;

        System.out.println("+++PerfLog.initLogging()");
        ServletContext servletContext = servlet.getServletContext();

        // set up the log path
        String logPath = ServletUtil.getContentPath(servlet) + "logs";
        File logPathFile = new File(logPath);
        if (!logPathFile.exists()) {
            if (!logPathFile.mkdirs()) {
                throw new RuntimeException("Creation of logfile directory failed." + logPath);
            }
        }

        // read in Log4J config file
        System.setProperty("logdir", logPath); // variable substitution
        try {
            String log4Jconfig = servletContext.getInitParameter("log4j-init-file");
            if (log4Jconfig == null){
                log4Jconfig = ServletUtil.getContentPath(servlet) + "log4j.xml";
                File f = new File(log4Jconfig);
                if (!f.exists()) {
                    log4Jconfig = ServletUtil.getRootPath(servlet) + "WEB-INF/log4j.xml";
                }
            }
            System.out.println("+++PerfLog.initLogging() - Log4j configuration using: "+log4Jconfig);
            DOMConfigurator.configure(log4Jconfig);
            System.out.println("+++PerfLog.initLogging() - Log4j configured.");
        } catch (FactoryConfigurationError t) {
            t.printStackTrace();
        }

        log = org.slf4j.LoggerFactory.getLogger(PerfLog.class);

        isLogInit = true;
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
     *
     * @param msg - the information log message logged when this method finishes.
     */
    public static void logServerSetup(String msg) {
        // Setup context.
        synchronized (PerfLog.class) {
            MDC.put("ID", Long.toString(++logID));
        }
        MDC.put("startTime", System.currentTimeMillis());
        log.info(msg);
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
     * 3) "userid" - the id of the remote user;
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
    public static void logServerAccessStart(HttpServletRequest req, String logName) {

        HttpSession session = req.getSession(false);

        Logger log = org.slf4j.LoggerFactory.getLogger(logName);

        // Setup context.
        synchronized (PerfLog.class) {
            MDC.put("ID", Long.toString(++logID));
        }
        MDC.put("host", req.getRemoteHost());
        MDC.put("ident", (session == null) ? "-" : session.getId());
        MDC.put("userid", req.getRemoteUser() != null ? req.getRemoteUser() : "-");
        MDC.put("startTime", System.currentTimeMillis());
        String query = req.getQueryString();
        query = (query != null) ? "?" + query : "";
        StringBuffer request = new StringBuffer();
        request.append("\"").append(req.getMethod()).append(" ")
                .append(req.getRequestURI()).append(query)
                .append(" ").append(req.getProtocol()).append("\"");

        MDC.put("request", request.toString());


        log.info("Remote host: " + req.getRemoteHost() + " - Request: " + request);
    }




    /**
     * Write log entry to named log.
     *
     * @param resCode        - the result code for this request.
     * @param resSizeInBytes - the number of bytes returned in this result, -1 if unknown.
     * @param logName the name of the Logger to which to write stuff.
     */
    public static void logServerAccessEnd(int resCode,
                                          long resSizeInBytes,
                                          String logName) {

        long endTime = System.currentTimeMillis();
        long startTime = (Long) MDC.get("startTime");
        long duration = endTime - startTime;


        Logger log = org.slf4j.LoggerFactory.getLogger(logName);


        log.info(   "Request Completed - [" +
                    resCode +
                    "] [" +
                    resSizeInBytes +
                    "] [" +
                    duration +
                    "]");
    }

}
