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

        String tmp[], s;


        // Make sure the client is looking for a WCS service....
        s = kvp.get("service");
        if(s==null || !s.equals(_service))
            throw new WcsException("Only the WCS service (version "+
                    WCS.VERSIONS+") is supported.",
                    WcsException.OPERATION_NOT_SUPPORTED,s);



        // Make sure the client can accept the correct WCS version...
        boolean compatible = false;
        s = kvp.get("AcceptVersions");
        if(s!=null){
            tmp = s.split(",");
            for(String ver:tmp){
                if(WCS.VERSIONS.contains(ver)){
                    compatible=true;
                }
            }
            if(!compatible)
                throw new WcsException("Client requested unsupported WCS " +
                        "version(s): ["+s+"]\nThis WCS supports version(s) "+WCS.VERSIONS,
                        WcsException.VERSION_NEGOTIATION_FAILED,null);

            _AcceptVersions = tmp;
        }








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





        // Get the list of section the client has requested. Returning
        // individual sections may not be supported, but we'll keep track of
        // that partof the request regardless.
        s = kvp.get("Sections");
        if(s!=null){
            tmp = s.split(",");
            for(String section:tmp){
                if(!sectionNames.contains(section))
                    throw new WcsException("Client requested unsupported " +
                            "section name: "+section+"\n This WCS may support the following section names "+sectionNames,
                            WcsException.INVALID_PARAMETER_VALUE,"Sections");
            }
            _Sections = tmp;

        }

        // Store the updatSequence information in the event that the server
        // supports it at some point...
        _updateSequence = kvp.get("updateSequence");


        // Store the AccptedFormats offered by the client in the event that the
        // the server eventually supports more than text/html
        s = kvp.get("AcceptFormats");
        if(s!=null){
            tmp = s.split(",");
            _AcceptFormats = tmp;

        }


    }

    public Element getRequestElement(){

        Element requestElement;
        String _schemaLocation = "";

        requestElement = new Element(_request, WCS.WCS_NS);
        _schemaLocation += WCS.WCS_NAMESPACE_STRING + "  " +WCS.WCS_SCHEMA_LOCATION_BASE+"wcsGetCapabilities.xsd  ";

        requestElement.addNamespaceDeclaration(WCS.XSI_NS);

        requestElement.addNamespaceDeclaration(WCS.OWCS_NS);
        _schemaLocation += WCS.OWCS_NAMESPACE_STRING + "  " +WCS.OWCS_SCHEMA_LOCATION_BASE+"owsGetCapabilities.xsd  ";

        requestElement.setAttribute("schemaLocation", _schemaLocation,WCS.XSI_NS);


        requestElement.setAttribute("service",_service);

        if(_updateSequence!=null)
            requestElement.setAttribute("updateSequence",_updateSequence);



        if(_AcceptVersions != null){
            Element av = new Element("AcceptVersions",WCS.OWCS_NS);
            Element ver;
            for(String v: _AcceptVersions){
                ver = new Element("Version",WCS.OWCS_NS);
                ver.setText(v);
                av.addContent(ver);
            }
            requestElement.addContent(av);
        }

        if(_Sections != null){
            Element sections = new Element("Sections",WCS.OWCS_NS);
            Element sec;
            for(String sv : _Sections){
                sec = new Element("Section",WCS.OWCS_NS);
                sec.setText(sv);
                sections.addContent(sec);
            }
            requestElement.addContent(sections);
        }

        if(_AcceptFormats != null){
            Element af = new Element("AcceptFormats",WCS.OWCS_NS);
            Element fe;
            for(String f : _AcceptFormats){
                fe = new Element("OutputFormat",WCS.OWCS_NS);
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
