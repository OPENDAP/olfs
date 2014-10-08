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


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 5, 2010
 * Time: 8:31:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class TimeConversion {



    // @todo Implement time format conversion to build correct dap constraints. Need units for dap time variable.


    /*
    Examples:

   COARDS TIME:
   <pre>
   <Array name="TIME">
       <Attribute name="units" type="String">
           <value>hour since 0000-01-01 00:00:00</value>
       </Attribute>
       <Attribute name="time_origin" type="String">
           <value>1-JAN-0000 00:00:00</value>
       </Attribute>
       <Attribute name="modulo" type="String">
           <value> </value>
       </Attribute>
       <Float64/>
       <dimension name="TIME" size="12"/>
   </Array>



   ECMWF


   <Array name="time">
       <Attribute name="units" type="String">
           <value>hours since 1900-01-01 00:00:0.0</value>
       </Attribute>
       <Attribute name="long_name" type="String">
           <value>time</value>
       </Attribute>
       <Int32/>
       <dimension name="time" size="62"/>
   </Array>



   HFRADAR

   <Array name="time">
       <Attribute name="standard_name" type="String">
           <value>time</value>
       </Attribute>
       <Attribute name="units" type="String">
           <value>seconds since 1970-01-01</value>
       </Attribute>
       <Attribute name="calendar" type="String">
           <value>gregorian</value>
       </Attribute>
       <Int32/>
       <dimension name="time" size="1"/>
   </Array>



   Roy's PH file

       <Map name="time">
           <Attribute name="actual_range" type="Float64">
               <value>1139961600.0000000</value>
               <value>1139961600.0000000</value>
           </Attribute>
           <Attribute name="fraction_digits" type="Int32">
               <value>0</value>
           </Attribute>
           <Attribute name="long_name" type="String">
               <value>Centered Time</value>
           </Attribute>
           <Attribute name="units" type="String">
               <value>seconds since 1970-01-01T00:00:00Z</value>
           </Attribute>
           <Attribute name="standard_name" type="String">
               <value>time</value>
           </Attribute>
           <Attribute name="axis" type="String">
               <value>T</value>
           </Attribute>
           <Attribute name="_CoordinateAxisType" type="String">
               <value>Time</value>
           </Attribute>
           <Float64/>
           <dimension name="time" size="1"/>
       </Map>






    */
    public static void main(String[] args) throws ParseException {

        GregorianCalendar gc = new GregorianCalendar();
        String timeUnits;
        Date epoch;
        Date now = new Date();

        String dateFormat = "yyyy-MM-d hh:mm:ss";


        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String epochDateStr = "1900-01-01 00:00:0.0";
        epoch = sdf.parse(epochDateStr);

        System.out.println("epoch date string: "+epochDateStr);
        System.out.println("epoch:      "+epoch);
        System.out.println("epoch.getTime():      "+epoch.getTime());


        gc.setTime(epoch);
        System.out.println("    Epoch GregorianCalendar: " + gc);
        System.out.println(showTime(gc, "        "));


        timeUnits =  "hours since "+epochDateStr;
        epoch = getEpoch(timeUnits);
        System.out.println("    getEpoch(\""+timeUnits+"\") returned: " + epoch);
        gc.setTime(epoch);
        System.out.println("    getEpoch() GregorianCalendar: " + gc);
        System.out.println(showTime(gc, "        "));

        System.out.println("    convertDateToTimeUnits(epoch,timeUnits): " + convertDateToTimeUnits(epoch, timeUnits));



        try {
            System.out.println("    timeUnits: '" + timeUnits +"'");
            String timePosStr = "2002-07-18T12:00Z";
            System.out.println("    timePosStr: " + timePosStr);
            Date timePoint  = parseWCSTimePosition(timePosStr);
            System.out.println("    timePoint: " + timePoint);
            System.out.println("    convertDateToTimeUnits(timePoint): " + convertDateToTimeUnits(timePoint, timeUnits));
        } catch (WcsException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }






        gc.setTime(now);
        System.out.println("    NOW GregorianCalendar: " + gc);
        System.out.println(showTime(gc, "        "));






        System.out.println("########################################################");
        System.out.println("Testing time units conversion...");

        timeUnits = "hour since 0000-01-01 00:00:00";
        testTimeConversion(now, timeUnits);


        timeUnits = "hours since 1900-01-01 00:00:0.0";
        testTimeConversion(now, timeUnits);

        timeUnits = "seconds since 1970-01-01";
        testTimeConversion(now, timeUnits);

        timeUnits = "seconds since 1970-01-01T00:00:00Z";
        testTimeConversion(now, timeUnits);

        //epoch = new Date((long)1139961600*1000);

        //testTimeConversion(epoch, timeUnits);


        System.out.println("########################################################");
        System.out.println("Parsing WCS (ISO-8601) Dates.");


        String wcsDate = "2002-07-18T10:00Z";

        testWcsTimeStringParsing(wcsDate,timeUnits);


    }

    public static void testWcsTimeStringParsing(String wcsDateString, String timeUnits){

        System.out.println("wcsDateString: "+wcsDateString);

        try {
            Date date = parseWCSTimePosition(wcsDateString);
            System.out.println("    parsed to: "+date);

            testTimeConversion(date, timeUnits);




        } catch (WcsException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    public static void testTimeConversion(Date date, String timeUnits){

        GregorianCalendar gc = new GregorianCalendar();
        Date epoch = getEpoch(timeUnits);


        System.out.println("Testing Date: "+date);
        System.out.println("    timeUnits:     " + timeUnits);
        System.out.println("    epoch is:      " + epoch);
        System.out.println("    convertDateToTimeUnits(): " + convertDateToTimeUnits(date, timeUnits));

        gc.setTime(epoch);
        System.out.println("    GregorianCalendar: " + gc);
        System.out.println(showTime(gc, "        "));



    }


    /**
     * Converts the supplied date into what ever units/epoch is supplied in the timUnits string.
     * Time units are typically expressed as a string that says "timeInterval since epochDate" where time interval is
     * one of days/hours/minutes/seconds/milliseconds and the epochDate may have one of
     * several forms. See getEpoch() for more details on supported epoch date representations.
     * @param time The time instance to convert.
     * @param timeUnits  The time units to convert it to.
     * @return  A string representation of the submitted time after conversion to the supplied time units.
     * @see TimeConversion
     */
    public static String convertDateToTimeUnits(Date time, String timeUnits){

        // Trim leading whitespace from timeUnits string
        while(timeUnits.startsWith(" ") || timeUnits.startsWith("\t"))
            timeUnits = timeUnits.substring(1);


        // Retrieve the epoch date from the time units string
        Date epoch = getEpoch(timeUnits);

        // Get the epoch time in ms since whatever.
        long eTime = epoch.getTime();

        // Get the time we want to represent in ms since the same whatever
        long thisTime = time.getTime();

        // Compute the time, in ms, of the supplied time point from the epoch.
        long elapsedTime = thisTime - eTime;


        // Convert the elapsed time from ms to whatever it is the time units string asked for.
        if(timeUnits.startsWith("second")){
            elapsedTime = elapsedTime/1000;
        }
        else if(timeUnits.startsWith("minute")){
            elapsedTime = elapsedTime/1000/60;
        }
        else if(timeUnits.startsWith("hour")){
            elapsedTime = elapsedTime/1000/60/60;
        }
        else if(timeUnits.startsWith("day")){
            elapsedTime = elapsedTime/1000/60/60/24;
        }


        return Long.toString(elapsedTime);

    }

    /**
     * Time units are typically expressed as a string that says "timeInterval since epochDate" where time interval is
     * one of years/months/weeks/days/hours/minutes/seconds/milliseconds/microseconds and the epochDate may have one of
     * several forms.
     *
     * This method can parse the following epoch date expressions:
     * <ul>
     *     <li>yyyy-MM-d hh:mm:ssTZ</li>
     *     <li>yyyy-MM-d'T'hh:mm:ssTZ</li>
     *     <li>yyyy-MM-dTZ</li>
     * </ul>
     * Where TZ is an optional Time Zone designation.
     * @param timeUnits a string containing the time units as described above
     * @return Returns the epoch date as defined in the passed time units string.
     */
    static Date getEpoch(String timeUnits) {

        Date epochDate;
        if(timeUnits.contains("since")){


            // find the end of the word "since"
            int startIndex = timeUnits.lastIndexOf("since") + "since".length();

            // grab everything after "since"
            String epoch = timeUnits.substring(startIndex);

            // Trim the string of leading whitespace
            while(epoch.startsWith(" ") || epoch.startsWith("\t"))
                epoch = epoch.substring(1);


            // Let's try to figure out if we have a partial or complete date
            // string.

            // Does it end in a timeZone?
            SimpleTimeZone stz = null;
            String tzones[] = TimeZone.getAvailableIDs();
            for(String tz:tzones){
                if(epoch.endsWith(tz)){
                    stz = new SimpleTimeZone(0,tz);
                    epoch = epoch.replace(tz,"");
                    break;
                }
            }
            if(stz==null){
                // It doesn't appear to end in a timeZone, but since people often
                // use the "Z" timeZone designation, lets check for that and
                // replace it with UTC, which the DateFormat classes can
                // correctly parse.
                if(epoch.endsWith("Z")){
                    epoch = epoch.replace("Z","");
                    stz = new SimpleTimeZone(0,"UTC");
                }
                else {
                    stz = new SimpleTimeZone(0,"GMT");
                }
            }

            SimpleDateFormat sdf;
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeZone(stz);

            String dateFormat = "yyyy-MM-d hh:mm:ss";

            try {
                sdf = new SimpleDateFormat(dateFormat);
                sdf.setCalendar(gc);
                epochDate = sdf.parse(epoch);
            } catch (ParseException e) {

                dateFormat = "yyyy-MM-d'T'hh:mm:ss";
                sdf = new SimpleDateFormat(dateFormat);
                sdf.setCalendar(gc);
                try {
                    epochDate = sdf.parse(epoch);
                } catch (ParseException e1) {

                    dateFormat = "yyyy-MM-d";
                    sdf = new SimpleDateFormat(dateFormat);
                    sdf.setCalendar(gc);
                    try {
                        epochDate = sdf.parse(epoch);
                    }
                    catch (ParseException e2) {
                        epochDate = new Date((long)0);
                    }
                }
            }

        }
        else {
            epochDate =  new Date((long)0);
        }


        return epochDate;
    }

    public static String  showTime(Calendar gc, String indent){

        StringBuilder sb = new StringBuilder();
        Date time = gc.getTime();

        long timInt =  time.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");


        sb.append(indent).append("time.getTime(): ").append(timInt).append("\n");

        sb.append(indent).append("Year:        ").append(gc.get(Calendar.YEAR)).append("\n");
        sb.append(indent).append("Month:       ").append(gc.get(Calendar.MONTH)).append("\n");
        sb.append(indent).append("Day:         ").append(gc.get(Calendar.DAY_OF_MONTH)).append("\n");
        sb.append(indent).append("Hour:        ").append(gc.get(Calendar.HOUR)).append("\n");
        sb.append(indent).append("Minute:      ").append(gc.get(Calendar.MINUTE)).append("\n");
        sb.append(indent).append("Second:      ").append(gc.get(Calendar.SECOND)).append("\n");
        sb.append(indent).append("Millisecond: ").append(gc.get(Calendar.MILLISECOND)).append("\n");
        sb.append(indent).append(sdf.format(time)).append("\n");



        return sb.toString();

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


            if(stz==null){
                // It doesn't appear to end in a timeZone, but since WCS allows the
                // use of the "Z" timeZone designation, lets check for that and
                // replace it with UTC, which the DateFormat classes can
                // correctly parse.
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
