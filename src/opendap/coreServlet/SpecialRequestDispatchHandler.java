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

import org.jdom.Element;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Performs dispatching for "special" server requests.
 * <ui>
 * <li> help - returns the help page for Hyrax  </li>
 * <li> systemproperties - returns an html document describing the state of the "system" </li>
 * <li> debug -   </li>
 * <li> status -    </li>
 * </ui>
 * @deprecated
 */
public class SpecialRequestDispatchHandler implements DispatchHandler {

    private org.slf4j.Logger log;
    private HttpServlet servlet;
    private boolean initialized;

    public SpecialRequestDispatchHandler() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());
        servlet = null;
        initialized = false;

    }


    public void init(HttpServlet s, Element config) throws Exception {

        if(initialized) return;


        servlet = s;

        initialized = true;

        log.info("Initialized.");
    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {
        return false;

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

        return;
    }

    public long getLastModified(HttpServletRequest req) {
        return -1;
    }





    public void destroy() {
        log.info("Destroy complete.");

    }











}
