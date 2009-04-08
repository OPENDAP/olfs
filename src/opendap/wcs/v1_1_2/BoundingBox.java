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
package opendap.wcs.v1_1_2;

import org.jdom.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import opendap.coreServlet.Scrub;

/**
 * User: ndp
 * Date: Aug 18, 2008
 * Time: 12:14:43 PM
 */
public class BoundingBox {

    double[] _lowerCorner;
    double[] _upperCorner;
    URI      _crs;


    public BoundingBox(double[] lowerCorner, double[] upperCorner, URI crs ){
        _lowerCorner = lowerCorner;
        _upperCorner = upperCorner;
        _crs = crs;
    }


    public static BoundingBox fromKVP(HashMap<String,String> kvp) throws WcsException{


        String s = kvp.get("BoundingBox");

        if(s==null)
            return null;

        URI crs = null;
        double[] lowerCorner;
        double[] upperCorner;


        String tmp[] = s.split(",");
        int    valCount = tmp.length;

        if(valCount < 2)
            throw new WcsException("The BoundingBox used in the request is " +
                    "incorrect. It must specify both a lower corenr and an " +
                    "upper corner. This means at LEAST two numeric values.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "BoundingBox");

        // Check to see if they inculded a CRS URI. If they did, then the number
        // of elements in tmp must be odd. This is because the BB string must
        // contain a lower and upper corner. Every coordinate in the lower
        // corner has a mate in the upper corner, so there must be an even
        // number of coordinate values. THis means that is the number of comma
        // delimited items is odd then the user either bungled the URL, OR
        // they included a CRS URI at the end. We'll assume the latter...
        if((valCount % 2) != 0){
            String st = tmp[tmp.length-1];
            valCount--;
            try {
                crs = new URI(st);
            }
            catch(URISyntaxException e){
                throw new WcsException(e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "BoundingBox");
            }

        }


        try {
            lowerCorner = new double[valCount/2];
            int index;
            for(index=0; index<valCount/2; index++){
                lowerCorner[index] = Double.parseDouble(tmp[index]);
            }


            upperCorner = new double[valCount/2];
            for( int i = 0; index<valCount ; i++,index++){
                upperCorner[i] = Double.parseDouble(tmp[index]);
            }
        }
        catch(NumberFormatException e){
            throw new WcsException(e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "BoundingBox");
        }

        return new BoundingBox(lowerCorner,upperCorner,crs);


    }

    public double[] getLowerCorner(){
        return _lowerCorner;
    }

    public double[] getUpperCorner(){
        return _upperCorner;
    }

    public URI getCRSURI(){
        return _crs;
    }





    public Element getBoundingBoxElement(){
        Element bbox = new Element("BoundingBox",WCS.OWS_NS);

        if(_crs != null)
            bbox.setAttribute("crs",_crs.toString());


        String txt = "";
        Element e = new Element("LowerCorner",WCS.OWS_NS);
        for (double coordinate : _lowerCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);


        txt = "";
        e = new Element("UpperCorner",WCS.OWS_NS);
        for (double coordinate : _upperCorner) {
            txt += coordinate + "  ";
        }
        e.setText(txt);
        bbox.addContent(e);

        return bbox;
    }

}
