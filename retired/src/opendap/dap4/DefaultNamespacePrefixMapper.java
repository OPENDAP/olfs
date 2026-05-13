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

package opendap.dap4;

import java.util.HashMap;
import java.util.Map;
// removing all dependencies
// import com.sun.xml.bind.marshaller.NamespacePrefixMapper;


/**
 * This class untangles various namespaces in OGC
 * @author ukari
 *
 */
public class DefaultNamespacePrefixMapper 
  //extends NamespacePrefixMapper 
  {
	 
		//private Map<String, String> namespaceMap = new HashMap<>();
	 
		/**
		 * Create mappings.
		 */
	   /*
		public DefaultNamespacePrefixMapper() {
			namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
			namespaceMap.put("http://www.opengis.net/wcs/2.0", "wcs");
			namespaceMap.put("http://www.opengis.net/gml/3.2", "gml");
			namespaceMap.put("http://www.opengis.net/swe/2.0", "swe");
			namespaceMap.put("http://www.w3.org/1999/xlink", "gml");
			namespaceMap.put("http://www.opengis.net/ows/2.0", "ows");
			namespaceMap.put("http://www.opengis.net/gmlcov/1.0", "gmlcov");
		}
		*/
	 
		/* (non-Javadoc)
		 * Returning null when not found based on spec.
		 * @see com.sun.xml.bind.marshaller.NamespacePrefixMapper#getPreferredPrefix(java.lang.String, java.lang.String, boolean)
		 */
		/*
		@Override
		public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
			return namespaceMap.getOrDefault(namespaceUri, suggestion);
		}
		*/
	}

