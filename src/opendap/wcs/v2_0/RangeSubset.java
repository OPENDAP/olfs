/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2017 OPeNDAP, Inc.
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
package opendap.wcs.v2_0;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

public class RangeSubset {
    private Logger _log;
    private String _kvpSubsetString;
    private Vector<String> _requestedFields;

    RangeSubset(String kvpSubsetString, Vector<Field> fields) throws WcsException {
        _log = LoggerFactory.getLogger(getClass());
        _kvpSubsetString = kvpSubsetString;
        _requestedFields = new Vector<>();

        String[] ids = _kvpSubsetString.split(",");
        for(String id:ids){
            if(!id.isEmpty()){

                // Ranges of fields are expressed with a ":"
                if(id.contains(":")){
                    // Looks like a range expression - evaluate it.
                    String[] ss = id.split(":");
                    if(ss.length!=2)
                        throw new WcsException("Unable to process field list range expression.",
                                WcsException.INVALID_PARAMETER_VALUE);
                    String start = Util.stripQuotes(ss[0]);
                    String stop = Util.stripQuotes(ss[1]);

                    boolean gitit = false;
                    for(Field field : fields){
                        if(field.getName().equals(start)) gitit=true;

                        if(gitit) _requestedFields.add(field.getName());

                        if(field.getName().equals(stop))  gitit=false;
                    }
                }
                else {
                    _requestedFields.add(Util.stripQuotes(id));
                }


            }
        }
    }

    public Vector<String> getRequestedFields(){
        return  new Vector<>(_requestedFields);
    }
}
