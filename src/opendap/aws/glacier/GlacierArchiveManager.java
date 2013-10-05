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
import opendap.aws.auth.Credentials;
import opendap.aws.s3.S3Object;
import opendap.bes.BadConfigurationException;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
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

    public static final String CONFIG_ELEMENT_GLACIER_ENDPOINT = "GlacierEndpoint";
    public static final String CONFIG_ELEMENT_GLACIER_ARCHIVE_ROOT = "GlacierArchiveRootDirectory";


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


        Element glacierEndpointElement = config.getChild(CONFIG_ELEMENT_GLACIER_ENDPOINT);
        if(glacierEndpointElement==null || glacierEndpointElement.getTextTrim().equals(""))
            throw new ConfigurationException("Configuration must contain a Glacier Endpoint URL");
        URL glacierEndpointUrl = new URL(glacierEndpointElement.getTextTrim());
        setGlacierEndpoint(glacierEndpointUrl.toString());


        Element glacierArciveRootDirElement = config.getChild(CONFIG_ELEMENT_GLACIER_ARCHIVE_ROOT);
        if(glacierArciveRootDirElement==null || glacierArciveRootDirElement.getTextTrim().equals(""))
            throw new ConfigurationException("Configuration must identify a top level directory for the Glacier archive using a GlacierArchiveRootDirectory element. ");
        setGlacierArchiveRootDirectory(glacierArciveRootDirElement.getTextTrim());

        setParentContext("");

        loadVaults();
        DownloadManager.theManager().init(getResourceCacheDir());


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
        String dapServiceForVault =  getDapServiceContext() +_pathDelimiter+ getVaultName(resourceId);

        return dapServiceForVault ;
    }

    public String getCatalogServiceContext(){
        return _catalogServiceContext ;
    }

    public String getCatalogServiceContext(String resourceId){

        String catalogServiceForVault =  getCatalogServiceContext() +_pathDelimiter+ getVaultName(resourceId);

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


    public GlacierRecord getArchiveRecord(String vaultName, String resourceId) throws IOException, JDOMException {
        GlacierVaultManager gvm =  getVaultManager(vaultName);
        if(gvm==null)
            return null;

        return gvm.getArchiveRecord(resourceId);
    }

    public GlacierRecord getArchiveRecord(String combinedVaultResourceId) throws IOException, JDOMException {
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
                        // gvm.loadArchiveRecords();
                        gvm.loadIndexObjects();
                        _vaults.put(gvm.name(),gvm);
                    }
                }

            }
        }


    }

    public static Element getDefaultConfig(String glacierEndpoint, String glacierArchiveRootDir){



        Element  glacierConfig = new Element("GlacierService");


        Element e;


        e = new Element(GlacierArchiveManager.CONFIG_ELEMENT_GLACIER_ENDPOINT);
        e.setText(glacierEndpoint);
        glacierConfig.addContent(e);

        e = new Element(GlacierArchiveManager.CONFIG_ELEMENT_GLACIER_ARCHIVE_ROOT);
        e.setText(glacierArchiveRootDir);
        glacierConfig.addContent(e);




        return glacierConfig;
    }





    public void destroy() {
        DownloadManager.theManager().destroy();

    }


}
