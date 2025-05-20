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
package opendap.viewers;

import opendap.PathBuilder;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 6/4/14
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class NcWmsService implements WebServiceHandler {


    public static final String ID = "ncWms";


    private String _serviceId;
    private String _base;
    private String _applicationName;
    private String _ncWmsDynamicServiceId;

    private Element _config;

    private String _ncWmsServiceUrl;

    public NcWmsService() {

        _serviceId = ID;
        _applicationName = ID + " Service";
        _ncWmsServiceUrl = "http://localhost:8080/"+ID+"/wms";
        _base = "/" + ID + "/wms";
        _ncWmsDynamicServiceId = "lds";


    }


    @Override
    public void init(HttpServlet servlet, Element config) throws ServletException {


        _config = config;

        Element e;
        String s;

        s = _config.getAttributeValue("serviceId");
        if (s != null && s.length() != 0)
            _serviceId = s;

        e = _config.getChild("applicationName");
        if (e != null) {
            s = e.getTextTrim();
            if (s != null && s.length() != 0)
                _applicationName = s;
        }


        e = _config.getChild("NcWmsService");

        if (e != null) {

            s = e.getAttributeValue("href");
            if (s != null && s.length() != 0)
                _ncWmsServiceUrl = s;

            s = e.getAttributeValue("base");
            if (s != null && s.length() != 0)
                _base = s;

            s = e.getAttributeValue("ncWmsDynamicServiceId");
            if (s != null && s.length() != 0)
                _ncWmsDynamicServiceId = s;
        }


    }

    @Override
    public String getName() {
        return _applicationName;
    }

    @Override
    public String getServiceId() {
        return _serviceId;
    }


    @Override
    public boolean datasetCanBeViewed(String datasetId, Document ddx) {
        //Element dataset = ddx.getRootElement();

        //Iterator i = dataset.getDescendants(new ElementFilter("Grid", DAP.DAPv32_NS));

        return true; // i.hasNext();
    }

    @Override
    public String getServiceLink(String datasetUrl) {

        PathBuilder pb = new PathBuilder();
        pb.append(_ncWmsServiceUrl).pathAppend(_ncWmsDynamicServiceId).pathAppend(datasetUrl).append("?SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0");
        return pb.toString();


       // return _ncWmsServiceUrl + "/" + _ncWmsDynamicServiceId + datasetUrl + "?SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0";

    }


    public String getBase() {

        return _base;

    }


    public String getDynamicServiceId(){
        return _ncWmsDynamicServiceId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("\n");
        sb.append("    serviceId: ").append(_serviceId).append("\n");
        sb.append("    base: ").append(_base).append("\n");
        sb.append("    dynamicServiceId: ").append(_ncWmsDynamicServiceId).append("\n");
        sb.append("    applicationName: ").append(_applicationName).append("\n");
        sb.append("    WmsService: ").append(_ncWmsServiceUrl).append("\n");

        return sb.toString();
    }

    public String getThreddsServiceType() {
        return "WMS";
    }



    public String getThreddsUrlPath(String datasetUrl)  {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(_ncWmsDynamicServiceId).pathAppend(datasetUrl).append("?SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0");
        return pb.toString();
    }


}
