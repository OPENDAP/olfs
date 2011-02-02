/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2011 OPeNDAP, Inc.
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

package opendap.ppt;

import org.slf4j.Logger;

/**
 * @deprecated
 */
public class MarkFinder {


    private int     _markIndex;
    private byte[]  _mark;

    Logger log;




    public MarkFinder(byte[] mark){
        log = org.slf4j.LoggerFactory.getLogger(getClass());

        _mark = mark.clone();
        _markIndex = 0;

        log.debug("New MarkFinder. _mark="+new String(_mark));
    }

    public byte[] getMark(){

        return _mark.clone();
    }

    public int getMarkIndex(){
        return _markIndex;

    }


    public boolean markCheck(byte b) {

        if (_mark[_markIndex] == b) {
            log.debug("Found mark byte: "+b+" at index: "+_markIndex);
            _markIndex++;
            if (_markIndex == _mark.length) {
                _markIndex = 0;
                return (true);
            }
        } else {
            log.debug("*");
            _markIndex = 0;
        }

        return false;
    }

}
