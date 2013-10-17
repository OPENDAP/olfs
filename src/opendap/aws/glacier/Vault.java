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
import org.apache.commons.cli.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.jdom.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Vector;

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
    private String _endpointUrl;
    private String _vaultName;
    private String _archiveId;
    private String _COMMAND;
    private AWSCredentials _credentials;

    private long _nominalGlacierResponseTime;

    public static final String GLACIER_ARCHIVE_RETRIEVAL   = "archive-retrieval";
    public static final String GLACIER_INVENTORY_RETRIEVAL = "inventory-retrieval";

    public static  String INVENTORY_SUFFIX =  "-INVENTORY.json";


    private Vault(){
        _log = LoggerFactory.getLogger(this.getClass());
        _vaultName = null;
        _endpointUrl = null;
        _credentials = null;
        _workingDir = new File("/tmp");
        _archiveId = null;
        _nominalGlacierResponseTime = 14400; // In seconds

    }

    public Vault(String name, AWSCredentials credentials, String endpointUrl){
        this();
        _vaultName = name;
        _endpointUrl = endpointUrl;
        _credentials = credentials;

    }

    public VaultInventory getInventory()throws IOException{

        File inventoryFile = new File(getWorkingDir(),getVaultName()+INVENTORY_SUFFIX);
        ObjectMapper mapper = new ObjectMapper();
        if(inventoryFile.exists())
            return mapper.readValue(inventoryFile, VaultInventory.class);

        throw new IOException("getInventory() - Inventory file "+inventoryFile.getAbsolutePath()+" does not exist.");

    }



    public void showInventory()throws IOException{

        VaultInventory inventory = getInventory();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        System.out.println(mapper.writeValueAsString(inventory));


    }

    public void purgeDuplicatesOLD() throws IOException{

        VaultInventory inventory = getInventory();

        HashMap<String, Vector<GlacierArchive>> archives = new HashMap<String, Vector<GlacierArchive>>();

        GregorianCalendar gc = new GregorianCalendar();

        Vector<GlacierArchive> tuples;
        for(GlacierArchive gar: inventory.getArchiveList()){
            String resourceID = gar.getResourceId();
            tuples = archives.get(resourceID);
            if(tuples==null)
                tuples = new Vector<GlacierArchive>();
            tuples.add(gar);
        }

        for(Vector<GlacierArchive> archiveTuple: archives.values()){
            if(archiveTuple.size()>1){
                GlacierArchive newest = archiveTuple.firstElement();
                for(GlacierArchive gar: archiveTuple){
                    if(newest.getCreationDate().getTime() > gar.getCreationDate().getTime()){
                        deleteArchive(gar.getArchiveId());
                    }
                    else {
                        deleteArchive(newest.getArchiveId());
                        newest = gar;
                    }
                }
            }
        }


    }

    public void purgeDuplicates() throws IOException{

        VaultInventory inventory = getInventory();

        HashMap<String, GlacierArchive> uniqueArchives = new HashMap<String, GlacierArchive>();


        // Look at all the stuff in the inventory
        for(GlacierArchive gar: inventory.getArchiveList()){
            gar.setVaultName(getVaultName());

            String resourceID = gar.getResourceId();

            // Have we encountered this resourceId?
            GlacierArchive uniqueArchive =  uniqueArchives.get(resourceID);
            if(uniqueArchive==null){
                // It's a new resourceID - add it to the uniqueArchives HashMap
                uniqueArchives.put(resourceID,gar);
            }
            else {
                // It's a resourceID we've already seen. Let's compare the creation dates

                _log.info("purgeDuplicates() - Found duplicate resourceID in vault.");
                _log.info("purgeDuplicates() - resourceID: {}",resourceID);
                _log.info("purgeDuplicates() - archiveID: {} creationDate{}",uniqueArchive.getArchiveId(),uniqueArchive.getCreationDate());
                _log.info("purgeDuplicates() - archiveID: {} creationDate{}",gar.getArchiveId(),gar.getCreationDate());

                if(uniqueArchive.getCreationDate().getTime() > gar.getCreationDate().getTime()){
                    // ah, the candidate archive was created before the current uniqueArchive
                    // for this resourceID. Thus - delete it.
                    deleteArchive(gar.getArchiveId());
                }
                else {
                    // Hmmm The new candidate archive was created after the current
                    // unique archive for this resourceID.
                    // Lets replace the uniqueArchive with the ewer one..
                    uniqueArchives.remove(resourceID);
                    uniqueArchives.put(resourceID,gar);
                    // And the delete the older one from the vault.
                    deleteArchive(uniqueArchive.getArchiveId());
                }

            }
        }

    }


    public void purgeVault() throws IOException{

        VaultInventory inventory = getInventory();

        for(GlacierArchive gar: inventory.getArchiveList()){
            deleteArchive(gar.getArchiveId());
        }
    }

    public void deleteVault() throws IOException{

        purgeVault();
        AmazonGlacierClient client = new AmazonGlacierClient(getCredentials());
        client.setEndpoint(getEndpoint());

        DeleteVaultRequest request = new DeleteVaultRequest()
            .withVaultName(getVaultName());

        client.deleteVault(request);
        System.out.println("Deleted vault: " + getVaultName());



    }


    public AWSCredentials getCredentials(){
        return _credentials;
    }

    public String getVaultName(){
        return _vaultName;
    }

    public String getEndpoint(){
        return _endpointUrl;
    }

    public File getWorkingDir(){
        return new File(_workingDir.getAbsolutePath());
    }

    public String getArchiveId(){
        return _archiveId;
    }


    public String put(String resourceId, File uploadFile) throws FileNotFoundException {
        AmazonGlacierClient client = new AmazonGlacierClient(getCredentials());
        client.setEndpoint(getEndpoint());

        ArchiveTransferManager atm = new ArchiveTransferManager(client, getCredentials());

        _log.info("Transferring cache file content to Glacier. vault: " + getVaultName() + "  description: " + resourceId);
        UploadResult uploadResult = atm.upload(getVaultName(), resourceId, uploadFile);

        String archiveId = uploadResult.getArchiveId();

        _log.info("Upload Successful. archiveId: {} resourceId: {}",archiveId,resourceId);

        return archiveId;

    }



    public String describeVault(String vaultName, AWSCredentials credentials){

        DescribeVaultResult dvo;

        AmazonGlacierClient client = new AmazonGlacierClient(credentials);
        client.setEndpoint(getEndpoint());

        DescribeVaultRequest dvr = new DescribeVaultRequest(vaultName);

        dvo = client.describeVault(dvr);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("Found Vault: ").append(dvo.getVaultName()).append("\n");
        sb.append("    getCreationDateString(): ").append(dvo.getCreationDate()).append("\n");
        sb.append("    getLastInventoryDate(): ").append(dvo.getLastInventoryDate()).append("\n");
        sb.append("    getNumberOfArchives(): ").append(dvo.getNumberOfArchives()).append("\n");
        sb.append("    getSizeInBytes(): ").append(dvo.getSizeInBytes()).append("\n");
        sb.append("    getVaultARN(): ").append(dvo.getVaultARN()).append("\n");
        sb.append("    toString(): ").append(dvo.toString()).append("\n");
        return sb.toString();
    }


    public void deleteArchive(String archiveId){
        _log.warn("deleteArchive() - Removing {}", archiveId);
        AmazonGlacierClient client = new AmazonGlacierClient(getCredentials());
        DeleteArchiveRequest dar = new DeleteArchiveRequest(getVaultName(),archiveId);
        client.deleteArchive(dar);

    }




    private  boolean processCommandline(String[] args) throws ParseException {

        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("h", "help", false, "Usage information.");
        options.addOption("v", "verbose", false, "Makes more output...");

        options.addOption("i", "awsId", true, "AWS access key ID for working with Glacier.");
        options.addOption("k", "awsKey", true, "AWS secret key for working with Glacier.");

        options.addOption("n", "vault-name", true, "Name of glacier vault to operate on");
        options.addOption("e", "endpoint-url", true, "Glacier endpoint URL for this vault/user.");

        options.addOption("a", "archive-id", true, "A Glacier Archive ID .");


        options.addOption("w", "working-directory", true, "Name of the working directory to use to store persistent information about glacier jobs.");

        CommandLine line =   parser.parse(options, args);

        String usage  = this.getClass().getName()+" -i AWSAccessKeyID -k AWSSecretKey -v vaultName -e endpointURl [-v] [-h] COMMAND";

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

        _endpointUrl = line.getOptionValue("endpoint-url");
        if(_endpointUrl ==null){
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
            _log.info("ArchiveIs set to: {}", _archiveId);
        }

        String [] commands = line.getArgs();

        if(commands.length==0 || commands.length>1){
            errorMessage.append("Missing Parameter - You must provide (after all of the other command line parameters) " +
                    "a single command for the program to perform. The command must be one of:\n" +
                    "    destroyVault       - Retrieve vault index. Delete all archives. Delete vault. Will block\n" +
                    "                         for roughly 4 hours.\n" +
                    "    deleteArchive      - Delete the archive whose ID is supplied in --archive-id parameter.\n" +
                    "    getInventory       - Retrieve the inventory for the vault. Will block for roughly 4 hours.\n" +
                    "    startInventoryJob  - Start the inventory.\n" +
                    "    isInventoryReady   - Find out if the inventory job is done.\n" +
                    "    downloadInventory  - Download completed inventory job.\n" +
                    "    describeVault      - Describe Vault (Simple statistics).\n" +
                    "    purgeVault         - Remove all archives from vault.\n" +
                    "    describeVault      - Purge vault of archives and then delete vault..\n" +
                    ""
            );

        }
        else {
            _COMMAND = commands[0];
        }


        if(errorMessage.length()!=0){

            System.out.println(errorMessage);

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, options);

            return false;
        }

        return true;

    }






    public void performCommand(String command) throws IOException, JDOMException {


        DownloadManager.theManager().init(getWorkingDir(), getCredentials());
        try {

            if(command.equalsIgnoreCase("startInventoryJob")){
                DownloadManager.theManager().initiateVaultInventoryDownload(getVaultName(), getEndpoint(), getCredentials());
            }

            else if(command.equalsIgnoreCase("isInventoryReady")){
                DownloadManager.theManager().inventoryRetrievalJobCompleted(getVaultName());
            }
            else if(command.equalsIgnoreCase("downloadInventory")){
                DownloadManager.theManager().downloadInventory(getVaultName());
            }
            else if(command.equalsIgnoreCase("showInventory")){
                showInventory();
            }
            else if(command.equalsIgnoreCase("describeVault")){
                describeVault(getVaultName(), getCredentials());
            }
            else if(command.equalsIgnoreCase("deleteArchive")){
                deleteArchive(getArchiveId());
            }
            else if(command.equalsIgnoreCase("purgeVault")){
                purgeVault();
            }
            else if(command.equalsIgnoreCase("purgeDuplicates")){
                purgeDuplicates();
            }
            else if(command.equalsIgnoreCase("deleteVault")){
                deleteVault();
            }



        }
        finally {
            DownloadManager.theManager().destroy();
        }


    }




    public static void main(String[] args)  {

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
