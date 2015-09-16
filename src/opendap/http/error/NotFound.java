/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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
package opendap.http.error;

import javax.servlet.http.HttpServletResponse;

/**
 * The server has not found anything matching the Request-URI. No indication is
 * given of whether the condition is temporary or permanent. The 410 (Gone)
 * status code SHOULD be used if the server knows, through some internally
 * configurable mechanism, that an old resource is permanently unavailable and
 * has no forwarding address. This status code is commonly used when the server
 * does not wish to reveal exactly why the request has been refused, or when no
 * other response is applicable.
 */
public class NotFound extends HttpError {

    private static final int _status = HttpServletResponse.SC_NOT_FOUND;

    public NotFound(String msg) {
        super(msg);
        super._status = _status;
    }

    public NotFound(String msg, Exception e) {
        super(msg, e);
        super._status = _status;

    }

    public NotFound(String msg, Throwable cause) {
        super(msg, cause);
        super._status = _status;

    }

    public NotFound(Throwable cause) {
        super(cause);
        super._status = _status;
    }

}
