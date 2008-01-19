/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
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

package opendap.bes;

import opendap.coreServlet.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

import org.jdom.Element;

import java.util.Date;

/**
 * User: ndp
 * Date: Apr 16, 2007
 * Time: 4:34:20 PM
 */
public class DirectoryDispatchHandler implements DispatchHandler {

    private org.slf4j.Logger log;
    private boolean initialized;
    private boolean useDefaultOpendapDirectoryView;


    public DirectoryDispatchHandler() {



        log = org.slf4j.LoggerFactory.getLogger(getClass());
        useDefaultOpendapDirectoryView = false;
        initialized = false;

    }



    public void init(DispatchServlet s, Element config) throws Exception {

        if(initialized) return;


        Element dv = config.getChild("DefaultDirectoryView");
        if(dv!=null){
            String val = dv.getTextTrim();
            if(val!=null) {
                if(val.equalsIgnoreCase("opendap")){
                    useDefaultOpendapDirectoryView = true;
                } else if(val.equalsIgnoreCase("thredds")){
                    useDefaultOpendapDirectoryView = false;
                } else {
                    throw new BadConfigurationException("The " +
                            "<DefaultDirectoryView> may have one of two " +
                            "values. Ethier \"OPeNDAP\" or \"THREDDS\".");
                }
            }
        }

        log.info("Initialized. Using " + (useDefaultOpendapDirectoryView?"OPeNDAP":"THREDDS") + " default directory view.");

        initialized = true;
    }

    public boolean requestCanBeHandled(HttpServletRequest request)
            throws Exception {

        boolean val = directoryDispatch(request, null, false);

        log.debug("requestCanBeHandled: "+val);
        return val;

    }


    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response)
            throws Exception {
        log.debug("Handling Request.");
       directoryDispatch(request, response, true);

    }


    public long getLastModified(HttpServletRequest req) {

        String name = ReqInfo.getFullSourceName(req);

        if(name.endsWith("contents.html"))
            name = name.substring(0,name.lastIndexOf("contents.html"));

        log.debug("getLastModified():  Tomcat requesting getlastModified() for collection: " + name );


        try {
            DataSourceInfo dsi = new BESDataSource(name);
            log.debug("getLastModified():  Returning: " + new Date(dsi.lastModified()));

            return dsi.lastModified();
        }
        catch (Exception e) {
            log.debug("getLastModified():  Returning: -1");
            return -1;
        }
    }


    public void destroy() {
        log.info("Destroy complete.");

    }

    /**
     * Performs dispatching for file requests. If a request is not for a
     * special service or an OPeNDAP service then we attempt to resolve the
     * request to a "file" located within the context of the data handler and
     * return it's contents.
     *
     * @param request      .
     * @param response     .
     * @param sendResponse If this is true a response will be sent. If it is
     *                     the request will only be evaluated to determine if a response can be
     *                     generated.
     * @return true if the request was serviced as a file request, false
     *         otherwise.
     * @throws Exception .
     */
    private boolean directoryDispatch(HttpServletRequest request,
                                     HttpServletResponse response,
                                     boolean sendResponse) throws Exception {


        String dataSetName = ReqInfo.getDataSetName(request);
        String requestSuffix = ReqInfo.getRequestSuffix(request);


        boolean isDirectoryResponse = false;

        if(dataSetName != null &&
            dataSetName.equalsIgnoreCase("contents") &&
            requestSuffix != null &&
            requestSuffix.equalsIgnoreCase("html")) {

            isDirectoryResponse = true;

        } else {
            try {
                String dsName = ReqInfo.getFullSourceName(request);
                DataSourceInfo dsi = new BESDataSource(dsName);
                if (dsi.sourceExists() &&
                        dsi.isCollection() &&
                        useDefaultOpendapDirectoryView) {

                        isDirectoryResponse = true;
                }
            }
            catch (BESException e){
                isDirectoryResponse = false;

            }

        }


        if (isDirectoryResponse && sendResponse) {

            S4Dir.sendDIR(request, response);

        }


        return isDirectoryResponse;

    }



}
