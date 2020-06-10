/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
 * // Author: Nathan David Potter  <ndp@opendap.org>
 * //
 * // This library is free software; you can redistribute it and/or
 * // modify it under the terms of the GNU Lesser General Public
 * // License as published by the Free Software Foundation; either
 * // version 2.1 of the License, or (at your option) any later version.
 * //
 * // This library is distributed in the hope that it will be useful,
 * // but WITHOUT ANY WARRANTY; without even the implied warranty of
 * // MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * // Lesser General Public License for more details.
 * //
 * // You should have received a copy of the GNU Lesser General Public
 * // License along with this library; if not, write to the Free Software
 * // Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */

package opendap.coreServlet;


import opendap.bes.dap4Responders.MediaType;
import opendap.http.mediaTypes.*;
import opendap.io.HyraxStringEncoding;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps the Exception class so that it can be serialized as a DAP2 error object.
 * Includes methods for assigning DAP2 Error codes to the error.
 * <p/>
 *
 * @author ndp
 */
public class OPeNDAPException extends Exception {

    Logger _log;


    public static final String ERROR_RESPONSE_MEDIA_TYPE_KEY = "ErrorResponseMediaType";


    private static ConcurrentHashMap<Thread, String> _errorMessageCache;
    static {
        _errorMessageCache = new ConcurrentHashMap<>();
    }


    /**
     * Undefined error.
     */
    public static final int UNDEFINED_ERROR = -1;


    /**
     * The error message.
     *
     * @serial
     */
    private String _errorMessage;


    private int _httpStatusCode;


    private MediaType _responseMediaType;


    public void setResponseMediaType(MediaType mt){
        _responseMediaType = mt;
    }

    public MediaType getResponseMediaType(){
        return _responseMediaType;
    }


    protected String _systemPath;

    public void setSystemPath(String sysPath){
        _systemPath = sysPath;
    }


    /**
     * Construct an empty <code>OPeNDAPException</code>.
     */
    protected OPeNDAPException() {
        // this should never be seen, since this class overrides getMessage()
        // to display its own error message.
        super("OPeNDAPException");
        _responseMediaType = new TextPlain();
        _httpStatusCode =  HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        _systemPath=null;
        _log = LoggerFactory.getLogger(this.getClass());
    }



    /**
     * Construct a <code>OPeNDAPException</code>.
     * @param msg A message describing the error.
     * @param cause The cause (which is saved for later retrieval by the
     * Throwable.getCause() method). (A null value is permitted, and indicates
     * that the cause is nonexistent or unknown.)
     */
    public OPeNDAPException(int httpStatus, String msg, Throwable cause) {
        super(msg, cause);
        _responseMediaType = new TextPlain();
        _httpStatusCode =  httpStatus;
        _errorMessage = msg;
        _systemPath=null;
        _log = LoggerFactory.getLogger(this.getClass());
    }




    /**
     * Construct a <code>OPeNDAPException</code>.
     * @param cause The cause (which is saved for later retrieval by the
     * Throwable.getCause() method). (A null value is permitted, and indicates
     * that the cause is nonexistent or unknown.)
     */
    public OPeNDAPException(int httpStatus,Throwable cause) {
        super(cause);
        _responseMediaType = new TextPlain();
        _httpStatusCode =  httpStatus;
        _errorMessage = cause.getMessage();
        _systemPath=null;
        _log = LoggerFactory.getLogger(this.getClass());
    }


    /**
     * Construct a <code>OPeNDAPException</code> with the given message.
     *
     * @param code the error core
     * @param msg  the error message
     */
    public OPeNDAPException(int code, String msg) {
        super(msg);
        _responseMediaType = new TextPlain();
        _httpStatusCode =  code;
        _errorMessage = msg;
        _systemPath=null;
        _log = LoggerFactory.getLogger(this.getClass());
    }






    /**
     * Returns the detail message of this throwable object.
     *
     * @return the detail message of this throwable object.
     */
    public String getMessage() {
        return _errorMessage;
    }


    /**
     * Sets the error message.
     *
     * @param msg the error message.
     */
    public final void setErrorMessage(String msg) {
        _errorMessage = msg;
    }



    /**
     * ************************************************************************
     * Recasts any Throwable to be an OPeNDAPException and then transmits it
     * on to the passed stream as a DAP2 error object. If the passed Throwable
     * is already an OPeNDAPException, it is not recast.
     *
     * @param t        The Exception that caused the problem.
     * @param response The <code>HttpServletResponse</code> for the client.
     */
    public static int anyExceptionHandler(Throwable t, HttpServlet servlet, HttpServletResponse response) {

        Logger log = org.slf4j.LoggerFactory.getLogger(OPeNDAPException.class);


        try {

            log.error("anyExceptionHandler(): " + t);


            ByteArrayOutputStream baos =new ByteArrayOutputStream();
            PrintStream ps = new PrintStream( baos,  true, HyraxStringEncoding.getCharset().name());
            t.printStackTrace(ps);
            log.debug(baos.toString(HyraxStringEncoding.getCharset().name()));

            OPeNDAPException oe;

            if (t instanceof OPeNDAPException)
                oe = (OPeNDAPException) t;
            else {


                String msg = t.getClass().getName()+": ";
                msg += t.getMessage();

                msg += " [" + t.getStackTrace()[0].getFileName() +
                        " - line " + t.getStackTrace()[0].getLineNumber() +
                        "]";


                msg = msg.replace('\"', '\'');


                oe = new OPeNDAPException(UNDEFINED_ERROR, msg);
                oe.setHttpStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            }


            if(!response.isCommitted()){

                response.reset();

                oe.setSystemPath(ServletUtil.getSystemPath(servlet,""));

                oe.sendHttpErrorResponse(response);
            }
            else {
                oe.sendAsDap2Error(response);
                /*
                try {
                    oe.sendAsDap2Error(response);
                }
                catch(IOException ioe){
                    log.error("Failed to transmit error object to requesting client. Message: {}", ioe.getMessage());
                }
                */
            }

            return oe.getHttpStatusCode();

        } catch (Throwable ioe) {
            log.error("Bad things happened! Cannot process incoming exception! New Exception thrown: {}" , ioe.getMessage());
        }

        return -1;

    }




    public void sendHttpErrorResponse(HttpServletResponse response) throws Exception {

        MediaType errorResponseMediaType = (MediaType) RequestCache.get(ERROR_RESPONSE_MEDIA_TYPE_KEY);

        if(errorResponseMediaType==null)
            errorResponseMediaType = new TextHtml();

        if(errorResponseMediaType.getPrimaryType().equalsIgnoreCase("text")) {

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(TextHtml.SUB_TYPE)) {
                sendAsHtmlErrorPage(response);
                return;
            }


            if (errorResponseMediaType.getSubType().equalsIgnoreCase(TextPlain.SUB_TYPE)) {
                sendAsDap2Error(response);
                return;
            }

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(TextXml.SUB_TYPE)) {
                sendAsDap4Error(response);
                return;
            }

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(TextCsv.SUB_TYPE)) {
                sendAsCsvError(response);
                return;
            }
        }
        if(errorResponseMediaType.getPrimaryType().equalsIgnoreCase("application")) {

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(Json.SUB_TYPE)) {
                sendAsJsonError(response);
                return;
            }

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(DMR.SUB_TYPE)) {
                sendAsDap4Error(response);
                return;
            }
            if (errorResponseMediaType.getSubType().equalsIgnoreCase(Dap4Data.SUB_TYPE)) {
                sendAsDap4Error(response);
                return;
            }

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(Dap2Data.SUB_TYPE)) {
                sendAsDap2Error(response);
                return;
            }

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(RDF.SUB_TYPE)) {
                sendAsDap4Error(response);
                return;
            }
        }
        sendAsHtmlErrorPage(response);
    }





    public void setHttpStatusCode(int code){
        //@TODO Make this thing look at the code and QC it's HTTP codyness.
        _httpStatusCode = code;
    }


    /**
     *
     * @return  The HTTP status code associated with the error.
     */
    public int getHttpStatusCode(){

        return _httpStatusCode;
    }


    /**
     * Transmits a DAP2 encoding of the error object.
     * Error {
     *    code = 1005;
     *    message = "libdap error transmitting DDS: Constraint expression parse error: No such identifier in dataset: foo";
     * };
     *
     *
     * @param response  The response object to load up with the error response.
     * @throws IOException
     */
    public void sendAsDap2Error(HttpServletResponse response) throws IOException {

        _log.debug("sendAsDap2Error(): Sending DAP2 Error Object.");

        MediaType mt = new TextPlain();
        response.setContentType(mt.getMimeType());
        response.setHeader("Content-Description", "DAP2 Error Object");
        response.setStatus(getHttpStatusCode());

        ServletOutputStream sos  = response.getOutputStream();

        sos.println("Error { ");

        sos.print("    code = ");
        sos.print(getHttpStatusCode());
        sos.println(";");

        sos.print("    message = \"");
        sos.print(getMessage());
        sos.println("\";");
        sos.println("}");

        sos.flush();

    }

    /**
     * Transmits a DAP4 encoding of the error object.
     *
     * @param response  The response object to load up with the error response.
     * @throws IOException
     */
    public void sendAsDap4Error(HttpServletResponse response) throws IOException{

        opendap.dap4.Dap4Error d4e = new opendap.dap4.Dap4Error();
        d4e.setHttpStatusCode(getHttpStatusCode());
        d4e.setMessage(Encode.forXmlContent(getMessage()));

        response.setContentType(d4e.getMediaType().getMimeType());
        response.setHeader("Content-Description", "DAP4 Error Object");
        response.setStatus(getHttpStatusCode());

        ServletOutputStream sos  = response.getOutputStream();
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(d4e.getErrorDocument(),sos);

        sos.flush();
    }


    /**
     * Transmits a CSV encoding of a DAP error object.
     *
     * @param response  The response object to load up with the error response.
     * @throws IOException
     */
    public void sendAsCsvError(HttpServletResponse response) throws IOException {
        TextCsv csvMediaType = new TextCsv();
        response.setContentType(csvMediaType.getMimeType());
        response.setHeader("Content-Description", "Error Object");
        response.setStatus(getHttpStatusCode());
        ServletOutputStream sos = response.getOutputStream();
        sos.println("Dataset: ERROR");
        sos.println("status, " + getHttpStatusCode());
        sos.println("message, \""+getMessage()+"\"");
    }



    /**
     * {
     *   "name": "ERROR",
     *   "type": "String",
     *   "data": "Message"
     * }
     *
     * @param response  The response object to load up with the error response.
     * @throws IOException
     */
    public void sendAsJsonError(HttpServletResponse response) throws IOException {

        _log.debug("sendAsJsonError(): Sending JSON Error Object.");

        Json jsonMediaType = new Json();
        response.setContentType(jsonMediaType.getMimeType());
        // response.setContentType("text/plain");
        response.setHeader("Content-Description", "Error Object");
        response.setStatus(getHttpStatusCode());

        ServletOutputStream sos  = response.getOutputStream();

        sos.println("{");
        sos.println("  \"name\":  =  \"ERROR\",");
        sos.println("  \"type\":  =  \"node\",");
        sos.println("  \"attributes\":  =  \"[]\",");
        sos.println("  \"leaves\":  =  [\"");
        sos.println("    {");
        sos.println("      \"name\":  =  \"Message\",");
        sos.println("      \"type\":  =  \"String\",");
        sos.println("      \"attributes\":  =  \"[]\",");
        sos.print("      \"data\":  =  \"");
        sos.print(Encode.forJavaScriptBlock(getMessage()));
        sos.println("\"");
        sos.println("    },");
        sos.println("      \"name\":  =  \"HttpStatus\",");
        sos.println("      \"type\":  =  \"Int32\",");
        sos.println("      \"attributes\":  =  \"[]\",");
        sos.print("      \"data\":  =  ");
        sos.print(getHttpStatusCode());
        sos.println("");
        sos.println("    }");
        sos.println("}");
        sos.flush();
    }


    /**
     *
     * @param response  The response object to load up with the error response.
     * @throws Exception
     */
    public void sendAsHtmlErrorPage(HttpServletResponse response) throws Exception {
        
        int httpStatus = getHttpStatusCode();

        // Because the error messages are utilized by the associated JSP page they must be made available
        // for the JSP to retrieve. The RequestCache  for this thread gets destroyed when the doGet/doPost
        // methods exit which is normal and expected behavior, but the JSP page is invoked afterward so we
        // need a rendezvous for the message. We utilize this errorMessage cache for this purpose. The only
        // public method for retrieving the message is tied to the thread of execution and it removes the
        // message from the cache (clears the cache for the thread) once it is retrieved.
        _errorMessageCache.put(Thread.currentThread(), getMessage());

        // Invokes the appropriate JSP page.
        response.sendError(httpStatus);
    }


    /**
     *
     * @return  The error message cached by the current thread. This message is
     * not encoded for inclusion in a particular message type (such as HTML)
     * encoding is left to the receiver.
     */
    public static String getAndClearCachedErrorMessage(){
        return  _errorMessageCache.remove(Thread.currentThread());
    }

    /**
     * Adds the passed string to the error message cache for the current thread.
     * @param errMsg The error message.
     */
    public static void setCachedErrorMessage(String errMsg){
        _errorMessageCache.put(Thread.currentThread(),errMsg);
    }


    /**
     * Builds the mailto link to be utilized in various form , error, and directory pages
     * @param request
     * @param http_status
     * @param errorMessage
     * @param adminEmail
     * @return The support mailto link, encoded forHtmlAttribute
     */
    public static String getSupportMailtoLink(HttpServletRequest request, int http_status, String errorMessage, String adminEmail) throws UnsupportedEncodingException {

        StringBuilder sb = new StringBuilder();
        if(http_status!=200){
            sb.append("mailto:").append(adminEmail).append("?subject=Hyrax Error ").append(http_status);
        }
        else {
            sb.append("mailto:").append(adminEmail).append("?subject=Hyrax Usage Question");
        }
        sb.append("&body=");
        sb.append("%0A");
        sb.append("%0A");
        sb.append("%0A");
        sb.append("%0A");
        sb.append("%0A");
        sb.append("# -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --%0A");
        sb.append("# %0A");
        sb.append("# We're sorry you had a problem using the server.%0A");
        sb.append("# Please use the space above to describe what you%0A");
        sb.append("# were trying to do and we will try to assist you.%0A");
        sb.append("# Thanks,%0A");
        sb.append("# OPeNDAP Support.%0A");
        sb.append("# %0A");
        if(http_status !=200) {
            sb.append("# -- -- -- hyrax error info, please include -- -- --%0A");
        }
        else {
            sb.append("# -- -- -- hyrax location info, please include -- -- --%0A");
        }
        sb.append("# %0A");
        sb.append("# request_url: ");
        sb.append(request.getRequestURL().toString()).append("%0A");
        sb.append("# protocol: ").append(request.getProtocol()).append("%0A");
        sb.append("# server: ").append(request.getServerName()).append("%0A");
        sb.append("# port: ").append(request.getServerPort()).append("%0A");

        String cleanUri = (String) request.getAttribute("javax.servlet.forward.request_uri");
        if(cleanUri!=null){
            cleanUri = URLDecoder.decode(cleanUri, HyraxStringEncoding.getCharset().name());
            cleanUri = Scrub.urlContent(cleanUri);
        }
        else {
            cleanUri = "null";
        }

        sb.append("# javax.servlet.forward.request_uri: ");
        sb.append(cleanUri);

        sb.append("%0A");

        sb.append("# query_string: ");
        String queryString = request.getQueryString();
        if(queryString!=null && !queryString.isEmpty()){
            sb.append(Scrub.simpleQueryString(queryString)).append("%0A");
        }
        else {
            sb.append("n/a%0A");
        }
        sb.append("# status: ").append(http_status).append("%0A");
        if(http_status !=200) {
            sb.append("# message: ");
            if(errorMessage!=null)
                sb.append(errorMessage).append("%0A");
            else
                sb.append("no_error_message_found").append("%0A");
        }
        sb.append("# %0A");
        sb.append("# -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --%0A");
        return Encode.forHtmlAttribute(sb.toString());
    }

}
