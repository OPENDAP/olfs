package opendap.wcs.srs;

import opendap.coreServlet.Util;
import opendap.wcs.v2_0.BadParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class SimpleSrs {
    protected String _name;
    protected String _axisLabels;
    protected String _uomLabels;
    protected long   _srsDimension;

    protected Vector<String> _axisLabelList;
    protected Vector<String> _uomLabelList;

    protected SimpleSrs(){
        _name = null;
        _axisLabels = null;
        _uomLabels = null;
        _srsDimension = 0;
        _axisLabelList = new Vector<>();
        _uomLabelList = new Vector<>();
    }

    public SimpleSrs(SimpleSrs s){
        _name =s._name;
        _axisLabels = s._axisLabels;
        _uomLabels = s._uomLabels;
        _srsDimension = s._srsDimension;
        _axisLabelList = new Vector<>();
        _axisLabelList.addAll(s._axisLabelList);
        _uomLabelList = new Vector<>();
        _uomLabelList.addAll(s._uomLabelList);
    }

    public SimpleSrs(String name, String axisLabels, String uomLabels, long  srsDimension) throws BadParameterException{
        this();

        if(name==null) throw new BadParameterException("A name attribute is required for every SimpleSrs.");
        _name = name;

        if(axisLabels==null || axisLabels.isEmpty()) throw new BadParameterException("The axisLabels element must have content!");
        _axisLabels = axisLabels;


        String[] labels = _axisLabels.split(Util.WHITE_SPACE_REGEX_STRING);
        _axisLabelList.addAll(Arrays.asList(labels));

        if(uomLabels==null || uomLabels.isEmpty()) throw new BadParameterException("The uomLabels element must have content!");
        _uomLabels = uomLabels;
        labels = _uomLabels.split(Util.WHITE_SPACE_REGEX_STRING);
        _uomLabelList.addAll(Arrays.asList(labels));


        if(srsDimension < 1)
            throw new BadParameterException("The SRS must have a dimesnion greater than 0! dimension: "+srsDimension);
        _srsDimension = srsDimension;
    }


    public String getName(){  return _name;  }

    public String getAxisLabels(){  return _axisLabels;  }
    public List<String> getAxisLabelsList(){  return _axisLabelList;  }

    public String getUomLabels(){  return _uomLabels;  }
    public List<String> getUomLabelsList(){ return _uomLabelList; }
    public long getSrsDimension(){  return _srsDimension;  }

}
