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

package opendap.noaa_s3;

import opendap.coreServlet.RequestCache;
import opendap.coreServlet.Util;
import opendap.gateway.BesGatewayApi;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;


/**
 * This subclass of BesGatewayApi knows how to build remote data source Urls  for resource held in an S3.
 * The bucket name is aliased to a bucket context in the local URL by the S3CatalogManager.
 */
public class S3BesApi extends BesGatewayApi {

    org.slf4j.Logger log;

    public S3BesApi(String servicePrefix){
        super(servicePrefix);
        log = LoggerFactory.getLogger(S3BesApi.class);
    }


    @Override
    public String getRemoteDataSourceUrl(String relativeURL, String pathPrefix, Pattern suffixMatchPattern )  {

        String s3ResourceId = relativeURL;

        // Drop the suffix for whatever dap service or alt media-type this thing matches.
        if(!s3ResourceId.equals("")){
            s3ResourceId = Util.dropSuffixFrom(s3ResourceId, suffixMatchPattern);
        }

        S3IndexedFile s3if = S3CatalogManager.theManager().getIndexedFile(s3ResourceId);

        if(s3if==null)
            return null;

        return s3if.getResourceUrl();


    }



    /**
     * Returns the last modified time of the remote resource identified by the passed remote resource URL.
     *
     * @param remoteResourceUrl The remote resource URL to check.
     * @return The last-modified time OR -1 if the last modified was not available for any reason (including a
     * not found - 404)
     */
    private long getLastModified(String remoteResourceUrl) {

        // @TODO Cache this! We do this for every response  - we should just read it from the index files
        // (cause it's already there) and then we can focus on caching/updating/refreshing just the catalog index.





        log.debug("getLastModified() - remoteResourceUrl: "+remoteResourceUrl);


        // Try to get it from the request cache.
        String lmt_cache_key = remoteResourceUrl+"_last-modified";
        Long lmt =  (Long) RequestCache.get(lmt_cache_key);
        if(lmt!=null){
            log.debug("getLastModified() - using cached lmt: {}",lmt);
            return lmt;
        }

        // It's not in the cache so hop to it!

        long lastModifiedTime;

        try {
            // Go get the HEAD for the catalog:
            HttpClient httpClient = new HttpClient();
            HeadMethod headReq = new HeadMethod(remoteResourceUrl);

            try {
            int statusCode = httpClient.executeMethod(headReq);

                if (statusCode != HttpStatus.SC_OK) {
                    log.error("getLastModified() - Unable to HEAD s3 object: " + remoteResourceUrl);
                    lastModifiedTime = -1;
                }
                else {
                    log.debug("getLastModified(): Executed HTTP HEAD for "+remoteResourceUrl);

                    Header lastModifiedHeader = headReq.getResponseHeader("Last-Modified");

                    if(lastModifiedHeader==null){
                     lastModifiedTime =  -1;
                    }
                    else {
                        String lmtString = lastModifiedHeader.getValue();
                        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                        Date d = format.parse(lmtString);
                        lastModifiedTime =  d.getTime();
                    }
                }

            } catch (Exception e) {
                log.error("Unable to HEAD the s3 resource: {} Error Msg: {}", remoteResourceUrl, e.getMessage());
                lastModifiedTime = -1;
            }

        } catch (Exception e) {
            log.debug("getLastModified(): Returning: -1");
            lastModifiedTime =  -1;
        }

        RequestCache.put(lmt_cache_key, lastModifiedTime);

        return lastModifiedTime;


    }


}
