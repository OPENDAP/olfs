/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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
package opendap.wcs;

import org.slf4j.Logger;
import org.jdom.Element;
import org.jdom.Attribute;

import java.util.*;
import java.text.SimpleDateFormat;

/**
 * User: ndp
 * Date: Mar 13, 2008
 * Time: 10:12:31 PM
 */
public class WcsCoverageOffering {


    private Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    private Element config;



    public WcsCoverageOffering(Element configuration) throws Exception{

        log.debug("Configuring.");


        config = (Element) configuration.clone();

        config();
        log.debug("Configured.");

    }

    private void config() throws Exception{

        Attribute attr;
        Element elm;
        List k;
        Site site;

        if(!config.getName().equals(WCS.COVERAGE_OFFERING))
            throw new Exception("Cannot build a "+getClass()+" using " +
                    "<"+config.getName()+"> element.");

    }


    public String getName(){
        return config.getChild(WCS.NAME,WCS.NS).getTextTrim();
    }

    public String getLabel(){
        return config.getChild(WCS.LABEL,WCS.NS).getTextTrim();
    }



    public Vector<String> generateDateStrings() throws Exception {


        Vector<String> dates = new Vector<String>();

        Element e1,e2;

        e1 = config.getChild(WCS.DOMAIN_SET,WCS.NS);
        e1 = e1.getChild(WCS.TEMPORAL_DOMAIN,WCS.NS);
        e1 = e1.getChild(WCS.TIME_PERIOD,WCS.GML_NS);

        e2 = e1.getChild(WCS.BEGIN_POSITION,WCS.GML_NS);
        String begin = e2.getTextTrim();

        e2 = e1.getChild(WCS.END_POSITION,WCS.GML_NS);
        String end   = e2.getTextTrim();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date start = sdf.parse(begin);
        Date stop = sdf.parse(end);


        GregorianCalendar calendar = new GregorianCalendar();
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        calendar.setTime(start);
        String date;
        boolean done = false;
        while(!done){
            date = sdf.format(calendar.getTime());
            dates.add(date);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            /*
            log.trace("calendar.after(stop): "+ calendar.after(stop)+ "   " +
                      "cal.getTime().getTime(): "+calendar.getTime().getTime() + "   "+
                      "stop.getTime()"+stop.getTime());
                      */
            if(calendar.getTime().getTime() > stop.getTime())
                done = true;

        }

        return dates;

    }


    public int getDateCount() throws Exception {

        Element e1,e2;

        e1 = config.getChild(WCS.DOMAIN_SET,WCS.NS);
        e1 = e1.getChild(WCS.TEMPORAL_DOMAIN,WCS.NS);
        e1 = e1.getChild(WCS.TIME_PERIOD,WCS.GML_NS);

        e2 = e1.getChild(WCS.BEGIN_POSITION,WCS.GML_NS);
        String begin = e2.getTextTrim();

        e2 = e1.getChild(WCS.END_POSITION,WCS.GML_NS);
        String end   = e2.getTextTrim();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date start = sdf.parse(begin);
        Date stop  = sdf.parse(end);


        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(start);

        log.debug("start: "+start+"   stop: "+stop+"  stop.getTime(): "+stop.getTime());

        int dayCount = 0;
        boolean done = false;
        while(!done){
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            dayCount++;
            log.debug("dayCount: "+dayCount + "  " +
                      "calender: "+calendar.getTime().getTime() + "  " +
                      "stop.getTime(): "+stop.getTime());
            if(calendar.getTime().getTime() > stop.getTime())
                done = true;

        }

        return dayCount;

    }


}
