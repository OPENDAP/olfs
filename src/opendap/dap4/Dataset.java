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
 * JAXB for DMR dataset, with some helper, aggregation methods 
 * 
 * Supports unmarshaling the DMR XML into Java by reflection
 * 
 * This is NOT complete (i.e. supporting yet of all possible DMRs)  
 * 
 * Specifically, does not cover all DAP4 variables, and, 
 * supports nesting of container attributes to one level only.
 * 
 * @author Uday Kari
 * @author Nathan Potter
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

	// DMR generates prefixed attribute "xml:base", 
	// just using base here, the respective xml prefix 
	// being handled in package.info
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

	public Vector<Variable> getVariables() {

	    Vector<Variable> vars = new Vector<>();

		vars.addAll(getVars32bitFloats());
		vars.addAll(getVars64bitFloats());
		vars.addAll(getVars32bitIntegers());
		vars.addAll(getVars64bitIntegers());

        return vars;
    }

  /**
   * This finds the named Dimension if it exists.
   * First scans the root of Dataset
   * FIXME: Next it should scan all its variable groups
   *
   * @param name attribution of Dimesion tag
   * @return opendap.dap4.Dimension 
   */
  public Dimension getDimension(String name){
    while(name.startsWith("/") && name.length()>1)
        name = name.substring(1);

    // First, scan the root of Dataset
    for(Dimension dim: getDimensions()){
        if(dim.getName().equals(name))
            return dim;
      }
    
    // next scan its variable groups

      return null;
  }
  
  /**
   * Searches for global container attributes and looks for conventions tag
   * if it is found with value CF, then sets the CF compliance flag, returns true
   * @return true
   */
  public boolean usesCfConventions(){
        if(_checkedForCF)
            return _isCFConvention;

        _checkedForCF = true;
        for (ContainerAttribute containerAttribute : attributes) {
            if (containerAttribute.getName().toLowerCase().endsWith("_global") || 
                containerAttribute.getName().equalsIgnoreCase("DODS_EXTRA")) {
              
                _log.debug("Found container attribute name ending in _GLOBAL or DODS_EXTRA");
                _log.debug("Looking for conventions...attribute");
                
                if (containerAttribute.getAttributeValue("Conventions", true, true).contains("CF")) _isCFConvention = true;
           } 
        }
        return _isCFConvention;
    }

   /**
    * Scans the attributes of all container attributes and returns the FIRST match
    *  
    * @param name The Attribute name being searched for
    * @return value of attribute, if found, null otherwise 
    */
   public String getValueOfGlobalAttributeWithNameLike(String name) {
     for (ContainerAttribute containerAttribute : attributes) {
       	for (Attribute a : containerAttribute.getAttributes()) {
          if (Util.caseInsensitiveStringContains(a.getName(), name))
            return a.getValue();
        }
      }
     return null;
   }

    public double getValueOfGlobalAttributeWithNameLikeAsDouble(String attributeName, double defaultValue){

        String valueStr = getValueOfGlobalAttributeWithNameLike(attributeName);
        double result =defaultValue;
        if(valueStr != null){
            try{
                result = Double.parseDouble(valueStr);;
            }
            catch(NumberFormatException nfe){
                String msg = "getValueOfGlobalAttributeWithNameLikeAsDouble() - "+
                        "Failed to parse value of Dataset global Attribute '"+attributeName+
                        "' value: "+valueStr+"  Using value: "+result;
                _log.warn(msg);
            }
        }
        else {
            String msg = "getValueOfGlobalAttributeWithNameLikeAsDouble() - "+
                    "Failed to locate global Attribute named '"+attributeName+
                    "'  Using value: "+result;
            _log.warn(msg);
        }
        return result;
    }


    /**
    * Helper method returning the size attribute of Dimension tag
    * @param name of Dimension
    * @return size attribute value 
    */
   
   public String getSizeOfDimensionWithNameLike(String name)
   {
     for (Dimension dim : dimensions)
       if (Util.caseInsensitiveStringContains(dim.getName(), name)) return dim.getSize();
    
     return null;
   }
   
   /**
    * Helper method to scan dataset by variable name 
    * @param name of variable
    * @return first instance of opendap.dap4.Variable matching the name, case insensitive
    */
   public Variable getVariable (String name)
   {
     for (Variable v : this.getVariables())
       if (v.getName().equalsIgnoreCase(name)) return v;
     
     return null;
   }  
}
