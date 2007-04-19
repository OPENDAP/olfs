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


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;


import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.concurrent.Semaphore;

import thredds.servlet.ServletUtil;
import org.slf4j.Logger;

/**
 * This mini servlet provides access to distributed or, if it exisits, persistent documentation in the
 * content directory.
 */
public class DocServlet extends HttpServlet {


    private String documentsDirectory;

    private MimeTypes mimeTypes;

    private Logger log;

    private Semaphore syncLock;

    private int reqNumber;


    public void init() {

        reqNumber = 0;
        syncLock = new Semaphore(1);

        String dir = ServletUtil.getContentPath(this) + "docs";

        File f = new File(dir);

        if (f.exists() && f.isDirectory())
            documentsDirectory = dir;
        else {

            documentsDirectory = this.getServletContext().getRealPath("docs");

        }

        mimeTypes = new MimeTypes();

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        log.info("documentsDirectory: " + documentsDirectory);


    }


    public long getLastModified(HttpServletRequest req) {

        long lmt;


        String name = getName(req);



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
            res.sendRedirect(req.getRequestURI()+"/index.html");
            log.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }


    private String getName(HttpServletRequest req) {

        String name = req.getPathInfo();

        if(name == null)
            name = "/";

        if (name.endsWith("/"))
            name += "index.html";

        name = documentsDirectory + name;
        return name;
    }





    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        syncLock.acquireUninterruptibly();
        reqNumber++;
        PerfLog.logServerAccessStart(request, "DocServletAccess","GET", Integer.toString(reqNumber));
        syncLock.release();

        if (!redirect(request, response)) {

            String name = getName(request);

            log.debug("DocServlet - The client requested this: " + name);


            File f = new File(name);

            if (f.exists()) {
                log.debug("   Requested item exists.");


                String suffix = null;
                if (name.lastIndexOf("/") < name.lastIndexOf(".")) {
                    suffix = name.substring(name.lastIndexOf('.') + 1);
                }


                if (suffix != null) {
                    String mType = mimeTypes.getMimeType(suffix);
                    if (mType != null)
                        response.setContentType(mType);
                    log.debug("   MIME type: " + mType + "  ");
                }


                log.debug("   Sending.");


                FileInputStream fis = new FileInputStream(f);

                ServletOutputStream sos = response.getOutputStream();

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

                response.setStatus(HttpServletResponse.SC_OK);
                PerfLog.logServerAccessEnd(HttpServletResponse.SC_OK, f.length(), "DocServletAccess");

                sos.flush();

            } else {
                log.debug("   Requested item does not exist. Returning '404 Not Found'");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                PerfLog.logServerAccessEnd(HttpServletResponse.SC_NOT_FOUND, -1, "DocServletAccess");

            }

        }
        else
            PerfLog.logServerAccessEnd(HttpServletResponse.SC_MOVED_TEMPORARILY , -1, "DocServletAccess");


    }

}
