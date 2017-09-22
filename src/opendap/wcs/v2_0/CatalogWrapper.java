/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0;

import opendap.xml.Util;
import org.apache.http.client.CredentialsProvider;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is used to wrap an instance of the WcsCatalog class with a set of
 * static methods, creating a singleton catalog from a non-singleton class.
 */

public class CatalogWrapper {


    private Date _cacheTime;

    private static final Logger log = org.slf4j.LoggerFactory.getLogger("CatalogWrapper");

    private static File serviceIdFile;
    private static AtomicReference<Element> serviceIdentification = new AtomicReference<Element>();
    private static File serviceProviderFile;
    private static AtomicReference<Element> serviceProvider       = new AtomicReference<Element>();
    private static File opsMetadataFile;
    private static AtomicReference<Element> operationsMetadata    = new AtomicReference<Element>();


    /**
     * This is the string in the template OperationsMetadata file and we replace it with the one
     * we get from Tomcat
     * TODO Make the value of this something like @SERVICE_ENDPOINT_URL@ so people don't spaz ad change it when the localize their Capabilities conent.
     * TODO And change the variable name from _defaultServiceUrl to SERVICE_END_POINT;
     */
    private static String _defaultServiceUrl= "http://your.domain.name:8080/opendap/WCS";


    private static WcsCatalog _catalogImpl;

    private static boolean intitialized = false;



    public static void init(String  metadataDir, WcsCatalog catalog) throws Exception {
        if(intitialized)
            return;

        // ingestCapabilitiesMetadata(metadataDir);

        _catalogImpl = catalog;

        intitialized = true;
    }



    public static String getDataAccessUrl(String coverageID) throws InterruptedException {
        return _catalogImpl.getDataAccessUrl(coverageID);
    }


    public static CoverageDescription getCoverageDescription(String id) throws InterruptedException, WcsException{
        return _catalogImpl.getCoverageDescription(id);

    }

    public static Element getCoverageDescriptionElement(String id)  throws InterruptedException, WcsException{
        return _catalogImpl.getCoverageDescriptionElement(id);
    }



    public static Collection<Element> getCoverageSummaryElements()  throws InterruptedException, WcsException{
        return _catalogImpl.getCoverageSummaryElements();
    }






    public static long getLastModified(){

        //return -1;
        return _catalogImpl.getLastModified();
    }










    public static void destroy() {

        _catalogImpl.destroy();

        _catalogImpl = null;

        log.info("Destroy Complete");
    }

    public static void update() throws Exception{

        _catalogImpl.update();

        log.info("Update Complete");
          
    }


    public static EOCoverageDescription getEOCoverageDescription(String id) throws WcsException {
        return _catalogImpl.getEOCoverageDescription(id);
    }


   public static  EODatasetSeries getEODatasetSeries(String id) throws WcsException {
        return _catalogImpl.getEODatasetSeries(id);
    }
    

}
