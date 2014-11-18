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
package opendap.threddsHandler;

/**
 * User: ndp
 * Date: Apr 19, 2008
 * Time: 10:47:53 AM
 */
public class Util {


    public static String basename(String s){
        return basename(s,"");
    }


    public static String basename(String s, String suffix){
        int i;

        while(s.endsWith("/") && s.length()>1)
            s = s.substring(0,s.length()-1);


        if(s.equals("/"))
            return "";

        i = s.lastIndexOf("/");

        if(i<0)
            return s;


        s = s.substring(i+1,s.length());

        if(s.endsWith(suffix) && !s.equals(suffix))
            s = s.substring(0,s.length()-suffix.length());

        return s;


    }



}
