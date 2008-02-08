/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2007 OPeNDAP, Inc.
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
import org.jdom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import opendap.coreServlet.ReqInfo;

/**
 * Contains the Version and UUID information for Hyrax Server.
 */
public class Version  {


    private static String olfsVersion  = "1.4.0";
    private static String hyraxVersion = "1.4.0";





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





    private static Document getVersionDocument(String path) throws Exception{

        return BesAPI.getVersionDocument(path);
    }


    /**
     *
     * @param request The client request for which to return the verison.
     * @return A string containing the value of the XDODS-Server MIME header as ascertained
     *         by querying the BES.
     * @throws Exception
     */
    public static String getXDODSServerVersion(HttpServletRequest request)throws Exception {

        if (getVersionDocument(ReqInfo.getDataSource(request)) != null) {

            for (Object o : getVersionDocument(ReqInfo.getDataSource(request)).getRootElement().getChild("BES").getChildren("lib")) {
                Element e = (Element) o;
                if (e.getChildTextTrim("name").equalsIgnoreCase("libdap")) {
                    return ("dods/" + e.getChildTextTrim("version"));
                }
            }
        }

        return ("Server-Version-Unknown");

    }

    /**
     *
     *
     * @param request The client request for which to return the verison.
     * @return A String containing the value of the XOPeNDAP-Server MIME header ascertained by querying
     *         the BES and conforming to the DAP4 specification.
     * @throws Exception
     */
    public static String getXOPeNDAPServerVersion(HttpServletRequest request)throws Exception {
        if (getVersionDocument(ReqInfo.getDataSource(request)) != null) {

            String opsrv = "";

            for (Object o : getVersionDocument(ReqInfo.getDataSource(request)).getRootElement().getChildren()) {
                Element pkg = (Element) o;
                boolean first = true;
                for (Object o1 : pkg.getChildren("lib")) {
                    Element lib = (Element) o1;
                    if (!first)
                        opsrv += ",";
                    opsrv += " " + lib.getChildTextTrim("name") + "/" + lib.getChildTextTrim("version");
                    first = false;
                }
            }
            return (opsrv);
        }
        return ("Server-Version-Unknown");

    }


    /**
     *
     * @param request
     * @return A String containing the XDAP MIME header value that describes
     * the DAP specifcation that the server response conforms to.
     * @throws Exception
     */
    public static String getXDAPVersion(HttpServletRequest request) throws Exception {
        double hval = 0.0;
        String hver = "";

        String clientDapVer = null;

        if (request != null)
            clientDapVer = request.getHeader("XDAP");



        if (getVersionDocument(ReqInfo.getDataSource(request)) != null) {

            String responseDAP = null;

            for (Object o : getVersionDocument(ReqInfo.getDataSource(request)).getRootElement().getChild("Handlers").getChild("DAP").getChildren("version")) {
                Element v = (Element) o;
                String ver = v.getTextTrim();
                double vval = Double.parseDouble(ver);
                if (hval < vval) {
                    hval = vval;
                    hver = ver;
                }

                if (clientDapVer != null && clientDapVer.equals(ver))
                    responseDAP = ver;
            }
            if (responseDAP == null)
                return (hver);
            return (responseDAP);
        }

        return ("DAP-Version-Unknown");


    }



    public static void setOpendapMimeHeaders(HttpServletRequest request,
                                      HttpServletResponse response)
            throws Exception{

        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));

    }


}
