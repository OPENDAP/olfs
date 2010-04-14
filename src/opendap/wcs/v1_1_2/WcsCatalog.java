/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2009 OPeNDAP, Inc.
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

import org.jdom.Element;

import java.util.List;

/**
 *
 * Defines an interface for WCS catalog. The catalog is expected to contain
 * a collection of wcs:CoverageDescription elements and should be able answer
 * questions about its inventory.
 *
 *
 *
 *
 *
 */
public interface WcsCatalog {

    /**
     * Initializes the catalog. The config element passed should be a WcsCatalog
     * Element (associated with no namespace) and will (typically) be retrived
     * from a configuration file at runtime. The contents of the WcsCatalog
     * element may be any valid XML. This cerates a mechanism that allows
     * implementations of this interface to ingest configuration information
     * specific to the implementation.
     *
     * In Hyrax this element is included in the Handler defintion for the
     * openda.wcs.v1_1_2.DispatchHandler.
     *
     * This method is intended to be calle donly once, and should protect it's
     * self from multiple calls.
     *
     *
     * @param config The WcsCatalog configuration element.
     * @param cacheDir The directory into which the catalog may choose to write persistent content,
     * intermediate files, etc.
     * @param resourcePath The path to the resource bundle delivered with the software.
     * @throws Exception When the bad things happen.
     */
    public void init(Element config, String cacheDir, String resourcePath) throws Exception;




    /**
     * Queries the catalog to determine if it contains information on a Coverage
     * with an ID (wcs:Identifier) equal to the passed String
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return true if the catalog contains the a wcs:Coverage wher the
     * value wcs:Identifer is equal to the passed string.
     */
    public boolean hasCoverage(String coverageId);




    /**
     * Queries the catalog for a CoverageDescription
     * with an ID (wcs:Identifier) equal to the passed String.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return A CoverageDescription onject whose
     *  wcs:Identifer value is equal to the passed string. Returns null if the
     * catalog does not contain a matching Coverage.
     * @throws WcsException When the bad things happen.
     */
    public CoverageDescription getCoverageDescription(String coverageId) throws WcsException;




    /**
     * Queries the catalog for a CoverageDescription
     * with an ID (wcs:Identifier) equal to the passed String.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return A CoverageDescription Element where the
     * value wcs:Identifer is equal to the passed string. Returns null if the
     * catalog does not contain a matching Coverage.
     * @throws WcsException When the bad things happen.
     */
    public Element getCoverageDescriptionElement(String coverageId) throws WcsException;




    /**
     * <p><b>
     * THIS METHOD MAY NOT BE NEEDED.
     * </b></p>
     *
     * Queries the catalog and returns all wcs:CoverageDescription elements
     * found therin.
     *
     * @return A List containing all of the wcs:CoverageDescription Elements found in
     * the catalog. The list may be empty.
     * @throws WcsException When the bad things happen.
     */
    public List<Element> getCoverageDescriptionElements() throws WcsException;





    /**
     * Queries the catalog for a wcs:CoverageOfferingBrief
     * with an ID (wcs:Identifier) equal to the passed String.
     *
     *
     * <p><b>
     * Since the wcs:CoverageOfferingBrief is derivable from the
     * conent of wcs:CoverageDecription it may be that we want to migrate this
     * method out of the interface.
     * </b></p>
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return A CoverageOfferingBrief Element where the
     * value wcs:Identifer is equal to the passed string. Returns null if the
     * catalog does not contain a matching Coverage.
     * @throws WcsException When the bad things happen.
     */
    public Element getCoverageSummaryElement(String coverageId) throws WcsException;




    /**
     * Queries the catalog and returns all wcs:CoverageOfferingBrief elements
     * found therin.
     *
     * <p><b>
     * Since the wcs:CoverageOfferingBrief is derivable from the
     * conent of wcs:CoverageDecription it may be that we want to migrate this
     * method out of the interface.
     * </b></p>
     *
     * @return A List containing all of the wcs:CoverageOfferingBrief Elements
     * found in the catalog. The list may be empty.
     * @throws WcsException When the bad things happen.
     */
    public List<Element> getCoverageSummaryElements() throws WcsException;




    /**
     * The queries the catalog to return a list of all of the unique
     * wcs:SupportedFormat Elements found (across all of the
     * wcs:CoverageDescriptions) in the catalog.
     *
     * <p><b>
     * This may be a server level configuration item. Given that we are working
     * with an OPeNDAP server (probablly Hyrax) it may be limited the list of
     * possible output formts provided by the server software (+ the native file
     * formats of the data?) In the OGC document
     * "OGC 07-067r5" Table 5 in section 8.3.3.1  says:
     * <br>
     * "This list of SupportedFormats shall be the union of all of the
     * supported formats in all of the nested CoverageSummaries."
     * <br>
     * So, if we thought that we could return each coverage in it's native
     * storage format then we would wannt a list of those. In reality if we
     * intend to subset the data, then the list of returned formats is probably
     * limited to what the server software can excrete and would not also
     * include fomarts which it can consume.
     *
     * </b></p>
     *
     *
     *
     * @return The list of unique wcs:SupportedFormat Elements in the catalog.
     */
    public List<Element> getSupportedFormatElements();




    /**
     *
     * The queries the catalog to return a list of all of the unique
     * wcs:SupportedCRS Elements found (across all of the
     * wcs:CoverageDescriptions) in the catalog.
     *
     * <p><b>
     * Is this a server level configuration item?
     * <br>
     * If the server does not support coordinate transformations, different
     * coverages MAY have different coordinate systems. In the OGC document
     * "OGC 07-067r5" Table 5 in section 8.3.3.1  says:
     * <br>
     * "This list of SupportedCRSs shall be the union of all of the
     * SupportedCRSs in all of the nested CoverageSummaries."
     * <br>
     * Given that each coverage has a native CRS, and given that the server
     * may not support CRS transformations it seems that the minimum list of
     * SupportedCRSs would be union of all of the native CRSs found in the
     * Coverage catalog.
     *
     * However, if coordinate transformation are supported then the
     * activity would become more complex, as the server would have to look at
     * each Coverage's native CRS and determine/compute a list of additional
     * CRSs that data in the native CRS can be transformed into using the
     * transformation software available to the server at runtime.
     *
     * With that in mind we may wish to rename this function to something like
     * getNativeCrsElements() ... or not.
     *
     * </b></p>
     *
     * @return The list of unique wcs:SupportedFormat Elements in the catalog.
     */
    public List<Element> getSupportedCrsElements();




    /**
     * The name of the DAP variable in the target dataset that is asscoiated
     * with the latitude coordinate variable.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return The name of the DAP variable in the target dataset that
     * represents the latitude coordinate. Returns null if information is not
     * known.
     */
    public String getLatitudeCoordinateDapId(String coverageId);




    /**
     * The name of the DAP variable in the target dataset that is asscoiated
     * with the longitude coordinate variable. Returns null if information is
     * not known.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return The name of the DAP variable in the target dataset that
     * represents the longitude coordinate.
     */
    public String getLongitudeCoordinateDapId(String coverageId);




    /**
     *
     * The name of the DAP variable in the target dataset that is asscoiated
     * with the elevation coordinate variable.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return The name of the DAP variable in the target dataset that
     * represents the elevation coordinate. Returns null if information is not
     * known.
     */
    public String getElevationCoordinateDapId(String coverageId);




    /**
     *
     * The name of the DAP variable in the target dataset that is asscoiated
     * with the time coordinate variable.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return The name of the DAP variable in the target dataset that
     * represents the time coordinate. Returns null if information is not
     * known.
     */
    public String getTimeCoordinateDapId(String coverageId);




    /**
     * This should provide the last modified time of the catalog. How this
     * should be computed needs to be defined.
     *
     * @return The last modfied date of the catalog.
     */
    public long getLastModified();


    /**
     * Always called when the service is shutting down so that the catalog can perform
     * sensible exiting activities.
     */
    public void destroy();



}
