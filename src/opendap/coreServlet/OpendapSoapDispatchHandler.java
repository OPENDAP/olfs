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
import javax.servlet.ServletException;

/**
 * Impelmentations of this interface may plugged into the DispatchServlet_OLD (via the web.xml file) to
 * provide specific Server responses to SOAP requests. The DispatchServlet_OLD REQUIRES an implmentation of this interface
 * to be identified in the web.xml file.
 */
public interface OpendapSoapDispatchHandler extends OpendapMimeHeaders {

    public void init(HttpServlet ds) throws ServletException;



    /**
     * Handles a SOAP request for a OPeNDAP Data.
     *
     * @param reqID The request ID from the Request Element in the SOAP body
     * @param xmlBase The request URL for use in the DDX.
     * @param cmd The GetDATA commad Element from the the Request.
     * @param mpr The multipart response into which the DATA response should be added.
     * @throws Exception When things go awry.
     */
    public void getDATA(String reqID, String xmlBase, Element cmd, MultipartResponse mpr) throws Exception;

    /**
     * Handles a SOAP request for an OPeNDAP DDX.
     *
     * @param reqID The request ID from the Request Element in the SOAP body
     * @param xmlBase The request URL for use in the DDX.
     * @param cmd The GetDDX commad Element from the the Request.
     * @param mpr The multipart response into which the DDX should be added.
     * @throws Exception When things go awry.
     */
    public void getDDX(String reqID, String xmlBase, Element cmd, MultipartResponse mpr) throws Exception;

    /**
     * Handles a SOAP request for a THREDDS catalog.
     *
     * @param srvReq The HttpServletRequest object associated with this SOAP request
     * @param reqID The request ID from the Request Element in the SOAP body
     * @param cmd The GetTHREDDSCatalog commad Element from the the Request.
     * @param mpr The multipart response into which the THREDDS catalog should be added.
     * @throws Exception When things go awry.
     */
    public void getTHREDDSCatalog(HttpServletRequest srvReq,  String reqID, Element cmd, MultipartResponse mpr) throws Exception;





}
