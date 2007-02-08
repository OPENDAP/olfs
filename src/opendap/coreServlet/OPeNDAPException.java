/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
// Author: Nathan David Potter  <ndp@opendap.org>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.coreServlet;



import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Wraps the Exception class so that it can be serialized as a DAP2 error object.
 * Includes methods for assigning DAP2 Error codes to the error.
 * <p/>
 *
 * @author ndp
 */
public class OPeNDAPException extends Exception {

    /**
     * Undefined error.
     */
    public static final int UNDEFINED_ERROR = -1;
    /**
     * Unknown error.
     */
    public static final int UNKNOWN_ERROR = 0;
    /**
     * The file specified by the OPeNDAP URL does not exist.
     */
    public static final int NO_SUCH_FILE = 1;
    /**
     * The variable specified in the OPeNDAP URL does not exist.
     */
    public static final int NO_SUCH_VARIABLE = 2;
    /**
     * The expression specified in the OPeNDAP URL is not valid.
     */
    public static final int MALFORMED_EXPR = 3;
    /**
     * The user has no authorization to read the OPeNDAP URL.
     */
    public static final int NO_AUTHORIZATION = 4;
    /**
     * The file specified by the OPeNDAP URL can not be read.
     */
    public static final int CANNOT_READ_FILE = 5;


    /**
     * The error code.
     *
     * @serial
     */
    private int errorCode;
    /**
     * The error message.
     *
     * @serial
     */
    private String errorMessage;


    /**
     * Construct an empty <code>OPeNDAPException</code>.
     */
    public OPeNDAPException() {
        // this should never be seen, since this class overrides getMessage()
        // to display its own error message.
        super("OPeNDAPException");
    }

    /**
     * Construct a <code>OPeNDAPException</code>.
     */
    public OPeNDAPException(String msg) {
        super(msg);
        errorCode = UNKNOWN_ERROR;
        errorMessage = msg;
    }


    /**
     * Construct a <code>OPeNDAPException</code>.
     */
    public OPeNDAPException(String msg, Exception e) {
        super(msg, e);
        errorCode = UNKNOWN_ERROR;
        errorMessage = msg;
    }


    /**
     * Construct a <code>OPeNDAPException</code>.
     */
    public OPeNDAPException(String msg, Throwable t) {
        super(msg, t);
        errorCode = UNKNOWN_ERROR;
        errorMessage = msg;
    }


    /**
     * Construct a <code>OPeNDAPException</code>.
     */
    public OPeNDAPException(Throwable t) {
        super(t);
        errorCode = UNKNOWN_ERROR;
        errorMessage = t.getMessage();
    }


    /**
     * Construct a <code>OPeNDAPException</code> with the given message.
     *
     * @param code the error core
     * @param msg  the error message
     */
    public OPeNDAPException(int code, String msg) {
        super(msg);
        errorCode = code;
        errorMessage = msg;
    }


    /**
     * Returns the error code.
     *
     * @return the error code.
     */
    public final int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the error message.
     *
     * @return the error message.
     */
    public final String getErrorMessage() {
        return errorMessage;
    }


    /**
     * Returns the detail message of this throwable object.
     *
     * @return the detail message of this throwable object.
     */
    public String getMessage() {
        return errorMessage;
    }

    /**
     * Sets the error code.
     *
     * @param code the error code.
     */
    public final void setErrorCode(int code) {
        errorCode = code;
    }

    /**
     * Sets the error message.
     *
     * @param msg the error message.
     */
    public final void setErrorMessage(String msg) {
        errorMessage = msg;
    }


    public String getDAP2Error() {

        String err = "Error {\n";

        err += "    code = " + errorCode + ";\n";

        // If the error message is wrapped in double quotes, print it, else,
        // add wrapping double quotes.
        if ((errorMessage != null) && (errorMessage.charAt(0) == '"'))
            err += "    message = " + errorMessage + ";\n";
        else
            err += "    message = \"" + errorMessage + "\";\n";

        err += "};\n";

        return err;

    }

    /**
     * Print the DAP2 Error object on the given <code>PrintWriter</code>.
     * This code can be used by servlets to throw an OPeNDAPException to a client.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public void print(PrintWriter os) {

        os.println(getDAP2Error());
    }

    /**
     * Print the DAP2 Error object on the given <code>OutputStream</code>.
     *
     * @param os the <code>OutputStream</code> to use for output.
     * @see OPeNDAPException#print(PrintWriter)
     */
    public final void print(OutputStream os) {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
        print(pw);
        pw.flush();
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
    public static void anyExceptionHandler(Throwable t, HttpServletResponse response) {

        Logger log = org.slf4j.LoggerFactory.getLogger(OPeNDAPException.class);


        log.error("anyExceptionHandler(): " + t);
        DebugLog.printThrowable(t);


        try {


            if(!response.isCommitted()){
                response.reset();
                response.setHeader("Content-Description", "dods_error");

                // This should probably be set to "plain" but this works, the
                // C++ slients don't barf as they would if I sent "plain" AND
                // the C++ don't expect compressed data if I do this...
                response.setHeader("Content-Encoding", "");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            BufferedOutputStream eOut = new BufferedOutputStream(response.getOutputStream());


            OPeNDAPException oe;

            if (t instanceof OPeNDAPException)
                oe = (OPeNDAPException) t;
            else {


                String msg = t.getClass().getName()+": ";
                msg += t.getMessage();

                msg += " [" + t.getStackTrace()[0].getFileName() +
                        " - line " + t.getStackTrace()[0].getLineNumber() + "]";


                if (msg != null)
                    msg = msg.replace('\"', '\'');


                oe = new OPeNDAPException(UNDEFINED_ERROR, msg);

            }
            oe.print(eOut);


        } catch (IOException ioe) {
            log.error("Cannot respond to client! IO Error: " + ioe.getMessage());
            DebugLog.println("Cannot respond to client! IO Error: " + ioe.getMessage());
        }


    }
    /***************************************************************************/


}
