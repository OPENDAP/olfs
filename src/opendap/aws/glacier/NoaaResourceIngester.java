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
import com.amazonaws.services.glacier.model.*;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import opendap.aws.auth.Credentials;
import opendap.aws.s3.S3Object;
import opendap.coreServlet.Util;
import opendap.noaa_s3.S3Index;
import opendap.noaa_s3.S3IndexedFile;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/17/13
 * Time: 1:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoaaResourceIngester {



    public static Element getSimpleGlacierArchiveConfig(String glacierEndpoint, String glacierArchiveRootDir){
        Element  glacierConfig = new Element("config");


        Element e;


        e = new Element(GlacierArchiveManager.CONFIG_ELEMENT_GLACIER_ENDPOINT);
        e.setText(glacierEndpoint);
        glacierConfig.addContent(e);

        e = new Element(GlacierArchiveManager.CONFIG_ELEMENT_GLACIER_ARCHIVE_ROOT);
        e.setText(glacierArchiveRootDir);
        glacierConfig.addContent(e);




        return glacierConfig;
    }



    public static void main(String[] args)  {




        System.out.println("===========================================");

        String s3CacheRoot        = "/Users/ndp/scratch/s3Test";
        //String noaaS3BucketName = "ocean-archive.data.nodc.noaa.gov";
        String noaaS3BucketName   = "foo-s3cmd.nodc.noaa.gov";

        String glacierEndpoint    = "https://glacier.us-east-1.amazonaws.com/";
        String glacierArchiveRoot =  "/Users/ndp/scratch/glacier";

        String besInstallPrefix          = "/Users/ndp/hyrax/trunk";

        BesMetadataExtractor.init(besInstallPrefix);



        Element glacierConfig = getSimpleGlacierArchiveConfig(glacierEndpoint,glacierArchiveRoot);






        try {
            GlacierArchiveManager.theManager().init(glacierConfig);


            S3Index topLevelIndex = new S3Index(noaaS3BucketName,"//index.xml",s3CacheRoot);
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

            System.out.println("Loaded Top Level Index: "+topLevelIndex.getResourceUrl());
            System.out.println("Content: ");
            xmlo.output(topLevelIndex.getIndexElement(), System.out);

            System.out.println(Util.getMemoryReport());



            topLevelIndex.updateCachedIndexAsNeeded(true, 3);
            Vector<S3IndexedFile> resourceObjects = topLevelIndex.getChildIndexedFiles(true, 3);



            System.out.println("Located " +resourceObjects.size()+" resource objects.");
            //topLevelIndex.updateCachedIndexAsNeeded(true,0);

            Credentials creds =  new Credentials();

            //AmazonGlacierClient client = new AmazonGlacierClient(creds);
            //client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");

            inspectVaults();

            //CreateVaultRequest request = new CreateVaultRequest().withAccountId("-").withVaultName(topLevelIndex.getVaultName());
            //CreateVaultResult result = client.createVault(request);




            for(S3Object resource : resourceObjects){
                GlacierRecord gar = addS3ObjectToGlacier(creds, glacierEndpoint, resource);

                Document garDoc = gar.getArchiveRecordDocument();
                System.out.println();
                xmlo.output(garDoc,System.out);
                System.out.println();

            }




            System.out.println(Util.getMemoryReport());

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            BesMetadataExtractor.destroy();
        }

    }


    public static void inspectVaults(){

        Credentials creds =  new Credentials();

        AmazonGlacierClient client = new AmazonGlacierClient(creds);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");

        ListVaultsResult listVaultsResult =  client.listVaults(new ListVaultsRequest());
        for(DescribeVaultOutput dvo : listVaultsResult.getVaultList() ){
            System.out.println(describeVault(dvo));

        }



    }



    public static String describeVault(DescribeVaultOutput dvo){
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("Found Vault: ").append(dvo.getVaultName()).append("\n");
        sb.append("    getCreationDate(): ").append(dvo.getCreationDate()).append("\n");
        sb.append("    getLastInventoryDate(): ").append(dvo.getLastInventoryDate()).append("\n");
        sb.append("    getNumberOfArchives(): ").append(dvo.getNumberOfArchives()).append("\n");
        sb.append("    getSizeInBytes(): ").append(dvo.getSizeInBytes()).append("\n");
        sb.append("    getVaultARN(): ").append(dvo.getVaultARN()).append("\n");
        sb.append("    toString(): ").append(dvo.toString()).append("\n");
        sb.append("    toString(): ").append(dvo.getVaultName()).append("\n");
        return sb.toString();
    }




    public static GlacierRecord addS3ObjectToGlacier(Credentials glacierCreds, String glacierEndpPoint, S3Object s3Object ) throws JDOMException, IOException {


        System.out.println("-  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -");
        System.out.println("Uploading S3Object to Glacier:  ");
        System.out.println("S3 bucket name: " + s3Object.getBucketName());
        System.out.println("        S3 key: " + s3Object.getKey());


        GlacierRecord gar;


        String vaultName  = s3Object.getBucketName();
        String resourceId = s3Object.getKey();

        GlacierVaultManager gvm = GlacierArchiveManager.theManager().makeVaultManagerIfNeeded(vaultName);

        GlacierRecord cachedGar = gvm.getArchiveRecord(resourceId);
        if(cachedGar!=null){
            System.out.println("Found cached archive record for  [vault: "+vaultName+"]  resourceId: " + resourceId);
            gar = cachedGar;
            // @todo Check last modified time of s3Object and of cached file. Update as needed (delete old glacier archive etc.)

        }
        else {
            System.out.println("Retrieving S3 Object.");
            s3Object.cacheObjectToFile();
            File cacheFile = s3Object.getCacheFile();
            System.out.println("Cache File:  " + cacheFile);
            System.out.println("Cache file size: " + cacheFile.length() + " bytes");

            File targetDir = s3Object.getCacheDir();
            gar = new GlacierRecord(vaultName,resourceId,"NOT_UPLOADED_TO_GLACIER");

            try {
                BesMetadataExtractor.extractMetadata(gar,targetDir, cacheFile);
                System.out.println("Got metadata for " + gar.getResourceId());
            } catch (Exception e) {
                System.out.println("ERROR: Failed to extract metadata from "+cacheFile.getName()+". Msg: " + e.getMessage());
                e.printStackTrace();
            }


            AmazonGlacierClient client = new AmazonGlacierClient(glacierCreds);
            client.setEndpoint(glacierEndpPoint);

            ArchiveTransferManager atm = new ArchiveTransferManager(client, glacierCreds);

            System.out.println("Transferring cache file content to Glacier. vault: "+vaultName+"  description: " + resourceId);
            UploadResult uploadResult = atm.upload(vaultName, resourceId, cacheFile);
            String archiveId = uploadResult.getArchiveId();

            gar.setArchiveId(archiveId);

            GlacierArchiveManager.theManager().addArchiveRecord(gar);
            s3Object.deleteCacheFile();

        }

        return gar;
    }



}
