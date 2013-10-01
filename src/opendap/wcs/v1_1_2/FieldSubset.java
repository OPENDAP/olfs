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
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/9/12
 * Time: 11:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class FieldSubset {
    String _id;
    URI _codeType;
    String _interpolationType;
    AxisSubset[] _axisSubsets;

    void init(){
        _id = null;
        _codeType = null;
        _interpolationType = null;
        _axisSubsets = null;
    }

    FieldSubset(String fieldSubsetString) throws WcsException{

        _interpolationType = null;
        String fieldName;


        _axisSubsets = new AxisSubset[0];
        if(fieldSubsetString.indexOf("[")>=0){
                throw new WcsException("Axis subsetting is not supported by this service..",
                        WcsException.OPERATION_NOT_SUPPORTED,
                        "KVP Axis subset");
        }
        else {
            fieldName = fieldSubsetString;
        }

        if(fieldName.contains(":")) {
            if (fieldName.endsWith(":")) {
                throw new WcsException("The name of the interpolation method must be provided after " +
                        "the ':' character in the request URL.",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "KVP Interpolation Method.");

            }

            _interpolationType = fieldName.substring(fieldName.lastIndexOf(":") + 1, fieldName.length());
            fieldName = fieldName.substring(0, fieldName.lastIndexOf(":"));


        }

        _id = fieldName;

    }




    FieldSubset(Element fs) throws WcsException{
        init() ;

        Element e;
        String  s;

        WCS.checkNamespace(fs,"FieldSubset",WCS.WCS_NS);



        e = fs.getChild("Identifier",WCS.OWS_NS);
        if(e==null){
            throw new WcsException("The wcs:FieldSubset is required to have a ows:Identifier child element.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:FieldSubset/ows:Identifier");
        }
        _id = e.getTextNormalize();
        s = e.getAttributeValue("codeSpace");
        if(s!=null){
            try {
                _codeType = new URI(s);
            }
            catch(URISyntaxException use){
                throw new WcsException(use.getMessage(),
                        WcsException.INVALID_PARAMETER_VALUE,
                        "ows:Identifier@codeType");
            }
        }


        e = fs.getChild("InterpolationType", WCS.WCS_NS);
        if (e != null) {
            _interpolationType = e.getText();
            if (_interpolationType.isEmpty())
                throw new WcsException("The wcs:InterpolationType element is required to have content!",
                        WcsException.MISSING_PARAMETER_VALUE,
                        "wcs:InterpolationType");

        }

        List asl = fs.getChildren("AxisSubset",WCS.WCS_NS);


        // STOP PROCESSING! DO NOT PROCESS AXIS SUB_SETTING ELEMENTS!
        _axisSubsets = new AxisSubset[0];
        if(asl.size()>0)
            throw new WcsException("Axis sub-setting is not supported by this service..",
                    WcsException.OPERATION_NOT_SUPPORTED,
                    "wcs:AxisSubset");


        // The following code is blocked from procesing by the previous exception. This is intentional as this
        // Is the logical place to detect a request for the unsupported Axis sub-setting activity.
        _axisSubsets = new AxisSubset[asl.size()];
        Iterator i = asl.iterator();
        int index = 0;
        while(i.hasNext()){
            e = (Element) i.next();
            _axisSubsets[index++] = new AxisSubset(e);
        }

    }


    String getID(){
        return _id;
    }

    URI getIDCodeType(){
        return _codeType;
    }

    String getInterpolationType(){
        return _interpolationType;
    }

    AxisSubset[] getAxisSubsets(){
        return _axisSubsets;
    }

    Element getElement(){
        Element fieldSubset = new Element("FieldSubset",WCS.WCS_NS);

        Element fieldId = new Element("Identifier",WCS.OWS_NS);
        fieldId.setText(_id);


        if(_codeType!=null)
            fieldId.setAttribute("codeType",_codeType.toASCIIString());
        fieldSubset.addContent(fieldId);


        if(_interpolationType!=null) {
            fieldId = new Element("InterpolationType",WCS.WCS_NS);
            fieldId.setText(_interpolationType);
            fieldSubset.addContent(fieldId);
        }



        for(AxisSubset as: _axisSubsets){
            fieldSubset.addContent(as.getElement());
        }

     return fieldSubset;
    }


}
