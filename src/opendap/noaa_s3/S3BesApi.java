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
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/21/13
 * Time: 1:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3BesApi extends BesGatewayApi {


    private String s3BucketName = "ocean-archive.data.nodc.noaa.gov";




    public S3BesApi(){
        this("");
    }


    public S3BesApi(String servicePrefix){
        super(servicePrefix);
    }


    @Override
    public String getRemoteDataSourceUrl(String relativeURL, String pathPrefix, Pattern suffixMatchPattern )  {



        //String requestSuffix = ReqInfo.getSuffix(relativeURL);



        // Strip leading slash(es)
        while(relativeURL.startsWith("/") && !relativeURL.equals("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        String dataSourceUrl = relativeURL;


        // Strip the path off.
        if(pathPrefix!=null && dataSourceUrl.startsWith(pathPrefix))
            dataSourceUrl = dataSourceUrl.substring(pathPrefix.length());


        if(!dataSourceUrl.equals("")){
            dataSourceUrl = Util.dropSuffixFrom(dataSourceUrl, suffixMatchPattern);
        }


        // http://ocean-archive.data.nodc.noaa.gov/0087989/1.1/data/0-data/cortadv4_row00_col14.nc


        // @TODO Cache this! We do this twice for every response - lastModified plus doGet
        // Maybe even we should just read it from the index files (cause it's there) and then we can
        // Focus on caching/updating/refreshing just the catalog index.
        dataSourceUrl = getS3DataAccessUrlString(dataSourceUrl);

        long lmt = getLastModified(dataSourceUrl);


        // That's broken.

        if(lmt==-1)
            return null;  // Can't set it to null, because null gets turned in to "null", a string. *sigh*

//        URL url = new URL(dataSourceUrl);
        //log.debug(urlInfo(url));

        return dataSourceUrl;


    }

    public String getS3DataAccessUrlString(String s3ResourceId){

        StringBuilder sb = new StringBuilder();

        sb.append("http://").append(s3BucketName).append(".s3.amazonaws.com/").append(s3ResourceId);

        return sb.toString();

    }



    /**
     *
     * @param remoteResourceUrl
     * @return
     * @throws java.io.IOException
     * @throws org.jdom.JDOMException
     */
    public long getLastModified(String remoteResourceUrl) {

        org.slf4j.Logger log = LoggerFactory.getLogger(S3BesApi.class);


        log.debug("getLastModified() - remoteResourceUrl: "+remoteResourceUrl);

        try {
            log.debug("remoteDataSourceUrl: " + remoteResourceUrl);

            // Go get the HEAD for the catalog:
            HttpClient httpClient = new HttpClient();
            HeadMethod headReq = new HeadMethod(remoteResourceUrl);

            try {
            int statusCode = httpClient.executeMethod(headReq);

                if (statusCode != HttpStatus.SC_OK) {
                    log.error("Unable to HEAD s3 object: " + remoteResourceUrl);
                    return -1;
                }

                log.debug("getLastModified(): Getting HTTP HEAD for "+remoteResourceUrl);

                Header lastModifiedHeader = headReq.getResponseHeader("Last-Modified");

                if(lastModifiedHeader==null){
                 return -1;
                }
                String lmtString = lastModifiedHeader.getValue();

                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                Date d = format.parse(lmtString);
                return d.getTime();

            } catch (Exception e) {
                log.error("Unable to HEAD the s3 resource: {} Error Msg: {}",remoteResourceUrl,e.getMessage());
            }

            return -1;



        } catch (Exception e) {
            log.debug("getLastModified(): Returning: -1");
            return -1;
        }

    }


}
