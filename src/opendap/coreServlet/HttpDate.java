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

package opendap.coreServlet;

import java.util.Date;
import java.util.SimpleTimeZone;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.text.FieldPosition;

/**
 * This class provides methods for dealing with HTTP/1.1 dates.
 * User: ndp
 * Date: Aug 22, 2006
 * Time: 5:52:32 PM
 */
public class HttpDate {

    /**
     * Indicates to <code>getHttpDate()</code> to use RFC-1123 date format when returning
     * the string representation of a date.
     *
     * @see #getHttpDateString(java.util.Date)
     */
    public static final int       RFC_1123 = 1;


    /**
     * Indicates to <code>getHttpDate()</code> to use RFC-850 date format when returning
     * the string representation of a date.
     *
     * @see #getHttpDateString(java.util.Date)
     */
    public static final int        RFC_850 = 2;


    /**
     * Indicates to <code>getHttpDate()</code> to use ANSI C asctime() function date format
     * when returning the string representation of a date.
     *
     * @see #getHttpDateString(java.util.Date)
     */
    public static final int ANSI_C_ASCTIME_DATE = 3;


    /**
     * Time format string representing a date based on RFC 1123. Use this with a SimplDateFormat object.
     * <pre>
     *          Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123
     * </pre>
     */
    public static final String rfc1123DateFormat = "E, dd MMM yyyy HH:mm:ss z";

    /**
     * Time format string representing a date based on RFC 1123. Use this with a SimplDateFormat object.
     * <pre>
     *          Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036
     * </pre>
     */
    public static final String rfc850DateFormat  = "E, dd-MMM-yy HH:mm:ss z";

    /**
     * Time format string representing a date based on RFC 1123. Use this with a SimplDateFormat object.
     * <pre>
     *          Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format
     * </pre>
     */
    public static final String asctimeDateFormat = "E MMM d HH:mm:ss yyyy";

    /**
     * This method converts a string containing an HTTP date as described in RFC 2616 into
     * a java.util.Date object. It attempts to parse the passed string as an RFC 1123 date.
     * If this fails it attempts to parse it as an RFC 850 date. If that fails it attempts
     * to parse it as an ANSI C asctime() formatted date. If that fails, then the method returns null.
     *
     * <pre>
     * HTTP-date    = rfc1123-date | rfc850-date | asctime-date
     * rfc1123-date = wkday "," SP date1 SP time SP "GMT"
     * rfc850-date  = weekday "," SP date2 SP time SP "GMT"
     * asctime-date = wkday SP date3 SP time SP 4DIGIT
     * date1        = 2DIGIT SP month SP 4DIGIT
     *                ; day month year (e.g., 02 Jun 1982)
     * date2        = 2DIGIT "-" month "-" 2DIGIT
     *                ; day-month-year (e.g., 02-Jun-82)
     * date3        = month SP ( 2DIGIT | ( SP 1DIGIT ))
     *                ; month day (e.g., Jun  2)
     * time         = 2DIGIT ":" 2DIGIT ":" 2DIGIT
     *                ; 00:00:00 - 23:59:59
     * wkday        = "Mon" | "Tue" | "Wed"
     *              | "Thu" | "Fri" | "Sat" | "Sun"
     * weekday      = "Monday" | "Tuesday" | "Wednesday"
     *              | "Thursday" | "Friday" | "Saturday" | "Sunday"
     * month        = "Jan" | "Feb" | "Mar" | "Apr"
     *              | "May" | "Jun" | "Jul" | "Aug"
     *              | "Sep" | "Oct" | "Nov" | "Dec"
     * </pre>
     *
     * @param dateString A String containing an HTTP date.
     * @return The Date object represented by the passed String, null if the string fails to parse.
     */
    public static Date getHttpDate(String dateString){

        SimpleDateFormat sdf;
        Date d;

        // Try to read it as an RFC1123 (formerly RFC822) date.
        sdf = new SimpleDateFormat(rfc1123DateFormat);
        d = sdf.parse(dateString, new ParsePosition(0));

        // Did it work?
        if(d==null){

            //System.out.println("Not RFC-1123, trying RFC-850...");
            // No. Try to read it as an RFC 850 date.
            sdf = new SimpleDateFormat(rfc850DateFormat);
            d = sdf.parse(dateString, new ParsePosition(0));

        }

        // Did that work?
        if(d==null){
            //System.out.println("Not RFC-850, trying ANSI C asctime() format...");

            // No. Try to read it as an ANSI C asctime() formated date.

            sdf = new SimpleDateFormat(asctimeDateFormat);

            // Since the asctime() string contains no time zone info, we add it
            // so that in the event that this works then the timezone is correctly
            // identified as GMT.
            sdf.setTimeZone(new SimpleTimeZone(0,"GMT"));

            d = sdf.parse(dateString, new ParsePosition(0));

        }

        // Return it, if it's null, then they sent crap.
        return d;


    }


    /**
     * Converts a java.util.Date to an HTTP/1.1 date string. In particular, this will
     * return a date formatted accirding to RFC 1123.
     *
     * @param d The Date we want to get an HTTP representation for.
     * @return The HTTP/1.1 date string represented by the passed date.
     */
    public static String getHttpDateString(Date d){

        return getHttpDateString(d,RFC_1123);
    }


    /**
     * Converts a java.util.Date to an HTTP/1.1 date string. The format of the date string
     * may be compliant with RFC-1123, RFC-850, or ANSI C asctime() function output. THis
     * is specified in the passed parameter <code>format</code>.
     * <ul>
     *    <li> See <a href="http://tools.ietf.org/html/rfc1123>RFC 1123</a></li>
     *    <li> See <a href="http://tools.ietf.org/html/rfc850>RFC 850</a></li>
     *    <li> See ANSI C ASCTIME DATE functions</li>
     * </ul>
     * @param d The Date we want to get an HTTP representation for.
     * @param format The format id for the format to use in representing the date as a String.
     * @return The HTTP/1.1 date string represented by the passed date.
     */
    public static String getHttpDateString(Date d, int format){

        if(d==null)
            return null;


        //System.out.println("From Date: "+d);

        SimpleDateFormat sdf;

        switch(format){
            case RFC_1123:
                sdf = new SimpleDateFormat(rfc1123DateFormat);
                break;

            case RFC_850:
                sdf = new SimpleDateFormat(rfc850DateFormat);
                break;

            case ANSI_C_ASCTIME_DATE:
                sdf = new SimpleDateFormat(asctimeDateFormat);
                break;

            default:
                return null;

        }

        sdf.setTimeZone(new SimpleTimeZone(0,"GMT"));
        StringBuffer date = sdf.format(d,new StringBuffer(),new FieldPosition(0));

        //System.out.println("Http Date: "+date);

        return date.toString();
    }

    /**
     * Provides a simple experiments of this classes date handling methods.
     * @param args Um... arguments!
     */
    public static void main(String args[]){

        String rfc1123Date = "Sun, 06 Nov 1994 08:49:37 GMT";
        String rfc850Date  = "Friday, 19-May-95 21:12:09 GMT";
        String asctimeDate = "Thu Jun  22 06:34:21 1961";


        String date = getHttpDateString(new Date());

        System.out.println("getHttpDate(Date):             "+date);

        Date d = getHttpDate(date);
        System.out.println("getHttpDate(String):           "+d);

        d = getHttpDate(rfc1123Date);
        System.out.println("getHttpDate(RFC-1123 String):  "+d);

        d = getHttpDate(rfc850Date);
        System.out.println("getHttpDate(RFC-850 String):   "+d);

        d = getHttpDate(asctimeDate);
        System.out.println("getHttpDate(asctime() String): "+d);

    }

}
