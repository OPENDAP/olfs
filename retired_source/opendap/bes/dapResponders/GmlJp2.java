/*
 * /////////////////////////////////////////////////////////////////////////////
 * // This file is part of the "OPeNDAP 4 Data Server (aka Hyrax)" project.
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
 * // Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * //
 * // You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
 * /////////////////////////////////////////////////////////////////////////////
 */
package opendap.bes.dapResponders;

import opendap.bes.BesDapResponder;
import opendap.bes.Version;
import opendap.coreServlet.ReqInfo;
import opendap.coreServlet.Scrub;
import opendap.dap.User;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ndp
 * Date: 1/31/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
@Deprecated
public class GmlJp2 extends BesDapResponder {
    private Logger log;




    private static String defaultRequestSuffixRegex = "\\.jp2";

    public GmlJp2(String sysPath, BesApi besApi) {
        this(sysPath,null, defaultRequestSuffixRegex,besApi);
    }

    public GmlJp2(String sysPath, String pathPrefix, BesApi besApi) {
        this(sysPath,pathPrefix, defaultRequestSuffixRegex,besApi);
    }



    public GmlJp2(String sysPath, String pathPrefix,  String requestSuffixRegex, BesApi besApi) {
        super(sysPath, pathPrefix, requestSuffixRegex, besApi);
        log = org.slf4j.LoggerFactory.getLogger(this.getClass());
    }



    public void respondToHttpGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {




        String relativeUrl = ReqInfo.getLocalUrl(request);
        String dataSource = getBesApi().getBesDataSourceID(relativeUrl, getRequestSuffixMatchPattern(), false);
        String constraintExpression = ReqInfo.getConstraintExpression(request);
        String xmlBase = getXmlBase(request);

        BesApi besApi = getBesApi();



        log.debug("respondToHttpGetRequest(): Sending XML Data response For: " + dataSource +
                    "    CE: '" + constraintExpression + "'");


        String downloadFileName = Scrub.fileName(relativeUrl.substring(relativeUrl.lastIndexOf("/") + 1, relativeUrl.length()));



        String contentDisposition = " attachment; filename=\"" +downloadFileName+"\"";

        response.setContentType("image/jp2;application=gmljp2");
        response.setHeader("Content-Disposition", contentDisposition);

        Version.setOpendapMimeHeaders(request,response,besApi);
        response.setHeader("Content-Description", "GML_JPEG2000 Image");

        response.setStatus(HttpServletResponse.SC_OK);
        String xdap_accept = request.getHeader("XDAP-Accept");


        User user = new User(request);


        OutputStream os = response.getOutputStream();
        ByteArrayOutputStream erros = new ByteArrayOutputStream();


        boolean result = besApi.writeGmlJpeg2000DataResponse(
                        dataSource,
                        constraintExpression,
                        xdap_accept,
                        user.getMaxResponseSize(),
                        os,
                        erros);


        if(!result){
            String msg = new String(erros.toByteArray());
            log.error("respondToHttpGetRequest() encountered a BESError: "+msg);
            os.write(msg.getBytes());
        }



        os.flush();
        log.info("Sent GML_JPEG2000 Data response.");


    }
}
