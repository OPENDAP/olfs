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

import net.opengis.gml.v_3_2_1.*;


/**
 * Thin wrapper of net.opengis.gml.v_3_2_1.EnvelopeWithTimePeriodType
 * facilitates setting of its parameters while iterating over 
 * the container attributes in DMR Dataset
 * @author Uday Kari
 *
 */
public class EnvelopeWithTimePeriod {

	private String southernmostLatitude = "";
	private String northernmostLatitude = "";
	private String westernmostLongitude = "";
	private String easternmostLongitude = "";
	
	private String rangeBeginningDate = "";
	private String rangeBeginningTime = "";
	private String rangeEndingDate = "";
	private String rangeEndingTime = "";
    
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
        List<Double> lowerCorner = Arrays.asList(new Double(southernmostLatitude), new Double(westernmostLongitude));
        envelopeLowerCorner.setValue(lowerCorner);
        envelope.setLowerCorner(envelopeLowerCorner);

        DirectPositionType envelopeUpperCorner = new DirectPositionType();
        List<Double> upperCorner = Arrays.asList(new Double(northernmostLatitude), new Double(easternmostLongitude));
        envelopeUpperCorner.setValue(upperCorner);
        envelope.setUpperCorner(envelopeUpperCorner);

        TimePositionType beginTimePosition = new TimePositionType();
        // attribute called frame seems like right place to put ISO-8601 timestamp
        String beginTimeStr = rangeBeginningDate + "T" + rangeBeginningTime + "Z"; 
        beginTimePosition.setFrame(beginTimeStr);
        // However, it can also be specified as below.
        List<String> timeStrings = Arrays.asList(beginTimeStr);
        beginTimePosition.setValue(timeStrings);
        envelope.setBeginPosition(beginTimePosition);

        TimePositionType endTimePosition = new TimePositionType();
        String endTimeStr = rangeEndingDate + "T" + rangeEndingTime + "Z";
        endTimePosition.setFrame(endTimeStr);
        // However, it can also be specified as below.
        timeStrings = Arrays.asList(beginTimeStr);
        endTimePosition.setValue(timeStrings);
        envelope.setEndPosition(endTimePosition);
        
        return envelope;

	}

	
	///////////////////////
	// getters and setters
	
	public String getSouthernmostLatitude() {
		return southernmostLatitude;
	}

	public void setSouthernmostLatitude(String southernmostLatitude) {
		this.southernmostLatitude = southernmostLatitude;
	}

	public String getNorthernmostLatitude() {
		return northernmostLatitude;
	}

	public void setNorthernmostLatitude(String northernmostLatitude) {
		this.northernmostLatitude = northernmostLatitude;
	}

	public String getWesternmostLongitude() {
		return westernmostLongitude;
	}

	public void setWesternmostLongitude(String westernmostLongitude) {
		this.westernmostLongitude = westernmostLongitude;
	}

	public String getEasternmostLongitude() {
		return easternmostLongitude;
	}

	public void setEasternmostLongitude(String easternmostLongitude) {
		this.easternmostLongitude = easternmostLongitude;
	}

	public String getRangeBeginningDate() {
		return rangeBeginningDate;
	}

	public void setRangeBeginningDate(String rangeBeginningDate) {
		this.rangeBeginningDate = rangeBeginningDate;
	}

	public String getRangeBeginningTime() {
		return rangeBeginningTime;
	}

	public void setRangeBeginningTime(String rangeBeginningTime) {
		this.rangeBeginningTime = rangeBeginningTime;
	}

	public String getRangeEndingDate() {
		return rangeEndingDate;
	}

	public void setRangeEndingDate(String rangeEndingDate) {
		this.rangeEndingDate = rangeEndingDate;
	}

	public String getRangeEndingTime() {
		return rangeEndingTime;
	}

	public void setRangeEndingTime(String rangeEndingTime) {
		this.rangeEndingTime = rangeEndingTime;
	}
	
	public String toString()
	{
		return "Envelope with Time Period " +
	           ", southernmostLatitude = "  + southernmostLatitude +
	           ", northernmostLatitude = "  + northernmostLatitude +
	           ", easternmostLongitude = "  + easternmostLongitude +
	           ", westernmostLongitude = "  + westernmostLongitude +
	           ", time from " + 
	           rangeBeginningDate + "T" + rangeBeginningTime + "Z" + 
	           " to " + 
	           rangeEndingDate + "T" + rangeEndingTime + "Z";
	}
	
}
