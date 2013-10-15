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
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/17/13
 * Time: 7:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierArchive implements Serializable {

    private Logger _log = null;

    //private String _glacierArchiveCacheRoot = null;

    private String _vaultName = null;
    private String _archiveId = null;

    private String _resourceId;

    private ConcurrentHashMap<String, Element> _metadata = null;

    private File _cacheFile;

    public static final String DDS = "DDS";
    public static final String DAS = "DAS";
    public static final String DDX = "DDX";


    public static final Namespace GlacierRecordNameSpace = Namespace.getNamespace("gar","http://xml.opendap.org/ns/aws/glacier/ArchiveRecord/01#");


    public GlacierArchive(String vaultName, String resourceId, String archiveId) {
        super();
        _log = LoggerFactory.getLogger(getClass());

        setVaultName(vaultName);
        setResourceId(resourceId);
        setArchiveId(archiveId);
        _metadata = new ConcurrentHashMap<String, Element>();
    }


    public GlacierArchive(File archiveRecordFile) throws IOException, JDOMException {
        super();
        _log = LoggerFactory.getLogger(getClass());
        _metadata = new ConcurrentHashMap<String, Element>();
        loadArchiveRecordFromFile(archiveRecordFile);
    }

    public String getVaultName(){
        return _vaultName;
    }
    public void setVaultName(String vaultName){
        _vaultName = vaultName;
    }

    public String getArchiveId(){
        return _archiveId;
    }

    public void setArchiveId(String archiveId){
        _archiveId = archiveId;
    }
    public String getResourceId(){
        return _resourceId;
    }

    public void setResourceId(String resourceId){
        _resourceId = resourceId;
    }

    public void addMetaDataElement(String key, Element metadata){
        _metadata.put(key,metadata);
    }


    public Element getMetadataElement(String key){
        return _metadata.get(key);
    }

    private Element[] getMetaDataElements(){
        Element[] metadata = new Element[_metadata.size()];
        metadata = _metadata.values().toArray(metadata);
        return metadata;
    }



    /**
     * <gar:GlacierArchive
     *         xmlns:gar="http://xml.opendap.org/ns/aws/glacier/ArchiveRecord/01#"
     *         resourceId="/gdr/cycle097/JA2_GPN_2PdP097_019_20110219_155025_20110219_164637.nc"
     *         vault="foo-s3cmd.nodc.noaa.gov"
     *         archiveId="DUApQbY05dAB50FCci6NFTVkp0MpswEg_YwcYyy7x9Jn1UohMOEywbj1iMuXHNu53HKTRX1kMTNJUEbxDazAciwk5CvBCHkx66khGPxKm2TcHGsLNByPgNH6jOWroN5Yg5V9tdb9Og" />
     * @return
     */
    public Element getArchiveRecordElement(){
        Element glacierRecord = new Element("GlacierArchive", GlacierRecordNameSpace);

        glacierRecord.setAttribute("resourceId", getResourceId());
        glacierRecord.setAttribute("vault", getVaultName());
        glacierRecord.setAttribute("archiveId", getArchiveId());

        if(_cacheFile!=null)
            glacierRecord.setAttribute("cacheFile",getCacheFile().getAbsolutePath());

        Element[] metadataElements = getMetaDataElements();

        if(metadataElements.length>0){
            for(Element mde: metadataElements)
                glacierRecord.addContent((Element)mde.clone());
        }

        return glacierRecord;

    }



    /**
     * <gar:GlacierArchive
     *         xmlns:gar="http://xml.opendap.org/ns/aws/glacier/ArchiveRecord/01#"
     *         resourceId="/gdr/cycle097/JA2_GPN_2PdP097_019_20110219_155025_20110219_164637.nc"
     *         vault="foo-s3cmd.nodc.noaa.gov"
     *         archiveId="DUApQbY05dAB50FCci6NFTVkp0MpswEg_YwcYyy7x9Jn1UohMOEywbj1iMuXHNu53HKTRX1kMTNJUEbxDazAciwk5CvBCHkx66khGPxKm2TcHGsLNByPgNH6jOWroN5Yg5V9tdb9Og" />
     * @return
     */
    public Document getArchiveRecordDocument(){

        return new Document(getArchiveRecordElement());


    }


    private void loadArchiveRecordFromFile(File file) throws IOException, JDOMException {

        Element archiveRecordElement = opendap.xml.Util.getDocumentRoot(file);


        // Ingest vault name
        String vaultName  =  archiveRecordElement.getAttributeValue("vault");
        if(vaultName==null)
            throw new IOException("loadArchiveRecordFromFile() - The element " +
                    archiveRecordElement.getName() +
                    " is missing the required attribute 'vaultName'.");
        setVaultName(vaultName);



        // Ingest setResourceId
        String resourceId =  archiveRecordElement.getAttributeValue("resourceId");
        if(resourceId==null)
            throw new IOException("loadArchiveRecordFromFile() - The element " +
                    archiveRecordElement.getName() +
                    " is missing the required attribute 'resourceId'.");
        setResourceId(resourceId);


        // Ingest archiveId
        String archiveId  =  archiveRecordElement.getAttributeValue("archiveId");
        if(archiveId==null)
            throw new IOException("loadArchiveRecordFromFile() - The element " +
                    archiveRecordElement.getName() +
                    " is missing the required attribute 'archiveId'.");
        setArchiveId(archiveId);


        // Load Metadata Elements

        Element e;

        e = archiveRecordElement.getChild(DDS,GlacierRecordNameSpace);
        if(e!=null){
            e.detach();
            addMetaDataElement(DDS,e);
        }

        e = archiveRecordElement.getChild(DAS,GlacierRecordNameSpace);
        if(e!=null){
            e.detach();
            addMetaDataElement(DAS,e);
        }

        e = archiveRecordElement.getChild(DDX,GlacierRecordNameSpace);
        if(e!=null){
            e.detach();
            addMetaDataElement(DDX,e);
        }



    }


    public File getCacheFile(){
       return  _cacheFile;
    }



    public File createCacheFile(File resourceCacheDirectory) throws IOException {

        if(_cacheFile!=null)
            return _cacheFile;

        _log.debug("getCacheFile() - Cache Dir: '{}'",resourceCacheDirectory);

        String cacheFileName =  getVaultName() + getResourceId();
        cacheFileName = AwsUtil.encodeKeyForFileSystemName(cacheFileName);

        File cacheFile = new File(resourceCacheDirectory, cacheFileName);
        _log.debug("getCacheFile() - cacheFile: '{}'", cacheFile);

        if(cacheFile.exists() && !cacheFile.canWrite())
            throw new IOException("createCacheFile() - Unable to write to cache file: "+cacheFile.getPath());

        _cacheFile = cacheFile;

        return _cacheFile;

    }


    public boolean resourceIsCached() throws IOException {

        File cacheFile = getCacheFile();

        return cacheFile!=null && cacheFile.exists();

    }

    public long getCachedResourceLastModifiedTime(){

        File cacheFile = getCacheFile();

        if(cacheFile== null)
            return -1;

        return cacheFile.lastModified();




    }



}
