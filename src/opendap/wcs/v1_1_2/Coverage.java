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

import org.jdom.Element;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Represents a wcs:Coverage object. A wcs:Coverage object is part of a response to a wcs:GetCoverageRequest
 *
 *
 *
 *
 *
 *
 */
public class Coverage {


    private URI _coverageRole;
    private URI _metadataRole;


    private String _title;
    public void setTitle(String s){ _title = s; }
    public String getTitle(){ return _title; }

    private String _abstract;
    public void setAbstract(String s){ _abstract = s; }
    public String getAbstract(){ return _abstract; }

    private String _id;
    public void setID(String s){ _id = s; }
    public String getID(){ return _id; }

    private String _dataAccessURL;
    public void setDataAccessURL(String s){ _dataAccessURL = s; }
    public String getDataAccessURL(){ return _dataAccessURL; }

    private String _metadataURL;
    public void setMetadataAccessURL(String s){ _metadataURL = s; }
    public String getMetadataAccessURL(){ return _metadataURL; }





    void init() throws WcsException {

        _id=null;
        _title= null;
        _abstract = null;
        _dataAccessURL = null;
        _metadataURL   = null;

        try {
            _coverageRole = new URI("urn:ogc:def:role:WCS:1.1:coverage");
            _metadataRole = new URI("urn:ogc:def:role:WCS:1.1:metadata");
        } catch (URISyntaxException e) {
            throw new WcsException(e.getMessage(),WcsException.NO_APPLICABLE_CODE);

        }

    }




    public Coverage(String id, String title, String abstrct, String dataAccessURL, String metadataURL)throws WcsException {


        init();
        _id=id;
        _title= title;
        _abstract = abstrct;
        _dataAccessURL = dataAccessURL;
        _metadataURL   = metadataURL;

    }

    public Coverage(String id, String dataAccessURL, String metadataURL)throws WcsException {
        this(id,null,null,dataAccessURL,metadataURL);
    }

    public Coverage()throws WcsException {
        init();
    }

    public  Element getCoverageElement() throws WcsException {

        Element e;
        Element cvg = new Element("Coverage",WCS.WCS_NS);

        cvg.addNamespaceDeclaration(WCS.OWS_NS);
        cvg.addNamespaceDeclaration(WCS.XLINK_NS);



        if(_title!=null){
            e = new Element("Title",WCS.OWS_NS);
            e.setText(_title);
            cvg.addContent(e);
        }

        if(_abstract!=null){
            e = new Element("Abstract",WCS.OWS_NS);
            e.setText(_abstract);
            cvg.addContent(e);
        }

        if(_id!=null){
            e = new Element("Identifier",WCS.OWS_NS);
            e.setText(_id);
            cvg.addContent(e);
        }


        if(_dataAccessURL==null)
            throw new WcsException("This wcs:Coverage element (ows:Identifier = "+_id+") is missing a data " +
                    "access URL.",WcsException.MISSING_PARAMETER_VALUE,"ows:Reference");


        e = new Element("Reference",WCS.OWS_NS);
        e.setAttribute("href",_dataAccessURL,WCS.XLINK_NS);
        e.setAttribute("type","simpleLink",WCS.XLINK_NS);
        e.setAttribute("role",_coverageRole.toASCIIString(),WCS.XLINK_NS);
        cvg.addContent(e);

        if(_metadataURL==null)
            throw new WcsException("This wcs:Coverage element (ows:Identifier = "+_id+") is missing a metadata " +
                    "access URL.",WcsException.MISSING_PARAMETER_VALUE,"ows:Reference");

        e = new Element("Reference",WCS.OWS_NS);
        e.setAttribute("href",_metadataURL,WCS.XLINK_NS);
        e.setAttribute("type","simpleLink",WCS.XLINK_NS);
        e.setAttribute("role",_metadataRole.toASCIIString(),WCS.XLINK_NS);
        cvg.addContent(e);



        return cvg;



        
    }



    
}
