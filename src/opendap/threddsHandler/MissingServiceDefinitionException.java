package opendap.threddsHandler;

/**
 * Created by ndp on 9/11/15.
 */
public class MissingServiceDefinitionException extends Exception {

    public MissingServiceDefinitionException(){
        super();
    }

    public MissingServiceDefinitionException(String message){
        super(message);
    }

    public MissingServiceDefinitionException(String message,Throwable cause){
        super(message,cause);
    }

    public MissingServiceDefinitionException(Throwable cause){
        super(cause);
    }

}
