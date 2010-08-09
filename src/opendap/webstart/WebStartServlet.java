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

import opendap.coreServlet.*;
import opendap.logging.LogUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
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

    private boolean disabled = false;
    private String resourcesDirectory;
    private Document configDoc;


    private JwsHandler idvViewer = null;
    private Vector<JwsHandler> jwsHandlers = null;

    //private Document configDoc;
    private AtomicInteger reqNumber;


    public void init() throws ServletException {
        super.init();
        log = org.slf4j.LoggerFactory.getLogger(getClass());



        reqNumber = new AtomicInteger(0);

        String dir = ServletUtil.getContentPath(this) + "WebStart";

        File f = new File(dir);

        log.info("Checking for resources Directory: " + dir);
        if (f.exists() && f.isDirectory()){
            resourcesDirectory = dir;
            log.info("Found resources Directory: " + dir);
        }
        else {
            log.warn("Could not locate resources Directory: " + dir);
            dir = this.getServletContext().getRealPath("WebStart");
            f = new File(dir);
            log.info("Checking for resources Directory: " + dir);
            if (f.exists() && f.isDirectory()){
                resourcesDirectory = dir;
                log.info("Found resources Directory: " + dir);
            }
            else {
                disabled = true;
                log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                log.error("Could not locate resources Directory: " + dir);
                log.error("Java WebStart Disabled!");
                log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");


            }

        }

        if(!disabled){
            log.info("resourcesDirectory: " + resourcesDirectory);

            configDoc = loadConfig();


            // Build Handler Objects
            jwsHandlers = buildJwsHandlers(resourcesDirectory,configDoc.getRootElement());
        }


    }




    /**
     * Loads the configuration file specified in the servlet parameter
     * OLFSConfigFileName.
     *
     * @throws ServletException When the file is missing, unreadable, or fails
     *                          to parse (as an XML document).
     */
    private Document loadConfig() throws ServletException {

        Document doc;

        String filename = getInitParameter("WebStartConfigFileName");
        if (filename == null) {
            String msg = "Servlet configuration must include a file name for " +
                    "the WebStart configuration!\n";
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
                doc = sb.build(fis);
            }
            finally {
            	fis.close();
            }

        } catch (FileNotFoundException e) {
            String msg = "WebStart configuration file \"" + filename + "\" cannot be found.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (IOException e) {
            String msg = "WebStart configuration file \"" + filename + "\" is not readable.";
            log.error(msg);
            throw new ServletException(msg, e);
        } catch (JDOMException e) {
            String msg = "WebStart configuration file \"" + filename + "\" cannot be parsed.";
            log.error(msg);
            throw new ServletException(msg, e);
        }

        log.debug("WebStart Configuration loaded and parsed.");
        return doc;

    }

    /**
     * Navigates the config document to instantiate an ordered list of
     * JwsHandler Handlers. Then all of the handlers are initialized by
     * calling their init() methods and passing into them the XML Element
     * that defined them from the config document.
     *
     * @return A VEector of JwsHandlers that have been intialized and are ready to use.
     * @throws ServletException When things go poorly
     */
    private Vector<JwsHandler> buildJwsHandlers(String resourcesDir, Element webStartConfig) throws ServletException {

        String msg;

        Vector<JwsHandler> jwsHandlers = new Vector<JwsHandler>();

        log.debug("Building JwsHandlers");


        for (Object o : webStartConfig.getChildren("JwsHandler")) {
            Element handlerElement = (Element) o;
            String className = handlerElement.getAttribute("className").getValue();
            JwsHandler dh;
            try {

                log.debug("Building Handler: " + className);
                Class classDefinition = Class.forName(className);
                dh = (JwsHandler) classDefinition.newInstance();

            } catch (ClassNotFoundException e) {
                msg = "Cannot find class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (InstantiationException e) {
                msg = "Cannot instantiate class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (IllegalAccessException e) {
                msg = "Cannot access class: " + className;
                log.error(msg);
                throw new ServletException(msg, e);
            } catch (ClassCastException e) {
                msg = "Cannot cast class: " + className + " to opendap.coreServlet.DispatchHandler";
                log.error(msg);
                throw new ServletException(msg, e);
            }

            log.debug("Initializing Handler: " + className);
            dh.init(handlerElement,resourcesDir);

            jwsHandlers.add(dh);
        }

        log.debug("JwsHandlers have been built.");
        return jwsHandlers;

    }


    public long getLastModified(HttpServletRequest req) {

        long lmt;

        if(disabled)
            return -1;

        String name = Scrub.fileName(getName(req));



        File f = new File(name);

        if (f.exists())
            lmt = f.lastModified();
        else
            lmt = -1;


        //log.debug("getLastModified() - Tomcat requested lastModified for: " + name + " Returning: " + new Date(lmt));

        return lmt;


    }



    private String getName(HttpServletRequest req) {

        String name = req.getPathInfo();

        if(name == null)
            name = "/";

        name = resourcesDirectory + name;
        return name;
    }



    public void doGet(HttpServletRequest req, HttpServletResponse resp) {

        LogUtil.logServerAccessStart(req, "WebStartServletAccess","GET", Integer.toString(reqNumber.incrementAndGet()));

       String serviceName = req.getPathInfo();
        while(serviceName.startsWith("/")){
            serviceName = serviceName.substring(1,serviceName.length());
        }
        if(serviceName.equals(""))
            serviceName = null;
       String query = req.getQueryString();

        log.debug(opendap.coreServlet.Util.showRequest(req,reqNumber.get()));
        log.debug(opendap.coreServlet.Util.probeRequest(this,req));

        try {

            if(disabled){
                log.error("Java WebStart is disabled!");
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;

            }

            Vector<JwsHandler> whoWantsIt = new Vector<JwsHandler>();


            for(JwsHandler vrh: jwsHandlers){
                if(vrh.datasetCanBeViewed(serviceName,query)){
                    whoWantsIt.add(vrh);
                    log.debug("The Java WebStart Handler '"+vrh.getClass().getName()+"' wants" +
                            "to handle the request: '"+serviceName+"?"+query+"'");
                }
            }


            if(!whoWantsIt.isEmpty()){
                JwsHandler jwsh  = whoWantsIt.get(0);
                if(jwsh!=null){
                    String jnlpContent = jwsh.getJnlpForDataset(query);
                    String mType = MimeTypes.getMimeType("jnlp");
                    if (mType != null)
                        resp.setContentType(mType);

                    PrintWriter pw = resp.getWriter();
                    pw.print(jnlpContent);
                }

            }
            else {
                log.error("Unable to locate a Java WebStart handler to respond to: '"+serviceName+"?"+query+"'");
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
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
