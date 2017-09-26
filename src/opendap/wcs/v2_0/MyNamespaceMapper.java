/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
 * // Author: Uday Kari  <ukari@opendap.org>
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

package opendap.wcs.v2_0;

// Reference Implementation of JAXB (JSR-222)
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
// Per https://dzone.com/articles/jaxb-and-namespace-prefixes, this is likely for  EclipseLink MOXy
// import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;

public class MyNamespaceMapper extends NamespacePrefixMapper {

    private static final String WCS_PREFIX = ""; // DEFAULT NAMESPACE
    private static final String WCS_URI = "http://www.opengis.net/wcs/2.0";

    private static final String GML_PREFIX = "gml";
    private static final String GML_URI = "http://www.opengis.net/gml/3.2";

    private static final String SWE_PREFIX = "swe";
    private static final String SWE_URI = "http://www.opengis.net/swe/2.0";

    private static final String GMLCOV_PREFIX = "gmlcov";
    private static final String GMLCOV_URI = "http://www.opengis.net/gmlcov/1.0";

    private static final String OWS_PREFIX = "ows";
    private static final String OWS_URI = "http://www.opengis.net/ows/2.0";

    private static final String XLINK_PREFIX = "xlink";
    private static final String XLINK_URI = "http://www.w3.org/1999/xlink";
    
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
    	
        if(WCS_URI.equals(namespaceUri)) {
            return WCS_PREFIX;
        } else if (GML_URI.equals(namespaceUri)) {
            return GML_PREFIX;
        } else if (GMLCOV_URI.equals(namespaceUri)) {
            return GMLCOV_PREFIX;
        } else if (SWE_URI.equals(namespaceUri)) {
            return SWE_PREFIX;
        } else if (OWS_URI.equals(namespaceUri)) {
            return OWS_PREFIX;
        } else if (XLINK_URI.equals(namespaceUri)) {
            return XLINK_PREFIX;
        }
        return suggestion;
    }

    @Override
    public String[] getPreDeclaredNamespaceUris() {
        return new String[] { WCS_URI, GML_URI, SWE_URI, GMLCOV_URI, OWS_URI, XLINK_URI};
    }

}
