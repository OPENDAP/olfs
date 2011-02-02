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

package opendap.experiments;

/**
 * User: ndp
 * Date: Sep 5, 2006
 * Time: 11:54:50 PM
 */
public class StringUtil {


    public static String toHexString(byte b){

        int base;

	String s;

	base = ((int)b) & 0xFF;
	s = Integer.toHexString(base);
	if( base < 16)
	    s = "0" +s;

        return(s);

    }


    public static String toHexString(int i){

        String s;

        s = Integer.toHexString(i);

        for(int j=0, k=1; j<7 ;j++){
            k*=16;
            System.out.println("k: "+k);

            if(i<k)
                s = "0" +s;
        }

        return(s);
    }


    public static String toHexString(int i, int pad){

        String s;

        s = Integer.toHexString(i);

        if(pad > 8)
            pad = 7;
        else
            pad = pad - 1;

        for(int j=0, k=1; j<pad ;j++){
            k*=16;
            //System.out.println("k: "+k);

            if(i<k)
                s = "0" +s;
        }

        return(s);
    }


    public static void main(String[] args) throws Exception{

        for(int i=1; i<=65536*16*16 ;i*=16){

            System.out.println(i+": "+StringUtil.toHexString(i,7));
        }

        System.out.println("0x03ea = "+Integer.valueOf("03ea",16));


    }
}
