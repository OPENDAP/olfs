package opendap.wcs.v2_0;

/**
 * Created by ndp on 9/22/16.
 */
public class CoordinateDimension implements Cloneable {

    private double _lowerBound;
    private double _upperBound;


    private Coordinate _coordinate;

    public enum Coordinate {
        LATITUDE("lat"),
        LONGITUDE("long"),
        TIME("phenomenonTime");

        private final String name;

        Coordinate(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }


    CoordinateDimension(){
        _coordinate = null;
        _lowerBound = Double.NaN;
        _upperBound = Double.NaN;

    }

    CoordinateDimension(CoordinateDimension d){
        _coordinate = d._coordinate;
        _lowerBound = d._lowerBound;
        _upperBound = d._upperBound;

    }


    CoordinateDimension(Coordinate c, double min, double max) throws WcsException{
        setCoordinate(c);
        setMin(min);
        setMax(max);
    }

    public void setMin(double min){ _lowerBound = min; }
    public double getMin(){ return _lowerBound;}

    public void setMax(double max){ _upperBound = max; }
    public double getMax(){ return _upperBound;}

    public Coordinate getCoordinate() { return _coordinate;}

    public void setCoordinate(Coordinate c) throws WcsException {
        _coordinate = c;
    }

    public static Coordinate getCoordinateByName(String name) throws WcsException {

        Coordinate coordinate;
        String lienient_name = name.toLowerCase();

        if(lienient_name.startsWith("lat")) {
            coordinate = Coordinate.LATITUDE;
        }
        else if(lienient_name.startsWith("lon")){
            coordinate = Coordinate.LONGITUDE;

        }
        else if(lienient_name.equals("phenomenontime") || lienient_name.startsWith("time")){
            coordinate = Coordinate.TIME;

        }
        else {

            throw new WcsException("Unknown coordinate axis '"+name+"'",WcsException.INVALID_PARAMETER_VALUE,"subset");

        }


        return coordinate;

    }
}
