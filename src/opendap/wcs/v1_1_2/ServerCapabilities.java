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
package opendap.wcs.v1_1_2;

import org.jdom.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 16, 2009
 * Time: 11:28:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerCapabilities {

    private static String[] sf = {"application/x-netcdf-cf1.0","application/x-dap-3.2"};



    /**
     *
     * @return
     * @param dapServer
     */
    public static String[] getSupportedFormatStrings(URL dapServer){
        return sf;
    }



    /**
     *
     * @param coverageID
     * @param fieldID
     * @return
     */
    static String[] getInterpolationMethods(String coverageID, String fieldID){
        String[] im = {"nearest"};
        return im;
        
    }


    public static void main(String[] args) throws Exception{


        String[] sf = getSupportedFormatStrings(null);
        for(String s : sf)
            System.out.println("Supported Format: "+s);


        String[] im = getInterpolationMethods(null,null);
        for(String s : im)
            System.out.println("InterpolationMethods: "+s);

    }
}
