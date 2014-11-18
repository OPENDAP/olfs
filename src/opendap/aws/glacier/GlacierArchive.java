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
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    private Date _archiveCreationDate;
    private long _size;
    private String _sha256TreeHash;

    private String _ddx;
    private String _das;
    private String _dds;

    private File _cacheFile;

    public static final String DDS = "DDS";
    public static final String DAS = "DAS";
    public static final String DDX = "DDX";

    public static String AWS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";


    public static final Namespace GlacierRecordNameSpace = Namespace.getNamespace("gar","http://xml.opendap.org/ns/aws/glacier/ArchiveRecord/01#");

    public GlacierArchive() {
        super();
        _log = LoggerFactory.getLogger(getClass());

        _size           = -1;
        _vaultName      = null;
        _archiveId      = null;
        _resourceId     = null;
        _archiveCreationDate = null;
        _sha256TreeHash = null;
        _cacheFile      = null;
        _ddx            = null;
        _dds            = null;
        _das            = null;

    }


    public GlacierArchive(String vaultName, String resourceId, String archiveId) {
        this();
        setVaultName(vaultName);
        setResourceId(resourceId);
        setArchiveId(archiveId);
    }




    public GlacierArchive(File archiveRecordFile) throws IOException, JDOMException {
        this();
        loadArchiveRecordFromFile(archiveRecordFile);
    }

    public String getVaultName(){
        return _vaultName;
    }
    public void setVaultName(String vaultName){
        _vaultName = vaultName;
    }

    /**
     *
     * @return  Glacier Archive ID - Mapped to resourceId
     */
    @JsonProperty("ArchiveId")
    public String getArchiveId(){
        return _archiveId;
    }

    public void setArchiveId(String archiveId){
        _archiveId = archiveId;
    }

    /**
     *
     * @return Glacier Archive Description - Mapped to resourceId
     */
    @JsonProperty("ArchiveDescription")
    public String getArchiveDescription(){
        return _resourceId;
    }
    public void setArchiveDescription(String archiveDescription){
        _resourceId = archiveDescription;
    }


    /**
     *
     * @return   Creation date(in Glacier land) of archive.
     */
    @JsonProperty("CreationDate")
    public String getCreationDateString(){

        if(_archiveCreationDate==null)
            return null;

        SimpleDateFormat sdf = new SimpleDateFormat(AWS_DATE_FORMAT);
        return sdf.format(_archiveCreationDate);
    }

    public void setCreationDateString(String d){
        _log.debug("setCreationDateString({})",d);
        if(d!=null){
           //  2013-10-08T21:04:35Z
            SimpleDateFormat sdf = new SimpleDateFormat(AWS_DATE_FORMAT);
            _archiveCreationDate = sdf.parse(d, new ParsePosition(0));
        }
        else
            _archiveCreationDate = null ;
    }


    @JsonIgnore
    public Date getCreationDate(){
        return _archiveCreationDate;
    }

    public void setCreationDateDate(Date d){
        _log.debug("setCreationDate({})",d);
        _archiveCreationDate = new Date(d.getTime()) ;
    }


    /**
     *
     * @return   Size of archive in bytes.
     */
    @JsonProperty("Size")
    public long getSize(){
        return _size;
    }

    public void setSize(long size){
        _size = size;
    }


    /**
     *
     * @return   The SHA256TreeHash for the archived object.
     */
    @JsonProperty("SHA256TreeHash")
    public String getSHA256TreeHash() {
        return _sha256TreeHash;
    }

    public void setSHA256TreeHash(String sha256TreeHash) {
        _sha256TreeHash = sha256TreeHash;
    }

    @JsonIgnore
    public Element getDDXElement(){
        Element ddxElement = new Element(DDX,GlacierRecordNameSpace);
        ddxElement.setText(_ddx);
        return ddxElement;
    }
    public String getDDX(){
        return _ddx;
    }
    public void setDDX(String ddx){
            _ddx = ddx;
    }


    @JsonIgnore
    public Element getDDSElement(){
        Element ddsElement = new Element(DDS,GlacierRecordNameSpace);
        ddsElement.setText(_dds);
        return ddsElement;
    }
    public String getDDS(){
        return _dds;
    }
    public void setDDS(String dds){
            _dds = dds;
    }


    @JsonIgnore
    public Element getDASElement(){
        Element dasElement = new Element(DAS,GlacierRecordNameSpace);
        dasElement.setText(_das);
        return dasElement;
    }
    public String getDAS(){
        return _das;
    }
    public void setDAS(String das){
            _das = das;
    }


    /**
     *
     * @return
     */
    public String getResourceId(){
        return _resourceId;
    }

    public void setResourceId(String resourceId){
        _resourceId = resourceId;
    }






    /**
     * <gar:GlacierArchive
     *         xmlns:gar="http://xml.opendap.org/ns/aws/glacier/ArchiveRecord/01#"
     *         resourceId="/gdr/cycle097/JA2_GPN_2PdP097_019_20110219_155025_20110219_164637.nc"
     *         vault="foo-s3cmd.nodc.noaa.gov"
     *         archiveId="DUApQbY05dAB50FCci6NFTVkp0MpswEg_YwcYyy7x9Jn1UohMOEywbj1iMuXHNu53HKTRX1kMTNJUEbxDazAciwk5CvBCHkx66khGPxKm2TcHGsLNByPgNH6jOWroN5Yg5V9tdb9Og" />
     * @return
     */
    @JsonIgnore
    public Element getArchiveRecordElement(){
        Element glacierRecord = new Element("GlacierArchive", GlacierRecordNameSpace);

        glacierRecord.setAttribute("resourceId", getResourceId());
        if(getVaultName()!=null)
            glacierRecord.setAttribute("vault", getVaultName());
        glacierRecord.setAttribute("archiveId", getArchiveId());

        if(_cacheFile!=null)
            glacierRecord.setAttribute("cacheFile",getCacheFile().getAbsolutePath());

        if(_archiveCreationDate!=null)
            glacierRecord.setAttribute("creationDate", getCreationDateString());


        Element e = getDDXElement();
        if(e!=null){
            glacierRecord.addContent(e);

        }
        e = getDDSElement();
        if(e!=null){
            glacierRecord.addContent(e);

        }
        e = getDASElement();
        if(e!=null){
            glacierRecord.addContent(e);

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
    @JsonIgnore
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
            setDDS(e.getTextTrim());
        }

        e = archiveRecordElement.getChild(DAS,GlacierRecordNameSpace);
        if(e!=null){
            setDAS(e.getTextTrim());
        }

        e = archiveRecordElement.getChild(DDX,GlacierRecordNameSpace);
        if(e!=null){
            setDDX(e.getTextTrim());
        }



    }

    @JsonIgnore
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

    @JsonIgnore
    public long getCachedResourceLastModifiedTime(){

        File cacheFile = getCacheFile();

        if(cacheFile== null)
            return -1;

        return cacheFile.lastModified();

    }

    public String toString(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);

        try {
            return mapper.writeValueAsString(this);
        }
        catch (Exception e){
            e.printStackTrace();
            return e.toString();
        }


    }



    public static void main(String[] args)  {


        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);


            GlacierArchive gar = mapper.readValue(new File("single-archive.json"), GlacierArchive.class);

            System.out.println(gar.toString());


            File archiveFile = new File("/Users/ndp/scratch/glacier/foo-s3cmd.nodc.noaa.gov/archive/#2Fgdr#2Fcycle097#2FJA2_GPN_2PdP097_149_20110224_173818_20110224_183431.nc");

            gar = new GlacierArchive(archiveFile);

            System.out.println(gar.toString());


            gar = mapper.readValue(gar.toString(),GlacierArchive.class);

            System.out.println(gar.toString());


        }
        catch (Exception e){
            e.printStackTrace();
        }


    }


}
