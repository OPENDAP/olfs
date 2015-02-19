package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

/**
 * Created by ndp on 1/27/15.
 */
public class Netcdf3 extends MediaType {

    public static final String NAME = "nc";

    public Netcdf3(){

        this("."+ NAME);
        setName(NAME);

    }

    public Netcdf3(String typeMatchString){
        super("application","x-netcdf", typeMatchString);
    }

}