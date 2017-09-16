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

package opendap.wcs.v2_0;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import net.opengis.gml.v_3_2_1.*;
import opendap.wcs.srs.SimpleSrs;


/**
 * Thin wrapper of net.opengis.gml.v_3_2_1.EnvelopeWithTimePeriodType
 * facilitates setting of its parameters while iterating over 
 * the container attributes in DMR Dataset
 * @author Uday Kari
 *
 */
public class EnvelopeWithTimePeriod {

    private Vector<Double> _lowerCorner;
    private Vector<Double> _upperCorner;

	private String _beginTimePosition = "";
	private String _endTimePosition = "";


    public String toString(){
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());

        sb.append(": lowerCorner: [");
        for(double value:_lowerCorner)
            sb.append(" ").append(value);
        sb.append("]");
        sb.append(", upperCorner: [");
        for(double value:_upperCorner)
            sb.append(" ").append(value);
        sb.append("]");
        sb.append(", beginTime: ").append(_beginTimePosition);
        sb.append(", endTime: ").append(_endTimePosition);
        return sb.toString();
    }





    public EnvelopeWithTimePeriod(){
	    _lowerCorner = new Vector<>();
	    _upperCorner = new Vector<>();
    }
	/**
	 * Provides the OGC GML EnvelopeWithTimePeriodType object using 
	 * member variables that were captured while iterating 
	 * over container attributes of a DMR
	 */
	public EnvelopeWithTimePeriodType getEnvelope(SimpleSrs srs)
	{
        // EnvelopeWithTimePeriodType is part of GML
        net.opengis.gml.v_3_2_1.EnvelopeWithTimePeriodType envelope = new net.opengis.gml.v_3_2_1.EnvelopeWithTimePeriodType();
        



        ///////////////////////////////////////////////////////////////////////////////////////////////
        // default to EPSG 4326 - or WGS 84 - or use "SRS" instead of "CRS"
        // both are equivalent spatial reference systems for the ENTIRE globe

        envelope.setSrsName(srs.getName());
        List<String> axisLabelsAsList = srs.getAxisLabelsList();
        envelope.setAxisLabels(axisLabelsAsList);
        List<String> uomLabelsAsList = srs.getUomLabelsList();
        envelope.setUomLabels(uomLabelsAsList);
        envelope.setSrsDimension(BigInteger.valueOf(srs.getSrsDimension()));

        net.opengis.gml.v_3_2_1.DirectPositionType envelopeLowerCorner = new net.opengis.gml.v_3_2_1.DirectPositionType();
        envelopeLowerCorner.setValue(_lowerCorner);
        envelope.setLowerCorner(envelopeLowerCorner);

        DirectPositionType envelopeUpperCorner = new DirectPositionType();
        envelopeUpperCorner.setValue(_upperCorner);
        envelope.setUpperCorner(envelopeUpperCorner);

        TimePositionType beginTimePosition = new TimePositionType();
        // attribute called frame seems like right place to put ISO-8601 timestamp
        beginTimePosition.setFrame(_beginTimePosition);
        // However, it can also be specified as below.
        List<String> timeStrings = Arrays.asList(_beginTimePosition);
        beginTimePosition.setValue(timeStrings);
        envelope.setBeginPosition(beginTimePosition);

        TimePositionType endTimePosition = new TimePositionType();
        endTimePosition.setFrame(_endTimePosition);
        // However, it can also be specified as below.
        timeStrings = Arrays.asList(_endTimePosition);
        endTimePosition.setValue(timeStrings);
        envelope.setEndPosition(endTimePosition);
        
        return envelope;

	}

    // TODO Use the expected pattern and Date to parse time for QC
    public void setBeginTimePosition(String beginTime){
        _beginTimePosition = beginTime;
    }

    // TODO Use the expected pattern and Date to parse time for QC
    public void setEndTimePosition(String endTime){
        _endTimePosition = endTime;
    }

    public void addLowerCornerCoordinateValues(List<Double> values){
        _lowerCorner.addAll(values);

    }

    public void addUpperCornerCoordinateValues(List<Double> values){
        _upperCorner.addAll(values);
    }





}
