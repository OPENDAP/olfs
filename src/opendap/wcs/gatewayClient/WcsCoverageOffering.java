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
package opendap.wcs.gatewayClient;

import org.slf4j.Logger;
import org.jdom.Element;

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


        config = configuration;

        config();
        log.debug("Configured.");

    }

    private void config() throws Exception{

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


    public boolean hasTemporalDomain(){
        Element e1;
        e1 = config.getChild(WCS.DOMAIN_SET,WCS.NS);
        e1 = e1.getChild(WCS.TEMPORAL_DOMAIN,WCS.NS);

        return e1!=null;
    }



    public boolean hasSpatialDomain(){
        Element e1;
        e1 = config.getChild(WCS.DOMAIN_SET,WCS.NS);
        e1 = e1.getChild(WCS.SPATIAL_DOMAIN,WCS.NS);

        return e1!=null;
    }

    public String getEPSG4326BoundingBox(){

        int i;
        String  pos, bb="", coordinates[]=null;
        Element e1, lle;
        lle = config.getChild(WCS.LON_LAT_ENVELOPE,WCS.NS);
        Iterator corners = lle.getChildren(GML.POS,GML.NS).iterator();

        while(corners.hasNext()){
            e1 = (Element)corners.next();
            pos =  e1.getTextTrim();

            coordinates = pos.split(" ");

            for(i=0; i<coordinates.length ;i++){
                bb +=  (i>0||!bb.equals("")?",":"") + coordinates[i];
            }
        }
        if (coordinates!=null && coordinates.length==2)
            bb += ",0.0,0.0";

        return bb;
    }

    public String getBoundingBox(){

        int i;
        String  pos, bb="", coordinates[];
        Element e1, lle;
        lle = config.getChild(WCS.LON_LAT_ENVELOPE,WCS.NS);
        Iterator corners = lle.getChildren(GML.POS,GML.NS).iterator();

        while(corners.hasNext()){
            e1 = (Element)corners.next();
            pos =  e1.getTextTrim();

            coordinates = pos.split(" ");

            for(i=0; i<coordinates.length ;i++){
                bb +=  (i>0||!bb.equals("")?",":"") + coordinates[i];
            }
        }

        return bb;
    }

    public String getBoundingBoxCRS(){
        String  bb;
        Element lle;
        lle = config.getChild(WCS.LON_LAT_ENVELOPE,WCS.NS);
        //bb = lle.getAttributeValue(WCS.SRS_NAME,WCS.NS);
        bb = lle.getAttributeValue(WCS.SRS_NAME);
        return bb;
    }

    /**
     * Considers WGS84, WGS84(DD), to be equivalent.
     *
     *
     * 		<supportedCRSs>
     *          <requestCRSs>WGS84(DD)</requestCRSs>
	 *          <responseCRSs>WGS84(DD)</responseCRSs>
	 *		    <nativeCRSs>WGS84(DD)</nativeCRSs>
     *      </supportedCRSs>
     *
     *
     * @return true if the list of requestCRSs or requestResponseCRSs
     * contains WGS84.
     *
     */
    public boolean acceptsWGS84RequestCRS(){
        Element e1, e2;
        String crsList;

        e1 = config.getChild(WCS.SUPPORTED_CRSS,WCS.NS);

        for (Object o : e1.getChildren(WCS.REQUEST_RESPONSE_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("WGS84"))
                return true;
        }

        for (Object o : e1.getChildren(WCS.REQUEST_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("WGS84"))
                return true;
        }
        return false;
    }




    /**
     * Considers WGS84, WGS84(DD), to be equivalent.
     *
     *
     * 		<supportedCRSs>
     *          <requestCRSs>WGS84(DD)</requestCRSs>
	 *          <responseCRSs>WGS84(DD)</responseCRSs>
	 *		    <nativeCRSs>WGS84(DD)</nativeCRSs>
     *      </supportedCRSs>
     *
     *
     * @return true if the list of responseCRSs or requestResponseCRSs
     * contains WGS84.
     *
     */
    public boolean producesWGS84ResponseCRS(){
        Element e1, e2;
        String crsList;

        e1 = config.getChild(WCS.SUPPORTED_CRSS,WCS.NS);

        for (Object o : e1.getChildren(WCS.REQUEST_RESPONSE_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("WGS84"))
                return true;
        }

        for (Object o : e1.getChildren(WCS.RESPONSE_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("WGS84"))
                return true;
        }
        return false;
    }



    /**
     *
     * @return true if the list of requestCRSs or requestResponseCRSs
     * contains EPSG:4326.
     *
     */
    public boolean acceptsEPSG4326RequestCRS(){
        Element e1, e2;
        String crsList;

        e1 = config.getChild(WCS.SUPPORTED_CRSS,WCS.NS);

        for (Object o : e1.getChildren(WCS.REQUEST_RESPONSE_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("EPSG:4326"))
                return true;
        }

        for (Object o : e1.getChildren(WCS.REQUEST_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("EPSG:4326"))
                return true;
        }
        return false;
    }

    /**
     *
     * @return true if the list of responseCRSs or requestResponseCRSs
     * contains EPSG:4326.
     *
     */
    public boolean producesEPSG4326ResponseCRS(){
        Element e1, e2;
        String crsList;

        e1 = config.getChild(WCS.SUPPORTED_CRSS,WCS.NS);

        for (Object o : e1.getChildren(WCS.REQUEST_RESPONSE_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("EPSG:4326"))
                return true;
        }

        for (Object o : e1.getChildren(WCS.RESPONSE_CRSS, WCS.NS)) {
            e2 = (Element) o;
            crsList = e2.getTextTrim();
            if (crsList.contains("EPSG:4326"))
                return true;
        }
        return false;
    }

    /**
     *
     * @return The spatial domain informations for a WCS request that will
     * get the entire spatial extent for this coverage. This means
     * both the CRS and BBOX clauses.
     */
    public String getSpatialDomainConstraint() {
        String sdc=null;

        String bboxCRS = getBoundingBoxCRS();

        if(bboxCRS.contains("WGS84")){

            String bbox, reqCRS, resCRS;


            if(acceptsWGS84RequestCRS()){
                    reqCRS = "CRS="+bboxCRS;
                    bbox = "BBOX="+getBoundingBox();
                    if(producesWGS84ResponseCRS()){
                        resCRS = "RESPONSE_CRS="+bboxCRS;
                    }
                    else if(producesEPSG4326ResponseCRS()){
                        resCRS = "RESPONSE_CRS=EPSG:4326";

                    }
                    else {
                        log.error("Coverage: "+getName()+" does not respond in WGS84 or EPSG:4326 coordinate systems");
                        return null;
                    }
            }
            else if(acceptsEPSG4326RequestCRS()){
                reqCRS = "CRS=EPSG:4326";
                bbox = "BBOX="+ getEPSG4326BoundingBox();

                if(producesWGS84ResponseCRS()){
                    resCRS = "RESPONSE_CRS=WGS84"; /// this is iffy.

                }
                else if(producesEPSG4326ResponseCRS()){
                    resCRS = "RESPONSE_CRS=EPSG:4326";

                }
                else {
                    log.error("Coverage: "+getName()+" does not respond in WGS84 or EPSG:4326 coordinate systems");
                    return null;
                }

            }
            else {
                log.error("Coverage: "+getName()+" does not accept requests in the WGS84 or EPSG:4326 coordinate systems.");
                return null;
            }


            sdc = reqCRS + "&" + resCRS + "&" + bbox;


        }


        return sdc;

    }



    public Element getConfigElement(){
        return (Element)config.clone();
    }



    public Vector<String> generateDateStrings() throws Exception {


        Vector<String> dates = new Vector<String>();

        Element e1,e2;

        e1 = config.getChild(WCS.DOMAIN_SET,WCS.NS);
        e2 = e1.getChild(WCS.TEMPORAL_DOMAIN,WCS.NS);
        e1 = e2.getChild(GML.TIME_PERIOD,GML.NS);
        if(e1==null)
            e1 = e2.getChild(GML.TIME_PERIOD,WCS.NS);


        e2 = e1.getChild(GML.BEGIN_POSITION,GML.NS);
        if(e2==null)
            e2 = e1.getChild(GML.BEGIN_POSITION,WCS.NS);

        String begin = e2.getTextTrim();

        e2 = e1.getChild(GML.END_POSITION,GML.NS);
        if(e2==null)
            e2 = e1.getChild(GML.END_POSITION,WCS.NS);
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
        e1 = e1.getChild(GML.TIME_PERIOD,GML.NS);

        e2 = e1.getChild(GML.BEGIN_POSITION,GML.NS);
        String begin = e2.getTextTrim();

        e2 = e1.getChild(GML.END_POSITION,GML.NS);
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
