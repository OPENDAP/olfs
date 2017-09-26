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
package opendap.services;

import opendap.services.Service;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 5/30/14
 * Time: 6:46 PM
 * To change this template use File | Settings | File Templates.
 */
public interface WebServiceHandler extends Service {

    public void init(HttpServlet servlet, Element config) throws ServletException;

    public String getName();

    public boolean datasetCanBeViewed(String datasetId, Document ddx);

    public String getServiceLink(String datasetUrl);

    public String getThreddsServiceType();

    public String getThreddsUrlPath(String datasetUrl);


}
