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
package opendap.bes.dap2Responders;

import opendap.bes.Version;
import opendap.bes.dap4Responders.Dap4Responder;
import opendap.bes.dap4Responders.MediaType;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.dap.User;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/31/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class NetcdfFileOut extends Dap4Responder {

    private Logger log;


    private static String _defaultRequestSuffix = ".nc";

    public NetcdfFileOut(String sysPath, BesApi besApi) {
        this(sysPath,null, _defaultRequestSuffix,besApi);
    }

    public NetcdfFileOut(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, _defaultRequestSuffix,besApi);
    }


    public NetcdfFileOut(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
        setServiceRoleId("http://services.opendap.org/dap2/netcdf-3");
        setServiceTitle("DAP2 NetCDF-3 File");
        setServiceDescription("DAP2 data returned in a NetCDF-3 file.");
        setServiceDescriptionLink("http://docs.opendap.org/index.php/DAP4_Web_Services#DAP4:_NetCDF_File-out_Service");

        setNormativeMediaType(new MediaType("application","x-netcdf", getRequestSuffix()));
        log.debug("Using RequestSuffix:              '{}'", getRequestSuffix());
        log.debug("Using CombinedRequestSuffixRegex: '{}'", getCombinedRequestSuffixRegex());

    }

    public boolean isDataResponder(){ return true; }
    public boolean isMetadataResponder(){ return false; }


    public void sendNormativeRepresentation(HttpServletRequest request, HttpServletResponse response) throws Exception {


        String relativeUrl = ReqInfo.getLocalUrl(request);
        String resourceID = getResourceId(relativeUrl, false);
        String fullSourceName = ReqInfo.getLocalUrl(request);
        String constraintExpression = ReqInfo.getConstraintExpression(request);

        BesApi besApi = getBesApi();


        log.debug("respondToHttpGetRequest(): Sending netCDF File Out response for dataset: " + resourceID + "?" +
                    constraintExpression);

        String downloadFileName = Scrub.fileName(fullSourceName.substring(fullSourceName.lastIndexOf("/")+1,fullSourceName.length()));
        Pattern startsWithNumber = Pattern.compile("[0-9].*");
        if(startsWithNumber.matcher(downloadFileName).matches())
            downloadFileName = "nc_"+downloadFileName;

        log.debug("respondToHttpGetRequest(): NetCDF file downloadFileName: " + downloadFileName );

        String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";

        response.setContentType(getNormativeMediaType().getMimeType());
        response.setHeader("Content-Disposition", contentDisposition);

        Version.setOpendapMimeHeaders(request, response, besApi);

        response.setStatus(HttpServletResponse.SC_OK);

        String xdap_accept = request.getHeader("XDAP-Accept");


        User user = new User(request);


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        if(!besApi.writeNetcdf3FileOut(resourceID,constraintExpression,xdap_accept,user.getMaxResponseSize(),os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
            os.write(msg.getBytes());
        }


        /*

        Document reqDoc =
                besApi.getRequestDocument(
                        BesApi.DAP2_DATA,
                        resourceID,
                        constraintExpression,
                        xdap_accept,
                        user.getMaxResponseSize(),
                        null,
                        null,
                        "netcdf",
                        BesApi.DAP2_ERRORS);


        XMLOutputter xmlo = new XMLOutputter(Format.getPrettyFormat());

        log.debug("_besApi.getRequestDocument() returned:\n "+xmlo.outputString(reqDoc));

        if(!besApi.besTransaction(resourceID,reqDoc,os,erros)){
            String msg = new String(erros.toByteArray());
            log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
            os.write(msg.getBytes());

        }
        */



        os.flush();
        log.info("Sent DAP2 data as netCDF file.");

    }


}
