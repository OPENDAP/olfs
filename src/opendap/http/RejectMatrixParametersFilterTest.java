package opendap.http;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.*;

public class RejectMatrixParametersFilterTest {

    private RejectMatrixParametersFilter filter;

    @Before
    public void setUp() {
        filter = new RejectMatrixParametersFilter();
    }

    @Test
    public void testInitAndDestroyDoNotThrowException() throws ServletException {
        filter.init(null);
        filter.destroy();
    }

    @Test
    public void testDoFilterWithoutMatrixParametersPassesChain() throws IOException, ServletException {
        TestFilterChain chain = new TestFilterChain();
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestHttpServletRequest request = new TestHttpServletRequest("/dap/data/test.nc");

        filter.doFilter(request, response, chain);

        assertTrue("Chain should have been invoked", chain.wasCalled());
        assertFalse("Error response should not have been sent", response.wasSendErrorCalled());
    }

    @Test
    public void testDoFilterWithNullUriPassesChain() throws IOException, ServletException {
        TestFilterChain chain = new TestFilterChain();
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestHttpServletRequest request = new TestHttpServletRequest(null);

        filter.doFilter(request, response, chain);

        assertTrue("Chain should have been invoked when URI is null", chain.wasCalled());
        assertFalse("Error response should not have been sent", response.wasSendErrorCalled());
    }

    @Test
    public void testDoFilterWithMatrixParameterRejectsRequest() throws IOException, ServletException {
        TestFilterChain chain = new TestFilterChain();
        TestHttpServletResponse response = new TestHttpServletResponse();
        TestHttpServletRequest request = new TestHttpServletRequest("/dap/data/test.nc;jsessionid=12345");

        filter.doFilter(request, response, chain);

        assertFalse("Chain should NOT have been invoked", chain.wasCalled());
        assertTrue("Error response should have been sent", response.wasSendErrorCalled());
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getErrorCodeSent());
        assertEquals("Hyrax", response.getHeader("Server"));
    }

    @Test
    public void testDoFilterWithNonHttpRequestPassesChain() throws IOException, ServletException {
        TestFilterChain chain = new TestFilterChain();
        ServletRequest genericRequest = new DummyServletRequest();
        ServletResponse genericResponse = new DummyServletResponse();

        filter.doFilter(genericRequest, genericResponse, chain);

        assertTrue("Chain should have been invoked for non-HTTP request", chain.wasCalled());
    }

    // =========================================================================
    // Lightweight Test Fakes (No Mockito Required)
    // =========================================================================

    private static class TestFilterChain implements FilterChain {
        private boolean called = false;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            this.called = true;
        }

        public boolean wasCalled() {
            return called;
        }
    }

    private static class TestHttpServletRequest extends DummyHttpServletRequest {
        private final String uri;

        public TestHttpServletRequest(String uri) {
            this.uri = uri;
        }

        @Override
        public String getRequestURI() {
            return uri;
        }
    }

    private static class TestHttpServletResponse extends DummyHttpServletResponse {
        private String serverHeader;
        private int errorCodeSent = -1;
        private boolean sendErrorCalled = false;

        @Override
        public void setHeader(String name, String value) {
            if ("Server".equalsIgnoreCase(name)) {
                this.serverHeader = value;
            }
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.sendErrorCalled = true;
            this.errorCodeSent = sc;
        }

        @Override
        public String getHeader(String name) {
            return serverHeader;
        }

        public boolean wasSendErrorCalled() {
            return sendErrorCalled;
        }

        public int getErrorCodeSent() {
            return errorCodeSent;
        }
    }

    // Abstract dummy classes providing default no-op / null returns for unused Servlet interface methods
    private abstract static class DummyHttpServletRequest implements HttpServletRequest {
        public String getAuthType() { return null; }
        public javax.servlet.http.Cookie[] getCookies() { return null; }
        public long getDateHeader(String name) { return 0; }
        public String getHeader(String name) { return null; }
        public java.util.Enumeration<String> getHeaders(String name) { return null; }
        public java.util.Enumeration<String> getHeaderNames() { return null; }
        public int getIntHeader(String name) { return 0; }
        public String getMethod() { return null; }
        public String getPathInfo() { return null; }
        public String getPathTranslated() { return null; }
        public String getContextPath() { return null; }
        public String getQueryString() { return null; }
        public String getRemoteUser() { return null; }
        public boolean isUserInRole(String role) { return false; }
        public java.security.Principal getUserPrincipal() { return null; }
        public String getRequestedSessionId() { return null; }
        public String getRequestURI() { return null; }
        public StringBuffer getRequestURL() { return null; }
        public String getServletPath() { return null; }
        public javax.servlet.http.HttpSession getSession(boolean create) { return null; }
        public javax.servlet.http.HttpSession getSession() { return null; }
        public String changeSessionId() { return null; }
        public boolean isRequestedSessionIdValid() { return false; }
        public boolean isRequestedSessionIdFromCookie() { return false; }
        public boolean isRequestedSessionIdFromURL() { return false; }
        public boolean isRequestedSessionIdFromUrl() { return false; }
        public boolean authenticate(HttpServletResponse response) { return false; }
        public void login(String username, String password) {}
        public void logout() {}
        public java.util.Collection<javax.servlet.http.Part> getParts() { return null; }
        public javax.servlet.http.Part getPart(String name) { return null; }
        public <T extends javax.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        public Object getAttribute(String name) { return null; }
        public java.util.Enumeration<String> getAttributeNames() { return null; }
        public String getCharacterEncoding() { return null; }
        public void setCharacterEncoding(String env) {}
        public int getContentLength() { return 0; }
        public long getContentLengthLong() { return 0; }
        public String getContentType() { return null; }
        public ServletInputStream getInputStream() { return null; }
        public String getParameter(String name) { return null; }
        public java.util.Enumeration<String> getParameterNames() { return null; }
        public String[] getParameterValues(String name) { return null; }
        public java.util.Map<String, String[]> getParameterMap() { return null; }
        public String getProtocol() { return null; }
        public String getScheme() { return null; }
        public String getServerName() { return null; }
        public int getServerPort() { return 0; }
        public java.io.BufferedReader getReader() { return null; }
        public String getRemoteAddr() { return null; }
        public String getRemoteHost() { return null; }
        public void setAttribute(String name, Object o) {}
        public void removeAttribute(String name) {}
        public java.util.Locale getLocale() { return null; }
        public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
        public boolean isSecure() { return false; }
        public RequestDispatcher getRequestDispatcher(String path) { return null; }
        public String getRealPath(String path) { return null; }
        public int getRemotePort() { return 0; }
        public String getLocalName() { return null; }
        public String getLocalAddr() { return null; }
        public int getLocalPort() { return 0; }
        public ServletContext getServletContext() { return null; }
        public AsyncContext startAsync() { return null; }
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) { return null; }
        public boolean isAsyncStarted() { return false; }
        public boolean isAsyncSupported() { return false; }
        public AsyncContext getAsyncContext() { return null; }
        public DispatcherType getDispatcherType() { return null; }
    }

    private abstract static class DummyHttpServletResponse implements HttpServletResponse {
        public void addCookie(javax.servlet.http.Cookie cookie) {}
        public boolean containsHeader(String name) { return false; }
        public String encodeURL(String url) { return null; }
        public String encodeRedirectURL(String url) { return null; }
        public String encodeUrl(String url) { return null; }
        public String encodeRedirectUrl(String url) { return null; }
        public void sendError(int sc, String msg) {}
        public void sendError(int sc) throws IOException {}
        public void sendRedirect(String location) {}
        public void setDateHeader(String name, long date) {}
        public void addDateHeader(String name, long date) {}
        public void setHeader(String name, String value) {}
        public void addHeader(String name, String value) {}
        public void setIntHeader(String name, int value) {}
        public void addIntHeader(String name, int value) {}
        public void setStatus(int sc) {}
        public void setStatus(int sc, String sm) {}
        public int getStatus() { return 0; }
        public String getHeader(String name) { return null; }
        public java.util.Collection<String> getHeaders(String name) { return null; }
        public java.util.Collection<String> getHeaderNames() { return null; }
        public String getCharacterEncoding() { return null; }
        public String getContentType() { return null; }
        public ServletOutputStream getOutputStream() { return null; }
        public java.io.PrintWriter getWriter() { return null; }
        public void setCharacterEncoding(String charset) {}
        public void setContentLength(int len) {}
        public void setContentLengthLong(long len) {}
        public void setContentType(String type) {}
        public void setBufferSize(int size) {}
        public int getBufferSize() { return 0; }
        public void flushBuffer() {}
        public void resetBuffer() {}
        public boolean isCommitted() { return false; }
        public void reset() {}
        public void setLocale(java.util.Locale loc) {}
        public java.util.Locale getLocale() { return null; }
    }

    private static class DummyServletRequest implements ServletRequest {
        public Object getAttribute(String name) { return null; }
        public java.util.Enumeration<String> getAttributeNames() { return null; }
        public String getCharacterEncoding() { return null; }
        public void setCharacterEncoding(String env) {}
        public int getContentLength() { return 0; }
        public long getContentLengthLong() { return 0; }
        public String getContentType() { return null; }
        public ServletInputStream getInputStream() { return null; }
        public String getParameter(String name) { return null; }
        public java.util.Enumeration<String> getParameterNames() { return null; }
        public String[] getParameterValues(String name) { return null; }
        public java.util.Map<String, String[]> getParameterMap() { return null; }
        public String getProtocol() { return null; }
        public String getScheme() { return null; }
        public String getServerName() { return null; }
        public int getServerPort() { return 0; }
        public java.io.BufferedReader getReader() { return null; }
        public String getRemoteAddr() { return null; }
        public String getRemoteHost() { return null; }
        public void setAttribute(String name, Object o) {}
        public void removeAttribute(String name) {}
        public java.util.Locale getLocale() { return null; }
        public java.util.Enumeration<java.util.Locale> getLocales() { return null; }
        public boolean isSecure() { return false; }
        public RequestDispatcher getRequestDispatcher(String path) { return null; }
        public String getRealPath(String path) { return null; }
        public int getRemotePort() { return 0; }
        public String getLocalName() { return null; }
        public String getLocalAddr() { return null; }
        public int getLocalPort() { return 0; }
        public ServletContext getServletContext() { return null; }
        public AsyncContext startAsync() { return null; }
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) { return null; }
        public boolean isAsyncStarted() { return false; }
        public boolean isAsyncSupported() { return false; }
        public AsyncContext getAsyncContext() { return null; }
        public DispatcherType getDispatcherType() { return null; }
    }

    private static class DummyServletResponse implements ServletResponse {
        public String getCharacterEncoding() { return null; }
        public String getContentType() { return null; }
        public ServletOutputStream getOutputStream() { return null; }
        public java.io.PrintWriter getWriter() { return null; }
        public void setCharacterEncoding(String charset) {}
        public void setContentLength(int len) {}
        public void setContentLengthLong(long len) {}
        public void setContentType(String type) {}
        public void setBufferSize(int size) {}
        public int getBufferSize() { return 0; }
        public void flushBuffer() {}
        public void resetBuffer() {}
        public boolean isCommitted() { return false; }
        public void reset() {}
        public void setLocale(java.util.Locale loc) {}
        public java.util.Locale getLocale() { return null; }
    }
}