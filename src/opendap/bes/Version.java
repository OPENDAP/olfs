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

package opendap.bes;

import opendap.bes.dapResponders.BesApi;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.Document;
import org.jdom.Namespace;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import opendap.coreServlet.ReqInfo;

import java.util.List;
import java.util.Iterator;

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
     *
     * @param request The client request for which to return the verison.
     * @return A string containing the value of the XDODS-Server MIME header as
     * ascertained by querying the BES.
     * @throws Exception If these is a problem getting the version document.
     */
    public static String getXDODSServerVersion(HttpServletRequest request, BesApi besApi)
            throws Exception {



        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource =  ReqInfo.getBesDataSourceID(relativeUrl);
        Document versionDoc = besApi.getVersionDocument(dataSource);

        if (versionDoc != null) {

            Element bes = versionDoc.getRootElement();
            List services = bes.getChildren("serviceVersion",BES_NS);

            if(services.isEmpty())
                log.error("The BES with prefix='"+bes.getAttributeValue("prefix")+"' has no defined services! " +
                        "(bes:serviceRef elements are missing");

            Element srvc;
            Element v;
            String ver;
            String highestVer="0.0";
            int result;
            Iterator i = services.iterator();
            while(i.hasNext()){
                srvc = (Element) i.next();

                if(srvc.getAttributeValue("name").equalsIgnoreCase("dap")){
                    for (Object o : srvc.getChildren("version",BES_NS)) {
                        v = (Element) o;
                        ver = v.getTextTrim();
                        result = ver.compareToIgnoreCase(highestVer);

                        if(result>0){
                            highestVer = ver;
                        }

                    }


                }

            }
            return ("dods/" + highestVer);
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
    public static String getXOPeNDAPServerVersion(HttpServletRequest request, BesApi besApi)
            throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String datasource =  ReqInfo.getBesDataSourceID(relativeUrl);
        Document versionDoc = besApi.getVersionDocument(datasource);

        if (versionDoc != null) {


            Element bes = versionDoc.getRootElement();
            List libraries = bes.getChildren("library",BES_NS);
            List modules = bes.getChildren("module",BES_NS);

            if(libraries.isEmpty())
                log.error("The BES with prefix='"+bes.getAttributeValue("prefix")+"' is running without code libraries! " +
                        "(bes:library elements are missing");

            if(modules.isEmpty())
                log.error("The BES with prefix='"+bes.getAttributeValue("prefix")+"' is running without modules! " +
                        "(bes:module elements are missing");

            String opsrv = "";
            Element lib, module;
            Element v;
            String ver;
            String highestVer="0.0";
            int result;
            boolean first = true;
            Iterator i = libraries.iterator();
            while(i.hasNext()){
                lib = (Element) i.next();

                if (!first)
                    opsrv += ",";
                else
                    first = false;

                opsrv += " " + lib.getAttributeValue("name") + "/" + lib.getTextTrim();
            }


            i = modules.iterator();
            while(i.hasNext()){
                module = (Element) i.next();

                if (!first)
                    opsrv += ",";
                else
                    first = false;

                opsrv += " " + module.getAttributeValue("name") + "/" + module.getTextTrim();
            }




                                      /*



            for (Object o : versionDoc.getRootElement().getChildren()) {
                Element pkg = (Element) o;
                 first = true;
                for (Object o1 : pkg.getChildren("lib")) {
                    Element lib = (Element) o1;
                    if (!first)
                        opsrv += ",";
                    opsrv += " " + lib.getChildTextTrim("name") + "/" + lib.getChildTextTrim("version");
                    first = false;
                }
            }
            */

            
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
    public static String getXDAPVersion(HttpServletRequest request, BesApi besApi) throws Exception {
        double hval = 0.0;
        String hver = "";

        String clientDapVer = null;

        if (request != null)
            clientDapVer = request.getHeader("XDAP");

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String datasource =  ReqInfo.getBesDataSourceID(relativeUrl);
        Document versionDoc = besApi.getVersionDocument(datasource);


        if (versionDoc != null) {


            Element bes = versionDoc.getRootElement();


            List services = bes.getChildren("serviceVersion",BES_NS);

            if(services.isEmpty())
                log.error("The BES with prefix='"+bes.getAttributeValue("prefix")+"' has no defined services! " +
                        "(bes:serviceRef elements are missing");

            String responseDAP = null;
            Element srvc;
            Element v;
            String ver;
            double vval;
            Iterator i = services.iterator();
            while(i.hasNext()){
                srvc = (Element) i.next();

                if(srvc.getAttributeValue("name").equalsIgnoreCase("dap")){
                    for (Object o : srvc.getChildren("version",BES_NS)) {
                        v = (Element) o;
                        ver = v.getTextTrim();
                        vval = Double.parseDouble(ver);
                        if (hval < vval) {
                            hval = vval;
                            hver = ver;
                        }

                        if (clientDapVer != null && clientDapVer.equals(ver))
                            responseDAP = ver;
                    }
                    

                }

            }
            if (responseDAP == null)
                return (hver);
            return (responseDAP);


               /*

            for (Object o : getVersionDocument(ReqInfo.getBesDataSourceID(request)).getRootElement().getChild("serviceVersion").getChild("DAP").getChildren("version")) {
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

            */
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
    public static void setOpendapMimeHeaders(HttpServletRequest request, HttpServletResponse response, BesApi besApi)
            throws Exception{

        response.setHeader("XDODS-Server", getXDODSServerVersion(request, besApi));
        response.setHeader("XOPeNDAP-Server", getXOPeNDAPServerVersion(request, besApi));
        response.setHeader("XDAP", getXDAPVersion(request, besApi));

    }


}
