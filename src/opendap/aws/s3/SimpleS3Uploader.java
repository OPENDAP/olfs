/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2014 OPeNDAP, Inc.
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

package opendap.aws.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * Created by ndp on 8/27/14.
 */
public class SimpleS3Uploader {

    private AmazonS3 s3;

    private boolean verbose;

    //private String s3LocalCacheRoot;
    private String _s3BucketName;


    private String _awsAccessKeyId;
    private String _awsSecretKey;

    private String _targetUploadFile;
    private String _uploadFileKey;

    public SimpleS3Uploader(String bucket, String keyId, String key){
        _s3BucketName = bucket;
        _awsAccessKeyId = keyId;
        _awsSecretKey = key;
        initS3();
    }

    private  SimpleS3Uploader(){
        _s3BucketName     = null;
        _awsAccessKeyId   = null;
        _awsSecretKey     = null;
        _targetUploadFile = null;
        _uploadFileKey    = null;
    }

    private  boolean processCommandline(String[] args) throws Exception {

        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("h", "help", false, "Usage information.");

        options.addOption("i", "awsId", true, "AWS access key ID for working with S3.");
        options.addOption("s", "awsKey", true, "AWS secret key for working with S3.");

        options.addOption("v", "verbose", false, "Makes more output...");

//        options.addOption("s", "s3-root", true, "Top level directory for the S3 cache.");

        options.addOption("n", "s3-bucket-name", true, "Name of S3 bucket on which to operate.");

        options.addOption("f", "uploadFile", true, "Local file to upload to bucket.");
        options.addOption("k", "uploadKey", true, "S3 Key to associate with upload file.");
        options.addOption("B", "buildRepo", false, "Attempts to upload the test repository.");


        CommandLine line =   parser.parse(options, args);


        String usage  = this.getClass().getName()+" -i AWSAccessKeyID -k AWSSecretKey -n S3BucketName [-v] ";


        StringBuilder errorMessage = new StringBuilder();



        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            return false;
        }

        verbose = line.hasOption("verbose");

        /*
        s3LocalCacheRoot = line.getOptionValue("s3-root");
        if(s3LocalCacheRoot==null){
            errorMessage.append("Missing Parameter - You must provide an S3 cache directory with the --s3-root option.\n");
        }
        */

        _s3BucketName = line.getOptionValue("s3-bucket-name");
        if(_s3BucketName ==null){
            errorMessage.append("Missing Parameter - You must provide a S3 bucket name with the --s3-bucket-name option.\n");
        }



        _awsAccessKeyId =  line.getOptionValue("awsId");
        if(_awsAccessKeyId == null){
            errorMessage.append("Missing Parameter - You must provide an AWS access key ID (to access the S3 service) with the --awsId option.\n");
        }

        _awsSecretKey = line.getOptionValue("awsKey");
        if(_awsSecretKey == null){
            errorMessage.append("Missing Parameter - You must provide an AWS secret key (to access the S3 service) with the --awsKey option.\n");
        }

        _targetUploadFile = line.getOptionValue("uploadFile");
        if (_targetUploadFile !=null) {
            _uploadFileKey = line.getOptionValue("uploadKey");
            if (_uploadFileKey ==null) {
                errorMessage.append("Bad Parameter - You must provide an upload Key in conjunction with an upload file.\n");
            }
        }



        if(errorMessage.length()!=0){

            System.err.println(errorMessage);

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);

            return false;
        }

        initS3();


        if (line.hasOption("buildRepo")) {


            build_repo();
        }



        return true;

    }


    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
    private void initS3()  {
        System.out.println("initS3() - BEGIN");
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(_awsAccessKeyId, _awsSecretKey);
        s3  = new AmazonS3Client(basicAWSCredentials);
        System.out.println("initS3() - END");
    }

    public static void main(String[] args) throws Exception {

        System.out.println("===========================================");
        System.out.println("Welcome to the Simple S3 Uploader!");

        SimpleS3Uploader s3up = new SimpleS3Uploader();
        if(s3up.processCommandline(args)) {
            s3up.listBucket();
            s3up.uploadFile();
            s3up.listBucket();

        }

    }


    private void build_repo(){

        // Catalog components

        uploadFile("index.xml" , "/index.xml");
        uploadFile("index.xsl" , "/index.xsl");
        uploadFile("index.css" , "/index.css");

        uploadFile("data_index.xml" , "data//index.xml");
        uploadFile("nc_index.xml"   , "data/nc//index.xml");



        // Data Files

        uploadFile("/Users/ndp/data/data/nc/coads_climatology.nc"  , "data/nc/coads_climatology.nc");
        uploadFile("/Users/ndp/data/data/nc/fnoc1.nc"              , "data/nc/fnoc1.nc");
        uploadFile("/Users/ndp/data/data/nc/sst.mnmean.nc"         , "data/nc/sst.mnmean.nc");
        uploadFile("/Users/ndp/data/data/nc/200803061600_HFRadar_USEGC_6km_rtv_SIO.nc"  , "data/nc/200803061600_HFRadar_USEGC_6km_rtv_SIO.nc");
        uploadFile("/Users/ndp/data/data/nc/AG2006001_2006003_ssta.nc"  , "data/nc/AG2006001_2006003_ssta.nc");
        uploadFile("/Users/ndp/data/data/nc/MB2006001_2006001_chla.nc"  , "data/nc/MB2006001_2006001_chla.nc");
        uploadFile("/Users/ndp/data/data/nc/a21160601.nc"               , "data/nc/a21160601.nc");

    }













    public void listBucket() {
        System.out.println("- - - - - - - - - - - - - - - - - - - - - -");
        System.out.println("S3 Bucket: "+ _s3BucketName);
        System.out.println("Listing: ");



        long totalSize = 0;
        int totalItems = 0;



        ObjectListing objects = s3.listObjects(_s3BucketName);
        do {
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                System.out.println("   " + objectSummary.getKey() + " " + objectSummary.getSize()+" bytes");
                totalSize += objectSummary.getSize();
                totalItems++;
            }
            objects = s3.listNextBatchOfObjects(objects);
        } while (objects.isTruncated());

        System.out.println("The  Amazon S3 bucket '" + _s3BucketName + "'" +
                "contains " + totalItems + " objects with a total size of " + totalSize + " bytes.");


    }

    public void uploadFile(){
        if(_targetUploadFile !=null){
            uploadFile(_targetUploadFile, _uploadFileKey);
        }
    }

    public void uploadFile( String filename, String key){

        File f = new File(filename);
        uploadFile(f, key);
    }




    public void uploadFile( File f, String key){
        System.out.println("- - - - - - - - - - - - - - - - - - - - - -");
        System.out.println("S3 File Uploader");
        System.out.println("    S3 Bucket:      "+ _s3BucketName);
        System.out.println("    Uploading file: "+ f.getAbsolutePath());
        System.out.println("    S3 Key:         "+ key);

        s3.putObject(_s3BucketName, key, f);
    }


}
