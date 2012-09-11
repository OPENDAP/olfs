/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
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
package opendap.bes.dap4Responders;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/4/12
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceMediaType extends MediaType {

    public ServiceMediaType(String pType, String sType, String suffix){
        mimeType = pType + "/" + sType;
        primaryType = pType;
        subType = sType;
        twc = pType.equals(wildcard);
        stwc = subType.equals(wildcard);
        quality = 1.0;
        score = 0.0;
        mediaSuffix = suffix;
    }




}
