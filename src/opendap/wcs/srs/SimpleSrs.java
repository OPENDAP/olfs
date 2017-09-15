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

    public SimpleSrs(SimpleSrs s){
        _name =s._name;
        _axisLabels = s._axisLabels;
        _uomLabels = s._uomLabels;
        _srsDimension = s._srsDimension;
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
