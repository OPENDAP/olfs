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

import java.util.TreeMap;

/**
 * A minimal implementation of a globally-accessible set of Debug flags.
 *
 * @author John Caron, Nathan Potter
 */

public class Debug {
    static private TreeMap <String, Boolean> map = new TreeMap<String, Boolean>();
    static private boolean debug = false;

    static public boolean isSet(String flagName) {
        Boolean val;
        val = map.get(flagName);
        if ( val == null){
            if (debug) System.out.println("Debug.isSet new " + flagName);
            map.put(flagName, false);
            return false;
        }

        return  val;
    }

    static public void set(String flagName, boolean value) {

        map.put(flagName, value);
        if (debug) System.out.println("  Debug.set " + flagName + " " + value);
    }

    static public void clear() {
        map = new TreeMap<String, Boolean>();
    }

    static public java.util.Set keySet() {
        return map.keySet();
    }
}
