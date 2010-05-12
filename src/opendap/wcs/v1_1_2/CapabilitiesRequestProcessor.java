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
package opendap.wcs.v1_1_2;

import org.jdom.Document;
import org.jdom.Element;

import java.util.Iterator;

import opendap.coreServlet.Scrub;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Jan 12, 2010
 * Time: 2:02:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class CapabilitiesRequestProcessor {



    public static Document getFullCapabilitiesDocument(String serviceUrl) throws WcsException {

        Element capabilities = new Element("Capabilities", WCS.WCS_NS);

        String updateSequence = getUpdateSequence();

        capabilities.setAttribute("updateSequence", updateSequence);


        capabilities.addContent(CatalogWrapper.getServiceIdentificationElement());
        capabilities.addContent(CatalogWrapper.getServiceProviderElement());
        capabilities.addContent(CatalogWrapper.getOperationsMetadataElement(serviceUrl));
        capabilities.addContent(getContents());

        return new Document(capabilities);


    }


    public static Document processGetCapabilitiesRequest(GetCapabilitiesRequest req, String serviceUrl) throws WcsException {

        Element capabilities = new Element("Capabilities",WCS.WCS_NS);

        String updateSequence = getUpdateSequence();

        capabilities.setAttribute("updateSequence",updateSequence);

        Element section;


            boolean all = false;


            if(!req.hasSectionsElement())
                all = true;

            // Now the spec (OGC 06-121r3, section 7.3.3) says:  "If no names are listed, the service
            // metadata returned may not contain any of the sections that could be listed."
            //
            // If thats's the case then it would appear we are to return an empty document.
            // Basically they are just going to get the updateSequence attribute value.

            if(req.hasSection(GetCapabilitiesRequest.ALL))
                all = true;



            if(all  ||  req.hasSection(GetCapabilitiesRequest.SERVICE_IDENTIFICATION)){
                capabilities.addContent(CatalogWrapper.getServiceIdentificationElement());
            }

            if(all  ||  req.hasSection(GetCapabilitiesRequest.SERVICE_PROVIDER)){
                capabilities.addContent(CatalogWrapper.getServiceProviderElement());
            }

            if(all  ||  req.hasSection(GetCapabilitiesRequest.OPERATIONS_METADATA)){
                capabilities.addContent(CatalogWrapper.getOperationsMetadataElement(serviceUrl));
            }

            if(all  ||  req.hasSection(GetCapabilitiesRequest.CONTENTS)){
                capabilities.addContent(getContents());
            }

            return new Document(capabilities);



    }


    public static String getUpdateSequence(){

        return CatalogWrapper.getLastModified()+"";

    }


    public static Element getContents() throws WcsException {

        Element contents = new Element("Contents",WCS.WCS_NS);
        Element cs;
        Iterator i = CatalogWrapper.getCoverageSummaryElements().iterator();

        if(i.hasNext()){

            while(i.hasNext()){
                cs = (Element) i.next();
                contents.addContent(cs);
            }

            i = CatalogWrapper.getSupportedCrsElements().iterator();
            while(i.hasNext()){
                cs = (Element) i.next();
                contents.addContent(cs);
            }
            i = CatalogWrapper.getSupportedFormatElements().iterator();
            while(i.hasNext()){
                cs = (Element) i.next();
                contents.addContent(cs);
            }

        }else {
            cs = new Element("OnlineResource",WCS.WCS_NS);
            XLink xlink = new XLink(XLink.Type.SIMPLE,"http://www.google.com",null,null,null,null,null,null,null,null);
            cs.setAttributes(xlink.getAttributes());
        }

        return contents;


    }



}
