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
package opendap.wcs.v2_0;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:01:09 PM
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
public class DescribeCoverageRequest {



    private final String   _request = "DescribeCoverage";

    private String[] ids;

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public DescribeCoverageRequest(Element descrCovElem) throws WcsException {

        Element e;
        Iterator i;
        int index;

        // Make sure we got the correct request object.
        WCS.checkNamespace(descrCovElem,_request,WCS.WCS_NS);

        // Make sure the client is looking for a WCS service....
        WCS.checkService(descrCovElem.getAttributeValue("service"));

        // Make sure the client can accept a supported WCS version...
        WCS.checkVersion(descrCovElem.getAttributeValue("version"));

        // Get the Identifiers that client wants CoverageDescriptions for.
        List vlist = descrCovElem.getChildren("CoverageId",WCS.WCS_NS);
        if(vlist.size()==0){
            throw new WcsException("The wcs:DescribeCoverage element is required to have one or more wcs:Identifier child elements.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:CoverageId");
        }

        ids =  new String[vlist.size()];
        i = vlist.iterator();
        index = 0;
        while(i.hasNext()){
            e = (Element) i.next();
            ids[index++] = e.getTextNormalize();

        }

    }



    public DescribeCoverageRequest(Map<String,String[]> kvp)
            throws WcsException {


        String tmp[], s[];


        // Make sure the client is looking for a WCS service....
        s = kvp.get("service");
        WCS.checkService(s==null? null : s[0]);

        // Make sure the client can accept a supported WCS version...
        s = kvp.get("version");
        WCS.checkVersion(s==null? null : s[0]);



        // Make sure the client is actually asking for this operation
        s = kvp.get("request");
        if(s == null){
            throw new WcsException("Poorly formatted request URL. Missing " +
                    "key value pair for 'request'",
                    WcsException.MISSING_PARAMETER_VALUE,"request");
        }
        else if(!s[0].equalsIgnoreCase(_request)){
            throw new WcsException("The servers internal dispatch operations " +
                    "have failed. The WCS request for the operation '"+s+"' " +
                    "has been incorrectly routed to the '"+_request+"' " +
                    "request processor.",
                    WcsException.NO_APPLICABLE_CODE);
        }


        // Get the list of identifiers for the coverage to describe.
        s = kvp.get("coverageId".toLowerCase());
        if(s!=null){
            tmp = s[0].split(",");
            ids = tmp;
        }
        else {
            throw new WcsException("Request is missing required list of " +
                    "Coverage identifiers.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "coverageId");

        }












    }


    /**
     * <pre>
     *     &lt;element name="DescribeCoverage"&gt;
     *       &lt;annotation&gt;
     *         &lt;documentation&gt;Request to a WCS to perform the DescribeCoverage operation. This operation allows a client to retrieve descriptions of one or more coverages. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation. &lt;/documentation&gt;
     *       &lt;/annotation&gt;
     *       &lt;complexType&gt;
     *         &lt;complexContent&gt;
     *           &lt;extension base="wcs:RequestBaseType"&gt;
     *             &lt;sequence&gt;
     *               &lt;element ref="wcs:Identifier" maxOccurs="unbounded"&gt;
     *                 &lt;annotation&gt;
     *                   &lt;documentation&gt;Unordered list of identifiers of desired coverages. A client can obtain identifiers by a prior GetCapabilities request, or from a third-party source. &lt;/documentation&gt;
     *                 &lt;/annotation&gt;
     *               &lt;/element&gt;
     *             &lt;/sequence&gt;
     *           &lt;/extension&gt;
     *         &lt;/complexContent&gt;
     *       &lt;/complexType&gt;
     *     &lt;/element&gt;
     * </pre>
     * @return The DescribeCoverage request element.
     */
    public Element getRequestElement(){

        Element requestElement;

        requestElement = new Element(_request, WCS.WCS_NS);
        requestElement.addNamespaceDeclaration(WCS.XSI_NS);

        String schemaLocation  =
                WCS.WCS_NAMESPACE_STRING + "  " +WCS.WCS_SCHEMA_LOCATION_BASE+"wcsDescribeCoverage.xsd";

        requestElement.setAttribute("schemaLocation", schemaLocation,WCS.XSI_NS);
        requestElement.setAttribute("service",WCS.SERVICE);
        requestElement.setAttribute("version",WCS.CURRENT_VERSION);

        Element e;
        for(String id: ids){
            e = new Element("CoverageId",WCS.WCS_NS);
            e.setText(id);
            requestElement.addContent(e);
        }

        return requestElement;

    }


    public Document getRequestDoc(){
        return new Document(getRequestElement());
    }


    public void serialize(OutputStream os) throws IOException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(getRequestDoc(), os);
    }

    public String toString(){
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        return xmlo.outputString(getRequestDoc());       
    }



}
