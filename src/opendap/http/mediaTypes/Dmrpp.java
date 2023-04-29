package opendap.http.mediaTypes;

import opendap.bes.dap4Responders.MediaType;

public class Dmrpp extends MediaType {
    public static final String NAME = "dmrpp";
    public static final String SUFFIX ;
    static {
        SUFFIX= "."+ NAME;
    }

    public static final String PRIMARY_TYPE = "application";
    public static final String SUB_TYPE = "vnd.opendap.dap4.dmrpp+xml";

    public Dmrpp(){
        this(SUFFIX);
        setName(NAME);
    }

    public Dmrpp(String typeMatchString){
        super(PRIMARY_TYPE,SUB_TYPE, typeMatchString);
    }

}
