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
import java.util.Iterator;

/**
 */
public class TemporalSubset {


    private TimeSequenceItem[] _items=null;



    public TemporalSubset(Element ts) throws WcsException {

        Iterator i;
        Element e;
        int index;
        int count;

        if (ts == null)
            throw new WcsException("Missing wcs:TemporalSubset element. ",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:TemporalSubset");

        WCS.checkNamespace(ts,"TemporalSubset",WCS.WCS_NS);

        i = ts.getDescendants();
        if(!i.hasNext()){
            throw new WcsException("The wcs:TemporalSubset element must have one or child elements. ",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "wcs:TemporalSubset");
        }

        count = 0;
        while(i.hasNext()){
            i.next();
            count++;
        }
        _items = new TimeSequenceItem[count];

        index = 0;
        i = ts.getDescendants();
        while(i.hasNext()){
            e = (Element) i.next();
            _items[index++] = new TimeSequenceItem(e);
        }



    }



    public TemporalSubset(HashMap<String,String> kvp) throws WcsException {

        String s = kvp.get("TimeSequence");

        if (s == null)
            throw new WcsException("Missing required " +
                    "TimeSequence element. This is used to identify " +
                    "a physical world bounding space for which data will be returned.",
                    WcsException.MISSING_PARAMETER_VALUE,
                    "TimeSequence");

        String tmp[];


        // Time Sequences can be a comma separated list of time instances
        // and time ranges.
        tmp = s.split(",");

        _items = new TimeSequenceItem[tmp.length];

        for(int i=0; i<tmp.length ;i++){
            _items[i] = new TimeSequenceItem(tmp[i]);
        }

    }


    public Element getTemporalSubsetElement() throws WcsException {
        Element ts = new Element("TemporalSubset",WCS.WCS_NS);

        for(TimeSequenceItem tsi: _items){
            ts.addContent(tsi.getXMLElementRepresentation());
        }

        return ts;
    }


}
