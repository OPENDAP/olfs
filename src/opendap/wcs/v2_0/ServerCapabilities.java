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

import opendap.wcs.v2_0.formats.*;
import org.jdom.Element;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
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

        rf = new Jpeg2000Format();
        _responseFormats.put(rf.name(),rf);

        rf = new Dap2DataFormat();
        _responseFormats.put(rf.name(),rf);
    }




    /**
     * Returns the names of the supported formats as the appear in the OperationsMetadata for GetCoverage.
     * @return
     * @param dapServer
     */
    public static Vector<String> getSupportedFormatNames(URL dapServer){
        Vector<String> supportedFormatNames = new Vector<>();
        supportedFormatNames.addAll(Collections.list(_responseFormats.keys())); // Confines use of ConcurrentHashMap to Map Interface and is compatible with Java-7 jre
        // supportedFormatNames.addAll(_responseFormats.keySet()); // Utilizes Java-8 call that borks this when run on Java 7 JRE
        return supportedFormatNames;
    }

    /**
     * Makes a lenient attempt to locate the requested format. If it can't be
     * worked out a null is returned.
     *
     *
     * @param name
     * @return
     */
    public static WcsResponseFormat getFormat(String name){

        name = name.toLowerCase();

        // If it's a slam dunk the woot.
        if(_responseFormats.containsKey(name))
            return _responseFormats.get(name);

        // Otherwise we try to be lenient about it first
        for(WcsResponseFormat wrf: _responseFormats.values()){

            // Case insensitive name match?
            if(wrf.name().equalsIgnoreCase(name))
                return wrf;

            // Case insensitive mime-type match because people might request using mime-type
            if(wrf.mimeType().equalsIgnoreCase(name))
                return wrf;

            // Hail Mary #1
            if(wrf.name().contains(name))
                return wrf;

            // Hail Mary #2
            if(wrf.mimeType().contains(name))
                return wrf;
        }
        // can't reach it...
        return null;
    }

    /**
     * Returns the wcs:ServiceMetadata section of the wcs:Capabilities response.
     * This section itemizes the return formats and we return the MIME types because
     * that makes sense, right?
     *
     * @return Returns the wcs:Contents section of the wcs:Capabilities response.
     * @throws WcsException   When bad things happen.
     * @throws InterruptedException
     */
    public static Element getServiceMetadata()  throws InterruptedException, WcsException {
        Element serviceMetadata = new Element("ServiceMetadata",WCS.WCS_NS);
        for(WcsResponseFormat wrf: _responseFormats.values()){
            Element formatSupported = new Element("formatSupported",WCS.WCS_NS);
            formatSupported.setText(wrf.mimeType());
            serviceMetadata.addContent(formatSupported);
        }
        return serviceMetadata;
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
