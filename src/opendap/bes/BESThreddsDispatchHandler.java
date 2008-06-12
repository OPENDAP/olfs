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

import thredds.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

import opendap.coreServlet.*;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.transform.XSLTransformer;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 11:28:25 AM
 */
public class BESThreddsDispatchHandler implements DispatchHandler {

    private DispatchServlet servlet;
    private org.slf4j.Logger log;

    private boolean initialized;



    public BESThreddsDispatchHandler(){

        servlet  = null;

        log = org.slf4j.LoggerFactory.getLogger(getClass());

        initialized = false;


    }


    public void init(HttpServlet s,Element config) throws Exception{
        if(s instanceof DispatchServlet){
            init(((DispatchServlet)s),config);
        }
        else {
            throw new Exception(getClass().getName()+" must be used in " +
                    "conjunction with a "+DispatchServlet.class.getName());
        }
    }


    public void init(DispatchServlet s,Element config) throws Exception{

        if(initialized) return;

        servlet  = s;


        log.info("Initialized.");
        initialized = true;

    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception{


        boolean isThreddsRequest = false;


        String datasetname = ReqInfo.getDataSetName(request);
        String reqSuffix = ReqInfo.getRequestSuffix(request);


        if(datasetname.equalsIgnoreCase("catalog") && reqSuffix.equalsIgnoreCase("xml")){
            isThreddsRequest = true;
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


        String collectionName = Scrub.urlContent(ReqInfo.getFullSourceName(request));

        if (collectionName.endsWith("/catalog.xml")) {
            collectionName = collectionName.substring(0, collectionName.lastIndexOf("catalog.xml"));
        }

        if (!collectionName.endsWith("/"))
            collectionName += "/";

        if (collectionName.startsWith("/"))
            collectionName = collectionName.substring(1,collectionName.length());

        log.debug("sendThreddsCatalog() - collectionName:  " + collectionName);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        Document showCatalogDoc = new Document();

        if (BesAPI.getCatalog(collectionName, showCatalogDoc)) {

            String xsltDoc = ServletUtil.getPath(servlet, "/docs/xsl/catalog.xsl");

            XSLTransformer transformer = new XSLTransformer(xsltDoc);

            Document threddsCatalogDoc = transformer.transform(showCatalogDoc);

            response.setContentType("text/plain");
            response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
            response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
            response.setHeader("XDAP", Version.getXDAPVersion(request));
            response.setHeader("Content-Description", "thredds_catalog");
            response.setStatus(HttpServletResponse.SC_OK);

            xmlo.output(threddsCatalogDoc, response.getWriter());

            //xmlo.output(showCatalogDoc, System.out);
            //xmlo.output(threddsCatalogDoc, System.out);

        } else {
            BESError besError = new BESError(showCatalogDoc);
            besError.sendErrorResponse(servlet, response);
            log.error(besError.getMessage());

        }

        log.debug("THREDDS showCatalogDoc request processed.");


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
        servlet  = null;
        initialized = false;
        log.info("Destroy complete.");

    }




}
