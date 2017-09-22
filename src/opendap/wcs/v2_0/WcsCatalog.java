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

import org.apache.http.client.CredentialsProvider;
import org.jdom.Element;

import java.util.Collection;

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
     * Element (associated with no namespace) and will (typically) be retrieved
     * from a configuration file at runtime. The contents of the WcsCatalog
     * element may be any valid XML. This creates a mechanism that allows
     * implementations of this interface to ingest configuration information
     * specific to the implementation.
     *     *
     * This method is intended to be called only once, and should protect it's
     * self from multiple calls.
     *
     *
     * @param config A URL the when de-referenced will return a document that contains
     * a WcsCatalog configuration element as a child of the root element.
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
    public boolean hasCoverage(String coverageId) throws InterruptedException;




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
    public CoverageDescription getCoverageDescription(String coverageId) throws InterruptedException, WcsException;




    /**
     * Queries the catalog for a CoverageDescription
     * with an ID (wcs:Identifier) equal to the passed String.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return A CoverageDescription Element where the
     * value wcs:Identifer is equal to the passed string. Returns null if the
     * catalog does not contain a matching Coverage.
     * @throws WcsException When the bad things happen.
     * @throws InterruptedException
     */
    public Element getCoverageDescriptionElement(String coverageId) throws InterruptedException,  WcsException;




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
     * @throws InterruptedException
     */
    //public List<Element> getCoverageDescriptionElements() throws InterruptedException, WcsException;





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
     * @throws InterruptedException
     */
     public Element getCoverageSummaryElement(String coverageId) throws InterruptedException, WcsException;




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
     * @throws InterruptedException
     */
     public Collection<Element> getCoverageSummaryElements() throws InterruptedException, WcsException;








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
     * @throws InterruptedException
     */
    public Collection<Element> getDatasetSeriesSummaryElements() throws InterruptedException, WcsException;






    /**
     * The name of the DAP variable in the target dataset that is asscoiated
     * with the latitude coordinate variable.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return The name of the DAP variable in the target dataset that
     * represents the latitude coordinate. Returns null if information is not
     * known.
     */
    //public String getLatitudeCoordinateDapId(String coverageId, String fieldId);




    /**
     * The name of the DAP variable in the target dataset that is asscoiated
     * with the longitude coordinate variable. Returns null if information is
     * not known.
     *
     * @param coverageId The Coverage ID (wcs:Identifier)
     * @return The name of the DAP variable in the target dataset that
     * represents the longitude coordinate.
     */
    //public String getLongitudeCoordinateDapId(String coverageId, String fieldId);




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
    //public String getElevationCoordinateDapId(String coverageId, String fieldId);




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
    //public String getTimeCoordinateDapId(String coverageId, String fieldId);


    /**
     * Returns the base data access URL for this coverage. Null otherwise.
     * @param coverageID
     * @return  The base data access URL for this coverage. Null otherwise.
     * @throws InterruptedException
     */
    public String getDataAccessUrl(String coverageID) throws InterruptedException;



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



    public void update() throws Exception;



    public  EOCoverageDescription getEOCoverageDescription(String id) throws WcsException;


    public   EODatasetSeries getEODatasetSeries(String id) throws WcsException;

    public boolean hasEoCoverage(String id);

    // public CredentialsProvider getCredentials();

    public boolean matches(String coverageId);


}
