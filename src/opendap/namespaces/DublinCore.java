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
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 4/16/12
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class DublinCore {
    /**
     * This class should never be instantiated.
     */
    private DublinCore(){ throw new IllegalStateException("opendap.namespaces.DublinCore class"); }

    public static final String    NAMESPACE_STRING = "http://purl.org/dc/elements/1.1/";
    public static final Namespace NS = Namespace.getNamespace("dc",NAMESPACE_STRING);


}
