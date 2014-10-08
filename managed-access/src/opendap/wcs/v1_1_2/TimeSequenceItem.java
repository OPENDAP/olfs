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
package opendap.wcs.v1_1_2;

import org.jdom.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * From the Web Coverage Service (WCS) Implementation Standard version
 * 1.2.2 (document: OGC 07-067r5):
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
 *
 * User: ndp
 * Date: Aug 19, 2008
 * Time: 12:24:55 PM
 */
public class TimeSequenceItem {

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


    boolean _isTimePeriod;
    public void setIsTimePeriod(boolean val){
        _isTimePosition = !val;
        _isTimePeriod = val;
    }
    public boolean getIsTimePeriod(){
        return _isTimePeriod;
    }
    public boolean isTimePeriod(){
        return _isTimePeriod;
    }


    Date _timePosition;
    public void setTimePosition(Date tPos){
        _timePosition  = tPos;
    }
    public Date getTimePosition(){
        return _timePosition;
    }


    boolean _isTimePosition;
    public void setIsTimePosition(boolean val){
        _isTimePosition = val;
        _isTimePeriod = !val;
    }
    public boolean getIsTimePosition(){
        return _isTimePosition;
    }
    public boolean isTimePosition(){
        return _isTimePosition;
    }






    private void init(){
        // Makes the date parser strictly enforce the pattern.
        outputFormatter.setLenient(false);
        outputFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        _beginPosition = null;
        _endPosition = null;
        _timeResolution = null;
        _isTimePeriod = false;

        _timePosition = null;
        _isTimePosition = false;


    }


    TimeSequenceItem(String s) throws WcsException{
        init();
        String tmp[];
        // Is this time sequence a time range (with a possible time
        // period?
        tmp = s.split("/");
        if(tmp.length > 1){

            if(tmp.length!=3)
                throw new WcsException("It looks like you may have been " +
                        "attempting to specify a time range. You gave me '" +
                        s+"', which has the wrong number of fields. Time " +
                        "ranges must be expressed as start/stop/period where " +
                        "start, stop, and period are expressed in terms " +
                        "of ISO 8601 as described in section 9.3.2.4 of OGC " +
                        "document 07-067r5",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "wcs:TemporalSubset");

            _beginPosition = parseWCSTimePosition(tmp[0]);
            _endPosition   = parseWCSTimePosition(tmp[1]);
            _timeResolution = tmp[2];
            _isTimePeriod = true;
        }
        else {
            // It's not a range so it's a "time position"
            _timePosition = parseWCSTimePosition(s);
            _isTimePosition = true;

        }

    }


    TimeSequenceItem(Element tsi) throws WcsException{
        init();

        if(tsi.getName().equals("TimePeriod")  &&  tsi.getNamespace().equals(WCS.WCS_NS)){


            Element e = tsi.getChild("BeginPosition",WCS.WCS_NS);
            if(e==null) {
                throw new WcsException("The wcs:TimePeriod element must contain a wcs:BeginPosition element. ",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "wcs:TimePeriod");
            }
            _beginPosition = parseWCSTimePosition(e.getTextNormalize());

            e = tsi.getChild("EndPosition",WCS.WCS_NS);
            if(e==null) {
                throw new WcsException("The wcs:TimePeriod element must contain a wcs:EndPosition element. ",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "wcs:EndPosition");
            }
            _endPosition = parseWCSTimePosition(e.getTextNormalize());

            e = tsi.getChild("TimeResolution",WCS.WCS_NS);
            if(e!=null)
                _timeResolution = e.getTextNormalize();

            _isTimePeriod = true;

        }
        else if(tsi.getName().equals("timePosition")  &&  tsi.getNamespace().equals(WCS.GML_NS)){
            // It's na GML gml:timePosition
            _timePosition = parseWCSTimePosition(tsi.getTextNormalize());
            _isTimePosition = true;

        }
        else {
            throw new WcsException("The wcs:TemporalSubset element may only contain wcs:TimePeriod or " +
                    "gml:timePosition elements as children. I got a "+tsi.getNamespaceURI()+tsi.getName(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:TemporalSubset");

        }



    }


    public Element getXMLElementRepresentation() throws WcsException {
        Element item, e;

        if(_isTimePeriod){
            item = new Element("TimePeriod",WCS.WCS_NS);

            e = new Element("BeginPosition",WCS.WCS_NS);
            e.setText(outputFormatter.format(_beginPosition));
            item.addContent(e);

            e = new Element("EndPosition",WCS.WCS_NS);
            e.setText(outputFormatter.format(_endPosition));
            item.addContent(e);

            e = new Element("TimeResolution",WCS.WCS_NS);
            e.setText(_timeResolution);
            item.addContent(e);

        }
        else {
            if(!_isTimePosition)
                throw new WcsException("The TemporalSubset specified in the " +
                        "key value pairs is malformed and has caused the " +
                        "service to be unable to correctly interpret it's " +
                        "meaning. Pervious quality checks should have " +
                        "identified the problem at an earlier point, thus" +
                        "the fact that you are seeing this message means " +
                        "that BAD THINGS HAVE HAPPENED.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "TemporalSubset");

            item = new Element("timePosition",WCS.GML_NS);
            item.setText(outputFormatter.format(_timePosition));
        }
        return item;
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
