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


/**
 * This class embodies a WCS Dimension Subset concept and is able to produce a DAP constraint expression
 * that requests the correct subset of the data.
 *
 * Essentially this class acts as a map from WCS 2.0 dimension sub-setting syntax to DAP constraint expression syntax.
 * The class handles both the "trim" (interval) and "slice" (point) requests, both by value and by array index.
 *
 * WCS 2.0 allows domain coordinate dimensions to be subset by array index or by coordinate value. It distinguishes
 * instances of this by evaluating the string representation of the subsets constraint values. If a value parses as an
 * Integer, then it's interpreted as an array index. If doesn't parse as an Integer, then it assumed to by a value in
 * the associated CoordinateDomain and will be used in a by-value sub-setting expression.
 *
 * Subsets can also be either a "trim" (interval) or a "slicepoint", which is orthogonal to the arrayIndex/byValue facet.
 * Trim subsets are specified as an interval (with mono steps assumed). The interval is defined with two values
 * and it is required (by this software, not WCS per say) that both values be either array indices (integers) or,
 * values (non integer). In other words: Both values must be in the arrayIndex facet, or both in the byValue facet.
 *
*/
public class DimensionSubset {


    public enum Type {TRIM, SLICE_POINT}

    Type myDimensionType;


    private String dimensionId;
    private String trimLow;
    private String trimHigh;
    private String slicePoint;

    private boolean isArrayIndexSubset;

    public DimensionSubset(DimensionSubset source) {
        setDimensionType(source.getType());
        setDimensionId(source.getDimensionId());
        setTrimLow(source.getTrimLow());
        setTrimHigh(source.getTrimHigh());
        setSlicePoint(source.getSlicePoint());
        setIsArraySubset(source.isArraySubset());
    }


    protected void setDimensionId(String s){
        dimensionId = s;
    }

    protected void setDimensionType(Type t){
        myDimensionType = t;
    }

    protected void setTrimLow(String s){

        trimLow = trimQuotesAndWhiteSpace(s);
    }

    protected void setTrimHigh(String s){
        trimHigh = trimQuotesAndWhiteSpace(s);
    }

    protected void setIsArraySubset(boolean s){
        isArrayIndexSubset = s;
    }

    protected void setSlicePoint(String s){
        slicePoint = trimQuotesAndWhiteSpace(s);
    }

    private String trimQuotesAndWhiteSpace(String s){

        if(s==null)
            return null;

        // Trim them from the front.
        while(s.startsWith(" ") || s.startsWith("\t") || s.startsWith("\"") || s.startsWith("'"))
            s = s.substring(1,s.length());


        // Trim them from the back.
        while(s.endsWith(" ") || s.endsWith("\t") || s.endsWith("\"") || s.endsWith("'"))
            s = s.substring(0,s.length()-1);

        return s;
    }

    /**
     * Accepts the KVP encoding of a subset parameter for WCS 2.0
     * @param kvpSubsetString  the KVP encoding of a subset parameter value.
     * @throws WcsException When it's funky like an old sock.
     */
    public DimensionSubset(String kvpSubsetString) throws WcsException {
        int leftParen = kvpSubsetString.indexOf("(");
        int rghtParen = kvpSubsetString.indexOf(")");

        setIsArraySubset(false);

        String s = kvpSubsetString.substring(0,leftParen);
        if(s==null){
            throw new WcsException("The subset parameter must begin with a dimension name.",
                WcsException.INVALID_PARAMETER_VALUE,
                "subset");
        }
        setDimensionId(s);

        String intervalOrPoint = kvpSubsetString.substring(leftParen+1,rghtParen);

        if(intervalOrPoint.contains(",")){
            int commaIndex = intervalOrPoint.indexOf(",");
            // It's an interval!
            setDimensionType(Type.TRIM);
            setTrimLow(intervalOrPoint.substring(0,commaIndex));
            setTrimHigh(intervalOrPoint.substring(commaIndex+1,intervalOrPoint.length()));

            if(isArraySubsetString(getTrimLow())){
                if(isArraySubsetString(getTrimHigh())){
                    setIsArraySubset(true);
                }
                else {
                    throw new WcsException("Subset syntax error! You cannot mix array index (integer valued) subsetting" +
                            "with value based (either float or time string valued) subsetting in the same " +
                            "subset clause.", WcsException.INVALID_PARAMETER_VALUE,"subset");
                }


            }
            else if(isArraySubsetString(getTrimHigh())){
                throw new WcsException("Subset syntax error! You cannot mix array index (integer valued) subsetting" +
                        "with value based (either float or time string valued) subsetting in the same " +
                        "subset clause.", WcsException.INVALID_PARAMETER_VALUE,"subset");
            }

            setSlicePoint(null);
        }
        else {
            // It's a slicePoint;
            setDimensionType(Type.SLICE_POINT);
            setSlicePoint(intervalOrPoint);
            setTrimHigh(null);
            setTrimLow(null);

            setIsArraySubset(isArraySubsetString(getSlicePoint()));
        }
    }



    public boolean isArraySubsetString(String subsetStr){

        if(subsetStr.equals("*"))
            return true;

        try {
            Integer.parseInt(subsetStr);
        }
        catch(NumberFormatException e){

            return false;
        }

        return true;
    }


    public boolean isValueSubset(){
        return !isArrayIndexSubset;
    }

    public boolean isArraySubset(){
        return isArrayIndexSubset;
    }

    public Type getType(){
        return myDimensionType;
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

        setDimensionId(id);



        setIsArraySubset(false);


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

            setSlicePoint(s);
            setTrimHigh(null);
            setTrimLow(null);

            setIsArraySubset(isArraySubsetString(getSlicePoint()));


        }
        else if(type.equals("DimensionTrim")){

            Element trimLowElement = dimensionSubsetType.getChild("TrimLow",WCS.WCS_NS);
            if(trimLowElement==null)
                setTrimLow("*");
            else
                setTrimLow(trimLowElement.getTextTrim());

            Element trimHighElement = dimensionSubsetType.getChild("TrimHigh",WCS.WCS_NS);
            if(trimHighElement==null)
                setTrimHigh("*");
            else
                setTrimHigh(trimHighElement.getTextTrim());

            if(isArraySubsetString(getTrimLow())){
                if(isArraySubsetString(getTrimHigh())){
                    setIsArraySubset(true);
                }
                else {
                    throw new WcsException("Subset syntax error! You cannot mix array index (integer valued) subsetting" +
                            "with value based (either float or time string valued) subsetting in the same " +
                            "subset clause.", WcsException.INVALID_PARAMETER_VALUE,"subset");
                }


            }
            else if(isArraySubsetString(getTrimHigh())){
                throw new WcsException("Subset syntax error! You cannot mix array index (integer valued) subsetting" +
                        "with value based (either float or time string valued) subsetting in the same " +
                        "subset clause.", WcsException.INVALID_PARAMETER_VALUE,"subset");
            }

            setSlicePoint(null);
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



    /**
     * The is method produces the correct DAP value based constraint for inclusion in a DAP constraint expression
     * call to the DAP server side function "grid"
     *
     * If the time point/slice values are not integers (if they fail to parse as an Integer correctly) then they are
     * assumed to be in the target dataset DomainCoordinate units and they are used to construct a value based
     * constraint/filter expression for the DAP server side function "grid".
     * @return A value constraint for the DAP server side function "grid". if the instance of the DimensionSubset
     * is not a value based subset (i.e. as an array index subset) the empty string is returned.
     * @throws WcsException
     */
    public String getDapGridValueConstraint() throws WcsException {
        StringBuilder subsetClause = new StringBuilder();

        if(isValueSubset()){

          switch (getType()) {
              case TRIM:
                  subsetClause
                          .append("\"")
                          .append(getTrimLow())
                          .append("<=")
                          .append(getDimensionId())
                          .append("<=")
                          .append(getTrimHigh())
                          .append("\"");

                  break;
              case SLICE_POINT:
                  subsetClause
                          .append("\"")
                          .append(getDimensionId())
                          .append("=")
                          .append(getSlicePoint())
                          .append("\"");



                  break;
              default:
                  throw new WcsException("Unknown Subset Type!", WcsException.INVALID_PARAMETER_VALUE, "subset");
          }

        }

        return subsetClause.toString();

      }


    /**
     * The is method produces the DAP array index constraint that represents the subset.
     *
     *
     * If the time point/slice values are integers (if they  parse as an Integer correctly) then they are
     * interpreted to be array indices in the target dataset DomainCoordinate referenced by this DimensionSubset.
     * The time point/slice values are used to construct an array index based constraint/filter expression for
     * a DAP Grid or Array type.
     * @return An array index constraint for a DAP Array or Grid object. If the instance of the DomainSubset is not
     * an array index constraint, the empty string is returned.
     * @throws WcsException
     */
    public String getDapArrayIndexConstraint() throws WcsException {
        StringBuilder subsetClause = new StringBuilder();

        if (isArraySubset()) {

            switch (getType()) {
                case TRIM:
                    String lowIndex = getTrimLow();

                    if(lowIndex.equals("*"))
                        lowIndex = "0";

                    String highIndex = getTrimHigh();

                    if(highIndex.equals("*") && lowIndex.equals("*")) {
                        subsetClause.append("[*]");
                    }
                    else {

                        subsetClause
                                .append("[")
                                .append(lowIndex)
                                .append(":1:")
                                .append(highIndex)
                                .append("]");
                    }



                    break;
                case SLICE_POINT:
                    subsetClause
                            .append("[")
                            .append(getSlicePoint())
                            .append("]");


                    break;
                default:
                    throw new WcsException("Unknown Subset Type!", WcsException.INVALID_PARAMETER_VALUE, "subset");
            }


        }
        return subsetClause.toString();

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
