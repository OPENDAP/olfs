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
import opendap.namespaces.DAP;
import opendap.namespaces.DAP4;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Scanner;

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


    private MediaType _responseType;


    public void setResponseMediaType(MediaType mt){
        _responseType = mt;
    }

    public MediaType getResponseMediaType(){
        return _responseType;
    }


    protected String _systemPath;

    public void setSystemPath(String sysPath){
        _systemPath = sysPath;
    }


    /**
     * Construct an empty <code>OPeNDAPException</code>.
     */
    public OPeNDAPException() {
        // this should never be seen, since this class overrides getMessage()
        // to display its own error message.
        super("OPeNDAPException");
        _responseType = new TextPlain();
        _httpStatusCode =  HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        _systemPath=null;
        _log = LoggerFactory.getLogger(this.getClass());
    }

    /**
     *
     * Construct a <code>OPeNDAPException</code>.
     *
     * @param msg A message describing the error.
     *
     */
    public OPeNDAPException(String msg) {
        super(msg);
        _responseType = new TextPlain();
        _httpStatusCode =  HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        _errorMessage = msg;
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
    public OPeNDAPException(String msg, Throwable cause) {
        super(msg, cause);
        _responseType = new TextPlain();
        _httpStatusCode =  HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
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
    public OPeNDAPException(Throwable cause) {
        super(cause);
        _responseType = new TextPlain();
        _httpStatusCode =  HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
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
        _responseType = new TextPlain();
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


    public static String getDap2Error(int code, String errorMessage) {

        StringBuilder err = new StringBuilder("Error {\n");
        err.append("     code = ").append(code).append(";\n");


        // If the error message is wrapped in double quotes, print it, else,
        // add wrapping double quotes.
        if ((errorMessage != null) && (errorMessage.charAt(0) == '"'))
            err.append("    message = ").append(errorMessage).append(";\n");
        else
            err.append("    message = \"").append(errorMessage).append("\";\n");


        err.append("};\n");

        return err.toString();

    }

    /**
     * Print the DAP2 Error object on the given <code>PrintWriter</code>.
     * This code can be used by servlets to throw an OPeNDAPException to a client.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public void print(PrintStream os) {

        os.println(getDap2Error(-1,_errorMessage));
    }

    /**
     * Print the DAP2 Error object on the given <code>OutputStream</code>.
     *
     * @param os the <code>OutputStream</code> to use for output.
     */
    public final void print(OutputStream os) {
        try {
            PrintStream pw;
            pw = new PrintStream(os, true,  HyraxStringEncoding.getCharset().name());
            print(pw);
            pw.flush();
        } catch (UnsupportedEncodingException e) {
            // Oh well...
            _log.error("Unable to print error because the character set '{}' is an unsupported encoding. msg: {}",
                    HyraxStringEncoding.getCharset().displayName(), e.getMessage());
        }
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
    public static void anyExceptionHandler(Throwable t, HttpServlet servlet, String context, HttpServletResponse response) {

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

            }


            if(!response.isCommitted()){

                response.reset();

                oe.setSystemPath(ServletUtil.getSystemPath(servlet,""));

                oe.sendHttpErrorResponse(context, response);
            }
            else {
                oe.sendAsDap2Error(response);
            }

        } catch (Throwable ioe) {
            log.error("Bad things happened! Cannot process incoming " +
                    "exception! New Exception thrown: " + ioe);
        }


    }




    public void sendHttpErrorResponse(String context,  HttpServletResponse response) throws Exception {

        MediaType errorResponseMediaType = (MediaType) RequestCache.get(ERROR_RESPONSE_MEDIA_TYPE_KEY);

        if(errorResponseMediaType==null)
            errorResponseMediaType = new TextHtml();

        if(errorResponseMediaType.getPrimaryType().equalsIgnoreCase("text")) {

            if (errorResponseMediaType.getSubType().equalsIgnoreCase(TextHtml.SUB_TYPE)) {
                sendAsHtmlErrorPage(context, response);
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


        }
        sendAsHtmlErrorPage(context, response);

    }



    public static String loadHtmlTemplate(String htmlTemplateFile, String context) throws Exception {
        String template = readFileAsString(htmlTemplateFile);
        template = template.replaceAll("<CONTEXT />",context);
        return template;
    }





    public static String readFileAsString(String fileName) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        Scanner scanner = new Scanner(new File(fileName), HyraxStringEncoding.getCharset().name());

        try {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine()).append("\n");
            }
        } finally {
            scanner.close();
        }
        return stringBuilder.toString();
    }





    /**
     *
     * @param errorCode
     * @param errorMessage
     * @return Return a Document containing a Dap version 3.2 error object.
     */
    public static Document getDAP32Error(int errorCode, String errorMessage) {


        Element err = new Element("Error", DAP.DAPv32_NS);

        err.setAttribute("code",errorCode+"");

        err.setText(errorMessage);

        return new Document(err);

    }


    public int setHttpStatusCode(int code){
        //@TODO Make this thing look at the code and QC it's HTTP codyness.

        _httpStatusCode = code;
        return getHttpStatusCode();

    }



    public int getHttpStatusCode(){

        return _httpStatusCode;
    }


    /**
     * Error {
     *    code = 1005;
     *    message = "libdap error transmitting DDS: Constraint expression parse error: No such identifier in dataset: foo";
     * };
     *
     *
     * @param response
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

        sos.print("    code =  ");
        sos.print(getHttpStatusCode());
        sos.println(";");

        sos.print("    message =  ");
        sos.print(getMessage());
        sos.println(";");
        sos.println("}");

        sos.flush();

    }

    public void sendAsDap4Error(HttpServletResponse response) throws IOException{

        Dap4Error d4e = new Dap4Error();

        response.setContentType(d4e.getMimeType());
        response.setHeader("Content-Description", "DAP4 Error Object");
        response.setStatus(getHttpStatusCode());


        Element error = new Element("Error", DAP4.NS);

        error.setAttribute("httpcode",Integer.toString(getHttpStatusCode()));

        Element message = new Element("Message", DAP4.NS);

        message.setText(getMessage());

        error.addContent(message);


        ServletOutputStream sos  = response.getOutputStream();

        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(error,sos);

        sos.flush();



    }


    /**
     * {
     *   "name": "ERROR",
     *   "type": "String",
     *   "data": "Message"
     * }
     *
     * @param response
     * @throws IOException
     */
    public void sendAsJsonError(HttpServletResponse response) throws IOException {

        _log.debug("sendAsJsonError(): Sending JSON Error Object.");

        Json jsonMediaType = new Json();
        response.setContentType(jsonMediaType.getMimeType());
        // response.setContentType("text/plain");
        response.setHeader("Content-Description", "DAP2 Error Object");
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
        sos.print(getMessage());
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

    public void sendAsHtmlErrorPage(String context, HttpServletResponse response) throws Exception {


        String errorPageTemplate = _systemPath + "/error/error.html.proto";


        String template = loadHtmlTemplate(errorPageTemplate, context);

        template = template.replaceAll("<ERROR_MESSAGE />",getMessage());
        template = template.replaceAll("<ERROR_CODE />",Integer.toString(getHttpStatusCode()));

        _log.debug("sendHttpErrorResponse(): Sending Error Page ");

        MediaType responseType = new TextHtml();
        response.setContentType(responseType.getMimeType());
        response.setHeader("Content-Description", "error_page");
        response.setStatus(getHttpStatusCode());

        ServletOutputStream sos  = response.getOutputStream();

        sos.println(template);


    }



}
