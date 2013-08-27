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

import opendap.aws.s3.S3Object;
import org.jdom.JDOMException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/27/13
 * Time: 3:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class S3CatalogManager {

    org.slf4j.Logger log;

    private ConcurrentHashMap<String, S3IndexedFile> _indexedFiles;
    private ConcurrentHashMap<String, S3Index> _catalogNodes;
    private ConcurrentHashMap<String, String> _s3BucketList;

    private static S3CatalogManager theManager = null;

    private String _catalogServiceContext;
    private String _dapServiceContext;

    String _s3CatalogCache;


    private S3CatalogManager() {
        log = LoggerFactory.getLogger(this.getClass());
        _catalogNodes = new ConcurrentHashMap<String, S3Index>();
        _s3BucketList = new ConcurrentHashMap<String, String>();
        _indexedFiles = new ConcurrentHashMap<String, S3IndexedFile>();
        _dapServiceContext = "/dap";
        _catalogServiceContext = "/catalog";
        _s3CatalogCache = "/Users/ndp/scratch/s3Test/catalogCache";

    }



    public static S3CatalogManager theManager(){

        if(theManager==null)
            theManager = new S3CatalogManager();

        return theManager;
    }

    public void setS3CatalogCacheDir(String s3CatalogCacheDir){
        _s3CatalogCache = s3CatalogCacheDir;
    }

    public void setCatalogServiceContext(String catalogServiceContext){
        _catalogServiceContext = catalogServiceContext;
    }

    public void setDapServiceContext(String dapServiceContext){
        _dapServiceContext = dapServiceContext;
    }

    public String getCatalogServiceContext(){
        return _catalogServiceContext;
    }

    public String getDapServiceContext(){
        return _dapServiceContext;
    }


    public S3Index getIndex(String id){
        if(id==null)
            return null;
        S3Index s3i = _catalogNodes.get(id);
        log.debug("getIndex() - Request for '{}' returning s3i: {}",id,s3i);
        return s3i;
    }

    public void putIndex(S3Index s3i) throws JDOMException, IOException {
        String key = s3i.getKey();
        String s3ServiceId = s3i.getBucketContext()+key;
        log.debug("putIndex() - Putting index for '{} in memory cache. S3Index URL: '{}'",s3ServiceId, s3i.getResourceUrl());
        _catalogNodes.putIfAbsent(s3ServiceId, s3i);

    }

    public S3IndexedFile getIndexedFile(String id){
        if(id==null)
            return null;
        S3IndexedFile s3if = _indexedFiles.get(id);
        log.debug("getIndexedFile() - Request for '{}' returning {}",id,s3if);
        return s3if;
    }


    public void putIndexedFile(S3IndexedFile s3if, String bucketContext) throws JDOMException, IOException {
        String key = s3if.getKey();
        String s3ServiceId = bucketContext+key;
        log.debug("putIndexedFile() - Putting indexed file for '{} in memory cache. Resource URL: '{}'",s3ServiceId, s3if.getResourceUrl());
        _indexedFiles.putIfAbsent(s3ServiceId, s3if);

    }


    public void addBucket(String bucketContext, String bucketName){

        while(bucketContext.startsWith("/"))
            bucketContext = bucketContext.substring(1);

        bucketContext = "/" + bucketContext;

        _s3BucketList.putIfAbsent(bucketContext, bucketName);
    }

    public String getBucketNameForContext(String bucketContext){
        return _s3BucketList.get(bucketContext);
    }

    public String getBucketContext(String relativeUrl){

        String bucketContext = "";
        for(String context: _s3BucketList.keySet()){
            if(relativeUrl.startsWith(context)){
                if(context.length()>bucketContext.length())
                    bucketContext = context;
            }
        }

        return bucketContext;
    }

    public String getBucketName(String relativeUrl){

        return _s3BucketList.get(getBucketContext(relativeUrl));
    }


    public Enumeration<String> getBucketContexts(){
        return _s3BucketList.keys();
    }

    public void ingestIndex(String bucketContext,String bucketName) throws JDOMException, IOException {

        S3Index rootIndex = new S3Index(bucketName);
        rootIndex.setS3CacheRoot(_s3CatalogCache);
        rootIndex.setBucketContext(bucketContext);


        Vector<S3Index> indices = rootIndex.getChildren(true,0);

        putIndex(rootIndex);
        for(S3Index s3i : indices){
            putIndex(s3i);
        }



    }

    public void ingestIndexedFiles(String bucketContext, String bucketName) throws JDOMException, IOException {


        String rootIndexId = bucketContext + "/" + S3Index.getCatalogIndexString();


        S3Index rootIndex = _catalogNodes.get(rootIndexId);

        if(rootIndex==null){
            throw new IOException("Unable to load S3IndexedFiles for bucket "+bucketName+" because the root S3Index has not been loaded.");
        }

        Vector<S3IndexedFile> indexedFiles =rootIndex.getChildIndexedFiles(true,0);
        for(S3IndexedFile s3if : indexedFiles){
            putIndexedFile(s3if,bucketContext);
        }


    }



}
