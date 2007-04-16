/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
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

import thredds.servlet.DataRootHandler;
import thredds.servlet.HtmlWriter;
import thredds.servlet.ServletUtil;
import thredds.catalog.InvDatasetScan;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Date;
import java.io.File;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 11:28:25 AM
 */
public class ThreddsDispatchHandler implements DispatchHandler{

    private long threddsInitTime;
    private ReentrantLock threddsUpdateLock;


    private DataRootHandler dataRootHandler;
    private org.slf4j.Logger log;



    public void init(DispatchServlet servlet) {

        log = org.slf4j.LoggerFactory.getLogger(getClass());


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
        try {
            dataRootHandler.initCatalog("catalog.xml");
            //dataRootHandler.initCatalog( "extraCatalog.xml" );
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
                odh.getVersionStringForTHREDDSCatalog(),  // Version
                servlet.getDocsPath(),                       // docs path
                "docs/css/thredds.css",                   // userCssPath
                "docs/images/folder.gif",                 // context Logo
                "Context Logo",                           // Alternate text for context logo
                "docs/images/logo.gif",                   // Institute Logo path
                "OPeNDAP Inc.",                           // Alternate text for Institute logo
                "docs/images/sml-folder.png",             // Folder Image
                "This is a collection  "                  // Alternate text for folder image
        );

        threddsUpdateLock = new ReentrantLock(true);
        threddsInitTime = new Date().getTime();

        log.info("THREDDS initialized ");

    }

    public boolean requestCanBeHandled(DispatchServlet servlet,
                                       HttpServletRequest request)
            throws Exception{


        threddsUpdateLock.lock();
        try {
            String masterCatalog = ServletUtil.getContentPath(servlet) + "catalog.xml";
            File f = new File(masterCatalog);
            if (f.lastModified() > threddsInitTime) {
                threddsInitTime = f.lastModified();
                log.info("getThreddsCatalog(): Reinitializing THREDDS catalogs.  ");
                dataRootHandler.reinit();
                dataRootHandler.initCatalog("catalog.xml");
                log.info("getThreddsCatalog(): THREDDS has been reinitialized.  ");
            }
        }
        finally {
            threddsUpdateLock.unlock();
        }

        if(dataRootHandler.hasDataRootMatch(ServletUtil.getRequest(request))){
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
        dataRootHandler.processReqForCatalog(request,response);
    }








}
