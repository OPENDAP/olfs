/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "Hyrax Data Server" project.
 * //
 * //
 * // Copyright (c) 2015 OPeNDAP, Inc.
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

package opendap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ndp on 4/21/15.
 */
public class PathBuilder  {

    private Logger _log;

    private StringBuilder _sb;

    public PathBuilder(){
        _log = LoggerFactory.getLogger(this.getClass());
        _sb = new StringBuilder();
    }

    public PathBuilder(String s){
        this();
        _sb.append(s);
    }

    public PathBuilder(CharSequence cs){
        this();
        _sb.append(cs);
    }

    public PathBuilder pathAppend(String s){
        if(s==null || s.length()==0)
            return this;

        while (s.startsWith("/") && s.length() > 0) {
            s = s.substring(1);
        }
        //_log.debug("pathAppend: _sb: '{}' s: '{}'",_sb.toString(),s);
        //_log.debug("pathAppend: _sb.lastIndexOf(\"/\"): '{}' _sb.length(): '{}'",_sb.lastIndexOf("/"),_sb.length());
        if (_sb.length()==0 || (_sb.lastIndexOf("/") == _sb.length()-1)) {
            _sb.append(s);
        } else {
            _sb.append("/").append(s);
        }
        _log.info("pathAppend: result _sb: ",_sb.toString());
        return this;
    }


    public static String pathConcat(String path1, String path2){
        String result;
        if(path1==null || path1.length()==0) {
            result = path2;

        }
        else if(path2==null || path2.length()==0){
            result = path1;
        }
        else {
            while (path2.startsWith("/") && path2.length() > 0) {
                path2 = path2.substring(1);
            }
            if (path1.endsWith("/")) {
                result = path1 + path2;
            } else {
                result = path1 + "/" + path2;
            }
        }
        return result;
    }



    public PathBuilder append(String s){
        _sb.append(s);
        return this;
    }

    @Override
    public String toString(){
        return _sb.toString();
    }




}
