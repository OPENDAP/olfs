/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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

/**
 * Created by ndp on 9/22/16.
 */
public class CoordinateDimension { //implements Cloneable {

    private double _lowerBound;
    private double _upperBound;
    private String _name;



    CoordinateDimension(){
        _name = null;
        _lowerBound = Double.NaN;
        _upperBound = Double.NaN;

    }

    CoordinateDimension(CoordinateDimension d){
        _name = d._name;
        _lowerBound = d._lowerBound;
        _upperBound = d._upperBound;

    }


    CoordinateDimension(String name, double min, double max) throws WcsException{
        setName(name);
        setMin(min);
        setMax(max);
    }

    /*
    @Override
    public CoordinateDimension clone() throws CloneNotSupportedException {
        CoordinateDimension cd;
        cd = (CoordinateDimension) super.clone();
        // Deep clone member fields here
        cd._name = _name;
        cd._lowerBound = _lowerBound;
        cd._upperBound = _upperBound;
        return cd;
    }
   */

    public void setMin(double min){ _lowerBound = min; }
    public double getMin(){ return _lowerBound;}

    public void setMax(double max){ _upperBound = max; }
    public double getMax(){ return _upperBound;}

    public String getName() { return _name;}

    public void setName(String name) throws WcsException {
        _name = name;
    }

}
