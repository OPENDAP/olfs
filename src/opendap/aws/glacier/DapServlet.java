/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.aws.glacier;

import opendap.bes.BESManager;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Scrub;
import opendap.logging.LogUtil;
import opendap.noaa_s3.S3DapDispatchHandler;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
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

    private S3DapDispatchHandler _s3DapDispatcher;




    public DapServlet() {

        _log = org.slf4j.LoggerFactory.getLogger(getClass());
        _initialized = false;
        _reqNumber = new AtomicInteger(0);
        _servletContext = null;
    }



    @Override
    public void init() throws ServletException {

        if(_initialized) return;


        _servletContext = this.getServletContext().getContextPath();


        /**
         * ###########################################################################
         *
         * These things could be in a configuration file
         *
         */
        String s3BucketContext = "/nodc";
        String s3BucketName = "ocean-archive.data.nodc.noaa.gov";
        try {
            GlacierArchiveManager.theManager().init(null);
        } catch (IOException e) {
            throw new ServletException("Failed to initialize the GlacierArchive Manager!! IOException: "+e.getMessage());
        } catch (JDOMException e) {
            throw new ServletException("Failed to initialize the GlacierArchive Manager!! JDOMException: "+e.getMessage());
        }


        Element besConfiguration = getDefaultBesManagerConfig();

        /**
         * ###########################################################################
         */




        try {
            BESManager besManager = new BESManager();
            besManager.init(this,besConfiguration);
            _s3DapDispatcher.init(this, getDefaultDapDispatchConfig() );

        } catch (Exception e) {
            _log.error("Failed to initialize BESManager.");
            throw new ServletException(e);
        }






        _initialized = true;
    }

    Element getDefaultDapDispatchConfig() {
        Element config = new Element("config");

        config.addContent(new Element("AllowDirectDataSourceAccess"));

        return config;
    }


    Element getDefaultBesManagerConfig() {

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

        try {
            LogUtil.logServerAccessStart(request, "S3_ACCESS", "HTTP-GET", Integer.toString(_reqNumber.incrementAndGet()));



            String requestURI = request.getRequestURI();

            if (!_s3DapDispatcher.requestDispatch(request, response, true)) { // Is it a DAP request?

                // We don't know how to cope, looks like it's time to 404!
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to locate requested resource.");
                _log.info("Sent 404 Response.");

            }


        } catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            } catch (Throwable t2) {
                _log.error("BAD THINGS HAPPENED!", t2);
            }
        } finally {
            RequestCache.closeThreadCache();
            LogUtil.logServerAccessEnd(0, -1, "S3_ACCESS");
        }

    }

    public long getLastModified(HttpServletRequest req) {


        RequestCache.openThreadCache();
        long reqno = _reqNumber.incrementAndGet();
        LogUtil.logServerAccessStart(req, "S3", "LastModified", Long.toString(reqno));

        _log.debug("getLastModified() - BEGIN");

        try {
            LogUtil.logServerAccessStart(req, "S3CATALOG_ACCESS", "LastModified", Long.toString(reqno));
            return _s3DapDispatcher.getLastModified(req);

        }
        finally {
            _log.debug("getLastModified() - END");

        }



    }



    public void destroy() {
        _log.info("Destroy complete.");

    }







}
