package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

/**
 * Created by ndp on 1/27/15.
 */
public class Json extends MediaType {

    public static final String NAME = "json";


    public Json(){
        this("."+ NAME);
        setName(NAME);

    }

    public Json(String typeMatchString){
        super("application","json", typeMatchString);
    }

}
