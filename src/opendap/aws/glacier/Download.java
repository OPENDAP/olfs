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
import com.amazonaws.services.glacier.model.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 10/10/13
 * Time: 11:40 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Download implements Serializable {




    Logger _log;

    private boolean _started;


    Date _startDate;
    private long _expectedDelay;   // In seconds
    InitiateJobResult _initiateJobResult;

    private String _vaultName;
    private String _endPointUrl;

    private AWSCredentials _credentials;

    private File _downloadFile;

    private String _identifier;


    public Download(String vaultName, String glacierEndpointUrl, AWSCredentials awsCredentials, long expectedDelay) {

        _log = LoggerFactory.getLogger(this.getClass());

        _vaultName = vaultName;
        _endPointUrl = glacierEndpointUrl;
        _credentials = awsCredentials;
        _expectedDelay = expectedDelay;

        _startDate = null;
        _initiateJobResult = null;
        _started = false;
        _identifier = null;
    }

    public boolean started() {
        return _started;
    }


    public Date getStartDate(){ return new Date(_startDate.getTime()); }

    public long getExpectedDelay(){ return _expectedDelay; }

    public String getVaultName() { return _vaultName;}
    public String getEndpointUrl() { return _endPointUrl; }
    public AWSCredentials getCredentials() { return _credentials; }

    public InitiateJobResult getInitiateJobResult(){ return _initiateJobResult; }


    public void setDownloadFile(File file){
        _downloadFile = new File(file.getAbsolutePath());
    }

    public File getDownloadFile() throws IOException {
        if(_downloadFile==null)  {
            String msg = "Download file has not been set for this Download!";
            _log.error(msg);
            throw new IOException(msg);
        }
        return new File(_downloadFile.getAbsolutePath());
    }



    public abstract boolean startJob() throws IOException;


    public String getDownloadId() { return _identifier; }

    public void setDownloadId(String id) {
        if(_identifier!=null){
            _log.warn("setDownloadId() - Download identifier cannot be set to '{}' because it has already been set to '{}'",id,_identifier);
        } else
            _identifier = id;
    }



        // Returns Estimated wait time in seconds
    public long estimatedTimeRemaining() throws IOException {

        if(_startDate == null){
            throw new IOException("Glacier Job has not been started!");
        }

        long start = getStartDate().getTime();
        long now = new Date().getTime();

        long elapsed = now - start;

        long remaining  = (getExpectedDelay()*1000) - elapsed;
        if(remaining<0)
            remaining = 0;

        return remaining/1000;

    }



    public boolean startJob(JobParameters jobParameters) throws IOException {

        _log.debug("startJob() - BEGIN ");

        if(_started)
            throw new IOException("Glacier inventory retrieval job has already been started!");

        AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(getCredentials());
        client.setEndpoint(getEndpointUrl());

        InitiateJobRequest ijb = new InitiateJobRequest(getVaultName(),jobParameters);
        try {

            _startDate = new Date();

            _initiateJobResult = client.initiateJob(ijb);


            _log.debug("startJob() - Glacier downloadJobOutput job started. jobId: {}",_initiateJobResult.getJobId());

            _started = true;

        }
        catch (Exception e) {
            _log.error("startJob() - Download failed to start! msg: {}",e.getMessage());
        }

        _log.debug("startJob() - END ");

        return _started;

    }


    public boolean jobCompleted() throws IOException {

        _log.debug("archiveRetrievalJobCompleted() - BEGIN ");

        if(!_started)
            throw new IOException("Glacier retrieval job has NOT been started!");


        AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(getCredentials());
        client.setEndpoint(getEndpointUrl());


        InitiateJobResult initiateJobResult = getInitiateJobResult();

        long timeRemaining = estimatedTimeRemaining();

        _log.debug("archiveRetrievalJobCompleted() - Estimated Time Remaining: {} seconds  jobId: {}",timeRemaining,initiateJobResult.getJobId());

        DescribeJobRequest djr = new DescribeJobRequest(getVaultName(),initiateJobResult.getJobId());

        DescribeJobResult describeJobResult = client.describeJob(djr);

        _log.debug("archiveRetrievalJobCompleted() - DescribeJobResult: {}",describeJobResult.toString());
        _log.debug("archiveRetrievalJobCompleted() - DescribeJobResult.isCompleted(): {}",describeJobResult.isCompleted());
        _log.debug("archiveRetrievalJobCompleted() - DescribeJobResult.status(): {}",describeJobResult.getStatusCode());

        _log.debug("archiveRetrievalJobCompleted() - END ");

        return describeJobResult.isCompleted();

    }


    public boolean downloadJobOutput() throws IOException {

        return downloadJobOutput(getDownloadFile());
    }




    public boolean downloadJobOutput(File downloadFile) throws IOException {


        if(!jobCompleted()) {
            _log.warn("Glacier retrieval job has not completed!");
            return false;
        }
        boolean success = false;


        String jobId = getInitiateJobResult().getJobId();

        _log.debug("downloadJobOutput() - BEGIN (Retrieving Glacier Job Result. JobID: {})",jobId);


        try {
            AmazonGlacierAsyncClient client = new AmazonGlacierAsyncClient(getCredentials());
            client.setEndpoint(getEndpointUrl());


            GetJobOutputRequest jobOutputRequest = new GetJobOutputRequest()
                    .withJobId(jobId)
                    .withVaultName(getVaultName());
            GetJobOutputResult jobOutputResult = client.getJobOutput(jobOutputRequest);

            InputStream in = jobOutputResult.getBody();

            FileOutputStream out = new FileOutputStream(downloadFile);
            IOUtils.copy(in, out);

            _log.error("downloadJobOutput() - Retrieved Glacier job output. CacheFile: {}",downloadFile.getAbsolutePath());

            success = true;

        }
        catch (Exception e) {
            _log.error("downloadJobOutput() - Failed to retrieve Glacier job output. Msg: {}",e.getMessage());
        }



        _log.debug("downloadJobOutput() - END");


        return success;

    }



}
