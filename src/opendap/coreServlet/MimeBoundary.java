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
package opendap.coreServlet;

import java.rmi.server.UID;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: Aug 18, 2009
 * Time: 12:25:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class MimeBoundary {


    String boundary;

    public MimeBoundary(){
        boundary =    getNewMimeBoundary();

    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    private static String getNewMimeBoundary(){
        //Date date = new Date();
        return "Part_"+newUidString();
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    public String getEncapsulationBoundary(){
        //Date date = new Date();
        return "--"+boundary;
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    public String getClosingBoundary(){
        //Date date = new Date();
        return "\n--"+boundary+"--\n";
    }

    /**
     * This is a utility function that returns a new MIME boundary string suitable for use in a Multipart MIME respone.
     * <p><b>Do not confuse this method with <code>getMimeBoundary</code> </b></p>
     * @return Returns a NEW MIME Boundary string.
     */
    public String getBoundary(){
        //Date date = new Date();
        return boundary;
    }


    /**
     *
     * @return Returns a new UID String
     */
    public String newContentID(){
        //return newUidString();
        return new UID().toString()+"@opendap.org";
    }

    /**
     *
     * @return Returns a new UID String
     */
    public static String newUidString(){
        UID uid = new UID();

        byte[] val = uid.toString().getBytes();

        StringBuilder suid  = new StringBuilder();
        int v;

        for (byte aVal : val) {
            v = aVal;
            suid.append(Integer.toHexString(v));
        }

        return suid.toString();
    }


}
