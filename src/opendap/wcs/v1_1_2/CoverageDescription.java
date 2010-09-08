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

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;

import java.util.*;
import java.io.*;

/**
 * User: ndp
 * Date: Oct 15, 2008
 * Time: 9:56:41 PM
 */
public class CoverageDescription {

    private Element myCD;

    private Logger log;

    private BoundingBox _bbox;

    private long lastModified;

    private File myFile;

    private HashMap<String,String> _latitudeCoordinateDapId;
    private HashMap<String,String> _longitudeCoordinateDapId;
    private HashMap<String,String> _elevationCoordinateDapId;
    private HashMap<String,String> _timeCoordinateDapId;
    private HashMap<String,String> _timeUnits;



    public CoverageDescription(Element cd, long lastModified) throws WcsException{
        log = org.slf4j.LoggerFactory.getLogger(getClass());

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


    public long lastModified() {

        try {
            if (myFile != null && lastModified < myFile.lastModified()) {
                myCD = ingestCoverageDescription(myFile);
                ;
                lastModified = myFile.lastModified();
            }
        }
        catch (Exception e) {
            log.error("Failed to update CoverageDescription from file " +
                    myFile.getAbsoluteFile());
        }
        finally {
            return lastModified;

        }

    }


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






    public List getTitles(){
        return cloneElementList(myCD.getChildren("Title",WCS.OWS_NS));
    }

    public List getAbstracts(){
        return cloneElementList(myCD.getChildren("Abstract",WCS.OWS_NS));
    }

    public List getKeywords(){
        return cloneElementList(myCD.getChildren("KeyWords",WCS.OWS_NS));
    }

    public String getIdentifier(){
        Element wcsIdentifier =  myCD.getChild("Identifier",WCS.WCS_NS);
        return wcsIdentifier.getText();
    }
    public Element getIdentifierElement(){
        return (Element) myCD.getChild("Identifier",WCS.WCS_NS).clone();
    }

    public BoundingBox getBoundingBox() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        Element boundingBox =  spatialDomain.getChild("BoundingBox",WCS.OWS_NS);
        return new BoundingBox(boundingBox);
    }

    public Element getBoundingBoxElement() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("BoundingBox",WCS.OWS_NS).clone();
    }

    public GridCRS getGridCRS() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        Element gridCRS =  spatialDomain.getChild("GridCRS",WCS.WCS_NS);
        if(gridCRS==null)
            return null;
        return new GridCRS(gridCRS);
    }

    public Element getGridCRSElement() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("GridCRS",WCS.WCS_NS).clone();
    }

    public Element get_CoordinateOperationElement(){
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("_CoordinateOperation",WCS.GML_NS).clone();
    }

    public Element getImageCRSElement() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  (Element) spatialDomain.getChild("ImageCRS",WCS.WCS_NS).clone();
    }


    public List getPolygonElements() throws WcsException {
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        Element spatialDomain = domain.getChild("SpatialDomain",WCS.WCS_NS);
        return  cloneElementList(spatialDomain.getChildren("Polygon",WCS.GML_NS));
    }

    public Element getTemporalDomainElement(){
        Element domain =  myCD.getChild("Domain",WCS.WCS_NS);
        return  (Element) domain.getChild("TemporalDomain",WCS.WCS_NS).clone();
    }

    public Element getRangeElement(){
        return  (Element) myCD.getChild("Range",WCS.WCS_NS).clone();
    }

    public List getSupportedCrsElements(){
        return  cloneElementList(myCD.getChildren("SupportedCRS",WCS.WCS_NS));
    }

    public List<Element> getSupportedFormatElements(){

        return  cloneElementList(myCD.getChildren("SupportedFormat",WCS.WCS_NS));
    }

    private List<Element> cloneElementList(List<Element> list){
        ArrayList newList = new ArrayList<Element>();

        Iterator i = list.iterator();
        Element e;

        while(i.hasNext()){
            e = (Element)i.next();
            newList.add((Element)e.clone());
        }

        return newList;
    }




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


    public Element getElement(){
        return (Element) myCD.clone();
    }



    public String getLatitudeCoordinateDapId(String fieldID) {
        return _latitudeCoordinateDapId.get(fieldID);

    }
    public void setLatitudeCoordinateDapId(String fieldID, String dapVariableID) {
        _latitudeCoordinateDapId.put(fieldID,dapVariableID);

    }

    public String getLongitudeCoordinateDapId(String fieldID) {
        return _longitudeCoordinateDapId.get(fieldID);
    }
    public void setLongitudeCoordinateDapId(String fieldID, String dapVariableID) {
        _longitudeCoordinateDapId.put(fieldID,dapVariableID);
    }

    public String getElevationCoordinateDapId(String fieldID) {
        return _elevationCoordinateDapId.get(fieldID);

    }
    public void setElevationCoordinateDapId(String fieldID, String dapVariableID) {
        _elevationCoordinateDapId.put(fieldID,dapVariableID);

    }

    public String getTimeCoordinateDapId(String fieldID) {
        return _timeCoordinateDapId.get(fieldID);
    }
    public void setTimeCoordinateDapId(String fieldID, String dapVariableID) {
        _timeCoordinateDapId.put(fieldID,dapVariableID);
    }

    public void setTimeUnits(String fieldID, String timeUnits) {
        _timeUnits.put(fieldID,timeUnits);
    }
    public String getTimeUnits(String fieldID) {
        return _timeUnits.get(fieldID);
    }


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
