/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2010 OPeNDAP, Inc.
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
package opendap.webstart;

import opendap.coreServlet.MimeTypes;
import opendap.coreServlet.OPeNDAPException;
import opendap.coreServlet.Scrub;
import opendap.coreServlet.ServletUtil;
import opendap.logging.LogUtil;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jul 23, 2010
 * Time: 1:41:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebStartServlet extends HttpServlet {

    private Logger log;

    private String resourcesDirectory;


    private ViewerRequestHandler idvViewer = null;
    private Vector<ViewerRequestHandler> viewers = null;

    //private Document configDoc;
    private AtomicInteger reqNumber;


    public void init() throws ServletException {
        super.init();
        LogUtil.initLogging(this);
        log = org.slf4j.LoggerFactory.getLogger(getClass());



        reqNumber = new AtomicInteger(0);

        String dir = ServletUtil.getContentPath(this) + "resources/WebStart";

        File f = new File(dir);

        if (f.exists() && f.isDirectory())
            resourcesDirectory = dir;
        else {

            resourcesDirectory = this.getServletContext().getRealPath("resources/WebStart");

        }
        log.info("resourcesDirectory: " + resourcesDirectory);




        // Build Handler Objects
        idvViewer = new IdvViewerRequestHandler();

        viewers = new Vector<ViewerRequestHandler>();

        viewers.add(idvViewer);

        for(ViewerRequestHandler vrh: viewers){
            vrh.init(resourcesDirectory);
        }


        // Build configuration elements


        try {

        } catch (Exception e) {
            throw new ServletException(e);
        }
    }


    public long getLastModified(HttpServletRequest req) {

        long lmt;


        String name = Scrub.fileName(getName(req));



        File f = new File(name);

        if (f.exists())
            lmt = f.lastModified();
        else
            lmt = -1;


        //log.debug("getLastModified() - Tomcat requested lastModified for: " + name + " Returning: " + new Date(lmt));

        return lmt;


    }


    private boolean redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (req.getPathInfo() == null) {
            res.sendRedirect(Scrub.urlContent(req.getRequestURI()+"/index.html"));
            log.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }


    private String getName(HttpServletRequest req) {

        String name = req.getPathInfo();

        if(name == null)
            name = "/";

        name = resourcesDirectory + name;
        return name;
    }



    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        try {

            LogUtil.logServerAccessStart(request, "DocServletAccess","GET", Integer.toString(reqNumber.incrementAndGet()));

            if (!redirect(request, response)) {

                String name = Scrub.fileName(getName(request));

                log.debug("DocServlet - The client requested this: " + name);


                File f = new File(name);

                if (f.exists()) {
                    log.debug("Requested item exists.");


                    String suffix = null;
                    if (name.lastIndexOf("/") < name.lastIndexOf(".")) {
                        suffix = name.substring(name.lastIndexOf('.') + 1);
                    }


                    if (suffix != null) {
                        String mType = MimeTypes.getMimeType(suffix);
                        if (mType != null)
                            response.setContentType(mType);
                        log.debug("   MIME type: " + mType + "  ");
                    }

                    log.debug("Found Potential WebStart file: " +name);











                    ServletOutputStream sos = null;
                    FileInputStream fis = new FileInputStream(f);

                    try {


                        sos = response.getOutputStream();

                        byte buff[] = new byte[8192];
                        int rc;
                        boolean doneReading = false;
                        while (!doneReading) {
                            rc = fis.read(buff);
                            if (rc < 0) {
                                doneReading = true;
                            } else if (rc > 0) {
                                sos.write(buff, 0, rc);
                            }

                        }
                    }
                    finally {

                        if(fis!=null)
                            fis.close();

                        if(sos!=null)
                            sos.flush();
                    }

                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_OK, f.length(), "DocServletAccess");


                } else {
                    log.debug("   Requested item does not exist. Returning '404 Not Found'");
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    LogUtil.logServerAccessEnd(HttpServletResponse.SC_NOT_FOUND, -1, "DocServletAccess");

                }

            }
            else
                LogUtil.logServerAccessEnd(HttpServletResponse.SC_MOVED_TEMPORARILY , -1, "DocServletAccess");


        }
        catch( Throwable t){
            try {
                OPeNDAPException.anyExceptionHandler(t, response);
            }
            catch(Throwable t2) {
                log.error("BAD THINGS HAPPENED!", t2);
            }
        }
    }

    public void doGet2(HttpServletRequest req, HttpServletResponse resp) {

        String requestUrl = "";
        try {

            for(ViewerRequestHandler vrh: viewers){
                if(vrh.datasetCanBeViewed(requestUrl)){
                    resp.getWriter();

                }
            }



        }
        catch (Throwable t) {
            try {
                OPeNDAPException.anyExceptionHandler(t, resp);
            }
            catch (Throwable t2) {
                try {
                    log.error("\n########################################################\n" +
                            "Request proccessing failed.\n" +
                            "Normal Exception handling failed.\n" +
                            "This is the last error log attempt for this request.\n" +
                            "########################################################\n", t2);
                }
                catch (Throwable t3) {
                    // It's boned now.. Leave it be.
                }
            }
        }
        finally {
            this.destroy();
        }
    }


}
