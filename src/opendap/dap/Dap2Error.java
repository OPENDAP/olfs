/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Hyrax" project, a server implementation
// of the OPeNDAP Data Access Protocol.
//
// Copyright (c) 2010, OPeNDAP, Inc.
// Copyright (c) 2002,2003 OPeNDAP, Inc.
// 
// Author: James Gallagher <jgallagher@opendap.org>
// 
// All rights reserved.
// 
// Redistribution and use in source and binary forms,
// with or without modification, are permitted provided
// that the following conditions are met:
// 
// - Redistributions of source code must retain the above copyright
//   notice, this list of conditions and the following disclaimer.
// 
// - Redistributions in binary form must reproduce the above copyright
//   notice, this list of conditions and the following disclaimer in the
//   documentation and/or other materials provided with the distribution.
// 
// - Neither the name of the OPeNDAP nor the names of its contributors may
//   be used to endorse or promote products derived from this software
//   without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
// IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
// LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
/////////////////////////////////////////////////////////////////////////////


package opendap.dap;


import opendap.io.HyraxStringEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Holds an exception thrown by OPeNDAP server to a client.
 * <p/>
 * Unlike the other OPeNDAP exceptions, this one contains extra methods to
 * get the various fields sent by the server, and a <code>parse</code> method
 * to parse the <code>Error</code> sent from the server.
 *
 * <h3>
 * This class will be changing it's name to opendap.dap.DAP2Exception.
 * I expect that it will be deprecated in the next release.
 * <h2>You've been warned.</h2> Questions? Ask ndp@opendap.org.
 * </h3>
 *
 * @author jehamby
 * @version $Revision: 25753 $
 *
 */
public class Dap2Error extends Exception {

    Logger _log;


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

    /*
    * Some Error objects may contain programs which can be used to
    * correct the reported error. These programs are run using a public
    * member function of the Error class. If an Error object does not
    * have an associated correction program, the program type is NO_PROGRAM.
    */

    /**
     * Undefined program type.
     */
    public static final int UNDEFINED_PROG_TYPE = -1;
    /**
     * This Error does not contain a program.
     */
    public static final int NO_PROGRAM = 0;
    /**
     * This Error contains Java bytecode.
     */
    public static final int JAVA_PROGRAM = 1;
    /**
     * This Error contains TCL code.
     */
    public static final int TCL_PROGRAM = 2;

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
     * The program type.
     *
     * @serial
     */
    private int programType;

    /**
     * The program source.  if programType is TCL_PROGRAM, then this is ASCII
     * text.  Otherwise, undefined (this will need to become a byte[] array if
     * the server sends Java bytecodes, for example).
     *
     * @serial
     */
    private String programSource;

    /**
     * Construct an empty <code>DAP2Exception</code>.
     */
    public Dap2Error() {
        // this should never be seen, since this class overrides getMessage()
        // to display its own error message.
        super("Dap2Error");
        _log = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Construct a <code>DAP2Exception</code>.
     */
    public Dap2Error(String msg) {
        this();
        errorCode = UNKNOWN_ERROR;
        errorMessage = msg;
        _log = LoggerFactory.getLogger(this.getClass());
    }


    /**
     * Construct a <code>DAP2Exception</code> with the given message.
     *
     * @param code the error core
     * @param msg  the error message
     */
    public Dap2Error(int code, String msg) {
        this();
        errorCode = code;
        errorMessage = msg;
        _log = LoggerFactory.getLogger(this.getClass());
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
     * Returns the program type.
     *
     * @return the program type.
     */
    public final int getProgramType() {
        return programType;
    }

    /**
     * Returns the program source.
     *
     * @return the program source.
     */
    public final String getProgramSource() {
        return programSource;
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

    /**
     * Sets the program type.
     *
     * @param type the program type.
     */
    public final void setProgramType(int type) {
        programType = type;
    }

    /**
     * Sets the program source.
     *
     * @param source the program source.
     */
    public final void setProgramSource(String source) {
        programSource = source;
    }

    /**
     * Print the Error message on the given <code>PrintWriter</code>.
     * This code can be used by servlets to throw DAP2Exception to client.
     *
     * @param os the <code>PrintWriter</code> to use for output.
     */
    public void print(PrintStream os) {
        os.println("Error {");
        os.println("    code = " + errorCode + ";");

        // If the error message is wrapped in double quotes, print it, else,
        // add wrapping double quotes.
        if ((errorMessage != null) && (errorMessage.charAt(0) == '"'))
            os.println("    message = " + errorMessage + ";");
        else
            os.println("    message = \"" + errorMessage + "\";");

        os.println("};");
    }

    /**
     * Print the Error message on the given <code>OutputStream</code>.
     *
     * @param os the <code>OutputStream</code> to use for output.
     * @see Dap2Error#print(PrintStream)
     */
    public final void print(OutputStream os) {
        PrintStream pw = null;
        try {
            pw = new PrintStream(os, true, HyraxStringEncoding.getCharsetName());
            print(pw);
            pw.flush();
        } catch (UnsupportedEncodingException e) {
            // Oh well...
            _log.error("Unable to print error because the character set '{}' is an unsupported encoding. msg: {}",
                    HyraxStringEncoding.getCharset().displayName(), e.getMessage());
        }
    }
}


