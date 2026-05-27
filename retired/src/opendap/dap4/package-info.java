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

/**
 *  This was a dummy namespace, discarded in favor of what is really
 *  coming out of the DMR, for WcsMarshaller.  
 *  
 *  The proliferation of names spaces needs better package organization
 *  
 *  @TODO perhaps configuration parameter instead of hard-coded below
 *  
 * 
@XmlSchema(namespace="http://xml.opendap.org/ns/WCS/2.0/LocalFileCatalog#",
         elementFormDefault = XmlNsForm.QUALIFIED)
*/
@XmlSchema(
	    namespace="http://xml.opendap.org/ns/DAP/4.0#",
	    elementFormDefault=XmlNsForm.QUALIFIED,
	    xmlns={@XmlNs(prefix="xml", namespaceURI="http://xml.opendap.org/ns/DAP/4.0#")}) 



package opendap.dap4;



import javax.xml.bind.annotation.*;