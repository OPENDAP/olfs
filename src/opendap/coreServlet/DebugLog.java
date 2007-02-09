/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrex)" project.
//
//
// Copyright (c) 2006 OPeNDAP, Inc.
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

package opendap.coreServlet;

import java.io.*;

/**
 * A minimal implementation of a logging facility.
 *
 * @author John Caron
 */

public class DebugLog {

    static private PrintStream logger = null;
    static private ByteArrayOutputStream buff = null;

    public static  void println(String s) {
        if (logger != null)
            logger.println(s);
    }

    public static void printDODSException(OPeNDAPException de) {
        if (logger != null) {
            de.print(logger);
            de.printStackTrace(logger);
        }
    }

    public static void printThrowable(Throwable t) {
        if (logger != null) {
            logger.println(t.getMessage());
            t.printStackTrace(logger);
        }
    }

    public static void reset() {
        buff = new ByteArrayOutputStream();
        logger = new PrintStream(buff);
    }

    public static boolean isOn() {
        return (logger != null);
    }

    public static void close() {
        logger = null;
        buff = null;
    }

    public static String getContents() {
        if (buff == null)
            return "null";
        else {
            logger.flush();
            return buff.toString();
        }
    }

}
