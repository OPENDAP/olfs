package opendap.http.error;

import javax.servlet.http.HttpServletResponse;

/**
 * The server has not found anything matching the Request-URI. No indication is
 * given of whether the condition is temporary or permanent. The 410 (Gone)
 * status code SHOULD be used if the server knows, through some internally
 * configurable mechanism, that an old resource is permanently unavailable and
 * has no forwarding address. This status code is commonly used when the server
 * does not wish to reveal exactly why the request has been refused, or when no
 * other response is applicable.
 */
public class NotFound extends HttpError {

    private static final int _status = HttpServletResponse.SC_NOT_FOUND;

    public NotFound(String msg) {
        super(msg);
        super._status = _status;
    }

    public NotFound(String msg, Exception e) {
        super(msg, e);
        super._status = _status;

    }

    public NotFound(String msg, Throwable cause) {
        super(msg, cause);
        super._status = _status;

    }

    public NotFound(Throwable cause) {
        super(cause);
        super._status = _status;
    }

}
