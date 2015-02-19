package opendap.http.error;

import javax.servlet.http.HttpServletResponse;

/**
 * The request could not be understood by the server due to malformed syntax.
 * The client SHOULD NOT repeat the request without modifications.
 */
public class BadRequest extends HttpError {
    private static final int _status = HttpServletResponse.SC_BAD_REQUEST;

    public BadRequest(String msg) {
        super(msg);
        super._status = _status;
    }

    public BadRequest(String msg, Exception e) {
        super(msg, e);
        super._status = _status;
    }

    public BadRequest(String msg, Throwable cause) {
        super(msg, cause);
        super._status = _status;
    }

    public BadRequest(Throwable cause) {
        super(cause);
        super._status = _status;
    }
}
