package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

/**
 * Created by ndp on 2/13/15.
 */
public class Dap2Data extends MediaType {

    public static final String NAME = "dods";

    public Dap2Data(){
        this("."+ NAME);
        setName(NAME);
    }


    public Dap2Data(String typeMatchString){
        super("application","octet-stream", typeMatchString);
    }



}
