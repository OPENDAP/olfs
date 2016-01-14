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


package opendap.ppt ;


import opendap.coreServlet.OPeNDAPException;

import javax.servlet.http.HttpServletResponse;

/**
 * Exception used with DODS client and server request handline.
 *
 * @author Patrick West <A * HREF="mailto:pwest@hao.ucar.edu">pwest@hao.ucar.edu</A>
 */

public class PPTException  extends OPeNDAPException {
    /**
     * Exception used with OPeNDAP client and server request handling where the
     * msg passed to the constructor represents the error that has occurred
     * in handling the OPeNDAP request.
     *
     * @param msg The error message assoicated with this exception. In many
     *            cases this message includes exception messages handled
     *            within OPeNDAP methods, including client server connection
     *            errors, send and receive error messages, etc...
     */
    public PPTException(String msg) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg);
    }
    /**
     * Exception used with OPeNDAP client and server request handling where the
     * msg passed to the constructor represents the error that has occurred
     * in handling the OPeNDAP request.
     *
     * @param msg The error message assoicated with this exception. In many
     *            cases this message includes exception messages handled
     *            within OPeNDAP methods, including client server connection
     *            errors, send and receive error messages, etc...
     * @param cause - the cause (which is saved for later retrieval by the
     * Throwable.getCause() method). (A null value is permitted, and indicates
     * that the cause is nonexistent or unknown.)
     */
    public PPTException(String msg,Throwable cause) {

        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg,cause);
    }

    /**
     * Exception used with OPeNDAP client and server request handling where the
     * msg passed to the constructor represents the error that has occurred
     * in handling the OPeNDAP request.
     *
     * @param cause - the cause (which is saved for later retrieval by the
     * Throwable.getCause() method). (A null value is permitted, and indicates
     * that the cause is nonexistent or unknown.)
     */
    public PPTException(Throwable cause) {
        super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,cause);
    }
}

