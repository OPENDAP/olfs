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


@XmlSchema (
      elementFormDefault=XmlNsForm.QUALIFIED,
      namespace="http://www.opengis.net/wcs/2.0",
      xmlns = 
      {
        @XmlNs(prefix = "gml", namespaceURI="http://www.opengis.net/gml/3.2"),
        @XmlNs(prefix = "xlink", namespaceURI="http://www.w3.org/1999/xlink"),
        @XmlNs(prefix="gmlcov", namespaceURI="http://www.opengis.net/gmlcov/1.0"),
        @XmlNs(prefix="swe", namespaceURI="http://www.opengis.net/swe/2.0"),
        @XmlNs(prefix="ows", namespaceURI="http://www.opengis.net/ows/2.0")      
      }
)

/**
 * Vestigial info class - to untangle namespaces probably more suited for generation of XML schema, not marshaling...
 * 
 * xmlns="http://www.opengis.net/wcs/2.0" 
 * xmlns:ns2="http://www.opengis.net/gml/3.2" 
 * xmlns:ns3="http://www.w3.org/1999/xlink" 
 * xmlns:ns4="http://www.opengis.net/gmlcov/1.0" 
 * xmlns:ns5="http://www.opengis.net/swe/2.0" 
 * xmlns:ns6="http://www.opengis.net/ows/2.0"
 * 
 * Per Blaise Doughan https://stackoverflow.com/questions/17478317/how-to-set-the-default-namespace-using-jaxb:
 * 
 * The namespaces specified in the @XmlSchema annotation are meant to affect the generation of the XML Schema....
 * are **NOT** guaranteed to be used when a object model is marshalled to XML. 
 * 
 * However EclipseLink JAXB (MOXy) and recent versions of the JAXB reference implementation (RI) will use them whenever possible.
 * https://wiki.eclipse.org/EclipseLink/Examples/MOXy/JAXB/SpecifyRuntime 
 * 
 * (did not work)
 * 
 * What really worked was the MyNameSpaceMapper.  But this introduces dependency on legacy Sun Microsystems JAXB Runtime.  
 * Which is OK since several new libraries are being used (from OGC - like GML, SWE etc).
 * Legacy Sun Microsystems - but still that JAXB runtime is still active as of June 2017(!)
 * https://mvnrepository.com/artifact/com.sun.xml.bind/jaxb-impl/2.3.0-b170127.1453
 * https://mvnrepository.com/artifact/com.sun.xml.bind/jaxb-core/2.3.0-b170127.1453
 * 
 * 
 */
package opendap.wcs.v2_0;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
