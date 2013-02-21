/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
 * //
 * //
 * // Copyright (c) 2012 OPeNDAP, Inc.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.bes.dap4Responders;

import opendap.bes.dapResponders.BesApi;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/6/12
 * Time: 12:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class Version extends Dap4Responder {


    private Logger log;
    private static String defaultRequestSuffix = ".ver";



    public Version(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffix,besApi);
    }

    public Version(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public Version(String sysPath, String pathPrefix, String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());

        setServiceRoleId("http://services.opendap.org/dap4/server-version");
        setServiceTitle("Server Software Version.");
        setServiceDescription("An XML document containing detailed software version information for this server.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Dataset_Service_-_The_metadata");

        setNormativeMediaType(new ServiceMediaType("text","xml", getRequestSuffix()));

        log.debug("defaultRequestSuffix: '{}'", defaultRequestSuffix);

        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }




    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        BesApi besApi = getBesApi();

        log.debug("respondToHttpGetRequest() - Sending Version response...");

        response.setContentType(getServiceMediaType());
        response.setHeader("Content-Description", "dods_version");

        response.setStatus(HttpServletResponse.SC_OK);

        PrintStream ps = new PrintStream(response.getOutputStream());

        Document vdoc = besApi.getCombinedVersionDocument();

        if (vdoc == null) {
            throw new ServletException("Internal Error: Version Document not initialized.");
        }
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        //XMLOutputter xout = new XMLOutputter();
        xout.output(vdoc, ps);
        ps.flush();


        log.debug("respondToHttpGetRequest() - Sent Version response.");




        ps.flush();
        log.info("Sent {}",getServiceTitle());


    }




    /**
     * If we want to only check the relative URL and not ask the BES if it's a valid dataset then we
     * uncomment the folowing method. THis will override the one in the the parent class(es) so that the BES is
     * not queried for this response.
     *
     * @param relativeUrl
     * @return
     */
    /*
    @Override
    public boolean matches(String relativeUrl) {

        return matches(relativeUrl,false);

    }

    */



}
