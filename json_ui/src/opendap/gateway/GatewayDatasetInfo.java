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

package opendap.gateway;

import opendap.coreServlet.DataSourceInfo;
import opendap.ncml.NcmlManager;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 5/12/11
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayDatasetInfo implements DataSourceInfo {

    String datasetId;

    public GatewayDatasetInfo(String DatasetId){
        datasetId = DatasetId;
    }

    public  boolean sourceExists(){
        return true;
    }

    public  boolean sourceIsAccesible(){

        return true;
    }

    public  boolean isNode(){
        return false;
    }

    public  boolean isDataset(){
        return true;
    }

    public  long    lastModified(){
        return -1;
    }

}
