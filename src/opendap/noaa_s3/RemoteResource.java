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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.io.IOUtils;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/28/13
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteResource {

    Logger log;
    private String _resourceUrl;
    private long _lastModified;
    private String _contentType;

    private Header _responseHeaders[];


    public RemoteResource() {
        log = LoggerFactory.getLogger(this.getClass());
        _resourceUrl = null;
        _lastModified = 0;
        _responseHeaders = null;
    }

    public RemoteResource(String url) {
        log = LoggerFactory.getLogger(this.getClass());
        _resourceUrl = url;
        _lastModified = 0;
        _responseHeaders = null;
    }


    public String getResourceUrl(){
        return _resourceUrl;
    }

    public void setResourceUrl(String url){
        if(_resourceUrl==null || !_resourceUrl.equals(url)){
            _resourceUrl  = url;
            _responseHeaders = null;
        }
    }

    public void clearResponseHeaders(){
        _responseHeaders = null;
    }


    public void setLastModifiedTime(long lmt){
        _lastModified = lmt;

    }

    /**
     *
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    public InputStream getResourceAsStream() throws IOException  {

        return getRemoteHttpResourceAsStream();
    }



    /**
     *
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    protected InputStream getRemoteHttpResourceAsStream() throws IOException  {

        String resourceUrl = getResourceUrl();

        log.debug("getResourceAsStream() - Retrieving content from " + resourceUrl);

        HttpClient httpClient = new HttpClient();
        GetMethod getRequest = new GetMethod(resourceUrl);

        int statusCode = httpClient.executeMethod(getRequest);

        if (statusCode != HttpStatus.SC_OK) {
            log.error("Unable to GET remote resource: " + resourceUrl);
            _lastModified = -1;
            _responseHeaders = null;
            log.error("getResourceAsStream() - Unable to GET the resource: {} HTTP status: {}",resourceUrl,statusCode);
            throw new IOException("RemoteResource.getResourceAsStream() - Failed to retrieve resource. HTTP status: " + statusCode);
        }
        _responseHeaders = getRequest.getResponseHeaders();

        return getRequest.getResponseBodyAsStream();
    }








    /**
     *
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    public void updateResponseHeaders()  {


        log.debug("updateResponseHeaders() - Retrieving HTTP HEAD for '{}'",  _resourceUrl);

        // Go get the HEAD for the catalog:
        HttpClient httpClient = new HttpClient();
        HeadMethod headReq = new HeadMethod(_resourceUrl);

        try {
        int statusCode = httpClient.executeMethod(headReq);

            if (statusCode != HttpStatus.SC_OK) {
                log.error("Unable to HEAD s3 object: " + _resourceUrl);
            }
            else {

                _responseHeaders = headReq.getResponseHeaders();


            }

        } catch (Exception e) {
            log.error("Unable to HEAD the resource: {} Error Msg: {}",_resourceUrl,e.getMessage());
        }
    }




    public String getContentType(){

        if(_contentType == null){
            _contentType = getHeaderValue("content-type");
        }
        return _contentType;
    }


    public long getLastModified() {

        if (_lastModified == 0) {
            String lmt_string = getHeaderValue("last-modified");

            if (lmt_string == null)
                _lastModified = -1;

            else {
                try {
                    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                    Date d = format.parse(lmt_string);
                    _lastModified = d.getTime();
                } catch (ParseException e) {
                    _lastModified = -1;
                }
            }
        }
        log.debug("getLastModified() - {}", _lastModified);

        return _lastModified;
    }


    public String getHeaderValue(String hdrName){

        if(_responseHeaders==null)
            updateResponseHeaders();

        if(_responseHeaders==null)
           return null;

        for(Header hdr : _responseHeaders){
            String name = hdr.getName();
            if(name.equalsIgnoreCase(hdrName)){
                return hdr.getValue();
            }
        }
        return null;
    }



    public void writeResourceToFile(File targetFile)throws  IOException {

        log.debug("writeResourceToFile() - targetFile: '{}'",targetFile);

        File parent = targetFile.getParentFile();

        if(!parent.exists()){

            try {
                boolean madeIt = parent.mkdirs();

                if(!madeIt){
                    String msg = "Unable to create the directory path: '" + parent + "'.";
                    log.error("writeResourceToFile() - "+msg);
                    throw new IOException(msg);
                }

            }
            catch (SecurityException e){
                String msg = "This process does not have permission to create the directory path: '" + parent+"' msg: "+e.getMessage();
                log.error("writeResourceToFile() -"+msg);
                throw new IOException(msg);
            }
        }
        String url = getResourceUrl();
        log.debug("writeResourceToFile() - resource url: '{}'",url);


        if(!targetFile.exists()) {
            log.debug("Attempting to create target file: '{}'",targetFile.getAbsolutePath());
            targetFile.createNewFile();
        }

        FileOutputStream target_os = null;
        InputStream resource_is = null;

        try {
            target_os = new FileOutputStream(targetFile);
            resource_is = getRemoteHttpResourceAsStream();
            IOUtils.copy(resource_is, target_os);
        }
        finally {
            if(resource_is!=null)
                resource_is.close();

            if(target_os!=null)
                target_os.close();
        }

    }

    public void updateResourceFileIfNeeded(File targetFile)throws  IOException {


        if(!targetFile.exists()){
            writeResourceToFile(targetFile);
            return;
        }

        long targetFileLastModifiedTime =  targetFile.lastModified();

        updateResponseHeaders();

        long resourceLastModified = getLastModified();

        if(targetFileLastModifiedTime < resourceLastModified){
            log.debug("updateResourceFileIfNeeded() - Updating resource file. ");
            writeResourceToFile(targetFile);
            return;
        }


        log.debug("updateResourceFileIfNeeded() - Resource file is current.");


    }




}
