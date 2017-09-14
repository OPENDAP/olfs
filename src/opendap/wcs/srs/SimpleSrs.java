package opendap.wcs.srs;

import opendap.wcs.v2_0.ConfigurationException;
import org.jdom.Element;
import opendap.wcs.v2_0.BadParameterException;
import java.util.Arrays;
import java.util.List;

public class SimpleSrs {
    protected String _name;
    protected String _axisLabels;
    protected String _uomLabels;
    protected long   _srsDimension;

    protected SimpleSrs(){
        _name = null;
        _axisLabels = null;
        _uomLabels = null;
        _srsDimension = 0;
    }
    /**
     *
     <DefaultSRS name="urn:ogc:def:crs:EPSG::4326">
        <axisLabels>latitude longitude</axisLabels>;
        <uomLabels>deg deg</uomLabels>;
        <srsDimension>2</srsDimension>
     </DefaultSRS>

     * @param srs
     */
    public SimpleSrs(Element srs) throws ConfigurationException {

        String s = srs.getAttributeValue("name");
        if(s==null) throw new ConfigurationException("A name attribute is required for every SimpleSrs.");
        _name = s;

        Element e = srs.getChild("axisLabels");
        if(e==null) throw new ConfigurationException("An axisLabels element is required for every SimpleSrs.");
        s = e.getTextTrim();
        if(s==null || s.isEmpty()) throw new ConfigurationException("The axisLabels element must have content!");
        _axisLabels = s;

        e = srs.getChild("uomLabels");
        if(e==null) throw new ConfigurationException("A uomLabels element is required for every SimpleSrs.");
        s = e.getTextTrim();
        if(s==null || s.isEmpty()) throw new ConfigurationException("The uomLabels element must have content!");
        _uomLabels = s;

        e = srs.getChild("srsDimension");
        if(e==null) throw new ConfigurationException("An srsDimension element is required for every SimpleSrs.");
        s = e.getTextTrim();
        if(s==null || s.isEmpty()) throw new ConfigurationException("The srsDimension element must have content!");

        try {
            _srsDimension = Long.parseLong(s);
        } catch (NumberFormatException nfe){
            throw new ConfigurationException(nfe.getMessage());
        }
        if(_srsDimension < 1)
            throw new ConfigurationException("The SRS must have a dimesnion greater than 0! dimension: "+_srsDimension);
    }


    public SimpleSrs(String name, String axisLabels, String uomLabels, long  srsDimension) throws BadParameterException{

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


    public String getName(){  return _name;  }

    public String getAxisLabels(){  return _axisLabels;  }
    public List<String> getAxisLabelsList(){  return Arrays.asList(getAxisLabels());  }

    public String getUomLabels(){  return _uomLabels;  }
    public List<String> getUomLabelsList(){  return Arrays.asList(getUomLabels());  }
    public long getSrsDimension(){  return _srsDimension;  }

}
