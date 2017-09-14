package opendap.wcs.v2_0;

import java.io.IOException;

public class BadParameterException extends IOException {
    public BadParameterException(){
        super();
    }


    public BadParameterException(String msg){
        super(msg);

    }


    public BadParameterException(String msg,Throwable cause){
        super(msg,cause);
    }

    public BadParameterException(Throwable cause){
        super(cause);
    }
}
