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
 * JAXB for DMR dataset, with some helper access, aggregation methods
 * <p>
 * Supports unmarshaling the DMR XML into Java by reflection
 * <p>
 * This is NOT complete (i.e. supporting yet of all possible DMRs)
 * <p>
 * Specifically, does NOT
 * 1.  cover all DAP4 variables
 * 2.  support nesting of container attributes beyond one level
 * 3.  provide for variable groups
 *
 * @author Uday Kari
 * @author Nathan Potter
 */
@XmlRootElement(name = "Dataset")
public class Dataset {

    Logger _log;

    private String name;
    private String url;

    private List<Dimension> dimensions;
    private List<ContainerAttribute> attributes;

    private Vector<Variable> variables;


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
        float64Vars = new Vector<>();
        float32Vars = new Vector<>();
        int64Vars = new Vector<>();
        int32Vars = new Vector<>();
        int16Vars = new Vector<>();
        int8Vars = new Vector<>();
        uInt64Vars = new Vector<>();
        uInt32Vars = new Vector<>();
        uInt16Vars = new Vector<>();
        uInt8Vars = new Vector<>();
        byteVars = new Vector<>();
        charVars = new Vector<>();

        variables = new Vector<>();

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
    @XmlAttribute(name = "base")
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    @XmlElement(name = "Attribute")
    public List<ContainerAttribute> getAttributes() {
        return attributes;
    }
    public void setAttributes(List<ContainerAttribute> attributes) {
        this.attributes = attributes;
    }

    @XmlElement(name = "Dimension")
    public List<Dimension> getDimensions() {
        return dimensions;
    }
    public void setDimensions(List<Dimension> dimensions) {
        this.dimensions = dimensions;
    }

    public Vector<Variable> getVariables() {

        Vector<Variable> vars = new Vector<>();

        vars.addAll(float64Vars);
        vars.addAll(float32Vars);

        vars.addAll(int64Vars);
        vars.addAll(uInt64Vars);

        vars.addAll(int32Vars);
        vars.addAll(uInt32Vars);

        vars.addAll(int16Vars);
        vars.addAll(uInt16Vars);

        vars.addAll(int8Vars);
        vars.addAll(uInt8Vars);

        vars.addAll(byteVars);
        vars.addAll(charVars);

        return vars;
    }




    private List<Float64> float64Vars;
    @XmlElement(name = "Float64")
    public List<Float64> getFloat64Vars() {
        return float64Vars;
    }
    public void setFloat64Vars(List<Float64> f64List) {
        variables.addAll(f64List);
        this.float64Vars = f64List;
    }

    private List<Float32> float32Vars;
    @XmlElement(name = "Float32")
    public List<Float32> getFloat32Vars() { return float32Vars; }
    public void setFloat32Vars(List<Float32> f32List) {
        variables.addAll(f32List);
        this.float32Vars = f32List;
    }

    private List<Byte> byteVars;
    @XmlElement(name = "UInt8")
    public List<Byte> getByteVars() {
        return byteVars;
    }
    public void setByteVars(List<Byte> byteList) {
        variables.addAll(byteList);
        byteVars = byteList;
    }

    private List<Char> charVars;
    @XmlElement(name = "Char")
    public List<Char> getCharVars() {
        return charVars;
    }
    public void setCharVars(List<Char> charList) {
        variables.addAll(charList);
        charVars = charList;
    }

    private List<UInt8> uInt8Vars;
    @XmlElement(name = "UInt8")
    public List<UInt8> getUInt8Vars() {
        return uInt8Vars;
    }
    public void setUInt8Vars(List<UInt8> uInt8List) {
        variables.addAll(uInt8List);
        uInt8Vars = uInt8List;
    }

    private List<Int8> int8Vars;
    @XmlElement(name = "Int8")
    public List<Int8> getInt8Vars() {
        return int8Vars;
    }
    public void setInt8Vars(List<Int8> int8List) {
        variables.addAll(int8List);
        this.int8Vars = int8List;
    }

    private List<UInt16> uInt16Vars;
    @XmlElement(name = "Int16")
    public List<UInt16> getUInt16Vars() {
        return uInt16Vars;
    }
    public void setUInt16Vars(List<UInt16> uInt16List) {
        variables.addAll(uInt16List);
        this.uInt16Vars = uInt16List;
    }

    private List<Int16> int16Vars;
    @XmlElement(name = "Int16")
    public List<Int16> getInt16Vars() {
        return int16Vars;
    }
    public void setInt16Vars(List<Int16> int16List) {
        variables.addAll(int16List);
        this.int16Vars = int16List;
    }

    private List<UInt32> uInt32Vars;
    @XmlElement(name = "UInt32")
    public List<UInt32> getUInt32Vars() {
        return uInt32Vars;
    }
    public void setUInt32Vars(List<UInt32> uInt32List) {
        variables.addAll(uInt32List);
        this.uInt32Vars = uInt32List;
    }

    private List<Int32> int32Vars;
    @XmlElement(name = "Int32")
    public List<Int32> getInt32Vars() {
        return int32Vars;
    }
    public void setInt32Vars(List<Int32> int32List) {
        variables.addAll(int32List);
        this.int32Vars = int32List;
    }

    private List<UInt64> uInt64Vars;
    @XmlElement(name = "UInt64")
    public List<UInt64> getUInt64Vars() {
        return uInt64Vars;
    }
    public void setUInt64Vars(List<UInt64> uInt64List) {
        variables.addAll(uInt64List);
        this.uInt64Vars = uInt64List;
    }

    private List<Int64> int64Vars;
    @XmlElement(name = "Int64")
    public List<Int64> getInt64Vars() {
        return int64Vars;
    }
    public void setInt64Vars(List<Int64> int64List) {
        variables.addAll(int64List);
        this.int64Vars = int64List;
    }


    /**
     * This finds the named Dimension if it exists.
     * First scans the root of Dataset
     * FIXME: Next it should scan all its variable groups
     *
     * @param name attribution of Dimesion tag
     * @return opendap.dap4.Dimension
     */
    public Dimension getDimension(String name) {
        while (name.startsWith("/") && name.length() > 1)
            name = name.substring(1);

        // First, scan the root of Dataset
        for (Dimension dim : getDimensions()) {
            if (dim.getName().equals(name))
                return dim;
        }

        // next scan its variable groups

        return null;
    }

    /**
     * Searches for global container attributes and looks for conventions tag
     * if it is found with value CF, then sets the CF compliance flag, returns true
     *
     * @return true
     */
    public boolean usesCfConventions() {
        if (_checkedForCF)
            return _isCFConvention;

        _checkedForCF = true;
        for (ContainerAttribute containerAttribute : attributes) {
            if (containerAttribute.getName().toLowerCase().endsWith("_global") ||
                    containerAttribute.getName().equalsIgnoreCase("DODS_EXTRA")) {

                _log.debug("Found container attribute name ending in _GLOBAL or DODS_EXTRA");
                _log.debug("Looking for conventions...attribute");

                if (containerAttribute.getAttributeValue("Conventions", true, true).contains("CF"))
                    _isCFConvention = true;
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
                if (caseInsensitiveStringContains(a.getName(), name))
                    return a.getValue();
            }
        }
        return null;
    }

    public double getValueOfGlobalAttributeWithNameLikeAsDouble(String attributeName, double defaultValue) {

        String valueStr = getValueOfGlobalAttributeWithNameLike(attributeName);
        double result = defaultValue;
        if (valueStr != null) {
            try {
                result = Double.parseDouble(valueStr);
            } catch (NumberFormatException nfe) {
                String msg = "getValueOfGlobalAttributeWithNameLikeAsDouble() - " +
                        "Failed to parse value of Dataset global Attribute '" + attributeName +
                        "' value: " + valueStr + "  Using value: " + result;
                _log.warn(msg);
            }
        } else {
            String msg = "getValueOfGlobalAttributeWithNameLikeAsDouble() - " +
                    "Failed to locate global Attribute named '" + attributeName +
                    "'  Using value: " + result;
            _log.warn(msg);
        }
        return result;
    }

    /**
     * Performs a null proof case insensitive check to see
     * if s1 contains s2.
     *
     * @param s1 The string to search
     * @param s2 The candiate sub-string
     * @return true only if str contains sub
     */
    private boolean caseInsensitiveStringContains(String s1, String s2) {
        if (
                s1 != null &&
                        s2 != null &&
                        s1.trim().length() > 0 &&
                        s2.trim().length() > 0
                ) {
            return s1.trim().toLowerCase().contains(s2.trim().toLowerCase());
        }
        return false;
    }


    /**
     * Helper method to scan dataset by variable name
     *
     * @param name of variable
     * @return first instance of opendap.dap4.Variable matching the name, case insensitive
     */
    public Variable getVariable(String name) {
        for (Variable v : this.getVariables())
            if (v.getName().equalsIgnoreCase(name)) return v;

        return null;
    }
}
