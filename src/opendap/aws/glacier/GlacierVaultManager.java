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

import opendap.aws.AwsUtil;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/19/13
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierVaultManager {

   Logger _log = LoggerFactory.getLogger(this.getClass());


    private String _name;

    private String _parentContext;
    private String _serviceContext;
    private String _pathDelimiter = "/";

    public  static final String DefaultIndexDirectoryName = "index";
    private File _indexDirectory;

    public  static final String DefaultArchiveRecordsDirectoryName = "archive";
    private File _archiveRecordsDirectory;

    public  static final String DefaultResourceCacheDirectoryName = "cache";
    private File _resourceCacheDirectory;


    private File _vaultInventory;



    public static int DEFAULT_MAX_RECORDS_IN_MEMORY = 100;

    private int _max_records;



    //private ConcurrentHashMap<String, GlacierArchive> _archiveRecords;
    private ConcurrentHashMap<String, Index> _indexObjects;


    private ConcurrentHashMap<String, ResourceId> _resourceIds;
    private ConcurrentSkipListMap<ResourceId, GlacierArchive> _glacierRecords;


    public GlacierVaultManager(String vaultName, File glacierRootDir) throws IOException {

        if(vaultName ==null){
            throw new IOException("Vault name was null!");
        }

        _name = vaultName;
        File vaultDir = mkDir(glacierRootDir,_name);

        _indexDirectory = mkDir(vaultDir,DefaultIndexDirectoryName);
        _archiveRecordsDirectory = mkDir(vaultDir,DefaultArchiveRecordsDirectoryName);
        _resourceCacheDirectory = mkDir(glacierRootDir,DefaultResourceCacheDirectoryName);

        _resourceIds = new ConcurrentHashMap<String, ResourceId>();
        _glacierRecords = new ConcurrentSkipListMap<ResourceId, GlacierArchive>();

        _indexObjects   = new ConcurrentHashMap<String, Index>();

        _vaultInventory = new File(_resourceCacheDirectory,_name+"-INVENTORY.json");

        _max_records = DEFAULT_MAX_RECORDS_IN_MEMORY;
    }


    public String getServiceContext(){

        return _serviceContext;

    }

    public void setParentContext(String parentContext){
        _parentContext = parentContext;
        String myContext = _parentContext + _pathDelimiter + name();
        _serviceContext = myContext;

    }


    public File getInventory() {
        return new File(_vaultInventory.getAbsolutePath());
    }



    public String name(){
        return _name;
    }


    public File getIndexDir() throws IOException {
        if(_indexDirectory ==null)
            throw new IOException("Index directory was null valued.");

        return new File(_indexDirectory.getCanonicalPath());
    }

    public File getArchiveRecordsDir() throws IOException {
        if(_archiveRecordsDirectory ==null)
            throw new IOException("Archive Records directory was null valued.");

        return new File(_archiveRecordsDirectory.getCanonicalPath());
    }

    public File getResourceCacheDir() throws IOException {
        if(_resourceCacheDirectory ==null)
            throw new IOException("Resource Cache directory was null valued.");

        return new File(_resourceCacheDirectory.getCanonicalPath());
    }




    public File mkDir(File parent, String dirName) throws IOException {

        File newDir = new File(parent,dirName);
        if(!newDir.exists() && !newDir.mkdirs()){
            throw new IOException("Unable to create directory: "+newDir);
        }
        if(!newDir.canWrite()){
            throw new IOException("Unable to write to directory: "+newDir);
        }
        return newDir;

    }




    public void cacheArchiveRecord(GlacierArchive gar) throws IOException {


        File targetFile = new File(getArchiveRecordsDir(), AwsUtil.encodeKeyForFileSystemName(gar.getResourceId()));
        _log.debug("cacheArchiveRecord() - targetFile: '{}'", targetFile);

        if(targetFile.exists()){
            _log.warn("cacheArchiveRecord() - OVERWRITING RESOURCE ARCHIVE RECORD: '{}'", targetFile);

        }
        else {
            File parent = targetFile.getParentFile();

            if(!parent.exists() && !parent.mkdirs()){
                throw new IOException("Couldn't create the parent directory: " + parent);
            }

            _log.debug("Attempting to create target file: '{}'", targetFile.getAbsolutePath());
            targetFile.createNewFile();


        }

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        FileOutputStream fis = null;
        try {
            fis = new FileOutputStream(targetFile);
            xmlo.output(gar.getArchiveRecordDocument(),fis);
        }
        finally {
            if(fis!=null)
                fis.close();
        }

    }


    public GlacierArchive getArchiveRecord(String resourceId) throws IOException, JDOMException {
        ResourceId id = _resourceIds.get(resourceId);
        if(id==null)
            return loadArchiveRecord(resourceId);

        id.updateLastAccessed();
        return _glacierRecords.get(id);
    }

    public Index getIndex(String resourceId){
        return _indexObjects.get(resourceId);
    }



    /*
    public void loadArchiveRecords() throws IOException, JDOMException {

        GlacierArchive gar;

        File archiveDir = getArchiveRecordsDir();
        File[] archiveRecords = archiveDir.listFiles((FileFilter) HiddenFileFilter.VISIBLE);

        if (archiveRecords != null) {
            for (File archiveRecord : archiveRecords) {
                if (archiveRecord.isFile()) {
                    gar = new GlacierArchive(archiveRecord);

                    gar.createCacheFile(_resourceCacheDirectory);

                    ResourceId resourceId = new ResourceId(gar.getResourceId());
                    _resourceIds.put(resourceId.toString(), resourceId);
                    _glacierRecords.put(resourceId,gar);
                    _log.debug("Loaded Glacier Archive Record. vault: {} resourceId: {}", name(), resourceId);

                }
                else {
                    _log.debug("Skipping directory/link {}", archiveRecord);
                }
            }
        } else {
            _log.debug("No archive records found for vault {}", name());
        }

    }

*/

    private GlacierArchive loadArchiveRecord(String resourceId) throws IOException, JDOMException {

        GlacierArchive gar;

        String baseFileName = AwsUtil.encodeKeyForFileSystemName(resourceId);

        File archiveRecord = new File(getArchiveRecordsDir(),baseFileName);

        if (archiveRecord.isFile()) {
            gar = new GlacierArchive(archiveRecord);

            gar.createCacheFile(_resourceCacheDirectory);

            ResourceId rId = new ResourceId(gar.getResourceId());

            while(_resourceIds.size() >= _max_records){
                _log.debug("loadArchiveRecord() - Max Records limit reached. Unloading Glacier Archive Record. vault: {} resourceId: {}", name(), resourceId);
                ResourceId mostStaleId = _glacierRecords.firstKey();
                _resourceIds.remove(mostStaleId.getId());
                _glacierRecords.remove(mostStaleId);
            }

            _resourceIds.put(rId.toString(), rId);
            _glacierRecords.put(rId,gar);
            _log.debug("loadArchiveRecord() - Loaded Glacier Archive Record. vault: {} resourceId: {}", name(), resourceId);
            return gar;

        }

        _log.warn("loadArchiveRecord() - Could not locate Glacier Archive Record. vault: {} resourceId: {}", name(), resourceId);


        return null;
    }




    public void loadIndexObjects() throws IOException, JDOMException {

        Index index;

        File indexDir = getIndexDir();
        File[] indexFiles = indexDir.listFiles((FileFilter) HiddenFileFilter.VISIBLE);

        if (indexFiles != null) {
            for (File indexFile : indexFiles) {
                if (indexFile.isFile()) {
                    index = new Index(indexFile);

                    StringBuilder resourceId = new StringBuilder();
                    String pathDelimiter = index.getDelimiter();

                    resourceId
                            //.append(getGlacierServiceContext())
                            .append(index.getPath())
                            .append(pathDelimiter)
                            .append(index.getIndexFileConvention());

                    index.setResourceId(resourceId.toString());
                    _indexObjects.put(index.getResourceId(),index);
                    _log.debug("Loaded Index. Vault: {} resourceId: {}", name(), index.getResourceId());

                }
                else {
                    _log.debug("Skipping directory/link {}", indexFile);
                }
            }
        } else {
            _log.debug("No index files found for vault {}", name());
        }

    }






}
