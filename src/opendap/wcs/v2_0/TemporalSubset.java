/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////
package opendap.wcs.v2_0;

import org.slf4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 */
public class TemporalSubset extends DimensionSubset {


    private Logger log;

    private final SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");


    private Date _beginPosition;
    public void setBeginPosition(Date beginPos){
        _beginPosition  = beginPos;
    }
    public Date getBeginPosition(){
        return _beginPosition;
    }


    private Date _endPosition;
    public void setEndPosition(Date beginPos){
        _endPosition  = beginPos;
    }
    public Date getEndPosition(){
        return _endPosition;
    }

    private String _timeResolution;
    public void setTimeResolution(String timeRes){
        _timeResolution = timeRes;
    }
    public String getTimeResolution(){
        return _timeResolution;
    }


    Date _timePosition;
    public void setTimePosition(Date tPos){
        _timePosition  = tPos;
    }
    public Date getTimePosition(){
        return _timePosition;
    }



    public TemporalSubset(String kvpSubsetString) throws WcsException {


        super(kvpSubsetString);
        // Makes the date parser strictly enforce the pattern.
        outputFormatter.setLenient(false);
        outputFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        _beginPosition = null;
        _endPosition = null;
        _timeResolution = null;
        _timePosition = null;




        if(isTrimSubset()){
            String beginTimePosStr = super.getTrimLow();
            _beginPosition = parseWCSTimePosition(beginTimePosStr);

            String endTimePosStr = super.getTrimHigh();
            _endPosition = parseWCSTimePosition(endTimePosStr);

        }
        else if(isSliceSubset()){
            String timePosStr = this.getSlicePoint();
            _timePosition = parseWCSTimePosition(timePosStr);

        }
        else {
            throw new WcsException("Cannot build TemporalSubset. " +
                    "Unknown subsetting pattern",WcsException.NO_APPLICABLE_CODE);
        }
    }






    /**
     * Returns a time subset string for the geogrid() server side function.
     *
     * Since geogrid() does not implement time semantics that must be handled here.
     *
     * @param dapTimeVar The name of the time variable.
     * @return
     * @throws WcsException
     */
    public String getDapGeogridFunctionTimeSubset(String dapTimeVar, String timeUnits)throws WcsException {

        String dapTemporalSubset = null;

        TimeSequenceItem[] _timeTequenceItems=null;

        for(TimeSequenceItem tsi: _timeTequenceItems){

            if(dapTemporalSubset != null)
                dapTemporalSubset += ",";

            if(tsi.isTimePeriod()){
                String beginPosition = TimeConversion.convertDateToTimeUnits(tsi.getBeginPosition(), timeUnits);
                String endPosition = TimeConversion.convertDateToTimeUnits(tsi.getEndPosition(), timeUnits);

                dapTemporalSubset += "\""+beginPosition + "<="+dapTimeVar+"<="+endPosition+"\"";

            }
            else if(tsi.isTimePosition()){
                String timePosition = TimeConversion.convertDateToTimeUnits(tsi.getTimePosition(), timeUnits);
                dapTemporalSubset += "\""+timePosition + "="+dapTimeVar+"\"";
            }
            else {
                log.error("getDapGeogridFunctionTimeSubset(): TimeSequence is neither a TimePeriod or " +
                        "TimePosition! This should never ever happen.");
                throw new WcsException("TimeSequence is neither a " +
                        "TimePeriod or TimePosition! This should " +
                        "never ever happen.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "TimeSequence");

            }
        }
        return dapTemporalSubset;

    }





    /**
     * From the Web Coverage Service (WCS) Implementation Standard version
     * 1.1.2 (document: OGC 07-067r5):
     * <p>
     * 9.3.2.4 Summary of ISO 8601 syntax for time positions and time periods
     * The [ISO 8601:2000] syntax for dates and times may be summarized by the
     * following template (see Annex D of the OGC Web Map Service
     * [OGC 06-042]): </p>
     * <pre>         ccyy-mm-dd'T'hh:mm:ss:sssZ</pre>
     * <p>
     * Where:
     * <ul>
     * <li>ccyy-mm-dd" is the date (a four digit year, and a two-digit month
     * and day);</li>
     * <li>"T" is a separator between the date and time strings;</li>
     * <li>"hh:mm:ss.sss" is the time ( a two digit hour and minute, and
     * fractional seconds);</li>
     * <li>"Z" represents the Coordinate Universal Time (UTC or "zulu") time
     * zone;</li>
     * </ul>
     * ISO 8601:2000 allows (i) up to a 14-digit year, with a negative sign,
     * to denote the distant past; (ii) omitting less-significant numbers in
     * the template to reduce the precision of the date and time; and (iii) use
     * of other time zones, denoted as in Subclause 5.3.4.1 of ISO8601:2000.
     * Here a few examples:
     * <table>
     * <tr><td>EXAMPLE 1</td><td>2006</td><td>The year 2006</td></tr>
     * <tr><td>EXAMPLE 2</td><td>2006-09</td><td>September 2006</td></tr>
     * <tr><td>EXAMPLE 3</td><td>2006-09-27T10:00Z</td><td>10 o'clock
     * (Universal Time) on 27 September, 2006</td></tr>
     * </table>
     * ISO 8601:2000 also provides a syntax for expressing time periods: the
     * designator P, followed by a number of years Y, months M, days D, a time
     * designator T, number of hours H, minutes M, and seconds S. Unneeded
     * elements may be omitted. Here are a few examples:
     *
     * <table>
     * <tr><td>EXAMPLE 4</td><td>P1Y</td><td>1 year</td></tr>
     * <tr><td>EXAMPLE 5</td><td>P1M10D</td><td>1 month plus 10 days</td></tr>
     * <tr><td>EXAMPLE 6</td><td>PT2H</td><td>2 hours</td></tr>
     * </table>
     *
     * The WCS GetCoverage KVP syntax (defined in 10.2.2) extends ISO 8601:2000
     * with a syntax for time lists and ranges, as specified in Annex D of
     * [OGC 06-042]. Some examples follow:
     * <table>
     * <tr><td>EXAMPLE 7</td><td>2000-06-23T20:07:48.11Z</td>
     * <td>A single moment.</td></tr>
     * <tr><td>EXAMPLE 8</td><td>1999-01-01,1999-04-01,1999-07-01,1999-10-01
     * </td><td>A list of four dates</td></tr>
     * <tr><td>EXAMPLE 9</td><td>1995-04-22T12:00Z/2000-06-21T12:00Z/P1D</td>
     * <td>Daily ("P1D") between 12 noon April 22, 1995 and 12 noon June
     * 21,2000</td></tr>
     * </table>
     *
     *
     * "yyyy-MM-dd'T'HH:mm:ss:SSSZ"
     * @param t The time string to parse.
     * @return The Date object that represents the passed time.
     * @throws WcsException When the time string cannot be correctly interpreted.
     */
    public static Date parseWCSTimePosition(String t) throws WcsException {

        SimpleDateFormat parseFormat;
        Date d;
        String time, parseFormatString;
        String[] tmp1, tmp2;

        try {

            // Let's try to figure out if we have a partial or complete date
            // string.

            // Does it end in a timeZone?
            SimpleTimeZone stz = null;
            String tzones[] = TimeZone.getAvailableIDs();
            for(String tz:tzones){
                if(t.endsWith(tz)){
                    stz = new SimpleTimeZone(0,tz);
                    t = t.replace(tz,"");
                    break;
                }
            }
            // It doesn't appear to end in a timeZone, but since WCS allows the
            // use of the "Z" timeZone designation, lets check for that and
            // replace it with UTC, which the DateFormat classes can
            // correctly parse.
            if(stz==null){
                if(t.endsWith("Z")){
                    t = t.replace("Z","");
                    stz = new SimpleTimeZone(0,"UTC");
                }
            }

            // At this point the timezone, if present, has been removed from
            // the end of the time string. This means that the only "T" that
            // should appear in the time is the separator between the date
            // and time sections.
            tmp1 = t.split("T");

            // Did we only get a date?
            if(tmp1.length==1){

                tmp2 = t.split("-");

                parseFormatString = "y";

                if(tmp2.length==2)
                    parseFormatString += "-M";
                if(tmp2.length==3)
                    parseFormatString += "-M-d";
                if(tmp2.length>3){
                    throw new WcsException("It appears you have attempted to " +
                            "specify a simple date ("+tmp1[0]+") as part of a " +
                            "TemporalSubset. There appear to be more than 3 " +
                            "fields separated by '-' signs. That's just not " +
                            "going to work. Sorry.",
                            WcsException.INVALID_PARAMETER_VALUE,
                            "TemporalSubset");
                }

                parseFormat = new SimpleDateFormat(parseFormatString);
                if(stz!=null)
                    parseFormat.setTimeZone(stz);

                d = parseFormat.parse(t);
            }
            else {
                // Looks like we got a date and a time of some sort.

                parseFormatString = "y-M-d'T'H";

                time = tmp1[1];

                tmp2 = time.split(":");


                if(tmp2.length==2)
                    parseFormatString += ":m";
                if(tmp2.length==3) {
                    parseFormatString += ":m:s";

                    if(tmp2[2].contains("."))
                        parseFormatString += ".S";

                }
                if(tmp2.length>3) {
                    throw new WcsException("It appears you have attempted to " +
                            "specify a time ("+time+") as part of a " +
                            "TemporalSubset. There appear to be more than 3 " +
                            "fields separated by ':' signs. That's just not " +
                            "going to work. Sorry.",
                            WcsException.INVALID_PARAMETER_VALUE,
                            "TemporalSubset");
                }

                parseFormat = new SimpleDateFormat(parseFormatString);
                if(stz!=null)
                    parseFormat.setTimeZone(stz);

                d = parseFormat.parse(t);

            }

        }
        catch (ParseException e) {
            throw new WcsException(e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "TemporalSubset");
        }

        return d;
    }





    
}
