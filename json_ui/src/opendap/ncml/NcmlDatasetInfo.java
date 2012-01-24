/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) $year OPeNDAP, Inc.
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

package opendap.ncml;

import opendap.coreServlet.DataSourceInfo;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/12/11
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class NcmlDatasetInfo implements DataSourceInfo {

    String datasetId;

    public NcmlDatasetInfo(String ncmlDatasetId){
        datasetId = ncmlDatasetId;
    }

    public  boolean sourceExists(){
        return NcmlManager.isNcmlDataset(datasetId);
    }

    public  boolean sourceIsAccesible(){

        return NcmlManager.isNcmlDataset(datasetId);
    }

    public  boolean isNode(){
        return false;
    }

    public  boolean isDataset(){
        return NcmlManager.isNcmlDataset(datasetId);
    }

    public  long    lastModified(){
        return NcmlManager.getLastModified(datasetId);
    }

}
