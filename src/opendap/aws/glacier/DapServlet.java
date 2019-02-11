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

package opendap.aws.glacier;

import opendap.bes.BESManager;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.logging.LogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/21/13
 * Time: 7:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class DapServlet extends HttpServlet {

    private Logger _log;
    private boolean _initialized;

    //private String systemPath;
    private AtomicInteger _reqNumber;


    private String _servletContext;

    private GlacierDapDispatcher _glacierDapDispatcher;

    private Document _configDoc;



    public DapServlet() {

        _log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;
        _reqNumber = new AtomicInteger(0);
        _servletContext = null;

        _glacierDapDispatcher = new GlacierDapDispatcher();

    }



    @Override
    public void init() throws ServletException {

        if(_initialized) return;


        _servletContext = this.getServletContext().getContextPath();


        loadConfig();






        /**
         * ###########################################################################
         *
         * These things could be in a configuration file
         *
         */
        Element glacierServiceConfiguration = _configDoc.getRootElement();
        Element besConfiguration = glacierServiceConfiguration.getChild("BesConfig");


        /**
         * ###########################################################################
         */



        try {
            GlacierManager.theManager().init(glacierServiceConfiguration);
        } catch (IOException e) {
            String msg = new StringBuilder().append("Failed to initialize the GlacierArchive Manager!! IOException: ").append(e.getMessage()).toString();
            e.printStackTrace();
            throw new ServletException(msg);
        } catch (JDOMException e) {
            String msg = new StringBuilder().append("Failed to initialize the GlacierArchive Manager!! IOException: ").append(e.getMessage()).toString();
            e.printStackTrace();
            throw new ServletException(msg);
        }


        try {
            BESManager besManager = new BESManager();
            besManager.init(besConfiguration);
            _glacierDapDispatcher.init(this, getDefaultDapDispatchConfig());

        } catch (Exception e) {
            _log.error("Failed to initialize BESManager.");
            throw new ServletException(e);
        }


        /**
         * ###########################################################################
         */

        _initialized = true;
    }




    /**
     * Loads the configuration file specified in the servlet parameter
     * OLFSConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private void loadConfig() throws ServletException {

        String basename = getInitParameter("ConfigFileName");
        if (basename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the Glacier Service configuration!\n";
            System.err.println(msg);
            throw new ServletException(msg);
        }

        String filename = Scrub.fileName(ServletUtil.getConfigPath(this) + basename);


        File confFile = new File(filename);

        if(!confFile.exists()){
            filename = Scrub.fileName(ServletUtil.getSystemPath(this,"conf") + "/" + basename);
            confFile = new File(filename);
        }



        _log.debug("Loading Configuration File: " + filename);

        try {



            FileInputStream fis = new FileInputStream(confFile);

            try {
                // Parse the XML doc into a Document object.
                SAXBuilder sb = new SAXBuilder();
                _configDoc = sb.build(fis);
            }
            finally {
            	fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "loadConfig() - The configuration file \"" + filename + "\" cannot be found.";
            _log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "loadConfig() - The configuration file \"" + filename + "\" is not readable.";
            _log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "loadConfig() - The configuration file \"" + filename + "\" cannot be parsed.";
            _log.error(msg);
            throw new ServletException(msg, e);
        }

        _log.debug("Configuration loaded and parsed.");

    }








    Element getDefaultDapDispatchConfig() {
        Element config = new Element("config");

        config.addContent(new Element("AllowDirectDataSourceAccess"));

        return config;
    }


    public static Element getDefaultBesManagerConfig() {

        Element e;
        Element handler = new Element("Handler");
        handler.setAttribute("className","opendap.bes.BESManager");

        Element bes = new Element("BES");
        handler.addContent(bes);

        e = new Element("prefix");
        e.setText("/");
        bes.addContent(e);


        e = new Element("host");
        e.setText("localhost");
        bes.addContent(e);


        //e = new Element("adminPort");
        //e.setText("11022");
        //bes.addContent(e);


        e = new Element("port");
        e.setText("10022");
        bes.addContent(e);


        e = new Element("maxResponseSize");
        e.setText("0");
        bes.addContent(e);


        e = new Element("ClientPool");
        e.setAttribute("maximum","10");
        e.setAttribute("maxCmds","2000");
        bes.addContent(e);



        return handler;
    }



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response) {

        int status = HttpServletResponse.SC_OK;
        try {
            LogUtil.logServerAccessStart(request, "GLACIER_DAP_ACCESS", "HTTP-GET", Integer.toString(_reqNumber.incrementAndGet()));



            String requestURI = request.getRequestURI();

            if (!_glacierDapDispatcher.requestDispatch(request, response, true)) { // Is it a DAP request?

                // We don't know how to cope, looks like it's time to 404!
                status = HttpServletResponse.SC_NOT_FOUND;
                response.sendError(status, "Unable to locate requested resource.");
                _log.info("Sent 404 Response.");

            }


        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, this, response);
            } catch (Throwable t2) {
                _log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            RequestCache.closeThreadCache();
            LogUtil.logServerAccessEnd(status, "GLACIER_DAP_ACCESS");
        }

    }

    public long getLastModified(HttpServletRequest req) {


        RequestCache.openThreadCache();
        long reqno = _reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "GLACIER_DAP_ACCESS", "LastModified", Long.toString(reqno));

        _log.debug("getLastModified() - BEGIN");

        try {
            return _glacierDapDispatcher.getLastModified(req);

        }
        finally {
            _log.debug("getLastModified() - END");

        }



    }



    public void destroy() {

        GlacierManager.theManager().destroy();
        _log.info("Destroy complete.");

    }







}
