package opendap.http.error;

import javax.servlet.http.HttpServletResponse;

/**
 * The server understood the request, but is refusing to fulfill it. Authorization will
 * not help and the request SHOULD NOT be repeated. If the request method was not HEAD
 * and the server wishes to make public why the request has not been fulfilled, it SHOULD
 * describe the reason for the refusal in the entity. If the server does not wish to make
 * this information available to the client, the status code 404 (Not Found) can be used instead.
 */
public class Forbidden extends HttpError{
    private static final int _status = HttpServletResponse.SC_FORBIDDEN;

    public Forbidden(String msg) {
        super(msg);
        super._status = _status;
    }

    public Forbidden(String msg, Exception e) {
        super(msg, e);
        super._status = _status;
    }

    public Forbidden(String msg, Throwable cause) {
        super(msg, cause);
        super._status = _status;
    }

    public Forbidden(Throwable cause) {
        super(cause);
        super._status = _status;
    }
}
