/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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

import opendap.coreServlet.Util;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 *
 * This class represents a ows:BoundingBox with additional properties and methods to
 * allow it to produce DAP constraint expressions that use the geogrid server side function
 * in Hyrax 1.6.2 and later.
 *
**/
public class NewBoundingBox {


    private Logger _log;

    private LinkedHashMap<String, CoordinateDimension> _dimensions;

    private boolean _hasTimePeriod;
    private Date _startTime;
    private Date _endTime;
    private URI _srsName;

    public NewBoundingBox() throws WcsException {

        _log = LoggerFactory.getLogger(this.getClass());
        _dimensions = new LinkedHashMap<>();
        _srsName = null;

        _startTime     =  null;
        _endTime       =  null;
        _hasTimePeriod = false;
    }

    public NewBoundingBox(NewBoundingBox bb) throws WcsException, URISyntaxException {
        this(bb._dimensions, bb._startTime, bb._endTime, bb._srsName);
    }

    public NewBoundingBox(LinkedHashMap<String, CoordinateDimension> dims, Date startTime, Date endTime, URI srsName) throws WcsException {

        this();

        for(CoordinateDimension dim : dims.values()) {
            CoordinateDimension newDim = new CoordinateDimension(dim);
            _dimensions.put(newDim.getName(),newDim);
        }

        /*
        for(String coordinate : dims.keySet()) {
            CoordinateDimension dim = dims.get(coordinate);
            CoordinateDimension newDim = new CoordinateDimension(dim);
            _dimensions.put(coordinate,newDim);
        }
        */

        if(startTime!=null && endTime!=null) {

            if(startTime.after(endTime))
                throw new WcsException("BoundingBox parameter 'startTime' is after BoundingBox parmater 'endTime'",
                        WcsException.INVALID_PARAMETER_VALUE,"TimePeriod");

            _hasTimePeriod = true;
            _startTime = new Date(startTime.getTime());
            _endTime = new Date(endTime.getTime());
        }

        if(srsName!=null) {
            try {
                _srsName = new URI(srsName.toString());
            } catch (URISyntaxException e) {
                throw new WcsException("The srsName is not a valid URI. Msg: " + e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:boundedBy/gml:Envelope@srsName");
            }
        }

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
    public NewBoundingBox(Element bbElement) throws WcsException {

        this();

        if (bbElement == null)
            throw new WcsException("Missing " +
                    "gml:boundedBy element. This is used to identify " +
                    "a physical world bounding space for which data will be returned.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "gml:boundedBy");

        URI srsName = null;
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


            String axisLabelsString = envelope.getAttributeValue("axisLabels");
            String axisLabels[] = axisLabelsString.split(Util.WHITE_SPACE_REGEX_STRING);
            if (axisLabels.length < 2)
                throw new WcsException("The axisLabels attribute must contain at least two values.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:lowerCorner");



            for(String aLabel: axisLabels){
                CoordinateDimension d = new CoordinateDimension();
                d.setName(aLabel);
                _dimensions.put(aLabel,d);
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

            if (valCount != _dimensions.size())
                throw new WcsException("The gml:lowerCorner must contain at the same number of values as the axisLabels " +
                        "attribute of gml:Envelope.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:lowerCorner");

            int index=0;
            for(String dimName: _dimensions.keySet()){
                double value = Double.parseDouble(tmp[index++]);
                _dimensions.get(dimName).setMin(value);
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

            if (valCount != _dimensions.size())
                throw new WcsException("The gml:upperCorner must contain at the same number of values as the axisLabels " +
                        "attribute of gml:Envelope.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "gml:upperCorner");

            index=0;
            for(String dimName: _dimensions.keySet()){
                double value = Double.parseDouble(tmp[index++]);
                _dimensions.get(dimName).setMax(value);
            }



            s = envelope.getAttributeValue("srsName");
            if (s != null) {
                try {
                    _srsName = new URI(s);
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
        double lowerCorner[] = new double[_dimensions.size()];
        int i = 0;
        for(CoordinateDimension dim: _dimensions.values()){
            lowerCorner[i++] = dim.getMin();
        }

        return lowerCorner;
    }

    /**
     *
     * @return Upper corner array [Maximum Longitude, Maximum Latitude, ...] - the largest value for each dimension.
     */
    public double[] getUpperCorner() {
        double upperCorner[] = new double[_dimensions.size()];
        int i = 0;
        for(CoordinateDimension dim: _dimensions.values()){
            upperCorner[i++] = dim.getMax();
        }

        return upperCorner;

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
        for (double coordinate : getLowerCorner()) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);


        txt = "";
        e = new Element("UpperCorner", WCS.OWS_NS);
        for (double coordinate : getUpperCorner()) {
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


        StringBuilder txt = new StringBuilder();
        Element e = new Element("LowerCorner", WCS.OWS_NS);
        for (double coordinate : getLowerCorner()) {
            if(txt.length()>0)
                txt.append("  ");
            txt.append(coordinate);
        }
        e.setText(txt.toString());
        bbox.addContent(e);


        txt = new StringBuilder();
        e = new Element("UpperCorner", WCS.OWS_NS);
        for (double coordinate : getUpperCorner()) {
            if(txt.length()>0)
                txt.append("  ");
            txt.append(coordinate);
        }
        e.setText(txt.toString());
        bbox.addContent(e);

        return bbox;
    }


    /**
     *
     * @param bb
     * @throws WcsException
     */
    private void qcIncomingBB(NewBoundingBox bb) throws WcsException {

        if(_dimensions.size() != bb._dimensions.size())
            throw new WcsException("The union() operation only works on BoundingBoxes with the same number of dimensions." +
                    "The base Bounding box has "+_dimensions.size()+" the offered BoudningBox has "+bb._dimensions.size()+
                    " dimensions.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "gml:boundedBy");


        if(_srsName !=null &&
                bb.getCRSURI()!=null &&
                !_srsName.equals(bb.getCRSURI())){

            String msg = "Cannot check for BoundingBox intersections since the " +
                    "passed BoundingBox is expressed in a different CRS. " +
                    "My CRS: "+ _srsName.toASCIIString()+" " +
                    "Passed CRS: "+bb.getCRSURI().toASCIIString();

            _log.error(msg);
            throw new WcsException(msg,
                    WcsException.INVALID_PARAMETER_VALUE,
                    "ows:BoundingBox/@crs");
        }

    }

    public LinkedHashMap<String, CoordinateDimension> getDimensions(){

        LinkedHashMap<String,CoordinateDimension> dims =  new LinkedHashMap<>();

        for(String dimName: _dimensions.keySet()){
            CoordinateDimension dim = _dimensions.get(dimName);
            CoordinateDimension newDim = new CoordinateDimension(dim);
            dims.put(dimName, newDim);
        }

        return dims;
    }




    /**
     *
     * @param bb The BoundingBox we want to compare.
     * @return  True is the intersection of the two bounding boxes is empty.
     * @throws WcsException
     */
    public boolean intersects(NewBoundingBox bb) throws WcsException {

        boolean hasIntersection = true;
        boolean overlap;

        qcIncomingBB(bb);
        // @todo transform coordinates of passed BB to those of this one before check intersection

        for(String dimName: _dimensions.keySet()){
            CoordinateDimension myDim =  _dimensions.get(dimName);
            CoordinateDimension bbDim =  bb._dimensions.get(dimName);
            overlap = myDim.getMin()<bbDim.getMax()  && myDim.getMax()>bbDim.getMin();
            _log.debug("intersects() - The candidate BoundingBox {} dimension {} me.",bbDim.getName(),overlap?"overlaps":"do not overlap");
            hasIntersection = overlap && hasIntersection;

        }
        _log.debug("intersects() - The candidate BoundingBox  coordinate dimensions {} me!",hasIntersection?"intersects":"is disjoint from");

        if(hasTimePeriod() && bb.hasTimePeriod()) {
            boolean timePeriodOverlap = (getStartTime().before(bb.getEndTime()) && getEndTime().after(bb.getStartTime()));
            _log.debug("intersects() - The candidate BoundingBox TimePeriod {} me.",timePeriodOverlap?"overlaps":"do not overlap");
            hasIntersection = hasIntersection && timePeriodOverlap;
        }

        _log.debug("intersects() - The candidate BoundingBox {} me!",hasIntersection?"intersects":"is disjoint from");


        return hasIntersection;

    }

    /**
     *
     * @param bb
     * @return
     * @throws WcsException
     */
    public NewBoundingBox union(NewBoundingBox bb) throws WcsException {

        qcIncomingBB(bb);

        LinkedHashMap<String, CoordinateDimension> newDims = new LinkedHashMap<>();

        for(String dimName: _dimensions.keySet()){
            CoordinateDimension myDim =  _dimensions.get(dimName);
            CoordinateDimension bbDim =  bb._dimensions.get(dimName);
            double newMin =    (myDim.getMin() <= bbDim.getMin()) ? myDim.getMin() : bbDim.getMin();
            double newMax =    (myDim.getMax() >= bbDim.getMax()) ? myDim.getMax() : bbDim.getMax();
            CoordinateDimension newDim = new CoordinateDimension(dimName, newMin, newMax);
            newDims.put(dimName,newDim);
        }


        Date newStartTime=null, newEndTime=null;
        if(hasTimePeriod() && bb.hasTimePeriod()){

            newStartTime = (_startTime.before(bb._startTime))?new Date(_startTime.getTime()):new Date(bb._startTime.getTime());
            newEndTime = (_endTime.after(bb._endTime))?new Date(_endTime.getTime()):new Date(bb._endTime.getTime());
        }

        return new NewBoundingBox(newDims,newStartTime,newEndTime,_srsName);

    }


    /**
     *
     * @param bb THe bounding box to evaluate
     * @return True if the passed BB is contained both sptially and temporally
     * by this BB.
     * @throws WcsException
     */
    public boolean contains(NewBoundingBox bb) throws WcsException {
        qcIncomingBB(bb);

        boolean contains = true;

        for(String dimName :_dimensions.keySet()){
            CoordinateDimension myDim = _dimensions.get(dimName);
            CoordinateDimension bbDim = bb._dimensions.get(dimName);

            // myMin is less than their min
            contains = contains && ( myDim.getMin() <= bbDim.getMin() );


            // myMax is bigger than their max
            contains = contains && ( bbDim.getMax() <= myDim.getMax() );

            _log.debug("contains() - The candidate BoundingBox {} dimension {} me.",bbDim.getName(),contains?"is contained by":"is NOT contained by");
        }

        if(hasTimePeriod() && bb.hasTimePeriod()){
            contains = contains && _startTime.before(bb._startTime);
            contains = contains && _endTime.after(bb._endTime);
        }

        _log.debug("contains() - The candidate BoundingBox {} me.",contains?"is contained by":"is NOT contained by");

        return contains;
    }


    /**
     * THe EO schema has this to say about the footprint type:
     *   Acquisition footprint coordinates, described by a closed polygon (last point=first point),
     *   using CRS:WGS84, Latitude,Longitude pairs (per-WGS84 definition of point ordering, not necessarily
     *   per all WFS implementations).
     *
     *   Expected structure is:
     *   gml:Polygon/gml:exterior/gml:LinearRing/gml:posList.
     *
     *   eop/EOLI : polygon/coordinates (F B b s)
     *
     * @return
     */
     public String getEOFootprintPositionListValue() throws WcsException {

         StringBuilder footprint = new StringBuilder();
         Vector<CoordinateDimension> latLonDims = getLatNLonDims();
         String sep = " ";

         CoordinateDimension dim0 = latLonDims.get(0);
         CoordinateDimension dim1 = latLonDims.get(1);

         footprint.append(dim0.getMin()).append(sep).append(dim1.getMin()).append(sep);
         footprint.append(dim0.getMax()).append(sep).append(dim1.getMin()).append(sep);
         footprint.append(dim0.getMax()).append(sep).append(dim1.getMax()).append(sep);
         footprint.append(dim0.getMin()).append(sep).append(dim1.getMax()).append(sep);
         footprint.append(dim0.getMin()).append(sep).append(dim1.getMin());

         return footprint.toString();


     }

     private Vector<CoordinateDimension> getLatNLonDims() throws WcsException {

         Vector<CoordinateDimension> latLonDims = new Vector<>();
         Iterator<String> keys = _dimensions.keySet().iterator();

         while(keys.hasNext() && latLonDims.size()<2){
             String dimName = keys.next();
             if(dimName.toLowerCase().startsWith("lat") ||
                     dimName.toLowerCase().startsWith("lon")) {
                 latLonDims.add(_dimensions.get(dimName));
             }
         }
         if(latLonDims.size()!=2){
             throw new WcsException("getLatNLonDims() - OUCH! Unable to locate the latitude and longitude dimensions!",
                     WcsException.NO_APPLICABLE_CODE,"NewBounndingBox");
         }
         return latLonDims;
     }



}
