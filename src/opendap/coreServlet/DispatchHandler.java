/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2007 OPeNDAP, Inc.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * The DispatchHandler interface is implemented by classes that are used to
 * handle dispatch activities for Hyrax.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
public interface DispatchHandler {


    /**
     * Since a constructor cannot be defined for an interface there needs to
     * be a way to initialize the objects state. The init() method is that way.
     * The DispatchServlet that creates an instance of DispatchHandler will
     * pass itself into it along with the XML element that declared the
     * DispatchHandler in the configuration file (usually olfs.xml). The
     * contents of this XML Element are not restricted and may (should?)
     * contain any required information for configuration not available by
     * interogating the DispatchServlet’s methods.
     *
     * @param servlet This should be the DispatchServlet that creates the
     * instance of DispatychHandler that is being intialized.
     * @param config A JDOM Element objct containing the XML Element that
     * announced which implementation of DispatchHandler to use. It may (or
     * may not) contain additional confguration information.
     * @throws Exception When the bad things happen.
     * @see DispatchServlet
     */
    public void init(DispatchServlet servlet, Element config) throws Exception;

    /**
     *
     * @param request The request to be handled.
     * @return True if the DispatchHandler can service the request, false
     * otherwise.
     * @throws Exception When the bad things happen.
     */
    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception;

    /**
     *
     * @param request The request to be handled.
     * @param response The response object into which the response information
     * will be placed.
     * @throws Exception When the bad things happen.
     */
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception;

    /**
     *
     *
     * @param req The request for which we need to get a last modified date.
     * @return The last modified date of the URI referenced in th request.
     * @see javax.servlet.http.HttpServlet
     */
    public long getLastModified(HttpServletRequest req);


    /**
     * Called when the servlet is shutdown. Here is where to clean up open
     * connections etc.
     */
    public void destroy();

}
