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
package opendap.threddsHandler;

import opendap.PathBuilder;
import opendap.namespaces.THREDDS;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;


/**
 * Created by ndp on 4/21/15.
 */
public class SimpleWebServiceHandler implements WebServiceHandler {


    private String _serviceId;
    private String _base;
    private String _name;
    private String _threddsServiceType;


    public SimpleWebServiceHandler(Element serviceElement) {


        _name = serviceElement.getAttributeValue(THREDDS.NAME);
        _serviceId = _name;
        _threddsServiceType = serviceElement.getAttributeValue(THREDDS.SERVICE_TYPE);
        _base = serviceElement.getAttributeValue(THREDDS.BASE);


    }


    public SimpleWebServiceHandler(String serviceId, String name, String base, String threddsServiceType) {

        _serviceId = serviceId;
        _name = name;
        _base = base;
        _threddsServiceType = threddsServiceType;
    }


    @Override
    public void init(HttpServlet servlet, Element config) {

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

    @Override
    public boolean datasetCanBeViewed(Document ddx) {
        return true;
    }

    @Override
    public String getServiceLink(String datasetUrl) {
        return _base + datasetUrl;
    }

    @Override
    public String getThreddsServiceType() {
        return _threddsServiceType;
    }


    public String getThreddsUrlPath(String datasetUrl)  {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(datasetUrl);
        return pb.toString();
    }


}
