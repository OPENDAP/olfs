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
import org.slf4j.LoggerFactory;

import java.io.*;

public class InventoryDownload  extends Download implements  Serializable {

    Logger _log;
    public static final String GLACIER_INVENTORY_RETRIEVAL = "inventory-retrieval";


    public InventoryDownload(String vaultName, String glacierEndpointUrl, AWSCredentials awsCredentials, long expectedDelay) {

        super(vaultName,glacierEndpointUrl,awsCredentials,expectedDelay);
        _log = LoggerFactory.getLogger(this.getClass());
        setDownloadId(vaultName);
    }

    @Override
    public boolean startJob() throws IOException {

        JobParameters jobParameters = new JobParameters()
            .withType(GLACIER_INVENTORY_RETRIEVAL)
                .withDescription(GLACIER_INVENTORY_RETRIEVAL);

        return startJob(jobParameters);

    }



}