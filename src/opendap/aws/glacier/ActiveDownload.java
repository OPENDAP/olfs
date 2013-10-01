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

import com.amazonaws.services.glacier.AmazonGlacierAsyncClient;
import com.amazonaws.services.glacier.model.*;
import opendap.aws.auth.Credentials;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;

public class ActiveDownload  implements  Serializable {

    Logger _log;

    private boolean _started;


    Date _startDate;
    private long _expectedDelay;   // In seconds
    InitiateJobResult _initiateJobResult;
    GlacierRecord _glacierRecord;
    public static final String GLACIER_ARCHIVE_RETRIEVAL = "archive-retrieval";

    ActiveDownload(
        GlacierRecord glacierRecord,
        long expectedDelay
    ) {

        _log = LoggerFactory.getLogger(this.getClass());

        _startDate = null;
        _initiateJobResult = null;
        _glacierRecord = glacierRecord;
        _expectedDelay = expectedDelay;
        _started = false;
    }

    public Date getStartDate(){ return _startDate; }

    public long getExpectedDelay(){ return _expectedDelay; }


    // Returns Estimated wait time in seconds
    public long estimatedTimeRemaining() throws IOException{

        if(_startDate == null){
            throw new IOException("Glacier Job has not been started!");
        }

        long start = _startDate.getTime();
        long now = new Date().getTime();

        long elapsed = now - start;

        long remaining  = (_expectedDelay*1000) - elapsed;
        if(remaining<0)
            remaining = 0;

        return remaining/1000;

    }

    public InitiateJobResult getInitiateJobResult(){ return _initiateJobResult; }

    public GlacierRecord getGlacierRecord(){ return _glacierRecord; }



    public boolean startGlacierRetrieval(Credentials glacierCreds) throws IOException {

        _log.debug("startGlacierRetrieval() - BEGIN ");

        if(_started)
            throw new IOException("Glacier retrieval job for resourceID '"+_glacierRecord.getResourceId()+" has already been started!");

        AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(glacierCreds);
        client.setEndpoint(GlacierArchiveManager.theManager().getGlacierEndpoint());

        String vaultName  = _glacierRecord.getVaultName();
        String archiveId  = _glacierRecord.getArchiveId();
        String resourceId = _glacierRecord.getResourceId();

        JobParameters jobParameters = new JobParameters()
            .withArchiveId(archiveId)
            .withDescription("Retrieving " + resourceId)
            .withType(GLACIER_ARCHIVE_RETRIEVAL);

        try {

            _startDate = new Date();

            _initiateJobResult = client.initiateJob(new InitiateJobRequest()
                    .withJobParameters(jobParameters)
                    .withVaultName(vaultName));


            _log.debug("startGlacierRetrieval() - Glacier download job started. jobId: {}",_initiateJobResult.getJobId());

            _started = true;

        }
        catch (Exception e) {
            _log.error("startGlacierRetrieval() - Download failed to start! msg: {}",e.getMessage());
        }

        _log.debug("startGlacierRetrieval() - END ");

        return _started;

    }

    public boolean isReadyForDownload(Credentials glacierCreds) throws IOException {

        _log.debug("isReadyForDownload() - BEGIN ");

        if(!_started)
            throw new IOException("Glacier retrieval job for resourceID '"+_glacierRecord.getResourceId()+" has already NOT been started!");


        AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(glacierCreds);
        client.setEndpoint(GlacierArchiveManager.theManager().getGlacierEndpoint());

        String vaultName  = _glacierRecord.getVaultName();


        InitiateJobResult initiateJobResult = getInitiateJobResult();


        long timeRemaining = estimatedTimeRemaining();

        _log.debug("isReadyForDownload() - Estimated Time Remaining: {} seconds  jobId: {}",timeRemaining,initiateJobResult.getJobId());

        DescribeJobRequest djr = new DescribeJobRequest(vaultName,initiateJobResult.getJobId());

        DescribeJobResult describeJobResult = client.describeJob(djr);

        _log.debug("isReadyForDownload() - DescribeJobResult: {}",describeJobResult.toString());
        _log.debug("isReadyForDownload() - DescribeJobResult.isCompleted(): {}",describeJobResult.isCompleted());
        _log.debug("isReadyForDownload() - DescribeJobResult.status(): {}",describeJobResult.getStatusCode());

        _log.debug("isReadyForDownload() - END ");

        return describeJobResult.isCompleted();

    }


    public boolean download(Credentials glacierCreds){

        _log.debug("download() - BEGIN (retrieving Glacier Resource {})",getGlacierRecord().getArchiveId());
        boolean success = false;

        String jobId = getInitiateJobResult().getJobId();
        String vaultName = getGlacierRecord().getVaultName();
        File cacheFile = getGlacierRecord().getCacheFile();

        try {
            AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(glacierCreds);
            client.setEndpoint(GlacierArchiveManager.theManager().getGlacierEndpoint());


            GetJobOutputRequest jobOutputRequest = new GetJobOutputRequest()
                    .withJobId(jobId)
                    .withVaultName(vaultName);
            GetJobOutputResult jobOutputResult = client.getJobOutput(jobOutputRequest);

            InputStream in = jobOutputResult.getBody();

            FileOutputStream out = new FileOutputStream(cacheFile);
            IOUtils.copy(in, out);

            _log.error("download() - Retrieved glacier resource. CacheFile: ",cacheFile.getAbsolutePath());

            success = true;

        }
        catch (Exception e) {
            _log.error("download() - Failed to to download glacier resource. Msg: {}",e.getMessage());
        }



        _log.debug("download() - END");


        return success;

    }


}