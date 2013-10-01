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
package opendap.namespaces;

import org.jdom.Namespace;

/**
 * User: ndp
 * Date: Apr 18, 2008
 * Time: 7:06:27 PM
 */
public class NCML {


    public static final String NAMESPACE_STRING = "http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2";
    public static final String NAMESPACE_PREFIX = "ncml";

    public static final Namespace NS =  Namespace.getNamespace(NAMESPACE_STRING);

    public static final String NETCDF = "netcdf";
    public static final String AGGREGATION = "catalogRef";
    public static final String VARIABLE_AGG = "variableAgg";
    public static final String VARIABLE = "variable";
    public static final String ATTRIBUTE = "attribute";
}
