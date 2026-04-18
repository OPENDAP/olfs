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
package opendap.w10n;

import opendap.PathBuilder;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;

/**
 * Created by ndp on 4/15/15.
 */
public class W10nService implements WebServiceHandler {

    public static final String ID = "w10n";

    private String _serviceId;
    private String _base;
    private String _name;
    private String _threddsServiceType;



    public W10nService() {

        _serviceId = ID;
        _name = ID +" Service";
        _base = "/opendap/"+ID;
        _threddsServiceType = ID;
    }

    @Override
    public void init(HttpServlet servlet, Element config) {
        String base = servlet.getServletContext().getContextPath() + "/" +servlet.getServletName() + "/";
        setBase(base);
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getServiceId() {
        return _serviceId;
    }

    @Override
    public String getBase() {
        return _base;
    }

    public void setBase(String base) {
        _base = base;
    }

    @Override
    public boolean datasetCanBeViewed(String datasetId, Document ddx) {
        return true;
    }

    @Override
    public String getServiceLink(String datasetUrl) {

        PathBuilder pb = new PathBuilder();
        pb.append(_base).pathAppend(datasetUrl).append("/");
        return pb.toString();
    }



    @Override
    public String getThreddsServiceType() {
        return _threddsServiceType;
    }

    public String getThreddsUrlPath(String datasetUrl)  {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(datasetUrl).append("/");
        return pb.toString();
    }



}

