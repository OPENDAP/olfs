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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * User: ndp
 * Date: Aug 18, 2008
 * Time: 1:32:03 PM
 */
public class RangeSubset {


    private static Logger log = LoggerFactory.getLogger(RangeSubset.class);
    private FieldSubset[] fieldSubsets;

    /**
     *
     * @param kvp Key Value Pairs from WCS URL
     * @throws WcsException  When things don't go well.
     */
    public RangeSubset(HashMap<String,String> kvp) throws WcsException{


        String s = kvp.get("RangeSubset");

        if(s!=null){
        String[] fieldSubsetStrings = s.split(";",0);

        if(fieldSubsetStrings.length < 1)
            throw new WcsException("The RangeSubset is required to have one or more FieldSubsets.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "FieldSubset");

        fieldSubsets = new FieldSubset[fieldSubsetStrings.length];

        int i = 0;
        for(String fs : fieldSubsetStrings){

            fieldSubsets[i++] = new FieldSubset(fs);


        }
        }
        else {
            fieldSubsets = new FieldSubset[0];
        }



    }



    public RangeSubset(Element rangeSubset) throws WcsException{

        Element e;
        Iterator i;
        int index;
        String s;

        WCS.checkNamespace(rangeSubset,"RangeSubset",WCS.WCS_NS);

        List rsl = rangeSubset.getChildren("FieldSubset",WCS.WCS_NS);

        if(rsl.size()==0){
            throw new WcsException("The wcs:RangeSubset element is required to have one or more wcs:FieldSubset child elements.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:FieldSubset");
        }

        fieldSubsets = new FieldSubset[rsl.size()];
        i = rsl.iterator();
        index = 0;
        while(i.hasNext()){
            e = (Element) i.next();
            fieldSubsets[index++] = new FieldSubset(e);

        }

    }


    public FieldSubset[] getFieldSubsets(){
        return fieldSubsets;

    }


    public Element getElement() {

        if (fieldSubsets.length > 0) {
            Element range = new Element("RangeSubset", WCS.WCS_NS);

            for (FieldSubset fs : fieldSubsets) {
                range.addContent(fs.getElement());
            }
            return range;
        }
        else
            return null;
    }




}
