package opendap.wcs.v2_0;

public class BadParameterException extends Exception {
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
