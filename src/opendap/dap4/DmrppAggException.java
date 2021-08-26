package opendap.dap4;

import java.lang.Exception;

public class DmrppAggException extends Exception {

    public DmrppAggException(){
        super();
    }

    public DmrppAggException(String message){
        super(message);
    }

    public DmrppAggException(String message, Throwable cause){
        super(message,cause);
    }

    protected DmrppAggException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace){
        super(message, cause, enableSuppression, writableStackTrace);
    }
    protected DmrppAggException(Throwable cause){
        super(cause);
    }


}
