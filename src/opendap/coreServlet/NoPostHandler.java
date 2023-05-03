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

import opendap.bes.BesApi;
import org.jdom.Element;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 7/27/12
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoPostHandler implements DispatchHandler {
    public void init(HttpServlet servlet, Element config) throws Exception {
        // Do nothing
    }

    public void init(HttpServlet servlet, Element config, BesApi besApi) throws Exception {
        // Do nothing
    }

    public boolean requestCanBeHandled(HttpServletRequest request) throws Exception {
        return true;  // Always respond
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletOutputStream sos = response.getOutputStream();
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.setHeader("Allow","GET");
        sos.println("POST is not supported for this resource on this server.");
    }

    public long getLastModified(HttpServletRequest req) {
        return new Date().getTime();  // Return current date/time
    }

    public void destroy() {
        // Do nothing
    }

}
