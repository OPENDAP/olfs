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
package opendap.wcs.v2_0;

import opendap.wcs.v2_0.formats.*;

import java.net.URL;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Sep 16, 2009
 * Time: 11:28:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerCapabilities {

    private static ConcurrentHashMap<String, WcsResponseFormat> _responseFormats;
    static {
        _responseFormats = new ConcurrentHashMap<>();

        WcsResponseFormat rf;

        rf = new NetCdfFormat();
        _responseFormats.put(rf.name(),rf);

        rf = new GeotiffFormat();
        _responseFormats.put(rf.name(),rf);

        rf = new Jpeg200Format();
        _responseFormats.put(rf.name(),rf);

        rf = new Dap2DataFormat();
        _responseFormats.put(rf.name(),rf);
    }




    /**
     *
     * @return
     * @param dapServer
     */
    public static Vector<String> getSupportedFormatNames(URL dapServer){
        Vector<String> supportedFormatNames = new Vector<>();
        supportedFormatNames.addAll(_responseFormats.keySet());
        return supportedFormatNames;
    }

    public static WcsResponseFormat getFormat(String name){
        return _responseFormats.get(name);
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


        Vector<String> sf = getSupportedFormatNames(null);
        for(String s : sf)
            System.out.println("Supported Format: "+s);


        String[] im = getInterpolationMethods(null,null);
        for(String s : im)
            System.out.println("InterpolationMethods: "+s);

    }
}
