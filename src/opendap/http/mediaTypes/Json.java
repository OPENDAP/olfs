package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

/**
 * Created by ndp on 1/27/15.
 */
public class Json extends MediaType {

    public Json(){
        this("json");
    }

    public Json(String typeMatchString){
        super("application","json", typeMatchString);
    }

}
