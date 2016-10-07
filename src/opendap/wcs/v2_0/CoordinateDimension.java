package opendap.wcs.v2_0;

/**
 * Created by ndp on 9/22/16.
 */
public class CoordinateDimension implements Cloneable {

    private double _lowerBound;
    private double _upperBound;


    private String _name;



    CoordinateDimension(){
        _name = null;
        _lowerBound = Double.NaN;
        _upperBound = Double.NaN;

    }

    CoordinateDimension(CoordinateDimension d){
        _name = d._name;
        _lowerBound = d._lowerBound;
        _upperBound = d._upperBound;

    }


    CoordinateDimension(String name, double min, double max) throws WcsException{
        setName(name);
        setMin(min);
        setMax(max);
    }

    public void setMin(double min){ _lowerBound = min; }
    public double getMin(){ return _lowerBound;}

    public void setMax(double max){ _upperBound = max; }
    public double getMax(){ return _upperBound;}

    public String getName() { return _name;}

    public void setName(String name) throws WcsException {
        _name = name;
    }

}
