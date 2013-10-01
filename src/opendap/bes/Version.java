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

package opendap.bes;

import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.ReqInfo;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.TreeSet;

/**
 * Contains the Version and UUID information for Hyrax Server.
 */
public class Version  {

    private static Logger log = org.slf4j.LoggerFactory.getLogger(Version.class);

    private static final Namespace BES_NS = opendap.namespaces.BES.BES_NS;



    private static String olfsVersion  = "@OlfsVersion@";
    private static String hyraxVersion = "@HyraxVersion@";




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

        olfs.setAttribute("version",olfsVersion);

        return (olfs);

    }

    /**
     * Returns a JDOM ELement containing the Hyrax version.
     * @return The version of Hyrax.
     */
    public static Element getHyraxVersionElement() {

        Element hyrax = new Element("Hyrax");

        hyrax.setAttribute("version",hyraxVersion);

        return (hyrax);

    }

    /**
     *  Produce the ServerUUID value used by the top level of Hyrax.
     * @return The UUID.
     */
    public static String getServerUUID(){
        return "e93c3d09-a5d9-49a0-a912-a0ca16430b91";
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



        String relativeUrl = ReqInfo.getLocalUrl(request);

        BesGroup besGroup = BESManager.getBesGroup(relativeUrl);

        TreeSet<String> commonDapVersions = besGroup.getCommonDapVersions();

        return ("dods/" + commonDapVersions.last());


    }

    /**
     * @param request The client request for which to return the verison.
     * @return A String containing the value of the XOPeNDAP-Server MIME header ascertained by querying
     *         the BES and conforming to the DAP4 specification.
     * @throws Exception If these is a problem getting the version document.
     */
    public static String getXOPeNDAPServerVersion(HttpServletRequest request)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);



        BesGroup besGroup = BESManager.getBesGroup(relativeUrl);

        TreeSet<String> componentVersions = besGroup.getGroupComponentVersions();

        if(componentVersions.isEmpty())
            return ("Server-Version-Unknown");


        StringBuilder xOpendapServerVersion = new StringBuilder();

        String comma = "";
        for(String componentVersion : componentVersions){
            xOpendapServerVersion.append(comma).append(componentVersion);
            comma= ", ";
        }

        return (xOpendapServerVersion.toString());

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


        String responseDAP = null;

        String clientDapVer = null;

        if (request != null)
            clientDapVer = request.getHeader("X-DAP");

        String relativeUrl = ReqInfo.getLocalUrl(request);


        BesGroup besGroup = BESManager.getBesGroup(relativeUrl);

        TreeSet<String> commonDapVersions = besGroup.getCommonDapVersions();

        for(String version: commonDapVersions){
            if (clientDapVer != null && clientDapVer.equals(version)){
                responseDAP = version;
            }
        }

        String highestVersionString = commonDapVersions.last();

        if (responseDAP == null)
            return (highestVersionString);

        return (responseDAP);


    }







    /**
     * Adds the response HTTP headers with the OPeNDAP version content.
     *
     * @param request Client request to serviced
     * @param response The response in which to set the headers.
     * @param besApi
     * @throws Exception If these is a problem getting the version document.
     */
    public static void setOpendapMimeHeaders(HttpServletRequest request, HttpServletResponse response, BesApi besApi)
            throws Exception{

        response.setHeader("XDODS-Server", getXDODSServerVersion(request));
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion(request));
        response.setHeader("X-DAP", getXDAPVersion(request));

    }


}
