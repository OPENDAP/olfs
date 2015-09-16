/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.threddsHandler;

import opendap.namespaces.THREDDS;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by ndp on 4/19/15.
 */
public class LatestComplete extends SimpleLatest {

    Logger _log;

    double _lastModifiedLimit_ms;

    SimpleDateFormat _threddsDate;


    public LatestComplete (Element e){
        super(e);
        _log = LoggerFactory.getLogger(this.getClass());



        Double lms_minutes = 60.0;
        _log.info("DEFAULT: lastModifiedLimit=" + lms_minutes + " m");


        String lmString = e.getAttributeValue(THREDDS.LAST_MODIFIED_LIMIT);
        if(lmString == null){
            lmString = "60.0";
        }

        try {
            lms_minutes = Double.parseDouble(lmString);
        }
        catch(NumberFormatException  pe){
            _log.error("Failed to parse lastModifiedLimit attribute in element '"+e.getName()+"' value: '"+lmString+"' message: '"+pe.getMessage()+"'");
        }


        // Convert to milliseconds: x (min) * 60 (s/min) * 1000 (ms/s) = ms
        _lastModifiedLimit_ms = lms_minutes * 60.0 * 1000.0;
        _log.info("lastModifiedLimit value set to: " + _lastModifiedLimit_ms + " ms");



        // <thredds:date type="modified">2010-10-08T01:42:23</thredds:date>
        _threddsDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        _threddsDate.setTimeZone(TimeZone.getTimeZone("GMT"));


    }



    @Override
    public Element getProxyDataset(TreeMap<String, Element> datasets) {




        GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        long timeZoneCorrectedNow_ms = gc.getTime().getTime();
        _log.debug("timeZoneCorrectedNow_ms: {}",timeZoneCorrectedNow_ms);

        Date lmt;




        Element dataset, date;
        String dateString, typeString;
        long millisSinceLmt;

        Element lastSuitableDataset = null;
        boolean done = false;
        NavigableSet<String> ks = datasets.descendingKeySet();
        Iterator<String> i = ks.iterator();
        String name;

        while(!done && i.hasNext()){
            name = i.next();
            _log.debug("getProxyDataset() - Evaluating dataset '{}'",name);
            dataset = datasets.get(name);
            date = dataset.getChild(THREDDS.DATE,THREDDS.NS);

            if(date!=null){
                typeString = date.getAttributeValue(THREDDS.TYPE);
                if(typeString.equalsIgnoreCase(THREDDS.MODIFIED)){

                    _log.debug("getProxyDataset() Dataset date is 'modified' type");

                    dateString = date.getText();
                    if(dateString!=null) {
                        try {
                            lmt = _threddsDate.parse(dateString);

                            _log.debug("getProxyDataset() Dataset LMT: {} ({})",lmt,lmt.getTime());


                            _log.debug("getProxyDataset() time zone corrected NOW: {}",timeZoneCorrectedNow_ms);

                            millisSinceLmt = timeZoneCorrectedNow_ms - lmt.getTime();
                            _log.debug("getProxyDataset() Time since LMT: {} ms   Minimum: {} ms",millisSinceLmt,_lastModifiedLimit_ms);

                            if(_lastModifiedLimit_ms < millisSinceLmt){
                                lastSuitableDataset = dataset;
                                done = true;
                            }

                        } catch (ParseException e) {
                            _log.error("getProxyDataset() Failed to parse thredds:date value. Dataset: '{}' Message: '{}'",dataset.getAttributeValue(THREDDS.NAME), e.getMessage());
                        }



                    }

                }



            }


        }


        //Element lastDataset = datasets.get(datasets.lastKey());

        Element proxy = null;

        if(lastSuitableDataset!=null){
            proxy = getCopy(lastSuitableDataset);
            proxy.setAttribute(THREDDS.SERVICE_NAME,_serviceName);
            proxy.setAttribute(THREDDS.NAME,_name);
        }

        return proxy;
    }

    private Element getCopy(Element e){

        if(e==null)
            return null;


        return (Element) e.clone();

    }


}
