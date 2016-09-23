package opendap.wcs.v2_0;

/**
 * Created by ndp on 9/22/16.
 */
public class Dimension implements Cloneable {

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


    Dimension(){
        _coordinate = null;
        _lowerBound = Double.NaN;
        _upperBound = Double.NaN;

    }

    Dimension(Dimension d){
        _coordinate = d._coordinate;
        _lowerBound = d._lowerBound;
        _upperBound = d._upperBound;

    }

    Dimension(String name, double min, double max) throws WcsException{
        setCoordinate(name);
        setMax(min);
        setMax(max);
    }

    public void setMax(double max){ _upperBound = max; }
    public double getMax(){ return _upperBound;}

    public void setMin(double min){ _lowerBound = min; }
    public double getMin(){ return _lowerBound;}

    public Coordinate getCoordinate() { return _coordinate;}

    public Coordinate setCoordinate(String name) throws WcsException {

        Coordinate coordinate;
        name = name.toLowerCase();

        if(name.startsWith("lat")) {
            coordinate = Coordinate.LATITUDE;
        }
        else if(name.startsWith("lon")){
            coordinate = Coordinate.LONGITUDE;

        }
        else if(name.equals("phenomenonTime") || name.startsWith("time")){
            coordinate = Coordinate.TIME;

        }
        else {

            throw new WcsException("Unknown coordinate axis '"+name+"'",WcsException.INVALID_PARAMETER_VALUE,"subset");

        }

        _coordinate = coordinate;

        return coordinate;

    }
}
