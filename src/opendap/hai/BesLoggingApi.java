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
import java.util.HashMap;
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
public class BesLoggingApi extends HttpResponder {


    private Logger log;

    private static String defaultRegex = ".*\\/besLog";


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


    }


    public BesLoggingApi(String sysPath) {
        super(sysPath, null, defaultRegex);
        init();
    }

    public BesLoggingApi(String sysPath, String pathPrefix) {
        super(sysPath, pathPrefix, defaultRegex);
        init();
    }

    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String queryString = request.getQueryString();

        log.debug("queryString: "+queryString);
        HashMap<String,String> kvp = Util.processQuery(request);

        showLog(request, response, kvp);
    }


    private void showLog(HttpServletRequest req, HttpServletResponse resp, HashMap<String,String> kvp)
            throws ServletException, IOException {




        String lines=kvp.get("lines");

        if(lines == null)
            lines = "500";




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
    }




}
