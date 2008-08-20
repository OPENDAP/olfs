/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.HashMap;
import java.io.OutputStream;
import java.io.IOException;

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
<pre>
 &lt;element name="DescribeCoverage"&gt;
    &lt;annotation&gt;
        &lt;documentation&gt;Request to a WCS to perform the DescribeCoverage operation. This operation allows a client to retrieve descriptions of one or more coverages. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation. &lt;/documentation&gt;
    &lt;/annotation&gt;
    &lt;complexType&gt;
        &lt;complexContent&gt;
            &lt;extension base="wcs:RequestBaseType"&gt;
                &lt;sequence&gt;
                    &lt;element ref="wcs:Identifier" maxOccurs="unbounded"&gt;
                        &lt;annotation&gt;
                            &lt;documentation&gt;Unordered list of identifiers of desired coverages. A client can obtain identifiers by a prior GetCapabilities request, or from a third-party source. &lt;/documentation&gt;
                        &lt;/annotation&gt;
                    &lt;/element&gt;
                &lt;/sequence&gt;
            &lt;/extension&gt;
        &lt;/complexContent&gt;
    &lt;/complexType&gt;
&lt;/element&gt;
</pre>



 *
 */
public class DescribeCoverageRequest {

    private static final Namespace _nameSpace    = WCS.WCS_NS;


    private String   _service = "WCS";
    private String   _request = "DescribeCoverage";
    private String _version;
    private String[] _ids;


    public DescribeCoverageRequest(HashMap<String,String> kvp)
            throws WcsException {


        String tmp[], s;


        // Make sure the client is looking for a WCS service....
        s = kvp.get("service");
        if(s==null || !s.equals(_service))
            throw new WcsException("Only the WCS service (version "+
                    WCS.VERSIONS+") is supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,s);



        // Make sure the client can accept a supported WCS version...
        boolean compatible = false;
        s = kvp.get("version");
        if(s!=null){
            if(WCS.VERSIONS.contains(s)){
                compatible=true;
                _version = s;
            }
        }
        if(!compatible)
            throw new WcsException("Client requested unsupported WCS " +
                    "version(s): "+s,
                    WcsException.VERSION_NEGOTIATION_FAILED,null);


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


        // Get the list of identifiers for the coverage to describe.
        s = kvp.get("identifiers");
        if(s!=null){
            tmp = s.split(",");
            _ids = tmp;
        }
        else {
            throw new WcsException("Request is missing required list of " +
                    "Coverage identifiers.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "identifiers");

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
     * @return
     */
    public Element getRequestElement(){

        Element requestElement;

        requestElement = new Element(_request, _nameSpace);
        requestElement.addNamespaceDeclaration(WCS.XSI_NS);

        String schemaLocation  =
                WCS.WCS_NAMESPACE_STRING + "  " +WCS.WCS_SCHEMA_LOCATION_BASE+"wcsDescribeCoverage.xsd";

        requestElement.setAttribute("schemaLocation", schemaLocation,WCS.XSI_NS);
        requestElement.setAttribute("service",_service);
        requestElement.setAttribute("version",_version);

        Element e;
        for(String id: _ids){
            e = new Element("Identifier",_nameSpace);
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




}
