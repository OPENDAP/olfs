package opendap.wcs.srs;



public class Epsg4326 extends SimpleSrs{
    public Epsg4326(){
        _name = "urn:ogc:def:crs:EPSG::4326";
        _axisLabels = "latitude longitude";
        _uomLabels = "deg deg";
        _srsDimension = 2;
    }
}
