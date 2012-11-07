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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:00:22 PM
 */
public class GetCoverageRequest {



    private static final String _request = "GetCoverage";

    private String coverageID = null;
    private String format = null;
    private String mediaType = null;
    private Vector<DimensionSubset> subsets = null;






    public void setMediaType(String mType) throws WcsException {

        if(mType!=null && !mType.equalsIgnoreCase("multipart/related")){
            throw new WcsException("Optional mediaType MUST be set to'multipart/related' " +
                "No other value is allowed. OGC [09-110r4] section 8.4.1",
                WcsException.INVALID_PARAMETER_VALUE,
                "mediaType");
        }

        mediaType = mType;
    }

    public String getMediaType(){
        return mediaType;
    }


    public String getCoverageID() {
        return coverageID;
    }


    public void setCoverageID(String coverageID) {
        this.coverageID = coverageID;
    }


    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }





    public GetCoverageRequest(Map<String,String[]> kvp)
            throws WcsException {

        subsets = new Vector<DimensionSubset>();
        String s[];

        // Make sure the client is looking for a WCS service....
        s = kvp.get("service");
        WCS.checkService(s==null? null : s[0]);

        // Make sure the client can accept a supported WCS version...
        s = kvp.get("version");
        WCS.checkVersion(s==null? null : s[0]);


        // Make sure the client is acutally asking for this operation
        s = kvp.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(!s[0].equalsIgnoreCase(_request)){
            throw new WcsException("The servers internal dispatch operations " +
                    "have failed. The WCS request for the operation '"+s+"' " +
                    "has been incorrectly routed to the 'GetCapabilities' " +
                    "request processor.",
                    WcsException.NO_APPLICABLE_CODE);
        }



        // Get the identifier for the coverage.
        s = kvp.get("coverageId".toLowerCase());
        if(s==null){
            throw new WcsException("Request is missing required " +
                    "Coverage 'coverageId'.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "coverageId");
        }
        coverageID = s[0];



        // Get the format. It's not required (defaults to coverage's nativeFormat) and a null is used to indicate that
        // it was not specified.
        s = kvp.get("format");
        format = s==null? null : s[0];




        // Get the mediaType. It's not required and a null is used to indicate that
        // it was not specified. If it is specified it's value MUST BE "multipart/related" and the
        // the response MUST be a multipart MIME document with the gml:Coverage document in the first
        // part and the second part must contain whatever response format the user specified in the format parameter.
        s = kvp.get("mediaType".toLowerCase());
        if(s!=null){
            setMediaType(s[0]);
        }



        s = kvp.get("subset");
        if(s!=null){
            for(String subsetStr:s){
                DimensionSubset subset = new DimensionSubset(subsetStr);
                subsets.add(subset);
            }
        }

    }


    public GetCoverageRequest(Element getCoverageRequestElem)
            throws WcsException {

        subsets = new Vector<DimensionSubset>();

        Element e;
        String s;


        // Make sure we got the correct request object.
        WCS.checkNamespace(getCoverageRequestElem,"GetCoverage",WCS.WCS_NS);

        // Make sure the client is looking for a WCS service....
        WCS.checkService(getCoverageRequestElem.getAttributeValue("service"));

        // Make sure the client can accept a supported WCS version...
        WCS.checkVersion(getCoverageRequestElem.getAttributeValue("version"));



        // Get the identifier for the coverage.
        e = getCoverageRequestElem.getChild("CoverageId",WCS.WCS_NS);
        if(e==null ){
            throw new WcsException("Missing required wcs:CoverageId element. ",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:CoverageId");
        }
        coverageID =e.getText();


        ingestDimensionSubset(getCoverageRequestElem);


        // Get the format for the coverage output.
        Element formatElement  = getCoverageRequestElem.getChild("format",WCS.WCS_NS);
        if(formatElement!=null){
            format = formatElement.getTextTrim();
        }


        // Get the mediaType. It's not required and a null is used to indicate that
        // it was not specified. If it is specified it's value MUST BE "multipart/related" and the
        // the response MUST be a multipart MIME document with the gml:Coverage document in the first
        // part and the second part must contain whatever response format the user specified in the format parameter.
        Element mediaTypeElement = getCoverageRequestElem.getChild("mediaType",WCS.WCS_NS);
        if(mediaTypeElement!=null){
            s = mediaTypeElement.getTextTrim();
            setMediaType(s);
        }
    }




    public void ingestDimensionSubset(Element getCoverageRequestElem) throws WcsException {


        WCS.checkNamespace(getCoverageRequestElem,"GetCoverage", WCS.WCS_NS);

        MultiElementFilter dimensionTypeFilter = new MultiElementFilter("DimensionTrim",WCS.WCS_NS);
        dimensionTypeFilter.addTargetElement("DimensionSlice", WCS.WCS_NS);

        Iterator dtei = getCoverageRequestElem.getDescendants(dimensionTypeFilter);

        while(dtei.hasNext()){
            Element dimensionType = (Element) dtei.next();
            DimensionSubset ds = new DimensionSubset(dimensionType);
            subsets.add(ds);
        }

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

        //requestElement.addNamespaceDeclaration(WCS.OWS_NS);
        //schemaLocation += WCS.OWS_NAMESPACE_STRING + "  "+ WCS.OWS_SCHEMA_LOCATION_BASE+"owsAll.xsd  ";

        //requestElement.addNamespaceDeclaration(WCS.GML_NS);
        //schemaLocation += WCS.GML_NAMESPACE_STRING + "  "+ WCS.GML_SCHEMA_LOCATION_BASE+"gml.xsd  ";


        //requestElement.addNamespaceDeclaration(WCS.XSI_NS);

        //requestElement.setAttribute("schemaLocation", schemaLocation,WCS.XSI_NS);


        requestElement.setAttribute("service",WCS.SERVICE);
        requestElement.setAttribute("version",WCS.CURRENT_VERSION);

        Element e = new Element("CoverageId",WCS.WCS_NS);
        e.setText(coverageID);
        requestElement.addContent(e);

        for(DimensionSubset ds: subsets){
            requestElement.addContent(ds.getDimensionSubsetElement());
        }

        if(format !=null){
            Element formatElement = new Element("format",WCS.WCS_NS);
            formatElement.setText(format);
            requestElement.addContent(formatElement);
        }


        if(mediaType!=null){
            Element mediaTypeElement = new Element("mediaType",WCS.WCS_NS);
            mediaTypeElement.setText(mediaType);
            requestElement.addContent(mediaTypeElement);
        }


        return requestElement;

    }







}
