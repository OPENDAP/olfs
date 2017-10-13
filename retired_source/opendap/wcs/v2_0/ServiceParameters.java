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

import javax.xml.bind.annotation.XmlElement;

public class ServiceParameters {
	
	private String coverageSubtype = "";
	private String nativeFormat = "";
	
	@XmlElement(name="CoverageSubtype")
	public String getCoverageSubtype() {
		return coverageSubtype;
	}
	public void setCoverageSubtype(String coverageSubtype) {
		this.coverageSubtype = coverageSubtype;
	}
	
	@XmlElement
	public String getNativeFormat() {
		return nativeFormat;
	}
	public void setNativeFormat(String nativeFormat) {
		this.nativeFormat = nativeFormat;
	}
	
	

}
