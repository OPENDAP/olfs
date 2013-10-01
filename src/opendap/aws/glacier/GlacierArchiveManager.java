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

package opendap.aws.glacier;

import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import opendap.aws.AwsUtil;
import opendap.aws.auth.Credentials;
import opendap.aws.s3.S3Object;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/18/13
 * Time: 11:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierArchiveManager {


    org.slf4j.Logger log;

    private boolean isInitialized ;

    private String _glacierRootDirectoryName;
    private File _glacierRootDirectory;

    private String _galcierEndpoint;

    private String _parentContext;
    private String _glacierServiceContext;
    private String _dapServiceContext;
    private String _catalogServiceContext;

    private String _pathDelimiter = "/";

    private ConcurrentHashMap<String, GlacierVaultManager> _vaults;

    private static GlacierArchiveManager theManager = null;

    public  static final String DefaultResourceCacheDirectoryName = "cache";


    private GlacierArchiveManager() {
        log = LoggerFactory.getLogger(this.getClass());
        _glacierRootDirectoryName = null;
        _glacierRootDirectory = null;
        isInitialized = false;
        _vaults = new ConcurrentHashMap<String, GlacierVaultManager>();
    }


    public void init(Element config) throws IOException, JDOMException {

        if(isInitialized) {
            log.debug("init() - Already initialized, nothing to do.");
            return;
        }

        setGlacierEndpoint("https://glacier.us-east-1.amazonaws.com/");

        setGlacierArchiveRootDirectory("/Users/ndp/scratch/glacier");

        setParentContext("");

        loadVaults();


        DownloadManager.theManager().init(config);


        isInitialized = true;
    }

    public String getGlacierEndpoint(){
        return _galcierEndpoint;
    }


    public void setGlacierEndpoint(String endpoint){
        _galcierEndpoint = endpoint;
    }

    public String getGlacierServiceContext(){
        return _glacierServiceContext;
    }

    public String getDapServiceContext(){
        return _dapServiceContext;
    }

    public String getDapServiceContext(String resourceId){
        String dapServiceForVault =  getDapServiceContext() +"/"+ getVaultName(resourceId);

        return dapServiceForVault ;
    }

    public String getCatalogServiceContext(){
        return _catalogServiceContext ;
    }

    public String getCatalogServiceContext(String resourceId){

        String catalogServiceForVault =  getCatalogServiceContext() +"/"+ getVaultName(resourceId);

        return catalogServiceForVault ;
    }



    public void setParentContext(String parentContext){
        _parentContext = parentContext;
        String myContext = _parentContext + _pathDelimiter + getName();
        setGlacierServiceContext(myContext);
    }

    public void setGlacierServiceContext(String serviceContext){
        _glacierServiceContext = serviceContext;
        _dapServiceContext = _glacierServiceContext + "/dap";
        _catalogServiceContext = _glacierServiceContext + "/catalog";
    }


    String getName(){
        return "glacier";
    }




    public static GlacierArchiveManager theManager(){

        if(theManager==null)
            theManager = new GlacierArchiveManager();

        return theManager;
    }

    public void setGlacierArchiveRootDirectory(String archiveRootDirectory) throws IOException {

        if(archiveRootDirectory==null)
            throw new IOException("Cache Root Directory String was null valued.");

        _glacierRootDirectoryName = archiveRootDirectory;

        File glacierDir = new File(_glacierRootDirectoryName);
        if(!glacierDir.exists() && !glacierDir.mkdirs()){
            throw new IOException("Unable to create top level glacier archive directory: "+glacierDir);
        }
        _glacierRootDirectory = glacierDir;


    }




    public String getGlacierRootDirName()  {
        return _glacierRootDirectoryName;
    }



    public File getGlacierRootDir() throws IOException {
        if(_glacierRootDirectory ==null)
            throw new IOException("Cache Root Directory was null valued.");

        return new File(_glacierRootDirectory.getCanonicalPath());
    }

    public File getResourceCacheDir() throws IOException {
        if(_glacierRootDirectory ==null)
            throw new IOException("Cache Root Directory was null valued.");

        return new File(_glacierRootDirectory,DefaultResourceCacheDirectoryName);
    }


    public GlacierRecord getArchiveRecord(String vaultName, String resourceId){
        GlacierVaultManager gvm =  getVaultManager(vaultName);
        if(gvm==null)
            return null;

        return gvm.getArchiveRecord(resourceId);
    }

    public GlacierRecord getArchiveRecord(String combinedVaultResourceId){
        String glacierVaultName = GlacierArchiveManager.theManager().getVaultName(combinedVaultResourceId);

        String resourceId  = combinedVaultResourceId.substring(glacierVaultName.length());

        return GlacierArchiveManager.theManager().getArchiveRecord(glacierVaultName,resourceId);

    }


    public void addArchiveRecord(GlacierRecord gar) throws IOException {

        String vaultName =  gar.getVaultName();
        GlacierVaultManager gvm = makeVaultManagerIfNeeded(vaultName);
        gvm.cacheArchiveRecord(gar);
    }

    public GlacierVaultManager makeVaultManagerIfNeeded(String vaultName) throws IOException {
        GlacierVaultManager gvm =  _vaults.get(vaultName);

        if(gvm==null){
            gvm = new GlacierVaultManager(vaultName,getGlacierRootDir());
            _vaults.put(vaultName,gvm);

        }
        return gvm;
    }

    public GlacierVaultManager getVaultManager(String vaultName) {
        return  _vaults.get(vaultName);
    }

    public Index getIndex(String vaultName, String resourceId){
        GlacierVaultManager gvm =  getVaultManager(vaultName);
        if(gvm==null)
            return null;

        return gvm.getIndex(resourceId);
    }

    public Set<String> getVaultNames(){

        return _vaults.keySet();

    }



    public Index getIndex(String resourceId){

        String vaultName = getVaultName(resourceId);

        if(vaultName!= null){
            log.debug("getIndex() - Found vault name {} for resourceId {}. Trimming resourceId",vaultName,resourceId);
            resourceId = resourceId.substring(vaultName.length());
            log.debug("getIndex() - Vault adjusted resourceId: {}",resourceId);
            return getIndex(vaultName,resourceId);
        }

        return null;

    }

    public String getVaultName(String resourceId){
        String vaultName = null;
        for(String vName: _vaults.keySet()){
            if(resourceId.startsWith(vName)){
                if(vaultName==null){
                    vaultName = vName;
                }
                else if(vaultName.length() < vName.length()){
                    vaultName = vName;
                }
            }

        }
        return vaultName;
    }




    public GlacierRecord addS3ObjectToGlacier(Credentials glacierCreds, S3Object s3Object ) throws JDOMException, IOException {

        Logger log = LoggerFactory.getLogger(GlacierRecord.class);

        log.debug("-  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -");
        log.debug("Uploading S3Object to Glacier:  ");
        log.debug("S3 bucket name: {}", s3Object.getBucketName());
        log.debug("        S3 key: {}", s3Object.getKey());


        GlacierRecord gar;


        String vaultName  = s3Object.getBucketName();
        String resourceId = s3Object.getKey();

        GlacierVaultManager gvm = makeVaultManagerIfNeeded(vaultName);

        GlacierRecord cachedGar = gvm.getArchiveRecord(resourceId);
        if(cachedGar!=null){
            log.debug("Found cached archive record for  [vault: {}]  resourceId: {}",vaultName,resourceId);
            gar = cachedGar;
            // @todo Check last modified time of s3Object and of cached file. Update as needed (delete old glacier archive etc.)

        }
        else {
            log.debug("Retrieving S3 Object.");
            s3Object.cacheObjectToFile();
            File cacheFile = s3Object.getCacheFile();
            log.debug("Cache File:  " + cacheFile);
            log.debug("Cache file size: " + cacheFile.length() + " bytes");


            AmazonGlacierClient client = new AmazonGlacierClient(glacierCreds);
            client.setEndpoint(getGlacierEndpoint());

            ArchiveTransferManager atm = new ArchiveTransferManager(client, glacierCreds);

            log.debug("Transferring cache file content to Glacier. vault: {}  description: {}",vaultName,resourceId);
            UploadResult uploadResult = atm.upload(vaultName, resourceId, cacheFile);
            String archiveId = uploadResult.getArchiveId();
            gar = new GlacierRecord(vaultName,resourceId,archiveId);

            addArchiveRecord(gar);
            s3Object.deleteCacheFile();

        }

        return gar;
    }

    public void loadVaults() throws IOException, JDOMException {

        GlacierVaultManager gvm;

        File gRootDir = getGlacierRootDir();
        log.debug("loadVaults(): getGlacierRootDir",getGlacierRootDir());

        File[] vaults = gRootDir.listFiles((FileFilter) HiddenFileFilter.VISIBLE);
        if (vaults != null) {
            log.debug("loadVaults(): Got {} vaults",vaults.length);

            for (File vault : vaults) {

                if(!vault.getName().equals(DefaultResourceCacheDirectoryName)){

                    String vaultName = vault.getName();
                    log.debug("loadVaults(): Loading vault: {} vaultName: {}",vault, vaultName);

                    if (vault.isDirectory()) {

                        gvm = new GlacierVaultManager(vaultName,gRootDir);
                        gvm.setParentContext(getGlacierServiceContext());
                        gvm.loadArchiveRecords();
                        gvm.loadIndexObjects();
                        _vaults.put(gvm.name(),gvm);
                    }
                }

            }
        }


    }





    public void destroy() {
        DownloadManager.theManager().destroy();

    }


}
