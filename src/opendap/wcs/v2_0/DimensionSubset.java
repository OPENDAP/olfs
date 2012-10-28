/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.wcs.v2_0;

import org.jdom.Element;

/**
* Created by IntelliJ IDEA.
* User: ndp
* Date: 10/16/12
* Time: 6:42 PM
* To change this template use File | Settings | File Templates.
*/
public class DimensionSubset {
    private String dimensionId;
    private String trimLow;
    private String trimHigh;
    private String slicePoint;


    /**
     * Accepts the KVP encoding of a subset parameter for WCS 2.0
     * @param kspSubsetStr  the KVP encoding of a subset parameter value.
     * @throws WcsException When it's funky like an old sock.
     */
    public DimensionSubset(String kspSubsetStr) throws WcsException {
        int leftParen = kspSubsetStr.indexOf("(");
        int rghtParen = kspSubsetStr.indexOf(")");


        String s = kspSubsetStr.substring(0,leftParen);
        if(s==null){
            throw new WcsException("The subset parameter must begin with a valid NCNAME.",
                WcsException.INVALID_PARAMETER_VALUE,
                "subset");
        }
        dimensionId = s;

        String intervalOrPoint = kspSubsetStr.substring(leftParen,rghtParen);

        if(intervalOrPoint.contains(",")){
            int commaIndex = intervalOrPoint.indexOf(",");
            // It's an interval!

            trimLow = intervalOrPoint.substring(0,commaIndex);
            trimHigh = intervalOrPoint.substring(commaIndex,intervalOrPoint.length());

            slicePoint = null;
        }
        else {
            // It's a slicePoint;
            slicePoint = intervalOrPoint;
            trimHigh = null;
            trimLow = null;
        }
    }

    /**
     * Accepts the an instance of the DomainSubset abstract element in WCS 2.0. Currently this may be either
     * a DimensionTrim or DimensionSlice element.
     * @param dimensionSubsetType  One of the DomainSubsetType elements from the WCS 2.0 GetCoverage element. This may be either
     * DimensionTrim or DimensionSlice
     * @throws WcsException When it's funky like an old sock.
     */
    public DimensionSubset(Element dimensionSubsetType) throws WcsException {


        String type = dimensionSubsetType.getName();

        Element dimensionElement = dimensionSubsetType.getChild("Dimension",WCS.WCS_NS);
        if(dimensionElement==null)
            throw new WcsException("Missing wcs:Dimension element in wcs:DimensionSubsetType.",
                WcsException.MISSING_PARAMETER_VALUE,
                "wcs:Dimension");

        String id = dimensionElement.getTextTrim();
        if(id==null)
            throw new WcsException("Missing value for wcs:Dimension element in wcs:DimensionSubsetType.",
                WcsException.MISSING_PARAMETER_VALUE,
                "wcs:Dimension");

        dimensionId = id;


        if(type.equals("DimensionSlice")){

            Element slicePointElement = dimensionSubsetType.getChild("SlicePoint",WCS.WCS_NS);
            if(slicePointElement==null)
                throw new WcsException("Missing wcs:SlicePoint element in wcs:DimensionSlice.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:SlicePoint");

            String s = slicePointElement.getTextTrim();
            if(s==null)
                throw new WcsException("Missing value for wcs:SlicePoint element in wcs:DimensionSlice.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:SlicePoint");

            slicePoint = s;
            trimLow = null;
            trimHigh = null;

        }
        else if(type.equals("DimensionTrim")){

            Element trimLowElement = dimensionSubsetType.getChild("TrimLow",WCS.WCS_NS);
            if(trimLowElement==null)
                trimLow = "*";
            else
                trimLow = trimLowElement.getTextTrim();

            Element trimHighElement = dimensionSubsetType.getChild("TrimHigh",WCS.WCS_NS);
            if(trimHighElement==null)
                trimHigh = "*";
            else
                trimHigh = trimHighElement.getTextTrim();

            slicePoint = null;
        }
        else {
            throw new WcsException("Unrecognized wcs:DimensionSubsetType.",
                WcsException.INVALID_PARAMETER_VALUE,
                "wcs:DimensionSubset");
        }


    }

    public Element getDimensionSubsetElement(){

        Element ds = null;

        if(isSliceSubset()){
            ds = new Element("DimensionSlice",WCS.WCS_NS);

            Element e = new Element("Dimension",WCS.WCS_NS);
            e.setText(dimensionId);
            ds.addContent(e);

            e = new Element("SlicePoint",WCS.WCS_NS);
            e.setText(slicePoint);
            ds.addContent(e);



        }

        if(isTrimSubset()){
            ds = new Element("DimensionTrim",WCS.WCS_NS);

            Element e = new Element("Dimension",WCS.WCS_NS);
            e.setText(dimensionId);
            ds.addContent(e);

            if(!trimLow.equals("*")){
                e = new Element("TrimLow",WCS.WCS_NS);
                e.setText(trimLow);
                ds.addContent(e);
            }
            if(!trimHigh.equals("*")){
                e = new Element("TrimHigh",WCS.WCS_NS);
                e.setText(trimHigh);
                ds.addContent(e);
            }
        }
        return ds;
    }



    public boolean isSliceSubset(){
        return slicePoint !=null;
    }

    public boolean isTrimSubset(){
        return trimHigh !=null && trimLow !=null;
    }

    public String getSlicePoint(){
        return slicePoint;
    }

    public String getTrimHigh(){
        return trimHigh;
    }

    public String getTrimLow(){
        return trimLow;
    }

    public String getDimensionId(){
        return dimensionId;
    }

}
