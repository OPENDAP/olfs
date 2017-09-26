/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.srs;


import opendap.coreServlet.Util;

import java.util.Arrays;

public class Epsg4326 extends SimpleSrs{
    public Epsg4326(){
        super();
        _name = "urn:ogc:def:crs:EPSG::4326";
        _axisLabels = "latitude longitude";
        _uomLabels = "deg deg";
        _srsDimension = 2;
        String[] labels = _axisLabels.split(Util.WHITE_SPACE_REGEX_STRING);
        _axisLabelList.addAll(Arrays.asList(labels));
        labels = _uomLabels.split(Util.WHITE_SPACE_REGEX_STRING);
        _uomLabelList.addAll(Arrays.asList(labels));
    }
}
