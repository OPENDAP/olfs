/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
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

import org.jdom.Element;
import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 *
 <swe:field name="u.u">
     <swe:Quantity definition="http://www.unidata.edu/def/property/CF-1.1/0/surface_eastward_sea_water_velocity">
         <swe:description>Eastern component of sea surface current vector.</swe:description>
         <swe:uom code="m/s"/>
         <swe:constraint>
             <swe:AllowedValues>
                 <swe:interval>-327868 32768</swe:interval>
                 <swe:significantFigures>3</swe:significantFigures>
             </swe:AllowedValues>
         </swe:constraint>
     </swe:Quantity>
 </swe:field>

 *
 *
 */
public class Field {

    Element _mySweElement;

    public Field(Element swe) throws WcsException {

        if(swe.getName().equals("field") && swe.getNamespace().equals(WCS.SWE_NS)){
            _mySweElement = swe;
        }
        else {
            throw new WcsException("Cannot instantiate a Field class with a non-conformant " +
                "intializer element.",WcsException.INVALID_PARAMETER_VALUE);
        }
    }


    /**
     * Builds a Dummy swe:Field with just a name attribute.
     * Use this only for testing purposes!!!
     * @param name
     */
    public Field(String name){
        Element e = new Element("field", WCS.SWE_NS);
        e.setAttribute("name", name);
        _mySweElement =e;
    }

    @XmlAttribute
    public String getName(){
        return _mySweElement.getAttributeValue("name");
    }

}
