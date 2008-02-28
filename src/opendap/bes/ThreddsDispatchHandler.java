/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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

package opendap.bes;

import thredds.servlet.DataRootHandler;
import thredds.servlet.HtmlWriter;
import thredds.servlet.ServletUtil;
import thredds.catalog.InvDatasetScan;
import thredds.catalog.InvCatalog;
import thredds.crawlabledataset.CrawlableDataset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Date;
import java.util.Vector;
import java.io.File;
import java.net.URI;

import opendap.coreServlet.DispatchServlet;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.ThreddsHandler;
import opendap.coreServlet.Scrub;
import org.jdom.Element;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 11:28:25 AM
 */
public class ThreddsDispatchHandler implements ThreddsHandler {

    private long threddsInitTime;
    private ReentrantLock threddsUpdateLock;


    private DataRootHandler dataRootHandler;
    private DispatchServlet servlet;
    private org.slf4j.Logger log;

    private boolean initialized;

    private WcsCatalog wcs;
    private Vector<String> topLevelCatalogs;


    public ThreddsDispatchHandler(){

        threddsInitTime = 0;
        threddsUpdateLock = new ReentrantLock(true);
        dataRootHandler = null;
        servlet  = null;
        wcs = null;

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        initialized = false;

        topLevelCatalogs = new Vector<String>();

    }

    public DataRootHandler getDataRootHandler(){
        return dataRootHandler;
    }


    public WcsCatalog getWcsCatalog(){
        return wcs;
    }

    public void init(HttpServlet s,Element config) throws Exception{
        if(s instanceof DispatchServlet){
            init(s,config);
        }
        else {
            throw new Exception(getClass().getName()+" must be used in " +
                    "conjunction with a "+DispatchServlet.class.getName());
        }
    }


    public void init(DispatchServlet s,Element config) throws Exception{

        if(initialized) return;

        servlet  = s;

        wcs = new WcsCatalog();

        // We may wish to add a configuration option that allows users
        // to specify a number of top level catalogs. This is where we wd add
        // those catalogs from the configuration to the THREDDS "database"
        //
        topLevelCatalogs.add("catalog.xml");


        String contextPath = ServletUtil.getContextPath(servlet);
        String contentPath = ServletUtil.getContentPath(servlet);

        thredds.servlet.ServletUtil.initDebugging(servlet); // read debug flags


        InvDatasetScan.setContext(contextPath); // This gets your context path
        // from web.xml above.

        // This allows you to specify which servlet handles catalog requests.
        // We set it to "/catalog". Is "/ts" the servlet path for you? If so,
        // set this to "/ts".
        // If you use the default servlet for everything (path mapping of
        // "/*" in web.xml). set it to the empty string.
        InvDatasetScan.setCatalogServletName("/" + servlet.getServletName());

        // handles all catalogs, including ones with DatasetScan elements,
        // ie dynamic
        DataRootHandler.init(contentPath, contextPath);
        dataRootHandler = DataRootHandler.getInstance();

        dataRootHandler.registerConfigListener(wcs);

        try {
            dataRootHandler.initCatalogs(topLevelCatalogs);
        }
        catch (Throwable e) {
            log.error("Error initializing catalog: " + e.getMessage(), e);
        }

        //this.makeDebugActions();
        //dataRootHandler.makeDebugActions();
        //DatasetHandler.makeDebugActions();

        HtmlWriter.init(
                contextPath,                              // context path
                "Hyrax Data Server",                      // Name of Webb Application
                opendap.bes.Version.getVersionStringForTHREDDSCatalog(),  // Version
                servlet.getDocsPath(),                    // docs path
                "docs/css/thredds.css",                   // userCssPath
                "docs/images/folder.gif",                 // context Logo
                "Context Logo",                           // Alternate text for context logo
                "docs/images/logo.gif",                   // Institute Logo path
                "OPeNDAP Inc.",                           // Alternate text for Institute logo
                "docs/images/sml-folder.png",             // Folder Image
                "This is a collection  "                  // Alternate text for folder image
        );

        threddsInitTime = new Date().getTime();

        log.info("Initialized.");
        initialized = true;

    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception{


        updateCatalog();

        boolean isThreddsRequest = false;


        String path = request.getPathInfo();


        if(path.equals("/")){
            path = "/catalog.html";
        }

        log.debug("path:         " + path);


        if(dataRootHandler.hasDataRootMatch(path)){

            if(path.endsWith("catalog.html"))
                path = path.substring(0,path.lastIndexOf("catalog.html"));
            else if(path.endsWith("catalog.xml"))
                path = path.substring(0,path.lastIndexOf("catalog.xml"));

            CrawlableDataset cd = dataRootHandler.getCrawlableDataset(path);

            if(cd!=null && cd.isCollection()){
                log.debug("Request hasDataRootMatch, a CrawlableDataset and isCollection()");
                isThreddsRequest = true;
            }



        } else {
            String catalogPath = path.endsWith(".html")?path.substring(0,path.lastIndexOf(".html"))+".xml":path;
            URI baseURI = new URI(Scrub.completeURL(ReqInfo.getBaseURI(request)));
            InvCatalog ic = dataRootHandler.getCatalog(catalogPath,baseURI);

            log.debug("catalogPath:  " + catalogPath);
            log.debug("basURI:       " + baseURI);
            log.debug("dataRootHandler.getCatalog() returned: " + ic);

            if(ic !=null){
                log.debug("Request has a catalog.");
                isThreddsRequest = true;
            }
        }



        if(isThreddsRequest){
            log.debug("Identified a THREDDS request.");
            return true;
        }

        log.debug("Not a THREDDS request.");
        return false;
    }





    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        log.debug("Processing THREDDS request.");
        boolean ret = dataRootHandler.processReqForCatalog(request,response);
        log.debug("DataRootHandler returned: "+ret);
    }


    /**
     * Since the user can modify the THREDDS catalogs without
     * changing the underlying data source, AND we can't ask the THREDDS
     * library to tell us about the last modified times of the catalog, AND we
     * don't know which time to return (catalog modified time, OR dataset
     * modified time) we punt and return -1.
     *
     * @param req The request for which to get the last modified time.
     * @return The last time the thing refered to in the request was modified.
     */
    public long getLastModified(HttpServletRequest req){
        String name = ReqInfo.getFullSourceName(req);

        log.debug("getLastModified(): Tomcat requesting getlastModified() for " +
                "collection: " + name );
        log.debug("getLastModified(): Returning: -1" );

        return -1;
    }



    public void destroy(){
        threddsInitTime = 0;
        threddsUpdateLock = null;
        dataRootHandler = null;
        servlet  = null;
        wcs = null;
        initialized = false;
        topLevelCatalogs = null;
        log.info("Destroy complete.");

    }


    private void updateCatalog() throws Exception{
        threddsUpdateLock.lock();
        try {
            String masterCatalog = ServletUtil.getContentPath(servlet) + "catalog.xml";
            File f = new File(masterCatalog);
            if (f.lastModified() > threddsInitTime) {
                threddsInitTime = f.lastModified();
                log.info("updateCatalog(): Reinitializing THREDDS catalogs.  ");
                dataRootHandler.reinit();
                dataRootHandler.initCatalogs(topLevelCatalogs);
                log.info("updateCatalog(): THREDDS has been reinitialized.  ");
            }
        }
        finally {
            threddsUpdateLock.unlock();
        }

    }



}
