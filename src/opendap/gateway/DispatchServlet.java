/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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
package opendap.gateway;

import opendap.gateway.dapResponders.DDX;
import opendap.logging.LogUtil;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.slf4j.Logger;
import org.jdom.Document;

import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.regex.Pattern;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import opendap.coreServlet.*;
import opendap.gateway.dapResponders.*;

/**
 * User: ndp
 * Date: Apr 22, 2008
 * Time: 3:36:36 PM
 */
public class DispatchServlet extends HttpServlet {


    private Logger log;


    private Document _config;


    private AtomicInteger reqNumber;

    private String systemPath;

    private boolean isInitialized;


    private Vector<HttpResponder> responders;




    public void init() {

        if (isInitialized)
            return;

        LogUtil.logServerStartup("init()");


        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        isInitialized = false;
        reqNumber = new AtomicInteger(0);
        systemPath = ServletUtil.getSystemPath(this, "");

        responders = new Vector<HttpResponder>();



        /*
        try {
            _config = loadConfig();

        } catch (Exception e) {
            log.error("init() Failed to load it's configuration! Caught " + e.getClass().getName() + " Message: " + e.getMessage());
        }
        */


        BesGatewayApi besApi = new BesGatewayApi();


        responders.add(new DDX(systemPath, besApi));
        responders.add(new DDS(systemPath, besApi));
        responders.add(new DAS(systemPath, besApi));
        responders.add(new RDF(systemPath, besApi));

        responders.add(new HtmlDataRequestForm(systemPath, besApi));
        responders.add(new DatasetInfoHtmlPage(systemPath, besApi));

        responders.add(new Dap2Data(systemPath, besApi));
        responders.add(new Ascii(systemPath, besApi));


        responders.add(new DataDDX(systemPath, besApi));
        responders.add(new NetcdfFileOut(systemPath, besApi));
        responders.add(new XmlData(systemPath, besApi));

        responders.add(new GatewayForm(systemPath));


        log.info("masterDispatchRegex=\"" + getDispatchRegex() + "\"");
        log.info("Initialized.");


        isInitialized = true;
    }


    /**
     * Loads the configuration file specified in the servlet parameter
     * OLFSConfigFileName.
     *
     * @throws javax.servlet.ServletException When the file is missing, unreadable, or fails
     *                                        to parse (as an XML document).
     */
    private Document loadConfig() throws ServletException {

        String filename = getInitParameter("GatewayConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the Gateway configuration!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }

        filename = Scrub.fileName(ServletUtil.getContentPath(this) + filename);

        log.debug("Loading Configuration File: " + filename);


        try {

            File confFile = new File(filename);
            FileInputStream fis = new FileInputStream(confFile);

            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                return sb.build(fis);
            } finally {
                fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "gateway configuration file \"" + filename + "\" cannot be found.";
            log.warn(msg);
            //throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "gateway configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "gateway configuration file \"" + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
        }

        return null;


    }


    public Pattern getDispatchRegex() {
        String masterRegex = null;

        for (HttpResponder p : responders) {
            if (masterRegex != null)
                masterRegex += "|";
            else
                masterRegex = "";

            masterRegex += p.getRequestMatchRegexString();
        }
        return Pattern.compile(masterRegex);
    }


    /*
    public long getLastModified(HttpServletRequest req) {

        long lmt;
        lmt = -1;
        return lmt;


    }
    */


    /**
     * Gets the last modified date of the requested resource. Because the data handler is really
     * the only entity capable of determining the last modified date the job is passed  through to it.
     *
     * @param req The current request
     * @return Returns the time the HttpServletRequest object was last modified, in milliseconds
     *         since midnight January 1, 1970 GMT
     */
    protected long getLastModified(HttpServletRequest req) {

        RequestCache.startRequest();

        long reqno = reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "GATEWAY_SERVICE_ACCESS", "LastModified", Long.toString(reqno));

        if (ReqInfo.isServiceOnlyRequest(req))
            return -1;


        try {
            return -1;

        } catch (Exception e) {
            return -1;
        } finally {
            LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "GATEWAY_SERVICE_ACCESS");

        }


    }


    private boolean redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (req.getPathInfo() == null) {
            res.sendRedirect(Scrub.urlContent(req.getRequestURI() + "/"));
            log.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }


    private String getName(HttpServletRequest req) {
        return req.getPathInfo();
    }


    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        try {

            LogUtil.logServerAccessStart(request, "GATEWAY_SERVICE_ACCESS", "HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));

            if (!redirect(request, response)) {

                String name = getName(request);

                log.debug("The client requested this: " + name);

                String relativeUrl = ReqInfo.getLocalUrl(request);

                String dataSource = ReqInfo.getBesDataSourceID(relativeUrl);
                DataSourceInfo dsi;


                String requestURL = request.getRequestURL().toString();

                for (HttpResponder r : responders) {
                    if (r.matches(requestURL)) {
                        log.info("The request URL: " + requestURL + " matches " +
                                "the pattern: \"" + r.getRequestMatchRegexString() + "\"");

                        //dsi = new BESDataSource(dataSource);
                        //if(dsi.isDataset()){
                        r.respondToHttpGetRequest(request, response);
                        return;
                        //}

                    }
                }

                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                log.info("Sent BAD URL - not an OPeNDAP request suffix.");
            }

        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            } catch (Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            RequestCache.endRequest();
            LogUtil.logServerAccessEnd(0, -1, "GATEWAY_SERVICE_ACCESS");
        }
    }



    public void destroy() {

        LogUtil.logServerShutdown("destroy()");

        if(responders != null){
            for (HttpResponder r : responders) {
                log.debug("Shutting down HttpResponder: " + r.getClass().getName());
                r.destroy();
            }
        }


        super.destroy();

        log.info("Gateway service shutdown complete.");
    }



}
