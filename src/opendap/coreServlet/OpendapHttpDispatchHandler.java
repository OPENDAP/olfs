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
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

/**
 * Implmentations of this interface may plugged into the DispatchServlet (via the web.xml file) to
 * provide specific Server responses to HTTP GET requests. The DispatchServlet REQUIRES an implmentation of this interface
 * to be identified in the web.xml file.
 */
public interface OpendapHttpDispatchHandler {

    public void init(HttpServlet ds) throws ServletException;
    public void destroy();


    /**
     * @return A String containing the XDAP MIME header value that describes the
     *         DAP specifcation that the server response conforms to. This methof should
     *         determine which DAP secification that the client supports and then return
     *         the lowest matching version for the server being queried.
     */
    public String getXDAPVersion(HttpServletRequest request);


    /**
     * @return A String containing the value of the XOPeNDAP-Server MIME header
     *         and conforming to the DAP4 specification.
     */
    public String getXOPeNDAPServerVersion();


    /**
     * @return A string containing the value of the XDODS-Server MIME header.
     */
    public String getXDODSServerVersion();


    /**
     *
     * @return The version String for the THREDDS catalog.html page.
     */
    public String getVersionStringForTHREDDSCatalog();


    public long getLastModified(HttpServletRequest req);




    /**
     * Handles the client's DDX request.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDDX(HttpServletRequest request,
                        HttpServletResponse response)
            throws Exception;

    /**
     * Handles the client's DAS request.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDAS(HttpServletRequest request,
                        HttpServletResponse response)
                        throws Exception;

    /**
     * ------------------------------------------------------------------------------
     * <p/>
     * Handles the client's DDS request.
     * <p/>
     * <p>Once the DDS has been parsed and constrained it is sent to the
     * requesting client.
     *
     * @param request  The client's <code> HttpServletRequest</code> request object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDDS(HttpServletRequest request,
                        HttpServletResponse response)
                        throws Exception;

    /**
     * ------------------------------------------------------------------------------
     * <p/>
     * Handles the client's data request.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     * @see ReqInfo
     */
    public void sendDODS(HttpServletRequest request,
                         HttpServletResponse response)
                         throws Exception;


    /**--------------------------------------------------------------------------------
     *
     * Default handler for OPeNDAP ascii requests. Returns OPeNDAP data in
     * comma delimited ascii columns for ingestion into some not so
     * OPeNDAP enabled application such as MS-Excel.
     */
    public void sendASCII(HttpServletRequest request,
                          HttpServletResponse response)
                          throws Exception;

    /**
     * ************************************************************************
     * Handle OPeNDAP .info requests. Returns an html document
     * describing the contents of the servers datasets to the requesting
     * client.
     * <p/>
     * The "info_cache_dir" directory specified in the [Server] section
     * of the DODSiniFile is the designated location for:
     * <ul>
     * <li>".info" response override files.</li>
     * <li>Server specific HTML* files.</li>
     * <li>Dataset specific HTML* files .</li>
     * </ul>
     * <p/>
     * The server specific HTML* files must be named #servlet#.html
     * where #servlet# is the name of the servlet that is running as
     * the DODS server in question. This name is determined at run time
     * by using the class called Class ( this.getClass().getName() ).
     * <p/>
     * <p>In the C++ code the analogy is the per-cgi file names.</p>
     * <p/>
     * <p/>
     * The dataset specific HTML* files are located by catenating `.html'
     * to #name#, where #name# is the name of the dataset. If the filename part
     * of #name# is of the form [A-Za-z]+[0-9]*.* then this function also looks
     * for a file whose name is [A-Za-z].html For example, if #name# is
     * .../data/fnoc1.nc this function first looks for .../data/fnoc1.nc.html.
     * However, if that does not exist it will look for .../data/fnoc.html. This
     * allows one `per-dataset' file to be used for a collection of files with
     * the same root name.
     * </p>
     * <p/>
     * NB: An HTML* file contains HTML without the <html>, <head> or <body> tags
     * (my own notation).
     * <p/>
     * <h3>Look for the user supplied Server- and dataset-specific HTML* documents.</h3>
     *
     * @see ReqInfo
     */
    public void sendInfo(HttpServletRequest request,
                         HttpServletResponse response) throws Exception;

    public void sendDir(HttpServletRequest request,
                        HttpServletResponse response) throws Exception;


    public boolean useOpendapDirectoryView();



    /**
     * ************************************************************************
     * Default handler for the client's version request.
     * <p/>
     * <p>Returns a plain text document with server version and OPeNDAP core
     * version #'s
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */
    public void sendVersion(HttpServletRequest request,
                            HttpServletResponse response) throws Exception;

    public void sendHelpPage(HttpServletRequest request,
                             HttpServletResponse response) throws Exception;

    public void sendHTMLRequestForm(HttpServletRequest request,
                                    HttpServletResponse response) throws Exception;


    /**
     * ************************************************************************
     * Default handler for OPeNDAP catalog.xml requests.
     *
     * @param request  The client's <code> HttpServletRequest</code> request
     *                 object.
     * @param response The server's <code> HttpServletResponse</code> response
     *                 object.
     */

    public void sendCatalog(HttpServletRequest request,
                            HttpServletResponse response) throws Exception;


}
