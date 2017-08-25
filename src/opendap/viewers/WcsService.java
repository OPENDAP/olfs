package opendap.viewers;

import opendap.PathBuilder;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;

import javax.servlet.http.HttpServlet;


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
/**
 * Created with IntelliJ IDEA.
 * User: ndp
 * Date: 6/4/14
 * Time: 7:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class WcsService implements WebServiceHandler {


    public static final String ID = "WCS-2.0";


    private String _serviceId;
    private String _base;
    private String _applicationName;
    private String _wcsDynamicServiceId;

    private Element _config;

    private String _wcsServiceUrl;

    public WcsService() {

        _serviceId = ID;
        _applicationName = ID + " Service";
        _wcsServiceUrl = "http://localhost:8080/"+ID+"/";
        _base = "/" + ID;
        _wcsDynamicServiceId = "lds";


    }


    @Override
    public void init(HttpServlet servlet, Element config) {


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


        e = _config.getChild("WcsService");

        if (e != null) {

            s = e.getAttributeValue("href");
            if (s != null && s.length() != 0)
                _wcsServiceUrl = s;

            s = e.getAttributeValue("base");
            if (s != null && s.length() != 0)
                _base = s;

            s = e.getAttributeValue("wcsDynamicServiceId");
            if (s != null && s.length() != 0)
                _wcsDynamicServiceId = s;
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
    public boolean datasetCanBeViewed(Document ddx) {
        //Element dataset = ddx.getRootElement();

        //Iterator i = dataset.getDescendants(new ElementFilter("Grid", DAP.DAPv32_NS));

        return true; // i.hasNext();
    }

    @Override
    public String getServiceLink(String datasetUrl) {
        PathBuilder pb = new PathBuilder();


        pb.append(_wcsServiceUrl).pathAppend(_wcsDynamicServiceId).pathAppend(datasetUrl).append("?SERVICE=WCS&VERSION=2.0.1&REQUEST=GetCapabilities");

        /*

        pb.append("<a href=\"").append(_wcsServiceUrl).pathAppend(_wcsDynamicServiceId);
        pb.pathAppend(datasetUrl).append("\">");
        pb.append(_applicationName).append("</a>");

        pb.append("<a href=\"").append(_wcsServiceUrl).pathAppend(_wcsDynamicServiceId);
        pb.pathAppend(datasetUrl).append("?SERVICE=WCS&VERSION=2.0.1&REQUEST=GetCapabilities\">");
        pb.append("GetCapabilities").append("</a>");

        pb.append("<a href=\"").append(_wcsServiceUrl).pathAppend(_wcsDynamicServiceId);
        pb.pathAppend(datasetUrl).append("?SERVICE=WCS&VERSION=2.0.1&REQUEST=DescribeCoverage");
        pb.append("&coverageId=").append(_wcsDynamicServiceId).pathAppend(datasetUrl).append("\">");
        pb.append("DescribeCoverage").append("</a>");

        */

//        pb.append("<a href=\"").append(_wcsServiceUrl).pathAppend(_wcsDynamicServiceId).pathAppend(datasetUrl).append("?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=2.0.1");
  //      pb.append("<a href=\"").append(_wcsServiceUrl).pathAppend(_wcsDynamicServiceId).pathAppend(datasetUrl).append("?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=2.0.1");


        return pb.toString();
    }


    public String getBase() {

        return _base;

    }


    public String getDynamicServiceId(){
        return _wcsDynamicServiceId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("\n");
        sb.append("    serviceId: ").append(_serviceId).append("\n");
        sb.append("    base: ").append(_base).append("\n");
        sb.append("    dynamicServiceId: ").append(_wcsDynamicServiceId).append("\n");
        sb.append("    applicationName: ").append(_applicationName).append("\n");
        sb.append("    WcsService: ").append(_wcsServiceUrl).append("\n");

        return sb.toString();
    }

    public String getThreddsServiceType() {
        return "WCS";
    }



    public String getThreddsUrlPath(String datasetUrl)  {
        PathBuilder pb = new PathBuilder();
        pb.pathAppend(_wcsDynamicServiceId).pathAppend(datasetUrl).append("?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=2.0.1");
        return pb.toString();
    }


}

