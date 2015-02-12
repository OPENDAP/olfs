package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

/**
 * Created by ndp on 1/27/15.
 */
public class Netcdf4 extends MediaType {

    public Netcdf4(){
        this("nc4");
    }

    public Netcdf4(String typeMatchString){
        super("application","x-netcdf;ver=4", typeMatchString);
    }

}