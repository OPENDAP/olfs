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
 * JAXB spec for DMR variables like Float, Int
 * @author ukari
 *
 */
public class Variable {
	
	private String name;
	private List<Dim> dims;
	private List<Attribute> attributes;
	
	Variable(){
		name="";
		dims = new Vector<>();
		attributes = new Vector<>();
	}

	@XmlAttribute
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement (name="Dim")
	public List<Dim> getDims() {
		return dims;
	}

	public void setDims(List<Dim> dims) {
		this.dims = dims;
	}

	@XmlElement(name="Attribute")
	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}
	
	
	/**
	 * return value of attribute given its name 
	 * @param attributeName
	 * @return attributeValue
	 */
  public String getAttributeValue(String attributeName)
  {
    List<opendap.dap4.Attribute> attributes = this.getAttributes();
    Hashtable<String, opendap.dap4.Attribute> attributesHash = new Hashtable();
    Iterator<opendap.dap4.Attribute> iter = attributes.iterator();
    while (iter.hasNext()) {
        opendap.dap4.Attribute attribute = iter.next();
        attributesHash.put(attribute.getName(), attribute);
    }
    Attribute a = attributesHash.get(attributeName);
    return a.getValue();
  }

}
