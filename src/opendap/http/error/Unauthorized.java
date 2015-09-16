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
 * The request requires user authentication. The response MUST include a
 * WWW-Authenticate header field (section 14.47) containing a challenge
 * applicable to the requested resource. The client MAY repeat the request
 * with a suitable Authorization header field (section 14.8). If the
 * request already included Authorization credentials, then the 401
 * response indicates that authorization has been refused for those
 * credentials. If the 401 response contains the same challenge as the
 * prior response, and the user agent has already attempted authentication
 * at least once, then the user SHOULD be presented the entity that was
 * given in the response, since that entity might include relevant diagnostic
 * information. HTTP access authentication is explained in
 * "HTTP Authentication: Basic and Digest Access Authentication" [43].
 */
public class Unauthorized extends HttpError {
    private static final int _status = HttpServletResponse.SC_UNAUTHORIZED;

    public Unauthorized(String msg) {
        super(msg);
        super._status = _status;
    }

    public Unauthorized(String msg, Exception e) {
        super(msg, e);
        super._status = _status;
    }

    public Unauthorized(String msg, Throwable cause) {
        super(msg, cause);
        super._status = _status;
    }

    public Unauthorized(Throwable cause) {
        super(cause);
        super._status = _status;
    }
}
