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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 *
 * This class represents a ows:BoundingBox with additional properties and methods to
 * allow it to produce DAP constraint expressions that use the geogrid server side function
 * in Hyrax 1.6.2 and later.
 *
**/
public class BoundingBox {


    Logger log;
    private double[] lowerCorner;
    private double[] upperCorner;
    private URI crs;

    /**
     * Builds a BoundingBox from the passed corner positions.
     * 
     * @param lowerCorner   Lower corner array [Minumum Longitude, Minimum Latitude, ...]
     * @param upperCorner   Upper corner array [Maximum Longitude, Maximum Latitude, ...]
     * @param crs A URI for the cooridinate reference system.
     */
    public BoundingBox(double[] lowerCorner, double[] upperCorner, URI crs) {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;

        this.crs = crs;
    }


    /**
     * Builds a BoundingBox from one specified in a KVP encoded WCS request.
     *
     * @param kvp A HashMap containing the KVP.
     * @throws WcsException When the KVP has issues.
     */
    public BoundingBox(HashMap<String, String> kvp) throws WcsException {
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        String s = kvp.get("BoundingBox");

        if (s == null)
            throw new WcsException("Request is missing required " +
                    "BoundingBox key value pairs. This is used to identify " +
                    "what data will be returned.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "BoundingBox");

        URI crs = null;
        double[] lowerCorner;
        double[] upperCorner;


        String tmp[] = s.split(",");
        int valCount = tmp.length;

        if (valCount < 2)
            throw new WcsException("The BoundingBox used in the request is " +
                    "incorrect. It must specify both a lower corner and an " +
                    "upper corner. This means at LEAST two numeric values.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "BoundingBox");

        // Check to see if they included a CRS URI. If they did, then the number
        // of elements in tmp must be odd. This is because the BB string must
        // contain a lower and upper corner. Every coordinate in the lower
        // corner has a mate in the upper corner, so there must be an even
        // number of coordinate values. This means that is the number of comma
        // delimited items is odd then the user either bungled the URL, OR
        // they included a CRS URI at the end. We'll assume the latter...
        if ((valCount % 2) != 0) {
            String st = tmp[tmp.length - 1];
            valCount--;
            try {
                crs = new URI(st);
            }
            catch (URISyntaxException e) {
                throw new WcsException(e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "BoundingBox");
            }

        }


        try {
            lowerCorner = new double[valCount / 2];
            int index;
            for (index = 0; index < valCount / 2; index++) {
                lowerCorner[index] = Double.parseDouble(tmp[index]);
            }


            upperCorner = new double[valCount / 2];
            for (int i = 0; index < valCount; i++, index++) {
                upperCorner[i] = Double.parseDouble(tmp[index]);
            }
        }
        catch (NumberFormatException e) {
            throw new WcsException(e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "BoundingBox");
        }

        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;
        this.crs = crs;


    }

    /**
     * Builds a BoundingBox from an ows:BoundingBox Element.
     * @param bbElement The ows:BoundingBox element from which to initialize this
     * BoundingBox object.
     * @throws WcsException When the ows:BoundingBox has issues.
     */
    public BoundingBox(Element bbElement) throws WcsException {
        log = org.slf4j.LoggerFactory.getLogger(getClass());


        if (bbElement == null)
            throw new WcsException("Missing required " +
                    "ows:BoundingBox element. This is used to identify " +
                    "a physical world bounding space for which data will be returned.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:BoundingBox");

        URI crs = null;
        double[] lowerCorner;
        double[] upperCorner;

        Element e;
        String s;

        WCS.checkNamespace(bbElement,"BoundingBox",WCS.OWS_NS);

        try {


            // Process Lower Corner.
            e = bbElement.getChild("LowerCorner", WCS.OWS_NS);
            if (e == null) {
                throw new WcsException("The BoundingBox used is incomplete. " +
                        "It is missing the required ows:LowerCorner.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "wcs:BoundingBox");
            }

            String tmp[] = e.getTextNormalize().split(" ");
            int valCount = tmp.length;

            if (valCount < 2)
                throw new WcsException("The LowerCorner must contain at least two values.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "ows:LowerCorner");

            lowerCorner = new double[valCount];
            int index;
            for (index = 0; index < valCount; index++) {
                lowerCorner[index] = Double.parseDouble(tmp[index]);
            }




            // Process Upper Corner.
            e = bbElement.getChild("UpperCorner", WCS.OWS_NS);
            if (e == null) {
                throw new WcsException("The wcs:BoundingBox used is incomplete. " +
                        "It is missing the required ows:UpperCorner " +
                        "This means at LEAST two numeric values.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "wcs:BoundingBox");
            }

            tmp = e.getTextNormalize().split(" ");
            valCount = tmp.length;

            if (valCount < 2)
                throw new WcsException("The ows:UpperCorner must contain at least two values.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "ows:UpperCorner");

            upperCorner = new double[valCount];
            for (int i = 0; i < valCount; i++) {
                upperCorner[i] = Double.parseDouble(tmp[i]);
            }


            //Make sure they have the same number of values!
            if (upperCorner.length != lowerCorner.length)
                throw new WcsException("The ows:UpperCorner must contain the same number of values as the ows:LowerCorner.",
                        WcsException.INVALID_PARAMETER_VALUE,
                        "ows:UpperCorner");

        }
        catch (NumberFormatException nfe) {
            throw new WcsException(nfe.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:BoundingBox");
        }


        s = bbElement.getAttributeValue("crs");
        if (s != null) {
            try {
                crs = new URI(s);
            }
            catch (URISyntaxException use) {
                throw new WcsException(use.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "wcs:BoundingBox@crs");
            }

        }


        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;
        this.crs = crs;


    }

    /**
     *
     * @return  Lower corner array [Minumum Longitude, Minimum Latitude, ...] - the smallest value for each dimension.
     */
    public double[] getLowerCorner() {
        return lowerCorner;
    }

    /**
     *
     * @return Upper corner array [Maximum Longitude, Maximum Latitude, ...] - the largest value for each dimension.
     */
    public double[] getUpperCorner() {
        return upperCorner;
    }

    /**
     *
     * @return The URI in which the BoundingBox coordinates are expressed.
     */
    public URI getCRSURI() {
        return crs;
    }


    /**
     *
     * @return A bounding box string representing this BoundingBox
     * suitable for use with the DAP "geogrid()" server side function.
     */
    public String getDapGeogridFunctionBoundingBox(){
        double minLongitude = lowerCorner[0];
        double maxLongitude = upperCorner[0];
        double minLatitude  = lowerCorner[1];
        double maxLatitude  = upperCorner[1];

        //String bb = minLongitude + "," + maxLatitude + "," + maxLongitude + "," + minLatitude;
        String bb =  maxLatitude + "," + minLongitude  + "," + minLatitude + "," +  maxLongitude;

        return bb;
    }


    public String getDapGeogridFunctionElevationSubset(String dapElevationVariableName){
        String subset=null;
        if(hasElevation()){
            subset = "\""+getElevationMin() + "<="+dapElevationVariableName+"<="+getElevationMax()+"\"";
        }
        return subset;

    }

    public boolean hasElevation(){
        if(lowerCorner.length>2 && upperCorner.length>2)
            return true;
        return false;

    }
    public double getElevationMin(){
        return lowerCorner[2];
    }

    public double getElevationMax(){
        return upperCorner[2];
    }


    /**
     *
     * @return A ows:BoundingBox element representing this BoundingBox.
     */
    public Element getBoundingBoxElement() {
        Element bbox = new Element("BoundingBox", WCS.OWS_NS);

        if (crs != null)
            bbox.setAttribute("crs", crs.toString());


        String txt = "";
        Element e = new Element("LowerCorner", WCS.OWS_NS);
        for (double coordinate : lowerCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);


        txt = "";
        e = new Element("UpperCorner", WCS.OWS_NS);
        for (double coordinate : upperCorner) {
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

        if (crs != null)
            bbox.setAttribute("crs", crs.toString());

        //@todo transform coordinates!!


        String txt = "";
        Element e = new Element("LowerCorner", WCS.OWS_NS);
        for (double coordinate : lowerCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);


        txt = "";
        e = new Element("UpperCorner", WCS.OWS_NS);
        for (double coordinate : upperCorner) {
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

        if(crs!=null &&
                bb.getCRSURI()!=null &&
                !crs.equals(bb.getCRSURI())){

            String msg = "Cannot check for BoundingBox intersections since the " +
                    "passed BoundingBox is expressed in a different CRS. " +
                    "My CRS: "+crs.toASCIIString()+" " +
                    "Passed CRS: "+bb.getCRSURI().toASCIIString();

            log.error(msg);
            throw new WcsException(msg,
                    WcsException.INVALID_PARAMETER_VALUE,
                    "ows:BoundingBox/@crs");
        }

        /*

        boolean lngInt=false, latInt=false;

        double myMinLongitude = lowerCorner[0];
        double myMaxLongitude = upperCorner[0];
        double myMinLatitude  = lowerCorner[1];
        double myMaxLatitude  = upperCorner[1];

        double minLongitude = bb.lowerCorner[0];
        double maxLongitude = bb.upperCorner[0];
        double minLatitude  = bb.lowerCorner[1];
        double maxLatitude  = bb.upperCorner[1];


        if(myMinLongitude<maxLongitude  && myMaxLongitude>minLongitude)
            lngInt = true;

        if(myMinLatitude<maxLatitude  && myMaxLatitude>minLatitude)
            latInt = true;


        if(lowerCorner[0]<bb.upperCorner[0]  && upperCorner[0]>bb.lowerCorner[0])
            lngInt = true;

        if(lowerCorner[1]<bb.lowerCorner[1]  && upperCorner[1]>bb.lowerCorner[1])
            latInt = true;

        */


        for(int i=0; i<lowerCorner.length ;i++){
            overlap = lowerCorner[i]<bb.upperCorner[i]  && upperCorner[i]>bb.lowerCorner[i];
            hasIntersection = overlap & hasIntersection;
        }

         return hasIntersection;

    }




}
