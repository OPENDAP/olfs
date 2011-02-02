/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.soap;

import org.jdom.Namespace;

/**
 * Class wrapper for various Namespace names.
 *
 * User: ndp
 * Date: Apr 26, 2006
 * Time: 2:45:19 PM
 */
public class XMLNamespaces {

    public static final String    WCS_NAMESPACE_STRING = "http://www.opengis.net/wcs/1.1";
    public static final Namespace WCS_NS = Namespace.getNamespace(WCS_NAMESPACE_STRING);
    public static final String    WCS_SCHEMA_LOCATION_BASE= "http://schemas.opengis.net/wcs/1.1/";
    /**
     * OPeNDAP SOAP Interface Namespace
     */
    public static final String OpendapSoapNamespaceString = "http://xml.opendap.org/ns/soap1";

    /**
     *
     * @return OPeNDAP SOAP Interface Namespace
     */
    public static org.jdom.Namespace getOpendapSoapNamespace(){
        return org.jdom.Namespace.getNamespace("ons",OpendapSoapNamespaceString);
    }


    /**
     * Default SOAP Namespace
     */
    private static final String DefaultSoapEnvNamespace = "http://schemas.xmlsoap.org/soap/envelope/";


    /**
     *
     * @return Default SOAP Namespace
     */
    public static org.jdom.Namespace getDefaultSoapEnvNamespace(){
        return org.jdom.Namespace.getNamespace("soapenv",DefaultSoapEnvNamespace);
    }


    /**
     * OPeNDAP Schema Namespace
     */
    public static final String OpendapDAP2NamespaceString = "http://xml.opendap.org/ns/DAP2";

    /**
     *
     * @return OPeNDAP Schema Namespace
     */
    public static org.jdom.Namespace getOpendapDAP2Namespace(){
        return org.jdom.Namespace.getNamespace("od2nms",OpendapDAP2NamespaceString);
    }

    /**
     *  THREDDS Catalog Namespace
     */
    public static final String ThreddsCatalogNamespaceString ="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0";


    /**
     *
     * @return THREDDS Catalog Namespace
     */
    public static org.jdom.Namespace getThreddsCatalogNamespace(){
        return org.jdom.Namespace.getNamespace("tcnms",ThreddsCatalogNamespaceString);
    }




}
