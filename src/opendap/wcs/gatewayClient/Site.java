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

import org.jdom.Element;
import org.jdom.Attribute;
import org.slf4j.Logger;

import java.util.List;

/**
 * This class represents a study site. Each Project has one or more Site object.
 * The Site contains the WCS Parameters that will be used to create queries
 * for thhe site. These parameter include (for WCS 1.0) bbox, time, format,
 * resx, resy, and interpolation method. </p>
 *
 * <p><b>ConfigurationExample:</b>
 *
 * <pre>
        &lt;Site name="BER"&gt;
            &lt;label&gt;BERMS Old Black Spruce&lt;/label&gt;
            &lt;WCSParameters&gt;
                &lt;bbox&gt;-107.475000,45.935000,-102.725000,50.685000&lt;/bbox&gt;
                &lt;time&gt;2003-10-01/2003-11-30&lt;/time&gt;
                &lt;format&gt;netCDF&lt;/format&gt;
                &lt;resx&gt;0.25&lt;/resx&gt;
                &lt;resy&gt;0.25&lt;/resy&gt;
                &lt;interpolationMethod&gt;Nearest neighbor&lt;/interpolationMethod&gt;
            &lt;/WCSParameters&gt;
        &lt;/Site&gt;
 * </pre>
 * It is possible this can be used with later versions of WCS as long as the
 * general structure of the holdings remains.
*/
public class Site {

    private Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    private Element config;




    Site(Element configuration) throws Exception{

        log.debug("Configuring...");

        config = (Element) configuration.clone();

        config();

        log.debug(this.toString());

    }

    public List getWCSParameters(){
        return config.getChild("WCSParameters").getChildren();
    }


    public Element getWCSParameterElement(){
        return config.getChild("WCSParameters");
    }

    public Element getLabelElement(){
        return config.getChild("label");
    }

    public String getLabelString(){
        return config.getChild("label").getTextTrim();
    }

    public String getName(){
        return config.getAttributeValue("name");
    }


    private void config() throws Exception {

        Attribute attr;
        Element elm;


        if(!config.getName().equals("Site"))
            throw new Exception("Cannot build a "+getClass()+" using " +
                    "<"+config.getName()+"> element.");

        attr = config.getAttribute("name");
        if(attr==null)
            throw new Exception("Missing name attribute. The " +
                    "<Site> element must have a attribute called \"name\". " );
        log.debug("name: "+getName());

        elm = config.getChild("label");
        if(elm==null)
            throw new Exception("Missing <label> element. The " +
                    "<Site name=\""+getName()+"\"> element must have a <label>  child " +
                    "element.");
        log.debug("label: "+getLabelString());

        elm = config.getChild("WCSParameters");

        if(elm==null)
            throw new Exception("Missing <WCSParameters> element. The " +
                    "<Site name=\""+getName()+"\"> element must have a <WCSParameters>  child " +
                    "element.");
/*

        BBOX=minx, miny, maxx, maxy,
        minz, maxz Request a subset defined by the specified bounding box, with min/max coordinate pairs ordered according to the Co-
        ordinate Reference System identified by the CRS parameter.
        One of BBOX or TIME is required.


        TIME= time1,time2,...
        or
        TIME= min/max/res, ...
        Request a subset corresponding to the specified time instants
        or intervals, expressed in an extended ISO 8601 syntax.
        Optional if a default time (or fixed time, or no time) is de-
        fined for the selected layer.
        One of BBOX or TIME is required.
  */

        /*
        boolean hasBounds = false;

        if(elm.getChild("bbox")!=null)
            hasBounds = true;

        if(elm.getChild("time")!=null)
            hasBounds = true;

        if(!hasBounds)
            throw new Exception("Missing bounds.  The " +
                    "<Site name=\""+getName()+"\"> <WCSParameters>  element must " +
                    "have 1 or both the child elements <bbox> and <time>.");
*/




        /*
        WIDTH = w (integer)
        HEIGHT = h (integer)
        [DEPTH =d (integer)]
        Request a grid of the specified width (w), height (h), and
        [for 3D grids] depth (d) (integer number of gridpoints).
        Either these or RESX, RESY, [for 3D grids] RESZ are re-
        quired.
        */
        boolean hasWHD = false;

        if(elm.getChild("width")!=null && elm.getChild("height")!=null)
            hasWHD = true;

/*

        RESX=x (double)
        RESY=y (double)
        [RESZ=z (double)]
        [when requesting georectified grid coverages]
        Request a coverage subset with a specific spatial resolution
        along each axis of the reply CRS. The values are given in
        the units appropriate to each axis of the CRS.
        Either these or WIDTH, HEIGHT, and [for 3D grids]
        DEPTH are required.
*/
        boolean hasRes = false;

        if(elm.getChild("resx")!=null && elm.getChild("resy")!=null)
            hasRes = true;

        if(!hasRes && !hasWHD)
            throw new Exception("Missing resolution definition.  The " +
                    "<Site name=\""+getName()+"\"> <WCSParameters>  element must " +
                    "have 1 of the child element sets: [<resx> <resy>]  or " +
                    "[<width> <height>].");

        if(hasRes && hasWHD)
            throw new Exception("Dual resolution definition.  The " +
                    "<Site name=\""+getName()+"\"> <WCSParameters>  element must " +
                    "have 1 of the child element sets: [<resx> <resy>]  or " +
                    "[<width> <height>].");

/*
CRS=crs_identifier Coordinate Reference System in which the request is ex-
pressed. Required.
*/


        /*
        boolean hasCRS = false;
        if(elm.getChild("crs")!=null)
            hasCRS = true;

        if(!hasCRS)
            throw new Exception("Missing Coordinate Reference System.  The " +
                    "<Site name=\""+getName()+"\"> <WCSParameters>  element must " +
                    "have 1 <crs> child element.");

*/

/*
FORMAT= format Requested output format of Coverage. Must be one of those
listed under the description of the selected coverage. Re-
quired.
*/

        /*
        boolean hasFormat = false;
        if(elm.getChild("format")!=null)
            hasFormat = true;

        if(!hasFormat)
            throw new Exception("Missing data format.  The " +
                    "<Site name=\""+getName()+"\"> <WCSParameters>  element must " +
                    "have 1 <format> child element.");

*/

    }



}
