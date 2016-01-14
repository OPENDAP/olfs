package opendap.http.error;

import opendap.coreServlet.OPeNDAPException;

import javax.servlet.http.HttpServletResponse;

/**
 * The server encountered an unexpected condition which prevented it from fulfilling the request.
 */
public class InternalError extends OPeNDAPException{

    public InternalError(String msg) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg);
    }

}
