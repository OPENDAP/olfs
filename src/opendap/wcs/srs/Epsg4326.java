package opendap.wcs.srs;


import opendap.coreServlet.Util;

import java.util.Arrays;

public class Epsg4326 extends SimpleSrs{
    public Epsg4326(){
        super();
        _name = "urn:ogc:def:crs:EPSG::4326";
        _axisLabels = "latitude longitude";
        _uomLabels = "deg deg";
        _srsDimension = 2;
        String[] labels = _axisLabels.split(Util.WHITE_SPACE_REGEX_STRING);
        _axisLabelList.addAll(Arrays.asList(labels));
        labels = _uomLabels.split(Util.WHITE_SPACE_REGEX_STRING);
        _uomLabelList.addAll(Arrays.asList(labels));
    }
}
