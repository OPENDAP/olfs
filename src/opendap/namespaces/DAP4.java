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
 * Date: 2/19/13
 * Time: 10:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class DAP4 {

    /**
     * This class should never be instantiated.
     */
    private DAP4(){ throw new IllegalStateException("opendap.namespaces.DAP4 class"); }

    public static final String    NAMESPACE_STRING = "http://xml.opendap.org/ns/DAP/4.0#";
    public static final Namespace NS = Namespace.getNamespace("dap4",NAMESPACE_STRING);

    public static final String ERROR = "Error";

    public static final String DATASET = "Dataset";
    public static final String GROUP = "Group";
    public static final String DIMENSION = "Dimension";
    public static final String ENUMERATION = "Enumeration";
    public static final String INT8   = "Int8";
    public static final String UINT8  = "UInt8";
    public static final String BYTE   = "Byte";
    public static final String CHAR   = "Char";
    public static final String INT16  = "Int16";
    public static final String UINT16 = "UInt16";
    public static final String INT32  = "Int32";
    public static final String UINT32 = "UInt32";
    public static final String INT64  = "Int64";
    public static final String UINT64 = "UInt64";
    public static final String FLOAT32 = "Float32";
    public static final String FLOAT64 = "Float64";
    public static final String STRING  = "String";
    public static final String D_URI   = "URI";
    public static final String OPAQUE = "Opaque";
    public static final String ENUM = "Enum";
    public static final String STRUCTURE = "Structure";
    public static final String SEQUENCE = "Sequence";
    public static final String DIM = "Dim";
    public static final String NAME = "name";
    public static final String SIZE = "size";


}
