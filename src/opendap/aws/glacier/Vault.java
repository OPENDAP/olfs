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
import com.amazonaws.services.glacier.AmazonGlacierAsyncClient;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.*;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import opendap.aws.auth.Credentials;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 10/9/13
 * Time: 6:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class Vault {

    Logger _log;

    private boolean _verbose;
    private File _workingDir;
    private String _glacierEndpointUrl;
    private String _vaultName;
    private String _archiveId;
    private String _COMMAND;
    private AWSCredentials _credentials;

    public static final String GLACIER_ARCHIVE_RETRIEVAL = "archive-retrieval";
    public static final String GLACIER_INVENTORY_RETRIEVAL = "inventory-retrieval";


    private Vault(){
        _log = LoggerFactory.getLogger(this.getClass());
        _vaultName = null;
        _glacierEndpointUrl = null;
        _credentials = null;
        _workingDir = new File("/tmp");
        _archiveId = null;

    }

    public Vault(String name, AWSCredentials credentials, String endpointUrl){
        this();
        _vaultName = name;
        _glacierEndpointUrl = endpointUrl;
        _credentials = credentials;

    }

    public AWSCredentials getCredentials(){
        return _credentials;
    }

    public String getName(){
        return _vaultName;
    }

    public String getEndpoint(){
        return _glacierEndpointUrl;
    }


    public String put(String resourceId, File uploadFile) throws FileNotFoundException {
        AmazonGlacierClient client = new AmazonGlacierClient(getCredentials());
        client.setEndpoint(getEndpoint());

        ArchiveTransferManager atm = new ArchiveTransferManager(client, getCredentials());

        _log.info("Transferring cache file content to Glacier. vault: " + getName() + "  description: " + resourceId);
        UploadResult uploadResult = atm.upload(getName(), resourceId, uploadFile);

        String archiveId = uploadResult.getArchiveId();

        _log.info("Upload Successful. archiveId: {} resourceId: {}",archiveId,resourceId);

        return archiveId;

    }

    public String describeVault(){

        DescribeVaultResult dvo;

        AmazonGlacierClient client = new AmazonGlacierClient(getCredentials());
        client.setEndpoint(getEndpoint());

        DescribeVaultRequest dvr = new DescribeVaultRequest(getName());

        dvo = client.describeVault(dvr);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("Found Vault: ").append(dvo.getVaultName()).append("\n");
        sb.append("    getCreationDate(): ").append(dvo.getCreationDate()).append("\n");
        sb.append("    getLastInventoryDate(): ").append(dvo.getLastInventoryDate()).append("\n");
        sb.append("    getNumberOfArchives(): ").append(dvo.getNumberOfArchives()).append("\n");
        sb.append("    getSizeInBytes(): ").append(dvo.getSizeInBytes()).append("\n");
        sb.append("    getVaultARN(): ").append(dvo.getVaultARN()).append("\n");
        sb.append("    toString(): ").append(dvo.toString()).append("\n");
        return sb.toString();
    }


    public void deleteArchive(String archiveId){
        AmazonGlacierClient client = new AmazonGlacierClient(getCredentials());
        DeleteArchiveRequest dar = new DeleteArchiveRequest(getName(),archiveId);
        client.deleteArchive(dar);

    }


    public String initiateInventoryRetrieval(){

        JobParameters jobParameters = new JobParameters()
            .withDescription("Inventory Retrieval")
            .withType(GLACIER_INVENTORY_RETRIEVAL);

        InitiateJobRequest ijb = new InitiateJobRequest(getName(),jobParameters);

        AmazonGlacierClient client = new AmazonGlacierClient(getCredentials());

        InitiateJobResult jobResult = client.initiateJob(ijb);

        return jobResult.getJobId();

    }

    public boolean jobIsReady(String jobId){


        _log.debug("jobIsReady() - BEGIN ");

        AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(getCredentials());
        client.setEndpoint(GlacierArchiveManager.theManager().getGlacierEndpoint());


        DescribeJobRequest djr = new DescribeJobRequest(getName(),jobId);

        DescribeJobResult describeJobResult = client.describeJob(djr);

        _log.debug("jobIsReady() - DescribeJobResult: {}",describeJobResult.toString());
        _log.debug("jobIsReady() - DescribeJobResult.isCompleted(): {}",describeJobResult.isCompleted());
        _log.debug("jobIsReady() - DescribeJobResult.status(): {}",describeJobResult.getStatusCode());

        _log.debug("jobIsReady() - END ");

        return describeJobResult.isCompleted();
    }

    public boolean downloadJobResult(String jobId, File file){


        _log.debug("downloadInventory() - BEGIN");
        boolean success = false;


        try {
            AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(getCredentials());
            client.setEndpoint(GlacierArchiveManager.theManager().getGlacierEndpoint());


            GetJobOutputRequest jobOutputRequest = new GetJobOutputRequest()
                    .withJobId(jobId)
                    .withVaultName(getName());
            GetJobOutputResult jobOutputResult = client.getJobOutput(jobOutputRequest);

            InputStream in = jobOutputResult.getBody();

            FileOutputStream out = new FileOutputStream(file);
            IOUtils.copy(in, out);

            _log.error("downloadInventory() - Retrieved glacier resource. CacheFile: ",file.getAbsolutePath());

            success = true;

        }
        catch (Exception e) {
            _log.error("downloadInventory() - Failed to to download glacier resource. Msg: {}",e.getMessage());
        }



        _log.debug("downloadInventory() - END");


        return success;
    }




    private  boolean processCommandline(String[] args) throws ParseException {

        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("h", "help", false, "Usage information.");
        options.addOption("v", "verbose", false, "Makes more output...");

        options.addOption("i", "awsId", true, "AWS access key ID for working with Glacier.");
        options.addOption("k", "awsKey", true, "AWS secret key for working with Glacier.");

        options.addOption("n", "vault-name", false, "Name of glacier vault to operate on");
        options.addOption("e", "glacier-endpoint-url", false, "Glacier endpoint URL for this vault/user.");

        options.addOption("a", "archive-id", false, "A Glacier Archive ID .");


        options.addOption("w", "working-directory", false, "Name of the working directory to use to store persistent information about glacier jobs.");

        CommandLine line =   parser.parse(options, args);

        String usage  = this.getClass().getName()+" -i AWSAccessKeyID -k AWSSecretKey -v vaultName -e endpointURl [-v] [-h] ";

        StringBuilder errorMessage = new StringBuilder();

        if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);
            return false;
        }

        _verbose = line.hasOption("verbose");

        _vaultName  = line.getOptionValue("vault-name");
        if(_vaultName==null){
            errorMessage.append("Missing Parameter - You must provide a Glacier vault name with the --vault-name option.\n");
        }

        _glacierEndpointUrl = line.getOptionValue("glacier-endpoint-url");
        if(_glacierEndpointUrl==null){
            errorMessage.append("Missing Parameter - You must provide a Glacier endpoint URL with the --glacier-endpoint-url option.\n");
        }

        String awsAccessKeyId =  line.getOptionValue("awsId");
        if(awsAccessKeyId == null){
            errorMessage.append("Missing Parameter - You must provide an AWS access key ID (to access the Glacier service) with the --awsId option.\n");
        }

        String awsSecretKey = line.getOptionValue("awsKey");
        if(awsSecretKey == null){
            errorMessage.append("Missing Parameter - You must provide an AWS secret key (to access the Glacier service) with the --awsKey option.\n");
        }

        if(awsAccessKeyId!=null && awsSecretKey!=null)
            _credentials = new Credentials(awsAccessKeyId,awsSecretKey);

        String dirName  = line.getOptionValue("working-directory");
        if(dirName==null){
            _log.warn("No working directory supplied by user! Using default of: {}",_workingDir.getAbsolutePath());
        }
        else {
            _workingDir = new File(dirName);
        }

        _archiveId  = line.getOptionValue("archive-id");
        if(_archiveId!=null){
            _log.info("ArchiveIs set to: {}",_archiveId);
        }

        String [] commands = line.getArgs();

        if(commands.length==0 || commands.length>1){
            errorMessage.append("Missing Parameter - You must provide (after all of the other command line parameters) " +
                    "a single command for the program to perform. The command must be one of:\n" +
                    "    destroyVault  - Retrieve vault index. Delete all archives. Delete vault. Takes 4 hours.\n" +
                    "    deleteArchive - Delete the archive whose ID is supplied in --archive-id parameter.\n" +
                    "    getInventory  - Retrieve the inventory for the vault. Takes 4 hours.\n" +
                    "    startInventory  - Start the inventory.\n" +
                    "    isInventoryReady  - Find out if the inventory job is done.\n" +
                    "    downloadInventory  - Download completed inventory job.\n" +
                    ""
            );

        }
        _COMMAND = commands[0];


        if(errorMessage.length()!=0){

            System.err.println(errorMessage);

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);

            return false;
        }

        return true;

    }


    public void performCommand(String command){


        if(command.equalsIgnoreCase("startInventory")){

        }






    }




    public static void main(String[] args)  {

        String vaultName   = "foo-s3cmd.nodc.noaa.gov";
        String endpointUrl = "https://glacier.us-east-1.amazonaws.com";


        Vault vault = new Vault();

        try {
            if(vault.processCommandline(args)){

                vault.performCommand(vault._COMMAND);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }



    }






}
