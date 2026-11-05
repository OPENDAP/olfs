package opendap.http;

import opendap.coreServlet.OPeNDAPException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RejectMatrixParametersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No state, no configuration, nothing to do here.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String uri = httpRequest.getRequestURI();

            // Reject requests containing a semicolon in the URI path
            if (uri != null && uri.contains(";")) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setHeader("Server","Hyrax");
                OPeNDAPException.setCachedErrorMessage("Hyrax does not support the use of HTTP matrix parameters.");
                httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Nothing to do because we have no state.
    }
}