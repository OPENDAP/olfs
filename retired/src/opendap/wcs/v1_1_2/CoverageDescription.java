/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2013 OPeNDAP, Inc.
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
package opendap.wcs.v1_1_2;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An implementation of a wcs:CoverageDescription object. This implementation includes methods that assist in the
 * creation of DAP constraint expressions to retrieve coverage data as NetCDF.
 *
 */
public class CoverageDescription {

    private Element myCD;

    private Logger log;


    private long lastModified;

    private File myFile;

    private HashMap<String,String> _dapGridId;
    private HashMap<String,String> _latitudeCoordinateDapId;
    private HashMap<String,String> _longitudeCoordinateDapId;
    private HashMap<String,String> _elevationCoordinateDapId;
    private HashMap<String,String> _timeCoordinateDapId;
    private HashMap<String,String> _timeUnits;


    /**
     * Builds the CoverageDescription object from a wcs:CoverageDescription element.
     * @param cd A wcs:CoverageDescription element
     * @param lastModified The last modified time of coverage.
     * @throws WcsException  When bad things happen.
     */
    public CoverageDescription(Element cd, long lastModified) throws WcsException{
        log = org.slf4j.LoggerFactory.getLogger(getClass());

        _dapGridId = new HashMap<String,String>();
        _latitudeCoordinateDapId = new HashMap<String,String>();
        _longitudeCoordinateDapId = new HashMap<String,String>();
        _elevationCoordinateDapId = new HashMap<String,String>();
        _timeCoordinateDapId = new HashMap<String,String>();
        _timeUnits = new HashMap<String,String>();


        WCS.checkCoverageDescription(cd);
        myCD = cd;

        myFile = null;
        this.lastModified = lastModified;
    }

    /**
     * Builds the CoverageDescription object from a wcs:CoverageDescription element that is stored as the root
     * element in a local File.
     * @param cdFile The file containing the wcs:CoverageDescription element.
     * @throws IOException When the file cannot be read.
     * @throws JDOMException When the file cannot be parsed.
     * @throws WcsException When the file contains an incorrect wcs:CoverageDescription
     */
    public CoverageDescription(File cdFile) throws IOException, JDOMException, WcsException {
        log = org.slf4j.LoggerFactory.getLogger(getClass());
        myCD = ingestCoverageDescription(cdFile);
        myFile = cdFile;
        lastModified = cdFile.lastModified();
    }

    private Element ingestCoverageDescription(File cdFile) throws IOException, JDOMException, WcsException{
        String msg;
        if(!cdFile.canRead()){
            msg = "Cannot read file: "+ cdFile.getName();
            log.error(msg);
            throw new IOException(msg);
        }
        if(!cdFile.isFile()){
            msg = "CoverageDescription file '"+ cdFile.getName() +"' is not a regular file.";
            log.error(msg);
            throw new IOException(msg);
        }

        SAXBuilder sb = new SAXBuilder();
        Document cdDoc = sb.build(cdFile);

        Element cd = cdDoc.getRootElement();


        WCS.checkCoverageDescription(cd);

        return cd;

    }

    /**
     * Returns the last modified time of the Coverage.
     * @return Returns the last modified time of the Coverage.
     *
     */
    public long lastModified() {

        try {
            if (myFile != null && lastModified < myFile.lastModified()) {
                myCD = ingestCoverageDescription(myFile);
                lastModified = myFile.lastModified();
            }
        }
        catch (Exception e) {

            String msg ="Failed to update CoverageDescription from file ";

            if(myFile != null){
                msg += myFile.getAbsoluteFile();
            }
            log.error(msg);
        }

        return lastModified;

    }


    /**
     * Checks the Coverage to see if it's range contains a particular field.
     * @param fieldID The value of the wcs:Identifier for the wcs:Field in question.
     * @return True if the Field is present, false otherwise.
     */
    public boolean hasField(String fieldID){
        Element range =  myCD.getChild("Range",WCS.WCS_NS);

        boolean foundIt = false;
        Element field;
        Element id;
        Iterator i = range.getChildren("Field",WCS.WCS_NS).iterator();

        while(i.hasNext()){
            field = (Element)i.next();
            id = field.getChild("Identifier",WCS.WCS_NS);
            if(id!=null && fieldID.equals(id.getTextTrim()))
                foundIt = true;

        }

        return foundIt;
    }


    /**
     *
     * @return Returns the ows:Titles (if any) associated with this CoverageDescription.
     */
    public List getTitles(){
        return cloneElementList(myCD.getChildren("Title",WCS.OWS_NS));
    }

    /**
     *
     * @return Returns the ows:Abstracts (if any) associated with this CoverageDescription.
     */
    public List getAbstracts(){
        return cloneElementList(myCD.getChildren("Abstract",WCS.OWS_NS));
    }

    /**
     *
     * @return Returns the ows:KeyWords (if any) associated with this CoverageDescription.
     */
    public List getKeywords(){
        return cloneElementList(myCD.getChildren("KeyWords",WCS.OWS_NS));
    }


    /**
     *
     * @return Returns the value of the unique wcs:Identifier associated with this CoverageDescription.
     */
    public String getIdentifier(){
        Element wcsIdentifier =  myCD.getChild("Identifier",WCS.WCS_NS);
        return wcsIdentifier.getText();
    }

    /**
     *
     * @return Returns the unique wcs:Identifier associated with this CoverageDescription.
     */
    public Element getIdentifierElement(){
        return (Element) myCD.getChild("Identifier",WCS.WCS_NS).clone();
    }

    /**
     *
     * @return Returns the BoundingBox  associated with the SpatialDomain of this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public BoundingBox getBoundingBox() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        Element boundingBox =  spatialDomain.getChild("BoundingBox",WCS.OWS_NS);
        return new BoundingBox(boundingBox);
    }

    /**
     *
     * @return Returns the ows:BoundingBox element associated with the SpatialDomain of this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public Element getBoundingBoxElement() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("BoundingBox",WCS.OWS_NS).clone();
    }


    /**
     *
     * @return Returns the GridCRS object associated with the SpatialDomain of this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public GridCRS getGridCRS() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        Element gridCRS =  spatialDomain.getChild("GridCRS",WCS.WCS_NS);
        if(gridCRS==null)
            return null;
        return new GridCRS(gridCRS);
    }


    /**
     *
     * @return Returns the GridCRS element associated with the SpatialDomain of this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public Element getGridCRSElement() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("GridCRS",WCS.WCS_NS).clone();
    }

    /**
     * @return Returns the gml:_CoordinateOperation element associated with the SpatialDomain of this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public Element get_CoordinateOperationElement(){
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("_CoordinateOperation",WCS.GML_NS).clone();
    }

    /**
     *
     * @return Returns the wcs:ImageCRS element associated with the SpatialDomain of this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public Element getImageCRSElement() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("ImageCRS",WCS.WCS_NS).clone();
    }


    /**
     * @return Returns the wcs:Polygon elements associated with the SpatialDomain of this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public List getPolygonElements() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  cloneElementList(spatialDomain.getChildren("Polygon",WCS.GML_NS));
    }

    /**
     * @return Returns the wcs:TemporalDomain element associated with the Domain of this CoverageDescription.
     */
    public Element getTemporalDomainElement(){
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        return  (Element) domain.getChild("TemporalDomain",WCS.WCS_NS).clone();
    }

    /**
     * @return Returns the wcs:Range element associated with this CoverageDescription.
     */
    public Element getRangeElement(){
        return  (Element) myCD.getChild("Range",WCS.WCS_NS).clone();
    }

    /**
     *
     * @return Returns the wcs:SupportedCRS elements associated with this CoverageDescription.
     */
    public List getSupportedCrsElements(){
        return  cloneElementList(myCD.getChildren("SupportedCRS",WCS.WCS_NS));
    }

    /**
     *
     * @return Returns the wcs:SupportedFormat elements associated with this CoverageDescription.
     */
    public List<Element> getSupportedFormatElements(){

        return  cloneElementList(myCD.getChildren("SupportedFormat",WCS.WCS_NS));
    }

    private List<Element> cloneElementList(List list){
        ArrayList<Element> newList = new ArrayList<Element>();

        Iterator i = list.iterator();
        Element e;

        while(i.hasNext()){
            e = (Element)i.next();
            newList.add((Element)e.clone());
        }

        return newList;
    }


    /**
     *
     * @return Returns the wcs:CoverageSummary element that represents this CoverageDescription.
     * @throws WcsException When bad things happen.
     */
    public Element getCoverageSummary() throws WcsException {

        Element e;
        Element cob = new Element("CoverageSummary",WCS.WCS_NS);

        // ows:Description type

        Iterator i = getTitles().iterator();
        while(i.hasNext()){
            e = (Element)i.next();
            cob.addContent((Element)e.clone());
        }

        i = getAbstracts().iterator();
        while(i.hasNext()){
            e = (Element)i.next();
            cob.addContent((Element)e.clone());
        }

        i = getTitles().iterator();
        while(i.hasNext()){
            e = (Element)i.next();
            cob.addContent((Element)e.clone());
        }

        // wcs:CoverageDescriptionType
        cob.addContent(getBoundingBox().getWgs84BoundingBoxElement());

        i = getSupportedCrsElements().iterator();
        while(i.hasNext()){
            e = (Element)i.next();
            cob.addContent((Element)e.clone());
        }

        i = getSupportedFormatElements().iterator();
        while(i.hasNext()){
            e = (Element)i.next();
            cob.addContent((Element)e.clone());
        }

        cob.addContent((Element)getIdentifierElement().clone());

        return cob;
    }


    /**
     *
     * @return Returns the wcs:CoverageDescription element that represents this CoverageDescription.
     */
    public Element getElement(){
        return (Element) myCD.clone();
    }


    /**
     * Gets the DAP local ID for the DAP Grid variable data array  that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @return  Returns the DAP local ID for the DAP Grid variable data array  that is associated by the wcs:Identifier
     */
    public String getDapGridArrayId(String fieldID){
        return _dapGridId.get(fieldID);
    }

    /**
     * Sets the DAP local ID for the Grid variable data array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @param  dapGridId The DAP Variable ID of the Grid variable data array that is associated with the wcs:Field's
     * wcs:Identifier represented by filedID.
     */
    public void setDapGridArrayId(String fieldID, String dapGridId){
        _dapGridId.put(fieldID,dapGridId);
    }

    /**
     * Gets the DAP local ID for the Latitude coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @return  Returns the DAP local ID for the Latitude coordinate map array that is associated by the wcs:Identifier
     */
    public String getLatitudeCoordinateDapId(String fieldID) {
        return _latitudeCoordinateDapId.get(fieldID);

    }


    /**
     * Sets the DAP local ID for the Latitude coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @param  dapVariableID The DAP Variable ID of the Latitude coordinate map that is associated with the wcs:Field's
     * wcs:Identifier represented by filedID.
     */
    public void setLatitudeCoordinateDapId(String fieldID, String dapVariableID) {
        _latitudeCoordinateDapId.put(fieldID,dapVariableID);

    }

    /**
     * Gets the DAP local ID for the Longitude coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @return  Returns the DAP local ID for the Longitude coordinate map array that is associated by the wcs:Identifier
     */
    public String getLongitudeCoordinateDapId(String fieldID) {
        return _longitudeCoordinateDapId.get(fieldID);
    }

    /**
     * Sets the DAP local ID for the Longitude coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @param  dapVariableID The DAP Variable ID of the Longitude coordinate map that is associated with the wcs:Field's
     * wcs:Identifier represented by filedID.
     */
    public void setLongitudeCoordinateDapId(String fieldID, String dapVariableID) {
        _longitudeCoordinateDapId.put(fieldID,dapVariableID);
    }



    /**
     * Gets the DAP local ID for the Elevation coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @return  Returns the DAP local ID for the Elevation coordinate map array that is associated by the wcs:Identifier
     */
    public String getElevationCoordinateDapId(String fieldID) {
        return _elevationCoordinateDapId.get(fieldID);

    }


    /**
     * Sets the DAP local ID for the Elevation coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @param  dapVariableID The DAP Variable ID of the Elevation coordinate map that is associated with the wcs:Field's
     * wcs:Identifier represented by filedID.
     */
    public void setElevationCoordinateDapId(String fieldID, String dapVariableID) {
        _elevationCoordinateDapId.put(fieldID,dapVariableID);

    }

    /**
     * Gets the DAP local ID for the Time coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @return  Returns the DAP local ID for the Time coordinate map array that is associated by the wcs:Identifier
     */
    public String getTimeCoordinateDapId(String fieldID) {
        return _timeCoordinateDapId.get(fieldID);
    }


    /**
     * Sets the DAP local ID for the Time coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @param  dapVariableID The DAP Variable ID of the Time coordinate map that is associated with the wcs:Field's
     * wcs:Identifier represented by filedID.
     */
    public void setTimeCoordinateDapId(String fieldID, String dapVariableID) {
        _timeCoordinateDapId.put(fieldID,dapVariableID);
    }



    /**
     * Sets the time units string for DAP local ID for the Time coordinate map array that is associated
     * by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @param  timeUnits The time units string for the DAP Variable ID of the Time coordinate map that
     * is associated with the wcs:Field's
     * wcs:Identifier represented by filedID.
     */
    public void setTimeUnits(String fieldID, String timeUnits) {
        _timeUnits.put(fieldID,timeUnits);
    }

    /**
     * Gets the time units string for the Time coordinate map array that is associated by the wcs:Identifier
     * for the wcs:Field
     * @param fieldID The value of the wcs:Identifier associated with the wcs:Field in question.
     * @return  Returns the time units string for DAP local ID for the Time coordinate map array that
     * is associated by the wcs:Identifier
     */
    public String getTimeUnits(String fieldID) {
        return _timeUnits.get(fieldID);
    }


    /**
     * Gets the wcs:Identifier of each wcs:Field in this wcs:Coverage.
     * @return An array of strings containing the values of each wcs:Identifier of each wcs:Field in this wcs:Coverage.
     */
    public String[] getFieldIDs(){
        Vector<String> fIDs = new Vector<String>();
        Element field, identifier;
        String id;

        Iterator i =  myCD.getDescendants(new ElementFilter("Field",WCS.WCS_NS));
        while(i.hasNext()){
            field = (Element) i.next();
            identifier = field.getChild("Identifier",WCS.WCS_NS);
            if(identifier!=null){
                id = identifier.getTextTrim();
                fIDs.add(id);
            }
        }

        String[] fieldIDs = new String[fIDs.size()];

        return fIDs.toArray(fieldIDs);
    }

}
