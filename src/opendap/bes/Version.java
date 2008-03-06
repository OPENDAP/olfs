/////////////////////////////////////////////////////////////////////////////
// This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
//
//
// Copyright (c) 2008 OPeNDAP, Inc.
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




    /**
     * Returns a String containing the OLFS version.
     * @return The version of OLFS.
     */
    public static String getOLFSVersionString() {
        return (olfsVersion);
    }

    /**
     * Returns a String containing the Hyrax version.
     * @return The version of Hyrax.
     */    public static String getHyraxVersionString() {
        return (hyraxVersion);
    }


    /**
     * Returns a JDOM ELement containing the OLFS version.
     * @return The version of OLFS.
     */
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

    /**
     * Returns a JDOM ELement containing the Hyrax version.
     * @return The version of Hyrax.
     */
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

    /**
     *  Produce the ServerUUID value used by the top level of Hyrax.
     * @return The UUID.
     */
    public static String getServerUUID(){
        return "e93c3d09-a5d9-49a0-a912-a0ca16430b91";
    }


    /**
     * Get's the version string that we add to the base of the THREDDS
     * generated catalog.html pages.
     *
     * @return A String containing a snippett of HTML that contains the
     * Hyrax version and ServerUUID .
     */
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
     * @return A string containing the value of the XDODS-Server MIME header as
     * ascertained by querying the BES.
     * @throws Exception If these is a problem getting the version document.
     */
    public static String getXDODSServerVersion(HttpServletRequest request)
            throws Exception {

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
     * @throws Exception If these is a problem getting the version document.
     */
    public static String getXOPeNDAPServerVersion(HttpServletRequest request)
            throws Exception {

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
     * Looks at an incoming client request and determines which version of the
     * DAP the client can understand.
     *
     * @param request The client request.
     * @return A String containing the XDAP MIME header value that describes
     * the DAP specifcation that the server response conforms to.
     * @throws Exception If these is a problem getting the version document.
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


    /**
     * Adds the response HTTP headers with the OPeNDAP version content.
     *
     * @param request Client request to serviced
     * @param response The response in which to set the headers.
     * @throws Exception If these is a problem getting the version document.
     */
    public static void setOpendapMimeHeaders(HttpServletRequest request,
                                      HttpServletResponse response)
            throws Exception{

        response.setHeader("XDODS-Server", Version.getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", Version.getXOPeNDAPServerVersion(request));
        response.setHeader("XDAP", Version.getXDAPVersion(request));

    }


}
