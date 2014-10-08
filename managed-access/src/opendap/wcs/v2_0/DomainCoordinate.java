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

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 12/19/12
 * Time: 1:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class DomainCoordinate {


    private String name;
    private String dapID;
    private String units;
    private String _arraySubset;

    DomainCoordinate(Element dc) throws ConfigurationException {

        name   = dc.getAttributeValue("name");
        if(name==null){
            throw new ConfigurationException("In the configuration a DomainCoordinate element is " +
                    "missing the required attribute 'name'.");
        }

        dapID  = dc.getAttributeValue("dapID");
        if(dapID==null){
            throw new ConfigurationException("In the configuration a DomainCoordinate element is " +
                    "missing the required attribute 'dapID'.");
        }

        units  = dc.getAttributeValue("units");
        if(units==null){
            throw new ConfigurationException("In the configuration a DomainCoordinate element is " +
                    "missing the required attribute 'units'.");
        }


    }

    DomainCoordinate(DomainCoordinate dc){

        name   = dc.getName();
        dapID  = dc.getDapID();
        units  = dc.getUnits();

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


            name   = dc.getName();
            dapID  = dc.getDapID();
            units  = dc.getUnits();
            _arraySubset = null;



            return dc;
        }


    public String getDapID(){
        return dapID;
    }


    public String getName(){
        return name;
    }


    public String getUnits(){
        return units;
    }

    public void setArraySubset(String arraySubset){
        _arraySubset = arraySubset;

    }

    public String getArraySubset(){
        return _arraySubset;
    }


}
