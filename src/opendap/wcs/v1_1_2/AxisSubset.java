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

import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 2/9/12
 * Time: 11:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class AxisSubset {
    String _id;
    String[] _keyIDs;


    void init(){
        _id = null;
        _keyIDs = null;
    }

    AxisSubset(Element as) throws WcsException {

        init();

        Element e;

        WCS.checkNamespace(as,"AxisSubset",WCS.WCS_NS);

        e = as.getChild("Identifier",WCS.WCS_NS);
        if(e==null){
            throw new WcsException("The wcs:AxisSubset is required to have a wcs:Identifier child element.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:AxisSubset/wcs:Identifier");
        }
        _id = e.getTextNormalize();


        List keyList = as.getChildren("Key",WCS.WCS_NS);

        _keyIDs = new String[keyList.size()];

        Iterator i = keyList.iterator();
        int index = 0;
        while(i.hasNext()){
            e = (Element)i.next();
            _keyIDs[index++] = e.getText();
        }
    }

    String getID(){
        return _id;
    }

    String[] getKeys(){
        return _keyIDs;
    }

    Element getElement(){
        Element axisSubset = new Element("AxisSubset",WCS.WCS_NS);
        Element e = new Element("Identifier",WCS.WCS_NS);
        e.setText(_id);
        axisSubset.addContent(e);

        for(String keyID: _keyIDs){
            e = new Element("Key",WCS.WCS_NS);
            e.setText(keyID);
            axisSubset.addContent(e);
        }

        return axisSubset;


    }


}
