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
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:00:22 PM
 */
public class GetCoverageRequest {



    private static final String _request = "GetCoverage";



    private String          coverageID = null;
    private BoundingBox     bbox = null;
    private String          format = null;
    private GridCRS         gridCRS = null;
    private TemporalSubset  tseq = null;
    private RangeSubset     rangeSubset = null;
    private boolean         store = false;

    public String getCoverageID() {
        return coverageID;
    }

    public void setCoverageID(String coverageID) {
        this.coverageID = coverageID;
    }

    public BoundingBox getBbox() {
        return bbox;
    }

    public void setBbox(BoundingBox bbox) {
        this.bbox = bbox;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public GridCRS getGridCRS() {
        return gridCRS;
    }

    public void setGridCRS(GridCRS gridCRS) {
        this.gridCRS = gridCRS;
    }

    public TemporalSubset getTemporalSubset() {
        return tseq;
    }

    public void setTseq(TemporalSubset tseq) {
        this.tseq = tseq;
    }

    public RangeSubset getRangeSubset() {
        return rangeSubset;
    }

    public void setRangeSubset(RangeSubset rangeSubset) {
        this.rangeSubset = rangeSubset;
    }

    public boolean isStore() {
        return store;
    }

    public void setStore(boolean store) {
        this.store = store;
    }




    public GetCoverageRequest(HashMap<String,String> kvp)
            throws WcsException {

        String s;

        // Make sure the client is looking for a WCS service....
        WCS.checkService(kvp.get("service"));

        // Make sure the client can accept a supported WCS version...
        WCS.checkVersion(kvp.get("version"));


        // Make sure the client is acutally asking for this operation
        s = kvp.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(!s.equals(_request)){
            throw new WcsException("The servers internal dispatch operations " +
                    "have failed. The WCS request for the operation '"+s+"' " +
                    "has been incorrectly routed to the 'GetCapabilities' " +
                    "request processor.",
                    WcsException.NO_APPLICABLE_CODE);
        }



        // Get the identifier for the coverage.
        s = kvp.get("identifier");
        if(s==null){
            throw new WcsException("Request is missing required " +
                    "Coverage 'identifier'.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "identifier");
        }
        coverageID = s;


        // Get the BoundingBox for the coverage.
        bbox = new BoundingBox(kvp);

        // Get the format.
        s = kvp.get("format");
        if(s==null){
            throw new WcsException("Request is missing required " +
                    "format key. This is used to specify what the " +
                    "retuned format of the data response should be.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "format");
        }
        format = s;


        // Get the optional TemporalSubset selection
        if(kvp.containsKey("TimeSequence"))
            tseq = new TemporalSubset(kvp);


        // Get the optional RangeSubset subset
        rangeSubset = new RangeSubset(kvp);


        // Get the optional store imperative
        s = kvp.get("store");
        if(s!=null){
            store = Boolean.parseBoolean(s);
        }


        if(GridCRS.hasGridCRS(kvp))
            gridCRS = new GridCRS(kvp);


    }


    public GetCoverageRequest(Element getCoverageRequestElem)
            throws WcsException {


        Element e;
        String s;


        // Make sure we got the correct request object.
        WCS.checkNamespace(getCoverageRequestElem,"GetCoverage",WCS.WCS_NS);

        // Make sure the client is looking for a WCS service....
        WCS.checkService(getCoverageRequestElem.getAttributeValue("service"));

        // Make sure the client can accept a supported WCS version...
        WCS.checkVersion(getCoverageRequestElem.getAttributeValue("version"));



        // Get the identifier for the coverage.
        e = getCoverageRequestElem.getChild("Identifier",WCS.OWS_NS);
        if(e==null ){
            throw new WcsException("Missing required ows:Identifier element. ",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "ows:Identifier");
        }
        coverageID =e.getText();

        ingestDomainSubset(getCoverageRequestElem.getChild("DomainSubset",WCS.WCS_NS));

        
        e = getCoverageRequestElem.getChild("RangeSubset",WCS.WCS_NS);
        if(e!=null)
            rangeSubset = new RangeSubset(e);



        // Get the Output for the coverage.
        Element output  = getCoverageRequestElem.getChild("Output",WCS.WCS_NS);
        if(output==null ){
            throw new WcsException("Missing required ows:Output element. ",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:Output");
        }
        s=output.getAttributeValue("format");
        if(s==null){
            throw new WcsException("The wcs:Output element is missing the required " +
                    "format attribute. This is used to specify what the " +
                    "returned format of the data response should be.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:Ouput@format");
        }
        format = s;


        e = output.getChild("GridCRS",WCS.WCS_NS);
        if(e!=null ){
            gridCRS = new GridCRS(e);

        }


        s=output.getAttributeValue("store");
        store = false;
        if(s!=null){

            if(s.equalsIgnoreCase("true")){
                store =true;
            }
            else if(s.equalsIgnoreCase("false")){
                store = false;
            }
            else {
                throw new WcsException("The wcs:Output@store attribute has an incorrect value. " +
                        "It may be 'true' or 'false'. " ,
                        WcsException.INVALID_PARAMETER_VALUE,
                        "wcs:Ouput@store");
            }

        }







    }




    public void ingestDomainSubset(Element domainSubset) throws WcsException {


        WCS.checkNamespace(domainSubset,"DomainSubset", WCS.WCS_NS);

        // Get the BoundingBox for the coverage.
        Element e = domainSubset.getChild("BoundingBox",WCS.OWS_NS);
        bbox = new BoundingBox(e);


        // Get the optional TemporalSubset selection
        // Get the BoundingBox for the coverage.
        e = domainSubset.getChild("TemporalSubset",WCS.OWS_NS);
        if(e!=null)
            tseq = new TemporalSubset(e);
                  

    }








    public Document getRequestDoc()throws WcsException{
        return new Document(getRequestElement());
    }


    public void serialize(OutputStream os) throws IOException, WcsException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(getRequestDoc(), os);
    }

    public String toString(){
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        try {
            return xmlo.outputString(getRequestDoc());
        } catch (WcsException e) {
            WcsExceptionReport er = new WcsExceptionReport(e);
            return er.toString();
        }

    }





    public Element getRequestElement() throws WcsException{

        Element requestElement;
        String schemaLocation;

        requestElement = new Element(_request, WCS.WCS_NS);
        schemaLocation = WCS.WCS_NAMESPACE_STRING + "  "+ WCS.WCS_SCHEMA_LOCATION_BASE+"wcsGetCoverage.xsd  ";

        requestElement.addNamespaceDeclaration(WCS.OWS_NS);
        schemaLocation += WCS.OWS_NAMESPACE_STRING + "  "+ WCS.OWS_SCHEMA_LOCATION_BASE+"owsCommon.xsd  ";

        requestElement.addNamespaceDeclaration(WCS.GML_NS);
        schemaLocation += WCS.GML_NAMESPACE_STRING + "  "+ WCS.GML_SCHEMA_LOCATION_BASE+"gml.xsd  ";


        requestElement.addNamespaceDeclaration(WCS.OWCS_NS);
        schemaLocation += WCS.OWCS_NAMESPACE_STRING + "  "+ WCS.OWCS_SCHEMA_LOCATION_BASE+"owcsAll.xsd  ";

        requestElement.addNamespaceDeclaration(WCS.XSI_NS);




        requestElement.setAttribute("schemaLocation", schemaLocation,WCS.XSI_NS);


        requestElement.setAttribute("service",WCS.SERVICE);
        requestElement.setAttribute("version",WCS.CURRENT_VERSION);

        Element e = new Element("Identifier",WCS.OWS_NS);
        e.setText(coverageID);
        requestElement.addContent(e);

        requestElement.addContent(getDomainSubsetElement());

        if(rangeSubset !=null){
            Element rs = rangeSubset.getElement();
            if(rs!=null)
                requestElement.addContent(rs    );
        }

        requestElement.addContent(getOutputElement());

        return requestElement;

    }



    public Element getDomainSubsetElement() throws WcsException {


        Element domainSubset = new Element("DomainSubset",WCS.WCS_NS);

        Element e = bbox.getBoundingBoxElement();

        domainSubset.addContent(e);

        if(tseq !=null){
            e = tseq.getTemporalSubsetElement();
            domainSubset.addContent(e);
        }


        return domainSubset;
    }



    public Element getOutputElement(){
        Element ot = new Element("Output",WCS.WCS_NS);
        ot.setAttribute("format", format);

        if(store)
            ot.setAttribute("store", store +"");

        if(gridCRS != null)
            ot.addContent(gridCRS.getElement());


        return ot;
    }


}
