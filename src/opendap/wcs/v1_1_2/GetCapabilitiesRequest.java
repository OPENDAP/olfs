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

import org.jdom.Namespace;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.util.HashMap;
import java.util.HashSet;
import java.io.IOException;
import java.io.OutputStream;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:00:35 PM
 */
public class GetCapabilitiesRequest {

    private static final Namespace _nameSpace    = WCS.OWS_NS;
    private static final String _schemaLocation  = WCS.OWS_SCHEMA_LOCATION_BASE+"owsGetCapabilities.xsd";


    private String   _service = "WCS";
    private String   _request = "GetCapabilities";
    private String[] _Sections = null;
    private String   _updateSequence = null;
    private String[] _AcceptFormats = null;
    private String[] _AcceptVersions = null;


    private static final HashSet<String> sectionNames = new HashSet<String>();
    static {
        sectionNames.add("ServiceIdentification");
        sectionNames.add("ServiceProvider");
        sectionNames.add("OperationsMetadata");
        sectionNames.add("Contents");
        sectionNames.add("All");
    }

    public GetCapabilitiesRequest(HashMap<String,String> kvp)
            throws WcsException {

        String[] tmp;

        String s = kvp.get("Sections");
        if(s!=null){
            tmp = s.split(",");
            for(String section:tmp){
                if(sectionNames.contains(section))
                    throw new WcsException("Client requested unsupported " +
                            "section name: "+section,WcsException.INVALID_PARAMETER_VALUE,"Sections");
            }
            _Sections = tmp;

        }

        _updateSequence = kvp.get("updateSequence");

        s = kvp.get("AcceptFormats");
        if(s!=null){
            tmp = s.split(",");
            _AcceptFormats = tmp;

        }

        s = kvp.get("AcceptVersions");
        if(s!=null){
            tmp = s.split(",");
            _AcceptVersions = tmp;

        }

    }

    public Element getRequestElement(){

        Element requestElement;

        requestElement = new Element(_request, _nameSpace);
        requestElement.addNamespaceDeclaration(WCS.XSI_NS);
        requestElement.setAttribute("_schemaLocation", _schemaLocation,WCS.XSI_NS);
        requestElement.setAttribute("service",_service);

        if(_updateSequence!=null)
            requestElement.setAttribute("updateSequence",_updateSequence);



        if(_AcceptVersions != null){
            Element av = new Element("AcceptVersions",_nameSpace);
            Element ver;
            for(String v: _AcceptVersions){
                ver = new Element("Version",_nameSpace);
                ver.setText(v);
                av.addContent(ver);
            }
            requestElement.addContent(av);
        }

        if(_Sections != null){
            Element sections = new Element("Sections",_nameSpace);
            Element sec;
            for(String sv : _Sections){
                sec = new Element("Section",_nameSpace);
                sec.setText(sv);
                sections.addContent(sec);
            }
            requestElement.addContent(sections);
        }

        if(_AcceptFormats != null){
            Element af = new Element("AcceptedFormats",_nameSpace);
            Element fe;
            for(String f : _AcceptFormats){
                fe = new Element("OutputFormat",_nameSpace);
                fe.setText(f);
                af.addContent(fe);
            }
            requestElement.addContent(af);

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
