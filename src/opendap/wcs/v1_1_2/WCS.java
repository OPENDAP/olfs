/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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
package opendap.wcs.v1_1_2;

import org.jdom.Namespace;

/**
 * User: ndp
 * Date: Aug 14, 2008
 * Time: 10:45:26 AM
 */

public class WCS {

    public static final String WCS_VERSION = "1.1.2";

    public static final String    OWS_NAMESPACE_STRING = "http://www.opengis.net/ows/1.1";
    public static final Namespace OWS_NS = Namespace.getNamespace(OWS_NAMESPACE_STRING);
    public static final String    OWS_SCHEMA_LOCATION_BASE= "http://schemas.opengis.net/ows/1.1.0/";


    public static final String    XSI_NAMESPACE_STRING = "http://www.w3.org/2001/XMLSchema-instance";
    public static final Namespace XSI_NS = Namespace.getNamespace("xsi",XSI_NAMESPACE_STRING);



    public static String    WCS_NAMESPACE_STRING = "http://www.opengis.net/wcs/1.1";
    public static Namespace WCS_NS = Namespace.getNamespace(OWS_NAMESPACE_STRING);
    public static final String    WCS_SCHEMA_LOCATION_BASE= "http://schemas.opengis.net/wcs/1.1.0/";


    public static String COVERAGE_OFFERING = "CoverageOffering";
    public static String COVERAGE_OFFERING_BRIEF = "CoverageOfferingBrief";
    public static String CONTENT_METADATA = "ContentMetadata";
    public static String NAME = "name";
    public static String LABEL = "label";
    public static String LON_LAT_ENVELOPE = "lonLatEnvelope";
    public static String DOMAIN_SET = "domainSet";
    public static String TEMPORAL_DOMAIN = "temporalDomain";
    public static String SPATIAL_DOMAIN = "spatialDomain";
    public static String SRS_NAME = "srsName";

    public static String SUPPORTED_CRSS = "supportedCRSs";
    public static String REQUEST_CRSS = "requestCRSs";
    public static String RESPONSE_CRSS = "responseCRSs";
    public static String NATIVE_CRSS = "nativeCRSs";
    public static String REQUEST_RESPONSE_CRSS = "requestResponseCRSs";





}
