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

package opendap.aws.s3;




import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.model.S3Object;
import opendap.aws.auth.CredentialsProvider;
import opendap.aws.auth.NullCredentialsProvider;
import opendap.coreServlet.Util;
import opendap.noaa_s3.S3Index;
import opendap.noaa_s3.S3IndexedFile;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Welcome to your new AWS Java SDK based project!
 *
 * This class is meant as a starting point for your console-based application that
 * makes one or more calls to the AWS services supported by the Java SDK, such as EC2,
 * SimpleDB, and S3.
 *
 * In order to use the services in this sample, you need:
 *
 *  - A valid Amazon Web Services account. You can register for AWS at:
 *       https://aws-portal.amazon.com/gp/aws/developer/registration/index.html
 *
 *  - Your account's Access Key ID and Secret Access Key:
 *       http://aws.amazon.com/security-credentials
 *
 *  - A subscription to Amazon EC2. You can sign up for EC2 at:
 *       http://aws.amazon.com/ec2/
 *
 *  - A subscription to Amazon SimpleDB. You can sign up for Simple DB at:
 *       http://aws.amazon.com/simpledb/
 *
 *  - A subscription to Amazon S3. You can sign up for S3 at:
 *       http://aws.amazon.com/s3/
 */
public class Testy {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */


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
    public Testy()  {


    }






    /**
    * Amazon EC2
    *
    * The AWS EC2 client allows you to create, delete, and administer
    * instances programmatically.
    *
    * In this sample, we use an EC2 client to get a list of all the
    * availability zones, and all instances sorted by reservation id.
    */
    public void ec2(AWSCredentialsProvider credentialsProvider){
        try {
            AmazonEC2 ec2 = new AmazonEC2Client(credentialsProvider);
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");

            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();

            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }

            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }

    }


    public void s3(AWSCredentialsProvider credentialsProvider){
        AmazonS3Client s3 =  new AmazonS3Client(credentialsProvider);
        s3(s3);

    }

    /**
    * Amazon S3
    *
    * The AWS S3 client allows you to manage buckets and programmatically
    * put and get objects to those buckets.
    *
    * In this sample, we use an S3 client to iterate over all the buckets
    * owned by the current user, and all the object metadata in each
    * bucket, to obtain a total object and space usage count. This is done
    * without ever actually downloading a single object -- the requests
    * work with object metadata only.

    */
    public void s3(AmazonS3Client s3){
        try {
            List<Bucket> buckets = s3.listBuckets();

            for (Bucket bucket : buckets) {

                printS3Bucket(s3, bucket.getName());
                // demoS3BucketIter(s3, bucket.getName());

            }

            System.out.println("You have " + buckets.size() + " Amazon S3 bucket(s)");
        } catch (AmazonServiceException ase) {
            /*
             * AmazonServiceExceptions represent an error response from an AWS
             * services, i.e. your request made it to AWS, but the AWS service
             * either found it invalid or encountered an error trying to execute
             * it.
             */
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            /*
             * AmazonClientExceptions represent an error that occurred inside
             * the client on the local host, either while trying to send the
             * request to AWS or interpret the response. For example, if no
             * network connection is available, the client won't be able to
             * connect to AWS to execute a request and will throw an
             * AmazonClientException.
             */
            System.out.println("Error Message: " + ace.getMessage());
        } catch (IOException e) {
            System.out.println("Error Message: " + e.getMessage());
        }


    }


    public void demoS3(AWSCredentialsProvider credentialsProvider){

        /*
         * Amazon S3
         *
         * The AWS S3 client allows you to manage buckets and programmatically
         * put and get objects to those buckets.
         *
         * In this sample, we use an S3 client to iterate over all the buckets
         * owned by the current user, and all the object metadata in each
         * bucket, to obtain a total object and space usage count. This is done
         * without ever actually downloading a single object -- the requests
         * work with object metadata only.
         */
        try {
            AmazonS3 s3 =  new AmazonS3Client(credentialsProvider);


            List<Bucket> buckets = s3.listBuckets();

            long totalSize  = 0;
            int  totalItems = 0;
            for (Bucket bucket : buckets) {
                /*
                 * In order to save bandwidth, an S3 object listing does not
                 * contain every object in the bucket; after a certain point the
                 * S3ObjectListing is truncated, and further pages must be
                 * obtained with the AmazonS3Client.listNextBatchOfObjects()
                 * method.
                 */
                ObjectListing objects = s3.listObjects(bucket.getName());
                do {
                    for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                        totalSize += objectSummary.getSize();
                        totalItems++;
                    }
                    objects = s3.listNextBatchOfObjects(objects);
                } while (objects.isTruncated());
            }

            System.out.println("You have " + buckets.size() + " Amazon S3 bucket(s), " +
                    "containing " + totalItems + " objects with a total size of " + totalSize + " bytes.");
        } catch (AmazonServiceException ase) {
            /*
             * AmazonServiceExceptions represent an error response from an AWS
             * services, i.e. your request made it to AWS, but the AWS service
             * either found it invalid or encountered an error trying to execute
             * it.
             */
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            /*
             * AmazonClientExceptions represent an error that occurred inside
             * the client on the local host, either while trying to send the
             * request to AWS or interpret the response. For example, if no
             * network connection is available, the client won't be able to
             * connect to AWS to execute a request and will throw an
             * AmazonClientException.
             */
            System.out.println("Error Message: " + ace.getMessage());
        }
    }








    public void noaa_s3_bucket(String noaaS3BucketName) throws IOException{

        System.out.println("===========================================");
        System.out.println("NOAA S3 Bucket: "+noaaS3BucketName);
        System.out.println("");


        AWSCredentials nullCreds = null;
        AmazonS3Client anonS3  = new AmazonS3Client(nullCreds);


        printS3Bucket(anonS3, noaaS3BucketName);
    }


    public void printS3Bucket(AWSCredentialsProvider credentialsProvider, String bucketName) throws IOException {
        AmazonS3Client s3 =  new AmazonS3Client(credentialsProvider);
        printS3Bucket(s3,bucketName);

    }


    public void printS3Bucket(String bucketName) throws IOException {
        printS3Bucket(new NullCredentialsProvider(),bucketName);

    }

    public void printS3Bucket(AmazonS3Client s3, String bucketName) throws IOException {

        StringBuilder sb = new StringBuilder();

        System.out.println("### printS3Bucket() BEGIN ######################################################################");
        System.out.println("Bucket: " + bucketName);

        long totalSize = 0;
        long totalItems = 0;

        ObjectListing objects = s3.listObjects(bucketName);
        do {
            for (S3ObjectSummary s3ObjectSummary : objects.getObjectSummaries()) {

                String myBucket = s3ObjectSummary.getBucketName();
                String key      = s3ObjectSummary.getKey();

                long size       = s3ObjectSummary.getSize();
                String url      = s3.getResourceUrl(myBucket, key);


                System.out.print("S3ObjectSummary - ");
                System.out.print("[BucketName:"  + myBucket + "] ");
                System.out.print("[Key:"         + key      + "] ");
                System.out.print("[Size:"        + size     + "] ");
                System.out.print("[ResourceURL:" + url      + "] ");
                System.out.println();

                try {
                    //ObjectMetadata objectMetadata = s3.getObjectMetadata(myBucket,key);
                    //printOS3ObjectMetadata(objectMetadata);
                    printS3Object(s3,bucketName,key);
                }
                catch(Exception e){
                    System.err.println("Caught "+e.getClass().getName()+"  msg: "+e.getMessage());
                }

                //printS3Object(s3,myBucket,key);



                totalSize += size;
                totalItems++;
            }
            objects = s3.listNextBatchOfObjects(objects);
            System.out.println("Size: "+totalSize+"  Items: "+totalItems+"  sb.length(): "+sb.length());
        } while (objects.isTruncated());

        System.out.println("    Total Items: " + totalItems + "\n");
        System.out.println("    Total Size:  " + totalSize + "\n");


        System.out.println(sb.toString());
        System.out.println("### printS3Bucket() END ########################################################################");

    }

    public void demoS3BucketIter(AmazonS3 s3, String bucketName){

        System.out.println("===========================================================================");

        long totalSize  = 0;
        int  totalItems = 0;

        /*
         * In order to save bandwidth, an S3 object listing does not
         * contain every object in the bucket; after a certain point the
         * S3ObjectListing is truncated, and further pages must be
         * obtained with the AmazonS3Client.listNextBatchOfObjects()
         * method.
         */
        ObjectListing objects = s3.listObjects(bucketName);
        do {
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                System.out.println("demoS3BucketIter() - objectSummary: [BucketName:"+objectSummary.getBucketName()+"][key:"+objectSummary.getKey()+"][size:"+objectSummary.getSize()+"]");
                totalSize += objectSummary.getSize();
                totalItems++;
            }
            objects = s3.listNextBatchOfObjects(objects);
        } while (objects.isTruncated());

        System.out.println("demoS3BucketIter() - [Bucket:"+bucketName+"][items:"+totalItems+"][totalSize:"+totalSize+"]");
        System.out.println("===========================================================================");
    }



    public void printS3Object(AmazonS3Client s3, String bucketName, String key) throws IOException {

        GetObjectRequest objectRequest = new GetObjectRequest(bucketName,key);
        S3Object s3Object = null;
        try {
            s3Object = s3.getObject(objectRequest);
            System.out.print("S3Object - ");
            System.out.print("[BucketName: "+ s3Object.getBucketName()+"]");
            System.out.print("[Key: "+ s3Object.getKey()+"]");
            System.out.println();
        }
        finally {
            if(s3Object!=null)
                s3Object.close();
        }


    }


    public void printOS3ObjectMetadata(ObjectMetadata objectMetadata){


        StringBuilder sb = new StringBuilder();

        sb.append("S3ObjectMetadata - \n");
        sb.append("    getCacheControl():          ").append(objectMetadata.getCacheControl()).append("\n");
        sb.append("    getContentDisposition():    ").append(objectMetadata.getContentDisposition()).append("\n");
        sb.append("    getContentEncoding():       ").append(objectMetadata.getContentEncoding()).append("\n");
        sb.append("    getContentLength():         ").append(objectMetadata.getContentLength()).append("\n");
        sb.append("    getContentMD5():            ").append(objectMetadata.getContentMD5()).append("\n");
        sb.append("    getContentType():           ").append(objectMetadata.getContentType()).append("\n");
        sb.append("    getETag():                  ").append(objectMetadata.getETag()).append("\n");
        sb.append("    getExpirationTime():        ").append(objectMetadata.getExpirationTime()).append("\n");
        sb.append("    getExpirationTimeRuleId():  ").append(objectMetadata.getExpirationTimeRuleId()).append("\n");
        sb.append("    getHttpExpiresDate():       ").append(objectMetadata.getHttpExpiresDate()).append("\n");
        sb.append("    getLastModified():          ").append(objectMetadata.getLastModified()).append("\n");
        sb.append("    getOngoingRestore():        ").append(objectMetadata.getOngoingRestore()).append("\n");
        sb.append("    getRestoreExpirationTime(): ").append(objectMetadata.getRestoreExpirationTime()).append("\n");
        sb.append("    getServerSideEncryption():  ").append(objectMetadata.getServerSideEncryption()).append("\n");
        sb.append("    getVersionId():             ").append(objectMetadata.getVersionId()).append("\n");


        System.out.println(sb);

    }




    public static void main(String[] args)  {

        System.out.println("===========================================");
        System.out.println("Welcome to the AWS Java SDK!");
        System.out.println("===========================================");

        Testy test = new Testy();

        AWSCredentialsProvider credentialsProvider = new CredentialsProvider();
        //test.ec2(credentialsProvider);
        //test.s3(credentialsProvider);
        //test.demoS3(credentialsProvider);


        //String noaaS3BucketName = "ocean-archive.data.nodc.noaa.gov";
        String noaaS3BucketName = "foo-s3cmd.nodc.noaa.gov";

/*

        try {
            test.printS3Bucket("opendap-test");
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            test.printS3Bucket(noaaS3BucketName);
        } catch (Exception e) {
            e.printStackTrace();
        }

 //*/


        try {
            String s3CacheRoot = "/Users/ndp/scratch/s3Test";

            S3Index s3i = new S3Index(noaaS3BucketName,"//index.xml",s3CacheRoot);
            XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
            xmlo.output(s3i.getIndexElement(), System.out);

            System.out.println(Util.getMemoryReport());

            //s3i.updateCachedIndexAsNeeded(true, 3);

            //Vector<String> keys = s3i.getChildIndexKeys(true, 0);
            //System.out.println("Located " +keys.size()+" Index keys");

            ///for(String key: keys){
            //    System.out.println("key: "+ key);
            //}

            Vector<S3IndexedFile> resourceObject = s3i.getChildIndexedFiles(true, 0);
            Vector<String> resourceKeys = s3i.getChildResourceKeys(true, 0);



            for(String resourceKey : resourceKeys){
               // System.out.println("    resource key: "+ resourceKey);
                System.out.println(resourceKey);

            }
            System.out.println("Located " +resourceObject.size()+" resource objects.");


            //s3i.updateCachedIndexAsNeeded(true,0);

            System.out.println(Util.getMemoryReport());




        } catch (Exception e) {
            e.printStackTrace();
        }


/*

        try {
            test.printS3Bucket(noaaS3BucketName);
        } catch (Exception e) {
            e.printStackTrace();
        }

//*

        try {
            test.noaa_s3_bucket(noaaS3BucketName);
        } catch (Exception e) {
            e.printStackTrace();
        }

*/


    }




}
