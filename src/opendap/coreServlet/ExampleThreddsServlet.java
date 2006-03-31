// $Id: ExampleThreddsServlet.java,v 1.1 2006/03/07 23:45:33 edavis Exp $
package opendap.coreServlet;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.servlet.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.util.*;

import org.apache.log4j.*;

/**
 * _more_
 *
 * @author edavis
 * @since Feb 15, 2006 2:52:54 PM
 */
public class ExampleThreddsServlet extends HttpServlet {
    protected org.slf4j.Logger log;
    protected String rootPath;    // Path to location war file was unpacked.
    protected String contentPath; // Path to ${TOMCAT_HOME}/content/thredds
    // @todo Is this allowed in all servlet engines?

    protected CatalogRootHandler catHandler;

    protected String getPath() {
        return "";
    }

    protected String getContextPath() {
        return "/opendap";
    }

    protected String getContextName() {
        return "Example THREDDS Server";
    }

    protected String getVersion() {
        return "ETS version 0.1";
    }

    protected String getDocsPath() {
        return "docs/";
    }

    public void init() throws javax.servlet.ServletException {
        ServletUtil.initDebugging(this); // read debug flags
        rootPath = ServletUtil.getRootPath(this);
        contentPath = ServletUtil.getContentPath(this) + getPath();

        // init logging
        ServletUtil.initLogging(this);
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        ServletUtil.logServerSetup(this.getClass().getName() + ".init()");

        log.info("servlet context name= " + this.getServletContext().getServletContextName());
        log.info("rootPath= " + rootPath);
        log.info("contentPath= " + contentPath);

        // first time, create content directory
        String initialContentPath = ServletUtil.getInitialContentPath(this) + getPath();
        File initialContentFile = new File(initialContentPath);
        if (initialContentFile.exists()) {
            try {
                if (ServletUtil.copyDir(initialContentPath, contentPath)) {
                    log.info("copyDir " + initialContentPath + " to " + contentPath);
                }
            }
            catch (IOException ioe) {
                log.error("failed to copyDir " + initialContentPath + " to " + contentPath, ioe);
            }
        }

        // handles all catalogs, including ones with DatasetScan elements, ie dynamic
        CatalogRootHandler.init(contentPath, this.getContextPath());
        catHandler = CatalogRootHandler.getInstance();
        try {
            catHandler.initCatalog("catalog.xml");
            catHandler.initCatalog("extraCatalog.xml");
        }
        catch (Throwable e) {
            log.error("Error initializing catalog: " + e.getMessage(), e);
        }

        this.makeDebugActions();
        catHandler.makeDebugActions();
        DatasetHandler.makeDebugActions();

        HtmlWriter2.init(this.getContextPath(), this.getContextName(),
                this.getVersion(), this.getDocsPath());

        log.info("--- initialized " + getClass().getName());
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // Setup logging for this request.
        ServletUtil.logServerAccessSetup(req);

        // Get the request path.
        String path = ServletUtil.getRequestPath(req);
        // @todo Filter for ".." directories here?

        // Permanent redirect to "/" (HTTP status code 301)
        if (path == null || path.equals("")) {
            String newPath = req.getRequestURI() + "/";
            ServletUtil.sendPermanentRedirect(newPath, req, res);
            return;
        }

        // Handle top-level catalog.html requests.
        else if (path.equals("/") || path.equals("/catalog.html")) {
            ServletUtil.forwardToCatalogServices(req, res);
            return;
        }

        // Handle top-level catalog.xml requests.
        else if (path.equals("/catalog.xml")) {
            if (catHandler.processReqForCatalog(this, req, res, path)) {
                return;
            } else {
                res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
                ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
                return;
            }
        }

        // Handle requests for files in content directory (authorization required).
        else if (path.startsWith("/content/")) {
            ServletUtil.handleRequestForContentFile(path, this, req, res);
            return;
        }

        // Handle requests for files in root directory (authorization required).
        else if (path.startsWith("/root/")) {
            ServletUtil.handleRequestForRootFile(path, this, req, res);
            return;
        }

        // debugging
        else if (path.equals("/debug") || path.equals("/debug/")) {
            DebugHandler.doDebug(this, req, res);
            return;
        }

        // Handle static and dynamic catalog requests.
        else if (catHandler.processReqForCatalog(this, req, res, path)) {
            return;
        }

        // Handle dataset requests.
        else {
            // Check if request path recognized by DatasetHandler.
            String dsPath = this.datasetHandler_convertReqPathToDatasetPath(path);
            if (dsPath != null) {
                // Check if path matches datasetScan path.
                if (catHandler.translatePath(dsPath) != null) {
                    CrawlableDataset crDs = catHandler.findRequestedDataset(dsPath);
                    if (crDs == null) {
                        // Request is not for a known (or allowed) dataset.
                        res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
                        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
                        return;
                    }
                    this.datasetHandler_serveDataset(path, crDs, res);
                } else {
                    // Drop through and handle as regular file request.
                }
            }
            // Not recognized by DatasetHandler.
            else {
                // Check if path matches datasetScan path.
                if (catHandler.translatePath(dsPath) != null) {
                    CrawlableDataset crDs = catHandler.findRequestedDataset(dsPath);
                    if (crDs == null) {
                        // Request is not for a known (or allowed) dataset.
                        res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
                        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
                        return;
                    }
                    if (crDs.isCollection()) {
                        String newPath = req.getRequestURI() + "/catalog.html";
                        ServletUtil.sendPermanentRedirect(newPath, req, res);
                        return;
                    } else {
                        // Request is not for a known (or allowed) dataset.
                        res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
                        ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
                        return;
                    }
                } else {
                    // Drop through and handle as regular file request.
                }
            }
        }

        // If none of the above, try to handle as a request for a regular file.
        ServletUtil.handleRequestForRawFile(path, this, req, res);
    }

    public String datasetHandler_convertReqPathToDatasetPath(String reqPath) {
        String dsPath;
        if (reqPath.endsWith(".dds"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf(".dds"));
        else if (reqPath.endsWith(".das"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf(".das"));
        else if (reqPath.endsWith(".dods"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf(".dods"));
        else if (reqPath.endsWith(".ddx"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf(".ddx"));
        else if (reqPath.endsWith(".info"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf(".info"));
        else if (reqPath.endsWith(".html"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf(".html"));
        else if (reqPath.endsWith(".ver"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf(".ver"));
        else if (reqPath.endsWith("/version"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf("/version"));
        else if (reqPath.endsWith("/version/"))
            dsPath = reqPath.substring(0, reqPath.lastIndexOf("/version/"));
        else
            dsPath = reqPath;

        return dsPath;
    }

    /**
     * Serve the dataset requested.
     * <p/>
     * The request path is
     * <p/>
     * If recognize this request as one that can be handled, check that the
     * request is for an allowed dataset by using
     * CatalogRootHandler.findRequestedDatasets(). If it is not recognized or
     * is not an allowed dataset, respond with an HTTP 404 (Not Found) response,
     * Otherwise, handle the dataset request.
     *
     * @param path the dataset request path.
     * @param crDs the CrawlableDataset to be served (corresponds to the given path).
     * @param res  the HttpServletResponse
     * @throws IOException if can't complete request due to IO problems.
     */
    private void datasetHandler_serveDataset(String path,
                                             CrawlableDataset crDs,
                                             HttpServletResponse res)
            throws IOException {
        if (crDs == null) {
            throw new IllegalArgumentException("CrawlableDataset must not be null.");
        }

        String ext;
        if (path.endsWith(".dds"))
            ext = ".dds";
        else if (path.endsWith(".das"))
            ext = ".das";
        else if (path.endsWith(".dods"))
            ext = ".dods";
        else if (path.endsWith(".ddx"))
            ext = ".ddx";
        else if (path.endsWith(".info"))
            ext = ".info";
        else if (path.endsWith(".html"))
            ext = ".html";
        else if (path.endsWith(".ver"))
            ext = ".ver";
        else if (path.endsWith("/version"))
            ext = "/version";
        else if (path.endsWith("/version/"))
            ext = "/version/";
        else
            throw new IllegalArgumentException("Path not recognzied as valid request to this DatasetHandler.");

        // Check if know how to handle the data request.
        if (crDs instanceof CrawlableDatasetFile) {
            File crDsFile = ((CrawlableDatasetFile) crDs).getFile();
            if (crDsFile.exists()) {
                PrintWriter out = res.getWriter();
                res.setContentType("text/html");
                StringBuffer responseString = new StringBuffer();
                responseString
                        .append("<html><head><title>Test Response to Data Request</title></head><body>")
                        .append("<h1>Test Response to Data Request</h1>")
                        .append("File to serve: ").append(crDsFile.getPath())
                        .append("OPeNDAP request: ").append(ext)
                        .append("</body></html>");
                out.print(responseString.toString());
                res.setStatus(HttpServletResponse.SC_OK);
                out.flush();

                ServletUtil.logServerAccess(HttpServletResponse.SC_OK, responseString.length());
                return;
            } else {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Valid CrawlableDatasetFile but getFile() does not exist.");
                ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, -1);
                return;
            }
        }
//      else if ( crDs instanceof S4CrawlableDataset )
//      {
//        // serve data
//        return;
//      }
        // Don't know how to serve this type of CrawlableDataset
        else {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Type of CrawlableDataset <> not undersood.");
            ServletUtil.logServerAccess(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0);
            return;
        }
    }

    protected void makeDebugActions() {
        DebugHandler debugHandler = new DebugHandler("General");
        DebugHandler.Action act;

        act = new DebugHandler.Action("showVersion", "Show Build Version") {
            public void doAction(DebugHandler.Event e) {
                try {
                    thredds.util.IO.copyFile(rootPath + "README.txt", e.pw);
                }
                catch (Exception ioe) {
                    e.pw.println(ioe.getMessage());
                }
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showRuntime", "Show Runtime info") {
            public void doAction(DebugHandler.Event e) {
                Runtime runt = Runtime.getRuntime();
                double scale = 1.0 / (1000.0 * 1000.0);
                e.pw.println(" freeMemory= " + scale * runt.freeMemory() + " Mb");
                e.pw.println(" totalMemory= " + scale * runt.totalMemory() + " Mb");
                e.pw.println(" maxMemory= " + scale * runt.maxMemory() + " Mb");
                e.pw.println(" availableProcessors= " + runt.availableProcessors());
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showFlags", "Show Debugging Flags") {
            public void doAction(DebugHandler.Event e) {
                showFlags(e.req, e.pw);
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("toggleFlag", null) {
            public void doAction(DebugHandler.Event e) {
                if (e.target != null) {
                    String flag = e.target;
                    Debug.set(flag, !Debug.isSet(flag));
                } else
                    e.pw.println(" Must be toggleFlag=<flagName>");

                showFlags(e.req, e.pw);
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showLoggers", "Show Log4J info") {
            public void doAction(DebugHandler.Event e) {
                showLoggers(e.req, e.pw);
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("setLogger", null) {
            public void doAction(DebugHandler.Event e) {
                if (e.target == null) {
                    e.pw.println(" Must be setLogger=loggerName");
                    return;
                }

                StringTokenizer stoker = new StringTokenizer(e.target, "&=");
                if (stoker.countTokens() < 3) {
                    e.pw.println(" Must be setLogger=loggerName&setLevel=levelName");
                    return;
                }

                String loggerName = stoker.nextToken();
                stoker.nextToken(); // level=
                String levelName = stoker.nextToken();

                boolean isRootLogger = loggerName.equals("root");
                if (!isRootLogger && LogManager.exists(loggerName) == null) {
                    e.pw.println(" Unknown logger=" + loggerName);
                    return;
                }

                if (Level.toLevel(levelName, null) == null) {
                    e.pw.println(" Unknown level=" + levelName);
                    return;
                }

                Logger log = isRootLogger ? LogManager.getRootLogger() : LogManager.getLogger(loggerName);
                log.setLevel(Level.toLevel(levelName));
                e.pw.println(loggerName + " set to " + levelName);
                showLoggers(e.req, e.pw);
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showRequest", "Show HTTP Request info") {
            public void doAction(DebugHandler.Event e) {
                e.pw.println(ServletUtil.showRequestDetail(ExampleThreddsServlet.this, e.req));
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showServerInfo", "Show Server info") {
            public void doAction(DebugHandler.Event e) {
                ServletUtil.showServerInfo(ExampleThreddsServlet.this, e.pw);
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showServletInfo", "Show Servlet info") {
            public void doAction(DebugHandler.Event e) {
                ServletUtil.showServletInfo(ExampleThreddsServlet.this, e.pw);
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showSession", "Show HTTP Session info") {
            public void doAction(DebugHandler.Event e) {
                ServletUtil.showSession(e.req, e.res, e.pw);
            }
        };
        debugHandler.addAction(act);

        act = new DebugHandler.Action("showSecurity", "Show Security info") {
            public void doAction(DebugHandler.Event e) {
                e.pw.println(ServletUtil.showSecurity(e.req));
            }
        };
        debugHandler.addAction(act);
    }

    void showFlags(HttpServletRequest req, PrintStream pw) {
        Iterator iter = Debug.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String url = req.getRequestURI() + "?toggleFlag=" + key;
            pw.println("  <a href='" + url + "'>" + key + " = " + Debug.isSet(key) + "</a>");
        }
    }

    void showLoggers(HttpServletRequest req, PrintStream pw) {
        Logger root = LogManager.getRootLogger();
        showLogger(req, root, pw);

        Enumeration logEnums = LogManager.getCurrentLoggers();
        List loggersSorted = Collections.list(logEnums);
        Collections.sort(loggersSorted, new LoggerComparator());
        Iterator loggers = loggersSorted.iterator();
        while (loggers.hasNext()) {
            Logger logger = (Logger) loggers.next();
            showLogger(req, logger, pw);
        }
    }

    private void showLogger(HttpServletRequest req, Logger logger, PrintStream pw) {
        pw.print(" logger = " + logger.getName() + " level= ");
        String url = req.getRequestURI() + "?setLogger=" + logger.getName() + "&level=";
        showLevel(url, Level.ALL, logger.getEffectiveLevel(), pw);
        showLevel(url, Level.DEBUG, logger.getEffectiveLevel(), pw);
        showLevel(url, Level.INFO, logger.getEffectiveLevel(), pw);
        showLevel(url, Level.WARN, logger.getEffectiveLevel(), pw);
        showLevel(url, Level.ERROR, logger.getEffectiveLevel(), pw);
        showLevel(url, Level.FATAL, logger.getEffectiveLevel(), pw);
        showLevel(url, Level.OFF, logger.getEffectiveLevel(), pw);
        pw.println();

        Enumeration appenders = logger.getAllAppenders();
        while (appenders.hasMoreElements()) {
            Appender app = (Appender) appenders.nextElement();
            pw.println("  appender= " + app.getName() + " " + app.getClass().getName());
            if (app instanceof AppenderSkeleton) {
                AppenderSkeleton skapp = (AppenderSkeleton) app;
                if (skapp.getThreshold() != null)
                    pw.println("    threshold=" + skapp.getThreshold());
            }
            if (app instanceof FileAppender) {
                FileAppender fapp = (FileAppender) app;
                pw.println("    file=" + fapp.getFile());
            }
        }
    }

    private void showLevel(String baseUrl, Level show, Level current, PrintStream pw) {
        if (show.toInt() != current.toInt())
            pw.print(" <a href='" + baseUrl + show + "'>" + show + "</a>");
        else
            pw.print(" " + show);
    }

    private class LoggerComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Logger l1 = (Logger) o1;
            Logger l2 = (Logger) o2;
            return l1.getName().compareTo(l2.getName());
        }

        public boolean equals(Object o) {
            return this == o;
        }
    }

}
/*
 * $Log: ExampleThreddsServlet.java,v $
 * Revision 1.1  2006/03/07 23:45:33  edavis
 * Remove hardwiring of "/thredds" as the context path in TDS framework.
 * Start refactoring URL mappings in TDS framework, use ExampleThreddsServlet as test servlet.
 *
 */