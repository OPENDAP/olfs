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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * JAXB spec for DMR dataset
 * @author ukari
 *
 */
@XmlRootElement (name="Dataset")
public class Dataset {

  Logger _log;

	private String name;
	private String url;
	
	private List<Dimension> dimensions; 
	private List<ContainerAttribute> attributes;
	
	private List<Float64> vars64bitFloats;
	private List<Float32> vars32bitFloats;
	private List<Int64>   vars64bitIntegers;
	private List<Int32>   vars32bitIntegers;

	private boolean _checkedForCF;
	private boolean _isCFConvention;

	/**
	 * This default constructor initializes all of the stuff so things can never be null.
	 */
	public Dataset() {
	    _log = LoggerFactory.getLogger(this.getClass());
		name = "";
		url = "";
		dimensions = new Vector<>();
		attributes = new Vector<>();
		vars64bitFloats = new Vector<>();
		vars32bitFloats = new Vector<>();
		vars64bitIntegers = new Vector<>();
		vars32bitIntegers = new Vector<>();
		_checkedForCF = false;
		_isCFConvention = false;
	}
	
	
	@XmlAttribute
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// DMR generate prefixed attribute "xml:base", 
	// just using base here
	// the respective xml prefix is handled in package.info
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

    /**
     * This finds the named imension if it exisits.
     *
     * @param name
     * @return
     */
	public Dimension getDimension(String name){
	    while(name.startsWith("/") && name.length()>1)
            name = name.substring(1);

	    // FIXME This should handle groups and Dimensions declared at places other than the root Group
	    for(Dimension dim: getDimensions()){
	        if(dim.getName().equals(name))
	            return dim;
        }
        return null;
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

	public Vector<Variable> getVariables() {

	    Vector<Variable> vars = new Vector<>();

		vars.addAll(getVars32bitFloats());
		vars.addAll(getVars64bitFloats());
		vars.addAll(getVars32bitIntegers());
		vars.addAll(getVars64bitIntegers());

        return vars;
    }

    public boolean usesCfConventions(){
		if(!_checkedForCF) {
			_checkedForCF = true;
			for (ContainerAttribute containerAttribute : attributes) {
				boolean foundGlobal = false;

				String ca_name = containerAttribute.getName();
				if (ca_name.toLowerCase().contains("convention")) {
					_log.debug("Found container attribute named convention(s)");
				} // this will find plural conventions
				else if (ca_name.toLowerCase().endsWith("_global") || ca_name.equalsIgnoreCase("DODS_EXTRA")) {
					_log.debug("Found container attribute name ending in _GLOBAL or DODS_EXTRA");
					_log.debug("Looking for conventions...attribute");
					foundGlobal = true;
				}
				for (Attribute a : containerAttribute.getAttributes()) {
					_log.debug(a.toString());

					String a_name = a.getName();
					a_name = a_name == null ? "" : a_name;

					String a_value = a.getValue();
					a_value = a_value == null ? "" : a_value;

					if (foundGlobal) {
						// test for conventions
						if (a_name.toLowerCase().contains("convention")) {

							_log.debug(
									"Found attribute named convention(s), value = " + a_value);
							if (a_value.toLowerCase().contains("cf-")) {
								_log.debug("Dataset is CF Compliant!!");
								_isCFConvention = true;
								return true;
							}
						}
					}
				} // end for loop on all container attributes
			}
		}
        return _isCFConvention;
    }
        
   public String getValueOfGlobalAttributeWithNameLike(String name) {
	 
     for (ContainerAttribute containerAttribute : attributes) {
       	for (Attribute a : containerAttribute.getAttributes()) {

          String a_name = Util.nullProof(a.getName());
          String a_value = Util.nullProof(a.getValue());
          
          if (Util.stringContains(a_name, name)) 
          {
            return a_value;
          }
        }
      }
     
     return null;
   }

   
   public String getSizeOfDimensionWithNameLike(String name)
   {
     for (Dimension dim : dimensions)
       if (Util.stringContains(dim.getName(), name)) return dim.getSize();
    
     return null;
   }
   
   public Variable getVariable (String name)
   {
     for (Variable v : this.getVariables())
       if (v.getName().equalsIgnoreCase(name)) return v;
     
     return null;
   }  
}
