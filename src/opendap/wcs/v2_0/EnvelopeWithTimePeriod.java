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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class EnvelopeWithTimePeriod {

	private String srsName = "";
	private String axisLabels = "";
	private String uomLabels = "";
	private String srsDimension = "";
	
	private String lowerCorner = "";
	private String upperCorner = "";
	private String beginPosition = "";
	private String endPosition = "";
    
	@XmlAttribute
	public String getSrsName() {
		return srsName;
	}
	public void setSrsName(String srsName) {
		this.srsName = srsName;
	}
	
	@XmlAttribute
	public String getAxisLabels() {
		return axisLabels;
	}
	public void setAxisLabels(String axisLabels) {
		this.axisLabels = axisLabels;
	}
	
	@XmlAttribute
	public String getUomLabels() {
		return uomLabels;
	}
	public void setUomLabels(String uomLabels) {
		this.uomLabels = uomLabels;
	}
	
	@XmlAttribute
	public String getSrsDimension() {
		return srsDimension;
	}
	public void setSrsDimension(String srsDimension) {
		this.srsDimension = srsDimension;
	}
	
	@XmlElement
	public String getLowerCorner() {
		return lowerCorner;
	}
	public void setLowerCorner(String lowerCorner) {
		this.lowerCorner = lowerCorner;
	}
	
	@XmlElement
	public String getUpperCorner() {
		return upperCorner;
	}
	public void setUpperCorner(String upperCorner) {
		this.upperCorner = upperCorner;
	}
	
	@XmlElement
	public String getBeginPosition() {
		return beginPosition;
	}
	public void setBeginPosition(String beginPosition) {
		this.beginPosition = beginPosition;
	}
	
	@XmlElement
	public String getEndPosition() {
		return endPosition;
	}
	public void setEndPosition(String endPosition) {
		this.endPosition = endPosition;
	}
}
