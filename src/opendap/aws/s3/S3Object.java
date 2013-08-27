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

package opendap.aws.s3;

import opendap.aws.AwsUtil;
import opendap.noaa_s3.RemoteResource;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/14/13
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3Object extends RemoteResource {

    private Logger _log = null;

    private String _s3CacheRoot = null;

    private String _bucketName = null;
    private String _key = null;

    private File _cacheFile = null;


    protected S3Object(){
        super();
        _log = LoggerFactory.getLogger(getClass());
        _bucketName = null;
        _key = null;
    }

    public S3Object(String bucketName, String key) {
        this(bucketName, key, null);
    }

    public S3Object(String bucketName, String key, String s3CacheRoot) {
        this();
        setS3CacheRoot(s3CacheRoot);
        setBucketName(bucketName);
        setKey(key);
        String resourceUrl = getS3Url(getBucketName(), getKey());
        setResourceUrl(resourceUrl);
    }

    public String getBucketName(){
        return _bucketName;
    }
    public void setBucketName(String bucketName){
        _bucketName = bucketName;
    }


    public String getKey(){
        return _key;
    }

    public void setKey(String key){
        _key = key;
    }

    public String getS3CacheRoot(){
        return _s3CacheRoot;
    }
    public void setS3CacheRoot(String s3CacheRoot){
        _s3CacheRoot = s3CacheRoot;
    }




    public static String getS3Url(String bucketName, String key){
        StringBuilder sb = new StringBuilder();
        sb.append("http://").append(bucketName).append(".s3.amazonaws.com").append(key);
        return sb.toString();

    }



    public File getCacheFile() throws IOException {

        return _cacheFile;

    }

    private File makeCacheFile() throws IOException {

        if(_cacheFile!=null)
            return _cacheFile;

        String s3RootDir = getS3CacheRoot();

        if(s3RootDir==null)
            throw new IOException("getCacheFile() - s3CacheRoot Directory has not been set.");

        _log.debug("getCacheFile() - s3RootDir: '{}'", s3RootDir);

        File bucketDir = new File(s3RootDir,getBucketName());
        _log.debug("getCacheFile() - bucketDir: '{}'", bucketDir);

        _cacheFile = new File(bucketDir, AwsUtil.encodeKeyForFileSystemName(getKey()));
        _log.debug("getCacheFile() - cacheFile: '{}'", _cacheFile);

        return _cacheFile;

    }





    protected void setCacheFile(File f){
        _cacheFile = f;
    }

    public void updateCachedObjectAsNeeded() throws JDOMException, IOException {

        if(getS3CacheRoot()==null)
            throw new IOException("updateCachedIndexAsNeeded() - s3CacheRoot Directory has not been set.");

        updateResourceFileIfNeeded(makeCacheFile());
    }


    public void deleteCacheFile(){

        if(_cacheFile==null){
            _log.debug("deleteCacheFile() - Cache file is null. Nothing to do.");
            return;
        }

        if(_cacheFile.exists()){
            _log.debug("deleteCacheFile() - Deleting {}", _cacheFile);
            _cacheFile.delete();
            setCacheFile(null);
        }
        else {
            _log.debug("deleteCacheFile() - The file {} does not exist. Nothing to do.");
        }

    }


    public void cacheObjectToFile() throws JDOMException, IOException {

        writeResourceToFile(makeCacheFile());

    }


    @Override
    public InputStream getResourceAsStream() throws IOException  {

        if(getS3CacheRoot()==null){
            _log.debug("getResourceAsStream() - Caching not enabled. Retrieving remote resource as stream.");
            return super.getResourceAsStream();
        }

        File cacheFile = makeCacheFile();

        if(!cacheFile.exists() || cacheFile.length()==0){
            _log.debug("getResourceAsStream() - Cache configured, resource not cached. Retrieving...");
            writeResourceToFile(cacheFile);
        }

        _log.debug("getResourceAsStream() - Retrieving resource from cached file: {}", cacheFile);
        return new FileInputStream(cacheFile);


    }




}
