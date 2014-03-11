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

package opendap.bes.dap4Responders.DataResponse;

import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.bes.dapResponders.BesApi;
import opendap.coreServlet.MimeBoundary;
import opendap.coreServlet.ReqInfo;
import opendap.dap.User;
import opendap.dap4.QueryParameters;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 9/5/12
 * Time: 10:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class NormativeDR extends Dap4Responder {
    private Logger log;
    private static String defaultRequestSuffix = ".dap";

    private String storedResultPrefix = "storedResults/";


    public NormativeDR(String sysPath, BesApi besApi) {
        this(sysPath, null, defaultRequestSuffix, besApi);
    }

    public NormativeDR(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath, pathPrefix, defaultRequestSuffix, besApi);
    }

    public NormativeDR(String sysPath, String pathPrefix, String requestSuffix, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffix, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());


        setServiceRoleId("http://services.opendap.org/dap4/data");
        setServiceTitle("DAP4 Data Response");
        setServiceDescription("DAP4 Data Response object.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_Data_Service");


        setNormativeMediaType(new MediaType("application","vnd.opendap.dap4.data", getRequestSuffix()));

        addAltRepResponder(new CsvDR(sysPath, pathPrefix, besApi));
        addAltRepResponder(new XmlDR(sysPath, pathPrefix, besApi));
        addAltRepResponder(new Netcdf3DR(sysPath, pathPrefix, besApi));
        addAltRepResponder(new Netcdf4DR(sysPath, pathPrefix, besApi));
        addAltRepResponder(new GeoTiffDR(sysPath, pathPrefix, besApi));
        addAltRepResponder(new GmlJpeg2000DR(sysPath, pathPrefix, besApi));


        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }


    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }





    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {

        String relativeUrl = ReqInfo.getLocalUrl(request);
        String xmlBase = getXmlBase(request);
        String resourceID = getResourceId(relativeUrl, false);

        QueryParameters qp = new  QueryParameters(request);


        BesApi besApi = getBesApi();

        log.debug("Sending {} for dataset: {}",getServiceTitle(),resourceID);

        response.setContentType(getNormativeMediaType().getMimeType());
        Version.setOpendapMimeHeaders(request, response, besApi);
        response.setHeader("Content-Description", getNormativeMediaType().getMimeType());
        // Commented because of a bug in the OPeNDAP C++ stuff...
        //response.setHeader("Content-Encoding", "plain");

        String xdap_accept = request.getHeader("XDAP-Accept");


        MimeBoundary mb = new MimeBoundary();
        String startID = mb.newContentID();

        User user = new User(request);



        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();




        boolean worked = besApi.writeDap4Data(
                resourceID,
                qp,
                xdap_accept,
                user.getMaxResponseSize(),
                xmlBase,
                startID,
                mb.getBoundary(),
                os,
                erros);


        if(!worked){
            String msg = new String(erros.toByteArray());
            log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }





        os.flush();
        log.info("Sent {}.",getServiceTitle());




    }




}
