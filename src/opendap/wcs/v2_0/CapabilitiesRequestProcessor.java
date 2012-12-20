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
package opendap.wcs.v2_0;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 *
 * This class houses methods that return the wcs:Capabilities response for the service.
 *
 *
 */
public class CapabilitiesRequestProcessor {

    static Logger log = LoggerFactory.getLogger(CapabilitiesRequestProcessor.class);


    /**
     * Queries the CatalogWrapper for all of the components of the wcs:Capablitites response.
     * and returns the complete wcs:Capabilities document.
     *
     * @param serviceUrl The service URL of the WCS service. Used to build the service URLs that appear in the
     * OperationsMetadata section of the wcs:Capabilities response document.
     * @return The complete wcs:Capabilities document, suitable for serialization to a requesting client.
     * @throws WcsException When bad things happen.
     */
    public static Document getFullCapabilitiesDocument(String serviceUrl)  throws InterruptedException, WcsException {

        Element capabilities = new Element("Capabilities", WCS.WCS_NS);

        String updateSequence = getUpdateSequence();

        capabilities.setAttribute("updateSequence", updateSequence);


        capabilities.addContent(CatalogWrapper.getServiceIdentificationElement());
        capabilities.addContent(CatalogWrapper.getServiceProviderElement());
        capabilities.addContent(CatalogWrapper.getOperationsMetadataElement(serviceUrl));
        capabilities.addContent(getServiceMetadata());

        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        //log.debug(xmlo.outputString(capabilities));

        capabilities.addContent(getContents());

        return new Document(capabilities);


    }


    /**
     * Evaluates the passed GetCapabilitiesRequest object and builds the appropriate wcs:Capabilities document.
     * by query the CatalogWrapper for all the requestd components of the wcs:Capablitites response.
     *
     *
     *
     * @param req The client service request as a GetCapabilitiesRequest object
     * @param serviceUrl The service URL of the WCS service. Used to build the service URLs that appear in the
     * OperationsMetadata section of the wcs:Capabilities response document.
     * @return The wcs:Capabilities document with the componets requested by the client.
     * @throws WcsException  When bad things happen.
     */
    public static Document processGetCapabilitiesRequest(GetCapabilitiesRequest req, String serviceUrl)  throws InterruptedException, WcsException {


        Element capabilities = new Element("Capabilities",WCS.WCS_NS);

        capabilities.addNamespaceDeclaration(WCS.WCS_NS);
        capabilities.addNamespaceDeclaration(WCS.OWS_NS);
        capabilities.addNamespaceDeclaration(WCS.XSI_NS);

        StringBuilder schemaLocation = new StringBuilder();

        schemaLocation
                .append(WCS.OWS_NAMESPACE_STRING).append("  ")
                .append(WCS.OWS_SCHEMA_LOCATION_BASE).append("owsAll.xsd").append(" ")
                .append(WCS.WCS_NAMESPACE_STRING).append("  ")
                .append(WCS.WCS_SCHEMA_LOCATION_BASE).append("wcsAll.xsd")

        ;
        capabilities.setAttribute("schemaLocation",schemaLocation.toString(),WCS.XSI_NS);

        String updateSequence = getUpdateSequence();

        capabilities.setAttribute("updateSequence",updateSequence);

        capabilities.setAttribute("version",WCS.CURRENT_VERSION);

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

            if(all  ||  req.hasSection(GetCapabilitiesRequest.SERVICE_METADATA)){
                capabilities.addContent(getServiceMetadata());
            }

            if(all  ||  req.hasSection(GetCapabilitiesRequest.CONTENTS)){
                capabilities.addContent(getContents());
            }




            return new Document(capabilities);



    }


    /**
     * Returns the wcs:UpdateSequence for the catalog. This is currently taken to be the String representation of the
     * catalogs last modified time in seconds since 1/1/1970
     * @return Returns the wcs:UpdateSequence for the catalog. This is currently taken to be the String representation of the
     * catalogs last modified time in seconds since 1/1/1970
     */
    public static String getUpdateSequence(){

        return CatalogWrapper.getLastModified()+"";

    }


    /**
     * Returns the wcs:Contents section of the wcs:Capabilities response.
     *
     * @return Returns the wcs:Contents section of the wcs:Capabilities response.
     * @throws WcsException   When bad things happen.
     * @throws InterruptedException
     */
    public static Element getContents()  throws InterruptedException, WcsException {

        Element contents = new Element("Contents",WCS.WCS_NS);
        Element cs;
        Iterator i = CatalogWrapper.getCoverageSummaryElements().iterator();

        if(i.hasNext()){

            while(i.hasNext()){
                cs = (Element) i.next();
                contents.addContent(cs);
            }

        }else {
            cs = new Element("OtherSource",WCS.WCS_NS);
            XLink xlink = new XLink(XLink.Type.SIMPLE,"http://www.google.com",null,null,"No Coverages found. You could try Google...",null,null,null,null,null);
            cs.setAttributes(xlink.getAttributes());
        }

        return contents;


    }



    /**
     * Returns the wcs:Contents section of the wcs:Capabilities response.
     *
     * @return Returns the wcs:Contents section of the wcs:Capabilities response.
     * @throws WcsException   When bad things happen.
     * @throws InterruptedException
     */
    public static Element getServiceMetadata()  throws InterruptedException, WcsException {

        Element serviceMetadata = new Element("ServiceMetadata",WCS.WCS_NS);

        Element formatSupported = new Element("formatSupported",WCS.WCS_NS);
        formatSupported.setText("application/x-netcdf-cf1.0");
        serviceMetadata.addContent(formatSupported);

        formatSupported = new Element("formatSupported",WCS.WCS_NS);
        formatSupported.setText("image/geotiff");
        serviceMetadata.addContent(formatSupported);

        formatSupported = new Element("formatSupported",WCS.WCS_NS);
        formatSupported.setText("application/octet-stream");
        serviceMetadata.addContent(formatSupported);



        return serviceMetadata;


    }



}
