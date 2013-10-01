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

import opendap.aws.AwsUtil;
import opendap.bes.dapResponders.BesApi;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 9/25/13
 * Time: 12:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlacierBesApi extends BesApi {



    public String getBesDataSourceID(String relativeUrl, boolean checkWithBes){

        String id = super.getBesDataSourceID(relativeUrl,checkWithBes);

        return AwsUtil.encodeKeyForFileSystemName(id);
    }

    public String getBesDataSourceID(String relativeUrl, Pattern matchPattern, boolean checkWithBes){

        while(relativeUrl.startsWith("/") && relativeUrl.length()>1)
            relativeUrl = relativeUrl.substring(1);


        String id = super.getBesDataSourceID(relativeUrl,matchPattern,checkWithBes);
        if(id!=null)
            id = AwsUtil.encodeKeyForFileSystemName(id);
        return id;


    }
}
