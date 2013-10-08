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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.*;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import opendap.aws.auth.Credentials;
import opendap.aws.s3.S3Object;
import opendap.coreServlet.Util;
import opendap.noaa_s3.S3Index;
import opendap.noaa_s3.S3IndexedFile;
import org.apache.commons.cli.*;
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


    private static String s3Root = null;

    private static String s3BucketName = null;

    private static String glacierEndpointUrl =  null;

    private static String glacierArchiveRoot = null;

    private static String besInstallPrefix = null;

    private static boolean useDefaults = false;

    private static String awsAccessKeyId = null;
    private static String awsSecretKey = null;

    private static boolean processCommandline(String[] args) throws ParseException {

        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("h", "help", false, "Usage information.");

        options.addOption("i", "awsId", true, "AWS access key ID for working with Glacier.");
        options.addOption("k", "awsKey", true, "AWS secret key for working with Glacier.");



        options.addOption("s", "s3-root", true, "Top level directory for the S3 cache.");

        options.addOption("n", "s3-bucket-name", true, "Name of S3 bucket whose contents will be used to build a Glacier archive.");

        options.addOption("d", "use-defaults", false, "Use the baked in default values for everything (only works on my development machine). This overrides any other options set on the commandline.");


        options.addOption("e", "glacier-endpoint-url", true , "The Glacier endpoint URL.");

        options.addOption("a", "glacier-archive-root", true, "Top level directory for the Glacier Archive Database");
        // options.addOption("d", "dir", true, "Write eml files to this directory.");

        options.addOption("b", "bes-install-prefix", true, "The prefix used when installing the BES and libdap.");

        CommandLine line =   parser.parse(options, args);


        StringBuilder errorMessage = new StringBuilder();

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NoaaResourceIngester [options] --cache-name <name prefix>", options);
            return false;
        }

        useDefaults =  line.hasOption("use-defaults");
        if(!useDefaults) {
            s3Root = line.getOptionValue("s3-root");
            if(s3Root==null){
                errorMessage.append("Missing Parameter - You must provide an S3 cache directory with the --s3-root option.\n");
            }

            s3BucketName = line.getOptionValue("s3-bucket-name");
            if(s3BucketName==null){
                errorMessage.append("Missing Parameter - You must provide a S3 bucket name with the --s3-bucket-name option.\n");
            }

            glacierEndpointUrl = line.getOptionValue("glacier-endpoint-url");
            if(glacierEndpointUrl==null){
                errorMessage.append("Missing Parameter - You must provide a Glacier endpoint URL with the --glacier-endpoint-url option.\n");
            }

            glacierArchiveRoot = line.getOptionValue("glacier-archive-root");
            if(glacierArchiveRoot==null){
                errorMessage.append("Missing Parameter - You must provide a root directory for the Glacier Archive with the --glacier-archive-root option.\n");
            }

            besInstallPrefix =  line.getOptionValue("bes-install-prefix");
            if(s3BucketName==null){
                errorMessage.append("Missing Parameter - You must provide the local location of the BES and libdap with the --bes-install-prefix option.\n");
            }
        }
        awsAccessKeyId =  line.getOptionValue("awsId");
        if(awsAccessKeyId == null){
            errorMessage.append("Missing Parameter - You must provide an AWS access key ID (to access the Glacier service) with the --awsId option.\n");
        }

        awsSecretKey = line.getOptionValue("awsKey");
        if(awsSecretKey == null){
            errorMessage.append("Missing Parameter - You must provide an AWS secret key (to access the Glacier service) with the --awsKey option.\n");
        }


        if(errorMessage.length()!=0){

            System.err.println(errorMessage);

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NoaaResourceIngester -s S3Dir -n S3Bucket -e GlacierEndpointURL -a GlacierArchiveDir -b BESLocation", options);

            return false;
        }

        return true;

    }

    private static void defaults(){

        s3Root        = "/Users/ndp/scratch/s3Test";
        //String noaaS3BucketName = "ocean-archive.data.nodc.noaa.gov";
        s3BucketName   = "foo-s3cmd.nodc.noaa.gov";

        glacierEndpointUrl    = "https://glacier.us-east-1.amazonaws.com/";
        glacierArchiveRoot =  "/Users/ndp/scratch/glacier";

        besInstallPrefix          = "/Users/ndp/hyrax/trunk";

    }


    public static void main(String[] args)  {



        System.out.println("===========================================");




        try {

            if(processCommandline(args)){

                if(useDefaults)
                    defaults();

                BesMetadataExtractor.init(besInstallPrefix);

                Element glacierConfig = GlacierArchiveManager.getDefaultConfig(glacierEndpointUrl, glacierArchiveRoot,awsAccessKeyId,awsSecretKey);


                GlacierArchiveManager.theManager().init(glacierConfig);


                S3Index topLevelIndex = new S3Index(s3BucketName,"//index.xml",s3Root);
                XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

                System.out.println("Loaded Top Level Index: "+topLevelIndex.getResourceUrl());
                System.out.println("Content: ");
                xmlo.output(topLevelIndex.getIndexElement(), System.out);

                System.out.println(Util.getMemoryReport());



                topLevelIndex.updateCachedIndexAsNeeded(true, 3);
                Vector<S3IndexedFile> resourceObjects = topLevelIndex.getChildIndexedFiles(true, 3);



                System.out.println("Located " +resourceObjects.size()+" resource objects.");
                //topLevelIndex.updateCachedIndexAsNeeded(true,0);

                Credentials creds =  new Credentials(awsAccessKeyId,awsSecretKey);

                //AmazonGlacierClient client = new AmazonGlacierClient(creds);
                //client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");

                inspectVaults(creds);

                //CreateVaultRequest request = new CreateVaultRequest().withAccountId("-").withVaultName(topLevelIndex.getVaultName());
                //CreateVaultResult result = client.createVault(request);


                for(S3Object resource : resourceObjects){
                    GlacierRecord gar = addS3ObjectToGlacier(creds, glacierEndpointUrl, resource);

                    Document garDoc = gar.getArchiveRecordDocument();
                    System.out.println();
                    xmlo.output(garDoc,System.out);
                    System.out.println();

                }

            }
            System.out.println(Util.getMemoryReport());


        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            BesMetadataExtractor.destroy();
        }

    }


    public static void inspectVaults(AWSCredentials creds){


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
