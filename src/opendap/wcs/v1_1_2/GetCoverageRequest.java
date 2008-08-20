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
import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: ndp
 * Date: Aug 13, 2008
 * Time: 4:00:22 PM
 */
public class GetCoverageRequest {



    private String   _service  = "WCS";
    private String   _version  = "1.1.2";
    private String   _request  = "GetCoverage";
    private String        _id  = null;
    private BoundingBox _bbox  = null;
    private String     _format = null;
    private TimeSequence _tseq = null;
    private RangeSubset    _rs = null;

    private boolean _store = false;


    private URI _gridBaseCRS       = null;
    private URI _gridType          = null;
    private URI _gridCS            = null;
    private double[] _gridOrigin   = null;
    private double[] _gridOffsets  = null;

    private boolean hasUserGridCRS = false;


    public GetCoverageRequest(HashMap<String,String> kvp)
            throws WcsException {

        String s;


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



        // Get the identifier for the coverage.
        s = kvp.get("identifier");
        if(s!=null){
            _id = s;
        }
        else {
            throw new WcsException("Request is missing required " +
                    "Coverage identifier.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "identifier");

        }


        // Get the BoundingBox for the coverage.
        _bbox = BoundingBox.fromKVP(kvp);
        if(_bbox==null){
            throw new WcsException("Request is missing required " +
                    "BoundingBox key value pairs. This is used to identify " +
                    "what data will be returned.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "BoundingBox");

        }

        // Get the format.
        s = kvp.get("format");
        if(s!=null){
            _format = s;
        }
        else {
            throw new WcsException("Request is missing required " +
                    "format key. This is used to specify what the " +
                    "retuned format of the data response should be.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "format");

        }


        // Get the optional TimeSequence selection
        _tseq = TimeSequence.fromKVP(kvp);


        // Get the optional store imperative
        _rs = RangeSubset.fromKVP(kvp);


        // Get the optional store imperative
        s = kvp.get("store");
        if(s!=null){
            _store = Boolean.parseBoolean(s);
        }


        ingestUserCRS(kvp);


    }




    public void ingestUserCRS(HashMap<String,String> kvp) throws WcsException{

        String s, tmp[];

        // Get the optional GridBaseCRS
        s = kvp.get("GridBaseCRS");
        if(s!=null){

            try {
                _gridBaseCRS = new URI(s);
                hasUserGridCRS = true;
            }
            catch(URISyntaxException e){
                throw new WcsException(e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "GridBaseCRS");
            }

        }

        // Get the optional GridType
        s = kvp.get("GridType");
        if(s!=null){
            try {
                _gridType = new URI(s);
                hasUserGridCRS = true;
            }
            catch(URISyntaxException e){
                throw new WcsException(e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "GridType");
            }
        }


        // Get the optional GridCS
        s = kvp.get("GridCS");
        if(s!=null){
            try {
                _gridCS = new URI(s);
                hasUserGridCRS = true;
            }
            catch(URISyntaxException e){
                throw new WcsException(e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "GridCS");
            }
        }


        // Get the optional GridOrigin
        s = kvp.get("GridOrigin");
        if(s!=null){

            tmp = s.split(",");

            _gridOrigin = new double[tmp.length];

            try {
                for(int i=0; i<tmp.length; i++){
                    _gridOrigin[i] = Double.parseDouble(tmp[i]);
                }
                hasUserGridCRS = true;
            }
            catch(NumberFormatException e){
                throw new WcsException(e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "GridOrigin");
            }


        }


        // Get the optional GridOffsets
        s = kvp.get("GridOffsets");
        if(s!=null){
            tmp = s.split(",");

            _gridOffsets = new double[tmp.length];

            try {
                for(int i=0; i<tmp.length; i++){
                    _gridOffsets[i] = Double.parseDouble(tmp[i]);
                }
                hasUserGridCRS = true;
            }
            catch(NumberFormatException e){
                throw new WcsException(e.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "GridOffsets");
            }

        }

    }






    public Document getRequestDoc()throws WcsException{
        return new Document(getRequestElement());
    }


    public void serialize(OutputStream os) throws IOException, WcsException {
        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());
        xmlo.output(getRequestDoc(), os);
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

/*


        requestElement.addNamespaceDeclaration(WCS.OWCS_NS);
        _schemaLocation += WCS.OWCS_NAMESPACE_STRING + "  "+ WCS.OWCS_SCHEMA_LOCATION_BASE+"owcsAll.xsd  ";
*/
        requestElement.addNamespaceDeclaration(WCS.XSI_NS);




        requestElement.setAttribute("schemaLocation", schemaLocation,WCS.XSI_NS);


        requestElement.setAttribute("service",_service);
        requestElement.setAttribute("version",_version);

        Element e = new Element("Identifier",WCS.WCS_NS);
        e.setText(_id);
        requestElement.addContent(e);

        requestElement.addContent(getDomainSubsetElement());

        if(_rs!=null){
            requestElement.addContent(_rs.getRangeSubsetElement());
        }

        requestElement.addContent(getOutputTypeElement());

        return requestElement;

    }



    public Element getDomainSubsetElement() throws WcsException {


        Element domainSubset = new Element("DomainSubset",WCS.WCS_NS);

        Element e = _bbox.getBoundingBoxElement();

        domainSubset.addContent(e);

        if(_tseq!=null){
            e = _tseq.getTemporalSubsetElement();
            domainSubset.addContent(e);
        }



        return domainSubset;
    }



    public Element getOutputTypeElement(){
        Element ot = new Element("OutputType",WCS.WCS_NS);
        ot.setAttribute("format",_format);

        if(_store)
            ot.setAttribute("store",_store+"");

        if(hasUserGridCRS)
            ot.addContent(getGridCRSElement());


        return ot;
    }






    public Element getGridCRSElement(){


        if(!hasUserGridCRS)
            return null;

        Element crs = new Element("GridCRS",WCS.WCS_NS);
        Element e;

        if(_gridBaseCRS!=null){
            e = new Element("GridBaseCRS",WCS.WCS_NS);
            e.setText(_gridBaseCRS.toString());
            crs.addContent(e);
        }


        if(_gridType!=null){
            e = new Element("GridType",WCS.WCS_NS);
            e.setText(_gridType.toString());
            crs.addContent(e);
        }


        if(_gridOrigin!=null){
            e = new Element("GridOrigin",WCS.WCS_NS);
            String txt  = "";

            for (double originCoordinate : _gridOrigin) {
                txt += originCoordinate + "  ";
            }

            e.setText(txt);
            crs.addContent(e);

        }


        if(_gridOffsets!=null){
            e = new Element("GridOffsets",WCS.WCS_NS);
            String txt  = "";

            for (double offsetCoordinate : _gridOffsets) {
                txt += offsetCoordinate + "  ";
            }

            e.setText(txt);
            crs.addContent(e);

        }



        if(_gridCS!=null){
            e = new Element("GridCS",WCS.WCS_NS);
            e.setText(_gridCS.toString());
            crs.addContent(e);
        }



        return crs;

    }








}
