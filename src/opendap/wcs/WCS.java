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
package opendap.wcs;

import org.jdom.Namespace;
import org.jdom.Element;

/**
 * User: ndp
 * Date: Mar 14, 2008
 * Time: 8:16:30 AM
 */
public class WCS {

    public static String    NAMESPACE_STRING = "http://www.opengis.net/wcs";
    public static Namespace NS = Namespace.getNamespace(NAMESPACE_STRING);

    public static String    GML_NAMEPACES_STRING ="http://www.opengis.net/gml";
    public static Namespace GML_NS = Namespace.getNamespace(GML_NAMEPACES_STRING);


    public static String COVERAGE_OFFERING = "CoverageOffering";
    public static String NAME = "name";
    public static String LABEL = "label";
    public static String DOMAIN_SET = "domainSet";
    public static String TEMPORAL_DOMAIN = "temporalDomain";
    public static String TIME_PERIOD = "timePeriod";
    public static String BEGIN_POSITION = "beginPosition";
    public static String END_POSITION = "endPosition";



}
