package opendap.wcs.v2_0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

public class RangeSubset {
    private Logger _log;
    private String _kvpSubsetString;
    private Vector<String> _requestedFields;

    RangeSubset(String kvpSubsetString){
        _log = LoggerFactory.getLogger(getClass());
        _kvpSubsetString = kvpSubsetString;
        _requestedFields = new Vector<>();

        String[] ids = _kvpSubsetString.split(",");
        for(String id:ids){
            if(!id.isEmpty()){
                _requestedFields.add(id);
            }
        }
    }

    public Vector<String> getRequestedFields(){
        return  new Vector<>(_requestedFields);
    }
}
