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

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * JAXB spec for DMR dataset
 * @author ukari
 *
 */
@XmlRootElement (name="Dataset")
public class Dataset {

	// FIXME: change the name from "coverageId" to "name" and modify setter and getter accordingly. ndp 9/7/17
	private String coverageId;
	private String url;
	
	private List<Dimension> dimensions; 
	private List<ContainerAttribute> attributes;
	
	private List<Float64> vars64bitFloats;
	private List<Float32> vars32bitFloats;
	private List<Int64>   vars64bitIntegers;
	private List<Int32>   vars32bitIntegers;


	/**
	 * This default constructor intializes all of the stuff so things can never be null.
	 */
	public Dataset() {
		coverageId = "";
		url = "";
		dimensions = new Vector<>();
		attributes = new Vector<>();
		vars64bitFloats = new Vector<>();
		vars32bitFloats = new Vector<>();
		vars64bitIntegers = new Vector<>();
		vars32bitIntegers = new Vector<>();
	}
	
	
	@XmlAttribute(name="name")
	public String getCoverageId() {
		return coverageId;
	}

	public void setCoverageId(String coverageId) {
		this.coverageId = coverageId;
	}

	// DMR generate prefixed attribute "xml:base", 
	// just using base here
	// the xml prefix is handled in package.info
	@XmlAttribute(name="base")
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    @XmlElement(name="Attribute")
	public List<ContainerAttribute> getAttributes() {
		return attributes;
	}


	public void setAttributes(List<ContainerAttribute> attributes) {
		this.attributes = attributes;
	}

	@XmlElement(name="Dimension")
	public List<Dimension> getDimensions() {
		return dimensions;
	}

	
	public void setDimensions(List<Dimension> dimensions) {
		this.dimensions = dimensions;
	}

	@XmlElement(name="Float64")
	public List<Float64> getVars64bitFloats() {
		return vars64bitFloats;
	}


	public void setVars64bitFloats(List<Float64> vars64bitFloats) {
		this.vars64bitFloats = vars64bitFloats;
	}

	@XmlElement(name="Float32")
	public List<Float32> getVars32bitFloats() {
		return vars32bitFloats;
	}


	public void setVars32bitFloats(List<Float32> vars32bitFloats) {
		this.vars32bitFloats = vars32bitFloats;
	}

	@XmlElement(name="Int32")
	public List<Int32> getVars32bitIntegers() {
		return vars32bitIntegers;
	}


	public void setVars32bitIntegers(List<Int32> vars32bitIntegers) {
		this.vars32bitIntegers = vars32bitIntegers;
	}
	
	@XmlElement(name="Int64")
	public List<Int64> getVars64bitIntegers() {
		return vars64bitIntegers;
	}

	public void setVars64bitIntegers(List<Int64> vars64bitIntegers) {
		this.vars64bitIntegers = vars64bitIntegers;
	}

}
