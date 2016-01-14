package opendap.http.error;

import opendap.coreServlet.OPeNDAPException;

import javax.servlet.http.HttpServletResponse;

/**
 * The server does not support the functionality required to fulfill the request. This is the appropriate response when the server does not recognize the request method and is not capable of supporting it for any resource.
 */
public class NotImplemented extends OPeNDAPException {
    public NotImplemented(String msg) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg);
    }
}
