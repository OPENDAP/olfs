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

package opendap.ppt;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 11/18/13
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class PPTEndOfStreamException extends PPTException{
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
    public PPTEndOfStreamException(String msg) {
        super(msg);
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
    public PPTEndOfStreamException(String msg,Throwable cause) {
        super(msg,cause);
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
    public PPTEndOfStreamException(Throwable cause) {
        super(cause);
    }
}
