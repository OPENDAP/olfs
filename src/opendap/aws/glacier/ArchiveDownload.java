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
import com.amazonaws.services.glacier.model.*;
import org.slf4j.Logger;

import java.io.*;

public class ArchiveDownload extends Download implements  Serializable {

    Logger _log;
    private GlacierArchive _glacierRecord;


    public static final String GLACIER_ARCHIVE_RETRIEVAL = "archive-retrieval";


    ArchiveDownload(GlacierArchive glacierRecord, AWSCredentials awsCredentials, long expectedDelay) {
        super(glacierRecord.getVaultName(), GlacierManager.theManager().getGlacierEndpoint(),awsCredentials,expectedDelay);
        _glacierRecord = glacierRecord;
        setDownloadFile(_glacierRecord.getCacheFile());
    }


    @Override
    public boolean startJob() throws IOException {

        JobParameters jobParameters = new JobParameters()
            .withArchiveId(_glacierRecord.getArchiveId())
            .withDescription("Retrieving " + _glacierRecord.getResourceId())
            .withType(GLACIER_ARCHIVE_RETRIEVAL);

        return startJob(jobParameters);

    }

    public GlacierArchive getGlacierRecord(){ return _glacierRecord; }




}