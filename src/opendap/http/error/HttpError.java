package opendap.http.error;

/**
 * Created by ndp on 1/27/15.
 */
public class HttpError extends Exception {
    protected int _status;


    public HttpError(String msg) {
        super(msg);
    }

    public HttpError(String msg, Exception e) {
        super(msg, e);
    }

    public HttpError(String msg, Throwable cause) {
        super(msg, cause);
    }

    public HttpError(Throwable cause) {
        super(cause);
    }

    public int status() { return _status; }


}
