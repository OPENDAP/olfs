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

package opendap.noaa_s3;

import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/27/13
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3CatalogManager {

    org.slf4j.Logger log;

    private ConcurrentHashMap<String, S3Index> catalogNodes;

    private static S3CatalogManager theManager = null;


    private S3CatalogManager() {
        log = LoggerFactory.getLogger(this.getClass());
        catalogNodes = new ConcurrentHashMap<String, S3Index>();
    }


    public static S3CatalogManager theManager(){

        if(theManager==null)
            theManager = new S3CatalogManager();

        return theManager;
    }



    public S3Index getIndex(String requestURL){

        S3Index s3i = catalogNodes.get(requestURL);

        log.debug("getIndex() - Request for '{}' returning s3i: {}",requestURL,s3i);

        return s3i;
    }

    public void putIndex(String requestURL, S3Index s3i) {

        log.debug("putIndex() - Putting '{}' with s3i: {}",requestURL, s3i);

        if(requestURL!=null  &&  s3i!=null)
            catalogNodes.put(s3i.getS3IndexUrlString(),s3i);

    }



}
