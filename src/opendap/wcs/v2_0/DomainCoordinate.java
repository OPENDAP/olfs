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
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 12/19/12
 * Time: 1:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class DomainCoordinate {


    private String _name;
    private String _dapId;
    private String _units;
    private String _arraySubset;
    private long _size;

    public DomainCoordinate(Element dc) throws ConfigurationException {

        _name = dc.getAttributeValue("name");
        if(_name ==null){
            throw new ConfigurationException("In the configuration a DomainCoordinate element is " +
                    "missing the required attribute 'name'.");
        }

        _dapId = dc.getAttributeValue("dapID");
        if(_dapId ==null){
            throw new ConfigurationException("In the configuration a DomainCoordinate element is " +
                    "missing the required attribute 'dapID'.");
        }

        _units = dc.getAttributeValue("units");
        if(_units ==null){
            throw new ConfigurationException("In the configuration a DomainCoordinate element is " +
                    "missing the required attribute 'units'.");
        }


        String s = dc.getAttributeValue("size");
        if(s ==null){
            throw new ConfigurationException("In the configuration a DomainCoordinate element is " +
                    "missing the required attribute 'size'.");
        }
        try {
            _size = Long.parseLong(s);
        }
        catch (NumberFormatException e){
            throw new ConfigurationException("Unable to parse the value of the " +
                    "size attribute: '"+s+"' as a long integer. msg: "+e.getMessage());
        }


    }

    public DomainCoordinate(DomainCoordinate dc){

        _name = dc._name;
        _dapId = dc._dapId;
        _units = dc._units;
        _arraySubset = dc._arraySubset;
        _size = dc._size;

    }
    public DomainCoordinate(String name,
                            String dapId,
                            String units,
                            String arraySubset,
                            long size)
            throws BadParameterException {
        _name = name;
        _dapId = dapId;
        _units = units;
        _arraySubset = arraySubset;
        _size = size;

        if (_name == null) {
            throw new BadParameterException("In DomainCoordinate the 'name' parameter may not have a null value.");
        }
        if (_dapId == null) {
            throw new BadParameterException("In DomainCoordinate the 'dapId' parameter may not have a null value.");
        }
        if (_units == null) {
            throw new BadParameterException("In DomainCoordinate the 'units' parameter may not have a null value.");
        }
        if (_size < 0) {
            throw new BadParameterException("In DomainCoordinate the 'size' parameter may not less than zero.");
        }
    }





    @Override
        public DomainCoordinate clone()
        {
            DomainCoordinate dc;
            try
            {
                dc = (DomainCoordinate) super.clone();
            }
            catch (CloneNotSupportedException e)
            {
                throw new Error();
            }

            // Deep clone member fields here


            _name = dc._name;
            _dapId = dc._dapId;
            _units = dc._units;
            _arraySubset = dc._arraySubset;
            _size = dc._size;



            return dc;
        }

    @XmlAttribute(name="dapId")
    public String getDapID(){
        return _dapId;
    }

    @XmlAttribute    
    public String getName(){
        return _name;
    }

    @XmlAttribute 
    public String getUnits(){
        return _units;
    }

    public void setArraySubset(String arraySubset){
        _arraySubset = arraySubset;

    }

    public String getArraySubset(){
        return _arraySubset;
    }
    
    @XmlAttribute
    public long getSize(){
        return _size;
    }


}
