/////////////////////////////////////////////////////////////////////////////
// This file is part of the "Server4" project, a Java implementation of the
// OPeNDAP Data Access Protocol.
//
// Copyright (c) 2006 OPeNDAP, Inc.
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
import javax.servlet.http.HttpServletResponse;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 4:17:12 PM
 */
public class FileDispatchHandler implements DispatchHandler {

    private org.slf4j.Logger log;


    public FileDispatchHandler() {

        log = org.slf4j.LoggerFactory.getLogger(getClass());

    }

    public void init(DispatchServlet servlet) throws Exception
    {

    }

    public boolean requestCanBeHandled(DispatchServlet servlet,
                                       HttpServletRequest request)
            throws Exception {
        return fileDispatch(request,null,servlet,false);

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {

    }


    public void destroy() {

    }

    /**
     * Performs dispatching for file requests. If a request is not for a
     * special service or an OPeNDAP service then we attempt to resolve the
     * request to a "file" located within the context of the data handler and
     * return it's contents.
     *
     * @param request  .
     * @param response .
     * @return true if the request was serviced as a file request, false
     * otherwise.
     * @throws Exception .
     */
    public boolean fileDispatch(HttpServletRequest request,
                                HttpServletResponse response) throws Exception {


        String fullSourceName = ReqInfo.getFullSourceName(request);

        DataSourceInfo dsi = odh.getDataSourceInfo(fullSourceName);

        boolean isFileResponse = false;

        if (dsi.sourceExists()) {
            if (dsi.isCollection()) {
                if (odh.useOpendapDirectoryView()) {
                    odh.sendDir(request, response);
                    PerfLog.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");
                } else {
                    getThreddsCatalog(request, response);
                }

            } else {

                if (!dsi.isDataset() || odh.allowDirectDataSourceAccess()) {
                    odh.sendFile(request, response);
                    PerfLog.logServerAccessEnd(HttpServletResponse.SC_OK, -1, "HyraxAccess");
                } else {
                    sendDirectAccessDenied(request, response);
                    PerfLog.logServerAccessEnd(HttpServletResponse.SC_FORBIDDEN, -1, "HyraxAccess");
                }
            }
            isFileResponse = true;

        }

        return isFileResponse;

    }







}
