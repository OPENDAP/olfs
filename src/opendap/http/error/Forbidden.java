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

import jakarta.servlet.http.HttpServletResponse;

/**
 * The server understood the request, but is refusing to fulfill it. Authorization will
 * not help and the request SHOULD NOT be repeated. If the request method was not HEAD
 * and the server wishes to make public why the request has not been fulfilled, it SHOULD
 * describe the reason for the refusal in the entity. If the server does not wish to make
 * this information available to the client, the status code 404 (Not Found) can be used instead.
 */
public class Forbidden extends OPeNDAPException{

    public Forbidden(String msg) {
        super(HttpServletResponse.SC_FORBIDDEN,msg);
    }

}
