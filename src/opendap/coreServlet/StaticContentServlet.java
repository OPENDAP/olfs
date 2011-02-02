/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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


import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Globals;
import org.slf4j.Logger;

/**
 * Provides access to
 */
public class StaticContentServlet extends org.apache.catalina.servlets.DefaultServlet {

    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(StaticContentServlet.class);
    }

    /**
     * This overrides the same method in DefaultServlet so that the servlet mappings get included
     * in the relative path.
     *
     * @param request  The request from which to derive th relative path
     * @return  A String containing the relative path
     */
    protected String getRelativePath(HttpServletRequest request) {
        // Are we being processed by a RequestDispatcher.include()?
        if (request.getAttribute(Globals.INCLUDE_REQUEST_URI_ATTR) != null) {
            String result = (String) request.getAttribute(Globals.INCLUDE_PATH_INFO_ATTR);
            if (result == null) {
                result = (String) request.getAttribute(Globals.INCLUDE_SERVLET_PATH_ATTR);
            }
            if (result == null || result.equals("")) result = "/";
            log.debug("StaticContentServlet returning " + result);
            return result;
        }
        // No, extract the desired path directly from the request.
        String result = request.getPathInfo();
        if (result == null) {
            result = request.getServletPath();
        } else {
            result = request.getServletPath() + result;
        }
        if (result == null || result.equals("")) result = "/";
        log.debug("StaticContentServlet returning " + result);
        return result;
    }
}
