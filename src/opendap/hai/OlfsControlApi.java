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
package opendap.hai;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.CyclicBufferAppender;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.ResourceInfo;
import opendap.coreServlet.Scrub;
import opendap.logging.LogUtil;
import opendap.logging.ServletLogUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Nov 12, 2010
 * Time: 2:35:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class OlfsControlApi extends HttpResponder {


    private org.slf4j.Logger log;

    private static String defaultRegex = ".*\\/olfsctl";


    private String AdminLogger = "HAI_DEBUG_LOGGER";

    // LoggerContext.ROOT_NAME = "root"
    private String ROOT_NAME = "ROOT";


    private CyclicBufferAppender _cyclicBufferAppender;

    public void init() {
        log = LoggerFactory.getLogger(getClass());
        _cyclicBufferAppender = null;

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        if(log.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder();

            for (ch.qos.logback.classic.Logger logger : lc.getLoggerList()) {
                msg.append("   Logger: ").append(logger.getName()).append("\n");
                Iterator<Appender<ILoggingEvent>> i = logger.iteratorForAppenders();
                while (i.hasNext()) {
                    Appender<ILoggingEvent> a = i.next();
                    msg.append("        Appender: ").append(a.getName()).append("\n");
                }
            }
            log.debug("Initializing ViewLastLog Servlet. \n" + msg);
        }
    }

    private CyclicBufferAppender getCyclicBufferAppender(){
        if(_cyclicBufferAppender!=null)
            return _cyclicBufferAppender;
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = lc.getLogger(ROOT_NAME);
        _cyclicBufferAppender = (CyclicBufferAppender) rootLogger.getAppender(AdminLogger);
        return _cyclicBufferAppender;
    }


    public OlfsControlApi(String sysPath) {
        super(sysPath, null, defaultRegex);
        init();
    }


    @Override
    public ResourceInfo getResourceInfo(String resourceName) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getLastModified(HttpServletRequest request) throws Exception {
        return new Date().getTime();
    }

    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {



        HashMap<String,String> kvp = Util.processQuery(request);

        response.getWriter().print(processOlfsCommand(kvp));

    }



    private String getOlfsLog(String lines) {
        StringBuilder logContent = new StringBuilder();
        int count = -1;

        CyclicBufferAppender cyclicBufferAppender = getCyclicBufferAppender();

        if(lines!=null){
            try {
                int maxLines = Integer.parseInt(lines);
                if(maxLines>0 && maxLines<20000)
                    cyclicBufferAppender.setMaxSize(maxLines);
            }
            catch(NumberFormatException e){
                log.error("Failed to parse the value of the parameter 'lines': {}",Scrub.integerString(lines));
            }
        }

        count = cyclicBufferAppender.getLength();
        if (count == -1) {
            logContent.append("Failed to locate CyclicBuffer content!\n");
        } else if (count == 0) {
            logContent.append("No logging events to display.\n");
        } else {
            LoggingEvent le;
            for (int i = 0; i < count ; i++) {
                le = (LoggingEvent) cyclicBufferAppender.get(i);
                logContent.append(formatLoggingEvent(le));
            }
        }
        return logContent.toString();
    }


    private String formatLoggingEvent(LoggingEvent event){


        //private String PATTERN = "%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z} [thread:%t] [%r][%X{ID}] [%X{SOURCE}]   %-5p - %c - %m%n";

        StringBuffer sbuf = new StringBuffer(128);
        Date date = new Date(event.getTimeStamp());

        SimpleDateFormat simpleDateFormat;
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");


        sbuf.append(simpleDateFormat.format(date));
        sbuf.append(" ");
        sbuf.append(" [").append(event.getThreadName()).append("] ");
        sbuf.append(event.getLevel());
        sbuf.append(" - ");
        sbuf.append(event.getLoggerName());
        sbuf.append(" - ");
        sbuf.append(event.getFormattedMessage());
        sbuf.append("\n");

        IThrowableProxy itp = event.getThrowableProxy();
        if(itp!=null){
            for(StackTraceElementProxy ste  : itp.getStackTraceElementProxyArray()){
                sbuf.append("    ").append(ste);
                sbuf.append("\n");
            }
        }
        return sbuf.toString();

    }


    public String getLogLevel(String loggerName){

        StringBuilder sb = new StringBuilder();


        if(loggerName != null){
            Logger namedLog = (Logger) LoggerFactory.getLogger(loggerName);

            Level level = namedLog.getLevel();

            String levelStr = "off";
            if(level!=null)
                levelStr = level.toString().toLowerCase();

            sb.append(levelStr);
        }

        return sb.toString();

    }




    private enum olfsCmds {
        cmd, getLog, lines, getLogLevel, setLogLevel, logger, level
    }



    /**
     *
     * @param kvp
     * @return
     */
    public String processOlfsCommand(HashMap<String, String> kvp) {

        String loggerName, logLevel;
        StringBuilder sb = new StringBuilder();

        String olfsCmd = kvp.get(olfsCmds.cmd.toString());


        if ( olfsCmd != null) {
            switch(olfsCmds.valueOf(olfsCmd)){

                case getLog :
                    String lines = kvp.get(olfsCmds.lines.toString());

                    if(lines!=null)
                        lines = Scrub.integerString(lines);

                    String log =  getOlfsLog(lines);
                    log = StringEscapeUtils.escapeXml(log);
                    sb.append(log);
                    break;


                case getLogLevel:
                    loggerName = getValidLoggerName(kvp.get(olfsCmds.logger.toString()));

                    sb.append(getLogLevel(loggerName));
                    break;


                case setLogLevel:
                    logLevel = kvp.get(olfsCmds.level.toString());
                    loggerName = getValidLoggerName(kvp.get(olfsCmds.logger.toString()));

                    if(loggerName!=null  && logLevel!=null){
                        sb.append(LogUtil.setLogLevel(loggerName, logLevel));
                    }
                    else {
                        sb.append("Unable to set log level. ");
                        sb.append("LoggerName: ").append(Scrub.urlContent(loggerName)).append(" ");
                        sb.append(" LogLevel: ").append(Scrub.simpleString(logLevel)).append(" ");
                    }

                    break;


                default:
                    sb.append(" Unrecognized OLFS command: ").append(Scrub.simpleString(olfsCmd));
                    break;

            }

        }
        else {

            sb.append(" Waiting for you to do something...");
        }


        return sb.toString();


    }






    private static String javaClassNameInclusionRegex = "([a-zA-Z_$][a-zA-Z\\d_$]*\\.)*[a-zA-Z_$][a-zA-Z\\d_$]*";
    private static Pattern javaClassNameInclusionPattern = Pattern.compile(javaClassNameInclusionRegex);

    private boolean isJavaClassName(String loggerName) {
        Matcher m = javaClassNameInclusionPattern.matcher(loggerName);
        return m.matches();
    }


    private String getValidLoggerName(String loggerName){

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        for (ch.qos.logback.classic.Logger logger : lc.getLoggerList()) {
            if(logger.getName().equals(loggerName))
                return logger.getName();
        }



        return null;
    }




}
