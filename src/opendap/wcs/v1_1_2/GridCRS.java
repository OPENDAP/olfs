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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 *
 *
 *
 * User: ndp
 * Date: Feb 5, 2009
 * Time: 1:22:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class GridCRS {

    private void init() throws WcsException {


        _gridBaseCRS = null;
        _gridOffsets = null;

        try {
            _gridType = new URI("urn:ogc:def:method:WCS:1.1:2dSimpleGrid");
            _gridOrigin = new double[] {0.0, 0.0};
            _gridCS = new URI("urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS");
        }
        catch(URISyntaxException e){
            throw new WcsException(e.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "GridCRS");
        }

    }


    public GridCRS(Element g) throws WcsException {
        init();

        WCS.checkNamespace(g, "GridCRS" ,WCS.WCS_NS);

        Element e;


        _gmlID = g.getAttributeValue("id",WCS.GML_NS);       //@todo CHECK FOR NULL VALUE HERE!

        e = g.getChild("srsName",WCS.GML_NS);
        _srsName = null;
        if(e!=null){
            setSrsName(e);
        }


        e = g.getChild("GridBaseCRS",WCS.WCS_NS);
        if(e==null){
            throw new WcsException("The element wcs:GridCRS is missing the required content, wcs:GridBaseCRS." ,
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:GridBaseCRS");
        }
        setGridBaseCRS(e);


        e = g.getChild("GridType",WCS.WCS_NS);
        _gridType = null;
        if(e!=null){
            setGridType(e);
        }

        e = g.getChild("GridOrigin",WCS.WCS_NS);
        _gridOrigin = null;
        if(e!=null){
            setGridOrigin(e);
        }


        e = g.getChild("GridOffsets",WCS.WCS_NS);
        _gridOffsets = null;
        if(e==null){
            throw new WcsException("The element wcs:GridCRS is missing the required content, wcs:GridOffsets." ,
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:GridOffsets");
        }
        setGridOffsets(e);

        e = g.getChild("GridCS",WCS.WCS_NS);
        _gridCS = null;
        if(e!=null){
            setGridCS(e);
        }
    }


    public static boolean hasGridCRS(HashMap<String,String> kvp){
        String s = kvp.get("GridBaseCRS");
        return s != null;
    }



    public GridCRS(HashMap<String,String> kvp) throws WcsException {

        init();
        boolean hasUserGridCRS = false;
        String s;


        // Get the optional GridBaseCRS
        s = kvp.get("GridBaseCRS");
        if(s!=null){
            setGridBaseCRS(s);
            hasUserGridCRS = true;
        }

        if(hasUserGridCRS){

            // Get the required GridOffsets
            s = kvp.get("GridOffsets");
            if(s==null){
                throw new WcsException("When specifying a GridCRS if a GridBaseCRS " +
                        "is specified  GridOffsets Must also be spcified.",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "GridOffsets");

            }
            setGridOffsets(s);

            // Get the optional GridType
            s = kvp.get("GridType");
            if(s!=null){
                setGridType(s);
            }


            // Get the optional GridCS
            s = kvp.get("GridCS");
            if(s!=null){
                setGridCS(s);
            }


            // Get the optional GridOrigin
            s = kvp.get("GridOrigin");
            if(s!=null){
                setGridOrigin(s);
            }



        }

    }


    public static double[] doubleListWorker(String list, String seperator, int minCount, String containerName) throws WcsException {

        double[] vals;
        String tmp[] = list.split(seperator);
        int valCount = tmp.length;

        if (valCount < minCount)
            throw new WcsException("At minimum the "+containerName+" must have "+minCount+" values.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "containerName");

        vals = new double[valCount];
        for (int i = 0; i < valCount; i++) {
            vals[i] = Double.parseDouble(tmp[i]);
        }

        return vals;
    }


    private String _gmlID;
    public void setGmlID(String s) {
        _gmlID = s;
    }
    public String getGmlID() {
        return _gmlID;
    }



    /**
     *

     <element name="name" type="gml:CodeType">
         <annotation>
             <documentation>
                 Label for the object, normally a descriptive name.
                 An object may have several names, typically
                 assigned by different authorities.  The authority
                 for a name is indicated by the value of its (optional)
                 codeSpace attribute.  The name may or may not be
                 unique, as determined by the rules of the organization
                 responsible for the codeSpace.
             </documentation>
         </annotation>
     </element>

     <complexType name="CodeType">
       <annotation>
         <documentation>
            Name or code with an (optional) authority.  Text token.
            If the codeSpace attribute is present, then its value should identify a dictionary, thesaurus
            or authority for the term, such as the organisation who assigned the value,
            or the dictionary from which it is taken.
            A text string with an optional codeSpace attribute.
        </documentation>
       </annotation>
       <simpleContent>
         <extension base="string">
           <attribute name="codeSpace" type="anyURI" use="optional"/>
         </extension>
       </simpleContent>
     </complexType>

     *
     */
    private String _srsName;
    public void setSrsName(Element s) throws WcsException {
        WCS.checkNamespace(s, "srsName" ,WCS.GML_NS);
        _srsName = s.getText();
    }
    public void setSrsName(String s) {
        _srsName = s;
    }
    public String getSrsName() {
        return _srsName;
    }

    private URI _srsCodeSpace;
    public void setSrsCodeSpace(String s) throws URISyntaxException {
        _srsCodeSpace = new URI(s);
    }
    public URI getCodeSpace() {
        return _srsCodeSpace;
    }


    /**
     *
        <pre>
         <element name="GridBaseCRS" type="anyURI">
            <annotation>
                <documentation>
                    Association to the coordinate reference system (CRS) in which this Grid
                    CRS is specified. A GridCRS can use any type of GridBaseCRS, including
                    GeographicCRS, ProjectedCRS, ImageCRS, or a different GridCRS.
                </documentation>
                <documentation>
                    For a GridCRS, this association is limited to a remote definition of the
                    GridBaseCRS (not encoded in-line).
                </documentation>
            </annotation>
        </element>
        </pre>

     */
    private URI    _gridBaseCRS;
    public void setGridBaseCRS(Element s) throws WcsException {
        WCS.checkNamespace(s, "GridBaseCRS" ,WCS.WCS_NS);
        try {
            _gridBaseCRS = new URI(s.getText());
        }
        catch(URISyntaxException use){
            throw new WcsException(use.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridBaseCRS");

        }
    }
    public void setGridBaseCRS(String s) throws WcsException {
        try {
            _gridBaseCRS = new URI(s);
        }
        catch(URISyntaxException use){
            throw new WcsException(use.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridBaseCRS");

        }
    }
    public void setGridBaseCRS(URI uri) {
        _gridBaseCRS = uri;
    }
    public URI getGridBaseCRS() {
        return _gridBaseCRS;
    }



    /**
     *
         <pre>
         <element name="GridType" type="anyURI" default="urn:ogc:def:method:WCS:1.1:2dSimpleGrid">
             <annotation>
                 <documentation>
                    Association to the OperationMethod used to define this Grid CRS. This
                    association defaults to an association to the most commonly used method,
                    which is referenced by the URN "urn:ogc:def:method:WCS:1.1:2dSimpleGrid".
                 </documentation>
                 <documentation>
                     For a GridCRS, this association is limited to a remote definition
                     of a grid definition Method (not encoded in-line) that encodes a
                     variation on the method implied by the CV_RectifiedGrid class in
                     ISO 19123, without the inheritance from CV_Grid.
                 </documentation>
             </annotation>
         </element>
         </pre>
     *
     */
    private URI    _gridType;
    public void setGridType(Element s) throws WcsException {
        WCS.checkNamespace(s, "GridType" ,WCS.WCS_NS);
        try {
            _gridType = new URI(s.getText());
        }
        catch(URISyntaxException use){
            throw new WcsException(use.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridType");

        }
    }
    public void setGridType(String s) throws WcsException {
        try {
            _gridType = new URI(s);
        }
        catch(URISyntaxException use){
            throw new WcsException(use.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridType");

        }
    }
    public void setGridType(URI uri) {
        _gridType = uri;
    }
    public URI getGridType() {
        return _gridType;
    }






    /**
     *
         <pre>
         <element name="GridOrigin" type="gml:doubleList" default="0 0">
             <annotation>
                 <documentation>
                     Coordinates of the grid origin position in the GridBaseCRS
                     of this GridCRS. This origin defaults be the most commonly
                     used origin in a GridCRS used in the output part of a
                     GetCapabilities operation request, namely "0 0".
                 </documentation>
                 <documentation>This element is adapted from gml:pos. </documentation>
             </annotation>
         </element>
         </pre>

     *
     */
    private double _gridOrigin[];
    public void setGridOrigin(Element e) throws WcsException {
        WCS.checkNamespace(e, "GridOrigin" ,WCS.WCS_NS);
        _gridOrigin = doubleListWorker(e.getTextNormalize()," ",2,"wcs:GridOrigin");

    }
    public void setGridOrigin(String s) throws WcsException {
        _gridOrigin = doubleListWorker(s," ",2,"wcs:GridOrigin");


    }
    public void setGridOrigin(double[] origin) throws WcsException{
        if(origin.length < 2)
            throw new WcsException("At minimum the wcs:GridOrigin must have 2 values.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridOrigin");
        _gridOrigin = origin;

    }
    public double[] getGridOrigin() {
        return _gridOrigin;
    }







    /**
     *
         <pre>
         <element name="GridOffsets" type="gml:doubleList">
             <annotation>
                 <documentation>
                     Two or more grid position offsets from the grid origin
                     in the GridBaseCRS of this GridCRS. Example: For the
                     grid2dIn2dCRS OperationMethod, this Offsets element
                     shall contain four values, the first two values shall
                     specify the grid offset for the first grid axis in
                     the 2D base CRS, and the second pair of values shall
                     specify the grid offset for the second grid axis.
                     In this case, the middle two values are zero for
                     un-rotated and un-skewed grids.
                 </documentation>
             </annotation>
         </element>
         </pre>

     *
     */
    private double _gridOffsets[];
    public void setGridOffsets(Element e) throws WcsException {
        WCS.checkNamespace(e, "GridOffsets" ,WCS.WCS_NS);
        _gridOffsets = doubleListWorker(e.getTextNormalize()," ",2,"wcs:GridOffsets");
    }

    public void setGridOffsets(String s) throws WcsException {
        _gridOffsets = doubleListWorker(s," ",2,"wcs:GridOffsets");

    }
    public void setGridOffsets(double[] offsets) throws WcsException {
        if(offsets.length < 2)
            throw new WcsException("At minimum the wcs:GridOffsets must have 2 values.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridOffsets");
        _gridOffsets = offsets;
    }
    public double[] getGridOffsets() {
        return _gridOffsets;
    }






    /**
     *
         <pre>
         <element name="GridCS" type="anyURI" default="urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS">
             <annotation>
                 <documentation>
                     Association to the (Cartesian) grid coordinate system
                     used by this Grid CRS. In this use of a (Cartesian)
                     grid coordinate system, the grid positions shall be
                     in the centers of the image or other grid coverage
                     values (not between the grid values), as specified in
                     ISO 19123. Also, the grid point indices at the origin
                     shall be 0, 0 (not 1,1), as specified in ISO 19123.
                     This GridCS defaults to the most commonly used grid
                     coordinate system, which is referenced by the URN
                     "urn:ogc:def:cs:OGC:0.0:Grid2dSquareCS".
                 </documentation>
                 <documentation>
                     For a GridCRS, this association is limited to a remote
                     definition of the GridCS (not encoded in-line).
                 </documentation>
             </annotation>
         </element>
         </pre>

     *
     */
    private URI    _gridCS;
    public void setGridCS(Element s) throws WcsException {
        WCS.checkNamespace(s, "GridCS" ,WCS.WCS_NS);
        try {
            _gridCS = new URI(s.getText());
        }
        catch(URISyntaxException use){
            throw new WcsException(use.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridCS");

        }
    }
    public void setGridCS(String s) throws WcsException {
        try {
            _gridCS = new URI(s);
        }
        catch(URISyntaxException use){
            throw new WcsException(use.getMessage(),
                    WcsException.INVALID_PARAMETER_VALUE,
                    "wcs:GridCS");

        }
    }
    public void setGridCS(URI uri) {
        _gridCS = uri;
    }
    public URI getGridCS() {
        return _gridCS;
    }




    public Element getElement(){

        Element crs = new Element("GridCRS",WCS.WCS_NS);
        Element e;
        String txt;
        boolean flag;

        if(_srsName!=null){
            e = new Element("srsName",WCS.GML_NS);
            e.setText(_srsName);
            crs.addContent(e);
        }

        if(_gridBaseCRS!=null){
            e = new Element("GridBaseCRS",WCS.WCS_NS);
            e.setText(_gridBaseCRS.toString());
            crs.addContent(e);
        }


        if(_gridType!=null){
            e = new Element("GridType",WCS.WCS_NS);
            e.setText(_gridType.toString());
            crs.addContent(e);
        }


        if(_gridOrigin!=null){
            e = new Element("GridOrigin",WCS.WCS_NS);
            txt  = "";
            flag = false;
            for (double originCoordinate : _gridOrigin) {
                if(flag)
                   txt += " ";
                txt += originCoordinate;
                flag = true;
            }

            e.setText(txt);
            crs.addContent(e);

        }


        if(_gridOffsets!=null){
            e = new Element("GridOffsets",WCS.WCS_NS);
            txt  = "";
            flag = false;
            for (double offsetCoordinate : _gridOffsets) {
                if(flag)
                   txt += " ";
                txt += offsetCoordinate;
                flag = true;
            }

            e.setText(txt);

            crs.addContent(e);

        }

        if(_gridCS!=null){
            e = new Element("GridCS",WCS.WCS_NS);
            e.setText(_gridCS.toString());
            crs.addContent(e);
        }

        if(_gmlID!=null){
            crs.setAttribute("crs",_gmlID,WCS.GML_NS);
        }

        return crs;

    }


}

    /*
*/