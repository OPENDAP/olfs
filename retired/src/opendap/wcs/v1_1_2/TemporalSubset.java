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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;

/**
 */
public class TemporalSubset {


    private Logger log;


    private TimeSequenceItem[] _timeTequenceItems =null;



    public TemporalSubset(Element ts) throws WcsException {

        log = LoggerFactory.getLogger(this.getClass());

        Iterator i;
        Element e;
        int index;
        int count;

        if (ts == null)
            throw new WcsException("Missing wcs:TemporalSubset element. ",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:TemporalSubset");

        WCS.checkNamespace(ts,"TemporalSubset",WCS.WCS_NS);

        i = ts.getDescendants();
        if(!i.hasNext()){
            throw new WcsException("The wcs:TemporalSubset element must have one or child elements. ",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:TemporalSubset");
        }

        count = 0;
        while(i.hasNext()){
            i.next();
            count++;
        }
        _timeTequenceItems = new TimeSequenceItem[count];

        index = 0;
        i = ts.getDescendants();
        while(i.hasNext()){
            e = (Element) i.next();
            _timeTequenceItems[index++] = new TimeSequenceItem(e);
        }



    }



    public TemporalSubset(HashMap<String,String> kvp) throws WcsException {

        String s = kvp.get("TimeSequence");

        if (s == null)
            throw new WcsException("Missing required " +
                    "TimeSequence element. This is used to identify " +
                    "a physical world bounding space for which data will be returned.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "TimeSequence");

        String tmp[];


        // Time Sequences can be a comma separated list of time instances
        // and time ranges.
        tmp = s.split(",");

        _timeTequenceItems = new TimeSequenceItem[tmp.length];

        for(int i=0; i<tmp.length ;i++){
            _timeTequenceItems[i] = new TimeSequenceItem(tmp[i]);
        }

    }


    public Element getTemporalSubsetElement() throws WcsException {
        Element ts = new Element("TemporalSubset",WCS.WCS_NS);

        for(TimeSequenceItem tsi: _timeTequenceItems){
            ts.addContent(tsi.getXMLElementRepresentation());
        }

        return ts;
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

        for(TimeSequenceItem tsi: _timeTequenceItems){

            if(dapTemporalSubset != null)
                dapTemporalSubset += ",";

            if(tsi.isTimePeriod()){
                String beginPosition = TimeConversion.convertTime(tsi.getBeginPosition(),timeUnits);
                String endPosition = TimeConversion.convertTime(tsi.getEndPosition(),timeUnits);

                dapTemporalSubset += "\""+beginPosition + "<="+dapTimeVar+"<="+endPosition+"\"";

            }
            else if(tsi.isTimePosition()){
                String timePosition = TimeConversion.convertTime(tsi.getTimePosition(),timeUnits);
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






    
}
