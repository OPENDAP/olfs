package opendap.wcs.v2_0;

import org.jdom.Element;

/**
 * Created by ndp on 9/26/16.
 */
public class CoordinateDimensionSubset extends DimensionSubset {


    public CoordinateDimensionSubset(CoordinateDimensionSubset source) {
        super(source);
        _coordinate = source._coordinate;
    }

    public CoordinateDimensionSubset(DimensionSubset source) throws WcsException {
        super(source);
        setDimensionId(getDimensionId());
    }

    public CoordinateDimensionSubset(String kvpSubsetString) throws WcsException {
        super(kvpSubsetString);
    }

    public CoordinateDimensionSubset(Element dimensionSubsetType) throws WcsException {
        super(dimensionSubsetType);
    }


    private CoordinateDimension.Coordinate _coordinate;
    public void setCoordinate(CoordinateDimension.Coordinate coordinate){
        _coordinate = coordinate;
    }

    public CoordinateDimension.Coordinate getCoordinate(){
        return _coordinate;
    }

    @Override
    public void setDimensionId(String s) throws WcsException {

        _coordinate =   CoordinateDimension.getCoordinateByName(s);

        super.setDimensionId(s);


    }
}



