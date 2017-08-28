package opendap.wcs.v2_0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

public class RangeSubset {
    private Logger _log;
    private String _kvpSubsetString;
    private Vector<String> _requestedFields;

    RangeSubset(String kvpSubsetString, Vector<Field> fields) throws WcsException {
        _log = LoggerFactory.getLogger(getClass());
        _kvpSubsetString = kvpSubsetString;
        _requestedFields = new Vector<>();

        String[] ids = _kvpSubsetString.split(",");
        for(String id:ids){
            if(!id.isEmpty()){

                // Ranges of fields are expressed with a ":"
                if(id.contains(":")){
                    // Looks like a range expression - evaluate it.
                    String[] ss = id.split(":");
                    if(ss.length!=2)
                        throw new WcsException("Unable to process field list range expression.",
                                WcsException.INVALID_PARAMETER_VALUE);
                    String start = ss[0];
                    String stop = ss[1];

                    boolean gitit = false;
                    for(Field field : fields){
                        if(field.getName().equals(start)) gitit=true;

                        if(gitit) _requestedFields.add(field.getName());

                        if(field.getName().equals(stop))  gitit=false;
                    }
                }
                else {
                    _requestedFields.add(id);
                }


            }
        }
    }

    public Vector<String> getRequestedFields(){
        return  new Vector<>(_requestedFields);
    }
}
