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
 * Time: 4:00:22 PM
 */
public class GetCoverageRequest {

    private static final Namespace _nameSpace    = WCS.WCS_NS;
    private static final String _schemaLocation  = WCS.WCS_SCHEMA_LOCATION_BASE+"wcsGetCoverage.xsd";


    private String   _service = "WCS";
    private String   _request = "GetCoverage";


    public GetCoverageRequest(HashMap<String,String> kvp)
            throws WcsException {

    }
    public Element getRequestElement(){

        Element requestElement;

        requestElement = new Element(_request, _nameSpace);
        requestElement.addNamespaceDeclaration(WCS.XSI_NS);
        requestElement.setAttribute("_schemaLocation", _schemaLocation,WCS.XSI_NS);
        requestElement.setAttribute("service",_service);

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
