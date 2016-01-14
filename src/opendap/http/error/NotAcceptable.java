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

import opendap.coreServlet.OPeNDAPException;

import javax.servlet.http.HttpServletResponse;

/**
 * The resource identified by the request is only capable of generating
 * response entities which have content characteristics not acceptable
 * according to the accept headers sent in the request.<br/>
 *
 * Unless it was a HEAD request, the response SHOULD include an entity
 * containing a list of available entity characteristics and location(s)
 * from which the user or user agent can choose the one most appropriate.
 * The entity format is specified by the media type given in the
 * Content-Type header field. Depending upon the format and the
 * capabilities of the user agent, selection of the most appropriate choice
 * MAY be performed automatically. However, this specification does not
 * define any standard for such automatic selection. <br/>
 *
 * <blockquote>
 *     Note: HTTP/1.1 servers are allowed to return responses which are
 *     not acceptable according to the accept headers sent in the
 *     request. In some cases, this may even be preferable to sending a
 *     406 response. User agents are encouraged to inspect the headers of
 *     an incoming response to determine if it is acceptable.
 * </blockquote>
 * If the response could be unacceptable, a user agent SHOULD temporarily
 * stop receipt of more data and query the user for a decision on further
 * actions.
 */
public class NotAcceptable extends OPeNDAPException {

    public NotAcceptable(String msg) {
        super(HttpServletResponse.SC_NOT_ACCEPTABLE,msg);
    }


}
