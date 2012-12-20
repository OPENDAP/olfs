/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.wcs.v2_0;


import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 12/19/12
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class TemporalDimensionSubset extends DimensionSubset {


    private String _units;


    public TemporalDimensionSubset(DimensionSubset ds, String units) {
        super(ds);
        _units = units;


    }

    public String getUnits(){ return _units; }


    @Override
    public String getDapValueConstraint() throws WcsException {
        StringBuilder subsetClause = new StringBuilder();

        if(isValueSubset()){

          switch (getType()) {
              case TRIM:

                  Date beginTime   = TimeConversion.parseWCSTimePosition(getTrimLow());
                  Date endTime     = TimeConversion.parseWCSTimePosition(getTrimHigh());

                  String beginPosition = TimeConversion.convertDateToTimeUnits(beginTime, getUnits());
                  String endPosition   = TimeConversion.convertDateToTimeUnits(endTime, getUnits());

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

                  String timeString = getSlicePoint();
                  Date timePoint =  TimeConversion.parseWCSTimePosition(timeString);
                  String timePosition = TimeConversion.convertDateToTimeUnits(timePoint, getUnits());

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








}
