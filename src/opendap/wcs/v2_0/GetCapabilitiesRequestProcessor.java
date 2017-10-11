/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Vector;

/**
 *
 * This class houses methods that return the wcs:Capabilities response for the service.
 *
 *
 */
public class GetCapabilitiesRequestProcessor {

    static Logger log = LoggerFactory.getLogger(GetCapabilitiesRequestProcessor.class);


    /**
     * Queries the WcsServiceManager for all of the components of the wcs:Capablitites response.
     * and returns the complete wcs:Capabilities document.
     *
     * @param serviceUrl The service URL of the WCS service. Used to build the service URLs that appear in the
     * OperationsMetadata section of the wcs:Capabilities response document.
     * @return The complete wcs:Capabilities document, suitable for serialization to a requesting client.
     * @throws WcsException When bad things happen.
     */
    public static Document getFullCapabilitiesDocument(String serviceUrl, String[] cids)  throws InterruptedException, WcsException {

        Element capabilities = new Element("Capabilities", WCS.WCS_NS);

        String updateSequence = getUpdateSequence();

        capabilities.setAttribute("updateSequence", updateSequence);


        capabilities.addContent(WcsServiceManager.getServiceIdentificationElement());
        capabilities.addContent(WcsServiceManager.getServiceProviderElement());
        capabilities.addContent(WcsServiceManager.getOperationsMetadataElement(serviceUrl));
        capabilities.addContent(ServerCapabilities.getServiceMetadata());

        //XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        //log.debug(xmlo.outputString(capabilities));

        capabilities.addContent(getContents(true,true,true,GetCapabilitiesRequest.DEFAULT_MAX_CONTENTS_SECTIONS_COUNT,cids));

        return new Document(capabilities);


    }


    /**
     * Evaluates the passed GetCapabilitiesRequest object and builds the appropriate wcs:Capabilities document.
     * by query the WcsServiceManager for all the requestd components of the wcs:Capablitites response.
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
        capabilities.addNamespaceDeclaration(WCS.WCSEO_NS);

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
                capabilities.addContent(WcsServiceManager.getServiceIdentificationElement());
            }

            if(all  ||  req.hasSection(GetCapabilitiesRequest.SERVICE_PROVIDER)){
                capabilities.addContent(WcsServiceManager.getServiceProviderElement());
            }

            if(all  ||  req.hasSection(GetCapabilitiesRequest.OPERATIONS_METADATA)){
                capabilities.addContent(WcsServiceManager.getOperationsMetadataElement(serviceUrl));
            }

            if(all  ||  req.hasSection(GetCapabilitiesRequest.SERVICE_METADATA)){
                capabilities.addContent(ServerCapabilities.getServiceMetadata());
            }

            if(all  ||
                    req.hasSection(GetCapabilitiesRequest.CONTENTS) ||
                    req.hasSection(GetCapabilitiesRequest.DATASET_SERIES_SUMMARY) ||
                    req.hasSection(GetCapabilitiesRequest.COVERAGE_SUMMARY)){

                capabilities.addContent(getContents(
                        all || req.hasSection(GetCapabilitiesRequest.CONTENTS),
                        req.hasSection(GetCapabilitiesRequest.DATASET_SERIES_SUMMARY),
                        req.hasSection(GetCapabilitiesRequest.COVERAGE_SUMMARY),
                        req.getCount(),req.getRequestedCoverageIds()));
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
        return "SomethingBetterWillBeUsedSoon";//WcsServiceManager.getLastModified()+"";
    }


    /**
     * Returns the wcs:Contents section of the wcs:Capabilities response.
     *
     * @return Returns the wcs:Contents section of the wcs:Capabilities response.
     * @throws WcsException   When bad things happen.
     * @throws InterruptedException
     */
    public static Element getContents(
            boolean allContent,
            boolean dataset_series_summary,
            boolean coverage_summary,
            long maxContentsSectionsCount,
            String[] coverageIds
    )  throws InterruptedException, WcsException {

        Vector<Element> extensionElements =  new Vector<>();
        long sectionCount = 0;

        Element contentsElement = new Element("Contents",WCS.WCS_NS);


        if(allContent  | coverage_summary){
            if(coverageIds!=null && coverageIds.length>0){

                log.info("getContents() Building contents from supplied list of coverageIds");
                for(String coverageId:coverageIds) {
                    WcsCatalog wcsCatalog = WcsServiceManager.getCatalog(coverageId);
                    Element coverageSummaryElement = wcsCatalog.getCoverageSummaryElement(coverageId);
                    log.debug("coverageId: {} coverageSummaryElement: {}",coverageId, coverageSummaryElement);
                    if(coverageSummaryElement!=null){
                        contentsElement.addContent(coverageSummaryElement);
                        sectionCount++;
                    }
                    if(sectionCount<maxContentsSectionsCount && (allContent | dataset_series_summary)) {
                        sectionCount =
                                getExtensionsElements(extensionElements, wcsCatalog,
                                        sectionCount,maxContentsSectionsCount);
                        contentsElement.addContent(extensionElements);

                    }

                    if( maxContentsSectionsCount < sectionCount)
                        break;
                }
            }
            else {
                log.info("getContents() Building contents from the default WcsCatalog");
                WcsCatalog defaultWcsCatalog = WcsServiceManager.getDefaultCatalog();
                Iterator i = defaultWcsCatalog.getCoverageSummaryElements().iterator();
                if (i.hasNext()) {
                    Element cs;
                    while (i.hasNext()) {
                        cs = (Element) i.next();
                        sectionCount++;
                        if (sectionCount < maxContentsSectionsCount)
                            contentsElement.addContent(cs);
                    }
                }
                if(sectionCount<maxContentsSectionsCount && (allContent | dataset_series_summary)) {
                    getExtensionsElements(extensionElements, defaultWcsCatalog,
                                    sectionCount,maxContentsSectionsCount);
                    contentsElement.addContent(extensionElements);
                }
            }
        }




        if(contentsElement.getChildren().isEmpty()){
            Element os;
            os = new Element("OtherSource",WCS.WCS_NS);
            XLink xlink = new XLink(XLink.Type.SIMPLE,"http://www.google.com",null,null,"No Coverages found. You could try Google...",null,null,null,null,null);
            os.setAttributes(xlink.getAttributes());
        }

        return contentsElement;
    }


    private static long getExtensionsElements(Vector<Element> extensionElements, WcsCatalog wcsCatalog, long sectionsCount, long maxContentsSectionsCount) throws WcsException, InterruptedException {

        Iterator i = wcsCatalog.getDatasetSeriesSummaryElements().iterator();
        if(i.hasNext()){
            Element wcsExtensionElement = new Element("Extension",WCS.WCS_NS);
            Element dss;
            while(i.hasNext()){
                dss = (Element) i.next();
                sectionsCount++;
                if(sectionsCount<maxContentsSectionsCount)
                    wcsExtensionElement.addContent(dss);
            }
            extensionElements.add(wcsExtensionElement);
        }
        return sectionsCount;

    }


}
