package opendap.wcs.v2_0;

import org.jdom.Element;

import java.util.Arrays;
import java.util.List;

public class SimpleSrs {
    private String _name;
    private String _axisLabels;
    private String _uomLabels;
    private long   _srsDimension;

    /**
     *
     <DefaultSRS name="urn:ogc:def:crs:EPSG::4326">
        <axisLabels>latitude longitude</axisLabels>;
        <uomLabels>deg deg</uomLabels>;
        <srsDimension>2</srsDimension>
     </DefaultSRS>

     * @param srs
     */
    SimpleSrs(Element srs) throws BadParameterException{

        String s = srs.getAttributeValue("name");
        if(s==null) throw new BadParameterException("A name attribute is required for every SimpleSrs.");
        _name = s;

        Element e = srs.getChild("axisLabels");
        if(e==null) throw new BadParameterException("An axisLabels element is required for every SimpleSrs.");
        s = e.getTextTrim();
        if(s==null || s.isEmpty()) throw new BadParameterException("The axisLabels element must have content!");
        _axisLabels = s;

        e = srs.getChild("uomLabels");
        if(e==null) throw new BadParameterException("A uomLabels element is required for every SimpleSrs.");
        s = e.getTextTrim();
        if(s==null || s.isEmpty()) throw new BadParameterException("The uomLabels element must have content!");
        _uomLabels = s;

        e = srs.getChild("srsDimension");
        if(e==null) throw new BadParameterException("An srsDimension element is required for every SimpleSrs.");
        s = e.getTextTrim();
        if(s==null || s.isEmpty()) throw new BadParameterException("The srsDimension element must have content!");

        try {
            _srsDimension = Long.parseLong(s);
        } catch (NumberFormatException nfe){
            throw new BadParameterException(nfe.getMessage());
        }
        if(_srsDimension < 1)
            throw new BadParameterException("The SRS must have a dimesnion greater than 0! dimension: "+_srsDimension);
    }


    SimpleSrs(String name, String axisLabels, String uomLabels, long  srsDimension) throws BadParameterException{

        if(name==null) throw new BadParameterException("A name attribute is required for every SimpleSrs.");
        _name = name;

        if(axisLabels==null || axisLabels.isEmpty()) throw new BadParameterException("The axisLabels element must have content!");
        _axisLabels = axisLabels;

        if(uomLabels==null || uomLabels.isEmpty()) throw new BadParameterException("The uomLabels element must have content!");
        _uomLabels = uomLabels;

        if(srsDimension < 1)
            throw new BadParameterException("The SRS must have a dimesnion greater than 0! dimension: "+srsDimension);
        _srsDimension = srsDimension;
    }


    String getName(){  return _name;  }

    String getAxisLabels(){  return _axisLabels;  }
    List<String> getAxisLabelsList(){  return Arrays.asList(getAxisLabels());  }

    String getUomLabels(){  return _uomLabels;  }
    List<String> getUomLabelsList(){  return Arrays.asList(getUomLabels());  }
    long getSrsDimension(){  return _srsDimension;  }

}
