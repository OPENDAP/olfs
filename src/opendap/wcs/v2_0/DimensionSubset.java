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
 * This class embodies a WCS CoordinateDimension Subset concept and is able to produce a DAP constraint expression
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
public class DimensionSubset implements Cloneable {


    public enum Type {TRIM, SLICE_POINT}

    private Type _mySubsetType;


    private String _dimensionId;
    private String _trimLow;
    private String _trimHigh;
    private String _slicePoint;

    private boolean _isArrayIndexSubset;

    private DomainCoordinate _domainCoordinate;


    private DimensionSubset() {
        _dimensionId = null;
        _trimLow = null;
        _trimHigh = null;
        _slicePoint = null;
        _isArrayIndexSubset = false;
        _domainCoordinate = null;
    }

    public DimensionSubset(DimensionSubset source) {
        this();
        _mySubsetType = source._mySubsetType;
        _dimensionId = source._dimensionId;
        _trimLow = source._trimLow;
        _trimHigh =  source._trimHigh;
        _slicePoint =  source._slicePoint;
        _isArrayIndexSubset = source._isArrayIndexSubset;
        _domainCoordinate = source._domainCoordinate==null ? null : new DomainCoordinate(source._domainCoordinate);
    }

    public DimensionSubset(
            String dimId,
            String low,
            String high,
            boolean isInditial,
            DomainCoordinate dc
    )  {
        this();
        _mySubsetType = Type.TRIM;
        _dimensionId = dimId;
        _trimLow = low;
        _trimHigh = high;
        _isArrayIndexSubset = isInditial;
        _domainCoordinate = dc;
    }

    public DimensionSubset(
            String dimId,
            String slicePoint,
            boolean isInditial,
            DomainCoordinate dc
    )  {
        this();
        _mySubsetType = Type.SLICE_POINT;
        _dimensionId = dimId;
        _slicePoint = slicePoint;
        _isArrayIndexSubset = isInditial;
        _domainCoordinate = dc;
    }


    /**
     * Makes a DimensioSubset that requests the entire DomainCoordinate
     * @param dc
     */
    public DimensionSubset(DomainCoordinate dc){
        _mySubsetType = Type.TRIM;
        _dimensionId = dc.getName();
        _trimLow = "0";
        _trimHigh = ""+(dc.getSize()-1);
        _isArrayIndexSubset = true;
        _domainCoordinate = dc;
    }


    /**
         * Accepts the KVP encoding of a subset parameter for WCS 2.0
         * @param kvpSubsetString  the KVP encoding of a subset parameter value.
         * @throws WcsException When it's funky, like an old sock.
         */
    public DimensionSubset(String kvpSubsetString) throws WcsException {
        this();

        int leftParen = kvpSubsetString.indexOf("(");
        int rghtParen = kvpSubsetString.indexOf(")");

        if(leftParen<0 || rghtParen<0 || leftParen > rghtParen){
            throw new WcsException("Invalid subset expression. The subset expression '"+kvpSubsetString+"' lacks " +
                    "correctly organized parenthetical content.",
                    WcsException.INVALID_PARAMETER_VALUE,
                    "KVP subset");
        }

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
            setSubsetType(Type.TRIM);
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
            // It's a _slicePoint;
            setSubsetType(Type.SLICE_POINT);
            setSlicePoint(intervalOrPoint);
            setTrimHigh(null);
            setTrimLow(null);

            setIsArraySubset(isArraySubsetString(getSlicePoint()));
        }
    }

    public void setDomainCoordinate(DomainCoordinate dc){
        _domainCoordinate =dc;
    }

    protected void setDimensionId(String s) throws WcsException {
        _dimensionId = s;
    }


    protected void setSubsetType(Type t){
        _mySubsetType = t;
    }

    protected void setTrimLow(String s){ _trimLow = trimQuotesAndWhiteSpace(s); }

    protected void setTrimHigh(String s){
        _trimHigh = trimQuotesAndWhiteSpace(s);
    }

    protected void setIsArraySubset(boolean s){
        _isArrayIndexSubset = s;
    }

    protected void setSlicePoint(String s){

        _slicePoint = trimQuotesAndWhiteSpace(s);
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
        return !_isArrayIndexSubset;
    }

    public boolean isArraySubset(){
        return _isArrayIndexSubset;
    }

    public Type getType(){
        return _mySubsetType;
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


        Element dimensionElement = dimensionSubsetType.getChild("CoordinateDimension",WCS.WCS_NS);
        if(dimensionElement==null)
            throw new WcsException("Missing wcs:CoordinateDimension element in wcs:DimensionSubsetType.",
                WcsException.MISSING_PARAMETER_VALUE,
                "wcs:CoordinateDimension");

        String id = dimensionElement.getTextTrim();
        if(id==null)
            throw new WcsException("Missing value for wcs:CoordinateDimension element in wcs:DimensionSubsetType.",
                WcsException.MISSING_PARAMETER_VALUE,
                "wcs:CoordinateDimension");

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

            Element e = new Element("CoordinateDimension",WCS.WCS_NS);
            e.setText(_dimensionId);
            ds.addContent(e);

            e = new Element("SlicePoint",WCS.WCS_NS);
            e.setText(_slicePoint);
            ds.addContent(e);

        }

        if(isTrimSubset()){
            ds = new Element("DimensionTrim",WCS.WCS_NS);

            Element e = new Element("CoordinateDimension",WCS.WCS_NS);
            e.setText(_dimensionId);
            ds.addContent(e);

            if(!_trimLow.equals("*")){
                e = new Element("TrimLow",WCS.WCS_NS);
                e.setText(_trimLow);
                ds.addContent(e);
            }
            if(!_trimHigh.equals("*")){
                e = new Element("TrimHigh",WCS.WCS_NS);
                e.setText(_trimHigh);
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
    public String getDap2GridValueConstraint() throws WcsException {

        if(_domainCoordinate == null){
            StringBuilder msg = new StringBuilder("The DimensionSubset '").append(getDimensionId()).append("' ");
            msg.append("Has not been associated with a DomainCoordinate instance.");

            throw new WcsException(msg.toString(),WcsException.INVALID_PARAMETER_VALUE,"DimensionSubset");
        }

        String dapVarName = _domainCoordinate.getDapID();

        StringBuilder subsetClause = new StringBuilder();

        if(isValueSubset()){

          switch (getType()) {
              case TRIM:
                  subsetClause
                          .append("\"")
                          .append(getTrimLow())
                          .append("<=")
                          .append(dapVarName)
                          .append("<=")
                          .append(getTrimHigh())
                          .append("\"");

                  break;
              case SLICE_POINT:
                  subsetClause
                          .append("\"")
                          .append(dapVarName)
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
        return _slicePoint !=null;
    }

    public boolean isTrimSubset(){
        return _trimHigh !=null && _trimLow !=null;
    }

    public String getSlicePoint(){
        return _slicePoint;
    }

    public String getTrimHigh(){
        return _trimHigh;
    }

    public String getTrimLow(){
        return _trimLow;
    }

    public String getDimensionId(){
        return _dimensionId;
    }

}
