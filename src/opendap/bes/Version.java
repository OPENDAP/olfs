/////////////////////////////////////////////////////////////////////////////
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

package opendap.bes;

import org.jdom.Element;
import org.jdom.Text;

/**
 * Contains the Version and UUID information for Hyrax Server.
 */
public class Version {


    private static String olfsVersion  = "1.2.0";
    private static String hyraxVersion = "1.2.0";





    public static String getOLFSVersionString() {
        return (olfsVersion);
    }

    public static String getHyraxVersionString() {
        return (hyraxVersion);
    }

    public static Element getOLFSVersionElement() {

        Element olfs = new Element("OLFS");

        Element lib = new Element("lib");
        Element name = new Element("name");
        Element ver = new Element("version");

        name.addContent(new Text("olfs"));
        lib.addContent(name);

        ver.addContent(new Text(olfsVersion));
        lib.addContent(ver);


        olfs.addContent(lib);

        return (olfs);

    }

    public static Element getHyraxVersionElement() {

        Element olfs = new Element("Hyrax");

        Element lib = new Element("lib");
        Element name = new Element("name");
        Element ver = new Element("version");

        name.addContent(new Text("Hyrax"));
        lib.addContent(name);

        ver.addContent(new Text(hyraxVersion));
        lib.addContent(ver);


        olfs.addContent(lib);

        return (olfs);

    }

    public static String getServerUUID(){
        return "e93c3d09-a5d9-49a0-a912-a0ca16430b91";
    }


    public static String getVersionStringForTHREDDSCatalog() {
        return "OPeNDAP Hyrax (" + Version.getHyraxVersionString() + ")" +
                "<font size='-5' color='#7A849E'> " +
                "ServerUUID=" + Version.getServerUUID() + "-catalog" +
                "</font><br />";

    }

}
