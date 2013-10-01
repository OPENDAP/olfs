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
import opendap.aws.auth.Credentials;
import opendap.aws.s3.S3Object;
import opendap.coreServlet.Util;
import opendap.noaa_s3.S3Index;
import opendap.noaa_s3.S3IndexedFile;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 7/17/13
 * Time: 1:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoaaResourceIngester {


    public static void main(String[] args)  {




        System.out.println("===========================================");

        String s3CacheRoot = "/Users/ndp/scratch/s3Test";
        //String noaaS3BucketName = "ocean-archive.data.nodc.noaa.gov";
        String noaaS3BucketName = "foo-s3cmd.nodc.noaa.gov";


        try {
            GlacierArchiveManager.theManager().init(null);


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
                GlacierRecord gar = GlacierArchiveManager.theManager().addS3ObjectToGlacier(creds, resource);

                Document garDoc = gar.getArchiveRecordDocument();
                System.out.println();
                xmlo.output(garDoc,System.out);
                System.out.println();

            }






            System.out.println(Util.getMemoryReport());




        } catch (Exception e) {
            e.printStackTrace();
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

}
