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
 * JAXB spec for Top Level Container Attribute elements of DMR
 *   <Attribute name="HDF5_GLOBAL" type="container">
 *   </Attribute>
 */
public class ContainerAttribute {
	
	private String name;
	private String type = "Container";
	private List<Attribute> attributes;

	public ContainerAttribute(){
		name="";
		attributes = new Vector<>();
	}
	
	@XmlAttribute
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@XmlAttribute
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	@XmlElement(name="Attribute")
	public List<Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}
	
	public String toString()
	{
		return "CONTAINER Attribute name =  " + name +
			   ",  type = " + type;
    }

    /**
     * Returns the value of the requested attribute, ignore the case of the name as directed, and returning null
     * or empty string as directed.
     * @param name Name of Attribute whose value to return
     * @param ignoreCase Ignore case in the name search
     * @param nullProof If true return empty string instead of null.
     * @return
     */
    public String getAttributeValue(String name, boolean ignoreCase, boolean nullProof){
        String value;
		for(Attribute attribute:attributes){
			if(ignoreCase) {
                if (name.equalsIgnoreCase(attribute.getName())) {
                    value = attribute.getValue();
                    value = (value == null && nullProof) ? "" : value;
                    return value;
                }
            }
            else {
                if (name.equals(attribute.getName())) {
                    value = attribute.getValue();
                    value = (value == null && nullProof) ? "" : value;
                    return value;
                }
            }
		}
		return nullProof?"":null;
	}
}
