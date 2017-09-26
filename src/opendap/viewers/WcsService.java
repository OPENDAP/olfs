package opendap.viewers;

import opendap.PathBuilder;
import opendap.services.WebServiceHandler;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


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

    Logger _log;
    private String _serviceId;
    private String _applicationName;

    public static final String ID = "WCS";

    private String _serviceEndpoint;
    private String _dynamicServiceId;
    private String  _pathMatchRegexString;
    private Pattern _pathMatchPattern;


    private Element _config;

    public WcsService() {
        _log = LoggerFactory.getLogger(this.getClass());
        _serviceId = ID;
        _applicationName = ID + " Service";
    }


    @Override
    public void init(HttpServlet servlet, Element config) throws ServletException {

        _config = config;

        Element e;
        String s;

        Vector<String> configErrors = new Vector<>();

        s = _config.getAttributeValue("serviceId");
        if (s != null && s.length() != 0) {
            _serviceId = s;
        }
        else {
            configErrors.add("Failed to locate required attribute 'serviceId'");
        }

        // <ApplicationName>Testbed-12 WCS Service</applicationName>
        e = _config.getChild("ApplicationName");
        if (e != null) {
            s = e.getTextTrim();
            if (s != null && s.length() != 0) {
                _applicationName = s;
            }
            else {
            }
        }
        else {
            _applicationName = "WCS";
            _log.warn("Using Default Application Name for WCS service! name: {}",_applicationName);
        }

        // <ServiceEndpoint>http://localhost:8080/WCS-2.0/</ServiceEndpoint>
        e = _config.getChild("ServiceEndpoint");
        if (e != null) {
            s = e.getTextTrim();
            if (s != null && s.length() != 0) {
                if(s.startsWith("http://") || s.startsWith("https://") ) {
                    try {
                        URL url = new URL(s);
                        _serviceEndpoint = url.toString();
                    } catch (MalformedURLException e1) {
                        configErrors.add("The value of 'ServiceEndpoint' element could not be parsed as a URL. msg: "+e1.getMessage());
                    }
                }
                else
                    _serviceEndpoint = s;
            }
            else {
                configErrors.add("The 'ServiceEndpoint' element did not contain any text.");
            }
        }
        else {
            configErrors.add("Failed to locate required element 'ServiceEndpoint'");
        }


        // <MatchRegex>testbed-12/.*</MatchRegex>
        e = _config.getChild("MatchRegex");
        if (e != null) {
            s = e.getTextTrim();
            if (s != null && s.length() != 0) {
                try {
                    _pathMatchPattern = Pattern.compile(s);
                    _pathMatchRegexString = s;
                }
                catch(PatternSyntaxException pse){
                    configErrors.add("The value of the 'MatchRegex' failed to compile as a regular expression. msg: "+pse.getMessage());
                }
            }
            else {
                configErrors.add("The 'MatchRegex' element did not contain any text.");
            }
        }
        else {
            configErrors.add("Failed to locate required element 'MatchRegex'");
        }


        // <DynamicServiceId>tb12</DynamicServiceId>
        e = _config.getChild("DynamicServiceId");
        if (e != null) {
            s = e.getTextTrim();
            if (s != null && s.length() != 0) {
                _dynamicServiceId = s;
            }
            else {
                configErrors.add("The 'DynamicServiceId' element did not contain any text.");
            }
        }
        else {
            configErrors.add("Failed to locate required element 'DynamicServiceId'");
        }

        if(!configErrors.isEmpty()){
            StringBuilder sb = new StringBuilder("WCS Service Configuration ERRORS!");
            _log.error("init() - {}",sb.toString());
            sb.append("\n");
            for(String errMsg:configErrors){
                sb.append(errMsg).append("\n");
                _log.error("init() - {}",errMsg);
            }
            throw new ServletException(sb.toString());
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

        return _pathMatchPattern.matcher(datasetId).matches();

        //Element dataset = ddx.getRootElement();

        //Iterator i = dataset.getDescendants(new ElementFilter("Grid", DAP.DAPv32_NS));

        //return true; // i.hasNext();
    }

    @Override
    public String getServiceLink(String datasetId) {
        PathBuilder pb = new PathBuilder(_serviceEndpoint);
        pb.pathAppend(_dynamicServiceId).pathAppend(datasetId);
        //pb.append("?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=2.0.1");
        return pb.toString();
    }

    public String getThreddsUrlPath(String datasetId) {
        return getServiceLink(datasetId);
    }

    public String getBase() {
        return _serviceEndpoint;
    }


    public String getDynamicServiceId() {
        return _dynamicServiceId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append("\n");
        sb.append("    serviceId: ").append(_serviceId).append("\n");
        sb.append("    ApplicationName: ").append(_applicationName).append("\n");
        sb.append("    WcsService: ").append(_serviceEndpoint).append("\n");
        sb.append("    DynamicServiceId: ").append(_dynamicServiceId).append("\n");
        sb.append("    MatchRegex: ").append(_pathMatchRegexString).append("\n");
        return sb.toString();
    }

    public String getThreddsServiceType() {
        return "WCS";
    }

    public String getMatchRegexString(){
        return _pathMatchRegexString;
    }

}

