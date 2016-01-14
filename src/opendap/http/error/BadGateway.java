package opendap.http.error;

import opendap.coreServlet.OPeNDAPException;

import javax.servlet.http.HttpServletResponse;

/**
 * The server encountered an unexpected condition which prevented it from fulfilling the request.
 */
public class BadGateway extends OPeNDAPException{

    public BadGateway(String msg) {
        super(HttpServletResponse.SC_BAD_GATEWAY,msg);
    }

}
