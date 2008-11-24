/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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
package opendap.wcs.gatewayClient;

import opendap.bes.BesXmlAPI;
import opendap.bes.BadConfigurationException;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;

/**
 * User: ndp
 * Date: Nov 24, 2008
 * Time: 10:42:40 AM
 */
public class BesAPI extends BesXmlAPI {


    private static Logger log;
    static {
        log = org.slf4j.LoggerFactory.getLogger(BesAPI.class);
    }


    public static  Document getRequestDocument(String type,
                                                String wcsRequestURL,
                                                String ce,
                                                String xdap_accept,
                                                String xmlBase,
                                                String formURL,
                                                String returnAs,
                                                String errorContext)
                throws BadConfigurationException {


        log.debug("Building request for BES wcs_gateway_module request. wcsRequestURL: "+wcsRequestURL);
        Element e, request = new Element("request", BES_NS);

        String reqID = "["+Thread.currentThread().getName()+":"+
                Thread.currentThread().getId()+":wcs_request]";
        request.setAttribute("reqID",reqID);


        if(xdap_accept!=null)
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT,xdap_accept));
        else
            request.addContent(setContextElement(XDAP_ACCEPT_CONTEXT, DEFAULT_XDAP_ACCEPT));

        request.addContent(setContextElement(EXPICIT_CONTAINERS_CONTEXT,"no"));

        request.addContent(setContextElement(ERRORS_CONTEXT,errorContext));

        if(xmlBase!=null)
            request.addContent(setContextElement(XMLBASE_CONTEXT,xmlBase));


        request.addContent(setContainerElement("wcsContainer","wcsg","\""+wcsRequestURL+"\"",type));

        Element def = defineElement("d1","default");
        e = (containerElement("wcsContainer"));

        if(ce!=null && !ce.equals(""))
            e.addContent(constraintElement(ce));

        def.addContent(e);

        request.addContent(def);

        e = getElement(type,"d1",formURL,returnAs);

        request.addContent(e);

        log.debug("Built request for BES wcs_gateway_module request.");


        return new Document(request);

    }







}
