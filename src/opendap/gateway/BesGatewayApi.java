/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2010 OPeNDAP, Inc.
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
package opendap.gateway;

import opendap.bes.BesXmlAPI;
import opendap.bes.BadConfigurationException;
import opendap.coreServlet.ReqInfo;
import opendap.namespaces.BES;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This child class of opendap.bes.BesXmlAPI provides an implementation of the
 * getRequestDocument method that utilizes the BES wcs_gateway_module. The
 * intention was to Animal the methd from the parent class but that didn't
 * work out (can't Animal static methods? I don't know why) So the
 * implementation of the DAP dispatch methods had to be recreated.
 *
 * @see opendap.wcs.gatewayClient.WcsDispatchHandler
 *
 */
public class BesGatewayApi extends BesXmlAPI {


    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(BesGatewayApi.class);
    }


    /**
     * This child class of opendap.bes.BesXmlAPI provides an implementation of the
     * getRequestDocument method that utilizes the BES wcs_gateway_module.
     * @param type The type of thing being requested. For example a DDX would be
     * opendap.bes.BesXmlAPI.DDX
     * @param remoteDataSourceUrl See opendap.bes.BesXmlAPI.DDX
     * @param ce See opendap.bes.BesXmlAPI
     * @param xdap_accept See opendap.bes.BesXmlAPI
     * @param xmlBase See opendap.bes.BesXmlAPI
     * @param formURL See opendap.bes.BesXmlAPI
     * @param returnAs See opendap.bes.BesXmlAPI
     * @param errorContext See opendap.bes.BesXmlAPI
     * @return The request Document
     * @throws BadConfigurationException When the bad things happen.
     * @see opendap.bes.BesXmlAPI
     */
   // @Override
    public static  Document getRequestDocument(String type,
                                                String remoteDataSourceUrl,
                                                String ce,
                                                String xdap_accept,
                                                String xmlBase,
                                                String formURL,
                                                String returnAs,
                                                String errorContext)
                throws BadConfigurationException {


        log.debug("Building request for BES gateway_module request. remoteDataSourceUrl: "+ remoteDataSourceUrl);
        Element e, request = new Element("request", BES.BES_NS);

        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":gateway_request]";
        request.setAttribute("reqID",reqID);


        if(xdap_accept!=null)
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT,xdap_accept));
        else
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));

        request.addContent(setContainerElement("gatewayContainer","gateway",remoteDataSourceUrl,type));

        Element def = defineElement("d1","default");
        e = (containerElement("gatewayContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs);

        request.addContent(e);

        log.debug("Built request for BES gateway_module.");


        return new Document(request);

    }

    public static String getDataSourceUrl(HttpServletRequest req, String pathPrefix) throws MalformedURLException {


        String relativeURL = ReqInfo.getRelativeUrl(req);
        String requestSuffix = ReqInfo.getRequestSuffix(req);

        if(relativeURL.startsWith("/"))
            relativeURL = relativeURL.substring(1,relativeURL.length());



        String dataSourceUrl = relativeURL;

        if(pathPrefix!=null && dataSourceUrl.startsWith(pathPrefix))
            dataSourceUrl = dataSourceUrl.substring(pathPrefix.length());

        dataSourceUrl = dataSourceUrl.substring(0,dataSourceUrl.lastIndexOf("."+requestSuffix));



        dataSourceUrl = HexAsciiEncoder.hexToString(dataSourceUrl);

//        URL url = new URL(dataSourceUrl);
        //log.debug(urlInfo(url));

        return dataSourceUrl;


    }
}
