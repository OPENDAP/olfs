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
package opendap.wcs.gatewayClient;

import org.jdom.Namespace;
import org.jdom.Element;

/**
 * WCS Namespaces and vocabulary.
 */
public class WCS {

    public static String    NAMESPACE_STRING = "http://www.opengis.net/wcs";
    public static Namespace NS = Namespace.getNamespace(NAMESPACE_STRING);


    public static String    DAPWCS_NAMEPACES_STRING ="http://www.opendap.org/ns/dapwcs";
    public static Namespace DAPWCS_NS = Namespace.getNamespace(DAPWCS_NAMEPACES_STRING);


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
