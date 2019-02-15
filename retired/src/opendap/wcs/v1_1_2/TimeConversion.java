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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

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


        timeUnits = "hour since 0000-01-01 00:00:00";
        System.out.println("Converted Time: "+convertTime(new Date(), timeUnits));
        epoch = getEpoch(timeUnits);
        gc.setTime(epoch);
        showTime(gc);

        timeUnits = "hours since 1900-01-01 00:00:0.0";
        System.out.println("Converted Time: "+convertTime(new Date(), timeUnits));
        epoch = getEpoch(timeUnits);
        gc.setTime(epoch);
        showTime(gc);

        timeUnits = "seconds since 1970-01-01";
        System.out.println("Converted Time: "+convertTime(new Date(), timeUnits));
        epoch = getEpoch(timeUnits);
        gc.setTime(epoch);
        showTime(gc);

        timeUnits = "seconds since 1970-01-01T00:00:00Z";
        System.out.println("Converted Time: "+convertTime(new Date(), timeUnits));
        epoch = getEpoch(timeUnits);
        gc.setTime(epoch);
        showTime(gc);

        epoch = new Date((long)1139961600*1000);

        System.out.println("Source Time: "+epoch+" Converted Time: "+convertTime(epoch, timeUnits));
        gc.setTime(epoch);
        showTime(gc);
    }


    public static String convertTime(Date time, String timeUnits){

        while(timeUnits.startsWith(" ") || timeUnits.startsWith("\t"))
            timeUnits = timeUnits.substring(1);


        Date epoch = getEpoch(timeUnits);

        long eTime = epoch.getTime();
        long thisTime = time.getTime();
        long elapsedTime = thisTime - eTime;

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


        return elapsedTime+"";

    }

    static Date getEpoch(String timeUnits) {

        Date epochDate;
        if(timeUnits.contains("since")){

            int startIndex = timeUnits.lastIndexOf("since") + "since".length();
            String epoch = timeUnits.substring(startIndex);

            while(epoch.startsWith(" ") || epoch.startsWith("\t"))
                epoch = epoch.substring(1);

            SimpleDateFormat sdf;
            GregorianCalendar gc = new GregorianCalendar();
            TimeZone tz = TimeZone.getTimeZone("GMT");
            gc.setTimeZone(tz);


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

    public static void showTime(Calendar gc){
        Date time = gc.getTime();

        long timInt =  time.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

        System.out.println("Time: " + timInt +  "\n" +
                "Year:        " +gc.get(Calendar.YEAR) +  "\n" +
                "Month:       " +gc.get(Calendar.MONTH) +  "\n" +
                "Day:         " +gc.get(Calendar.DAY_OF_MONTH) +  "\n" +
                "Hour:        " +gc.get(Calendar.HOUR) +  "\n" +
                "Minute:      " +gc.get(Calendar.MINUTE) +  "\n" +
                "Second:      " +gc.get(Calendar.SECOND) +  "\n" +
                "Millisecond: " +gc.get(Calendar.MILLISECOND)
        );

        System.out.println(sdf.format(time));
        System.out.println();

    }

}
