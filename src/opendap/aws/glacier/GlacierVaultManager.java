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

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/19/13
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierVaultManager {

   Logger log = LoggerFactory.getLogger(this.getClass());


    private String _name;

    private String _parentContext;
    private String _serviceContext;
    private String _pathDelimiter = "/";

    public static final String IndexDirectoryName = "index";
    private File _indexDirectory;

    public static final String ArchiveRecordsDirectoryName = "archive";
    private File _archiveRecordsDirectory;

    public static final String ResourceCacheDirectoryName = "cache";
    private File _resourceCacheDirectory;


    private ConcurrentHashMap<String, GlacierArchiveRecord> _archiveRecords;
    private ConcurrentHashMap<String, Index> _indexObjects;


    public GlacierVaultManager(String vaultName, File glacierRootDir) throws IOException {

        if(vaultName ==null){
            throw new IOException("Vault name was null!");
        }

        _name = vaultName;
        File vaultDir = mkDir(glacierRootDir,_name);

        _indexDirectory = mkDir(vaultDir,IndexDirectoryName);
        _archiveRecordsDirectory = mkDir(vaultDir,ArchiveRecordsDirectoryName);
        _resourceCacheDirectory = mkDir(vaultDir,ResourceCacheDirectoryName);

        _archiveRecords = new ConcurrentHashMap<String, GlacierArchiveRecord>();
        _indexObjects   = new ConcurrentHashMap<String, Index>();
    }


    public String getServiceContext(){

        return _serviceContext;

    }

    public void setParentContext(String parentContext){
        _parentContext = parentContext;
        String myContext = _parentContext + _pathDelimiter + name();
        _serviceContext = myContext;

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


    public void addArchiveRecord(GlacierArchiveRecord gar) throws IOException {


        File targetFile = new File(getArchiveRecordsDir(), AwsUtil.encodeKeyForFileSystemName(gar.getResourceId()));
        log.debug("addArchiveRecord() - targetFile: '{}'",targetFile);

        if(targetFile.exists()){
            log.warn("addArchiveRecord() - OVERWRITING RESOURCE ARCHIVE RECORD: '{}'", targetFile);

        }
        else {
            File parent = targetFile.getParentFile();

            if(!parent.exists() && !parent.mkdirs()){
                throw new IOException("Couldn't create the parent directory: " + parent);
            }

            log.debug("Attempting to create target file: '{}'",targetFile.getAbsolutePath());
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


    public GlacierArchiveRecord getArchiveRecord(String resourceId){
        return _archiveRecords.get(resourceId);
    }

    public Index getIndex(String resourceId){
        return _indexObjects.get(resourceId);
    }


    public void loadArchiveRecords() throws IOException, JDOMException {

        GlacierArchiveRecord gar;

        File archiveDir = getArchiveRecordsDir();
        File[] archiveRecords = archiveDir.listFiles((FileFilter) HiddenFileFilter.VISIBLE);

        if (archiveRecords != null) {
            for (File archiveRecord : archiveRecords) {
                if (archiveRecord.isFile()) {
                    gar = new GlacierArchiveRecord(archiveRecord);


                    String resourceId = getServiceContext() + gar.getResourceId();

                    _archiveRecords.put(resourceId,gar);
                    log.debug("Loaded Glacier Archive Record. vault: {} resourceId: {}", name(), resourceId);

                }
                else {
                    log.debug("Skipping directory/link {}", archiveRecord);
                }
            }
        } else {
            log.debug("No archive records found for vault {}", name());
        }

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
                            .append(getServiceContext())
                            .append(index.getPath())
                            .append(pathDelimiter)
                            .append(index.getIndexFileConvention());

                    index.setResourceId(resourceId.toString());
                    _indexObjects.put(index.getResourceId(),index);
                    log.debug("Loaded Index. Vault: {} resourceId: {}", name(), index.getResourceId());

                }
                else {
                    log.debug("Skipping directory/link {}", indexFile);
                }
            }
        } else {
            log.debug("No index files found for vault {}", name());
        }

    }



}
