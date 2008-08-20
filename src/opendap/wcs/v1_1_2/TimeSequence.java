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

import java.util.HashMap;

/**
 * User: ndp
 * Date: Aug 18, 2008
 * Time: 1:11:15 PM
 */
public class TimeSequence {


    private TimeSequenceItem _items[];

    private TimeSequence(){
        _items = null;
    }


    public static TimeSequence fromKVP(HashMap<String,String> kvp) throws WcsException {

        String s = kvp.get("TimeSequence");

        if(s==null)
            return null;

        TimeSequence ts = new TimeSequence();

        String tmp[];


        // Time Sequences can be a comma separated list of time instances
        // and time ranges.
        tmp = s.split(",");

        ts._items = new TimeSequenceItem[tmp.length];

        for(int i=0; i<tmp.length ;i++){
            ts._items[i] = new TimeSequenceItem(tmp[i]);
        }
        return ts;



    }






    public Element getTemporalSubsetElement() throws WcsException {
        Element ts = new Element("TemporalSubset",WCS.WCS_NS);

        for(TimeSequenceItem tsi: _items){
            ts.addContent(tsi.getXMLElementRepresentation());
        }

        return ts;
    }


}
