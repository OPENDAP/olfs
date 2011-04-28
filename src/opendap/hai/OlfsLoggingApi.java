package opendap.hai;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.Appender;
import opendap.bes.Version;
import opendap.coreServlet.HttpResponder;
import opendap.coreServlet.MimeBoundary;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ServletUtil;
import opendap.gateway.BesGatewayApi;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;


import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.html.HTMLLayout;
import ch.qos.logback.classic.html.UrlCssBuilder;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.read.CyclicBufferAppender;


/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Nov 12, 2010
 * Time: 2:35:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class OlfsLoggingApi extends HttpResponder {


    private Logger log;

    private static String defaultRegex = ".*\\/olfsLog";


    private String AdminLogger = "HAI_DEBUG_LOGGER";

    // LoggerContext.ROOT_NAME = "root"
    private String ROOT_NAME = "ROOT";
    private SimpleDateFormat sdf;


    private CyclicBufferAppender cyclicBufferAppender;

    public void init() {
        log = LoggerFactory.getLogger(getClass());
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS Z");

        String msg = "";

        for (ch.qos.logback.classic.Logger logger : lc.getLoggerList()) {
            msg += "   Logger: " + logger.getName() + "\n";

            Iterator<Appender<ILoggingEvent>> i = logger.iteratorForAppenders();
            while (i.hasNext()) {
                Appender<ILoggingEvent> a = i.next();
                msg += "        Appender: " + a.getName() + "\n";

            }


        }
        log.debug("Initializing ViewLastLog Servlet. \n" + msg);

        ch.qos.logback.classic.Logger rootLogger = lc.getLogger(ROOT_NAME);

        cyclicBufferAppender = (CyclicBufferAppender) rootLogger.getAppender(AdminLogger);

    }


    public OlfsLoggingApi(String sysPath) {
        super(sysPath, null, defaultRegex);
        init();
    }

    public OlfsLoggingApi(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        init();
    }

    public void respondToHttpRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String queryString = request.getQueryString();

        log.debug("queryString: "+queryString);
        if(queryString!=null){
            // @todo Make this accept control parameters:
            // @todo   - set buffer length
            // @todo   - change debugging params
        }

        showLog(request, response);
    }


    private void showLog(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {


        //log.debug("Sending logging info");
        resp.setContentType("text/html");
        PrintWriter output = resp.getWriter();

        String localRef = req.getContextPath();
        output.append("<a href=\"#bottom\">Jump to bottom</a>");
        output.append("<hr/>");
        output.append("<pre>");



        printLogs(output);


        output.append("<a name=\"bottom\" />");

        output.append("</pre>");
        output.append("<hr/>");




        output.flush();
        output.close();
    }

    private void printLogs(PrintWriter output) {
        int count = -1;
        if (cyclicBufferAppender != null) {
            count = cyclicBufferAppender.getLength();
        }

        if (count == -1) {
            output.append("<h3>Failed to locate CyclicBuffer</h3>\r\n");
        } else if (count == 0) {
            output.append("<h3><td>No logging events to display</h3>\r\n");
        } else {
            LoggingEvent le;
            for (int i = 0; i < count; i++) {
                le = (LoggingEvent) cyclicBufferAppender.get(i);
                output.append(StringEscapeUtils.escapeHtml(formatLoggingEvent(le)));
            }
        }
    }


    private String formatLoggingEvent(LoggingEvent event){


        //private String PATTERN = "%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z} [thread:%t] [%r][%X{ID}] [%X{SOURCE}]   %-5p - %c - %m%n";

        StringBuffer sbuf = new StringBuffer(128);
        Date date = new Date(event.getTimeStamp());


        sbuf.append(sdf.format(date));
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


    private void initialize(LoggerContext context) {




    }

}
