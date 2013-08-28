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

import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import opendap.aws.auth.Credentials;
import opendap.aws.s3.S3Object;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/17/13
 * Time: 7:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierArchiveRecord  {

    private Logger log = null;

    //private String _glacierArchiveCacheRoot = null;

    private String _vaultName = null;
    private String _archiveId = null;

    private String _resourceId;

    private Element _archiveRecordElement =  null;

    private Vector<Element> _metadata = null;


    public static final Namespace glacierRecordNameSpace = Namespace.getNamespace("gar","http://xml.opendap.org/ns/aws/glacier/ArchiveRecord/01#");


    public GlacierArchiveRecord(String vaultName, String resourceId, String archiveId) {
        super();
        log = LoggerFactory.getLogger(getClass());

        setVaultName(vaultName);
        setResourceId(resourceId);
        setArchiveId(archiveId);
        _metadata = new Vector<Element>();
    }


    public GlacierArchiveRecord(File archiveRecordFile) throws IOException, JDOMException {
        super();
        log = LoggerFactory.getLogger(getClass());
        _metadata = new Vector<Element>();
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

    public void addMetaDataElement(Element metadata){
        _metadata.add(metadata);
    }

    public Element[] getMetaDataElements(){
        Element[] metadata = new Element[_metadata.size()];
        metadata = _metadata.toArray(metadata);
        return metadata;
    }



    /**
     * <gar:GlacierArchiveRecord
     *         xmlns:gar="http://xml.opendap.org/ns/aws/glacier/ArchiveRecord/01#"
     *         resourceId="/gdr/cycle097/JA2_GPN_2PdP097_019_20110219_155025_20110219_164637.nc"
     *         vault="foo-s3cmd.nodc.noaa.gov"
     *         archiveId="DUApQbY05dAB50FCci6NFTVkp0MpswEg_YwcYyy7x9Jn1UohMOEywbj1iMuXHNu53HKTRX1kMTNJUEbxDazAciwk5CvBCHkx66khGPxKm2TcHGsLNByPgNH6jOWroN5Yg5V9tdb9Og" />
     * @return
     */
    public Element getArchiveRecordElement(){
        Element index = new Element("GlacierArchiveRecord", glacierRecordNameSpace);

        index.setAttribute("resourceId",getResourceId());
        index.setAttribute("vault",getVaultName());
        index.setAttribute("archiveId",getArchiveId());

        Element[] metadataElements = getMetaDataElements();

        if(metadataElements.length>0){
            for(Element mde: metadataElements)
                index.addContent(mde);
        }

        return index;

    }



    /**
     * <gar:GlacierArchiveRecord
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
        List children  = archiveRecordElement.getChildren();
        for(Object o: children){
            Element metadataElement = (Element)o;
            metadataElement.detach();
            addMetaDataElement(metadataElement);
        }

    }






}
