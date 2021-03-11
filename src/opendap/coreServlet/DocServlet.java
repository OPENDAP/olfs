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

package opendap.coreServlet;


import opendap.http.error.NotFound;
import opendap.io.HyraxStringEncoding;
import opendap.logging.LogUtil;
import org.slf4j.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This mini servlet provides access to distributed or, if it exists, persistent documentation in the
 * content directory.
 */
public class DocServlet extends HttpServlet {


    private static String documentsDirectory;
    private static Logger log;
    private static AtomicInteger reqNumber;

    @Override
    public void init() {
        reqNumber = new AtomicInteger(0);
        String dir = ServletUtil.getConfigPath(this) + "docs";
        File f = new File(dir);

        if (f.exists() && f.isDirectory())
            documentsDirectory = dir;
        else {
            documentsDirectory = this.getServletContext().getRealPath("docs");
        }
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        log.info("documentsDirectory: {}", documentsDirectory);
    }

    @Override
    public long getLastModified(HttpServletRequest req) {

        long lmt = new Date().getTime(); // time is now...

        String name = Scrub.fileName(getName(req));
        if (name != null) {
            File f = new File(name);
            if (f.exists())
                lmt = f.lastModified();
        }
        return lmt;
    }


    private boolean redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {

        if (req.getPathInfo() == null) {
            res.sendRedirect(Scrub.urlContent(req.getRequestURI() + "/index.html"));
            log.debug("Sent redirect to make the web page work!");
            return true;
        }
        return false;
    }


    private String getName(HttpServletRequest req) {

        String name = req.getPathInfo();

        if (name == null)
            name = "/";

        if (name.endsWith("/"))
            name += "index.html";

        name = documentsDirectory + name;
        return name;
    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {

        int status = HttpServletResponse.SC_OK;

        int response_size = 0;
        try {
            String contextPath = ServletUtil.getContextPath(this);
            String servletName = "/" + this.getServletName();

            LogUtil.logServerAccessStart(request, LogUtil.DOCS_ACCESS_LOG_ID, "HTTP-GET", Integer.toString(reqNumber.incrementAndGet()));

            if (!redirect(request, response)) {

                String name = Scrub.fileName(getName(request));

                log.debug("DocServlet - The client requested this: {}", name);

                if (name == null) {
                    // throw new NotFound("Unable to locate: "+name)
                    status = OPeNDAPException.anyExceptionHandler(new NotFound("Unable to locate: " + name), this, response);
                } else {
                    File f = new File(name);
                    if (f.exists()) {
                        log.debug("   Requested item exists.");
                        if (f.isFile()) {
                            log.debug("   It's a file...");


                            String suffix = null;
                            if (name.lastIndexOf('/') < name.lastIndexOf('.')) {
                                suffix = name.substring(name.lastIndexOf('.') + 1);
                            }

                            String mType = null;
                            if (suffix != null) {
                                mType = MimeTypes.getMimeType(suffix);
                                if (mType != null)
                                    response.setContentType(mType);
                                log.debug("   MIME type: {}", mType);
                            }

                            log.debug("   Sending.");
                            if (mType != null)
                                response.setContentType(mType);


                            if (mType != null && mType.startsWith("text/")) {
                                PrintWriter sos = response.getWriter();
                                String docString = readFileAsString(f);
                                log.debug("Read file {} into a String.", f.getAbsolutePath());
                                docString = docString.replace("<CONTEXT_PATH />", contextPath);
                                docString = docString.replace("<SERVLET_NAME />", servletName);
                                sos.println(docString);
                                response_size = docString.length();
                            } else {
                                ServletOutputStream sos = response.getOutputStream();
                                DataOutputStream dos = new DataOutputStream(sos);
                                try (FileInputStream fis = new FileInputStream(f)) {
                                    byte[] buff = new byte[8192];
                                    int rc;
                                    boolean doneReading = false;
                                    while (!doneReading) {
                                        rc = fis.read(buff);
                                        if (rc < 0) {
                                            doneReading = true;
                                        } else if (rc > 0) {
                                            dos.write(buff, 0, rc);
                                        }

                                    }
                                } finally {
                                    dos.flush();
                                    response_size = dos.size();
                                }
                            }

                        } else if (f.isDirectory()) {
                            log.debug("   Requested directory exists.");
                            try {
                                response.sendRedirect(Scrub.completeURL(request.getRequestURL().toString()) + "/");
                            } catch (IOException e) {
                                status = OPeNDAPException.anyExceptionHandler(e, this, response);
                            }
                        } else {
                            String msg = "Unable to determine type of requested item: " + f.getName();
                            log.error("doGet() - {}", msg);
                            status = OPeNDAPException.anyExceptionHandler(new NotFound(msg), this, response);
                        }
                    } else {
                        status = OPeNDAPException.anyExceptionHandler(new NotFound("Failed to locate resource: " + name), this, response);
                    }
                }
            }
        } catch (Throwable t) {
            try {
                status = OPeNDAPException.anyExceptionHandler(t, this, response);
            } catch (Throwable t2) {
                try {
                    log.error("BAD THINGS HAPPENED!", t2);
                } catch (Throwable t3) {
                    // Never mind we can't manage anything sensible at this point....
                }
            }
        } finally {
            LogUtil.logServerAccessEnd(status, response_size, LogUtil.DOCS_ACCESS_LOG_ID);
        }
    }


    private static String readFileAsString(File file) throws IOException {

        Scanner scanner = new Scanner(file, HyraxStringEncoding.getCharset().name());
        StringBuilder stringBuilder = new StringBuilder();

        try {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine()).append("\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }


}
