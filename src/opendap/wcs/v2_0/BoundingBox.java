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

import org.jdom.Element;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 *
 * This class represents a ows:BoundingBox with additional properties and methods to
 * allow it to produce DAP constraint expressions that use the geogrid server side function
 * in Hyrax 1.6.2 and later.
 *
**/
public class BoundingBox {


    Logger log;
    private double[] _lowerCorner;
    private double[] _upperCorner;
    private boolean _hasTimePeriod;
    private Date _startTime;
    private Date _endTime;
    private URI _srsName;

    public BoundingBox(double lowerCorner[], double upperCorner[], URI srsName) throws WcsException {

        if(lowerCorner.length != upperCorner.length){
            throw new WcsException("A Bounding box cannot be construct in which the number of dimensions is inconsistent." +
                    "The gml:lowerCorner has "+lowerCorner.length+" dimensions and the gml:upperCorner has "+upperCorner.length,
                    WcsException.INVALID_PARAMETER_VALUE,
                    "gml:Envelope");
        }
        _lowerCorner = lowerCorner.clone();
        _upperCorner = upperCorner.clone();

        if(srsName!=null)
            try {
                _srsName = new URI(srsName.toASCIIString());
            }
            catch (URISyntaxException e) {
                throw new WcsException("The BoundingBox SRS name is not a proper URI. Message: " + e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:Envelope/@srsName");
            }
        else
            _srsName = null;

        _startTime     =  null;
        _endTime       =  null;
        _hasTimePeriod = false;



    }
    public BoundingBox(double _lowerCorner[], double upperCorner[], Date startTime, Date endTime, URI _srsName) throws WcsException {
        this(_lowerCorner, upperCorner, _srsName);

        if(startTime!=null)
            _startTime =  new Date(startTime.getTime());
        if(endTime!=null)
            _endTime =  new Date(endTime.getTime());
        if(_startTime!=null && _endTime!=null)
            _hasTimePeriod = true;
    }


    public BoundingBox(BoundingBox bb) throws WcsException, URISyntaxException {
        this(bb._lowerCorner,bb._upperCorner,bb._startTime, bb._endTime, bb._srsName);
    }


    /**
     *
     *
     *
     <boundedBy>
         <Envelope _srsName="http://www.opengis.net/def/srsName/EPSG/0/4326" axisLabels="time latitude longitude"
                   uomLabels="h deg deg" srsDimension="3">
             <_lowerCorner>898476 -90.000 0.000</_lowerCorner>
             <_upperCorner>899202 90.000 360.000</_upperCorner>
         </Envelope>
     </boundedBy>

     * Builds a BoundingBox from an gml:boundedBy Element.
     * @param bbElement The gml:boundedBy element from which to initialize this
     * BoundingBox object.
     * @throws WcsException When the ows:BoundingBox has issues.
     */
    public BoundingBox(Element bbElement) throws WcsException {
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        if (bbElement == null)
            throw new WcsException("Missing " +
                    "gml:boundedBy element. This is used to identify " +
                    "a physical world bounding space for which data will be returned.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "gml:boundedBy");

        URI srsName = null;
        double[] lowerCorner;
        double[] upperCorner;

        Element e;
        String s;
        String tmp[];

        WCS.checkNamespace(bbElement, "boundedBy", WCS.GML_NS);

        _hasTimePeriod = false;
        try {

            Element envelope = bbElement.getChild("Envelope", WCS.GML_NS);

            if (envelope == null){

                envelope = bbElement.getChild("EnvelopeWithTimePeriod", WCS.GML_NS);

                if (envelope == null){
                    throw new WcsException("The gml:boundedBy element is missing the required" +
                            "gml:Envelope element or gml:EnvelopeWithTimePeriod element. This is used to identify " +
                            "a physical world bounding space for which data will be returned.",
                            WcsException.MISSING_PARAMETER_VALUE,
                            "gml:boundedBy");

                }
                _hasTimePeriod = true;
            }


            // Process Lower Corner.
            e = envelope.getChild("lowerCorner", WCS.GML_NS);
            if (e == null) {
                throw new WcsException("The gml:Envelope[WithTimePeriod] element is incomplete. " +
                        "It is missing the required gml:lowerCorner.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:Envelope[WithTimePeriod]");
            }

            tmp = e.getTextNormalize().split(" ");
            int valCount = tmp.length;

            if (valCount < 2)
                throw new WcsException("The gml:lowerCorner must contain at least two values.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:lowerCorner");

            lowerCorner = new double[valCount];
            int index;
            for (index = 0; index < valCount; index++) {
                lowerCorner[index] = Double.parseDouble(tmp[index]);
            }


            // Process Upper Corner.
            e = envelope.getChild("upperCorner", WCS.GML_NS);
            if (e == null) {
                throw new WcsException("The gml:Envelope[WithTimePeriod] is incomplete. " +
                        "It is missing the required gml:upperCorner element " +
                        "This means at LEAST two numeric values.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:Envelope[WithTimePeriod]");
            }

            tmp = e.getTextNormalize().split(" ");
            valCount = tmp.length;

            if (valCount < 2)
                throw new WcsException("The gml:upperCorner must contain at least two values.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:upperCorner");

            upperCorner = new double[valCount];
            for (int i = 0; i < valCount; i++) {
                upperCorner[i] = Double.parseDouble(tmp[i]);
            }


            //Make sure they have the same number of values!
            if (upperCorner.length != lowerCorner.length)
                throw new WcsException("The gml:upperCorner must contain the same number of values as the gml:lowerCorner.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:boundedBy");


            s = envelope.getAttributeValue("srsName");
            if (s != null) {
                try {
                    srsName = new URI(s);
                } catch (URISyntaxException use) {
                    throw new WcsException(use.getMessage(),
                            WcsException.INVALID_PARAMETER_VALUE,
                            "gml:Envelope/@srsName");
                }
            }

            if(_hasTimePeriod){
                ingestTimePeriod(envelope);
            }


        } catch (NumberFormatException nfe) {
            throw new WcsException(nfe.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "gml:boundedBy");
        }


        this._lowerCorner = lowerCorner;
        this._upperCorner = upperCorner;
        this._srsName = srsName;


    }

    public void ingestTimePeriod(Element timePeriodEnvelope) throws WcsException {
        String s;
        Element e;

        // Process beginPosition.
        e = timePeriodEnvelope.getChild("beginPosition", WCS.GML_NS);
        if (e == null) {
            throw new WcsException("The gml:EnvelopeWithTimePeriod element is incomplete. " +
                    "It is missing the required gml:beginPosition.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "gml:EnvelopeWithTimePeriod");
        }

        s = e.getTextNormalize();
        _startTime = TimeConversion.parseWCSTimePosition(s);


        // Process endPosition.
        e = timePeriodEnvelope.getChild("endPosition", WCS.GML_NS);
        if (e == null) {
            throw new WcsException("The gml:EnvelopeWithTimePeriod element is incomplete. " +
                    "It is missing the required gml:endPosition.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "gml:EnvelopeWithTimePeriod");
        }

        s = e.getTextNormalize();
        _endTime = TimeConversion.parseWCSTimePosition(s);

        _hasTimePeriod = true;

    }


    public boolean hasTimePeriod(){
        return _hasTimePeriod;
    }

    public Date getStartTime(){
        if(_startTime ==null)
            return null;

        return (Date) _startTime.clone();
    }


    public Date getEndTime(){
        if(_endTime ==null)
            return null;

        return (Date) _endTime.clone();
    }


    public Element getGmlTimePeriod(String id){

        Element timePeriod = null;

        if(hasTimePeriod()){
            timePeriod = new Element("TimePeriod",WCS.GML_NS);
            if(id!=null)
                timePeriod.setAttribute("id",id,WCS.GML_NS);

            Element begin = new Element("beginPosition",WCS.GML_NS);
            begin.setText(TimeConversion.formatDateInGmlTimeFormat(getStartTime()));
            timePeriod.addContent(begin);

            Element end = new Element("endPosition",WCS.GML_NS);
            end.setText(TimeConversion.formatDateInGmlTimeFormat(getEndTime()));
            timePeriod.addContent(end);
        }

        return timePeriod;

    }

    /**
     *
     * @return  Lower corner array [Minumum Longitude, Minimum Latitude, ...] - the smallest value for each dimension.
     */
    public double[] getLowerCorner() {
        return _lowerCorner.clone();
    }

    /**
     *
     * @return Upper corner array [Maximum Longitude, Maximum Latitude, ...] - the largest value for each dimension.
     */
    public double[] getUpperCorner() {
        return _upperCorner.clone();
    }

    /**
     *
     * @return The URI in which the BoundingBox coordinates are expressed.
     */
    public URI getCRSURI() {
        return _srsName;
    }


    /*

    public String getDapGeogridFunctionElevationSubset(String dapElevationVariableName){
        String subset=null;
        if(hasElevation()){
            subset = "\""+getElevationMin() + "<="+dapElevationVariableName+"<="+getElevationMax()+"\"";
        }
        return subset;

    }

    public boolean hasElevation(){
        if(_lowerCorner.length>2 && _upperCorner.length>2)
            return true;
        return false;

    }
    public double getElevationMin(){
        return _lowerCorner[2];
    }

    public double getElevationMax(){
        return _upperCorner[2];
    }

*/

    /**
     *
     * @return A ows:BoundingBox element representing this BoundingBox.
     */
    public Element getOwsBoundingBoxElement() {
        Element bbox = new Element("BoundingBox", WCS.OWS_NS);

        if (_srsName != null)
            bbox.setAttribute("crs", _srsName.toString());


        String txt = "";
        Element e = new Element("LowerCorner", WCS.OWS_NS);
        for (double coordinate : _lowerCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);


        txt = "";
        e = new Element("UpperCorner", WCS.OWS_NS);
        for (double coordinate : _upperCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);

        return bbox;
    }

    /**
     * @return A ows:WGS84BoundingBox element representing this BoundingBox.
     * @return
     */
    public Element getWgs84BoundingBoxElement() {
        Element bbox = new Element("WGS84BoundingBox", WCS.OWS_NS);

        if (_srsName != null)
            bbox.setAttribute("crs", _srsName.toString());

        //@todo transform coordinates!!


        String txt = "";
        Element e = new Element("LowerCorner", WCS.OWS_NS);
        for (double coordinate : _lowerCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);


        txt = "";
        e = new Element("UpperCorner", WCS.OWS_NS);
        for (double coordinate : _upperCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);

        return bbox;
    }

    /**
     *
     * @param bb The BoundingBox we want to compare.
     * @return  True is the intersection of the two bounding boxes is empty.
     * @throws WcsException
     */
    public boolean intersects(BoundingBox bb) throws WcsException {

        boolean hasIntersection = true;
        boolean overlap;

        // @todo transform coordinates of passed BB to those of this one before check intersection

        if(_srsName !=null &&
                bb.getCRSURI()!=null &&
                !_srsName.equals(bb.getCRSURI())){

            String msg = "Cannot check for BoundingBox intersections since the " +
                    "passed BoundingBox is expressed in a different CRS. " +
                    "My CRS: "+ _srsName.toASCIIString()+" " +
                    "Passed CRS: "+bb.getCRSURI().toASCIIString();

            log.error(msg);
            throw new WcsException(msg,
                    WcsException.INVALID_PARAMETER_VALUE,
                    "ows:BoundingBox/@crs");
        }

        /*

        boolean lngInt=false, latInt=false;

        double myMinLongitude = _lowerCorner[0];
        double myMaxLongitude = _upperCorner[0];
        double myMinLatitude  = _lowerCorner[1];
        double myMaxLatitude  = _upperCorner[1];

        double minLongitude = bb._lowerCorner[0];
        double maxLongitude = bb._upperCorner[0];
        double minLatitude  = bb._lowerCorner[1];
        double maxLatitude  = bb._upperCorner[1];


        if(myMinLongitude<maxLongitude  && myMaxLongitude>minLongitude)
            lngInt = true;

        if(myMinLatitude<maxLatitude  && myMaxLatitude>minLatitude)
            latInt = true;


        if(_lowerCorner[0]<bb._upperCorner[0]  && _upperCorner[0]>bb._lowerCorner[0])
            lngInt = true;

        if(_lowerCorner[1]<bb._lowerCorner[1]  && _upperCorner[1]>bb._lowerCorner[1])
            latInt = true;

        */


        for(int i = 0; i< _lowerCorner.length ; i++){
            overlap = _lowerCorner[i]<bb._upperCorner[i]  && _upperCorner[i]>bb._lowerCorner[i];
            hasIntersection = overlap & hasIntersection;
        }

         return hasIntersection;

    }



    public BoundingBox union(BoundingBox bb) throws WcsException {

        qcIncomingBB(bb);

        double newLowerCorner[] = new double[_lowerCorner.length];
        for(int i=0; i<_lowerCorner.length ; i++){
            newLowerCorner[i] = (_lowerCorner[i] <= bb._lowerCorner[i])?_lowerCorner[i]:bb._lowerCorner[i];
        }
        double newUpperCorner[] = new double[_upperCorner.length];
        for(int i=0; i<_upperCorner.length ; i++){
            newUpperCorner[i] = (_upperCorner[i] >= bb._upperCorner[i])?_upperCorner[i]:bb._upperCorner[i];
        }

        Date newStartTime=null, newEndTime=null;
        if(hasTimePeriod() && bb.hasTimePeriod()){

            newStartTime = (_startTime.before(bb._startTime))?new Date(_startTime.getTime()):new Date(bb._startTime.getTime());
            newEndTime = (_endTime.after(bb._endTime))?new Date(_endTime.getTime()):new Date(bb._endTime.getTime());
        }

        return new BoundingBox(newLowerCorner,newUpperCorner,newStartTime,newEndTime,_srsName);

    }



    private void qcIncomingBB(BoundingBox bb) throws WcsException {
        if(_lowerCorner.length != bb._lowerCorner.length || _upperCorner.length != bb._upperCorner.length)
            throw new WcsException("The union() operation only works on BoundingBoxes with the same number of dimensions." +
                    "The gml:lowerCorner elements have "+_lowerCorner.length+" and "+bb._lowerCorner.length+" dimensions. "+
                    "The gml:upperCorner elements have "+_upperCorner.length+" and "+bb._upperCorner.length+" dimensions. ",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "gml:boundedBy");


        if(_srsName !=null &&
                bb.getCRSURI()!=null &&
                !_srsName.equals(bb.getCRSURI())){

            String msg = "Cannot check for BoundingBox intersections since the " +
                    "passed BoundingBox is expressed in a different CRS. " +
                    "My CRS: "+ _srsName.toASCIIString()+" " +
                    "Passed CRS: "+bb.getCRSURI().toASCIIString();

            log.error(msg);
            throw new WcsException(msg,
                    WcsException.INVALID_PARAMETER_VALUE,
                    "ows:BoundingBox/@crs");
        }

    }


    public boolean contains(BoundingBox bb) throws WcsException {
        qcIncomingBB(bb);

        boolean contains = true;

        for(int i=0; i<_lowerCorner.length ; i++){
            contains = contains && ( _lowerCorner[i] <= bb._lowerCorner[i]);
        }
        for(int i=0; i<_upperCorner.length ; i++){
            contains =  contains && (bb._upperCorner[i] >=_upperCorner[i]);
        }
        if(hasTimePeriod() && bb.hasTimePeriod()){
            contains = contains && _startTime.before(bb._startTime);
            contains = contains && _endTime.after(bb._endTime);
        }
        return contains;
    }



}
