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


import java.util.Date;

/**
 * This class extends DimensionSubset concept to correctly handle time strings.
 *
*/
public class TemporalDimensionSubset  extends DimensionSubset implements Cloneable {


    private String _units;


    public TemporalDimensionSubset(DimensionSubset ds) {
        super(ds);
        _units = null;
    }

    public TemporalDimensionSubset(TemporalDimensionSubset tds) {
        super(tds);
        _units = tds._units;
    }

    public TemporalDimensionSubset(DimensionSubset ds, String units) {
        super(ds);
        _units = units;


    }

    public String getUnits(){ return _units; }


    /**
     * The is method produces the correct DAP value based constraint for inclusion in a DAP constraint expression
     * call to the DAP server side function "grid"
     *
     * If the time point/slice values are numeric (if they parse as a Double correctly) then they are assumed to be in
     * the target dataset time units and they are added to the constraint unchanged. If the time point/slice values are
     * not numeric then they are passed to the TimeConversion.parseWCSTimePosition() utility where they are turned into
     * java Date objects. Each Date is passed into the TimeConversion.convertDateToTimeUnits() method along with the
     * time units of the coverage/dataset. The returned string is placed in the DAP value based constraint for inclusion
     * into a call to the server side function "grid".
     * @return
     * @throws WcsException
     */
    @Override
    public String getDap2GridValueConstraint() throws WcsException {
        StringBuilder subsetClause = new StringBuilder();

        if (isValueSubset()) {

            switch (getType()) {

                case TRIM:

                    String beginPosition = getTrimLow();
                    if (!smellsLikeFloat(beginPosition)) {
                        Date beginTime = TimeConversion.parseWCSTimePosition(beginPosition);
                        beginPosition = TimeConversion.convertDateToTimeUnits(beginTime, getUnits());
                    }

                    String endPosition = getTrimHigh();
                    if (!smellsLikeFloat(endPosition)) {
                        Date endTime = TimeConversion.parseWCSTimePosition(getTrimHigh());
                        endPosition = TimeConversion.convertDateToTimeUnits(endTime, getUnits());
                    }

                    subsetClause
                            .append("\"")
                            .append(beginPosition)
                            .append("<=")
                            .append(getDimensionId())
                            .append("<=")
                            .append(endPosition)
                            .append("\"");

                    break;


                case SLICE_POINT:

                    String timePosition = getSlicePoint();
                    if (!smellsLikeFloat(timePosition)) {
                        Date timePoint = TimeConversion.parseWCSTimePosition(timePosition);
                        timePosition = TimeConversion.convertDateToTimeUnits(timePoint, getUnits());
                    }


                    subsetClause
                            .append("\"")
                            .append(getDimensionId())
                            .append("=")
                            .append(timePosition)
                            .append("\"");


                    break;
                default:
                    throw new WcsException("Unknown Subset Type!", WcsException.INVALID_PARAMETER_VALUE, "subset");
            }

        }

        return subsetClause.toString();

    }


    private boolean smellsLikeFloat(String s){
        try {
            Double.parseDouble(s);
        }
        catch(NumberFormatException e){

            return false;
        }

        return true;

    }



    public Date getStartTime() throws WcsException {
        return TimeConversion.parseWCSTimePosition(getTrimLow());

    }
    public Date getEndTime() throws WcsException {
        return TimeConversion.parseWCSTimePosition(getTrimHigh());
    }

    public Date getSlicePointTime() throws WcsException {
        return TimeConversion.parseWCSTimePosition(getSlicePoint());
    }

    }
